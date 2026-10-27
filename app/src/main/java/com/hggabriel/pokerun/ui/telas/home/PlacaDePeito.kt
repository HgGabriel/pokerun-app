package com.hggabriel.pokerun.ui.telas.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.molduraDupla
import com.hggabriel.pokerun.ui.componentes.molduraTracejada
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.Raios

/*
 * A placa de peito da Home (docs/02 §4.5): os dias até a prova como o número de peito
 * que o corredor já carrega. É o objeto do plano, como a Pokébola é o objeto do jogo.
 *
 *   ╭──────────────────────────────────╮
 *   │ o  SÃO SILVESTRE 2026 · 31 DEZ  o │  faixa
 *   │                78                │  numero-peito
 *   │        DIAS PARA A LARGADA       │
 *   │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │  corte
 *   │ o SEMANA 10 DE 21  ▁▂▃▄▅▆█▇▅┆┆┆  o │  tarja
 *   ╰──────────────────────────────────╯
 *
 * Sem movimento: a batida do carimbo continua o único momento animado da Home (§6).
 */

/** A faixa de cima. Os furos de alfinete de cima ficam sobre ela. */
private val AlturaDaFaixa = 36.dp

/**
 * O recuo lateral da faixa e da tarja. Deixa os furos de alfinete na margem, sem tocar o
 * texto: centro do furo a 14dp da borda, raio de 4dp, e o texto começa em 28dp.
 */
private val RecuoLateral = 28.dp

private val RaioDoFuro = 4.dp
private val DistanciaDoFuro = 10.dp
private val ContornoDoFuro = 1.5.dp

private val TracoDoCorte = 6.dp
private val IntervaloDoCorte = 4.dp

private val AlturaDoArco = 24.dp

private val AlturaDaPlaca = 220.dp
private val FolgaDaFicha = 4.dp
private val EspacoEntreBarras = 2.dp
private val AlturaDoTraco = 2.dp
private val TracoDoTaper = 3.dp
private val IntervaloDoTaper = 2.dp

/** A altura das maiúsculas e dos algarismos da Poppins, em fração do corpo. */
private const val PROPORCAO_DA_MAIUSCULA = 0.70f

/** Acima disto o número cai para `numero-xl` (docs/02 §3). */
private const val ESCALA_QUE_REBAIXA = 1.5f

/** Acima disto a tarja empilha a semana sobre o arco (docs/02 §8, item 9). */
private const val ESCALA_QUE_EMPILHA = 1.3f

/**
 * O corpo da placa: a `Ficha` de dado com a faixa, o [centro], o corte e a [tarja].
 *
 * [viva] pinta a faixa em `destaque`, para o plano aberto; sem ela a faixa fica em
 * `texto-secundario`, para quem não tem prova ou já correu a dele. O vermelho é
 * progresso do usuário (docs/02 §1, princípio 3), e plano encerrado não é mais progresso.
 *
 * A placa inteira é um nó para o leitor de tela, com [descricao]: os furos, o arco e o
 * corte são desenho.
 *
 * [sobreODestaque] é a placa presa na camisa vermelha do topo da Home: os furos mostram o
 * vermelho de trás, e a faixa viva vai para `texto-primario`, senão ela se funde ao fundo.
 */
@Composable
internal fun PlacaDePeito(
    faixa: String,
    viva: Boolean,
    descricao: String,
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    sobreODestaque: Boolean = false,
    tarja: (@Composable () -> Unit)? = null,
    centro: @Composable ColumnScope.() -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val corDaFaixa = when {
        !viva -> esquema.onSurfaceVariant
        sobreODestaque -> esquema.onSurface
        else -> esquema.primary
    }
    val fundo = if (sobreODestaque) esquema.primary else esquema.background
    val contorno = esquema.outline

    Ficha(modifier = modifier, aoTocar = aoTocar) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = descricao }
                // Os furos vão por cima de tudo, na margem que a faixa e a tarja deixam.
                .drawWithFuros(fundo, contorno, embaixo = tarja != null),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDaFaixa)
                        .background(corDaFaixa)
                        .padding(horizontal = RecuoLateral, vertical = Espacos.relacionado),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = faixa,
                        style = MaterialTheme.typography.labelMedium,
                        color = esquema.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Espacos.bloco, bottom = Espacos.margem),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = centro,
                )

                if (tarja != null) {
                    Corte()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RecuoLateral, vertical = Espacos.interno),
                    ) {
                        tarja()
                    }
                }
            }
        }
    }
}

