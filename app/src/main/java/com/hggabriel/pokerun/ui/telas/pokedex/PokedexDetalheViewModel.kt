package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.dominio.regras.conquistaNaSemana
import com.hggabriel.pokerun.dominio.regras.gradeDaPokedex
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** A `PokedexDetailScreen` (docs/03 §3.19). */
sealed interface DetalheUiState {
    data object Carregando : DetalheUiState
    data class Conteudo(val especie: DetalheDaEspecie) : DetalheUiState
    data object Falhou : DetalheUiState
}

/**
 * A `PokedexDetailScreen` (`F4-T08`): uma espécie da temporada corrente. O número, o tier
 * e o que falta saem da definição da temporada e do progresso; da PokéAPI, só nome, tipos
 * e arte (D-10, decisão nº 102). Nada se escreve.
 *
 * A tela abre já com o número e o que a escada diz, e o nome e os tipos entram quando
 * chegam. A **semana da conquista** sai do `historico` do progresso: a primeira semana
 * fechada cuja posição já alcançava a espécie (decisão nº 139).
 */
class PokedexDetalheViewModel(
    private val numero: Int,
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val planos: PlanoRepositorio,
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

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun carregar(): Flow<DetalheUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(DetalheUiState.Falhou)

        return flow { emit(temporadaDe(relogio.instant(), temporadas.todas())) }.flatMapLatest { temporada ->
            if (temporada == null) return@flatMapLatest flowOf<DetalheUiState>(DetalheUiState.Falhou)

            progresso.observarProgresso(uid, temporada.id).flatMapLatest { lido ->
                flow<DetalheUiState> {
                    val xp = lido?.xpTotal ?: 0L
                    val posicao = posicaoNaEscada(xp, temporada)
                    val colecao = maxOf(lido?.colecaoMax ?: posicao, posicao)
                    val celula = gradeDaPokedex(temporada, posicao, colecao, xp).firstOrNull { it.numero == numero }
                    if (celula == null) {
                        emit(DetalheUiState.Falhou)
                        return@flow
                    }
                    val base = DetalheDaEspecie(celula, nome = null, tipos = emptyList(), arte = especies.sprite(numero), conquista = null)
                    emit(DetalheUiState.Conteudo(base))

                    val alcancada = celula.estado != EstadoDaCelula.NAO_ALCANCADA
                    val conquista = if (alcancada) conquista(lido?.historico.orEmpty(), celula.posicao) else null
                    emit(
                        DetalheUiState.Conteudo(
                            base.copy(
                                nome = especies.nome(numero),
                                tipos = if (alcancada) especies.tipos(numero) else emptyList(),
                                conquista = conquista,
                            ),
                        ),
                    )
                }
            }
        }
    }

    /** A semana do plano em que a espécie entrou na coleção, com o período se a grade responder. */
    private suspend fun conquista(
        historico: List<com.hggabriel.pokerun.dominio.regras.MarcoDaSemana>,
        posicao: Int,
    ): ConquistaDaEspecie? {
        val (planoId, numeroDaSemana) = conquistaNaSemana(posicao, historico) ?: return null
        val periodo: Pair<LocalDate, LocalDate>? = try {
            val plano = planos.buscar(planoId)
            val semana = planos.buscarSemanas(planoId).firstOrNull { it.numero == numeroDaSemana }
            if (plano == null || semana == null) {
                null
            } else {
                semana.dataInicio.atZone(plano.fuso).toLocalDate() to
                    semana.dataFim.atZone(plano.fuso).toLocalDate().minusDays(1)
            }
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            null
        }
        return ConquistaDaEspecie(numeroDaSemana, periodo?.first, periodo?.second)
    }
}
