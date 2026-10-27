package com.hggabriel.pokerun.ui.telas.pokedex

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.FichaDaEspecie
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.IconeDeTipo
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.navegacao.chaveDaFigurinha
import com.hggabriel.pokerun.ui.navegacao.vooCompartilhado
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.CorDeTipo
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * Uma página do detalhe: a espécie como o álbum a vê, com o que a PokéAPI já devolveu.
 *
 * @property kmFalta o `~790 km` da não alcançada, ou nulo sem corrida.
 * @property fracao quanto do XP dela já foi feito, para a barra do verso trancado.
 */
@Immutable
internal data class PaginaDaEspecie(
    val numero: Int,
    val posicao: Int,
    val total: Int,
    val colada: Boolean,
    val tier: Int,
    val nomeDoTier: String,
    val nome: String?,
    val tipos: List<TipoDePokemon>,
    val coladaEm: Corrida?,
    val kmFalta: Double?,
    val fracao: Float,
    val ficha: FichaDaEspecie?,
    val evolucao: List<List<Int>>,
)

private val LarguraDaAmpliada = 212.dp
private val AlturaDaAmpliada = 236.dp
private val BordaDaGrande = 6.dp

/** O 3D fica dentro da figurinha, abaixo do número: o GIF do Let's Go enche o quadro. */
private val TopoDoTresD = 24.dp

/** A vizinha é a própria figurinha grande reduzida a 96dp de largura. */
private const val EscalaDaVizinha = 96f / 212f

/** O centro da vizinha fica 18dp fora da borda da tela e 20dp acima do centro da grande. */
private val ForaDaBorda = 18.dp
private val SubidaDaVizinha = 20.dp

/** O passo da figurinha que entra de fora da tela até o lugar da vizinha. */
private val PassoDeFora = 120.dp
private val DeslizeDoNome = 96.dp

/** O ícone de tipo do detalhe tem a altura da etiqueta do tier. */
private val LadoDoIconeDeTipo = 28.dp

/** O lance mais lento que ainda passa de página ao soltar o arraste. */
private val VelocidadeDoLance = 400.dp

/** Quanto da página o arraste lento anda para passar à seguinte: 15%, na frente e no verso. */
internal const val LimiarDoArraste = 0.15f

/** A posição do arraste, em páginas: 3,4 é a página 3 com 40% da 4 à vista. */
private fun PagerState.posicao(): Float =
    (currentPage + currentPageOffsetFraction).coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())

/**
 * O fundo da frente, para ler no desenho: o `tipo-*-suave` da página passa ao da vizinha
 * no arraste. A barra de status lê o mesmo, para seguir a frente sem emenda.
 */
@Composable
internal fun fundoDaFrente(pager: PagerState, pagina: (Int) -> PaginaDaEspecie): () -> Color {
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val coresDeTipo = LocalCoresDeTipo.current
    val fundoDe = { i: Int ->
        val p = pagina(i)
        p.tipos.firstOrNull()?.takeIf { p.colada }?.let { coresDeTipo[it].suave } ?: afundada
    }
    return {
        val p = pager.posicao()
        val a = floor(p).toInt()
        lerp(fundoDe(a), fundoDe(ceil(p).toInt()), p - a)
    }
}

/**
 * A frente da figurinha ampliada (docs/03 §3.19): de borda a borda em `tipo-*-suave`, com a
 * seta de voltar, a posição, o palco com as figurinhas, o nome e as etiquetas.
 *
 * A frente fica parada, e o arraste lateral move só as figurinhas: a que vem cresce até o
 * centro, e a que sai diminui até o lugar da vizinha. O fundo passa de uma cor à outra, e o
 * nome troca com um esmaecer. O mesmo [pager] move o verso, que desliza inteiro.
 *
 * Tocar no Pokémon toca o grito. Tocar numa vizinha vai até ela. A não alcançada é a vaga
 * grande, com a silhueta chapada, sem tipo e sem grito.
 *
 * @param pagina a página da espécie em cada índice do [pager].
 * @param inicial a espécie que abriu a tela, que voa da lista até o centro.
 */
