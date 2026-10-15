package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.regras.Recordes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A `StatsDashboardScreen` (`F3-T09`, docs/03 §3.17): "todo bloco declara um mínimo de
 * dado, e abaixo dele renderiza a distância que falta". O painel monta os cinco blocos
 * mesmo sem nenhuma corrida, e cada um responde por si.
 */
class PainelTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val hoje = LocalDate.of(2026, 10, 15)

    @Test
    fun `sem corrida nenhuma o painel existe, com o heatmap e a distancia que falta em cada bloco`() {
        val painel = montarPainel(emptyList(), emptyList(), recordes = null, hoje = hoje)

        assertEquals(2026, painel.heatmap.ano)
        assertEquals(0, painel.heatmap.diasComCorrida)
        assertNull(painel.recordes)
        assertEquals(VolumeAcumulado.Faltam(3), painel.volume)
        assertEquals(PaceMovel.Faltam(8), painel.pace)
        assertEquals(EficienciaCardiaca.Faltam(8), painel.eficiencia)
        assertEquals(saoPaulo, painel.fuso)
    }

    @Test
    fun `os blocos leem o mesmo historico, entre planos`() {
        val corridas = (0..3).flatMap { semana ->
            listOf(
                corrida("a$semana", semana, plano = "p1"),
                corrida("b$semana", semana, plano = "p2", dia = 2),
            )
        }
        val recordes = Recordes(maiorDistanciaKm = 8.0)

        val painel = montarPainel(corridas, listOf(plano("p1"), plano("p2")), recordes, hoje)

        assertEquals(8, painel.heatmap.diasComCorrida)
        assertEquals(recordes, painel.recordes)
        assertTrue(painel.volume is VolumeAcumulado.Serie)
        assertTrue(painel.pace is PaceMovel.Serie)
    }

    @Test
    fun `o ano do heatmap e o ano de hoje`() {
        val painel = montarPainel(emptyList(), emptyList(), null, LocalDate.of(2027, 1, 3))

        assertEquals(2027, painel.heatmap.ano)
    }

    @Test
    fun `o fuso do eixo e o do plano da corrida mais recente`() {
        val corridas = listOf(corrida("a", 0, plano = "p1"), corrida("b", 1, plano = "p2"))
        val planos = listOf(plano("p1"), plano("p2", fuso = ZoneId.of("America/Manaus")))

        val painel = montarPainel(corridas, planos, null, hoje)

        assertEquals(ZoneId.of("America/Manaus"), painel.fuso)
    }

    private fun corrida(id: String, semana: Int, plano: String, dia: Int = 1) = Corrida(
        id = id,
        dataHoraInicio = LocalDate.of(2026, 9, 21).plusWeeks(semana.toLong()).plusDays(dia.toLong())
            .atTime(8, 0).atZone(saoPaulo).toInstant(),
        km = 5.0,
        duracaoSeg = 1650,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = plano,
        semanaRef = 1,
        temporadaId = "kanto-2026",
    )

    private fun plano(id: String, fuso: ZoneId = saoPaulo) = Plano(
        id = id,
        nome = "Plano $id",
        distanciaAlvoKm = 15.0,
        dataProva = Instant.parse("2026-12-31T11:00:00Z"),
        fuso = fuso,
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(Instant.parse("2026-12-31T11:00:00Z"), 15.0, 5.0, 3),
    )
}
