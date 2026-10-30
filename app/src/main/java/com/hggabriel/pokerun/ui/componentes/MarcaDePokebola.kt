package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A Pokébola em marca d'água das peças em tinta: o círculo, a faixa do meio e o botão, só
 * em traço. Quem chama corta pela borda da peça, para ela ficar cortada no canto.
 */
internal fun DrawScope.marcaDePokebola(cor: Color, centro: Offset, raio: Float) {
    val traco = raio * 0.16f
    drawCircle(cor, raio, centro, style = Stroke(traco))
    drawLine(cor, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), traco)
    drawCircle(cor, raio * 0.33f, centro, style = Stroke(traco))
}

/** A Pokébola desenhada: metade em [vermelho], metade em [claro], a faixa e o botão em [tinta]. */
internal fun DrawScope.pokebola(
    centro: Offset,
    raio: Float,
    vermelho: Color,
    claro: Color,
    tinta: Color,
    botaoAceso: Float = 0f,
) {
    val canto = centro - Offset(raio, raio)
    val tamanho = Size(raio * 2, raio * 2)
    drawArc(vermelho, 180f, 180f, useCenter = true, topLeft = canto, size = tamanho)
    drawArc(claro, 0f, 180f, useCenter = true, topLeft = canto, size = tamanho)
    drawLine(tinta, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), raio * 0.14f)
    drawCircle(tinta, raio, centro, style = Stroke(raio * 0.09f))
    drawCircle(tinta, raio * 0.32f, centro)
    drawCircle(claro, raio * 0.2f, centro)
    if (botaoAceso > 0f) drawCircle(vermelho.copy(alpha = botaoAceso), raio * 0.14f, centro)
    drawCircle(Color.White.copy(alpha = 0.35f), raio * 0.16f, Offset(centro.x - raio * 0.45f, centro.y - raio * 0.5f))
}