@Composable
internal fun FrenteDaEspecie(
    pager: PagerState,
    pagina: (Int) -> PaginaDaEspecie,
    aoVoltar: () -> Unit,
    aoIrPara: (Int) -> Unit,
    aoTocarGrito: (Int) -> Unit,
    modifier: Modifier = Modifier,
    inicial: Int? = null,
) {
    val cores = LocalCoresPokerun.current
    val coresDeTipo = LocalCoresDeTipo.current
    val corDe: (PaginaDaEspecie) -> CorDeTipo? = { p -> p.tipos.firstOrNull()?.takeIf { p.colada }?.let { coresDeTipo[it] } }
    val fundo = fundoDaFrente(pager, pagina)
    val atual = pager.currentPage
    // No palco: a aberta, as duas vizinhas e, no arraste, a que entra de fora da tela.
    val noPalco by remember(pager) {
        derivedStateOf {
            val p = pager.posicao()
            (floor(p).toInt() - 1).coerceAtLeast(0)..(ceil(p).toInt() + 1).coerceAtMost(pager.pageCount - 1)
        }
    }
    val nosRotulos by remember(pager) { derivedStateOf { val p = pager.posicao(); floor(p).toInt()..ceil(p).toInt() } }
    var largura by remember { mutableIntStateOf(0) }
    val escopo = rememberCoroutineScope()
    val limiteDoLance = with(LocalDensity.current) { VelocidadeDoLance.toPx() }
    // [inicio] é a página em que o arraste começou: o limiar conta a partir dela.
    val soltar: (Float, Int) -> Unit = { velocidade, inicio ->
        val p = pager.posicao()
        val alvo = when {
            velocidade < -limiteDoLance -> floor(p).toInt() + 1
            velocidade > limiteDoLance -> ceil(p).toInt() - 1
            p >= inicio -> floor(p - LimiarDoArraste).toInt() + 1
            else -> ceil(p + LimiarDoArraste).toInt() - 1
        }.coerceIn(0, pager.pageCount - 1)
        escopo.launch { pager.animateScrollToPage(alvo) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // O arraste na frente move o mesmo pager do verso. Ao soltar, o lance rápido vai à
            // página seguinte no sentido dele; o lento passa dela com 15% da página andados.
            .pointerInput(pager) {
                val rastro = VelocityTracker()
                var inicio = 0
                detectHorizontalDragGestures(
                    onDragStart = {
                        rastro.resetTracking()
                        inicio = round(pager.posicao()).toInt()
                        escopo.launch { pager.stopScroll() }
                    },
                    onDragEnd = { soltar(rastro.calculateVelocity().x, inicio) },
                    onDragCancel = { soltar(0f, inicio) },
                ) { mudanca, delta ->
                    rastro.addPosition(mudanca.uptimeMillis, mudanca.position)
                    mudanca.consume()
                    pager.dispatchRawDelta(-delta)
                }
            }
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .drawBehind { drawRect(fundo()) }
            .padding(bottom = 22.dp),
    ) {
        val aberta = pagina(atual)
        Row(
            modifier = Modifier.fillMaxWidth().height(Espacos.alvoToque).padding(horizontal = Espacos.relacionado),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.editar_voltar), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                stringResource(R.string.pokedex_detalhe_topo, aberta.posicao, aberta.total).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // O palco: cada figurinha se põe pela distância até o centro, quadro a quadro.
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).onSizeChanged { largura = it.width }.padding(bottom = 12.dp)) {
            // A do centro vem por último, para passar por cima das outras.
            for (i in noPalco.sortedByDescending { abs(it - atual) }) {
                val p = pagina(i)
                key(p.numero) {
                    val noCentro = i == atual
                    val descricao = descricaoDaFigurinha(p.numero, p.nome, p.tipos, p.colada)
                    val irAte = Modifier
                        .clearAndSetSemantics {
                            contentDescription = descricao
                            role = Role.Button
                            onClick { aoIrPara(p.numero); true }
                        }
                        .clickable { aoIrPara(p.numero) }
                    // A camada vem primeiro: o toque acerta a figurinha onde ela aparece.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { porNoPalco(i - pager.posicao(), largura, inclina = p.colada) }
                            .vooCompartilhado(chaveDaFigurinha(p.numero), p.numero == inicial && noCentro)
                            .then(if (noCentro) Modifier else irAte),
                    ) {
                        if (p.colada) {
                            FigurinhaAmpliada(
                                pagina = p,
                                fundo = corDe(p)?.let { lerp(it.suave, it.base, 0.16f) } ?: cores.superficieAfundada,
                                ativa = noCentro,
                                aoTocarGrito = { aoTocarGrito(p.numero) },
                            )
                        } else {
                            VagaAmpliada(p)
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth()) {
            for (i in nosRotulos) {
                val p = pagina(i)
                key(p.numero) {
                    Rotulos(
                        p,
                        Modifier.graphicsLayer {
                            val d = i - pager.posicao()
                            // Um nome some antes do outro chegar: no meio do arraste os dois não se cruzam.
                            alpha = (1f - 2.2f * abs(d)).coerceAtLeast(0f)
                            translationX = d * DeslizeDoNome.toPx()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Põe a figurinha a [d] páginas do centro: em 0, a grande, inclinada −2°; em ±1, a vizinha,
 * reduzida e reta, com o centro fora da borda; entre 1 e 2, ela sai da tela e some.
 */
private fun GraphicsLayerScope.porNoPalco(d: Float, largura: Int, inclina: Boolean) {
    val a = abs(d).coerceAtMost(2f)
    val t = a.coerceAtMost(1f)
    val fora = (a - 1f).coerceAtLeast(0f)
    val escala = 1f + (EscalaDaVizinha - 1f) * t
    scaleX = escala
    scaleY = escala
    translationX = sign(d) * ((largura / 2f + ForaDaBorda.toPx()) * t + PassoDeFora.toPx() * fora)
    translationY = -SubidaDaVizinha.toPx() * t
    rotationZ = if (inclina) -2f * (1f - t) else 0f
    alpha = (1f - 0.1f * t) * (1f - fora)
}

/** O nome e as etiquetas de uma espécie. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Rotulos(pagina: PaginaDaEspecie, modifier: Modifier) {
    val texto = if (pagina.colada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth()) {
        Text(
            text = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero),
            style = Estilos.tituloTela.copy(fontSize = 28.sp, lineHeight = 34.sp),
            color = texto,
            modifier = Modifier.padding(horizontal = Espacos.margem).padding(top = 6.dp).semantics { heading() },
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = Espacos.margem).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            if (pagina.colada && pagina.tipos.isNotEmpty()) {
                // Só o ícone, da altura da etiqueta do tier; o nome do tipo vai na descrição falada.
                pagina.tipos.forEach { tipo ->
                    val nomeDoTipo = stringResource(nomeDoTipo(tipo))
                    IconeDeTipo(tipo, Modifier.semantics { contentDescription = nomeDoTipo }, lado = LadoDoIconeDeTipo)
                }
            } else {
                // Na página tingida, o `Tipo ?` vai em branco, como a etiqueta do tier.
                ChipDeTipo(null, grande = true, fundoSemTipo = MaterialTheme.colorScheme.surface)
            }
            Text(
                stringResource(R.string.escada_tier_nome, pagina.tier, pagina.nomeDoTier).uppercase(LocaleDoApp),
                style = Estilos.rotulo.copy(letterSpacing = Estilos.rotulo.letterSpacing * 0.75f),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .heightIn(min = 28.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                    .padding(horizontal = Espacos.interno, vertical = 6.dp),
            )
        }
    }
}

/**
 * A figurinha grande, com borda branca de 6dp e o 3D inteiro dentro dela. Só a [ativa], a do
 * centro, toca o grito.
 */
@Composable
private fun FigurinhaAmpliada(pagina: PaginaDaEspecie, fundo: Color, ativa: Boolean, aoTocarGrito: () -> Unit, modifier: Modifier = Modifier) {
    val nome = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero)
    val descricao = stringResource(R.string.pokedex_tocar_grito, nome)
    val tocar = aoTocarGrito
    Box(
        modifier = modifier
            .size(LarguraDaAmpliada, AlturaDaAmpliada)
            .shadow(10.dp, RoundedCornerShape(20.dp), clip = false, spotColor = MaterialTheme.colorScheme.onSurface)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(BordaDaGrande),
    ) {
        Box(Modifier.fillMaxSize().background(fundo, RoundedCornerShape(15.dp)))
        Text(
            stringResource(R.string.especie_numero, pagina.numero),
            style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily, fontSize = 13.sp)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 6.dp),
        )
        SpriteAnimado(
            numero = pagina.numero,
            paraNoReduzido = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, end = 4.dp, top = TopoDoTresD, bottom = 4.dp)
                // O TalkBack chega ao grito pelo selo: o toque no Pokémon é o atalho visual. Na
                // vizinha, nem o 3D nem o selo têm clique: um clique desligado ainda toma o toque, e
                // o toque é da figurinha inteira, que vai até ela.
                .then(
                    if (ativa) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = tocar)
                    } else {
                        Modifier
                    },
                )
                .clearAndSetSemantics { },
        )
        SeloDeSom(descricao, tocar, ativa, Modifier.align(Alignment.BottomEnd).offset(x = BordaDaGrande, y = BordaDaGrande))
    }
}

/** A vaga grande da não alcançada: bloco cheio com a silhueta chapada do render do HOME. */
@Composable
private fun VagaAmpliada(pagina: PaginaDaEspecie, modifier: Modifier = Modifier) {
    val cores = LocalCoresPokerun.current
    val nome = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero)
    val descricao = pagina.kmFalta?.let { stringResource(R.string.pokedex_vaga_descricao_km, nome, formatarKm(it)) }
        ?: stringResource(R.string.pokedex_vaga_descricao, nome)
    Box(
        modifier = modifier
            .size(LarguraDaAmpliada, AlturaDaAmpliada)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clearAndSetSemantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        val filtro = remember(cores.silhueta) { ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) }
        AsyncImage(
            model = com.hggabriel.pokerun.dados.rede.spritesAnimadosDe(pagina.numero)[4],
            contentDescription = null,
            colorFilter = filtro,
            modifier = Modifier.size(210.dp),
        )
        Text(
            stringResource(R.string.especie_numero, pagina.numero),
            style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily, fontSize = 13.sp)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp),
        )
    }
}

