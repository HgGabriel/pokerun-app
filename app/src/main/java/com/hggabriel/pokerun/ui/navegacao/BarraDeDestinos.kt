package com.hggabriel.pokerun.ui.navegacao

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.Elevacao
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.Raios
import com.hggabriel.pokerun.ui.theme.Tamanhos
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

/** O filete que acompanha o contorno de cima da barra, recorte incluído, em `borda`. */
private val AlturaDoFilete = 1.dp

/** O anel de foco de teclado e D-pad (docs/02 §4.2): 2dp, afastado 2dp. */
private val LarguraDoFoco = 2.dp
private val FolgaDoFoco = 2.dp

/** O indicador do destino ativo: 60 × 32dp em `destaque-suave`, com `raio-controle`. */
private val LarguraDoIndicador = 60.dp
private val AlturaDoIndicador = 32.dp

/** A altura da faixa das abas, a mesma da `NavigationBar` do M3. */
private val AlturaDaBarra = 80.dp

/**
 * A Pokébola tem 60dp: maior que os 56dp do FAB que ela substitui, porque agora é a peça
 * que chama o olho na barra inteira.
 */
private val DiametroDaBola = 60.dp

/** O vão entre a bola e o berço, por onde a página aparece. */
private val FolgaDoBerco = 6.dp

/** O raio que concorda a borda reta da barra com o berço, sem quina. */
private val RaioDaConcordancia = 12.dp

/** O centro da bola fica 4dp abaixo da borda: 26dp dela sobem sobre o conteúdo. */
private val DescidaDaBola = 4.dp

/**
 * Quanto a Pokébola sobe acima da barra, sobre o conteúdo: 26dp. Tela com ação presa no pé
 * soma isto à folga de baixo, ou a bola cobre a ação.
 */
val SubidaDaPokebola: Dp = DiametroDaBola / 2 - DescidaDaBola

/** O vão que as abas deixam no meio para o berço. */
private val VaoDoBerco = DiametroDaBola + FolgaDoBerco * 2 + RaioDaConcordancia * 2

/** A sombra suave que a barra joga para cima, em tinta. */
private const val OpacidadeDaSombraDaBarra = 0.07f
private val DesfoqueDaSombraDaBarra = 18.dp

/**
 * Os destinos de topo (docs/03 §2).
 *
 * **A quarta aba, `Pokédex`, só entra depois do primeiro fechamento semanal** (`F4-T11`).
 * Mostrar uma aba vazia e bloqueada até lá não é aceitável.
 *
 * **Os glifos continuam provisórios, e a razão não mudou desde `F0-T17`:**
 * `material-icons-core` não tem gráfico nem grupo, e `material-icons-extended` ficou
 * de fora quando dependência nova pedia ok. `Home`, `DateRange` e `Person` são o que
 * existe, e a troca é barata: dois campos deste enum.
 */
enum class DestinoDeTopo(
    @param:StringRes val rotulo: Int,
    /** O **grafo** da aba, não a tela raiz: é ele que carrega a pilha. */
    val grafo: Any,
) {
    HOJE(R.string.destino_hoje, AbaHoje),
    PROGRESSO(R.string.destino_progresso, AbaProgresso),
    GRUPO(R.string.destino_grupo, AbaGrupo),

    /**
     * A quarta aba, da Fase 4 (docs/03 §2). **Não aparece sozinha**: a porta é o primeiro
     * fechamento semanal (`F4-T09`), e até lá a barra tem três destinos.
     */
    POKEDEX(R.string.destino_pokedex, AbaPokedex);

    val iconeDeContorno
        @Composable get() = when (this) {
            HOJE -> Icons.Outlined.Home
            PROGRESSO -> Icons.Outlined.DateRange
            GRUPO -> Icons.Outlined.Person
            POKEDEX -> IconesDaPokedex.Contorno
        }

    val iconePreenchido
        @Composable get() = when (this) {
            HOJE -> Icons.Filled.Home
            PROGRESSO -> Icons.Filled.DateRange
            GRUPO -> Icons.Filled.Person
            POKEDEX -> IconesDaPokedex.Preenchido
        }
}

/** O anel de foco, afastado [FolgaDoFoco] da borda do item, com `raio-controle`. */
private fun Modifier.anelDeFoco(cor: Color): Modifier = drawWithContent {
    drawContent()
    val traco = LarguraDoFoco.toPx()
    val recuo = FolgaDoFoco.toPx() + traco / 2
    drawRoundRect(
        color = cor,
        topLeft = Offset(recuo, recuo),
        size = Size(size.width - 2 * recuo, size.height - 2 * recuo),
        cornerRadius = CornerRadius(Raios.controle.toPx()),
        style = Stroke(width = traco),
    )
}

