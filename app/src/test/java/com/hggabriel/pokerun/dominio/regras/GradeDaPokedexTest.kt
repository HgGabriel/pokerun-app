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

    @Test
    fun `a conquista e a primeira semana fechada que ja alcancava a especie`() {
        val historico = listOf(
            MarcoDaSemana("plano_1", 3),
            MarcoDaSemana("plano_2", 5),
            MarcoDaSemana("plano_3", 5),
            MarcoDaSemana("plano_4", 9),
        )

        assertEquals("plano" to 2, conquistaNaSemana(5, historico))
        assertEquals("plano" to 4, conquistaNaSemana(7, historico))
        assertEquals("plano" to 1, conquistaNaSemana(2, historico))
        assertEquals(null, conquistaNaSemana(12, historico))
    }

    @Test
    fun `o plano com sublinhado no id continua inteiro na chave da semana`() {
        assertEquals("abc_def" to 12, conquistaNaSemana(3, listOf(MarcoDaSemana("abc_def_12", 3))))
        assertEquals(null, conquistaNaSemana(3, listOf(MarcoDaSemana("sem-numero", 3))))
    }

    @Test
    fun `o tier da posicao sai da ordem dos tiers da temporada`() {
        assertEquals(1, tierDaPosicao(temporada, 1))
        assertEquals(1, tierDaPosicao(temporada, 4))
        assertEquals(2, tierDaPosicao(temporada, 5))
        assertEquals(2, tierDaPosicao(temporada, 6))
        // Fora da escada: o último tier, e não um erro.
        assertEquals(2, tierDaPosicao(temporada, 99))
    }

    @Test
    fun `so as onze especies curadas tem linha de velocidade`() {
        val comLinha = (1..150).filter { com.hggabriel.pokerun.ui.telas.pokedex.linhaDeVelocidade(it) != null }

        assertEquals(listOf(18, 22, 51, 53, 59, 78, 85, 121, 135, 142, 149), comLinha)
    }

    @Test
    fun `a nao alcancada fora do tier atual e do ultimo e segredo`() {
        // Tier 1: posições 1 e 2; tier 2: 3 e 4; tier 3: 5 e 6; o último, 7 e 8.
        val grade = gradeDaPokedex(comQuatroTiers, posicao = 2, colecaoMax = 2, xp = 100)

        assertEquals(listOf(false, false, true, true, true, true, false, false), grade.map { it.secreta })
    }

    @Test
    fun `o segredo acompanha o tier atual e nunca cobre a alcancada`() {
        // Atual na 3, que é do tier 2; a coleção já foi até a 5, do tier 3.
        val grade = gradeDaPokedex(comQuatroTiers, posicao = 3, colecaoMax = 5, xp = 0)

        assertEquals(listOf(false, false, false, false, false, true, false, false), grade.map { it.secreta })
    }

    private val comQuatroTiers = Temporada(
        id = "t4",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 8,
        tiers = listOf(
            Tier(1, "A", "", listOf(10, 11), 100),
            Tier(2, "B", "", listOf(1, 4), 200),
            Tier(3, "C", "", listOf(7, 25), 300),
            Tier(4, "D", "", listOf(144, 150), 400),
        ),
    )

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
