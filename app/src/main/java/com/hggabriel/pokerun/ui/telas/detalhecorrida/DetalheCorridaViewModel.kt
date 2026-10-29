package com.hggabriel.pokerun.ui.telas.detalhecorrida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * A `RunDetailScreen` (`F2-T09`). Observa a corrida e o plano dela: o replay que segue
 * uma gravação ou uma correção reescreve `xp_creditado` e `xp_detalhe`, e a conta aberta
 * se atualiza sem a tela pedir.
 *
 * A temporada, o progresso e o histórico dão a espécie, as figurinhas e os recordes da
 * corrida. Chegam depois e podem falhar sem derrubar a tela: cada um começa vazio e, se
 * falhar, fica vazio. Os tipos das espécies (a luz atrás do Pokémon e o fundo das
 * figurinhas) vêm da PokéAPI, à parte, em [tipos].
 */
class DetalheCorridaViewModel(
    private val corridaId: String,
    private val autenticacao: AutenticacaoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val especies: EspeciesRepositorio,
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    private val _tipos = MutableStateFlow<Map<Int, List<TipoDePokemon>>>(emptyMap())
    val tipos: StateFlow<Map<Int, List<TipoDePokemon>>> = _tipos.asStateFlow()
    private val pedidos = mutableSetOf<Int>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalheCorridaUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(DetalheCorridaUiState.Falhou) }
        .onEach { if (it is DetalheCorridaUiState.Conteudo) pedirTipos(listOfNotNull(it.especie) + it.figurinhas) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = DetalheCorridaUiState.Carregando,
        )

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<DetalheCorridaUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(DetalheCorridaUiState.Falhou)

        val corrida = corridas.observar(uid, corridaId)
        val plano = corrida.map { it?.planoId }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(null) else planos.observar(id)
        }
        val album = corrida.map { it?.temporadaId }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(SemAlbum) else temporadaComProgresso(uid, id)
        }
        val historico = corridas.observarHistorico(uid)
            .map<List<Corrida>, List<Corrida>?> { it }
            .onStart { emit(null) }
            .catch { emit(null) }

        return combine(corrida, plano, album, historico) { lida, doPlano, (temporada, lido), todas ->
            detalheDaCorrida(lida, doPlano, temporada, todas, lido)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun temporadaComProgresso(uid: String, temporadaId: String): Flow<Pair<Temporada?, ProgressoNaTemporada?>> =
        flow { emit(temporadas.todas().firstOrNull { it.id == temporadaId }) }
            .flatMapLatest { temporada ->
                if (temporada == null) {
                    flowOf(SemAlbum)
                } else {
                    progresso.observarProgresso(uid, temporada.id)
                        .map<ProgressoNaTemporada?, Pair<Temporada?, ProgressoNaTemporada?>> { temporada to it }
                        .onStart { emit(temporada to null) }
                }
            }
            .onStart { emit(SemAlbum) }
            .catch { emit(SemAlbum) }

    /** Os tipos guardados no aparelho entram na hora; os que faltam, pela rede. */
    private fun pedirTipos(numeros: List<Int>) {
        val novos = numeros.filter { pedidos.add(it) }
        if (novos.isEmpty()) return
        val guardados = novos.mapNotNull { n -> especies.tiposGuardados(n).takeIf { it.isNotEmpty() }?.let { n to it } }.toMap()
        if (guardados.isNotEmpty()) _tipos.update { it + guardados }
        val faltam = novos - guardados.keys
        if (faltam.isEmpty()) return
        viewModelScope.launch {
            faltam.forEach { numero ->
                try {
                    especies.tipos(numero).takeIf { it.isNotEmpty() }?.let { tipos -> _tipos.update { it + (numero to tipos) } }
                } catch (cancelamento: CancellationException) {
                    throw cancelamento
                } catch (_: Exception) {
                    pedidos.remove(numero)
                }
            }
        }
    }

    private companion object {
        val SemAlbum: Pair<Temporada?, ProgressoNaTemporada?> = null to null
    }
}
