package com.hggabriel.pokerun.dominio.regras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O treinador (estudo de Progresso e Perfil, Revisão 3): a URL do sprite, a silhueta sem
 * escolha, o nome fora da lista recusado, e a lista embutida no APK.
 */
class TreinadorTest {

    /** A lista real de `assets/`. O teste roda com o diretório do módulo como raiz. */
    private val lista: List<String> by lazy {
        val arquivo = listOf(File("src/main/assets/treinadores.txt"), File("app/src/main/assets/treinadores.txt"))
            .first { it.exists() }
        Treinador.lerLista(arquivo.readText(Charsets.UTF_8))
    }

    @Test
    fun `a url e a do sprite do Showdown pelo nome do arquivo`() {
        assertEquals("https://play.pokemonshowdown.com/sprites/trainers/red.png", Treinador.url("red"))
    }

    @Test
    fun `sem escolha nao ha sprite e quem desenha mostra a silhueta`() {
        assertNull(Treinador.urlOuSilhueta(null))
        assertEquals(Treinador.url("red"), Treinador.urlOuSilhueta("red"))
    }

    @Test
    fun `nome fora da lista e recusado`() {
        assertTrue(Treinador.aceita("red", lista))
        assertFalse(Treinador.aceita("ash-de-mentira", lista))
        assertFalse(Treinador.aceita("", lista))
    }

    @Test
    fun `a lista embutida tem os nomes do Showdown sem repeticao`() {
        // Trava de contagem (ARMADILHAS §8.2, item 37): a lista vazia passaria em tudo acima.
        assertTrue("a lista tem ${lista.size} nomes", lista.size >= 1_400)
        assertEquals(lista.size, lista.toSet().size)
        assertTrue(lista.all { Regex("[a-z0-9-]+").matches(it) })
    }

    @Test
    fun `todo treinador dos grupos curados esta na lista`() {
        val curados = Treinador.grupos.flatMap { it.treinadores }

        assertEquals(34, curados.size)
        assertEquals(emptyList<String>(), curados.filterNot { it in lista })
    }

    @Test
    fun `o rotulo e o nome em portugues ou o nome do arquivo arrumado`() {
        assertEquals("Corredor", Treinador.rotulo("jogger"))
        assertEquals("Leaf", Treinador.rotulo("leaf-masters"))
        assertEquals("Red", Treinador.rotulo("red"))
        assertEquals("Acerola Masters2", Treinador.rotulo("acerola-masters2"))
    }

    @Test
    fun `a busca ignora acento e caixa e poe o comeco antes do meio`() {
        val achados = Treinador.buscar("RED", listOf("fred", "red-masters", "red-gen1", "red", "brock"))

        assertEquals(listOf("red", "red-gen1", "red-masters", "fred"), achados)
        assertEquals(listOf("acetrainer"), Treinador.buscar("ás", listOf("acetrainer", "brock")))
        assertTrue(Treinador.buscar("  ", lista).isEmpty())
    }

    @Test
    fun `a leitura do arquivo ignora linha vazia, comentario e repeticao`() {
        assertEquals(listOf("red", "leaf"), Treinador.lerLista("red\n\n# comentário\nleaf\nred\n"))
    }
}
