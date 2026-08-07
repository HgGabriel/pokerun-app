package com.hggabriel.pokerun.ui.telas.politica

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * A política de privacidade que o Health Connect exige (docs/05 §4.4).
 *
 * **Ela não é uma das vinte telas do app.** Quem a abre é o app Health Connect,
 * quando o usuário toca na política antes de decidir sobre a permissão, e o
 * caminho de volta é o botão de voltar do sistema. Por isso não tem sobrancelha:
 * a sobrancelha de docs/02 §10 é o caminho dentro da navegação (`PROGRESSO ·
 * HISTÓRICO`), e aqui não há aba nem profundidade a informar. Ficam o título e o
 * filete, que são as outras duas partes do cabeçalho de ficha.
 *
 * Também não tem `ViewModel` nem `UiState`, ao contrário do que `ui/telas`
 * descreve: não há dado, não há rede e não há estado. O texto é constante.
 *
 * A tela é rolável de propósito. Ela é um dos piores casos de `fontScale` 2,0 do
 * app, porque é a única que é quase toda texto corrido (docs/02 §8, item 9).
 */
@Composable
fun PoliticaDePrivacidadeScreen(
    aoFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O papel embaixo vem daqui, e não do `windowBackground` do tema XML: sem a
    // Surface, a tela herda o branco do framework em todo contexto que não seja a
    // janela desta Activity, a começar pelo preview.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.politica_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SecaoDaPolitica(R.string.politica_le_rotulo, R.string.politica_le_corpo)
            SecaoDaPolitica(R.string.politica_nao_le_rotulo, R.string.politica_nao_le_corpo)
            SecaoDaPolitica(R.string.politica_quando_rotulo, R.string.politica_quando_corpo)
            SecaoDaPolitica(R.string.politica_para_que_rotulo, R.string.politica_para_que_corpo)
            SecaoDaPolitica(R.string.politica_onde_rotulo, R.string.politica_onde_corpo)

            Spacer(Modifier.height(32.dp))
            // O botão é conveniência: o caminho canônico de volta é o voltar do
            // sistema, porque quem abriu esta Activity foi outro app. Fica alinhado
            // à esquerda com o texto, não centralizado, porque a tela é documento.
            TextButton(
                onClick = aoFechar,
                // ButtonDefaults.MinHeight é 40dp, abaixo do piso de 48dp (docs/02 §8).
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.politica_fechar))
            }
        }
    }
}

/**
 * Rótulo em mono com caixa alta aplicada aqui, e não na escala (`Type.kt`), mais o
 * corpo em `bodyLarge`. O rótulo é `heading` para o TalkBack: sem isso a tela
 * inteira é um bloco de texto só, e navegar por cabeçalhos é como se lê documento.
 */
@Composable
private fun SecaoDaPolitica(
    @StringRes rotulo: Int,
    @StringRes corpo: Int,
) {
    Column(
        modifier = Modifier.padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(rotulo).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(corpo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoliticaDePrivacidadePreview() {
    PokerunTheme {
        PoliticaDePrivacidadeScreen(aoFechar = {})
    }
}

@Preview(showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun PoliticaDePrivacidadeAmpliadaPreview() {
    PokerunTheme {
        PoliticaDePrivacidadeScreen(aoFechar = {})
    }
}
