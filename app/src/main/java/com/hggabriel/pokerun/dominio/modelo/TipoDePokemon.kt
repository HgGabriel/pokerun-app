package com.hggabriel.pokerun.dominio.modelo

/**
 * Os 18 tipos de Pokémon. É a chave das cores de tipo do tema (docs/02 §2.5) e o
 * único enum de tipo do app: quem precisar de tipo no domínio usa este, não cria
 * um segundo.
 *
 * [id] é o sufixo dos tokens `tipo-<id>` do design system, sem acento. [nomeNaPokeApi] é o
 * `name` do tipo em `pokemon/{id}`, em inglês.
 */
enum class TipoDePokemon(val id: String, val nomeNaPokeApi: String) {
    NORMAL("normal", "normal"),
    FOGO("fogo", "fire"),
    AGUA("agua", "water"),
    PLANTA("planta", "grass"),
    ELETRICO("eletrico", "electric"),
    GELO("gelo", "ice"),
    LUTADOR("lutador", "fighting"),
    VENENOSO("venenoso", "poison"),
    TERRESTRE("terrestre", "ground"),
    VOADOR("voador", "flying"),
    PSIQUICO("psiquico", "psychic"),
    INSETO("inseto", "bug"),
    PEDRA("pedra", "rock"),
    FANTASMA("fantasma", "ghost"),
    DRAGAO("dragao", "dragon"),
    SOMBRIO("sombrio", "dark"),
    ACO("aco", "steel"),
    FADA("fada", "fairy");

    companion object {
        /** O tipo pelo `name` da PokéAPI (`grass`), ou nulo para um tipo que o app não conhece. */
        fun daPokeApi(nome: String): TipoDePokemon? = entries.firstOrNull { it.nomeNaPokeApi == nome }
    }
}
