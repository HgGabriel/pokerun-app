package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * O bloco de recordes pessoais (`F3-T03`, docs/04 §3.3, docs/03 §3.17): os cinco slots,
 * sempre na mesma ordem, e o slot sem recorde sem valor.
 */
class BlocoDeRecordesTest {

    @Test
    fun `os cinco slots saem na ordem de docs 04`() {
        assertEquals(
            listOf(
                TipoDeRecorde.MELHOR_PACE,
                TipoDeRecorde.MELHOR_1K,
                TipoDeRecorde.MELHOR_5K,
                TipoDeRecorde.MAIOR_DISTANCIA,
                TipoDeRecorde.MAIOR_DURACAO,
            ),
            slotsDeRecorde(Recordes()).map { it.tipo },
        )
    }

    @Test
    fun `sem corrida todo slot fica sem valor`() {
        slotsDeRecorde(Recordes()).forEach { assertNull(it.valor) }
    }

    @Test
    fun `os valores saem na forma de leitura`() {
        val slots = slotsDeRecorde(
            Recordes(
                melhorPaceSegKm = 311.0,
                melhor1kSeg = 271,
                melhor5kSeg = 1634,
                maiorDistanciaKm = 21.1,
                maiorDuracaoSeg = 7_260,
            ),
        ).associate { it.tipo to it.valor }

        assertEquals("5:11", slots[TipoDeRecorde.MELHOR_PACE])
        assertEquals("4:31", slots[TipoDeRecorde.MELHOR_1K])
        assertEquals("27:14", slots[TipoDeRecorde.MELHOR_5K])
        assertEquals("21,1", slots[TipoDeRecorde.MAIOR_DISTANCIA])
        assertEquals("2:01:00", slots[TipoDeRecorde.MAIOR_DURACAO])
    }
}
