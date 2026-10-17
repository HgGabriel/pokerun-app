package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A ficha de espécie é a Pokébola aberta (docs/02 §4.3): metade de cima em
 * `tipo-*-suave`, faixa de 4dp em `texto-primario` com botão central em `FormaCirculo`,
 * `raio-especie` e `elevacao-especie`.
 */
class FichaDeEspecimeTest {

    private val ficha by lazy {
        File(raizDeFontes(), "com/hggabriel/pokerun/ui/componentes/Ficha.kt").readText()
    }

    @Test
    fun `a faixa tem 4dp`() {
        assertEquals(4.dp, AlturaDaFaixa)
    }

    @Test
    fun `a ficha de especie tem a forma e a sombra dela`() {
        val especime = ficha.substringAfter("fun FichaDeEspecime(")
        assertTrue("Sem raio-especie", "forma = FormaEspecie" in especime)
        assertTrue("Sem elevacao-especie", "sombra = Elevacao.especie" in especime)
        assertTrue("A metade de cima não lê a cor de tipo", "LocalCoresDeTipo.current[it].suave" in especime)
        assertTrue("O botão central não é círculo", "FormaCirculo" in especime)
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
