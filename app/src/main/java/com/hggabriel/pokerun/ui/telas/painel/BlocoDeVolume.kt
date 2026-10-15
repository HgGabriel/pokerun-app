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
    data class Faltam(val semanas: Int) : VolumeAcumulado

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
    if (porSemana.size < SEMANAS_DE_TREINO_MINIMAS) {
        return VolumeAcumulado.Faltam(SEMANAS_DE_TREINO_MINIMAS - porSemana.size)
    }

    val semanas = semanasEntre(porSemana.keys.min(), maxOf(porSemana.keys.max(), inicioDaSemanaDe(hoje)))
    var acumulado = 0.0
    val pontos = semanas.map { inicio ->
        val doTreino = porSemana[inicio].orEmpty().sumOf { it.km }
        acumulado += doTreino
        PontoDeVolume(inicio, doTreino, acumulado)
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

/**
 * O bloco do volume (`F3-T04`): a linha do acumulado, com uma linha vertical em cada prova
 * concluída, ou a frase do mínimo.
 */
@Composable
fun BlocoDeVolume(estado: VolumeAcumulado, modifier: Modifier = Modifier) {
    val titulo = stringResource(R.string.painel_volume_titulo)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titulo.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        when (estado) {
            is VolumeAcumulado.Faltam -> Text(
                text = pluralStringResource(R.plurals.painel_volume_faltam, estado.semanas, estado.semanas),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is VolumeAcumulado.Serie -> {
                GraficoCanvas(
                    titulo = titulo,
                    pontos = estado.pontos.map { ponto ->
                        PontoDoGrafico(
                            rotulo = "%02d/%02d".format(ponto.inicioDaSemana.dayOfMonth, ponto.inicioDaSemana.monthValue),
                            valor = ponto.kmAcumulado,
                            valorEscrito = formatarKm(ponto.kmAcumulado, LocaleDoApp),
                        )
                    },
                    unidade = stringResource(R.string.painel_unidade_km),
                    forma = FormaDoGrafico.LINHA,
                    apartirDoZero = true,
                    marcadores = estado.marcadores,
                )
                if (estado.marcadores.isNotEmpty()) {
                    Spacer(Modifier.height(EspacoEntreLinhas))
                    Text(
                        text = pluralStringResource(R.plurals.painel_volume_provas, estado.marcadores.size, estado.marcadores.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun VolumeComSeriePreview() {
    val inicio = LocalDate.of(2026, 8, 3)
    val kms = listOf(12.0, 18.5, 0.0, 22.0, 26.4, 30.0, 14.0, 33.2)
    var soma = 0.0
    val pontos = kms.mapIndexed { i, km ->
        soma += km
        PontoDeVolume(inicio.plusWeeks(i.toLong()), km, soma)
    }
    PokerunTheme { BlocoDeVolume(VolumeAcumulado.Serie(pontos, setOf(5))) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun VolumeComFaltaPreview() {
    PokerunTheme { BlocoDeVolume(VolumeAcumulado.Faltam(2)) }
}
