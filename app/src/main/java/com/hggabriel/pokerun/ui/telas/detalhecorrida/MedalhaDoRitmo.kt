package com.hggabriel.pokerun.ui.telas.detalhecorrida

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.telas.corridamanual.nome
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * A medalha do ritmo (29/10/2026): o ritmo da corrida vira uma medalha de prova com fitas,
 * presa no canto de cima do cartão do placar, em par com o Pokémon da corrida. No miolo, a
 * espécie do ritmo no sprite de Ruby/Sapphire. A medalha anima toda vez que a tela abre, e a
 * animação fica mais forte quanto mais rápido o ritmo: o Slowpoke pousa, o Jolteon é batido
 * como carimbo, gira, sacode o cartão e solta faíscas.
 */

/** O sprite de Ruby/Sapphire tem 64×64 px e só aparece em escala inteira. */
private const val PIXELS_DO_SPRITE = 64

/** As medidas do esboço aprovado, em pixels do sprite: a medalha cresce com a escala dele. */
private const val ROSETA = 92f
private const val ALTURA = 112f
private const val TOPO_DAS_FITAS = 58f
private const val LARGURA_DA_FITA = 22f
private const val ALTURA_DA_FITA = 54f
private const val AFASTAMENTO_DA_FITA = 13f
private const val ABERTURA_DA_FITA = 14f
private const val SPRITE_X = 14f
private const val SPRITE_Y = 10f
private const val TOPO_DA_PILULA = 78f
private const val DENTES = 24
private const val RAIO_DO_DENTE = 0.46f
private const val RAIO_DO_VAO = 0.41f
private const val RAIO_DO_MIOLO = 0.36f

/** A inclinação da medalha presa no canto. */
private const val INCLINACAO = -8f

/** Os balanços depois de assentar, cada um menor que o anterior. */
private val AMPLITUDES = listOf(10f, 6f, 3f)

/** Quanto a medalha espera o sprite carregar antes de entrar sem ele. */
private const val ESPERA_DO_SPRITE_MS = 1_200L

/** As três entradas da medalha, da mais calma à mais forte. */
internal enum class EntradaDaMedalha {
    /** Desce pouco e devagar, em `tempo-lento`: o Slowpoke. */
    POUSO,

    /** Cai de mais alto e quica no fim, em `curva-captura`. */
    QUEDA,

    /** Entra grande e é batida no cartão, como o carimbo da sessão na Home. */
    CARIMBO,
}

/**
 * Como a medalha entra, para um ritmo.
 *
 * @property voltas giros inteiros da medalha em torno do eixo vertical durante a entrada,
 *   como moeda jogada.
 * @property balancos quantas vezes a medalha balança presa pelo topo depois de assentar.
 * @property brilho uma faixa de luz que passa pela medalha depois de assentar.
 * @property impacto o cartão sente o tranco e o Pokémon da corrida pula.
 * @property faiscas quantas faíscas saem da medalha no impacto, na cor do tipo do ritmo.
 */
@Immutable
internal data class CoreografiaDaMedalha(
    val entrada: EntradaDaMedalha,
    val voltas: Int,
    val balancos: Int,
    val brilho: Boolean,
    val impacto: Boolean,
    val faiscas: Int,
)

/** A coreografia de cada ritmo: quanto mais rápido, mais expressiva. */
internal fun coreografiaDaMedalha(ritmo: Ritmo): CoreografiaDaMedalha = when (ritmo) {
    Ritmo.SLOWPOKE -> CoreografiaDaMedalha(EntradaDaMedalha.POUSO, voltas = 0, balancos = 0, brilho = false, impacto = false, faiscas = 0)
    Ritmo.PSYDUCK -> CoreografiaDaMedalha(EntradaDaMedalha.QUEDA, voltas = 0, balancos = 1, brilho = false, impacto = false, faiscas = 0)
    Ritmo.PIKACHU -> CoreografiaDaMedalha(EntradaDaMedalha.QUEDA, voltas = 0, balancos = 2, brilho = true, impacto = false, faiscas = 0)
    Ritmo.GROWLITHE -> CoreografiaDaMedalha(EntradaDaMedalha.CARIMBO, voltas = 0, balancos = 2, brilho = true, impacto = false, faiscas = 0)
    Ritmo.PONYTA -> CoreografiaDaMedalha(EntradaDaMedalha.CARIMBO, voltas = 1, balancos = 2, brilho = true, impacto = true, faiscas = 0)
    Ritmo.RAPIDASH -> CoreografiaDaMedalha(EntradaDaMedalha.CARIMBO, voltas = 1, balancos = 3, brilho = true, impacto = true, faiscas = 8)
    Ritmo.JOLTEON -> CoreografiaDaMedalha(EntradaDaMedalha.CARIMBO, voltas = 2, balancos = 3, brilho = true, impacto = true, faiscas = 12)
}

