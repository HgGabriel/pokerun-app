package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.Raios

/** Altura mínima de um carimbo: dia, número e unidade, com o ar da moldura dupla. */
private val AlturaDoCarimbo = 80.dp

private val EspacoEntreCarimbos = Espacos.relacionado

/** A moldura de fora, e a de dentro, recuada [RecuoDaMolduraInterna]. */
private val MolduraExterna = 2.dp
private val MolduraInterna = 1.dp
private val RecuoDaMolduraInterna = 4.dp

/** O tracejado da vaga: traço e intervalo. */
private val TracoDaVaga = 6.dp
private val IntervaloDaVaga = 4.dp
private val ContornoDaVaga = 1.5.dp

/** O longão ocupa mais largura que uma curta: é a sessão que pesa mais (RN-10). */
private const val PESO_DO_LONGAO = 1.4f

/**
 * A inclinação de cada carimbo, em graus, pela posição. Fixa, e não sorteada: o mesmo
 * carimbo não pode mudar de ângulo a cada vez que a Home recompõe.
 */
private val Inclinacoes = listOf(-3f, 2.5f, -1.5f, 3f)

/** Quanto o carimbo recém-batido começa maior, antes de assentar. */
private const val ESCALA_DA_BATIDA = 0.4f

/**
 * Os carimbos da semana (docs/03 §3.3.1, delight de 17/10/2026): **uma vaga por sessão
 * prevista** (RN-34), e cada corrida que cumpre uma sessão é um carimbo na vaga dela.
 *
 * ```
 *  ╔═════════╗  ╔═════════╗   ┌ ─ ─ ─ ─ ─ ─ ┐
 *  ║   TER   ║  ║   QUI   ║      LONGÃO
 *  ║   6,2   ║  ║   5,8   ║   │    8,5      │
 *  ║   km    ║  ║   km    ║        km
 *  ╚═════════╝  ╚═════════╝   └ ─ ─ ─ ─ ─ ─ ┘
 * ```
 *
 * Substituiu a barra de sessões e a grade de sete dias, que diziam *quantas* e *quando*
 * em duas figuras. O carimbo diz as duas coisas: a vaga carimbada é a sessão cumprida, e o
 * carimbo traz o dia da corrida. A semana lê como diário de campo, e não como dashboard.
 *
 * - **Carimbo:** moldura dupla em `destaque`, o dia em `rotulo`, a distância em
 *   `numero-s`, levemente inclinado. `destaque` porque é progresso do usuário (docs/02
 *   §1, princípio 3), e não a cor do tipo da espécie: o tipo só aparece onde é informação,
 *   e várias bases de tipo não passam 4,5:1 como texto sobre branco.
 * - **Vaga:** contorno tracejado em `borda-controle`, reto. A curta diz `prevista`; a do
 *   longão diz `LONGÃO` e o alvo, em `texto-secundario`.
 * - **O longão é a vaga larga** ([PESO_DO_LONGAO]). Na barra ele tinha o dobro da altura.
 *
 * Toque no carimbo abre a corrida que o bateu, como o segmento fazia.
 *
 * @param corridaRecente a corrida que acabou de ser registrada. O carimbo dela é batido:
 *   entra maior e mais inclinado e assenta na vaga, em `duracao-media` com `curva-captura`
 *   (docs/02 §6). Com movimento reduzido, ele já aparece assentado.
 */
