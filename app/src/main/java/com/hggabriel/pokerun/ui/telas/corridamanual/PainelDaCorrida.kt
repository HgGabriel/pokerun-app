package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.EscolhaCheia
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import java.time.LocalDate

/*
 * Os cartões abaixo da arena (26/10/2026): quando foi, como foi e os recordes. São brancos
 * sobre a página tingida, como o detalhe do plano, e cada um responde uma pergunta.
 */

private val AlturaDasBarras = 56.dp
private val BarraMinima = 14.dp

/** O esforço em palavra: a faixa de cada nível. */
internal fun palavraDoEsforco(nivel: Int): Int = when {
    nivel <= 3 -> R.string.manual_esforco_leve
    nivel <= 6 -> R.string.manual_esforco_moderado
    nivel <= 8 -> R.string.manual_esforco_forte
    else -> R.string.manual_esforco_limite
}

// ---------------------------------------------------------------------------
// Quando
// ---------------------------------------------------------------------------

/**
 * A data em três escolhas, `Hoje`, `Ontem` e `Outro dia` (o calendário), e a hora numa
 * linha que abre o relógio. Nada vem marcado: a data decide a semana (RN-01), e uma
 * resposta pronta seria inventada.
 */
@Composable
internal fun CartaoQuando(
    estado: CorridaManualUiState,
    aoEscolherData: (LocalDate) -> Unit,
    aoAbrirCalendario: () -> Unit,
    aoAbrirRelogio: () -> Unit,
) {
    val hoje = estado.hoje ?: LocalDate.now()
    val ontem = hoje.minusDays(1)
    val outro = estado.data?.takeIf { it != hoje && it != ontem }

    Cartao {
        RotuloDoCartao(stringResource(R.string.manual_quando))
        Spacer(Modifier.height(Espacos.interno))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
            EscolhaCheia(stringResource(R.string.manual_hoje), estado.data == hoje, { aoEscolherData(hoje) })
            EscolhaCheia(stringResource(R.string.manual_ontem), estado.data == ontem, { aoEscolherData(ontem) })
            EscolhaCheia(
                texto = outro?.let { stringResource(R.string.manual_dia_curto, it.dayOfMonth, nomeCurtoDoMes(it)) }
                    ?: stringResource(R.string.manual_outro_dia),
                selecionada = outro != null,
                icone = Icons.Filled.DateRange,
                aoTocar = aoAbrirCalendario,
            )
        }
        Spacer(Modifier.height(Espacos.relacionado))
        LinhaDaHora(estado, aoAbrirRelogio)

        if (estado.erros.dataHora != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            BannerDeAlerta(
                rotulo = stringResource(R.string.alerta_corrija),
                texto = stringResource(estado.erros.dataHora),
            )
        }
    }
}