/** O número e o rótulo do centro. Acima de `fontScale` 1,5 o número cai um degrau. */
@Composable
internal fun NumeroDaPlaca(numero: String, rotulo: String) {
    val estilo = if (LocalDensity.current.fontScale > ESCALA_QUE_REBAIXA) Estilos.numeroXl else Estilos.numeroPeito
    Text(
        text = numero,
        style = estilo,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.recortadoNaAlturaDasMaiusculas(estilo.fontSize),
    )
    Spacer(Modifier.height(Espacos.interno))
    RotuloDaPlaca(rotulo)
}

/** `SemPlano`: no lugar do número, a vaga tracejada dos carimbos, ainda sem prova. */
@Composable
internal fun VagaDoNumero(rotulo: String) {
    val contorno = MaterialTheme.colorScheme.outline
    Spacer(
        modifier = Modifier
            .size(width = 136.dp, height = 72.dp)
            .drawBehind { molduraTracejada(contorno) },
    )
    Spacer(Modifier.height(Espacos.relacionado))
    RotuloDaPlaca(rotulo)
}

/**
 * `PlanoEncerrado`: o carimbo `CONCLUÍDO`, na moldura dupla dos carimbos da semana e com
 * a mesma inclinação do primeiro deles. É o fim do diário de campo.
 */
@Composable
internal fun CarimboConcluido(texto: String) {
    val tinta = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(vertical = Espacos.margem)
            .graphicsLayer { rotationZ = -3f }
            .drawBehind { molduraDupla(tinta) }
            .padding(horizontal = Espacos.bloco, vertical = Espacos.interno),
    ) {
        Text(text = texto, style = Estilos.numeroM, color = tinta)
    }
}

@Composable
private fun RotuloDaPlaca(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = RecuoLateral),
    )
}

/**
 * A tarja: [rotulo] (a semana, ou o dia em que o plano começa) e o arco do longão. Acima
 * de `fontScale` 1,3 os dois empilham, porque a linha não cabe sem quebrar o rótulo.
 */
@Composable
internal fun TarjaDaPlaca(rotulo: String, arco: List<BarraDoArco>) {
    val texto: @Composable () -> Unit = {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
            texto()
            ArcoDoLongao(arco, Modifier.fillMaxWidth().height(AlturaDoArco))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            texto()
            Spacer(Modifier.width(Espacos.interno))
            ArcoDoLongao(arco, Modifier.weight(1f).height(AlturaDoArco))
        }
    }
}

/**
 * Uma barra por semana, na altura do longão sobre o maior longão do plano.
 *
 * Passada em `texto-secundario`, a atual em `destaque`, futura em `borda`. O taper futuro
 * é contorno tracejado: o volume cai de propósito, e uma barra cheia mais baixa leria
 * como semana fraca. Semana sem longão é um traço de 2dp, e não um buraco.
 */
@Composable
internal fun ArcoDoLongao(arco: List<BarraDoArco>, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    val passada = esquema.onSurfaceVariant
    val atual = esquema.primary
    val futura = esquema.outlineVariant
    val taper = esquema.outline

    Canvas(modifier = modifier) {
        val n = arco.size
        val maior = arco.mapNotNull { it.longaoKm }.maxOrNull() ?: return@Canvas
        if (n == 0 || maior <= 0.0) return@Canvas

        val intervalo = EspacoEntreBarras.toPx()
        val largura = (size.width - intervalo * (n - 1)) / n
        val raio = minOf(Raios.dado.toPx(), largura / 2)
        val traco = AlturaDoTraco.toPx()

        arco.forEachIndexed { i, barra ->
            val altura = barra.longaoKm?.let { (size.height * (it / maior)).toFloat().coerceAtLeast(traco) } ?: traco
            val x = i * (largura + intervalo)
            val topo = size.height - altura
            if (barra.situacao == SituacaoDaBarra.FUTURA && barra.taper && barra.longaoKm != null) {
                barraTracejada(taper, Offset(x, topo), Size(largura, altura), raio)
            } else {
                val cor = when (barra.situacao) {
                    SituacaoDaBarra.PASSADA -> passada
                    SituacaoDaBarra.ATUAL -> atual
                    SituacaoDaBarra.FUTURA -> if (barra.taper) taper else futura
                }
                drawRoundRect(cor, Offset(x, topo), Size(largura, altura), CornerRadius(raio))
            }
        }
    }
}

