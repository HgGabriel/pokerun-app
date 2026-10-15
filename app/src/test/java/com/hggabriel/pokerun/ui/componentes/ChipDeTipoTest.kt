package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O chip de tipo (docs/02 §2.5, `ChipDeTipo.md`): a cor de tipo nunca aparece sem o nome.
 */
class ChipDeTipoTest {

    @Test
    fun `cada tipo tem o seu nome`() {
        // Dois tipos com a mesma string deixariam a cor como único canal entre eles.
        val nomes = TipoDePokemon.entries.map { nomeDoTipo(it) }
        assertEquals(TipoDePokemon.entries.size, nomes.toSet().size)
    }

    @Test
    fun `o chip do card de especie tem 22dp`() {
        assertEquals(22.dp, AlturaDoChipPequeno)
    }
}
