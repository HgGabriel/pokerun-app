package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaDado
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * A espera de tudo o que ainda não chegou: o bloco na forma do conteúdo, que pulsa
 * (27/10/2026, revoga o esqueleto estático de docs/design.md §9). Um bloco parado não diz
 * se o app está trabalhando ou travou; o pulso diz.
 *
 * Todos os esqueletos pulsam juntos, porque a fase sai do relógio do quadro e não do
 * momento em que cada um entrou. Com movimento reduzido, o bloco fica parado em [cor].
 *
 * @param forma a forma do conteúdo que vai chegar.
 * @param cor a cor do bloco no ponto alto do pulso.
 * @param recuo a fração do menor lado que o bloco recua de cada borda: o sprite ocupa o
 *   miolo da caixa, e o bloco cheio leria como um cartão.
 */
@Composable
fun Modifier.esqueleto(
    forma: Shape = MaterialTheme.shapes.medium,
    cor: Color = MaterialTheme.colorScheme.outlineVariant,
    recuo: Float = 0f,
): Modifier = this.then(ElementoDoEsqueleto(cor, forma, recuo, pulsa = !LocalMovimentoReduzido.current))

/** O bloco parado: a vaga do que não chegou e não vai chegar, como a imagem que falhou. */
@Composable
fun Modifier.vagaParada(
    forma: Shape = MaterialTheme.shapes.medium,
    cor: Color = MaterialTheme.colorScheme.outlineVariant,
    recuo: Float = 0f,
): Modifier = this.then(ElementoDoEsqueleto(cor, forma, recuo, pulsa = false))

/**
 * A imagem da rede com a espera desenhada por baixo: enquanto ela carrega, o [esqueleto]
 * pulsa no lugar dela. Se ela falha e [aoFalhar] não troca o [model], a vaga fica parada.
 *
 * @param formaDaEspera e [recuoDaEspera] desenham a espera: por padrão, um disco no miolo
 *   da caixa, do tamanho de um sprite.
 */
@Composable
fun ImagemDaRede(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = FilterQuality.Low,
    formaDaEspera: Shape = FormaCirculo,
    recuoDaEspera: Float = RecuoDoSprite,
    corDaEspera: Color = MaterialTheme.colorScheme.outlineVariant,
    aoCarregar: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    aoFalhar: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    var fase by remember(model) { mutableStateOf(FaseDaImagem.ESPERANDO) }
    val espera = when (fase) {
        FaseDaImagem.ESPERANDO -> Modifier.esqueleto(formaDaEspera, corDaEspera, recuoDaEspera)
        FaseDaImagem.FALHOU -> Modifier.vagaParada(formaDaEspera, corDaEspera, recuoDaEspera)
        FaseDaImagem.PRONTA -> Modifier
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        onState = { estado ->
            fase = when (estado) {
                is AsyncImagePainter.State.Success -> FaseDaImagem.PRONTA.also { aoCarregar?.invoke(estado) }
                is AsyncImagePainter.State.Error -> FaseDaImagem.FALHOU.also { aoFalhar?.invoke(estado) }
                else -> FaseDaImagem.ESPERANDO
            }
        },
        modifier = modifier.then(espera),
    )
}

/** Um bloco de [altura] em espera: a linha de texto ou o número que ainda vai chegar. */
@Composable
fun BlocoEmEspera(
    altura: Dp,
    modifier: Modifier = Modifier,
    forma: Shape = FormaDado,
    cor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Spacer(modifier.height(altura).esqueleto(forma, cor))
}

/**
 * Cartões em espera, um embaixo do outro e na largura toda. A cor padrão é a `superficie`
 * do cartão, para a página tingida; sobre fundo branco, passe `outlineVariant`.
 */
@Composable
fun CartoesEmEspera(
    alturas: List<Dp>,
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.surface,
    espaco: Dp = Espacos.interno,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(espaco)) {
        alturas.forEach { altura ->
            Spacer(Modifier.fillMaxWidth().height(altura).esqueleto(FormaDoCartao, cor))
        }
    }
}

/** O recuo da espera de um sprite: o Pokémon ocupa o miolo da imagem, com ar em volta. */
const val RecuoDoSprite = 0.22f

private enum class FaseDaImagem { ESPERANDO, PRONTA, FALHOU }

/** Um ciclo do pulso, do ponto alto ao baixo e de volta. */
private const val CICLO_DO_PULSO = 1400L

/** A intensidade no ponto baixo do pulso. */
private const val FUNDO_DO_PULSO = 0.4f

private data class ElementoDoEsqueleto(
    val cor: Color,
    val forma: Shape,
    val recuo: Float,
    val pulsa: Boolean,
) : ModifierNodeElement<NoDoEsqueleto>() {
    override fun create() = NoDoEsqueleto(cor, forma, recuo, pulsa)

    override fun update(node: NoDoEsqueleto) {
        node.cor = cor
        node.forma = forma
        node.recuo = recuo
        if (node.pulsa != pulsa) {
            node.pulsa = pulsa
            node.ligarPulso()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "esqueleto"
    }
}

private class NoDoEsqueleto(
    var cor: Color,
    var forma: Shape,
    var recuo: Float,
    var pulsa: Boolean,
) : Modifier.Node(), DrawModifierNode {

    private var intensidade = 1f
    private var pulso: Job? = null

    override fun onAttach() = ligarPulso()

    fun ligarPulso() {
        pulso?.cancel()
        pulso = null
        intensidade = 1f
        if (!isAttached) return
        if (pulsa) {
            pulso = coroutineScope.launch {
                while (isActive) {
                    withFrameMillis { agora ->
                        val fase = (agora % CICLO_DO_PULSO).toFloat() / CICLO_DO_PULSO
                        intensidade = FUNDO_DO_PULSO + (1f - FUNDO_DO_PULSO) * (0.5f + 0.5f * cos(2f * PI.toFloat() * fase))
                    }
                    invalidateDraw()
                }
            }
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val borda = min(size.width, size.height) * recuo
        val area = Size(size.width - 2 * borda, size.height - 2 * borda)
        if (area.width > 0f && area.height > 0f) {
            translate(borda, borda) {
                drawOutline(forma.createOutline(area, layoutDirection, this), cor, alpha = intensidade)
            }
        }
        drawContent()
    }
}
