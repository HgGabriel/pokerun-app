package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import kotlin.math.pow

/**
 * Os testes que travam a fundação de cor (docs/02 §2).
 *
 * Nenhum dos defeitos que eles pegam gera aviso de compilação, e nenhum aparece numa
 * revisão de diff: papel não preenchido cai no baseline lavanda do Material 3, um
 * hex digitado errado num dos 54 tokens de tipo derruba o contraste de um chip só,
 * e `import ...ui.theme.Destaque` numa tela é tecnicamente um token.
 *
 * Os valores esperados estão copiados do `tokens.json` do design system, de
 * propósito: o teste não lê `docs/`, que não existe num clone limpo.
 */
class TemaTest {

    // ---------------------------------------------------------------------
    // O vazamento do baseline do Material 3 (docs/02 §4.1)
    // ---------------------------------------------------------------------

    /**
     * Os 36 papéis do [ColorScheme], nomeados. Se o Material 3 acrescentar um papel
     * numa versão futura, [o esquema tem 36 papeis] falha e este mapa precisa crescer
     * junto.
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
        // A forma forte: todo papel tem de ser um token de interface do PokéRun.
        val permitidas = interfaceEsperada.values.map(::argb).toSet() + argb(Color.Transparent)
        papeis(PokerunColorScheme).forEach { (nome, cor) ->
            assertTrue(
                "$nome = ${cor.hex()} não é um token do PokéRun. " +
                    "Papel não preenchido cai no baseline do Material 3 (docs/02 §4.1).",
                argb(cor) in permitidas,
            )
        }

        // A forma explícita, para os papéis cuja consequência está catalogada em
        // docs/02 §4.1. Redundante de propósito: nomeia o defeito.
        val baseline = papeis(lightColorScheme()).toMap()
        val nossos = papeis(PokerunColorScheme).toMap()
        listOf(
            "primaryContainer", "secondaryContainer", "surfaceContainer",
            "surfaceContainerLow", "surfaceContainerHigh", "surfaceContainerHighest",
            "inverseSurface", "inversePrimary", "scrim", "surfaceTint",
            "errorContainer", "onErrorContainer",
        ).forEach { nome ->
            assertNotEquals("$nome ainda é a cor baseline do Material 3", baseline[nome], nossos[nome])
        }
    }

    // ---------------------------------------------------------------------
    // Os valores dos tokens de interface (docs/02 §2.1)
    // ---------------------------------------------------------------------

    /** Os tokens de `color` do `tokens.json`, menos os 54 de tipo. */
    private val interfaceEsperada: Map<String, Color> = mapOf(
        "fundo" to Color(0xFFFFFFFF),
        "superficie" to Color(0xFFFFFFFF),
        "superficie-afundada" to Color(0xFFF0F3F8),
        "borda" to Color(0xFFE2E7EF),
        "borda-controle" to Color(0xFF6E7787),
        "texto-primario" to Color(0xFF14161F),
        "texto-secundario" to Color(0xFF4A5262),
        "silhueta" to Color(0xFF4A5262),
        "foco" to Color(0xFF14161F),
        "destaque" to Color(0xFFDD1F26),
        "destaque-pressionado" to Color(0xFFC40218),
        "texto-sobre-destaque" to Color(0xFFFFFFFF),
        "destaque-suave" to Color(0xFFFEE9E6),
        "alerta" to Color(0xFF5A3200),
        "alerta-suave" to Color(0xFFFEEDDD),
        "sucesso" to Color(0xFF0B7A4B),
        "sucesso-suave" to Color(0xFFDAFCE6),
        "estado-pressionado" to Color(0xFF14161F).copy(alpha = 0.08f),
        "scrim" to Color(0xFF14161F).copy(alpha = 0.55f),
        "padrao-pokebola" to Color(0xFF14161F).copy(alpha = 0.05f),
        "intensidade-1" to Color(0xFFE75E63),
        "intensidade-2" to Color(0xFFE44F55),
        "intensidade-3" to Color(0xFFE13C42),
        "fase-base" to Color(0xFF3559C7),
        "fase-polimento" to Color(0xFF00796B),
    )

