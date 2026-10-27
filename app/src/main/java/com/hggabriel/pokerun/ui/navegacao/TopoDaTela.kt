package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * O topo que a tela pede para a barra de status, quando ele muda com a rolagem: a Pokédex
 * começa na capa em tinta e, com a capa fora da tela, passa à página tingida. A casca lê
 * [emTinta] e pinta a faixa; a tela escreve e devolve nulo ao sair.
 */
@Stable
class TopoDaTela {
    var emTinta: Boolean? by mutableStateOf(null)
}

val LocalTopoDaTela = staticCompositionLocalOf { TopoDaTela() }
