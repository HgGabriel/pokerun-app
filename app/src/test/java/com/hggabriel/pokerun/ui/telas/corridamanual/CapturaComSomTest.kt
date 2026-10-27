package com.hggabriel.pokerun.ui.telas.corridamanual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A chave "Som e vibração na captura" do Perfil (estudo de Progresso e Perfil, Revisão 3):
 * desligada por padrão, e desligada não toca nem vibra.
 */
class CapturaComSomTest {

    @Test
    fun `a chave desligada nao toca nem vibra`() {
        assertTrue(efeitosDaCaptura(ligada = false).isEmpty())
        assertTrue(efeitosDaCaptura(ligada = false, somDisponivel = true).isEmpty())
    }

    @Test
    fun `ligada sem arquivo de som so vibra`() {
        assertEquals(setOf(EfeitoDaCaptura.VIBRAR), efeitosDaCaptura(ligada = true, somDisponivel = false))
    }

    @Test
    fun `ligada com arquivo de som vibra e toca`() {
        assertEquals(
            setOf(EfeitoDaCaptura.VIBRAR, EfeitoDaCaptura.TOCAR_SOM),
            efeitosDaCaptura(ligada = true, somDisponivel = true),
        )
    }

    @Test
    fun `hoje o APK nao leva som e a chave liga so a vibracao`() {
        // Decisão aberta da Revisão 3: o humano ainda não escolheu o clique. Quando escolher,
        // esta trava cai junto com a constante, e o teste acima passa a valer no aparelho.
        assertFalse(SOM_DA_CAPTURA_DISPONIVEL)
        assertEquals(setOf(EfeitoDaCaptura.VIBRAR), efeitosDaCaptura(ligada = true))
    }
}
