package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.componentes.AtalhoDoPerfil
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.fonteAmpliada
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.marcaDePokebola
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import kotlin.math.roundToInt

/**
 * O que a capa do álbum mostra (docs/03 §3.18).
 *
 * @property proximo o número da Pokédex do próximo, ou nulo na última espécie.
 * @property kmAteOProximo o `~6 km`, ou nulo sem corrida na temporada.
 * @property recuo o corredor recuou (RN-37): a capa diz `Para retomar` e a regra.
 * @property fita o trecho da escada em volta da posição, na ordem.
 * @property indice cada tier com a fração colada, na ordem.
 */
@Immutable
internal data class CapaDoAlbum(
    val proximo: Int?,
    val kmAteOProximo: Double?,
    val recuo: Recuo?,
    val fita: List<FigurinhaDoAlbum>,
    val indice: List<Pair<Int, Float>>,
    val tierAtual: Int,
    val colecao: Int,
    val total: Int,
)

/** A linguagem de recuo (docs/04 §5.3): a coleção que já foi e a forma de hoje. */
@Immutable
internal data class Recuo(val colecao: Int, val formaAtual: Int)

/** Os tiers que a meta da semana alcança ficam marcados na fita por uma barra embaixo. */
private val AlturaDaMeta = 3.dp
private val VaoDaFita = 10.dp

/** Onde a figurinha grande abre na fita: 80dp da borda, com uma colada à esquerda. */
private val InicioDaGrande = 80.dp

/**
 * A capa em tinta, a peça em tinta da Pokédex: o próximo e o km até ele, a fita do tier
 * com as últimas coladas, você em figurinha grande e as vagas seguintes, o índice dos tiers
 * e a contagem da coleção. O pacote mora no canto, ao lado do atalho do perfil.
 *
 * @param pacote o envelope das figurinhas novas, desenhado por quem tem o movimento dele.
 */
@Composable
internal fun CapaDoAlbum(
    capa: CapaDoAlbum,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoAbrirPerfil: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
    aoEscolherTier: (Int) -> Unit,
    modifier: Modifier = Modifier,
    batidas: Map<Int, Int> = emptyMap(),
    pacote: @Composable () -> Unit = {},
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(tinta),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val raio = 104.dp.toPx()
            marcaDePokebola(claro.copy(alpha = 0.07f), Offset(size.width - 36.dp.toPx(), size.height - 24.dp.toPx()), raio)
        }

        Column(modifier = Modifier.padding(bottom = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, end = Espacos.minimo, top = Espacos.relacionado),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.destino_pokedex).uppercase(LocaleDoApp),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                pacote()
                Spacer(Modifier.width(Espacos.relacionado))
                AtalhoDoPerfil(aoTocar = aoAbrirPerfil)
            }

            Proximo(capa, nomes, Modifier.padding(horizontal = Espacos.margem))

            if (capa.fita.isNotEmpty()) {
                FitaDoTier(capa.fita, nomes, tipos, capa.kmAteOProximo, batidas, aoAbrirEspecie)
            }

            IndiceDosTiers(capa, aoEscolherTier, Modifier.padding(horizontal = Espacos.margem))

            if (capa.recuo != null) {
                Text(
                    text = stringResource(R.string.pokedex_ja_foi, capa.recuo.colecao) + " " +
                        stringResource(R.string.pokedex_forma_atual, capa.recuo.formaAtual),
                    style = Estilos.legenda,
                    color = claro.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = Espacos.margem),
                )
            }
        }
    }
}

