package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro fixo, derivado dos tokens em Color.kt.
 *
 * O app é claro SEMPRE: não segue o tema do sistema, não expõe alternância e não
 * usa cor dinâmica do Material You. Um app de instrumento tem uma leitura só —
 * e o contraste dos tokens foi medido contra papel e painel, não contra uma
 * paleta gerada pelo aparelho do usuário.
 *
 * **Os 36 papéis estão preenchidos, e isso não é zelo.** `lightColorScheme()` não
 * deixa vazio o que não recebe: preenche com o baseline lavanda do Material 3,
 * derivado do roxo `#6750A4`. Sem aviso, sem erro, sem aparecer em revisão de
 * diff — e com o FAB nascendo `#EADDFF`, o card `#E6E0E9` e a aba selecionada
 * `#E8DEF8`. É o vício nº 1 de docs/02 §9.1 entrando pela porta dos fundos.
 * `TemaTest` trava isso.
 *
 * `internal` e não `private` porque o teste precisa enxergar o esquema. Continua
 * fora do alcance de qualquer tela, que lê por `MaterialTheme.colorScheme`.
 */
internal val PokerunColorScheme = lightColorScheme(
    primary = Leitura,
    onPrimary = Painel,
    primaryContainer = LeituraFraca,       // FAB e chip assist nascem daqui
    onPrimaryContainer = Tinta,
    inversePrimary = Leitura,              // ação do Snackbar

    secondary = Sinal,
    onSecondary = Painel,
    secondaryContainer = LeituraFraca,     // indicador da NavigationBar
    onSecondaryContainer = Tinta,

    tertiary = Sinal,
    onTertiary = Painel,
    tertiaryContainer = LeituraFraca,
    onTertiaryContainer = Tinta,

    background = Papel,
    onBackground = Tinta,
    surface = Painel,
    onSurface = Tinta,
    surfaceVariant = Papel,
    onSurfaceVariant = TintaFraca,

    // Elevação é contraste de superfície, não sombra e não tinta (docs/02 §4).
    surfaceTint = Color.Transparent,
    surfaceBright = Painel,
    surfaceDim = Papel,
    surfaceContainerLowest = Painel,
    surfaceContainerLow = Painel,
    surfaceContainer = Painel,             // NavigationBar, menu
    surfaceContainerHigh = Painel,         // AlertDialog
    surfaceContainerHighest = Painel,      // Card preenchido

    inverseSurface = Tinta,                // Snackbar
    inverseOnSurface = Papel,

    outline = BordaForte,
    outlineVariant = Borda,
    scrim = Tinta,                         // o M3 aplica a opacidade; o baseline seria preto puro

    error = Alerta,
    onError = Painel,
    errorContainer = Painel,
    onErrorContainer = Alerta,
)

/**
 * Sem parâmetro `darkTheme`. D-13 proíbe *shipar* dois temas; a estrutura para um
 * segundo é o [CoresPokerun] e a disciplina de acesso, não um booleano — no
 * instante em que o parâmetro existe, toda revisão de tela ganha dois casos mesmo
 * sem o caminho escuro nunca renderizar.
 */
@Composable
fun PokerunTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoresPokerun provides CoresPokerunClaro) {
        MaterialTheme(
            colorScheme = PokerunColorScheme,
            typography = Typography,
            shapes = PokerunShapes,
            content = content
        )
    }
}
