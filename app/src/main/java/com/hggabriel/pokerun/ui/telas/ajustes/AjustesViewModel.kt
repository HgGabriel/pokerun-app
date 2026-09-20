package com.hggabriel.pokerun.ui.telas.ajustes

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * O motor dos Ajustes (`F1-T17`, docs/03 §3.11).
 *
 * ### O perfil é lido uma vez, e não observado
 *
 * `buscar` e não `observar`: esta tela **edita** o documento, e um listener devolveria a
 * própria escrita como emissão nova — sobrescrevendo o campo debaixo do dedo de quem
 * ainda está digitando. A `HomeScreen` observa porque só lê. Aqui o valor gravado é a
 * base da comparação de [perfilMudou], e ele precisa ficar parado enquanto a edição
 * acontece.
 *
 * ### A fonte canônica reusa o caminho do cadastro
 *
 * `status`, `podeLerTreinos` e `origensRecentes` são os mesmos três de `F1-T08`, e a
 * ordem entre eles é a mesma: sem permissão a lista sai vazia, e essa vazia é
 * indistinguível do aparelho sem treino gravado. É [blocoDeOrigem] quem decide qual das
 * três seções a tela desenha.
 *
 * ### Sair não espera nada
 *
 * `signOut` é local e síncrono (o KDoc de [AutenticacaoRepositorio.sair] diz isso). Não
 * há escrita a confirmar, então não há estado de espera — o diálogo confirma e a tela
 * sai.
 */
class AjustesViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val saude: SaudeRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(AjustesUiState())
    val estado: StateFlow<AjustesUiState> = _estado.asStateFlow()

    /** O que está gravado. É a base de [perfilMudou], e só muda quando a escrita volta. */
    private var gravado: Usuario? = null

    /** O conjunto que o contrato de permissão pede, igual ao do passo 3 do cadastro. */
    val permissoesDeSaude: Set<String> get() = saude.permissoesDeLeitura

    init {
        carregar()
    }

    // -----------------------------------------------------------------------
    // Abertura
    // -----------------------------------------------------------------------

    private fun carregar() {
        val uid = autenticacao.uidAtual ?: run {
            _estado.update { it.copy(carregando = false, erroDeTela = R.string.ajustes_erro_salvar) }
            return
        }

        viewModelScope.launch {
            val usuario = try {
                usuarios.buscar(uid)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                null
            }

            if (usuario == null) {
                _estado.update {
                    it.copy(carregando = false, erroDeTela = R.string.ajustes_erro_salvar)
                }
                return@launch
            }

            gravado = usuario
            _estado.update {
                it.copy(
                    carregando = false,
                    nome = usuario.nome,
                    baseline = textoDaBaseline(usuario.baselineKm),
                    fonteAtual = usuario.fonteCanonica,
                    escolhida = usuario.fonteCanonica,
                    erroDeTela = null,
                )
            }
            conferirOrigem()
        }
    }

    /**
     * Decide qual seção de origem desenhar e, quando dá, lê as origens.
     *
     * Roda na abertura e de novo quando a folha de permissão fecha: conceder muda o
     * bloco, e sem reler a tela continuaria pedindo a permissão que acabou de ser dada.
     */
    private fun conferirOrigem() {
        viewModelScope.launch {
            val bloco = try {
                blocoDeOrigem(saude.status(), saude.podeLerTreinos())
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // Falhar ao perguntar ao Health Connect não é falha de tela: o app opera
                // em modo manual sem ele (docs/05 §4.4).
                BlocoDeOrigem.Indisponivel
            }

            // Só pergunta pela loja quando ela pode ser oferecida: nos outros três
            // blocos a resposta não é lida, e a consulta ao `PackageManager` seria
            // trabalho jogado fora na abertura de toda tela de Ajustes.
            val temLoja = bloco == BlocoDeOrigem.PrecisaAtualizar && saude.temLojaParaAtualizar()

            _estado.update { it.copy(bloco = bloco, temLojaParaAtualizar = temLoja) }
            if (bloco == BlocoDeOrigem.PodeEscolher) lerOrigens()
        }
    }

    // -----------------------------------------------------------------------
    // O perfil
    // -----------------------------------------------------------------------

    /** Digitação limpa o erro daquele campo e a confirmação: a pessoa está mexendo. */
    fun nomeMudou(texto: String) = _estado.update {
        it.copy(nome = texto, erros = it.erros.copy(nome = null), perfilSalvo = false)
            .comPodeSalvar()
    }

    fun baselineMudou(texto: String) = _estado.update {
        it.copy(baseline = texto, erros = it.erros.copy(baseline = null), perfilSalvo = false)
            .comPodeSalvar()
    }

    /**
     * Valida os dois campos e grava `nome` e `baseline_km`.
     *
     * **Validação no toque e não na digitação**, como no cadastro: acusar "escreva o seu
     * nome" na primeira letra apagada é ruído sobre um campo que ainda está sendo
     * mexido.
     */
    fun salvarPerfil() {
        val atual = _estado.value
        if (atual.salvandoPerfil) return

        when (val validacao = validarPerfil(atual.nome, atual.baseline)) {
            is ValidacaoDoPerfil.Invalido ->
                _estado.update { it.copy(erros = validacao.erros, erroDeTela = null) }

            is ValidacaoDoPerfil.Valido -> {
                val antes = gravado
                // O texto diferia, mas o número não: `7,50` sobre `7,5` gravaria o valor
                // que já está lá. A tela confirma sem custar uma escrita.
                if (antes != null && !perfilMudou(antes, validacao)) {
                    _estado.update {
                        it.copy(
                            nome = validacao.nome,
                            baseline = textoDaBaseline(validacao.baselineKm),
                            erros = ErrosDoPerfil(),
                            perfilSalvo = true,
                        ).comPodeSalvar()
                    }
                    return
                }
                gravarPerfil(validacao)
            }
        }
    }

    private fun gravarPerfil(valido: ValidacaoDoPerfil.Valido) {
        val uid = autenticacao.uidAtual ?: run {
            _estado.update { it.copy(erroDeTela = R.string.ajustes_erro_salvar) }
            return
        }

        _estado.update { it.copy(salvandoPerfil = true, erros = ErrosDoPerfil(), erroDeTela = null) }
        viewModelScope.launch {
            try {
                usuarios.atualizarPerfil(uid, valido.nome, valido.baselineKm)
                gravado = gravado?.copy(nome = valido.nome, baselineKm = valido.baselineKm)
                _estado.update {
                    it.copy(
                        salvandoPerfil = false,
                        nome = valido.nome,
                        baseline = textoDaBaseline(valido.baselineKm),
                        perfilSalvo = true,
                    ).comPodeSalvar()
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // O formulário volta como estava: perder o que foi digitado numa queda de
                // rede é o jeito mais rápido de fazer alguém desistir da edição.
                _estado.update {
                    it.copy(salvandoPerfil = false, erroDeTela = R.string.ajustes_erro_salvar)
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // A origem dos treinos
    // -----------------------------------------------------------------------

    /** A folha de permissão fechou. Quem responde é o `permissionController`, não o contrato. */
    fun permissaoRespondida() = conferirOrigem()

    fun tentarLerOrigens() {
        _estado.update { it.copy(falhouALeitura = false) }
        viewModelScope.launch { lerOrigens() }
    }

    fun escolherOrigem(pacote: String) = _estado.update {
        it.copy(escolhida = pacote, erroDeTela = null)
    }

    /** // RN-22 — grava a fonte canônica. É o mesmo `update` do passo 5 do cadastro. */
    fun salvarFonte() {
        val atual = _estado.value
        val pacote = atual.escolhida ?: return
        if (atual.salvandoFonte || pacote == atual.fonteAtual) return

        val uid = autenticacao.uidAtual ?: run {
            _estado.update { it.copy(erroDeTela = R.string.ajustes_erro_salvar) }
            return
        }

        _estado.update { it.copy(salvandoFonte = true, erroDeTela = null) }
        viewModelScope.launch {
            try {
                usuarios.definirFonteCanonica(uid, pacote)
                gravado = gravado?.copy(fonteCanonica = pacote)
                _estado.update { it.copy(salvandoFonte = false, fonteAtual = pacote) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.update {
                    it.copy(salvandoFonte = false, erroDeTela = R.string.ajustes_erro_salvar)
                }
            }
        }
    }

    private suspend fun lerOrigens() {
        _estado.update { it.copy(lendoOrigens = true) }
        _estado.update {
            try {
                it.copy(lendoOrigens = false, origens = saude.origensRecentes(), falhouALeitura = false)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                it.copy(lendoOrigens = false, origens = emptyList(), falhouALeitura = true)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Sair
    // -----------------------------------------------------------------------

    fun pedirParaSair() = _estado.update { it.copy(confirmandoSaida = true) }

    fun desistirDeSair() = _estado.update { it.copy(confirmandoSaida = false) }

    /**
     * Encerra a sessão. A navegação de volta à `LoginScreen` é decidida lá fora, pelo
     * mesmo motivo do `OnboardingUiState.Concluido`: uma tela não conhece a rota das
     * outras.
     */
    fun sair() {
        autenticacao.sair()
        _estado.update { it.copy(confirmandoSaida = false, saiuDaConta = true) }
    }

    fun saidaConsumida() = _estado.update { it.copy(saiuDaConta = false) }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    /** Ver o KDoc de [AjustesUiState.podeSalvar]: é comparação de texto, de propósito. */
    private fun AjustesUiState.comPodeSalvar(): AjustesUiState {
        val antes = gravado ?: return copy(podeSalvar = false)
        return copy(
            podeSalvar = nome != antes.nome || baseline != textoDaBaseline(antes.baselineKm),
        )
    }
}
