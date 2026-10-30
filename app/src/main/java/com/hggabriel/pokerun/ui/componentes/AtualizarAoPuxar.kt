package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** O disco do indicador e a Pokébola dentro dele. */
private val DiscoDoIndicador = 40.dp
private val RaioDaPokebolaDoIndicador = 13.dp

/**
 * O puxar para atualizar (29/10/2026), o mesmo em toda tela que mostra dado da rede: o dedo
 * puxa a lista do topo para baixo, a Pokébola desce e gira com ele e, solta, dá uma volta de
 * loader.
 *
 * Soltar faz duas coisas. Toda [ImagemDaRede] abaixo refaz o pedido (a que carregou volta do
 * cache na hora, a que falhou tenta a rede de novo), e [aoAtualizar] relê os dados da tela.
 * Quem chama passa o `tentarDeNovo` do `ViewModel`, ou uma leitura que não volta ao esqueleto.
 *
 * O [conteudo] precisa ser a lista ou a coluna que rola: o gesto chega pelo `nestedScroll` dela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtualizarAoPuxar(
    aoAtualizar: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable BoxScope.() -> Unit,
) {
    var atualizando by remember { mutableStateOf(false) }
    var geracao by remember { mutableIntStateOf(0) }
    val escopo = rememberCoroutineScope()
    val estado = rememberPullToRefreshState()
    val atualizar by rememberUpdatedState(aoAtualizar)
    val geracaoDeFora = LocalGeracaoDasImagens.current
    PullToRefreshBox(
        isRefreshing = atualizando,
        onRefresh = {
            atualizando = true
            geracao++
            atualizar()
            escopo.launch {
                delay(Movimento.Duracao.LOADER.toLong())
                atualizando = false
            }
        },
        state = estado,
        modifier = modifier,
        indicator = { IndicadorDePokebola(estado, atualizando, Modifier.align(Alignment.TopCenter)) },
    ) {
        CompositionLocalProvider(LocalGeracaoDasImagens provides geracaoDeFora + geracao) {
            conteudo()
        }
    }
}

/**
 * O indicador: o disco branco da barra do topo com a Pokébola, abaixo da barra do topo na raiz
 * de aba e abaixo da barra de status nas telas da pilha. Ele desce com o dedo e gira junto; solto,
 * dá voltas de loader até a atualização terminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndicadorDePokebola(estado: PullToRefreshState, atualizando: Boolean, modifier: Modifier = Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val vermelho = MaterialTheme.colorScheme.primary
    val folga = LocalFolgaDaBarraDoTopo.current
    val reduzido = LocalMovimentoReduzido.current
    val giro = if (atualizando && !reduzido) {
        rememberInfiniteTransition(label = "loader do puxar").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(Movimento.Duracao.LOADER, easing = LinearEasing)),
            label = "giro",
        )
    } else {
        null
    }
    Canvas(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .offset {
                val puxado = if (atualizando) 1f else estado.distanceFraction.coerceIn(0f, 1f)
                IntOffset(0, (folga + Espacos.interno - (DiscoDoIndicador + Espacos.interno) * (1f - puxado)).roundToPx())
            }
            .size(DiscoDoIndicador)
            .graphicsLayer {
                alpha = if (atualizando) 1f else estado.distanceFraction.coerceIn(0f, 1f)
                rotationZ = giro?.value ?: (estado.distanceFraction * 300f)
            }
            .shadow(4.dp, CircleShape)
            .background(claro, CircleShape),
    ) {
        pokebola(Offset(size.width / 2, size.height / 2), RaioDaPokebolaDoIndicador.toPx(), vermelho, claro, tinta)
    }
}
