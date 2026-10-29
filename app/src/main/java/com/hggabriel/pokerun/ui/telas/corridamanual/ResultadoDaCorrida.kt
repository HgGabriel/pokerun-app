package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.RendimentoDaCorrida
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.esqueleto
import com.hggabriel.pokerun.ui.telas.pokedex.PacoteDeFigurinhas
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A Pokébola fechada fica onde a captura a deixou: 42% da altura, raio de 48dp. */
private const val ALTURA_DA_BOLA = 0.42f
private val RaioDaBola = 48.dp

/** O mesmo véu da captura, para a passagem não piscar. */
private const val OPACIDADE_DO_VEU = 0.86f

/**
 * O resultado da arena (29/10/2026): a captura termina aqui, e não na saída da tela. Sob a
 * Pokébola fechada sobem o `+N XP`, os recordes que a corrida bateu e, se ela liberou
 * espécie, o pacote fechado com o número de figurinhas.
 *
 * O pacote não mostra quem veio: quem abre é a Pokédex, onde as figurinhas colam. Tocar o
 * envelope ou `Abrir o pacote` leva até lá; `Agora não` volta, e o pacote espera no badge
 * da aba. Enquanto a simulação não volta, o lugar do número pulsa; sem número, fica
 * `Corrida registrada`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResultadoDaCorrida(
    pronto: Boolean,
    rendimento: RendimentoDaCorrida?,
    aoAbrirPacote: () -> Unit,
    aoConcluir: () -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val vermelho = MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current
    val novas = rendimento?.novas.orEmpty()

    BackHandler(onBack = aoConcluir)

    // O conteúdo sobe de baixo da bola quando o número chega.
    val subida = remember { Animatable(if (reduzido) 1f else 0f) }
    LaunchedEffect(pronto) {
        if (pronto && !reduzido) subida.animateTo(1f, Movimento.pop())
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val topoDoConteudo = maxHeight * ALTURA_DA_BOLA + RaioDaBola + Espacos.bloco
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(tinta.copy(alpha = OPACIDADE_DO_VEU))
            pokebola(Offset(size.width / 2, size.height * ALTURA_DA_BOLA), RaioDaBola.toPx(), vermelho, claro, tinta, botaoAceso = 1f)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topoDoConteudo, start = Espacos.margem, end = Espacos.margem)
                .animateContentSize(Movimento.padrao(reduzido))
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            val xp = rendimento?.xp
            when {
                !pronto -> Spacer(
                    Modifier
                        .size(width = 180.dp, height = 56.dp)
                        .esqueleto(FormaControle, claro.copy(alpha = 0.22f)),
                )
                xp != null -> Text(
                    text = stringResource(R.string.manual_resultado_xp, String.format(LocaleDoApp, "%,d", xp)),
                    style = Estilos.numeroXl,
                    color = claro,
                    modifier = Modifier.subindo(subida.value),
                )
                else -> Text(
                    text = stringResource(R.string.manual_registrada),
                    style = Estilos.tituloTela,
                    color = claro,
                )
            }

            val recordes = rendimento?.recordes.orEmpty()
            if (recordes.isNotEmpty()) {
                Spacer(Modifier.height(Espacos.margem))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
                    modifier = Modifier.subindo(subida.value),
                ) {
                    TipoDeRecorde.entries.filter { it in recordes }.forEach { tipo ->
                        Text(
                            text = stringResource(R.string.manual_resultado_recorde, stringResource(rotuloDoRecorde(tipo))),
                            style = Estilos.rotulo,
                            color = claro,
                            modifier = Modifier
                                .background(vermelho, FormaControle)
                                .padding(horizontal = Espacos.interno, vertical = Espacos.relacionado),
                        )
                    }
                }
            }

            if (novas.isNotEmpty()) {
                Spacer(Modifier.height(Espacos.relacionado))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.subindo(subida.value)) {
                    // A folga cobre a inclinação, o selo e a sombra: o `animateContentSize` da coluna corta o que vaza.
                    PacoteDeFigurinhas(quantas = novas.size, aoAbrir = aoAbrirPacote, modifier = Modifier.padding(Espacos.margem))
                    Text(
                        text = pluralStringResource(R.plurals.destino_figurinhas_novas, novas.size, novas.size),
                        style = Estilos.corpoForte,
                        color = claro,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Espacos.margem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (novas.isNotEmpty()) {
                BotaoPrimario(aoTocar = aoAbrirPacote, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.manual_abrir_pacote))
                }
                BotaoDeTexto(aoTocar = aoConcluir, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.manual_agora_nao), color = claro)
                }
            } else {
                BotaoPrimario(aoTocar = aoConcluir, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.manual_concluir))
                }
            }
        }
    }
}

/** Sobe 24dp e acende junto com [progresso], de 0 a 1. */
private fun Modifier.subindo(progresso: Float): Modifier = graphicsLayer {
    alpha = progresso.coerceIn(0f, 1f)
    translationY = 24.dp.toPx() * (1f - progresso)
}

@Preview(showBackground = true)
@Composable
private fun ResultadoComPacotePreview() {
    PokerunTheme {
        ResultadoDaCorrida(
            pronto = true,
            rendimento = RendimentoDaCorrida(xp = 450, novas = listOf(10, 11, 12), recordes = setOf(TipoDeRecorde.MAIOR_DISTANCIA)),
            aoAbrirPacote = {},
            aoConcluir = {},
        )
    }
}

private fun rotuloDoRecorde(tipo: TipoDeRecorde): Int = when (tipo) {
    TipoDeRecorde.MELHOR_PACE -> R.string.painel_recorde_pace
    TipoDeRecorde.MELHOR_1K -> R.string.painel_recorde_1k
    TipoDeRecorde.MELHOR_5K -> R.string.painel_recorde_5k
    TipoDeRecorde.MAIOR_DISTANCIA -> R.string.painel_recorde_distancia
    TipoDeRecorde.MAIOR_DURACAO -> R.string.painel_recorde_duracao
}
