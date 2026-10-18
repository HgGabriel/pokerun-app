package com.hggabriel.pokerun.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Durações, curvas e movimento reduzido (docs/02 §6).
 *
 * As curvas não expõem os pontos de controle, então o teste confere a saída de cada
 * uma em x = 0,25, 0,5 e 0,75. Os valores esperados foram calculados à parte, por
 * bissecção sobre a cúbica de Bézier de cada token.
 */
class MovimentoTest {

    @Test
    fun `as duracoes tem os valores do design system`() {
        assertEquals(100, Movimento.Duracao.TOQUE)
        assertEquals(160, Movimento.Duracao.CURTA)
        assertEquals(280, Movimento.Duracao.MEDIA)
        assertEquals(560, Movimento.Duracao.PREENCHIMENTO)
        assertEquals(1200, Movimento.Duracao.LOADER)
        assertEquals(1000, Movimento.Duracao.SPLASH)
        assertEquals(800, Movimento.Duracao.ABERTURA)
    }

    @Test
    fun `a saida dura 60 por cento da entrada`() {
        assertEquals(168, Movimento.Duracao.saida(Movimento.Duracao.MEDIA))
        assertEquals(96, Movimento.Duracao.saida(Movimento.Duracao.CURTA))
    }

    @Test
    fun `a navegacao tem os deslocamentos de transicoes md`() {
        assertEquals(30f, Movimento.Navegacao.APROFUNDAR.value, 0f)
        assertEquals(0.1f, Movimento.Navegacao.CAMADA, 0f)
        assertEquals(0.35f, Movimento.Navegacao.ESMAECER, 0f)
        assertEquals(98, Movimento.Navegacao.esmaecer(Movimento.Duracao.MEDIA))
        assertEquals(59, Movimento.Navegacao.esmaecer(Movimento.Duracao.saida(Movimento.Duracao.MEDIA)))
        assertEquals(56, Movimento.Navegacao.esmaecer(Movimento.Duracao.CURTA))
    }

    private fun exigeCurva(nome: String, curva: Easing, esperados: List<Float>) {
        assertEquals("$nome em 0", 0f, curva.transform(0f), 1e-3f)
        assertEquals("$nome em 1", 1f, curva.transform(1f), 1e-3f)
        listOf(0.25f, 0.5f, 0.75f).zip(esperados).forEach { (x, y) ->
            assertEquals("$nome em $x", y, curva.transform(x), 2e-3f)
        }
    }

    @Test
    fun `as curvas desenham os tokens`() {
        exigeCurva("curva-padrao", Movimento.Curva.padrao, listOf(0.6072f, 0.8778f, 0.9755f))
        exigeCurva("curva-saida", Movimento.Curva.saida, listOf(0.1285f, 0.3720f, 0.6676f))
        exigeCurva("curva-captura", Movimento.Curva.captura, listOf(0.8163f, 1.0874f, 1.0596f))
    }

    @Test
    fun `a curva de captura passa do alvo e assenta`() {
        val pico = (0..1000).maxOf { Movimento.Curva.captura.transform(it / 1000f) }
        assertTrue("A captura tem de ultrapassar o estado final: pico $pico", pico > 1f)
        assertEquals("O pico da curva-captura saiu do token", 1.098f, pico, 3e-3f)
    }

    @Test
    fun `escala de animacao zero e movimento reduzido`() {
        assertTrue(Movimento.reduzido(0f))
        assertFalse(Movimento.reduzido(1f))
        assertFalse(Movimento.reduzido(0.5f))
    }

    @Test
    fun `com movimento reduzido a animacao salta para o fim`() {
        val reduzida = Movimento.especificacao<Float>(Movimento.Duracao.MEDIA, Movimento.Curva.captura, reduzido = true)
        assertTrue("Com movimento reduzido a especificação tem de ser snap()", reduzida is SnapSpec)
        assertEquals(0, (reduzida as SnapSpec).delay)

        val normal = Movimento.especificacao<Float>(Movimento.Duracao.MEDIA, Movimento.Curva.captura, reduzido = false)
        assertTrue(normal is TweenSpec)
        normal as TweenSpec
        assertEquals(Movimento.Duracao.MEDIA, normal.durationMillis)
        assertEquals(Movimento.Curva.captura, normal.easing)
    }
}
