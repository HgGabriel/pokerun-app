package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Raios
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

/** Um segmento do nível 1: um tier, com o quanto da coleção já o preencheu. */
data class TierNaEscada(
    val ordem: Int,
    val nome: String,
    /** A posição, de 1 em diante, da primeira espécie do tier. */
    val primeiraPosicao: Int,
    val total: Int,
    /** Quantas espécies do tier a coleção já alcançou. */
    val alcancadas: Int,
) {
    val fracao: Float get() = if (total == 0) 0f else alcancadas.toFloat() / total
}

/** Uma marca do nível 2: uma espécie do tier exibido. */
data class MarcaDaEscada(val posicao: Int, val numero: Int, val alcancada: Boolean, val atual: Boolean)

/**
 * A escada da temporada (docs/02 §5): os tiers como segmentos, a **posição** que pode
 * recuar (RN-37) e a **coleção** que não recua (XP-06). Os dois níveis saem dela.
 *
 * Nada aqui é literal: quantos tiers, quantas espécies e o número da Pokédex de cada uma
 * vêm da temporada (RN-43).
 */
data class EscadaDeProgresso(
    val tiers: List<TierNaEscada>,
    val posicao: Int,
    val colecaoMax: Int,
    val totalEspecies: Int,
    private val numeros: List<Int>,
) {
    /** A ordem do tier da espécie na posição atual. */
    val tierAtual: Int
        get() = tiers.firstOrNull { posicao in it.primeiraPosicao until it.primeiraPosicao + it.total }?.ordem
            ?: tiers.firstOrNull()?.ordem
            ?: 0

    /** Há recuo quando a posição ficou atrás da coleção (RN-37). */
    val recuou: Boolean get() = posicao < colecaoMax

    /** O número da Pokédex da espécie numa posição da escada. */
    fun numeroNaPosicao(posicao: Int): Int? = numeros.getOrNull(posicao - 1)

    /** As espécies do tier [ordem], na ordem da escada: o nível 2. */
    fun marcasDoTier(ordem: Int): List<MarcaDaEscada> {
        val tier = tiers.firstOrNull { it.ordem == ordem } ?: return emptyList()
        return (0 until tier.total).map { indice ->
            val posicaoDaMarca = tier.primeiraPosicao + indice
            MarcaDaEscada(
                posicao = posicaoDaMarca,
                numero = numeros[posicaoDaMarca - 1],
                alcancada = posicaoDaMarca <= colecaoMax,
                atual = posicaoDaMarca == posicao,
            )
        }
    }
}

/**
 * // RN-37
 *
 * A escada de [temporada] para uma [posicao] e uma [colecaoMax]. A coleção nunca fica
 * atrás da posição: um documento sem `colecao_max` ainda (antes de `F4-T12`) lê como a
 * própria posição, e não como uma escada com recuo que nunca houve.
 */
fun escadaDe(temporada: Temporada, posicao: Int, colecaoMax: Int): EscadaDeProgresso {
    val ordenados = temporada.tiers.sortedBy { it.ordem }
    val numeros = ordenados.flatMap { it.especies }
    val total = numeros.size
    val posicaoValida = posicao.coerceIn(1, maxOf(1, total))
    val colecao = maxOf(colecaoMax, posicaoValida).coerceAtMost(maxOf(1, total))

    var primeira = 1
    val tiers = ordenados.map { tier ->
        val quantas = tier.especies.size
        TierNaEscada(
            ordem = tier.ordem,
            nome = tier.nome,
            primeiraPosicao = primeira,
            total = quantas,
            alcancadas = (colecao - (primeira - 1)).coerceIn(0, quantas),
        ).also { primeira += quantas }
    }
    return EscadaDeProgresso(tiers, posicaoValida, colecao, total, numeros)
}

/** O tier sob uma fração (0 a 1) da largura do nível 1, que tem segmentos de largura igual. */
fun tierNaFracao(fracao: Float, escada: EscadaDeProgresso): Int {
    if (escada.tiers.isEmpty()) return 0
    val indice = floor(fracao.coerceIn(0f, 1f) * escada.tiers.size).toInt().coerceAtMost(escada.tiers.lastIndex)
    return escada.tiers[indice].ordem
}

