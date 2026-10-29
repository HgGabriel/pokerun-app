package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.ui.theme.FormaCirculo

/**
 * O selo da espécie: o sprite de jogo num disco branco. Na tela Fitas, é o Pokémon que a
 * pessoa era na semana do recorde, preso ao canto da fita (decisão 18).
 */
/** O sprite passa da borda do disco: a espera recua até caber nele. */
private const val RecuoDoBusto = 0.3f

@Composable
fun BustoDeEspecie(
    numero: Int,
    tamanho: Dp,
    modifier: Modifier = Modifier,
    fundo: Color = MaterialTheme.colorScheme.surface,
    silhueta: ColorFilter? = null,
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(FormaCirculo)
            .background(fundo),
        contentAlignment = Alignment.Center,
    ) {
        ImagemDaRede(
            model = spriteDeJogoDe(numero),
            filterQuality = FilterQuality.None,
            colorFilter = silhueta,
            recuoDaEspera = RecuoDoBusto,
            modifier = Modifier.requiredSize(tamanho * 1.4f),
        )
    }
}
