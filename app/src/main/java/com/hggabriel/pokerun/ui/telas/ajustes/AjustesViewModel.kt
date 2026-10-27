package com.hggabriel.pokerun.ui.telas.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.MudancaDoPerfil
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
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
 * ### Sair não espera nada
 *
 * `signOut` é local e síncrono (o KDoc de [AutenticacaoRepositorio.sair] diz isso). Não
 * há escrita a confirmar, então não há estado de espera — o diálogo confirma e a tela
 * sai.
 */
class AjustesViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(AjustesUiState())
    val estado: StateFlow<AjustesUiState> = _estado.asStateFlow()

    /** O que está gravado. É a base de [perfilMudou], e só muda quando a escrita volta. */
    private var gravado: Usuario? = null

    init {
        carregar()
    }

    // -----------------------------------------------------------------------
    // Abertura
    // -----------------------------------------------------------------------

    private fun carregar() {
        val uid = autenticacao.uidAtual ?: run {
            _estado.update { it.copy(carregando = false, erroDeTela = R.string.ajustes_erro_ler) }
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
                // Falha de leitura, e não de gravação: dizer "não deu para salvar" a quem
                // ainda não salvou nada mandava a pessoa procurar o que perdeu.
                _estado.update {
                    it.copy(carregando = false, erroDeTela = R.string.ajustes_erro_ler)
                }
                return@launch
            }

            gravado = usuario
            _estado.update {
                it.copy(
                    carregando = false,
                    nome = usuario.nome,
                    baseline = textoDaBaseline(usuario.baselineKm),
                    erroDeTela = null,
                )
            }
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

        val antes = gravado
        // Sem esperar o servidor: o lote entra na fila local e sobe quando houver rede
        // (`UsuarioRepositorio.atualizarPerfil`). A falha que sobra aqui é a síncrona, de
        // montar o lote, e ela deixa o formulário como estava.
        try {
            usuarios.atualizarPerfil(
                uid,
                antes?.planos.orEmpty(),
                MudancaDoPerfil(nome = valido.nome, baselineKm = valido.baselineKm),
            )
        } catch (erro: Exception) {
            _estado.update { it.copy(erroDeTela = R.string.ajustes_erro_salvar) }
            return
        }
        gravado = antes?.copy(nome = valido.nome, baselineKm = valido.baselineKm)
        _estado.update {
            it.copy(
                salvandoPerfil = false,
                nome = valido.nome,
                baseline = textoDaBaseline(valido.baselineKm),
                erros = ErrosDoPerfil(),
                erroDeTela = null,
                perfilSalvo = true,
            ).comPodeSalvar()
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
