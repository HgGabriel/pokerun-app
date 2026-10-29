package com.hggabriel.pokerun.ui.telas.detalhecorrida

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.esqueleto
import com.hggabriel.pokerun.ui.componentes.fonteAmpliada
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarPace
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.componentes.nomeDoDia
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.telas.corridamanual.nome
import com.hggabriel.pokerun.ui.telas.corridamanual.palavraDoEsforco
import com.hggabriel.pokerun.ui.telas.pokedex.MiniFigurinha
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Goteira = 16.dp
private val AlvoDeToque = 48.dp
private val EntreCartoes = 12.dp
private val FolgaDoFim = 32.dp

/** O espaço acima do placar, onde sobem a cabeça do Pokémon e a medalha (esboço aprovado). */
private val FolgaDoPlacar = 64.dp
private val LadoDoPokemon = 196.dp
private val SobraDoPokemon = 76.dp
private val VazamentoDoPokemon = 4.dp
private val CantoDaMedalhaX = (-8).dp
private val CantoDaMedalhaY = (-48).dp
/** Dentro do cartão, abaixo da medalha: as fitas não encostam no `TEMPO`. */
private val EspacoDaMedalha = 70.dp
/** O tempo e a distância ficam à esquerda do Pokémon. */
private val LarguraDoPar = 190.dp
private val RaioDaFaixa = 18.dp
private val RaioDoTrecho = 16.dp
private val AlturaDasBarras = 44.dp
private val BarraMinima = 12.dp
private val LadoDoSelo = 44.dp

/** O deslocamento de uma minifigurinha para a seguinte no maço. */
private val PassoDoMaco = 24.dp
private const val FIGURINHAS_NO_MACO = 5

/**
 * O pace é o número grande do placar (§12, 29/10/2026): tempo ÷ distância = pace. 84sp,
 * entre o 88 da arena e o `numero-xl`; o pace de cinco caracteres (`10:30`) desce a 68 para
 * caber com `/km` na largura do cartão.
 */
private val EstiloDoPace = Estilos.numeroPeito.copy(fontSize = 84.sp, lineHeight = 80.sp, letterSpacing = (-0.035).em)
private val EstiloDoPaceLongo = EstiloDoPace.copy(fontSize = 68.sp, lineHeight = 68.sp)

private val Horario = DateTimeFormatter.ofPattern("HH:mm")

/**
 * A `RunDetailScreen` (`F2-T09`, docs/03 §3.13), na segunda linguagem (29/10/2026).
 *
 * Página tingida. No topo, o placar: um cartão branco com o Pokémon que a corrida deixou de pé
 * na borda, a medalha do ritmo presa no canto, tempo e distância em cima, o fio, o pace grande
 * embaixo e a faixa de XP em tinta com as figurinhas coladas. Abaixo, um cartão por pergunta:
 * a sessão, como foi, a conta do XP e os melhores trechos.
 *
 * Mora dentro da aba que a abriu (docs/03 §1). O lápis leva à `RunEditScreen`, na pilha modal.
 */
@Composable
fun DetalheCorridaScreen(
    corridaId: String,
    aoCorrigir: (String) -> Unit,
    modifier: Modifier = Modifier,
    aoAbrirMedalhas: (Ritmo) -> Unit = {},
    vm: DetalheCorridaViewModel = detalheCorridaViewModel(corridaId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val tipos by vm.tipos.collectAsStateWithLifecycle()

    DetalheCorridaScreen(
        estado = estado,
        tipos = tipos,
        aoCorrigir = aoCorrigir,
        aoAbrirMedalhas = aoAbrirMedalhas,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun DetalheCorridaScreen(
    estado: DetalheCorridaUiState,
    aoCorrigir: (String) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    tipos: Map<Int, List<TipoDePokemon>> = emptyMap(),
    aoAbrirMedalhas: (Ritmo) -> Unit = {},
) {
    val conteudo = estado as? DetalheCorridaUiState.Conteudo
    // Guardada aqui, fora da lista: rolar o placar para fora e de volta não repete a medalha.
    val reduzido = LocalMovimentoReduzido.current
    val medalha = remember(reduzido) { EstadoDaMedalha(reduzido) }

    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(conteudo?.nomeDoPlano ?: stringResource(R.string.corrida_detalhe_sobrancelha)),
                    titulo = conteudo?.let { textoDoDia(it) }.orEmpty(),
                    acao = conteudo?.let { { BotaoCorrigir(aoCorrigir = { aoCorrigir(it.corridaId) }) } },
                )
            }

            when (estado) {
                DetalheCorridaUiState.Carregando -> esqueleto()
                DetalheCorridaUiState.Falhou -> falhou(aoTentarDeNovo)
                is DetalheCorridaUiState.Conteudo -> conteudo(estado, tipos, medalha, aoAbrirMedalhas)
            }
        }
    }
}

