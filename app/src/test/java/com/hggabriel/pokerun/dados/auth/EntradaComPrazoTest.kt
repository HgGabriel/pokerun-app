package com.hggabriel.pokerun.dados.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A leitura do desfecho da entrada com o Google (`F1-T22`, docs/03 §3.1).
 *
 * Escrito **antes** da implementação (`EXECUCAO.md §3.2`), e cada caso aqui nasceu de
 * um defeito **visto em aparelho** em 20/09, na `F1-T18` — não de leitura de código.
 *
 * ### O que o aparelho mostrou, e que o emulador nunca mostrou
 *
 * Um Xiaomi com **nove contas Google** e sinal de celular fraco (`RSRP -116`) produziu
 * as duas metades do defeito, em tentativas seguidas:
 *
 * - **A primeira tentativa levou 10 segundos e foi cancelada pelo sistema.** O app
 *   traduziu o cancelamento para *"Não há conta Google neste aparelho"* — uma
 *   instrução **falsa**, que manda a pessoa adicionar a décima conta.
 * - **A segunda recebeu a credencial em 0,45 s**, o sistema pediu para desenhar a
 *   folha (`ui invocation is needed`) e **a folha nunca apareceu**. O app girou por
 *   mais de um minuto, sem erro, sem saída e sem prazo próprio.
 *
 * ### Por que isto é função pura, e não teste de corrotina
 *
 * Testar o `withTimeout` de verdade pediria `kotlinx-coroutines-test`, que **não é
 * dependência deste projeto** (`EXECUCAO.md §4.3`: nenhuma dependência nova sem
 * escalar). A saída é a que o próprio protocolo sugere: **extrair a decisão para
 * função pura** e testar ela. O `withTimeout` fica com uma linha sem ramo — o que a
 * suíte não alcança é a chamada do SDK, que nenhum teste de unidade alcançaria mesmo.
 */
class EntradaComPrazoTest {

    // -------------------------------------------------------------------------
    // desfechoDaEspera — o que o prazo e a resposta, juntos, significam
    // -------------------------------------------------------------------------

    @Test
    fun `token que chegou dentro do prazo e entrada`() {
        assertEquals(
            DesfechoDaEspera.Respondeu,
            desfechoDaEspera(token = "token-do-google", estourouOPrazo = false),
        )
    }

    @Test
    fun `prazo estourado e Demorou, e nao ausencia de conta`() {
        // **O defeito de 20/09, em uma linha.** O app dizia "não há conta Google neste
        // aparelho" para um aparelho com nove. São perguntas diferentes, e respondê-las
        // com a mesma frase é o que produz a instrução impossível de seguir.
        assertEquals(
            DesfechoDaEspera.Demorou,
            desfechoDaEspera(token = null, estourouOPrazo = true),
        )
    }

    @Test
    fun `resposta vazia sem prazo estourado e falha, nao demora`() {
        // O sistema respondeu e não trouxe token: é falha de configuração ou de rede,
        // e a tela oferece repetir. Chamar isso de demora mandaria a pessoa esperar
        // por algo que já terminou.
        assertEquals(
            DesfechoDaEspera.Falhou,
            desfechoDaEspera(token = null, estourouOPrazo = false),
        )
    }

    @Test
    fun `prazo estourado vence token que chegou tarde`() {
        // O log do aparelho mostrou exatamente isto: cancelado em 10 s, e o token
        // chegando 0,5 s **depois**. Quem chega depois do prazo não entra — a tela já
        // seguiu, e aceitar o retardatário autenticaria alguém que desistiu.
        assertEquals(
            DesfechoDaEspera.Demorou,
            desfechoDaEspera(token = "chegou tarde", estourouOPrazo = true),
        )
    }

    @Test
    fun `os tres desfechos sao alcancaveis e nao ha um quarto`() {
        val vistos = listOf(null, "token").flatMap { token ->
            listOf(true, false).map { desfechoDaEspera(token, it) }
        }

        assertEquals(DesfechoDaEspera.entries.toSet(), vistos.toSet())
    }

    // -------------------------------------------------------------------------
    // O prazo em si
    // -------------------------------------------------------------------------

    @Test
    fun `o prazo da folga a rede ruim sem prender a pessoa`() {
        // O aparelho de 20/09 cancelou sozinho em 10 s. O prazo do app tem de ser
        // **maior** que isso, ou ele corta antes de o sistema ter chance de responder,
        // e **menor** que a paciência de quem está olhando uma tela parada.
        assertTrue("prazo curto demais corta o sistema", PRAZO_DA_ENTRADA_MS > 10_000)
        assertTrue("prazo longo demais é a tela travada de volta", PRAZO_DA_ENTRADA_MS <= 30_000)
    }
}
