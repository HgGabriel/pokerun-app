package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.PainelDaPokedex
import com.hggabriel.pokerun.dominio.regras.gradeDaPokedex
import com.hggabriel.pokerun.dominio.regras.painelDaPokedex
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.dominio.regras.xpDaMetaDaSemana
import com.hggabriel.pokerun.ui.componentes.EscadaDeProgresso
import com.hggabriel.pokerun.ui.componentes.escadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** Quantas espécies buscam nome e tipo ao mesmo tempo: o bastante para não esperar 150 idas em fila. */
private const val BUSCAS_EM_PARALELO = 8

/** A `PokedexMainScreen` (docs/03 §3.18). */
sealed interface PokedexUiState {

    data object Carregando : PokedexUiState

    /** Nenhuma temporada publicada cobre hoje (RN-40). */
    data object SemTemporada : PokedexUiState

    data class Conteudo(
        val temporada: Temporada,
        val escada: EscadaDeProgresso,
        val painel: PainelDaPokedex,
        val celulas: List<CelulaDaPokedex>,
    ) : PokedexUiState

    data object Falhou : PokedexUiState
}

/**
 * A `PokedexMainScreen` (`F4-T07`, docs/03 §3.18): o progresso da temporada corrente, a
 * meta da semana do plano ativo e, da PokéAPI, só nome e tipo (D-10, decisão nº 102).
 * Nenhuma escrita.
 *
 * **Nome de toda espécie, tipo só das alcançadas**: a silhueta esconde o tipo, e o
 * número e o nome de uma espécie não alcançada nunca se escondem (docs/03 §3.18). As
 * buscas rodam em grupos de [BUSCAS_EM_PARALELO] e ficam guardadas em disco pelo
 * repositório depois da primeira vez.
 */
class PokedexViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<PokedexUiState> = tentativas
        .flatMapLatest { carregar() }
        .catch { emit(PokedexUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = PokedexUiState.Carregando,
        )

    private val nomesLidos = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val tiposLidos = MutableStateFlow<Map<Int, List<TipoDePokemon>>>(emptyMap())

    /** O nome em inglês pelo número da Pokédex, à medida que chega. */
    val nomes: StateFlow<Map<Int, String>> = nomesLidos.asStateFlow()

    /** Os tipos pelo número, só das espécies pedidas com tipo. */
    val tipos: StateFlow<Map<Int, List<TipoDePokemon>>> = tiposLidos.asStateFlow()

    private val pedidos = mutableSetOf<Int>()
    private val pedidosComTipo = mutableSetOf<Int>()

    fun tentarDeNovo() = tentativas.update { it + 1 }

    /** Pede nome de todas e tipo das [comTipo]. Pedir duas vezes a mesma espécie não repete a ida. */
    fun carregarEspecies(numeros: List<Int>, comTipo: Set<Int>) {
        val novas = numeros.filter { pedidos.add(it) }
        val novasComTipo = comTipo.filter { pedidosComTipo.add(it) }
        if (novas.isEmpty() && novasComTipo.isEmpty()) return
        viewModelScope.launch {
            try {
                (novas + novasComTipo).distinct().chunked(BUSCAS_EM_PARALELO).forEach { grupo ->
                    grupo.map { numero ->
                        async {
                            if (numero in novas) {
                                especies.nome(numero)?.let { nome -> nomesLidos.update { it + (numero to nome) } }
                            }
                            if (numero in novasComTipo) {
                                especies.tipos(numero).takeIf { it.isNotEmpty() }
                                    ?.let { lidos -> tiposLidos.update { it + (numero to lidos) } }
                            }
                        }
                    }.awaitAll()
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // Sem rede: o número fica, e a próxima abertura tenta de novo.
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun carregar(): Flow<PokedexUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(PokedexUiState.Falhou)

        return flow { emit(temporadaDe(relogio.instant(), temporadas.todas())) }.flatMapLatest { temporada ->
            if (temporada == null) {
                flowOf(PokedexUiState.SemTemporada)
            } else {
                combine(progresso.observarProgresso(uid, temporada.id), metaDaSemana(uid)) { lido, meta ->
                    val xp = lido?.xpTotal ?: 0L
                    val posicao = posicaoNaEscada(xp, temporada)
                    val colecao = maxOf(lido?.colecaoMax ?: posicao, posicao)
                    PokedexUiState.Conteudo(
                        temporada = temporada,
                        escada = escadaDe(temporada, posicao, colecao),
                        painel = painelDaPokedex(temporada, xp, colecao, lido?.historico.orEmpty(), meta),
                        celulas = gradeDaPokedex(temporada, posicao, colecao, xp),
                    )
                }
            }
        }
    }

    /** O XP que a semana corrente do plano ativo ainda rende, ou nulo sem plano ou sem semana. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun metaDaSemana(uid: String): Flow<Long?> = usuarios.observar(uid).flatMapLatest { usuario ->
        val planoId = usuario?.planoAtivoId ?: return@flatMapLatest flowOf(null)
        combine(
            planos.observar(planoId),
            planos.observarSemanas(planoId),
            corridas.observarDoPlano(uid, planoId),
        ) { plano, grade, doPlano ->
            plano?.let { xpDaMetaDaSemana(it, grade, doPlano, relogio.instant()) }
        }
    }
}
