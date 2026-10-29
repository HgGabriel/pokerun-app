@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.hggabriel.pokerun.ui.navegacao

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
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
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.AcoesDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.AlturaDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.LocalAcoesDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.LocalFolgaDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.LocalTreinadorDoUsuario
import com.hggabriel.pokerun.ui.componentes.LocalVoltarDoCabecalho
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualScreen
import com.hggabriel.pokerun.ui.telas.detalhecorrida.DetalheCorridaScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.DetalhePlanoScreen
import com.hggabriel.pokerun.ui.telas.detalheplano.GrupoDoPlanoScreen
import com.hggabriel.pokerun.ui.telas.detalhesemana.DetalheSemanaScreen
import com.hggabriel.pokerun.ui.telas.grupo.GrupoScreen
import com.hggabriel.pokerun.ui.telas.historico.HistoricoScreen
import com.hggabriel.pokerun.ui.telas.home.HomeScreen
import com.hggabriel.pokerun.ui.telas.home.HomeViewModel
import com.hggabriel.pokerun.ui.telas.membro.MembroScreen
import com.hggabriel.pokerun.ui.telas.painel.FitasScreen
import com.hggabriel.pokerun.ui.telas.painel.PortaDoProgresso
import com.hggabriel.pokerun.ui.telas.painel.RitmoScreen
import com.hggabriel.pokerun.ui.telas.painel.SelosScreen
import com.hggabriel.pokerun.ui.telas.painel.StatsDashboardScreen
import com.hggabriel.pokerun.ui.telas.perfil.DialogoDeAlteracoesPendentes
import com.hggabriel.pokerun.ui.telas.perfil.EscolherParceiroScreen
import com.hggabriel.pokerun.ui.telas.perfil.EscolherTreinadorScreen
import com.hggabriel.pokerun.ui.telas.perfil.PerfilScreen
import com.hggabriel.pokerun.ui.telas.pokedex.PokedexDetailScreen
import com.hggabriel.pokerun.ui.telas.pokedex.PokedexMainScreen
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * A casca com a barra inferior (`F1-T07`, docs/03 §1 e §2).
 *
 * **`Scaffold` com `NavigationBar`, nunca `NavigationSuiteScaffold`.** O adaptativo
 * troca a barra por rail lateral em largura expandida, e isso cobraria especificação e
 * teste de uma segunda navegação para atender ninguém: a premissa é celular no bolso.
 *
 * **As abas moram num `HorizontalPager` (29/10/2026).** O arraste lateral na raiz de uma
 * aba leva à vizinha, e a tela segue o dedo. O toque na barra rola o pager até a aba. Na
 * raiz não há mais arraste lateral que brigue com ele: a régua do odômetro e o heatmap do
 * Progresso passaram a ler só o toque. Fora da raiz o pager fica parado, e a Escada, o
 * trilho do longão e o pager do detalhe da espécie seguem com o arraste deles.
 *
 * ### Uma pilha por aba, e o voltar previsível
 *
 * - **Cada aba tem o próprio `NavHost` e o próprio controlador**, guardados aqui. O pager
 *   descarta a página que sai da tela; a pilha, a rolagem e os filtros ficam no controlador
 *   e no estado salvo da página, e voltam inteiros.
 * - **O grafo de cada aba continua aninhado** (`navigation<AbaHoje>`): a regra de camadas de
 *   [TransicoesDeNavegacao] e `PerfilPorAbaTest` leem a aba pela hierarquia.
 * - **O voltar na raiz de uma aba que não é `Hoje` rola até `Hoje`**; na raiz de `Hoje`, sai
 *   do app. É o único `BackHandler` da casca, e ele só liga onde a pilha da aba está vazia.
 * - **O segundo toque na aba corrente volta à raiz dela** ([voltarARaiz]).
 *
 * @param aoAbrirModal leva para a pilha modal, que vive **fora** desta casca — lista de
 *   planos, criação, entrada por código e edição de corrida (docs/03 §1). **O Perfil não
 *   está nesta lista**: ele é destino de cada aba, e sair pela modal o tiraria de baixo da
 *   barra.
 * @param aoRetomarCadastro **troca a casca pelo onboarding**, e não empilha em cima
 *   dela. Quem for morto entre autenticar e o passo 2 do cadastro chega aqui sem
 *   `users/{uid}`; navegar por [aoAbrirModal] deixaria a casca sem perfil embaixo.
 */