    @Test
    fun `os tokens de interface tem os valores do design system`() {
        val noTema = mapOf(
            "fundo" to Fundo,
            "superficie" to Superficie,
            "superficie-afundada" to SuperficieAfundada,
            "borda" to Borda,
            "borda-controle" to BordaControle,
            "texto-primario" to TextoPrimario,
            "texto-secundario" to TextoSecundario,
            "silhueta" to Silhueta,
            "foco" to Foco,
            "destaque" to Destaque,
            "destaque-pressionado" to DestaquePressionado,
            "texto-sobre-destaque" to TextoSobreDestaque,
            "destaque-suave" to DestaqueSuave,
            "alerta" to Alerta,
            "alerta-suave" to AlertaSuave,
            "sucesso" to Sucesso,
            "sucesso-suave" to SucessoSuave,
            "estado-pressionado" to EstadoPressionado,
            "scrim" to Scrim,
            "padrao-pokebola" to PadraoPokebola,
            "intensidade-1" to Intensidade1,
            "intensidade-2" to Intensidade2,
            "intensidade-3" to Intensidade3,
            "fase-base" to FaseBase,
            "fase-polimento" to FasePolimento,
        )
        assertEquals(interfaceEsperada.keys, noTema.keys)
        interfaceEsperada.forEach { (nome, esperada) ->
            assertEquals("$nome = ${noTema.getValue(nome).hex()}", argb(esperada), argb(noTema.getValue(nome)))
        }
    }

    @Test
    fun `o portador entrega os tokens sem papel no M3`() {
        val c = CoresPokerunClaro
        mapOf(
            "superficie-afundada" to c.superficieAfundada,
            "destaque-suave" to c.destaqueSuave,
            "destaque-pressionado" to c.destaquePressionado,
            "alerta" to c.alerta,
            "alerta-suave" to c.alertaSuave,
            "sucesso" to c.sucesso,
            "sucesso-suave" to c.sucessoSuave,
            "estado-pressionado" to c.estadoPressionado,
            "foco" to c.foco,
            "silhueta" to c.silhueta,
            "padrao-pokebola" to c.padraoPokebola,
            "intensidade-1" to c.intensidade1,
            "intensidade-2" to c.intensidade2,
            "intensidade-3" to c.intensidade3,
            "fase-base" to c.faseBase,
            "fase-polimento" to c.fasePolimento,
        ).forEach { (nome, cor) ->
            assertEquals(nome, argb(interfaceEsperada.getValue(nome)), argb(cor))
        }
    }

    @Test
    fun `a camada de toque e neutra`() {
        // Tinta a 8%, não o vermelho: o toque não soma cor (docs/02 §4.2).
        assertEquals(IndicacaoRetangular(EstadoPressionado), IndicacaoDeToque)
    }

    // ---------------------------------------------------------------------
    // Os 18 tipos × 3 tokens (docs/02 §2.5)
    // ---------------------------------------------------------------------

    /** base, sobre, suave, na ordem do `tokens.json`. */
    private val tiposEsperados: Map<String, Triple<Long, Long, Long>> = mapOf(
        "normal" to Triple(0xFF8B8B67, 0xFF14161F, 0xFFF1F2DC),
        "fogo" to Triple(0xFFD86A0C, 0xFF14161F, 0xFFFFEBE0),
        "agua" to Triple(0xFF4367C4, 0xFFFFFFFF, 0xFFDDE8FF),
        "planta" to Triple(0xFF347301, 0xFFFFFFFF, 0xFFCFF5BF),
        "eletrico" to Triple(0xFFA48705, 0xFF14161F, 0xFFFFF0BD),
        "gelo" to Triple(0xFF559394, 0xFF14161F, 0xFFD4F6F7),
        "lutador" to Triple(0xFF9F0308, 0xFFFFFFFF, 0xFFFEE0DB),
        "venenoso" to Triple(0xFF9D3D9E, 0xFFFFFFFF, 0xFFFEDBFD),
        "terrestre" to Triple(0xFF886C04, 0xFFFFFFFF, 0xFFF7E7BA),
        "voador" to Triple(0xFF947BD9, 0xFF14161F, 0xFFF2EFFE),
        "psiquico" to Triple(0xFFCA2862, 0xFFFFFFFF, 0xFFFEDEE4),
        "inseto" to Triple(0xFF849204, 0xFF14161F, 0xFFECF7BB),
        "pedra" to Triple(0xFF615101, 0xFFFFFFFF, 0xFFF1E8C3),
        "fantasma" to Triple(0xFF5E4685, 0xFFFFFFFF, 0xFFEBE3FF),
        "dragao" to Triple(0xFF6C32F3, 0xFFFFFFFF, 0xFFE6E5FE),
        "sombrio" to Triple(0xFF5E3E28, 0xFFFFFFFF, 0xFFF9E3D5),
        "aco" to Triple(0xFF8786A4, 0xFF14161F, 0xFFEEEEFF),
        "fada" to Triple(0xFFC27185, 0xFF14161F, 0xFFFFEAEE),
    )

