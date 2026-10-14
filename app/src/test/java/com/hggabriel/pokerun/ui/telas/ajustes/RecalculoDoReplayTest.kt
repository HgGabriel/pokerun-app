package com.hggabriel.pokerun.ui.telas.ajustes

import com.hggabriel.pokerun.dominio.regras.DetalheDeXp
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.ResultadoDoReplay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * O resumo do botão de recálculo (`F3-T08`, docs/05 §2.5): quantas corridas o replay
 * percorreu e o XP da temporada antes e depois.
 */
class RecalculoDoReplayTest {

    @Test
    fun `o resumo conta as corridas validas e abre o XP da temporada antes e depois`() {
        val antes = resultado(corridas = 3, xp = 4_200)
        val depois = resultado(corridas = 4, xp = 5_100)

        assertEquals(ResumoDoRecalculo(corridas = 4, xpAntes = 4_200, xpDepois = 5_100), resumoDoRecalculo(antes, depois, "kanto-2026"))
    }

    @Test
    fun `sem progresso na temporada o XP e zero`() {
        val vazio = ResultadoDoReplay(emptyMap(), emptyMap(), emptyMap(), Recordes())

        assertEquals(ResumoDoRecalculo(0, 0, 0), resumoDoRecalculo(vazio, vazio, "kanto-2026"))
        assertEquals(ResumoDoRecalculo(0, 0, 0), resumoDoRecalculo(vazio, vazio, null))
    }

    private fun resultado(corridas: Int, xp: Long) = ResultadoDoReplay(
        porCorrida = (1..corridas).associate { "c$it" to DetalheDeXp(0.0, 0.0, 0, 1.0, 0, emptySet(), 0L) },
        xpPorSemana = emptyMap(),
        porTemporada = mapOf("kanto-2026" to ProgressoNaTemporada(xp, 1, Instant.parse("2026-10-01T10:00:00Z"))),
        recordes = Recordes(),
    )
}