@Composable
fun CascaDeNavegacao(
    aoAbrirModal: (Any) -> Unit,
    aoRetomarCadastro: () -> Unit,
    aoSairDaConta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // `F2-T08`: o backfill do XP roda uma vez por entrada na casca, que é onde se chega
    // com sessão. Ele mesmo confere se já rodou, e trabalha no escopo do app.
    val container = LocalAppContainer.current
    LaunchedEffect(Unit) { container.backfillDeXp.seNecessario() }

    // `F4-T09`: o roteador da abertura (docs/01 §4.6). Semana fechada que ainda não foi
    // apresentada abre a tela cheia, uma vez. Uma falha de leitura abre a Home.
    LaunchedEffect(Unit) {
        if (container.roteadorDaAbertura.precisaDoFechamento()) aoAbrirModal(FechamentoDaSemana)
    }

    // O treinador da conta, para o atalho do Perfil de cada aba.
    val usuarioDaCasca by remember(container) {
        container.autenticacaoRepositorio.uidAtual
            ?.let { container.usuarioRepositorio.observar(it) }
            ?: flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)
    // A Pokédex está na barra desde o cadastro (29/10/2026): o álbum inteiro se vê com zero
    // corrida. Antes ela esperava o primeiro fechamento semanal (`F4-T11`), e quem começava
    // passava a primeira semana sem ver o que a corrida ia colar.
    //
    // O badge do destino novo some no primeiro acesso, e o primeiro acesso é lembrado. Depois
    // dele, o badge é o número de figurinhas que esperam no pacote: a corrida que liberou
    // espécie marca a aba onde a mudança está.
    val contexto = LocalContext.current
    val preferencias = remember(contexto) { contexto.getSharedPreferences("pokedex_aba", Context.MODE_PRIVATE) }
    var pokedexVista by remember { mutableStateOf(preferencias.getBoolean("vista", false)) }

    val noPacote by remember(container) { container.pacoteDaPokedex.quantas() }.collectAsStateWithLifecycle(initialValue = 0)
    val destinos = DestinoDeTopo.entries
    val pilhas = DestinoDeTopo.entries.associateWith { key(it) { rememberNavController() } }
    val ondeEsta = DestinoDeTopo.entries.associateWith { key(it) { pilhas.getValue(it).currentBackStackEntryAsState().value } }
    val acoesDaBarra = remember { DestinoDeTopo.entries.associateWith { AcoesDaBarraDoTopo() } }
    // A contagem e as chaves do pager leem o mesmo estado: liberar a Pokédex sobe as duas no
    // mesmo quadro, e a chave da quarta página nunca é pedida a uma lista de três.
    val paginas = rememberUpdatedState(destinos)
    val pager = rememberPagerState { paginas.value.size }
    val destinoAtual = destinos[pager.currentPage.coerceAtMost(destinos.lastIndex)]
    val pilhaAtual = pilhas.getValue(destinoAtual)
    val entradaAtual = ondeEsta.getValue(destinoAtual)
    val naRaiz = naRaizDaAba(entradaAtual?.destination)

    val reduzido = LocalMovimentoReduzido.current
    val escopo = rememberCoroutineScope()
    fun irParaAba(destino: DestinoDeTopo) {
        val pagina = destinos.indexOf(destino).takeIf { it >= 0 } ?: return
        escopo.launch {
            if (reduzido) pager.scrollToPage(pagina) else pager.animateScrollToPage(pagina, animationSpec = Movimento.padrao())
        }
    }

    LaunchedEffect(destinoAtual) {
        if (destinoAtual == DestinoDeTopo.POKEDEX && !pokedexVista) {
            pokedexVista = true
            preferencias.edit().putBoolean("vista", true).apply()
        }
    }

    // O `Abrir a Pokédex` da estreia (docs/03 §3.20.1) pede a aba pelo roteador, e a casca,
    // que é quem troca de aba, atende assim que ela existir.
    val pedidoDaPokedex by container.roteadorDaAbertura.pedidoDeAbrirAPokedex.collectAsStateWithLifecycle()
    LaunchedEffect(pedidoDaPokedex) {
        if (pedidoDaPokedex) {
            irParaAba(DestinoDeTopo.POKEDEX)
            container.roteadorDaAbertura.consumirPedidoDaPokedex()
        }
    }

    // Na raiz de uma aba que não é `Hoje`, o voltar rola até `Hoje`. Fora da raiz quem
    // atende é o `NavHost` da aba, e os dois nunca ligam juntos.
    BackHandler(enabled = naRaiz && destinoAtual != DestinoDeTopo.HOJE) { irParaAba(DestinoDeTopo.HOJE) }

    // Sair pela barra com nome, parceiro ou treinador sem salvar pergunta antes (27/10). O
    // voltar do Perfil pergunta na própria tela; aqui ficam a troca de aba e a Pokébola.
    val rascunhoDoPerfil = container.rascunhoDoPerfil
    var saidaPendente by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun guardado(acao: () -> Unit) {
        if (rascunhoDoPerfil.pendente) saidaPendente = acao else acao()
    }
    saidaPendente?.let { acao ->
        DialogoDeAlteracoesPendentes(
            aoSalvar = {
                saidaPendente = null
                rascunhoDoPerfil.salvar()
                acao()
            },
            aoContinuar = { saidaPendente = null },
            aoDescartar = {
                saidaPendente = null
                rascunhoDoPerfil.descartar()
                acao()
            },
        )
    }

    // As duas barras, a do topo e a inferior, moram só nas quatro raízes de aba (28/10/2026).
    val recolhimento = remember { RecolhimentoDaBarraDoTopo() }
    LaunchedEffect(destinoAtual, entradaAtual?.id) { recolhimento.mostrar() }
    // A semana da pílula sai do `ViewModel` da Home, que mora aqui e é o mesmo da tela: uma
    // cadeia de listeners só, e a Home que volta do arraste já chega com o dado.
    val home = viewModel(key = "semana-da-barra") {
        HomeViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
        )
    }
    val estadoDaSemana by home.estado.collectAsStateWithLifecycle()
    val semanaDaBarra = remember(estadoDaSemana) { semanaDaBarra(estadoDaSemana) }

    // O topo de cada página, para a barra de status deslizar junto com o dedo.
    val topoDaTela = remember { TopoDaTela() }
    val figurinhaEmVoo = remember { FigurinhaEmVoo() }
    val topos = destinos.map { topoDaAba(ondeEsta.getValue(it)?.destination, topoDaTela) }
    val topoAtual = topos[pager.currentPage.coerceAtMost(topos.lastIndex)]
    // O berço da Pokébola mostra o fundo do `Scaffold`, que acompanha a página aberta.
    val corDaPagina by animateColorAsState(topoAtual.pagina, Movimento.padrao(reduzido), label = "página atrás do berço")

    val saidas = SaidasDaCasca(
        aoAbrirModal = aoAbrirModal,
        aoRetomarCadastro = aoRetomarCadastro,
        aoSairDaConta = aoSairDaConta,
        aoIrParaAba = ::irParaAba,
        home = home,
    )

    CompositionLocalProvider(LocalTreinadorDoUsuario provides usuarioDaCasca?.treinador, LocalTopoDaTela provides topoDaTela) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = corDaPagina,
            bottomBar = {
                AnimatedVisibility(
                    visible = naRaiz,
                    enter = slideInVertically(Movimento.padrao()) { it },
                    exit = slideOutVertically(Movimento.saida()) { it },
                    label = "barra inferior",
                ) {
                    BarraDeDestinos(
                        destinoAtual = destinoAtual,
                        aoEscolher = { destino ->
                            guardado {
                                if (destino == destinoAtual) pilhaAtual.voltarARaiz() else irParaAba(destino)
                            }
                        },
                        // A Pokébola abre o registro por cima da aba em que se está.
                        aoRegistrarCorrida = { guardado { pilhaAtual.navigate(CorridaManual) { launchSingleTop = true } } },
                        destinos = destinos,
                        comBadge = if (!pokedexVista) setOf(DestinoDeTopo.POKEDEX) else emptySet(),
                        contagens = mapOf(DestinoDeTopo.POKEDEX to noPacote),
                    )
                }
            },
        ) { espacamento ->
            BarraDeStatus(pager, topos, iconesClaros = topoAtual.iconesClaros)

            val transicoes = rememberTransicoesDeNavegacao()
            // A barra do topo flutua sobre as abas e lê a rolagem de qualquer lista dentro delas.
            Box(
                modifier = Modifier
                    .padding(espacamento)
                    .consumeWindowInsets(espacamento)
                    .nestedScroll(recolhimento.conexao),
            ) {
                // A figurinha da Pokédex cresce até a frente do detalhe por cima do deslize.
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalEscopoCompartilhado provides this, LocalFigurinhaEmVoo provides figurinhaEmVoo) {
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.fillMaxSize(),
                            // A página assentada decide: o arraste não muda de regra no meio do gesto.
                            userScrollEnabled = naRaizDaAba(ondeEsta.getValue(destinos[pager.settledPage.coerceAtMost(destinos.lastIndex)])?.destination),
                            key = { paginas.value[it] },
                        ) { pagina ->
                            val aba = paginas.value[pagina]
                            AbaDaCasca(aba, pilhas.getValue(aba), acoesDaBarra.getValue(aba), transicoes, saidas)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = naRaiz,
                    enter = fadeIn(Movimento.padrao()) + slideInVertically(Movimento.padrao()) { -it / 2 },
                    exit = fadeOut(Movimento.saida()) + slideOutVertically(Movimento.saida()) { -it / 2 },
                    label = "barra do topo",
                ) {
                    BarraDoTopo(
                        semana = semanaDaBarra,
                        acoes = acoesDaBarra.getValue(destinoAtual),
                        recolhimento = recolhimento,
                        // O detalhe da semana abre por cima da aba em que se está, e o voltar
                        // devolve à raiz dela.
                        aoAbrirSemana = { pilhaAtual.navigate(DetalheDaSemana(it.planoId, it.numero)) { launchSingleTop = true } },
                        aoAbrirPerfil = { guardado { pilhaAtual.navigate(Perfil) { launchSingleTop = true } } },
                    )
                }
            }
        }
    }
}

