package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.hggabriel.pokerun.ui.componentes.EmConstrucao

/**
 * A casca com a barra inferior (`F1-T07`, docs/03 §1 e §2).
 *
 * **`Scaffold` com `NavigationBar`, nunca `NavigationSuiteScaffold`.** O adaptativo
 * troca a barra por rail lateral em largura expandida, e isso cobraria especificação e
 * teste de uma segunda navegação — incluindo onde ficam cabeçalho e engrenagem — para
 * atender ninguém: a premissa é celular no bolso, num grupo de oito.
 *
 * **Sem `HorizontalPager`.** Troca de aba é por toque, e só. O arraste da Escada
 * (Fase 4) competiria com o deslize logo na primeira tentativa de uso do elemento de
 * assinatura do app.
 *
 * ### Uma pilha por aba, e o voltar previsível
 *
 * As duas exigências de docs/03 §1 saem da mesma estrutura, e é por isso que aqui não
 * há `BackHandler` nenhum — comportamento de voltar escrito à mão é o que produz pilha
 * reiniciada em silêncio:
 *
 * - **Cada aba é um grafo aninhado.** É o grafo que dá identidade à pilha, e é ele que
 *   `saveState`/`restoreState` guardam e devolvem inteiros, com rolagem, filtros e aba
 *   interna onde estavam.
 * - **`popUpTo` mira a raiz e não é `inclusive`**, então `AbaHoje` continua embaixo de
 *   qualquer outra aba. O voltar do sistema na raiz de `Progresso` cai em `Hoje`
 *   sozinho, e na raiz de `Hoje` sai do app.
 * - **`launchSingleTop`** faz o segundo toque na aba corrente não empilhar uma cópia.
 *
 * @param aoAbrirModal leva para a pilha modal, que vive **fora** desta casca — ajustes,
 *   lista de planos, criação, entrada por código e edição de corrida (docs/03 §1). A
 *   casca não conhece aquele grafo: ela avisa quem a hospeda.
 */
@Composable
fun CascaDeNavegacao(
    aoAbrirModal: (Any) -> Unit,
    modifier: Modifier = Modifier,
    navegacao: NavHostController = rememberNavController(),
) {
    val entradaAtual by navegacao.currentBackStackEntryAsState()

    // A aba fica acesa mesmo com uma tela filha em cima: estando em `DetalheDaSemana`,
    // quem está aceso é `Hoje`. Comparar só o destino atual apagaria a barra ao descer
    // um nível, e é a hierarquia do grafo que responde isso.
    val destinoAtual = DestinoDeTopo.entries.firstOrNull { destino ->
        entradaAtual?.destination?.hierarchy?.any { it.hasRoute(destino.grafo::class) } == true
    } ?: DestinoDeTopo.HOJE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BarraDeDestinos(
                destinoAtual = destinoAtual,
                aoEscolher = navegacao::irParaAba,
            )
        },
    ) { espacamento ->
        NavHost(
            navController = navegacao,
            startDestination = AbaHoje,
            modifier = Modifier.padding(espacamento),
        ) {
            navigation<AbaHoje>(startDestination = Hoje) {
                composable<Hoje> {
                    EmConstrucao(tela = "HomeScreen", tarefa = "F1-T09")
                }
                composable<DetalheDoPlano> {
                    EmConstrucao(tela = "PlanDetailScreen", tarefa = "F1-T13")
                }
                composable<DetalheDaSemana> {
                    EmConstrucao(tela = "WeekDetailScreen", tarefa = "F1-T15")
                }
                composable<CorridaManual> {
                    EmConstrucao(tela = "ManualRunScreen", tarefa = "F1-T16")
                }
            }

            navigation<AbaProgresso>(startDestination = Progresso) {
                composable<Progresso> {
                    EmConstrucao(tela = "StatsDashboardScreen", tarefa = "F3-T09")
                }
            }

            navigation<AbaGrupo>(startDestination = Grupo) {
                composable<Grupo> {
                    EmConstrucao(tela = "SocialLeaderboardScreen", tarefa = "F2-T12")
                }
            }
        }
    }
}

/**
 * A troca de aba, com as três cláusulas que fazem a pilha se comportar.
 *
 * Está fora do `Composable` porque é regra de navegação, não desenho — e porque é ela
 * que a revisão precisa ler inteira num lugar só.
 */
private fun NavHostController.irParaAba(destino: DestinoDeTopo) {
    navigate(destino.grafo) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
