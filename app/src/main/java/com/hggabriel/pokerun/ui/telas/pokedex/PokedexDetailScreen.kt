package com.hggabriel.pokerun.ui.telas.pokedex

import com.hggabriel.pokerun.ui.componentes.AtualizarAoPuxar
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import androidx.compose.foundation.layout.size
import com.hggabriel.pokerun.ui.componentes.ComBrilhoDeEspera
import com.hggabriel.pokerun.ui.componentes.esperaComBrilho
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.FichaDaEspecie
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.navegacao.LocalTopoDaTela
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import kotlinx.coroutines.launch

/**
 * A linha de velocidade da criatura (docs/07, `03 §3.19`): só as espécies de que o
 * documento de curadoria escreve uma. O resto das espécies não tem linha, e a tela não
 * inventa uma. Devolve o recurso, que leva o nome da espécie.
 */
@StringRes
fun linhaDeVelocidade(numero: Int): Int? = when (numero) {
    18 -> R.string.velocidade_018
    22 -> R.string.velocidade_022
    51 -> R.string.velocidade_051
    53 -> R.string.velocidade_053
    59 -> R.string.velocidade_059
    78 -> R.string.velocidade_078
    85 -> R.string.velocidade_085
    121 -> R.string.velocidade_121
    135 -> R.string.velocidade_135
    142 -> R.string.velocidade_142
    149 -> R.string.velocidade_149
    else -> null
}

/**
 * A `PokedexDetailScreen` (docs/03 §3.19): a figurinha ampliada, num pager pela ordem dos
 * tiers. A frente é a figurinha grande em `tipo-*-suave`, com o 3D e o grito; o verso, os
 * cartões da corrida que a colou, da evolução e da ficha. A não alcançada fica trancada.
 * A tela não grava nada.
 */