/** O que as telas das abas pedem à casca. */
private class SaidasDaCasca(
    val aoAbrirModal: (Any) -> Unit,
    val aoRetomarCadastro: () -> Unit,
    val aoSairDaConta: () -> Unit,
    val aoIrParaAba: (DestinoDeTopo) -> Unit,
    val home: HomeViewModel,
)

/**
 * Uma aba: o `NavHost` dela, com o grafo aninhado e as telas que toda aba registra.
 *
 * **Perfil, corrida, semana e registro são registrados em cada aba**, e não num grafo global.
 * A tela abre por cima da aba em que se está, a barra continua com ela acesa, e o voltar
 * devolve à raiz dela.
 */
@Composable
private fun AbaDaCasca(
    aba: DestinoDeTopo,
    pilha: NavHostController,
    acoes: AcoesDaBarraDoTopo,
    transicoes: TransicoesDeNavegacao,
    saidas: SaidasDaCasca,
) {
    /**
     * O Perfil, registrado **uma vez em cada grafo de aba** (`F1-T07c`, docs/03 §1 e
     * docs/02 §10.1), com os dois seletores que ele abre. A tela é uma só e a rota é uma só;
     * o que se repete é o registro.
     */
    fun NavGraphBuilder.telaDePerfil() {
        composable<Perfil> {
            PerfilScreen(
                aoSairDaConta = saidas.aoSairDaConta,
                aoExcluirConta = { saidas.aoAbrirModal(ExcluirConta) },
                aoEscolherParceiro = { pilha.navigate(EscolherParceiro) },
                aoEscolherTreinador = { pilha.navigate(EscolherTreinador) },
                aoAbrirAlbum = { pilha.navigate(AlbumEncerrado(it)) },
                aoVoltar = { pilha.popBackStack() },
            )
        }
        // O álbum de uma temporada encerrada (RN-42) e as espécies dele, só leitura.
        composable<AlbumEncerrado> { entrada ->
            val temporadaId = entrada.toRoute<AlbumEncerrado>().temporadaId
            PokedexMainScreen(
                aoVoltar = { pilha.popBackStack() },
                aoAbrirEspecie = { pilha.navigate(EspecieEncerrada(it, temporadaId)) },
                temporadaId = temporadaId,
            )
        }
        composable<EspecieEncerrada> { entrada ->
            val rota = entrada.toRoute<EspecieEncerrada>()
            PokedexDetailScreen(
                numero = rota.numero,
                temporadaId = rota.temporadaId,
                aoVoltar = { pilha.popBackStack() },
                aoAbrirCorrida = {},
            )
        }
        composable<EscolherParceiro> { EscolherParceiroScreen(aoVoltar = { pilha.popBackStack() }) }
        composable<EscolherTreinador> { EscolherTreinadorScreen(aoVoltar = { pilha.popBackStack() }) }
    }

    /**
     * A corrida (`F2-T09`), a semana e o registro, que abrem de qualquer aba: a corrida pelo
     * histórico, pela Pokédex e pela semana; a semana pela pílula da barra do topo; o
     * registro pela Pokébola.
     */
    fun NavGraphBuilder.telasDeApoio() {
        composable<DetalheDaCorrida> { entrada ->
            ComVoltar(entrada, pilha) {
                DetalheCorridaScreen(
                    corridaId = entrada.toRoute<DetalheDaCorrida>().corridaId,
                    aoCorrigir = { saidas.aoAbrirModal(EditarCorrida(it)) },
                    aoAbrirMedalhas = { saidas.aoAbrirModal(MedalhasDoRitmo(it.name)) },
                )
            }
        }
        composable<DetalheDaSemana> { entrada ->
            val rota = entrada.toRoute<DetalheDaSemana>()
            ComVoltar(entrada, pilha) {
                DetalheSemanaScreen(
                    planoId = rota.planoId,
                    numero = rota.numero,
                    aoAbrirCorrida = { pilha.navigate(DetalheDaCorrida(it)) },
                )
            }
        }
        // docs/03 §1: registrar um treino é tarefa, e sobe por cima da aba.
        composable<CorridaManual> { entrada ->
            ComVoltar(entrada, pilha) {
                CorridaManualScreen(
                    aoSair = { pilha.popBackStack() },
                    aoAbrirMedalhas = { saidas.aoAbrirModal(MedalhasDoRitmo(it.name)) },
                )
            }
        }
    }

    NavHost(
        navController = pilha,
        startDestination = aba.grafo,
        modifier = Modifier.fillMaxSize(),
        enterTransition = transicoes.entrada,
        exitTransition = transicoes.saida,
        popEnterTransition = transicoes.entradaNoVoltar,
        popExitTransition = transicoes.saidaNoVoltar,
    ) {
        when (aba) {
            DestinoDeTopo.HOJE -> navigation<AbaHoje>(startDestination = Hoje) {
                composable<Hoje> {
                    NaRaizDeAba(acoes) {
                        HomeScreen(
                            vm = saidas.home,
                            aoAbrirPlanos = { saidas.aoAbrirModal(ListaDePlanos) },
                            aoCriarPlano = { saidas.aoAbrirModal(CriarPlano) },
                            aoEntrarComCodigo = { saidas.aoAbrirModal(EntrarComCodigo) },
                            aoRetomarCadastro = saidas.aoRetomarCadastro,
                            aoAbrirSemana = { planoId, numero -> pilha.navigate(DetalheDaSemana(planoId, numero)) },
                            aoAbrirCorrida = { pilha.navigate(DetalheDaCorrida(it)) },
                            // A placa de peito abre o plano (docs/03 §3.3), dentro da aba.
                            aoAbrirPlano = { pilha.navigate(DetalheDoPlano(it)) },
                            // O pulso do grupo leva à aba Grupo, onde mora o ranking.
                            aoAbrirGrupo = { saidas.aoIrParaAba(DestinoDeTopo.GRUPO) },
                            // Na Fase 4 o card da espécie é a porta da aba Pokédex (docs/03 §3.3.2).
                            aoAbrirPokedex = { saidas.aoIrParaAba(DestinoDeTopo.POKEDEX) },
                        )
                    }
                }
                composable<DetalheDoPlano> { entrada ->
                    val planoId = entrada.toRoute<DetalheDoPlano>().planoId
                    ComVoltar(entrada, pilha) {
                        DetalhePlanoScreen(
                            planoId = planoId,
                            aoAbrirCorrida = { pilha.navigate(DetalheDaCorrida(it)) },
                            aoAbrirGrupo = { pilha.navigate(GrupoDoPlano(planoId)) },
                        )
                    }
                }
                composable<GrupoDoPlano> { entrada ->
                    ComVoltar(entrada, pilha) {
                        GrupoDoPlanoScreen(planoId = entrada.toRoute<GrupoDoPlano>().planoId)
                    }
                }
                telasDeApoio()
                telaDePerfil()
            }

            DestinoDeTopo.PROGRESSO -> navigation<AbaProgresso>(startDestination = Progresso) {
                composable<Progresso> {
                    NaRaizDeAba(acoes) {
                        StatsDashboardScreen(
                            aoAbrirPorta = { porta ->
                                pilha.navigate(
                                    when (porta) {
                                        PortaDoProgresso.RITMO -> RitmoDoProgresso
                                        PortaDoProgresso.FITAS -> FitasDoProgresso
                                        PortaDoProgresso.SELOS -> SelosDoProgresso
                                        PortaDoProgresso.CORRIDAS -> CorridasDoProgresso
                                    },
                                )
                            },
                        )
                    }
                }
                composable<RitmoDoProgresso> { entrada -> ComVoltar(entrada, pilha) { RitmoScreen() } }
                composable<FitasDoProgresso> { entrada -> ComVoltar(entrada, pilha) { FitasScreen() } }
                composable<SelosDoProgresso> { entrada -> ComVoltar(entrada, pilha) { SelosScreen() } }
                composable<CorridasDoProgresso> { entrada ->
                    ComVoltar(entrada, pilha) {
                        HistoricoScreen(
                            aoAbrirCorrida = { pilha.navigate(DetalheDaCorrida(it)) },
                            aoRegistrarCorrida = { pilha.navigate(CorridaManual) },
                            cabecalho = {
                                CabecalhoDeFicha(
                                    sobrancelha = listOf(stringResource(R.string.destino_progresso)),
                                    titulo = stringResource(R.string.corridas_titulo),
                                )
                            },
                        )
                    }
                }
                telasDeApoio()
                telaDePerfil()
            }

            DestinoDeTopo.POKEDEX -> navigation<AbaPokedex>(startDestination = Pokedex) {
                // `F4-T07` e `F4-T08`: a grade e o detalhe da espécie, dentro da aba.
                composable<Pokedex> {
                    CompositionLocalProvider(LocalEscopoAnimado provides this) {
                        NaRaizDeAba(acoes) {
                            PokedexMainScreen(aoAbrirEspecie = { pilha.navigate(DetalheDaEspecie(it)) })
                        }
                    }
                }
                composable<DetalheDaEspecie> { entrada ->
                    CompositionLocalProvider(LocalEscopoAnimado provides this) {
                        PokedexDetailScreen(
                            numero = entrada.toRoute<DetalheDaEspecie>().numero,
                            aoVoltar = { pilha.popBackStack() },
                            aoAbrirCorrida = { pilha.navigate(DetalheDaCorrida(it)) },
                        )
                    }
                }
                telasDeApoio()
                telaDePerfil()
            }

            DestinoDeTopo.GRUPO -> navigation<AbaGrupo>(startDestination = Grupo) {
                // `F2-T12`: o ranking do grupo.
                composable<Grupo> {
                    NaRaizDeAba(acoes) {
                        GrupoScreen(aoAbrirMembro = { planoId, uid -> pilha.navigate(CartaoDoMembro(planoId, uid)) })
                    }
                }
                // O cartão do treinador de um membro, só leitura (27/10/2026).
                composable<CartaoDoMembro> { entrada ->
                    val rota = entrada.toRoute<CartaoDoMembro>()
                    MembroScreen(planoId = rota.planoId, uid = rota.uid, aoVoltar = { pilha.popBackStack() })
                }
                telasDeApoio()
                telaDePerfil()
            }
        }
    }
}

