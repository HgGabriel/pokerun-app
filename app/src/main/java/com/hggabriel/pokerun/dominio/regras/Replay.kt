package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Instant

/** O progresso de uma temporada que o replay deriva das corridas (docs/05 §2.5, passo 4). */
data class ProgressoNaTemporada(
    val xpTotal: Long,
    val especieAtual: Int,
    val ultimaCorridaEm: Instant,
)

/** Tudo o que o replay recalcula. As chaves de [xpPorSemana] são `{planId}_{n}`. */
data class ResultadoDoReplay(
    val porCorrida: Map<String, DetalheDeXp>,
    val xpPorSemana: Map<String, Long>,
    val porTemporada: Map<String, ProgressoNaTemporada>,
    val recordes: Recordes,
)

/** O que o replay grava num intervalo: o detalhe de cada corrida e o `xp_semana`. */
data class EscritasDoReplay(
    val corridas: Map<String, DetalheDeXp>,
    val semanas: Map<String, Long>,
)

object Replay {
    fun recalcular(
        corridas: List<Corrida>,
        grades: Map<String, List<Semana>>,
        temporadas: List<Temporada>,
    ): ResultadoDoReplay = ResultadoDoReplay(emptyMap(), emptyMap(), emptyMap(), Recordes())

    fun escritas(
        corridas: List<Corrida>,
        resultado: ResultadoDoReplay,
        desde: Instant,
        ate: Instant,
    ): EscritasDoReplay = EscritasDoReplay(emptyMap(), emptyMap())

    fun chaveDaSemana(planoId: String, numero: Int): String = "${planoId}_$numero"
}

fun posicaoNaEscada(xp: Long, temporada: Temporada): Int = 0
