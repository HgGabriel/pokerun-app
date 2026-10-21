package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.dominio.regras.PainelDaPokedex
import com.hggabriel.pokerun.dominio.regras.gradeDaPokedex
import com.hggabriel.pokerun.dominio.regras.painelDaPokedex
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.Escada
import com.hggabriel.pokerun.ui.componentes.EscadaDeProgresso
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.VarianteDaEscada
import com.hggabriel.pokerun.ui.componentes.escadaDe
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.componentes.padraoDePokebolas
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaCard
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Tamanhos
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity

private val AnelDaPosicao = 3.dp
private val LadoDoMarcador = 20.dp

/** Dois chips pequenos de 22dp e o espaço entre eles. */
private val AlturaDaAreaDeTipos = 22.dp * 2 + 4.dp

/** Quantos itens vêm antes da primeira célula: o cabeçalho, a escada e o painel. */
private const val ITENS_ANTES_DA_GRADE = 3

/**
 * A `PokedexMainScreen` (`F4-T07`, docs/03 §3.18): a escada em dois níveis no topo, o
 * painel de três linhas e a grade de 3 colunas por tier, em `LazyVerticalGrid`.
 *
 * - **Nunca esconde número nem nome** de espécie não alcançada: ela é a silhueta chapada,
 *   com o número e o nome em `texto-secundario`, sem tipo.
 * - **A espécie da coleção atrás da posição mantém a cor** (RN-38): o anel de 3dp em
 *   `destaque` e a etiqueta `Você` marcam só a posição atual.
 * - A grade e a escada vêm do mesmo estado: arrastar a escada e soltar rola a grade até a
 *   posição, e tocar num tier da escada troca o tier do nível 2.
 */
@Composable
fun PokedexMainScreen(
    aoAbrirAjustes: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val vm: PokedexViewModel = viewModel {
        PokedexViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.especiesRepositorio,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nomes by vm.nomes.collectAsStateWithLifecycle()
    val tipos by vm.tipos.collectAsStateWithLifecycle()
    val especies = container.especiesRepositorio

    (estado as? PokedexUiState.Conteudo)?.let { conteudo ->
        LaunchedEffect(conteudo.celulas) {
            vm.carregarEspecies(
                numeros = conteudo.celulas.map { it.numero },
                comTipo = conteudo.celulas.filter { it.estado != EstadoDaCelula.NAO_ALCANCADA }.map { it.numero }.toSet(),
            )
        }
    }

    PokedexMainScreen(
        estado = estado,
        nomes = nomes,
        tipos = tipos,
        sprite = especies::spriteDeJogo,
        aoAbrirAjustes = aoAbrirAjustes,
        aoAbrirEspecie = aoAbrirEspecie,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
internal fun PokedexMainScreen(
    estado: PokedexUiState,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    sprite: (Int) -> String,
    aoAbrirAjustes: () -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grade = rememberLazyGridState()
    val escopo = rememberCoroutineScope()
    // Três colunas é o desenho. Acima de `ESCALA_QUE_EMPILHA` o nome e o chip de tipo não
    // cabem em ~100dp e quebravam no meio da palavra: a grade passa a duas (achado de
    // `fontScale` 2,0 em 15/10, decisão nº 138).
    val colunas = if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) COLUNAS_COM_FONTE_GRANDE else COLUNAS
    var tierEscolhido by rememberSaveable { mutableIntStateOf(0) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(colunas),
            state = grade,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
            horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // O cabeçalho da Pokédex é um dos dois lugares do padrão de Pokébolas
                // (docs/02 §4.4). Ele acaba no filete, que fecha o cabeçalho.
                CabecalhoDeAba(
                    aba = stringResource(R.string.destino_pokedex),
                    titulo = stringResource(R.string.pokedex_titulo),
                    aoAbrirAjustes = aoAbrirAjustes,
                    modifier = Modifier.padraoDePokebolas(LocalCoresPokerun.current.padraoPokebola),
                )
            }

            when (estado) {
                PokedexUiState.Carregando -> Unit
                PokedexUiState.SemTemporada -> mensagem(R.string.pokedex_sem_temporada)
                PokedexUiState.Falhou -> falhou(aoTentarDeNovo)
                is PokedexUiState.Conteudo -> conteudo(
                    estado = estado,
                    nomes = nomes,
                    tipos = tipos,
                    sprite = sprite,
                    tierExibido = tierEscolhido.takeIf { it > 0 } ?: estado.escada.tierAtual,
                    aoEscolherTier = { tierEscolhido = it },
                    aoSoltarEm = { posicao ->
                        escopo.launch { grade.animateScrollToItem(ITENS_ANTES_DA_GRADE + indiceNaGrade(estado, posicao)) }
                    },
                    aoAbrirEspecie = aoAbrirEspecie,
                )
            }
        }
    }
}

