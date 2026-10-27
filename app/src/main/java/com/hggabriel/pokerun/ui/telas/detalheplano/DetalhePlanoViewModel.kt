package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.ResultadoDaEdicao
import com.hggabriel.pokerun.dominio.regras.editarLongao
import com.hggabriel.pokerun.dominio.regras.Parceiro
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.dominio.regras.tetoDoLongao
import com.hggabriel.pokerun.dominio.regras.trocarCurta
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/** Quanto tempo o estado sobrevive a uma rotação antes de os listeners caírem. */
private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * O motor do detalhe do plano (`F1-T13`, r2 de 26/10/2026, docs/03 §3.7).
 *
 * ### Cinco listeners, uma leitura avulsa, e o estado sai da combinação deles
 *
 * `users/{uid}` diz qual é o plano ativo (RN-12); `plans/{id}`, `weeks` e `members` são o
 * plano; `runs` filtradas pelo plano dão os seus carimbos e a sua barra no gráfico. O
 * `weekly` da outra pessoa é **leitura avulsa**, uma por semana da janela dela, uma vez
 * por abertura e a cada `Tentar de novo`, como o pulso do grupo: a ficha do ranking
 * proíbe listener contínuo sobre `weekly` (docs/03 §3.16). Quem transforma isso em estado
 * é [detalheDoPlano], que é função pura e tem teste — aqui só mora a costura e as escritas.
 *
 * **A leitura do grupo que falha não derruba a tela** (S3): a linha da outra pessoa fica
 * só com avatar e nome, e o traço dela some do gráfico. O mesmo vale para o avatar.
 *
 * ### As três escritas do dono, e por que nenhuma prende a tela
 *
 * `atualizarSemanas`, `atualizarCurta` e `encerrar` são remessas comuns, e escrita do
 * Firestore **resolve na confirmação do servidor** — offline ela fica pendurada em vez de
 * falhar (decisão nº 4 do `STATUS.md`, docs/05 §2.6). Como na `PlansListScreen`
 * (decisão nº 36), o diálogo fecha na hora: o cache local emite o valor novo
 * imediatamente, e uma falha de verdade — a rule negando uma semana que congelou no meio
 * da edição — aparece como mensagem. Esta tela **não grava nada novo**: a r2 só
 * acrescentou leituras.
 *
 * ### A conta não é refeita aqui
 *
 * `confirmarEdicao` chama [editarLongao] e `confirmarCurta` chama [trocarCurta], as
 * mesmas funções da revisão do rascunho e da migração, e grava as semanas que elas
 * devolvem. A regra é de docs/01 §3.2 e mora num lugar só.
 */
