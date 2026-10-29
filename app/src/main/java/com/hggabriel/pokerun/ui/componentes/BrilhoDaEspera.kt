package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.FormaDado
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento

/*
 * A espera com brilho (29/10/2026): o esqueleto clássico, com uma faixa de luz que varre os
 * blocos da esquerda para a direita. Pedido do humano para o verso da espécie, onde o pulso
 * não dizia "informação carregando" (docs/design.md §12). O resto do app segue no pulso de
 * `Modifier.esqueleto`.
 */

/** Um varrimento da faixa: 12 batidas, o mesmo ciclo do pulso da espera. */
private const val BATIDAS_DO_BRILHO = 12

/** A largura da faixa, em fração da largura da tela. */
private const val LARGURA_DA_FAIXA = 0.45f

private val LocalFaseDoBrilho = staticCompositionLocalOf<State<Float>?> { null }

/**
 * O relógio do brilho: todos os blocos dentro de [conteudo] varrem juntos, como uma faixa só
 * que atravessa a tela. Com movimento reduzido, os blocos ficam parados.
 */
@Composable
fun ComBrilhoDeEspera(conteudo: @Composable () -> Unit) {
    val fase = if (LocalMovimentoReduzido.current) {
        null
    } else {
        rememberInfiniteTransition(label = "brilho da espera").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = Movimento.Tempo.batidas(BATIDAS_DO_BRILHO), easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "fase",
        )
    }
    CompositionLocalProvider(LocalFaseDoBrilho provides fase) { conteudo() }
}

/**
 * Um bloco da espera: a forma do conteúdo que vai chegar, em [cor], com a faixa de [brilho]
 * passando por cima. A faixa é medida na tela inteira, e não no bloco: blocos vizinhos acendem
 * em sequência, como no esqueleto clássico. Fora de [ComBrilhoDeEspera], o bloco fica parado.
 */
@Composable
fun Modifier.esperaComBrilho(
    forma: Shape = FormaDado,
    cor: Color = LocalCoresPokerun.current.superficieAfundada,
    brilho: Color = MaterialTheme.colorScheme.surface,
): Modifier {
    val fase = LocalFaseDoBrilho.current
    val largura = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var x by remember { mutableFloatStateOf(0f) }
    return this
        .onGloballyPositioned { x = it.positionInRoot().x }
        .clip(forma)
        .background(cor)
        .drawWithContent {
            drawContent()
            val p = fase?.value ?: return@drawWithContent
            val faixa = largura * LARGURA_DA_FAIXA
            val centro = -faixa + p * (largura + 2 * faixa) - x
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to brilho.copy(alpha = 0.75f),
                    1f to Color.Transparent,
                    start = Offset(centro - faixa / 2, 0f),
                    end = Offset(centro + faixa / 2, size.height / 2),
                ),
            )
        }
}
