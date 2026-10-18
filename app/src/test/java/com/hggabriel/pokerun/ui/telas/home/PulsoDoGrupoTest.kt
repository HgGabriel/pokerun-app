package com.hggabriel.pokerun.ui.telas.home

import com.hggabriel.pokerun.dominio.modelo.Membro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * O pulso do grupo na Home (docs/03 §3.3). Três regras carregam a suíte: correu é ter
 * ao menos uma sessão cumprida, a ordem é a de entrada no plano, e quem saiu não entra.
 */
class PulsoDoGrupoTest {

    @Test
    fun `correu e ter ao menos uma sessao cumprida na semana`() {
        val pulso = pulsoDoGrupo(
            listOf(membro("a", 1), membro("b", 2), membro("c", 3)),
            mapOf("a" to 2, "b" to 0, "c" to null),
            uid = "a",
        ) { null }

        assertEquals(listOf(true, false, false), pulso.membros.map { it.correu })
        assertEquals(1, pulso.correram)
    }

    @Test
    fun `sem weekly na semana o membro nao correu`() {
        val pulso = pulsoDoGrupo(listOf(membro("a", 1), membro("b", 2)), emptyMap(), uid = "a") { null }

        assertEquals(0, pulso.correram)
    }

    @Test
    fun `a ordem e a de entrada no plano, nunca a de desempenho`() {
        val pulso = pulsoDoGrupo(
            listOf(membro("tarde", 9), membro("cedo", 1), membro("meio", 5)),
            mapOf("tarde" to 3, "cedo" to 0, "meio" to 1),
            uid = "meio",
        ) { null }

        assertEquals(listOf("cedo", "meio", "tarde"), pulso.membros.map { it.uid })
    }

    @Test
    fun `entrada no mesmo instante desempata pelo uid`() {
        val pulso = pulsoDoGrupo(listOf(membro("z", 1), membro("a", 1)), emptyMap(), uid = "a") { null }

        assertEquals(listOf("a", "z"), pulso.membros.map { it.uid })
    }

    @Test
    fun `quem saiu do plano nao entra no pulso`() {
        val pulso = pulsoDoGrupo(
            listOf(membro("a", 1), membro("b", 2).copy(ativo = false)),
            mapOf("b" to 3),
            uid = "a",
        ) { null }

        assertEquals(listOf("a"), pulso.membros.map { it.uid })
        assertTrue(pulso.solo)
    }

    @Test
    fun `voce e marcado e a sua situacao vem da Home`() {
        val pulso = pulsoDoGrupo(listOf(membro("a", 1), membro("b", 2)), mapOf("b" to 1), uid = "a") { null }

        assertTrue(pulso.membros.first { it.uid == "a" }.voce)
        assertFalse(pulso.membros.first { it.uid == "a" }.correu)
        val comVoce = pulso.comVoce(correu = true)
        assertTrue(comVoce.membros.first { it.uid == "a" }.correu)
        assertEquals(2, comVoce.correram)
    }

    @Test
    fun `o sprite sai do avatar de cada membro`() {
        val pulso = pulsoDoGrupo(listOf(membro("a", 1).copy(avatarEspecie = 26)), emptyMap(), uid = "a") { "sprite-${it.avatarEspecie}" }

        assertEquals("sprite-26", pulso.membros.single().sprite)
    }

    private fun membro(uid: String, minuto: Long) = Membro(
        uid = uid,
        nome = uid.uppercase(),
        entrouEm = Instant.parse("2026-08-01T12:00:00Z").plusSeconds(minuto * 60),
        entrouNaSemana = 1,
    )
}
