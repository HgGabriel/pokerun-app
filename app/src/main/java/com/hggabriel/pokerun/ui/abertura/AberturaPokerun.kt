package com.hggabriel.pokerun.ui.abertura

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A física da abertura (docs/02 §2.7). Duas molas em sequência: a entrada leva a
 * Pokébola ao repouso com quique, a saída abre a cena sobre a primeira tela. O tempo de
 * cada uma sai da física, não de uma duração; `AberturaTest` soma as duas contra
 * [com.hggabriel.pokerun.ui.theme.Movimento.Duracao.ABERTURA].
 */
internal object FisicaDaAbertura {
    /** Entrada. Maior: quique menor e pico mais baixo. Menor: pico mais alto e mais oscilações até o repouso. */
    const val AMORTECIMENTO_ENTRADA = 0.42f
    /** Entrada. Maior: o pico chega antes e a mola assenta mais rápido. Menor: pop lento e mole, abertura mais longa. */
    const val RIGIDEZ_ENTRADA = 520f
    /** Saída. Abaixo de 1 o logo volta a crescer no fim do sumiço. Acima de 1 o furo se arrasta antes de cobrir a tela. */
    const val AMORTECIMENTO_SAIDA = 1f
    /** Saída. Maior: o furo abre e o logo some mais depressa. Menor: a primeira tela aparece mais tarde. */
    const val RIGIDEZ_SAIDA = 700f
    /** As duas. Maior: a mola para mais cedo, com um salto final visível. Menor: mais quadros invisíveis no fim. */
    const val LIMIAR_DE_REPOUSO = 0.01f

    /** Escala do logo no repouso da entrada. Maior: o pop e o overshoot ficam maiores. Menor: o logo quase não cresce. */
    const val ESCALA_DE_REPOUSO = 1.25f
    /** Graus de balanço, para um lado na subida e para o outro no overshoot. Maior: o tranco da captura fica mais forte. */
    const val BALANCO = 10f
    /** Quanto o logo encolhe na saída, de 0 a 1. Maior: o logo cai para dentro do furo. Menor: só desbota. */
    const val ENCOLHE_NA_SAIDA = 0.35f
    /** Folga do raio que cobre a tela. Maior: o furo cobre os cantos antes do repouso. Menor: o canto mais longe some por último. */
    const val FOLGA_DO_RAIO = 1.04f

    val entrada: SpringSpec<Float> = spring(AMORTECIMENTO_ENTRADA, RIGIDEZ_ENTRADA, LIMIAR_DE_REPOUSO)
    val saida: SpringSpec<Float> = spring(AMORTECIMENTO_SAIDA, RIGIDEZ_SAIDA, LIMIAR_DE_REPOUSO)

    /**
     * O valor máximo da entrada: 1 mais o overshoot de uma mola subamortecida que parte do
     * repouso, `exp(-ζπ / √(1 - ζ²))`. Os previews mostram este quadro.
     */
    val PICO_DA_ENTRADA: Float = AMORTECIMENTO_ENTRADA.let { z ->
        1f + exp(-z * PI / sqrt(1.0 - z * z)).toFloat()
    }
}

/**
 * Quanto a abertura espera o aviso de que a splash saiu. O aviso chega junto do primeiro
 * quadro; o teto só existe para o caso de a splash não ter aparecido.
 */
private const val ESPERA_PELA_SPLASH = 500L

/**
 * O canvas de 108 da splash quando o sistema não informa o ícone: 288dp, com os 72 do
 * meio num círculo de 192dp, como no Android 12.
 */
private val LADO_PADRAO = 288.dp

/** A parte do canvas de 108 que a vista do ícone da splash mostra. */
private const val VISIVEL = 72f

/** O anel em `fundo` em volta da bola, em unidades do canvas da marca: separa a bola do disco vermelho. */
private const val ANEL = 2.5f

/** Um quadro da abertura: o valor de cada mola. A entrada passa de 1 no overshoot. */
internal data class QuadroDaAbertura(val entrada: Float, val saida: Float)

/** As duas molas da abertura. Só a cena lê os valores, e só no desenho. */
internal class MolasDaAbertura {
    val entrada = Animatable(0f, FisicaDaAbertura.LIMIAR_DE_REPOUSO)
    val saida = Animatable(0f, FisicaDaAbertura.LIMIAR_DE_REPOUSO)

    fun quadro() = QuadroDaAbertura(entrada.value, saida.value)
}

/** Chama o fim da abertura uma vez só, venha ele do repouso das molas ou de um toque. */
internal class Encerramento(private val aoTerminar: () -> Unit) {
    private val encerrou = AtomicBoolean(false)

