package com.hggabriel.pokerun.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaChip
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** O chip pequeno, do card de espécie (`ChipDeTipo.md`). O de 28dp é do detalhe. */
internal val AlturaDoChipPequeno = 22.dp

/** O nome do tipo em pt-BR, como o chip escreve e o TalkBack lê. */
@StringRes
fun nomeDoTipo(tipo: TipoDePokemon): Int = when (tipo) {
    TipoDePokemon.NORMAL -> R.string.tipo_normal
    TipoDePokemon.FOGO -> R.string.tipo_fogo
    TipoDePokemon.AGUA -> R.string.tipo_agua
    TipoDePokemon.PLANTA -> R.string.tipo_planta
    TipoDePokemon.ELETRICO -> R.string.tipo_eletrico
    TipoDePokemon.GELO -> R.string.tipo_gelo
    TipoDePokemon.LUTADOR -> R.string.tipo_lutador
    TipoDePokemon.VENENOSO -> R.string.tipo_venenoso
    TipoDePokemon.TERRESTRE -> R.string.tipo_terrestre
    TipoDePokemon.VOADOR -> R.string.tipo_voador
    TipoDePokemon.PSIQUICO -> R.string.tipo_psiquico
    TipoDePokemon.INSETO -> R.string.tipo_inseto
    TipoDePokemon.PEDRA -> R.string.tipo_pedra
    TipoDePokemon.FANTASMA -> R.string.tipo_fantasma
    TipoDePokemon.DRAGAO -> R.string.tipo_dragao
    TipoDePokemon.SOMBRIO -> R.string.tipo_sombrio
    TipoDePokemon.ACO -> R.string.tipo_aco
    TipoDePokemon.FADA -> R.string.tipo_fada
}

/**
 * O chip de tipo (docs/02 §2.5, `ChipDeTipo.md`): o nome do tipo em `tipo-<id>-sobre`
 * sobre `tipo-<id>`, em `raio-chip`. Os pares `sobre` × base passam 4,5:1 nos 18 tipos.
 *
 * **A cor de tipo é reforço, nunca o dado.** O nome está sempre escrito. Sem [tipo], o
 * chip diz `Tipo ?` em `superficie-afundada`: espécie ainda não revelada.
 *
 * **Informativo, não tocável.** Chip de filtro é outro componente, com `borda-controle`.
 *
 * Sem o ícone do tipo por ora: o app ainda não tem os 18 glifos. O nome sozinho já cumpre
 * a regra de nunca usar a cor sem o nome.
 */
@Composable
fun ChipDeTipo(tipo: TipoDePokemon?, modifier: Modifier = Modifier) {
    val (fundo, tinta) = if (tipo != null) {
        val cor = LocalCoresDeTipo.current[tipo]
        cor.base to cor.sobre
    } else {
        LocalCoresPokerun.current.superficieAfundada to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .heightIn(min = AlturaDoChipPequeno)
            .background(fundo, FormaChip)
            .padding(horizontal = Espacos.relacionado),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(tipo?.let(::nomeDoTipo) ?: R.string.tipo_desconhecido),
            style = MaterialTheme.typography.labelSmall,
            color = tinta,
            maxLines = 1,
        )
    }
}
