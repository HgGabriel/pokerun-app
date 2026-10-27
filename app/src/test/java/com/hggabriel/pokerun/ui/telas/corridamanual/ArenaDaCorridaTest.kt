package com.hggabriel.pokerun.ui.telas.corridamanual

import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.filtrarKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaDaCorridaTest {

    @Test
    fun `o pace ao vivo sai da distancia e do tempo`() {
        assertEquals(322L, paceAoVivo("8,4", "", "45", "08"))
        assertEquals(360L, paceAoVivo("10", "1", "", ""))
    }

    @Test
    fun `sem distancia ou sem tempo nao ha pace`() {
        assertNull(paceAoVivo("", "", "45", ""))
        assertNull(paceAoVivo("5", "", "", ""))
        assertNull(paceAoVivo("abc", "", "30", ""))
        assertNull(paceAoVivo("5", "", "90", ""))
    }

    @Test
    fun `cada faixa de pace tem a sua especie`() {
        assertEquals(Ritmo.JOLTEON, ritmoDe(240))
        assertEquals(Ritmo.RAPIDASH, ritmoDe(255))
        assertEquals(Ritmo.PONYTA, ritmoDe(322))
        assertEquals(Ritmo.GROWLITHE, ritmoDe(360))
        assertEquals(Ritmo.PIKACHU, ritmoDe(420))
        assertEquals(Ritmo.PSYDUCK, ritmoDe(500))
        assertEquals(Ritmo.SLOWPOKE, ritmoDe(540))
        assertEquals(Ritmo.SLOWPOKE, ritmoDe(1_200))
    }

    @Test
    fun `o chao para sem pace e acelera com ele`() {
        assertEquals(0f, velocidadeDoChao(null))
        assertTrue(velocidadeDoChao(240) > velocidadeDoChao(420))
    }

    @Test
    fun `o recorde so aparece quando a corrida alcanca a marca`() {
        assertFalse(mostraMarca("3", 5.0, "", "", null))
        assertTrue(mostraMarca("5", 5.0, "", "", null))
        assertTrue(mostraMarca("", 1.0, "4", "", null))
        assertTrue(mostraMarca("3", 5.0, "", "", R.string.manual_erro_marca_5k_curta))
    }

    @Test
    fun `o placar da distancia segura a forma na tecla`() {
        assertEquals("8,44", filtrarKm("8,4450"))
        assertEquals("123", filtrarKm("12345"))
        assertEquals("10.53", filtrarKm("10.5,3"))
        assertEquals("7,", filtrarKm("7,"))
        assertEquals("5", filtrarKm("5km"))
    }

    @Test
    fun `o esforco vira palavra por faixa`() {
        assertEquals(R.string.manual_esforco_leve, palavraDoEsforco(3))
        assertEquals(R.string.manual_esforco_moderado, palavraDoEsforco(4))
        assertEquals(R.string.manual_esforco_forte, palavraDoEsforco(8))
        assertEquals(R.string.manual_esforco_limite, palavraDoEsforco(9))
    }
}
