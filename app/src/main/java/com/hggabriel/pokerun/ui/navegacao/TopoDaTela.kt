package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * O topo que a tela pede para a barra de status, quando ele muda com a tela: a Pokédex
 * começa na capa em tinta e, com a capa fora da tela, passa à página tingida; o detalhe da
 * espécie pinta o topo na cor suave do tipo da página aberta.
 *
 * Cada pedido tem dono. Na troca de rota as duas telas convivem durante a transição, e a
 * que sai não pode apagar o pedido da que entrou.
 */
@Stable
class TopoDaTela {
    var cor: Color? by mutableStateOf(null)
        private set
    var iconesClaros: Boolean by mutableStateOf(false)
        private set

    /** A cor lida a cada quadro, quando ela muda com o arraste (o detalhe da espécie). */
    var corNoQuadro: (() -> Color)? by mutableStateOf(null)
        private set
    private var dono: Any? = null

    fun pedir(quem: Any, cor: Color, iconesClaros: Boolean, noQuadro: (() -> Color)? = null) {
        dono = quem
        this.cor = cor
        this.iconesClaros = iconesClaros
        corNoQuadro = noQuadro
    }

    fun soltar(quem: Any) {
        if (dono !== quem) return
        dono = null
        cor = null
        iconesClaros = false
        corNoQuadro = null
    }
}

val LocalTopoDaTela = staticCompositionLocalOf { TopoDaTela() }
