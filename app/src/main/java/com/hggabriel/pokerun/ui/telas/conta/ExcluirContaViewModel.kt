package com.hggabriel.pokerun.ui.telas.conta

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.ExclusaoDaContaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.firestore.confirmouAExclusao
import com.hggabriel.pokerun.dados.firestore.prazoDaExclusao
import com.hggabriel.pokerun.dominio.modelo.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/** As duas etapas da exclusão: ver o que sai, e digitar a confirmação. */
enum class EtapaDaExclusao { O_QUE_SAI, CONFIRMACAO }

data class ExcluirContaUiState(
    val etapa: EtapaDaExclusao = EtapaDaExclusao.O_QUE_SAI,
    /** As corridas válidas e os km delas. Nulos enquanto a leitura não volta. */
    val corridas: Int? = null,
    val km: Double? = null,
    /** O prazo se o pedido fosse agora: as datas da tela dizem quando a conta sai de vez. */
    val prazo: Instant,
    val digitado: String = "",
    /** A terceira etapa: o diálogo final, com a data. */
    val confirmandoFinal: Boolean = false,
    val excluindo: Boolean = false,
    @param:StringRes val erro: Int? = null,
    /** Terminal: a conta foi marcada e a sessão saiu; a tela leva ao login. */
    val saiu: Boolean = false,
) {
    val podeExcluir: Boolean get() = confirmouAExclusao(digitado) && !excluindo
}

/**
 * A exclusão da conta em três etapas (29/10/2026): o que sai, a palavra `EXCLUIR` e o diálogo
 * final com a data. Confirmada, marca tudo com o prazo de 30 dias e encerra a sessão.
 */
class ExcluirContaViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val corridas: CorridaRepositorio,
    private val exclusao: ExclusaoDaContaRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow(ExcluirContaUiState(prazo = prazoDaExclusao(relogio.instant())))
    val estado: StateFlow<ExcluirContaUiState> = _estado.asStateFlow()

    private var usuario: Usuario? = null

    init {
        viewModelScope.launch {
            val uid = autenticacao.uidAtual ?: return@launch
            try {
                usuario = usuarios.buscar(uid)
                val validas = corridas.buscarTodas(uid).filter { !it.descartada && !it.substituida }
                _estado.update { it.copy(corridas = validas.size, km = validas.sumOf { c -> c.km }) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // Sem os números, a primeira etapa diz só o que sai, sem contar.
            }
        }
    }

    fun continuar() = _estado.update { it.copy(etapa = EtapaDaExclusao.CONFIRMACAO) }

    fun voltarAoQueSai() = _estado.update { it.copy(etapa = EtapaDaExclusao.O_QUE_SAI, digitado = "", erro = null) }

    fun digitar(texto: String) = _estado.update { it.copy(digitado = texto, erro = null) }

    fun pedirConfirmacaoFinal() = _estado.update {
        if (it.podeExcluir) it.copy(confirmandoFinal = true, prazo = prazoDaExclusao(relogio.instant())) else it
    }

    fun cancelarConfirmacaoFinal() = _estado.update { it.copy(confirmandoFinal = false) }

    fun excluir() {
        val uid = autenticacao.uidAtual ?: return
        if (_estado.value.excluindo) return
        _estado.update { it.copy(confirmandoFinal = false, excluindo = true, erro = null) }
        viewModelScope.launch {
            try {
                val planos = (usuario ?: usuarios.buscar(uid))?.planos.orEmpty()
                exclusao.pedir(uid, planos, relogio.instant())
                autenticacao.sair()
                _estado.update { it.copy(excluindo = false, saiu = true) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.update { it.copy(excluindo = false, erro = R.string.conta_erro) }
            }
        }
    }
}
