package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.FichaDaEspecie
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** A `PokedexDetailScreen` (docs/03 §3.19). */
internal sealed interface DetalheUiState {
    data object Carregando : DetalheUiState

    /** O álbum inteiro, porque o detalhe é um pager pela ordem dos tiers. */
    data class Conteudo(val temporada: Temporada, val album: AlbumMontado) : DetalheUiState {
        val ordem: List<Int> = album.paginas.flatMap { p -> p.figurinhas.map { it.numero } }
    }

    data object Falhou : DetalheUiState
}

/**
 * A `PokedexDetailScreen` (docs/03 §3.19): a figurinha ampliada, num pager pela ordem dos
 * tiers. O álbum sai do progresso e das corridas da temporada, como na lista; da PokéAPI,
 * nome, tipos, ficha e evolução, buscados quando a página assenta (D-10, decisão nº 102).
 * Nada se escreve.
 */
internal class PokedexDetalheViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val corridas: CorridaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalheUiState> = tentativas
        .flatMapLatest { carregar() }
        .catch { emit(DetalheUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = DetalheUiState.Carregando,
        )

    private val _nomes = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val _tipos = MutableStateFlow<Map<Int, List<TipoDePokemon>>>(emptyMap())
    private val _fichas = MutableStateFlow<Map<Int, FichaDaEspecie>>(emptyMap())
    private val _evolucoes = MutableStateFlow<Map<Int, List<List<Int>>>>(emptyMap())

    val nomes: StateFlow<Map<Int, String>> = _nomes.asStateFlow()
    val tipos: StateFlow<Map<Int, List<TipoDePokemon>>> = _tipos.asStateFlow()
    val fichas: StateFlow<Map<Int, FichaDaEspecie>> = _fichas.asStateFlow()
    val evolucoes: StateFlow<Map<Int, List<List<Int>>>> = _evolucoes.asStateFlow()

    private val pedidas = mutableSetOf<Int>()

    fun tentarDeNovo() = tentativas.update { it + 1 }

    /** O grito da espécie, pela PokéAPI. */
    fun grito(numero: Int): String = especies.grito(numero)

    /**
     * Busca o que a página de [numero] mostra: nome e tipo dela e das vizinhas; ficha e
     * evolução só se ela está colada, porque o verso da vaga fica trancado. Sem rede, a
     * página mostra o que já tem, e a próxima abertura tenta de novo.
     */
    fun carregarPagina(numero: Int, colada: Boolean, vizinhas: List<Int>, permitidos: Set<Int>) {
        if (!pedidas.add(numero * 2 + if (colada) 1 else 0)) return
        viewModelScope.launch {
            try {
                (listOf(numero) + vizinhas).forEach { n -> nomeETipo(n) }
                if (!colada) return@launch
                especies.ficha(numero)?.let { ficha -> _fichas.update { it + (numero to ficha) } }
                val estagios = especies.evolucao(numero, permitidos)
                _evolucoes.update { it + (numero to estagios) }
                estagios.flatten().forEach { n -> nomeETipo(n) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                pedidas.remove(numero * 2 + if (colada) 1 else 0)
            }
        }
    }

    private suspend fun nomeETipo(numero: Int) {
        if (numero !in _nomes.value) especies.nome(numero)?.let { nome -> _nomes.update { it + (numero to nome) } }
        if (numero !in _tipos.value) {
            especies.tipos(numero).takeIf { it.isNotEmpty() }?.let { tipos -> _tipos.update { it + (numero to tipos) } }
        }
    }

    /** O que a lista já guardou no aparelho entra antes da primeira página: nada de `#029` e `Tipo ?`. */
    private fun semearDoAparelho(numeros: List<Int>) {
        val nomes = numeros.mapNotNull { n -> especies.nomeGuardado(n)?.let { n to it } }.toMap()
        val tipos = numeros.mapNotNull { n -> especies.tiposGuardados(n).takeIf { it.isNotEmpty() }?.let { n to it } }.toMap()
        _nomes.update { nomes + it }
        _tipos.update { tipos + it }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun carregar(): Flow<DetalheUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(DetalheUiState.Falhou)

        return flow { emit(temporadaDe(relogio.instant(), temporadas.todas())) }.flatMapLatest { temporada ->
            if (temporada == null) return@flatMapLatest flowOf<DetalheUiState>(DetalheUiState.Falhou)
            semearDoAparelho(temporada.tiers.flatMap { it.especies })
            combine(progresso.observarProgresso(uid, temporada.id), corridas.observarHistorico(uid)) { lido, historico ->
                val xp = lido?.xpTotal ?: 0L
                DetalheUiState.Conteudo(
                    temporada = temporada,
                    album = montarAlbum(
                        temporada = temporada,
                        xp = xp,
                        colecaoMax = maxOf(lido?.colecaoMax ?: 1, posicaoNaEscada(xp, temporada)),
                        historico = lido?.historico.orEmpty(),
                        corridas = historico,
                        xpDaMeta = null,
                        agora = relogio.instant(),
                    ),
                )
            }
        }
    }
}