/**
 * O tipo de cada item da grade. Sem ele, o cabeçalho de tier e a célula dividem o mesmo
 * pool de reuso, e a grade recria a célula do zero em vez de reaproveitar outra no scroll.
 */
private const val TIPO_TIER = "tier"
private const val TIPO_ESPECIE = "especie"

private const val COLUNAS = 3
private const val COLUNAS_COM_FONTE_GRANDE = 2

/**
 * O índice de uma posição entre os itens da grade, que intercalam um cabeçalho por tier
 * antes das espécies dele.
 */
private fun indiceNaGrade(estado: PokedexUiState.Conteudo, posicao: Int): Int {
    val celula = estado.celulas.getOrNull(posicao - 1) ?: return 0
    val tiersAntes = estado.celulas.take(posicao - 1).map { it.tier }.distinct().size
    val cabecalhoDoProprio = if (estado.celulas.take(posicao - 1).none { it.tier == celula.tier }) 1 else 0
    return (posicao - 1) + tiersAntes + cabecalhoDoProprio
}

/** O vazio sem temporada publicada, sobre o padrão de Pokébolas (docs/02 §4.2). */
private fun LazyGridScope.mensagem(texto: Int) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        EstadoVazio(modifier = Modifier.padding(Espacos.margem)) {
            Text(
                text = stringResource(texto),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun LazyGridScope.falhou(aoTentarDeNovo: () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(modifier = Modifier.padding(Espacos.margem)) {
            Text(
                text = stringResource(R.string.pokedex_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Espacos.bloco))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

private fun LazyGridScope.conteudo(
    estado: PokedexUiState.Conteudo,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    sprite: (Int) -> String,
    tierExibido: Int,
    aoEscolherTier: (Int) -> Unit,
    aoSoltarEm: (Int) -> Unit,
    aoAbrirEspecie: (Int) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(modifier = Modifier.padding(horizontal = Espacos.margem)) {
            Spacer(Modifier.height(Espacos.relacionado))
            Escada(
                escada = estado.escada,
                variante = VarianteDaEscada.COMPLETA,
                tierExibido = tierExibido,
                aoEscolherTier = aoEscolherTier,
                nomeDaEspecie = { nomes[it] },
                aoSoltarEm = aoSoltarEm,
            )
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        PainelDeTresLinhas(
            painel = estado.painel,
            nomes = nomes,
            modifier = Modifier.padding(horizontal = Espacos.margem, vertical = Espacos.bloco),
        )
    }

    var tierDoCabecalho = -1
    estado.celulas.forEach { celula ->
        if (celula.tier != tierDoCabecalho) {
            tierDoCabecalho = celula.tier
            item(key = "tier-${celula.tier}", span = { GridItemSpan(maxLineSpan) }, contentType = TIPO_TIER) {
                Text(
                    text = stringResource(R.string.escada_tier_nome, celula.tier, celula.nomeDoTier).uppercase(LocaleDoApp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.relacionado),
                )
            }
        }
        item(key = "especie-${celula.numero}", contentType = TIPO_ESPECIE) {
            Box(modifier = Modifier.padding(horizontal = Espacos.relacionado / 2)) {
                ItemDaPokedex(
                    celula = celula,
                    nome = nomes[celula.numero],
                    tipos = tipos[celula.numero].orEmpty(),
                    sprite = sprite(celula.numero),
                    aoTocar = { aoAbrirEspecie(celula.numero) },
                )
            }
        }
    }
}

/**
 * O painel de docs/03 §3.18, em três linhas. Com recuo, a linguagem de `04 §5.3`:
 * *"Você já foi mais rápido que 120 Pokémon. Sua forma atual: 107."* e *"Para retomar"*.
 * A linha de prazo só existe com 4 semanas de histórico.
 */
@Composable
private fun PainelDeTresLinhas(painel: PainelDaPokedex, nomes: Map<Int, String>, modifier: Modifier = Modifier) {
    @Composable fun nome(numero: Int?): String? = numero?.let { nomes[it] ?: stringResource(R.string.especie_numero, it) }
    val xp = String.format(LocaleDoApp, "%,d", painel.xp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        Text(
            text = stringResource(
                if (painel.recuou) R.string.pokedex_ja_foi else R.string.pokedex_ja_e,
                painel.ultrapassados,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (painel.recuou) {
                stringResource(R.string.pokedex_forma_atual_xp, painel.posicao, xp)
            } else {
                stringResource(R.string.pokedex_xp, xp)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Espacos.minimo))

        painel.superadoNaSemana?.let { LinhaDoPainel(stringResource(R.string.pokedex_superado), nome(it).orEmpty()) }
        painel.proximoAlvo?.let { alvo ->
            val faltam = painel.faltamXp?.let { String.format(LocaleDoApp, "%,d", it) }
            val prazo = painel.prazoEmDias?.let { pluralStringResource(R.plurals.pokedex_prazo, it, it) }
            LinhaDoPainel(
                rotulo = stringResource(if (painel.retomar) R.string.pokedex_retomar else R.string.pokedex_proximo_alvo),
                valor = nome(alvo).orEmpty(),
                detalhe = listOfNotNull(
                    faltam?.let { stringResource(R.string.pokedex_faltam_xp, it) },
                    prazo,
                ).joinToString(" · ").ifEmpty { null },
            )
        }
        painel.aoCumprirAMeta?.let { LinhaDoPainel(stringResource(R.string.pokedex_meta), nome(it).orEmpty()) }
    }
}

@Composable
private fun LinhaDoPainel(rotulo: String, valor: String, detalhe: String? = null) {
    val descricao = listOfNotNull(rotulo, valor, detalhe).joinToString(", ")
    val empilhar = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

    if (empilhar) {
        Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descricao }) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descricao },
        horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f),
        )
        Column(modifier = Modifier.weight(0.62f)) {
            Text(valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * O item da Pokédex (docs/02 §4.4, docs/03 §3.18): sprite de jogo sobre `tipo-*-suave`,
 * marcador de captura, número e nome. A célula inteira é o alvo.
 *
 * - **Alcançada e atual:** cor do tipo, chips de tipo e marcador de captura. A atual leva
 *   anel de 3dp em `destaque` e a etiqueta `Você`.
 * - **Não alcançada:** silhueta chapada (`ColorFilter.tint(silhueta, SrcIn)`, nunca
 *   opacidade) sobre `superficie-afundada`, **sem tipo**, com número e nome em
 *   `texto-secundario`.
 *
 * A cor de tipo é reforço: o nome do tipo vem escrito no chip e no `contentDescription`.
 */
@Composable
internal fun ItemDaPokedex(
    celula: CelulaDaPokedex,
    nome: String?,
    tipos: List<TipoDePokemon>,
    sprite: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = LocalCoresPokerun.current
    val alcancada = celula.estado != EstadoDaCelula.NAO_ALCANCADA
    val atual = celula.estado == EstadoDaCelula.ATUAL
    val fundo = if (alcancada) tipos.firstOrNull()?.let { LocalCoresDeTipo.current[it].suave } ?: cores.superficieAfundada else cores.superficieAfundada
    val tinta = if (alcancada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    val numero = stringResource(R.string.especie_numero, celula.numero)
    val nomesDosTipos = tipos.map { stringResource(nomeDoTipo(it)) }
    val descricao = buildString {
        append(nome ?: numero)
        append(", ").append(numero)
        if (alcancada && nomesDosTipos.isNotEmpty()) append(", ").append(nomesDosTipos.joinToString(" e "))
        append(", ")
        append(
            stringResource(
                when (celula.estado) {
                    EstadoDaCelula.ATUAL -> R.string.pokedex_item_atual
                    EstadoDaCelula.ALCANCADA -> R.string.pokedex_item_alcancada
                    EstadoDaCelula.NAO_ALCANCADA -> R.string.pokedex_item_nao_alcancada
                },
            ),
        )
    }

    Ficha(
        aoTocar = aoTocar,
        modifier = modifier
            .then(if (atual) Modifier.border(AnelDaPosicao, MaterialTheme.colorScheme.primary, FormaCard) else Modifier)
            .clearAndSetSemantics { contentDescription = descricao },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = Espacos.alvoToque).padding(Espacos.relacionado),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacos.minimo),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().background(fundo, FormaCard).padding(vertical = Espacos.minimo),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = sprite,
                    contentDescription = null,
                    modifier = Modifier.size(Tamanhos.spriteLista),
                    filterQuality = FilterQuality.None,
                    colorFilter = if (alcancada) null else ColorFilter.tint(cores.silhueta, BlendMode.SrcIn),
                )
                if (alcancada) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) { MarcadorDeCaptura() }
                }
            }
            if (atual) {
                Text(
                    text = stringResource(R.string.pokedex_voce),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = numero,
                style = MaterialTheme.typography.labelSmall,
                color = tinta,
                textAlign = TextAlign.Center,
            )
            Text(
                text = nome ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = tinta,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // A área dos chips tem a altura de dois, tenha a espécie um tipo, dois ou nenhum
            // (silhueta): sem isso as células da mesma linha da grade ficam de alturas
            // diferentes (achado da captura de 15/10).
            Column(
                modifier = Modifier.heightIn(min = AlturaDaAreaDeTipos),
                verticalArrangement = Arrangement.spacedBy(Espacos.minimo),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (alcancada) tipos.forEach { ChipDeTipo(it) }
            }
        }
    }
}

