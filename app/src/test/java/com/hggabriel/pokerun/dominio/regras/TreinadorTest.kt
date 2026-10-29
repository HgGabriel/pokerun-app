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
    fun `o indice ordena exato, prefixo, contem, o mais curto e no empate a ordem da lista`() {
        val indice = IndiceDeTreinadores(listOf("fred", "red-b", "red-masters", "red-a", "red", "brock", "lyra"))

        // `red-b` e `red-a` empatam em faixa e comprimento: fica a ordem da lista.
        assertEquals(listOf("red", "red-b", "red-a", "red-masters", "fred"), indice.buscar("red"))
        assertEquals(emptyList<String>(), indice.buscar("x"))
        assertTrue(indice.contem("brock"))
        assertFalse(indice.contem("ash-de-mentira"))
    }

    @Test
    fun `o indice devolve o mesmo que a busca antiga em toda a lista`() {
        val indice = IndiceDeTreinadores(lista)
        val termos = listOf("a", "ac", "ace", "acerola", "red", "ás", "masters", "gen", "x", "  ", "Corredor", "LEAF")

        termos.forEach { termo ->
            assertEquals("termo \"$termo\"", buscaAntiga(termo, lista), indice.buscar(termo))
        }
        // Trava contra o verde vazio: "a" casa quase a lista inteira.
        assertTrue(indice.buscar("a").size > 500)
    }

    @Test
    fun `os curados nao se repetem, porque a chave da grade e o nome`() {
        val curados = Treinador.grupos.flatMap { it.treinadores }

        assertEquals(curados.size, curados.toSet().size)
    }

    /** A busca até 29/10/2026, copiada como referência da ordem que o índice preserva. */
    private fun buscaAntiga(termo: String, lista: List<String>): List<String> {
        fun normalizar(texto: String) = java.text.Normalizer.normalize(texto.trim().lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        val alvo = normalizar(termo)
        if (alvo.isEmpty()) return emptyList()
        val achados = lista.filter { normalizar(it).contains(alvo) || normalizar(Treinador.rotulo(it)).contains(alvo) }
        return achados.sortedWith(
            compareBy<String>(
                { if (normalizar(it) == alvo || normalizar(Treinador.rotulo(it)) == alvo) 0 else 1 },
                { if (normalizar(it).startsWith(alvo) || normalizar(Treinador.rotulo(it)).startsWith(alvo)) 0 else 1 },
                { it.length },
            ),
        )
    }

    @Test
    fun `a leitura do arquivo ignora linha vazia, comentario e repeticao`() {
        assertEquals(listOf("red", "leaf"), Treinador.lerLista("red\n\n# comentário\nleaf\nred\n"))
    }
}
