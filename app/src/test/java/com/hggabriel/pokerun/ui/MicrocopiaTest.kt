package com.hggabriel.pokerun.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * O lint de microcopy (`F1-T20`, docs/02 §9.2 item 3 e §9.1).
 *
 * Existe porque **a regra escapou no documento que a define**: `01 §5.3` e `03 §3.14`
 * traziam `Corrida corrigida — 12/09` até 06/08. Uma proibição que escapa em duas
 * páginas de especificação escapa em vinte telas, e nenhum revisor humano distingue
 * `—` de `-` de `·` numa linha de diff.
 *
 * Duas proibições, as duas de docs/02 §9.1:
 *
 * - **Travessão em microcopy** (`—`, `–`) — vício de linguagem de LLM. O separador do
 *   sistema é o ponto médio `·`, que já aparece 12 vezes aqui.
 * - **Emoji como ícone** — infantiliza a interface e polui a hierarquia. O sistema tem
 *   ícones vetoriais e mono; não tem uma única figura decorativa.
 *
 * ### Por que o alvo é o valor da string, e não o arquivo inteiro
 *
 * Os comentários de `strings.xml` são documentação de quem escreve o app, não texto de
 * tela: eles citam seções da spec e explicam decisões, em prosa, onde o travessão é
 * pontuação legítima. Varrer o arquivo cru reprovaria hoje em dois comentários corretos
 * e ensinaria a próxima pessoa a contornar o lint — que é como uma defesa morre. O
 * parser de XML do JDK entrega só os valores e descarta o resto, sem dependência nova.
 *
 * ### Por que a contagem é um teste
 *
 * Um lint que não acha nada e um lint que não lê nada devolvem a mesma tela verde. Se o
 * nome do arquivo mudar, se o `values/` virar `values-pt-rBR/` ou se a busca deixar de
 * casar, [o lint enxerga a microcopy inteira] cai — e é ele que impede que os outros
 * dois passem a vida inteira sem ter olhado uma linha.
 *
 * > A tarefa que roda isto declara `src/main/res` como entrada em `build.gradle.kts`.
 * > Sem essa linha, editar `strings.xml` não muda o classpath, o `testDebugUnitTest`
 * > fica `UP-TO-DATE` e o lint não roda (`EXECUCAO.md §8.2`, armadilha 5).
 */
class MicrocopiaTest {

    /** `—` (em dash) e `–` (en dash). O hífen comum não entra: ele é pontuação. */
    private val travessoes = mapOf(
        '—' to "travessão (—)",
        '–' to "meia-risca (–)",
    )

    /**
     * As faixas de emoji e de pictograma, por ponto de código.
     *
     * Fora delas ficam de propósito o ponto médio `·` (U+00B7), que é o separador do
     * sistema, as reticências `…` (U+2026) e os acentos do português — os três aparecem
     * na microcopy de hoje e nenhum é figura.
     */
    private val faixasDeEmoji = listOf(
        0x1F000..0x1FAFF, // pictogramas, emoticons, transporte, símbolos suplementares
        0x2600..0x27BF, // símbolos diversos e dingbats: ☀ ✨ ➡
        0x2B00..0x2BFF, // setas e formas geométricas suplementares
        0xFE00..0xFE0F, // seletores de variação — o que transforma um glifo em emoji
        0x1F1E6..0x1F1FF, // bandeiras
    )

    // -----------------------------------------------------------------------
    // As duas proibições
    // -----------------------------------------------------------------------

    @Test
    fun `nenhuma microcopy usa travessao`() {
        val violacoes = microcopy().flatMap { (nome, texto) ->
            texto.mapIndexedNotNull { i, ch ->
                travessoes[ch]?.let { "$nome tem ${it} na posição $i: \"${trecho(texto, i)}\"" }
            }
        }

        assertTrue(
            "docs/02 §9.1 proíbe travessão em microcopy — o separador do sistema é `·`, " +
                "e a frase direta em voz ativa dispensa o conector.\n" +
                violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }

    @Test
    fun `nenhuma microcopy usa emoji`() {
        val violacoes = mutableListOf<String>()

        microcopy().forEach { (nome, texto) ->
            var i = 0
            while (i < texto.length) {
                val ponto = texto.codePointAt(i)
                if (faixasDeEmoji.any { ponto in it }) {
                    violacoes += "$nome tem U+%04X na posição %d: \"%s\""
                        .format(ponto, i, trecho(texto, i))
                }
                i += Character.charCount(ponto)
            }
        }

        assertTrue(
            "docs/02 §9.1 proíbe emoji como ícone — o sistema usa ícone vetorial e mono.\n" +
                violacoes.joinToString("\n"),
            violacoes.isEmpty(),
        )
    }

    // -----------------------------------------------------------------------
    // A guarda contra o verde vazio
    // -----------------------------------------------------------------------

    @Test
    fun `o lint enxerga a microcopy inteira`() {
        val lida = microcopy()

        // O piso é a contagem de 25/08 com margem: 260 `<string>` e 20 `<item>` de
        // plural. Ele não precisa acompanhar cada string nova — precisa cair no dia em
        // que a leitura devolver um punhado, que é a única forma de os dois testes de
        // cima passarem sem ter olhado nada.
        assertTrue(
            "O lint leu só ${lida.size} entradas de ${arquivoDeStrings().path}. " +
                "O arquivo mudou de forma e a varredura ficou vazia.",
            lida.size >= 250,
        )

        // E que ele leu **valor**, não nó vazio.
        assertTrue(
            "Nenhuma entrada tem texto: a leitura casou os nós e perdeu o conteúdo.",
            lida.count { it.second.isNotBlank() } >= 250,
        )
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    /**
     * Toda microcopy do app, como pares `nome do recurso` para `texto`.
     *
     * `<string>` e os `<item>` de `<plurals>`, que é tudo que o `strings.xml` tem hoje.
     * `<string-array>` cai no mesmo laço no dia em que existir, porque a busca é pelo
     * nome da tag e não pelo pai.
     */
    private fun microcopy(): List<Pair<String, String>> {
        val documento = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(arquivoDeStrings())

        val entradas = mutableListOf<Pair<String, String>>()

        listOf("string", "item").forEach { tag ->
            val nos = documento.getElementsByTagName(tag)
            for (i in 0 until nos.length) {
                val no = nos.item(i) as Element
                val dono = (no.parentNode as? Element)?.getAttribute("name").orEmpty()
                val nome = no.getAttribute("name").ifBlank {
                    "$dono[${no.getAttribute("quantity")}]"
                }
                entradas += nome to no.textContent
            }
        }

        return entradas
    }

    /** O trecho ao redor do achado, para a mensagem nomear a frase e não só a posição. */
    private fun trecho(texto: String, posicao: Int): String =
        texto.substring(maxOf(0, posicao - 20), minOf(texto.length, posicao + 20))

    /**
     * `app/src/main/res/values/strings.xml`, a partir de onde o Gradle rodar o teste.
     *
     * Sobe até achar, como [com.hggabriel.pokerun.ui.theme.TemaTest] faz com os fontes:
     * o working dir do `testDebugUnitTest` não é garantido entre versões do AGP.
     */
    private fun arquivoDeStrings(): File {
        val relativo = "src/main/res/values/strings.xml"
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            File(dir, relativo).takeIf { it.isFile }?.let { return it }
            File(dir, "app/$relativo").takeIf { it.isFile }?.let { return it }
            dir = dir.parentFile
        }
        error("Não achei $relativo a partir de ${File("").absolutePath}")
    }
}
