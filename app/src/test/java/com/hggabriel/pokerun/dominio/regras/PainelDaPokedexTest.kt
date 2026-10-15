package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * O painel de três linhas da Pokédex (`F4-T07`, docs/03 §3.18, docs/04 §5.3). A temporada
 * de teste tem 4 espécies de 100 XP e 4 de 250 XP, com números fora de ordem.
 */
class PainelDaPokedexTest {

    @Test
    fun `o corredor sem corrida esta no primeiro e o alvo e o segundo`() {
        val painel = painelDaPokedex(temporada, xp = 0, colecaoMax = 1, historico = emptyList(), xpDaMeta = null)

        assertEquals(1, painel.posicao)
        assertEquals(1, painel.ultrapassados)
        assertEquals(11, painel.proximoAlvo)
        assertEquals(200L, painel.faltamXp)
        assertNull(painel.superadoNaSemana)
        assertNull(painel.prazoEmDias)
        assertNull(painel.aoCumprirAMeta)
        assertFalse(painel.recuou)
    }

    @Test
    fun `superado esta semana e a especie da posicao quando ela subiu depois da ultima semana fechada`() {
        val painel = painelDaPokedex(
            temporada, xp = 400, colecaoMax = 4,
            historico = listOf(MarcoDaSemana("p_1", 2)), xpDaMeta = null,
        )

        assertEquals(4, painel.posicao)
        assertEquals(14, painel.superadoNaSemana)
    }

    @Test
    fun `nada superado quando a posicao e a mesma do fim da ultima semana`() {
        val painel = painelDaPokedex(
            temporada, xp = 400, colecaoMax = 4,
            historico = listOf(MarcoDaSemana("p_1", 4)), xpDaMeta = null,
        )

        assertNull(painel.superadoNaSemana)
    }

    @Test
    fun `o prazo so aparece com 4 semanas de historico e usa o XP medio por semana`() {
        val tres = (1..3).map { MarcoDaSemana("p_$it", it) }
        val quatro = (1..4).map { MarcoDaSemana("p_$it", it) }

        assertNull(painelDaPokedex(temporada, 400, 4, tres, null).prazoEmDias)

        // 400 XP em 4 semanas: 100 por semana. Faltam 250 para a 5ª: 2,5 semanas, 18 dias.
        assertEquals(18, painelDaPokedex(temporada, 400, 4, quatro, null).prazoEmDias)
    }

    @Test
    fun `se cumprir a meta cai na especie do XP que a semana ainda rende`() {
        // 400 XP + 700 de meta = 1.100: passa a 6ª espécie da escada (900 acumulados),
        // que é o número 4 da Pokédex, e não a 7ª (1.150).
        val painel = painelDaPokedex(temporada, 400, 4, emptyList(), xpDaMeta = 700)

        assertEquals(6, posicaoNaEscada(1_100, temporada))
        assertEquals(4, painel.aoCumprirAMeta)
    }

    @Test
    fun `com recuo a linguagem e da colecao e o alvo e retomar`() {
        // XP 400 é a posição 4; a coleção já foi a 7.
        val painel = painelDaPokedex(temporada, xp = 400, colecaoMax = 7, historico = emptyList(), xpDaMeta = null)

        assertTrue(painel.recuou)
        assertEquals(7, painel.ultrapassados)
        assertEquals(4, painel.posicao)
        assertTrue(painel.retomar)
        assertEquals(1, painel.proximoAlvo)
    }

    @Test
    fun `na ultima especie nao ha proximo alvo`() {
        val painel = painelDaPokedex(temporada, xp = 1_000_000, colecaoMax = 8, historico = emptyList(), xpDaMeta = null)

        assertEquals(temporada.totalEspecies, painel.posicao)
        assertNull(painel.proximoAlvo)
        assertNull(painel.faltamXp)
        assertNull(painel.prazoEmDias)
    }

    private val temporada = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 8,
        tiers = listOf(
            Tier(1, "Passo", "", listOf(10, 11, 13, 14), 100),
            Tier(2, "Trote", "", listOf(1, 4, 7, 9), 250),
        ),
    )
}
