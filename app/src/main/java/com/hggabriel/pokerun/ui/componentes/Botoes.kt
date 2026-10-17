package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento

/** Altura de todo botão (docs/02 §4, `Botao.md`). Acima do piso de toque de 48dp. */
internal val AlturaDoBotao = 52.dp

/** O botão pressionado encolhe para 97% (docs/02 §4.2). */
internal const val EscalaPressionada = 0.97f

/** Desabilitado é o botão inteiro a 38%, e não a paleta cinza do M3 (docs/02 §4.2). */
internal const val OpacidadeDoBotaoDesabilitado = 0.38f

private val LarguraDoContorno = 1.dp

/** A escala do botão em cada estado. Separada do `@Composable` para o teste ler a regra. */
internal fun escalaDoBotao(pressionado: Boolean): Float = if (pressionado) EscalaPressionada else 1f

/*
 * Os botões do sistema. Nenhuma tela chama `Button`, `OutlinedButton`, `TextButton`
 * nem `FloatingActionButton` direto: `BotoesTest` varre `ui/telas`.
 *
 * O M3 desenha o botão em pílula e não lê slot de `Shapes` para ele (docs/02 §4).
 * Daqui sai `FormaControle` (12dp) e 52dp de altura, uma vez só.
 *
 * O M3 também fixa `ripple()` no `Surface` do botão e ignora o `LocalIndication` do
 * tema. O ripple sai por `LocalRippleConfiguration provides null`, e o pressionado
 * passa a ser o de docs/02 §4.2: escala de 0,97 em `duracao-toque` e, conforme a
 * variante, o primário escurecendo para `destaque-pressionado` ou a camada de
 * `estado-pressionado` no retângulo do alvo.
 *
 * `ocupado` não é `desabilitado`. O botão recusa o toque (`enabled = false`, e o
 * TalkBack sabe), mas mantém a cor: um indicador a 38% é o que ninguém enxerga.
 */

/** A ação principal da tela, em `destaque`. Um por tela. */
@Composable
fun BotaoPrimario(
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    ocupado: Boolean = false,
    conteudo: @Composable RowScope.() -> Unit,
) {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val esquema = MaterialTheme.colorScheme
    // Fundo opaco: a camada de `estado-pressionado` ficaria por baixo dele. No
    // primário o sinal do toque é o escurecimento.
    val fundo by animateColorAsState(
        targetValue = if (pressionado) LocalCoresPokerun.current.destaquePressionado else esquema.primary,
        animationSpec = especificacaoDoToque(),
        label = "fundo do botão primário",
    )

    SemRipple {
        Button(
            onClick = aoTocar,
            enabled = habilitado && !ocupado,
            shape = FormaControle,
            colors = ButtonDefaults.buttonColors(
                containerColor = fundo,
                contentColor = esquema.onPrimary,
                disabledContainerColor = fundo,
                disabledContentColor = esquema.onPrimary,
            ),
            interactionSource = fonte,
            modifier = modifier.estadoDoBotao(pressionado, apagado = !habilitado && !ocupado),
            content = conteudo,
        )
    }
}

/** A ação secundária: contorno em `borda-controle`, que passa os 3:1 de controle. */
@Composable
fun BotaoSecundario(
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    conteudo: @Composable RowScope.() -> Unit,
) {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val esquema = MaterialTheme.colorScheme

    SemRipple {
        OutlinedButton(
            onClick = aoTocar,
            enabled = habilitado,
            shape = FormaControle,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = esquema.onSurface,
                disabledContentColor = esquema.onSurface,
            ),
            // O contorno não troca de cor ao desabilitar: os 38% valem para o botão
            // inteiro.
            border = BorderStroke(LarguraDoContorno, esquema.outline),
            interactionSource = fonte,
            modifier = modifier
                .estadoDoBotao(pressionado, apagado = !habilitado)
                .camadaDeToque(fonte, FormaControle),
            content = conteudo,
        )
    }
}

/** Botão só de texto: ações de diálogo e ações de menor peso. */
@Composable
fun BotaoDeTexto(
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    conteudo: @Composable RowScope.() -> Unit,
) {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val esquema = MaterialTheme.colorScheme

    SemRipple {
        TextButton(
            onClick = aoTocar,
            enabled = habilitado,
            shape = FormaControle,
            colors = ButtonDefaults.textButtonColors(
                contentColor = esquema.primary,
                disabledContentColor = esquema.primary,
            ),
            interactionSource = fonte,
            modifier = modifier
                .estadoDoBotao(pressionado, apagado = !habilitado)
                .camadaDeToque(fonte, FormaControle),
            content = conteudo,
        )
    }
}

/**
 * O FAB: `raio-card` pelo slot `shapes.large` e `elevacao-flutuante` ([Elevacao]).
 *
 * A sombra sai do `Modifier.shadow` com cor em `texto-primario`, e não da elevação do
 * FAB, que pinta a sombra em preto puro. Os 56dp são o tamanho padrão do M3.
 */
@Composable
fun BotaoFlutuante(
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    val fonte = remember { MutableInteractionSource() }
    val pressionado by fonte.collectIsPressedAsState()
    val esquema = MaterialTheme.colorScheme
    val forma = MaterialTheme.shapes.large
    val fundo by animateColorAsState(
        targetValue = if (pressionado) LocalCoresPokerun.current.destaquePressionado else esquema.primaryContainer,
        animationSpec = especificacaoDoToque(),
        label = "fundo do FAB",
    )

    SemRipple {
        FloatingActionButton(
            onClick = aoTocar,
            shape = forma,
            containerColor = fundo,
            contentColor = esquema.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            interactionSource = fonte,
            modifier = modifier
                .escalaDoToque(pressionado)
                .sombraFlutuante(forma),
            content = conteudo,
        )
    }
}

// ---------------------------------------------------------------------------
// Peças comuns
// ---------------------------------------------------------------------------

/**
 * Tira o ripple dos componentes do M3 aqui dentro. O toque fica por conta do botão.
 * `LocalRippleConfiguration` ainda é experimental no material3 1.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SemRipple(conteudo: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides null, content = conteudo)
}

@Composable
private fun <T> especificacaoDoToque(): FiniteAnimationSpec<T> =
    Movimento.especificacao(Movimento.Duracao.TOQUE, Movimento.Curva.padrao, LocalMovimentoReduzido.current)

@Composable
private fun Modifier.escalaDoToque(pressionado: Boolean): Modifier {
    val escala by animateFloatAsState(
        targetValue = escalaDoBotao(pressionado),
        animationSpec = especificacaoDoToque(),
        label = "escala do botão",
    )
    return graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

/** Altura, escala e os 38% do desabilitado. */
@Composable
private fun Modifier.estadoDoBotao(pressionado: Boolean, apagado: Boolean): Modifier =
    heightIn(min = AlturaDoBotao)
        .escalaDoToque(pressionado)
        .graphicsLayer { alpha = if (apagado) OpacidadeDoBotaoDesabilitado else 1f }

/**
 * `estado-pressionado` no retângulo do alvo, pelo `LocalIndication` do tema. Só serve
 * para botão de fundo transparente: a camada é desenhada por baixo do conteúdo.
 */
@Composable
internal fun Modifier.camadaDeToque(fonte: MutableInteractionSource, forma: Shape): Modifier =
    clip(forma).indication(fonte, LocalIndication.current)

@Composable
private fun Modifier.sombraFlutuante(forma: CornerBasedShape): Modifier {
    val tinta = MaterialTheme.colorScheme.onSurface
    return shadow(Elevacao.flutuante, forma, clip = false, ambientColor = tinta, spotColor = tinta)
}
