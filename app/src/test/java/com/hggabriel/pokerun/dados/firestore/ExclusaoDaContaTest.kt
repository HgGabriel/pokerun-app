package com.hggabriel.pokerun.dados.firestore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExclusaoDaContaTest {

    private val pedido = Instant.parse("2026-10-29T12:00:00Z")

    @Test
    fun `a conta fica guardada trinta dias a partir do pedido`() {
        assertEquals(Instant.parse("2026-11-28T12:00:00Z"), prazoDaExclusao(pedido))
    }

    @Test
    fun `os dias que faltam arredondam para cima e nunca ficam negativos`() {
        val prazo = prazoDaExclusao(pedido)
        assertEquals(30, diasAteExcluir(prazo, pedido))
        // Faltando uma hora, ainda é um dia: "0 dias" leria como já excluída.
        assertEquals(1, diasAteExcluir(prazo, prazo.minusSeconds(3_600)))
        assertEquals(0, diasAteExcluir(prazo, prazo))
        assertEquals(0, diasAteExcluir(prazo, prazo.plusSeconds(86_400)))
    }

    @Test
    fun `o prazo vencido manda apagar o login`() {
        val prazo = prazoDaExclusao(pedido)
        assertFalse(prazoVencido(prazo, prazo.minusSeconds(1)))
        assertTrue(prazoVencido(prazo, prazo))
    }

    @Test
    fun `a palavra de confirmacao aceita caixa e espacos nas pontas`() {
        assertTrue(confirmouAExclusao(" excluir "))
        assertTrue(confirmouAExclusao("EXCLUIR"))
        assertFalse(confirmouAExclusao("EXCLUI"))
        assertFalse(confirmouAExclusao(""))
    }
}
