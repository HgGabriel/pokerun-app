package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * Estado pressionado como **retângulo**, não como ripple circular (docs/02 §4.2).
 *
 * Num sistema sem sombra, sem elevação e com raio de 2 a 4dp, o feedback de toque é
 * a única coisa que diz que um elemento é tocável. O ripple padrão deriva do
 * `primary` e desenharia um círculo âmbar espalhado sobre o card branco, que é o
 * oposto da estética de instrumento.
 *
 * Trocado uma vez só, em [PokerunTheme], por `LocalIndication`. Nenhuma tela monta
 * a sua.
 */
internal class IndicacaoRetangular(private val cor: Color) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        NoDePressao(interactionSource, cor)

    override fun equals(other: Any?): Boolean = other is IndicacaoRetangular && other.cor == cor

    override fun hashCode(): Int = cor.hashCode()
}

private class NoDePressao(
    private val fonte: InteractionSource,
    private val cor: Color,
) : Modifier.Node(), DrawModifierNode {

    /**
     * Contagem, não booleano: um segundo dedo que desce e sobe emitiria um Release
     * que apagaria a camada com o primeiro dedo ainda na tela.
     */
    private var pressoes = 0
    private var pressionado = false

    override fun onAttach() {
        coroutineScope.launch {
            fonte.interactions.collect { interacao ->
                when (interacao) {
                    is PressInteraction.Press -> pressoes++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressoes--
                }
                val agora = pressoes > 0
                if (pressionado != agora) {
                    pressionado = agora
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // A camada vai por baixo do conteúdo e por cima da superfície do pai, e é
        // clipada pela forma de quem a hospeda — o retângulo de 4dp da Ficha.
        if (pressionado) drawRect(cor)
        drawContent()
    }
}

/** A instância única do app, montada sobre o token de docs/02 §2.6. */
internal val IndicacaoDeToque = IndicacaoRetangular(CoresPokerunClaro.leituraToque)