private fun LazyListScope.conteudo(
    estado: DetalheCorridaUiState.Conteudo,
    tipos: Map<Int, List<TipoDePokemon>>,
    medalha: EstadoDaMedalha,
    aoAbrirMedalhas: (Ritmo) -> Unit,
) {
    item(key = "placar") { Placar(estado, tipos, medalha, aoAbrirMedalhas) }
    item(key = "cartoes") {
        Column(
            modifier = Modifier.padding(start = Goteira, end = Goteira, top = EntreCartoes),
            verticalArrangement = Arrangement.spacedBy(EntreCartoes),
        ) {
            if (estado.semanaRef != null || estado.sessao != null) CartaoDaSessao(estado)
            if (estado.esforco != null || estado.fcMedia != null) CartaoComoFoi(estado.esforco, estado.fcMedia)
            if (estado.conta != null && !estado.foraDosCalculos) CartaoDaConta(estado.conta, estado.xpCreditado)
            if (estado.tempo1kSeg != null || estado.tempo5kSeg != null) CartaoDosTrechos(estado)
        }
    }
}

// ---------------------------------------------------------------------------
// O placar
// ---------------------------------------------------------------------------

/**
 * O cartão do placar, com o Pokémon da corrida de pé na borda de cima e a medalha do ritmo
 * presa no canto oposto. A luz do tipo do Pokémon fica atrás dele, dentro do cartão.
 */
@Composable
private fun Placar(
    estado: DetalheCorridaUiState.Conteudo,
    tipos: Map<Int, List<TipoDePokemon>>,
    medalha: EstadoDaMedalha,
    aoAbrirMedalhas: (Ritmo) -> Unit,
) {
    val ritmo = estado.ritmo
    val luz = estado.especie?.let { tipos[it]?.firstOrNull() }?.let { LocalCoresDeTipo.current[it].base }

    if (ritmo != null) {
        LaunchedEffect(medalha, ritmo) { medalha.tocar(coreografiaDaMedalha(ritmo)) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Goteira, end = Goteira, top = FolgaDoPlacar)
            .graphicsLayer { translationX = medalha.tremor.value.dp.toPx() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FormaDoCartao)
                .background(MaterialTheme.colorScheme.surface)
                .drawBehind {
                    if (luz == null) return@drawBehind
                    val raio = 150.dp.toPx()
                    val centro = Offset(size.width - 70.dp.toPx(), 80.dp.toPx())
                    drawCircle(Brush.radialGradient(listOf(luz.copy(alpha = 0.5f), Color.Transparent), centro, raio), raio, centro)
                }
                .padding(start = Goteira, end = Goteira, top = 18.dp, bottom = Goteira),
        ) {
            if (ritmo != null) Spacer(Modifier.height(EspacoDaMedalha))
            ParDoPlacar(estado)
            if (estado.paceSeg != null && ritmo != null) {
                Spacer(Modifier.height(Goteira))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
                Spacer(Modifier.height(6.dp))
                PaceDoPlacar(estado, stringResource(ritmo.nome))
            }
            Spacer(Modifier.height(EntreCartoes))
            FaixaDoXp(estado, tipos)
        }

        estado.especie?.let { numero ->
            SpriteAnimado(
                numero = numero,
                paraNoReduzido = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = VazamentoDoPokemon, y = -SobraDoPokemon)
                    .size(LadoDoPokemon)
                    .graphicsLayer { translationY = medalha.salto.value.dp.toPx() },
            )
        }
        if (ritmo != null) {
            MedalhaDoRitmo(
                ritmo = ritmo,
                estado = medalha,
                aoTocar = { aoAbrirMedalhas(ritmo) },
                descricao = stringResource(R.string.medalhas_abrir),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = CantoDaMedalhaX, y = CantoDaMedalhaY),
            )
        }
    }
}