/** O tipo da espécie do ritmo, que pinta o miolo, as fitas e as faíscas. */
internal val Ritmo.tipo: TipoDePokemon
    get() = when (this) {
        Ritmo.JOLTEON, Ritmo.PIKACHU -> TipoDePokemon.ELETRICO
        Ritmo.RAPIDASH, Ritmo.PONYTA, Ritmo.GROWLITHE -> TipoDePokemon.FOGO
        Ritmo.PSYDUCK, Ritmo.SLOWPOKE -> TipoDePokemon.AGUA
    }

private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

/** O sprite de Ruby/Sapphire, com o de Emerald de reserva. Os dois têm 64×64 px. */
internal fun spritesDoRitmo(numero: Int): List<String> = listOf(
    "$SPRITES/versions/generation-iii/ruby-sapphire/$numero.png",
    "$SPRITES/versions/generation-iii/emerald/$numero.png",
)

/**
 * O estado da animação da medalha, guardado pela tela e não pelo item da lista: rolar o
 * placar para fora e de volta não repete a animação. Abrir a tela de novo repete.
 *
 * Também move o que a medalha atinge: o tranco do cartão ([tremor]) e o pulo do Pokémon
 * da corrida ([salto]). Com movimento reduzido, tudo nasce assentado.
 */
@Stable
internal class EstadoDaMedalha(reduzido: Boolean) {
    val alfa = Animatable(if (reduzido) 1f else 0f)
    /** Em dp: negativo é acima do lugar dela. */
    val queda = Animatable(0f)
    val escala = Animatable(1f)
    /** Graus em torno do eixo vertical: o giro da moeda. */
    val giro = Animatable(0f)
    /** Graus em torno do topo, somados à [INCLINACAO]. */
    val balanco = Animatable(0f)
    /** A faixa de luz, de 0 (antes de entrar) a 1 (depois de sair). */
    val brilho = Animatable(0f)
    /** As faíscas, de 0 (no miolo) a 1 (longe e apagadas). */
    val faiscas = Animatable(0f)
    /** Em dp, no eixo x do cartão inteiro. */
    val tremor = Animatable(0f)
    /** Em dp, no eixo y do Pokémon da corrida. */
    val salto = Animatable(0f)

    var spritePronto by mutableStateOf(false)
    private var tocou = reduzido

    suspend fun tocar(coreografia: CoreografiaDaMedalha) {
        if (tocou) return
        val carimbo = coreografia.entrada == EntradaDaMedalha.CARIMBO
        alfa.snapTo(0f)
        queda.snapTo(
            when (coreografia.entrada) {
                EntradaDaMedalha.POUSO -> -24f
                EntradaDaMedalha.QUEDA -> -72f
                EntradaDaMedalha.CARIMBO -> 0f
            },
        )
        escala.snapTo(if (carimbo) 1.7f else 1f)
        giro.snapTo(-360f * coreografia.voltas)
        balanco.snapTo(0f)
        brilho.snapTo(0f)
        faiscas.snapTo(0f)

        // A medalha entra com o Pokémon dentro. Sem rede, entra vazia depois da espera.
        withTimeoutOrNull(ESPERA_DO_SPRITE_MS) { snapshotFlow { spritePronto }.first { it } }
        delay(Movimento.Tempo.BATIDA.toLong())

        coroutineScope {
            when (coreografia.entrada) {
                EntradaDaMedalha.POUSO -> {
                    launch { queda.animateTo(0f, Movimento.lento()) }
                    launch { alfa.animateTo(1f, Movimento.lento()) }
                }
                EntradaDaMedalha.QUEDA -> {
                    launch { queda.animateTo(0f, Movimento.pop()) }
                    launch { alfa.animateTo(1f, Movimento.padrao()) }
                }
                EntradaDaMedalha.CARIMBO -> {
                    val gira = coreografia.voltas > 0
                    launch { escala.animateTo(1f, if (gira) Movimento.lento(curva = Movimento.Curva.captura) else Movimento.pop()) }
                    launch { alfa.animateTo(1f, Movimento.padrao()) }
                    if (gira) launch { giro.animateTo(0f, Movimento.lento()) }
                }
            }
        }

        coroutineScope {
            if (coreografia.impacto) {
                launch {
                    tremor.snapTo(6f)
                    tremor.animateTo(0f, Movimento.pop())
                }
                launch {
                    salto.animateTo(-16f, Movimento.padrao())
                    salto.animateTo(0f, Movimento.pop())
                }
            }
            if (coreografia.faiscas > 0) launch { faiscas.animateTo(1f, Movimento.lento()) }
            if (coreografia.brilho) {
                launch {
                    delay(Movimento.Tempo.BATIDA.toLong())
                    brilho.animateTo(1f, Movimento.lento())
                }
            }
            launch {
                AMPLITUDES.take(coreografia.balancos).forEachIndexed { i, amplitude ->
                    balanco.animateTo(if (i % 2 == 0) amplitude else -amplitude, Movimento.padrao())
                }
                if (coreografia.balancos > 0) balanco.animateTo(0f, Movimento.padrao())
            }
        }
        tocou = true
    }
}

