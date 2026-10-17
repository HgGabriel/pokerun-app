package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.FormaDado
import java.time.LocalDate

/** A altura de um segmento curto. O longão tem o dobro (docs/03 §3.3.1). */
private val AlturaDoSegmento = 12.dp

/** A faixa em que os segmentos são desenhados, alinhados pela base. */
private val AlturaDaBarra = AlturaDoSegmento * 2

/** O piso de alvo de toque, para o segmento endereçável (docs/02 §8, item 2). */
private val AlvoDeToque = 48.dp

private val EspacoEntreSegmentos = 4.dp
private val EspacoAntesDoRotulo = 4.dp

/**
 * Acima desta escala de fonte o layout cede e empilha (docs/02 §8, item 9): a linha com
 * ponto médio desmonta, e os carimbos da semana passam a duas colunas.
 */
internal const val ESCALA_QUE_EMPILHA = 1.3f

private val LarguraDoContorno = 1.dp

/**
 * O rótulo do segmento pendente, que **encurta acima de [ESCALA_QUE_EMPILHA]** em vez de
 * partir no meio da palavra (`F1-T20`, docs/02 §8 item 9).
 *
 * Em 320dp com três segmentos sobram uns 106dp por coluna, e `prevista` em `legenda` a
 * `fontScale` 2,0 pede uns 117dp: o emulador mostrou `previs`/`ta` nas três colunas, sem
 * hífen. O item 9 manda encurtar em vez de truncar, e é o que acontece aqui — **truncar
 * seria pior**, porque `previst` não é palavra nenhuma.
 *
 * **A descrição do TalkBack não passa por aqui.** Ela vem de `semana_segmento_pendente`,
 * no nó pai, e continua dizendo a sessão inteira por extenso: o que encurta é o glifo.
 */
internal fun rotuloDoSegmentoPendente(escala: Float): Int =
    if (escala > ESCALA_QUE_EMPILHA) R.string.semana_prevista_curta else R.string.semana_prevista

/*
 * O card da semana **sem anel** (`F1-T09`, docs/03 §3.3.1 e docs/02 §9.1.1).
 *
 * As três peças são separadas porque três telas as consomem em arranjos diferentes: a
 * `HomeScreen` monta as três, a `WeekDetailScreen` (`F1-T15`) monta as três com a lista
 * de corridas embaixo, e o `PlanDetailScreen` (`F1-T13`) usa **só a fração**, agregada
 * no plano inteiro. As linhas da grade de semanas dele não desenham barra: quem mudar a
 * barra mexe em duas telas, não em três.
 *
 * O anel de progresso circular está proibido: é a assinatura de outro produto, é um
 * display retroiluminado dentro de um app que é ficha impressa, e com denominador 2, 3
 * ou 4 ele lê pior que a fração. Nenhuma destas funções desenha arco nem percentual.
 */

/**
 * A fração de aderência da semana, em `titulo-secao`: `2 de 3 sessões`.
 *
 * **Nunca `67%`** (docs/02 §9.1.1): percentual isolado é decoração, e com denominador
 * pequeno ele pede que o usuário converta de volta para "2 de 3". A razão explícita, com
 * numerador e denominador visíveis, é a regra do sistema.
 */
@Composable
fun FracaoDeSessoes(
    feitas: Int,
    previstas: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.semana_fracao, previstas, feitas, previstas),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/**
 * A barra de sessões: **exatamente um segmento por sessão prevista** (RN-34).
 *
 * Preenchido em `destaque` com a distância embaixo; pendente com contorno em
 * `borda-controle` e a palavra `prevista`. Raio `raio-dado` (2dp, `FormaDado`):
 * barra de dado quase não arredonda (docs/02 §4).
 *
 * **O segmento do longão tem o dobro da altura**, e é aí que RN-10 ganha forma. O
 * indicador textual de cumprido vem logo abaixo, porque altura não se lê no TalkBack.
 *
 * @param aoAbrirCorrida o toque no segmento cumprido, que abre a corrida que o cumpriu
 *   (docs/03 §3.3.1). **Nulo na Fase 1**, e a ausência tem dono: a `RunDetailScreen` é
 *   `F2-T09` e não existe rota para ela em `Rotas.kt`. Com nulo o segmento não fica
 *   tocável, em vez de virar um alvo que não faz nada.
 */
