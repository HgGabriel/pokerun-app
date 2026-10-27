package com.hggabriel.pokerun.ui.abertura

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.BroadcastFrameClock
import com.hggabriel.pokerun.ui.theme.Movimento
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * O clique, as molas e o fim da abertura, os tetos, a espera da fase 2, a splash parada e a
 * geometria que a splash e a abertura dividem (docs/02 §2.7, §6).
 *
 * O tema da splash e o ícone são XML: uma mudança lá não muda o classpath. A tarefa de
 * teste declara `src/main/res` como entrada (`build.gradle.kts`), e por isso este teste
 * roda quando eles mudam.
 */
class AberturaTest {

    /** O tempo das molas sai da física: do clique ao repouso da última mola tem de caber nos dois tetos. */
    @Test
    fun `o clique e as molas cabem no teto`() {
        val relogio = Relogio()
        rodar(relogio, aCadaQuadro = {}) { tocarAbertura(MolasDaAbertura(), Encerramento {}) }
        val total = relogio.nanos / 1_000_000
        val teto = Movimento.Duracao.CLIQUE + Movimento.Duracao.ABERTURA
        assertTrue("A abertura leva ${total}ms e o teto é ${teto}ms", total <= teto)
    }

    @Test
    fun `o token do clique e o botao de volta no fundo`() {
        val f = FisicaDaAbertura
        assertEquals(Movimento.Duracao.CLIQUE, f.ATRASO_DO_BOTAO + f.MOVIMENTO_DO_CLIQUE)
        // O botão começa antes de a coroa assentar: um clique só, e não dois separados.
        assertTrue(f.ATRASO_DO_BOTAO < 2 * f.MOVIMENTO_DO_CLIQUE)
    }

    /** A captura dispara com o botão no fundo, e não antes: o clique é visto inteiro. */
    @Test
    fun `a entrada so comeca depois do clique, com o botao no fundo`() {
        val molas = MolasDaAbertura()
        val relogio = Relogio()
        var inicioDaEntrada: Long? = null
        var botaoAntesDaEntrada = 0f
        var coroaAndou = false
        rodar(relogio, aCadaQuadro = {
            if (molas.coroa.value > 0.5f) coroaAndou = true
            if (inicioDaEntrada == null) {
                if (molas.entrada.value > 0f) inicioDaEntrada = relogio.nanos / 1_000_000
                else botaoAntesDaEntrada = maxOf(botaoAntesDaEntrada, molas.botao.value)
            }
        }) { tocarAbertura(molas, Encerramento {}) }

        assertTrue("A coroa não afundou", coroaAndou)
        assertTrue("A entrada começou em ${inicioDaEntrada}ms, antes do clique", inicioDaEntrada!! >= Movimento.Duracao.CLIQUE)
        assertEquals("O botão não chegou ao fundo antes da entrada", 1f, botaoAntesDaEntrada, 0.001f)
        assertEquals(CliqueDaAbertura(0f, 0f), molas.clique())
    }

    /** Fase 2: três quadros seguidos sem atraso. Um quadro perdido zera a conta. */
    @Test
    fun `a espera por quadros calmos zera com um quadro perdido`() = runBlocking {
        val quadros = BroadcastFrameClock()
        val intervalo = 16_000_000L
        var voltou = false
        val tarefa = launch(quadros) {
            esperarQuadrosCalmos(intervalo)
            voltou = true
        }
        var t = 0L
        suspend fun quadro(passo: Long) {
            t += passo
            quadros.sendFrame(t)
            yield()
        }
        yield()
        quadro(intervalo) // o primeiro quadro só marca o tempo
        quadro(intervalo)
        quadro(intervalo)
        quadro(3 * intervalo) // thread ocupada: dois quadros perdidos
        quadro(intervalo)
        quadro(intervalo)
        assertEquals("Voltou com um quadro perdido no meio", false, voltou)
        quadro(intervalo)
        assertEquals(true, voltou)
        tarefa.join()
    }

