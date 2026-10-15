package com.hggabriel.pokerun.ui.componentes

import com.hggabriel.pokerun.dominio.modelo.Temporada

/** Um segmento do nível 1: um tier, com o quanto da coleção já o preencheu. */
data class TierNaEscada(
    val ordem: Int,
    val nome: String,
    /** A posição, de 1 em diante, da primeira espécie do tier. */
    val primeiraPosicao: Int,
    val total: Int,
    /** Quantas espécies do tier a coleção já alcançou. */
    val alcancadas: Int,
) {
    val fracao: Float get() = 0f
}

/** Uma marca do nível 2: uma espécie do tier exibido. */
data class MarcaDaEscada(val posicao: Int, val numero: Int, val alcancada: Boolean, val atual: Boolean)

/** A escada da temporada (docs/02 §5): os tiers, a posição que pode recuar e a coleção que não. */
data class EscadaDeProgresso(
    val tiers: List<TierNaEscada>,
    val posicao: Int,
    val colecaoMax: Int,
    val totalEspecies: Int,
    private val numeros: List<Int>,
) {
    val tierAtual: Int get() = 0

    /** Há recuo quando a posição ficou atrás da coleção (RN-37). */
    val recuou: Boolean get() = false

    fun marcasDoTier(ordem: Int): List<MarcaDaEscada> = emptyList()
}

fun escadaDe(temporada: Temporada, posicao: Int, colecaoMax: Int): EscadaDeProgresso =
    EscadaDeProgresso(emptyList(), posicao, colecaoMax, 0, emptyList())

/** O tier sob uma fração (0 a 1) da largura do nível 1, que tem segmentos de largura igual. */
fun tierNaFracao(fracao: Float, escada: EscadaDeProgresso): Int = 0

/** A posição sob uma fração da largura do nível 1: o tier pelo segmento, a espécie pela fração dentro dele. */
fun posicaoNaFracao(fracao: Float, escada: EscadaDeProgresso): Int = 0
