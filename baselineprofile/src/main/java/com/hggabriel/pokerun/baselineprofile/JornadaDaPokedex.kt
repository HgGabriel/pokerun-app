package com.hggabriel.pokerun.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/** Quantas vezes a grade é arremessada para baixo: o bastante para compor todos os tiers. */
private const val ARREMESSOS = 6

/**
 * Abre a aba `Pokédex` e rola a grade inteira, para o perfil levar a composição e a
 * medição das células. Sem isso, o primeiro scroll da grade roda interpretado e engasga.
 *
 * A aba só existe depois do primeiro fechamento semanal, e só com login. Sem ela, a
 * jornada termina sem erro: o perfil fica com a partida a frio, como antes.
 */
fun MacrobenchmarkScope.rolarAPokedex() {
    val aba = device.wait(Until.findObject(By.text("Pokédex")), 3_000) ?: return
    aba.click()
    val grade = device.wait(Until.findObject(By.scrollable(true)), 5_000) ?: return
    // Margem nas bordas, para o gesto não começar sobre a barra do sistema.
    grade.setGestureMargin(device.displayWidth / 5)
    repeat(ARREMESSOS) { grade.fling(Direction.DOWN) }
    grade.fling(Direction.UP)
    device.waitForIdle()
}