    @Test
    fun `o enum de tipo tem os 18 ids do design system`() {
        assertEquals(tiposEsperados.keys.toList(), TipoDePokemon.entries.map { it.id })
    }

    @Test
    fun `os 18 tipos tem base, sobre e suave com os valores do design system`() {
        TipoDePokemon.entries.forEach { tipo ->
            val (base, sobre, suave) = tiposEsperados.getValue(tipo.id)
            val cor = CoresDeTipoClaro[tipo]
            assertEquals("tipo-${tipo.id}", Color(base).hex(), cor.base.hex())
            assertEquals("tipo-${tipo.id}-sobre", Color(sobre).hex(), cor.sobre.hex())
            assertEquals("tipo-${tipo.id}-suave", Color(suave).hex(), cor.suave.hex())
        }
    }

    @Test
    fun `o portador de tipo recusa tabela incompleta`() {
        val incompleta = TipoDePokemon.entries.drop(1).associateWith { CoresDeTipoClaro[it] }
        val falhou = runCatching { CoresDeTipo(incompleta) }.isFailure
        assertTrue("CoresDeTipo aceitou uma tabela sem ${TipoDePokemon.entries.first()}", falhou)
    }

    @Test
    fun `os pares de cada tipo passam o contraste declarado`() {
        val falhas = mutableListOf<String>()
        fun exige(par: String, a: Color, b: Color, piso: Double) {
            val r = contraste(a, b)
            if (r < piso) falhas += "$par mede %.2f:1, piso %.1f:1".format(r, piso)
        }
        TipoDePokemon.entries.forEach { tipo ->
            val c = CoresDeTipoClaro[tipo]
            val id = tipo.id
            exige("tipo-$id-sobre × tipo-$id", c.sobre, c.base, TEXTO)
            exige("tipo-$id × superficie", c.base, Superficie, GRAFICO)
            exige("tipo-$id × fundo", c.base, Fundo, GRAFICO)
            exige("tipo-$id × tipo-$id-suave", c.base, c.suave, GRAFICO)
            exige("texto-primario × tipo-$id-suave", TextoPrimario, c.suave, TEXTO)
            exige("texto-secundario × tipo-$id-suave", TextoSecundario, c.suave, TEXTO)
        }
        assertTrue("Pares de tipo abaixo do piso (docs/02 §2.5):\n" + falhas.joinToString("\n"), falhas.isEmpty())
    }

    @Test
    fun `nenhuma base de tipo cai na faixa morta de luminancia`() {
        // Entre Y 0,18 e 0,23 nem branco nem tinta passam de 4,5:1. Abaixo da faixa o
        // texto é branco; acima, tinta. Terrestre fica em 0,16, com branco.
        TipoDePokemon.entries.forEach { tipo ->
            val c = CoresDeTipoClaro[tipo]
            val y = luminancia(c.base)
            assertTrue("tipo-${tipo.id} em Y %.3f, na faixa 0,18–0,23".format(y), y < 0.18 || y > 0.23)
            val esperado = if (y < 0.18) Color.White else TextoPrimario
            assertEquals("tipo-${tipo.id}-sobre com Y %.3f".format(y), esperado.hex(), c.sobre.hex())
        }
    }

