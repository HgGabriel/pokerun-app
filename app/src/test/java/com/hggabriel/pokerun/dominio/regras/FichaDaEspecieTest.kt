package com.hggabriel.pokerun.dominio.regras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A leitura da ficha e da evolução na PokéAPI (docs/03 §3.19), com respostas de amostra. */
class FichaDaEspecieTest {

    @Test
    fun `altura e peso saem em metro e quilo`() {
        val ficha = fichaDaEspecie("""{"id":64,"height":13,"weight":565,"types":[]}""")

        assertEquals(1.3, ficha!!.alturaM, 1e-9)
        assertEquals(56.5, ficha.pesoKg, 1e-9)
    }

    @Test
    fun `ficha sem altura ou resposta que nao e JSON e nula`() {
        assertNull(fichaDaEspecie("""{"id":64,"weight":565}"""))
        assertNull(fichaDaEspecie("<html>"))
    }

    @Test
    fun `o endereco da cadeia sai de pokemon-species`() {
        val json = """{"id":64,"evolution_chain":{"url":"https://pokeapi.co/api/v2/evolution-chain/26/"}}"""

        assertEquals("https://pokeapi.co/api/v2/evolution-chain/26/", cadeiaDaEspecie(json))
        assertNull(cadeiaDaEspecie("""{"id":64}"""))
    }

    @Test
    fun `a cadeia vira estagios pelo numero da especie`() {
        assertEquals(listOf(listOf(63), listOf(64), listOf(65)), estagiosDaEvolucao(abra, permitidos = (1..150).toSet()))
    }

    @Test
    fun `o ramo fora da temporada sai e o estagio vazio some`() {
        // Eevee: em Kanto só Vaporeon, Jolteon e Flareon.
        assertEquals(listOf(listOf(133), listOf(134, 135, 136)), estagiosDaEvolucao(eevee, permitidos = (1..150).toSet()))
        // Pichu (172) é de Johto: a cadeia começa no Pikachu.
        assertEquals(listOf(listOf(25), listOf(26)), estagiosDaEvolucao(pichu, permitidos = (1..150).toSet()))
    }

    @Test
    fun `especie sem evolucao tem um estagio so e resposta ruim e lista vazia`() {
        assertEquals(listOf(listOf(128)), estagiosDaEvolucao(cadeia(no(128)), permitidos = (1..150).toSet()))
        assertEquals(emptyList<List<Int>>(), estagiosDaEvolucao("nada", permitidos = (1..150).toSet()))
    }

    private fun no(n: Int, vararg filhos: String) =
        """{"species":{"name":"x","url":"https://pokeapi.co/api/v2/pokemon-species/$n/"},"evolves_to":[${filhos.joinToString(",")}]}"""

    private fun cadeia(raiz: String) = """{"id":1,"chain":$raiz}"""

    private val abra = cadeia(no(63, no(64, no(65))))
    private val eevee = cadeia(no(133, no(134), no(135), no(136), no(196), no(197)))
    private val pichu = cadeia(no(172, no(25, no(26))))
}
