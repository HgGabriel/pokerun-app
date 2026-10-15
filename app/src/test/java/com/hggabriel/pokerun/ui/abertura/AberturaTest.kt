package com.hggabriel.pokerun.ui.abertura

import com.hggabriel.pokerun.ui.theme.Movimento
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Os tetos da splash e da abertura, e a geometria que as duas dividem (docs/02 §2.7, §6).
 *
 * A AVD e o tema da splash são XML: um tempo acrescentado lá não muda o classpath. A
 * tarefa de teste declara `src/main/res` como entrada (`build.gradle.kts`), e por isso
 * este teste roda quando eles mudam.
 */
class AberturaTest {

    @Test
    fun `a abertura cabe no teto`() {
        assertTrue(
            "As fases somam ${FasesDaAbertura.TOTAL}ms e o teto é ${Movimento.Duracao.ABERTURA}ms",
            FasesDaAbertura.TOTAL <= Movimento.Duracao.ABERTURA,
        )
    }

    @Test
    fun `as fases da abertura sao tokens de duracao`() {
        assertEquals(Movimento.Duracao.MEDIA, FasesDaAbertura.ABRE)
        assertEquals(Movimento.Duracao.saida(Movimento.Duracao.PREENCHIMENTO), FasesDaAbertura.CORRE)
        assertEquals(Movimento.Duracao.CURTA, FasesDaAbertura.DISSOLVE)
    }

    @Test
    fun `a AVD da splash cabe no token e no teto do Android`() {
        val avd = xml("drawable/splash_icone_animado.xml")
        val alvos = avd.getElementsByTagName("target")
        assertTrue("A AVD não tem alvo nenhum", alvos.length > 0)
        val total = (0 until alvos.length).maxOf { i -> duracao(primeiroFilhoAnimado(alvos.item(i) as Element)) }

        assertTrue("A AVD dura ${total}ms e o token é ${Movimento.Duracao.SPLASH}ms", total <= Movimento.Duracao.SPLASH)
        assertTrue("O Android corta o ícone animado em 1000ms", Movimento.Duracao.SPLASH <= 1000)
    }

    @Test
    fun `o tema da splash usa o token de duracao`() {
        val itens = xml("values/themes.xml").getElementsByTagName("item")
        val duracao = (0 until itens.length).map { itens.item(it) as Element }
            .single { it.getAttribute("name") == "windowSplashScreenAnimationDuration" }
            .textContent.trim().toInt()
        assertEquals(Movimento.Duracao.SPLASH, duracao)
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

    private fun caminhos(arquivo: String): List<String> {
        val paths = xml(arquivo).getElementsByTagName("path")
        return (0 until paths.length).map { (paths.item(it) as Element).getAttribute("android:pathData") }
    }

    private fun primeiroFilhoAnimado(alvo: Element): Element {
        val attr = alvo.getElementsByTagName("aapt:attr").item(0) as Element
        return (0 until attr.childNodes.length).map { attr.childNodes.item(it) }.filterIsInstance<Element>().first()
    }

    /** Duração de um animador ou de um `set`: sequencial soma, junto pega o maior. */
    private fun duracao(no: Element): Int {
        val atraso = no.getAttribute("android:startOffset").toIntOrNull() ?: 0
        return when (no.tagName) {
            "objectAnimator" -> atraso + no.getAttribute("android:duration").toInt()
            "set" -> {
                val filhos = (0 until no.childNodes.length).map { no.childNodes.item(it) }.filterIsInstance<Element>().map(::duracao)
                atraso + if (no.getAttribute("android:ordering") == "sequentially") filhos.sum() else filhos.maxOrNull() ?: 0
            }
            else -> error("Animador desconhecido na AVD: ${no.tagName}")
        }
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
