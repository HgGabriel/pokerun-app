package com.hggabriel.pokerun.ui.telas.pokedex

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlin.math.PI
import kotlin.math.sin

/** Onde o grito de uma espécie está: o selo desenha cada fase. */
internal enum class FaseDoGrito {
    PARADO,

    /** O `.ogg` está baixando: o anel gira em volta do selo. */
    CARREGANDO,

    /** O som saiu: as ondas do alto-falante pulsam até ele acabar. */
    TOCANDO,

    /** Sem rede ou sem arquivo: o selo balança uma vez e volta ao parado. */
    FALHOU,
}

/**
 * Toca o grito da espécie (`cries.latest`): o primeiro som de conteúdo do app, resposta a
 * um toque explícito no play. Toca pelo volume de mídia, como um vídeo, também com a
 * campainha no silencioso. Um grito novo corta o anterior.
 *
 * A fase é estado de Compose: o selo da espécie que tocou mostra o carregando e o tocando,
 * e os outros ficam parados.
 */
@Stable
internal class TocadorDeGrito {
    private var numero by mutableIntStateOf(-1)
    private var fase by mutableStateOf(FaseDoGrito.PARADO)
    private var tocador: MediaPlayer? = null

    fun faseDe(especie: Int): FaseDoGrito = if (numero == especie) fase else FaseDoGrito.PARADO

    fun tocar(especie: Int, endereco: String) {
        soltar()
        numero = especie
        fase = FaseDoGrito.CARREGANDO
        tocador = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(endereco)
                // Os avisos chegam pelo looper principal, depois da atribuição: um tocador
                // cortado por outro grito não muda mais a fase.
                setOnPreparedListener { mp ->
                    if (tocador === mp) {
                        mp.start()
                        fase = FaseDoGrito.TOCANDO
                    }
                }
                setOnCompletionListener { mp -> if (tocador === mp) fase = FaseDoGrito.PARADO }
                setOnErrorListener { mp, _, _ ->
                    if (tocador === mp) fase = FaseDoGrito.FALHOU
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            fase = FaseDoGrito.FALHOU
            null
        }
    }

    fun soltar() {
        tocador?.release()
        tocador = null
        fase = FaseDoGrito.PARADO
    }
}

/** O tocador da tela, solto quando ela sai. */
@Composable
internal fun rememberTocadorDeGrito(): TocadorDeGrito {
    val tocador = remember { TocadorDeGrito() }
    DisposableEffect(tocador) { onDispose { tocador.soltar() } }
    return tocador
}

/** Um giro do anel do carregando, e uma onda do tocando. */
private const val CICLO_DO_GRITO = 900

/** O anel do carregando: o arco que gira em volta do disco. */
private const val ARCO_DO_ANEL = 110f

/**
 * O selo de som no canto da figurinha: o play do grito. O disco tem 36dp, e o alvo de toque
 * tem 48dp. O selo fica dentro da borda da figurinha, que corta o que passa dela.
 *
 * O toque afunda o disco na hora. Depois, o selo diz a [fase]: o anel em `primary` gira
 * enquanto o som baixa, as ondas pulsam enquanto ele toca, e o selo balança se falhar. Com
 * movimento reduzido, o anel e as ondas ficam parados e o balanço não acontece.
 */
