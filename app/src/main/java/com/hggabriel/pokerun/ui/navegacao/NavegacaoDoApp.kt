package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.ui.componentes.EmConstrucao
import com.hggabriel.pokerun.ui.telas.login.LoginScreen
import com.hggabriel.pokerun.ui.telas.onboarding.OnboardingScreen

/**
 * O grafo de fora: a porta de entrada e a pilha modal (`F1-T07`, docs/03 §1).
 *
 * **São dois `NavHost` aninhados, e a divisão é a da especificação:** aqui ficam os
 * destinos que docs/03 §1 lista como *"pilha modal, fora da barra inferior"*, e a
 * [CascaDeNavegacao] é um destino só deste grafo — o que tem abas. É a estrutura que
 * torna a regra "fora da barra" verdadeira por construção: uma tela declarada aqui
 * **não tem como** desenhar a barra, porque o `Scaffold` dela vive um nível abaixo.
 *
 * ### O destino inicial
 *
 * `uidAtual` é síncrono e sem rede — a sessão do Firebase é persistida no aparelho e
 * sobrevive a reinício —, então dá para escolher a rota inicial sem estado de
 * carregamento e sem a tela de entrada piscar para quem já entrou.
 *
 * **Quem tem sessão abre na casca, não no onboarding.** A conta existir não prova que
 * o perfil existe, mas quem tem sessão e não tem perfil é caso de uma abertura só, na
 * primeira instalação, e essa passa pela `LoginScreen` de qualquer jeito. Mandar todo
 * mundo para uma verificação de rede na abertura custaria um estado de carregamento em
 * toda abertura para cobrir um caso que só acontece se o app for morto entre autenticar
 * e completar o onboarding — e esse caso é de `F1-T08`, que é quem sabe retomar.
 */
@Composable
fun NavegacaoDoApp(
    modifier: Modifier = Modifier,
    navegacao: NavHostController = rememberNavController(),
) {
    val autenticado = LocalAppContainer.current.autenticacaoRepositorio.uidAtual != null

    NavHost(
        navController = navegacao,
        startDestination = if (autenticado) Casca else Login,
        modifier = modifier,
    ) {
        composable<Login> {
            LoginScreen(
                aoEntrarComPerfil = { navegacao.trocarPorta<Login>(Casca) },
                aoEntrarSemPerfil = { navegacao.trocarPorta<Login>(Onboarding) },
            )
        }

        composable<Onboarding> {
            OnboardingScreen(aoConcluir = { navegacao.trocarPorta<Onboarding>(Casca) })
        }

        composable<Casca> {
            CascaDeNavegacao(aoAbrirModal = { rota -> navegacao.navigate(rota) })
        }

        // A pilha modal de docs/03 §1.
        composable<Ajustes> {
            EmConstrucao(tela = "SettingsScreen", tarefa = "F1-T17")
        }
        composable<ListaDePlanos> {
            EmConstrucao(tela = "PlansListScreen", tarefa = "F1-T12")
        }
        composable<CriarPlano> {
            EmConstrucao(tela = "CreatePlanScreen", tarefa = "F1-T10")
        }
        composable<RevisarRascunho> {
            EmConstrucao(tela = "PlanDraftReviewScreen", tarefa = "F1-T11")
        }
        composable<EntrarComCodigo> {
            EmConstrucao(tela = "JoinPlanScreen", tarefa = "F1-T14")
        }
        composable<EditarCorrida> {
            EmConstrucao(tela = "RunEditScreen", tarefa = "F2-T10")
        }
    }
}

/**
 * Troca a porta de entrada por outra, **sem deixar volta**.
 *
 * Login e onboarding não são etapas de um caminho: são portas. O voltar do sistema
 * depois de entrar não pode devolver o usuário à tela de login com sessão criada, que
 * é o estado sem saída clássico deste fluxo — a tela mostraria um botão de entrar para
 * quem já entrou.
 *
 * **A porta que sai é o parâmetro de tipo, e isso não é preciosismo.** A primeira
 * versão desta função usava `popUpTo(graph.id) { inclusive = true }`, contando com
 * "limpe o grafo inteiro" — e no emulador o voltar devolveu à `LoginScreen` já
 * autenticado, com a mensagem de erro anterior ainda na tela. Popar o grafo em que
 * se está não remove as entradas dele. Mirar a porta pelo tipo remove.
 *
 * Cada porta fecha a si mesma: a `LoginScreen` sai com `trocarPorta<Login>`, e
 * `F1-T08` sai com `trocarPorta<Onboarding>`. Sobra um destino na pilha, e o voltar
 * sai do app — o mesmo comportamento da raiz de `Hoje` (docs/03 §1).
 */
private inline fun <reified Porta : Any> NavHostController.trocarPorta(destino: Any) {
    navigate(destino) {
        popUpTo<Porta> { inclusive = true }
        launchSingleTop = true
    }
}
