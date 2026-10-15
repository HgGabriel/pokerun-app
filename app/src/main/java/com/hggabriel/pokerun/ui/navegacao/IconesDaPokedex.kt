package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * O ícone da aba Pokédex (docs/02 §4.4, "Ícone da aba Pokédex"): a Pokébola em **contorno**
 * quando inativa e com a **metade superior preenchida** quando ativa. A cor chega pelo
 * `tint` do `Icon` (a barra pinta o ativo em `destaque`), e por isso o desenho é preto.
 *
 * Desenhado à mão porque `material-icons-extended` é dependência fora da lista do projeto
 * (a mesma razão dos outros três glifos, `DestinoDeTopo`). Grade de 24, traço de 1,75.
 */
internal object IconesDaPokedex {

    private const val TRACO = 1.75f

    /** Inativa: o círculo, a faixa do meio e o botão central, só em contorno. */
    val Contorno: ImageVector by lazy {
        ImageVector.Builder(
            name = "PokedexContorno",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = TRACO, strokeLineCap = StrokeCap.Round) {
                circuloEExterno()
            }
        }.build()
    }

    /** Ativa: o mesmo desenho com a metade de cima preenchida. */
    val Preenchido: ImageVector by lazy {
        ImageVector.Builder(
            name = "PokedexPreenchido",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 12f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 12f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = TRACO, strokeLineCap = StrokeCap.Round) {
                circuloEExterno()
            }
        }.build()
    }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.circuloEExterno() {
        // O contorno: duas semicircunferências.
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
        arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
        close()
        // A faixa do meio.
        moveTo(2f, 12f)
        horizontalLineTo(22f)
        // O botão central.
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 9f, 12f)
        arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 15f, 12f)
        close()
    }
}
