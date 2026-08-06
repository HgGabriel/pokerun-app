package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * F0-T13 — os três testes que travam a fundação de cor.
 *
 * Existem porque nenhum dos três defeitos que eles pegam gera aviso de compilação
 * e nenhum aparece numa revisão de diff: papel não preenchido cai no baseline
 * lavanda do Material 3, token cromático acima de L 45 colide com banda de tipo,
 * e `import ...ui.theme.Leitura` numa tela é tecnicamente um token.
 *
 * Referências: docs/02 §2.5, §2.6 e §4.1 · project-review/02 §7.2 e §7.3
 */
class TemaTest {

    // ---------------------------------------------------------------------
    // Teste 1 — o vazamento do baseline do Material 3
    // ---------------------------------------------------------------------

    /**
     * Os 36 papéis do [ColorScheme], nomeados. Se o Material 3 acrescentar um papel
     * numa versão futura, [o esquema tem 36 papéis] falha e este mapa precisa crescer
     * junto — que é exatamente o alarme que se quer.
     */
    private fun papeis(esquema: ColorScheme): List<Pair<String, Color>> = listOf(
        "primary" to esquema.primary,
        "onPrimary" to esquema.onPrimary,
        "primaryContainer" to esquema.primaryContainer,
        "onPrimaryContainer" to esquema.onPrimaryContainer,
        "inversePrimary" to esquema.inversePrimary,
        "secondary" to esquema.secondary,
        "onSecondary" to esquema.onSecondary,
        "secondaryContainer" to esquema.secondaryContainer,
        "onSecondaryContainer" to esquema.onSecondaryContainer,
        "tertiary" to esquema.tertiary,
        "onTertiary" to esquema.onTertiary,
        "tertiaryContainer" to esquema.tertiaryContainer,
        "onTertiaryContainer" to esquema.onTertiaryContainer,
        "background" to esquema.background,
        "onBackground" to esquema.onBackground,
        "surface" to esquema.surface,
        "onSurface" to esquema.onSurface,
        "surfaceVariant" to esquema.surfaceVariant,
        "onSurfaceVariant" to esquema.onSurfaceVariant,
        "surfaceTint" to esquema.surfaceTint,
        "inverseSurface" to esquema.inverseSurface,
        "inverseOnSurface" to esquema.inverseOnSurface,
        "error" to esquema.error,
        "onError" to esquema.onError,
        "errorContainer" to esquema.errorContainer,
        "onErrorContainer" to esquema.onErrorContainer,
        "outline" to esquema.outline,
        "outlineVariant" to esquema.outlineVariant,
        "scrim" to esquema.scrim,
        "surfaceBright" to esquema.surfaceBright,
        "surfaceDim" to esquema.surfaceDim,
        "surfaceContainer" to esquema.surfaceContainer,
        "surfaceContainerHigh" to esquema.surfaceContainerHigh,
        "surfaceContainerHighest" to esquema.surfaceContainerHighest,
        "surfaceContainerLow" to esquema.surfaceContainerLow,
        "surfaceContainerLowest" to esquema.surfaceContainerLowest,
    )

    /** A paleta de docs/02 §2.1 e §2.6, mais o transparente de `surfaceTint`. */
    private val paletaPermitida: Map<Color, String> = mapOf(
        Papel to "Papel",
        Painel to "Painel",
        Borda to "Borda",
        BordaForte to "BordaForte",
        Leitura to "Leitura",
        Sinal to "Sinal",
        Alerta to "Alerta",
        Tinta to "Tinta",
        TintaFraca to "TintaFraca",
        LeituraFraca to "LeituraFraca",
        Color.Transparent to "Color.Transparent",
    )

    @Test
    fun `o esquema tem 36 papeis`() {
        assertEquals(
            "O ColorScheme mudou de tamanho: revise papeis() e PokerunColorScheme",
            36,
            papeis(PokerunColorScheme).size,
        )
    }

