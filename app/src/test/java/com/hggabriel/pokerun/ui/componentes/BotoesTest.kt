package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Os botões do sistema (docs/02 §4 e §4.2).
 *
 * Os valores esperados estão escritos aqui, e não lidos de `docs/`, porque `docs/` não
 * é versionado.
 */
class BotoesTest {

    @Test
    fun `o botao tem 52dp e passa o piso de toque`() {
        assertEquals(52.dp, AlturaDoBotao)
        assertTrue(AlturaDoBotao >= 48.dp)
    }

    @Test
    fun `o botao pressionado encolhe para 97 por cento`() {
        assertEquals(0.97f, EscalaPressionada, 0f)
        assertEquals(EscalaPressionada, escalaDoBotao(pressionado = true), 0f)
        assertEquals(1f, escalaDoBotao(pressionado = false), 0f)
    }

    @Test
    fun `desabilitado e o botao inteiro a 38 por cento`() {
        assertEquals(0.38f, OpacidadeDoBotaoDesabilitado, 0f)
    }

    @Test
    fun `os tres botoes usam raio-controle e o FAB usa elevacao-flutuante`() {
        // O M3 desenha o botão em pílula e não lê `Shapes`: sem `FormaControle` passado
        // à mão, o botão volta a ser pílula e compila igual.
        val fonte = File(raizDeFontes(), CAMINHO_DOS_BOTOES).readText()
        assertEquals(
            "Button, OutlinedButton e TextButton recebem FormaControle",
            3,
            fonte.split("shape = FormaControle").size - 1,
        )
        assertTrue("O FAB perdeu a sombra flutuante", "Elevacao.flutuante" in fonte)
        assertTrue("O ripple do M3 voltou", "LocalRippleConfiguration provides null" in fonte)
    }

    /**
     * Os botões do M3 que só `Botoes.kt` pode chamar. `RadioButton` fica de fora, e o
     * `SegmentedButton` é de `Selecao.kt`, que `SelecaoTest` guarda.
     */
    private val chamadasDiretas = Regex(
        """(?<![\w.])(Button|OutlinedButton|TextButton|FilledTonalButton|ElevatedButton|""" +
            """FloatingActionButton|ExtendedFloatingActionButton|SmallFloatingActionButton)\(""",
    )

    @Test
    fun `nenhuma tela chama um botao do M3 direto`() {
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "Botoes.kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    val codigo = linha.substringBefore("//")
                    if (codigo.trimStart().startsWith("*")) return@mapIndexedNotNull null
                    chamadasDiretas.find(codigo)?.let { "${arquivo.name}:${i + 1} chama ${it.groupValues[1]}" }
                }
            }
            .toList()

        assertTrue(
            "Botão sai de `ui/componentes/Botoes.kt` (BotaoPrimario, BotaoSecundario, " +
                "BotaoDeTexto, BotaoFlutuante). Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }
}

private const val CAMINHO_DOS_BOTOES = "com/hggabriel/pokerun/ui/componentes/Botoes.kt"

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
