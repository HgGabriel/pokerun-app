package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.width
import com.hggabriel.pokerun.ui.componentes.BlocoEmEspera
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.LocalFolgaDaBarraDoTopo
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import java.time.LocalDate
import kotlin.math.roundToInt

private val LarguraDaRoda = 42.dp
private val AlturaDaRoda = 62.dp
private val AlturaDaRegua = 46.dp
private val TamanhoDoParceiro = 92.dp

/** As casas do odômetro: 4 inteiras e 1 decimal. Acima de 9.999,9 km, fica no máximo. */
private const val CASAS = 5

/**
 * Os dígitos do odômetro para [km]: 4 inteiros e 1 decimal, com zero à esquerda. `120,4`
 * vira `0,1,2,0,4`.
 */
internal fun digitosDoOdometro(km: Double): List<Int> {
    val decimos = (km * 10).roundToInt().coerceIn(0, 99_999)
    return decimos.toString().padStart(CASAS, '0').map { it - '0' }
}

/**
 * O topo do Progresso (27/10/2026): o odômetro da temporada. Ele substitui a estrada em
 * tinta, que deixava o Progresso parecido demais com a capa da Pokédex; a tinta fica só lá,
 * e os rivais moram no álbum.
 *
 * O km da temporada em rodas, com a decimal em `destaque`; o parceiro em 3D apoiado no canto
 * do cartão; a pílula da decisão 17; e a régua das semanas. **Tocar na régua gira
 * as rodas** até o km acumulado daquela semana. Com movimento reduzido, as rodas saltam.
 */
@Composable
internal fun OdometroDaTemporada(estado: PainelUiState.Conteudo, esperando: Boolean = false) {
    val estrada = estado.estrada
    var selecionada by remember(estrada?.indiceDeHoje, estrada?.semanas?.size) { mutableIntStateOf(estrada?.indiceDeHoje ?: 0) }
    val km = estrada?.semanas?.getOrNull(selecionada)?.kmAcumulado ?: 0.0
    val emHoje = estrada == null || selecionada == estrada.indiceDeHoje
    val pilula = textoDaPilula(estado.pilula)
    val descricao = stringResource(R.string.progresso_odometro_descricao, formatarKm(estrada?.kmNaTemporada ?: 0.0), pilula)

    Column {
        // Na raiz da aba a linha de cima é a barra do topo (28/10/2026).
        val folga = LocalFolgaDaBarraDoTopo.current
        if (folga > 0.dp) {
            Spacer(Modifier.height(folga))
        } else {
            Text(
                stringResource(R.string.destino_progresso).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, top = Espacos.relacionado + Espacos.interno),
            )
        }

        Box(modifier = Modifier.padding(start = Espacos.margem, end = Espacos.margem, top = 44.dp)) {
            Cartao {
                // Lendo, o rótulo, as rodas e a pílula pulsam no lugar deles: zero km leria como dado.
                if (esperando) {
                    Column {
                        BlocoEmEspera(14.dp, Modifier.width(120.dp))
                        BlocoEmEspera(56.dp, Modifier.padding(top = Espacos.interno).fillMaxWidth(0.7f))
                        BlocoEmEspera(32.dp, Modifier.padding(top = Espacos.interno).width(140.dp), RoundedCornerShape(50))
                    }
                    return@Cartao
                }
                Column(modifier = Modifier.clearAndSetSemantics { contentDescription = descricao }) {
                    Text(
                        (if (emHoje) stringResource(R.string.progresso_na_temporada) else rotuloDaSemana(estrada!!.semanas[selecionada].inicio))
                            .uppercase(LocaleDoApp),
                        style = Estilos.rotulo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Rodas(km, Modifier.padding(top = Espacos.interno))
                    Text(
                        pilula,
                        style = Estilos.corpoForte,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(top = Espacos.interno)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .padding(horizontal = Espacos.interno, vertical = Espacos.minimo),
                    )
                }
                if (estrada != null && estrada.semanas.isNotEmpty()) {
                    Regua(estrada, selecionada, aoSelecionar = { selecionada = it }, modifier = Modifier.padding(top = Espacos.margem))
                }
            }
            // O parceiro apoiado no canto de cima, saindo do cartão.
            estado.parceiro?.let { numero ->
                SpriteAnimado(
                    numero = numero,
                    paraNoReduzido = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-50).dp)
                        .size(TamanhoDoParceiro),
                )
            }
        }
    }
}

