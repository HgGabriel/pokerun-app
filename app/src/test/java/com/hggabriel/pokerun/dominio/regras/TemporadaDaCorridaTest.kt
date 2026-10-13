package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Os casos de RN-40 que docs/06 §3 exige: a corrida credita na temporada em que
 * **aconteceu**, e a fronteira é a do fuso da temporada.
 *
 * O erro que a regra existe para impedir é tirar o ano do instante em UTC: a corrida
 * de 31/12 às 23h em São Paulo já é 01/01 lá, e cairia na temporada seguinte.
 */
class TemporadaDaCorridaTest {

    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")

    // -----------------------------------------------------------------------
    // RN-40 — a temporada é a da data, no fuso da temporada
    // -----------------------------------------------------------------------

    @Test
    fun `corrida de 31-12 as 23h em Sao Paulo, que em UTC ja e 01-01, e kanto-2026`() {
        val instante = emSaoPaulo(2026, 12, 31, 23, 0)
        // A premissa do caso: em UTC a corrida já é de 2027.
        assertEquals(2027, instante.atZone(ZoneId.of("UTC")).year)

        assertEquals("kanto-2026", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `corrida de 01-01 as 00h30 em Sao Paulo e da temporada seguinte`() {
        val instante = emSaoPaulo(2027, 1, 1, 0, 30)

        assertEquals("johto-2027", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `a meia-noite da virada ja e da temporada seguinte`() {
        // Fim exclusivo, como o data_fim das semanas (decisão nº 100).
        val instante = emSaoPaulo(2027, 1, 1, 0, 0)

        assertEquals("johto-2027", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `corrida sem temporada publicada que a cubra fica sem temporada`() {
        val instante = emSaoPaulo(2027, 1, 1, 0, 30)

        assertNull(temporadaDe(instante, listOf(kanto())))
    }

    @Test
    fun `a temporada e a da data e nao a ativa`() {
        // Na virada, johto já é a ativa e a corrida de 31/12 ainda é de Kanto.
        val temporadas = listOf(kanto().copy(ativa = false), johto().copy(ativa = true))

        assertEquals("kanto-2026", temporadaDe(emSaoPaulo(2026, 12, 31, 23, 0), temporadas)?.id)
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private fun emSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto).atZone(saoPaulo).toInstant()

    private fun kanto() = Temporada(
        id = "kanto-2026",
        nome = "Kanto 2026",
        regiao = "Kanto",
        inicio = LocalDate.of(2026, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fim = LocalDate.of(2027, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ativa = true,
        totalEspecies = 150,
        tiers = emptyList(),
    )

    private fun johto() = Temporada(
        id = "johto-2027",
        nome = "Johto 2027",
        regiao = "Johto",
        inicio = LocalDate.of(2027, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fim = LocalDate.of(2028, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ativa = false,
        totalEspecies = 100,
        tiers = emptyList(),
    )

    private fun temporadas() = listOf(kanto(), johto())
}
