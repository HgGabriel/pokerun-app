package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O cabeçalho de ficha (`F1-T07b`, docs/02 §10).
 *
 * Duas coisas do componente são verificáveis sem tela, e as duas são exatamente as que
 * a revisão de olho deixa passar:
 *
 * 1. **A regra do índice** (§10.3). Numeração ornamental é um dos vícios de layout
 *    gerado automaticamente, e a diferença entre `SEMANA 3` e `SEMANA 3 DE 21` some
 *    numa captura de tela.
 * 2. **Nenhuma tela desenha o seu próprio cabeçalho** (§10). Uma `TopAppBar` no topo de
 *    uma tela compila, renderiza e parece certa — e é um segundo vocabulário de
 *    cabeçalho num app que tem um só.
 */
class CabecalhoDeFichaTest {

    // ---------------------------------------------------------------------
    // A sobrancelha (docs/02 §10.1)
    // ---------------------------------------------------------------------

    @Test
    fun `a sobrancelha sai em caixa alta e separada por ponto medio`() {
        assertEquals(
            "PROGRESSO · HISTÓRICO",
            sobrancelhaVisivel(listOf("Progresso", "Histórico"), indice = null),
        )
    }

    @Test
    fun `a sobrancelha nunca passa de dois niveis`() {
        val erro = runCatching {
            sobrancelhaVisivel(listOf("Progresso", "Corrida", "Splits"), indice = null)
        }.exceptionOrNull()

        assertTrue(
            "Três níveis não cabem numa linha e precisam falhar (docs/02 §10.1)",
            erro is IllegalArgumentException,
        )
    }

    @Test
    fun `sobrancelha vazia falha`() {
        val erro = runCatching { sobrancelhaVisivel(emptyList(), indice = null) }.exceptionOrNull()
        assertTrue(erro is IllegalArgumentException)
    }

    // ---------------------------------------------------------------------
    // A regra do índice (docs/02 §10.3)
    // ---------------------------------------------------------------------

    @Test
    fun `o indice sai na forma n de N, colado ao ultimo nivel`() {
        assertEquals(
            "HOJE · SEMANA 3 DE 21",
            sobrancelhaVisivel(listOf("Hoje", "Semana"), Indice(n = 3, total = 21)),
        )
    }

    @Test
    fun `sem indice nao ha numero nenhum`() {
        // A forma que a regra proíbe: o número escrito à mão dentro do nível, sem N.
        // Se isto passasse, `SEMANA 3` entraria numa tela e ninguém veria a diferença.
        val erro = runCatching {
            sobrancelhaVisivel(listOf("Hoje", "Semana 3"), indice = null)
        }.exceptionOrNull()

        assertTrue(
            "Número sem N tem de falhar: sem N não há número (docs/02 §10.3)",
            erro is IllegalArgumentException,
        )
    }

    @Test
    fun `o indice recusa total vazio e posicao fora da faixa`() {
        val semTotal = runCatching {
            sobrancelhaVisivel(listOf("Pokédex"), Indice(n = 1, total = 0))
        }.exceptionOrNull()
        assertTrue("Total zero não é um total que o usuário reconhece", semTotal is IllegalArgumentException)

        val foraDaFaixa = runCatching {
            sobrancelhaVisivel(listOf("Escada"), Indice(n = 9, total = 8))
        }.exceptionOrNull()
        assertTrue("9 de 8 não existe", foraDaFaixa is IllegalArgumentException)
    }

    // ---------------------------------------------------------------------
    // O que o TalkBack ouve
    // ---------------------------------------------------------------------

    @Test
    fun `a leitura falada vai em caixa mista e com virgula`() {
        // A caixa alta é desenho. Parte dos leitores de tela soletra palavra inteira em
        // maiúscula, e o ponto médio é pronunciado como glifo.
        assertEquals(
            "Hoje, Semana 3 de 21",
            sobrancelhaFalada(listOf("Hoje", "Semana"), Indice(n = 3, total = 21)),
        )
    }

    // ---------------------------------------------------------------------
    // Nenhuma tela desenha o seu próprio (docs/02 §10)
    // ---------------------------------------------------------------------

    @Test
    fun `nenhuma tela desenha o seu proprio cabecalho`() {
        // O caminho realista da violação é a `TopAppBar`: ela é o reflexo de quem vem do
        // Material, compila, e traz elevação, sombra ao rolar e um segundo vocabulário
        // de cabeçalho para um app que tem um só. O cabeçalho do PokéRun é conteúdo e
        // rola com a `LazyColumn` da tela.
        //
        // Não cobre toda forma possível de reinventar o cabeçalho — cobre a única que
        // alguém escreveria sem perceber que está reinventando.
        val proibidos = listOf("TopAppBar", "topBar =")

        val violacoes = File(raizDeFontes(), "com/hggabriel/pokerun/ui/telas")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    proibidos.firstOrNull { it in linha }?.let { "${arquivo.name}:${i + 1} usa $it" }
                }
            }
            .toList()

        assertTrue(
            "O cabeçalho de toda tela é o CabecalhoDeFicha de docs/02 §10, e ele é " +
                "conteúdo, não barra do Scaffold. Violações:\n" + violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }
}

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 *
 * Cópia deliberada da mesma função em `TemaTest`: ela é `private` de um arquivo de
 * `F0-T13`, e promovê-la a utilitário compartilhado seria mexer em teste de fase
 * anterior de passagem (`EXECUCAO.md §6`). Dez linhas duplicadas custam menos que uma
 * regressão silenciosa na fundação de cor.
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