/** A posição sob uma fração da largura do nível 1: o tier pelo segmento, a espécie pela fração dentro dele. */
fun posicaoNaFracao(fracao: Float, escada: EscadaDeProgresso): Int {
    if (escada.tiers.isEmpty()) return 1
    val x = fracao.coerceIn(0f, 1f) * escada.tiers.size
    val indice = floor(x).toInt().coerceAtMost(escada.tiers.lastIndex)
    val tier = escada.tiers[indice]
    val dentro = (x - indice).coerceIn(0f, 1f)
    val especie = floor(dentro * tier.total).toInt().coerceIn(0, tier.total - 1)
    return tier.primeiraPosicao + especie
}

/** As três variações de docs/02 §5. A completa tem o arraste, porque tem a grade para rolar. */
enum class VarianteDaEscada { COMPACTA, MEDIA, COMPLETA }

private val AlturaDoNivel1 = 48.dp
private val AlturaDaTrilha = 12.dp
private val AlturaDoNivel2 = 36.dp
private val EspessuraDaMarca = 2.dp
private val AlturaDaMarca = 16.dp
private val EspessuraDaPosicao = 3.dp
private val FolgaEntreSegmentos = 2.dp
private val EspacoEntreLinhas = 8.dp

/**
 * O elemento assinatura (docs/02 §5), em dois níveis, desenhado em `Canvas`.
 *
 * - **Nível 1:** um segmento de largura igual por tier, enchido pela coleção em `destaque`
 *   sobre a trilha em `superficie-afundada`, e a posição como marca vertical mais alta em
 *   `texto-primario`. Com recuo a posição fica **atrás** da coleção.
 * - **Nível 2:** as espécies de [tierExibido], em marcas finas: alcançadas em `destaque`,
 *   as outras em `borda-controle`.
 * - **O nível 1 é um único controle** de 48dp com semântica de slider: o toque resolve
 *   para o tier mais próximo ([aoEscolherTier]). Marca de espécie abaixo de 48dp **não**
 *   é alvo de toque, e a seleção de espécie é da grade (docs/02 §5).
 * - **Arrastar** (só na [VarianteDaEscada.COMPLETA]) move um cursor com a leitura
 *   `#087 · Marowak · Tier 5`; soltar chama [aoSoltarEm] com a posição.
 * - **O desenho não fala:** a semântica é uma frase, no padrão de docs/02 §5.
 *
 * @param nomeDaEspecie o nome em inglês pelo número da Pokédex (D-10), se já se sabe.
 */