/**
 * O selo de som no canto da figurinha: o play do grito. O disco tem 36dp, e o alvo de toque
 * tem 48dp. O selo fica dentro da borda da figurinha, que corta o que passa dela.
 */
@Composable
private fun SeloDeSom(descricao: String, aoTocar: () -> Unit, ativo: Boolean, modifier: Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(Espacos.alvoToque)
            .clip(FormaCirculo)
            .then(if (ativo) Modifier.clickable(onClick = aoTocar) else Modifier)
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { aoTocar(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(4.dp, FormaCirculo)
                .background(MaterialTheme.colorScheme.surface, FormaCirculo),
        )
        Canvas(Modifier.size(18.dp)) {
            val u = size.width / 24f
            val alto = Path().apply {
                moveTo(4 * u, 9 * u); lineTo(8 * u, 9 * u); lineTo(13 * u, 5 * u)
                lineTo(13 * u, 19 * u); lineTo(8 * u, 15 * u); lineTo(4 * u, 15 * u); close()
            }
            drawPath(alto, tinta)
            val traco = Stroke(1.75f * u, cap = StrokeCap.Round)
            drawArc(tinta, -45f, 90f, false, Offset(11.5f * u, 8.5f * u), androidx.compose.ui.geometry.Size(7f * u, 7f * u), style = traco)
            drawArc(tinta, -45f, 90f, false, Offset(9.5f * u, 4f * u), androidx.compose.ui.geometry.Size(13f * u, 16f * u), style = traco)
        }
    }
}

/**
 * Toca o grito da espécie (`cries.latest`): o primeiro som de conteúdo do app, resposta a
 * um toque explícito no play. Toca pelo volume de mídia, como um vídeo, também com a
 * campainha no silencioso. Um grito novo corta o anterior.
 */
@Composable
internal fun rememberTocadorDeGrito(): (String) -> Unit {
    val contexto = LocalContext.current
    val tocador = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) {
        onDispose {
            tocador[0]?.release()
            tocador[0] = null
        }
    }
    return remember(contexto) {
        { endereco ->
            tocador[0]?.release()
            tocador[0] = try {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(endereco)
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** `Quinta, 23 de out`, no fuso da temporada. */
internal fun diaDaCorrida(corrida: Corrida, fuso: java.time.ZoneId): String {
    val dia = corrida.dataHoraInicio.atZone(fuso).toLocalDate()
    val semana = com.hggabriel.pokerun.ui.componentes.nomeDoDia(dia.dayOfWeek).substringBefore('-')
        .replaceFirstChar { it.titlecase(LocaleDoApp) }
    return "$semana, ${dia.dayOfMonth} de ${com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes(dia)}"
}
