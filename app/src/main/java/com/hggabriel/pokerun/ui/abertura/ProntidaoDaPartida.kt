package com.hggabriel.pokerun.ui.abertura

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * O aviso de que a primeira tela está pronta: a abertura só anima depois dele (fase 2).
 *
 * Antes do aviso, a thread principal compõe a primeira tela e recompõe quando os dados
 * chegam. Uma animação nesse intervalo disputa quadros com a partida e engasga.
 */
@Stable
class ProntidaoDaPartida {
    var pronta by mutableStateOf(false)
        private set

    fun marcar() {
        pronta = true
    }
}

/**
 * Provida pela `MainActivity`. O padrão já nasce pronto: um preview, um teste ou uma tela
 * fora da partida não seguram abertura nenhuma.
 */
val LocalProntidaoDaPartida = staticCompositionLocalOf { ProntidaoDaPartida().apply { marcar() } }

/** Avisa a abertura quando [pronta] fica verdadeiro. Chamar mais de uma vez não muda nada. */
@Composable
fun AvisarProntidao(pronta: Boolean) {
    val prontidao = LocalProntidaoDaPartida.current
    LaunchedEffect(pronta) { if (pronta) prontidao.marcar() }
}
