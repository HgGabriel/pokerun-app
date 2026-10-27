package com.hggabriel.pokerun.ui.telas.perfil

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Os campos da credencial do Perfil (estudo de Progresso e Perfil, Revisão 1 e 3). */
class CredencialTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val kanto = Temporada(
        "kanto-2026", "Kanto 2026", "kanto", Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2027-01-01T03:00:00Z"),
        fuso, true, 150, listOf(Tier(1, "T1", "", (1..150).toList(), 100)),
    )

    @Test
    fun `os dados reais de 27 de outubro`() {
        val dados = dadosDaCredencial(
            corridas = listOf(corrida("a", "2026-10-11T09:00:00Z", 10.0), corrida("b", "2026-10-15T22:00:00Z", 3.0)),
            temporada = kanto,
            colecaoCorrente = 25,
            planoAtivo = plano(Instant.parse("2026-12-31T03:00:00Z")),
            fuso = fuso,
        )

        assertEquals(LocalDate.of(2026, 10, 11), dados.desde)
        assertEquals(13.0, dados.naVidaKm, 0.001)
        assertEquals(25, dados.colecao)
        assertEquals(150, dados.totalEspecies)
        assertEquals(LocalDate.of(2026, 12, 31), dados.prova)
        assertEquals("Kanto 2026", dados.temporada)
    }

    @Test
    fun `corrida descartada ou substituida nao conta`() {
        val dados = dadosDaCredencial(
            corridas = listOf(
                corrida("a", "2026-10-01T09:00:00Z", 5.0).copy(descartada = true),
                corrida("b", "2026-10-02T09:00:00Z", 6.0).copy(substituida = true),
                corrida("c", "2026-10-03T09:00:00Z", 7.0),
            ),
            temporada = kanto, colecaoCorrente = null, planoAtivo = null, fuso = fuso,
        )

        assertEquals(LocalDate.of(2026, 10, 3), dados.desde)
        assertEquals(7.0, dados.naVidaKm, 0.001)
    }

    @Test
    fun `sem corrida e sem plano a credencial fica vazia sem inventar data`() {
        val dados = dadosDaCredencial(emptyList(), kanto, colecaoCorrente = null, planoAtivo = null, fuso = fuso)

        assertNull(dados.desde)
        assertEquals(0.0, dados.naVidaKm, 0.0)
        assertEquals(1, dados.colecao)
        assertNull(dados.prova)
    }

    @Test
    fun `a primeira corrida conta no fuso da temporada`() {
        // 01h de 12/10 em UTC ainda é 11/10 em São Paulo.
        val dados = dadosDaCredencial(listOf(corrida("a", "2026-10-12T01:00:00Z", 3.0)), kanto, 1, null, fuso)

        assertEquals(LocalDate.of(2026, 10, 11), dados.desde)
    }

    private fun corrida(id: String, quando: String, km: Double) = Corrida(
        id = id, dataHoraInicio = Instant.parse(quando), km = km, duracaoSeg = 1800, tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.entries.first(), planoId = "p", semanaRef = null, temporadaId = "kanto-2026",
    )

    private fun plano(prova: Instant) = Plano(
        id = "p", nome = "São Silvestre", distanciaAlvoKm = 15.0, dataProva = prova, fuso = fuso, ownerUid = "u",
        codigoConvite = "X", encerrado = false,
        parametros = ParametrosDeGeracao(prova, 15.0, 10.0, 4, 3.0),
    )
}
