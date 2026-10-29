package com.hggabriel.pokerun.ui.componentes

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Ícones claros na barra de status, para a tela que põe tinta atrás dela. As telas da pilha
 * modal (criar plano, editar corrida) ficam fora da `CascaDeNavegacao`, que faz isso nas abas,
 * e ligam os ícones elas mesmas. Com [ativo] falso, ou ao sair, voltam a escuros (D-13).
 */
@Composable
fun IconesClarosNaBarraDeStatus(ativo: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(ativo) {
        val janela = (view.context as? Activity)?.window
        val controle = janela?.let { WindowCompat.getInsetsController(it, view) }
        controle?.isAppearanceLightStatusBars = !ativo
        onDispose { controle?.isAppearanceLightStatusBars = true }
    }
}
