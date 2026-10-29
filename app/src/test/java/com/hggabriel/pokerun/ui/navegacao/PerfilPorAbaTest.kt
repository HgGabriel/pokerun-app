package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.componentes.AlvoDoAtalho
import com.hggabriel.pokerun.ui.componentes.BustoDoAtalho
import com.hggabriel.pokerun.ui.componentes.MolduraDoAtalho
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * O `Perfil` (até 27/10/2026, `Ajustes`) dentro de cada aba (`F1-T07c`, docs/03 §1 e docs/02 §10.1), escrito
 * **antes** da implementação.
 *
 * ### As duas regras que brigam, e por que o destino sozinho não resolve
 *
 * `docs/03 §1` desenha `Ajustes` **sob `Hoje`**, e a mesma seção manda a engrenagem
 * aparecer na **raiz de todas as abas**. Cumprir só a primeira — um destino no grafo
 * de `Hoje` — faz a barra acender `Hoje` quando alguém toca na engrenagem estando em
 * `Grupo`: a aba muda embaixo do usuário, e o voltar devolve à pilha errada. Foi por
 * causa desse defeito que `F1-T07` pôs `Ajustes` na pilha modal, contra o desenho.
 *
 * **A saída é `Ajustes` ser destino de cada grafo de aba**, e não um destino global. A
 * barra deriva a aba acesa da `hierarchy` do grafo (`CascaDeNavegacao`), então estando
 * em `AbaGrupo/Ajustes` quem acende é `Grupo` — por construção, sem a casca guardar de
 * onde o usuário veio. O voltar cai na raiz daquela aba, que é a outra metade.
 *
 * ### O que este arquivo prova, e o que ele não prova
 *
 * Navegação não renderiza em teste de JVM: não há Robolectric no projeto. **A aba
 * acesa e os três casos do voltar são verificação de aparelho**, e a ficha de `F1-T07c`
 * exige olho.
 *
 * O que dá para provar aqui é o que a revisão de olho não pega e o aparelho de hoje
 * também não: **as abas que ainda não existem**. `StatsDashboardScreen` é `F3-T09` e
 * `SocialLeaderboardScreen` é `F2-T12`; a Pokédex é `F4-T11`. Quem escrever a quarta
 * aba em outubro não vai reler `docs/02 §10.1` para descobrir que a engrenagem existe —
 * a varredura é o que faz a regra ser construção em vez de disciplina, do mesmo jeito
 * que `CabecalhoDeAba` faz no componente.
 *
 * **A varredura exige o bloco de cada aba, e não uma contagem no arquivo inteiro.** Um
 * registro a mais em `AbaHoje` e nenhum em `AbaGrupo` fecha qualquer conta global e
 * deixa o defeito de pé. E o registro é conferido em duas metades — a chamada em cada
 * aba **e** o destino dentro dela —, que é a lição de `CanalDeAlertaTest`, onde a
 * declaração da constante contava como uso e apagar o filete passava verde.
 */
class PerfilPorAbaTest {

    // ---------------------------------------------------------------------
    // A varredura precisa varrer alguma coisa
    // ---------------------------------------------------------------------

    @Test
    fun `a varredura encontra os grafos de aba`() {
        // Sem esta trava os dois testes seguintes passam vazios no dia em que alguém
        // reescrever a casca com outra forma de declarar aba: `all` sobre lista vazia é
        // verdadeiro, e o teste viraria carimbo. Três é o que docs/03 §2 dá às Fases 1
        // a 3; a Pokédex entra na Fase 4 e só faz o número subir.
        val abas = blocosDeAba(casca())

        assertTrue(
            "A varredura de `F1-T07c` achou ${abas.size} grafo(s) de aba em " +
                "`CascaDeNavegacao.kt`, e docs/03 §2 tem três. Se a forma de declarar " +
                "aba mudou, `blocosDeAba` tem de mudar junto — senão os testes desta " +
                "classe passam sem olhar nada",
            abas.size >= 3,
        )
    }

    // ---------------------------------------------------------------------
    // Um destino de Ajustes por aba (docs/03 §1)
    // ---------------------------------------------------------------------

