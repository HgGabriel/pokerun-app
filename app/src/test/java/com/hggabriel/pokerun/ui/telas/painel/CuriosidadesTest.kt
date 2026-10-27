package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * O Você sabia (estudo de Progresso e Perfil, §c e decisão 20): um fato por vez, calculado
 * de `runs` e `xp_detalhe`, e a marca de horário dos jogos na faixa preferida.
 */
class CuriosidadesTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p" to fuso)

    @Test
    fun `com as duas corridas reais a maior responde por 77 por cento dos km`() {
        val fatos = curiosidades(listOf(corrida("a", "2026-10-11T09:00", 10.0, 2400), corrida("b", "2026-10-15T19:30", 3.0, 300)), fusos)

        assertEquals(Curiosidade.ParteDaMaior(77, 10.0, LocalDate.of(2026, 10, 11)), fatos.first())
    }

    @Test
    fun `dia e horario so entram quando o preferido tem duas corridas`() {
        val duas = curiosidades(listOf(corrida("a", "2026-10-11T09:00", 10.0), corrida("b", "2026-10-15T19:30", 3.0)), fusos)
        assertTrue(duas.none { it is Curiosidade.DiaPreferido || it is Curiosidade.HorarioPreferido })

        val tres = curiosidades(
            listOf(
                corrida("a", "2026-10-11T20:30", 10.0),
                corrida("b", "2026-10-18T21:00", 5.0),
                corrida("c", "2026-10-15T07:00", 3.0),
            ),
            fusos,
        )
        assertTrue(Curiosidade.DiaPreferido(DayOfWeek.SUNDAY, 2, 3) in tres)
        assertTrue(Curiosidade.HorarioPreferido(FaixaDoDia.TARDE_DA_NOITE, 2, 3) in tres)
    }

    @Test
    fun `a corrida que mais deu XP vira fato mesmo sozinha`() {
        val fatos = curiosidades(listOf(corrida("a", "2026-10-11T09:00", 10.0, 2400)), fusos)

        assertEquals(listOf(Curiosidade.MaiorXp(2400, 10.0, LocalDate.of(2026, 10, 11))), fatos)
    }

    @Test
    fun `a parte do multiplicador e o que passou de um vezes`() {
        // (100 + 0 + 0) × 1,5 = 150; o multiplicador deu 50 dos 150: 33%.
        val fatos = curiosidades(
            listOf(corrida("a", "2026-10-11T09:00", 1.0, 150, ParcelasDeXp(100.0, 0.0, 0, 1.5, 0))),
            fusos,
        )

        assertTrue(Curiosidade.ParteDoMultiplicador(33) in fatos)
    }

    @Test
    fun `sem multiplicador e sem corrida nao ha fato inventado`() {
        assertTrue(curiosidades(emptyList(), fusos).isEmpty())
        val fatos = curiosidades(listOf(corrida("a", "2026-10-11T09:00", 1.0, 100)), fusos)
        assertTrue(fatos.none { it is Curiosidade.ParteDoMultiplicador })
    }

    @Test
    fun `as quatro faixas com marca de horario sao as dos jogos`() {
        assertEquals("dawn-mark", FaixaDoDia.daHora(6).marca)
        assertEquals("lunchtime-mark", FaixaDoDia.daHora(12).marca)
        assertEquals("dusk-mark", FaixaDoDia.daHora(18).marca)
        assertEquals("sleepy-time-mark", FaixaDoDia.daHora(22).marca)
        assertNull(FaixaDoDia.daHora(10).marca)
        assertNull(FaixaDoDia.daHora(15).marca)
        assertNull(FaixaDoDia.daHora(19).marca)
        assertEquals(FaixaDoDia.CEDO, FaixaDoDia.daHora(0))
        assertEquals(FaixaDoDia.TARDE_DA_NOITE, FaixaDoDia.daHora(23))
    }

    private fun corrida(
        id: String,
        quando: String,
        km: Double,
        xp: Long = 100,
        detalhe: ParcelasDeXp = ParcelasDeXp(km * 100, 0.0, 0, 1.0, 0),
    ) = Corrida(
        id = id,
        dataHoraInicio = ZonedDateTime.of(LocalDateTime.parse(quando), fuso).toInstant(),
        km = km,
        duracaoSeg = (km * 480).toLong(),
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = null,
        temporadaId = "kanto-2026",
        xpCreditado = xp,
        xpDetalhe = detalhe,
    )
}
