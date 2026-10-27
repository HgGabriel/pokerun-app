package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

private val AlturaDoBanner = 156.dp

/** O seu Pokémon, grande, à direita. Pixel art de 96px: a espécie lê no miolo. */
private val SpriteDoBanner = 132.dp

private val AvatarDaPilha = 30.dp
private val PassoDaPilha = 20.dp
private val AnelDaPilha = 2.dp
private const val MAXIMO_NA_PILHA = 5

/** A fração da largura que o texto ocupa; o resto é do Pokémon. */
private const val LARGURA_DO_TEXTO = 0.64f

private const val OPACIDADE_DA_SOBRANCELHA = 0.72f
private const val OPACIDADE_DO_BRILHO = 0.6f
private const val OPACIDADE_DA_POKEBOLA = 0.08f

/**
 * O banner do grupo: fundo em tinta, o brilho da cor do tipo do seu Pokémon à direita, a
 * Pokébola em marca d'água e o sprite grande por cima. À esquerda, a sobrancelha, quantos
 * correram nesta semana, a pílula `Ver grupo` e os avatares sobrepostos.
 *
 * Com [aoTocar] o banner inteiro é o alvo e a pílula aparece; sem ele, na própria tela do
 * grupo, é só o cabeçalho dela.
 *
 * O anel dos avatares segue o pulso da Home: `destaque` em quem correu na semana, a cor do
 * fundo em quem não correu.
 */
@Composable
internal fun BannerDoGrupo(
    estado: DetalhePlanoUiState.Conteudo,
    aoTocar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val membros = estado.progresso
    val semana = estado.semanaCorrente
    val correu: (ProgressoDoMembro) -> Boolean = { m -> semana != null && (m.na(semana)?.sessoesFeitas ?: 0) >= 1 }
    val solo = membros.size <= 1
    val sobrancelha = stringResource(if (semana != null && !solo) R.string.home_grupo_rotulo else R.string.detalhe_grupo_rotulo)
    val titulo = when {
        solo -> stringResource(R.string.home_grupo_solo)
        semana != null -> membros.count(correu).let { pluralStringResource(R.plurals.home_grupo_contagem, it, it, membros.size) }
        else -> pluralStringResource(R.plurals.detalhe_grupo_membros, membros.size, membros.size)
    }
    val ver = stringResource(R.string.detalhe_grupo_ver)
    val descricao = listOfNotNull("$sobrancelha: $titulo.", ver.takeIf { aoTocar != null }).joinToString(" ")

    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val seu = estado.voce?.let { estado.avatares[it.uid] }
    val brilho = seu?.tipo?.let { LocalCoresDeTipo.current[it].base } ?: MaterialTheme.colorScheme.primary

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
            .heightIn(min = AlturaDoBanner)
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

        seu?.sprite?.let { sprite ->
            AsyncImage(
                model = sprite,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Espacos.relacionado)
                    .requiredSize(SpriteDoBanner),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(LARGURA_DO_TEXTO)
                .padding(Espacos.margem),
        ) {
            Text(
                text = sobrancelha.uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = claro.copy(alpha = OPACIDADE_DA_SOBRANCELHA),
            )
            Spacer(Modifier.height(Espacos.minimo))
            Text(text = titulo, style = MaterialTheme.typography.titleLarge, color = claro)
            Spacer(Modifier.height(Espacos.interno))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (aoTocar != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, FormaControle)
                            .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
                    ) {
                        Text(text = ver, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(Espacos.interno))
                }
                PilhaDeAvatares(membros, estado.avatares, correu, fundo = tinta)
            }
        }
    }
}

/** Os avatares sobrepostos, até cinco. O anel em [fundo] separa um do outro. */
@Composable
private fun PilhaDeAvatares(
    membros: List<ProgressoDoMembro>,
    avatares: Map<String, AvatarDoMembro>,
    correu: (ProgressoDoMembro) -> Boolean,
    fundo: Color,
) {
    val visiveis = membros.take(MAXIMO_NA_PILHA)
    val largura = AvatarDaPilha + PassoDaPilha * (visiveis.size - 1).coerceAtLeast(0)
    Box(modifier = Modifier.size(width = largura, height = AvatarDaPilha)) {
        visiveis.forEachIndexed { i, membro ->
            val avatar = avatares[membro.uid]
            val miolo = avatar?.tipo?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada
            Box(
                modifier = Modifier
                    .offset(x = PassoDaPilha * i)
                    .size(AvatarDaPilha)
                    .clip(FormaCirculo)
                    .background(miolo)
                    .border(AnelDaPilha, if (correu(membro)) MaterialTheme.colorScheme.primary else fundo, FormaCirculo),
                contentAlignment = Alignment.Center,
            ) {
                if (avatar?.sprite != null) {
                    AsyncImage(
                        model = avatar.sprite,
                        contentDescription = null,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.requiredSize(AvatarDaPilha * 1.5f),
                    )
                } else {
                    Text(
                        text = membro.nome.take(1).uppercase(LocaleDoApp),
                        style = Estilos.rotulo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
