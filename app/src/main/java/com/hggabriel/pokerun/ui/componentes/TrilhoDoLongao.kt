package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Raios
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * O trilho do longão da `PlanDetailScreen` (r2 de 26/10/2026, docs/03 §3.7): o plano
 * inteiro numa figura, e a régua que leva a edição até a semana.
 *
 * A geometria é do sketch `gerar-detalhe-r2.py` (função `grafico` e `regua`), em dp, e mora
 * em funções puras: é a parte que a revisão de olho não confere — uma coluna que encaixa na
 * vizinha ao arrastar, ou um rótulo repetido, compilam igual.
 */

/** Altura do gráfico e da linha de base, em dp. O gráfico não escala com a fonte (docs/02 §8, item 9). */
internal const val ALTURA_DO_TRILHO = 196f
internal const val LINHA_DE_BASE_DO_TRILHO = 180f

/** O espaço entre duas colunas, em dp. */
internal const val VAO_DO_TRILHO = 2f

/** A folga acima da maior coluna, para o rótulo e a data da prova. */
internal const val FOLGA_DO_TOPO_DO_TRILHO = 34f

/** A régua tem a altura do alvo de toque. A faixa inteira é o alvo (docs/02 §8, item 2). */
private val AlturaDaRegua = Espacos.alvoToque

private const val OPACIDADE_DA_MARCA_FUTURA = 0.3f
private val TracoDoOutro = 2.5.dp
private val AfastamentoDoRotulo = 4.dp

/** A pílula na fração da coluna, e a sua barra na fração da pílula. */
private const val LARGURA_DA_PILULA = 0.62f
private const val LARGURA_DA_SUA_BARRA = 0.5f

/** A opacidade da cor da fase no planejado, no topo e na base da pílula. */
private val OPACIDADE_FEITA = 0.22f to 0.45f
private val OPACIDADE_FUTURA = 0.1f to 0.2f
private val OPACIDADE_SELECIONADA = 0.5f to 0.85f

private val RecheioDoBalaoH = 6.dp
private val RecheioDoBalaoV = 3.dp

/** A fase da semana no plano. A prova usa `texto-primario`; as outras duas têm token. */
enum class FaseDoPlano { BASE, POLIMENTO, PROVA }

/** Onde a semana está no tempo do plano. No encerrado, todas são passadas. */
enum class TempoDaSemana { PASSADA, CORRENTE, FUTURA }

/**
 * Uma coluna do trilho: uma semana.
 *
 * @property planejadoKm o contorno. Nulo não desenha coluna (plano antigo, de antes da
 *   DA-10, com a 2ª de taper sem longão).
 * @property seuKm a barra vermelha: o longão de **quem olha**.
 * @property outroKm o traço em tinta: o longão da outra pessoa (D8).
 */
data class ColunaDoTrilho(
    val numero: Int,
    val fase: FaseDoPlano,
    val tempo: TempoDaSemana,
    val planejadoKm: Double?,
    val seuKm: Double?,
    val outroKm: Double?,
)

/**
 * As colunas lado a lado, na largura dada, com [vao] entre elas: `(L − vão·(N−1)) / N`.
 * É a mesma conta no gráfico, na régua e nas células do grupo, para as três alinharem.
 */
internal data class GeometriaDoTrilho(val larguraDaColuna: Float, val vao: Float, val colunas: Int) {
    fun x(indice: Int): Float = indice * (larguraDaColuna + vao)

    fun centro(indice: Int): Float = x(indice) + larguraDaColuna / 2

    /** O recuo da barra de cada lado: `max(3, 20% da coluna)`. É o que deixa o contorno visível ao lado dela. */
    val recuoDaBarra: Float get() = maxOf(3f, larguraDaColuna * 0.2f)

    /** A coluna sob o dedo, com encaixe na mais próxima. Fora da faixa, a da ponta. */
    fun indiceNoPonto(x: Float): Int =
        floor((x + vao / 2) / (larguraDaColuna + vao)).toInt().coerceIn(0, colunas - 1)
}

internal fun geometriaDoTrilho(largura: Float, colunas: Int, vao: Float = VAO_DO_TRILHO): GeometriaDoTrilho {
    val n = colunas.coerceAtLeast(1)
    return GeometriaDoTrilho((largura - vao * (n - 1)) / n, vao, n)
}