    // ---------------------------------------------------------------------
    // O contraste dos pares de interface (docs/02 §2.2)
    // ---------------------------------------------------------------------

    @Test
    fun `os pares de interface passam o contraste declarado`() {
        val pares = listOf(
            Triple("texto-primario × fundo", TextoPrimario to Fundo, TEXTO),
            Triple("texto-primario × superficie-afundada", TextoPrimario to SuperficieAfundada, TEXTO),
            Triple("texto-secundario × fundo", TextoSecundario to Fundo, TEXTO),
            Triple("texto-secundario × superficie-afundada", TextoSecundario to SuperficieAfundada, TEXTO),
            Triple("destaque × fundo", Destaque to Fundo, TEXTO),
            Triple("texto-sobre-destaque × destaque", TextoSobreDestaque to Destaque, TEXTO),
            Triple("texto-sobre-destaque × destaque-pressionado", TextoSobreDestaque to DestaquePressionado, TEXTO),
            Triple("texto-primario × destaque-suave", TextoPrimario to DestaqueSuave, TEXTO),
            Triple("destaque × destaque-suave (ícone)", Destaque to DestaqueSuave, GRAFICO),
            Triple("alerta × fundo", Alerta to Fundo, TEXTO),
            Triple("alerta × alerta-suave", Alerta to AlertaSuave, TEXTO),
            Triple("texto-primario × alerta-suave", TextoPrimario to AlertaSuave, TEXTO),
            Triple("sucesso × fundo", Sucesso to Fundo, TEXTO),
            Triple("sucesso × sucesso-suave", Sucesso to SucessoSuave, TEXTO),
            Triple("borda-controle × fundo", BordaControle to Fundo, GRAFICO),
            Triple("borda-controle × superficie-afundada", BordaControle to SuperficieAfundada, GRAFICO),
            Triple("foco × fundo", Foco to Fundo, GRAFICO),
            Triple("foco × superficie-afundada", Foco to SuperficieAfundada, GRAFICO),
            Triple("silhueta × superficie-afundada", Silhueta to SuperficieAfundada, GRAFICO),
            // O heatmap (docs/03 §3.17): todo degrau corrido se distingue do dia vazio e do fundo.
            Triple("intensidade-1 × fundo", Intensidade1 to Fundo, GRAFICO),
            Triple("intensidade-1 × superficie-afundada", Intensidade1 to SuperficieAfundada, GRAFICO),
            Triple("intensidade-2 × fundo", Intensidade2 to Fundo, GRAFICO),
            Triple("intensidade-2 × superficie-afundada", Intensidade2 to SuperficieAfundada, GRAFICO),
            Triple("intensidade-3 × fundo", Intensidade3 to Fundo, GRAFICO),
            Triple("intensidade-3 × superficie-afundada", Intensidade3 to SuperficieAfundada, GRAFICO),
            // As fases do plano (docs/03 §3.7). Servem de contorno, mas passam o piso de
            // texto nos dois chãos: foi o que tirou `#00897A` (4,32:1) da escolha.
            Triple("fase-base × fundo", FaseBase to Fundo, TEXTO),
            Triple("fase-base × superficie-afundada", FaseBase to SuperficieAfundada, TEXTO),
            Triple("fase-polimento × fundo", FasePolimento to Fundo, TEXTO),
            Triple("fase-polimento × superficie-afundada", FasePolimento to SuperficieAfundada, TEXTO),
            // O traço da outra pessoa no gráfico do longão cruza a sua barra vermelha.
            Triple("texto-primario × destaque (traço sobre a barra)", TextoPrimario to Destaque, GRAFICO),
        )
        val falhas = pares.mapNotNull { (nome, par, piso) ->
            val r = contraste(par.first, par.second)
            if (r < piso) "$nome mede %.2f:1, piso %.1f:1".format(r, piso) else null
        }
        assertTrue("Pares de interface abaixo do piso:\n" + falhas.joinToString("\n"), falhas.isEmpty())
    }

