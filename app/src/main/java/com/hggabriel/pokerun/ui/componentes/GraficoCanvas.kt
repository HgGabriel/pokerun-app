package com.hggabriel.pokerun.ui.componentes

/** Um ponto do gráfico: o rótulo do eixo X, o valor e o valor já escrito para leitura. */
data class PontoDoGrafico(val rotulo: String, val valor: Double, val valorEscrito: String)

data class EscalaDoEixo(val minimo: Double, val maximo: Double, val marcas: List<Double>)

fun escalaDoEixo(valores: List<Double>, apartirDoZero: Boolean = true): EscalaDoEixo = EscalaDoEixo(0.0, 0.0, emptyList())

fun descricaoFalada(titulo: String, pontos: List<PontoDoGrafico>, unidade: String): String = ""
