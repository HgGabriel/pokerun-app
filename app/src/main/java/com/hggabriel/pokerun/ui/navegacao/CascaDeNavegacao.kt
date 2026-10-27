package com.hggabriel.pokerun.ui.navegacao

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.ui.telas.ajustes.AjustesScreen
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualScreen
import com.hggabriel.pokerun.ui.telas.detalhecorrida.DetalheCorridaScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.DetalhePlanoScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.GrupoDoPlanoScreen
import com.hggabriel.pokerun.ui.telas.detalhesemana.DetalheSemanaScreen
import com.hggabriel.pokerun.ui.telas.grupo.GrupoScreen
import com.hggabriel.pokerun.ui.telas.painel.StatsDashboardScreen
import com.hggabriel.pokerun.ui.telas.pokedex.PokedexDetailScreen
import com.hggabriel.pokerun.ui.telas.pokedex.PokedexMainScreen
import com.hggabriel.pokerun.ui.telas.home.HomeScreen
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

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
 * - **O segundo toque na aba corrente volta à raiz dela** ([voltarARaizDaAba]), sem
 *   empilhar cópia.
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
    aoSairDaConta: () -> Unit,
    modifier: Modifier = Modifier,
    navegacao: NavHostController = rememberNavController(),
) {
    val entradaAtual by navegacao.currentBackStackEntryAsState()

    // `F2-T08`: o backfill do XP roda uma vez por entrada na casca, que é onde se chega
    // com sessão. Ele mesmo confere se já rodou, e trabalha no escopo do app.
    val container = LocalAppContainer.current
    LaunchedEffect(Unit) { container.backfillDeXp.seNecessario() }

    // `F4-T09`: o roteador da abertura (docs/01 §4.6). Semana fechada que ainda não foi
    // apresentada abre a tela cheia, uma vez, e a marca só é gravada ao exibi-la. Roda
    // depois do backfill na mesma entrada, e uma falha de leitura abre a Home.
    LaunchedEffect(Unit) {
        if (container.roteadorDaAbertura.precisaDoFechamento()) aoAbrirModal(FechamentoDaSemana)
    }

    // `F4-T11`: a Pokédex só entra na barra depois do primeiro fechamento semanal, que é
    // quando `users/{uid}.semana_apresentada` deixa de ser nulo (docs/03 §3.20.1).
    val usuarioDaCasca by remember(container) {
        container.autenticacaoRepositorio.uidAtual
            ?.let { container.usuarioRepositorio.observar(it) }
            ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)
    val pokedexLiberada = usuarioDaCasca?.semanaApresentada != null

    // O badge do destino novo some no primeiro acesso, e o primeiro acesso é lembrado.
    val contexto = LocalContext.current
    val preferencias = remember(contexto) { contexto.getSharedPreferences("pokedex_aba", Context.MODE_PRIVATE) }
    var pokedexVista by remember { mutableStateOf(preferencias.getBoolean("vista", false)) }

    // A aba fica acesa mesmo com uma tela filha em cima: estando em `DetalheDaSemana`,
    // quem está aceso é `Hoje`. Comparar só o destino atual apagaria a barra ao descer
    // um nível, e é a hierarquia do grafo que responde isso.
    val destinoAtual = DestinoDeTopo.entries.firstOrNull { destino ->
        entradaAtual?.destination?.hierarchy?.any { it.hasRoute(destino.grafo::class) } == true
    } ?: DestinoDeTopo.HOJE

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
     * **Existe como função e não como três blocos copiados** porque `F1-T17` trocou o
     * andaime pela `SettingsScreen` de verdade, e três blocos em sincronia manual é o que
     * produz uma aba com a tela velha. `AjustesPorAbaTest` exige a chamada em cada grafo
     * de aba **e** o `composable<Ajustes>` aqui dentro.
     *
     * **É local, e não de arquivo, porque `F1-T17` trouxe o sair da conta.** Sair troca a
     * porta de entrada no `NavHost` de fora (`NavegacaoDoApp`), então o destino precisa do
     * callback — e recebê-lo por parâmetro mudaria as três chamadas para
     * `telaDeAjustes(aoSairDaConta)`. Fechada sobre o parâmetro da casca, a chamada segue
     * sendo `telaDeAjustes()` nas três abas, que é a forma que a varredura conhece.
     */
    LaunchedEffect(destinoAtual) {
        if (destinoAtual == DestinoDeTopo.POKEDEX && !pokedexVista) {
            pokedexVista = true
            preferencias.edit().putBoolean("vista", true).apply()
        }
    }

    // O `Abrir a Pokédex` da estreia (docs/03 §3.20.1) pede a aba pelo roteador, e a casca,
    // que é quem troca de aba, atende assim que ela existir.
    val pedidoDaPokedex by container.roteadorDaAbertura.pedidoDeAbrirAPokedex.collectAsStateWithLifecycle()
    LaunchedEffect(pedidoDaPokedex, pokedexLiberada) {
        if (pedidoDaPokedex && pokedexLiberada) {
            navegacao.irParaAba(DestinoDeTopo.POKEDEX)
            container.roteadorDaAbertura.consumirPedidoDaPokedex()
        }
    }

    fun NavGraphBuilder.telaDeAjustes() {
        composable<Ajustes> {
            AjustesScreen(aoSairDaConta = aoSairDaConta)
        }
    }

    /**
     * A `RunDetailScreen` (`F2-T09`), registrada nas abas que a abrem, como Ajustes: em
     * `Hoje` pelo segmento da barra, e em `Progresso` pelo histórico (docs/03 §1). O grafo
     * dono decide a aba acesa e o voltar.
     */
    fun NavGraphBuilder.telaDeCorrida() {
        composable<DetalheDaCorrida> { entrada ->
            DetalheCorridaScreen(
                corridaId = entrada.toRoute<DetalheDaCorrida>().corridaId,
                aoCorrigir = { aoAbrirModal(EditarCorrida(it)) },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BarraDeDestinos(
                destinoAtual = destinoAtual,
                // O toque na aba acesa volta à raiz dela; `launchSingleTop` sozinho não
                // fazia nada, e quem estava no detalhe do plano ficava lá.
                aoEscolher = { destino ->
                    if (destino == destinoAtual) navegacao.voltarARaizDaAba(destino) else navegacao.irParaAba(destino)
                },
                destinos = DestinoDeTopo.entries.filter { it != DestinoDeTopo.POKEDEX || pokedexLiberada },
                comBadge = if (pokedexLiberada && !pokedexVista) setOf(DestinoDeTopo.POKEDEX) else emptySet(),
            )
        },
    ) { espacamento ->
        // A barra de status continua o topo da tela: a camisa vermelha da Home, com ícones
        // brancos, e a página tingida do detalhe do plano.
        val destino = entradaAtual?.destination
        val naHome = destino?.hasRoute(Hoje::class) == true
        val naArena = destino?.hasRoute(CorridaManual::class) == true
        val corDoTopo = when {
            naHome -> MaterialTheme.colorScheme.primary
            naArena -> MaterialTheme.colorScheme.onSurface
            destino?.hasRoute(DetalheDoPlano::class) == true || destino?.hasRoute(GrupoDoPlano::class) == true ||
                destino?.hasRoute(Grupo::class) == true || destino?.hasRoute(DetalheDaSemana::class) == true ->
                LocalCoresPokerun.current.superficieAfundada
            else -> null
        }
        BarraDeStatus(corDoTopo, iconesClaros = naHome || naArena)

        // A barra fica fora do `NavHost` e não anima: só a área acima dela troca.
        val transicoes = rememberTransicoesDeNavegacao()
        NavHost(
            navController = navegacao,
            startDestination = AbaHoje,
            // Consumido: tela que também vive na pilha modal leva `safeDrawingPadding`, e
            // aqui dentro ela somaria o mesmo recuo duas vezes.
            modifier = Modifier.padding(espacamento).consumeWindowInsets(espacamento),
            enterTransition = transicoes.entrada,
            exitTransition = transicoes.saida,
            popEnterTransition = transicoes.entradaNoVoltar,
            popExitTransition = transicoes.saidaNoVoltar,
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
                        aoAbrirCorrida = { navegacao.navigate(DetalheDaCorrida(it)) },
                        // A placa de peito abre o plano (docs/03 §3.3): a aresta
                        // `HomeScreen → PlanDetailScreen` de docs/03 §1, dentro da aba.
                        aoAbrirPlano = { navegacao.navigate(DetalheDoPlano(it)) },
                        // O pulso do grupo leva à aba Grupo, onde mora o ranking.
                        aoAbrirGrupo = { navegacao.irParaAba(DestinoDeTopo.GRUPO) },
                        // Na Fase 4 o card da espécie é a porta da aba Pokédex (docs/03 §3.3.2).
                        aoAbrirPokedex = if (pokedexLiberada) {
                            { navegacao.irParaAba(DestinoDeTopo.POKEDEX) }
                        } else {
                            null
                        },
                    )
                }
                composable<DetalheDoPlano> { entrada ->
                    val planoId = entrada.toRoute<DetalheDoPlano>().planoId
                    DetalhePlanoScreen(
                        planoId = planoId,
                        // O carimbo do card da semana abre a corrida, como na Home.
                        aoAbrirCorrida = { navegacao.navigate(DetalheDaCorrida(it)) },
                        aoAbrirGrupo = { navegacao.navigate(GrupoDoPlano(planoId)) },
                    )
                }
                composable<GrupoDoPlano> { entrada ->
                    GrupoDoPlanoScreen(planoId = entrada.toRoute<GrupoDoPlano>().planoId)
                }
                composable<DetalheDaSemana> { entrada ->
                    val rota = entrada.toRoute<DetalheDaSemana>()
                    DetalheSemanaScreen(
                        planoId = rota.planoId,
                        numero = rota.numero,
                        aoAbrirCorrida = { navegacao.navigate(DetalheDaCorrida(it)) },
                    )
                }
                // docs/03 §1: o FAB da Home. Fica dentro da aba, e não na pilha
                // modal, porque registrar um treino é o que se faz em `Hoje`.
                composable<CorridaManual> {
                    CorridaManualScreen(aoSair = { navegacao.popBackStack() })
                }
                telaDeCorrida()
                telaDeAjustes()
            }

            navigation<AbaProgresso>(startDestination = Progresso) {
                // `F3-T09`: o painel e o histórico lado a lado, numa troca por toque
                // (decisão nº 97). O histórico, raiz de Progresso na Fase 2, é a aba interna.
                composable<Progresso> {
                    StatsDashboardScreen(
                        aoAbrirAjustes = { navegacao.navigate(Ajustes) },
                        aoAbrirCorrida = { navegacao.navigate(DetalheDaCorrida(it)) },
                        aoRegistrarCorrida = { navegacao.navigate(CorridaManual) },
                    )
                }
                telaDeCorrida()
                telaDeAjustes()
            }

            navigation<AbaPokedex>(startDestination = Pokedex) {
                // `F4-T07` e `F4-T08`: a grade e o detalhe da espécie, dentro da aba.
                composable<Pokedex> {
                    PokedexMainScreen(
                        aoAbrirAjustes = { navegacao.navigate(Ajustes) },
                        aoAbrirEspecie = { navegacao.navigate(DetalheDaEspecie(it)) },
                    )
                }
                composable<DetalheDaEspecie> { entrada ->
                    PokedexDetailScreen(numero = entrada.toRoute<DetalheDaEspecie>().numero)
                }
                telaDeAjustes()
            }

            navigation<AbaGrupo>(startDestination = Grupo) {
                // `F2-T12`: o ranking do grupo troca o `EmConstrucao` da raiz.
                composable<Grupo> {
                    GrupoScreen(aoAbrirAjustes = { navegacao.navigate(Ajustes) })
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
 * O segundo toque na aba acesa: tira da pilha tudo acima da raiz dela, como a barra
 * inferior do Android faz. Já na raiz, não muda nada.
 */
private fun NavHostController.voltarARaizDaAba(destino: DestinoDeTopo) {
    val grafo = currentDestination?.hierarchy?.firstOrNull { it.hasRoute(destino.grafo::class) } as? NavGraph ?: return
    popBackStack(grafo.startDestinationId, inclusive = false)
}

/**
 * A cor do topo da tela atrás da barra de status. O `NavHost` recebe o recuo do topo, então
 * uma tela de topo colorido começaria abaixo de uma faixa branca. Ao sair, os ícones voltam
 * a escuros, que é o padrão do app (D-13).
 */
@Composable
private fun BarraDeStatus(cor: Color?, iconesClaros: Boolean) {
    val view = LocalView.current
    DisposableEffect(iconesClaros) {
        val janela = (view.context as? Activity)?.window
        val controle = janela?.let { WindowCompat.getInsetsController(it, view) }
        controle?.isAppearanceLightStatusBars = !iconesClaros
        onDispose { controle?.isAppearanceLightStatusBars = true }
    }
    if (cor != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(cor),
        )
    }
}
