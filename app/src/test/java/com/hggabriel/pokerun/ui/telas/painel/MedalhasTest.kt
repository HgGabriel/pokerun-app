package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.regras.Recordes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * As fitas dos recordes (decisões 10 e 18): o valor de `agregados/records`, e o dia e a espécie
 * de cada recorde recalculados no aparelho, em ordem, pelo mesmo motor do replay.
 *
 * O aceite da F4: as duas corridas reais dão `8:00 /km` em 15/10 e `10 km` em 11/10, com as
 * espécies da época.
 */
class MedalhasTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p" to fuso)

    /** Escada de teste: 30 espécies de 100 XP, números 101 a 130. */
    private val kanto = Temporada(
        "kanto-2026", "Kanto 2026", "kanto",
        Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2027-01-01T03:00:00Z"),
        fuso, true, 30, listOf(Tier(1, "T1", "", (101..130).toList(), 100)),
    )

    private val reais = listOf(
        corrida("a", "2026-10-11T09:00", 10.0, 6600, xp = 2400),
        corrida("b", "2026-10-15T19:30", 3.0, 1440, xp = 300),
    )
    private val gravados = Recordes(melhorPaceSegKm = 480.0, maiorDistanciaKm = 10.0, maiorDuracaoSeg = 6600)

    @Test
    fun `as cinco fitas saem na ordem da tela, cada uma com a sua arte`() {
        val fitas = fitasDosRecordes(reais, gravados, listOf(kanto), fusos)

        assertEquals(FitaDeRecorde.entries, fitas.map { it.fita })
        assertEquals("footprint-ribbon", FitaDeRecorde.MAIOR_DISTANCIA.arquivo)
        assertEquals("effort-ribbon", FitaDeRecorde.MAIOR_DURACAO.arquivo)
        assertEquals("record-ribbon", FitaDeRecorde.MELHOR_PACE.arquivo)
        assertEquals("royal-ribbon", FitaDeRecorde.MELHOR_1K.arquivo)
        assertEquals("champion-ribbon", FitaDeRecorde.MELHOR_5K.arquivo)
        assertTrue(FitaDeRecorde.MELHOR_5K.url.endsWith("/sprites/ribbons/generation-viii/champion-ribbon.png"))
    }

    @Test
    fun `o pace de 15 de outubro e a distancia de 11 de outubro, com as especies da epoca`() {
        val fitas = fitasDosRecordes(reais, gravados, listOf(kanto), fusos).associateBy { it.fita }

        val distancia = fitas.getValue(FitaDeRecorde.MAIOR_DISTANCIA)
        assertEquals(10.0, distancia.valor!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 11), distancia.dia)
        // 2.400 XP depois da primeira corrida: posição 24, número 124.
        assertEquals(124, distancia.especie)

        val pace = fitas.getValue(FitaDeRecorde.MELHOR_PACE)
        assertEquals(480.0, pace.valor!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 15), pace.dia)
        // 2.700 XP depois da segunda: posição 27, número 127.
        assertEquals(127, pace.especie)

        assertEquals(LocalDate.of(2026, 10, 11), fitas.getValue(FitaDeRecorde.MAIOR_DURACAO).dia)
    }

    @Test
    fun `a fita vaga nao tem valor, dia nem especie`() {
        val vaga = fitasDosRecordes(reais, gravados, listOf(kanto), fusos).first { it.fita == FitaDeRecorde.MELHOR_1K }

        assertTrue(vaga.vaga)
        assertNull(vaga.dia)
        assertNull(vaga.especie)
    }

    @Test
    fun `o dia e o da ultima corrida que bateu o recorde, e a descartada nao conta`() {
        val corridas = reais +
            corrida("c", "2026-10-20T09:00", 12.0, 7200, xp = 100) +
            corrida("d", "2026-10-22T09:00", 20.0, 9000, xp = 100).copy(descartada = true)

        val distancia = fitasDosRecordes(corridas, gravados.copy(maiorDistanciaKm = 12.0), listOf(kanto), fusos)
            .first { it.fita == FitaDeRecorde.MAIOR_DISTANCIA }

        assertEquals(LocalDate.of(2026, 10, 20), distancia.dia)
    }

    @Test
    fun `sem temporada no catalogo a fita tem dia mas nao tem especie`() {
        val distancia = fitasDosRecordes(reais, gravados, emptyList(), fusos).first { it.fita == FitaDeRecorde.MAIOR_DISTANCIA }

        assertEquals(LocalDate.of(2026, 10, 11), distancia.dia)
        assertNull(distancia.especie)
    }

    @Test
    fun `a porta mostra a fita conquistada mais recente`() {
        val fitas = fitasDosRecordes(reais, gravados, listOf(kanto), fusos)

        assertEquals(FitaDeRecorde.MELHOR_PACE, fitaDaPorta(fitas)?.fita)
        assertEquals(FitaDeRecorde.MAIOR_DISTANCIA, fitaDaPorta(fitasDosRecordes(emptyList(), null, emptyList(), fusos))?.fita)
    }

    private fun corrida(id: String, quando: String, km: Double, duracao: Long, xp: Long) = Corrida(
        id = id,
        dataHoraInicio = ZonedDateTime.of(LocalDateTime.parse(quando), fuso).toInstant(),
        km = km,
        duracaoSeg = duracao,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = null,
        temporadaId = "kanto-2026",
        xpCreditado = xp,
    )
}
