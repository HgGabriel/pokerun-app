package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.CoresPokerunClaro
import com.hggabriel.pokerun.ui.theme.Fundo
import com.hggabriel.pokerun.ui.theme.SuperficieAfundada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

/**
 * O padrão de Pokébolas de fundo (docs/02 §4.4).
 *
 * Três coisas que nenhuma revisão de diff pega:
 *
 * 1. **A geometria é a do design system.** Um raio de 9 que vira 10 compila e desenha;
 *    só a comparação com o `padraoSvg` de `bundle.js` diz que o app e a referência se
 *    separaram.
 * 2. **O padrão não pesa no contraste.** O ponto mais escuro do traço sobre branco tem de
 *    ser mais claro que `superficie-afundada`, o segundo chão que `TemaTest` já mede. Se
 *    alguém escurecer `padrao-pokebola`, os pares de §2.2 deixam de valer em cima dele, e
 *    este teste é o único que sabe.
 * 3. **O padrão fica nos lugares dele.** É intenção, não piso: em todo canto ele vira papel
 *    de parede (§4.4). Um lugar novo é bem-vindo com linha em docs/02 §12, e a lista de
 *    [lugaresDoPadrao] cresce no mesmo commit.
 */
class PadraoDePokebolasTest {

    /** Os arquivos que desenham o padrão hoje: o componente e o cabeçalho da Pokédex. */
    private val lugaresDoPadrao = setOf("PadraoDePokebolas.kt", "PokedexMainScreen.kt")

    /**
     * Os estados vazios de docs/03, pela string que cada um mostra. Todo arquivo que mostra
     * uma delas desenha o [EstadoVazio]. O fechamento semanal fica de fora: é um momento
     * de §11, com composição própria.
     */
    private val stringsDeVazio = listOf(
        "home_sem_plano_corpo", // HomeScreen · SemPlano e PlansListScreen · Vazio (03 §3.3, §3.4)
        "semana_detalhe_vazia", // WeekDetailScreen (03 §3.9)
        "historico_vazio", // RunHistoryScreen (03 §3.15)
        "grupo_sozinho", // SocialLeaderboardScreen, membro único (03 §3.16)
        "grupo_sem_plano", // SocialLeaderboardScreen sem plano ativo
        "pokedex_sem_temporada", // PokedexMainScreen sem temporada publicada
    )

    /** A chamada, com argumentos ou só com o lambda: `EstadoVazio(…) { }` e `EstadoVazio { }`. */
    private val chamadaDoEstadoVazio = Regex("""\bEstadoVazio\s*[({]""")

    @Test
    fun `a geometria e a do padraoSvg do design system`() {
        val g = GeometriaDoPadrao
        assertEquals(56.dp, g.ladrilho)
        assertEquals(listOf(DpOffset(14.dp, 14.dp), DpOffset(42.dp, 42.dp)), g.centros)
        assertEquals(9.dp, g.raio)
        assertEquals(2.6.dp, g.raioDoBotao)
        assertEquals(3.dp, g.fimDaFaixa)
        assertEquals(1.5.dp, g.traco)
        assertEquals(2.dp, g.folgaDaBorda)
        // A faixa para fora do botão, senão as três peças viram uma mancha a 1,5dp de traço.
        assertTrue(g.fimDaFaixa > g.raioDoBotao)
    }

    @Test
    fun `so entra Pokebola inteira`() {
        val g = GeometriaDoPadrao
        val alcance = g.raio.value + g.traco.value / 2 + g.folgaDaBorda.value
        // O cabeçalho da Pokédex a 360dp, o EstadoVazio a 328dp e um bloco alto a 320dp.
        listOf(360f to 97f, 328f to 110f, 320f to 260f).forEach { (largura, altura) ->
            val centros = centrosQueCabem(largura, altura)
            assertTrue("Nenhuma Pokébola em $largura × $altura", centros.isNotEmpty())
            centros.forEach { c ->
                assertTrue(
                    "Pokébola em (${c.x}, ${c.y}) cortada pela borda de $largura × $altura",
                    c.x - alcance >= 0f && c.y - alcance >= 0f &&
                        c.x + alcance <= largura && c.y + alcance <= altura,
                )
            }
        }
    }

    @Test
    fun `a fileira que nao cabe sai inteira e a que cabe fica inteira`() {
        // Dois ladrilhos por dois: as oito cabem, porque 98 + 11,75 ≤ 112.
        assertEquals(8, centrosQueCabem(112f, 112f).size)
        // A 50dp de altura a Pokébola de y = 42 tocaria a borda: sobra a fileira de y = 14.
        val baixo = centrosQueCabem(112f, 50f)
        assertEquals(listOf(14f, 14f), baixo.map { it.y })
    }

    @Test
    fun `o traco mais escuro e mais claro que superficie-afundada`() {
        val traco = CoresPokerunClaro.padraoPokebola.compositeOver(Fundo)
        assertTrue(
            "O traço do padrão sobre branco tem luminância ${luminancia(traco)}, abaixo da de " +
                "superficie-afundada (${luminancia(SuperficieAfundada)}): os pares de " +
                "docs/02 §2.2 deixam de valer sobre o padrão.",
            luminancia(traco) >= luminancia(SuperficieAfundada),
        )
    }

    @Test
    fun `o padrao so aparece nos lugares dele`() {
        val fora = fontes()
            .filter { arquivo -> arquivo.name !in lugaresDoPadrao }
            .filter { arquivo -> arquivo.readLines().any { "padraoDePokebolas(" in it.substringBefore("//") } }
            .map { it.name }
            .toList()
        assertTrue(
            "O padrão de Pokébolas apareceu fora do cabeçalho da Pokédex e do EstadoVazio " +
                "(docs/02 §4.4): $fora. Um lugar novo ganha linha em docs/02 §12 e entra em " +
                "lugaresDoPadrao.",
            fora.isEmpty(),
        )
    }

    @Test
    fun `todo estado vazio desenha o EstadoVazio`() {
        val semPadrao = stringsDeVazio.flatMap { nome ->
            val usos = fontes().filter { "R.string.$nome" in it.readText() }.toList()
            assertTrue("Ninguém mostra R.string.$nome: a lista de vazios ficou velha.", usos.isNotEmpty())
            usos.filterNot { chamadaDoEstadoVazio.containsMatchIn(it.readText()) }.map { "${it.name} mostra $nome" }
        }
        assertTrue(
            "Estado vazio sem o padrão de Pokébolas (docs/02 §4.2):\n" + semPadrao.joinToString("\n"),
            semPadrao.isEmpty(),
        )
    }

    private fun fontes(): Sequence<File> =
        File(raizDeFontes(), "com/hggabriel/pokerun/ui")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
}

/** Luminância relativa do WCAG 2.1, a mesma fórmula de `TemaTest`. */
private fun luminancia(cor: Color): Double {
    fun canal(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * canal(cor.red) + 0.7152 * canal(cor.green) + 0.0722 * canal(cor.blue)
}

/** O diretório de fontes de produção. Mesma função de `ElevacaoTest`. */
private fun raizDeFontes(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        File(dir, "src/main/java").takeIf { it.isDirectory }?.let { return it }
        File(dir, "app/src/main/java").takeIf { it.isDirectory }?.let { return it }
        dir = dir.parentFile
    }
    error("Não achei src/main/java a partir de ${File("").absolutePath}")
}
