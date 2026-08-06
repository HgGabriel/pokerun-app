package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema claro fixo, derivado dos tokens em Color.kt.
 *
 * O app Ã© claro SEMPRE: nÃ£o segue o tema do sistema, nÃ£o expÃµe alternÃ¢ncia e nÃ£o
 * usa cor dinÃ¢mica do Material You. Um app de instrumento tem uma leitura sÃ³ â€”
 * e o contraste dos tokens foi medido contra papel e painel, nÃ£o contra uma
 * paleta gerada pelo aparelho do usuÃ¡rio.
 */
private val PokerunColorScheme = lightColorScheme(
    primary = Leitura,
    onPrimary = Painel,
    secondary = Sinal,
    onSecondary = Painel,
    tertiary = Sinal,
    onTertiary = Painel,
    background = Papel,
    onBackground = Tinta,
    surface = Painel,
    onSurface = Tinta,
    surfaceVariant = Papel,
    onSurfaceVariant = TintaFraca,
    outline = BordaForte,
    outlineVariant = Borda,
    error = Alerta,
    onError = Painel,
)

@Composable
fun PokerunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PokerunColorScheme,
        typography = Typography,
        shapes = PokerunShapes,
        content = content
    )
}
