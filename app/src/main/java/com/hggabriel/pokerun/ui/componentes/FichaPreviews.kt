package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * Os cinco estados de [Ficha], lado a lado (docs/02 §4.2).
 *
 * Pressionado e foco não acontecem sozinhos num preview estático, e é justamente
 * onde os defeitos moram: o ripple circular do Material e o contorno de foco que
 * alguém remove sem perceber. Por isso a fonte de interação é injetada já carregada
 * com a interação — o preview renderiza o estado de verdade, não uma imitação dele.
 */
@Preview(showBackground = true, backgroundColor = 0xFFFAF9F7, widthDp = 300)
@Composable
private fun FichaEstadosPreview() {
    PokerunTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Ficha(aoTocar = {}) { Rotulo("Repouso") }

            Ficha(aoTocar = {}, fonteDeInteracao = fonteCom(PressInteraction.Press(Offset.Zero))) {
                Rotulo("Pressionado")
            }

            Ficha(aoTocar = {}, selecionada = true) {
                Rotulo("Selecionado", marca = true)
            }

            Ficha(aoTocar = {}, habilitada = false) { Rotulo("Desabilitado") }

            Ficha(aoTocar = {}, fonteDeInteracao = fonteCom(FocusInteraction.Focus())) {
                Rotulo("Foco")
            }
        }
    }
}

/**
 * [FichaDeEspecime] é onde vai o sprite. Sem sprite ainda — eles chegam em `F2-T14`
 * como asset local, e placeholder de imagem não entra no repositório.
 */
@Preview(showBackground = true, backgroundColor = 0xFFFAF9F7, widthDp = 300)
@Composable
private fun FichaDeEspecimePreview() {
    PokerunTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            FichaDeEspecime {
                Rotulo("#001 · Bulbasaur")
            }
        }
    }
}

@Composable
private fun Rotulo(texto: String, marca: Boolean = false) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (marca) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Uma fonte de interação já carregada, para o preview poder mostrar o estado. */
@Composable
private fun fonteCom(interacao: Interaction): MutableInteractionSource {
    val fonte = remember { MutableInteractionSource() }
    LaunchedEffect(fonte) { fonte.emit(interacao) }
    return fonte
}
