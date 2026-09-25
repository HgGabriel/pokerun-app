package com.hggabriel.pokerun.dados.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * As contas que o contrato de leitura de `F2-T01` faz sobre o que o Health Connect
 * devolveu. O acesso ao Health Connect em si só se prova em aparelho; o que dá para
 * provar aqui é que a duração, a FC e a cadência saem certas do que chegou.
 */
class ResumoDaSessaoTest {

    private val inicio: Instant = Instant.parse("2026-09-20T09:00:00Z")
    private val fim: Instant = inicio.plusSeconds(3_600)

    private fun aos(segundos: Long): Instant = inicio.plusSeconds(segundos)

    // -----------------------------------------------------------------------
    // Duração em movimento
    // -----------------------------------------------------------------------

    @Test
    fun `sem pausa, a duracao e o relogio`() {
        assertEquals(3_600L, duracaoEmMovimento(inicio, fim, emptyList()))
    }

    @Test
    fun `a pausa sai da duracao`() {
        val sinal = Intervalo(aos(600), aos(900))
        assertEquals(3_300L, duracaoEmMovimento(inicio, fim, listOf(sinal)))
    }

    @Test
    fun `a pausa que passa do fim da sessao conta so ate o fim`() {
        val depois = Intervalo(aos(3_500), aos(4_000))
        assertEquals(3_500L, duracaoEmMovimento(inicio, fim, listOf(depois)))
    }

    @Test
    fun `duas pausas sobrepostas contam o trecho comum uma vez`() {
        val pausas = listOf(Intervalo(aos(600), aos(900)), Intervalo(aos(800), aos(1_000)))
        assertEquals(3_200L, duracaoEmMovimento(inicio, fim, pausas))
    }

    @Test
    fun `sessao com fim antes do inicio nao da duracao negativa`() {
        assertEquals(0L, duracaoEmMovimento(fim, inicio, emptyList()))
    }

    // -----------------------------------------------------------------------
    // Faixa cardíaca e cadência
    // -----------------------------------------------------------------------

    @Test
    fun `o aquecimento antes do inicio fica fora da faixa cardiaca`() {
        val amostras = listOf(
            inicio.minusSeconds(60) to 95L,
            aos(60) to 140L,
            aos(1_800) to 160L,
            aos(3_540) to 171L,
        )
        assertEquals(FaixaCardiaca(media = 157, max = 171, min = 140), faixaCardiaca(amostras, inicio, fim))
    }

    @Test
    fun `sem amostra na janela a faixa cardiaca e nula, nao zero`() {
        assertNull(faixaCardiaca(listOf(fim.plusSeconds(1) to 120L), inicio, fim))
    }

    @Test
    fun `a cadencia e a media das amostras da janela`() {
        val amostras = listOf(aos(10) to 160.0, aos(20) to 170.0, fim.plusSeconds(30) to 90.0)
        assertEquals(165.0, mediaNaJanela(amostras, inicio, fim)!!, 0.001)
    }

    @Test
    fun `sem amostra de cadencia a media e nula`() {
        assertNull(mediaNaJanela(emptyList(), inicio, fim))
    }
}
