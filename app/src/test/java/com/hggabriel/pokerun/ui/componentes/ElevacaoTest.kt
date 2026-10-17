package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Os níveis de sombra do design system (docs/02 §4).
 *
 * Onde a sombra entra é intenção, não regra: o colecionável é o objeto que sai da
 * página, e uma sombra nova é desvio registrado em docs/02 §12. O que este teste
 * guarda é o vazamento do M3, que ninguém decide: a elevação padrão do `Card`.
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
    fun `nenhuma tela usa a elevacao do Card do M3`() {
        // `cardElevation()` traz 1dp de sombra e a tinta tonal do M3 (docs/02 §4.1). Ele
        // aparece quando alguém chama o `Card` direto em vez da `Ficha`, e põe o lavanda
        // do Material na tela sem ninguém ter escolhido.
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    linha.substringBefore("//").takeIf { "cardElevation(" in it }
                        ?.let { "${arquivo.name}:${i + 1} usa cardElevation(" }
                }
            }
            .toList()
        assertTrue("Elevação do Card do M3 numa tela:\n" + violacoes.joinToString("\n"), violacoes.isEmpty())
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
