package com.hggabriel.pokerun.ui.telas.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.DesafioRepositorio
import com.hggabriel.pokerun.dados.firestore.MudancaDoPerfil
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.local.PreferenciasDoAparelho
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.dominio.regras.Insignias
import com.hggabriel.pokerun.dominio.regras.Parceiro
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.launch

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** Quanto tempo o botão diz "Alterações salvas" depois do toque. */
private const val RESPOSTA_DO_SALVAR_MS = 2_000L

/** O fuso da temporada, que é o do ano civil de `kanto-2026` (nº 100). */
private val FUSO_DA_TEMPORADA: ZoneId = ZoneId.of("America/Sao_Paulo")

/** O que o servidor e o cache dizem, antes da edição local. */
private data class Lido(
    val usuario: Usuario?,
    val corridas: List<Corrida>,
    val colecoes: Map<String, Int>,
    val catalogo: List<Temporada>,
    val plano: Plano?,
    val parceiro: ParceiroDaCredencial?,
    val insignias: List<String> = emptyList(),
)

/** O que só esta tela sabe: a folha da baseline, os diálogos e a resposta do salvar. */
private data class Local(
    /** Liga ao salvar e desliga sozinho: o botão diz "Alterações salvas" por um instante. */
    val acabouDeSalvar: Boolean = false,
    val editandoBaseline: Boolean = false,
    val baselineDigitada: String = "",
    val erroDaBaseline: Int? = null,
    val confirmandoSaida: Boolean = false,
    val saiuDaConta: Boolean = false,
)

/**
 * O Perfil (estudo de Progresso e Perfil, decisões 5, 7, 12 a 15 e 22 a 24).
 *
 * ### Observa, e não lê uma vez
 *
 * Os Ajustes liam `users/{uid}` uma vez, para a escrita não voltar como emissão nova debaixo
 * do dedo. Aqui a edição mora em [Local] e só ela alimenta o campo enquanto existe: a
 * emissão do servidor pode chegar à vontade, e a troca de parceiro feita no seletor chega
 * sozinha quando a pessoa volta.
 *
 * ### Nome, parceiro e treinador esperam o "Salvar alterações"
 *
 * Desde 27/10 a edição deles mora no [RascunhoDoPerfil], e o usuário lido chega com o
 * rascunho por cima: a credencial já mostra o que vai ser gravado. O botão grava o lote de
 * `UsuarioRepositorio.atualizarPerfil`, que entra na fila local do Firestore e não espera o
 * servidor. A baseline continua salvando ao confirmar a folha.
 */
class PerfilViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val corridas: CorridaRepositorio,
    private val planos: PlanoRepositorio,
    private val especies: EspeciesRepositorio,
    private val preferencias: PreferenciasDoAparelho,
    private val rascunho: RascunhoDoPerfil,
    private val desafios: DesafioRepositorio? = null,
    private val relogio: Clock = Clock.system(FUSO_DA_TEMPORADA),
) : ViewModel() {

    private val local = MutableStateFlow(Local())
    private var ultimoLido: Lido? = null
    private var parceiroVisto: Int? = null
    private var trocas = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lido: Flow<Lido?> = run {
        val uid = autenticacao.uidAtual ?: return@run flowOf(null)
        val catalogo = flow { emit(temporadas.todas()) }
        combine(
            rascunho.sobre(usuarios.observar(uid), comNome = false),
            corridas.observarHistorico(uid),
            progresso.observarColecoes(uid),
            catalogo,
        ) { usuario, lista, colecoes, todas -> Lido(usuario, lista, colecoes, todas, null, null) }
            .flatMapLatest { base ->
                val planoId = base.usuario?.planoAtivoId
                (if (planoId == null) flowOf(null) else planos.observar(planoId)).map { base.copy(plano = it) }
            }
            .flatMapLatest { base -> comParceiro(base) }
            .mapLatest { base -> base.copy(insignias = insigniasDaVida(base.usuario?.planos.orEmpty())) }
    }

    val estado: StateFlow<PerfilUiState> = combine(lido, local, preferencias.somNaCaptura, rascunho.mudanca) { lido, local, som, mudanca ->
        montar(lido, local, som, mudanca.nome)
    }
        .catch { emit(PerfilUiState(carregando = false, erroDeTela = R.string.ajustes_erro_ler)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = PerfilUiState(),
        )

    /**
     * Uma leitura de `plans/{id}/desafios` por plano da pessoa (hoje, 1 ou 2). Plano que não
     * se lê (apagado, ou sem a membresia) fica de fora, e a falha de todos deixa o cordão sem
     * broche: é enfeite, e não derruba a tela.
     */
    private suspend fun insigniasDaVida(planos: List<String>): List<String> {
        val repositorio = desafios ?: return emptyList()
        val lidos = planos.distinct().mapNotNull { planoId ->
            try {
                repositorio.buscarTodos(planoId).values
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                null
            }
        }
        return Insignias.daVida(lidos)
    }

    /** O número aparece já; nome e tipos, quando a PokéAPI ou a cópia guardada responder. */
    private fun comParceiro(base: Lido): Flow<Lido> = flow {
        val temporada = temporadaDe(relogio.instant(), base.catalogo)
        val numero = temporada?.let {
            Parceiro.efetivo(base.usuario?.parceiro, base.colecoes[it.id], it)
        } ?: base.usuario?.parceiro
        if (numero == null) {
            emit(base)
            return@flow
        }
        val semNome = ParceiroDaCredencial(numero, null, emptyList(), especies.spriteDoHome(numero), especies.sprite(numero))
        emit(base.copy(parceiro = semNome))
        emit(base.copy(parceiro = semNome.copy(nome = especies.nome(numero), tipos = especies.tipos(numero))))
    }

    private fun montar(lido: Lido?, local: Local, som: Boolean, nomeEmEdicao: String?): PerfilUiState {
        ultimoLido = lido
        if (lido?.usuario == null) {
            return PerfilUiState(
                carregando = lido == null,
                somNaCaptura = som,
                confirmandoSaida = local.confirmandoSaida,
                saiuDaConta = local.saiuDaConta,
            )
        }
        val usuario = lido.usuario
        val temporada = temporadaDe(relogio.instant(), lido.catalogo)
        val numero = lido.parceiro?.numero
        if (numero != null && numero != parceiroVisto) {
            if (parceiroVisto != null) trocas++
            parceiroVisto = numero
        }
        return PerfilUiState(
            carregando = false,
            nome = nomeEmEdicao ?: usuario.nome,
            treinador = usuario.treinador,
            parceiro = lido.parceiro,
            insignias = lido.insignias,
            credencial = dadosDaCredencial(
                corridas = lido.corridas,
                temporada = temporada,
                colecaoCorrente = temporada?.let { lido.colecoes[it.id] },
                planoAtivo = lido.plano,
                fuso = FUSO_DA_TEMPORADA,
            ),
            baselineKm = usuario.baselineKm,
            somNaCaptura = som,
            editandoBaseline = local.editandoBaseline,
            baselineDigitada = local.baselineDigitada,
            erroDaBaseline = local.erroDaBaseline,
            confirmandoSaida = local.confirmandoSaida,
            saiuDaConta = local.saiuDaConta,
            alteracoesPendentes = rascunho.pendente,
            acabouDeSalvar = local.acabouDeSalvar && !rascunho.pendente,
            trocasDeParceiro = trocas,
            albunsEncerrados = lido.catalogo
                .filter { it.fim <= relogio.instant() }
                .sortedByDescending { it.inicio }
                .map { it.id to it.nome },
        )
    }

    // -----------------------------------------------------------------------
    // O nome, editado no lugar
    // -----------------------------------------------------------------------

    /** Cada letra vai ao rascunho: trocar de aba no meio da digitação já pergunta antes. */
    fun nomeMudou(texto: String) = rascunho.definirNome(texto)

    // -----------------------------------------------------------------------
    // Salvar ou descartar o rascunho
    // -----------------------------------------------------------------------

    /**
     * Grava nome, parceiro e treinador num lote só, que também chega a `members/` de todo
     * plano. Nome em branco não é gravado: o campo volta ao nome que já estava.
     */
    fun salvarAlteracoes() {
        try {
            if (!rascunho.salvar()) return
        } catch (erro: Exception) {
            // Falha síncrona de montar o lote. O rascunho já foi esvaziado e o campo volta ao
            // valor gravado na próxima emissão.
            return
        }
        local.update { it.copy(acabouDeSalvar = true) }
        viewModelScope.launch {
            delay(RESPOSTA_DO_SALVAR_MS)
            local.update { it.copy(acabouDeSalvar = false) }
        }
    }

    fun descartarAlteracoes() = rascunho.descartar()

    // -----------------------------------------------------------------------
    // A baseline, numa folha com o placar
    // -----------------------------------------------------------------------

    fun abrirBaseline() {
        val km = ultimoLido?.usuario?.baselineKm ?: return
        local.update { it.copy(editandoBaseline = true, baselineDigitada = textoDaBaseline(km), erroDaBaseline = null) }
    }

    fun baselineMudou(texto: String) = local.update { it.copy(baselineDigitada = texto, erroDaBaseline = null) }

    fun fecharBaseline() = local.update { it.copy(editandoBaseline = false, erroDaBaseline = null) }

    /** A mesma validação do cadastro: número positivo, com vírgula ou ponto. */
    fun confirmarBaseline() {
        val km = distanciaEmKm(local.value.baselineDigitada)
        if (km == null) {
            local.update { it.copy(erroDaBaseline = R.string.ajustes_erro_baseline) }
            return
        }
        local.update { it.copy(editandoBaseline = false) }
        val usuario = ultimoLido?.usuario ?: return
        if (textoDaBaseline(km) != textoDaBaseline(usuario.baselineKm)) gravar(usuario, MudancaDoPerfil(baselineKm = km))
    }

    // -----------------------------------------------------------------------
    // App e conta
    // -----------------------------------------------------------------------

    fun somNaCapturaMudou(ligado: Boolean) = preferencias.definirSomNaCaptura(ligado)

    fun pedirParaSair() = local.update { it.copy(confirmandoSaida = true) }

    fun desistirDeSair() = local.update { it.copy(confirmandoSaida = false) }

    /** `signOut` é local e síncrono: não há escrita a confirmar. */
    fun sair() {
        rascunho.descartar()
        autenticacao.sair()
        local.update { it.copy(confirmandoSaida = false, saiuDaConta = true) }
    }

    fun saidaConsumida() = local.update { it.copy(saiuDaConta = false) }

    private fun gravar(usuario: Usuario, mudanca: MudancaDoPerfil) {
        try {
            usuarios.atualizarPerfil(usuario.uid, usuario.planos, mudanca)
        } catch (erro: Exception) {
            // Falha síncrona de montar o lote. O valor gravado volta ao campo na próxima
            // emissão, e a próxima confirmação tenta de novo.
        }
    }
}
