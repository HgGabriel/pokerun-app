package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.geometry.Offset
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
