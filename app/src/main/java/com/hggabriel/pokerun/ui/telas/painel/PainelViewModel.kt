package com.hggabriel.pokerun.ui.telas.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** O dia de hoje conta no fuso da temporada, que é o do ano civil de `kanto-2026` (nº 100). */
private val FUSO_DA_TEMPORADA: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * A `StatsDashboardScreen` (`F3-T09`, docs/03 §3.17): observa o histórico vitalício, os
 * planos das corridas (para o fuso de cada uma) e os recordes de `agregados/records`.
 * Nenhuma escrita: o painel só lê (docs/05 §1).
 */
class PainelViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val corridas: CorridaRepositorio,
    private val planos: PlanoRepositorio,
    private val progresso: ProgressoRepositorio,
    private val relogio: Clock = Clock.system(FUSO_DA_TEMPORADA),
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<PainelUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(PainelUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = PainelUiState.Carregando,
        )

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<PainelUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(PainelUiState.Falhou)

        return combine(corridas.observarHistorico(uid), progresso.observarRecordes(uid)) { lista, recordes ->
            lista to recordes
        }.flatMapLatest { (lista, recordes) ->
            planos.observarVarios(lista.map { it.planoId }.distinct()).map { lidos ->
                montarPainel(lista, lidos, recordes, LocalDate.now(relogio))
            }
        }
    }
}
