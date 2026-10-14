package com.hggabriel.pokerun.ui.telas.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.regras.especieAtual
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** O card da espécie atual (docs/03 §3.3.2). Nulo enquanto não há o que mostrar. */
data class CardDaEspecie(
    val numero: Int,
    val nome: String?,
    val proximoNumero: Int?,
    val proximoNome: String?,
    val faltamXp: Long?,
    val silhueta: Boolean,
    val sprite: String,
)

/**
 * O card da espécie atual na Home (`F2-T14`, D-19). Fica num `ViewModel` próprio, e não
 * no da Home, porque a fonte é outra: o progresso da temporada, que o replay escreve, e
 * a PokéAPI. A Home continua a mesma quando o card não tem o que mostrar.
 *
 * A temporada é a corrente, a da data de hoje (RN-40). Sem temporada publicada, sem card.
 */
class EspecieAtualViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val card: StateFlow<CardDaEspecie?> = flow { emit(autenticacao.uidAtual) }
        .flatMapLatest { uid ->
            val temporada = uid?.let { temporadaDe(relogio.instant(), temporadas.todas()) }
            if (uid == null || temporada == null) {
                emptyFlow()
            } else {
                progresso.observarProgresso(uid, temporada.id).flatMapLatest { lido ->
                    flow {
                        val especie = especieAtual(lido?.xpTotal ?: 0L, lido?.ultimaCorridaEm, temporada, relogio.instant())
                        val semNomes = CardDaEspecie(
                            numero = especie.numero,
                            nome = null,
                            proximoNumero = especie.proximoNumero,
                            proximoNome = null,
                            faltamXp = especie.faltamXp,
                            silhueta = especie.silhueta,
                            sprite = especies.sprite(especie.numero),
                        )
                        // O número aparece já; o nome, quando a PokéAPI ou a cópia guardada
                        // responder. Sem rede e sem cópia, fica só o número (ficha de F2-T14).
                        emit(semNomes)
                        emit(
                            semNomes.copy(
                                nome = especies.nome(especie.numero),
                                proximoNome = especie.proximoNumero?.let { especies.nome(it) },
                            ),
                        )
                    }
                }
            }
        }
        .catch { }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = null,
        )
}