@Composable
fun BarraDeSessoes(
    segmentos: List<SegmentoDaSemana>,
    longaoKm: Double?,
    longaoCumprido: Boolean?,
    modifier: Modifier = Modifier,
    aoAbrirCorrida: ((String) -> Unit)? = null,
) {
    val locale = LocaleDoApp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EspacoEntreSegmentos),
        ) {
            segmentos.forEachIndexed { posicao, segmento ->
                Segmento(
                    segmento = segmento,
                    posicao = posicao + 1,
                    total = segmentos.size,
                    longaoKm = longaoKm,
                    aoAbrirCorrida = aoAbrirCorrida,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (longaoKm != null) {
            Spacer(Modifier.height(EspacoAntesDoRotulo))
            Text(
                // RN-10 em texto, porque altura dupla não chega ao TalkBack.
                text = stringResource(
                    if (longaoCumprido == true) R.string.semana_longao_cumprido else R.string.semana_longao_previsto,
                    formatarKm(longaoKm, locale),
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

/**
 * Um segmento: a barra em cima, alinhada pela base, e a distância embaixo.
 *
 * O alinhamento pela base é o que permite o longão ter o dobro da altura sem empurrar a
 * linha: a faixa reserva a altura do maior, e o curto cresce de baixo para cima.
 */
@Composable
private fun Segmento(
    segmento: SegmentoDaSemana,
    posicao: Int,
    total: Int,
    longaoKm: Double?,
    aoAbrirCorrida: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val locale = LocaleDoApp
    val esquema = MaterialTheme.colorScheme
    val km = segmento.km
    val corrida = segmento.corridaId

    val descricao = when {
        segmento.longao && km != null ->
            stringResource(R.string.semana_segmento_longao_feito, formatarKm(km, locale))
        segmento.longao ->
            stringResource(
                R.string.semana_segmento_longao_pendente,
                formatarKm(longaoKm ?: 0.0, locale),
            )
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

    Column(
        modifier = modifier
            .heightIn(min = AlvoDeToque)
            .then(toque)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDaBarra),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val altura = if (segmento.longao) AlturaDaBarra else AlturaDoSegmento
            val preenchido = Modifier.background(esquema.primary, FormaDado)
            val pendente = Modifier.border(LarguraDoContorno, esquema.outline, FormaDado)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(altura)
                    .then(if (km != null) preenchido else pendente),
            )
        }

        Spacer(Modifier.height(EspacoAntesDoRotulo))

        Text(
            text = if (km != null) {
                stringResource(R.string.semana_km, formatarKm(km, locale))
            } else {
                stringResource(rotuloDoSegmentoPendente(LocalDensity.current.fontScale))
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (km != null) esquema.onSurface else esquema.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // O nó pai já anuncia a sessão inteira: o TalkBack não repete a distância.
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * O intervalo de datas da semana: `24 a 30 de agosto`, ou `28 de julho a 3 de agosto`
 * quando ela vira o mês.
 *
 * **Nasceu privado na `HomeScreen` e subiu para cá em `F1-T15`**, quando a
 * `WeekDetailScreen` passou a escrever a mesma frase no título. Duas cópias divergiriam
 * na virada de mês, que é o caso que ninguém revisa de olho — e as duas telas mostram a
 * **mesma semana**, uma no card e a outra aberta.
 *
 * As datas chegam como `LocalDate` já convertidas no fuso do plano (RN-28). Esta função
 * não converte nada: ela só escolhe entre as duas formas da frase.
 */
@Composable
internal fun periodoDaSemana(primeiro: LocalDate, ultimo: LocalDate): String {
    val locale = LocaleDoApp
    return if (primeiro.month == ultimo.month) {
        stringResource(
            R.string.semana_periodo,
            primeiro.dayOfMonth,
            ultimo.dayOfMonth,
            nomeDoMes(primeiro, locale),
        )
    } else {
        stringResource(
            R.string.semana_periodo_meses,
            primeiro.dayOfMonth,
            nomeDoMes(primeiro, locale),
            ultimo.dayOfMonth,
            nomeDoMes(ultimo, locale),
        )
    }
}
