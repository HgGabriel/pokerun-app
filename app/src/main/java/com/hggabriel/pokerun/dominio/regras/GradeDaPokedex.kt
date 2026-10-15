package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada

/** Os três estados de uma célula da grade (docs/03 §3.18). */
enum class EstadoDaCelula {
    /** Na coleção. Fica colorida mesmo atrás da posição atual (RN-38). */
    ALCANCADA,

    /** A posição atual: a espécie onde o XP corrente deixa o corredor. */
    ATUAL,

    /** Ainda fora da coleção: silhueta, com número e nome visíveis. */
    NAO_ALCANCADA,
}

/**
 * Uma espécie da grade da Pokédex.
 *
 * @property posicao a posição na escada, de 1 a `total_especies`.
 * @property numero o número da Pokédex, que dá nome, sprite e tipos (D-10).
 * @property tier a ordem do tier que a temporada põe a espécie, **o único lugar de onde sai
 *   tier ou custo**: da PokéAPI só saem nome, sprite e tipo (decisão nº 102).
 * @property xpParaAlcancar o XP total da temporada em que a escada põe o corredor nela. A
 *   primeira espécie é de graça (D-15).
 * @property faltamXp quanto falta desse XP para o corredor de hoje, zero se já passou.
 */
data class CelulaDaPokedex(
    val posicao: Int,
    val numero: Int,
    val tier: Int,
    val nomeDoTier: String,
    val estado: EstadoDaCelula,
    val xpParaAlcancar: Long,
    val faltamXp: Long,
)

/**
 * A grade da `PokedexMainScreen` (`F4-T02`, docs/03 §3.18, docs/07): as espécies da
 * temporada **na ordem dos tiers**, cada uma com o estado que a coleção e a posição lhe
 * dão. Quantas são, em quantos tiers e quanto custam vêm da definição da temporada
 * (XP-04, RN-43).
 *
 * @param posicao a posição atual na escada, que pode recuar (RN-37).
 * @param colecaoMax o máximo histórico da posição, que nunca recua (XP-06).
 * @param xp o XP corrente da temporada, já com a regressão (`F4-T10`).
 */
fun gradeDaPokedex(temporada: Temporada, posicao: Int, colecaoMax: Int, xp: Long): List<CelulaDaPokedex> {
    val degraus = escada(temporada)
    val nomes = temporada.tiers.sortedBy { it.ordem }.flatMap { tier -> tier.especies.map { tier.ordem to tier.nome } }
    val colecao = maxOf(colecaoMax, posicao)

    return degraus.mapIndexed { indice, (numero, acumulado) ->
        val posicaoDaCelula = indice + 1
        val custo = if (posicaoDaCelula == 1) 0L else acumulado
        CelulaDaPokedex(
            posicao = posicaoDaCelula,
            numero = numero,
            tier = nomes[indice].first,
            nomeDoTier = nomes[indice].second,
            estado = when {
                posicaoDaCelula == posicao -> EstadoDaCelula.ATUAL
                posicaoDaCelula <= colecao -> EstadoDaCelula.ALCANCADA
                else -> EstadoDaCelula.NAO_ALCANCADA
            },
            xpParaAlcancar = custo,
            faltamXp = (custo - xp).coerceAtLeast(0L),
        )
    }
}