    /** A saída não espera o fim do quique, que é invisível, mas também não corta o overshoot. */
    @Test
    fun `a saida comeca depois do pico e antes do repouso da entrada`() {
        val molas = MolasDaAbertura()
        var picoAntesDaSaida = 0f
        var entradaAndandoNaSaida: Boolean? = null
        rodar(aCadaQuadro = {
            if (molas.saida.value == 0f) {
                picoAntesDaSaida = maxOf(picoAntesDaSaida, molas.entrada.value)
            } else if (entradaAndandoNaSaida == null) {
                entradaAndandoNaSaida = molas.entrada.isRunning
            }
        }) { tocarAbertura(molas, Encerramento {}) }

        assertEquals("A saída começou antes do pico", FisicaDaAbertura.PICO_DA_ENTRADA, picoAntesDaSaida, 0.01f)
        assertEquals("A saída esperou o repouso da entrada", true, entradaAndandoNaSaida)
    }

    @Test
    fun `a entrada passa do alvo de forma visivel e o pico bate com a formula`() {
        val molas = MolasDaAbertura()
        var pico = 0f
        rodar(aCadaQuadro = { pico = maxOf(pico, molas.entrada.value) }) { tocarAbertura(molas, Encerramento {}) }

        assertTrue("A entrada não passou de 1,15: pico $pico", pico > 1.15f)
        assertEquals(FisicaDaAbertura.PICO_DA_ENTRADA, pico, 0.01f)
        assertTrue("O logo não cresce no overshoot", escalaDoLogo(QuadroDaAbertura(pico, 0f)) > FisicaDaAbertura.ESCALA_DE_REPOUSO)
    }

    @Test
    fun `o primeiro e o ultimo quadro do logo sao os da splash e do repouso`() {
        assertEquals(1f, escalaDoLogo(QuadroDaAbertura(0f, 0f)), 0f)
        assertEquals(0f, balancoDoLogo(0f), 0f)
        assertEquals(0f, balancoDoLogo(1f), 0f)
        assertEquals(FisicaDaAbertura.ESCALA_DE_REPOUSO, escalaDoLogo(QuadroDaAbertura(1f, 0f)), 0f)
    }

    @Test
    fun `aoTerminar vem so depois do repouso das duas molas`() {
        val molas = MolasDaAbertura()
        var chamadas = 0
        var emRepouso = false
        var instante = 0L
        val relogio = Relogio()
        val encerramento = Encerramento {
            chamadas++
            emRepouso = !molas.entrada.isRunning && !molas.saida.isRunning &&
                molas.entrada.value == 1f && molas.saida.value == 1f
            instante = relogio.nanos
        }
        val chamadasComMolaAndando = mutableListOf<Int>()
        rodar(relogio, aCadaQuadro = {
            if (molas.entrada.isRunning || molas.saida.isRunning) chamadasComMolaAndando += chamadas
        }) { tocarAbertura(molas, encerramento) }

        assertEquals(1, chamadas)
        assertTrue("aoTerminar veio com uma mola fora do repouso", emRepouso)
        assertTrue("aoTerminar veio durante a animação", chamadasComMolaAndando.all { it == 0 })
        val minimo = maxOf(msAteORepouso(FisicaDaAbertura.entrada), msAteORepouso(FisicaDaAbertura.saida))
        assertTrue("aoTerminar veio em ${instante / 1_000_000}ms, antes dos ${minimo}ms das molas", instante / 1_000_000 >= minimo)
    }

    @Test
    fun `aoTerminar e chamado uma unica vez mesmo com toques`() {
        val molas = MolasDaAbertura()
        var chamadas = 0
        val encerramento = Encerramento { chamadas++ }
        var quadro = 0
        rodar(aCadaQuadro = {
            // Um toque no meio da entrada, que encerra a abertura na hora.
            if (++quadro == 5) encerramento.encerrar()
        }) { tocarAbertura(molas, encerramento) }
        // E outro depois do fim.
        encerramento.encerrar()

        assertEquals(1, chamadas)
    }

