package com.example.pokerun.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens do PokéRun — direção "Pokédex como ficha impressa de campo".
 *
 * Tema claro fixo. Não existe variante escura nem cor dinâmica: ver [PokerunTheme].
 *
 * Contraste medido (WCAG 2.1) contra as DUAS superfícies, papel e painel.
 * Todo token de texto passa AA (4.5:1) nas duas. Não escurecer o papel nem
 * clarear os textos sem remedir.
 */

/** Fundo da aplicação. 16.1:1 contra `Tinta`. */
val Papel = Color(0xFFFAF9F7)

/** Cards, superfícies elevadas e contêineres. */
val Painel = Color(0xFFFFFFFF)

/** Divisores e contornos de card. Decorativo — não usar para delimitar controle. */
val Borda = Color(0xFFE2DFD9)

/** Contorno de componente interativo: input, chip, segmented button. 3.5:1 — piso de WCAG 1.4.11. */
val BordaForte = Color(0xFF8A8578)

/** Acento primário, âmbar queimado. Posição na Pokédex, marcas superadas, ação principal. 4.8:1 / 5.1:1 */
val Leitura = Color(0xFFA25E00)

/** Séries de dado secundário: FC, cadência, comparativos. 5.6:1 / 5.9:1 */
val Sinal = Color(0xFF1F6F6E)

/** Exclusivo para avisos funcionais. Nunca decorativo. 5.8:1 / 6.1:1 */
val Alerta = Color(0xFFB3341F)

/** Texto primário. 16.1:1 / 16.9:1 */
val Tinta = Color(0xFF1A1D26)

/** Rótulos, texto secundário e silhuetas de espécie não alcançada. 5.9:1 / 6.3:1 */
val TintaFraca = Color(0xFF5A6172)
