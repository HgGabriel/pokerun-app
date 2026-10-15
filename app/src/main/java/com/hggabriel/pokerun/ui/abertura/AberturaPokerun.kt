package com.hggabriel.pokerun.ui.abertura

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.Movimento
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * As fases da abertura (docs/02 §6, exceção da abertura). Só tokens de duração e de
 * curva já existentes; o total cabe em [Movimento.Duracao.ABERTURA], e `AberturaTest`
 * trava isso.
 */
internal object FasesDaAbertura {
    /** Início: a tampa abre e o Ponyta salta da bola. `curva-captura`. */
    val ABRE = Movimento.Duracao.MEDIA
    /** Clímax: o Ponyta dispara para a direita. É uma saída: 60% do preenchimento, `curva-saida`. */
    val CORRE = Movimento.Duracao.saida(Movimento.Duracao.PREENCHIMENTO)
    /** Saída: a cena se dissolve sobre a primeira tela. `curva-saida`. */
    val DISSOLVE = Movimento.Duracao.CURTA
    val TOTAL = ABRE + CORRE + DISSOLVE

    /** Quanto a tampa gira para abrir, em graus, para cima e para trás. */
    const val GIRO_DA_TAMPA = -55f

    /**
     * Quanto a abertura espera o aviso de que a splash saiu. O aviso chega junto do
     * primeiro quadro; o teto só existe para o caso de a splash não ter aparecido.
     */
    const val ESPERA_PELA_SPLASH = 500L
}

/**
 * O sprite de jogo do Ponyta (#77), de `res/drawable-nodpi`. A caixa é a área opaca do
 * PNG de 96px: o resto é margem transparente.
 */
private val CAIXA_DO_SPRITE = IntOffset(27, 20) to IntSize(52, 57)

/** Unidades do canvas da marca por pixel do sprite: o Ponyta fica da altura da bola. */
private const val ESCALA_DO_SPRITE = 0.8f

/** O traço mais longo do rastro, em unidades do canvas da marca. */
private const val RASTRO = 34f

/**
 * O canvas de 108 da splash quando o sistema não informa o ícone: 288dp, com os 72 do
 * meio num círculo de 192dp, como no Android 12.
 */
private val LADO_PADRAO = 288.dp

/** A parte do canvas de 108 que a vista do ícone da splash mostra. */
private const val VISIVEL = 72f

/** Um quadro da abertura: o progresso de cada fase, de 0 a 1. */
internal data class QuadroDaAbertura(val abre: Float, val corre: Float, val dissolve: Float)

/**
 * A ponte entre a splash do sistema e a abertura em Compose.
 *
 * Guarda onde o sistema desenhou o ícone, para a abertura começar no mesmo lugar, e
 * só tira a splash da tela depois que a abertura desenhou um quadro por baixo dela.
 */
@Stable
class SaidaDaSplash {
    /** A splash avisou que o app está pronto. */
    var saiu by mutableStateOf(false)
        private set

    /**
     * A vista do ícone da splash, em px da janela: os 72 do meio do canvas de 108.
     * `null` se o sistema não informou.
     */
    var icone by mutableStateOf<Rect?>(null)
        private set

    private var provedor: SplashScreenViewProvider? = null

    fun acompanhar(splash: SplashScreen) {
        splash.setOnExitAnimationListener { p ->
            provedor = p
            icone = runCatching {
                val vista = p.iconView
                val pos = IntArray(2).also(vista::getLocationInWindow)
                Rect(Offset(pos[0].toFloat(), pos[1].toFloat()), Offset((pos[0] + vista.width).toFloat(), (pos[1] + vista.height).toFloat()))
            }.getOrNull()
            saiu = true
        }
    }

    /**
     * Tira a splash da tela. Pode ser chamado mais de uma vez.
     *
     * Primeiro some a vista inteira, depois ela sai. Remover direto libera a superfície do
     * ícone antes da vista, e o fundo branco da splash ficava ~100ms na tela sem a bola.
     */
    fun removerSplash() {
        val p = provedor ?: return
        provedor = null
        p.view.visibility = View.INVISIBLE
        p.view.postOnAnimation { p.remove() }
    }
}

/**
 * A abertura: a Pokébola-cronômetro abre e o Ponyta sai correndo (docs/02 §4.4, §6).
 *
 * Começa no último quadro da splash do sistema e termina em [aoTerminar], que tira a
 * abertura de cima da primeira tela. A primeira tela compõe por baixo desde o início:
 * a abertura não segura carregamento nenhum. Um toque em qualquer ponto encerra.
 *
 * Quem chama não compõe isto com movimento reduzido: a abertura é pulada inteira.
 */
