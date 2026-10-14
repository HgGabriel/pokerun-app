package com.hggabriel.pokerun.ui.theme

import androidx.compose.ui.unit.dp

/**
 * A grade de 4/8 do design system (docs/02 §4). Um token por papel, não por valor:
 * `margem` é "margem lateral da tela", e o 16 é consequência.
 */
object Espacos {
    /** Entre ícone e rótulo dentro de chip; entre linhas de um mesmo dado. */
    val minimo = 4.dp
    /** Entre itens relacionados: chips de tipo, rótulo e valor, células da grade da Pokédex. */
    val relacionado = 8.dp
    /** Padding interno de chip grande, campo e item da Pokédex. */
    val interno = 12.dp
    /** Margem lateral da tela e padding de card. A 360dp sobram 328dp de conteúdo. */
    val margem = 16.dp
    /** Entre cards e blocos da mesma tela. */
    val bloco = 24.dp
    /** Entre seções, como o painel e a grade da Pokédex. */
    val secao = 32.dp
    /** Altura e largura mínimas de todo alvo tocável. Fica aqui porque o token é do grupo `spacing`. */
    val alvoToque = 48.dp
}

/** Os tamanhos fixos de ícone, sprite e arte (docs/02 §4). */
object Tamanhos {
    /** Ícone de interface, traço 1,75. */
    val icone = 24.dp
    /** Ícone de tipo dentro do chip e no item da Pokédex. */
    val iconeTipo = 16.dp
    /** Sprite de jogo no item da Pokédex: 96px de origem, pixelado. */
    val spriteLista = 72.dp
    /** Avatar de membro: sprite de jogo recortado sobre `tipo-*-suave`. */
    val avatar = 48.dp
    /** Arte oficial no card de espécie. */
    val arteCard = 152.dp
    /** Arte oficial no detalhe da espécie. */
    val arteDetalhe = 232.dp
}