/** O tempo e a distância, um sobre o outro, à esquerda do Pokémon. */
@Composable
private fun ParDoPlacar(estado: DetalheCorridaUiState.Conteudo) {
    val duracao = formatarDuracao(estado.duracaoSeg)
    val inicio = estado.inicio.format(Horario)
    val fim = estado.fim.format(Horario)
    val (km, metros) = partesDaDistancia(estado.km)
    val unidadeKm = stringResource(R.string.corrida_detalhe_unidade_km)
    val unidadeM = stringResource(R.string.corrida_detalhe_unidade_metros)
    val distancia = listOfNotNull(
        "$km $unidadeKm".takeIf { km > 0 || metros == 0 },
        "$metros $unidadeM".takeIf { metros > 0 },
    ).joinToString(" ")
    val unidade = SpanStyle(
        fontFamily = Estilos.rotulo.fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.08.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val recordeFalado = stringResource(R.string.corrida_detalhe_recorde_falado, "%s")

    Column(
        modifier = Modifier.widthIn(max = LarguraDoPar),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val tempoFalado = stringResource(R.string.corrida_detalhe_tempo_falado, duracao, inicio, fim)
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = if (TipoDeRecorde.MAIOR_DURACAO in estado.recordes) recordeFalado.format(tempoFalado) else tempoFalado
            },
        ) {
            RotuloComRecorde(stringResource(R.string.corrida_detalhe_tempo), TipoDeRecorde.MAIOR_DURACAO in estado.recordes)
            Text(text = duracao, style = naFigura(Estilos.numeroL), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                text = stringResource(R.string.corrida_detalhe_horario, inicio, fim),
                style = naFigura(Estilos.apoio),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val distanciaFalada = stringResource(R.string.corrida_detalhe_distancia_falada, distancia)
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription =
                    if (TipoDeRecorde.MAIOR_DISTANCIA in estado.recordes) recordeFalado.format(distanciaFalada) else distanciaFalada
            },
        ) {
            RotuloComRecorde(stringResource(R.string.corrida_detalhe_distancia), TipoDeRecorde.MAIOR_DISTANCIA in estado.recordes)
            Text(
                text = buildAnnotatedString {
                    if (km > 0 || metros == 0) {
                        append(km.toString())
                        withStyle(unidade) { append(" " + unidadeKm.uppercase(LocaleDoApp) + " ") }
                    }
                    if (metros > 0) {
                        append(metros.toString())
                        withStyle(unidade) { append(" " + unidadeM.uppercase(LocaleDoApp)) }
                    }
                },
                style = naFigura(Estilos.numeroL),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** O pace grande sobre o fio, com `/km` na linha de base. */
@Composable
private fun PaceDoPlacar(estado: DetalheCorridaUiState.Conteudo, nomeDoRitmo: String) {
    val pace = formatarPace(estado.km, estado.duracaoSeg) ?: return
    val recorde = TipoDeRecorde.MELHOR_PACE in estado.recordes
    val falado = stringResource(R.string.manual_ritmo_descricao, pace, nomeDoRitmo)
    val recordeFalado = stringResource(R.string.corrida_detalhe_recorde_falado, falado)
    val estilo = when {
        fonteAmpliada() -> Estilos.numeroXl
        pace.length > 4 -> EstiloDoPaceLongo
        else -> EstiloDoPace
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = if (recorde) recordeFalado else falado },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = pace,
            style = naFigura(estilo),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = stringResource(R.string.corrida_detalhe_por_km).uppercase(LocaleDoApp),
            style = naFigura(Estilos.rotulo),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
        if (recorde) PilulaDeRecorde(Modifier.alignByBaseline())
    }
}

/**
 * A única peça em tinta da tela: o XP que a corrida rendeu e o maço das figurinhas que ela
 * colou. Corrida fora dos cálculos diz isso no lugar do XP; sem a conta do replay, diz que
 * ela ainda vem.
 */
@Composable
private fun FaixaDoXp(estado: DetalheCorridaUiState.Conteudo, tipos: Map<Int, List<TipoDePokemon>>) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val xp = String.format(LocaleDoApp, "%,d", estado.xpCreditado)
    val figurinhas = estado.figurinhas.size
    val textoDasFigurinhas = pluralStringResource(R.plurals.corrida_detalhe_figurinhas, figurinhas, figurinhas)
    val pendente = estado.conta == null && estado.xpCreditado == 0L
    val ganho = stringResource(R.string.corrida_detalhe_xp_ganho, xp)
    val unidadeXp = stringResource(R.string.corrida_detalhe_xp_unidade)
    val falado = when {
        estado.foraDosCalculos -> stringResource(R.string.corrida_detalhe_fora)
        pendente -> stringResource(R.string.corrida_detalhe_xp_pendente)
        figurinhas > 0 -> stringResource(R.string.corrida_detalhe_xp_falado, xp) + ", " + textoDasFigurinhas
        else -> stringResource(R.string.corrida_detalhe_xp_falado, xp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RaioDaFaixa))
            .background(tinta)
            .padding(start = Goteira, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .clearAndSetSemantics { contentDescription = falado },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when {
                estado.foraDosCalculos || pendente -> {
                    Text(
                        text = stringResource(R.string.corrida_detalhe_xp_rotulo).uppercase(LocaleDoApp),
                        style = naFigura(Estilos.rotulo),
                        color = claro.copy(alpha = 0.72f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = falado, style = naFigura(Estilos.apoio), color = claro)
                }
                else -> {
                    Text(
                        text = buildAnnotatedString {
                            append(ganho)
                            withStyle(
                                SpanStyle(
                                    fontFamily = Estilos.rotulo.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.08.em,
                                    color = claro.copy(alpha = 0.5f),
                                ),
                            ) { append(" $unidadeXp") }
                        },
                        style = naFigura(Estilos.numeroM.copy(fontSize = 28.sp, lineHeight = 30.sp)),
                        color = claro,
                        maxLines = 1,
                    )
                    if (figurinhas > 0) {
                        Text(
                            text = textoDasFigurinhas.uppercase(LocaleDoApp),
                            style = naFigura(Estilos.rotulo),
                            color = claro.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
        if (figurinhas > 0) Maco(estado.figurinhas.takeLast(FIGURINHAS_NO_MACO), tipos)
    }
}

/** O maço das figurinhas coladas, sobrepostas e inclinadas uma para cada lado. */
@Composable
private fun Maco(numeros: List<Int>, tipos: Map<Int, List<TipoDePokemon>>) {
    Box(modifier = Modifier.width(36.dp + PassoDoMaco * (numeros.size - 1)).height(48.dp)) {
        numeros.forEachIndexed { i, numero ->
            MiniFigurinha(
                numero = numero,
                tipo = tipos[numero]?.firstOrNull(),
                vaga = false,
                inclinacao = if (i % 2 == 0) -4f else 3f,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = PassoDoMaco * i),
            )
        }
    }
}

@Composable
private fun RotuloComRecorde(texto: String, recorde: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = texto.uppercase(LocaleDoApp),
            style = naFigura(Estilos.rotulo),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (recorde) PilulaDeRecorde()
    }
}

/** O recorde batido, numa pílula cheia em `destaque`: é seu. */
@Composable
private fun PilulaDeRecorde(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.corrida_detalhe_recorde).uppercase(LocaleDoApp),
        style = naFigura(Estilos.legenda),
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, FormaCirculo)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// ---------------------------------------------------------------------------
// Os cartões de baixo
// ---------------------------------------------------------------------------

/** A sessão que a corrida cumpriu, com o selo da semana, e a semana do plano. */
@Composable
private fun CartaoDaSessao(estado: DetalheCorridaUiState.Conteudo) {
    val semana = estado.semanaRef?.let { stringResource(R.string.corrida_detalhe_semana, it) }
        ?: stringResource(R.string.corrida_detalhe_fora_das_semanas)
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotuloDoCartao(stringResource(R.string.corrida_detalhe_sessao), Modifier.weight(1f))
            Text(text = semana, style = naFigura(Estilos.apoio), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        estado.sessao?.let { sessao ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloDaSessao(cumprida = sessao != SessaoReivindicada.Avulsa)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = nomeDaSessao(sessao),
                    style = Estilos.tituloSecao,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** O selo cheio dos carimbos da semana; a avulsa não ocupa sessão e fica como vaga. */
@Composable
private fun SeloDaSessao(cumprida: Boolean) {
    val claro = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(LadoDoSelo)
            .graphicsLayer { rotationZ = -8f }
            .clip(FormaCirculo)
            .background(if (cumprida) MaterialTheme.colorScheme.primary else LocalCoresPokerun.current.superficieAfundada)
            .padding(3.dp)
            .border(1.dp, if (cumprida) claro.copy(alpha = 0.45f) else Color.Transparent, FormaCirculo),
        contentAlignment = Alignment.Center,
    ) {
        if (cumprida) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun nomeDaSessao(sessao: SessaoReivindicada): String = when (sessao) {
    SessaoReivindicada.Longao -> stringResource(R.string.corrida_detalhe_sessao_longao)
    SessaoReivindicada.Avulsa -> stringResource(R.string.corrida_detalhe_sessao_avulsa)
    is SessaoReivindicada.Curta -> stringResource(R.string.corrida_detalhe_sessao_curta, sessao.indice)
}

/** O esforço em número, palavra e barras, e o coração batendo na FC média. */
@Composable
private fun CartaoComoFoi(esforco: Int?, fc: Int?) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.corrida_detalhe_como_foi))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (esforco != null) {
                val palavra = stringResource(palavraDoEsforco(esforco))
                val falado = stringResource(R.string.manual_esforco_nivel, esforco) + ", " + palavra
                Row(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = falado },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = esforco.toString(),
                        style = naFigura(Estilos.numeroM),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = palavra,
                        style = naFigura(Estilos.apoio),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (fc != null) {
                val falado = stringResource(R.string.corrida_detalhe_fc_falada, fc)
                Row(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = falado },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoracaoNoBpm(fc)
                    Text(text = fc.toString(), style = naFigura(Estilos.numeroM), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = stringResource(R.string.corrida_detalhe_unidade_bpm).uppercase(LocaleDoApp),
                        style = naFigura(Estilos.rotulo),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (esforco != null) {
            Spacer(Modifier.height(10.dp))
            BarrasDoEsforco(esforco)
        }
    }
}

/** As dez barras do registro, só de leitura: cheias até o nível, do verde ao vermelho. */
@Composable
private fun BarrasDoEsforco(nivel: Int) {
    val vazio = LocalCoresPokerun.current.superficieAfundada
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDasBarras),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 1..10) {
            val fracao = (i - 1) / 9f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(BarraMinima + (AlturaDasBarras - BarraMinima) * fracao)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (i <= nivel) corDoEsforco(fracao) else vazio),
            )
        }
    }
}

/** A mesma rampa do registro: o matiz desce do verde ao vermelho (docs/02 §1). */
private fun corDoEsforco(fracao: Float): Color =
    Color.hsv(hue = 128f * (1 - fracao), saturation = 0.72f + 0.12f * fracao, value = 0.82f + 0.1f * fracao)

/**
 * O coração batendo a `60.000 / bpm` ms, com duas batidas, como na arena: o loop é o próprio
 * dado (docs/02 §6). Com movimento reduzido, parado.
 */
@Composable
private fun CoracaoNoBpm(bpm: Int) {
    val vermelho = MaterialTheme.colorScheme.primary
    if (LocalMovimentoReduzido.current) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = vermelho, modifier = Modifier.size(28.dp))
        return
    }
    val periodo = (60_000 / bpm.coerceAtLeast(1)).coerceIn(260, 2_000)
    val escala by rememberInfiniteTransition(label = "coração").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = periodo
                1f at 0
                1.24f at (periodo * 0.12f).toInt() using FastOutSlowInEasing
                0.96f at (periodo * 0.28f).toInt()
                1.12f at (periodo * 0.4f).toInt()
                1f at (periodo * 0.58f).toInt()
            },
        ),
        label = "batida",
    )
    Icon(
        Icons.Filled.Favorite,
        contentDescription = null,
        tint = vermelho,
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            },
    )
}