/**
 * O topo da escala: o maior entre o planejado e os feitos. Quem corre mais que o plano
 * passa do contorno, e a barra precisa caber.
 */
internal fun maximoDoTrilho(colunas: List<ColunaDoTrilho>): Double =
    colunas.flatMap { listOfNotNull(it.planejadoKm, it.seuKm, it.outroKm) }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

/** A altura de [km] acima da linha de base, em dp. */
internal fun alturaNoTrilho(km: Double, maximo: Double): Float =
    (km / maximo * (LINHA_DE_BASE_DO_TRILHO - FOLGA_DO_TOPO_DO_TRILHO)).toFloat()

/** O pico: a maior coluna planejada fora da prova. No empate, a primeira. */
internal fun indiceDoPico(colunas: List<ColunaDoTrilho>): Int? =
    colunas.withIndex()
        .filter { it.value.fase != FaseDoPlano.PROVA && it.value.planejadoKm != null }
        .maxByOrNull { it.value.planejadoKm!! }
        ?.index

/**
 * As colunas que ganham rótulo, na ordem de prioridade: a selecionada, a prova e o pico.
 * Rótulo que coincide com outro não se repete.
 */
internal fun colunasRotuladas(colunas: List<ColunaDoTrilho>, selecionada: Int): List<Int> {
    val indiceSelecionado = colunas.indexOfFirst { it.numero == selecionada }
    val prova = colunas.indexOfLast { it.fase == FaseDoPlano.PROVA }
    return listOfNotNull(
        indiceSelecionado.takeIf { it >= 0 },
        prova.takeIf { it >= 0 },
        indiceDoPico(colunas),
    ).distinct().filter { colunas[it].planejadoKm != null }
}

/**
 * // docs/02 §8, item 5
 *
 * O que o leitor de tela diz de uma coluna: *"Semana 4, base. Longão planejado 7,9 km.
 * Você, 8 km. jhusilva, 7,5 km."* Cada parte já vem como frase; a parte nula some, e é
 * assim que a semana sem feito omite o nome.
 */
internal fun descricaoDaColuna(partes: List<String?>): String = partes.filterNotNull().joinToString(" ")

