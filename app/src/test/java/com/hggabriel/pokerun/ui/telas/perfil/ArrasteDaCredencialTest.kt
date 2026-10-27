package com.hggabriel.pokerun.ui.telas.perfil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * A credencial pende do topo e a base segue o dedo no arraste.
 *
 * `rotationZ` positivo gira no sentido horário na tela (y para baixo). Com o pivô no topo,
 * a base de uma peça de altura h vai para x = -h · sen(θ): o giro horário a leva para a
 * esquerda.
 */
class ArrasteDaCredencialTest {

    private fun xDaBase(giroEmGraus: Float, altura: Float = 400f): Float =
        (-altura * sin(giroEmGraus * PI / 180.0)).toFloat()

    @Test
    fun `arrastar para a direita leva a base para a direita`() {
        val giro = giroDoArraste(0f, 60f)
        assertTrue("Arraste para a direita levou a base para x = ${xDaBase(giro)}", xDaBase(giro) > 0f)
    }

    @Test
    fun `arrastar para a esquerda leva a base para a esquerda`() {
        val giro = giroDoArraste(0f, -60f)
        assertTrue("Arraste para a esquerda levou a base para x = ${xDaBase(giro)}", xDaBase(giro) < 0f)
    }

    @Test
    fun `o giro para no teto dos dois lados`() {
        assertEquals(-GIRO_MAXIMO, giroDoArraste(0f, 10_000f), 0f)
        assertEquals(GIRO_MAXIMO, giroDoArraste(0f, -10_000f), 0f)
    }
}
