package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * O card da espécie atual (`F2-T14`, docs/01 §2.1, D-19): a posição vira o número da
 * Pokédex pela escada da temporada, o próximo alvo é a espécie seguinte (XP-12), e o
 * sprite esvanece aos 14 dias sem correr (RN-36). Nenhuma contagem é literal (RN-43).
 *
 * A temporada de teste tem 4 espécies no tier 1 (100 XP cada, números 10, 11, 13 e
 * 14) e 2 no tier 2 (250 XP, números 1 e 4): a escada não segue o número da Pokédex.
 */
class EspecieAtualTest {

    private val agora = Instant.parse("2026-10-14T12:00:00Z")

    @Test
    fun `com zero XP o corredor esta no 001 da escada e o alvo e a segunda especie`() {
        val especie = especieAtual(0, null, temporada, agora)

        assertEquals(1, especie.posicao)
        assertEquals(10, especie.numero)
        assertEquals(11, especie.proximoNumero)
        assertEquals(200L, especie.faltamXp)
    }

    @Test
    fun `o numero e o da escada, e o alvo pode estar no tier seguinte`() {
        // 400 XP passam as 4 espécies do tier 1: posição 4, número 14. O alvo é o
        // número 1, a primeira do tier 2, que custa 250.
        val especie = especieAtual(400, null, temporada, agora)

        assertEquals(4, especie.posicao)
        assertEquals(14, especie.numero)
        assertEquals(1, especie.proximoNumero)
        assertEquals(250L, especie.faltamXp)
    }

    @Test
    fun `faltam conta o que sobra ate o proximo alvo`() {
        // Kakuna a 60 XP de Paras, o exemplo de docs/01 §2.1, em escala de teste.
        assertEquals(40L, especieAtual(160, null, temporada, agora).faltamXp)
    }

    @Test
    fun `na ultima especie nao ha proximo alvo`() {
        val especie = especieAtual(1_000_000, null, temporada, agora)

        assertEquals(temporada.totalEspecies, especie.posicao)
        assertEquals(4, especie.numero)
        assertNull(especie.proximoNumero)
        assertNull(especie.faltamXp)
    }

    @Test
    fun `o sprite esvanece aos 14 dias sem correr, e nao antes`() {
        assertTrue(especieAtual(500, agora.minus(Duration.ofDays(14)), temporada, agora).silhueta)
        assertFalse(especieAtual(500, agora.minus(Duration.ofDays(13)), temporada, agora).silhueta)
    }

    @Test
    fun `quem nunca correu na temporada nao esvanece`() {
        assertFalse(especieAtual(0, null, temporada, agora).silhueta)
    }

    @Test
    fun `o numero de uma posicao sai da escada`() {
        assertEquals(10, numeroNaEscada(1, temporada))
        assertEquals(1, numeroNaEscada(5, temporada))
    }

    @Test
    fun `o nome sai do registro em ingles da PokeAPI`() {
        val json = """{"id":122,"names":[{"language":{"name":"ja","url":"x"},"name":"バリヤード"},""" +
            """{"language":{"name":"en","url":"y"},"name":"Mr. Mime"}]}"""

        assertEquals("Mr. Mime", nomeDaEspecie(json))
    }

    @Test
    fun `o nome com simbolo escapado volta inteiro`() {
        val json = """{"names":[{"language":{"name":"en","url":"y"},"name":"Nidoran♀"}]}"""

        assertEquals("Nidoran♀", nomeDaEspecie(json))
    }

    @Test
    fun `sem registro em ingles nao ha nome`() {
        assertNull(nomeDaEspecie("""{"names":[{"language":{"name":"ja","url":"x"},"name":"x"}]}"""))
        assertNull(nomeDaEspecie("isto não é JSON"))
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
            Tier(ordem = 2, nome = "Dois", descricao = "", especies = listOf(1, 4), xpPorEspecie = 250),
            Tier(ordem = 1, nome = "Um", descricao = "", especies = listOf(10, 11, 13, 14), xpPorEspecie = 100),
        ),
    )
}
