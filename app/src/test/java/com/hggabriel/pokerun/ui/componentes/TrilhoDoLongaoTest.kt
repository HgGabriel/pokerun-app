package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A geometria do trilho do longão (`PlanDetailScreen` r2), que é a do sketch
 * `gerar-detalhe-r2.py` em dp. O desenho não se prova sem aparelho; a conta, sim.
 */
class TrilhoDoLongaoTest {

    /** O plano do sketch: 13 de base, 2 de polimento e a prova. */
    private val planejado = listOf(6.0, 6.6, 7.2, 7.9, 8.0, 8.8, 9.5, 10.2, 10.9, 11.6, 12.3, 12.6, 13.0, 10.0, 7.0, 15.0)

    private fun colunas(seu: Map<Int, Double> = emptyMap()) = planejado.mapIndexed { i, km ->
        ColunaDoTrilho(
            numero = i + 1,
            fase = when {
                i < 13 -> FaseDoPlano.BASE
                i < 15 -> FaseDoPlano.POLIMENTO
                else -> FaseDoPlano.PROVA
            },
            tempo = if (i < 6) TempoDaSemana.PASSADA else TempoDaSemana.FUTURA,
            planejadoKm = km,
            seuKm = seu[i + 1],
            outroKm = null,
        )
    }

    @Test
    fun `as colunas dividem a largura com 2dp entre elas`() {
        // 390dp de tela menos as duas goteiras de 16.
        val g = geometriaDoTrilho(358f, 16)

        assertEquals(20.5f, g.larguraDaColuna, 0.001f)
        assertEquals(337.5f, g.x(15), 0.001f)
        assertEquals(358f, g.x(15) + g.larguraDaColuna, 0.001f)
    }

    @Test
    fun `a barra recua 20 por cento da coluna, e nunca menos de 3dp`() {
        assertEquals(4.1f, geometriaDoTrilho(358f, 16).recuoDaBarra, 0.001f)
        // A 320dp e com 21 semanas a coluna fica estreita, e o recuo para em 3.
        assertEquals(3f, geometriaDoTrilho(288f, 21).recuoDaBarra, 0.001f)
    }

    @Test
    fun `o dedo encaixa na coluna mais proxima, e fora da faixa na da ponta`() {
        val g = geometriaDoTrilho(358f, 16)

        assertEquals(0, g.indiceNoPonto(-12f))
        assertEquals(0, g.indiceNoPonto(10f))
        assertEquals(6, g.indiceNoPonto(g.centro(6)))
        // No meio do vão entre a 1ª e a 2ª, a 2ª: o encaixe é pela metade do vão.
        assertEquals(1, g.indiceNoPonto(g.x(1) - 1f))
        assertEquals(15, g.indiceNoPonto(400f))
    }

    @Test
    fun `a escala vai ate o maior entre planejado e feito`() {
        // Correr mais que o plano passa do contorno, e a barra precisa caber.
        assertEquals(15.0, maximoDoTrilho(colunas()), 0.0)
        assertEquals(16.2, maximoDoTrilho(colunas(seu = mapOf(16 to 16.2))), 0.0)
    }

    @Test
    fun `o maior valor fica a 34dp do topo`() {
        assertEquals(LINHA_DE_BASE_DO_TRILHO - FOLGA_DO_TOPO_DO_TRILHO, alturaNoTrilho(15.0, 15.0), 0.001f)
        assertEquals(0f, alturaNoTrilho(0.0, 15.0), 0.001f)
    }

    @Test
    fun `o pico e a maior coluna fora da prova`() {
        assertEquals(12, indiceDoPico(colunas()))
    }

    @Test
    fun `os rotulos sao a selecionada, a prova e o pico, sem repetir`() {
        assertEquals(listOf(6, 15, 12), colunasRotuladas(colunas(), selecionada = 7))
        assertEquals(listOf(12, 15), colunasRotuladas(colunas(), selecionada = 13))
        assertEquals(listOf(15, 12), colunasRotuladas(colunas(), selecionada = 16))
    }

    @Test
    fun `a semana sem feito nao diz o nome`() {
        assertEquals(
            "Semana 4, base. Longão planejado 7,9 km.",
            descricaoDaColuna(listOf("Semana 4, base.", "Longão planejado 7,9 km.", null, null)),
        )
        assertEquals(
            "Semana 4, base. Longão planejado 7,9 km. Você, 8 km. jhusilva, 7,5 km.",
            descricaoDaColuna(listOf("Semana 4, base.", "Longão planejado 7,9 km.", "Você, 8 km.", "jhusilva, 7,5 km.")),
        )
    }

    @Test
    fun `sem colunas nao ha pico`() {
        assertNull(indiceDoPico(emptyList()))
    }
}
