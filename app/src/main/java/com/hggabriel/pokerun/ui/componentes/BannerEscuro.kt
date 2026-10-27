package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** O Pokémon grande, à direita. Pixel art de 96px: a espécie lê no miolo. */
private val SpriteDoBanner = 132.dp

/** A fração da largura que o texto ocupa quando há sprite; o resto é do Pokémon. */
private const val LARGURA_DO_TEXTO = 0.64f

private const val OPACIDADE_DA_SOBRANCELHA = 0.72f
private const val OPACIDADE_DO_BRILHO = 0.6f
private const val OPACIDADE_DA_POKEBOLA = 0.08f

private val AvatarPadrao = 30.dp
private val PassoPadrao = 20.dp
private val AnelDaPilha = 2.dp
private const val MAXIMO_NA_PILHA = 5

/**
 * O banner em tinta (26/10/2026): o fundo escuro, o brilho de [brilho] à direita, a Pokébola
 * em marca d'água e, se houver, o [sprite] grande por cima. É a peça de personalidade das
 * telas do grupo: o banner do detalhe do plano, o pulso da Home e o desafio da aba Grupo.
 *
 * Com [aoTocar] o banner inteiro é o alvo, e a [descricao] é o nó único do TalkBack.
 */
@Composable
fun BannerEscuro(
    descricao: String,
    aoTocar: (() -> Unit)?,
    modifier: Modifier = Modifier,
    brilho: Color = MaterialTheme.colorScheme.primary,
    sprite: String? = null,
    /** O número do Pokémon em 3D no lugar de [sprite]: o Let's Go, com as reservas. */
    especie: Int? = null,
    alturaMinima: Dp = 0.dp,
    /** `None` para pixel art; `Medium` para o render do HOME, que é ilustração. */
    filtroDoSprite: FilterQuality = FilterQuality.None,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val toque = if (aoTocar != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            onClick = aoTocar,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = alturaMinima)
            .clip(FormaDoCartao)
            .background(tinta)
            .then(toque)
            .clearAndSetSemantics {
                contentDescription = descricao
                if (aoTocar != null) role = Role.Button
            },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(Brush.horizontalGradient(0.3f to Color.Transparent, 1f to brilho.copy(alpha = OPACIDADE_DO_BRILHO)))
            // A Pokébola em marca d'água, cortada pela borda do banner.
            val raio = size.height * 0.62f
            val centro = Offset(size.width - raio * 0.45f, size.height * 0.62f)
            val traco = raio * 0.14f
            val marca = claro.copy(alpha = OPACIDADE_DA_POKEBOLA)
            drawCircle(marca, raio, centro, style = Stroke(traco))
            drawLine(marca, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), traco)
            drawCircle(marca, raio * 0.3f, centro, style = Stroke(traco))
        }

        if (especie != null) {
            SpriteAnimado(
                numero = especie,
                paraNoReduzido = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Espacos.relacionado)
                    .requiredSize(SpriteDoBanner),
            )
        } else if (sprite != null) {
            AsyncImage(
                model = sprite,
                contentDescription = null,
                filterQuality = filtroDoSprite,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Espacos.relacionado)
                    .requiredSize(SpriteDoBanner),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(if (sprite != null || especie != null) LARGURA_DO_TEXTO else 1f)
                .padding(Espacos.margem),
            content = conteudo,
        )
    }
}

/** A sobrancelha do banner, em caixa alta e `superficie` a 72%. */
@Composable
fun SobrancelhaDoBanner(texto: String) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = Estilos.rotulo,
        color = MaterialTheme.colorScheme.surface.copy(alpha = OPACIDADE_DA_SOBRANCELHA),
    )
}

/** O título do banner, em `titleLarge` e `superficie`. */
@Composable
fun TituloDoBanner(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.surface)
}

/** A pílula de ação do banner (`Ver grupo`). Não é botão: o alvo é o banner inteiro. */
@Composable
fun PilulaDoBanner(texto: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, FormaControle)
            .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
    ) {
        Text(text = texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Um avatar da pilha: o busto do [treinador] (ou a silhueta, sem escolha) e o anel em
 * `destaque` quando [aceso] — quem correu na semana. Desde 27/10/2026 a pilha é da pessoa,
 * e não do Pokémon nem da inicial do nome (decisão 23).
 */
data class AvatarEmPilha(
    val treinador: String?,
    val aceso: Boolean = false,
)

/** Os avatares sobrepostos, até cinco. O anel em [fundo] separa um do outro. */
@Composable
fun PilhaDeAvatares(
    avatares: List<AvatarEmPilha>,
    fundo: Color,
    tamanho: Dp = AvatarPadrao,
    passo: Dp = PassoPadrao,
) {
    val visiveis = avatares.take(MAXIMO_NA_PILHA)
    val largura = tamanho + passo * (visiveis.size - 1).coerceAtLeast(0)
    val vazio = LocalCoresPokerun.current.superficieAfundada
    Box(modifier = Modifier.size(width = largura, height = tamanho)) {
        visiveis.forEachIndexed { i, avatar ->
            BustoDoTreinador(
                treinador = avatar.treinador,
                tamanho = tamanho,
                fundo = vazio,
                modifier = Modifier
                    .offset(x = passo * i)
                    .border(AnelDaPilha, if (avatar.aceso) MaterialTheme.colorScheme.primary else fundo, FormaCirculo),
            )
        }
    }
}
