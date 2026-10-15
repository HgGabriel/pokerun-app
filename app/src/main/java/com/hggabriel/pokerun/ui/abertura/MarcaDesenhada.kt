package com.hggabriel.pokerun.ui.abertura

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * A marca no canvas de 108 do adaptive icon: a Pokébola-cronômetro (docs/02 §2.7).
 *
 * São os mesmos números dos XML do launcher e da splash, que foram gerados deles. A
 * abertura desenha com estes valores para que o primeiro quadro dela caia exatamente
 * sobre o último quadro da splash do sistema.
 */
internal object GeometriaDaMarca {
    const val CANVAS = 108f
    const val CX = 54f
    const val CY = 57.5f
    const val R = 21f
    const val TRACO = 3.5f
    const val FAIXA = 5f
    const val BOTAO = 7.5f

    /** Haste e botão de disparo da coroa, no eixo. */
    val haste = Rect(51f, 30.5f, 57f, 37.5f)
    val coroa = Rect(47f, 26f, 61f, 31f)
    const val RAIO_COROA = 2f

    /** Botão de volta, girado 45 graus em torno do próprio centro. */
    val lateral = Rect(67.263f, 37.737f, 73.263f, 44.737f)
    const val RAIO_LATERAL = 1.5f
}

/**
 * Desenha a marca com o canto do canvas de 108 em [origem] e [u] px por unidade.
 *
 * A tampa (metade de cima, coroa e botão de volta) gira [giroDaTampa] graus em torno da
 * dobradiça, na ponta esquerda da faixa. [miolo] desenha o que sai da bola aberta: fica
 * na frente da concha e atrás da faixa, que cobre a base dele.
 */
internal fun DrawScope.desenharMarca(
    origem: Offset,
    u: Float,
    destaque: Color,
    tinta: Color,
    fundo: Color,
    giroDaTampa: Float = 0f,
    miolo: DrawScope.() -> Unit = {},
) {
    val g = GeometriaDaMarca
    fun p(x: Float, y: Float) = Offset(origem.x + x * u, origem.y + y * u)
    fun r(ret: Rect) = Rect(p(ret.left, ret.top), p(ret.right, ret.bottom))
    val bola = Rect(p(g.CX - g.R, g.CY - g.R), Size(2 * g.R * u, 2 * g.R * u))
    val contorno = Stroke(width = g.TRACO * u)
    val centro = p(g.CX, g.CY)

    // A tampa primeiro: a faixa e o botão passam por cima dela, como no ícone.
    rotate(giroDaTampa, pivot = p(g.CX - g.R, g.CY)) {
        val haste = r(g.haste)
        drawRect(tinta, haste.topLeft, haste.size)
        val coroa = r(g.coroa)
        drawRoundRect(tinta, coroa.topLeft, coroa.size, CornerRadius(g.RAIO_COROA * u))
        val lateral = r(g.lateral)
        rotate(45f, pivot = lateral.center) {
            drawRoundRect(tinta, lateral.topLeft, lateral.size, CornerRadius(g.RAIO_LATERAL * u))
        }
        drawArc(destaque, 180f, 180f, useCenter = true, topLeft = bola.topLeft, size = bola.size)
        drawArc(tinta, 180f, 180f, useCenter = false, topLeft = bola.topLeft, size = bola.size, style = contorno)
    }

    drawArc(fundo, 0f, 180f, useCenter = true, topLeft = bola.topLeft, size = bola.size)
    miolo()
    drawArc(tinta, 0f, 180f, useCenter = false, topLeft = bola.topLeft, size = bola.size, style = contorno)
    drawRect(tinta, p(g.CX - g.R, g.CY - g.FAIXA / 2), Size(2 * g.R * u, g.FAIXA * u))
    drawCircle(tinta, g.BOTAO * u, centro)
    drawCircle(fundo, (g.BOTAO - g.TRACO) * u, centro)
}
