package com.hggabriel.pokerun.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/**
 * Até onde o rótulo **dentro de uma figura** acompanha a fonte do sistema: a data sob a
 * estrada, o número sob a pílula, o balão do valor. Ele mora numa posição medida (a coluna,
 * o traço da semana), e a 200% a 320dp ele passava por cima do vizinho (`11 ouhoje`). O
 * mesmo dado está inteiro na descrição falada da figura, e o texto de fora dela cresce
 * normalmente. É a mesma solução da barra de destinos (`BarraDeDestinos`, 1,5).
 */
internal const val ESCALA_MAXIMA_NA_FIGURA = 1.3f

/** [estilo] com o tamanho que pararia em [ESCALA_MAXIMA_NA_FIGURA]. */
@Composable
internal fun naFigura(estilo: TextStyle): TextStyle {
    val escala = LocalDensity.current.fontScale
    if (escala <= ESCALA_MAXIMA_NA_FIGURA || estilo.fontSize.type != TextUnitType.Sp) return estilo
    val fator = ESCALA_MAXIMA_NA_FIGURA / escala
    return estilo.copy(
        fontSize = (estilo.fontSize.value * fator).sp,
        lineHeight = if (estilo.lineHeight.type == TextUnitType.Sp) (estilo.lineHeight.value * fator).sp else estilo.lineHeight,
    )
}

/** A fonte do sistema passou do limiar em que a tela empilha (1,3, o mesmo das outras telas). */
@Composable
internal fun fonteAmpliada(): Boolean = LocalDensity.current.fontScale > ESCALA_MAXIMA_NA_FIGURA
