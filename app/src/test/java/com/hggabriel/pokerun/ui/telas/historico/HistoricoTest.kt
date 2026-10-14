package com.hggabriel.pokerun.ui.telas.historico

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * O histórico da `RunHistoryScreen` (`F2-T11`, docs/03 §3.15): agrupado por mês, do mais
 * recente para o mais antigo, sem descartada nem substituída, com o filtro do plano
 * atual, e o mês no fuso do plano (RN-28).
 */
class HistoricoTest {

    @Test
    fun `agrupa por mes, do mais recente para o mais antigo`() {
        val meses = historico(
            listOf(corrida("a", "2026-08-20T10:00:00Z"), corrida("b", "2026-09-14T10:00:00Z"), corrida("c", "2026-09-02T10:00:00Z")),
            listOf(plano("p1")),
            "p1",
            FiltroDoHistorico.TODAS,
        )

        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8)), meses.map { it.mes })
        assertEquals(listOf("b", "c"), meses.first().corridas.map { it.id })
    }

    @Test
    fun `descartada e substituida nao aparecem`() {
        val meses = historico(
            listOf(
                corrida("v", "2026-09-14T10:00:00Z"),
                corrida("d", "2026-09-13T10:00:00Z").copy(descartada = true),
                corrida("s", "2026-09-12T10:00:00Z").copy(substituida = true),
            ),
            listOf(plano("p1")),
            "p1",
            FiltroDoHistorico.TODAS,
        )

        assertEquals(listOf("v"), meses.flatMap { it.corridas }.map { it.id })
    }

    @Test
    fun `plano atual mostra so as corridas do plano ativo`() {
        val corridas = listOf(corrida("a", "2026-09-14T10:00:00Z", "p1"), corrida("b", "2026-09-13T10:00:00Z", "p2"))
        val planos = listOf(plano("p1"), plano("p2"))

        assertEquals(listOf("a"), historico(corridas, planos, "p1", FiltroDoHistorico.PLANO_ATUAL).flatMap { it.corridas }.map { it.id })
        assertTrue(historico(corridas, planos, null, FiltroDoHistorico.PLANO_ATUAL).isEmpty())
    }

    @Test
    fun `o mes e o do fuso do plano`() {
        // 01/09 às 01h em UTC ainda é 31/08 às 22h em São Paulo.
        val meses = historico(listOf(corrida("a", "2026-09-01T01:00:00Z")), listOf(plano("p1")), "p1", FiltroDoHistorico.TODAS)

        assertEquals(YearMonth.of(2026, 8), meses.single().mes)
    }

    private fun corrida(id: String, em: String, planoId: String = "p1") = Corrida(
        id = id,
        dataHoraInicio = Instant.parse(em),
        km = 5.0,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = planoId,
        semanaRef = 1,
        temporadaId = "kanto-2026",
    )

    private fun plano(id: String) = Plano(
        id = id,
        nome = "Plano $id",
        distanciaAlvoKm = 15.0,
        dataProva = Instant.parse("2026-12-31T11:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(Instant.parse("2026-12-31T11:00:00Z"), 15.0, 5.0, 3),
    )
}
