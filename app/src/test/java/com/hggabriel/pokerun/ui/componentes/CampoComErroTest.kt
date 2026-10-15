package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O contorno do campo com erro (docs/02 §2.4): 2,5dp em `alerta`, com e sem foco. É o
 * contorno que liga o aviso ao campo certo, e o `OutlinedTextField` do M3 o deixaria
 * em 1dp assim que o foco saísse.
 */
class CampoComErroTest {

    @Test
    fun `campo com erro tem 2,5dp com e sem foco`() {
        assertEquals(2.5.dp, ContornoDoCampoComErro)
        assertEquals(2.5.dp, contornoDoCampo(comErro = true, focado = true))
        assertEquals(2.5.dp, contornoDoCampo(comErro = true, focado = false))
    }

    @Test
    fun `campo sem erro segue o padrao do M3`() {
        assertEquals(2.dp, contornoDoCampo(comErro = false, focado = true))
        assertEquals(1.dp, contornoDoCampo(comErro = false, focado = false))
    }
}
