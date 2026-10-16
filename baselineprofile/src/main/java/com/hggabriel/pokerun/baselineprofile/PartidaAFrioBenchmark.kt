package com.hggabriel.pokerun.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A partida a frio sem compilação nenhuma, que é o primeiro uso depois de instalar sem
 * perfil, contra a partida com o Baseline Profile. `FrameTimingMetric` mede os quadros da
 * abertura.
 */
@RunWith(AndroidJUnit4::class)
class PartidaAFrioBenchmark {

    @get:Rule
    val regra = MacrobenchmarkRule()

    @Test
    fun semCompilacao() = medir(CompilationMode.None())

    @Test
    fun comBaselineProfile() = medir(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun medir(modo: CompilationMode) = regra.measureRepeated(
        packageName = PACOTE,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = modo,
        startupMode = StartupMode.COLD,
        iterations = 10,
    ) {
        partidaAFrio()
    }
}
