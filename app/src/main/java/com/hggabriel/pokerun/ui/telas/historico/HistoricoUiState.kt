package com.hggabriel.pokerun.ui.telas.historico

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Os dois chips de docs/03 §3.15. */
enum class FiltroDoHistorico { TODAS, PLANO_ATUAL }

/** A `RunHistoryScreen` (docs/03 §3.15). */
sealed interface HistoricoUiState {

    data object Carregando : HistoricoUiState

    data class Lista(val meses: List<MesDoHistorico>) : HistoricoUiState

    /** Nenhuma corrida no filtro escolhido. */
    data object Vazio : HistoricoUiState

    data object Falhou : HistoricoUiState
}

data class MesDoHistorico(val mes: YearMonth, val corridas: List<LinhaDoHistorico>)

data class LinhaDoHistorico(
    val id: String,
    val dia: LocalDate,
    val km: Double,
    val duracaoSeg: Long,
)

/**
 * O histórico vitalício agrupado por mês, do mais recente para o mais antigo.
 *
 * **Descartada não aparece** (ficha de `F2-T11`, RN-31), e a substituída também não: o
 * registro que a corrigiu está na lista no lugar dela (RN-24). O dia de cada corrida é o
 * do fuso do plano dela (RN-28): uma corrida de 31/08 às 22h em São Paulo fica em agosto.
 * Plano que não foi lido cai no fuso de São Paulo, o da temporada.
 *
 * `PLANO_ATUAL` mostra só as corridas do plano ativo (RN-12); sem plano ativo, nada.
 */
fun historico(
    corridas: List<Corrida>,
    planos: List<Plano>,
    planoAtivoId: String?,
    filtro: FiltroDoHistorico,
): List<MesDoHistorico> {
    val fusos = planos.associate { it.id to it.fuso }
    return corridas
        .asSequence()
        .filter { !it.descartada && !it.substituida }
        .filter { filtro == FiltroDoHistorico.TODAS || it.planoId == planoAtivoId }
        .sortedByDescending { it.dataHoraInicio }
        .map { corrida ->
            val fuso = fusos[corrida.planoId] ?: FUSO_DE_RESERVA
            LinhaDoHistorico(
                id = corrida.id,
                dia = corrida.dataHoraInicio.atZone(fuso).toLocalDate(),
                km = corrida.km,
                duracaoSeg = corrida.duracaoSeg,
            )
        }
        .groupBy { YearMonth.from(it.dia) }
        .map { (mes, linhas) -> MesDoHistorico(mes, linhas) }
}

private val FUSO_DE_RESERVA: ZoneId = ZoneId.of("America/Sao_Paulo")
