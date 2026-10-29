package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FormaDoEnvelope = RoundedCornerShape(6.dp)

/**
 * O pacote de figurinhas (docs/03 §3.18): um envelope inclinado no canto da capa com o
 * número de figurinhas novas. Tocar rasga a aba e o envelope some; quem chama cola as
 * figurinhas depois de [aoAbrir]. Com movimento reduzido, abre direto.
 */
@Composable
internal fun PacoteDeFigurinhas(quantas: Int, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val reduzido = LocalMovimentoReduzido.current
    val escopo = rememberCoroutineScope()
    val aba = remember { Animatable(0f) }
    val saida = remember { Animatable(0f) }
    var abrindo by remember { mutableStateOf(false) }
    val descricao = pluralStringResource(R.plurals.pokedex_pacote, quantas, quantas)

    fun abrir() {
        if (abrindo) return
        abrindo = true
        if (reduzido) {
            aoAbrir()
            return
        }
        escopo.launch {
            aba.animateTo(1f, Movimento.padrao())
            saida.animateTo(1f, Movimento.saida())
            aoAbrir()
        }
    }

    Box(
        modifier = modifier
            .size(width = 70.dp, height = 52.dp)
            .graphicsLayer {
                rotationZ = -9f
                translationY = -20.dp.toPx() * saida.value
                val escala = 1f - 0.4f * saida.value
                scaleX = escala
                scaleY = escala
                alpha = 1f - saida.value
            }
            .clickable(onClick = ::abrir)
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { abrir(); true }
            },
    ) {
        // O corpo, com as duas dobras de baixo.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(6.dp, FormaDoEnvelope)
                .background(claro, FormaDoEnvelope),
        ) {
            val dobra = tinta.copy(alpha = 0.12f)
            val traco = 1.dp.toPx()
            drawLine(dobra, Offset(0f, size.height), Offset(size.width / 2, size.height * 0.45f), traco)
            drawLine(dobra, Offset(size.width, size.height), Offset(size.width / 2, size.height * 0.45f), traco)
        }
        // A aba: vira para cima ao rasgar.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    rotationX = 180f * aba.value
                    cameraDistance = 12f * density
                },
        ) {
            val forma = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height * 0.58f)
                close()
            }
            drawPath(forma, afundada)
            drawPath(forma, tinta.copy(alpha = 0.08f), style = Stroke(1.dp.toPx()))
        }
        // O selo em destaque, no bico da aba.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .size(16.dp)
                .graphicsLayer { alpha = 1f - aba.value }
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
        )
        Text(
            text = quantas.toString(),
            style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily)),
            color = tinta,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .widthIn(min = 22.dp)
                .border(2.dp, tinta, RoundedCornerShape(50))
                .background(claro, RoundedCornerShape(50))
                .padding(horizontal = 4.dp, vertical = 3.dp),
        )
    }
}

/**
 * A batida do carimbo ao colar (docs/03 §3.18): a figurinha chega maior, torta e alta, cai no
 * lugar, afunda um pouco com o tranco e assenta. Até o [atraso] passar, ela não aparece. Nulo,
 * fica parada no lugar.
 *
 * Uma linha do tempo só, linear em [Movimento.Album.COLAR], e cada canal com a curva dele: a
 * queda e o giro na curva padrão, a escala na de captura (que passa do alvo e dá o afundar), e
 * o brilho acendendo no primeiro quinto. Antes, a figurinha surgia inteira num quadro e só
 * encolhia, e o fim do pacote cortava no meio quem ainda estava no ar.
 *
 * Se o atraso some no meio (o toque que pula a cascata, ou o fim do pacote), quem não começou
 * vai direto ao lugar e quem está no ar termina a queda, sem salto.
 */
@Composable
internal fun Modifier.batida(atraso: Int?): Modifier {
    val progresso = remember { Animatable(if (atraso != null) 0f else 1f) }
    // Zera na mesma composição em que o atraso chega: a figurinha não aparece pronta por um quadro.
    var partiu by remember(atraso) { mutableStateOf(false) }
    // A camada só existe enquanto a figurinha cola. Assentada, ela some, e o selo `nova` e a
    // inclinação voltam a passar da caixa sem recorte.
    var colando by remember { mutableStateOf(atraso != null) }
    LaunchedEffect(atraso) {
        if (atraso != null) {
            colando = true
            progresso.snapTo(0f)
            partiu = true
            delay(atraso.toLong())
            progresso.animateTo(1f, Movimento.lento(curva = LinearEasing))
        } else if (progresso.value == 0f) {
            progresso.snapTo(1f)
        } else if (progresso.value < 1f) {
            progresso.animateTo(1f, Movimento.padrao())
        }
        colando = false
    }
    if (atraso == null && !colando) return this
    // Vizinhas caem de lados alternados.
    val lado = if ((atraso ?: 0) / Movimento.Album.ENTRE_FIGURINHAS % 2 == 0) -1f else 1f
    return graphicsLayer {
        val t = if (atraso != null && !partiu) 0f else progresso.value
        if (t >= 1f) return@graphicsLayer
        val queda = 1f - Movimento.Curva.padrao.transform(t)
        val carimbo = Movimento.Curva.captura.transform(t)
        // O alfa entra modulado, sem buffer fora da tela: o buffer recorta na caixa da figurinha.
        compositingStrategy = CompositingStrategy.ModulateAlpha
        alpha = (t * 5f).coerceIn(0f, 1f)
        val escala = 1.45f - 0.45f * carimbo
        scaleX = escala
        scaleY = escala
        translationY = -28.dp.toPx() * queda
        rotationZ = 10f * lado * queda
    }
}
