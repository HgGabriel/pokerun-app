package com.hggabriel.pokerun.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens do PokéRun — direção "Pokédex como ficha impressa de campo".
 *
 * Tema claro fixo. Não existe variante escura nem cor dinâmica: ver [PokerunTheme].
 *
 * Contraste medido (WCAG 2.1) contra as DUAS superfícies, papel e painel.
 * Todo token de texto passa AA (4.5:1) nas duas. Não escurecer o papel nem
 * clarear os textos sem remedir.
 *
 * Separação contra as cores de tipo da Pokédex (docs/02 §2.5): nenhum token
 * cromático de UI acima de L 45, nenhuma banda de tipo abaixo de L 47.
 *
 * NENHUMA TELA LÊ UM `val` DAQUI. O acesso é `MaterialTheme.colorScheme.*` ou
 * `LocalCoresPokerun.current.*` — há teste que falha se um import escapar.
 */

/** Fundo da aplicação. 16.00:1 contra `Tinta`. L 97. */
val Papel = Color(0xFFFAF9F7)

/** Cards, superfícies elevadas e contêineres. */
val Painel = Color(0xFFFFFFFF)

/** Divisores e contornos de card. Decorativo — não usar para delimitar controle. */
val Borda = Color(0xFFE2DFD9)

/** Contorno de componente interativo: input, chip, segmented button. 3.50:1 — piso de WCAG 1.4.11. */
val BordaForte = Color(0xFF8A8578)

/**
 * Acento primário, âmbar queimado. Posição na Pokédex, marcas superadas, ação principal.
 * 4.82:1 / 5.08:1. L 32.
 *
 * É a menor folga de contraste do sistema, 0,32 acima do piso AA. Não escurecer o
 * papel, não criar superfície intermediária entre papel e painel, e não usar sobre
 * `Borda` — os três quebram o piso (docs/02 §2.2).
 */
val Leitura = Color(0xFFA25E00)

/** Séries de dado secundário: FC, cadência, comparativos. 5.61:1 / 5.90:1. L 28. */
val Sinal = Color(0xFF1F6F6E)

/**
 * Exclusivo para avisos funcionais. Nunca decorativo. 5.8:1 / 6.1:1. L 41.
 *
 * `F0-T14` troca este hex por `#7E1D12`: sob deuteranopia o tijolo e o âmbar de
 * [Leitura] caem em ΔE 7,1, e o aviso de risco de lesão fica idêntico ao acento
 * de ação (docs/02 §2.1). O valor aqui é o vigente até aquela tarefa.
 */
val Alerta = Color(0xFFB3341F)

/** Texto primário. 16.00:1 / 16.83:1. */
val Tinta = Color(0xFF1A1D26)

/** Rótulos, texto secundário e silhuetas de espécie não alcançada. 5.89:1 / 6.20:1. */
val TintaFraca = Color(0xFF5A6172)

// --- Derivadas (docs/02 §2.6) ------------------------------------------------
//
// Estas não têm papel correspondente no ColorScheme do Material 3, e é por isso
// que seriam o primeiro caminho para uma tela inventar a sua. Chegam às telas
// pelo portador CoresPokerun, nunca por import.

/** Camada de estado pressionado sobre superfície clara. */
val LeituraToque = Leitura.copy(alpha = 0.12f)

/** Trilha da escada, fundo de barra, passo 1 do heatmap. Decorativo, nunca texto. */
val LeituraFraca = Color(0xFFEDE3D4)

/**
 * Quatro degraus de `Leitura` sobre papel para o heatmap de calendário, do vazio
 * ao máximo (docs/02 §2.6). Contraste sobre papel: 1.10, 1.22, 1.80, 2.86, 4.82.
 *
 * Quatro degraus de uma cor só não são distinguíveis por todo mundo: o heatmap
 * exige legenda e leitura por toque. Nunca o verde do GitHub.
 */
val HeatmapPassos = listOf(
    Color(0xFFF0EEEA),
    Color(0xFFEDE3D4),
    Color(0xFFD9B583),
    Color(0xFFBE8734),
    Leitura,
)
