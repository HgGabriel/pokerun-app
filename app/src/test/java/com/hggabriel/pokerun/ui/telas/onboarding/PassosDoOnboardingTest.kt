package com.hggabriel.pokerun.ui.telas.onboarding

import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A leitura dos dois campos do perfil (`F1-T08`, docs/03 §3.2).
 *
 * O que dá para verificar sem tela é exatamente o que a revisão de olho deixa passar:
 * o que o formulário aceita como nome e como distância.
 */
class PassosDoOnboardingTest {

    // ---------------------------------------------------------------------
    // O nome
    // ---------------------------------------------------------------------

    @Test
    fun `o nome sai sem espaco sobrando`() {
        assertEquals("Hiago", nomeDoPerfil("  Hiago  "))
    }

    @Test
    fun `nome em branco nao e nome`() {
        assertNull(nomeDoPerfil(""))
        assertNull(nomeDoPerfil("   "))
        assertNull(nomeDoPerfil("\t\n"))
    }

    // ---------------------------------------------------------------------
    // A distância confortável (docs/01 §3.1)
    // ---------------------------------------------------------------------

    @Test
    fun `a virgula e o ponto valem a mesma coisa`() {
        // O teclado decimal de um aparelho em pt-BR entrega vírgula. Recusá-la seria
        // recusar o que o aparelho digita.
        assertEquals(7.5, distanciaEmKm("7,5"))
        assertEquals(7.5, distanciaEmKm("7.5"))
    }

    @Test
    fun `inteiro e aceito, com ou sem espaco em volta`() {
        assertEquals(5.0, distanciaEmKm("5"))
        assertEquals(5.0, distanciaEmKm(" 5 "))
    }

    @Test
    fun `distancia menor que um quilometro e valida`() {
        // "Respondível por iniciantes" (docs/01 §3.1). Quem corre 800 m responde 0,8.
        assertEquals(0.8, distanciaEmKm("0,8"))
    }

    @Test
    fun `zero nao e uma distancia confortavel`() {
        // O gerador interpola de `baseline_km` até o alvo (docs/01 §3.2). Com zero, a
        // primeira semana nasce em zero e a grade inteira sai errada.
        assertNull(distanciaEmKm("0"))
        assertNull(distanciaEmKm("0,0"))
    }

    @Test
    fun `texto que nao e numero nao vira distancia`() {
        assertNull(distanciaEmKm(""))
        assertNull(distanciaEmKm("   "))
        assertNull(distanciaEmKm("cinco"))
        assertNull(distanciaEmKm("-3"))
        assertNull(distanciaEmKm("5,5,5"))
        assertNull(distanciaEmKm("5 km"))
    }

    @Test
    fun `notacao cientifica colada de fora nao passa`() {
        // `toDoubleOrNull("1e3")` devolve 1000.0 sem reclamar, e o teclado decimal não
        // digita `e` — mas colar do teclado passa por cima do teclado. Um plano com
        // baseline de 1.000 km é grade inteira absurda gerada em silêncio.
        assertNull(distanciaEmKm("1e3"))
        assertNull(distanciaEmKm("Infinity"))
        assertNull(distanciaEmKm("NaN"))
    }

    @Test
    fun `numero grande demais para uma perna humana nao passa`() {
        assertNull(distanciaEmKm("1000"))
        assertTrue((distanciaEmKm("999") ?: 0.0) > 0.0)
    }

    // ---------------------------------------------------------------------
    // A licença completa (29/10/2026)
    // ---------------------------------------------------------------------

    private val lista = setOf("red", "lyra")

    @Test
    fun `a licenca so sai com nome, distancia e treinador da lista`() {
        assertTrue(fichaCompleta("Hiago", "7,5", "red", lista::contains))
    }

    @Test
    fun `sem treinador a licenca nao sai`() {
        assertFalse(fichaCompleta("Hiago", "7,5", null, lista::contains))
        assertFalse(fichaCompleta("Hiago", "7,5", "ash-de-mentira", lista::contains))
    }

    @Test
    fun `com treinador mas sem nome ou distancia a licenca nao sai`() {
        assertFalse(fichaCompleta("   ", "7,5", "red", lista::contains))
        assertFalse(fichaCompleta("Hiago", "", "red", lista::contains))
        assertFalse(fichaCompleta("Hiago", "0", "red", lista::contains))
        assertFalse(fichaCompleta("Hiago", "7,", "red", lista::contains))
    }
}
