package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * O heatmap de calendário (`F3-T02`, docs/03 §3.17, RN-16): a grade anual, sempre
 * visível, que atravessa planos. Os limites da legenda são fixos (decisão nº 133).
 */
class HeatmapTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p1" to saoPaulo, "p2" to saoPaulo)

    @Test
    fun `a grade sem corrida ainda tem o ano inteiro, de segunda a domingo`() {
        val grade = gradeDoHeatmap(emptyList(), fusos, 2026)

        // 1º de janeiro de 2026 é quinta: a primeira coluna começa três dias antes do ano.
        assertEquals(53, grade.colunas.size)
        assertTrue(grade.colunas.all { it.size == 7 })
        assertEquals(listOf(null, null, null), grade.colunas.first().take(3))
        assertEquals(LocalDate.of(2026, 1, 1), grade.colunas.first()[3]?.data)
        assertEquals(DayOfWeek.THURSDAY, LocalDate.of(2026, 1, 1).dayOfWeek)
        assertEquals(LocalDate.of(2026, 12, 31), grade.colunas.last()[3]?.data)
        assertNull(grade.colunas.last()[4])
        assertEquals(365, grade.dias.size)
        assertEquals(0, grade.diasComCorrida)
        assertTrue(grade.dias.all { it.degrau == 0 })
    }

    @Test
    fun `duas corridas no mesmo dia somam os quilometros`() {
        val grade = gradeDoHeatmap(
            listOf(corrida("a", "2026-09-14T09:00:00-03:00", 5.0), corrida("b", "2026-09-14T18:00:00-03:00", 7.4)),
            fusos,
            2026,
        )

        val dia = grade.dias.single { it.corridas > 0 }
        assertEquals(LocalDate.of(2026, 9, 14), dia.data)
        assertEquals(12.4, dia.km, 1e-9)
        assertEquals(2, dia.corridas)
        assertEquals(1, grade.diasComCorrida)
    }

    @Test
    fun `descartada e substituida nao acendem o dia`() {
        val grade = gradeDoHeatmap(
            listOf(
                corrida("a", "2026-09-14T09:00:00-03:00", 5.0, descartada = true),
                corrida("b", "2026-09-15T09:00:00-03:00", 5.0, substituida = true),
            ),
            fusos,
            2026,
        )

        assertEquals(0, grade.diasComCorrida)
    }

    @Test
    fun `o dia e o do fuso do plano da corrida, e plano desconhecido cai em Sao Paulo`() {
        // 23h30 de 31/08 em São Paulo já é 1º/09 em Tóquio.
        val noite = "2026-09-01T02:30:00Z"
        val emToquio = gradeDoHeatmap(listOf(corrida("a", noite, 5.0, plano = "p1")), mapOf("p1" to ZoneId.of("Asia/Tokyo")), 2026)
        val semPlano = gradeDoHeatmap(listOf(corrida("a", noite, 5.0, plano = "p9")), fusos, 2026)

        assertEquals(LocalDate.of(2026, 9, 1), emToquio.dias.single { it.corridas > 0 }.data)
        assertEquals(LocalDate.of(2026, 8, 31), semPlano.dias.single { it.corridas > 0 }.data)
    }

    @Test
    fun `atravessa planos e deixa de fora o que e de outro ano`() {
        val grade = gradeDoHeatmap(
            listOf(
                corrida("a", "2026-03-02T07:00:00-03:00", 5.0, plano = "p1"),
                corrida("b", "2026-03-04T07:00:00-03:00", 5.0, plano = "p2"),
                corrida("c", "2025-12-31T07:00:00-03:00", 5.0, plano = "p1"),
            ),
            fusos,
            2026,
        )

        assertEquals(2, grade.diasComCorrida)
        assertEquals(10.0, grade.kmNoAno, 1e-9)
    }

    @Test
    fun `os degraus seguem os limites fixos de 5, 10 e 15 km`() {
        assertEquals(listOf(5, 10, 15), LIMITES_DO_HEATMAP_KM)
        assertEquals(0, degrauDoDia(0.0, 0))
        assertEquals(1, degrauDoDia(0.4, 1))
        assertEquals(1, degrauDoDia(4.99, 1))
        assertEquals(2, degrauDoDia(5.0, 1))
        assertEquals(2, degrauDoDia(9.99, 1))
        assertEquals(3, degrauDoDia(10.0, 1))
        assertEquals(3, degrauDoDia(14.99, 1))
        assertEquals(4, degrauDoDia(15.0, 1))
        assertEquals(4, degrauDoDia(42.2, 1))
    }

    @Test
    fun `o toque na coluna resolve para o dia valido mais proximo`() {
        val grade = gradeDoHeatmap(emptyList(), fusos, 2026)

        // Segunda antes do ano: o dia mais próximo da coluna é 1º de janeiro.
        assertEquals(LocalDate.of(2026, 1, 1), diaDaColuna(grade, 0, 0)?.data)
        // Domingo da última coluna cai fora do ano: o mais próximo é 31 de dezembro.
        assertEquals(LocalDate.of(2026, 12, 31), diaDaColuna(grade, 52, 6)?.data)
        // Toque dentro de uma coluna cheia devolve a linha tocada.
        assertEquals(LocalDate.of(2026, 1, 5), diaDaColuna(grade, 1, 0)?.data)
        assertEquals(LocalDate.of(2026, 1, 11), diaDaColuna(grade, 1, 6)?.data)
        // Coluna e linha fora da grade são cortadas, não erro.
        assertNotNull(diaDaColuna(grade, 999, -4))
    }

    @Test
    fun `o resumo falado traz os doze meses com dias e quilometros`() {
        val grade = gradeDoHeatmap(
            listOf(
                corrida("a", "2026-01-10T07:00:00-03:00", 5.0),
                corrida("b", "2026-01-12T07:00:00-03:00", 6.0),
                corrida("c", "2026-03-01T07:00:00-03:00", 10.0),
            ),
            fusos,
            2026,
        )

        val meses = resumoDosMeses(grade)

        assertEquals((1..12).toList(), meses.map { it.mes })
        assertEquals(2, meses[0].diasComCorrida)
        assertEquals(11.0, meses[0].km, 1e-9)
        assertEquals(0, meses[1].diasComCorrida)
        assertEquals(1, meses[2].diasComCorrida)
    }

    private fun corrida(
        id: String,
        inicio: String,
        km: Double,
        plano: String = "p1",
        descartada: Boolean = false,
        substituida: Boolean = false,
    ) = Corrida(
        id = id,
        dataHoraInicio = java.time.OffsetDateTime.parse(inicio).toInstant(),
        km = km,
        duracaoSeg = (km * 360).toLong(),
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = plano,
        semanaRef = 1,
        temporadaId = "kanto-2026",
        descartada = descartada,
        substituida = substituida,
    )
}
