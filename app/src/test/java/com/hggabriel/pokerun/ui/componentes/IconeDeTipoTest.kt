package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.graphics.Color
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.theme.CoresDeTipoClaro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * O ícone de tipo (`ChipDeTipo.md`): o glifo do Let's Go como máscara, pintado pelos tokens.
 * Como vinha do jogo, 13 dos 18 discos ficavam abaixo de 3:1; estes são os pares que a troca
 * garante.
 */
class IconeDeTipoTest {

    @Test
    fun `cada tipo tem o seu glifo`() {
        assertEquals(TipoDePokemon.entries.size, TipoDePokemon.entries.map(::glifoDoTipo).toSet().size)
    }

    @Test
    fun `o glifo passa 4,5 sobre o disco e o disco passa 3 sobre o branco e o suave`() {
        TipoDePokemon.entries.forEach { tipo ->
            val cor = CoresDeTipoClaro[tipo]
            assertTrue("glifo de ${tipo.id}", contraste(cor.sobre, cor.base) >= 4.5)
            assertTrue("disco de ${tipo.id} no branco", contraste(cor.base, Color.White) >= 3.0)
            assertTrue("disco de ${tipo.id} no suave", contraste(cor.base, cor.suave) >= 3.0)
        }
    }

    private fun luminancia(cor: Color): Double {
        fun canal(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(cor.red) + 0.7152 * canal(cor.green) + 0.0722 * canal(cor.blue)
    }

    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
