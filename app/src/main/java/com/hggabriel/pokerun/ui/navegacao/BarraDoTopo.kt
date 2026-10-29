package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.AcoesDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.AlturaDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.AtalhoDoPerfil
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.telas.home.HomeUiState
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento

private val AlturaDaPilula = 44.dp
private val FormaDaPilula = RoundedCornerShape(50)

/** As barrinhas das sessões: a curta e o longão, que é mais alto, como no card da semana. */
private val LarguraDaSessao = 7.dp
private val AlturaDaCurta = 14.dp
private val AlturaDoLongao = 24.dp

/** Uma sessão prevista da semana corrente, na ordem da barra do card da Home. */
@Immutable
data class SessaoDaBarra(val feita: Boolean, val longao: Boolean)

/**
 * A semana corrente do plano ativo, o que a pílula da barra do topo desenha. Nula sem plano
 * ativo, antes do início e depois do fim: aí a pílula não aparece.
 */
@Immutable
data class SemanaDaBarra(
    val planoId: String,
    val numero: Int,
    val sessoes: List<SessaoDaBarra>,
    val kmFeitos: Double,
    val kmAlvo: Double,
) {
    val feitas: Int get() = sessoes.count { it.feita }
}

/** A semana da barra a partir do estado da Home: o mesmo cálculo do card, sem cópia. */
internal fun semanaDaBarra(estado: HomeUiState): SemanaDaBarra? {
    val ativo = estado as? HomeUiState.Ativo ?: return null
    return SemanaDaBarra(
        planoId = ativo.planoId,
        numero = ativo.semana.numero,
        sessoes = ativo.semana.segmentos.map { SessaoDaBarra(feita = it.corridaId != null, longao = it.longao) },
        kmFeitos = ativo.semana.kmFeitos,
        kmAlvo = ativo.semana.kmAlvo,
    )
}

/**
 * A barra do topo some quando a lista desce e volta quando ela sobe (28/10/2026): a pessoa
 * lendo a tela não tem a barra em cima do dado, e um gesto para cima a devolve.
 *
 * Reage só à rolagem que a lista **consumiu**: tela curta, que não rola, nunca esconde a barra.
 * A casca devolve a barra inteira a cada troca de destino.
 */
@Stable
class RecolhimentoDaBarraDoTopo {
    /** De 0 (inteira) a 1 (fora da tela). */
    var fracao by mutableFloatStateOf(0f)
        private set
    internal var limitePx = 1f

    val conexao = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed.y != 0f) fracao = (fracao - consumed.y / limitePx).coerceIn(0f, 1f)
            return Offset.Zero
        }
    }

    fun mostrar() {
        fracao = 0f
    }
}

/**
 * A barra do topo das quatro raízes de aba (28/10/2026).
 *
 * Flutua sobre a tela, sem fundo próprio: a camisa vermelha da Home, a capa da Pokédex e a
 * página tingida continuam até a barra de status. Cada raiz reserva a faixa com
 * `LocalFolgaDaBarraDoTopo`.
 *
 * - **À esquerda, a semana:** uma barrinha por sessão prevista, cheia em `destaque` quando
 *   cumprida, e os km da semana contra o alvo. Vale de qualquer aba, e o toque abre o detalhe
 *   da semana. A sessão que acaba de ser cumprida enche com uma batida.
 * - **À direita, o busto do treinador**, que abre o Perfil, e à esquerda dele a ação que a raiz
 *   pedir (a lista de planos na Home, o pacote de figurinhas na Pokédex), que entra e sai com
 *   escala e esmaecimento.
 *
 * Não é uma `TopAppBar`: não ganha elevação ao rolar e não tem fundo (docs/02 §12).
 */
