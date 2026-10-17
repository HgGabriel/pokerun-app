package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O cabeçalho de ficha (`F1-T07b`, docs/02 §10).
 *
 * O que se verifica sem tela é **a regra do índice** (§10.3), e é exatamente o que a
 * revisão de olho deixa passar: numeração ornamental é um dos vícios de layout gerado
 * automaticamente, e a diferença entre `SEMANA 3` e `SEMANA 3 DE 21` some numa captura
 * de tela.
 *
 * Desde 17/10/2026 o teste não varre as telas atrás de `TopAppBar`: o cabeçalho como
 * conteúdo virou intenção com trade-off (§10.4), e uma tela que fuja dele é desvio
 * registrado em docs/02 §12.
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
}
