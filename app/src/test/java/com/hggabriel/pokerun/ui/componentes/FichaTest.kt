package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O anel de foco da [Ficha] (docs/02 §4.2): 2dp em `foco`, que é `texto-primario`.
 *
 * `colorScheme.primary` compila igual e desenha o foco em `destaque`, a cor da ação e
 * do progresso. O anel de foco não é nem uma coisa nem outra.
 */
class FichaTest {

    @Test
    fun `o foco da ficha sai do token foco e nao do primary`() {
        val ficha = File(raizDeFontes(), "com/hggabriel/pokerun/ui/componentes/Ficha.kt").readText()

        assertTrue("O anel de foco não usa `cores.foco`", "border(LarguraDoFoco, cores.foco" in ficha)
        assertFalse("A Ficha pinta algo em `destaque`", "esquema.primary" in ficha)
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
