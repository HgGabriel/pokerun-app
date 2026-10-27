package com.hggabriel.pokerun.ui.navegacao

import com.hggabriel.pokerun.ui.telas.editarcorrida.EditarCorridaScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.ui.abertura.AvisarProntidao
import com.hggabriel.pokerun.ui.telas.criarplano.CriarPlanoScreen
import com.hggabriel.pokerun.ui.telas.detalhecorrida.DetalheCorridaScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.DetalhePlanoScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.GrupoDoPlanoScreen
import com.hggabriel.pokerun.ui.telas.entrarcomcodigo.EntrarComCodigoScreen
import com.hggabriel.pokerun.ui.telas.fechamento.WeeklyCloseScreen
import com.hggabriel.pokerun.ui.telas.listadeplanos.ListaDePlanosScreen
import com.hggabriel.pokerun.ui.telas.login.LoginScreen
import com.hggabriel.pokerun.ui.telas.onboarding.OnboardingScreen
import com.hggabriel.pokerun.ui.telas.revisarrascunho.RevisarRascunhoScreen

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
    // O login não lê nada para aparecer. Com sessão, quem avisa é a Home.
    AvisarProntidao(!autenticado)
    // As mesmas da casca: a regra sai da relação entre as telas (`TransicoesDeNavegacao`).
    val transicoes = rememberTransicoesDeNavegacao()

    NavHost(
        navController = navegacao,
        startDestination = if (autenticado) Casca else Login,
        modifier = modifier,
        enterTransition = transicoes.entrada,
        exitTransition = transicoes.saida,
        popEnterTransition = transicoes.entradaNoVoltar,
        popExitTransition = transicoes.saidaNoVoltar,
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
            CascaDeNavegacao(
                aoAbrirModal = { rota -> navegacao.navigate(rota) },
                // A casca sai da pilha junto: quem chega aqui sem `users/{uid}` não pode
                // voltar para uma Home que não tem perfil para mostrar.
                aoRetomarCadastro = { navegacao.trocarPorta<Casca>(Onboarding) },
                // `Sair` da `SettingsScreen` (`F1-T17`). É porta, e não etapa: a casca
                // sai da pilha, senão o voltar devolveria a uma Home cuja sessão acabou
                // de ser encerrada — o mesmo estado sem saída que `trocarPorta` existe
                // para impedir do outro lado.
                aoSairDaConta = { navegacao.trocarPorta<Casca>(Login) },
            )
        }

        // A pilha modal de docs/03 §1.
        //
        // O `Perfil` **não está aqui**, desde `F1-T07c`: docs/02 §10.1 põe o atalho dele na
        // raiz de toda aba, então ele é destino de cada grafo de aba, dentro da casca.
        // Declarado aqui, abriria por cima da barra inferior e apagaria a aba —
        // `PerfilPorAbaTest` falha se voltar.
        composable<ListaDePlanos> {
            ListaDePlanosScreen(
                aoAbrirPlano = { planoId -> navegacao.navigate(DetalheDoPlano(planoId)) },
                aoCriarPlano = { navegacao.navigate(CriarPlano) },
                aoEntrarComCodigo = { navegacao.navigate(EntrarComCodigo) },
            )
        }

        /*
         * `DetalheDoPlano` aparece **duas vezes no app**, e docs/03 §1 desenha as duas:
         * uma sob `Hoje`, na casca, e outra aqui, saindo da lista de planos. Não é
         * duplicação por descuido — é a mesma tela em duas pilhas diferentes, e a
         * alternativa seria a lista abrir um detalhe com a barra inferior de `Hoje`
         * acesa embaixo de uma tela modal.
         *
         * **As duas apontam para o mesmo composable**, então a tela só existe uma vez.
         * Quem acrescentar argumento de rota mexe nos dois pontos.
         */
        composable<DetalheDoPlano> { entrada ->
            val planoId = entrada.toRoute<DetalheDoPlano>().planoId
            DetalhePlanoScreen(
                planoId = planoId,
                aoAbrirCorrida = { navegacao.navigate(DetalheDaCorrida(it)) },
                aoAbrirGrupo = { navegacao.navigate(GrupoDoPlano(planoId)) },
            )
        }
        composable<GrupoDoPlano> { entrada ->
            GrupoDoPlanoScreen(planoId = entrada.toRoute<GrupoDoPlano>().planoId)
        }
        // A corrida aberta pelo carimbo do detalhe que saiu da lista de planos. A mesma
        // tela que a casca registra em cada aba; aqui ela fica na pilha modal, por cima do
        // detalhe de onde saiu.
        composable<DetalheDaCorrida> { entrada ->
            DetalheCorridaScreen(
                corridaId = entrada.toRoute<DetalheDaCorrida>().corridaId,
                aoCorrigir = { navegacao.navigate(EditarCorrida(it)) },
            )
        }
        composable<CriarPlano> {
            CriarPlanoScreen(
                // A criação sai da pilha ao gerar: o voltar da revisão devolve ao ponto
                // de partida, e não a um formulário que já cumpriu o seu papel.
                aoGerar = { rascunho ->
                    navegacao.navigate(rascunho) {
                        popUpTo<CriarPlano> { inclusive = true }
                    }
                },
            )
        }
        composable<RevisarRascunho> { entrada ->
            RevisarRascunhoScreen(
                rascunho = entrada.toRoute(),
                // Criado o plano, a revisão sai da pilha e o usuário cai na Home, que
                // passa a mostrar o plano novo sozinha — o listener de `users/{uid}` já
                // estava assinado. Não há para onde mais ir: a `PlanDetailScreen` é
                // `F1-T13`.
                aoCriar = { navegacao.popBackStack() },
            )
        }
        composable<EntrarComCodigo> {
            EntrarComCodigoScreen(
                // Entrou, a tela sai da pilha e a pessoa cai onde estava. Não há para
                // onde mais ir: `users/{uid}.planos` já ganhou o ID, então a lista de
                // planos e a Home mostram o plano novo sozinhas, pelos listeners que já
                // estavam assinados.
                aoEntrar = { navegacao.popBackStack() },
            )
        }
        // `F4-T09`: a `WeeklyCloseScreen`, tela cheia e fora da barra (docs/03 §3.20). O
        // voltar do sistema é dela: salta para o final da sequência, e o segundo fecha.
        composable<FechamentoDaSemana> {
            WeeklyCloseScreen(aoFechar = { navegacao.popBackStack() })
        }
        // `F2-T10`: a `RunEditScreen`, aberta pelo `Corrigir` da `RunDetailScreen`.
        composable<EditarCorrida> { entrada ->
            EditarCorridaScreen(
                corridaId = entrada.toRoute<EditarCorrida>().corridaId,
                aoSair = { navegacao.popBackStack() },
            )
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
