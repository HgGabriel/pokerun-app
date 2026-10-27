package com.hggabriel.pokerun.ui.telas.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.RecalculoDeXp
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dominio.regras.ResultadoDoReplay
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/** O que o botão de recálculo mostra depois do replay. */
data class ResumoDoRecalculo(val corridas: Int, val xpAntes: Long, val xpDepois: Long)

/** Os estados do bloco de recálculo: confirmação antes, progresso, resumo ou erro. */
sealed interface EstadoDoRecalculo {
    data object Parado : EstadoDoRecalculo
    data object Confirmando : EstadoDoRecalculo
    data object Recalculando : EstadoDoRecalculo
    data class Concluido(val resumo: ResumoDoRecalculo) : EstadoDoRecalculo
    data object Falhou : EstadoDoRecalculo
}

/**
 * O resumo de [antes] e [depois]: as corridas válidas que o replay percorreu e o XP da
 * temporada corrente nos dois momentos. Sem progresso na temporada, o XP é zero.
 */
fun resumoDoRecalculo(antes: ResultadoDoReplay, depois: ResultadoDoReplay, temporadaId: String?): ResumoDoRecalculo =
    ResumoDoRecalculo(
        corridas = depois.porCorrida.size,
        xpAntes = temporadaId?.let { antes.porTemporada[it]?.xpTotal } ?: 0L,
        xpDepois = temporadaId?.let { depois.porTemporada[it]?.xpTotal } ?: 0L,
    )

/**
 * O botão **Recalcular Agregados** (`F3-T08`, docs/03 §3.11, decisão nº 68): o replay de
 * docs/05 §2.5 desde a primeira corrida. Não é opcional: é o único reparo quando dois
 * aparelhos do mesmo usuário divergem (docs/05 §2.6).
 *
 * O replay roda no escopo do app (`RecalculoDeXp`): sair de Ajustes no meio não o corta.
 */
class RecalculoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val recalculo: RecalculoDeXp,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoDoRecalculo>(EstadoDoRecalculo.Parado)
    val estado: StateFlow<EstadoDoRecalculo> = _estado.asStateFlow()

    fun pedirConfirmacao() {
        _estado.value = EstadoDoRecalculo.Confirmando
    }

    fun cancelar() {
        _estado.value = EstadoDoRecalculo.Parado
    }

    fun recalcular() {
        val uid = autenticacao.uidAtual ?: return
        _estado.value = EstadoDoRecalculo.Recalculando
        viewModelScope.launch {
            try {
                val (antes, depois) = recalculo.comResumo(uid, Instant.EPOCH) { }.await()
                val temporada = temporadaDe(relogio.instant(), temporadas.todas())
                _estado.value = EstadoDoRecalculo.Concluido(resumoDoRecalculo(antes, depois, temporada?.id))
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.value = EstadoDoRecalculo.Falhou
            }
        }
    }
}
