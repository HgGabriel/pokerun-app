package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Hierarquia por LARGURA, nÃ£o sÃ³ por peso: display expandido contra corpo normal.
 *
 * TODO(Fase 0): empacotar as fontes em res/font e trocar as trÃªs famÃ­lias abaixo.
 *   Display  â†’ Archivo Expanded 700   (minSdk 26 permite FontVariation para o eixo `wdth`;
 *                                      alternativa: instÃ¢ncia estÃ¡tica Archivo_Expanded-Bold)
 *   Corpo/UI â†’ IBM Plex Sans 400/500/600
 *   Dado     â†’ IBM Plex Mono 400/500
 * AtÃ© lÃ¡, Default e Monospace mantÃªm a escala correta com as mÃ©tricas erradas.
 */
private val DisplayFamily = FontFamily.Default
private val CorpoFamily = FontFamily.Default
private val DadoFamily = FontFamily.Monospace

val Typography = Typography(
    // Display XL â€” contagem regressiva, posiÃ§Ã£o na PokÃ©dex, distÃ¢ncia principal
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
    ),
    // Display L
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    // Heading
    headlineSmall = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    // Title
    titleLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // RÃ³tulo â€” caixa alta aplicada no componente, nÃ£o aqui
    labelMedium = TextStyle(
        fontFamily = DadoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.96.sp,  // +0.08em
    ),
)

/** Dado tabular: splits, tempos, pace, bpm, nÃºmero da PokÃ©dex. */
val EstiloDado = TextStyle(
    fontFamily = DadoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)
