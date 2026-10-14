package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Semana

/** O documento `plans/{planId}/desafios/{semanaRef}` (docs/05 §1). */
data class Desafio(
    val tipo: String,
    val meta: Double,
    val acumulado: Double,
    val concluido: Boolean,
    val insigniaId: String?,
)

object DesafioColetivo {
    val INSIGNIAS: List<String> = emptyList()

    fun atualizar(
        existente: Desafio?,
        semana: Semana,
        membrosAtivos: Int,
        semanais: Collection<AgregadoDaSemana?>,
        concluidosAntes: Int,
    ): Desafio = Desafio("", 0.0, 0.0, false, null)

    fun conquistadas(desafios: Collection<Desafio>): List<String> = emptyList()
}
