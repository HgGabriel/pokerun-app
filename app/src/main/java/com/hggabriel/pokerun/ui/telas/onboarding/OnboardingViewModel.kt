package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.healthconnect.SaudeRepositorio
import com.hggabriel.pokerun.dominio.modelo.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * O motor do cadastro (`F1-T08`, docs/03 §3.2).
 *
 * ### Duas escritas, e não uma
 *
 * O perfil é gravado ao fim do passo 2, e a fonte canônica ao fim do passo 5. Não é
 * desperdício: `definirFonteCanonica` é um `update`, que exige o documento existindo, e
 * a ordem do cadastro é rígida — a fonte só se conhece três passos depois do nome
 * (RN-22, e o KDoc do [UsuarioRepositorio]).
 *
 * O que se ganha junto é retomada: quem for interrompido durante os passos do Health
 * Connect já tem perfil, e reabre o app na `HomeScreen` com `fonte_canonica` nula, que
 * é exatamente o modo manual de docs/05 §4.4. A alternativa — segurar tudo em memória e
 * gravar uma vez no fim — perde nome e distância em toda interrupção.
 *
 * ### O erro fica no passo, não numa tela de erro
 *
 * Gravação falha sem rede. O passo que falhou continua desenhado, com a mensagem
 * embaixo do próprio botão que a disparou, e é esse botão que repete (docs/02 §8, item
 * 7) — o mesmo desenho da `LoginScreen`, e pelo mesmo motivo: dois controles para a
 * mesma ação só fariam o usuário escolher entre sinônimos.
 */
class OnboardingViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val saude: SaudeRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Perfil())
    val estado: StateFlow<OnboardingUiState> = _estado.asStateFlow()

    /** O conjunto que o contrato de permissão do passo 3 pede. */
    val permissoesDeSaude: Set<String> get() = saude.permissoesDeLeitura

    // -----------------------------------------------------------------------
    // Passos 1 e 2 — o perfil
    // -----------------------------------------------------------------------

    /** Digitação limpa o erro daquele campo: o usuário está consertando. */
    fun nomeMudou(texto: String) = comPerfil { it.copy(nome = texto, erroNoNome = null) }

    fun distanciaMudou(texto: String) =
        comPerfil { it.copy(distancia = texto, erroNaDistancia = null) }

    /**
     * Valida os dois campos, grava `users/{uid}` e segue para o passo 3.
     *
     * A validação é no toque e não na digitação: acusar "escreva o seu nome" na
     * primeira letra apagada é ruído, e o campo ainda está sendo preenchido.
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

        val uid = autenticacao.uidAtual ?: run {
            _estado.value = perfil.copy(erroAoGravar = R.string.onboarding_erro_gravar)
            return
        }

        gravar(perfil) {
            usuarios.salvar(Usuario(uid = uid, nome = nome, baselineKm = km))
            avancarDepoisDoPerfil()
        }
    }

    // -----------------------------------------------------------------------
    // Passo 3 — a permissão
    // -----------------------------------------------------------------------

    /**
     * A folha de permissão do Health Connect fechou.
     *
     * O resultado do contrato não é consultado: quem responde é o
     * `permissionController`. A folha devolve o conjunto que **ela** pediu, e o usuário
     * pode ter concedido parte das caixas — a pergunta que importa é se `READ_EXERCISE`
     * ficou de pé, e só o controlador sabe.
     */
    fun permissaoRespondida() {
        viewModelScope.launch {
            val passo = try {
                passoDepoisDaPermissao(saude.podeLerTreinos())
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                OnboardingUiState.SolicitandoPermissao(negada = true)
            }
            _estado.value = passo
            if (passo == OnboardingUiState.LendoOrigens) lerOrigens()
        }
    }

    // -----------------------------------------------------------------------
    // Passos 4 e 5 — as origens e a fonte canônica
    // -----------------------------------------------------------------------

    /** Relê os últimos 30 dias depois de uma falha de leitura (docs/02 §8, item 7). */
    fun tentarLerOrigens() {
        _estado.value = OnboardingUiState.LendoOrigens
        viewModelScope.launch { lerOrigens() }
    }

    fun escolherOrigem(pacote: String) {
        val passo = _estado.value as? OnboardingUiState.EscolhendoFonte ?: return
        _estado.value = passo.copy(escolhida = pacote, erroAoGravar = null)
    }

    /** Grava `fonte_canonica` e encerra o cadastro (RN-22). */
    fun salvarFonte() {
        val passo = _estado.value as? OnboardingUiState.EscolhendoFonte ?: return
        val pacote = passo.escolhida ?: return

        val uid = autenticacao.uidAtual ?: run {
            _estado.value = passo.copy(erroAoGravar = R.string.onboarding_erro_gravar)
            return
        }

        gravar(passo) {
            usuarios.definirFonteCanonica(uid, pacote)
            _estado.value = OnboardingUiState.Concluido
        }
    }

    /**
     * `Continuar sem sincronização` (docs/03 §3.2).
     *
     * **Não grava nada**, e é isso que o torna correto: `fonte_canonica` nula significa
     * "nada a filtrar porque nada é importado", nunca "aceite tudo" (RN-22). O perfil já
     * está no Firestore desde o passo 2, então não há o que salvar aqui.
     */
    fun seguirSemSincronizacao() {
        _estado.value = OnboardingUiState.Concluido
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    private suspend fun avancarDepoisDoPerfil() {
        val passo = try {
            passoDepoisDoPerfil(saude.status(), saude.podeLerTreinos())
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (erro: Exception) {
            // Falhar ao perguntar ao Health Connect não pode custar o cadastro: o
            // perfil já está gravado, e o app inteiro funciona em modo manual.
            OnboardingUiState.Concluido
        }
        _estado.value = passo
        if (passo == OnboardingUiState.LendoOrigens) lerOrigens()
    }

    private suspend fun lerOrigens() {
        _estado.value = try {
            OnboardingUiState.EscolhendoFonte(origens = saude.origensRecentes())
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (erro: Exception) {
            OnboardingUiState.EscolhendoFonte(falhouALeitura = true)
        }
    }

    /**
     * Põe o passo em [OnboardingUiState.Salvando], roda a escrita e devolve o passo com
     * a mensagem quando ela falha.
     *
     * O passo volta **como estava**, com o que o usuário digitou e a origem que ele
     * escolheu no lugar: perder o formulário numa queda de rede é o jeito mais rápido
     * de fazer alguém desistir do cadastro.
     */
    private fun gravar(passo: OnboardingUiState.PassoQueGrava, escrita: suspend () -> Unit) {
        if (_estado.value is OnboardingUiState.Salvando) return
        _estado.value = OnboardingUiState.Salvando(passo)
        viewModelScope.launch {
            try {
                escrita()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.value = when (passo) {
                    is OnboardingUiState.Perfil ->
                        passo.copy(erroAoGravar = R.string.onboarding_erro_gravar)
                    is OnboardingUiState.EscolhendoFonte ->
                        passo.copy(erroAoGravar = R.string.onboarding_erro_gravar)
                }
            }
        }
    }

    private inline fun comPerfil(bloco: (OnboardingUiState.Perfil) -> OnboardingUiState.Perfil) {
        val perfil = _estado.value as? OnboardingUiState.Perfil ?: return
        _estado.value = bloco(perfil)
    }
}
