package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.hggabriel.pokerun.dados.rede.spriteEmPixel
import com.hggabriel.pokerun.dados.rede.spritesAnimadosDe
import com.hggabriel.pokerun.ui.theme.Movimento

/**
 * O Pokémon animado em 3D, a altura de cena de docs/02 §4.4: tenta as fontes de
 * [spritesAnimadosDe] em ordem e passa à seguinte quando a espécie falta numa delas. Só a
 * pixel art do Showdown amplia sem suavização.
 *
 * [apagado] é a silhueta de RN-36, em cinza a 40%, que acorda quando a tela recebe o dado que
 * a faz andar (a distância no registro, a curva na criação de plano).
 */
@Composable
fun SpriteAnimado(numero: Int, modifier: Modifier = Modifier, apagado: Boolean = false) {
    val fontes = remember(numero) { spritesAnimadosDe(numero) }
    var indice by remember(numero) { mutableIntStateOf(0) }
    val url = fontes[indice]
    val pixel = spriteEmPixel(url)
    val alfa by animateFloatAsState(if (apagado) 0.4f else 1f, tween(Movimento.Duracao.MEDIA), label = "acordar")
    val filtro = remember(apagado) {
        if (apagado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        filterQuality = if (pixel) FilterQuality.None else FilterQuality.High,
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
        colorFilter = filtro,
        alpha = alfa,
        onSuccess = { if (pixel) it.result.drawable.isFilterBitmap = false },
        onError = { if (indice < fontes.lastIndex) indice++ },
        modifier = modifier,
    )
}
