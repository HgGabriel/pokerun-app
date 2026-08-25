package com.hggabriel.pokerun.ui

import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.campoEmUmaLinha
import com.hggabriel.pokerun.ui.componentes.rotuloDoSegmentoPendente
import com.hggabriel.pokerun.ui.componentes.rotuloSaiDoCampo
import com.hggabriel.pokerun.ui.navegacao.DestinoDeTopo
import com.hggabriel.pokerun.ui.navegacao.rotuloDe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * As quatro cessões de layout de `F1-T20` (docs/02 §8, item 9).
 *
 * O item 9 diz o que tem de acontecer e **quem cede**: *"nenhum rótulo trunca, nenhum
 * número quebra em duas linhas, nenhum bloco se sobrepõe (…) o layout é que cede"*. A
 * varredura de 25/08 em 320dp e `fontScale` 2,0 achou quatro lugares em que ele não
 * cedia, e os quatro estão em componente compartilhado — não em tela:
 *
 * | O que quebrava | Onde aparecia | Quem cede agora |
 * |---|---|---|
 * | `Progresso` em `Progr`/`esso` | barra inferior, nas três abas | o rótulo encurta |
 * | `prevista` em `previs`/`ta` | `HomeScreen` e `WeekDetailScreen` | o rótulo encurta |
 * | rótulo de 2 linhas cruzando o contorno | as três telas de distância | o rótulo sai do campo |
 * | a data truncada em `de 202` | `CriarPlanoScreen` e `ManualRunScreen` | o campo deixa de ser de uma linha |
 *
 * ### Por que isto é teste, e não olho
 *
 * Um limiar de escala não aparece em revisão de diff e não aparece na tela do
 * desenvolvedor, que roda em `fontScale` 1,0. Ele só falha na mão de quem aumentou a
 * fonte — que é exatamente a pessoa que o item 9 protege, e a que não vai reportar.
 *
 * **O limiar é o mesmo `ESCALA_QUE_EMPILHA` da grade de dias**, e é de propósito: um
 * segundo limiar faria o app trocar de forma em dois momentos diferentes ao arrastar a
 * régua de fonte, o que lê como defeito mesmo quando cada metade está certa.
 */
class CedeNaEscalaTest {

    /** `fontScale` 2,0, a escala em que a varredura achou os quatro. */
    private val ampliado = 2.0f

    /** A escala do aparelho de quem não mexeu em nada. */
    private val normal = 1.0f

    // -----------------------------------------------------------------------
    // 1 — o rótulo do segmento pendente
    // -----------------------------------------------------------------------

    @Test
    fun `na escala normal o segmento pendente diz prevista por extenso`() {
        assertEquals(R.string.semana_prevista, rotuloDoSegmentoPendente(normal))
    }

    @Test
    fun `ampliado o segmento pendente encurta em vez de quebrar no meio da palavra`() {
        // Em 320dp com três segmentos sobram uns 106dp por coluna, e `prevista` em mono
        // a 2,0 pede uns 117dp: a palavra partia em `previs`/`ta`, sem hífen, nas três
        // colunas. Encurtar é o que o item 9 manda fazer.
        assertEquals(R.string.semana_prevista_curta, rotuloDoSegmentoPendente(ampliado))
        assertNotEquals(R.string.semana_prevista, rotuloDoSegmentoPendente(ampliado))
    }

    @Test
    fun `o segmento pendente troca no mesmo limiar da grade de dias`() {
        assertEquals(R.string.semana_prevista, rotuloDoSegmentoPendente(ESCALA_QUE_EMPILHA))
        assertEquals(
            R.string.semana_prevista_curta,
            rotuloDoSegmentoPendente(ESCALA_QUE_EMPILHA + 0.01f),
        )
    }

    // -----------------------------------------------------------------------
    // 2 — o rótulo da barra de destinos
    // -----------------------------------------------------------------------

