package com.hggabriel.pokerun.ui.telas.pokedex

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
import androidx.compose.runtime.snapshotFlow
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
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
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
) {
    val container = LocalAppContainer.current
    val vm: PokedexDetalheViewModel = viewModel {
        PokedexDetalheViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.corridaRepositorio,
            container.especiesRepositorio,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nomes by vm.nomes.collectAsStateWithLifecycle()
    val tipos by vm.tipos.collectAsStateWithLifecycle()
    val fichas by vm.fichas.collectAsStateWithLifecycle()
    val evolucoes by vm.evolucoes.collectAsStateWithLifecycle()
    val tocarGrito = rememberTocadorDeGrito()

    when (val atual = estado) {
        DetalheUiState.Carregando -> Esqueleto(modifier)
        DetalheUiState.Falhou -> Column(modifier.fillMaxSize().background(LocalCoresPokerun.current.superficieAfundada).padding(Espacos.margem)) {
            BannerDeAlerta(rotulo = stringResource(R.string.pokedex_erro_rotulo), texto = stringResource(R.string.pokedex_detalhe_erro))
            Spacer(Modifier.height(Espacos.margem))
            BotaoPrimario(aoTocar = vm::tentarDeNovo, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_erro_repetir)) }
        }
        is DetalheUiState.Conteudo -> Pager(
            conteudo = atual,
            inicial = numero,
            nomes = nomes,
            tipos = tipos,
            fichas = fichas,
            evolucoes = evolucoes,
            aoAssentar = { n, colada, vizinhas -> vm.carregarPagina(n, colada, vizinhas, atual.ordem.toSet()) },
            aoTocarGrito = { tocarGrito(vm.grito(it)) },
            aoVoltar = aoVoltar,
            aoAbrirCorrida = aoAbrirCorrida,
            modifier = modifier,
        )
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
    aoAssentar: (Int, Boolean, List<Int>) -> Unit,
    aoTocarGrito: (Int) -> Unit,
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
    LaunchedEffect(corDoTopo) { topo.pedir(dono, corDoTopo, iconesClaros = false) }
    DisposableEffect(topo) { onDispose { topo.soltar(dono) } }

    HorizontalPager(
        state = pager,
        modifier = modifier.fillMaxSize().background(LocalCoresPokerun.current.superficieAfundada),
        beyondViewportPageCount = 0,
        key = { ordem[it] },
    ) { i ->
        val n = ordem[i]
        val (paginaDoTier, figurinha) = porNumero.getValue(n)
        val pagina = PaginaDaEspecie(
            numero = n,
            posicao = figurinha.posicao,
            total = conteudo.temporada.totalEspecies,
            colada = figurinha.colada,
            tier = paginaDoTier.ordem,
            nomeDoTier = paginaDoTier.nome,
            nome = nomes[n],
            tipos = tipos[n].orEmpty(),
            coladaEm = album.coladaEm[figurinha.posicao],
            kmFalta = album.kmParaAlcancar[figurinha.posicao],
            fracao = album.fracaoParaAlcancar[figurinha.posicao] ?: 0f,
            ficha = fichas[n],
            evolucao = evolucoes[n].orEmpty(),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.secao),
            verticalArrangement = Arrangement.spacedBy(Espacos.margem),
        ) {
            item {
                FrenteDaEspecie(
                    pagina = pagina,
                    anterior = ordem.getOrNull(i - 1)?.let { porNumero[it]?.second },
                    proxima = ordem.getOrNull(i + 1)?.let { porNumero[it]?.second },
                    nomes = nomes,
                    tipos = tipos,
                    aoVoltar = aoVoltar,
                    aoIrPara = irPara,
                    aoTocarGrito = { aoTocarGrito(n) },
                )
            }
            item {
                VersoDaEspecie(
                    pagina = pagina,
                    fuso = conteudo.temporada.fuso,
                    colada = { porNumero[it]?.second?.colada == true },
                    nomes = nomes,
                    tipos = tipos,
                    aoAbrirCorrida = aoAbrirCorrida,
                    aoIrPara = irPara,
                )
            }
        }
    }
}

/** A frente em `superficie-afundada` e dois cartões vazios, sem número. */
@Composable
private fun Esqueleto(modifier: Modifier) {
    val cores = LocalCoresPokerun.current
    Column(modifier.fillMaxSize().background(cores.superficieAfundada), verticalArrangement = Arrangement.spacedBy(Espacos.margem)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        )
        repeat(2) {
            Box(
                Modifier
                    .padding(horizontal = Espacos.margem)
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(MaterialTheme.colorScheme.surface, FormaDoCartao),
            )
        }
    }
}
