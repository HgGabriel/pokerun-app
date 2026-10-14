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
 * A amostra dos 13 estilos e dos 15 papéis, para conferir a tipografia no preview.
 *
 * Ela mora em `ui/theme` de propósito: não é tela do app, não entra em navegação
 * nenhuma e não deve virar uma. É instrumento de conferência da fundação, no mesmo
 * espírito de `FichaPreviews`.
 *
 * **O que olhar, em ordem:**
 *
 * 1. **As duas famílias.** Números, títulos, botão e rótulo em Poppins (geométrica,
 *    `a` de dois andares); corpo, apoio e legenda em Quicksand (terminais
 *    arredondados, `a` de um andar). Nenhuma linha em Roboto.
 * 2. **Os pesos do Quicksand.** `corpo` (500), `apoio` (600) e `corpoForte` (700)
 *    têm de ter traços visivelmente diferentes. Se os três saírem iguais, o
 *    `variationSettings` não pegou e o arquivo variável desenha só o peso padrão.
 * 3. **Os papéis do M3.** Cada papel tem de bater com o estilo que a etiqueta nomeia.
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Amostra("numeroXl · Poppins 700", Estilos.numeroXl, "12,40")
            Amostra("numeroL · Poppins 700", Estilos.numeroL, "24.100")
            Amostra("numeroM · Poppins 700", Estilos.numeroM, "5:32")
            Amostra("numeroS · Poppins 700 · EstiloDado", Estilos.numeroS, "1:04:22")
            Amostra("tituloTela · Poppins 700", Estilos.tituloTela, "Segunda, 14 de setembro")
            Amostra("nomeEspecie · Poppins 700", Estilos.nomeEspecie, "Raichu")
            Amostra("tituloSecao · Poppins 700", Estilos.tituloSecao, "Desafio da semana")
            Amostra("botao · Poppins 600", Estilos.botao, "Registrar corrida")
            Amostra("rotulo · Poppins 600", Estilos.rotulo, "SEMANA 10 · 12 A 18 DE OUTUBRO")

            Divisor()

            Amostra("corpo · Quicksand 500", Estilos.corpo, "Seu plano começa segunda, 10 de agosto.")
            Amostra("apoio · Quicksand 600", Estilos.apoio, "#026 Raichu")
            Amostra("corpoForte · Quicksand 700", Estilos.corpoForte, "dias para São Silvestre 2026")
            Amostra("legenda · Quicksand 700", Estilos.legenda, "Pokédex")
            Nota("corpo, apoio e corpoForte têm de ter traços diferentes.")

            Divisor()

            Amostra("displayLarge = numeroXl", MaterialTheme.typography.displayLarge, "32")
            Amostra("displayMedium = numeroL", MaterialTheme.typography.displayMedium, "32 dias")
            Amostra("displaySmall = numeroM", MaterialTheme.typography.displaySmall, "5:02")
            Amostra("headlineSmall = tituloTela", MaterialTheme.typography.headlineSmall, "Excluir corrida?")
            Amostra("titleLarge = tituloTela", MaterialTheme.typography.titleLarge, "Corrida de terça")
            Amostra("titleMedium = tituloSecao", MaterialTheme.typography.titleMedium, "Longão de 12 km")
            Amostra("titleSmall = corpoForte", MaterialTheme.typography.titleSmall, "Histórico")
            Amostra("bodyLarge = corpo", MaterialTheme.typography.bodyLarge, "Aderência da semana")
            Amostra("bodyMedium = apoio", MaterialTheme.typography.bodyMedium, "2 de 3 sessões")
            Amostra("bodySmall = legenda", MaterialTheme.typography.bodySmall, "Apoio de formulário")
            Amostra("labelLarge = botao", MaterialTheme.typography.labelLarge, "Registrar corrida")
            Amostra("labelMedium = rotulo", MaterialTheme.typography.labelMedium, "PROGRESSO · HISTÓRICO")
            Amostra("labelSmall = legenda", MaterialTheme.typography.labelSmall, "#059")
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

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun AmostraDeTipografiaPreview() {
    PokerunTheme {
        AmostraDeTipografia()
    }
}
