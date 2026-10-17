package com.hggabriel.pokerun.ui.telas.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** O recado do registro manual para a Home (docs/03 §3.3). */
class AvisoDeCorridaTest {

    @Test
    fun `o id chega antes do ganho`() {
        val aviso = AvisoDeCorrida()

        aviso.registrada("run-1")

        assertEquals(CorridaRegistrada("run-1"), aviso.atual.value)
    }

    @Test
    fun `o ganho completa o aviso da mesma corrida`() {
        val aviso = AvisoDeCorrida()
        aviso.registrada("run-1")

        aviso.ganhoCalculado("run-1", 340L)

        assertEquals(CorridaRegistrada("run-1", calculado = true, xp = 340L), aviso.atual.value)
    }

    @Test
    fun `um ganho atrasado nao sobrescreve a corrida nova`() {
        val aviso = AvisoDeCorrida()
        aviso.registrada("run-1")
        aviso.registrada("run-2")

        aviso.ganhoCalculado("run-1", 340L)

        assertEquals(CorridaRegistrada("run-2"), aviso.atual.value)
    }

    @Test
    fun `consumir so apaga a corrida consumida`() {
        val aviso = AvisoDeCorrida()
        aviso.registrada("run-2")

        aviso.consumir("run-1")
        assertEquals("run-2", aviso.atual.value?.corridaId)

        aviso.consumir("run-2")
        assertNull(aviso.atual.value)
    }
}
