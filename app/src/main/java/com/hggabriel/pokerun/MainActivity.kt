package com.hggabriel.pokerun

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.navegacao.NavegacaoDoApp
import com.hggabriel.pokerun.ui.theme.PokerunTheme

class MainActivity : ComponentActivity() {

    /**
     * A Activity resolve recursos em **pt-BR**, e não no idioma do aparelho (`F1-T20`,
     * docs/02 §7).
     *
     * ### O que isto conserta
     *
     * O `DatePicker` e o `TimePicker` do Material **não escrevem nada que o PokéRun tenha
     * escrito**: os rótulos vêm dos recursos da biblioteca, resolvidos pela configuração
     * do contexto. `LocaleDoApp` é pt-BR fixo e cobre tudo que o app formata — mês, dia
     * da semana, o `6,2` com vírgula —, e os dois diálogos passavam **por fora dele**.
     * Num aparelho em inglês, `Data da prova` abria `Select date`, `December 2026` e
     * `S M T W T F S` no meio de uma tela inteira em português, com `Cancel` e `OK`.
     * Achado em 18/08 com `F1-T16`, e valia desde 13/08 para `F1-T10`.
     *
     * ### Por que aqui, e não num provedor de `CompositionLocal`
     *
     * A primeira tentativa foi prover `LocalContext` em pt-BR em volta de cada diálogo.
     * **Não funciona, e o motivo não é óbvio:** o `Dialog` do Compose se constrói com o
     * `Context` da *View* hospedeira, não com o `LocalContext` do ponto de chamada, e a
     * subcomposição dele reprovê `LocalContext` a partir dali. O provedor era ignorado e
     * o calendário continuava em inglês — verde no build, errado na tela.
     *
     * O contexto da Activity é o que o diálogo herda de fato. Vale para os dois seletores
     * de hoje e para qualquer diálogo do Material que ainda venha.
     *
     * ### Por que não `LocaleManager` nem `appcompat`
     *
     * `LocaleManager.setApplicationLocales` é API 33 e o piso do app é 26; `appcompat` é
     * **dependência nova**, o único tipo de decisão que ainda para a sessão
     * (`EXECUCAO.md §7`). Isto é uma sobrescrita e nenhuma linha de dependência.
     *
     * **Não mexe no `Locale.getDefault()` do processo**: o que o app formata continua
     * passando por `LocaleDoApp` explicitamente, como sempre passou.
     */
    override fun attachBaseContext(base: Context) {
        val configuracao = Configuration(base.resources.configuration)
        configuracao.setLocale(LocaleDoApp)
        super.attachBaseContext(base.createConfigurationContext(configuracao))
    }

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
