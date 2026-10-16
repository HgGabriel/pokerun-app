package com.hggabriel.pokerun.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACOTE = "com.hggabriel.pokerun"

/** O que a partida a frio percorre: a splash, a abertura inteira e a primeira tela. */
fun MacrobenchmarkScope.partidaAFrio() {
    pressHome()
    startActivityAndWait()
    // A barra inferior compõe por baixo da abertura. Esperar o rótulo e o fim das
    // animações cobre a abertura até o repouso das molas e a primeira tela pronta.
    device.wait(Until.hasObject(By.text("Hoje")), 5_000)
    device.waitForIdle()
}
