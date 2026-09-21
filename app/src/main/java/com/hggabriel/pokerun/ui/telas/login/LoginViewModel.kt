package com.hggabriel.pokerun.ui.telas.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.auth.ResultadoDeEntrada
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A `LoginScreen` (`F1-T06`, docs/03 §3.1).
 *
 * Duas coisas acontecem em sequência num toque só, e é essa costura que justifica o
 * `ViewModel` numa tela de um botão: autenticar, e **em seguida descobrir se já
 * existe perfil**. As duas juntas são o que a tela precisa saber para sair, e
 * separá-las faria a navegação piscar na `HomeScreen` antes de cair no onboarding.
 */
class LoginViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow<LoginUiState>(LoginUiState.Ocioso)
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** A tentativa em curso. É o que o `Tentar de novo` cancela (`F1-T25`). */
    private var tentativa: Job? = null

    /**
     * O toque no botão. Também é o "repetir" do estado de erro (docs/02 §8, item 7)
     * — não há dois caminhos, e é por isso que o erro não tem botão próprio.
     *
     * **[contexto] é da Activity e não é guardado em lugar nenhum.** A folha de
     * contas do Google é desenhada por cima da Activity viva, então ela precisa
     * deste contexto e não do da aplicação; ele atravessa até o repositório e morre
     * ao fim da chamada.
     *
     * **A espera não tem prazo** (`F1-T25`, decisão nº 84). Aos
     * [ESPERA_ATE_O_REPETIR_MS] sem resposta o estado vira [LoginUiState.Demorando] e o
     * botão volta; nada é cancelado até a pessoa tocar. O que o toque faz em cada
     * estado é [toqueNoBotao], que é onde a regra é testada.
     */
    fun entrar(contexto: Context) {
        when (toqueNoBotao(_estado.value)) {
            ToqueNoBotao.Ignora -> return
            ToqueNoBotao.Reabre -> tentativa?.cancel()
            ToqueNoBotao.Abre -> Unit
        }

        _estado.value = LoginUiState.Entrando
        tentativa = viewModelScope.launch {
            val aviso = launch {
                delay(ESPERA_ATE_O_REPETIR_MS)
                _estado.value = LoginUiState.Demorando
            }
            try {
                val resultado = autenticacao.entrarComGoogle(
                    contexto,
                    // Escolhida a conta, o botão trava de novo: repetir durante a troca
                    // de token cancelaria uma entrada que está dando certo (nº 85).
                    aoEscolherConta = {
                        aviso.cancel()
                        _estado.value = LoginUiState.Entrando
                    },
                )
                _estado.value = when (resultado) {
                    is ResultadoDeEntrada.Autenticado -> autenticado(resultado.uid)
                    ResultadoDeEntrada.Cancelada -> LoginUiState.Ocioso
                    ResultadoDeEntrada.SemContaNoAparelho ->
                        LoginUiState.Erro(R.string.login_erro_sem_conta)
                    is ResultadoDeEntrada.Falhou -> LoginUiState.Erro(R.string.login_erro_generico)
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // A leitura do perfil também pode falhar, e sem rede ela falha.
                _estado.value = LoginUiState.Erro(R.string.login_erro_generico)
            } finally {
                // Filho vivo segura o pai: sem isto, a tentativa que terminou antes dos
                // 15 s esperaria o `delay` acabar e depois pintaria `Demorando` por cima
                // do resultado.
                aviso.cancel()
            }
        }
    }

    /**
     * Já autenticado: falta só saber para onde ir.
     *
     * A sessão do Firebase já está criada quando esta linha roda. Se a leitura do
     * perfil falhar, o estado vira erro e o botão repete — a segunda tentativa não
     * reabre a folha de contas à toa, porque o Credential Manager reconhece a conta
     * e resolve sozinho.
     */
    private suspend fun autenticado(uid: String) =
        LoginUiState.Autenticado(uid = uid, temPerfil = usuarios.buscar(uid) != null)
}
