package com.hggabriel.pokerun.ui.telas.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.local.TreinadoresRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.dominio.regras.Parceiro
import com.hggabriel.pokerun.dominio.regras.TierDeParceiros
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.ui.componentes.BuscaDeTreinadores
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

// ---------------------------------------------------------------------------
// O seletor de parceiro (estudo, d.2 regra 7)
// ---------------------------------------------------------------------------

data class SeletorDeParceiro(
    val carregando: Boolean = true,
    val grade: List<TierDeParceiros> = emptyList(),
    val escolhido: Int? = null,
    val nomeDoEscolhido: String? = null,
    val tipoDoEscolhido: TipoDePokemon? = null,
    val liberados: Int = 0,
    /** O tipo primário de cada espécie da grade, para o `tipo-suave` da célula. Chega aos poucos. */
    val tipos: Map<Int, TipoDePokemon> = emptyMap(),
    /** Sem escolha: o parceiro segue o último Pokémon da coleção ([padrao]). */
    val automatico: Boolean = false,
    /** O parceiro do modo automático: a espécie da `colecao_max` da temporada corrente. */
    val padrao: Int? = null,
)

/**
 * A grade por tier dos parceiros liberados. O toque na célula escolhe, o banner do topo troca
 * para o novo parceiro (decisão 11), e o "Salvar alterações" do Perfil grava (27/10).
 */
class EscolherParceiroViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val especies: EspeciesRepositorio,
    private val rascunho: RascunhoDoPerfil,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val tipos = MutableStateFlow<Map<Int, TipoDePokemon>>(emptyMap())
    private var usuario: Usuario? = null
    private var liberados: Set<Int> = emptySet()
    private var tiposPedidos = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<SeletorDeParceiro> = run {
        val uid = autenticacao.uidAtual ?: return@run flowOf(SeletorDeParceiro(carregando = false))
        combine(rascunho.sobre(usuarios.observar(uid)), progresso.observarColecoes(uid), flow { emit(temporadas.todas()) }) { u, colecoes, catalogo ->
            Triple(u, colecoes, catalogo)
        }.flatMapLatest { (u, colecoes, catalogo) ->
            usuario = u
            val grade = Parceiro.grade(colecoes, catalogo)
            liberados = grade.flatMap { it.especies }.toSet()
            pedirTipos(liberados)
            val temporada = temporadaDe(relogio.instant(), catalogo)
            val escolhido = temporada?.let { Parceiro.efetivo(u?.parceiro, colecoes[it.id], it) } ?: u?.parceiro
            val padrao = temporada?.let { Parceiro.padrao(colecoes[it.id], it) }
            combine(nomeETipo(escolhido), tipos) { (nome, tipo), mapa ->
                SeletorDeParceiro(false, grade, escolhido, nome, tipo, liberados.size, mapa, automatico = u?.parceiro == null, padrao = padrao)
            }
        }
    }
        .catch { emit(SeletorDeParceiro(carregando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS), SeletorDeParceiro())

    private fun nomeETipo(numero: Int?): Flow<Pair<String?, TipoDePokemon?>> =
        if (numero == null) flowOf<Pair<String?, TipoDePokemon?>>(null to null) else flow {
            emit(null to null)
            emit(especies.nome(numero) to especies.tipos(numero).firstOrNull())
        }

    private fun pedirTipos(numeros: Set<Int>) {
        if (tiposPedidos || numeros.isEmpty()) return
        tiposPedidos = true
        viewModelScope.launch {
            numeros.forEach { numero ->
                val tipo = try {
                    especies.tipos(numero).firstOrNull()
                } catch (cancelamento: CancellationException) {
                    throw cancelamento
                } catch (_: Exception) {
                    null
                }
                if (tipo != null) tipos.update { it + (numero to tipo) }
            }
        }
    }

    /** Regra 3: só um liberado é aceito. O toque vai ao rascunho; o Perfil salva. */
    fun escolher(numero: Int) {
        if (usuario == null || !Parceiro.aceita(numero, liberados)) return
        rascunho.definirParceiro(numero)
    }

    /** Apaga a escolha (29/10/2026): o parceiro volta a seguir a coleção. O Perfil salva. */
    fun voltarAoAutomatico() {
        if (usuario == null) return
        rascunho.voltarAoParceiroAutomatico()
    }
}

// ---------------------------------------------------------------------------
// O seletor de treinador (estudo, Revisão 3)
// ---------------------------------------------------------------------------

/** O banner do seletor. A busca e a grade vêm de [EscolherTreinadorViewModel.busca]. */
data class SeletorDeTreinador(
    val carregando: Boolean = true,
    val escolhido: String? = null,
    val parceiro: Int? = null,
    val nomeDoParceiro: String? = null,
)

/**
 * Os 1.500 treinadores do Showdown, com a busca no topo e os três grupos curados. O toque
 * escolhe e salva, como no seletor de parceiro. A busca é a [BuscaDeTreinadores] do
 * onboarding (29/10/2026): o campo não espera a busca, e a busca não roda na main.
 */
class EscolherTreinadorViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val especies: EspeciesRepositorio,
    private val treinadores: TreinadoresRepositorio,
    private val rascunho: RascunhoDoPerfil,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    val busca = BuscaDeTreinadores(viewModelScope, treinadores::lista)
    private var usuario: Usuario? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<SeletorDeTreinador> = run {
        val uid = autenticacao.uidAtual ?: return@run emptyFlow()
        combine(
            rascunho.sobre(usuarios.observar(uid)),
            progresso.observarColecoes(uid),
            flow { emit(temporadas.todas()) },
        ) { u, colecoes, catalogo ->
            usuario = u
            val temporada = temporadaDe(relogio.instant(), catalogo)
            temporada?.let { Parceiro.efetivo(u?.parceiro, colecoes[it.id], it) } ?: u?.parceiro
        }.flatMapLatest { numero ->
            val escolhido = usuario?.treinador
            if (numero == null) {
                flowOf(SeletorDeTreinador(carregando = false, escolhido = escolhido))
            } else {
                flow {
                    emit(SeletorDeTreinador(false, escolhido, numero, null))
                    emit(SeletorDeTreinador(false, escolhido, numero, especies.nome(numero)))
                }
            }
        }
    }
        .catch { emit(SeletorDeTreinador(carregando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS), SeletorDeTreinador())

    fun buscaMudou(texto: String) = busca.mudou(texto)

    /** Só um nome da lista é aceito (regra 1). O toque vai ao rascunho; o Perfil salva. */
    fun escolher(nome: String) {
        if (usuario == null || !busca.aceita(nome)) return
        rascunho.definirTreinador(nome)
    }
}