    @Test
    fun `nenhum papel do esquema cai no baseline do Material 3`() {
        // A forma forte da checagem: todo papel tem de ser um token do PokéRun.
        // Um papel esquecido no lightColorScheme() recebe o lavanda derivado do
        // roxo #6750A4, que não está na paleta e cai aqui.
        papeis(PokerunColorScheme).forEach { (nome, cor) ->
            assertTrue(
                "$nome = ${cor.hex()} não é um token do PokéRun. " +
                    "Papel não preenchido cai no baseline do Material 3 (docs/02 §4.1).",
                paletaPermitida.containsKey(cor),
            )
        }

        // A forma explícita, para os papéis cuja consequência está catalogada em
        // project-review/06 §2.1. Redundante de propósito: é o que nomeia o defeito
        // quando alguém remove um argumento do lightColorScheme().
        val baseline = lightColorScheme()
        val catalogados = listOf(
            "primaryContainer", "secondaryContainer", "surfaceContainer",
            "surfaceContainerLow", "surfaceContainerHigh", "surfaceContainerHighest",
            "inverseSurface", "inversePrimary", "scrim", "surfaceTint",
            "errorContainer", "onErrorContainer",
        )
        val nossos = papeis(PokerunColorScheme).toMap()
        val padrao = papeis(baseline).toMap()
        catalogados.forEach { nome ->
            assertNotEquals(
                "$nome ainda é a cor baseline do Material 3",
                padrao[nome],
                nossos[nome],
            )
        }
    }

    // ---------------------------------------------------------------------
    // Teste 2 — a separação medida contra as cores de tipo (docs/02 §2.5)
    // ---------------------------------------------------------------------

    @Test
    fun `token cromatico de UI nunca passa de L 45`() {
        // Os três cromáticos de docs/02 §2.1. A faixa L 45–47 fica vazia de propósito:
        // nenhuma banda de tipo desce de L 47, nenhum token de UI sobe de L 45.
        // Fora daqui só há neutros e a LeituraFraca decorativa, que não codificam UI.
        mapOf("Leitura" to Leitura, "Sinal" to Sinal, "Alerta" to Alerta)
            .forEach { (nome, cor) ->
                assertTrue(
                    "$nome ${cor.hex()} está em L ${cor.lightness()} e colide com banda de tipo",
                    cor.lightness() <= 45f,
                )
            }
    }

    // ---------------------------------------------------------------------
    // Teste 3 — nenhuma tela lê um `val` de cor (docs/02 §2.6)
    // ---------------------------------------------------------------------

    @Test
    fun `nenhum arquivo fora de ui-theme importa cor crua`() {
        val tokensDeCor = setOf(
            "Papel", "Painel", "Borda", "BordaForte", "Leitura", "Sinal", "Alerta",
            "Tinta", "TintaFraca", "LeituraToque", "LeituraFraca", "Scrim",
            "HeatmapPassos", "Chassi",
        )
        val importDeTema = Regex("""^\s*import\s+com\.hggabriel\.pokerun\.ui\.theme\.(\w+|\*)""")
        val literalDeCor = Regex("""\bColor\s*\(\s*0x""")

        val violacoes = mutableListOf<String>()

        raizDeFontes().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/ui/theme/") }
            .forEach { arquivo ->
                arquivo.readLines().forEachIndexed { i, linha ->
                    val importado = importDeTema.find(linha)?.groupValues?.get(1)
                    if (importado == "*" || importado in tokensDeCor) {
                        violacoes += "${arquivo.name}:${i + 1} importa $importado de ui.theme"
                    }
                    if (literalDeCor.containsMatchIn(linha)) {
                        violacoes += "${arquivo.name}:${i + 1} tem literal Color(0x…)"
                    }
                }
            }

        assertTrue(
            "O acesso a cor é MaterialTheme.colorScheme.* ou LocalCoresPokerun.current.* " +
                "(docs/02 §2.6). Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }
}

// -------------------------------------------------------------------------
// Apoio
// -------------------------------------------------------------------------

/** Luminosidade HSL em 0..100, que é a escala da regra de docs/02 §2.5. */
private fun Color.lightness(): Float {
    val r = red
    val g = green
    val b = blue
    return (maxOf(r, g, b) + minOf(r, g, b)) / 2f * 100f
}

private fun Color.hex(): String {
    if (alpha == 0f) return "transparente"
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 * Sobe até achar `src/main/java`, porque o working dir do `testDebugUnitTest`
 * não é garantido entre versões do AGP.
 */
private fun raizDeFontes(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${File("").absolutePath}")
}
