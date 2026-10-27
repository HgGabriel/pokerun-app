package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Treinador
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/**
 * O treinador escolhido por quem usa o app, pelo nome do arquivo do Showdown, ou nulo antes
 * da primeira escolha. A casca o provê a partir de `users/{uid}`, e o [AtalhoDoPerfil] de
 * cada aba o lê sem que cada tela precise de um `ViewModel` para isso.
 */
val LocalTreinadorDoUsuario = compositionLocalOf<String?> { null }

/** O alvo do atalho do Perfil: o mesmo piso de toque das ações do cabeçalho (docs/02 §8). */
internal val AlvoDoAtalho = 48.dp

/** O busto do atalho, dentro do alvo (estudo de Progresso e Perfil, Revisão 3, regra 4). */
internal val BustoDoAtalho = 40.dp

/** O corpo inteiro nunca passa de 80dp: é pixel art de 80px, e ampliada vira bloco borrado. */
internal val CorpoMaximo = 80.dp

/**
 * Quanto o sprite é ampliado dentro do círculo do busto, e quanto desce. O sprite tem o
 * corpo inteiro em 80px; o busto mostra a cabeça e os ombros, a faixa de ~9% a ~55% da altura.
 */
private const val ESCALA_DO_BUSTO = 2.2f
private const val DESCIDA_DO_BUSTO = 0.39f

/**
 * O busto do treinador num círculo: a pessoa nos lugares de "quem" (atalho do Perfil, pilha
 * de quem correu, membro do plano). Sem escolha, a silhueta em `superficie-afundada`, o
 * mesmo tratamento da espécie não alcançada (regra 3). Sempre sem suavização.
 *
 * @param fundo o miolo do círculo. Com treinador, o padrão é `superficie-afundada`.
 */
@Composable
fun BustoDoTreinador(
    treinador: String?,
    tamanho: Dp,
    modifier: Modifier = Modifier,
    fundo: Color = LocalCoresPokerun.current.superficieAfundada,
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(FormaCirculo)
            .background(fundo),
        contentAlignment = Alignment.Center,
    ) {
        if (treinador == null) {
            SilhuetaDeTreinador(Modifier.fillMaxSize())
        } else {
            ImagemDaRede(
                model = Treinador.url(treinador),
                filterQuality = FilterQuality.None,
                recuoDaEspera = 0.3f,
                modifier = Modifier
                    .requiredSize(tamanho * ESCALA_DO_BUSTO)
                    .offset(y = tamanho * DESCIDA_DO_BUSTO),
            )
        }
    }
}

/**
 * O treinador de corpo inteiro, no máximo a 80dp (regra 4: o treinador nunca vira cena
 * grande). Sem escolha, a silhueta.
 */
@Composable
fun SpriteDoTreinador(treinador: String?, modifier: Modifier = Modifier, tamanho: Dp = CorpoMaximo) {
    val lado = minOf(tamanho, CorpoMaximo)
    if (treinador == null) {
        SilhuetaDeTreinador(modifier.size(lado))
    } else {
        ImagemDaRede(
            model = Treinador.url(treinador),
            filterQuality = FilterQuality.None,
            formaDaEspera = MaterialTheme.shapes.medium,
            recuoDaEspera = 0.15f,
            modifier = modifier.size(lado),
        )
    }
}

@Composable
private fun SilhuetaDeTreinador(modifier: Modifier) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        // Preta a 14%, como a espécie não alcançada da Pokédex (regra 3).
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        modifier = modifier,
    )
}

/**
 * O atalho do Perfil na raiz de cada aba: o busto do treinador em 40dp num alvo de 48dp
 * (decisão 22). Substitui a engrenagem: a tela deixou de ser configuração e passou a ser
 * sobre quem corre. A descrição falada diz o treinador, quando há um.
 */
@Composable
fun AtalhoDoPerfil(aoTocar: () -> Unit, modifier: Modifier = Modifier, fundo: Color? = null) {
    val treinador = LocalTreinadorDoUsuario.current
    val descricao = if (treinador == null) {
        stringResource(R.string.perfil_atalho)
    } else {
        stringResource(R.string.perfil_atalho_com_treinador, Treinador.rotulo(treinador))
    }
    Box(
        modifier = modifier
            .size(AlvoDoAtalho)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = aoTocar,
            )
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        BustoDoTreinador(
            treinador = treinador,
            tamanho = BustoDoAtalho,
            // Branco: o atalho mora sobre a camisa vermelha da Home e sobre páginas tingidas,
            // e o círculo precisa se destacar das duas (mock `progresso-estrada-v4`).
            fundo = fundo ?: MaterialTheme.colorScheme.surface,
        )
    }
}
