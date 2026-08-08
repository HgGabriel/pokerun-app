package com.hggabriel.pokerun.ui.telas.dump

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.EstiloDado

/**
 * O dump do Health Connect (`F0-T09`). **Descartável**: instrumento de medição de
 * `F0-T10`, apagado junto com a tabela preenchida.
 *
 * Ela não segue o cabeçalho de ficha de `docs/02 §10` nem é uma das vinte telas do
 * app. **Não vale como precedente de nada:** o que se aprende dela é o conteúdo do
 * Health Connect de cada aparelho, não como o PokéRun se parece.
 *
 * O que ela respeita, porque é barato e porque há teste que falha: cor sai de
 * `MaterialTheme.colorScheme`, nunca de um `val` importado (`docs/02 §2.6`,
 * `TemaTest`), e alvo de toque tem 48dp (`docs/02 §8`).
 */
@Composable
fun DumpScreen(
    modifier: Modifier = Modifier,
    vm: DumpViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current

    val pedirPermissoes = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { vm.conferirDisponibilidade() }

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
                text = stringResource(R.string.dump_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = when (estado.status) {
                    StatusDoSdk.NaoConsultado -> stringResource(R.string.dump_consultando)
                    StatusDoSdk.Indisponivel -> stringResource(R.string.dump_sem_health_connect)
                    StatusDoSdk.PrecisaAtualizar -> stringResource(R.string.dump_health_connect_antigo)
                    StatusDoSdk.Disponivel -> stringResource(R.string.dump_health_connect_ok)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            estado.falha?.let { falha ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = falha,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (estado.status == StatusDoSdk.Disponivel && !estado.temPermissao) {
                    Button(
                        onClick = { pedirPermissoes.launch(PERMISSOES_DO_DUMP) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.dump_conceder))
                    }
                }
                Button(
                    onClick = { vm.lerJanela() },
                    enabled = estado.status == StatusDoSdk.Disponivel &&
                        estado.temPermissao &&
                        !estado.lendo,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.dump_ler))
                }
                if (estado.relatorio.isNotEmpty()) {
                    // O caminho de saída de F0-T10: o dump sai do aparelho de outra
                    // pessoa sem cabo, sem adb e sem ninguém transcrever número à mão.
                    OutlinedButton(
                        onClick = { contexto.startActivity(envioDe(estado.relatorio)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.dump_enviar))
                    }
                }
            }

            if (estado.lendo) {
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (estado.relatorio.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                // Rolagem horizontal própria: o dump é colunado e quebrar linha
                // destruiria o alinhamento que faz dele uma tabela.
                SelectionContainer {
                    Text(
                        text = estado.relatorio,
                        style = EstiloDado.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        softWrap = false,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

private fun envioDe(relatorio: String): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PokeRun - dump do Health Connect")
            putExtra(Intent.EXTRA_TEXT, relatorio)
        },
        null,
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
