package com.hggabriel.pokerun.ui.telas.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto

/**
 * A pergunta de quem sai do Perfil com nome, parceiro ou treinador sem salvar (27/10/2026).
 *
 * Três saídas, empilhadas porque os rótulos não cabem lado a lado: salvar e sair, continuar
 * no Perfil, ou descartar e sair. Tocar fora é continuar editando: fechar o diálogo nunca
 * apaga nada.
 */
@Composable
fun DialogoDeAlteracoesPendentes(
    aoSalvar: () -> Unit,
    aoContinuar: () -> Unit,
    aoDescartar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoContinuar,
        title = { Text(stringResource(R.string.perfil_pendente_titulo)) },
        text = { Text(stringResource(R.string.perfil_pendente_texto)) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                BotaoDeTexto(aoTocar = aoSalvar) { Text(stringResource(R.string.perfil_salvar)) }
                BotaoDeTexto(aoTocar = aoContinuar) { Text(stringResource(R.string.perfil_pendente_continuar)) }
                BotaoDeTexto(aoTocar = aoDescartar) { Text(stringResource(R.string.perfil_descartar)) }
            }
        },
    )
}