/**
 * As rodas: dígitos em Poppins 700 de 40sp numa fita vertical de 0 a 9, que gira até o
 * dígito com a curva de captura. A decimal é vermelha com o dígito branco; o zero à esquerda
 * fica apagado. As rodas são figura: param de crescer em 1,3 de fonte.
 */
@Composable
private fun Rodas(km: Double, modifier: Modifier) {
    val digitos = digitosDoOdometro(km)
    val primeiraSignificativa = digitos.take(CASAS - 2).indexOfFirst { it != 0 }.let { if (it < 0) CASAS - 2 else it }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        digitos.forEachIndexed { i, d ->
            if (i == CASAS - 1) {
                Text(",", style = naFigura(EstiloDaRoda), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp))
            }
            Roda(d, decimal = i == CASAS - 1, apagada = i < primeiraSignificativa)
        }
        Text(
            stringResource(R.string.painel_unidade_km),
            style = Estilos.numeroM,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
        )
    }
}

private val EstiloDaRoda = Estilos.numeroL.copy(fontSize = 40.sp, lineHeight = 44.sp)

@Composable
private fun Roda(digito: Int, decimal: Boolean, apagada: Boolean) {
    val reduzido = LocalMovimentoReduzido.current
    val giro = remember { Animatable(digito.toFloat()) }
    LaunchedEffect(digito) {
        if (reduzido) giro.snapTo(digito.toFloat()) else giro.animateTo(digito.toFloat(), Movimento.lento(curva = Movimento.Curva.captura))
    }
    val fundo = if (decimal) MaterialTheme.colorScheme.primary else LocalCoresPokerun.current.superficieAfundada
    val tinta = when {
        decimal -> MaterialTheme.colorScheme.onPrimary
        apagada -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sombra = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val densidade = LocalDensity.current
    val passo = with(densidade) { AlturaDaRoda.toPx() }
    Box(
        modifier = Modifier
            .size(LarguraDaRoda, AlturaDaRoda)
            .clip(RoundedCornerShape(10.dp))
            .background(fundo)
            // A sombra de dentro, em cima e embaixo: a roda é um tambor.
            .drawWithContent {
                drawContent()
                val faixa = 12.dp.toPx()
                drawRect(Brush.verticalGradient(listOf(sombra, Color.Transparent), 0f, faixa), size = Size(size.width, faixa))
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, sombra), size.height - faixa, size.height),
                    topLeft = Offset(0f, size.height - faixa),
                    size = Size(size.width, faixa),
                )
            },
    ) {
        // A fita de 0 a 9, que sobe até o dígito.
        Column(
            modifier = Modifier
                .layout { medida, restricoes ->
                    val p = medida.measure(restricoes.copy(maxHeight = Int.MAX_VALUE))
                    layout(p.width, restricoes.maxHeight) { p.place(0, 0) }
                }
                .graphicsLayer { translationY = -giro.value * passo },
        ) {
            (0..9).forEach { n ->
                Box(Modifier.size(LarguraDaRoda, AlturaDaRoda), contentAlignment = Alignment.Center) {
                    Text(n.toString(), style = naFigura(EstiloDaRoda), color = tinta)
                }
            }
        }
    }
}

/**
 * A régua das semanas: uma pílula fina por semana, da primeira corrida à semana da prova,
 * com a altura do km da semana. Passadas em cinza, a selecionada em `destaque`, as futuras
 * em `superficie-afundada` e a da prova em tinta. Datas nas pontas.
 */