/**
 * A medalha: roseta serrilhada branca com o miolo em `tipo-*-suave`, o sprite do ritmo no
 * meio, duas fitas na cor do tipo e o nome numa pílula branca. É figura: o pace ao lado já
 * diz o ritmo ao TalkBack.
 */
@Composable
internal fun MedalhaDoRitmo(ritmo: Ritmo, estado: EstadoDaMedalha, modifier: Modifier = Modifier) {
    val densidade = LocalDensity.current
    // Escala inteira: cada pixel do sprite vira um quadrado de [escala] pixels da tela.
    val escalaDoSprite = densidade.density.roundToInt().coerceAtLeast(1)
    val lado = with(densidade) { (PIXELS_DO_SPRITE * escalaDoSprite).toDp() }
    val u = lado / PIXELS_DO_SPRITE.toFloat()
    val cor = LocalCoresDeTipo.current[ritmo.tipo]
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    val coreografia = remember(ritmo) { coreografiaDaMedalha(ritmo) }

    Box(
        modifier = modifier
            .size(width = u * ROSETA, height = u * ALTURA)
            .clearAndSetSemantics {}
            .graphicsLayer {
                alpha = estado.alfa.value
                translationY = estado.queda.value.dp.toPx()
                rotationZ = INCLINACAO + estado.balanco.value
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .drawBehind {
                val p = estado.faiscas.value
                if (p <= 0f || p >= 1f) return@drawBehind
                val centro = Offset(size.width / 2, (u * ROSETA / 2).toPx())
                val raio = (u * ROSETA / 2).toPx()
                repeat(coreografia.faiscas) { i ->
                    val angulo = (i * 2 * PI / coreografia.faiscas).toFloat()
                    val direcao = Offset(cos(angulo), sin(angulo))
                    val de = raio * (0.7f + 0.6f * p)
                    val ate = de + 16.dp.toPx() * (1f - p)
                    drawLine(
                        color = if (i % 2 == 0) cor.base else tinta,
                        start = centro + direcao * de,
                        end = centro + direcao * ate,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        alpha = 1f - p,
                    )
                }
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = estado.escala.value
                    scaleY = estado.escala.value
                    transformOrigin = TransformOrigin(0.5f, ROSETA / 2 / ALTURA)
                },
        ) {
            Fitas(cor.base, u, estado.balanco.value)
            Roseta(cor.base, cor.suave, claro, tinta, u, estado) {
                SpriteDoRitmo(
                    numero = ritmo.numero,
                    lado = lado,
                    aoCarregar = { estado.spritePronto = true },
                    modifier = Modifier.offset(x = u * SPRITE_X, y = u * SPRITE_Y),
                )
            }
            Text(
                text = stringResource(ritmo.nome).uppercase(LocaleDoApp),
                style = naFigura(Estilos.rotulo).copy(fontWeight = FontWeight.Bold),
                color = tinta,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = u * TOPO_DA_PILULA)
                    .wrapContentWidth(unbounded = true)
                    .shadow(3.dp, FormaDaPilula)
                    .background(claro, FormaDaPilula)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

private val FormaDaPilula = RoundedCornerShape(percent = 50)

/** As duas fitas, abertas em V. No balanço elas atrasam: giram contra a medalha. */
@Composable
private fun Fitas(cor: Color, u: Dp, balanco: Float) {
    Canvas(modifier = Modifier.size(width = u * ROSETA, height = u * ALTURA)) {
        val px = u.toPx()
        val w = LARGURA_DA_FITA * px
        val h = ALTURA_DA_FITA * px
        val fita = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(w / 2, h * 0.8f)
            lineTo(0f, h)
            close()
        }
        val meio = size.width / 2
        val topo = TOPO_DAS_FITAS * px
        val atraso = -balanco * 0.6f
        listOf(-1, 1).forEach { lado ->
            val x = meio + lado * AFASTAMENTO_DA_FITA * px - w / 2
            rotate(degrees = -lado * ABERTURA_DA_FITA + atraso, pivot = Offset(x + w / 2, topo)) {
                translate(left = x, top = topo) { drawPath(fita, cor) }
            }
        }
    }
}

/**
 * A roseta de [DENTES] dentes com o miolo, a sombra curta e a faixa de brilho. A faixa só
 * pinta sobre o que já foi desenhado na camada (`SrcAtop`): a roseta e o sprite.
 */
@Composable
private fun Roseta(
    base: Color,
    suave: Color,
    claro: Color,
    tinta: Color,
    u: Dp,
    estado: EstadoDaMedalha,
    conteudo: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(u * ROSETA)
            .graphicsLayer {
                rotationY = estado.giro.value
                cameraDistance = 12f * density
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val raio = size.minDimension / 2
                val centro = Offset(size.width / 2, size.height / 2)
                val serrilha = serrilha(centro, raio)
                val sombra = 3.dp.toPx()
                val miolo = raio * RAIO_DO_MIOLO * 2
                onDrawWithContent {
                    translate(top = sombra) { drawPath(serrilha, tinta.copy(alpha = 0.16f)) }
                    drawPath(serrilha, claro)
                    drawCircle(suave, miolo, centro)
                    drawCircle(base, miolo, centro, style = Stroke(2.dp.toPx()))
                    drawContent()
                    val p = estado.brilho.value
                    if (p > 0f && p < 1f) {
                        val x = -size.width + p * size.width * 3
                        drawRect(
                            brush = Brush.linearGradient(
                                0f to Color.Transparent,
                                0.5f to claro.copy(alpha = 0.75f),
                                1f to Color.Transparent,
                                start = Offset(x - size.width * 0.35f, 0f),
                                end = Offset(x + size.width * 0.35f, size.height),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
            },
    ) {
        conteudo()
    }
}

/** A borda serrilhada da roseta: [DENTES] dentes em volta de [centro]. */
private fun serrilha(centro: Offset, raio: Float): Path = Path().apply {
    for (i in 0 until DENTES * 2) {
        val r = raio * (if (i % 2 == 0) RAIO_DO_DENTE else RAIO_DO_VAO) * 2
        val a = (-PI / 2 + i * PI / DENTES).toFloat()
        val ponto = centro + Offset(cos(a), sin(a)) * r
        if (i == 0) moveTo(ponto.x, ponto.y) else lineTo(ponto.x, ponto.y)
    }
    close()
}

/**
 * O selo do ritmo: a roseta da medalha sem as fitas, sem o nome e parada, para lugares onde
 * o ritmo é detalhe e não momento (a última corrida da Home). O sprite fica em escala
 * inteira, perto de dois terços da medalha.
 */
@Composable
internal fun SeloDoRitmo(ritmo: Ritmo, modifier: Modifier = Modifier) {
    val densidade = LocalDensity.current
    val escalaDoSprite = (densidade.density * 2 / 3).roundToInt().coerceAtLeast(1)
    val lado = with(densidade) { (PIXELS_DO_SPRITE * escalaDoSprite).toDp() }
    val u = lado / PIXELS_DO_SPRITE.toFloat()
    val cor = LocalCoresDeTipo.current[ritmo.tipo]
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(u * ROSETA)
            .clearAndSetSemantics {}
            .graphicsLayer { rotationZ = INCLINACAO }
            .drawWithCache {
                val raio = size.minDimension / 2
                val centro = Offset(size.width / 2, size.height / 2)
                val borda = serrilha(centro, raio)
                val sombra = 2.dp.toPx()
                val miolo = raio * RAIO_DO_MIOLO * 2
                onDrawBehind {
                    translate(top = sombra) { drawPath(borda, tinta.copy(alpha = 0.16f)) }
                    drawPath(borda, claro)
                    drawCircle(cor.suave, miolo, centro)
                    drawCircle(cor.base, miolo, centro, style = Stroke(1.5.dp.toPx()))
                }
            },
    ) {
        SpriteDoRitmo(
            numero = ritmo.numero,
            lado = lado,
            aoCarregar = {},
            modifier = Modifier.offset(x = u * SPRITE_X, y = u * (SPRITE_Y + 4f)),
        )
    }
}

/** O sprite de 64 px em escala inteira e sem suavizar, com a reserva de Emerald. */
@Composable
private fun SpriteDoRitmo(numero: Int, lado: Dp, aoCarregar: () -> Unit, modifier: Modifier = Modifier) {
    val fontes = remember(numero) { spritesDoRitmo(numero) }
    var indice by remember(numero) { mutableIntStateOf(0) }
    ImagemDaRede(
        model = fontes[indice],
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        aoCarregar = {
            it.result.drawable.isFilterBitmap = false
            aoCarregar()
        },
        aoFalhar = { if (indice < fontes.lastIndex) indice++ else aoCarregar() },
        modifier = modifier.requiredSize(lado),
    )
}