@Composable
fun PokedexDetailScreen(
    numero: Int,
    aoVoltar: () -> Unit,
    aoAbrirCorrida: (String) -> Unit,
    modifier: Modifier = Modifier,
    temporadaId: String? = null,
) {
    val container = LocalAppContainer.current
    val vm: PokedexDetalheViewModel = viewModel(key = "especies-" + temporadaId.orEmpty()) {
        PokedexDetalheViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.corridaRepositorio,
            container.especiesRepositorio,
            temporadaId,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nomes by vm.nomes.collectAsStateWithLifecycle()
    val tipos by vm.tipos.collectAsStateWithLifecycle()
    val fichas by vm.fichas.collectAsStateWithLifecycle()
    val evolucoes by vm.evolucoes.collectAsStateWithLifecycle()
    val versosProntos by vm.versosProntos.collectAsStateWithLifecycle()
    val tocarGrito = rememberTocadorDeGrito()

    when (val atual = estado) {
        DetalheUiState.Carregando -> Esqueleto(modifier)
        DetalheUiState.Falhou -> Column(modifier.fillMaxSize().background(LocalCoresPokerun.current.superficieAfundada).padding(Espacos.margem)) {
            BannerDeAlerta(rotulo = stringResource(R.string.pokedex_erro_rotulo), texto = stringResource(R.string.pokedex_detalhe_erro))
            Spacer(Modifier.height(Espacos.margem))
            BotaoPrimario(aoTocar = vm::tentarDeNovo, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_erro_repetir)) }
        }
        // O puxar relê o álbum e refaz o pedido das imagens que falharam.
        is DetalheUiState.Conteudo -> AtualizarAoPuxar(aoAtualizar = vm::tentarDeNovo, modifier = modifier) {
            Pager(
                conteudo = atual,
                inicial = numero,
                nomes = nomes,
                tipos = tipos,
                fichas = fichas,
                evolucoes = evolucoes,
                versosProntos = versosProntos,
                aoAssentar = { n, colada, vizinhas ->
                    vm.carregarPagina(n, colada, vizinhas, atual.ordem.toSet())
                    // A colada aberta no detalhe deixa de ser nova.
                    if (colada) container.autenticacaoRepositorio.uidAtual?.let { uid -> container.preferenciasDoAparelho.marcarVista(uid, atual.temporada.id, n) }
                },
                aoTocarGrito = { tocarGrito.tocar(it, vm.grito(it)) },
                faseDoGrito = tocarGrito::faseDe,
                aoVoltar = aoVoltar,
                aoAbrirCorrida = aoAbrirCorrida,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun Pager(
    conteudo: DetalheUiState.Conteudo,
    inicial: Int,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    fichas: Map<Int, FichaDaEspecie>,
    evolucoes: Map<Int, List<List<Int>>>,
    versosProntos: Set<Int>,
    aoAssentar: (Int, Boolean, List<Int>) -> Unit,
    aoTocarGrito: (Int) -> Unit,
    faseDoGrito: (Int) -> FaseDoGrito,
    aoVoltar: () -> Unit,
    aoAbrirCorrida: (String) -> Unit,
    modifier: Modifier,
) {
    val album = conteudo.album
    val ordem = conteudo.ordem
    val porNumero = remember(album) { album.paginas.flatMap { p -> p.figurinhas.map { it.numero to (p to it) } }.toMap() }
    val pager = rememberPagerState(initialPage = ordem.indexOf(inicial).coerceAtLeast(0)) { ordem.size }
    val escopo = rememberCoroutineScope()
    val reduzido = LocalMovimentoReduzido.current
    val irPara: (Int) -> Unit = { n ->
        val i = ordem.indexOf(n)
        if (i >= 0) escopo.launch { if (reduzido) pager.scrollToPage(i) else pager.animateScrollToPage(i) }
    }

    // A página que assenta busca o que mostra, e o topo da tela passa à cor dela.
    LaunchedEffect(pager, ordem) {
        snapshotFlow { pager.settledPage }.collect { i ->
            val n = ordem[i]
            val colada = porNumero[n]?.second?.colada == true
            aoAssentar(n, colada, listOfNotNull(ordem.getOrNull(i - 1), ordem.getOrNull(i + 1)))
        }
    }
    val topo = LocalTopoDaTela.current
    val dono = remember { Any() }
    val numeroAberto = ordem[pager.currentPage]
    val tipoAberto = tipos[numeroAberto]?.firstOrNull()?.takeIf { porNumero[numeroAberto]?.second?.colada == true }
    val corDoTopo = tipoAberto?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada

    val paginaDe: (Int) -> PaginaDaEspecie = { i ->
        val n = ordem[i]
        val (paginaDoTier, figurinha) = porNumero.getValue(n)
        PaginaDaEspecie(
            numero = n,
            posicao = figurinha.posicao,
            total = conteudo.temporada.totalEspecies,
            colada = figurinha.colada,
            tier = paginaDoTier.ordem,
            nomeDoTier = paginaDoTier.nome,
            nome = if (figurinha.secreta) null else nomes[n],
            tipos = if (figurinha.secreta) emptyList() else tipos[n].orEmpty(),
            coladaEm = album.coladaEm[figurinha.posicao],
            kmFalta = album.kmParaAlcancar[figurinha.posicao],
            fracao = album.fracaoParaAlcancar[figurinha.posicao] ?: 0f,
            ficha = fichas[n],
            evolucao = evolucoes[n].orEmpty(),
            secreta = figurinha.secreta,
        )
    }

    // No arraste, a barra de status segue o fundo da frente quadro a quadro.
    val fundo by rememberUpdatedState(fundoDaFrente(pager, paginaDe))
    val fundoNoQuadro = remember { { fundo() } }
    LaunchedEffect(corDoTopo) { topo.pedir(dono, corDoTopo, iconesClaros = false, noQuadro = fundoNoQuadro) }
    DisposableEffect(topo) { onDispose { topo.soltar(dono) } }

    // A frente fica parada e troca as figurinhas no palco; só o verso desliza no pager.
    LazyColumn(
        modifier = modifier.fillMaxSize().background(LocalCoresPokerun.current.superficieAfundada),
        contentPadding = PaddingValues(bottom = Espacos.secao),
        verticalArrangement = Arrangement.spacedBy(Espacos.margem),
    ) {
        item(key = "frente") {
            FrenteDaEspecie(
                pager = pager,
                pagina = paginaDe,
                aoVoltar = aoVoltar,
                aoIrPara = irPara,
                aoTocarGrito = aoTocarGrito,
                faseDoGrito = faseDoGrito,
                inicial = inicial,
            )
        }
        item(key = "verso") {
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 0,
                verticalAlignment = Alignment.Top,
                flingBehavior = PagerDefaults.flingBehavior(pager, snapPositionalThreshold = LimiarDoArraste),
                key = { ordem[it] },
            ) { i ->
                VersoDaEspecie(
                    pagina = paginaDe(i),
                    fuso = conteudo.temporada.fuso,
                    colada = { porNumero[it]?.second?.colada == true },
                    secreta = { porNumero[it]?.second?.secreta == true },
                    nomes = nomes,
                    tipos = tipos,
                    aoAbrirCorrida = aoAbrirCorrida,
                    aoIrPara = irPara,
                    carregando = ordem[i].let { n -> porNumero[n]?.second?.colada == true && n !in versosProntos },
                )
            }
        }
    }
}

/**
 * A frente na forma do palco, com a figurinha grande, o nome e o tipo, e os cartões do verso
 * colado (a corrida, a evolução e a ficha), todos com o brilho da espera (29/10/2026).
 */
@Composable
private fun Esqueleto(modifier: Modifier) {
    val afundada = LocalCoresPokerun.current.superficieAfundada
    ComBrilhoDeEspera {
        Column(modifier.fillMaxSize().background(afundada), verticalArrangement = Arrangement.spacedBy(Espacos.margem)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(bottom = Espacos.bloco),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.size(212.dp, 236.dp).esperaComBrilho(RoundedCornerShape(20.dp), afundada))
                Spacer(Modifier.height(Espacos.bloco))
                Spacer(Modifier.size(160.dp, 28.dp).esperaComBrilho(RoundedCornerShape(6.dp), afundada))
                Spacer(Modifier.height(Espacos.relacionado))
                Spacer(Modifier.size(28.dp).esperaComBrilho(FormaCirculo, afundada))
            }
            VersoEmEspera(Modifier.padding(horizontal = Espacos.margem))
        }
    }
}
