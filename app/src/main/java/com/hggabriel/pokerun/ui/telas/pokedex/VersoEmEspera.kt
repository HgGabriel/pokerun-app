package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.ComBrilhoDeEspera
import com.hggabriel.pokerun.ui.componentes.esperaComBrilho
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.delay

/*
 * A espera do verso da espécie (29/10/2026): cada cartão que ainda vem da rede aparece na
 * forma dele, com rótulo, linhas e figuras em blocos, e o brilho clássico varrendo. Quando o
 * conteúdo chega, ele entra subindo um pouco e acendendo, um cartão uma batida depois do outro.
 */

private val FormaDaLinha = RoundedCornerShape(6.dp)
private val FormaDaMini = RoundedCornerShape(12.dp)

/** Quanto o cartão sobe ao entrar. */
private val SubidaDaEntrada = 16.dp

/**
 * Os cartões do verso colado em espera: a corrida que colou, a evolução e a ficha. Cada um
 * pode ficar de fora, quando o conteúdo dele já está na tela.
 */
@Composable
internal fun VersoEmEspera(
    modifier: Modifier = Modifier,
    coladaEm: Boolean = true,
    evolucao: Boolean = true,
    ficha: Boolean = true,
) {
    ComBrilhoDeEspera {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            if (coladaEm) ColadaEmEspera()
            if (evolucao) EvolucaoEmEspera()
            if (ficha) FichaEmEspera()
        }
    }
}

@Composable
private fun Linha(largura: Dp, altura: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier.size(largura, altura).esperaComBrilho(FormaDaLinha))
}

/** O rótulo, o dia, o km e o pace, e o chevron. */
@Composable
private fun ColadaEmEspera() {
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Linha(80.dp, 12.dp)
                Spacer(Modifier.height(10.dp))
                Linha(160.dp, 18.dp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Linha(56.dp, 24.dp)
                    Linha(72.dp, 24.dp)
                }
            }
            Spacer(Modifier.size(24.dp).esperaComBrilho(FormaCirculo))
        }
    }
}

/** O rótulo e três minifigurinhas com o nome embaixo, separadas pelas setas. */
@Composable
private fun EvolucaoEmEspera() {
    Cartao {
        Linha(72.dp, 12.dp)
        Spacer(Modifier.height(Espacos.relacionado))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                if (i > 0) Linha(16.dp, 4.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.padding(4.dp).size(72.dp, 84.dp).esperaComBrilho(FormaDaMini))
                    Linha(52.dp, 12.dp)
                }
            }
        }
    }
}

/** Altura e peso: dois rótulos e dois números. */
@Composable
private fun FichaEmEspera() {
    Cartao {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.interno)) {
            repeat(2) {
                Column(modifier = Modifier.weight(1f)) {
                    Linha(48.dp, 12.dp)
                    Spacer(Modifier.height(8.dp))
                    Linha(64.dp, 20.dp)
                }
            }
        }
    }
}

/**
 * A entrada do cartão que chegou: sobe [SubidaDaEntrada] e acende em `tempo-padrao`, uma
 * batida depois do cartão anterior ([indice]). Sem [anima], ou com movimento reduzido, o
 * cartão já nasce no lugar.
 */
@Composable
internal fun Modifier.entradaDoCartao(indice: Int, anima: Boolean): Modifier {
    val reduzido = LocalMovimentoReduzido.current
    val progresso = remember { Animatable(if (anima && !reduzido) 0f else 1f) }
    LaunchedEffect(progresso) {
        if (progresso.value < 1f) {
            delay(Movimento.Tempo.batidas(indice).toLong())
            progresso.animateTo(1f, Movimento.padrao())
        }
    }
    return this.graphicsLayer {
        alpha = progresso.value
        translationY = (1f - progresso.value) * SubidaDaEntrada.toPx()
    }
}
