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
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.ui.componentes.FormaDoGrafico
import com.hggabriel.pokerun.ui.componentes.GraficoCanvas
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.PontoDoGrafico
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** O mínimo de dado do bloco (docs/03 §3.17): 3 semanas de treino. */
private const val SEMANAS_DE_TREINO_MINIMAS = 3

private val EspacoEntreLinhas = 8.dp

/** O fuso do plano da temporada, para a corrida cujo plano não foi lido (como o histórico). */
private val FusoDeReserva: ZoneId = ZoneId.of("America/Sao_Paulo")

/** Uma semana de calendário (segunda a domingo): o que correu nela e o somado até ela. */
data class PontoDeVolume(val inicioDaSemana: LocalDate, val kmDaSemana: Double, val kmAcumulado: Double)

/** O bloco tem dado bastante para a curva, ou ainda faltam semanas de treino. */
sealed interface VolumeAcumulado {
    /**
     * [parciais] são as semanas que já existem, na mesma forma da série: abaixo do mínimo o
     * gráfico mostra as pílulas que há, e a legenda diz quanto falta (Revisão 3).
     */
    data class Faltam(val semanas: Int, val parciais: List<PontoDeVolume> = emptyList()) : VolumeAcumulado

    /** [marcadores] são os índices de [pontos] que contêm uma prova concluída. */
    data class Serie(val pontos: List<PontoDeVolume>, val marcadores: Set<Int>) : VolumeAcumulado
}

/** A segunda-feira da semana de [data]: a semana do painel é a de calendário, e não a do plano. */
internal fun inicioDaSemanaDe(data: LocalDate): LocalDate = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** As corridas válidas (RN-24, RN-31) por semana de calendário, no fuso do plano de cada uma (RN-28). */
internal fun corridasPorSemana(corridas: List<Corrida>, fusos: Map<String, ZoneId>): Map<LocalDate, List<Corrida>> =
    corridas
        .filter { !it.descartada && !it.substituida && it.km > 0 && it.duracaoSeg > 0 }
        .groupBy { inicioDaSemanaDe(it.dataHoraInicio.atZone(fusos[it.planoId] ?: FusoDeReserva).toLocalDate()) }

/** As segundas-feiras de [primeira] a [ultima], sem buraco: semana sem corrida também conta. */
internal fun semanasEntre(primeira: LocalDate, ultima: LocalDate): List<LocalDate> =
    generateSequence(primeira) { it.plusWeeks(1) }.takeWhile { !it.isAfter(ultima) }.toList()

/**
 * O volume semanal acumulado de docs/03 §3.17 (decisão nº 134): a **soma corrente** dos
 * quilômetros, semana de calendário a semana de calendário, da primeira corrida do usuário
 * até a semana de [hoje], sem pular semana parada. O volume de cada semana vem junto, para
 * a leitura falada. Atravessa planos, como o heatmap (RN-16).
 *
 * O mínimo é 3 semanas **com corrida**: abaixo dele o bloco diz quantas faltam.
 *
 * Os marcadores são as provas concluídas: o plano que o usuário correu e cuja data da
 * prova já passou, na semana em que ela cai, no fuso do plano.
 */
fun volumeAcumulado(corridas: List<Corrida>, planos: List<Plano>, hoje: LocalDate): VolumeAcumulado {
    val fusos = planos.associate { it.id to it.fuso }
    val porSemana = corridasPorSemana(corridas, fusos)
    if (porSemana.isEmpty()) return VolumeAcumulado.Faltam(SEMANAS_DE_TREINO_MINIMAS)

    val semanas = semanasEntre(porSemana.keys.min(), maxOf(porSemana.keys.max(), inicioDaSemanaDe(hoje)))
    var acumulado = 0.0
    val pontos = semanas.map { inicio ->
        val doTreino = porSemana[inicio].orEmpty().sumOf { it.km }
        acumulado += doTreino
        PontoDeVolume(inicio, doTreino, acumulado)
    }
    if (porSemana.size < SEMANAS_DE_TREINO_MINIMAS) {
        return VolumeAcumulado.Faltam(SEMANAS_DE_TREINO_MINIMAS - porSemana.size, pontos)
    }

    val planosCorridos = corridas.filter { !it.descartada && !it.substituida }.map { it.planoId }.toSet()
    val marcadores = planos
        .filter { it.id in planosCorridos }
        .map { it.dataProva.atZone(it.fuso).toLocalDate() }
        .filter { !it.isAfter(hoje) }
        .map { semanas.indexOf(inicioDaSemanaDe(it)) }
        .filter { it >= 0 }
        .toSet()

    return VolumeAcumulado.Serie(pontos, marcadores)
}
