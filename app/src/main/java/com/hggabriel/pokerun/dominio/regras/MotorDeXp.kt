package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Semana

/** Os cinco recordes de docs/04 §3.3. Nulo é recorde ainda não estabelecido. */
data class Recordes(
    val melhorPaceSegKm: Double? = null,
    val melhor1kSeg: Long? = null,
    val melhor5kSeg: Long? = null,
    val maiorDistanciaKm: Double? = null,
    val maiorDuracaoSeg: Long? = null,
)

enum class TipoDeRecorde { MELHOR_PACE, MELHOR_1K, MELHOR_5K, MAIOR_DISTANCIA, MAIOR_DURACAO }

/** O que o motor precisa saber da história para pontuar uma corrida. */
data class ContextoDeXp(
    val ordemNoHistorico: Int,
    val recordesAnteriores: Recordes,
    val mediaMovelPaceSegKm: Double?,
    val semana: Semana?,
    val corridasDaSemana: List<Corrida> = emptyList(),
)

/** As parcelas da conta de docs/04 §3, que viram `xp_detalhe`. */
data class DetalheDeXp(
    val base: Double,
    val longao: Double,
    val pace: Int,
    val multiplicador: Double,
    val recorde: Int,
    val recordesBatidos: Set<TipoDeRecorde>,
    val total: Long,
)

object MotorDeXp {
    fun xpDaCorrida(corrida: Corrida, contexto: ContextoDeXp): DetalheDeXp =
        DetalheDeXp(0.0, 0.0, 0, 1.0, 0, emptySet(), 0L)
}