    @Test
    fun `cada aba registra o seu proprio destino de Perfil`() {
        val semPerfil = blocosDeAba(casca())
            .filterValues { corpo -> "telaDePerfil()" !in corpo }
            .keys

        assertTrue(
            "${semPerfil.joinToString()} não chama(m) `telaDePerfil()`. O " +
                "atalho da raiz de toda aba (docs/02 §10.1) só mantém a aba acesa " +
                "se o destino pertencer ao grafo daquela aba — um destino global " +
                "acenderia `Hoje` (docs/03 §1)",
            semPerfil.isEmpty(),
        )
    }

    @Test
    fun `o registro de Perfil declara mesmo os destinos`() {
        // A metade que falta do teste acima, e a lição de `CanalDeAlertaTest`: a
        // chamada existir em cada aba não prova que ela registra coisa alguma. Uma
        // `telaDeAjustes()` esvaziada deixa as três abas passando e o app sem destino
        // de Ajustes em nenhuma delas.
        val corpo = casca().substringAfter("fun NavGraphBuilder.telaDePerfil()", "")
            .substringBefore("fun NavGraphBuilder.")

        listOf("composable<Perfil>", "composable<EscolherParceiro>", "composable<EscolherTreinador>").forEach { destino ->
            assertTrue(
                "`telaDePerfil()` existe mas não declara `$destino` — as chamadas nas abas " +
                    "registram um destino a menos",
                destino in corpo,
            )
        }
    }

    @Test
    fun `a raiz de cada aba fica debaixo da barra do topo`() {
        // Desde 28/10/2026 o atalho mora na barra do topo da casca, e não no cabeçalho de
        // cada raiz. O destino existir não põe a barra em cima da tela: a raiz precisa da
        // folga dela (`NaRaizDeAba`), senão a pílula cobre o título. É esta metade que uma
        // aba nova esquece.
        val semBarra = blocosDeAba(casca())
            .filterValues { corpo -> "NaRaizDeAba(" !in corpo }
            .keys

        assertTrue(
            "${semBarra.joinToString()} não envolve a raiz em `NaRaizDeAba`, e a barra do " +
                "topo (com o atalho do Perfil) mora na raiz de **toda** aba",
            semBarra.isEmpty(),
        )
    }

    @Test
    fun `a barra do topo abre o Perfil dentro da aba`() {
        val chamada = casca().substringAfter("BarraDoTopo(", "").substringBefore("fun NaRaizDeAba")

        assertTrue("A casca não desenha a `BarraDoTopo`", chamada.isNotEmpty())
        assertTrue(
            "A `BarraDoTopo` tem de abrir o Perfil pela pilha da aba, com `navigate(Perfil)`",
            "navigate(Perfil)" in chamada,
        )
    }

    // ---------------------------------------------------------------------
    // E fora da aba, em lugar nenhum
    // ---------------------------------------------------------------------

    @Test
    fun `Perfil nao mora na pilha modal`() {
        // O caminho de volta do defeito é deixar as duas declarações no lugar: a de
        // dentro da aba compila e a da pilha modal também, e `aoAbrirModal(Ajustes)`
        // continua achando a de fora. A tela abriria sem barra nenhuma embaixo, que é o
        // sintoma que ninguém liga a esta regra.
        val modal = File(raizDeFontes(), "$PACOTE/NavegacaoDoApp.kt").readText()

        assertTrue(
            "`NavegacaoDoApp.kt` declara `composable<Perfil>`. docs/03 §1 lista " +
                "a pilha modal e o Perfil não está nela — ele é destino de aba desde " +
                "`F1-T07c`",
            "composable<Perfil>" !in modal,
        )
    }

