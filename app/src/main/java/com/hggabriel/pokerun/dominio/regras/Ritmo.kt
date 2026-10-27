package com.hggabriel.pokerun.dominio.regras

/**
 * Os ritmos, do mais rápido ao mais lento: o pace abaixo de [tetoSeg] (segundos por km) é o
 * ritmo daquela espécie. O último não tem teto. Nasceu na arena do registro (26/10/2026) e
 * mora aqui desde que a tela Ritmo do Progresso passou a usá-lo (estudo de Progresso e
 * Perfil, §e).
 *
 * @property numero o número da Pokédex da espécie do ritmo.
 */
enum class Ritmo(val tetoSeg: Long, val numero: Int) {
    JOLTEON(255, 135),
    RAPIDASH(300, 78),
    PONYTA(345, 77),
    GROWLITHE(390, 58),
    PIKACHU(450, 25),
    PSYDUCK(540, 54),
    SLOWPOKE(Long.MAX_VALUE, 79),
}

fun ritmoDe(paceSeg: Long): Ritmo = Ritmo.entries.first { paceSeg < it.tetoSeg }
