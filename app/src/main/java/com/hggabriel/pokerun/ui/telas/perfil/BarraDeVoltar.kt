package com.hggabriel.pokerun.ui.telas.perfil

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.theme.Espacos

/**
 * O topo do Perfil e dos seletores: a seta de voltar e, ao lado, o rótulo da tela (27/10/2026).
 *
 * O Perfil saiu da barra inferior: é uma tela de configuração, e a seta é a saída dela. O
 * gesto de voltar do sistema continua valendo e passa pela mesma pergunta das alterações.
 */
@Composable
internal fun BarraDeVoltar(aoVoltar: () -> Unit, modifier: Modifier = Modifier, rotulo: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Espacos.alvoToque)
            .padding(start = 4.dp, end = Espacos.margem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = aoVoltar) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.editar_voltar),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (rotulo != null) RotuloDoCartao(rotulo)
    }
}