/**
 * A raiz de uma aba, debaixo da barra do topo: a folga que ela reserva no próprio fundo e o
 * lugar para pedir uma ação na barra. Cada aba tem as próprias ações: no arraste, as duas
 * raízes estão na tela juntas, e uma não pode soltar o pedido da outra.
 */
@Composable
private fun NaRaizDeAba(acoes: AcoesDaBarraDoTopo, conteudo: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFolgaDaBarraDoTopo provides AlturaDaBarraDoTopo,
        LocalAcoesDaBarraDoTopo provides acoes,
        content = conteudo,
    )
}

/**
 * Um detalhe: a seta de voltar no cabeçalho de ficha dele (28/10/2026). Só a entrada que está
 * na frente volta; dois toques rápidos durante a saída não tiram a raiz da pilha.
 */
@Composable
internal fun ComVoltar(entrada: NavBackStackEntry, navegacao: NavHostController, conteudo: @Composable () -> Unit) {
    val voltar = remember(entrada, navegacao) {
        {
            if (entrada.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) navegacao.popBackStack()
            Unit
        }
    }
    CompositionLocalProvider(LocalVoltarDoCabecalho provides voltar, content = conteudo)
}

/** As quatro raízes, as únicas telas com as duas barras e com o arraste entre abas. */
private val RAIZES: List<KClass<out Any>> = listOf(Hoje::class, Progresso::class, Pokedex::class, Grupo::class)

