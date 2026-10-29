package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * O resultado da arena (29/10/2026): o que uma corrida rendeu sai da diferença entre a
 * simulação sem ela e com ela.
 */
class RendimentoDaCorridaTest {

    @Test
    fun `as especies novas sao as posicoes entre a colecao de antes e a de depois, na ordem da escada`() {
        val rendimento = rendimentoDaCorrida("c", resultado(xp = 300, colecao = 3), resultado(xp = 800, colecao = 6, recordes = setOf(TipoDeRecorde.MAIOR_DISTANCIA)), temporada, colecaoGravada = 3)

        // A escada começa no tier 1 (101..110), mesmo com o tier 2 primeiro na lista.
        assertEquals(listOf(104, 105, 106), rendimento.novas)
        assertEquals(500L, rendimento.xp)
        assertEquals(setOf(TipoDeRecorde.MAIOR_DISTANCIA), rendimento.recordes)
    }

    @Test
    fun `a colecao gravada acima da simulada nao conta como nova`() {
        // O máximo gravado (RN-38) já tinha a posição 5: só a 6 é nova.
        val rendimento = rendimentoDaCorrida("c", resultado(xp = 300, colecao = 3), resultado(xp = 800, colecao = 6), temporada, colecaoGravada = 5)

        assertEquals(listOf(106), rendimento.novas)
    }

    @Test
    fun `corrida sem ganho nem especie nem recorde e um rendimento vazio`() {
        val rendimento = rendimentoDaCorrida("c", resultado(xp = 300, colecao = 3), resultado(xp = 300, colecao = 3), temporada, colecaoGravada = null)

        assertNull(rendimento.xp)
        assertTrue(rendimento.novas.isEmpty())
        assertTrue(rendimento.vazio)
    }

    @Test
    fun `a escada nao passa do fim da temporada`() {
        val rendimento = rendimentoDaCorrida("c", resultado(xp = 0, colecao = 19), resultado(xp = 9_000, colecao = 25), temporada, colecaoGravada = null)

        assertEquals(listOf(210), rendimento.novas)
    }

    private fun resultado(xp: Long, colecao: Int, recordes: Set<TipoDeRecorde> = emptySet()) = ResultadoDoReplay(
        porCorrida = mapOf("c" to DetalheDeXp(0.0, 0.0, 0, 1.0, 0, recordes, xp)),
        xpPorSemana = emptyMap(),
        porTemporada = mapOf(
            temporada.id to ProgressoNaTemporada(
                xpTotal = xp,
                especieAtual = colecao,
                ultimaCorridaEm = Instant.parse("2026-10-29T10:00:00Z"),
                colecaoMax = colecao,
            ),
        ),
        recordes = Recordes(),
    )

    private val temporada = Temporada(
        id = "kanto-2026",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 20,
        tiers = listOf(
            Tier(ordem = 2, nome = "Dois", descricao = "", especies = (201..210).toList(), xpPorEspecie = 250),
            Tier(ordem = 1, nome = "Um", descricao = "", especies = (101..110).toList(), xpPorEspecie = 100),
        ),
    )
}
