package com.example.pokerun.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema claro fixo, derivado dos tokens em Color.kt.
 *
 * O app é claro SEMPRE: não segue o tema do sistema, não expõe alternância e não
 * usa cor dinâmica do Material You. Um app de instrumento tem uma leitura só —
 * e o contraste dos tokens foi medido contra papel e painel, não contra uma
 * paleta gerada pelo aparelho do usuário.
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
