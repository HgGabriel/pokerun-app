package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * A grade da Pokédex (`F4-T02`, docs/03 §3.18, docs/07). Tier e custo saem da definição
 * da temporada, nunca da PokéAPI (decisão nº 102). A temporada de teste tem 4 espécies no
 * tier 1 (100 XP) e 2 no tier 2 (250 XP), com números fora de ordem: a grade segue a
 * escada, e não o número.
 */
class GradeDaPokedexTest {

    @Test
    fun `a grade tem uma celula por especie da temporada, na ordem dos tiers`() {
        val grade = gradeDaPokedex(temporada, posicao = 1, colecaoMax = 1, xp = 0)

        assertEquals(temporada.totalEspecies, grade.size)
        assertEquals(listOf(10, 11, 13, 14, 1, 4), grade.map { it.numero })
        assertEquals(listOf(1, 1, 1, 1, 2, 2), grade.map { it.tier })
        assertEquals(listOf(1, 2, 3, 4, 5, 6), grade.map { it.posicao })
    }

    @Test
    fun `os tres estados vem da colecao e da posicao`() {
        val grade = gradeDaPokedex(temporada, posicao = 3, colecaoMax = 5, xp = 200)

        assertEquals(
            listOf(
                EstadoDaCelula.ALCANCADA, EstadoDaCelula.ALCANCADA, EstadoDaCelula.ATUAL,
                EstadoDaCelula.ALCANCADA, EstadoDaCelula.ALCANCADA, EstadoDaCelula.NAO_ALCANCADA,
            ),
            grade.map { it.estado },
        )
    }

    @Test
    fun `sem recuo so a ultima alcancada e a atual`() {
        val grade = gradeDaPokedex(temporada, posicao = 4, colecaoMax = 4, xp = 400)

        assertEquals(1, grade.count { it.estado == EstadoDaCelula.ATUAL })
        assertEquals(4, grade.first { it.estado == EstadoDaCelula.ATUAL }.posicao)
        assertEquals(3, grade.count { it.estado == EstadoDaCelula.ALCANCADA })
    }

    @Test
    fun `quanto falta de XP sai do custo acumulado da temporada`() {
        // 400 XP passam as 4 espécies do tier 1. A 5ª custa 250 a mais: 650 no total.
        val grade = gradeDaPokedex(temporada, posicao = 4, colecaoMax = 4, xp = 400)

        assertEquals(0L, grade[0].xpParaAlcancar)
        assertEquals(200L, grade[1].xpParaAlcancar)
        assertEquals(650L, grade[4].xpParaAlcancar)
        assertEquals(250L, grade[4].faltamXp)
        assertEquals(500L, grade[5].faltamXp)
        assertTrue(grade.take(4).all { it.faltamXp == 0L })
    }

    @Test
    fun `a colecao nunca fica atras da posicao`() {
        // Documento sem colecao_max ainda (antes de F4-T12): lido como zero.
        val grade = gradeDaPokedex(temporada, posicao = 3, colecaoMax = 0, xp = 200)

        assertEquals(EstadoDaCelula.ALCANCADA, grade[0].estado)
        assertEquals(EstadoDaCelula.ATUAL, grade[2].estado)
    }

    private val temporada = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 6,
        tiers = listOf(
            Tier(1, "Passo", "", listOf(10, 11, 13, 14), 100),
            Tier(2, "Trote", "", listOf(1, 4), 250),
        ),
    )
}
