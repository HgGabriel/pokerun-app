package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Membro

/** Uma linha do ranking de docs/03 §3.16. */
data class LinhaDoRanking(
    val posicao: Int,
    val uid: String,
    val nome: String,
    val xpSemana: Long,
    val sessoesFeitas: Int,
    val sessoesPrevistas: Int,
    val kmFeito: Double,
    val avatarEspecie: Int?,
)

object Ranking {
    fun ordenar(
        membros: List<Membro>,
        semanais: Map<String, AgregadoDaSemana?>,
        sessoesPrevistas: Int,
    ): List<LinhaDoRanking> = emptyList()
}
