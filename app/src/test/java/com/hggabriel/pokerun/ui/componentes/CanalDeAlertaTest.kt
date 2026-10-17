package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O canal de alerta (`F1-T06b`, docs/02 §2.4).
 *
 * A regra não abre exceção: **todo** uso de `alerta` carrega, junto, três coisas — o
 * bloco em `alerta-suave` com contorno em `alerta`, o ícone de triângulo em traço e o
 * rótulo em caixa alta no estilo `rotulo`. `destaque` e `alerta` ficam a ΔE 13,8 sob
 * protanopia: quem tem dicromacia reconhece o aviso **pela forma** antes da cor.
 *
 * **Reescrito em 14/10 para o design system novo.** A versão anterior exigia um filete
 * vertical de 3dp na borda esquerda e o rótulo em mono. O design system de 14/10 tirou
 * as duas coisas: o filete lateral colorido virou vício de docs/02 §9.1 (o canal redundante é o
 * ícone e o rótulo) e o sistema não tem mais fonte mono (o rótulo é Poppins, estilo
 * `rotulo`). O traço do ícone passou de 1,5dp para 1,75dp, o do conjunto de ícones.
 *
 * Nada aqui renderiza. O que dá para provar sem aparelho são as duas metades que a
 * revisão de olho não pega:
 *
 * 1. **As peças continuam lá, com as medidas certas, e o filete não volta.** Um
 *    triângulo sólido compila igual; um rótulo em `bodyMedium` compila igual e deixa
 *    de ser `rotulo`; um filete compila igual. Nenhum aparece num diff como defeito.
 * 2. **Nenhuma tela pinta `alerta` com a própria mão.** É esta metade que faz as
 *    telas herdarem o canal sem ninguém lembrar dele.
 */
class CanalDeAlertaTest {

    // ---------------------------------------------------------------------
    // As peças de forma de cada tipo (docs/02 §2.4, `Alerta.md`)
    // ---------------------------------------------------------------------

    @Test
    fun `alerta leva triangulo e contorno`() {
        assertEquals(PecasDoAviso(IconeDoAviso.Triangulo, contorno = true), pecasDoAviso(TipoDeAviso.Alerta))
    }

    @Test
    fun `sucesso leva check e informacao leva i`() {
        assertEquals(IconeDoAviso.Check, pecasDoAviso(TipoDeAviso.Sucesso).icone)
        assertEquals(IconeDoAviso.Info, pecasDoAviso(TipoDeAviso.Informacao).icone)
    }

    @Test
    fun `cada tipo tem um icone diferente`() {
        // Cor nunca é o único canal: se dois tipos dividissem o ícone, só a cor os
        // separaria.
        val icones = TipoDeAviso.entries.map { pecasDoAviso(it).icone }
        assertEquals(icones.size, icones.toSet().size)
    }

    @Test
    fun `o icone tem o traco do conjunto e o contorno tem 1dp`() {
        // O `Filled.Warning` do núcleo do Material é um triângulo **sólido**, e é o
        // caminho de menor esforço para quem mexer nisto depois. §2.4 pede traço.
        assertEquals(1.75.dp, TracoDoIcone)
        assertEquals(1.dp, ContornoDoAlerta)
    }

    // ---------------------------------------------------------------------
    // O rótulo em caixa alta (docs/02 §2.4)
    // ---------------------------------------------------------------------

    @Test
    fun `o rotulo sai em caixa alta`() {
        assertEquals("RISCO DE LESÃO", rotuloDoCanal("Risco de lesão"))
    }

    @Test
    fun `a caixa alta preserva os acentos do portugues`() {
        // `Locale` do sistema não serve: em turco o `i` de `sincronização` vira `İ`, e a
        // máquina de quem executa não é a máquina de quem lê. O locale é o do app.
        assertEquals("FALHA DE SINCRONIZAÇÃO", rotuloDoCanal("Falha de sincronização"))
    }

    @Test
    fun `rotulo em branco falha`() {
        // Canal sem rótulo é canal com duas peças. Passar `""` para calar o rótulo é o
        // atalho óbvio de quem achar o rótulo redundante numa tela específica, e §2.4
        // não abre exceção — então ele precisa quebrar aqui e não na revisão.
        val erro = runCatching { rotuloDoCanal("   ") }.exceptionOrNull()

        assertTrue(
            "Rótulo vazio deixa o alerta com duas peças (docs/02 §2.4)",
            erro is IllegalArgumentException,
        )
    }

    // ---------------------------------------------------------------------
    // As três juntas, e não duas delas
    // ---------------------------------------------------------------------

    @Test
    fun `o canal desenha as tres pecas no mesmo bloco`() {
        // As medidas acima continuam certas mesmo se a peça for apagada do desenho: a
        // constante sobrevive sem ninguém usá-la. O que prova a peça é ela estar
        // **referenciada**, e não só declarada. Daí o mínimo de duas ocorrências para o
        // que este arquivo declara, e de uma para o que ele só usa.
        val canal = File(raizDeFontes(), CAMINHO_DO_CANAL).readText()

        val pecas = mapOf(
            "o contorno em alerta" to ("ContornoDoAlerta" to 2),
            "o fundo em alerta-suave" to ("alertaSuave" to 1),
            "o triângulo em traço" to ("TracoDoIcone" to 2),
            "o rótulo em caixa alta" to ("rotuloDoCanal(" to 2),
            "o rótulo no estilo rotulo" to ("labelMedium" to 1),
        )

        val ausentes = pecas.filterValues { (nome, minimo) ->
            canal.split(nome).size - 1 < minimo
        }.keys

        assertTrue(
            "O bloco de alerta perdeu ${ausentes.joinToString()} — e docs/02 §2.4 " +
                "exige as três peças juntas, nunca duas",
            ausentes.isEmpty(),
        )
    }

