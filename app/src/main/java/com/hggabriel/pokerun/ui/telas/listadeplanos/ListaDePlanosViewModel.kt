package com.hggabriel.pokerun.ui.telas.listadeplanos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Quanto tempo o estado sobrevive a uma rotação antes de os listeners caírem. */
private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * O motor da lista de planos (`F1-T12`, docs/03 §3.4).
 *
 * ### A lista vem do documento do usuário, e não de uma consulta
 *
 * `plans` é `allow list: if false` (RN-17), então não existe consulta que responda "de
 * quais planos eu participo". O caminho é `users/{uid}.planos` → N leituras diretas por
 * ID (docs/05 §2.7), que é o que [PlanoRepositorio.observarVarios] faz. O
 * `flatMapLatest` refaz as assinaturas quando o array muda: entrar num plano por convite
 * noutro aparelho acrescenta a linha aqui sozinho.
 *
 * ### A troca do plano ativo passa obrigatoriamente pelo diálogo
 *
 * RN-13: a troca é decisão explícita, com confirmação. [pedirConfirmacao] só abre o
 * diálogo, e é [confirmarTroca] quem escreve — não existe caminho do toque à escrita que
 * não passe pelos dois. O botão nem aparece fora de plano dormente
 * ([ItemDePlano.podeTornarAtivo]).
 *
 * ### Por que o diálogo fecha antes de o servidor responder
 *
 * `definirPlanoAtivo` é um `update` comum, e escrita do Firestore **resolve na
 * confirmação do servidor** — offline ela fica pendurada em vez de falhar (decisão nº 4
 * do `STATUS.md`, docs/05 §2.6). Prender o diálogo ao retorno travaria a tela num
 * *"trocando…"* eterno no metrô, que é a mesma armadilha anotada na ficha de `F1-T16`.
 *
 * O que o usuário vê é verdade mesmo assim: o cache local emite o `plano_ativo_id` novo
 * na hora, então a marca muda de linha na frente dele. Se a escrita falhar de verdade —
 * regra negada, conflito —, o cache desfaz e a mensagem aparece no lugar do sucesso.
 */
class ListaDePlanosViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
) : ViewModel() {

    /**
     * O que é da tela e não do servidor: o diálogo aberto e o erro da última troca.
     *
     * Mora fora da cadeia de listeners porque uma emissão do Firestore não pode fechar
     * um diálogo que o usuário abriu — e é o que aconteceria se o estado inteiro fosse
     * derivado do `Flow`.
     */
    private data class EstadoLocal(
        val confirmando: ItemDePlano? = null,
        @param:StringRes val erro: Int? = null,
    )

    private val local = MutableStateFlow(EstadoLocal())

    /**
     * O contador de tentativas. Mexer nele reassina a cadeia inteira, que é o que o
     * botão de repetir de docs/02 §8, item 7 precisa fazer — o `catch` de um `Flow`
     * encerra a coleta, e sem uma nova assinatura o erro seria permanente.
     */
    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<ListaDePlanosUiState> = combine(
        tentativas.flatMapLatest { remoto() }.catch { emit(ListaDePlanosUiState.Falhou) },
        local,
    ) { remoto, local ->
        if (remoto is ListaDePlanosUiState.Lista) {
            remoto.copy(confirmando = local.confirmando, erro = local.erro)
        } else {
            remoto
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
        initialValue = ListaDePlanosUiState.Carregando,
    )

    /** Reassina os listeners depois de uma falha de leitura. */
    fun tentarDeNovo() {
        local.update { it.copy(erro = null) }
        tentativas.update { it + 1 }
    }

    // -----------------------------------------------------------------------
    // RN-13 — a troca do plano ativo
    // -----------------------------------------------------------------------

    /**
     * // RN-13
     *
     * Abre a confirmação. **Não escreve nada**, e a guarda de [ItemDePlano.podeTornarAtivo]
     * repete aqui o que a tela já faz ao esconder o botão: a regra não pode depender de
     * a tela lembrar dela.
     */
    fun pedirConfirmacao(item: ItemDePlano) {
        if (!item.podeTornarAtivo) return
        local.update { it.copy(confirmando = item, erro = null) }
    }

    fun cancelarTroca() = local.update { it.copy(confirmando = null) }

    /**
     * // RN-13
     *
     * O único lugar do app que troca o plano ativo depois do cadastro, e ele só é
     * alcançável pelo diálogo. Ver o KDoc da classe para o porquê de o diálogo fechar
     * antes de o servidor responder.
     */
    fun confirmarTroca() {
        val alvo = local.value.confirmando ?: return
        val uid = autenticacao.uidAtual

        if (uid == null) {
            local.update { it.copy(confirmando = null, erro = R.string.planos_erro_trocar) }
            return
        }

        local.update { EstadoLocal() }

        viewModelScope.launch {
            try {
                usuarios.definirPlanoAtivo(uid, alvo.id)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                local.update { it.copy(erro = R.string.planos_erro_trocar) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<ListaDePlanosUiState> {
        // Sem sessão não há lista, e esta tela é modal: quem devolve ao cadastro é a
        // Home embaixo dela, que tem o estado `SemPerfil`. Aqui isso é rede de segurança.
        val uid = autenticacao.uidAtual ?: return flowOf(ListaDePlanosUiState.Falhou)

        return usuarios.observar(uid).flatMapLatest { usuario ->
            when {
                usuario == null -> flowOf(ListaDePlanosUiState.Falhou)
                usuario.planos.isEmpty() -> flowOf(ListaDePlanosUiState.Vazio)
                else -> planos.observarVarios(usuario.planos).map { lidos ->
                    val itens = itensDePlano(lidos, usuario.planoAtivoId)
                    // `observarVarios` deixa cair o ID que não resolve, em vez de
                    // derrubar a lista inteira. Se nenhum resolver, o estado é o vazio.
                    if (itens.isEmpty()) ListaDePlanosUiState.Vazio
                    else ListaDePlanosUiState.Lista(itens)
                }
            }
        }
    }
}
