package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * As regras do parceiro (estudo de Progresso e Perfil, d.2): liberado pela coleção de
 * qualquer temporada, padrão sem escolha, a virada de temporada, a escolha recusada fora dos
 * liberados e o fallback dos leitores de membro.
 *
 * Kanto de teste: tier 1 com 10, 11, 13 e 14; tier 2 com 1 e 4. Johto de teste: tier 1 com
 * 161, 10 e 163. A escada não segue o número da Pokédex.
 */
class ParceiroTest {

    private val kanto = temporada(
        "kanto-2026", "2026-01-01T03:00:00Z",
        Tier(1, "Rastejantes", "Rastejam.", listOf(10, 11, 13, 14), 100),
        Tier(2, "Passos curtos", "Marcha curta.", listOf(1, 4), 250),
    )
    private val johto = temporada(
        "johto-2027", "2027-01-01T03:00:00Z",
        Tier(1, "Rastejantes", "Rastejam.", listOf(161, 10, 163), 100),
    )
    private val catalogo = listOf(kanto, johto)

    @Test
    fun `liberado e toda especie ate a colecao de cada temporada pela escada dela`() {
        // Kanto até a posição 5 (10, 11, 13, 14, 1); Johto até a 2 (161, 10).
        val liberados = Parceiro.liberados(mapOf("kanto-2026" to 5, "johto-2027" to 2), catalogo)

        assertEquals(setOf(10, 11, 13, 14, 1, 161), liberados)
    }

    @Test
    fun `temporada sem catalogo nao libera nada`() {
        assertTrue(Parceiro.liberados(mapOf("hoenn-2028" to 30), catalogo).isEmpty())
    }

    @Test
    fun `a grade vai do tier mais alto ao mais baixo, a temporada mais recente primeiro, sem repetir`() {
        val grade = Parceiro.grade(mapOf("kanto-2026" to 5, "johto-2027" to 2), catalogo)

        assertEquals(listOf("johto-2027" to 1, "kanto-2026" to 2, "kanto-2026" to 1), grade.map { it.temporadaId to it.ordem })
        assertEquals(listOf(161, 10), grade[0].especies)
        assertEquals(listOf(1), grade[1].especies)
        // O 10 já apareceu em Johto: em Kanto ele não se repete.
        assertEquals(listOf(11, 13, 14), grade[2].especies)
        assertEquals("Marcha curta.", grade[1].descricao)
    }

    @Test
    fun `sem escolha o parceiro e a especie da colecao da temporada corrente`() {
        assertEquals(1, Parceiro.efetivo(escolhido = null, colecaoCorrente = 5, temporadaCorrente = kanto))
    }

    @Test
    fun `sem escolha e sem progresso o parceiro e a primeira da escada`() {
        assertEquals(10, Parceiro.efetivo(escolhido = null, colecaoCorrente = null, temporadaCorrente = kanto))
    }

    @Test
    fun `com escolha o parceiro fica fixo mesmo com a colecao subindo`() {
        assertEquals(13, Parceiro.efetivo(escolhido = 13, colecaoCorrente = 6, temporadaCorrente = kanto))
    }

    @Test
    fun `na virada de temporada a escolha fica e o liberado da temporada passada continua`() {
        // 01/01/2027: Johto começa no 001, e o Nidoran de Kanto (aqui o 1) continua escolhido.
        val liberados = Parceiro.liberados(mapOf("kanto-2026" to 5, "johto-2027" to 1), catalogo)

        assertTrue(Parceiro.aceita(1, liberados))
        assertEquals(1, Parceiro.efetivo(escolhido = 1, colecaoCorrente = 1, temporadaCorrente = johto))
    }

    @Test
    fun `escolha fora dos liberados e recusada`() {
        val liberados = Parceiro.liberados(mapOf("kanto-2026" to 2), catalogo)

        assertFalse(Parceiro.aceita(4, liberados))
        assertFalse(Parceiro.aceita(13, liberados))
        assertTrue(Parceiro.aceita(11, liberados))
    }

    @Test
    fun `o leitor de membro usa o parceiro e, sem ele, a escada do avatar`() {
        assertEquals(29, Parceiro.doMembro(parceiro = 29, avatarEspecie = 3, temporada = kanto))
        assertEquals(13, Parceiro.doMembro(parceiro = null, avatarEspecie = 3, temporada = kanto))
        assertEquals(10, Parceiro.doMembro(parceiro = null, avatarEspecie = null, temporada = kanto))
    }

    private fun temporada(id: String, inicio: String, vararg tiers: Tier) = Temporada(
        id = id,
        nome = id,
        regiao = id.substringBefore('-'),
        inicio = Instant.parse(inicio),
        fim = Instant.parse(inicio).plusSeconds(365L * 24 * 3600),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = tiers.sumOf { it.especies.size },
        tiers = tiers.toList(),
    )
}
