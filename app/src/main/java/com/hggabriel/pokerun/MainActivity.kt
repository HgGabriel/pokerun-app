package com.hggabriel.pokerun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.hggabriel.pokerun.ui.navegacao.NavegacaoDoApp
import com.hggabriel.pokerun.ui.theme.PokerunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras de sistema fixas em claro: o app não segue o tema do aparelho (D-13).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            // O container vem da PokerunApp, nunca de um `AppContainer()` montado
            // aqui: a Activity morre e renasce na rotação, e o grafo de dependências
            // do app não pode renascer junto (F0-T04).
            CompositionLocalProvider(
                LocalAppContainer provides (application as PokerunApp).container,
            ) {
                PokerunTheme {
                    NavegacaoDoApp()
                }
            }
        }
    }
}
