package com.hggabriel.pokerun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hggabriel.pokerun.ui.telas.politica.PoliticaDePrivacidadeScreen
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * O destino de `ACTION_SHOW_PERMISSIONS_RATIONALE` (docs/05 §4.4).
 *
 * **Sem esta Activity o Health Connect não concede permissão nenhuma**, e o
 * onboarding trava no passo 3 sem dizer por quê: o Health Connect não avisa que
 * falta a política, ele só não oferece a concessão. Por isso a tarefa é da Fase 0
 * e não da Fase 2, junto do resto da ingestão.
 *
 * Ela é uma segunda `Activity` e não uma rota do grafo de navegação porque quem a
 * lança é outro app, com o PokéRun fechado. Uma rota exigiria a `MainActivity` de
 * pé e um deep link, e o usuário cairia dentro do app quando só quis ler o texto.
 *
 * **Não provê o `LocalAppContainer`.** A tela é texto constante: não lê Firestore,
 * não pede autenticação e não toca no Health Connect. Prover o container aqui
 * criaria os clientes do Firebase para exibir cinco parágrafos.
 */
class PoliticaDePrivacidadeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mesmas barras claras da MainActivity: o app não segue o tema do
        // aparelho (D-13), e esta janela abre a partir de um app que segue.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PokerunTheme {
                PoliticaDePrivacidadeScreen(aoFechar = { finish() })
            }
        }
    }
}