    fun encerrar() {
        if (encerrou.compareAndSet(false, true)) aoTerminar()
    }
}

/**
 * A escala de tempo das molas, fixa em 1. O Compose escala as animações pela escala de
 * animação do sistema, e com 0 elas pulam para o fim: a abertura não usa
 * `Movimento.especificacao` e roda inteira mesmo com movimento reduzido.
 */
private object TempoReal : MotionDurationScale {
    override val scaleFactor = 1f
}

/**
 * Toca as duas molas e encerra. `animateTo` só retorna quando a mola atinge o repouso, e
 * por isso [Encerramento.encerrar] vem depois do último quadro, sem tempo fixo nenhum.
 */
internal suspend fun tocarAbertura(molas: MolasDaAbertura, encerramento: Encerramento) {
    withContext(TempoReal) {
        molas.entrada.animateTo(1f, FisicaDaAbertura.entrada)
        molas.saida.animateTo(1f, FisicaDaAbertura.saida)
    }
    encerramento.encerrar()
}

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

    /**
     * A remoção foi pedida antes de a splash avisar. Acontece quando a abertura acaba
     * antes do aviso: um toque durante a splash, ou a espera pela splash vencida num
     * aparelho lento. Sem isto a splash chegava depois e ficava na tela para sempre.
     */
    private var removerAoChegar = false

    fun acompanhar(splash: SplashScreen) {
        splash.setOnExitAnimationListener { p ->
            provedor = p
            icone = runCatching {
                val vista = p.iconView
                val pos = IntArray(2).also(vista::getLocationInWindow)
                Rect(Offset(pos[0].toFloat(), pos[1].toFloat()), Offset((pos[0] + vista.width).toFloat(), (pos[1] + vista.height).toFloat()))
            }.getOrNull()
            saiu = true
            if (removerAoChegar) removerSplash()
        }
    }

    /**
     * Tira a splash da tela, agora ou assim que ela avisar. Pode ser chamado mais de uma vez.
     *
     * Primeiro some a vista inteira, depois ela sai. Remover direto libera a superfície do
     * ícone antes da vista, e o fundo branco da splash ficava ~100ms na tela sem a bola.
     */
    fun removerSplash() {
        val p = provedor
        if (p == null) {
            removerAoChegar = true
            return
        }
        provedor = null
        p.view.visibility = View.INVISIBLE
        p.view.postOnAnimation { p.remove() }
    }
}

/**
 * A abertura: a Pokébola-cronômetro dá um pop com quique sobre um disco em `destaque`, e
 * um furo aberto do centro da bola revela a primeira tela (docs/02 §2.7, §6).
 *
 * Começa no último quadro da splash do sistema e termina em [aoTerminar], que tira a
 * abertura de cima da primeira tela. A primeira tela compõe por baixo desde o início:
 * a abertura não segura carregamento nenhum. Um toque em qualquer ponto encerra.
 */
@Composable
fun AberturaPokerun(saida: SaidaDaSplash, aoTerminar: () -> Unit) {
    val terminar by rememberUpdatedState(aoTerminar)
    val encerramento = remember { Encerramento { terminar() } }
    val molas = remember { MolasDaAbertura() }
    // Marcado pelo desenho, não é estado: ler isto não recompõe nada.
    val desenhou = remember { AtomicBoolean(false) }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(ESPERA_PELA_SPLASH) { snapshotFlow { saida.saiu }.first { it } }
        // A splash só sai depois que a cena desenhou com o ícone no lugar. O aviso da splash
        // chega antes de o Compose desenhar o primeiro quadro: tirá-la ali mostrava ~150ms
        // de janela branca no emulador.
        // Sem splash (o aviso não veio), não há o que esperar.
        while (saida.saiu && !desenhou.get()) withFrameNanos { }
        withFrameNanos { }
        saida.removerSplash()
        tocarAbertura(molas, encerramento)
    }
    DisposableEffect(Unit) { onDispose { saida.removerSplash() } }

    CenaDaAbertura(
        quadro = molas::quadro,
        icone = { saida.icone },
        aoDesenhar = { if (saida.saiu) desenhou.set(true) },
        modifier = Modifier
            .pointerInput(Unit) { detectTapGestures { encerramento.encerrar() } }
            // Decorativa: o leitor de tela vai direto à primeira tela.
            .clearAndSetSemantics { },
    )
}

