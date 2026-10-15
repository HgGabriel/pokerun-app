package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Instant

/** F4-T10: ainda sem implementação (vermelho). */
object Regressao {
    fun semanasDeQueda(ultimaCorridaEm: Instant, agora: Instant): Int = 0

    fun pisoDoTier(xp: Long, temporada: Temporada): Long = 0L

    fun aplicar(xp: Long, semanas: Int, temporada: Temporada): Long = xp
}
