package com.hggabriel.pokerun.ui.telas.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.auth.ResultadoDeEntrada
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import kotlinx.coroutines.CancellationException
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

    /**
     * O toque no botão. Também é o "repetir" do estado de erro (docs/02 §8, item 7)
     * — não há dois caminhos, e é por isso que o erro não tem botão próprio.
     *
     * **[contexto] é da Activity e não é guardado em lugar nenhum.** A folha de
     * contas do Google é desenhada por cima da Activity viva, então ela precisa
     * deste contexto e não do da aplicação; ele atravessa até o repositório e morre
     * ao fim da chamada.
     */
    fun entrar(contexto: Context) {
        if (_estado.value is LoginUiState.Entrando) return

        _estado.value = LoginUiState.Entrando
        viewModelScope.launch {
            try {
                _estado.value = when (val resultado = autenticacao.entrarComGoogle(contexto)) {
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
