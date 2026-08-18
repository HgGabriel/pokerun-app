package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O canal de alerta (`F1-T06b`, docs/02 §2.4), escrito **antes** da implementação
 * (`EXECUCAO.md §3.2`).
 *
 * A regra não abre exceção: **todo** uso de `alerta` carrega, obrigatoriamente e
 * junto, três coisas — ícone de triângulo em traço de 1,5dp, filete vertical de 3dp
 * na borda esquerda do bloco e rótulo em mono caixa alta. Ela existe porque o token
 * foi remedido em 06/08 justamente por colapsar contra `leitura` sob deuteranopia
 * (§2.1): quem tem dicromacia reconhece o aviso **pela forma** antes de reconhecer
 * pela cor.
 *
 * Nada aqui renderiza. O que dá para provar sem aparelho são as duas metades que a
 * revisão de olho não pega:
 *
 * 1. **As três peças continuam lá, com as medidas certas.** Um filete de 1dp compila
 *    igual e vira `borda`, que é decorativa; um triângulo sólido compila igual; um
 *    rótulo em `bodyMedium` compila igual e deixa de ser mono. Nenhum dos três
 *    aparece num diff como defeito.
 * 2. **Nenhuma tela pinta `alerta` com a própria mão.** É esta metade que faz as
 *    telas restantes da Fase 1 herdarem o canal sem ninguém lembrar dele — que é a
 *    razão de `F1-T06b` ser um componente e não duas correções.
 */
class CanalDeAlertaTest {

    // ---------------------------------------------------------------------
    // Peça 1 — o filete de 3dp (docs/02 §2.4)
    // ---------------------------------------------------------------------

    @Test
    fun `o filete do canal tem 3dp`() {
        // 1dp aqui seria `borda`, que docs/02 §2.1 declara **decorativa** e proíbe como
        // único delimitador. O filete de alerta não delimita: ele é canal de forma, e é
        // a espessura que o separa do divisor.
        assertEquals(3.dp, LarguraDoFilete)
    }

    // ---------------------------------------------------------------------
    // Peça 2 — o triângulo em traço de 1,5dp (docs/02 §2.4)
    // ---------------------------------------------------------------------

    @Test
    fun `o traco do triangulo tem 1,5dp`() {
        // O `Filled.Warning` do núcleo do Material é um triângulo **sólido**, e é o
        // caminho de menor esforço para quem mexer nisto depois. §2.4 pede traço.
        assertEquals(1.5.dp, TracoDoTriangulo)
    }

    // ---------------------------------------------------------------------
    // Peça 3 — o rótulo em caixa alta (docs/02 §2.4)
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
        // As medidas acima continuam certas mesmo se o `Spacer` do filete for apagado:
        // a constante sobrevive sem ninguém usá-la. O que prova a peça é ela estar
        // **referenciada**, e não só declarada.
        //
        // A primeira versão deste teste procurava o nome no arquivo e parava aí. Um
        // defeito plantado — apagar o `Spacer` do filete e deixar a constante — passou
        // por ele: a linha da declaração era a ocorrência que ele encontrava. Daí o
        // mínimo de duas para o que este arquivo declara, e de uma para o que ele só
        // usa.
        val canal = File(raizDeFontes(), CAMINHO_DO_CANAL).readText()

        val pecas = mapOf(
            "o filete de 3dp" to ("LarguraDoFilete" to 2),
            "o triângulo em traço de 1,5dp" to ("TracoDoTriangulo" to 2),
            "o rótulo em caixa alta" to ("rotuloDoCanal(" to 2),
            "o rótulo em mono" to ("labelMedium" to 1),
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

    /**
     * A `DumpScreen` não é uma das vinte telas: é instrumento de medição de `F0-T10` e
     * o KDoc dela já a declara descartável. Ela sai por não ser tela do app, e não por
     * dívida — não há nada para `F1-T06c` drenar ali.
     */
    private val foraDoApp = setOf("DumpScreen.kt")

    @Test
    fun `nenhuma tela pinta alerta com a propria mao`() {
        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name !in dividaDeF1T06c && it.name !in foraDoApp }
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

/** O arquivo que desenha o canal, relativo à raiz de fontes. */
private const val CAMINHO_DO_CANAL =
    "com/hggabriel/pokerun/ui/componentes/BannerDeAlerta.kt"

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 *
 * Terceira cópia da mesma função — `TemaTest` e `CabecalhoDeFichaTest` têm as outras
 * duas, e as duas são `private` de arquivos de fases anteriores. Promovê-la a
 * utilitário compartilhado é mexer em teste de fase anterior de passagem
 * (`EXECUCAO.md §6`), e dez linhas duplicadas custam menos que uma regressão silenciosa
 * na varredura que protege a fundação de cor.
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
