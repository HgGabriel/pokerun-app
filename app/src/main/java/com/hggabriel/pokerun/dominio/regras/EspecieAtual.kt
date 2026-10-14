package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Instant

/** O que o card da espécie atual mostra (docs/01 §2.1). */
data class EspecieAtual(
    val posicao: Int,
    val numero: Int,
    val proximoNumero: Int?,
    val faltamXp: Long?,
    val silhueta: Boolean,
)

fun especieAtual(xp: Long, ultimaCorridaEm: Instant?, temporada: Temporada, agora: Instant): EspecieAtual =
    EspecieAtual(0, 0, null, null, false)

fun numeroNaEscada(posicao: Int, temporada: Temporada): Int = 0

fun nomeDaEspecie(json: String): String? = null
