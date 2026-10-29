package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada

/**
 * O que uma corrida rendeu, para o resultado da arena (29/10/2026): o XP, as espécies que
 * entraram na coleção e os recordes que ela bateu. Sai da mesma simulação que já dava o
 * `+N XP` da Home: o "antes" sem a corrida e o "depois" com ela.
 *
 * @property xp o ganho na temporada da corrida, ou nulo sem ganho.
 * @property novas os números da Pokédex que entraram na coleção, na ordem da escada.
 * @property recordes os recordes que a corrida bateu. Nas três primeiras corridas, os
 *   primeiros valores contam como batidos (docs/04 §3.3), embora ainda não paguem XP.
 */
data class RendimentoDaCorrida(
    val xp: Long?,
    val novas: List<Int>,
    val recordes: Set<TipoDeRecorde>,
) {
    val vazio: Boolean get() = xp == null && novas.isEmpty() && recordes.isEmpty()
}

/**
 * @param colecaoGravada a `colecao_max` gravada da temporada. A simulação não a conhece, e
 *   ela nunca recua (RN-38): uma espécie abaixo dela já estava colada, e não é nova.
 */
fun rendimentoDaCorrida(
    corridaId: String,
    antes: ResultadoDoReplay,
    depois: ResultadoDoReplay,
    temporada: Temporada,
    colecaoGravada: Int?,
): RendimentoDaCorrida {
    val de = maxOf(antes.porTemporada[temporada.id]?.colecaoMax ?: 1, colecaoGravada ?: 1)
    val ate = depois.porTemporada[temporada.id]?.colecaoMax ?: de
    val escada = temporada.tiers.sortedBy { it.ordem }.flatMap { it.especies }
    val novas = if (ate > de) escada.subList(minOf(de, escada.size), minOf(ate, escada.size)) else emptyList()
    return RendimentoDaCorrida(
        xp = Replay.ganhoNaTemporada(antes, depois, temporada.id),
        novas = novas,
        recordes = depois.porCorrida[corridaId]?.recordesBatidos.orEmpty(),
    )
}
