package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A base de gráficos (`F3-T01`): a escala do eixo e o texto que o leitor de tela recebe
 * no lugar do desenho (docs/02 §8, item 5).
 */
class GraficoCanvasTest {

    @Test
    fun `a escala parte do zero e fecha num numero redondo acima do maior valor`() {
        val escala = escalaDoEixo(listOf(12.0, 37.0, 21.5))

        assertEquals(0.0, escala.minimo, 1e-9)
        assertEquals(40.0, escala.maximo, 1e-9)
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0), escala.marcas)
    }

    @Test
    fun `valores pequenos ganham passo pequeno`() {
        val escala = escalaDoEixo(listOf(0.4, 1.3))

        assertEquals(1.5, escala.maximo, 1e-9)
        assertTrue(escala.marcas.size in 3..6)
    }

    @Test
    fun `serie vazia ou toda zero ainda tem uma escala desenhavel`() {
        val vazia = escalaDoEixo(emptyList())
        val zerada = escalaDoEixo(listOf(0.0, 0.0))

        assertTrue(vazia.maximo > vazia.minimo)
        assertTrue(zerada.maximo > zerada.minimo)
    }

    @Test
    fun `a escala de pace nao parte do zero`() {
        // Pace em segundos por km: 300 a 360. Partir do zero achataria a curva.
        val escala = escalaDoEixo(listOf(300.0, 342.0, 361.0), apartirDoZero = false)

        assertTrue(escala.minimo <= 300.0)
        assertTrue(escala.minimo > 0.0)
        assertTrue(escala.maximo >= 361.0)
    }

    @Test
    fun `o leitor de tela recebe cada ponto com rotulo, valor e unidade`() {
        val texto = descricaoFalada(
            titulo = "Volume semanal",
            pontos = listOf(PontoDoGrafico("semana 1", 10.0, "10"), PontoDoGrafico("semana 2", 12.5, "12,5")),
            unidade = "km",
        )

        assertEquals("Volume semanal. semana 1, 10 km; semana 2, 12,5 km.", texto)
    }
}
