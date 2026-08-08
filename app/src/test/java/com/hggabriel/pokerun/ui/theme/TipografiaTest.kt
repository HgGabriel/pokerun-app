package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Os testes que travam a tipografia (`F0-T08`, docs/02 §3).
 *
 * Existem pelo mesmo motivo de `TemaTest`: o defeito não gera aviso de compilação e
 * não aparece em revisão de diff. O `Typography` do Compose **preenche em silêncio**
 * o papel que não recebe, com Roboto na escala Material — e o resultado é um botão
 * em Roboto no meio de uma tela em Plex Sans, que ninguém consegue nomear olhando o
 * código.
 *
 * O segundo teste guarda a coisa que o projeto tem de mais fácil de "melhorar" por
 * engano: o valor do eixo `wdth`.
 */
class TipografiaTest {

    /** Os 15 papéis do Material 3, nomeados. Se o M3 acrescentar um, este mapa cresce. */
    private fun papeis(t: Typography): List<Pair<String, TextStyle>> = listOf(
        "displayLarge" to t.displayLarge,
        "displayMedium" to t.displayMedium,
        "displaySmall" to t.displaySmall,
        "headlineLarge" to t.headlineLarge,
        "headlineMedium" to t.headlineMedium,
        "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium,
        "labelSmall" to t.labelSmall,
    )

    @Test
    fun `a tipografia tem 15 papeis`() {
        assertEquals(
            "O Typography mudou de tamanho: revise papeis() e Type.kt",
            15,
            papeis(Typography).size,
        )
    }

    // ---------------------------------------------------------------------
    // Teste 1 — nenhum papel cai em Roboto
    // ---------------------------------------------------------------------

    @Test
    fun `nenhum dos 15 papeis usa a familia padrao`() {
        papeis(Typography).forEach { (nome, estilo) ->
            val familia = estilo.fontFamily
            assertTrue(
                "$nome não declara família. O Typography preenche com Roboto em " +
                    "silêncio (docs/02 §3.2) — labelLarge é o mais visível, porque " +
                    "leva todo botão do app junto.",
                familia != null,
            )
            assertTrue(
                "$nome usa FontFamily.Default, que é Roboto no aparelho.",
                familia != FontFamily.Default,
            )
        }
    }

    // ---------------------------------------------------------------------
    // Teste 2 — as figuras tabulares do display (docs/02 §3.3)
    // ---------------------------------------------------------------------

    @Test
    fun `os papeis de display pedem figuras tabulares`() {
        // Sem `tnum`, `111` é mais estreito que `888` e a contagem regressiva treme
        // ao virar o dia. A Plex Mono não entra aqui: ela já é monoespaçada.
        listOf(
            "displayLarge" to Typography.displayLarge,
            "displayMedium" to Typography.displayMedium,
            "displaySmall" to Typography.displaySmall,
        ).forEach { (nome, estilo) ->
            assertEquals(
                "$nome perdeu o tnum: o número vai tremer ao atualizar (docs/02 §3.3)",
                "tnum",
                estilo.fontFeatureSettings,
            )
        }
    }

    // ---------------------------------------------------------------------
    // Teste 3 — o eixo `wdth` no máximo (docs/02 §3.1)
    // ---------------------------------------------------------------------

    @Test
    fun `o display pede o maximo do eixo de largura`() {
        // 125 é o teto do eixo, e o valor não tem meio-termo: abaixo de ~112,5 a
        // expansão é lida como "negrito estranho" e a hierarquia por largura — que é
        // a identidade do projeto — colapsa numa hierarquia por peso igual à de todo
        // mundo. É um número num arquivo, então é a coisa mais fácil do projeto de
        // alguém arredondar em novembro achando que está ajustando.
        //
        // A varredura é de fonte porque a família é privada e o `variationSettings`
        // não é exposto pelo `TextStyle`. É o mesmo recurso do teste 3 de TemaTest.
        val fonte = File(raizDeFontes(), "com/hggabriel/pokerun/ui/theme/Type.kt")
        assertTrue("Não achei ${fonte.path}", fonte.isFile)

        // Sem os comentários: este arquivo explica o porquê do 125, e a explicação
        // não pode ser o que faz o teste passar.
        val codigo = fonte.readText()
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//.*"), "")

        assertTrue(
            "O eixo wdth do Archivo saiu de 125 (docs/02 §3.1). Ou a expansão é " +
                "evidente, ou não vale a fonte extra.",
            codigo.contains(Regex("""FontVariation\.width\(\s*125f?\s*\)""")),
        )
        assertTrue(
            "O display deixou de pedir wght 700 (docs/02 §3).",
            codigo.contains(Regex("""FontVariation\.weight\(\s*700\s*\)""")),
        )
    }
}

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 * Mesma escada de `TemaTest`: o working dir do `testDebugUnitTest` não é garantido
 * entre versões do AGP.
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
