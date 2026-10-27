package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pace em média móvel de 4 semanas (`F3-T05`, docs/03 §3.17, decisão nº 135). As semanas
 * são de calendário, de segunda a domingo, e as datas partem da segunda 3/8/2026.
 */
class PaceMovelTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p1" to saoPaulo)
    private val segunda = LocalDate.of(2026, 8, 3)

    // ---------------------------------------------------------------------
    // Pace em média móvel de 4 semanas
    // ---------------------------------------------------------------------

    @Test
    fun `sem corrida nenhuma faltam as 8 das quatro semanas`() {
        assertEquals(8, (paceEmMediaMovel(emptyList(), fusos, hoje(3)) as PaceMovel.Faltam).corridas)
    }

    @Test
    fun `quatro semanas com duas corridas dao um ponto ponderado pela distancia`() {
        // Cada semana: 1 km a 4:00 e 9 km a 6:00. Média simples dos paces seria 5:00; o
        // ponderado é (240 + 3240) / 10 = 348 s/km.
        val corridas = (0..3).flatMap { semana ->
            listOf(
                corrida("a$semana", semana, 1.0, duracaoSeg = 240),
                corrida("b$semana", semana, 9.0, duracaoSeg = 3240, dia = 2),
            )
        }

        val serie = paceEmMediaMovel(corridas, fusos, hoje(3)) as PaceMovel.Serie

        assertEquals(1, serie.pontos.size)
        assertEquals(348.0, serie.pontos.single().paceSegKm, 1e-9)
        assertEquals(segunda.plusWeeks(3), serie.pontos.single().inicioDaUltimaSemana)
    }

    @Test
    fun `a janela anda uma semana por vez`() {
        val corridas = (0..4).flatMap { semana ->
            val pace = 300L + semana * 10
            listOf(
                corrida("a$semana", semana, 5.0, duracaoSeg = 5 * pace),
                corrida("b$semana", semana, 5.0, duracaoSeg = 5 * pace, dia = 2),
            )
        }

        val serie = paceEmMediaMovel(corridas, fusos, hoje(4)) as PaceMovel.Serie

        assertEquals(2, serie.pontos.size)
        assertEquals(315.0, serie.pontos[0].paceSegKm, 1e-9) // semanas 0 a 3: 300, 310, 320, 330
        assertEquals(325.0, serie.pontos[1].paceSegKm, 1e-9) // semanas 1 a 4
    }

    @Test
    fun `uma semana com so uma corrida impede o ponto, e a conta fala so do que ainda da para correr`() {
        // Semanas 0 a 2 com 2 corridas e a semana corrente (3) com 1: falta 1.
        val corridas = (0..2).flatMap { listOf(corrida("a$it", it, 5.0), corrida("b$it", it, 5.0, dia = 2)) } +
            corrida("c", 3, 5.0)

        assertEquals(1, (paceEmMediaMovel(corridas, fusos, hoje(3)) as PaceMovel.Faltam).corridas)
    }

    @Test
    fun `semana passada sem as duas corridas nao entra na conta do que falta`() {
        // A semana 1 teve 1 corrida e já passou: a janela que a contém não se refaz. A que
        // começa hoje pede 2 por semana nas 4 semanas, 8, menos as 2 de hoje.
        val corridas = listOf(
            corrida("a", 0, 5.0), corrida("b", 0, 5.0, dia = 2),
            corrida("c", 1, 5.0),
            corrida("d", 4, 5.0), corrida("e", 4, 5.0, dia = 2),
        )

        assertEquals(6, (paceEmMediaMovel(corridas, fusos, hoje(4)) as PaceMovel.Faltam).corridas)
    }

    @Test
    fun `descartada nao conta para a janela`() {
        val corridas = (0..3).flatMap {
            listOf(corrida("a$it", it, 5.0), corrida("b$it", it, 5.0, dia = 2))
        } + corrida("x", 3, 5.0).copy(descartada = true)

        val serie = paceEmMediaMovel(corridas, fusos, hoje(3)) as PaceMovel.Serie

        assertEquals(1, serie.pontos.size)
        assertTrue(serie.pontos.single().paceSegKm > 0)
    }

    // ---------------------------------------------------------------------

    private fun hoje(semana: Int): LocalDate = segunda.plusWeeks(semana.toLong()).plusDays(2)

    private fun corrida(
        id: String,
        semana: Int,
        km: Double,
        duracaoSeg: Long = (km * 330).toLong(),
        dia: Int = 1,
        plano: String = "p1",
    ) = Corrida(
        id = id,
        dataHoraInicio = segunda.plusWeeks(semana.toLong()).plusDays(dia.toLong()).atTime(8, 0).atZone(saoPaulo).toInstant(),
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = plano,
        semanaRef = 1,
        temporadaId = "kanto-2026",
    )

    private fun plano(id: String, prova: LocalDate = LocalDate.of(2026, 12, 31)) = Plano(
        id = id,
        nome = "Plano $id",
        distanciaAlvoKm = 15.0,
        dataProva = prova.atTime(8, 0).atZone(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(Instant.parse("2026-12-31T11:00:00Z"), 15.0, 5.0, 3, 2.5),
    )
}
