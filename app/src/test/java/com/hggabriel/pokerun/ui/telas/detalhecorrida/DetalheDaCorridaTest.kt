package com.hggabriel.pokerun.ui.telas.detalhecorrida

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
 * O bloco de ficha da `RunDetailScreen` (`F2-T09`, docs/03 §3.13.1): campo ausente não
 * vira traço, a linha some; o pace é derivado; o dia é o do fuso do plano (RN-28).
 */
class DetalheDaCorridaTest {

    @Test
    fun `corrida completa tem as cinco linhas na ordem do bloco`() {
        val estado = conteudo(corrida(fc = 148, esforco = 7))

        assertEquals(
            listOf(TipoDeLinha.DISTANCIA, TipoDeLinha.DURACAO, TipoDeLinha.PACE, TipoDeLinha.FC_MEDIA, TipoDeLinha.ESFORCO),
            estado.linhas.map { it.tipo },
        )
    }

    @Test
    fun `sem FC e sem esforco as duas linhas somem`() {
        val tipos = conteudo(corrida()).linhas.map { it.tipo }

        assertEquals(listOf(TipoDeLinha.DISTANCIA, TipoDeLinha.DURACAO, TipoDeLinha.PACE), tipos)
    }

    @Test
    fun `o pace e derivado da distancia e da duracao`() {
        // 12,40 km em 1:04:22 é 5:11 por km, o exemplo de docs/03 §3.13.1.
        val linhas = conteudo(corrida()).linhas.associate { it.tipo to it.valor }

        assertEquals("12,40", linhas[TipoDeLinha.DISTANCIA])
        assertEquals("1:04:22", linhas[TipoDeLinha.DURACAO])
        assertEquals("5:11", linhas[TipoDeLinha.PACE])
    }

    @Test
    fun `o dia e o do fuso do plano`() {
        // 01h em UTC do dia 15 ainda é 22h do dia 14 em São Paulo.
        val estado = conteudo(corrida(em = Instant.parse("2026-09-15T01:00:00Z")))

        assertEquals(LocalDate.of(2026, 9, 14), estado.dia)
    }

    @Test
    fun `corrida descartada continua visivel e marcada fora dos calculos`() {
        assertTrue(conteudo(corrida(descartada = true)).foraDosCalculos)
    }

    @Test
    fun `corrida ou plano ausentes falham`() {
        assertEquals(DetalheCorridaUiState.Falhou, detalheDaCorrida(null, plano))
        assertEquals(DetalheCorridaUiState.Falhou, detalheDaCorrida(corrida(), null))
    }

    private val fuso = ZoneId.of("America/Sao_Paulo")

    private val plano = Plano(
        id = "p1",
        nome = "São Silvestre",
        distanciaAlvoKm = 15.0,
        dataProva = Instant.parse("2026-12-31T11:00:00Z"),
        fuso = fuso,
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(
            dataProva = Instant.parse("2026-12-31T11:00:00Z"),
            distanciaAlvoKm = 15.0,
            baselineKm = 5.0,
            sessoesPorSemana = 3,
        ),
    )

    private fun corrida(
        em: Instant = Instant.parse("2026-09-14T10:00:00Z"),
        fc: Int? = null,
        esforco: Int? = null,
        descartada: Boolean = false,
    ) = Corrida(
        id = "run-1",
        dataHoraInicio = em,
        km = 12.4,
        duracaoSeg = 3862,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = 6,
        temporadaId = "kanto-2026",
        fcMedia = fc,
        esforcoPercebido = esforco,
        descartada = descartada,
    )

    private fun conteudo(corrida: Corrida) = detalheDaCorrida(corrida, plano) as DetalheCorridaUiState.Conteudo
}
