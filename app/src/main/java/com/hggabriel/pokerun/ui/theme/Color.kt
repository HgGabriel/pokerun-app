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
 * Exclusivo para avisos funcionais. Nunca decorativo. 9.63:1 / 10.14:1. L 28.
 *
 * Era #B3341F, que passava AA a 5.82:1 e parecia correto. Medido por simulação de
 * dicromacia (matriz de Viénot 1999), colapsava contra [Leitura]: sob deuteranopia
 * o âmbar vira #777700 e aquele tijolo virava #6E6E0E, ΔE 7,1 — abaixo de 10 duas
 * cores são a mesma. Os dois tokens mais opostos do sistema eram os dois mais
 * próximos em matiz (35° contra 9°), e o banner de risco de lesão (RN-30) ficava
 * idêntico ao acento de ação principal. Deuteranomalia atinge ~8% dos homens; num
 * grupo de oito, é provável.
 *
 * #7E1D12 separa por VALOR, não só por matiz, que é o único jeito de sobreviver à
 * dicromacia: ΔE 24,6 sob deuteranopia e 36,4 sob protanopia. De quebra o contraste
 * sobe de 5.82:1 para 9.63:1. **Não voltar para um vermelho mais claro sem remedir
 * esse par** (docs/02 §2.1).
 *
 * Cor nunca é o único canal do aviso: todo uso carrega junto ícone de triângulo em
 * traço de 1,5dp, filete vertical de 3dp e rótulo em mono caixa alta (docs/02 §2.4).
 */
val Alerta = Color(0xFF7E1D12)

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
 * Fundo de bottom sheet e diálogo: [Tinta] a 55%. Sem ele o Material 3 usa preto puro.
 *
 * Não confundir com `colorScheme.scrim`, que recebe [Tinta] sólida porque o M3 aplica
 * a própria opacidade por cima. Este é para quando a opacidade for desenhada à mão.
 */
val Scrim = Tinta.copy(alpha = 0.55f)

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
