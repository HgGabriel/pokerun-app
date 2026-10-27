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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.repeatCount
import com.hggabriel.pokerun.dados.rede.spriteEmPixel
import com.hggabriel.pokerun.dados.rede.spritesAnimadosDe
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento

/** Na lista de [spritesAnimadosDe], a primeira fonte parada: o render do HOME. */
private const val PRIMEIRA_PARADA = 4

/**
 * O Pokémon animado em 3D, a altura de cena de docs/02 §4.4: tenta as fontes de
 * [spritesAnimadosDe] em ordem e passa à seguinte quando a espécie falta numa delas. Só a
 * pixel art do Showdown amplia sem suavização.
 *
 * [apagado] é a silhueta de RN-36, em cinza a 40%, que acorda quando a tela recebe o dado que
 * a faz andar (a distância no registro, a curva na criação de plano).
 *
 * @param umCiclo toca a animação uma vez e para no último quadro (o álbum da Pokédex). Sem
 *   ele, a animação segue em loop, como na arena do registro.
 * @param ciclo muda para tocar o ciclo de novo, como no toque sobre a figurinha.
 * @param silhueta pinta o sprite chapado nesta cor, a vaga do álbum (`SrcIn`, nunca opacidade).
 */
@Composable
fun SpriteAnimado(
    numero: Int,
    modifier: Modifier = Modifier,
    apagado: Boolean = false,
    umCiclo: Boolean = false,
    ciclo: Int = 0,
    silhueta: ColorFilter? = null,
) {
    // Com movimento reduzido o 3D fica parado: começa no render do HOME.
    val reduzido = LocalMovimentoReduzido.current && umCiclo
    val fontes = remember(numero, reduzido) { spritesAnimadosDe(numero).drop(if (reduzido) PRIMEIRA_PARADA else 0) }
    var indice by remember(numero, reduzido) { mutableIntStateOf(0) }
    val url = fontes[indice]
    val pixel = spriteEmPixel(url)
    val alfa by animateFloatAsState(if (apagado) 0.4f else 1f, tween(Movimento.Duracao.MEDIA), label = "acordar")
    val filtro = remember(apagado, silhueta) {
        silhueta ?: if (apagado) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    }
    val contexto = LocalContext.current
    val modelo = remember(url, umCiclo, ciclo) {
        if (!umCiclo) url else ImageRequest.Builder(contexto).data(url).repeatCount(0).build()
    }
    AsyncImage(
        model = modelo,
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
