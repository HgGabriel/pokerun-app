package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hggabriel.pokerun.ui.theme.FormaCirculo

/**
 * A faixa que a barra do topo ocupa sobre a raiz de uma aba (28/10/2026). A raiz desenha o
 * próprio fundo atrás dela (a camisa da Home, a capa da Pokédex, a página tingida), e por isso
 * a folga mora dentro da tela, e não no `NavHost`.
 */
val AlturaDaBarraDoTopo = 60.dp

/**
 * A folga da barra do topo na tela aberta. Zero fora das quatro raízes de aba: o detalhe e o
 * Perfil não têm barra, e a tela desenha o próprio topo.
 */
val LocalFolgaDaBarraDoTopo = staticCompositionLocalOf { 0.dp }

/**
 * A ação que a raiz da aba põe na barra, ao lado do treinador: a lista de planos na Home, o
 * pacote de figurinhas na Pokédex. Cada pedido tem dono, como em `TopoDaTela`: na troca de aba
 * as duas raízes convivem durante a transição, e a que sai não apaga a ação da que entrou.
 */
@Stable
class AcoesDaBarraDoTopo {
    var acao: (@Composable () -> Unit)? by mutableStateOf(null)
        private set
    private var dono: Any? = null

    fun pedir(quem: Any, acao: @Composable () -> Unit) {
        dono = quem
        this.acao = acao
    }

    fun soltar(quem: Any) {
        if (dono !== quem) return
        dono = null
        acao = null
    }
}

val LocalAcoesDaBarraDoTopo = staticCompositionLocalOf<AcoesDaBarraDoTopo?> { null }

/**
 * Põe [conteudo] na barra do topo enquanto a tela estiver na frente. Fora de uma raiz, não faz
 * nada. O pedido segue o ciclo da entrada da pilha, e não a composição: a tela que entra pede no
 * começo da transição (`ON_START`) e a que sai solta no mesmo instante (`ON_PAUSE`). A ação troca
 * no mesmo compasso do deslize da tela, e não depois dele.
 */
@Composable
fun AcaoNaBarraDoTopo(conteudo: @Composable () -> Unit) {
    val acoes = LocalAcoesDaBarraDoTopo.current ?: return
    val dono = remember { Any() }
    val atual by rememberUpdatedState(conteudo)
    val acao: @Composable () -> Unit = remember { { atual() } }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(acoes, ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> acoes.pedir(dono, acao)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> acoes.soltar(dono)
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            acoes.soltar(dono)
        }
    }
}

/**
 * O botão redondo da barra do topo: disco branco de 44dp com sombra curta, num alvo de 48dp. É a
 * mesma moldura do [AtalhoDoPerfil], para as peças da barra lerem como uma família.
 */
@Composable
fun BotaoDaBarraDoTopo(acao: AcaoDeIcone, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AlvoDoAtalho)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = acao.aoTocar,
            )
            .clearAndSetSemantics {
                contentDescription = acao.descricao
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BustoDoAtalho + MolduraDoAtalho * 2)
                .shadow(4.dp, FormaCirculo)
                .background(MaterialTheme.colorScheme.surface, FormaCirculo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(acao.icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
