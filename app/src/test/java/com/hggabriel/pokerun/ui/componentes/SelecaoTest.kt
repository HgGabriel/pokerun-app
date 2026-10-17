package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Os controles de seleção saem de `Selecao.kt` (docs/02 §4.2).
 *
 * O `SegmentedButton` e o `FilterChip` do M3 nascem com o selecionado em
 * `secondaryContainer`, que no app é `destaque-suave`, e o segmentado com a pílula. As duas
 * coisas compilam e passam na revisão de diff: foram vistas no aparelho em 17/10/2026. O
 * que este teste guarda é a chamada direta numa tela, que traria as duas de volta.
 */
class SelecaoTest {

    /** Os componentes de seleção do M3 que só `Selecao.kt` pode chamar. */
    private val chamadasDiretas = Regex(
        """(?<![\w.])(SegmentedButton|SingleChoiceSegmentedButtonRow|MultiChoiceSegmentedButtonRow|""" +
            """FilterChip|ElevatedFilterChip|InputChip)\(""",
    )

    @Test
    fun `nenhuma tela chama um controle de selecao do M3 direto`() {
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "Selecao.kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    val codigo = linha.substringBefore("//")
                    if (codigo.trimStart().startsWith("*")) return@mapIndexedNotNull null
                    chamadasDiretas.find(codigo)?.let { "${arquivo.name}:${i + 1} chama ${it.groupValues[1]}" }
                }
            }
            .toList()

        assertTrue(
            "Seleção sai de `ui/componentes/Selecao.kt` (SegmentosDeEscolha, ChipDeFiltro). " +
                "Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }

    @Test
    fun `o selecionado e superficie-afundada com contorno de controle e sem pilula`() {
        val fonte = File(raizDeFontes(), "com/hggabriel/pokerun/ui/componentes/Selecao.kt").readText()
        assertTrue("O segmentado voltou à pílula", "baseShape = FormaControle" in fonte)
        assertTrue("O selecionado deixou superficie-afundada", "activeContainerColor = cores.superficieAfundada" in fonte)
        assertTrue("O chip selecionado deixou superficie-afundada", "selectedContainerColor = cores.superficieAfundada" in fonte)
        assertTrue("O chip selecionado perdeu o check", "Icons.Default.Check" in fonte)
        assertTrue("O ripple do M3 voltou", "SemRipple" in fonte)
    }
}

/** O diretório de fontes de produção. Mesma função de `BotoesTest`. */
private fun raizDeFontes(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${File("").absolutePath}")
}