/** Antes do primeiro quadro da aba não há destino, e ela abre na raiz. */
private fun naRaizDaAba(destino: NavDestination?): Boolean = destino == null || RAIZES.any { destino.hasRoute(it) }

/**
 * O segundo toque na aba acesa: tira da pilha tudo acima da raiz dela, como a barra
 * inferior do Android faz. Já na raiz, não muda nada.
 */
private fun NavHostController.voltarARaiz() {
    if (currentBackStackEntry == null) return
    popBackStack(graph.findStartDestination().id, inclusive = false)
}

/**
 * O topo de uma página: a cor atrás da barra de status, os ícones dela e a página atrás do
 * berço da Pokébola. [noQuadro] é a cor lida a cada quadro no arraste do detalhe da espécie.
 */
@Immutable
private class TopoDaAba(val cor: Color, val iconesClaros: Boolean, val pagina: Color, val noQuadro: (() -> Color)?)

/**
 * A regra do topo de cada destino. A Home pinta a camisa vermelha, com ícones brancos; a
 * arena e a capa da Pokédex, a tinta; o detalhe da espécie, a cor que ele pede; o histórico,
 * o fundo branco; e todo o resto, a página tingida.
 */
@Composable
private fun topoDaAba(destino: NavDestination?, topo: TopoDaTela): TopoDaAba {
    val esquema = MaterialTheme.colorScheme
    val afundada = LocalCoresPokerun.current.superficieAfundada
    fun em(vararg rotas: KClass<out Any>) = destino != null && rotas.any { destino.hasRoute(it) }

    val naListaDaPokedex = em(Pokedex::class, AlbumEncerrado::class)
    val naPokedex = naListaDaPokedex || em(DetalheDaEspecie::class, EspecieEncerrada::class)
    val naHome = em(Hoje::class)
    val naArena = em(CorridaManual::class)
    // A raiz da Pokédex começa na capa em tinta; com a capa fora da tela, a página tingida.
    val pedido = topo.cor.takeIf { naPokedex } ?: afundada.takeIf { naPokedex && !naListaDaPokedex }
    val pokedexEmTinta = naListaDaPokedex && (topo.cor == null || topo.iconesClaros)
    val branca = destino == null || em(CorridasDoProgresso::class)
    val cor = when {
        naHome -> esquema.primary
        pedido != null -> pedido
        naArena || pokedexEmTinta -> esquema.onSurface
        branca -> esquema.background
        else -> afundada
    }
    return TopoDaAba(
        cor = cor,
        iconesClaros = naHome || naArena || pokedexEmTinta,
        pagina = if (cor == afundada || naArena || naPokedex) afundada else esquema.background,
        noQuadro = topo.corNoQuadro.takeIf { naPokedex && cor == topo.cor },
    )
}

/**
 * A faixa atrás da barra de status. O `NavHost` recebe o recuo do topo, então uma tela de
 * topo colorido começaria abaixo de uma faixa branca. **No arraste entre abas, a faixa de
 * cada página anda com ela**: a posição do pager é lida só no desenho, sem recompor. Ao
 * sair, os ícones voltam a escuros, que é o padrão do app (D-13).
 */
@Composable
private fun BarraDeStatus(pager: PagerState, topos: List<TopoDaAba>, iconesClaros: Boolean) {
    val view = LocalView.current
    DisposableEffect(iconesClaros) {
        val janela = (view.context as? Activity)?.window
        val controle = janela?.let { WindowCompat.getInsetsController(it, view) }
        controle?.isAppearanceLightStatusBars = !iconesClaros
        onDispose { controle?.isAppearanceLightStatusBars = true }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .drawBehind {
                val posicao = pager.currentPage + pager.currentPageOffsetFraction
                topos.forEachIndexed { i, topo ->
                    val x = (i - posicao) * size.width
                    if (x > -size.width && x < size.width) {
                        drawRect(topo.noQuadro?.invoke() ?: topo.cor, topLeft = Offset(x, 0f), size = size)
                    }
                }
            },
    )
}
