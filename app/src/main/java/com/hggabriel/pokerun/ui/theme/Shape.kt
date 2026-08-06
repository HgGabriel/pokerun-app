package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Instrumento, nÃ£o bolha: 4dp em cards e painÃ©is, 2dp em chips e tags,
 * 0dp em barras de dado e grÃ¡ficos.
 */
val PokerunShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),  // chips, tags
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),      // cards, painÃ©is
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

/** Barras de dado, grÃ¡ficos e marcas da escada. Sem arredondamento. */
val FormaDado = RoundedCornerShape(0.dp)
