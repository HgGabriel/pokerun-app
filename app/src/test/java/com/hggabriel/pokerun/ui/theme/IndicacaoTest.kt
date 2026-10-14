package com.hggabriel.pokerun.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O retângulo de toque chega às telas (docs/02 §4.2).
 *
 * O `MaterialTheme` provê o próprio `LocalIndication`, o ripple, para tudo o que está
 * dentro dele. Um `LocalIndication` provido **por fora** compila, roda e nunca é lido:
 * o app inteiro volta ao ripple sem erro. Foi assim até 14/10.
 */
class IndicacaoTest {

    @Test
    fun `o retangulo de toque e provido dentro do MaterialTheme`() {
        val tema = File(raizDeFontes(), "com/hggabriel/pokerun/ui/theme/Theme.kt").readText()
        val provedor = "LocalIndication provides IndicacaoDeToque"

        assertEquals("Um provedor só de LocalIndication", 1, tema.split(provedor).size - 1)
        val inicioDoMaterialTheme = tema.indexOf("MaterialTheme(")
        assertTrue("PokerunTheme não chama MaterialTheme", inicioDoMaterialTheme >= 0)
        assertTrue(
            "O LocalIndication está por fora do MaterialTheme, que o sobrescreve com o ripple",
            tema.indexOf(provedor) > inicioDoMaterialTheme,
        )
    }
}

/** O diretório de fontes de produção. Mesma função de `TemaTest`. */
private fun raizDeFontes(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${File("").absolutePath}")
}
