package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada

/**
 * // RN-43
 *
 * O que está errado na definição de uma temporada, ou a lista vazia se nada estiver
 * (docs/07, nota de integridade; ficha de `F2-T00`).
 *
 * Três conferências, e só as três que a spec pede: **cada ID uma vez só**, **a soma dos
 * tiers igual a `total_especies`**, e **nenhum ID fora de `1..total_especies`**. Juntas
 * elas dizem que a escada cobre a dex inteira, sem lacuna: com a soma certa, sem
 * repetição e dentro da faixa, não sobra lugar para um ID faltar.
 *
 * **A faixa sai de [Temporada.totalEspecies], nunca de literal** — é o que RN-43 existe
 * para garantir, e é o que deixa a mesma função valer para os 235 de 2027.
 *
 * Devolve a lista em vez de parar no primeiro erro: um erro de digitação no console
 * costuma produzir dois sintomas de uma vez (o ID repetido **e** a soma errada), e ver
 * os dois juntos é o que aponta a linha.
 */
fun problemasDaTemporada(temporada: Temporada): List<String> {
    val problemas = mutableListOf<String>()
    val todos = temporada.tiers.flatMap { it.especies }
    val faixa = 1..temporada.totalEspecies

    todos.groupingBy { it }.eachCount()
        .filterValues { it > 1 }
        .keys.sorted()
        .forEach { id -> problemas += "a espécie #$id aparece repetida" }

    todos.filter { it !in faixa }.distinct().sorted()
        .forEach { id -> problemas += "a espécie #$id está fora da faixa 1..${temporada.totalEspecies}" }

    if (todos.size != temporada.totalEspecies) {
        problemas += "os tiers somam ${todos.size} espécies e o total_especies é ${temporada.totalEspecies}"
    }
    return problemas
}
