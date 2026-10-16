package com.hggabriel.pokerun.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Coleta os métodos que a partida a frio executa. `includeInStartupProfile` também gera o
 * perfil de partida, que o R8 usa para pôr essas classes juntas no dex principal.
 */
@RunWith(AndroidJUnit4::class)
class GeradorDoBaselineProfile {

    @get:Rule
    val regra = BaselineProfileRule()

    @Test
    fun gerar() = regra.collect(packageName = PACOTE, includeInStartupProfile = true) {
        partidaAFrio()
    }
}
