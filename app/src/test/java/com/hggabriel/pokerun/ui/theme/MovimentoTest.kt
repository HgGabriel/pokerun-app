package com.hggabriel.pokerun.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O compasso, as durações, as curvas e o movimento reduzido (docs/02 §6).
 *
 * As curvas não expõem os pontos de controle, então o teste confere a saída de cada
 * uma em x = 0,25, 0,5 e 0,75. Os valores esperados foram calculados à parte, por
 * bissecção sobre a cúbica de Bézier de cada token.
 */
class MovimentoTest {

    @Test
    fun `o compasso tem dois tempos e a batida como unidade`() {
        assertEquals(120, Movimento.Tempo.BATIDA)
        assertEquals(240, Movimento.Tempo.PADRAO)
        assertEquals(480, Movimento.Tempo.LENTO)
        assertEquals(2 * Movimento.Tempo.BATIDA, Movimento.Tempo.PADRAO)
        assertEquals(2 * Movimento.Tempo.PADRAO, Movimento.Tempo.LENTO)
    }

    @Test
    fun `as duracoes com nome sao batidas inteiras`() {
        assertEquals(Movimento.Tempo.BATIDA, Movimento.Duracao.TOQUE)
        assertEquals(1200, Movimento.Duracao.LOADER)
        assertEquals(Movimento.Tempo.LENTO, Movimento.Duracao.CLIQUE)
        assertEquals(800, Movimento.Duracao.ABERTURA)
        listOf(Movimento.Duracao.TOQUE, Movimento.Duracao.LOADER, Movimento.Duracao.CLIQUE).forEach {
            assertEquals("$it não cabe no compasso", 0, it % Movimento.Tempo.BATIDA)
        }
    }

    @Test
    fun `a cascata do album assenta a ultima figurinha no fim do compasso`() {
        assertEquals(Movimento.Tempo.BATIDA, Movimento.Album.ENTRE_FIGURINHAS)
        assertEquals(0, Movimento.Album.atrasoNaCascata(0, 25))
        val ultima = Movimento.Album.atrasoNaCascata(24, 25)
        assertEquals(Movimento.Album.CASCATA, ultima + Movimento.Tempo.PADRAO)
        assertEquals(0, Movimento.Album.atrasoNaCascata(0, 1))
    }

    @Test
    fun `entrada e saida duram o mesmo compasso e mudam so a curva`() {
        val entrada = Movimento.padrao<Float>() as TweenSpec
        val saida = Movimento.saida<Float>() as TweenSpec
        val pop = Movimento.pop<Float>() as TweenSpec
        val lento = Movimento.lento<Float>() as TweenSpec
        assertEquals(Movimento.Tempo.PADRAO, entrada.durationMillis)
        assertEquals(Movimento.Tempo.PADRAO, saida.durationMillis)
        assertEquals(Movimento.Tempo.PADRAO, pop.durationMillis)
        assertEquals(Movimento.Tempo.LENTO, lento.durationMillis)
        assertEquals(Movimento.Curva.padrao, entrada.easing)
        assertEquals(Movimento.Curva.saida, saida.easing)
        assertEquals(Movimento.Curva.captura, pop.easing)
    }

    @Test
    fun `a navegacao anda no compasso`() {
        assertEquals(Movimento.Tempo.PADRAO, Movimento.Navegacao.DURACAO)
        assertEquals(0.25f, Movimento.Navegacao.RECUO, 0f)
        assertTrue("Transição de rota acima de 400ms", Movimento.Navegacao.DURACAO <= 400)
    }

    @Test
    fun `nenhuma tela anima fora do compasso`() {
        // A regra de 29/10/2026 por construção: mola sem duração, número solto no `tween` e
        // `fadeIn()` sem especificação terminam cada um na sua hora. A abertura fica de fora: o
        // tempo dela sai da física, com o teto de `duracao-abertura` (AberturaTest).
        val proibido = Regex("""spring\(|spring<|tween(<\w+>)?\(\d|durationMillis = \d|fade(In|Out)\(\)|(expand|shrink)(Vertically|Horizontally)\(\)""")
        val violacoes = raizDeFontes().walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "AberturaPokerun.kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    "${arquivo.name}:${i + 1}".takeIf { proibido.containsMatchIn(linha) && !linha.trimStart().startsWith("*") && !linha.trimStart().startsWith("//") }
                }
            }
            .toList()
        assertTrue("Animação fora do compasso (use Movimento.padrao, saida, pop ou lento): $violacoes", violacoes.isEmpty())
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
        val reduzida = Movimento.especificacao<Float>(Movimento.Tempo.PADRAO, Movimento.Curva.captura, reduzido = true)
        assertTrue("Com movimento reduzido a especificação tem de ser snap()", reduzida is SnapSpec)
        assertEquals(0, (reduzida as SnapSpec).delay)

        val normal = Movimento.especificacao<Float>(Movimento.Tempo.PADRAO, Movimento.Curva.captura, reduzido = false)
        assertTrue(normal is TweenSpec)
        normal as TweenSpec
        assertEquals(Movimento.Tempo.PADRAO, normal.durationMillis)
        assertEquals(Movimento.Curva.captura, normal.easing)
    }
}

/** O diretório de fontes de produção, a partir de onde o Gradle rodar o teste (cópia de `PerfilPorAbaTest`). */
private fun raizDeFontes(): java.io.File {
    var dir: java.io.File? = java.io.File("").absoluteFile
    while (dir != null) {
        java.io.File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        java.io.File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${java.io.File("").absolutePath}")
}
