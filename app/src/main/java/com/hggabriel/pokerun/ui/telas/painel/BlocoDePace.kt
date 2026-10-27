package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.ui.componentes.FormaDoGrafico
import com.hggabriel.pokerun.ui.componentes.GraficoCanvas
import com.hggabriel.pokerun.ui.componentes.PontoDoGrafico
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToLong

/** As semanas da janela da média móvel e o mínimo de corridas de cada uma (docs/03 §3.17). */
private const val SEMANAS_DA_JANELA = 4
private const val CORRIDAS_MINIMAS_POR_SEMANA = 2

private val EspacoEntreLinhas = 8.dp

/** O pace das quatro semanas que terminam em [inicioDaUltimaSemana], em segundos por km. */
data class PontoDePace(val inicioDaUltimaSemana: LocalDate, val paceSegKm: Double)

/** O bloco tem dado bastante para a tendência, ou ainda faltam corridas. */
sealed interface PaceMovel {
    /**
     * [parciais] são o pace de cada semana com corrida, sozinha, enquanto não há janela de 4
     * semanas: o gráfico mostra as pílulas que há, e a legenda diz quanto falta (Revisão 3).
     */
    data class Faltam(val corridas: Int, val parciais: List<PontoDePace> = emptyList()) : PaceMovel
    data class Serie(val pontos: List<PontoDePace>) : PaceMovel
}

/**
 * O pace em média móvel de 4 semanas de docs/03 §3.17 (decisão nº 135): **um ponto por
 * semana de calendário**, que é o pace das quatro semanas que terminam nela, e **ponderado
 * pela distância** (tempo total ÷ km total), como a média móvel do XP (nº 119): um trote
 * de 1 km não pesa como um longão.
 *
 * Um ponto só existe se **cada uma das quatro semanas** tem pelo menos 2 corridas válidas.
 * Sem nenhum ponto, o bloco diz quantas corridas faltam, **de hoje em diante**, para a janela
 * mais próxima de ficar completa: semana que já passou sem as 2 corridas não se refaz, e
 * não entra na conta. Atravessa planos (RN-16).
 */
fun paceEmMediaMovel(corridas: List<Corrida>, fusos: Map<String, ZoneId>, hoje: LocalDate): PaceMovel {
    val porSemana = corridasPorSemana(corridas, fusos)
    val atual = inicioDaSemanaDe(hoje)

    fun janela(inicio: LocalDate): List<List<Corrida>> =
        (0 until SEMANAS_DA_JANELA).map { porSemana[inicio.plusWeeks(it.toLong())].orEmpty() }

    if (porSemana.isNotEmpty()) {
        val primeira = porSemana.keys.min()
        val pontos = semanasEntre(primeira.plusWeeks((SEMANAS_DA_JANELA - 1).toLong()), maxOf(atual, porSemana.keys.max()))
            .mapNotNull { fim ->
                val semanas = janela(fim.minusWeeks((SEMANAS_DA_JANELA - 1).toLong()))
                if (semanas.any { it.size < CORRIDAS_MINIMAS_POR_SEMANA }) return@mapNotNull null
                val doTreino = semanas.flatten()
                PontoDePace(fim, doTreino.sumOf { it.duracaoSeg }.toDouble() / doTreino.sumOf { it.km })
            }
        if (pontos.isNotEmpty()) return PaceMovel.Serie(pontos)
    }

    // Sem janela completa: a mais próxima de ficar. Só vale a janela cujas semanas já
    // passadas estão boas (semana que passou não se refaz); o que falta é o que cabe nas
    // semanas de hoje em diante, e a janela que começa hoje sempre existe.
    val falta = semanasEntre(atual.minusWeeks((SEMANAS_DA_JANELA - 1).toLong()), atual)
        .filter { inicio ->
            val semanas = janela(inicio)
            semanas.indices.all { k -> !inicio.plusWeeks(k.toLong()).isBefore(atual) || semanas[k].size >= CORRIDAS_MINIMAS_POR_SEMANA }
        }
        .minOf { inicio ->
            janela(inicio).withIndex().sumOf { (k, corridasDaSemana) ->
                if (inicio.plusWeeks(k.toLong()).isBefore(atual)) 0 else (CORRIDAS_MINIMAS_POR_SEMANA - corridasDaSemana.size).coerceAtLeast(0)
            }
        }
    val parciais = porSemana.toSortedMap().map { (inicio, daSemana) ->
        PontoDePace(inicio, daSemana.sumOf { it.duracaoSeg }.toDouble() / daSemana.sumOf { it.km })
    }
    return PaceMovel.Faltam(falta, parciais)
}