class DetalhePlanoViewModel(
    private val planoId: String,
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    /** O que é da tela e não do servidor: a semana escolhida, os diálogos, a última edição e o último erro. */
    private data class EstadoLocal(
        /** A semana que o usuário escolheu pelas setas ou pela régua. Nula: a padrão (R4). */
        val semanaEscolhida: Int? = null,
        val editando: EdicaoDoLongao? = null,
        val editandoCurta: EdicaoDaCurta? = null,
        val tambemMudaram: List<MudancaDeLongao> = emptyList(),
        val confirmandoEncerrar: Boolean = false,
        @param:StringRes val erro: Int? = null,
    )

    /** O que se leu do grupo: o `weekly` de cada outro membro, e o avatar de cada um. */
    private data class Grupo(
        val feitos: Map<String, Map<Int, FeitoNaSemana>> = emptyMap(),
        val avatares: Map<String, AvatarDoMembro> = emptyMap(),
    )

    /** Os cinco listeners, crus, antes da função pura. */
    private data class Entrada(
        val uid: String,
        val usuario: Usuario?,
        val plano: Plano?,
        val grade: List<Semana>,
        val membros: List<Membro>,
        val corridas: List<Corrida>,
    )

    private val local = MutableStateFlow(EstadoLocal())
    private val grupo = MutableStateFlow(Grupo())

    /** Reassinar a cadeia é o que o botão de repetir de docs/02 §8, item 7 precisa fazer. */
    private val tentativas = MutableStateFlow(0)

    private var chaveLida: ChaveDoGrupo? = null
    private var avataresLidos: List<Pair<String, Triple<Int?, Int?, String?>>>? = null
    private var leituraDoGrupo: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalhePlanoUiState> = combine(
        // O `catch` fica dentro: a falha encerra só esta assinatura, e a próxima
        // tentativa reassina a cadeia inteira.
        tentativas.flatMapLatest { tentativa -> remoto(tentativa).catch { emit(null) } },
        grupo,
        local,
    ) { entrada, grupo, local ->
        if (entrada == null) return@combine DetalhePlanoUiState.Falhou
        val remoto = detalheDoPlano(
            plano = entrada.plano,
            grade = entrada.grade,
            membros = entrada.membros,
            corridas = entrada.corridas,
            uid = entrada.uid,
            planoAtivoId = entrada.usuario?.planoAtivoId,
            agora = relogio.instant(),
            feitosDoGrupo = grupo.feitos,
        )
        if (remoto is DetalhePlanoUiState.Conteudo) {
            remoto.copy(
                semanaSelecionada = local.semanaEscolhida?.let { semanaNaGrade(it, remoto.semanas) }
                    ?: remoto.semanaSelecionada,
                avatares = grupo.avatares,
                editando = local.editando,
                editandoCurta = local.editandoCurta,
                tambemMudaram = local.tambemMudaram,
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
    // R4 — a semana selecionada
    // -----------------------------------------------------------------------

    /** A régua: a semana tocada ou arrastada. Trocar de semana não lê nada novo. */
    fun selecionarSemana(numero: Int) {
        val conteudo = conteudo() ?: return
        local.update { it.copy(semanaEscolhida = semanaNaGrade(numero, conteudo.semanas)) }
    }

    fun semanaAnterior() {
        val conteudo = conteudo() ?: return
        selecionarSemana(conteudo.semanaSelecionada - 1)
    }

    fun proximaSemana() {
        val conteudo = conteudo() ?: return
        selecionarSemana(conteudo.semanaSelecionada + 1)
    }

    // -----------------------------------------------------------------------
    // RN-05, RN-06, D7 — a edição do longão
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
            it.copy(
                editando = EdicaoDoLongao(
                    numero = semana.numero,
                    texto = formatarKm(longao),
                    tetoKm = tetoDoLongao(conteudo.parametros, conteudo.semanas, semana.numero),
                    editada = semana.longaoPedidoKm != null,
                ),
                erro = null,
            )
        }
    }

    fun longaoMudou(texto: String) = local.update { estado ->
        // Digitar limpa o erro: o usuário está consertando.
        estado.copy(editando = estado.editando?.copy(texto = texto, erro = null))
    }

    fun cancelarEdicao() = local.update { it.copy(editando = null) }

    /**
     * // RN-05, D7
     *
     * Grava o longão pedido e as semanas que ele mexe (docs/01 §3.3). A permissão é
     * conferida **de novo no momento da escrita**, e não é preciosismo: o diálogo pode ter
     * ficado aberto atravessando a meia-noite de domingo, e aí a semana congelou embaixo
     * dele; ou alguém do grupo correu nela enquanto ele estava aberto. A rule faz a conta
     * do congelamento com `request.time` e negaria; melhor não pedir.
     */
    fun confirmarEdicao() {
        val edicao = local.value.editando ?: return
        val km = distanciaEmKm(edicao.texto)
        if (km == null) {
            local.update { it.copy(editando = edicao.copy(erro = R.string.criar_erro_distancia)) }
            return
        }
        aplicar(edicao, km)
    }

    /** Apaga o pedido da semana: ela volta à rampa, e as seguintes voltam junto. */
    fun desfazerEdicao() {
        val edicao = local.value.editando ?: return
        aplicar(edicao, null)
    }

    private fun aplicar(edicao: EdicaoDoLongao, km: Double?) {
        val conteudo = conteudo() ?: return
        val semana = conteudo.semanas.firstOrNull { it.numero == edicao.numero } ?: return

        if (!conteudo.podeEditar(semana)) {
            local.update { it.copy(editando = null, erro = R.string.detalhe_erro_congelada) }
            return
        }

        when (val r = editarLongao(conteudo.parametros, conteudo.semanas, edicao.numero, km, conteudo.congeladas)) {
            is ResultadoDaEdicao.AcimaDoTeto -> local.update {
                it.copy(editando = edicao.copy(tetoKm = r.tetoKm, erro = R.string.detalhe_editar_erro_teto))
            }
            ResultadoDaEdicao.NaoEditavel -> local.update { it.copy(editando = null) }
            is ResultadoDaEdicao.Aplicada -> {
                local.update { EstadoLocal(semanaEscolhida = it.semanaEscolhida, tambemMudaram = r.tambemMudam) }
                gravar(R.string.detalhe_erro_editar) {
                    planos.atualizarSemanas(planoId, mudadas(conteudo.semanas, r.grade))
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // RN-06 — a curta do plano (docs/01 §3.3)
    // -----------------------------------------------------------------------

    fun abrirCurta() {
        val conteudo = conteudo() ?: return
        if (!conteudo.podeMudarCurta) return
        local.update {
            it.copy(editandoCurta = EdicaoDaCurta(formatarKm(conteudo.parametros.curtaKm)), erro = null)
        }
    }

    fun curtaMudou(texto: String) = local.update { estado ->
        estado.copy(editandoCurta = estado.editandoCurta?.copy(texto = texto, erro = null))
    }

    fun cancelarCurta() = local.update { it.copy(editandoCurta = null) }

    /**
     * Troca a curta do plano **a partir da semana seguinte**: as semanas já começadas
     * ficam, e nenhuma corrida já feita perde o multiplicador. O longão não muda; o volume
     * das semanas futuras se refaz.
     */
    fun confirmarCurta() {
        val edicao = local.value.editandoCurta ?: return
        val conteudo = conteudo() ?: return
        if (!conteudo.podeMudarCurta) {
            local.update { it.copy(editandoCurta = null) }
            return
        }
        val km = distanciaEmKm(edicao.texto)
        if (km == null) {
            local.update { it.copy(editandoCurta = edicao.copy(erro = R.string.criar_erro_distancia)) }
            return
        }

        val nova = trocarCurta(conteudo.parametros, conteudo.semanas, km, conteudo.comecadas)
        local.update { EstadoLocal(semanaEscolhida = it.semanaEscolhida) }
        gravar(R.string.detalhe_erro_curta) {
            planos.atualizarCurta(planoId, km, mudadas(conteudo.semanas, nova))
        }
    }

    /** Só as semanas que mudaram vão para a remessa: as congeladas a rule recusaria. */
    private fun mudadas(antes: List<Semana>, depois: List<Semana>): List<Semana> =
        depois.filter { it != antes[it.numero - 1] }

    private fun gravar(@StringRes erroAoFalhar: Int, escrita: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                escrita()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                local.update { it.copy(erro = erroAoFalhar) }
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

        // A semana escolhida sai junto: o encerrado abre na última (R4).
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

    /**
     * Os cinco listeners. Nulo é falta de sessão ou leitura que falhou, e vira
     * [DetalhePlanoUiState.Falhou].
     */
    private fun remoto(tentativa: Int): Flow<Entrada?> {
        // Sem sessão não há detalhe, e esta tela é modal: quem devolve ao cadastro é a
        // Home embaixo dela. Aqui isso é rede de segurança.
        val uid = autenticacao.uidAtual ?: return flowOf(null)

        return combine(
            usuarios.observar(uid),
            planos.observar(planoId),
            planos.observarSemanas(planoId),
            planos.observarMembros(planoId),
            corridas.observarDoPlano(uid, planoId),
        ) { usuario, plano, grade, membros, corridasDoPlano ->
            Entrada(uid, usuario, plano, grade, membros, corridasDoPlano)
        }.onEach { lerGrupoSePreciso(it, tentativa) }
    }

    /**
     * Lê o `weekly` da outra pessoa quando a janela muda: na abertura, quando alguém entra,
     * quando a semana vira e a cada `Tentar de novo`. Uma corrida sua não relê nada.
     */
    private fun lerGrupoSePreciso(entrada: Entrada, tentativa: Int) {
        val base = detalheDoPlano(
            entrada.plano, entrada.grade, entrada.membros, entrada.corridas, entrada.uid,
            entrada.usuario?.planoAtivoId, relogio.instant(),
        ) as? DetalhePlanoUiState.Conteudo ?: return

        val chave = ChaveDoGrupo(semanasALer(base.membros, entrada.uid, base.ateSemana), tentativa)
        if (chave != chaveLida) {
            chaveLida = chave
            leituraDoGrupo?.cancel()
            leituraDoGrupo = viewModelScope.launch { grupo.update { it.copy(feitos = lerFeitos(chave)) } }
        }

        val quem = base.membros.map { it.uid to Triple(it.avatarEspecie, it.parceiro, it.treinador) }
        if (quem != avataresLidos) {
            avataresLidos = quem
            viewModelScope.launch { lerAvatares(base.membros) }
        }
    }

    /** Membro cuja leitura falhou sai do mapa: a linha dele fica sem dados (S3). */
    private suspend fun lerFeitos(chave: ChaveDoGrupo): Map<String, Map<Int, FeitoNaSemana>> = coroutineScope {
        chave.janelas.map { (uid, janela) -> async { uid to lerSemanais(uid, janela) } }
            .awaitAll()
            .mapNotNull { (uid, feitos) -> feitos?.let { uid to it } }
            .toMap()
    }

    private suspend fun lerSemanais(uid: String, janela: IntRange): Map<Int, FeitoNaSemana>? = try {
        coroutineScope {
            janela.map { n -> async { n to progresso.semanal(uid, Replay.chaveDaSemana(planoId, n)) } }
                .awaitAll()
                .toMap()
                .let(::feitosDosSemanais)
        }
    } catch (cancelamento: CancellationException) {
        throw cancelamento
    } catch (_: Exception) {
        null
    }

    /**
     * O avatar de cada membro: o sprite de jogo da espécie dele (docs/03 §3.16.1) sobre
     * `tipo-*-suave`. O tipo vem da PokéAPI e fica guardado depois da primeira leitura;
     * sem ele, o fundo é `superficie-afundada`. Falhou, o slot fica sem sprite.
     */
    private suspend fun lerAvatares(membros: List<Membro>) {
        try {
            val temporada = temporadaDe(relogio.instant(), temporadas.todas()) ?: return
            val avatares = coroutineScope {
                membros.map { membro ->
                    async {
                        val numero = Parceiro.doMembro(membro.parceiro, membro.avatarEspecie, temporada)
                        val tipo = runCatching { especies.tipos(numero).firstOrNull() }
                            .onFailure { if (it is CancellationException) throw it }
                            .getOrNull()
                        membro.uid to AvatarDoMembro(especies.spriteDeJogo(numero), tipo)
                    }
                }.awaitAll().toMap()
            }
            grupo.update { it.copy(avatares = avatares) }
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            // Sem avatar a linha mostra o slot vazio; a próxima abertura tenta de novo.
            avataresLidos = null
        }
    }
}
