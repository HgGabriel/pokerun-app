package com.hggabriel.pokerun.ui.navegacao

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O toque da barra inferior (docs/02 §4.2): `estado-pressionado` no retângulo do item,
 * pelo `LocalIndication` do tema, e não o ripple em pílula do `NavigationBarItem`.
 *
 * Nada aqui renderiza. Um ripple errado compila igual e só aparece no aparelho, então o
 * teste lê a fonte.
 */
class BarraDeDestinosTest {

    private val barra by lazy {
        File(raizDeFontes(), "com/hggabriel/pokerun/ui/navegacao/BarraDeDestinos.kt").readText()
    }

    @Test
    fun `o ripple em pilula do item sai`() {
        assertTrue("O ripple do NavigationBarItem voltou", "LocalRippleConfiguration provides null" in barra)
    }

    @Test
    fun `o item recebe o retangulo do tema`() {
        assertTrue("O item não usa o LocalIndication do tema", ".indication(fonte, indicacao)" in barra)
        assertTrue("O item não recebe a fonte de interação", "interactionSource = fonte" in barra)
    }

    @Test
    fun `o foco continua visivel sem o ripple`() {
        // O ripple também desenhava o foco. Sem ele, o anel de 2dp em `foco` é o que sobra.
        assertTrue("O item perdeu o anel de foco", "border(LarguraDoFoco, foco" in barra)
    }

    @Test
    fun `o item continua sendo aba`() {
        // `NavigationBarItem` dá `Role.Tab` e `selected` ao TalkBack. Trocá-lo por um
        // item próprio exige repor as duas coisas.
        assertTrue("A barra deixou de usar NavigationBarItem", "NavigationBarItem(" in barra)
        assertTrue("O item perdeu o estado selecionado", "selected = selecionado" in barra)
    }
}

/** O diretório de fontes de produção. Mesma função de `CanalDeAlertaTest`. */
private fun raizDeFontes(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${File("").absolutePath}")
}
