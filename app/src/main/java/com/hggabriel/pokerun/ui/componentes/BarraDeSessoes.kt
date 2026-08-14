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
import com.hggabriel.pokerun.ui.theme.EstiloDado
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
 * Acima desta escala de fonte o layout cede e a grade de dias empilha (docs/02 §8, item
 * 9). É o mesmo limiar que a `HomeScreen` usa para desmontar a linha com ponto médio.
 */
internal const val ESCALA_QUE_EMPILHA = 1.3f

private val LadoDoQuadrado = 12.dp
private val LadoDaMarcaVazia = 4.dp
private val LarguraDoContorno = 1.dp

/*
 * O card da semana **sem anel** (`F1-T09`, docs/03 §3.3.1 e docs/02 §9.1.1).
 *
 * As três peças são separadas porque três telas as consomem em arranjos diferentes: a
 * `HomeScreen` monta as três, a `WeekDetailScreen` (`F1-T15`) monta as três com a lista
 * de corridas embaixo, e o `PlanDetailScreen` (`F1-T13`) usa a fração e a barra dentro
 * de cada linha da grade de semanas.
 *
 * O anel de progresso circular está proibido: é a assinatura de outro produto, é um
 * display retroiluminado dentro de um app que é ficha impressa, e com denominador 2, 3
 * ou 4 ele lê pior que a fração. Nenhuma destas funções desenha arco nem percentual.
 */

/**
 * A fração de aderência da semana, em mono: `2 de 3 sessões`.
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
 * Preenchido em `leitura` com a distância embaixo; pendente com contorno em
 * `borda-forte` e a palavra `prevista`. Raio 0dp — barra de dado não arredonda
 * (docs/02 §4).
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
                style = EstiloDado,
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
                stringResource(R.string.semana_prevista)
            },
            style = EstiloDado,
            color = if (km != null) esquema.onSurface else esquema.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // O nó pai já anuncia a sessão inteira: o TalkBack não repete a distância.
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * A grade de dias: **a barra diz *quantas*, a grade diz *quando*** (docs/03 §3.3.1).
 *
 * Antes do card sem anel as duas coisas eram ditas duas vezes — anel e grade. Agora cada
 * uma responde a sua pergunta, e é por isso que a grade não tem teto: duas corridas no
 * sábado são dois treinos no sábado, mesmo que só uma reivindique slot (RN-34).
 *
 * **A quantidade de quadrados vem da semana**, e não de um `7` escrito aqui: a semana da
 * prova tem quatro dias (RN-26).
 *
 * Três marcas: quadrado cheio em `leitura` no dia com corrida, quadrado vazado em
 * `borda-forte` no dia de hoje sem corrida, e um ponto em `tinta-fraca` nos demais. Hoje
 * também acende o rótulo em `leitura`, para o dia corrente continuar visível quando já
 * tem corrida.
 *
 * **Acima de `fontScale` 1,3 a grade empilha**, e é o item 9 do piso de qualidade sendo
 * obedecido em vez de citado. Em sete colunas dentro de 320dp cabem uns 33dp por dia, e
 * o emulador mostrou `SEG` virando `SE` a 2,0: rótulo em caixa alta **encurta ou cede o
 * layout, nunca trunca**. Aqui não há o que encurtar — `SEG` já é a abreviação, e a
 * forma de uma letra confunde segunda com sábado e quarta com quinta —, então quem cede
 * é a linha, que vira uma coluna de sete.
 */
@Composable
fun GradeDeDias(
    dias: List<DiaDoTreino>,
    modifier: Modifier = Modifier,
) {
    val locale = LocaleDoApp

    val comCorrida = dias.filter { it.corridas > 0 }
    val descricao = if (comCorrida.isEmpty()) {
        stringResource(R.string.semana_grade_vazia)
    } else {
        stringResource(
            R.string.semana_grade_dias,
            comCorrida.joinToString { nomeDoDia(it.dia.dayOfWeek, locale) },
        )
    }

    val alvo = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { contentDescription = descricao }

    if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
        Column(modifier = alvo, verticalArrangement = Arrangement.spacedBy(EspacoAntesDoRotulo)) {
            dias.forEach { dia ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RotuloDoDia(dia)
                    Box(modifier = Modifier.padding(start = EspacoEntreSegmentos)) {
                        MarcaDoDia(dia)
                    }
                }
            }
        }
        return
    }

    Row(modifier = alvo, horizontalArrangement = Arrangement.spacedBy(EspacoEntreSegmentos)) {
        dias.forEach { dia ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RotuloDoDia(dia)
                Spacer(Modifier.height(EspacoAntesDoRotulo))
                Box(
                    modifier = Modifier.height(LadoDoQuadrado),
                    contentAlignment = Alignment.Center,
                ) {
                    MarcaDoDia(dia)
                }
            }
        }
    }
}

/** `SEG`, e em `leitura` quando for hoje — para o dia corrente aparecer mesmo com marca cheia. */
@Composable
private fun RotuloDoDia(dia: DiaDoTreino) {
    Text(
        text = rotuloCurtoDoDia(dia.dia.dayOfWeek),
        style = MaterialTheme.typography.labelSmall,
        color = if (dia.hoje) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
    )
}

@Composable
private fun MarcaDoDia(dia: DiaDoTreino) {
    val esquema = MaterialTheme.colorScheme
    when {
        dia.corridas > 0 -> Box(
            Modifier.size(LadoDoQuadrado).background(esquema.primary, FormaDado),
        )

        dia.hoje -> Box(
            Modifier.size(LadoDoQuadrado).border(LarguraDoContorno, esquema.outline, FormaDado),
        )

        else -> Box(
            Modifier.size(LadoDaMarcaVazia).background(esquema.onSurfaceVariant, FormaDado),
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
