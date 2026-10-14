package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Raio, espaço e tamanho (docs/02 §4).
 *
 * Os valores esperados estão escritos aqui, e não lidos de `docs/`, porque `docs/` não
 * é versionado.
 */
class FormaTest {

    /** O raio que a forma desenha numa caixa de 100 × 100 px a densidade 1. */
    private fun raio(forma: CornerBasedShape): List<Float> {
        val caixa = Size(100f, 100f)
        val densidade = Density(1f)
        return listOf(forma.topStart, forma.topEnd, forma.bottomEnd, forma.bottomStart)
            .map { it.toPx(caixa, densidade) }
    }

    private fun exigeRaio(nome: String, forma: CornerBasedShape, esperado: Dp) {
        raio(forma).forEach { assertEquals("$nome: raio", esperado.value, it, 0f) }
    }

    @Test
    fun `os raios tem os valores do design system`() {
        assertEquals(2.dp, Raios.dado)
        assertEquals(6.dp, Raios.chip)
        assertEquals(12.dp, Raios.controle)
        assertEquals(16.dp, Raios.card)
        assertEquals(24.dp, Raios.especie)
    }

    @Test
    fun `cada forma por papel desenha o seu raio nos quatro cantos`() {
        exigeRaio("FormaDado", FormaDado, Raios.dado)
        exigeRaio("FormaChip", FormaChip, Raios.chip)
        exigeRaio("FormaControle", FormaControle, Raios.controle)
        exigeRaio("FormaCard", FormaCard, Raios.card)
        exigeRaio("FormaEspecie", FormaEspecie, Raios.especie)
    }

    @Test
    fun `so a Pokebola e circulo`() {
        // Numa caixa de 100 px, 50 px de raio é círculo. raio-circulo é só para
        // Pokébola; nenhuma outra forma do sistema é pílula.
        raio(FormaCirculo).forEach { assertEquals("FormaCirculo não é círculo", 50f, it, 0f) }
        listOf(FormaDado, FormaChip, FormaControle, FormaCard, FormaEspecie).forEach { forma ->
            raio(forma).forEach { assertTrue("Forma de papel virou pílula", it < 50f) }
        }
    }

    @Test
    fun `cada slot do M3 recebe a forma dos componentes que o consomem`() {
        // TextField e Snackbar leem extraSmall; chips, small; Card, medium; FAB, large;
        // AlertDialog e bottom sheet, extraLarge. O FAB usa raio-card.
        assertSame("extraSmall: TextField e Snackbar", FormaControle, PokerunShapes.extraSmall)
        assertSame("small: chips", FormaChip, PokerunShapes.small)
        assertSame("medium: Card", FormaCard, PokerunShapes.medium)
        assertSame("large: FAB", FormaCard, PokerunShapes.large)
        assertSame("extraLarge: diálogo e sheet", FormaCard, PokerunShapes.extraLarge)
    }

    @Test
    fun `os espacos seguem a grade de 4 e 8`() {
        assertEquals(4.dp, Espacos.minimo)
        assertEquals(8.dp, Espacos.relacionado)
        assertEquals(12.dp, Espacos.interno)
        assertEquals(16.dp, Espacos.margem)
        assertEquals(24.dp, Espacos.bloco)
        assertEquals(32.dp, Espacos.secao)
        assertEquals(48.dp, Espacos.alvoToque)
    }

    @Test
    fun `os tamanhos tem os valores do design system`() {
        assertEquals(24.dp, Tamanhos.icone)
        assertEquals(16.dp, Tamanhos.iconeTipo)
        assertEquals(72.dp, Tamanhos.spriteLista)
        assertEquals(48.dp, Tamanhos.avatar)
        assertEquals(152.dp, Tamanhos.arteCard)
        assertEquals(232.dp, Tamanhos.arteDetalhe)
    }

    @Test
    fun `o avatar e o alvo de toque tem o mesmo piso`() {
        // O avatar do ranking é tocável; abaixo de 48dp ele reprovaria o piso de toque.
        assertTrue(Tamanhos.avatar >= Espacos.alvoToque)
    }
}
