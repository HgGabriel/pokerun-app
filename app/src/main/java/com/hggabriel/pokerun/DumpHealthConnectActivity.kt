package com.hggabriel.pokerun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hggabriel.pokerun.ui.telas.dump.DumpScreen
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * O instrumento de medição do Health Connect (`F0-T09`). **Descartável.**
 *
 * Ela existe para que `F0-T10` possa preencher a tabela campo × participante nos
 * oito aparelhos, e some junto com a tabela preenchida. Apagar `ui/telas/dump`,
 * este arquivo, o bloco do `AndroidManifest.xml` e o bloco cercado do
 * `strings.xml` devolve o projeto ao estado anterior. Nada mais depende dela.
 *
 * É uma `Activity` própria, e não uma rota do grafo, pelo mesmo motivo da
 * `PoliticaDePrivacidadeActivity`: o grafo é `F1-T07` e ainda não existe. Aqui há
 * um motivo a mais, que é a disposabilidade — uma rota deixaria rastro no grafo
 * definitivo depois de a tela sumir.
 *
 * **Não provê o `LocalAppContainer`.** O dump não escreve no Firestore: quem grava
 * corrida é a ingestão da Fase 2, e este aqui só lê e imprime.
 */
class DumpHealthConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PokerunTheme {
                DumpScreen()
            }
        }
    }
}
