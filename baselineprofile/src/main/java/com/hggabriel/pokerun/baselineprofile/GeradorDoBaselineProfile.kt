package com.hggabriel.pokerun.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Coleta os métodos que a partida a frio e o scroll da Pokédex executam. As duas coletas
 * se somam no Baseline Profile.
 */
@RunWith(AndroidJUnit4::class)
class GeradorDoBaselineProfile {

    @get:Rule
    val regra = BaselineProfileRule()

    /**
     * `includeInStartupProfile` também gera o perfil de partida, que o R8 usa para pôr
     * essas classes juntas no dex principal.
     */
    @Test
    fun gerar() = regra.collect(packageName = PACOTE, includeInStartupProfile = true) {
        partidaAFrio()
    }

    /**
     * Fora do perfil de partida: as classes da grade não são da abertura, e no dex
     * principal só atrasariam a partida a frio.
     */
    @Test
    fun gerarPokedex() = regra.collect(packageName = PACOTE) {
        partidaAFrio()
        rolarAPokedex()
    }
}
