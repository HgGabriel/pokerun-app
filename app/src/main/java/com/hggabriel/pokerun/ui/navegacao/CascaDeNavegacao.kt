package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hggabriel.pokerun.ui.componentes.EmConstrucao
import com.hggabriel.pokerun.ui.telas.detalheplano.DetalhePlanoScreen
import com.hggabriel.pokerun.ui.telas.detalhesemana.DetalheSemanaScreen
import com.hggabriel.pokerun.ui.telas.home.HomeScreen

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
 * @param aoAbrirModal leva para a pilha modal, que vive **fora** desta casca — lista de
 *   planos, criação, entrada por código e edição de corrida (docs/03 §1). A casca não
 *   conhece aquele grafo: ela avisa quem a hospeda. **`Ajustes` saiu desta lista em
 *   `F1-T07c`**: ele é destino de cada aba, e sair pela modal o tiraria de baixo da
 *   barra — ver [telaDeAjustes].
 * @param aoRetomarCadastro **troca a casca pelo onboarding**, e não empilha em cima
 *   dela. Quem for morto entre autenticar e o passo 2 do cadastro chega aqui sem
 *   `users/{uid}`, porque a abertura com sessão vai direto para a casca; navegar por
 *   [aoAbrirModal] deixaria a casca sem perfil embaixo, e o voltar cairia nela de novo.
 */
@Composable
fun CascaDeNavegacao(
    aoAbrirModal: (Any) -> Unit,
    aoRetomarCadastro: () -> Unit,
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
                    HomeScreen(
                        aoAbrirAjustes = { navegacao.navigate(Ajustes) },
                        aoAbrirPlanos = { aoAbrirModal(ListaDePlanos) },
                        aoCriarPlano = { aoAbrirModal(CriarPlano) },
                        aoEntrarComCodigo = { aoAbrirModal(EntrarComCodigo) },
                        aoRegistrarCorrida = { navegacao.navigate(CorridaManual) },
                        aoRetomarCadastro = aoRetomarCadastro,
                        // docs/03 §1: a aresta `HomeScreen → WeekDetailScreen`. Fica
                        // dentro da aba, e não na pilha modal, porque descer da Home
                        // para a semana não é sair de `Hoje`.
                        aoAbrirSemana = { planoId, numero ->
                            navegacao.navigate(DetalheDaSemana(planoId, numero))
                        },
                    )
                }
                composable<DetalheDoPlano> { entrada ->
                    DetalhePlanoScreen(planoId = entrada.toRoute<DetalheDoPlano>().planoId)
                }
                composable<DetalheDaSemana> { entrada ->
                    val rota = entrada.toRoute<DetalheDaSemana>()
                    DetalheSemanaScreen(planoId = rota.planoId, numero = rota.numero)
                }
                composable<CorridaManual> {
                    EmConstrucao(tela = "ManualRunScreen", tarefa = "F1-T16")
                }
                telaDeAjustes()
            }

            navigation<AbaProgresso>(startDestination = Progresso) {
                composable<Progresso> {
                    EmConstrucao(
                        tela = "StatsDashboardScreen",
                        tarefa = "F3-T09",
                        aba = stringResource(DestinoDeTopo.PROGRESSO.rotulo),
                        aoAbrirAjustes = { navegacao.navigate(Ajustes) },
                    )
                }
                telaDeAjustes()
            }

            navigation<AbaGrupo>(startDestination = Grupo) {
                composable<Grupo> {
                    EmConstrucao(
                        tela = "SocialLeaderboardScreen",
                        tarefa = "F2-T12",
                        aba = stringResource(DestinoDeTopo.GRUPO.rotulo),
                        aoAbrirAjustes = { navegacao.navigate(Ajustes) },
                    )
                }
                telaDeAjustes()
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

/**
 * O destino de Ajustes, registrado **uma vez em cada grafo de aba** (`F1-T07c`,
 * docs/03 §1 e docs/02 §10.1).
 *
 * **A tela é uma só e a rota é uma só; o que se repete é o registro**, e é ele que
 * precisa se repetir. A barra deriva a aba acesa da hierarquia do grafo, então é o
 * grafo dono do destino que decide quem acende: aberto de `Grupo`, `Ajustes` mora em
 * `AbaGrupo` e `Grupo` continua aceso. Um destino global acenderia `Hoje` sempre, que
 * é o defeito que pôs `Ajustes` na pilha modal em `F1-T07`.
 *
 * O voltar sai de graça pela mesma estrutura: `Ajustes` empilha sobre a raiz da aba em
 * que foi aberto, e o voltar devolve àquela raiz — não à de `Hoje`.
 *
 * **Existe como função e não como três blocos copiados** porque `F1-T17` troca o
 * `EmConstrucao` pela `SettingsScreen` de verdade, e três blocos em sincronia manual é
 * o que produz uma aba com a tela velha. `AjustesPorAbaTest` exige a chamada em cada
 * grafo de aba **e** o `composable<Ajustes>` aqui dentro.
 */
private fun NavGraphBuilder.telaDeAjustes() {
    composable<Ajustes> {
        EmConstrucao(tela = "SettingsScreen", tarefa = "F1-T17")
    }
}