    @Test
    fun `o canal nao tem filete lateral`() {
        // O filete compila igual e parece reforço. É vício de docs/02 §9.1: o canal
        // redundante já é o ícone e o rótulo. Os comentários ficam de fora da busca,
        // porque o KDoc explica a ausência.
        if (FILETE_REGISTRADO) return
        val codigo = File(raizDeFontes(), CAMINHO_DO_CANAL).readLines()
            .map { it.substringBefore("//") }
            .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("/**") }
            .joinToString("\n")

        assertFalse("O bloco voltou a ter filete", "filete" in codigo.lowercase())
        assertFalse("O bloco voltou a ter uma faixa de altura cheia", "fillMaxHeight" in codigo)
    }

    // ---------------------------------------------------------------------
    // Nenhuma tela pinta `alerta` com a própria mão
    // ---------------------------------------------------------------------

    /**
     * As formas de usar `alerta` sem o canal, e as únicas que alguém escreveria sem
     * perceber. `isError` entra na lista porque ele pinta contorno e rótulo do campo
     * em `error` — é uso de `alerta`, e sozinho não tem nenhuma das três peças.
     */
    private val proibidos = listOf(
        "colorScheme.error",
        "errorContainer",
        "isError",
    )

    /**
     * **Vazia desde `F1-T06c`, em 17/08, e é para continuar assim.**
     *
     * Ela nasceu com cinco nomes. `F1-T06b` converteu as duas telas que a ficha dela
     * previa e a varredura achou **mais cinco** — `CriarPlanoScreen`,
     * `RevisarRascunhoScreen`, `DetalhePlanoScreen`, `EntrarComCodigoScreen` e
     * `ListaDePlanosScreen` —, porque a decisão nº 12 é de 12/08, quando só duas telas
     * existiam, e as outras nasceram entre 10/08 e 14/08 copiando o idioma da vizinha.
     *
     * As cinco ficaram listadas aqui, nomeadas e datadas, em vez de virarem um commit
     * que reabria sete telas de uma vez. `F1-T06c` drenou a lista no mesmo dia.
     *
     * **Acrescentar nome aqui é reabrir a dívida**, e nenhuma tela nova precisa disso:
     * `CampoComErro` cobre erro de campo e `BannerDeAlerta` cobre falha de tela. Se a
     * tentação aparecer, o que falta é parâmetro no componente, não exceção na lista.
     */
    private val dividaDeF1T06c = emptySet<String>()

    @Test
    fun `nenhuma tela pinta alerta com a propria mao`() {
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name !in dividaDeF1T06c }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    proibidos.firstOrNull { it in linha }?.let { "${arquivo.name}:${i + 1} usa $it" }
                }
            }
            .toList()

        assertTrue(
            "Erro sai pelo canal de docs/02 §2.4, e o canal mora em `ui/componentes`: " +
                "`CampoComErro` para erro de campo, `BannerDeAlerta` para falha de tela. " +
                "Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }

    @Test
    fun `a divida de F1-T06c so encolhe`() {
        // Uma tela sai da lista quando é convertida, e a linha some daqui junto. O teste
        // existe para o caminho contrário: uma entrada que não corresponde mais a
        // violação nenhuma é lista mentindo sobre o tamanho da dívida, e é assim que
        // `F1-T06c` fecharia sem ter drenado nada.
        val telas = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { it.name to it.readText() }

        val jaConvertidas = dividaDeF1T06c.filter { nome ->
            val fonte = telas[nome] ?: return@filter true
            proibidos.none { it in fonte }
        }

        assertTrue(
            "Estas telas estão na dívida de `F1-T06c` e não violam mais nada. " +
                "Tire-as da lista: $jaConvertidas",
            jaConvertidas.isEmpty(),
        )
    }
}

/**
 * O filete lateral é vício de docs/02 §9.1, e vício entra com uma linha em docs/02 §12.
 * Quem registrar o filete troca isto para `true` no mesmo commit, com a data da linha
 * no comentário. Sem a linha, fica `false`.
 */
private const val FILETE_REGISTRADO = false

/** O arquivo que desenha o canal, relativo à raiz de fontes. */
private const val CAMINHO_DO_CANAL =
    "com/hggabriel/pokerun/ui/componentes/BannerDeAlerta.kt"

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 *
 * Cópia da mesma função de `TemaTest`, que é `private` de um arquivo de fase anterior.
 * Promovê-la a utilitário compartilhado é reestruturação que a entrega não pedia, e
 * dez linhas duplicadas custam menos que uma regressão silenciosa na varredura que
 * protege a fundação de cor.
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
