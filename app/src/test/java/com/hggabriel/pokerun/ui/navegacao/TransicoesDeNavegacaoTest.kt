package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * As transições de rota (`docs/design-system/transicoes.md`).
 *
 * Sem Robolectric, a animação em si é verificação de aparelho. O que dá para travar aqui é o
 * que decide a animação: toda rota tem lugar, cada relação cai na regra da tabela, o
 * movimento reduzido corta seco, e nenhum `composable<...>` declara transição avulsa.
 */
class TransicoesDeNavegacaoTest {

    private val hoje = Camada(TipoDeCamada.ABA, "HOJE")
    private val progresso = Camada(TipoDeCamada.ABA, "PROGRESSO")
    private val casca = (LUGAR_DA_ROTA.getValue(Casca::class) as Lugar.Fixo).camada
    private val login = (LUGAR_DA_ROTA.getValue(Login::class) as Lugar.Fixo).camada
    private fun fixa(rota: kotlin.reflect.KClass<out Any>) = (LUGAR_DA_ROTA.getValue(rota) as Lugar.Fixo).camada

    private fun fonte(nome: String): String {
        val caminho = "src/main/java/com/hggabriel/pokerun/ui/navegacao/$nome"
        return listOf(File(caminho), File("app/$caminho")).first { it.exists() }.readText()
    }

    @Test
    fun `toda rota de Rotas kt tem lugar`() {
        val declaradas = Regex("""@Serializable\s+data\s+(?:object|class)\s+(\w+)""")
            .findAll(fonte("Rotas.kt")).map { it.groupValues[1] }.toSet()
        assertTrue("A varredura de Rotas.kt não achou rota nenhuma", declaradas.size >= 20)

        val comLugar = LUGAR_DA_ROTA.keys.map { it.simpleName }.toSet()
        assertEquals(
            "Rota sem lugar em LUGAR_DA_ROTA cai na regra neutra, sem a direção de transicoes.md",
            declaradas,
            comLugar,
        )
    }

    @Test
    fun `cada relacao cai na regra da tabela`() {
        val planos = fixa(ListaDePlanos::class)
        val criacao = fixa(CriarPlano::class)
        val fechamento = fixa(FechamentoDaSemana::class)

        assertEquals(Regra.APROFUNDAR, regraEntre(hoje, hoje, voltando = false))
        assertEquals(Regra.VOLTAR_UM_NIVEL, regraEntre(hoje, hoje, voltando = true))
        assertEquals(Regra.TROCAR_DE_ABA, regraEntre(hoje, progresso, voltando = false))
        // O voltar na raiz de uma aba cai em `Hoje` esmaecendo, não deslizando.
        assertEquals(Regra.TROCAR_DE_ABA, regraEntre(progresso, hoje, voltando = true))
        assertEquals(Regra.ABRIR_TAREFA, regraEntre(casca, planos, voltando = false))
        assertEquals(Regra.FECHAR_TAREFA, regraEntre(planos, casca, voltando = true))
        // Tarefa sobre tarefa: `ListaDePlanos → CriarPlano`.
        assertEquals(Regra.ABRIR_TAREFA, regraEntre(planos, criacao, voltando = false))
        // `CriarPlano → RevisarRascunho` aprofunda; o voltar da revisão fecha o fluxo inteiro.
        assertEquals(criacao, fixa(RevisarRascunho::class))
        assertEquals(Regra.FECHAR_TAREFA, regraEntre(criacao, planos, voltando = true))
        assertEquals(Regra.ABRIR_MOMENTO, regraEntre(casca, fechamento, voltando = false))
        assertEquals(Regra.FECHAR_MOMENTO, regraEntre(fechamento, casca, voltando = true))
        assertEquals(Regra.PORTA, regraEntre(login, casca, voltando = false))
        assertEquals(Regra.PORTA, regraEntre(casca, login, voltando = false))
    }

    @Test
    fun `o registro de corrida abre como tarefa e o plano de fora mora nos planos`() {
        assertEquals(TipoDeCamada.TAREFA, fixa(CorridaManual::class).tipo)
        assertEquals(
            Regra.ABRIR_TAREFA,
            regraEntre(hoje, fixa(CorridaManual::class), voltando = false),
        )
        val lugarDoPlano = LUGAR_DA_ROTA.getValue(DetalheDoPlano::class) as Lugar.NaAba
        assertEquals(fixa(ListaDePlanos::class), lugarDoPlano.foraDaAba)
    }

    @Test
    fun `movimento reduzido corta seco em toda regra`() {
        val reduzidas = TransicoesDeNavegacao(deslocamentoPx = 90, reduzido = true)
        Regra.entries.forEach { regra ->
            assertEquals("entrada de $regra", EnterTransition.None, reduzidas.entrar(regra))
            assertEquals("saída de $regra", ExitTransition.None, reduzidas.sair(regra))
        }
    }

    @Test
    fun `sem movimento reduzido so o fundo de quem fecha deixa de animar`() {
        val normais = TransicoesDeNavegacao(deslocamentoPx = 90, reduzido = false)
        val semEntrada = setOf(Regra.FECHAR_TAREFA, Regra.FECHAR_MOMENTO)
        Regra.entries.forEach { regra ->
            assertNotEquals("saída de $regra", ExitTransition.None, normais.sair(regra))
            if (regra in semEntrada) {
                assertEquals("entrada de $regra", EnterTransition.None, normais.entrar(regra))
            } else {
                assertNotEquals("entrada de $regra", EnterTransition.None, normais.entrar(regra))
            }
        }
    }

    @Test
    fun `nenhuma rota declara transicao avulsa`() {
        listOf("NavegacaoDoApp.kt", "CascaDeNavegacao.kt").forEach { arquivo ->
            val texto = fonte(arquivo)
            listOf("enterTransition", "exitTransition", "popEnterTransition", "popExitTransition").forEach { nome ->
                val usos = Regex("""\b$nome\s*=\s*(\S+)""").findAll(texto).map { it.groupValues[1] }.toList()
                assertEquals(
                    "$arquivo deve passar $nome uma vez só, ao NavHost, vindo de TransicoesDeNavegacao: $usos",
                    1,
                    usos.size,
                )
                assertTrue("$arquivo: $nome = ${usos.single()}", usos.single().startsWith("transicoes."))
            }
        }
    }
}
