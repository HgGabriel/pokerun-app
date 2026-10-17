package com.hggabriel.pokerun.ui.componentes

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** O dia que o carimbo escreve (docs/03 §3.3.1): o da corrida, no fuso do plano (RN-28). */
class DiaDoCarimboTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `o dia sai no fuso do plano, e nao em UTC`() {
        // Domingo 22h em São Paulo já é segunda em UTC. O carimbo diz domingo.
        val corrida = corrida("run-1", "2026-08-31T01:00:00Z")
        val segmentos = listOf(SegmentoDaSemana(SessaoReivindicada.Curta(1), "run-1", 5.0))

        val comDia = comDias(segmentos, listOf(corrida), saoPaulo)

        assertEquals(LocalDate.of(2026, 8, 30), comDia.single().dia)
    }

    @Test
    fun `a vaga pendente fica sem dia`() {
        val segmentos = listOf(SegmentoDaSemana(SessaoReivindicada.Longao))

        assertNull(comDias(segmentos, emptyList(), saoPaulo).single().dia)
    }

    private fun corrida(id: String, inicio: String) = Corrida(
        id = id,
        dataHoraInicio = Instant.parse(inicio),
        km = 5.0,
        duracaoSeg = 1_800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-1",
        semanaRef = 1,
        temporadaId = "kanto-2026",
    )
}
