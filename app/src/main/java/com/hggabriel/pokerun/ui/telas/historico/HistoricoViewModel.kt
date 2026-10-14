package com.hggabriel.pokerun.ui.telas.historico

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private const val ASSINATURA_SOBREVIVE_MS = 5_000L
private const val CHAVE_DO_FILTRO = "filtro"

/**
 * A `RunHistoryScreen` (`F2-T11`, docs/03 §3.15). Observa o histórico vitalício, o
 * usuário (para o plano ativo) e os planos das corridas (para o fuso de cada uma).
 *
 * O filtro mora no `SavedStateHandle`: voltar da `RunDetailScreen`, trocar de aba e a
 * morte de processo devolvem a lista com o mesmo chip (docs/03 §1).
 */
class HistoricoViewModel(
    private val estadoSalvo: SavedStateHandle,
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
) : ViewModel() {

    val filtro: StateFlow<FiltroDoHistorico> =
        estadoSalvo.getStateFlow(CHAVE_DO_FILTRO, FiltroDoHistorico.TODAS)

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<HistoricoUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(HistoricoUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = HistoricoUiState.Carregando,
        )

    fun escolherFiltro(novo: FiltroDoHistorico) {
        estadoSalvo[CHAVE_DO_FILTRO] = novo
    }

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<HistoricoUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(HistoricoUiState.Falhou)

        return combine(corridas.observarHistorico(uid), usuarios.observar(uid), filtro) { lista, usuario, filtro ->
            Triple(lista, usuario?.planoAtivoId, filtro)
        }.flatMapLatest { (lista, planoAtivoId, filtro) ->
            val ids = lista.map { it.planoId }.distinct()
            val dosPlanos = if (ids.isEmpty()) flowOf(emptyList()) else planos.observarVarios(ids)
            dosPlanos.flatMapLatest { lidos ->
                val meses = historico(lista, lidos, planoAtivoId, filtro)
                flowOf(if (meses.isEmpty()) HistoricoUiState.Vazio else HistoricoUiState.Lista(meses))
            }
        }
    }
}
