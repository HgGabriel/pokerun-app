package com.hggabriel.pokerun.ui.telas.detalhecorrida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * A `RunDetailScreen` (`F2-T09`). Observa a corrida e o plano dela: o replay que segue
 * uma gravação ou uma correção reescreve `xp_creditado` e `xp_detalhe`, e a conta aberta
 * se atualiza sem a tela pedir.
 */
class DetalheCorridaViewModel(
    private val corridaId: String,
    private val autenticacao: AutenticacaoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalheCorridaUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(DetalheCorridaUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = DetalheCorridaUiState.Carregando,
        )

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<DetalheCorridaUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(DetalheCorridaUiState.Falhou)

        return corridas.observar(uid, corridaId).flatMapLatest { corrida ->
            if (corrida == null) {
                flowOf(DetalheCorridaUiState.Falhou)
            } else {
                planos.observar(corrida.planoId).map { plano -> detalheDaCorrida(corrida, plano) }
            }
        }
    }
}