/**
 * O gráfico do longão por semana, a régua de semanas e a legenda das fases (D6, D8).
 *
 * - **Trilho em `superficie-afundada`:** a escala inteira, atrás de cada coluna.
 * - **Pílula na cor da fase:** o longão planejado, cheio e em degradê. A semana futura é
 *   mais clara; a selecionada, mais forte e com o balão do número.
 * - **Barra vermelha estreita:** o seu longão feito. Passa do planejado quando você correu
 *   mais que o plano.
 * - **Traço em tinta:** o longão da outra pessoa. Tinta sobre vermelho mede 3,7:1.
 * - **A cor segue quem olha:** o vermelho é sempre de quem está com o aparelho.
 *
 * **A régua é um controle ajustável**, e a faixa inteira é o alvo: as colunas têm ~21dp,
 * abaixo do piso de 48, então a interação muda de forma, e não o piso (docs/02 §8,
 * item 2). Tocar ou arrastar encaixa na coluna mais próxima; o TalkBack anuncia
 * `Semana 7 de 16`, e os gestos de ajustar sobem e descem uma semana.
 *
 * @param rotuloDaProva o dia da prova, em caixa alta (`31 DEZ`), acima da coluna dela.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrilhoDoLongao(
    colunas: List<ColunaDoTrilho>,
    selecionada: Int,
    nomeDoOutro: String?,
    rotuloDaProva: String,
    aoSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (colunas.isEmpty()) return
    val esquema = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espacos.margem),
            verticalArrangement = Arrangement.spacedBy(Espacos.minimo),
        ) {
            ItemDaLegenda(stringResource(R.string.trilho_voce)) {
                Canvas(Modifier.size(width = 6.dp, height = 12.dp)) {
                    drawRoundRect(esquema.primary, cornerRadius = CornerRadius(1.dp.toPx()))
                }
            }
            if (nomeDoOutro != null) {
                ItemDaLegenda(nomeDoOutro) {
                    Canvas(Modifier.size(width = 14.dp, height = 3.dp)) {
                        drawRoundRect(esquema.onSurface, cornerRadius = CornerRadius(1.dp.toPx()))
                    }
                }
            }
        }

        Spacer(Modifier.height(Espacos.interno))

        Grafico(colunas, selecionada, nomeDoOutro, rotuloDaProva)
        Regua(colunas, selecionada, aoSelecionar)
    }
}

@Composable
private fun corDaFase(fase: FaseDoPlano): Color = when (fase) {
    FaseDoPlano.BASE -> LocalCoresPokerun.current.faseBase
    FaseDoPlano.POLIMENTO -> LocalCoresPokerun.current.fasePolimento
    FaseDoPlano.PROVA -> MaterialTheme.colorScheme.onSurface
}

private fun rotuloDaFase(fase: FaseDoPlano): Int = when (fase) {
    FaseDoPlano.BASE -> R.string.trilho_fase_base
    FaseDoPlano.POLIMENTO -> R.string.trilho_fase_polimento
    FaseDoPlano.PROVA -> R.string.trilho_fase_prova
}

@Composable
private fun ItemDaLegenda(texto: String, amostra: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        amostra()
        Spacer(Modifier.width(6.dp))
        Text(
            text = texto,
            style = Estilos.legenda,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
private fun Grafico(colunas: List<ColunaDoTrilho>, selecionada: Int, nomeDoOutro: String?, rotuloDaProva: String) {
    val cores = LocalCoresPokerun.current
    val esquema = MaterialTheme.colorScheme
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    // O gráfico não escala com a fonte: os rótulos medem em `fontScale` 1.
    val semEscala = Density(densidade.density, fontScale = 1f)
    val corDaFase = FaseDoPlano.entries.associateWith { corDaFase(it) }
    val estiloDoRotulo = Estilos.numeroS.copy(fontSize = 12.sp, lineHeight = 16.sp, color = esquema.onSurface)
    val estiloDoBalao = estiloDoRotulo.copy(color = esquema.surface)
    val estiloDaData = Estilos.rotulo.copy(color = esquema.onSurfaceVariant)
    val descricoes = colunas.map { descricaoFalada(it, nomeDoOutro) }

    Box(modifier = Modifier.fillMaxWidth().height(ALTURA_DO_TRILHO.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dp = density
            val g = geometriaDoTrilho(size.width / dp, colunas.size)
            val maximo = maximoDoTrilho(colunas)
            val base = LINHA_DE_BASE_DO_TRILHO * dp
            fun topo(km: Double) = base - alturaNoTrilho(km, maximo) * dp
            // A pílula ocupa o miolo da coluna; o resto é o vão até a vizinha.
            val largura = g.larguraDaColuna * dp * LARGURA_DA_PILULA
            val margem = (g.larguraDaColuna * dp - largura) / 2
            fun esquerda(i: Int) = g.x(i) * dp + margem

            // O trilho: a pílula clara da escala inteira, atrás de cada coluna.
            val topoDaEscala = FOLGA_DO_TOPO_DO_TRILHO * dp
            colunas.indices.forEach { i ->
                drawRoundRect(
                    color = cores.superficieAfundada,
                    topLeft = Offset(esquerda(i), topoDaEscala),
                    size = Size(largura, base - topoDaEscala),
                    cornerRadius = CornerRadius(largura / 2),
                )
            }

            // O planejado: a pílula cheia na cor da fase, em degradê do topo para a base. A
            // semana futura vem mais clara e a selecionada mais forte.
            colunas.forEachIndexed { i, c ->
                val km = c.planejadoKm ?: return@forEachIndexed
                val y = topo(km)
                val cor = corDaFase.getValue(c.fase)
                val (cima, baixo) = when {
                    c.numero == selecionada -> OPACIDADE_SELECIONADA
                    c.tempo == TempoDaSemana.FUTURA -> OPACIDADE_FUTURA
                    else -> OPACIDADE_FEITA
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to cor.copy(alpha = cima),
                        1f to cor.copy(alpha = baixo),
                        startY = y,
                        endY = base,
                    ),
                    topLeft = Offset(esquerda(i), y),
                    size = Size(largura, base - y),
                    cornerRadius = CornerRadius(largura / 2),
                )
            }

            // Você: a pílula vermelha estreita, no meio da do planejado.
            val larguraDaBarra = largura * LARGURA_DA_SUA_BARRA
            val recuo = (largura - larguraDaBarra) / 2
            colunas.forEachIndexed { i, c ->
                val km = c.seuKm ?: return@forEachIndexed
                val y = topo(km)
                drawRoundRect(
                    color = esquema.primary,
                    topLeft = Offset(esquerda(i) + recuo, y),
                    size = Size(larguraDaBarra, base - y),
                    cornerRadius = CornerRadius(larguraDaBarra / 2),
                )
            }

            // A outra pessoa: o traço em tinta, um pouco mais largo que a pílula.
            val espessura = TracoDoOutro.toPx()
            val sobra = 3.dp.toPx()
            colunas.forEachIndexed { i, c ->
                val km = c.outroKm ?: return@forEachIndexed
                drawRoundRect(
                    color = esquema.onSurface,
                    topLeft = Offset(esquerda(i) - sobra, topo(km) - espessura / 2),
                    size = Size(largura + 2 * sobra, espessura),
                    cornerRadius = CornerRadius(espessura / 2),
                )
            }

            rotulos(colunas, selecionada, g, dp, ::topo, medidor, semEscala, estiloDoRotulo, estiloDoBalao, esquema.onSurface, estiloDaData, rotuloDaProva)
        }

        // Um nó por coluna, percorrível, por cima do desenho (docs/02 §8, item 5).
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(VAO_DO_TRILHO.dp),
        ) {
            descricoes.forEach { texto ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clearAndSetSemantics { contentDescription = texto },
                )
            }
        }
    }
}

@Composable
private fun descricaoFalada(coluna: ColunaDoTrilho, nomeDoOutro: String?): String {
    val locale = LocaleDoApp
    return descricaoDaColuna(
        listOf(
            stringResource(R.string.trilho_coluna_semana, coluna.numero, stringResource(rotuloDaFase(coluna.fase))),
            coluna.planejadoKm?.let { stringResource(R.string.trilho_coluna_planejado, formatarKm(it, locale)) },
            coluna.seuKm?.let { stringResource(R.string.trilho_coluna_voce, formatarKm(it, locale)) },
            coluna.outroKm?.takeIf { nomeDoOutro != null }?.let {
                stringResource(R.string.trilho_coluna_outro, nomeDoOutro!!, formatarKm(it, locale))
            },
        ),
    )
}

/**
 * Os rótulos: o planejado da semana selecionada, a prova com o dia acima e o pico.
 * Sempre em `texto-primario`: `destaque` sobre `superficie-afundada` mede 4,39:1. O da
 * semana selecionada vai num balão em tinta, com o número em `superficie`. O que encosta
 * num rótulo de prioridade maior não entra.
 */
