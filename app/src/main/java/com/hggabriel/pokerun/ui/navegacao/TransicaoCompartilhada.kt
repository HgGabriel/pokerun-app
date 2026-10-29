@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento

/** O escopo da transição compartilhada, que envolve o `NavHost` da casca. */
val LocalEscopoCompartilhado = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** O escopo animado da rota, que cada `composable` da Pokédex fornece. */
val LocalEscopoAnimado = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * A figurinha que o dedo tocou, de onde ela saiu: a mesma espécie aparece na fita da capa
 * e na grade, e só a tocada pode crescer até a frente do detalhe. Duas com a mesma chave à
 * vista ao mesmo tempo brigariam pelo mesmo voo.
 */
@Stable
class FigurinhaEmVoo {
    var numero: Int? by mutableStateOf(null)
        private set
    var lugar: String? by mutableStateOf(null)
        private set

    fun marcar(numero: Int, lugar: String) {
        this.numero = numero
        this.lugar = lugar
    }
}

val LocalFigurinhaEmVoo = staticCompositionLocalOf { FigurinhaEmVoo() }

/** A chave do voo da figurinha [numero] até a frente do detalhe. */
fun chaveDaFigurinha(numero: Int): String = "figurinha-$numero"

/**
 * Liga o elemento à transição compartilhada quando [ativo]: a figurinha cresce da vaga até
 * virar a frente (`docs/design-system/transicoes.md`, desvio de 27/10/2026). Sem os escopos
 * (prévia, fechamento semanal) ou com movimento reduzido, não faz nada, e a rota desliza
 * como as outras.
 */
@Composable
fun Modifier.vooCompartilhado(chave: String, ativo: Boolean): Modifier {
    val compartilhado = LocalEscopoCompartilhado.current ?: return this
    val animado = LocalEscopoAnimado.current ?: return this
    if (!ativo || LocalMovimentoReduzido.current) return this
    return with(compartilhado) {
        // O voo dura o deslize da rota, no mesmo compasso, e não a mola padrão do Compose.
        this@vooCompartilhado.sharedBounds(
            rememberSharedContentState(chave),
            animado,
            enter = fadeIn(Movimento.padrao()),
            exit = fadeOut(Movimento.saida()),
            boundsTransform = VooNoCompasso,
        )
    }
}

private val VooNoCompasso = BoundsTransform { _, _ ->
    androidx.compose.animation.core.tween(Movimento.Navegacao.DURACAO, easing = Movimento.Curva.padrao)
}
