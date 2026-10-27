package com.hggabriel.pokerun.ui.telas.pokedex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** O encaixe denso da página do álbum (docs/03 §3.18), a 3 e a 2 colunas. */
class GradeDoAlbumTest {

    private fun e(linha: Int, coluna: Int) = Encaixe(linha, coluna)

    @Test
    fun `sem dobro a grade enche linha a linha`() {
        assertEquals(
            listOf(e(0, 0), e(0, 1), e(0, 2), e(1, 0)),
            encaixeDenso(listOf(false, false, false, false), colunas = 3),
        )
    }

    @Test
    fun `o dobro no inicio da linha ocupa 2x2 e as seguintes enchem a coluna livre`() {
        // [D D a]
        // [D D b]
        // [c . .]
        assertEquals(
            listOf(e(0, 0), e(0, 2), e(1, 2), e(2, 0)),
            encaixeDenso(listOf(true, false, false, false), colunas = 3),
        )
    }

    @Test
    fun `o dobro que nao cabe no fim da linha desce e a seguinte tapa o buraco`() {
        // [a b c]
        // [D D d]
        // [D D f]
        val encaixes = encaixeDenso(listOf(false, false, true, false, false, false), colunas = 3)

        assertEquals(e(1, 0), encaixes[2])
        assertEquals(e(0, 2), encaixes[3])
        assertEquals(e(1, 2), encaixes[4])
        assertEquals(e(2, 2), encaixes[5])
    }

    @Test
    fun `a 2 colunas o dobro ocupa a linha inteira`() {
        val encaixes = encaixeDenso(listOf(false, true, false), colunas = 2)

        assertEquals(listOf(e(0, 0), e(1, 0), e(0, 1)), encaixes)
    }

    @Test
    fun `nenhuma casa e ocupada por duas figurinhas`() {
        listOf(2, 3).forEach { colunas ->
            val dobros = List(25) { it == 11 }
            val encaixes = encaixeDenso(dobros, colunas)
            val casas = encaixes.flatMapIndexed { i, (l, c) ->
                val lado = if (dobros[i]) 2.coerceAtMost(colunas) else 1
                (0 until lado).flatMap { dl -> (0 until lado).map { dc -> e(l + dl, c + dc) } }
            }
            assertEquals(casas.size, casas.toSet().size)
            assertTrue(casas.all { it.coluna < colunas })
        }
    }
}
