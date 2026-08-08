package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A amostra dos três papéis de `F0-T08`, que é o que a ficha pede para conferir.
 *
 * Ela mora em `ui/theme` de propósito: não é tela do app, não entra em navegação
 * nenhuma e não deve virar uma. É instrumento de conferência da fundação, no mesmo
 * espírito de `FichaPreviews`.
 *
 * **O que olhar, em ordem:**
 *
 * 1. **A largura.** As duas primeiras linhas são o mesmo texto no mesmo corpo, uma
 *    em Archivo com `wdth = 125` e outra em Plex Sans. Se as duas tiverem a mesma
 *    largura, o `variationSettings` não pegou e a tese de docs/02 §3.1 morreu em
 *    silêncio: o Archivo em `wdth` 100 lê como um negrito qualquer.
 * 2. **As figuras tabulares.** `111` e `888` no display têm de ocupar exatamente a
 *    mesma largura (docs/02 §3.3). Se `111` for mais estreito, o `tnum` não pegou e
 *    a contagem regressiva vai tremer ao virar o dia.
 * 3. **As três famílias.** Display com serifa nenhuma e muito largo, corpo em Plex
 *    Sans, rótulo em Plex Mono. Nenhuma linha em Roboto.
 */
@Composable
fun AmostraDeTipografia(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Amostra("displayLarge · Archivo wdth 125", MaterialTheme.typography.displayLarge, "PokéRun")
            Amostra("bodyLarge · Plex Sans 400", MaterialTheme.typography.bodyLarge, "PokéRun")
            Nota("As duas linhas acima são o mesmo texto. A de cima tem de ser muito mais larga.")

            Divisor()

            Amostra("tnum · 111", MaterialTheme.typography.displayMedium, "111")
            Amostra("tnum · 888", MaterialTheme.typography.displayMedium, "888")
            Nota("As duas linhas acima têm de ocupar a mesma largura.")

            Divisor()

            Amostra("displayMedium", MaterialTheme.typography.displayMedium, "32 dias")
            Amostra("headlineSmall", MaterialTheme.typography.headlineSmall, "Semana 7 de 21")
            Amostra("titleLarge", MaterialTheme.typography.titleLarge, "Corrida de terça")
            Amostra("titleMedium", MaterialTheme.typography.titleMedium, "Longão de 12 km")
            Amostra("titleSmall", MaterialTheme.typography.titleSmall, "Histórico")
            Amostra("bodyMedium", MaterialTheme.typography.bodyMedium, "Aderência da semana")
            Amostra("bodySmall", MaterialTheme.typography.bodySmall, "Apoio de formulário")
            Amostra("labelLarge · botão", MaterialTheme.typography.labelLarge, "Registrar corrida")
            Amostra("labelMedium · mono", MaterialTheme.typography.labelMedium, "PROGRESSO · HISTÓRICO")
            Amostra("labelSmall · mono", MaterialTheme.typography.labelSmall, "#059")
            Amostra("EstiloDado · mono", EstiloDado, "5,42 km · 27:18 · 5:02 /km")
        }
    }
}

@Composable
private fun Amostra(rotulo: String, estilo: TextStyle, texto: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = texto,
            style = estilo,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun Nota(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Divisor() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun AmostraDeTipografiaPreview() {
    PokerunTheme {
        AmostraDeTipografia()
    }
}
