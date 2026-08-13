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
 * Para achar todos: `grep -rn "EmConstrucao" app/src`.
 */
@Composable
fun EmConstrucao(
    tela: String,
    tarefa: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tela,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = tarefa,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