/** `PRÓXIMO` e `#071 Victreebel` à esquerda, `~6 km` em `numero-xl` à direita. */
@Composable
private fun Proximo(capa: CapaDoAlbum, nomes: Map<Int, String>, modifier: Modifier) {
    val claro = MaterialTheme.colorScheme.surface
    val proximo = capa.proximo ?: return
    val nome = nomes[proximo]
    val numero = stringResource(R.string.especie_numero, proximo)
    val km = capa.kmAteOProximo?.let { formatarKm(it) }
    val rotulo = stringResource(if (capa.recuo != null) R.string.pokedex_retomar else R.string.pokedex_proximo)
    val descricao = listOfNotNull(
        rotulo,
        nome ?: numero,
        km?.let { stringResource(R.string.pokedex_cerca_de_km, it) },
        stringResource(R.string.pokedex_de, capa.colecao, capa.total),
    ).joinToString(", ")
    val empilhar = fonteAmpliada()

    @Composable
    fun Alvo() {
        Column {
            Text(rotulo.uppercase(LocaleDoApp), style = Estilos.rotulo, color = claro.copy(alpha = 0.72f))
            Text(
                listOfNotNull(numero, nome).joinToString(" "),
                style = Estilos.corpoForte,
                color = claro.copy(alpha = 0.9f),
            )
        }
    }

    @Composable
    fun Km() {
        if (km == null) return
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.pokedex_aproximado, km), style = Estilos.numeroXl, color = claro)
            Spacer(Modifier.width(Espacos.minimo))
            Text(stringResource(R.string.painel_unidade_km), style = Estilos.numeroM, color = claro, modifier = Modifier.padding(bottom = 6.dp))
        }
    }

    val semantica = Modifier.clearAndSetSemantics { contentDescription = descricao }
    if (empilhar) {
        Column(modifier = modifier.then(semantica)) {
            Alvo()
            Km()
        }
    } else {
        Row(modifier = modifier.fillMaxWidth().then(semantica), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) { Alvo() }
            Km()
        }
    }
}

/**
 * A fita do tier: uma linha arrastável em volta da posição. Abre com a figurinha grande a
 * 80dp da borda, uma colada à esquerda e o próximo à direita. A barra branca embaixo marca
 * as vagas que a meta da semana alcança.
 */