@Composable
internal fun BarraDoTopo(
    semana: SemanaDaBarra?,
    acoes: AcoesDaBarraDoTopo,
    recolhimento: RecolhimentoDaBarraDoTopo,
    aoAbrirSemana: (SemanaDaBarra) -> Unit,
    aoAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alturaPx = with(LocalDensity.current) { AlturaDaBarraDoTopo.toPx() }
    recolhimento.limitePx = alturaPx
    val reduzido = LocalMovimentoReduzido.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AlturaDaBarraDoTopo)
            // A barra sai pela borda de cima da tela, e não por cima da barra de status.
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDaBarraDoTopo)
                // Lido no desenho: a rolagem não recompõe a barra.
                .graphicsLayer {
                    translationY = -recolhimento.fracao * alturaPx
                    alpha = 1f - recolhimento.fracao
                }
                .padding(start = Espacos.margem - 4.dp, end = Espacos.minimo),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = semana != null,
                enter = fadeIn(Movimento.padrao(reduzido)) + expandHorizontally(Movimento.padrao(reduzido), Alignment.Start),
                exit = fadeOut(Movimento.saida(reduzido)) + shrinkHorizontally(Movimento.saida(reduzido), Alignment.Start),
                label = "pílula da semana",
            ) {
                // Na saída, `semana` já é nula: a pílula some com o último valor que mostrou.
                val ultima = remember { arrayOfNulls<SemanaDaBarra>(1) }
                if (semana != null) ultima[0] = semana
                ultima[0]?.let { PilulaDaSemana(it, aoAbrir = { aoAbrirSemana(it) }) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = acoes.acao,
                // Um compasso inteiro nas duas pontas, junto com o deslize da troca de aba.
                transitionSpec = {
                    (scaleIn(Movimento.pop(reduzido), initialScale = 0.4f) + fadeIn(Movimento.padrao(reduzido))) togetherWith
                        (scaleOut(Movimento.saida(reduzido), targetScale = 0.4f) + fadeOut(Movimento.saida(reduzido))) using
                        SizeTransform(clip = false) { _, _ -> Movimento.padrao(reduzido) }
                },
                contentAlignment = Alignment.CenterEnd,
                label = "ação da raiz",
            ) { acao ->
                if (acao != null) acao() else Spacer(Modifier.size(0.dp))
            }
            AtalhoDoPerfil(aoTocar = aoAbrirPerfil)
        }
    }
}

/**
 * A semana numa pílula branca: as sessões como barrinhas e os km contra o alvo. O TalkBack lê a
 * semana inteira numa frase e anuncia o toque.
 */
@Composable
private fun PilulaDaSemana(semana: SemanaDaBarra, aoAbrir: () -> Unit) {
    val feitos = formatarKm(semana.kmFeitos)
    val alvo = formatarKm(semana.kmAlvo)
    val descricao = stringResource(R.string.barra_semana_descricao, semana.numero, semana.feitas, semana.sessoes.size, feitos, alvo)
    val rotuloDoToque = stringResource(R.string.barra_semana_abrir)
    val reduzido = LocalMovimentoReduzido.current

    Row(
        modifier = Modifier
            .height(AlturaDaPilula)
            .shadow(4.dp, FormaDaPilula)
            .background(MaterialTheme.colorScheme.surface, FormaDaPilula)
            .clip(FormaDaPilula)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = aoAbrir,
            )
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick(rotuloDoToque) { aoAbrir(); true }
            }
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(AlturaDoLongao),
        ) {
            semana.sessoes.forEach { sessao -> BarrinhaDaSessao(sessao, reduzido) }
        }
        Spacer(Modifier.width(Espacos.relacionado + 2.dp))
        AnimatedContent(
            targetState = feitos,
            transitionSpec = {
                (slideInVertically(Movimento.padrao(reduzido)) { it } + fadeIn(Movimento.padrao(reduzido))) togetherWith
                    (slideOutVertically(Movimento.saida(reduzido)) { -it } + fadeOut(Movimento.saida(reduzido)))
            },
            label = "km da semana",
        ) { km ->
            Text(km, style = Estilos.numeroS, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            " / $alvo km",
            style = Estilos.legenda,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Uma sessão: pílula vertical em `superficie-afundada`, cheia em `destaque` quando cumprida. A que
 * acaba de encher cresce de baixo com uma batida (movimento como recompensa, docs/02 §1).
 */
@Composable
private fun BarrinhaDaSessao(sessao: SessaoDaBarra, reduzido: Boolean) {
    val cor by animateColorAsState(
        targetValue = if (sessao.feita) MaterialTheme.colorScheme.primary else LocalCoresPokerun.current.superficieAfundada,
        animationSpec = Movimento.padrao(reduzido),
        label = "sessão da barra",
    )
    val batida = remember { Animatable(1f) }
    // Só a mudança para cumprida bate; a primeira composição já chega no estado final.
    val anterior = remember { booleanArrayOf(sessao.feita) }
    LaunchedEffect(sessao.feita) {
        val acabouDeEncher = sessao.feita && !anterior[0]
        anterior[0] = sessao.feita
        if (acabouDeEncher && !reduzido) {
            batida.snapTo(0.2f)
            batida.animateTo(1f, Movimento.pop())
        }
    }
    Box(
        modifier = Modifier
            .width(LarguraDaSessao)
            .height(if (sessao.longao) AlturaDoLongao else AlturaDaCurta)
            .graphicsLayer {
                scaleY = batida.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .background(cor, FormaDaPilula),
    )
}