@Composable
fun AberturaPokerun(saida: SaidaDaSplash, aoTerminar: () -> Unit) {
    val terminar by rememberUpdatedState(aoTerminar)
    val abre = remember { Animatable(0f) }
    val corre = remember { Animatable(0f) }
    val dissolve = remember { Animatable(0f) }
    // Marcado pelo desenho, não é estado: ler isto não recompõe nada.
    val desenhou = remember { AtomicBoolean(false) }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(FasesDaAbertura.ESPERA_PELA_SPLASH) { snapshotFlow { saida.saiu }.first { it } }
        // A splash só sai depois que a cena desenhou com o ícone no lugar. O aviso da splash
        // chega antes de o Compose desenhar o primeiro quadro: tirá-la ali mostrava ~150ms
        // de janela branca no emulador.
        // Sem splash (o aviso não veio), não há o que esperar.
        while (saida.saiu && !desenhou.get()) withFrameNanos { }
        withFrameNanos { }
        saida.removerSplash()
        abre.animateTo(1f, tween(FasesDaAbertura.ABRE, easing = Movimento.Curva.captura))
        corre.animateTo(1f, tween(FasesDaAbertura.CORRE, easing = Movimento.Curva.saida))
        dissolve.animateTo(1f, tween(FasesDaAbertura.DISSOLVE, easing = Movimento.Curva.saida))
        terminar()
    }
    DisposableEffect(Unit) { onDispose { saida.removerSplash() } }

    CenaDaAbertura(
        quadro = { QuadroDaAbertura(abre.value, corre.value, dissolve.value) },
        icone = saida.icone,
        aoDesenhar = { if (saida.saiu) desenhou.set(true) },
        modifier = Modifier
            .pointerInput(Unit) { detectTapGestures { terminar() } }
            // Decorativa: o leitor de tela vai direto à primeira tela.
            .clearAndSetSemantics { },
    )
}

/**
 * A cena num quadro. [quadro] é lido só no desenho: a animação não recompõe a árvore.
 * [icone] é a vista do ícone da splash em px; `null` centraliza com o lado padrão.
 */
@Composable
internal fun CenaDaAbertura(
    quadro: () -> QuadroDaAbertura,
    icone: Rect?,
    modifier: Modifier = Modifier,
    aoDesenhar: () -> Unit = {},
) {
    val cores = MaterialTheme.colorScheme
    val destaque = cores.primary
    val tinta = cores.onBackground
    val fundo = cores.background
    val sprite = ImageBitmap.imageResource(R.drawable.sprite_ponyta)
    val ladoPadrao = with(LocalDensity.current) { LADO_PADRAO.toPx() }

    Canvas(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - quadro().dissolve },
    ) {
        val q = quadro()
        drawRect(fundo)
        // O sistema informa a vista do ícone, que mostra os 72 do meio do canvas de 108.
        val lado = icone?.let { it.width * GeometriaDaMarca.CANVAS / VISIVEL } ?: ladoPadrao
        val centro = icone?.center ?: Offset(size.width / 2f, size.height / 2f)
        val origem = Offset(centro.x - lado / 2f, centro.y - lado / 2f)
        val u = lado / GeometriaDaMarca.CANVAS
        desenharMarca(
            origem = origem,
            u = u,
            destaque = destaque,
            tinta = tinta,
            fundo = fundo,
            giroDaTampa = FasesDaAbertura.GIRO_DA_TAMPA * q.abre,
        ) {
            desenharPonyta(sprite, origem, u, q, rastro = tinta)
        }
        aoDesenhar()
    }
}

/**
 * O Ponyta salta da bola com escala ancorada nos cascos e depois corre para fora da tela,
 * para a direita, com o rastro de velocidade atrás. Espelhado: o sprite olha para a
 * esquerda. Sem suavização (docs/02 §4.4).
 */
private fun DrawScope.desenharPonyta(sprite: ImageBitmap, origem: Offset, u: Float, q: QuadroDaAbertura, rastro: Color) {
    if (q.abre <= 0f) return
    val g = GeometriaDaMarca
    val (fonte, caixa) = CAIXA_DO_SPRITE
    val k = ESCALA_DO_SPRITE * u                       // px por pixel do sprite
    val largura = caixa.width * k
    val altura = caixa.height * k
    val casco = Offset(origem.x + g.CX * u, origem.y + (g.CY - g.FAIXA / 2 + 1f) * u)
    val rastroMaximo = RASTRO * u
    // O bastante para o sprite e o rastro saírem inteiros pela direita.
    val ate = size.width - (casco.x - largura / 2f) + rastroMaximo + 2f * u
    val dx = ate * q.corre
    val esquerda = casco.x - largura / 2f + dx
    val topo = casco.y - altura

    // Rastro: três traços que crescem com a corrida, colados ao traseiro.
    if (q.corre > 0f) {
        val comprimento = minOf(dx, rastroMaximo)
        listOf(0.38f to 1f, 0.56f to 0.6f, 0.74f to 0.82f).forEach { (altura01, fator) ->
            val y = topo + altura * altura01
            val fim = esquerda - 2f * u
            drawLine(rastro, Offset(fim - comprimento * fator, y), Offset(fim, y), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
        }
    }

    val salto = q.abre.coerceAtLeast(0f)
    scale(salto, salto, pivot = casco) {
        // Espelha em torno do centro do sprite.
        scale(-1f, 1f, pivot = Offset(esquerda + largura / 2f, topo + altura / 2f)) {
            drawImage(
                image = sprite,
                srcOffset = fonte,
                srcSize = caixa,
                dstOffset = IntOffset(esquerda.toInt(), topo.toInt()),
                dstSize = IntSize(largura.toInt(), altura.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
}
