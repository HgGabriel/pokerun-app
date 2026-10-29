package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.local.TreinadoresRepositorio
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.ui.componentes.BuscaDeTreinadores
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * O motor do cadastro (`F1-T08`, docs/03 §3.2).
 *
 * ### Uma escrita, e ela é o marcador
 *
 * O cadastro grava `users/{uid}` com nome, distância e treinador, e termina. Não existe campo de
 * "cadastro concluído": a existência do documento é o que a `LoginScreen` lê para
 * decidir entre a casca e esta tela.
 *
 * ### O erro fica no passo, não numa tela de erro
 *
 * Gravação falha sem rede. O perfil continua desenhado, com a mensagem embaixo do
 * próprio botão que a disparou, e é esse botão que repete (docs/02 §8, item 7) — o
 * mesmo desenho da `LoginScreen`, e pelo mesmo motivo: dois controles para a mesma ação
 * só fariam o usuário escolher entre sinônimos.
 */
class OnboardingViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    treinadores: TreinadoresRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Perfil())
    val estado: StateFlow<OnboardingUiState> = _estado.asStateFlow()

    /** A busca entre os 1.500 treinadores, a mesma do seletor do Perfil. */
    val busca = BuscaDeTreinadores(viewModelScope, treinadores::lista)

    /** Digitação limpa o erro daquele campo: o usuário está consertando. */
    fun nomeMudou(texto: String) = comPerfil { it.copy(nome = texto, erroNoNome = null) }

    fun distanciaMudou(texto: String) =
        comPerfil { it.copy(distancia = texto, erroNaDistancia = null) }

    /** O campo da distância perdeu o foco: daqui em diante, a distância inválida vira erro. */
    fun distanciaSaiu() = comPerfil { it.copy(distanciaTocada = true) }

    /** Só um nome da lista vale (regra 1 do treinador). O toque de novo no mesmo não desmarca. */
    fun escolherTreinador(nome: String) {
        if (!busca.aceita(nome)) return
        comPerfil { it.copy(treinador = nome) }
    }

    /** O botão de emitir só liga com a licença completa. */
    fun completa(perfil: OnboardingUiState.Perfil): Boolean =
        fichaCompleta(perfil.nome, perfil.distancia, perfil.treinador, busca::aceita)

    /**
     * Valida a licença, grava `users/{uid}` e encerra o cadastro.
     *
     * O botão já vem desligado sem a licença completa; a validação aqui é a segunda trava,
     * e a mensagem de nome e de distância continua a mesma de antes.
     */
    fun salvarPerfil() {
        val perfil = _estado.value as? OnboardingUiState.Perfil ?: return

        val nome = nomeDoPerfil(perfil.nome)
        val km = distanciaEmKm(perfil.distancia)
        if (nome == null || km == null) {
            _estado.value = perfil.copy(
                erroNoNome = R.string.onboarding_erro_nome.takeIf { nome == null },
                erroNaDistancia = R.string.onboarding_erro_distancia.takeIf { km == null },
                erroAoGravar = null,
            )
            return
        }

        val treinador = perfil.treinador
        if (treinador == null || !completa(perfil)) return

        val uid = autenticacao.uidAtual ?: run {
            _estado.value = perfil.copy(erroAoGravar = R.string.onboarding_erro_gravar)
            return
        }

        gravar(perfil) {
            usuarios.salvar(Usuario(uid = uid, nome = nome, baselineKm = km, treinador = treinador))
            _estado.value = OnboardingUiState.Concluido
        }
    }

    /**
     * Põe o perfil em [OnboardingUiState.Salvando], roda a escrita e devolve o perfil
     * com a mensagem quando ela falha.
     *
     * O perfil volta **como estava**, com o que o usuário digitou: perder o formulário
     * numa queda de rede é o jeito mais rápido de fazer alguém desistir do cadastro.
     */
    private fun gravar(perfil: OnboardingUiState.Perfil, escrita: suspend () -> Unit) {
        if (_estado.value is OnboardingUiState.Salvando) return
        _estado.value = OnboardingUiState.Salvando(perfil)
        viewModelScope.launch {
            try {
                escrita()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.value = perfil.copy(erroAoGravar = R.string.onboarding_erro_gravar)
            }
        }
    }

    private inline fun comPerfil(bloco: (OnboardingUiState.Perfil) -> OnboardingUiState.Perfil) {
        val perfil = _estado.value as? OnboardingUiState.Perfil ?: return
        _estado.value = bloco(perfil)
    }
}
