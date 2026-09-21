package com.hggabriel.pokerun.ui.telas.login

import com.hggabriel.pokerun.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A entrada com o Google sem prazo, e o repetir aos 15 s (`F1-T25`, docs/03 §3.1).
 *
 * Escrito **antes** da implementação (`EXECUCAO.md §3.2`), a partir da decisão do humano
 * de 21/09, e não da leitura do código.
 *
 * ### Por que não há mais prazo
 *
 * `F1-T22` pôs 15 s, e em 21/09 eles cancelaram a folha aos 14,99 s com treze contas na
 * tela; `F1-T24` subiu para 120 s, e então o Google que nunca responde custava dois
 * minutos de espera. **Nenhum número serve às duas coisas, porque o SDK não diz quando a
 * folha apareceu.** O humano tirou o prazo e pôs a saída na mão da pessoa: aos 15 s o
 * botão volta como `Tentar de novo`.
 *
 * Fechar a folha continua sendo cancelamento, e volta ao ocioso na hora — isso não mudou
 * e não é desta tarefa. O botão dos 15 s é para o caso que não dá sinal nenhum: o
 * processo do Google morrendo sem responder (`F1-T23`).
 *
 * ### Por que isto é função pura
 *
 * `kotlinx-coroutines-test` não é dependência do projeto (`EXECUCAO.md §3.3`). O relógio
 * dos 15 s é um `delay` no `ViewModel`, sem ramo; o que tem ramo — o que um toque faz em
 * cada estado, e o que o botão diz — mora em funções puras, e é aqui que elas são
 * provadas.
 */
class RepetirDaEntradaTest {

    // -------------------------------------------------------------------------
    // toqueNoBotao — o que o toque faz em cada estado
    // -------------------------------------------------------------------------

    @Test
    fun `o toque no ocioso abre a folha`() {
        assertEquals(ToqueNoBotao.Abre, toqueNoBotao(LoginUiState.Ocioso))
    }

    @Test
    fun `o toque no erro abre a folha de novo, porque o mesmo botao e o repetir`() {
        // docs/02 §8, item 7. A tela tem um botão só desde `F1-T06`, e o erro não ganha
        // um segundo.
        assertEquals(ToqueNoBotao.Abre, toqueNoBotao(LoginUiState.Erro(R.string.login_erro_generico)))
    }

    @Test
    fun `o toque enquanto espera, antes dos 15 s, e ignorado`() {
        // O duplo toque não pode abrir duas folhas. Antes dos 15 s o pedido ainda está
        // dentro do tempo em que o próprio sistema responde.
        assertEquals(ToqueNoBotao.Ignora, toqueNoBotao(LoginUiState.Entrando))
    }

    @Test
    fun `o toque depois dos 15 s abandona o pedido pendurado e abre outro`() {
        // **É a saída do caso que não dá sinal.** Abrir outro sem abandonar o primeiro
        // deixaria dois pedidos disputando a mesma folha.
        assertEquals(ToqueNoBotao.Reabre, toqueNoBotao(LoginUiState.Demorando))
    }

    @Test
    fun `o toque depois de autenticado e ignorado`() {
        // A tela está saindo. Um toque aqui abriria uma segunda entrada por cima da
        // sessão que acabou de ser criada.
        assertEquals(
            ToqueNoBotao.Ignora,
            toqueNoBotao(LoginUiState.Autenticado(uid = "uid", temPerfil = true)),
        )
    }

    // -------------------------------------------------------------------------
    // rotuloDoBotao — o que o botão diz
    // -------------------------------------------------------------------------

    @Test
    fun `depois dos 15 s o botao diz Tentar de novo`() {
        assertEquals(R.string.login_tentar_de_novo, rotuloDoBotao(LoginUiState.Demorando))
    }

    @Test
    fun `enquanto espera o botao nao tem rotulo, tem o indicador`() {
        assertNull(rotuloDoBotao(LoginUiState.Entrando))
    }

    @Test
    fun `no ocioso e no erro o botao diz Entrar com o Google`() {
        assertEquals(R.string.login_entrar, rotuloDoBotao(LoginUiState.Ocioso))
        assertEquals(R.string.login_entrar, rotuloDoBotao(LoginUiState.Erro(R.string.login_erro_generico)))
    }

    // -------------------------------------------------------------------------
    // O tempo até o repetir
    // -------------------------------------------------------------------------

    @Test
    fun `o repetir aparece depois que o sistema teve a chance de responder`() {
        // Em 20/09 o próprio sistema foi visto cancelando aos 10 s. Um botão antes disso
        // disputaria com a resposta dele; muito depois, volta a ser a tela travada.
        assertTrue("antes dos 10 s do sistema", ESPERA_ATE_O_REPETIR_MS > 10_000)
        assertTrue("tarde demais, a tela parece travada", ESPERA_ATE_O_REPETIR_MS < 30_000)
    }
}
