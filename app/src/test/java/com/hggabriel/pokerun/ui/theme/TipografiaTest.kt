package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hggabriel.pokerun.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os testes que travam a tipografia (docs/02 §3).
 *
 * Existem pelo mesmo motivo de `TemaTest`: o defeito não gera aviso de compilação e
 * não aparece em revisão de diff. O `Typography` do Compose **preenche em silêncio**
 * o papel que não recebe, com Roboto na escala Material, e o resultado é um botão em
 * Roboto no meio de uma tela em Poppins.
 *
 * Os valores esperados estão escritos aqui, e não lidos de `docs/`, porque `docs/` não
 * é versionado: um teste que lê de lá passa ou falha conforme a máquina.
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

    /** O `type` do design system: família, tamanho, entrelinha, peso e tracking de cada estilo. */
    private data class Esperado(
        val familia: FontFamily,
        val tamanho: TextUnit,
        val entrelinha: TextUnit,
        val peso: Int,
        val tracking: TextUnit = TextUnit.Unspecified,
    )

    private val tokens: List<Triple<String, TextStyle, Esperado>> = listOf(
        Triple("numero-xl", Estilos.numeroXl, Esperado(FamiliaDisplay, 56.sp, 56.sp, 700, (-0.02).em)),
        Triple("numero-l", Estilos.numeroL, Esperado(FamiliaDisplay, 36.sp, 40.sp, 700, (-0.01).em)),
        Triple("numero-m", Estilos.numeroM, Esperado(FamiliaDisplay, 24.sp, 28.sp, 700)),
        Triple("numero-s", Estilos.numeroS, Esperado(FamiliaDisplay, 18.sp, 24.sp, 700)),
        Triple("titulo-tela", Estilos.tituloTela, Esperado(FamiliaDisplay, 24.sp, 30.sp, 700)),
        Triple("nome-especie", Estilos.nomeEspecie, Esperado(FamiliaDisplay, 22.sp, 28.sp, 700)),
        Triple("titulo-secao", Estilos.tituloSecao, Esperado(FamiliaDisplay, 18.sp, 24.sp, 700)),
        Triple("corpo", Estilos.corpo, Esperado(FamiliaTexto, 16.sp, 24.sp, 500)),
        Triple("corpo-forte", Estilos.corpoForte, Esperado(FamiliaTexto, 16.sp, 24.sp, 700)),
        Triple("apoio", Estilos.apoio, Esperado(FamiliaTexto, 14.sp, 20.sp, 600)),
        Triple("botao", Estilos.botao, Esperado(FamiliaDisplay, 16.sp, 20.sp, 600)),
        Triple("rotulo", Estilos.rotulo, Esperado(FamiliaDisplay, 12.sp, 16.sp, 600, 0.08.em)),
        Triple("legenda", Estilos.legenda, Esperado(FamiliaTexto, 12.sp, 16.sp, 700)),
    )

    @Test
    fun `a tipografia tem 15 papeis`() {
        assertEquals(
            "O Typography mudou de tamanho: revise papeis() e Type.kt",
            15,
            papeis(Typography).size,
        )
    }

    @Test
    fun `os 15 papeis usam Poppins ou Quicksand e nenhum usa a familia padrao`() {
        papeis(Typography).forEach { (nome, estilo) ->
            val familia = estilo.fontFamily
            assertTrue(
                "$nome não declara família. O Typography preenche com Roboto em " +
                    "silêncio (docs/02 §3.2); labelLarge é o mais visível, porque " +
                    "leva todo botão do app junto.",
                familia != null,
            )
            assertTrue(
                "$nome usa FontFamily.Default, que é Roboto no aparelho.",
                familia != FontFamily.Default,
            )
            assertTrue(
                "$nome usa uma família que não é Poppins nem Quicksand.",
                familia === FamiliaDisplay || familia === FamiliaTexto,
            )
        }
    }

    @Test
    fun `cada papel do M3 recebe o estilo do design system que o mapa nomeia`() {
        val mapa = mapOf(
            "displayLarge" to Estilos.numeroXl,
            "displayMedium" to Estilos.numeroL,
            "displaySmall" to Estilos.numeroM,
            "headlineLarge" to Estilos.tituloTela,
            "headlineMedium" to Estilos.tituloTela,
            "headlineSmall" to Estilos.tituloTela,
            "titleLarge" to Estilos.tituloTela,
            "titleMedium" to Estilos.tituloSecao,
            "titleSmall" to Estilos.corpoForte,
            "bodyLarge" to Estilos.corpo,
            "bodyMedium" to Estilos.apoio,
            "bodySmall" to Estilos.legenda,
            "labelLarge" to Estilos.botao,
            "labelMedium" to Estilos.rotulo,
            "labelSmall" to Estilos.legenda,
        )
        papeis(Typography).forEach { (nome, estilo) ->
            assertEquals("$nome não recebeu o estilo do mapa (docs/02 §3.2)", mapa.getValue(nome), estilo)
        }
    }

    @Test
    fun `os 13 estilos batem com o type do design system`() {
        assertEquals("O design system tem 13 estilos de texto", 13, tokens.size)
        tokens.forEach { (nome, estilo, esperado) ->
            assertSame("$nome: família", esperado.familia, estilo.fontFamily)
            assertEquals("$nome: tamanho", esperado.tamanho, estilo.fontSize)
            assertEquals("$nome: entrelinha", esperado.entrelinha, estilo.lineHeight)
            assertEquals("$nome: peso", FontWeight(esperado.peso), estilo.fontWeight)
            assertEquals("$nome: tracking", esperado.tracking, estilo.letterSpacing)
        }
    }

    @Test
    fun `o Quicksand nunca fica fino demais para ler sob sol`() {
        // O traço do Quicksand some sob sol forte em peso 400: mínimo de 500 em 16sp e
        // de 600 em 14sp ou menos (docs/02 §3).
        tokens.filter { it.second.fontFamily === FamiliaTexto }.forEach { (nome, estilo, _) ->
            val minimo = if (estilo.fontSize.value <= 14f) 600 else 500
            assertTrue(
                "$nome está em Quicksand ${estilo.fontWeight?.weight} a ${estilo.fontSize}; o mínimo é $minimo",
                (estilo.fontWeight?.weight ?: 400) >= minimo,
            )
        }
    }

    @Test
    fun `nenhum estilo pede figuras tabulares`() {
        // Poppins e Quicksand não têm `tnum`: pedir a feature não alinha nada e esconde
        // a regra real, que é alinhar à direita no componente.
        (tokens.map { it.first to it.second } + papeis(Typography)).forEach { (nome, estilo) ->
            assertTrue("$nome pede tnum, que nenhuma das duas fontes tem", estilo.fontFeatureSettings == null)
        }
    }

    @Test
    fun `EstiloDado e o numero-s`() {
        assertSame(Estilos.numeroS, EstiloDado)
    }

    @Test
    fun `a Poppins tem um arquivo por peso`() {
        val fontes = (FamiliaDisplay as FontListFontFamily).fonts.map { it as ResourceFont }
        assertEquals(
            mapOf(
                400 to R.font.poppins_regular,
                500 to R.font.poppins_medium,
                600 to R.font.poppins_semibold,
                700 to R.font.poppins_bold,
            ),
            fontes.associate { it.weight.weight to it.resId },
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun `o Quicksand instancia o desenho no mesmo peso que anuncia`() {
        // Se o `weight` do Font e o `wght` da variação divergirem, o Compose escolhe o
        // arquivo por um número e desenha outro.
        val fontes = (FamiliaTexto as FontListFontFamily).fonts.map { it as ResourceFont }
        assertEquals(listOf(300, 400, 500, 600, 700), fontes.map { it.weight.weight })
        val densidade = Density(1f)
        fontes.forEach { fonte ->
            assertEquals(R.font.quicksand_variable, fonte.resId)
            val wght = fonte.variationSettings.settings.single { it.axisName == "wght" }
            assertEquals(
                "Quicksand ${fonte.weight.weight} desenha wght ${wght.toVariationValue(densidade)}",
                fonte.weight.weight.toFloat(),
                wght.toVariationValue(densidade),
                0f,
            )
        }
    }
}
