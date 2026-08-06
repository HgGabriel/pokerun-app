package com.hggabriel.pokerun.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens do PokÃ©Run â€” direÃ§Ã£o "PokÃ©dex como ficha impressa de campo".
 *
 * Tema claro fixo. NÃ£o existe variante escura nem cor dinÃ¢mica: ver [PokerunTheme].
 *
 * Contraste medido (WCAG 2.1) contra as DUAS superfÃ­cies, papel e painel.
 * Todo token de texto passa AA (4.5:1) nas duas. NÃ£o escurecer o papel nem
 * clarear os textos sem remedir.
 */

/** Fundo da aplicaÃ§Ã£o. 16.1:1 contra `Tinta`. */
val Papel = Color(0xFFFAF9F7)

/** Cards, superfÃ­cies elevadas e contÃªineres. */
val Painel = Color(0xFFFFFFFF)

/** Divisores e contornos de card. Decorativo â€” nÃ£o usar para delimitar controle. */
val Borda = Color(0xFFE2DFD9)

/** Contorno de componente interativo: input, chip, segmented button. 3.5:1 â€” piso de WCAG 1.4.11. */
val BordaForte = Color(0xFF8A8578)

/** Acento primÃ¡rio, Ã¢mbar queimado. PosiÃ§Ã£o na PokÃ©dex, marcas superadas, aÃ§Ã£o principal. 4.8:1 / 5.1:1 */
val Leitura = Color(0xFFA25E00)

/** SÃ©ries de dado secundÃ¡rio: FC, cadÃªncia, comparativos. 5.6:1 / 5.9:1 */
val Sinal = Color(0xFF1F6F6E)

/** Exclusivo para avisos funcionais. Nunca decorativo. 5.8:1 / 6.1:1 */
val Alerta = Color(0xFFB3341F)

/** Texto primÃ¡rio. 16.1:1 / 16.9:1 */
val Tinta = Color(0xFF1A1D26)

/** RÃ³tulos, texto secundÃ¡rio e silhuetas de espÃ©cie nÃ£o alcanÃ§ada. 5.9:1 / 6.3:1 */
val TintaFraca = Color(0xFF5A6172)
