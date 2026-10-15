package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A sombra é do componente, e só três têm (docs/02 §4): card de espécie, FAB e bottom
 * sheet. Card de dado não tem sombra.
 */
class ElevacaoTest {

    @Test
    fun `os tres niveis em ordem`() {
        assertEquals(0.dp, Elevacao.plana)
        assertEquals(2.dp, Elevacao.especie)
        assertEquals(6.dp, Elevacao.flutuante)
        assertTrue(Elevacao.plana < Elevacao.especie && Elevacao.especie < Elevacao.flutuante)
    }

    @Test
    fun `so a ficha de especie passa sombra`() {
        // A Ficha de dado não passa `sombra`, e o padrão é zero. Se alguém passar
        // `Elevacao.especie` na Ficha de dado, o card do dado ganha sombra e compila igual.
        val ficha = File(raizDeFontes(), "com/hggabriel/pokerun/ui/componentes/Ficha.kt").readText()
        assertEquals("Só um consumidor de Elevacao.especie", 1, ficha.split("sombra = Elevacao.especie").size - 1)
        assertTrue("O padrão da sombra deixou de ser zero", "sombra: Dp = 0.dp" in ficha)
    }

    @Test
    fun `nenhuma tela desenha sombra propria`() {
        // A sombra mora nos componentes. Uma tela com `.shadow(` ou elevação de card
        // inventa um quarto nível.
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    val codigo = linha.substringBefore("//")
                    listOf(".shadow(", "shadowElevation", "cardElevation(").firstOrNull { it in codigo }
                        ?.let { "${arquivo.name}:${i + 1} usa $it" }
                }
            }
            .toList()
        assertTrue("Sombra fora dos componentes:\n" + violacoes.joinToString("\n"), violacoes.isEmpty())
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
