package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.RendimentoDaCorrida
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.esqueleto
import com.hggabriel.pokerun.ui.componentes.pokebola
import com.hggabriel.pokerun.ui.telas.pokedex.PacoteDeFigurinhas
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import kotlinx.coroutines.delay

/** A Pokébola fechada começa onde a captura a deixou (42% da altura) e sobe para 26% com o resultado. */
private const val ALTURA_DA_CAPTURA = 0.42f
private const val ALTURA_DO_RESULTADO = 0.26f
private val RaioDaBola = 48.dp

/** O véu da captura, que fecha até a tinta cheia: a arena por baixo não disputa a leitura. */
private const val OPACIDADE_DA_CAPTURA = 0.86f

/** O pacote no resultado é a peça principal: o envelope do álbum ampliado. */
private const val ESCALA_DO_PACOTE = 1.7f

/**
 * O resultado da arena (29/10/2026): a captura termina aqui, e não na saída da tela. Sob a
 * Pokébola fechada sobem o `+N XP`, os recordes que a corrida bateu e, se ela liberou
 * espécie, o pacote fechado com o número de figurinhas.
 *
 * O véu fecha até a tinta cheia e a bola sobe, abrindo lugar para o que a corrida rendeu, com a
 * luz vermelha da captura atrás dela. O pacote não mostra quem veio: quem abre é a Pokédex, onde
 * as figurinhas colam. Tocar o envelope ou `Abrir o pacote` leva até lá; `Agora não` volta, e o
 * pacote espera no badge da aba. Enquanto a simulação não volta, o lugar do número pulsa; sem
 * número, fica `Corrida registrada`.
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

    val veu = remember { Animatable(if (reduzido) 1f else OPACIDADE_DA_CAPTURA) }
    // A bola sobe e o conteúdo vem atrás dela quando o número chega.
    val subida = remember { Animatable(if (reduzido) 1f else 0f) }
    LaunchedEffect(Unit) { veu.animateTo(1f, Movimento.padrao(reduzido)) }
    LaunchedEffect(pronto) {
        if (pronto && !reduzido) subida.animateTo(1f, Movimento.lento())
    }
    // O conteúdo entra uma batida depois da bola, com o pop.
    val entrada = remember { Animatable(if (reduzido) 1f else 0f) }
    LaunchedEffect(pronto) {
        if (pronto && !reduzido) {
            delay(Movimento.Tempo.BATIDA.toLong())
            entrada.animateTo(1f, Movimento.pop())
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val altura = maxHeight
        val alturaDaBola = { lerp(ALTURA_DA_CAPTURA, ALTURA_DO_RESULTADO, subida.value) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(tinta.copy(alpha = veu.value))
            val centro = Offset(size.width / 2, size.height * alturaDaBola())
            val raioDaLuz = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(vermelho.copy(alpha = 0.32f * subida.value), Color.Transparent),
                    center = centro,
                    radius = raioDaLuz,
                ),
                radius = raioDaLuz,
                center = centro,
            )
            pokebola(centro, RaioDaBola.toPx(), vermelho, claro, tinta, botaoAceso = 1f)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                // O conteúdo segue a bola; a leitura fica no posicionamento, sem recompor a coluna.
                .offset { IntOffset(0, (altura * alturaDaBola() + RaioDaBola + Espacos.bloco).roundToPx()) }
                .padding(horizontal = Espacos.margem)
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
                    modifier = Modifier.entrando(entrada.value, 0),
                )
                else -> Text(
                    text = stringResource(R.string.manual_registrada),
                    style = Estilos.tituloTela,
                    color = claro,
                    modifier = Modifier.entrando(entrada.value, 0),
                )
            }

            val recordes = rendimento?.recordes.orEmpty()
            if (recordes.isNotEmpty()) {
                Spacer(Modifier.height(Espacos.margem))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
                    modifier = Modifier.entrando(entrada.value, 1),
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
                // O envelope ampliado é a peça: o número de figurinhas mora no selo dele. A folga
                // cobre a escala, a inclinação e o selo, que passam da caixa do envelope.
                PacoteDeFigurinhas(
                    quantas = novas.size,
                    aoAbrir = aoAbrirPacote,
                    modifier = Modifier
                        .padding(vertical = 44.dp, horizontal = 48.dp)
                        .entrando(entrada.value, 2)
                        .graphicsLayer {
                            scaleX = ESCALA_DO_PACOTE
                            scaleY = ESCALA_DO_PACOTE
                        },
                )
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
                Spacer(Modifier.height(Espacos.relacionado))
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

/**
 * Sobe 24dp e acende com [progresso], de 0 a 1. O item [ordem] da sequência fica uma fração
 * atrás do anterior, para o XP, os recordes e o pacote chegarem nessa ordem num pop só.
 */
private fun Modifier.entrando(progresso: Float, ordem: Int): Modifier = graphicsLayer {
    val atraso = ordem * 0.18f
    val p = ((progresso - atraso) / (1f - atraso)).coerceIn(0f, 1.2f)
    alpha = p.coerceIn(0f, 1f)
    translationY = 24.dp.toPx() * (1f - p)
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
