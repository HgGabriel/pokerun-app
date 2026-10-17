package com.hggabriel.pokerun.dominio.regras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * O `+N XP` da resposta ao registro (docs/03 §3.3): a diferença do total da temporada
 * entre o replay sem a corrida nova e o replay com ela.
 */
class GanhoNaTemporadaTest {

    @Test
    fun `o ganho e a diferenca do total da temporada`() {
        val antes = resultado("kanto-2026" to 1_200L)
        val depois = resultado("kanto-2026" to 1_850L)

        assertEquals(650L, Replay.ganhoNaTemporada(antes, depois, "kanto-2026"))
    }

    @Test
    fun `a primeira corrida da temporada ganha o total inteiro`() {
        val depois = resultado("kanto-2026" to 420L)

        assertEquals(420L, Replay.ganhoNaTemporada(resultado(), depois, "kanto-2026"))
    }

    @Test
    fun `a outra temporada nao entra na conta`() {
        // Um registro retroativo de dezembro feito em janeiro mexe na temporada da data
        // (RN-40). O total da temporada nova não pode vazar para o número.
        val antes = resultado("kanto-2026" to 1_000L, "johto-2027" to 300L)
        val depois = resultado("kanto-2026" to 1_400L, "johto-2027" to 300L)

        assertEquals(400L, Replay.ganhoNaTemporada(antes, depois, "kanto-2026"))
    }

    @Test
    fun `sem a temporada no resultado nao ha numero`() {
        assertNull(Replay.ganhoNaTemporada(resultado(), resultado(), "kanto-2026"))
    }

    @Test
    fun `total que nao subiu nao vira mais zero`() {
        val igual = resultado("kanto-2026" to 1_000L)

        assertNull(Replay.ganhoNaTemporada(igual, igual, "kanto-2026"))
    }

    private fun resultado(vararg totais: Pair<String, Long>) = ResultadoDoReplay(
        porCorrida = emptyMap(),
        xpPorSemana = emptyMap(),
        porTemporada = totais.associate { (temporada, xp) ->
            temporada to ProgressoNaTemporada(xpTotal = xp, especieAtual = 1, ultimaCorridaEm = Instant.EPOCH)
        },
        recordes = Recordes(),
    )
}
