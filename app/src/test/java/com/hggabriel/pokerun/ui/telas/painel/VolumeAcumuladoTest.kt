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
 * Volume semanal acumulado (`F3-T04`, docs/03 §3.17, decisão nº 134). As semanas são de
 * calendário, de segunda a domingo, e as datas dos testes partem da segunda 3/8/2026.
 */
class VolumeAcumuladoTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p1" to saoPaulo)
    private val segunda = LocalDate.of(2026, 8, 3)

    // ---------------------------------------------------------------------
    // Volume semanal acumulado
    // ---------------------------------------------------------------------

    @Test
    fun `com menos de 3 semanas de treino o bloco diz quantas faltam`() {
        val duas = listOf(corrida("a", semana = 0, km = 10.0), corrida("b", semana = 2, km = 8.0))

        assertEquals(VolumeAcumulado.Faltam(1), volumeAcumulado(duas, listOf(plano("p1")), hoje(2)))
        assertEquals(VolumeAcumulado.Faltam(3), volumeAcumulado(emptyList(), emptyList(), hoje(2)))
    }

    @Test
    fun `duas corridas na mesma semana contam como uma semana de treino`() {
        val corridas = listOf(corrida("a", 0, 5.0), corrida("b", 0, 6.0, dia = 3), corrida("c", 1, 7.0))

        assertEquals(VolumeAcumulado.Faltam(1), volumeAcumulado(corridas, listOf(plano("p1")), hoje(1)))
    }

    @Test
    fun `o acumulado e a soma corrente e a semana parada nao quebra a linha`() {
        val corridas = listOf(
            corrida("a", 0, 10.0),
            corrida("b", 1, 8.0),
            corrida("c", 3, 7.0),
        )

        val serie = volumeAcumulado(corridas, listOf(plano("p1")), hoje(3)) as VolumeAcumulado.Serie

        assertEquals(listOf(10.0, 18.0, 18.0, 25.0), serie.pontos.map { it.kmAcumulado })
        assertEquals(listOf(10.0, 8.0, 0.0, 7.0), serie.pontos.map { it.kmDaSemana })
        assertEquals(segunda, serie.pontos.first().inicioDaSemana)
    }

    @Test
    fun `a linha segue ate a semana de hoje, mesmo sem corrida nela`() {
        val corridas = listOf(corrida("a", 0, 10.0), corrida("b", 1, 8.0), corrida("c", 2, 7.0))

        val serie = volumeAcumulado(corridas, listOf(plano("p1")), hoje(4)) as VolumeAcumulado.Serie

        assertEquals(5, serie.pontos.size)
        assertEquals(25.0, serie.pontos.last().kmAcumulado, 1e-9)
    }

    @Test
    fun `descartada e substituida nao somam`() {
        val corridas = listOf(
            corrida("a", 0, 10.0),
            corrida("b", 1, 8.0),
            corrida("c", 2, 7.0),
            corrida("d", 2, 100.0).copy(descartada = true),
            corrida("e", 2, 100.0).copy(substituida = true),
        )

        val serie = volumeAcumulado(corridas, listOf(plano("p1")), hoje(2)) as VolumeAcumulado.Serie

        assertEquals(25.0, serie.pontos.last().kmAcumulado, 1e-9)
    }

    @Test
    fun `o domingo ainda e da semana que comecou na segunda`() {
        val corridas = listOf(
            corrida("a", 0, 10.0, dia = 6), // domingo 9/8
            corrida("b", 1, 8.0),
            corrida("c", 2, 7.0),
        )

        val serie = volumeAcumulado(corridas, listOf(plano("p1")), hoje(2)) as VolumeAcumulado.Serie

        assertEquals(10.0, serie.pontos.first().kmDaSemana, 1e-9)
    }

    @Test
    fun `a prova concluida marca a sua semana, e a prova futura ou sem corrida nao`() {
        val corridas = (0..3).map { corrida("c$it", it, 10.0) }
        val concluida = plano("p1", prova = segunda.plusWeeks(2).plusDays(5))
        val futura = plano("p2", prova = segunda.plusWeeks(40))
        val semCorrida = plano("p3", prova = segunda.plusWeeks(1))

        val serie = volumeAcumulado(corridas, listOf(concluida, futura, semCorrida), hoje(3)) as VolumeAcumulado.Serie

        assertEquals(setOf(2), serie.marcadores)
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
        parametros = ParametrosDeGeracao(Instant.parse("2026-12-31T11:00:00Z"), 15.0, 5.0, 3),
    )
}