/**
 * A cena num quadro. [icone] é a vista do ícone da splash em px; `null` centraliza com o
 * lado padrão.
 *
 * Por que não recompõe: [quadro] e [icone] chegam como lambdas e só são chamados dentro
 * de `graphicsLayer { }` e de `drawWithCache { }`. A leitura do estado das molas fica
 * registrada na fase de desenho, e cada quadro da animação invalida só o desenho do fundo
 * e as propriedades da camada do logo. Composição e layout rodam uma vez.
 *
 * O ganho no cold start: no primeiro segundo do app a thread principal ainda compõe a
 * primeira tela por baixo. A Pokébola é gravada uma vez numa camada; depois disso a escala,
 * o giro e o alpha mudam só as propriedades da camada, sem regravar os paths. O fundo
 * regrava um retângulo, um círculo e um clip. Nenhum quadro da abertura disputa a
 * composição com a primeira tela.
 */
@Composable
internal fun CenaDaAbertura(
    quadro: () -> QuadroDaAbertura,
    icone: () -> Rect?,
    modifier: Modifier = Modifier,
    aoDesenhar: () -> Unit = {},
) {
    val cores = MaterialTheme.colorScheme
    val destaque = cores.primary
    val tinta = cores.onBackground
    val fundo = cores.background
    val ladoPadrao = with(LocalDensity.current) { LADO_PADRAO.toPx() }

    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val m = moldura(size, icone(), ladoPadrao)
                val raio = raioQueCobre(size, m.centro)
                val furo = Path()
                onDrawBehind {
                    val q = quadro()
                    val raioDoFuro = raio * q.saida.coerceIn(0f, 1f)
                    furo.reset()
                    furo.addOval(Rect(m.centro, raioDoFuro))
                    // Revelação por clip: tudo fora do furo é cena; dentro dele, a primeira tela.
                    clipPath(furo, ClipOp.Difference) {
                        drawRect(fundo)
                        val raioDoDisco = raio * q.entrada.coerceIn(0f, 1f)
                        if (raioDoDisco > 0f) drawCircle(destaque, raioDoDisco, m.centro)
                    }
                    aoDesenhar()
                }
            },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val q = quadro()
                    val centro = moldura(size, icone(), ladoPadrao).centro
                    transformOrigin = TransformOrigin(centro.x / size.width, centro.y / size.height)
                    val escala = escalaDoLogo(q)
                    scaleX = escala
                    scaleY = escala
                    rotationZ = balancoDoLogo(q.entrada)
                    alpha = (1f - q.saida).coerceIn(0f, 1f)
                },
        ) {
            val m = moldura(size, icone(), ladoPadrao)
            val g = GeometriaDaMarca
            drawCircle(fundo, (g.R + g.TRACO / 2f + ANEL) * m.u, m.centro)
            desenharMarca(origem = m.origem, u = m.u, destaque = destaque, tinta = tinta, fundo = fundo)
        }
    }
}

/** Onde a marca cai na tela: o canto do canvas de 108, px por unidade e o centro da bola. */
private class Moldura(val origem: Offset, val u: Float, val centro: Offset)

private fun moldura(tela: Size, icone: Rect?, ladoPadrao: Float): Moldura {
    val g = GeometriaDaMarca
    // O sistema informa a vista do ícone, que mostra os 72 do meio do canvas de 108.
    val lado = icone?.let { it.width * g.CANVAS / VISIVEL } ?: ladoPadrao
    val meio = icone?.center ?: Offset(tela.width / 2f, tela.height / 2f)
    val origem = Offset(meio.x - lado / 2f, meio.y - lado / 2f)
    val u = lado / g.CANVAS
    return Moldura(origem, u, Offset(origem.x + g.CX * u, origem.y + g.CY * u))
}

/** O raio, a partir de [centro], que cobre a tela inteira até o canto mais longe. */
private fun raioQueCobre(tela: Size, centro: Offset): Float {
    val dx = maxOf(centro.x, tela.width - centro.x)
    val dy = maxOf(centro.y, tela.height - centro.y)
    return hypot(dx, dy) * FisicaDaAbertura.FOLGA_DO_RAIO
}

/** Cresce até [FisicaDaAbertura.ESCALA_DE_REPOUSO] com a entrada, passando dela no overshoot, e encolhe na saída. */
internal fun escalaDoLogo(q: QuadroDaAbertura): Float {
    val f = FisicaDaAbertura
    val entrada = 1f + (f.ESCALA_DE_REPOUSO - 1f) * q.entrada
    return entrada * (1f - f.ENCOLHE_NA_SAIDA * q.saida.coerceIn(0f, 1f))
}

/**
 * O balanço sai da própria mola: zero na partida e no repouso, para um lado na subida e
 * para o outro enquanto a entrada passa de 1. O giro fica no tempo do quique.
 */
internal fun balancoDoLogo(entrada: Float): Float = FisicaDaAbertura.BALANCO * 4f * entrada * (1f - entrada)