@Composable
fun CarimbosDaSemana(
    segmentos: List<SegmentoDaSemana>,
    longaoKm: Double?,
    longaoCumprido: Boolean?,
    modifier: Modifier = Modifier,
    aoAbrirCorrida: ((String) -> Unit)? = null,
    corridaRecente: String? = null,
) {
    val empilhar = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA
    // Acima de 1,3 o número não cabe numa quarta parte de 320dp: os carimbos passam a duas
    // colunas (docs/02 §8, item 9), e a ordem da semana segue da esquerda para a direita.
    val linhas = if (empilhar) segmentos.withIndex().chunked(2) else listOf(segmentos.withIndex().toList())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EspacoEntreCarimbos)) {
        linhas.forEach { linha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EspacoEntreCarimbos),
            ) {
                linha.forEach { (indice, segmento) ->
                    Vaga(
                        segmento = segmento,
                        posicao = indice + 1,
                        total = segmentos.size,
                        longaoKm = longaoKm,
                        recente = segmento.corridaId != null && segmento.corridaId == corridaRecente,
                        aoAbrirCorrida = aoAbrirCorrida,
                    )
                }
            }
        }

        // RN-10 em texto, só quando a vaga do longão já tem carimbo: pendente, o alvo está
        // escrito na própria vaga. Carimbado, a linha diz se a distância chegou lá.
        val longaoCarimbado = segmentos.any { it.longao && it.km != null }
        if (longaoKm != null && longaoCarimbado) {
            Text(
                text = stringResource(
                    if (longaoCumprido == true) R.string.semana_longao_cumprido else R.string.semana_longao_previsto,
                    formatarKm(longaoKm, LocaleDoApp),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (longaoCumprido == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** Uma vaga da semana: tracejada à espera, ou com o carimbo da corrida que a cumpriu. */
@Composable
private fun RowScope.Vaga(
    segmento: SegmentoDaSemana,
    posicao: Int,
    total: Int,
    longaoKm: Double?,
    recente: Boolean,
    aoAbrirCorrida: ((String) -> Unit)?,
) {
    val locale = LocaleDoApp
    val km = segmento.km
    val corrida = segmento.corridaId

    val descricao = when {
        segmento.longao && km != null ->
            stringResource(R.string.semana_segmento_longao_feito, formatarKm(km, locale))
        segmento.longao ->
            stringResource(R.string.semana_segmento_longao_pendente, formatarKm(longaoKm ?: 0.0, locale))
        km != null ->
            stringResource(R.string.semana_segmento_feito, posicao, total, formatarKm(km, locale))
        else -> stringResource(R.string.semana_segmento_pendente, posicao, total)
    }

    val toque = if (aoAbrirCorrida != null && corrida != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = { aoAbrirCorrida(corrida) },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .weight(if (segmento.longao) PESO_DO_LONGAO else 1f)
            .heightIn(min = AlturaDoCarimbo)
            .then(toque)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
    ) {
        if (km == null) {
            VagaTracejada(segmento, longaoKm)
        } else {
            Carimbo(segmento, km, Inclinacoes[(posicao - 1) % Inclinacoes.size], recente)
        }
    }
}

@Composable
private fun VagaTracejada(segmento: SegmentoDaSemana, longaoKm: Double?, modifier: Modifier = Modifier) {
    val contorno = MaterialTheme.colorScheme.outline
    val apagado = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoCarimbo)
            .drawBehind { molduraTracejada(contorno) }
            .padding(Espacos.relacionado),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (segmento.longao && longaoKm != null) {
            LinhaDoCarimbo(stringResource(R.string.semana_carimbo_longao).uppercase(LocaleDoApp), Estilos.rotulo, apagado)
            LinhaDoCarimbo(formatarKm(longaoKm, LocaleDoApp), Estilos.numeroS, apagado)
            LinhaDoCarimbo(stringResource(R.string.semana_carimbo_km), Estilos.legenda, apagado)
        } else {
            LinhaDoCarimbo(stringResource(R.string.semana_prevista), Estilos.legenda, apagado)
        }
    }
}

/**
 * O carimbo: moldura dupla em `destaque`, inclinado [inclinacao] graus.
 *
 * Recém-batido, ele entra [ESCALA_DA_BATIDA] maior e mais inclinado e assenta com a
 * ultrapassagem da `curva-captura`, que é o tranco do carimbo no papel. A vaga tracejada
 * fica embaixo e some enquanto ele desce.
 */
@Composable
private fun Carimbo(segmento: SegmentoDaSemana, km: Double, inclinacao: Float, recente: Boolean) {
    val tinta = MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current

    // A chave é a corrida, e não o aviso: consumir o aviso no meio não corta a batida.
    val batida = remember(segmento.corridaId) { Animatable(if (recente) 0f else 1f) }
    LaunchedEffect(segmento.corridaId) {
        if (batida.value < 1f) {
            batida.animateTo(
                1f,
                Movimento.especificacao(Movimento.Duracao.MEDIA, Movimento.Curva.captura, reduzido),
            )
        }
    }

    Box {
        if (batida.value < 1f) {
            VagaTracejada(
                segmento = segmento,
                longaoKm = null,
                modifier = Modifier.graphicsLayer { alpha = 1f - batida.value.coerceIn(0f, 1f) },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDoCarimbo)
                .graphicsLayer {
                    val p = batida.value
                    val escala = 1f + ESCALA_DA_BATIDA * (1f - p)
                    scaleX = escala
                    scaleY = escala
                    alpha = p.coerceIn(0f, 1f)
                    rotationZ = inclinacao * (1f + 1.5f * (1f - p))
                }
                .drawBehind { molduraDupla(tinta) }
                .padding(Espacos.relacionado),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val dia = segmento.dia?.let { rotuloCurtoDoDia(it.dayOfWeek) }
            val topo = when {
                segmento.longao && dia != null -> stringResource(R.string.semana_carimbo_longao_dia, dia)
                segmento.longao -> stringResource(R.string.semana_carimbo_longao)
                else -> dia
            }
            if (topo != null) LinhaDoCarimbo(topo.uppercase(LocaleDoApp), Estilos.rotulo, tinta)
            LinhaDoCarimbo(formatarKm(km, LocaleDoApp), Estilos.numeroS, tinta)
            LinhaDoCarimbo(stringResource(R.string.semana_carimbo_km), Estilos.legenda, tinta)
        }
    }
}

@Composable
private fun LinhaDoCarimbo(texto: String, estilo: androidx.compose.ui.text.TextStyle, cor: Color) {
    Text(
        text = texto,
        style = estilo,
        color = cor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

/** Duas molduras, a de fora mais grossa: é o que faz o retângulo ler como carimbo. */
private fun DrawScope.molduraDupla(tinta: Color) {
    val raio = Raios.chip.toPx()
    contornoArredondado(tinta, MolduraExterna, 0.dp, raio)
    contornoArredondado(tinta, MolduraInterna, RecuoDaMolduraInterna, (raio - RecuoDaMolduraInterna.toPx()).coerceAtLeast(0f))
}

private fun DrawScope.molduraTracejada(cor: Color) {
    val traco = ContornoDaVaga.toPx()
    drawRoundRect(
        color = cor,
        topLeft = Offset(traco / 2, traco / 2),
        size = Size(size.width - traco, size.height - traco),
        cornerRadius = CornerRadius(Raios.chip.toPx()),
        style = Stroke(
            width = traco,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(TracoDaVaga.toPx(), IntervaloDaVaga.toPx())),
        ),
    )
}

private fun DrawScope.contornoArredondado(cor: Color, largura: Dp, recuo: Dp, raio: Float) {
    val traco = largura.toPx()
    val inicio = recuo.toPx() + traco / 2
    drawRoundRect(
        color = cor,
        topLeft = Offset(inicio, inicio),
        size = Size(size.width - inicio * 2, size.height - inicio * 2),
        cornerRadius = CornerRadius(raio),
        style = Stroke(traco),
    )
}