    @Test
    fun `na escala normal os tres destinos saem por extenso`() {
        assertEquals(R.string.destino_hoje, rotuloDe(DestinoDeTopo.HOJE, normal))
        assertEquals(R.string.destino_progresso, rotuloDe(DestinoDeTopo.PROGRESSO, normal))
        assertEquals(R.string.destino_grupo, rotuloDe(DestinoDeTopo.GRUPO, normal))
    }

    @Test
    fun `ampliado so encurta o destino que nao cabe`() {
        // `Hoje` e `Grupo` cabem em 106dp a 2,0 — foram vistos inteiros no emulador.
        // Encurtar os três por simetria trocaria um defeito real por duas perdas de
        // legibilidade sem causa.
        assertEquals(R.string.destino_hoje, rotuloDe(DestinoDeTopo.HOJE, ampliado))
        assertEquals(R.string.destino_grupo, rotuloDe(DestinoDeTopo.GRUPO, ampliado))
        assertEquals(
            R.string.destino_progresso_curto,
            rotuloDe(DestinoDeTopo.PROGRESSO, ampliado),
        )
    }

    @Test
    fun `o nome inteiro do destino continua existindo para o TalkBack`() {
        // O que encurta é o glifo, não o nome: quem ouve a barra continua ouvindo
        // `Progresso`. Sem isto, encurtar seria renomear a aba de docs/03 §2.
        DestinoDeTopo.entries.forEach { destino ->
            assertNotEquals(
                "o rótulo de acessibilidade de $destino não pode ser o abreviado",
                R.string.destino_progresso_curto,
                destino.rotulo,
            )
        }
    }

    // -----------------------------------------------------------------------
    // 3 — o rótulo flutuante que cruzava o contorno
    // -----------------------------------------------------------------------

    @Test
    fun `na escala normal o rotulo fica dentro do campo`() {
        assertFalse(rotuloSaiDoCampo(normal))
    }

    @Test
    fun `ampliado o rotulo sai do campo em vez de cruzar o contorno`() {
        // `Maior distância confortável hoje` flutua em duas linhas a 2,0 em 320dp, e o
        // Material corta o entalhe do contorno para UMA. A primeira linha ficava acima
        // do campo e a segunda atravessava a borda — o rótulo parecia pertencer ao
        // campo de cima.
        assertTrue(rotuloSaiDoCampo(ampliado))
    }

    @Test
    fun `o rotulo sai no mesmo limiar dos outros`() {
        assertFalse(rotuloSaiDoCampo(ESCALA_QUE_EMPILHA))
        assertTrue(rotuloSaiDoCampo(ESCALA_QUE_EMPILHA + 0.01f))
    }

    // -----------------------------------------------------------------------
    // 4 — a data truncada no campo que abre seletor
    // -----------------------------------------------------------------------

    @Test
    fun `campo que se digita continua em uma linha`() {
        // Num campo editável a linha única é comportamento correto: o texto rola dentro
        // dele, e quem digita alcança o que saiu da vista.
        assertTrue(campoEmUmaLinha(umaLinha = true, somenteLeitura = false))
    }

    @Test
    fun `campo somente leitura nunca fica em uma linha`() {
        // `18 de agosto de 2026` saía `18 de agosto de 202` em 320dp a 2,0, e `31 de
        // dezembro de 2026` saía `31 de dezembro d`. O campo é somente leitura e abre um
        // seletor no toque: **não há como rolar o texto**, então o ano ficava
        // inalcançável — que é truncar, e o item 9 proíbe.
        assertFalse(campoEmUmaLinha(umaLinha = true, somenteLeitura = true))
    }

    @Test
    fun `campo de varias linhas continua de varias linhas`() {
        assertFalse(campoEmUmaLinha(umaLinha = false, somenteLeitura = false))
        assertFalse(campoEmUmaLinha(umaLinha = false, somenteLeitura = true))
    }
}