    /**
     * Fase 1: a splash do sistema mostra a bola parada. Um ícone animado ali tocaria durante a
     * partida, disputando a thread com ela, e seria cortado quando a splash saísse.
     */
    @Test
    fun `a splash do sistema mostra o icone parado`() {
        val itens = xml("values/themes.xml").getElementsByTagName("item")
        val porNome = (0 until itens.length).map { itens.item(it) as Element }.associateBy { it.getAttribute("name") }

        assertEquals("@drawable/splash_icone", porNome.getValue("windowSplashScreenAnimatedIcon").textContent.trim())
        assertTrue("A splash voltou a ter duração de animação", "windowSplashScreenAnimationDuration" !in porNome)
        assertEquals("vector", xml("drawable/splash_icone.xml").tagName)
    }

    /**
     * A abertura desenha com [GeometriaDaMarca], e o ícone e a splash são XML gerados da
     * mesma geometria. Se um mudar sem o outro, o primeiro quadro da abertura deixa de cair
     * sobre o último da splash.
     */
    @Test
    fun `a geometria do Compose e a dos XML sao a mesma`() {
        val g = GeometriaDaMarca
        fun n(v: Float) = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
        val esperados = listOf(
            "M${n(g.haste.left)},${n(g.haste.top)}h${n(g.haste.width)}v${n(g.haste.height)}h${n(-g.haste.width)}z",
            "M${n(g.CX - g.R)},${n(g.CY)}A${n(g.R)},${n(g.R)} 0 0 1 ${n(g.CX + g.R)},${n(g.CY)}z",
            "M${n(g.CX - g.R)},${n(g.CY - g.FAIXA / 2)}h${n(2 * g.R)}v${n(g.FAIXA)}h${n(-2 * g.R)}z",
            "M${n(g.CX - g.BOTAO)},${n(g.CY)}a${n(g.BOTAO)},${n(g.BOTAO)} 0 1 0 ${n(2 * g.BOTAO)},0",
        )
        listOf("drawable/ic_launcher_foreground.xml", "drawable/splash_icone.xml").forEach { arquivo ->
            val caminhos = caminhos(arquivo)
            esperados.forEach { d ->
                assertTrue("$arquivo não tem o path $d da GeometriaDaMarca", caminhos.any { it.startsWith(d) })
            }
        }
        assertEquals(
            "O ícone e a splash têm de desenhar os mesmos paths",
            caminhos("drawable/ic_launcher_foreground.xml").toSet(),
            caminhos("drawable/splash_icone.xml").toSet(),
        )
    }

    // ---------------------------------------------------------------------

    /** O tempo virtual do teste, em nanos. */
    private class Relogio {
        var nanos = 0L
    }

    /**
     * Roda [bloco] com quadros de 16ms em tempo virtual: as molas andam inteiras sem esperar
     * o relógio de verdade. Um quadro só por vez para todas as molas, como no Choreographer.
     */
    private fun rodar(relogio: Relogio = Relogio(), aCadaQuadro: () -> Unit, bloco: suspend () -> Unit) = runBlocking {
        val quadros = BroadcastFrameClock()
        val tarefa = launch(quadros) { bloco() }
        var voltas = 0
        while (tarefa.isActive) {
            check(++voltas < 100_000) { "A abertura parou sem pedir quadro" }
            yield()
            if (quadros.hasAwaiters) {
                relogio.nanos += 16_000_000L
                quadros.sendFrame(relogio.nanos)
                yield()
                aCadaQuadro()
            }
        }
    }

    private fun msAteORepouso(mola: SpringSpec<Float>): Long =
        TargetBasedAnimation(mola, Float.VectorConverter, 0f, 1f).durationNanos / 1_000_000

    private fun caminhos(arquivo: String): List<String> {
        val paths = xml(arquivo).getElementsByTagName("path")
        return (0 until paths.length).map { (paths.item(it) as Element).getAttribute("android:pathData") }
    }

    private fun xml(relativo: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(raizDeRecursos(), relativo)).documentElement
}

private fun raizDeRecursos(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/res").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/res").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/res a partir de ${File("").absolutePath}")
}