/** A hora de início: a linha inteira abre o relógio, e a hora sai em número. */
@Composable
private fun LinhaDaHora(estado: CorridaManualUiState, aoAbrir: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val rotulo = stringResource(R.string.manual_campo_hora)
    val hora = estado.hora?.let { stringResource(R.string.manual_hora_escolhida, it.hour, it.minute) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FormaControle)
            .background(LocalCoresPokerun.current.superficieAfundada)
            .clickable(role = Role.Button, onClick = aoAbrir)
            .clearAndSetSemantics { contentDescription = listOfNotNull(rotulo, hora).joinToString(", ") ; role = Role.Button }
            .padding(horizontal = Espacos.margem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Relogio(tinta)
        Spacer(Modifier.width(Espacos.interno))
        Text(text = rotulo, style = Estilos.corpoForte, color = tinta, modifier = Modifier.weight(1f))
        if (hora != null) {
            Text(text = hora, style = Estilos.numeroM, color = tinta)
        } else {
            Text(
                text = stringResource(R.string.manual_hora_vazia),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** O relógio desenhado: o mostrador e os dois ponteiros. */
@Composable
private fun Relogio(cor: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val r = size.minDimension / 2
        val traco = 2.dp.toPx()
        drawCircle(cor, r - traco / 2, style = Stroke(traco))
        drawLine(cor, center, Offset(center.x, center.y - r * 0.55f), traco, StrokeCap.Round)
        drawLine(cor, center, Offset(center.x + r * 0.4f, center.y), traco, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------
// Como foi
// ---------------------------------------------------------------------------

/**
 * O esforço em dez barras que sobem, do verde ao vermelho, e a frequência com um coração
 * que bate no ritmo digitado. Os dois são opcionais.
 */
@Composable
internal fun CartaoComoFoi(
    estado: CorridaManualUiState,
    aoMudarEsforco: (String) -> Unit,
    aoMudarFrequencia: (String) -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val nivel = estado.esforco.toIntOrNull()?.takeIf { it in 1..10 }

    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotuloDoCartao(stringResource(R.string.manual_como_foi), Modifier.weight(1f))
            Text(
                text = stringResource(R.string.manual_opcional),
                style = Estilos.legenda,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Espacos.margem))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.manual_campo_esforco),
                style = Estilos.corpoForte,
                color = tinta,
                modifier = Modifier.weight(1f),
            )
            if (nivel != null) {
                Text(text = nivel.toString(), style = Estilos.numeroM, color = tinta)
                Spacer(Modifier.width(Espacos.relacionado))
                Text(
                    text = stringResource(palavraDoEsforco(nivel)),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(Espacos.relacionado))
        BarrasDeEsforco(nivel) { escolhido -> aoMudarEsforco(if (escolhido == nivel) "" else escolhido.toString()) }
        if (estado.erros.esforco != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erros.esforco))
        }

        Spacer(Modifier.height(Espacos.bloco))

        val bpm = estado.fcMedia.toIntOrNull()?.takeIf { it in 30..230 }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(Espacos.alvoToque), contentAlignment = Alignment.Center) {
                if (bpm != null) CoracaoBatendo(bpm) else Coracao(MaterialTheme.colorScheme.outline, 1f)
            }
            Spacer(Modifier.width(Espacos.interno))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.manual_frequencia_curta), style = Estilos.corpoForte, color = tinta)
                Text(
                    text = stringResource(R.string.manual_campo_frequencia_apoio),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Espacos.relacionado))
            CaixaDeNumero(
                valor = estado.fcMedia,
                aoMudar = aoMudarFrequencia,
                marcador = "---",
                unidade = stringResource(R.string.manual_bpm),
                descricao = stringResource(R.string.manual_campo_frequencia),
                cores = coresClaras(),
                cursor = MaterialTheme.colorScheme.primary,
                erro = estado.erros.fcMedia != null,
                largura = 84.dp,
                altura = 52.dp,
                estilo = Estilos.numeroM,
                maximo = 3,
            )
        }
        if (estado.erros.fcMedia != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erros.fcMedia))
        }
    }
}

/**
 * As dez barras do esforço. O alvo de toque é a coluna inteira, não só a barra: a barra 1
 * tem 14dp de altura e não passaria no piso de 48dp. Tocar na marcada desmarca.
 */
@Composable
private fun BarrasDeEsforco(nivel: Int?, aoEscolher: (Int) -> Unit) {
    val vazio = LocalCoresPokerun.current.superficieAfundada

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDasBarras),
        horizontalArrangement = Arrangement.spacedBy(Espacos.minimo),
    ) {
        for (i in 1..10) {
            val cheia = nivel != null && i <= nivel
            val fracao = (i - 1) / 9f
            val cor = corDoEsforco(fracao)
            val fundo by animateColorAsState(if (cheia) cor else vazio, label = "barra do esforço")
            val descricao = stringResource(R.string.manual_esforco_nivel, i)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.RadioButton) { aoEscolher(i) }
                    .clearAndSetSemantics {
                        contentDescription = descricao
                        selected = nivel == i
                        role = Role.RadioButton
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarraMinima + (AlturaDasBarras - BarraMinima) * fracao)
                        .clip(RoundedCornerShape(6.dp))
                        .background(fundo),
                )
            }
        }
    }
}

/**
 * A cor da barra: o matiz desce do verde ao vermelho. Interpolar em RGB entre o verde e o
 * amarelo passa por um oliva sujo; pelo matiz a rampa fica viva do começo ao fim.
 */
private fun corDoEsforco(fracao: Float): Color =
    Color.hsv(hue = 128f * (1 - fracao), saturation = 0.72f + 0.12f * fracao, value = 0.82f + 0.1f * fracao)

/** O coração batendo em [bpm]: duas batidas por ciclo, a segunda mais fraca. */
@Composable
private fun CoracaoBatendo(bpm: Int) {
    val vermelho = MaterialTheme.colorScheme.primary
    if (LocalMovimentoReduzido.current) {
        Coracao(vermelho, 1f)
        return
    }
    val periodo = (60_000 / bpm).coerceIn(260, 2_000)
    val transicao = rememberInfiniteTransition(label = "coração")
    val escala by transicao.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = periodo
                1f at 0
                1.24f at (periodo * 0.12f).toInt() using FastOutSlowInEasing
                0.96f at (periodo * 0.28f).toInt()
                1.12f at (periodo * 0.4f).toInt()
                1f at (periodo * 0.58f).toInt()
            },
        ),
        label = "batida",
    )
    Coracao(vermelho, escala)
}