@Composable
private fun Regua(estrada: EstradaDaTemporada, selecionada: Int, aoSelecionar: (Int) -> Unit, modifier: Modifier) {
    val n = estrada.semanas.size
    val doSemana = estrada.semanas.mapIndexed { i, s -> s.kmAcumulado - (estrada.semanas.getOrNull(i - 1)?.kmAcumulado ?: 0.0) }
    val maior = doSemana.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val passada = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val futura = LocalCoresPokerun.current.superficieAfundada
    val destaque = MaterialTheme.colorScheme.primary
    val tinta = MaterialTheme.colorScheme.onSurface
    val rotulo = stringResource(R.string.progresso_regua)
    val estadoFalado = if (selecionada == estrada.indiceDeHoje) {
        stringResource(R.string.progresso_na_temporada)
    } else {
        rotuloDaSemana(estrada.semanas[selecionada].inicio)
    }
    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDaRegua)
                // Só toque: o arraste lateral troca de aba (29/10/2026).
                .pointerInput(n) {
                    detectTapGestures {
                        aoSelecionar((it.x / size.width * n).toInt().coerceIn(0, minOf(n - 1, estrada.indiceDeHoje)))
                    }
                }
                .clearAndSetSemantics {
                    contentDescription = rotulo
                    stateDescription = estadoFalado
                    progressBarRangeInfo = ProgressBarRangeInfo(selecionada.toFloat(), 0f..estrada.indiceDeHoje.toFloat().coerceAtLeast(1f))
                    setProgress { v ->
                        aoSelecionar(v.roundToInt().coerceIn(0, estrada.indiceDeHoje))
                        true
                    }
                },
        ) {
            val vao = 4.dp.toPx()
            val largura = ((size.width - vao * (n - 1)) / n).coerceAtLeast(1f)
            val minimo = 4.dp.toPx()
            doSemana.forEachIndexed { i, kmDaSemana ->
                val prova = estrada.comProva && i == n - 1
                val cor = when {
                    i == selecionada -> destaque
                    prova -> tinta
                    i > estrada.indiceDeHoje -> futura
                    else -> passada
                }
                val altura = if (i > estrada.indiceDeHoje && !prova) {
                    minimo
                } else if (prova) {
                    size.height
                } else {
                    (size.height * (kmDaSemana / maior).toFloat()).coerceAtLeast(minimo)
                }
                drawRoundRect(
                    cor,
                    topLeft = Offset(i * (largura + vao), size.height - altura),
                    size = Size(largura, altura),
                    cornerRadius = CornerRadius(minOf(3.dp.toPx(), largura / 2)),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = Espacos.minimo)) {
            Text(dataCurta(estrada.inicio), style = naFigura(Estilos.legenda), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(dataCurta(estrada.fim), style = naFigura(Estilos.legenda), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun dataCurta(data: LocalDate): String = "${data.dayOfMonth} ${nomeCurtoDoMes(data)}"

/** `ATÉ 2 NOV`: a semana escolhida na régua, pelo domingo dela. */
@Composable
private fun rotuloDaSemana(inicio: LocalDate): String = stringResource(R.string.progresso_ate, dataCurta(inicio.plusDays(6)))

/** O texto da pílula (decisão 17). */
@Composable
internal fun textoDaPilula(pilula: PilulaDaEstrada): String = when (pilula) {
    is PilulaDaEstrada.PartesDaProva -> stringResource(R.string.progresso_pilula_prova, pilula.porcento, formatarKm(pilula.provaKm))
    is PilulaDaEstrada.ProvasInteiras -> pluralStringResource(
        R.plurals.progresso_pilula_provas,
        pilula.vezes.toInt(),
        formatarKm(pilula.vezes),
        formatarKm(pilula.provaKm),
    )
    is PilulaDaEstrada.PartesDaClassica -> stringResource(
        when (pilula.distancia) {
            DistanciaClassica.CINCO -> R.string.progresso_pilula_5k
            DistanciaClassica.DEZ -> R.string.progresso_pilula_10k
            DistanciaClassica.MEIA -> R.string.progresso_pilula_meia
            DistanciaClassica.MARATONA -> R.string.progresso_pilula_maratona
        },
        pilula.porcento,
    )
    is PilulaDaEstrada.Maratonas -> pluralStringResource(R.plurals.progresso_pilula_maratonas, pilula.vezes.toInt(), formatarKm(pilula.vezes))
}