    @Test
    fun `os degraus do heatmap escurecem em ordem ate o destaque`() {
        // Vazio, 1, 2, 3 e 4: cada degrau mais escuro que o anterior, ou a legenda mente.
        val degraus = listOf(SuperficieAfundada, Intensidade1, Intensidade2, Intensidade3, Destaque).map(::luminancia)
        assertTrue(
            "A luminância dos degraus do heatmap não decresce: $degraus",
            degraus.zipWithNext().all { (claro, escuro) -> escuro < claro },
        )
    }

    @Test
    fun `o Snackbar e superficie clara`() {
        // Com `inverseSurface` em tinta, a ação em `destaque` mediria 3,69:1.
        val e = PokerunColorScheme
        assertEquals("inverseSurface: o Snackbar voltou a ser escuro", argb(Superficie), argb(e.inverseSurface))
        assertTrue(
            "Ação do Snackbar abaixo de 4,5:1",
            contraste(e.inversePrimary, e.inverseSurface) >= TEXTO,
        )
        assertTrue(
            "Texto do Snackbar abaixo de 4,5:1",
            contraste(e.inverseOnSurface, e.inverseSurface) >= TEXTO,
        )
    }

    // ---------------------------------------------------------------------
    // Nenhuma tela lê um `val` de cor (docs/02 §2.6)
    // ---------------------------------------------------------------------

    /**
     * Todo `val` de nível superior de Color.kt, lido por reflexão: um token novo entra
     * na lista sem ninguém lembrar de acrescentá-lo aqui.
     */
    private fun tokensDeCor(): Set<String> {
        val doArquivo = Class.forName("com.hggabriel.pokerun.ui.theme.ColorKt").declaredFields
            .filter { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertTrue("A reflexão sobre Color.kt não achou os tokens: $doArquivo", "Destaque" in doArquivo)
        return doArquivo + "CoresPokerunClaro"
    }

    @Test
    fun `nenhum arquivo fora de ui-theme importa cor crua`() {
        val tokens = tokensDeCor()
        val importDeTema = Regex("""^\s*import\s+com\.hggabriel\.pokerun\.ui\.theme\.(\w+|\*)""")
        val literalDeCor = Regex("""\bColor\s*\(\s*0x""")

        val violacoes = mutableListOf<String>()

        raizDeFontes().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/ui/theme/") }
            .forEach { arquivo ->
                arquivo.readLines().forEachIndexed { i, linha ->
                    val importado = importDeTema.find(linha)?.groupValues?.get(1)
                    if (importado == "*" || importado in tokens) {
                        violacoes += "${arquivo.name}:${i + 1} importa $importado de ui.theme"
                    }
                    if (literalDeCor.containsMatchIn(linha)) {
                        violacoes += "${arquivo.name}:${i + 1} tem literal Color(0x…)"
                    }
                }
            }

        assertTrue(
            "O acesso a cor é MaterialTheme.colorScheme.*, LocalCoresPokerun.current.* ou " +
                "LocalCoresDeTipo.current[tipo] (docs/02 §2.6). Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }

    private companion object {
        /** WCAG 1.4.3, texto. */
        const val TEXTO = 4.5

        /** WCAG 1.4.11, gráfico e contorno de componente. */
        const val GRAFICO = 3.0
    }
}

// -------------------------------------------------------------------------
// Apoio
// -------------------------------------------------------------------------

/** Luminância relativa do WCAG 2.1, a mesma fórmula da tabela do design system. */
private fun luminancia(cor: Color): Double {
    fun canal(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * canal(cor.red) + 0.7152 * canal(cor.green) + 0.0722 * canal(cor.blue)
}

private fun contraste(a: Color, b: Color): Double {
    val la = luminancia(a)
    val lb = luminancia(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** ARGB de 8 bits por canal: compara cores sem depender da precisão do float do alfa. */
private fun argb(cor: Color): Int {
    fun c(v: Float) = Math.round(v * 255f)
    return (c(cor.alpha) shl 24) or (c(cor.red) shl 16) or (c(cor.green) shl 8) or c(cor.blue)
}

private fun Color.hex(): String = "#%08X".format(argb(this))

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
