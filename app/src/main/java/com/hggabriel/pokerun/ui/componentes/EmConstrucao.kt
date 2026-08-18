package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * O lugar de uma tela que ainda não existe.
 *
 * **É andaime de navegação, e some sozinho:** cada uso nomeia a tarefa dona, e a
 * tarefa dona substitui a chamada pela tela de verdade. `F1-T07` precisa dele porque
 * o grafo é construído antes das telas — duas das três raízes de aba são de fases
 * seguintes (`StatsDashboardScreen` é `F3-T09`, `SocialLeaderboardScreen` é `F2-T12`)
 * e sem um corpo a aba não navega.
 *
 * **Não imita a tela futura.** Nada de skeleton, nada de dado falso, nada de
 * `picsum.photos`: um esqueleto convincente é indistinguível de tela pronta na
 * revisão, e este é o tipo de resíduo que atravessa fase inteira sem ninguém notar.
 * Ele diz o que falta e quem faz.
 *
 * **A raiz de aba é a exceção, e ela veio de `F1-T07c`.** `docs/02 §10.1` põe a
 * engrenagem de Ajustes na raiz de **toda** aba, e duas das três raízes são este
 * andaime até `F2-T12` e `F3-T09` chegarem. Sem [aba] e [aoAbrirAjustes] o app teria a
 * engrenagem só em `Hoje`, e o destino por aba que `F1-T07c` construiu ficaria sem
 * como ser exercido em duas das três abas — inclusive no aparelho, que é onde a ficha
 * manda conferir. Isso **não** é imitar a tela futura: é o cromo da aba, não o
 * conteúdo dela.
 *
 * Para achar todos: `grep -rn "EmConstrucao" app/src`.
 *
 * @param aba o rótulo da aba, quando este andaime é a **raiz** de uma. Vem junto com
 *   [aoAbrirAjustes]: um sem o outro deixa o cabeçalho sem caminho ou sem engrenagem,
 *   e nenhum dos dois é meia regra que valha desenhar.
 */
@Composable
fun EmConstrucao(
    tela: String,
    tarefa: String,
    modifier: Modifier = Modifier,
    aba: String? = null,
    aoAbrirAjustes: (() -> Unit)? = null,
) {
    val raizDeAba = aba != null && aoAbrirAjustes != null

    Column(modifier = modifier.fillMaxSize()) {
        if (aba != null && aoAbrirAjustes != null) {
            CabecalhoDeAba(
                aba = aba,
                titulo = tela,
                aoAbrirAjustes = aoAbrirAjustes,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Com cabeçalho, o nome da tela já é o título dele. Repetir aqui embaixo
            // seria a mesma palavra duas vezes na mesma tela.
            if (!raizDeAba) {
                Text(
                    text = tela,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = tarefa,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
