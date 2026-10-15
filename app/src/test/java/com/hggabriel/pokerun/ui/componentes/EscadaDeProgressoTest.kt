package com.hggabriel.pokerun.ui.componentes

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * A escada em dois níveis (`F4-T06`, docs/02 §5, docs/08 §5, RN-37), escrita antes da
 * implementação. O que se prova aqui é o modelo; o desenho em `Canvas` é de aparelho.
 *
 * A temporada de teste tem 3 tiers (4, 4 e 2 espécies, números fora de ordem de
 * propósito): a escada segue a ordem da temporada, e nenhuma contagem é literal (RN-43).
 */
class EscadaDeProgressoTest {

    @Test
    fun `os segmentos saem dos tiers da temporada, sem contagem literal`() {
        val escada = escadaDe(temporada, posicao = 1, colecaoMax = 1)

        assertEquals(3, escada.tiers.size)
        assertEquals(listOf(4, 4, 2), escada.tiers.map { it.total })
        assertEquals(listOf(1, 5, 9), escada.tiers.map { it.primeiraPosicao })
        assertEquals(10, escada.totalEspecies)
    }

    @Test
    fun `cada segmento enche pela colecao, e nao pela posicao`() {
        // Coleção 6: tier 1 inteiro (4), tier 2 com 2 de 4, tier 3 vazio.
        val escada = escadaDe(temporada, posicao = 3, colecaoMax = 6)

        assertEquals(listOf(4, 2, 0), escada.tiers.map { it.alcancadas })
        assertEquals(listOf(1f, 0.5f, 0f), escada.tiers.map { it.fracao })
    }

    @Test
    fun `o tier atual e o da espécie da posicao`() {
        assertEquals(1, escadaDe(temporada, posicao = 4, colecaoMax = 4).tierAtual)
        assertEquals(2, escadaDe(temporada, posicao = 5, colecaoMax = 5).tierAtual)
        assertEquals(3, escadaDe(temporada, posicao = 10, colecaoMax = 10).tierAtual)
    }

    @Test
    fun `as marcas do tier trazem o numero da escada, a colecao e a posicao`() {
        val marcas = escadaDe(temporada, posicao = 6, colecaoMax = 7).marcasDoTier(2)

        assertEquals(listOf(5, 6, 7, 8), marcas.map { it.posicao })
        assertEquals(listOf(21, 22, 23, 24), marcas.map { it.numero })
        assertEquals(listOf(true, true, true, false), marcas.map { it.alcancada })
        assertEquals(listOf(false, true, false, false), marcas.map { it.atual })
    }

    @Test
    fun `com recuo a colecao fica a frente do indicador de posicao`() {
        val escada = escadaDe(temporada, posicao = 3, colecaoMax = 7)

        assertTrue(escada.recuou)
        assertEquals(1, escada.tierAtual)
        // O tier 2 tem 3 espécies alcançadas pela coleção, e nenhuma é a posição atual.
        val marcas = escada.marcasDoTier(2)
        assertEquals(3, marcas.count { it.alcancada })
        assertFalse(marcas.any { it.atual })
        assertFalse(escadaDe(temporada, posicao = 7, colecaoMax = 7).recuou)
    }

    @Test
    fun `o toque no nivel 1 resolve para o tier mais proximo por fracao da largura`() {
        val escada = escadaDe(temporada, posicao = 1, colecaoMax = 1)

        assertEquals(1, tierNaFracao(0f, escada))
        assertEquals(1, tierNaFracao(0.33f, escada))
        assertEquals(2, tierNaFracao(0.34f, escada))
        assertEquals(3, tierNaFracao(0.99f, escada))
        assertEquals(3, tierNaFracao(1f, escada))
        assertEquals(1, tierNaFracao(-0.2f, escada))
    }

    @Test
    fun `o arraste resolve para a posicao, o tier pelo segmento e a especie pela fracao dentro dele`() {
        val escada = escadaDe(temporada, posicao = 1, colecaoMax = 1)

        assertEquals(1, posicaoNaFracao(0f, escada))
        // O meio do segmento 2 (fração 0,5 da escada) cai na 1ª espécie dele: posição 5.
        assertEquals(5, posicaoNaFracao(1f / 3f + 0.001f, escada))
        // O fim da escada é a última espécie.
        assertEquals(10, posicaoNaFracao(1f, escada))
        assertEquals(1, posicaoNaFracao(-1f, escada))
    }

    private val temporada = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 10,
        tiers = listOf(
            Tier(1, "Passo", "", listOf(11, 12, 13, 14), 100),
            Tier(2, "Trote", "", listOf(21, 22, 23, 24), 250),
            Tier(3, "Galope", "", listOf(31, 32), 500),
        ),
    )
}