/** A Pokébola de 20dp em tinta no canto do item alcançado (docs/02 §4.4). Sem animação aqui. */
@Composable
private fun MarcadorDeCaptura() {
    val tinta = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.padding(Espacos.minimo).size(LadoDoMarcador).clearAndSetSemantics { }) {
        val traco = 1.75.dp.toPx()
        val raio = size.minDimension / 2 - traco / 2
        drawCircle(tinta, radius = raio, style = Stroke(width = traco))
        drawLine(tinta, Offset(0f + traco, size.height / 2), Offset(size.width - traco, size.height / 2), traco)
        drawCircle(tinta, radius = raio / 3, center = center, style = Stroke(width = traco))
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
    totalEspecies = 12,
    tiers = listOf(
        Tier(1, "Passo", "", listOf(10, 11, 13, 14, 16, 17), 100),
        Tier(2, "Trote", "", listOf(19, 20, 21, 22, 23, 24), 250),
    ),
)

private fun conteudoDeExemplo(xp: Long, colecao: Int): PokedexUiState.Conteudo {
    val posicao = com.hggabriel.pokerun.dominio.regras.posicaoNaEscada(xp, TemporadaDoPreview)
    return PokedexUiState.Conteudo(
        temporada = TemporadaDoPreview,
        escada = escadaDe(TemporadaDoPreview, posicao, colecao),
        painel = painelDaPokedex(TemporadaDoPreview, xp, colecao, emptyList(), xpDaMeta = 300),
        celulas = gradeDaPokedex(TemporadaDoPreview, posicao, colecao, xp),
    )
}

@Composable
private fun PokedexPreview(estado: PokedexUiState) {
    PokerunTheme {
        PokedexMainScreen(
            estado = estado,
            nomes = (1..30).associateWith { "Especie $it" },
            tipos = (1..30).associateWith { listOf(TipoDePokemon.entries[it % TipoDePokemon.entries.size]) },
            sprite = { "" },
            aoAbrirAjustes = {},
            aoAbrirEspecie = {},
            aoTentarDeNovo = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PokedexNaPosicaoPreview() = PokedexPreview(conteudoDeExemplo(xp = 650, colecao = 5))

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PokedexComRecuoPreview() = PokedexPreview(conteudoDeExemplo(xp = 400, colecao = 9))

@Preview(showBackground = true, widthDp = 320, heightDp = 900, fontScale = 2.0f)
@Composable
private fun PokedexFonteGrandePreview() = PokedexPreview(conteudoDeExemplo(xp = 650, colecao = 5))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PokedexFalhouPreview() = PokedexPreview(PokedexUiState.Falhou)