@Composable
internal fun SeloDeSom(
    descricao: String,
    aoTocar: () -> Unit,
    ativo: Boolean,
    fase: FaseDoGrito,
    modifier: Modifier,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val destaque = MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val afundado by animateFloatAsState(
        if (pressionado) 0.86f else 1f,
        Movimento.especificacao(Movimento.Duracao.TOQUE, Movimento.Curva.padrao, reduzido),
        label = "toque do grito",
    )
    val relogio = remember { Animatable(0f) }
    val balanco = remember { Animatable(0f) }
    LaunchedEffect(fase, reduzido) {
        relogio.snapTo(0f)
        if (!reduzido && (fase == FaseDoGrito.CARREGANDO || fase == FaseDoGrito.TOCANDO)) {
            relogio.animateTo(1f, infiniteRepeatable(tween(CICLO_DO_GRITO, easing = LinearEasing), RepeatMode.Restart))
        }
    }
    LaunchedEffect(fase, reduzido) {
        if (fase == FaseDoGrito.FALHOU && !reduzido) {
            balanco.snapTo(0f)
            balanco.animateTo(1f, tween(Movimento.Duracao.MEDIA * 2, easing = LinearEasing))
            balanco.snapTo(0f)
        }
    }
    val estado = when (fase) {
        FaseDoGrito.CARREGANDO -> stringResource(R.string.pokedex_grito_carregando)
        FaseDoGrito.TOCANDO -> stringResource(R.string.pokedex_grito_tocando)
        FaseDoGrito.FALHOU -> stringResource(R.string.pokedex_grito_falhou)
        FaseDoGrito.PARADO -> ""
    }
    Box(
        modifier = modifier
            .size(Espacos.alvoToque)
            .clip(FormaCirculo)
            .then(if (ativo) Modifier.clickable(interactionSource = fonte, indication = null, onClick = aoTocar) else Modifier)
            .clearAndSetSemantics {
                contentDescription = descricao
                if (estado.isNotEmpty()) stateDescription = estado
                role = Role.Button
                onClick { aoTocar(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = afundado
                    scaleY = afundado
                    // Três idas e voltas que morrem até o fim do balanço.
                    val b = balanco.value
                    translationX = if (b == 0f) 0f else 4.dp.toPx() * (1f - b) * sin(b * 6f * PI.toFloat())
                }
                .size(36.dp)
                .shadow(4.dp, FormaCirculo)
                .background(MaterialTheme.colorScheme.surface, FormaCirculo),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val u = size.width / 24f
                val tocando = fase == FaseDoGrito.TOCANDO
                val alto = Path().apply {
                    moveTo(4 * u, 9 * u); lineTo(8 * u, 9 * u); lineTo(13 * u, 5 * u)
                    lineTo(13 * u, 19 * u); lineTo(8 * u, 15 * u); lineTo(4 * u, 15 * u); close()
                }
                drawPath(alto, if (tocando) destaque else tinta)
                val traco = Stroke(1.75f * u, cap = StrokeCap.Round)
                // Tocando, cada onda acende na sua vez: a de dentro, depois a de fora.
                val t = relogio.value
                val dentro = if (tocando && !reduzido) onda(t) else 1f
                val fora = if (tocando && !reduzido) onda(t - 0.35f) else 1f
                val corDaOnda = if (tocando) destaque else tinta
                drawArc(corDaOnda, -45f, 90f, false, Offset(11.5f * u, 8.5f * u), Size(7f * u, 7f * u), style = traco, alpha = dentro)
                drawArc(corDaOnda, -45f, 90f, false, Offset(9.5f * u, 4f * u), Size(13f * u, 16f * u), style = traco, alpha = fora)
            }
        }
        if (fase == FaseDoGrito.CARREGANDO) {
            Canvas(Modifier.size(42.dp)) {
                val espessura = 2.5.dp.toPx()
                val giro = if (reduzido) 0f else relogio.value * 360f
                drawArc(
                    color = destaque,
                    startAngle = giro - 90f,
                    sweepAngle = if (reduzido) 270f else ARCO_DO_ANEL,
                    useCenter = false,
                    topLeft = Offset(espessura / 2, espessura / 2),
                    size = Size(size.width - espessura, size.height - espessura),
                    style = Stroke(espessura, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** A onda acesa de 0,35 a 1 ao longo do ciclo, em seno. */
private fun onda(t: Float): Float {
    val fase = ((t % 1f) + 1f) % 1f
    return 0.35f + 0.65f * (0.5f - 0.5f * kotlin.math.cos(2f * PI.toFloat() * fase))
}