/**
 * O contorno da barra com o berço: a borda de cima desce num arco em volta da bola e
 * volta, concordada por dois arcos de [RaioDaConcordancia] (aprovado em 27/10/2026, a
 * opção `Berço` dos esboços).
 *
 * O berço é um círculo de raio `R` (bola + folga) com centro em `(cx, d)`, `d` abaixo da
 * borda. Cada concordância é um círculo de raio `f` tangente à borda (centro em `y = f`)
 * e tangente por fora ao berço, a `R + f` do centro dele.
 *
 * @param fechado `false` devolve só o contorno de cima, que é onde passa o filete.
 */
internal fun DrawScope.contornoDoBerco(fechado: Boolean): Path {
    val cx = size.width / 2
    val d = DescidaDaBola.toPx()
    val r = (DiametroDaBola / 2 + FolgaDoBerco).toPx()
    val f = RaioDaConcordancia.toPx()
    val dx = sqrt((r + f) * (r + f) - (f - d) * (f - d))
    // O ponto em que a concordância da esquerda encosta no berço.
    val tx = cx - dx * r / (r + f)
    val ty = d + (f - d) * r / (r + f)
    val anguloNaConcordancia = Math.toDegrees(atan2((ty - f).toDouble(), (tx - (cx - dx)).toDouble())).toFloat()
    val anguloNoBerco = Math.toDegrees(atan2((ty - d).toDouble(), (tx - cx).toDouble())).toFloat()

    return Path().apply {
        moveTo(0f, 0f)
        lineTo(cx - dx, 0f)
        arcTo(Rect(Offset(cx - dx, f), f), -90f, anguloNaConcordancia + 90f, false)
        arcTo(Rect(Offset(cx, d), r), anguloNoBerco, 180f - 2 * anguloNoBerco, false)
        arcTo(Rect(Offset(cx + dx, f), f), 180f - anguloNaConcordancia, 90f + anguloNaConcordancia, false)
        lineTo(size.width, 0f)
        if (fechado) {
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

/**
 * A barra inferior: as abas dos dois lados e a **Pokébola no centro**, que abre o registro
 * de corrida de qualquer aba (27/10/2026). Ela substitui o FAB da Home, que só existia lá.
 *
 * **As abas não têm rótulo** (aprovado nos esboços de 27/10/2026). O nome inteiro da aba
 * continua indo para o TalkBack no ícone. O estado selecionado fica em dois canais:
 *
 * | Canal | Não selecionado | Selecionado |
 * |---|---|---|
 * | Ícone | contorno, `texto-secundario` | preenchido, `destaque` |
 * | Indicador | nenhum | 60 × 32dp em `destaque-suave`, `raio-controle` |
 *
 * **As abas se dividem em duas metades iguais**, com o berço no meio. Com três destinos
 * (antes da Pokédex), `Grupo` fica sozinho no centro da metade da direita.
 *
 * **O indicador do Material está desligado e redesenhado no slot do ícone.** O
 * `NavigationBarItem` desenha o indicador sempre em pílula e não aceita outra forma, e
 * o design system pede `raio-controle`, sem pílula.
 *
 * **O toque também é do tema, e não do item.** O `NavigationBarItem` fixa um ripple em
 * pílula no lugar do indicador, para pressionado e foco. Ele sai por
 * `LocalRippleConfiguration provides null`, e o item inteiro recebe o `LocalIndication`
 * do tema: `estado-pressionado` no retângulo do alvo (docs/02 §4.2). O foco vira o anel
 * de 2dp em `foco`, afastado 2dp da borda do item **para dentro**. O item continua sendo
 * o do M3, e com ele a semântica de aba (`Role.Tab` e `selected`).
 *
 * **A bola passa da borda de cima da barra.** A barra mede só a faixa das abas, e é essa
 * altura que o `Scaffold` desconta do conteúdo; a bola é desenhada 26dp acima, sobre ele,
 * como o FAB era. O berço mostra o que está atrás da barra, que é o `containerColor` do
 * `Scaffold`: a casca o pinta com a cor da página da tela aberta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraDeDestinos(
    destinoAtual: DestinoDeTopo,
    aoEscolher: (DestinoDeTopo) -> Unit,
    aoRegistrarCorrida: () -> Unit,
    modifier: Modifier = Modifier,
    destinos: List<DestinoDeTopo> = DestinoDeTopo.entries.filter { it != DestinoDeTopo.POKEDEX },
    comBadge: Set<DestinoDeTopo> = emptySet(),
) {
    val esquema = MaterialTheme.colorScheme
    val superficie = esquema.surface
    val filete = esquema.outlineVariant
    val sombra = esquema.onSurface.copy(alpha = OpacidadeDaSombraDaBarra)
    val foco = LocalCoresPokerun.current.foco
    val indicacao = LocalIndication.current
    val daEsquerda = destinos.take((destinos.size + 1) / 2)
    val daDireita = destinos.drop(daEsquerda.size)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val forma = contornoDoBerco(fechado = true)
                // A sombra sobe da forma inteira. `setShadowLayer` em hardware vale do API 28
                // em diante; antes disso a barra fica só com o filete, que já delimita.
                drawIntoCanvas { canvas ->
                    val pincel = Paint().apply { color = superficie }
                    pincel.asFrameworkPaint().setShadowLayer(
                        DesfoqueDaSombraDaBarra.toPx(),
                        0f,
                        -AlturaDoFilete.toPx() * 4,
                        sombra.toArgb(),
                    )
                    canvas.drawPath(forma, pincel)
                }
                drawPath(contornoDoBerco(fechado = false), filete, style = Stroke(AlturaDoFilete.toPx()))
            },
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .fillMaxWidth()
                    .height(AlturaDaBarra)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    daEsquerda.forEach { destino ->
                        ItemDaBarra(destino, destino == destinoAtual, destino in comBadge, foco, indicacao, aoEscolher)
                    }
                }
                Spacer(Modifier.width(VaoDoBerco))
                Row(modifier = Modifier.weight(1f)) {
                    daDireita.forEach { destino ->
                        ItemDaBarra(destino, destino == destinoAtual, destino in comBadge, foco, indicacao, aoEscolher)
                    }
                }
            }
        }

        Pokebola(
            aoTocar = aoRegistrarCorrida,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = DescidaDaBola - DiametroDaBola / 2),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.ItemDaBarra(
    destino: DestinoDeTopo,
    selecionado: Boolean,
    comBadge: Boolean,
    foco: Color,
    indicacao: Indication,
    aoEscolher: (DestinoDeTopo) -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val nomeInteiro = stringResource(destino.rotulo)
    val novidade = stringResource(R.string.destino_novo)
    val fonte = remember { MutableInteractionSource() }
    val focado by fonte.collectIsFocusedAsState()

    NavigationBarItem(
        selected = selecionado,
        onClick = { aoEscolher(destino) },
        interactionSource = fonte,
        alwaysShowLabel = false,
        modifier = Modifier
            .then(if (focado) Modifier.anelDeFoco(foco) else Modifier)
            .indication(fonte, indicacao),
        icon = {
            Box(
                modifier = Modifier
                    .size(LarguraDoIndicador, AlturaDoIndicador)
                    .background(
                        color = if (selecionado) esquema.secondaryContainer else Color.Transparent,
                        shape = FormaControle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(
                    badge = {
                        // O destino novo leva o ponto até o primeiro acesso (docs/03 §3.20.1).
                        if (comBadge) Badge(containerColor = esquema.primary)
                    },
                ) {
                    Icon(
                        imageVector = if (selecionado) destino.iconePreenchido else destino.iconeDeContorno,
                        // Sem rótulo na barra, o nome da aba mora aqui, inteiro, para o TalkBack.
                        contentDescription = if (comBadge) "$nomeInteiro, $novidade" else nomeInteiro,
                        modifier = Modifier.size(Tamanhos.icone),
                    )
                }
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = esquema.primary,
            unselectedIconColor = esquema.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        ),
    )
}

/** O quanto a bola encolhe enquanto o dedo está nela. */
private const val EscalaPressionada = 0.93f

/** O balanço de captura: três idas e voltas que morrem, girando pela base da bola. */
private const val DuracaoDoBalanco = 3 * Movimento.Tempo.PADRAO

/**
 * A Pokébola da barra: o botão de registrar corrida.
 *
 * O toque navega na hora; o balanço de captura e o clique do botão acontecem enquanto a
 * tela do registro entra, porque a barra fica fora do `NavHost` e continua visível. Com
 * movimento reduzido, nada balança.
 *
 * Desenhada inteira com os tokens do tema: metade de cima em `destaque`, de baixo em
 * `superficie`, faixa e contorno em tinta. O volume é um degradê de cada metade em
 * direção a `destaque-pressionado` e `borda`, com um reflexo branco no alto à esquerda.
 */
@Composable
private fun Pokebola(aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    val cores = LocalCoresPokerun.current
    val reduzido = LocalMovimentoReduzido.current
    val escopo = rememberCoroutineScope()
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val focado by fonte.collectIsFocusedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) EscalaPressionada else 1f,
        animationSpec = tween(durationMillis = Movimento.Duracao.TOQUE),
        label = "toque na Pokébola",
    )
    val giro = remember { Animatable(0f) }
    val luz = remember { Animatable(0f) }
    val descricao = stringResource(R.string.home_registrar_corrida)

    val branco = esquema.surface
    val tinta = esquema.onSurface
    val destaque = esquema.primary
    val topoClaro = lerp(destaque, branco, 0.3f)
    val topoEscuro = lerp(cores.destaquePressionado, tinta, 0.25f)
    val baseMeia = cores.superficieAfundada
    val baseEscura = esquema.outlineVariant
    val anelDoBotao = esquema.outlineVariant

    Box(
        modifier = modifier
            .size(DiametroDaBola)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                rotationZ = giro.value
                transformOrigin = TransformOrigin(0.5f, 0.92f)
            }
            .shadow(Elevacao.flutuante, CircleShape, ambientColor = tinta, spotColor = tinta)
            .clickable(
                interactionSource = fonte,
                indication = null,
                role = Role.Button,
            ) {
                aoTocar()
                if (!reduzido) {
                    escopo.launch {
                        giro.snapTo(0f)
                        giro.animateTo(
                            0f,
                            keyframes {
                                durationMillis = DuracaoDoBalanco
                                -18f at (DuracaoDoBalanco * 0.14f).toInt()
                                14f at (DuracaoDoBalanco * 0.34f).toInt()
                                -9f at (DuracaoDoBalanco * 0.54f).toInt()
                                5f at (DuracaoDoBalanco * 0.74f).toInt()
                            },
                        )
                    }
                }
                escopo.launch {
                    luz.snapTo(1f)
                    luz.animateTo(0f, tween(durationMillis = Movimento.Tempo.LENTO))
                }
            }
            .semantics { contentDescription = descricao },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = size.minDimension / 2
            // As medidas abaixo são de uma bola de raio 30: `u` é a unidade dela.
            val u = r / 30f
            val centro = Offset(r, r)
            val circulo = Path().apply { addOval(Rect(centro, r)) }

            clipPath(circulo) {
                drawRect(
                    Brush.radialGradient(
                        0f to branco, 0.6f to baseMeia, 1f to baseEscura,
                        center = Offset(size.width * 0.4f, size.height * 0.1f),
                        radius = size.width * 0.95f,
                    ),
                )
                clipRect(bottom = r) {
                    drawRect(
                        Brush.radialGradient(
                            0f to topoClaro, 0.45f to destaque, 1f to topoEscuro,
                            center = Offset(size.width * 0.34f, size.height * 0.22f),
                            radius = size.width * 0.85f,
                        ),
                    )
                }
                val faixa = 5.2f * u
                drawRect(tinta, topLeft = Offset(0f, r - faixa / 2), size = Size(size.width, faixa))
            }
            drawCircle(tinta, radius = 29f * u, center = centro, style = Stroke(2.2f * u))

            // O reflexo: uma elipse branca inclinada, no alto à esquerda.
            rotate(-32f, pivot = Offset(r - 11f * u, r - 17f * u)) {
                drawOval(
                    branco.copy(alpha = 0.45f),
                    topLeft = Offset(r - 19f * u, r - 21f * u),
                    size = Size(16f * u, 8f * u),
                )
            }

            drawCircle(tinta, radius = 10.4f * u, center = centro)
            drawCircle(
                Brush.radialGradient(
                    0f to branco, 1f to baseMeia,
                    center = Offset(r - 2.6f * u, r - 2.2f * u),
                    radius = 7.4f * u,
                ),
                radius = 7.4f * u,
                center = centro,
            )
            // O miolo do botão acende em `destaque` no toque, e apaga.
            drawCircle(lerp(branco, destaque, luz.value), radius = 4.2f * u, center = centro)
            drawCircle(anelDoBotao, radius = 4.2f * u, center = centro, style = Stroke(1f * u))

            if (focado) {
                drawCircle(
                    cores.foco,
                    radius = r - LarguraDoFoco.toPx() / 2,
                    center = centro,
                    style = Stroke(LarguraDoFoco.toPx()),
                )
            }
        }
    }
}