/**
 * O XP com a conta aberta (docs/03 §3.13): as parcelas de `xp_detalhe`, na ordem da fórmula
 * de docs/04 §3. Bônus zerado não ganha linha.
 */
@Composable
private fun CartaoDaConta(conta: ParcelasDeXp, total: Long) {
    val locale = LocaleDoApp
    Cartao {
        RotuloDoCartao(stringResource(R.string.corrida_detalhe_conta))
        Spacer(Modifier.height(10.dp))
        LinhaDaConta(R.string.corrida_detalhe_xp_base, parcelaDaConta(conta.base, locale))
        if (conta.longao > 0) LinhaDaConta(R.string.corrida_detalhe_xp_longao, "+" + parcelaDaConta(conta.longao, locale))
        if (conta.pace > 0) LinhaDaConta(R.string.corrida_detalhe_xp_pace, "+" + inteiro(conta.pace.toDouble(), locale))
        LinhaDaConta(R.string.corrida_detalhe_xp_multiplicador, "×" + String.format(locale, "%.1f", conta.multiplicador))
        if (conta.recorde > 0) LinhaDaConta(R.string.corrida_detalhe_xp_recorde, "+" + inteiro(conta.recorde.toDouble(), locale))

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val rotulo = stringResource(R.string.corrida_detalhe_xp_total)
        val valor = stringResource(R.string.corrida_detalhe_xp_total_valor, String.format(locale, "%,d", total))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clearAndSetSemantics { contentDescription = "$rotulo $valor" },
        ) {
            Text(
                text = rotulo.uppercase(LocaleDoApp),
                style = naFigura(Estilos.rotulo),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
            Text(
                text = valor,
                style = naFigura(Estilos.numeroM),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun LinhaDaConta(rotulo: Int, valor: String) {
    val texto = stringResource(rotulo)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = "$texto $valor" },
    ) {
        Text(
            text = texto,
            style = Estilos.apoio,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
        Text(
            text = valor,
            style = Estilos.numeroS,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** Os melhores 1 km e 5 km digitados, cada um num bloco, com o recorde quando bateu. */
@Composable
private fun CartaoDosTrechos(estado: DetalheCorridaUiState.Conteudo) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.corrida_detalhe_trechos))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            var blocos = 0
            estado.tempo1kSeg?.let {
                Trecho(stringResource(R.string.corrida_detalhe_trecho_1k), it, TipoDeRecorde.MELHOR_1K in estado.recordes, Modifier.weight(1f))
                blocos++
            }
            estado.tempo5kSeg?.let {
                Trecho(stringResource(R.string.corrida_detalhe_trecho_5k), it, TipoDeRecorde.MELHOR_5K in estado.recordes, Modifier.weight(1f))
                blocos++
            }
            if (blocos == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Trecho(rotulo: String, segundos: Long, recorde: Boolean, modifier: Modifier) {
    val tempo = formatarDuracao(segundos)
    val falado = "$rotulo $tempo"
    val recordeFalado = stringResource(R.string.corrida_detalhe_recorde_falado, falado)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RaioDoTrecho))
            .background(LocalCoresPokerun.current.superficieAfundada)
            .padding(12.dp)
            .clearAndSetSemantics { contentDescription = if (recorde) recordeFalado else falado },
    ) {
        RotuloComRecorde(rotulo, recorde)
        Text(text = tempo, style = naFigura(Estilos.numeroM), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// O cabeçalho
// ---------------------------------------------------------------------------

/**
 * `Corrigir` como lápis no slot de ação do cabeçalho: é a única ação da tela, e não precisa
 * de menu. O alvo é um `Box` clicável de 48dp com `Role.Button` (`F1-T07b`).
 */
@Composable
private fun BotaoCorrigir(aoCorrigir: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AlvoDeToque)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = aoCorrigir,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.corrida_detalhe_corrigir),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** `Domingo, 13 de setembro`, com o ano quando não é o de hoje. */
@Composable
private fun textoDoDia(estado: DetalheCorridaUiState.Conteudo): String {
    val dia = estado.dia
    val semana = nomeDoDia(dia.dayOfWeek, LocaleDoApp).replaceFirstChar { it.titlecase(LocaleDoApp) }
    val mes = nomeDoMes(dia, LocaleDoApp)
    return if (estado.mostraAno) {
        stringResource(R.string.corrida_detalhe_titulo_dia_ano, semana, dia.dayOfMonth, mes, dia.year)
    } else {
        stringResource(R.string.corrida_detalhe_titulo_dia, semana, dia.dayOfMonth, mes)
    }
}

private fun inteiro(valor: Double, locale: Locale): String = String.format(locale, "%,d", Math.round(valor))

/**
 * Uma parcela `Double` da conta do XP (base e bônus de longão), com a casa decimal que
 * ela tem.
 *
 * **A conta aberta tem de somar na tela** (docs/02 §9.2, item 5). O motor arredonda só o
 * total, uma vez (`DetalheDeXp`), e o bônus de longão é `km × 50` a partir de 60% do
 * alvo: sai 232,5 com frequência. Arredondada a 233, a parcela fazia `1.000 + 233` vezes
 * `2,0` dar 2.466 contra o total de 2.465 (corrida de 10 km, aparelho, 17/10/2026). A
 * regra não muda; muda a forma de mostrar o número que ela já calcula.
 *
 * Parcela a menos de 0,05 de um inteiro sai inteira: `4,6 × 100` não é 460 em ponto
 * flutuante, e a tela não mostra `460,0`.
 */
internal fun parcelaDaConta(valor: Double, locale: Locale): String {
    val arredondado = Math.round(valor)
    return if (kotlin.math.abs(valor - arredondado) < 0.05) {
        String.format(locale, "%,d", arredondado)
    } else {
        String.format(locale, "%,.1f", valor)
    }
}

// ---------------------------------------------------------------------------
// Os estados sem conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(start = Goteira, end = Goteira, top = 24.dp)) {
            Text(
                text = stringResource(R.string.corrida_detalhe_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

/** O esqueleto na forma do placar e de um cartão, que pulsa (docs/02 §8, item 6). */
private fun LazyListScope.esqueleto() {
    item {
        Column(
            modifier = Modifier.padding(start = Goteira, end = Goteira, top = FolgaDoPlacar),
            verticalArrangement = Arrangement.spacedBy(EntreCartoes),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .esqueleto(FormaDoCartao),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .esqueleto(FormaDoCartao),
            )
        }
    }
}

/** A chave carrega o ID: sem ela, abrir a segunda corrida devolveria o `ViewModel` da primeira. */
@Composable
private fun detalheCorridaViewModel(corridaId: String): DetalheCorridaViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "corrida-$corridaId") {
        DetalheCorridaViewModel(
            corridaId,
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.especiesRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val CORRIDA_DE_EXEMPLO = DetalheCorridaUiState.Conteudo(
    corridaId = "run-1",
    dia = LocalDate.of(2026, 9, 13),
    mostraAno = false,
    inicio = LocalTime.of(6, 40),
    fim = LocalTime.of(7, 44),
    km = 12.4,
    duracaoSeg = 3862,
    paceSeg = 311,
    fcMedia = 148,
    esforco = 7,
    tempo1kSeg = 288,
    tempo5kSeg = 1502,
    nomeDoPlano = "São Silvestre",
    semanaRef = 6,
    sessao = SessaoReivindicada.Longao,
    xpCreditado = 3_550,
    conta = ParcelasDeXp(base = 1240.0, longao = 260.0, pace = 200, multiplicador = 2.0, recorde = 500),
    recordes = setOf(TipoDeRecorde.MELHOR_5K),
    foraDosCalculos = false,
    especie = 26,
    figurinhas = listOf(12, 20, 24, 26),
)

@Composable
private fun CorridaPreview(estado: DetalheCorridaUiState) {
    PokerunTheme {
        DetalheCorridaScreen(
            estado = estado,
            aoCorrigir = {},
            aoTentarDeNovo = {},
            tipos = mapOf(26 to listOf(TipoDePokemon.ELETRICO)),
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun CompletaPreview() = CorridaPreview(CORRIDA_DE_EXEMPLO)

@Preview(showBackground = true)
@Composable
private fun SemContaPreview() = CorridaPreview(
    CORRIDA_DE_EXEMPLO.copy(fcMedia = null, esforco = null, conta = null, xpCreditado = 0, figurinhas = emptyList()),
)

@Preview(showBackground = true, fontScale = 2.0f, widthDp = 320, heightDp = 1600)
@Composable
private fun FonteGrandePreview() = CorridaPreview(CORRIDA_DE_EXEMPLO)

@Preview(showBackground = true)
@Composable
private fun FalhouPreview() = CorridaPreview(DetalheCorridaUiState.Falhou)