private fun DrawScope.rotulos(
    colunas: List<ColunaDoTrilho>,
    selecionada: Int,
    g: GeometriaDoTrilho,
    dp: Float,
    topo: (Double) -> Float,
    medidor: TextMeasurer,
    semEscala: Density,
    estilo: androidx.compose.ui.text.TextStyle,
    estiloDoBalao: androidx.compose.ui.text.TextStyle,
    tinta: Color,
    estiloDaData: androidx.compose.ui.text.TextStyle,
    rotuloDaProva: String,
) {
    val ocupados = mutableListOf<Rect>()
    val afastamento = AfastamentoDoRotulo.toPx()

    fun colocar(layout: TextLayoutResult, x: Float, embaixo: Float, ancoraNoFim: Boolean, balao: Boolean = false): Rect? {
        val px = if (balao) RecheioDoBalaoH.toPx() else 0f
        val py = if (balao) RecheioDoBalaoV.toPx() else 0f
        val w = layout.size.width + 2 * px
        val h = layout.size.height + 2 * py
        val esquerda = if (ancoraNoFim) size.width - w else (x - w / 2).coerceIn(0f, size.width - w)
        val caixa = Rect(esquerda, embaixo - h, esquerda + w, embaixo)
        if (ocupados.any { it.overlaps(caixa) }) return null
        ocupados += caixa
        if (balao) drawRoundRect(tinta, caixa.topLeft, caixa.size, CornerRadius(h / 2))
        drawText(layout, topLeft = Offset(caixa.left + px, caixa.top + py))
        return caixa
    }

    colunasRotuladas(colunas, selecionada).forEach { i ->
        val c = colunas[i]
        val planejado = c.planejadoKm ?: return@forEach
        val maior = listOfNotNull(planejado, c.seuKm, c.outroKm).max()
        val prova = c.fase == FaseDoPlano.PROVA
        val balao = c.numero == selecionada
        val texto = medidor.measure(formatarKm(planejado, LocaleDoApp), if (balao) estiloDoBalao else estilo, density = semEscala)
        val caixa = colocar(texto, g.centro(i) * dp, topo(maior) - afastamento, ancoraNoFim = prova, balao = balao)
        if (prova && caixa != null) {
            val data = medidor.measure(rotuloDaProva, estiloDaData, density = semEscala)
            colocar(data, 0f, caixa.top, ancoraNoFim = true)
        }
    }
}

