package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * O ícone da aba Pokédex (docs/02 §4.4): **o aparelho Pokédex**, desde 27/10/2026. A
 * Pokébola passou a ser o registro de corrida no centro da barra, e duas Pokébolas na mesma
 * barra diriam duas vezes a mesma coisa.
 *
 * O aparelho tem a lente grande, a luz pequena e a tela. Inativo, só em contorno; ativo,
 * cheio, com as três peças vazadas (`EvenOdd`). A cor chega pelo `tint` do `Icon`, e por
 * isso o desenho é preto.
 *
 * Desenhado à mão porque `material-icons-extended` é dependência fora da lista do projeto.
 * Grade de 24, traço de 1,75.
 */
internal object IconesDaPokedex {

    private const val TRACO = 1.75f

    /** Inativo: o corpo, a lente, a luz e a tela, em contorno. */
    val Contorno: ImageVector by lazy {
        ImageVector.Builder(
            name = "PokedexContorno",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = TRACO,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                corpo()
                lente()
                tela()
            }
            path(fill = SolidColor(Color.Black)) { luz() }
        }.build()
    }

    /** Ativo: o corpo cheio, com a lente, a luz e a tela vazadas. */
    val Preenchido: ImageVector by lazy {
        ImageVector.Builder(
            name = "PokedexPreenchido",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                corpo()
                lente()
                luz()
                tela()
            }
        }.build()
    }

    /** O corpo: retângulo de 14 × 19 com cantos de 3,2. */
    private fun PathBuilder.corpo() = retanguloArredondado(5f, 2.5f, 14f, 19f, 3.2f)

    private fun PathBuilder.lente() = circulo(9.3f, 7f, 2.1f)

    private fun PathBuilder.luz() = circulo(14.6f, 6.2f, 0.9f)

    private fun PathBuilder.tela() = retanguloArredondado(7.6f, 11f, 8.8f, 6.4f, 1.4f)

    private fun PathBuilder.circulo(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx + r, cy)
        close()
    }

    private fun PathBuilder.retanguloArredondado(x: Float, y: Float, l: Float, a: Float, r: Float) {
        moveTo(x + r, y)
        horizontalLineTo(x + l - r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x + l, y + r)
        verticalLineTo(y + a - r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x + l - r, y + a)
        horizontalLineTo(x + r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x, y + a - r)
        verticalLineTo(y + r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x + r, y)
        close()
    }
}
