package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.regras.Recordes
import java.time.LocalDate
import java.time.ZoneId

/** O painel de estatísticas (`StatsDashboardScreen`, docs/03 §3.17). */
sealed interface PainelUiState {

    data object Carregando : PainelUiState

    /** Cinco blocos, sempre: cada um decide sozinho o que mostra com o dado que tem. */
    data class Conteudo(
        val hoje: LocalDate,
        val heatmap: GradeDoHeatmap,
        val recordes: Recordes?,
        val volume: VolumeAcumulado,
        val pace: PaceMovel,
        val eficiencia: EficienciaCardiaca,
        /** O fuso em que os dias do eixo da curva cardíaca são lidos (RN-28). */
        val fuso: ZoneId,
    ) : PainelUiState

    data object Falhou : PainelUiState
}

/** O fuso do plano da temporada, para quem ainda não tem plano lido (como o histórico). */
private val FUSO_DE_RESERVA: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * Monta os cinco blocos do painel a partir do que o usuário tem, vitalício e entre planos
 * (RN-16, RN-39). O ano do heatmap é o ano civil de [hoje] (decisão nº 133).
 *
 * O fuso do eixo é o do plano da corrida mais recente; sem corrida, o da temporada.
 */
fun montarPainel(
    corridas: List<Corrida>,
    planos: List<Plano>,
    recordes: Recordes?,
    hoje: LocalDate,
): PainelUiState.Conteudo {
    val fusos = planos.associate { it.id to it.fuso }
    val maisRecente = corridas.filter { !it.descartada && !it.substituida }.maxByOrNull { it.dataHoraInicio }

    return PainelUiState.Conteudo(
        hoje = hoje,
        heatmap = gradeDoHeatmap(corridas, fusos, hoje.year),
        recordes = recordes,
        volume = volumeAcumulado(corridas, planos, hoje),
        pace = paceEmMediaMovel(corridas, fusos, hoje),
        eficiencia = eficienciaCardiaca(corridas),
        fuso = maisRecente?.let { fusos[it.planoId] } ?: FUSO_DE_RESERVA,
    )
}