@Composable
private fun Coracao(cor: Color, escala: Float) {
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        tint = cor,
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            },
    )
}

// ---------------------------------------------------------------------------
// Recordes
// ---------------------------------------------------------------------------

/** Se a linha do melhor trecho aparece: a corrida alcança a marca, ou o campo já tem algo. */
internal fun mostraMarca(km: String, marcaKm: Double, minutos: String, segundos: String, erro: Int?): Boolean =
    (distanciaEmKm(km) ?: 0.0) >= marcaKm || minutos.isNotBlank() || segundos.isNotBlank() || erro != null

/**
 * Os melhores trechos de 1 km e de 5 km (decisão nº 110). Cada linha só aparece quando a
 * corrida alcança a marca: o 5 km de um treino de 3 km não tem o que perguntar.
 */
@Composable
internal fun CartaoRecordes(
    estado: CorridaManualUiState,
    aoMudarTempo1kMinutos: (String) -> Unit,
    aoMudarTempo1kSegundos: (String) -> Unit,
    aoMudarTempo5kMinutos: (String) -> Unit,
    aoMudarTempo5kSegundos: (String) -> Unit,
) {
    val mostra1k = mostraMarca(estado.km, 1.0, estado.tempo1kMinutos, estado.tempo1kSegundos, estado.erros.tempo1k)
    val mostra5k = mostraMarca(estado.km, 5.0, estado.tempo5kMinutos, estado.tempo5kSegundos, estado.erros.tempo5k)

    AnimatedVisibility(visible = mostra1k || mostra5k) {
        Cartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotuloDoCartao(stringResource(R.string.manual_recordes), Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.manual_opcional),
                    style = Estilos.legenda,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Espacos.interno))
            AnimatedVisibility(visible = mostra1k) {
                LinhaDeMarca(
                    medalha = "1K",
                    rotulo = stringResource(R.string.manual_campo_tempo_1k),
                    minutos = estado.tempo1kMinutos,
                    segundos = estado.tempo1kSegundos,
                    erro = estado.erros.tempo1k,
                    aoMudarMinutos = aoMudarTempo1kMinutos,
                    aoMudarSegundos = aoMudarTempo1kSegundos,
                )
            }
            AnimatedVisibility(visible = mostra5k) {
                Column {
                    Spacer(Modifier.height(Espacos.interno))
                    LinhaDeMarca(
                        medalha = "5K",
                        rotulo = stringResource(R.string.manual_campo_tempo_5k),
                        minutos = estado.tempo5kMinutos,
                        segundos = estado.tempo5kSegundos,
                        erro = estado.erros.tempo5k,
                        aoMudarMinutos = aoMudarTempo5kMinutos,
                        aoMudarSegundos = aoMudarTempo5kSegundos,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDeMarca(
    medalha: String,
    rotulo: String,
    minutos: String,
    segundos: String,
    erro: Int?,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val cores = coresClaras()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tinta, FormaCirculo),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = medalha, style = Estilos.rotulo, color = MaterialTheme.colorScheme.surface)
            }
            Spacer(Modifier.width(Espacos.interno))
            Text(text = rotulo, style = Estilos.corpoForte, color = tinta, modifier = Modifier.weight(1f))
            CaixaDeNumero(
                valor = minutos,
                aoMudar = aoMudarMinutos,
                marcador = "00",
                unidade = null,
                descricao = "$rotulo, " + stringResource(R.string.manual_campo_minutos),
                cores = cores,
                cursor = MaterialTheme.colorScheme.primary,
                erro = erro != null,
                largura = 56.dp,
                altura = 48.dp,
                estilo = Estilos.numeroS,
            )
            Text(
                text = ":",
                style = Estilos.numeroS,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Espacos.minimo),
            )
            CaixaDeNumero(
                valor = segundos,
                aoMudar = aoMudarSegundos,
                marcador = "00",
                unidade = null,
                descricao = "$rotulo, " + stringResource(R.string.manual_campo_segundos),
                cores = cores,
                cursor = MaterialTheme.colorScheme.primary,
                erro = erro != null,
                largura = 56.dp,
                altura = 48.dp,
                estilo = Estilos.numeroS,
            )
        }
        if (erro != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(erro))
        }
    }
}

// ---------------------------------------------------------------------------
// Peças
// ---------------------------------------------------------------------------

@Composable
private fun coresClaras() = CoresDaCaixa(
    fundo = LocalCoresPokerun.current.superficieAfundada,
    texto = MaterialTheme.colorScheme.onSurface,
    marcador = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    unidade = MaterialTheme.colorScheme.onSurfaceVariant,
)
