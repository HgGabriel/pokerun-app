package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.editarLongao
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Quanto tempo o estado sobrevive a uma rotação antes de os listeners caírem. */
private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * O motor do detalhe do plano (`F1-T13`, docs/03 §3.7).
 *
 * ### Cinco listeners, e o estado sai da combinação deles
 *
 * `users/{uid}` diz qual é o plano ativo (RN-12); `plans/{id}`, `weeks` e `members` são o
 * plano; `runs` filtradas pelo plano dão a aderência acumulada do cabeçalho. Quem
 * transforma isso em estado é [detalheDoPlano], que é função pura e tem teste — aqui só
 * mora a costura e as duas escritas.
 *
 * ### As duas escritas do dono, e por que nenhuma prende a tela
 *
 * `atualizarLongao` e `encerrar` são `update` comuns, e escrita do Firestore **resolve na
 * confirmação do servidor** — offline ela fica pendurada em vez de falhar (decisão nº 4
 * do `STATUS.md`, docs/05 §2.6). Como na `PlansListScreen` (decisão nº 36), o diálogo
 * fecha na hora: o cache local emite o valor novo imediatamente, então a linha da grade
 * já muda na frente do dono, e uma falha de verdade — a rule negando uma semana que
 * congelou no meio da edição — aparece como mensagem.
 *
 * ### A derivação do volume não é refeita aqui
 *
 * `confirmarEdicao` chama [editarLongao], a mesma função da revisão do rascunho, e grava
 * o `km_alvo` que ela devolve. Recalcular o volume aqui abriria a porta para as duas
 * telas divergirem — a fórmula é de docs/01 §3.2 e mora num lugar só.
 */
class DetalhePlanoViewModel(
    private val planoId: String,
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
) : ViewModel() {

    /** O que é da tela e não do servidor: os dois diálogos e o erro da última escrita. */
    private data class EstadoLocal(
        val editando: EdicaoDoLongao? = null,
        val confirmandoEncerrar: Boolean = false,
        @param:StringRes val erro: Int? = null,
    )

    private val local = MutableStateFlow(EstadoLocal())

    /** Reassinar a cadeia é o que o botão de repetir de docs/02 §8, item 7 precisa fazer. */
    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalhePlanoUiState> = combine(
        tentativas.flatMapLatest { remoto() }.catch { emit(DetalhePlanoUiState.Falhou) },
        local,
    ) { remoto, local ->
        if (remoto is DetalhePlanoUiState.Conteudo) {
            remoto.copy(
                editando = local.editando,
                confirmandoEncerrar = local.confirmandoEncerrar,
                erro = local.erro,
            )
        } else {
            remoto
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
        initialValue = DetalhePlanoUiState.Carregando,
    )

    fun tentarDeNovo() {
        local.update { it.copy(erro = null) }
        tentativas.update { it + 1 }
    }

    // -----------------------------------------------------------------------
    // RN-05, RN-06 — a edição do longão
    // -----------------------------------------------------------------------

    /**
     * // RN-06
     *
     * Abre o diálogo. A guarda repete aqui o que a tela já faz ao não oferecer o toque:
     * a permissão não pode depender de a tela lembrar dela.
     */
    fun abrirEdicao(semana: Semana) {
        val conteudo = conteudo() ?: return
        if (!conteudo.podeEditar(semana)) return
        val longao = semana.longaoKm ?: return

        local.update {
            it.copy(editando = EdicaoDoLongao(semana.numero, formatarKm(longao)), erro = null)
        }
    }

    fun longaoMudou(texto: String) = local.update { estado ->
        // Digitar limpa o erro: o usuário está consertando.
        estado.copy(editando = estado.editando?.copy(texto = texto, erro = null))
    }

    fun cancelarEdicao() = local.update { it.copy(editando = null) }

    /**
     * // RN-05
     *
     * Grava o longão e o volume derivado. A permissão é conferida **de novo no momento da
     * escrita**, e não é preciosismo: o diálogo pode ter ficado aberto atravessando a
     * meia-noite de domingo, e aí a semana congelou embaixo dele. A rule faz a mesma conta
     * com `request.time` e negaria; melhor não pedir.
     */
    fun confirmarEdicao() {
        val edicao = local.value.editando ?: return
        val conteudo = conteudo() ?: return
        val semana = conteudo.semanas.firstOrNull { it.numero == edicao.numero } ?: return

        if (!conteudo.podeEditar(semana)) {
            local.update { it.copy(editando = null, erro = R.string.detalhe_erro_congelada) }
            return
        }

        val km = distanciaEmKm(edicao.texto)
        if (km == null) {
            local.update { it.copy(editando = edicao.copy(erro = R.string.criar_erro_distancia)) }
            return
        }

        // A derivação é a de `F1-T11`, e vem da mesma função: um campo manda, o outro sai
        // dele pela fórmula de docs/01 §3.2.
        val atualizada = editarLongao(conteudo.semanas, edicao.numero, km)
            .first { it.numero == edicao.numero }

        local.update { EstadoLocal() }

        viewModelScope.launch {
            try {
                planos.atualizarLongao(
                    planoId = planoId,
                    numero = atualizada.numero,
                    longaoKm = atualizada.longaoKm,
                    kmAlvo = atualizada.kmAlvo,
                )
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                local.update { it.copy(erro = R.string.detalhe_erro_editar) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // RN-27 — o encerramento
    // -----------------------------------------------------------------------

    fun pedirEncerrar() {
        val conteudo = conteudo() ?: return
        if (!conteudo.podeEncerrar) return
        local.update { it.copy(confirmandoEncerrar = true, erro = null) }
    }

    fun cancelarEncerrar() = local.update { it.copy(confirmandoEncerrar = false) }

    /**
     * // RN-27
     *
     * Encerrar é do dono, com confirmação, e **não reabre**. A própria rule de `plans`
     * exige `encerrado == false` para aceitar a escrita: a que encerra é a última que o
     * documento aceita, e uma segunda tentativa seria negada pelo servidor.
     */
    fun confirmarEncerrar() {
        val conteudo = conteudo() ?: return
        if (!conteudo.podeEncerrar) return

        local.update { EstadoLocal() }

        viewModelScope.launch {
            try {
                planos.encerrar(planoId)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                local.update { it.copy(erro = R.string.detalhe_erro_encerrar) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    private fun conteudo(): DetalhePlanoUiState.Conteudo? =
        estado.value as? DetalhePlanoUiState.Conteudo

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<DetalhePlanoUiState> {
        // Sem sessão não há detalhe, e esta tela é modal: quem devolve ao cadastro é a
        // Home embaixo dela. Aqui isso é rede de segurança.
        val uid = autenticacao.uidAtual ?: return flowOf(DetalhePlanoUiState.Falhou)

        return combine(
            usuarios.observar(uid),
            planos.observar(planoId),
            planos.observarSemanas(planoId),
            planos.observarMembros(planoId),
            corridas.observarDoPlano(uid, planoId),
        ) { usuario, plano, grade, membros, corridasDoPlano ->
            detalheDoPlano(
                plano = plano,
                grade = grade,
                membros = membros,
                corridas = corridasDoPlano,
                uid = uid,
                planoAtivoId = usuario?.planoAtivoId,
                agora = Instant.now(),
            )
        }
    }
}
