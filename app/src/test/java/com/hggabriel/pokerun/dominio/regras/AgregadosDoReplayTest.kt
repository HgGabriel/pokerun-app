package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Os agregados que o replay grava (`F2-T08`, docs/05 §1 e §2.4), escritos antes da
 * implementação: o `weekly` completo, o `historico` da temporada e os recordes
 * vitalícios (RN-39).
 *
 * Mesma montagem de `ReplayTest`: três semanas de 3 sessões e longão de 10 km, e uma
 * temporada de 20 espécies (10 a 100 XP, 10 a 250 XP).
 */
class AgregadosDoReplayTest {

    // -----------------------------------------------------------------------
    // weekly
    // -----------------------------------------------------------------------

    @Test
    fun `o weekly conta sessoes com teto, soma km, guarda a maior e o XP da semana`() {
        val corridas = listOf(
            corrida("a", km = 5.0, duracaoSeg = 1800, em = dia(0)),
            corrida("b", km = 9.0, duracaoSeg = 3240, em = dia(1)),
            corrida("c", km = 4.0, duracaoSeg = 1440, em = dia(2)),
            corrida("d", km = 3.0, duracaoSeg = 1080, em = dia(3)),
        )
        val resultado = recalcular(corridas)

        val semana = escritas(corridas, resultado, ate = dia(30)).semanas.getValue("p1_1")

        assertEquals("p1", semana.planoId)
        assertEquals(3, semana.sessoesFeitas)
        assertEquals(21.0, semana.kmFeito, 1e-9)
        assertEquals(9.0, semana.longaoFeito, 1e-9)
        assertEquals(100.0, semana.aderenciaPct, 1e-9)
        assertEquals(corridas.sumOf { resultado.porCorrida.getValue(it.id).total }, semana.xpSemana)
    }

    @Test
    fun `a aderencia sai em porcentagem`() {
        val corridas = listOf(corrida("a", km = 5.0, duracaoSeg = 1800, em = dia(0)))

        val semana = escritas(corridas, recalcular(corridas), ate = dia(30)).semanas.getValue("p1_1")

        assertEquals(100.0 / 3, semana.aderenciaPct, 1e-9)
    }

    @Test
    fun `a semana e fechada quando o fim dela ja passou`() {
        val corridas = listOf(corrida("a", km = 5.0, duracaoSeg = 1800, em = dia(0)))
        val resultado = recalcular(corridas)

        assertTrue(escritas(corridas, resultado, ate = dia(30)).semanas.getValue("p1_1").fechada)
        assertFalse(escritas(corridas, resultado, ate = dia(3)).semanas.getValue("p1_1").fechada)
    }

    @Test
    fun `a semana que so tem corrida descartada e gravada zerada`() {
        val corridas = listOf(corrida("x", km = 5.0, duracaoSeg = 1800, em = dia(0), descartada = true))

        val semana = escritas(corridas, recalcular(corridas), ate = dia(30)).semanas.getValue("p1_1")

        assertEquals(0, semana.sessoesFeitas)
        assertEquals(0.0, semana.kmFeito, 1e-9)
        assertEquals(0.0, semana.longaoFeito, 1e-9)
        assertEquals(0.0, semana.aderenciaPct, 1e-9)
        assertEquals(0L, semana.xpSemana)
    }

    // -----------------------------------------------------------------------
    // historico da temporada
    // -----------------------------------------------------------------------

    @Test
    fun `o historico guarda a posicao no fim de cada semana fechada`() {
        // 9 km sozinha na semana 1: (900 + 150) × 2,0 = 2.100 XP, posição 14.
        // A semana 2 não tem corrida e repete a posição. A 3 ainda está aberta.
        val corridas = listOf(
            corrida("a", km = 9.0, duracaoSeg = 3240, em = dia(0)),
            corrida("b", km = 3.0, duracaoSeg = 1080, em = dia(15)),
        )

        val historico = Replay.historico(temporada, corridas, recalcular(corridas), mapOf("p1" to grade), ate = dia(16))

        assertEquals(listOf(MarcoDaSemana("p1_1", 14), MarcoDaSemana("p1_2", 14)), historico)
    }

    @Test
    fun `o historico ignora corrida descartada`() {
        val corridas = listOf(
            corrida("a", km = 9.0, duracaoSeg = 3240, em = dia(0), descartada = true),
        )

        val historico = Replay.historico(temporada, corridas, recalcular(corridas), mapOf("p1" to grade), ate = dia(8))

        assertEquals(listOf(MarcoDaSemana("p1_1", 1)), historico)
    }

    // -----------------------------------------------------------------------
    // RN-39 — recordes são vitalícios
    // -----------------------------------------------------------------------

    @Test
    fun `os recordes atravessam a virada da temporada`() {
        val corridas = listOf(
            corrida("a", km = 10.0, duracaoSeg = 3600, em = dia(0)),
            corrida("b", km = 8.0, duracaoSeg = 2880, em = dia(8), temporada = "kanto-2027"),
        )

        val resultado = Replay.recalcular(corridas, mapOf("p1" to grade), listOf(temporada, temporada.copy(id = "kanto-2027")))

        assertEquals(10.0, resultado.recordes.maiorDistanciaKm!!, 1e-9)
        assertEquals(3600L, resultado.recordes.maiorDuracaoSeg)
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private val inicio = Instant.parse("2026-08-10T03:00:00Z")

    private fun dia(n: Int): Instant = inicio.plusSeconds(n * 86_400L + 7 * 3_600L)

    private fun semanaInicio(numero: Int): Instant = inicio.plusSeconds((numero - 1) * 7 * 86_400L)

    private val grade: List<Semana> = (1..3).map { n ->
        Semana(
            numero = n,
            dataInicio = semanaInicio(n),
            dataFim = semanaInicio(n + 1),
            sessoesAlvo = 3,
            kmAlvo = 20.0,
            longaoKm = 10.0,
            tipo = TipoDeSemana.BUILD,
            parcial = false,
        )
    }

    private val temporada = Temporada(
        id = "kanto-2026",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 20,
        tiers = listOf(
            Tier(ordem = 1, nome = "Um", descricao = "", especies = (1..10).toList(), xpPorEspecie = 100),
            Tier(ordem = 2, nome = "Dois", descricao = "", especies = (11..20).toList(), xpPorEspecie = 250),
        ),
    )

    private fun corrida(
        id: String,
        km: Double,
        duracaoSeg: Long,
        em: Instant,
        descartada: Boolean = false,
        temporada: String = "kanto-2026",
    ) = Corrida(
        id = id,
        dataHoraInicio = em,
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = grade.firstOrNull { em >= it.dataInicio && em < it.dataFim }?.numero,
        temporadaId = temporada,
        descartada = descartada,
    )

    private fun recalcular(corridas: List<Corrida>) = Replay.recalcular(corridas, mapOf("p1" to grade), listOf(temporada))

    private fun escritas(corridas: List<Corrida>, resultado: ResultadoDoReplay, ate: Instant) =
        Replay.escritas(corridas, resultado, mapOf("p1" to grade), desde = inicio, ate = ate)
}
