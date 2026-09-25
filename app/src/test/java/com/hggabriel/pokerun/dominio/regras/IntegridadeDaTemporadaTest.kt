package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * O teste de integridade que docs/07 pede para a definição da temporada (`F2-T00`):
 * cada ID uma vez só, a soma dos tiers igual a `total_especies`, nenhum ID fora da faixa.
 *
 * Roda sobre o **rascunho** em `src/test/resources/temporadas/kanto-2026.json`, que é o
 * documento que o humano publica pelo console — a rule de `temporadas` é
 * `write: if false`. Depois de publicado, o console é a fonte e o rascunho não é mais
 * editado (ficha de `F2-T00`); o teste continua valendo para quando a função for usada
 * sobre o que o app lê do Firestore.
 *
 * **Os literais 150, 21, 26… moram só aqui**, conferindo o rascunho contra docs/07 e a
 * curva de docs/04 §4. RN-43 proíbe essas contagens no código do app, não no teste que
 * confere o dado.
 */
class IntegridadeDaTemporadaTest {

    private fun temporada(total: Int, vararg tiers: List<Int>) = Temporada(
        id = "teste",
        nome = "Teste",
        regiao = "Teste",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = total,
        tiers = tiers.mapIndexed { i, especies ->
            Tier(ordem = i + 1, nome = "T${i + 1}", descricao = "", especies = especies, xpPorEspecie = 100)
        },
    )

    // -----------------------------------------------------------------------
    // A função
    // -----------------------------------------------------------------------

    @Test
    fun `temporada inteira e sem repeticao nao tem problema`() {
        assertEquals(emptyList<String>(), problemasDaTemporada(temporada(5, listOf(1, 2), listOf(3, 4, 5))))
    }

    @Test
    fun `id repetido em dois tiers e acusado`() {
        val problemas = problemasDaTemporada(temporada(4, listOf(1, 2), listOf(2, 3, 4)))
        assertTrue(problemas.toString(), problemas.any { "2" in it && "repet" in it })
        // O segundo sintoma do mesmo erro de digitação: a soma também não fecha.
        assertTrue(problemas.toString(), problemas.any { "total" in it })
    }

    @Test
    fun `a faixa e a da temporada, nao a de Kanto`() {
        // RN-43: numa dex de 3, o #4 está fora; numa de 235, o #200 está dentro.
        val pequena = problemasDaTemporada(temporada(3, listOf(1, 2), listOf(4)))
        assertTrue(pequena.toString(), pequena.any { "#4" in it && "faixa" in it })
        assertEquals(emptyList<String>(), problemasDaTemporada(temporada(235, (1..235).toList())))
    }

    @Test
    fun `id repetido dentro do mesmo tier e acusado`() {
        val problemas = problemasDaTemporada(temporada(3, listOf(1, 1, 2, 3)))
        assertTrue(problemas.toString(), problemas.any { "repet" in it })
    }

    @Test
    fun `soma dos tiers diferente do total e acusada`() {
        val problemas = problemasDaTemporada(temporada(6, listOf(1, 2), listOf(3, 4, 5)))
        assertTrue(problemas.toString(), problemas.any { "total" in it })
    }

    @Test
    fun `id fora da faixa e acusado, mesmo com a soma batendo`() {
        val problemas = problemasDaTemporada(temporada(3, listOf(1, 2), listOf(151)))
        assertTrue(problemas.toString(), problemas.any { "151" in it && "faixa" in it })
    }

    @Test
    fun `id zero esta fora da faixa`() {
        val problemas = problemasDaTemporada(temporada(3, listOf(0, 1), listOf(2)))
        assertTrue(problemas.toString(), problemas.any { "0" in it && "faixa" in it })
    }

    // -----------------------------------------------------------------------
    // O rascunho de kanto-2026
    // -----------------------------------------------------------------------

    private fun rascunho(): Temporada {
        val texto = checkNotNull(javaClass.getResource("/temporadas/kanto-2026.json")).readText()
        val raiz = Json.parseToJsonElement(texto).jsonObject
        fun JsonObject.texto(campo: String) = getValue(campo).jsonPrimitive.content
        return Temporada(
            id = raiz.texto("id"),
            nome = raiz.texto("nome"),
            regiao = raiz.texto("regiao"),
            inicio = Instant.parse(raiz.texto("inicio")),
            fim = Instant.parse(raiz.texto("fim")),
            fuso = ZoneId.of(raiz.texto("fuso")),
            ativa = raiz.getValue("ativa").jsonPrimitive.boolean,
            totalEspecies = raiz.getValue("total_especies").jsonPrimitive.int,
            tiers = raiz.getValue("tiers").jsonArray.map { elemento ->
                val tier = elemento.jsonObject
                Tier(
                    ordem = tier.getValue("ordem").jsonPrimitive.int,
                    nome = tier.texto("nome"),
                    descricao = tier.texto("descricao"),
                    especies = tier.getValue("especies").jsonArray.map { it.jsonPrimitive.int },
                    xpPorEspecie = tier.getValue("xp_por_especie").jsonPrimitive.int,
                )
            },
        )
    }

    @Test
    fun `o rascunho de kanto-2026 passa no teste de integridade`() {
        assertEquals(emptyList<String>(), problemasDaTemporada(rascunho()))
    }

    @Test
    fun `o rascunho bate com docs 07 e com a curva de docs 04`() {
        val kanto = rascunho()
        assertEquals(150, kanto.totalEspecies)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), kanto.tiers.map { it.ordem })
        assertEquals(listOf(21, 26, 19, 25, 29, 15, 10, 5), kanto.tiers.map { it.especies.size })
        assertEquals(listOf(100, 250, 500, 800, 1300, 2200, 4000, 8000), kanto.tiers.map { it.xpPorEspecie })
    }

    @Test
    fun `a corrida de 31 de dezembro as 23h em Sao Paulo ainda e de kanto-2026`() {
        val kanto = rascunho()
        val corrida = Instant.parse("2027-01-01T02:00:00Z") // 31/12 23:00 em São Paulo
        assertTrue(!corrida.isBefore(kanto.inicio) && corrida.isBefore(kanto.fim))
        val virada = Instant.parse("2027-01-01T03:00:00Z") // 01/01 00:00 em São Paulo
        assertTrue(!virada.isBefore(kanto.fim))
    }
}
