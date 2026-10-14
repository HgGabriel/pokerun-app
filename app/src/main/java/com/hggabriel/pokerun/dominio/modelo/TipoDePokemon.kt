package com.hggabriel.pokerun.dominio.modelo

/**
 * Os 18 tipos de Pokémon. É a chave das cores de tipo do tema (docs/02 §2.5) e o
 * único enum de tipo do app: quem precisar de tipo no domínio usa este, não cria
 * um segundo.
 *
 * [id] é o sufixo dos tokens `tipo-<id>` do design system, sem acento.
 */
enum class TipoDePokemon(val id: String) {
    NORMAL("normal"),
    FOGO("fogo"),
    AGUA("agua"),
    PLANTA("planta"),
    ELETRICO("eletrico"),
    GELO("gelo"),
    LUTADOR("lutador"),
    VENENOSO("venenoso"),
    TERRESTRE("terrestre"),
    VOADOR("voador"),
    PSIQUICO("psiquico"),
    INSETO("inseto"),
    PEDRA("pedra"),
    FANTASMA("fantasma"),
    DRAGAO("dragao"),
    SOMBRIO("sombrio"),
    ACO("aco"),
    FADA("fada"),
}
