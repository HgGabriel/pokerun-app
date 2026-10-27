package com.hggabriel.pokerun.ui.telas.pokedex

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.componentes.fonteAmpliada
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.navegacao.LocalTopoDaTela
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUNAS = 3
private const val COLUNAS_COM_FONTE_GRANDE = 2

/** Os itens antes da primeira página: só a capa. */
private const val ITENS_ANTES_DAS_PAGINAS = 1

/**
 * A `PokedexMainScreen` (docs/03 §3.18): o álbum de figurinhas da temporada. A capa em
 * tinta mostra o trecho em que o corredor está, com a vaga do próximo marcada pelo km;
 * abaixo, uma página por tier. Só a página do tier atual vem aberta; as outras ficam
 * fechadas em maço e abrem no toque.
 *
 * - **Nunca esconde número nem nome** de espécie não alcançada: a vaga mostra os dois.
 * - **A coleção atrás da posição fica colada** (RN-38).
 * - A tela abre no topo, na capa: a primeira coisa é o próximo.
 */
@Composable
fun PokedexMainScreen(
    aoAbrirPerfil: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
    modifier: Modifier = Modifier,
    temporadaId: String? = null,
) {
    val container = LocalAppContainer.current
    val vm: PokedexViewModel = viewModel(key = "album-" + temporadaId.orEmpty()) {
        PokedexViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.especiesRepositorio,
            container.preferenciasDoAparelho,
            temporadaId,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nomes by vm.nomes.collectAsStateWithLifecycle()
    val tipos by vm.tipos.collectAsStateWithLifecycle()

    (estado as? PokedexUiState.Conteudo)?.let { conteudo ->
        LaunchedEffect(conteudo.album) {
            val todas = conteudo.album.paginas.flatMap { it.figurinhas }
            vm.carregarEspecies(
                numeros = todas.map { it.numero },
                comTipo = todas.filter { it.colada || it.posicao in conteudo.album.pacote }.map { it.numero }.toSet(),
            )
        }
    }

    PokedexMainScreen(
        estado = estado,
        nomes = nomes,
        tipos = tipos,
        aoAbrirPerfil = aoAbrirPerfil,
        aoAbrirEspecie = aoAbrirEspecie,
        aoTentarDeNovo = vm::tentarDeNovo,
        aoAbrirPacote = vm::abrirPacote,
        estreia = vm.estreia,
        aoTocarACascata = vm::cascataTocou,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews usam. */
@Composable
internal fun PokedexMainScreen(
    estado: PokedexUiState,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoAbrirPerfil: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    aoAbrirPacote: () -> Unit = {},
    estreia: Boolean = false,
    aoTocarACascata: () -> Unit = {},
) {
    val lista = rememberLazyListState()
    val escopo = rememberCoroutineScope()
    val reduzido = LocalMovimentoReduzido.current
    TopoEmTintaEnquantoACapaAparece(lista)
    // As páginas que o corredor abriu, além da do tier atual, que vem sempre aberta.
    var abertas by rememberSaveable { mutableStateOf(listOf<Int>()) }
    // As figurinhas que estão colando agora (posição → atraso em ms): o pacote ou a cascata.
    var batidas by remember { mutableStateOf(emptyMap<Int, Int>()) }
    var cascataPendente by rememberSaveable { mutableStateOf(estreia) }

    val conteudo = estado as? PokedexUiState.Conteudo
    if (conteudo != null && cascataPendente) {
        LaunchedEffect(Unit) {
            cascataPendente = false
            aoTocarACascata()
            if (!reduzido) batidas = cascata(conteudo.album)
        }
    }
    if (batidas.isNotEmpty()) {
        LaunchedEffect(batidas) {
            delay((batidas.values.max() + Movimento.Duracao.MEDIA).toLong())
            batidas = emptyMap()
        }
    }

    LazyColumn(
        state = lista,
        modifier = modifier
            .fillMaxSize()
            .background(LocalCoresPokerun.current.superficieAfundada)
            // Um toque durante a cascata pula para o fim, sem roubar o toque de ninguém.
            .pointerInput(batidas.isNotEmpty()) {
                if (batidas.isEmpty()) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    batidas = emptyMap()
                }
            },
        contentPadding = PaddingValues(bottom = Espacos.secao),
        verticalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        when (estado) {
            PokedexUiState.Carregando -> {
                item(key = "capa") { CapaEmEspera(aoAbrirPerfil, esqueleto = true) }
                esqueleto()
            }
            PokedexUiState.SemTemporada -> {
                item(key = "capa") { CapaEmEspera(aoAbrirPerfil, esqueleto = false) }
                item {
                    EstadoVazio(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                        Text(
                            text = stringResource(R.string.pokedex_sem_temporada),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            PokedexUiState.Falhou -> {
                item(key = "capa") { CapaEmEspera(aoAbrirPerfil, esqueleto = false) }
                item {
                    Column(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                        BannerDeAlerta(
                            rotulo = stringResource(R.string.pokedex_erro_rotulo),
                            texto = stringResource(R.string.pokedex_erro_corpo),
                        )
                        Spacer(Modifier.height(Espacos.margem))
                        BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_erro_repetir))
                        }
                    }
                }
            }
            is PokedexUiState.Conteudo -> album(
                album = estado.album,
                nomes = nomes,
                tipos = tipos,
                lista = lista,
                abertas = abertas.toSet() + estado.album.tierAtual,
                aoAbrirPagina = { if (it !in abertas) abertas = abertas + it },
                batidas = batidas,
                aoAbrirPacote = {
                    val pacote = estado.album.pacote.toList()
                    aoAbrirPacote()
                    if (!reduzido && pacote.isNotEmpty()) {
                        batidas = pacote.withIndex().associate { (i, p) -> p to i * Movimento.Album.ENTRE_FIGURINHAS }
                        // A rolagem leva à página da primeira figurinha nova.
                        val ordem = estado.album.paginas.firstOrNull { pg -> pg.figurinhas.any { it.posicao == pacote.first() } }?.ordem
                        if (ordem != null) {
                            if (ordem !in abertas) abertas = abertas + ordem
                            val indice = ITENS_ANTES_DAS_PAGINAS + estado.album.paginas.indexOfFirst { it.ordem == ordem }
                            escopo.launch { lista.animateScrollToItem(indice) }
                        }
                    }
                },
                aoAbrirPerfil = aoAbrirPerfil,
                aoAbrirEspecie = aoAbrirEspecie,
            )
        }
    }
}

/**
 * A barra de status acompanha a capa: em tinta enquanto ela aparece no topo, na página
 * tingida quando ela sai. Ao sair da tela, a casca volta ao padrão.
 */
@Composable
private fun TopoEmTintaEnquantoACapaAparece(lista: LazyListState) {
    val topo = LocalTopoDaTela.current
    val tinta = MaterialTheme.colorScheme.onSurface
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val dono = remember { Any() }
    LaunchedEffect(lista) {
        snapshotFlow { lista.firstVisibleItemIndex == 0 }.collect { emTinta ->
            topo.pedir(dono, if (emTinta) tinta else afundada, iconesClaros = emTinta)
        }
    }
    DisposableEffect(topo) { onDispose { topo.soltar(dono) } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.esqueleto() {
    val alturas = listOf(76.dp, 76.dp, 76.dp, 320.dp)
    alturas.forEachIndexed { i, altura ->
        item(key = "esqueleto-$i") {
            Box(
                Modifier
                    .padding(horizontal = Espacos.margem)
                    .fillMaxWidth()
                    .height(altura)
                    .background(MaterialTheme.colorScheme.surface, FormaDoCartao),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.album(
    album: AlbumMontado,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    lista: LazyListState,
    abertas: Set<Int>,
    aoAbrirPagina: (Int) -> Unit,
    batidas: Map<Int, Int>,
    aoAbrirPacote: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
) {
    item(key = "capa") {
        val escopo = rememberCoroutineScope()
        val reduzido = LocalMovimentoReduzido.current
        CapaDoAlbum(
            capa = album.capa,
            nomes = nomes,
            tipos = tipos,
            aoAbrirPerfil = aoAbrirPerfil,
            aoAbrirEspecie = aoAbrirEspecie,
            aoEscolherTier = { ordem ->
                aoAbrirPagina(ordem)
                val indice = ITENS_ANTES_DAS_PAGINAS + album.paginas.indexOfFirst { it.ordem == ordem }.coerceAtLeast(0)
                escopo.launch { if (reduzido) lista.scrollToItem(indice) else lista.animateScrollToItem(indice) }
            },
            batidas = batidas,
            pacote = {
                if (!album.pacote.isEmpty()) PacoteDeFigurinhas(quantas = album.pacote.count(), aoAbrir = aoAbrirPacote)
            },
        )
    }

    // O topo alcançável é o tier antes do lacrado: a página brilhante.
    val brilhante = album.paginas.lastOrNull { !it.lacrada }?.ordem
    album.paginas.forEach { pagina ->
        item(key = "tier-${pagina.ordem}", contentType = if (pagina.lacrada) "envelope" else "pagina") {
            val modificador = Modifier.padding(horizontal = Espacos.margem)
            val colunas = if (fonteAmpliada()) COLUNAS_COM_FONTE_GRANDE else COLUNAS
            when {
                pagina.lacrada -> EnvelopeLacrado(pagina, nomes, modificador)
                pagina.ordem in abertas -> PaginaAberta(
                    pagina = pagina,
                    nomes = nomes,
                    tipos = tipos,
                    colunas = colunas,
                    falta = album.capa.kmAteOProximo?.let {
                        stringResource(R.string.pokedex_falta_km, formatarKm(it))
                    },
                    aoAbrirEspecie = aoAbrirEspecie,
                    brilho = if (pagina.ordem == brilhante) reflexoPelaRolagem(lista, "tier-${pagina.ordem}") else null,
                    batidas = batidas,
                    modifier = modificador,
                )
                else -> PaginaFechada(pagina, tipos, aoAbrir = { aoAbrirPagina(pagina.ordem) }, modifier = modificador)
            }
        }
    }
}

/**
 * A posição do reflexo da página brilhante: de 0 a 1 enquanto ela atravessa a tela. Lida
 * no desenho, então a rolagem só redesenha a figurinha.
 */
/**
 * A cascata de estreia: as coladas que estão à vista (a fita e a página do tier atual)
 * caem em ordem, tier a tier, em [Movimento.Album.CASCATA].
 */
private fun cascata(album: AlbumMontado): Map<Int, Int> {
    val visiveis = (album.capa.fita + album.paginas.filter { it.ordem == album.tierAtual }.flatMap { it.figurinhas })
        .filter { it.colada }
        .map { it.posicao }
        .distinct()
        .sorted()
    return visiveis.withIndex().associate { (i, p) -> p to Movimento.Album.atrasoNaCascata(i, visiveis.size) }
}

private fun reflexoPelaRolagem(lista: LazyListState, chave: String): () -> Float = {
    val info = lista.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == chave }
    if (item == null) {
        0.5f
    } else {
        val altura = (info.viewportEndOffset - info.viewportStartOffset).toFloat().coerceAtLeast(1f)
        ((altura - item.offset) / (altura + item.size)).coerceIn(0f, 1f)
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val TemporadaDoPreview = Temporada(
    id = "kanto-2026",
    nome = "Kanto 2026",
    regiao = "Kanto",
    inicio = java.time.Instant.parse("2026-01-01T03:00:00Z"),
    fim = java.time.Instant.parse("2027-01-01T03:00:00Z"),
    fuso = java.time.ZoneId.of("America/Sao_Paulo"),
    ativa = true,
    totalEspecies = 15,
    tiers = listOf(
        Tier(1, "Passo", "", listOf(10, 11, 13, 14, 16), 100),
        Tier(2, "Trote", "", listOf(19, 20, 21, 22, 23), 250),
        Tier(3, "Lenda", "", listOf(144, 145, 146, 149, 150), 8000),
    ),
)

@Composable
private fun PokedexPreview(estado: PokedexUiState) {
    PokerunTheme {
        PokedexMainScreen(
            estado = estado,
            nomes = (1..150).associateWith { "Especie $it" },
            tipos = (1..150).associateWith { listOf(TipoDePokemon.entries[it % TipoDePokemon.entries.size]) },
            aoAbrirPerfil = {},
            aoAbrirEspecie = {},
            aoTentarDeNovo = {},
        )
    }
}

private fun conteudoDeExemplo(xp: Long, colecao: Int) = PokedexUiState.Conteudo(
    temporada = TemporadaDoPreview,
    album = montarAlbum(TemporadaDoPreview, xp, colecao, emptyList(), emptyList(), 300, java.time.Instant.now()),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun PokedexNaPosicaoPreview() = PokedexPreview(conteudoDeExemplo(xp = 900, colecao = 6))

@Preview(showBackground = true, widthDp = 320, heightDp = 1200, fontScale = 2.0f)
@Composable
private fun PokedexFonteGrandePreview() = PokedexPreview(conteudoDeExemplo(xp = 900, colecao = 6))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PokedexCarregandoPreview() = PokedexPreview(PokedexUiState.Carregando)