/**
 * A régua: uma marca por semana, na cor da fase. Fechada é cheia, a corrente vem em
 * `destaque` e mais alta, a futura é cheia e clara; a selecionada ganha a moldura em tinta.
 */
@Composable
private fun Regua(colunas: List<ColunaDoTrilho>, selecionada: Int, aoSelecionar: (Int) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    val corDaFase = FaseDoPlano.entries.associateWith { corDaFase(it) }
    val selecionar by rememberUpdatedState(aoSelecionar)
    val n = colunas.size
    val indiceSelecionado = colunas.indexOfFirst { it.numero == selecionada }.coerceAtLeast(0)
    val estado = stringResource(R.string.trilho_regua_estado, indiceSelecionado + 1, n)
    val rotulo = stringResource(R.string.trilho_regua_rotulo)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDaRegua)
            .pointerInput(colunas.map { it.numero }) {
                detectTapGestures { ponto ->
                    val g = geometriaDoTrilho(size.width.toFloat(), n, VAO_DO_TRILHO * density)
                    selecionar(colunas[g.indiceNoPonto(ponto.x)].numero)
                }
            }
            .pointerInput(colunas.map { it.numero }) {
                detectHorizontalDragGestures { mudanca, _ ->
                    val g = geometriaDoTrilho(size.width.toFloat(), n, VAO_DO_TRILHO * density)
                    selecionar(colunas[g.indiceNoPonto(mudanca.position.x)].numero)
                }
            }
            .clearAndSetSemantics {
                contentDescription = rotulo
                stateDescription = estado
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (indiceSelecionado + 1).toFloat(),
                    range = 1f..n.toFloat(),
                    steps = (n - 2).coerceAtLeast(0),
                )
                setProgress { alvo ->
                    val indice = (alvo.roundToInt() - 1).coerceIn(0, n - 1)
                    selecionar(colunas[indice].numero)
                    true
                }
            },
    ) {
        val dp = density
        val g = geometriaDoTrilho(size.width / dp, n)
        val largura = g.larguraDaColuna * dp
        val raio = CornerRadius(Raios.dado.toPx())

        colunas.forEachIndexed { i, c ->
            val x = g.x(i) * dp
            val cor = corDaFase.getValue(c.fase)
            when (c.tempo) {
                TempoDaSemana.PASSADA -> drawRoundRect(
                    cor, Offset(x + 1.dp.toPx(), 20.dp.toPx()), Size(largura - 2.dp.toPx(), 8.dp.toPx()), raio,
                )
                TempoDaSemana.CORRENTE -> drawRoundRect(
                    esquema.primary, Offset(x + 1.dp.toPx(), 19.dp.toPx()), Size(largura - 2.dp.toPx(), 10.dp.toPx()), raio,
                )
                TempoDaSemana.FUTURA -> drawRoundRect(
                    cor.copy(alpha = OPACIDADE_DA_MARCA_FUTURA), Offset(x + 1.dp.toPx(), 20.dp.toPx()), Size(largura - 2.dp.toPx(), 8.dp.toPx()), raio,
                )
            }
        }

        val moldura = 2.dp.toPx()
        val x = g.x(indiceSelecionado) * dp
        drawRoundRect(
            color = esquema.onSurface,
            topLeft = Offset(x - 1.dp.toPx() + moldura / 2, 9.dp.toPx() + moldura / 2),
            size = Size(largura + 2.dp.toPx() - moldura, 30.dp.toPx() - moldura),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = Stroke(moldura),
        )
    }
}