    @Test
    fun `o atalho nao abre pela pilha modal`() {
        // `aoAbrirModal` leva ao `NavHost` de fora, que é o que vive **sem** a barra
        // inferior. Mandar `Ajustes` por ele desfaz a tarefa inteira sem apagar nenhum
        // dos destinos que os testes acima conferem.
        val violacoes = File(raizDeFontes(), PACOTE)
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { arquivo ->
                arquivo.readLines().asSequence().mapIndexedNotNull { i, linha ->
                    "${arquivo.name}:${i + 1}".takeIf { "aoAbrirModal(Perfil)" in linha }
                }
            }
            .toList()

        assertTrue(
            "O atalho tem de navegar dentro da casca, e não pela pilha modal — " +
                "senão a tela abre fora da barra e a aba deixa de existir: " +
                violacoes.joinToString(),
            violacoes.isEmpty(),
        )
    }

    // ---------------------------------------------------------------------
    // O atalho: o busto do treinador (estudo de Progresso e Perfil, Revisão 3)
    // ---------------------------------------------------------------------

    @Test
    fun `o atalho tem alvo de 48dp e a foto emoldurada cabe nele`() {
        // 27/10: o busto encolheu para 38dp e ganhou moldura branca de 3dp. A foto inteira (44dp)
        // continua dentro do alvo de toque.
        assertEquals(48.dp, AlvoDoAtalho)
        assertEquals(38.dp, BustoDoAtalho)
        assertTrue(BustoDoAtalho + MolduraDoAtalho * 2 <= AlvoDoAtalho)
    }

    @Test
    fun `a descricao falada do atalho diz Seu perfil e o treinador`() {
        val strings = textosDoApp()

        assertEquals("Seu perfil", strings["perfil_atalho"])
        assertEquals("Seu perfil, treinador %1\$s", strings["perfil_atalho_com_treinador"])
    }

    @Test
    fun `a barra do topo desenha o atalho e nao a engrenagem`() {
        val barra = File(raizDeFontes(), "$PACOTE/BarraDoTopo.kt").readText()
            .substringAfter("fun BarraDoTopo(")

        assertTrue("AtalhoDoPerfil(" in barra)
        assertTrue("Icons.Default.Settings" !in barra)
    }
}

private const val PACOTE = "com/hggabriel/pokerun/ui/navegacao"

private fun casca(): String = File(raizDeFontes(), "$PACOTE/CascaDeNavegacao.kt").readText()

/**
 * Os corpos dos grafos de aba, por nome do grafo.
 *
 * Casa `navigation<X>` e devolve o que está entre as chaves do bloco, para cada aba ser
 * conferida **sozinha**. Contar ocorrências no arquivo inteiro deixaria duas em `Hoje`
 * e zero em `Grupo` passarem.
 */
private fun blocosDeAba(fonte: String): Map<String, String> =
    Regex("""navigation<(\w+)>""").findAll(fonte).associate { casamento ->
        val abertura = fonte.indexOf('{', casamento.range.last)
        require(abertura >= 0) { "`${casamento.value}` sem bloco" }
        casamento.groupValues[1] to corpoDoBloco(fonte, abertura)
    }

/** O texto entre `{` e o `}` que o fecha, contando aninhamento. */
private fun corpoDoBloco(fonte: String, abertura: Int): String {
    var profundidade = 0
    for (i in abertura until fonte.length) {
        when (fonte[i]) {
            '{' -> profundidade++
            '}' -> if (--profundidade == 0) return fonte.substring(abertura + 1, i)
        }
    }
    error("Bloco aberto em $abertura nunca fecha — `blocosDeAba` não serve mais")
}

/**
 * O diretório de fontes de produção, a partir de onde o Gradle rodar o teste.
 *
 * Cópia deliberada da mesma função em `TemaTest` e
 * `CanalDeAlertaTest`, pelo motivo que a primeira delas registra: promovê-la a
 * utilitário compartilhado seria reestruturação que a entrega não pedia.
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

/** Os valores de `values/strings.xml`, pelo nome. */
private fun textosDoApp(): Map<String, String> {
    val arquivo = File(raizDeFontes().parentFile, "res/values/strings.xml")
    val documento = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(arquivo)
    val nos = documento.getElementsByTagName("string")
    return (0 until nos.length).associate { i ->
        val no = nos.item(i) as org.w3c.dom.Element
        no.getAttribute("name") to no.textContent
    }
}
