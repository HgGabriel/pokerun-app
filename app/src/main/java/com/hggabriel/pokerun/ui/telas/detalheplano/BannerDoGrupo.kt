package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.AvatarEmPilha
import com.hggabriel.pokerun.ui.componentes.BannerEscuro
import com.hggabriel.pokerun.ui.componentes.PilhaDeAvatares
import com.hggabriel.pokerun.ui.componentes.PilulaDoBanner
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.TituloDoBanner
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo

private val AlturaDoBanner = 156.dp

/**
 * O banner do grupo no detalhe do plano: o [BannerEscuro] com o brilho da cor do tipo do seu
 * Pokémon e o sprite dele. À esquerda, a sobrancelha, quantos correram nesta semana, a
 * pílula `Ver grupo` e os avatares sobrepostos.
 *
 * Com [aoTocar] o banner inteiro é o alvo e a pílula aparece; sem ele, na própria tela do
 * grupo, é só o cabeçalho dela.
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

    val seu = estado.voce?.let { estado.avatares[it.uid] }
    val brilho = seu?.tipo?.let { LocalCoresDeTipo.current[it].base } ?: MaterialTheme.colorScheme.primary
    val cores = LocalCoresDeTipo.current
    val avatares = membros.map { m ->
        val avatar = estado.avatares[m.uid]
        AvatarEmPilha(avatar?.sprite, m.nome.take(1), avatar?.tipo?.let { cores[it].suave }, correu(m))
    }

    BannerEscuro(
        descricao = descricao,
        aoTocar = aoTocar,
        modifier = modifier,
        brilho = brilho,
        sprite = seu?.sprite,
        alturaMinima = AlturaDoBanner,
    ) {
        SobrancelhaDoBanner(sobrancelha)
        Spacer(Modifier.height(Espacos.minimo))
        TituloDoBanner(titulo)
        Spacer(Modifier.height(Espacos.interno))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (aoTocar != null) {
                PilulaDoBanner(ver)
                Spacer(Modifier.width(Espacos.interno))
            }
            PilhaDeAvatares(avatares, fundo = MaterialTheme.colorScheme.onSurface)
        }
    }
}