/**
 * A placa enquanto o plano carrega, em bloco estático (docs/02 §8, item 6). A altura é a
 * da placa a `fontScale` 1: faixa 36, ar 24, algarismo 67, 12, rótulo 16, 16, corte 1 e
 * tarja 48. A folga é a de foco da `Ficha`, para o esqueleto ocupar o mesmo lugar.
 */
@Composable
internal fun EsqueletoDaPlaca(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .padding(FolgaDaFicha)
            .height(AlturaDaPlaca)
            .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    )
}

/**
 * A caixa do número do tamanho do algarismo, da linha de base à altura das maiúsculas.
 *
 * A Poppins reserva ar de acento e de descendente na caixa da linha, e algarismo não tem
 * nenhum dos dois: em 96sp a sobra eram ~40dp acima e abaixo, e o número, o rótulo e a
 * tarja deixavam de ler como uma figura só. `LineHeightStyle.Trim` não resolve, porque a
 * entrelinha já é igual ao corpo e não há excesso de entrelinha a cortar.
 */
private fun Modifier.recortadoNaAlturaDasMaiusculas(corpo: TextUnit): Modifier = layout { medida, restricoes ->
    val texto = medida.measure(restricoes)
    val base = texto[FirstBaseline]
    if (base == AlignmentLine.Unspecified) return@layout layout(texto.width, texto.height) { texto.place(0, 0) }
    val maiuscula = (corpo.toPx() * PROPORCAO_DA_MAIUSCULA).roundToInt()
    layout(texto.width, maiuscula) { texto.place(0, maiuscula - base) }
}

/** O picote entre o número e a tarja, de borda a borda. */
@Composable
private fun Corte() {
    val cor = MaterialTheme.colorScheme.outline
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = cor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(TracoDoCorte.toPx(), IntervaloDoCorte.toPx())),
        )
    }
}

private fun DrawScope.barraTracejada(cor: Color, topo: Offset, tamanho: Size, raio: Float) {
    val traco = 1.dp.toPx()
    drawRoundRect(
        color = cor,
        topLeft = Offset(topo.x + traco / 2, topo.y + traco / 2),
        size = Size((tamanho.width - traco).coerceAtLeast(0f), (tamanho.height - traco).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(raio),
        style = Stroke(
            width = traco,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(TracoDoTaper.toPx(), IntervaloDoTaper.toPx())),
        ),
    )
}

/**
 * Os quatro furos de alfinete, a 10dp dos cantos. Os de cima caem sobre a faixa. Sem
 * tarja (a placa de quem não tem prova), os de baixo ficam fora: o número de peito sem
 * picote é só a metade de cima.
 */
private fun Modifier.drawWithFuros(fundo: Color, contorno: Color, embaixo: Boolean): Modifier =
    drawWithContent {
        drawContent()
        val r = RaioDoFuro.toPx()
        val d = DistanciaDoFuro.toPx() + r
        val linha = ContornoDoFuro.toPx()
        val largura = size.width
        val altura = size.height
        val centros = buildList {
            add(Offset(d, d))
            add(Offset(largura - d, d))
            if (embaixo) {
                add(Offset(d, altura - d))
                add(Offset(largura - d, altura - d))
            }
        }
        centros.forEach { c ->
            drawCircle(fundo, r, c)
            drawCircle(contorno, r - linha / 2, c, style = Stroke(linha))
        }
    }
