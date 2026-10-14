package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema claro fixo, derivado dos tokens em Color.kt (docs/02 §2.1).
 *
 * O app é claro SEMPRE: não segue o tema do sistema, não expõe alternância e não
 * usa cor dinâmica do Material You (D-13). O contraste dos tokens foi medido contra
 * branco e `superficie-afundada`, não contra uma paleta gerada pelo aparelho.
 *
 * **Os 36 papéis estão preenchidos.** `lightColorScheme()` não deixa vazio o que não
 * recebe: preenche com o baseline lavanda do Material 3, derivado do roxo `#6750A4`,
 * sem aviso e sem erro. O FAB nasceria `#EADDFF` e a aba selecionada `#E8DEF8`.
 * `TemaTest` trava isso.
 *
 * `destaque` é a única cor de interface fora dos tipos. Os papéis que o M3 pinta de
 * cor e o design system deixa em tinta (`secondary`, `tertiary`) recebem neutros.
 *
 * `internal` e não `private` porque o teste precisa enxergar o esquema. Continua
 * fora do alcance de qualquer tela, que lê por `MaterialTheme.colorScheme`.
 */
internal val PokerunColorScheme = lightColorScheme(
    // O FAB nasce de primaryContainer, e o FAB é `destaque` (docs/02 §2.1).
    primary = Destaque,
    onPrimary = TextoSobreDestaque,
    primaryContainer = Destaque,
    onPrimaryContainer = TextoSobreDestaque,
    inversePrimary = Destaque,             // ação do Snackbar, sobre superfície clara

    // Série secundária de gráfico em tinta: nada colorido compete com a métrica.
    secondary = TextoSecundario,
    onSecondary = Superficie,
    secondaryContainer = DestaqueSuave,    // indicador da NavigationBar: destino ativo
    onSecondaryContainer = TextoPrimario,

    tertiary = TextoSecundario,
    onTertiary = Superficie,
    tertiaryContainer = SuperficieAfundada,
    onTertiaryContainer = TextoPrimario,

    background = Fundo,
    onBackground = TextoPrimario,
    surface = Superficie,
    onSurface = TextoPrimario,
    surfaceVariant = SuperficieAfundada,
    onSurfaceVariant = TextoSecundario,

    // A sombra é por componente (docs/02 §4); a tinta tonal do M3 não entra.
    surfaceTint = Color.Transparent,
    surfaceBright = Superficie,
    surfaceDim = SuperficieAfundada,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,         // NavigationBar, menu
    surfaceContainerHigh = Superficie,     // AlertDialog
    surfaceContainerHighest = Superficie,  // Card preenchido, campo preenchido

    // O Snackbar NÃO é superfície escura (docs/02 §2.2). `destaque` sobre
    // `texto-primario` mede 3,69:1 e reprovaria a ação. Sobre branco mede 4,89:1.
    inverseSurface = Superficie,
    inverseOnSurface = TextoPrimario,

    outline = BordaControle,
    outlineVariant = Borda,
    scrim = Scrim,

    error = Alerta,
    onError = Superficie,
    errorContainer = AlertaSuave,
    onErrorContainer = Alerta,
)

/**
 * Sem parâmetro `darkTheme`. D-13 proíbe *shipar* dois temas; a estrutura para um
 * segundo é o [CoresPokerun], o [CoresDeTipo] e a disciplina de acesso, não um
 * booleano.
 */
@Composable
fun PokerunTheme(content: @Composable () -> Unit) {
    // Lida uma vez por contexto. A escala não é mudança de configuração: trocada com
    // o app aberto, ela só vale quando a Activity for recriada.
    val contexto = LocalContext.current
    val movimentoReduzido = remember(contexto) {
        Movimento.reduzido(Movimento.escalaDoAnimador(contexto))
    }
    CompositionLocalProvider(
        LocalCoresPokerun provides CoresPokerunClaro,
        LocalCoresDeTipo provides CoresDeTipoClaro,
        LocalIndication provides IndicacaoDeToque,
        LocalMovimentoReduzido provides movimentoReduzido,
    ) {
        MaterialTheme(
            colorScheme = PokerunColorScheme,
            typography = Typography,
            shapes = PokerunShapes,
            content = content
        )
    }
}