@Composable
private fun FitaDoTier(
    fita: List<FigurinhaDoAlbum>,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    km: Double?,
    batidas: Map<Int, Int>,
    aoAbrirEspecie: (Int) -> Unit,
) {
    val claro = MaterialTheme.colorScheme.surface
    val densidade = LocalDensity.current
    val atual = fita.indexOfFirst { it.atual }.coerceAtLeast(0)
    // A colada antes da grande começa fora da borda, para a grande cair em `InicioDaGrande`.
    val estado = rememberLazyListState(
        initialFirstVisibleItemIndex = (atual - 1).coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = if (atual == 0) 0 else with(densidade) {
            (LarguraNaFita + VaoDaFita - (InicioDaGrande - Espacos.margem)).roundToPx().coerceAtLeast(0)
        },
    )
    val falta = km?.let { stringResource(R.string.pokedex_falta_km, formatarKm(it)) }
    val vaoPx = with(densidade) { VaoDaFita.toPx() }

    LazyRow(
        state = estado,
        contentPadding = PaddingValues(start = Espacos.margem, end = Espacos.margem, top = 22.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(VaoDaFita),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().padding(top = Espacos.minimo),
    ) {
        itemsIndexed(fita, key = { _, f -> f.numero }) { i, f ->
            Column {
                when {
                    f.atual -> FigurinhaGrande(
                        numero = f.numero,
                        nome = nomes[f.numero],
                        tipos = tipos[f.numero].orEmpty(),
                        colada = f.colada,
                        nova = f.nova,
                        batida = batidas[f.posicao],
                        aoTocar = { aoAbrirEspecie(f.numero) },
                    )
                    f.colada -> FigurinhaColada(
                        numero = f.numero,
                        nome = nomes[f.numero],
                        tipos = tipos[f.numero].orEmpty(),
                        medida = NaFita,
                        nova = f.nova,
                        batida = batidas[f.posicao],
                        lugar = "fita",
                        aoTocar = { aoAbrirEspecie(f.numero) },
                        modifier = Modifier.width(LarguraNaFita),
                    )
                    else -> FigurinhaVaga(
                        numero = f.numero,
                        nome = nomes[f.numero],
                        medida = NaFita,
                        sobreTinta = true,
                        proximo = f.proximo,
                        falta = if (f.proximo) falta else null,
                        lugar = "fita",
                        aoTocar = { aoAbrirEspecie(f.numero) },
                        modifier = Modifier.width(LarguraNaFita),
                    )
                }
                // A barra da meta passa por baixo do vão entre as vagas, para ler como uma só.
                val continua = fita.getOrNull(i + 1)?.naMeta == true
                Spacer(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidthDaFigurinha(f.atual)
                        .height(AlturaDaMeta)
                        .drawBehind {
                            if (!f.naMeta) return@drawBehind
                            val alem = if (continua) vaoPx else 0f
                            drawRoundRect(
                                claro.copy(alpha = 0.6f),
                                size = Size(size.width + alem, size.height),
                                cornerRadius = CornerRadius(size.height / 2),
                            )
                        },
                )
            }
        }
    }
}

private fun Modifier.fillMaxWidthDaFigurinha(grande: Boolean): Modifier = width(if (grande) LarguraDaGrande else LarguraNaFita)

/**
 * O índice: os tiers em pílulas cheias de 8dp, com a fração colada em `destaque` e o tier
 * atual com contorno branco, e a contagem `82 de 150`. É um controle só, com semântica de
 * slider: tocar num tier abre a página dele e rola até ela.
 */
@Composable
private fun IndiceDosTiers(capa: CapaDoAlbum, aoEscolherTier: (Int) -> Unit, modifier: Modifier) {
    val claro = MaterialTheme.colorScheme.surface
    val destaque = MaterialTheme.colorScheme.primary
    val ordens = capa.indice.map { it.first }
    val primeiro = ordens.firstOrNull() ?: 1
    val ultimo = ordens.lastOrNull() ?: 1
    val rotulo = stringResource(R.string.pokedex_indice)
    val estadoFalado = stringResource(R.string.pokedex_indice_estado, capa.tierAtual, capa.colecao, capa.total)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Espacos.alvoToque)
            .semantics(mergeDescendants = true) {
                contentDescription = rotulo
                stateDescription = estadoFalado
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = capa.tierAtual.toFloat(),
                    range = primeiro.toFloat()..ultimo.toFloat(),
                    steps = (ultimo - primeiro - 1).coerceAtLeast(0),
                )
                setProgress { valor ->
                    aoEscolherTier(valor.roundToInt().coerceIn(primeiro, ultimo))
                    true
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(Espacos.alvoToque)
                .pointerInput(ordens) {
                    detectTapGestures { toque ->
                        if (ordens.isEmpty()) return@detectTapGestures
                        val i = (toque.x / size.width * ordens.size).toInt().coerceIn(0, ordens.lastIndex)
                        aoEscolherTier(ordens[i])
                    }
                },
        ) {
            val n = capa.indice.size.coerceAtLeast(1)
            val vao = 4.dp.toPx()
            val altura = 8.dp.toPx()
            val largura = (size.width - vao * (n - 1)) / n
            val topo = (size.height - altura) / 2
            val raio = CornerRadius(altura / 2)
            capa.indice.forEachIndexed { i, (ordem, fracao) ->
                val x = i * (largura + vao)
                drawRoundRect(claro.copy(alpha = 0.14f), Offset(x, topo), Size(largura, altura), raio)
                if (fracao > 0f) {
                    drawRoundRect(destaque, Offset(x, topo), Size((largura * fracao).coerceAtLeast(altura), altura), raio)
                }
                if (ordem == capa.tierAtual) {
                    val folga = 2.dp.toPx() + 1.dp.toPx()
                    drawRoundRect(
                        claro,
                        Offset(x - folga, topo - folga),
                        Size(largura + folga * 2, altura + folga * 2),
                        CornerRadius(altura / 2 + folga),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.pokedex_de, capa.colecao, capa.total),
            style = Estilos.corpoForte,
            color = claro,
        )
    }
}

/**
 * A capa sem dado: o topo em tinta com o atalho do perfil e, enquanto carrega, o esqueleto
 * estático da fita (blocos claros, sem número). Sem temporada ou com erro, só o topo.
 */
@Composable
internal fun CapaEmEspera(aoAbrirPerfil: () -> Unit, esqueleto: Boolean, modifier: Modifier = Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(tinta),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            marcaDePokebola(claro.copy(alpha = 0.07f), Offset(size.width - 36.dp.toPx(), size.height - 24.dp.toPx()), 104.dp.toPx())
        }
        Column(modifier = Modifier.padding(bottom = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, end = Espacos.minimo, top = Espacos.relacionado),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.destino_pokedex).uppercase(LocaleDoApp),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                AtalhoDoPerfil(aoTocar = aoAbrirPerfil)
            }
            if (esqueleto) {
                val bloco = claro.copy(alpha = 0.07f)
                Spacer(Modifier.height(64.dp))
                Row(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .padding(start = Espacos.margem, top = 22.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(VaoDaFita),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(2) { Box(Modifier.width(LarguraNaFita).height(100.dp).background(bloco, RoundedCornerShape(12.dp))) }
                    Box(Modifier.width(LarguraDaGrande).height(142.dp).background(bloco, RoundedCornerShape(12.dp)))
                    repeat(2) { Box(Modifier.width(LarguraNaFita).height(100.dp).background(bloco, RoundedCornerShape(12.dp))) }
                }
                Box(
                    Modifier
                        .padding(horizontal = Espacos.margem, vertical = 20.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(bloco, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}
