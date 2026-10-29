package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** O que o replay copia em `members/{uid}` para o cartão do treinador. */
class VitrineTest {

    @Test
    fun `soma os km e marca a primeira e a ultima corrida`() {
        val vitrine = vitrineDe(
            listOf(
                corrida("b", "2026-10-15T22:00:00Z", 3.0),
                corrida("a", "2026-10-11T09:00:00Z", 10.0),
            ),
        )

        assertEquals(13.0, vitrine.naVidaKm, 0.001)
        assertEquals(Instant.parse("2026-10-11T09:00:00Z"), vitrine.primeiraCorrida)
        assertEquals(Instant.parse("2026-10-15T22:00:00Z"), vitrine.ultimaCorrida)
    }

    @Test
    fun `corrida descartada ou substituida nao conta`() {
        val vitrine = vitrineDe(
            listOf(
                corrida("a", "2026-10-01T09:00:00Z", 5.0).copy(descartada = true),
                corrida("b", "2026-10-20T09:00:00Z", 6.0).copy(substituida = true),
                corrida("c", "2026-10-03T09:00:00Z", 7.0),
            ),
        )

        assertEquals(7.0, vitrine.naVidaKm, 0.001)
        assertEquals(Instant.parse("2026-10-03T09:00:00Z"), vitrine.primeiraCorrida)
        assertEquals(Instant.parse("2026-10-03T09:00:00Z"), vitrine.ultimaCorrida)
    }

    @Test
    fun `sem corrida a vitrine fica vazia`() {
        val vitrine = vitrineDe(emptyList())

        assertEquals(0.0, vitrine.naVidaKm, 0.0)
        assertNull(vitrine.primeiraCorrida)
        assertNull(vitrine.ultimaCorrida)
    }

    private fun corrida(id: String, quando: String, km: Double) = Corrida(
        id = id, dataHoraInicio = Instant.parse(quando), km = km, duracaoSeg = 1800, tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.entries.first(), planoId = "p", semanaRef = null, temporadaId = "kanto-2026",
    )
}
