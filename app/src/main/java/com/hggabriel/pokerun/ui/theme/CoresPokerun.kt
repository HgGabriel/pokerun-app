package com.hggabriel.pokerun.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens do PokéRun sem papel correspondente no `ColorScheme` do Material 3
 * (docs/02 §2.6).
 *
 * O `ColorScheme` acomoda quase toda a paleta de §2.1, e é por ele que as telas
 * leem. Estes quatro não cabem em papel nenhum — e é exatamente por isso que
 * seriam o primeiro caminho para uma tela importar cor crua de [Color.kt].
 *
 * Regra que vem junto, e vale para todos os tokens, não só estes:
 * **nenhuma tela lê um `val` de cor.** O acesso é `MaterialTheme.colorScheme.*`
 * ou `LocalCoresPokerun.current.*`. Um `import ...ui.theme.Leitura` dentro de
 * `ui/telas` é erro de revisão, e o teste de `F0-T13` falha nele.
 *
 * Isso não é preparação para um tema escuro que talvez venha (D-13 continua
 * proibindo *shipar* dois temas): é o que mantém a troca de um token — como a de
 * `alerta` em `F0-T14` — sendo uma linha em vez de uma varredura por 20 telas.
 */
@Immutable
data class CoresPokerun(
    /** Camada de estado pressionado, retangular, sobre superfície clara. docs/02 §4.2. */
    val leituraToque: Color,
    /** Preenchimento decorativo de fundo de barra. Nunca texto. */
    val leituraFraca: Color,
    /** Trilha da escada. Em `papel` a faixa sumiria; em `borda` competiria com os divisores. */
    val escadaTrilha: Color,
    /** Os cinco degraus do heatmap de calendário, do vazio ao máximo. Exigem legenda. */
    val heatmap: List<Color>,
)

/** A única instância que existe hoje. O app entrega um tema claro (D-13). */
val CoresPokerunClaro = CoresPokerun(
    leituraToque = LeituraToque,
    leituraFraca = LeituraFraca,
    escadaTrilha = LeituraFraca,
    heatmap = HeatmapPassos,
)

val LocalCoresPokerun = staticCompositionLocalOf { CoresPokerunClaro }
