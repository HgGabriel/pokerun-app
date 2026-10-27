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
    /** A posição na escada que o avatar mostra (docs/03 §3.16.1). Nula antes do replay. */
    val avatarEspecie: Int?,
    /**
     * A posição **atual** na Pokédex, que pode recuar (RN-37), em coluna ao lado do avatar,
     * que é a coleção (`F4-T12`, docs/03 §3.16.1). Nula antes do replay da Fase 4.
     */
    val posicaoPokedex: Int? = null,
    /** O parceiro escolhido pelo membro; nulo, vale o [avatarEspecie] (`Parceiro.doMembro`). */
    val parceiro: Int? = null,
    /** O treinador escolhido pelo membro; nulo é a silhueta. */
    val treinador: String? = null,
)

/**
 * O ranking do grupo na semana corrente (`F2-T12`, docs/03 §3.16).
 */
object Ranking {

    /**
     * // RN-18
     *
     * Ordena pelo **XP da semana**, nunca pelo acumulado, que compararia tempo de conta.
     * A aderência vai ao lado; o km vai num bloco à parte e **nunca ordena** (RN-09).
     *
     * Membro sem `weekly` na semana entra com zero: na semana 1 a tela mostra todo mundo
     * em 0%, e não uma lista vazia (docs/03 §3.16). Membro inativo não entra.
     *
     * **O empate divide a posição** (decisão nº 128): dois com 700 XP são ambos 1º, e o
     * seguinte é o 3º. Dentro do empate, a ordem é a do nome, para a lista não trocar de
     * lugar entre duas aberturas.
     */
    fun ordenar(
        membros: List<Membro>,
        semanais: Map<String, AgregadoDaSemana?>,
        sessoesPrevistas: Int,
    ): List<LinhaDoRanking> {
        val linhas = membros
            .filter { it.ativo }
            .map { membro ->
                val semanal = semanais[membro.uid]
                LinhaDoRanking(
                    posicao = 0,
                    uid = membro.uid,
                    nome = membro.nome,
                    xpSemana = semanal?.xpSemana ?: 0L,
                    sessoesFeitas = semanal?.sessoesFeitas ?: 0,
                    sessoesPrevistas = sessoesPrevistas,
                    kmFeito = semanal?.kmFeito ?: 0.0,
                    avatarEspecie = membro.avatarEspecie,
                    posicaoPokedex = membro.posicaoPokedex,
                    parceiro = membro.parceiro,
                    treinador = membro.treinador,
                )
            }
            .sortedWith(compareByDescending<LinhaDoRanking> { it.xpSemana }.thenBy { it.nome }.thenBy { it.uid })

        return linhas.mapIndexed { indice, linha ->
            val primeiroDoEmpate = linhas.indexOfFirst { it.xpSemana == linha.xpSemana }
            linha.copy(posicao = (if (primeiroDoEmpate >= 0) primeiroDoEmpate else indice) + 1)
        }
    }
}