@Composable
fun Escada(
    escada: EscadaDeProgresso,
    variante: VarianteDaEscada,
    modifier: Modifier = Modifier,
    tierExibido: Int = escada.tierAtual,
    aoEscolherTier: (Int) -> Unit = {},
    nomeDaEspecie: (Int) -> String? = { null },
    aoSoltarEm: (Int) -> Unit = {},
) {
    if (escada.tiers.isEmpty()) return
    val exibido = escada.tiers.firstOrNull { it.ordem == tierExibido } ?: escada.tiers.first()
    var cursor by remember { mutableStateOf<Int?>(null) }

    val atual = escada.tiers.first { it.ordem == escada.tierAtual }
    val proximoNumero = escada.numeroNaPosicao(escada.posicao + 1)
    val proximoNome = proximoNumero?.let(nomeDaEspecie)

    val descricao = buildString {
        append(stringResource(R.string.escada_descricao_tier, atual.ordem, escada.tiers.size, atual.nome))
        append(' ')
        append(stringResource(R.string.escada_descricao_tier_especies, atual.alcancadas, atual.total))
        append(' ')
        append(stringResource(R.string.escada_descricao_posicao, escada.posicao, escada.totalEspecies))
        if (escada.recuou) {
            append(' ')
            append(stringResource(R.string.escada_descricao_colecao, escada.colecaoMax))
        }
        if (proximoNome != null) {
            append(' ')
            append(stringResource(R.string.escada_descricao_alvo, proximoNome))
        }
    }
    val aberta = stringResource(R.string.escada_descricao_inicio, descricao)

    val cores = LocalCoresPokerun.current
    val trilha = cores.superficieAfundada
    val colecao = MaterialTheme.colorScheme.primary
    val tinta = MaterialTheme.colorScheme.onSurface
    val naoAlcancada = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        if (variante == VarianteDaEscada.COMPLETA) {
            val lido = cursor
            val leitura = lido?.let { posicao ->
                val numero = escada.numeroNaPosicao(posicao)
                val tier = escada.tiers.first { posicao in it.primeiraPosicao until it.primeiraPosicao + it.total }
                val nome = numero?.let(nomeDaEspecie)
                when {
                    numero == null -> ""
                    nome != null -> stringResource(R.string.escada_cursor_nome, numero, nome, tier.ordem)
                    else -> stringResource(R.string.escada_cursor, numero, tier.ordem)
                }
            }
            Text(
                text = leitura ?: stringResource(R.string.escada_arraste),
                style = MaterialTheme.typography.labelMedium,
                color = if (leitura != null) tinta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(EspacoEntreLinhas / 2))
        }

        // Nível 1: um controle só, de 48dp.
        // A compacta (o card da Home) é leitura, e não controle: não consome o toque, que é
        // da ficha inteira (a porta para a Pokédex), e não anuncia slider.
        val controle = variante != VarianteDaEscada.COMPACTA
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDoNivel1)
                .clearAndSetSemantics {
                    contentDescription = aberta
                    if (controle) {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = exibido.ordem.toFloat(),
                            range = 1f..escada.tiers.size.toFloat(),
                            steps = (escada.tiers.size - 2).coerceAtLeast(0),
                        )
                        setProgress { valor ->
                            aoEscolherTier(valor.roundToInt().coerceIn(1, escada.tiers.size))
                            true
                        }
                    }
                }
                .pointerInput(escada, controle) {
                    if (!controle) return@pointerInput
                    detectTapGestures { toque ->
                        aoEscolherTier(tierNaFracao(toque.x / size.width, escada))
                    }
                }
                .pointerInput(escada, variante) {
                    if (variante != VarianteDaEscada.COMPLETA) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { toque -> cursor = posicaoNaFracao(toque.x / size.width, escada) },
                        onHorizontalDrag = { mudanca, _ -> cursor = posicaoNaFracao(mudanca.position.x / size.width, escada) },
                        onDragEnd = {
                            cursor?.let(aoSoltarEm)
                            cursor = null
                        },
                        onDragCancel = { cursor = null },
                    )
                },
        ) {
            val folga = FolgaEntreSegmentos.toPx()
            val segmento = size.width / escada.tiers.size
            val topo = (size.height - AlturaDaTrilha.toPx()) / 2
            val raio = CornerRadius(Raios.dado.toPx())

            escada.tiers.forEachIndexed { indice, tier ->
                val x = segmento * indice + folga / 2
                val largura = segmento - folga
                drawRoundRect(trilha, Offset(x, topo), Size(largura, AlturaDaTrilha.toPx()), raio)
                if (tier.fracao > 0f) {
                    drawRoundRect(colecao, Offset(x, topo), Size(largura * tier.fracao, AlturaDaTrilha.toPx()), raio)
                }
                if (tier.ordem == exibido.ordem) {
                    // O tier exibido no nível 2 ganha um filete sob o segmento.
                    drawRoundRect(tinta, Offset(x, topo + AlturaDaTrilha.toPx() + 3.dp.toPx()), Size(largura, 2.dp.toPx()), raio)
                }
            }

            // A posição: marca vertical mais alta, no meio da espécie dentro do segmento dela.
            val dono = escada.tiers.first { escada.posicao in it.primeiraPosicao until it.primeiraPosicao + it.total }
            val dentro = (escada.posicao - dono.primeiraPosicao + 0.5f) / dono.total
            val xDaPosicao = segmento * (escada.tiers.indexOf(dono) + dentro)
            val altura = AlturaDaTrilha.toPx() * 2.2f
            drawRoundRect(
                tinta,
                Offset(xDaPosicao - EspessuraDaPosicao.toPx() / 2, (size.height - altura) / 2),
                Size(EspessuraDaPosicao.toPx(), altura),
                raio,
            )
            cursor?.let { posicaoDoCursor ->
                val tierDoCursor = escada.tiers.first { posicaoDoCursor in it.primeiraPosicao until it.primeiraPosicao + it.total }
                val interno = (posicaoDoCursor - tierDoCursor.primeiraPosicao + 0.5f) / tierDoCursor.total
                val xDoCursor = segmento * (escada.tiers.indexOf(tierDoCursor) + interno)
                drawRoundRect(
                    colecao,
                    Offset(xDoCursor - EspessuraDaPosicao.toPx() / 2, 0f),
                    Size(EspessuraDaPosicao.toPx(), size.height),
                    raio,
                )
            }
        }

        // O número de cada tier, sob o segmento dele.
        Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            escada.tiers.forEach { tier ->
                Text(
                    text = tier.ordem.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tier.ordem == exibido.ordem) tinta else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Na compacta o nome do tier centra, porque ela mora no corpo centrado da ficha de
        // espécie, e um rótulo à esquerda quebrava o eixo do objeto colecionável. Média e
        // completa vivem em colunas alinhadas à esquerda, e ficam como estavam.
        val compacta = variante == VarianteDaEscada.COMPACTA
        Text(
            text = stringResource(R.string.escada_tier_nome, exibido.ordem, exibido.nome),
            style = MaterialTheme.typography.labelMedium,
            color = tinta,
            textAlign = if (compacta) TextAlign.Center else null,
            modifier = Modifier
                .then(if (compacta) Modifier.fillMaxWidth() else Modifier)
                .clearAndSetSemantics { },
        )

        if (variante != VarianteDaEscada.COMPACTA) {
            Spacer(Modifier.height(EspacoEntreLinhas))
            val marcas = escada.marcasDoTier(exibido.ordem)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AlturaDoNivel2)
                    .clearAndSetSemantics { },
            ) {
                if (marcas.isEmpty()) return@Canvas
                val passo = size.width / marcas.size
                val espessura = EspessuraDaMarca.toPx()
                val raio = CornerRadius(Raios.dado.toPx())
                marcas.forEachIndexed { indice, marca ->
                    val x = passo * indice + passo / 2 - espessura / 2
                    val cor = if (marca.alcancada) colecao else naoAlcancada
                    val alta = marca.atual
                    val altura = if (alta) size.height else AlturaDaMarca.toPx()
                    drawRoundRect(
                        if (alta) tinta else cor,
                        Offset(if (alta) x - 0.5.dp.toPx() else x, size.height - altura),
                        Size(if (alta) EspessuraDaPosicao.toPx() else espessura, altura),
                        raio,
                    )
                }
            }
            val alcancadas = marcas.count { it.alcancada }
            val proximoDoTier = marcas.firstOrNull { !it.alcancada }?.numero?.let(nomeDaEspecie)
            Text(
                text = if (proximoDoTier != null) {
                    stringResource(R.string.escada_nivel2_alvo, alcancadas, marcas.size, proximoDoTier)
                } else {
                    stringResource(R.string.escada_nivel2, alcancadas, marcas.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.heightIn(min = 24.dp).clearAndSetSemantics { },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val TemporadaDoPreview = Temporada(
    id = "kanto-2026",
    nome = "Kanto 2026",
    regiao = "Kanto",
    inicio = Instant.parse("2026-01-01T03:00:00Z"),
    fim = Instant.parse("2027-01-01T03:00:00Z"),
    fuso = ZoneId.of("America/Sao_Paulo"),
    ativa = true,
    totalEspecies = 150,
    tiers = listOf(21 to 100, 26 to 250, 19 to 500, 25 to 800, 29 to 1300, 15 to 2200, 10 to 4000, 5 to 8000)
        .let { custos ->
            var numero = 0
            custos.mapIndexed { i, (quantas, xp) ->
                Tier(i + 1, listOf("Casulo", "Primeiros Passos", "Trote", "Fôlego", "Vento a Favor", "Liga Pokémon", "Velocidade Extrema", "Lenda")[i], "", List(quantas) { ++numero }, xp)
            }
        },
)

@Composable
private fun EscadaPreview(variante: VarianteDaEscada, posicao: Int, colecao: Int) {
    PokerunTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Escada(
                escada = escadaDe(TemporadaDoPreview, posicao, colecao),
                variante = variante,
                nomeDaEspecie = { "Especie $it" },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EscadaCompactaPreview() = EscadaPreview(VarianteDaEscada.COMPACTA, posicao = 87, colecao = 87)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EscadaCompletaComRecuoPreview() = EscadaPreview(VarianteDaEscada.COMPLETA, posicao = 107, colecao = 120)

@Preview(showBackground = true, widthDp = 320, fontScale = 2.0f)
@Composable
private fun EscadaFonteGrandePreview() = EscadaPreview(VarianteDaEscada.MEDIA, posicao = 87, colecao = 87)
