package com.hggabriel.pokerun.ui.telas.medalhas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.ritmoDe
import com.hggabriel.pokerun.ui.telas.detalhecorrida.paceDe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * As medalhas do ritmo: quantas corridas o usuário já fez em cada ritmo. Nulo enquanto o
 * histórico não chegou; o ritmo sem corrida não entra no mapa.
 */
data class MedalhasDoRitmoUiState(val corridasPorRitmo: Map<Ritmo, Int>?)

/**
 * O pace de cada ritmo: de [de] (inclusive) até [ate] (exclusivo), em segundos por km. O mais
 * rápido não tem piso; o mais lento não tem teto.
 */
data class FaixaDoRitmo(val de: Long?, val ate: Long?)

/** A faixa de [ritmo]: o piso é o teto do ritmo logo mais rápido. */
fun faixaDo(ritmo: Ritmo): FaixaDoRitmo {
    val anterior = Ritmo.entries.getOrNull(ritmo.ordinal - 1)
    return FaixaDoRitmo(de = anterior?.tetoSeg, ate = ritmo.tetoSeg.takeIf { it != Long.MAX_VALUE })
}

/**
 * Quantas corridas caem em cada ritmo, pelo mesmo pace arredondado que o detalhe da corrida
 * mostra. Descartada e substituída não contam: saíram dos cálculos (RN-24, RN-31).
 */
fun corridasPorRitmo(corridas: List<Corrida>): Map<Ritmo, Int> =
    corridas.asSequence()
        .filter { !it.descartada && !it.substituida }
        .mapNotNull { paceDe(it.km, it.duracaoSeg) }
        .map(::ritmoDe)
        .groupingBy { it }
        .eachCount()

/** O histórico vitalício, contado por ritmo: as medalhas atravessam planos e temporadas (RN-39). */
class MedalhasDoRitmoViewModel(
    autenticacao: AutenticacaoRepositorio,
    corridas: CorridaRepositorio,
) : ViewModel() {

    val estado: StateFlow<MedalhasDoRitmoUiState> =
        (autenticacao.uidAtual?.let(corridas::observarHistorico) ?: flowOf(emptyList()))
            .map { MedalhasDoRitmoUiState(corridasPorRitmo(it)) }
            // Sem histórico, a tela ainda mostra as sete medalhas: o que dá para ganhar.
            .catch { emit(MedalhasDoRitmoUiState(emptyMap())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedalhasDoRitmoUiState(null))
}
