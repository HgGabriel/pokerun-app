package com.hggabriel.pokerun.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de interface do PokéRun sem papel correspondente no `ColorScheme` do
 * Material 3 (docs/02 §2.6).
 *
 * O `ColorScheme` acomoda a maior parte da paleta de §2.1, e é por ele que as telas
 * leem. Estes não cabem em papel nenhum, e é por isso que seriam o primeiro caminho
 * para uma tela importar cor crua de [Color.kt]. As cores de tipo têm portador
 * próprio, [CoresDeTipo].
 *
 * Regra que vale para todos os tokens, não só estes: **nenhuma tela lê um `val` de
 * cor.** O acesso é `MaterialTheme.colorScheme.*`, `LocalCoresPokerun.current.*` ou
 * `LocalCoresDeTipo.current[tipo]`, e `TemaTest` falha num import cru.
 */
@Immutable
data class CoresPokerun(
    /** Trilha de barra, skeleton, espécie não alcançada, dia sem corrida, segmento selecionado. */
    val superficieAfundada: Color,
    /** Fundo da linha "você" e do destino ativo na barra inferior. Texto sobre ele em tinta. */
    val destaqueSuave: Color,
    /** Botão primário pressionado. */
    val destaquePressionado: Color,
    /** Risco de lesão e erro. Sempre com triângulo e rótulo em caixa alta (docs/02 §2.4). */
    val alerta: Color,
    /** Fundo do bloco de alerta. */
    val alertaSuave: Color,
    /** Confirmação. Sempre com check. */
    val sucesso: Color,
    /** Fundo do bloco de sucesso. */
    val sucessoSuave: Color,
    /** Camada de toque neutra, no retângulo do alvo (docs/02 §4.2). */
    val estadoPressionado: Color,
    /** Anel de foco de 2dp, deslocado 2dp. */
    val foco: Color,
    /** Preenchimento chapado da silhueta: `ColorFilter.tint(silhueta, SrcIn)`, nunca opacidade. */
    val silhueta: Color,
    /** Traço do padrão de Pokébolas de fundo, só no cabeçalho da Pokédex e em estados vazios. */
    val padraoPokebola: Color,
    /** Degraus 1 a 3 do heatmap de calendário (`03 §3.17`). O 0 é `superficieAfundada`, e o 4 é `destaque`. */
    val intensidade1: Color,
    val intensidade2: Color,
    val intensidade3: Color,
)

/** A única instância que existe hoje. O app entrega um tema claro (D-13). */
val CoresPokerunClaro = CoresPokerun(
    superficieAfundada = SuperficieAfundada,
    destaqueSuave = DestaqueSuave,
    destaquePressionado = DestaquePressionado,
    alerta = Alerta,
    alertaSuave = AlertaSuave,
    sucesso = Sucesso,
    sucessoSuave = SucessoSuave,
    estadoPressionado = EstadoPressionado,
    foco = Foco,
    silhueta = Silhueta,
    padraoPokebola = PadraoPokebola,
    intensidade1 = Intensidade1,
    intensidade2 = Intensidade2,
    intensidade3 = Intensidade3,
)

val LocalCoresPokerun = staticCompositionLocalOf { CoresPokerunClaro }
