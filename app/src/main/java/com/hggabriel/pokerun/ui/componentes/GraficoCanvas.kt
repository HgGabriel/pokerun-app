package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.EstiloDado
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/** Um ponto do gráfico: o rótulo do eixo X, o valor e o valor já escrito para leitura. */
data class PontoDoGrafico(val rotulo: String, val valor: Double, val valorEscrito: String)

/** A escala do eixo Y: os limites e as marcas da grade, em passos redondos. */
data class EscalaDoEixo(val minimo: Double, val maximo: Double, val marcas: List<Double>)

/** Barras para o que se soma (volume); linha para o que se acompanha (pace, FC). */
enum class FormaDoGrafico { BARRAS, LINHA }

/** Uns quatro intervalos na grade: o bastante para ler, pouco para poluir. */
private const val INTERVALOS_DESEJADOS = 4
private val PASSOS_REDONDOS = listOf(1.0, 2.0, 5.0, 10.0)

/**
 * A escala do eixo Y: parte do zero (barras de volume) ou do passo redondo abaixo do
 * menor valor (pace, FC), e fecha no passo redondo acima do maior. Os passos são 1, 2 ou
 * 5 vezes uma potência de 10. Série vazia ou toda igual ainda tem altura para desenhar.
 */
fun escalaDoEixo(valores: List<Double>, apartirDoZero: Boolean = true): EscalaDoEixo {
    val maior = valores.maxOrNull() ?: 0.0
    val menor = if (apartirDoZero) 0.0 else valores.minOrNull() ?: 0.0
    val amplitude = (maior - menor).takeIf { it > 0 } ?: maxOf(1.0, maior * 0.1)

    val bruto = amplitude / INTERVALOS_DESEJADOS
    val grandeza = 10.0.pow(floor(log10(bruto)))
    val passo = PASSOS_REDONDOS.first { it * grandeza >= bruto } * grandeza

    val primeira = floor(menor / passo).toLong()
    var ultima = ceil(maior / passo).toLong()
    if (ultima <= primeira) ultima = primeira + 1
    val marcas = (primeira..ultima).map { arredondar(it * passo) }
    return EscalaDoEixo(marcas.first(), marcas.last(), marcas)
}

/** Corta o ruído de ponto flutuante: `0,1 × 3` é `0,3`, e não `0,30000000000000004`. */
private fun arredondar(valor: Double): Double = (valor * 1e9).roundToLong() / 1e9

/**
 * O gráfico em texto, para o leitor de tela (docs/02 §8, item 5): o título e cada ponto
 * com rótulo, valor e unidade, na ordem do eixo.
 */
fun descricaoFalada(titulo: String, pontos: List<PontoDoGrafico>, unidade: String): String =
    "$titulo. " + pontos.joinToString("; ") { "${it.rotulo}, ${it.valorEscrito} $unidade" } + "."

private val AlturaPadrao = 160.dp
private val ColunaDoEixo = 40.dp
private val EspacoDoEixo = 4.dp
private val EspessuraDaLinha = 2.dp
private val EspessuraDaGrade = 1.dp
private const val FRACAO_DA_BARRA = 0.6f

/**
 * A base de gráficos da `StatsDashboardScreen` (`F3-T01`, docs/03 §3.17), em Canvas e sem
 * biblioteca.
 *
 * - **Raio 0dp** nas barras (`FormaDado`), com o valor em mono junto da barra.
 * - **Cor:** acento em `leitura` (o padrão); série secundária em `sinal`, que a curva de
 *   FC usa. Grade em `borda`. Nada de cor literal (docs/02 §2.6).
 * - **Nenhuma animação de entrada** (docs/02 §6).
 * - **O desenho não fala**: a semântica do bloco inteiro é [descricaoFalada], com todos os
 *   valores (docs/02 §8, item 5).
 *
 * @param marcadores índices de [pontos] que ganham uma linha vertical: as provas concluídas
 *   no volume semanal (`F3-T04`).
 */
@Composable
fun GraficoCanvas(
    titulo: String,
    pontos: List<PontoDoGrafico>,
    unidade: String,
    forma: FormaDoGrafico,
    modifier: Modifier = Modifier,
    secundaria: Boolean = false,
    apartirDoZero: Boolean = forma == FormaDoGrafico.BARRAS,
    marcadores: Set<Int> = emptySet(),
    altura: Dp = AlturaPadrao,
    escreverMarca: (Double) -> String = { formatarKm(it) },
) {
    val escala = escalaDoEixo(pontos.map { it.valor }, apartirDoZero)
    val cor: Color = if (secundaria) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val grade = MaterialTheme.colorScheme.outlineVariant
    val tintaFraca = MaterialTheme.colorScheme.onSurfaceVariant
    val medidor = rememberTextMeasurer()
    val estiloDoValor = EstiloDado.copy(color = tintaFraca, fontSize = MaterialTheme.typography.labelSmall.fontSize)

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = descricaoFalada(titulo, pontos, unidade) }) {
        Row(modifier = Modifier.fillMaxWidth().height(altura)) {
            Column(modifier = Modifier.width(ColunaDoEixo).fillMaxHeight()) {
                Text(text = escreverMarca(escala.maximo), style = EstiloDado, color = tintaFraca)
                Spacer(Modifier.weight(1f))
                Text(text = escreverMarca(escala.minimo), style = EstiloDado, color = tintaFraca)
            }
            Spacer(Modifier.width(EspacoDoEixo))
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val largura = size.width
                val alto = size.height
                fun y(valor: Double): Float =
                    (alto - ((valor - escala.minimo) / (escala.maximo - escala.minimo)) * alto).toFloat()

                escala.marcas.forEach { marca ->
                    drawLine(grade, Offset(0f, y(marca)), Offset(largura, y(marca)), EspessuraDaGrade.toPx())
                }
                if (pontos.isEmpty()) return@Canvas

                val fatia = largura / pontos.size
                fun centro(indice: Int): Float = fatia * indice + fatia / 2

                marcadores.filter { it in pontos.indices }.forEach { indice ->
                    drawLine(tintaFraca, Offset(centro(indice), 0f), Offset(centro(indice), alto), EspessuraDaGrade.toPx())
                }

                when (forma) {
                    FormaDoGrafico.BARRAS -> pontos.forEachIndexed { indice, ponto ->
                        val larguraDaBarra = fatia * FRACAO_DA_BARRA
                        val topo = y(ponto.valor)
                        // Raio 0dp: retângulo puro (FormaDado).
                        drawRect(cor, Offset(centro(indice) - larguraDaBarra / 2, topo), Size(larguraDaBarra, alto - topo))
                        val texto = medidor.measure(ponto.valorEscrito, estiloDoValor)
                        if (texto.size.width <= fatia) {
                            drawText(
                                texto,
                                topLeft = Offset(centro(indice) - texto.size.width / 2, (topo - texto.size.height).coerceAtLeast(0f)),
                            )
                        }
                    }
                    FormaDoGrafico.LINHA -> {
                        val caminho = Path()
                        pontos.forEachIndexed { indice, ponto ->
                            if (indice == 0) caminho.moveTo(centro(indice), y(ponto.valor)) else caminho.lineTo(centro(indice), y(ponto.valor))
                        }
                        drawPath(caminho, cor, style = Stroke(width = EspessuraDaLinha.toPx()))
                        pontos.forEachIndexed { indice, ponto ->
                            drawCircle(cor, radius = EspessuraDaLinha.toPx() * 1.5f, center = Offset(centro(indice), y(ponto.valor)))
                        }
                    }
                }
            }
        }
        if (pontos.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(ColunaDoEixo + EspacoDoEixo))
                Text(text = pontos.first().rotulo, style = EstiloDado, color = tintaFraca)
                Spacer(Modifier.weight(1f))
                if (pontos.size > 1) Text(text = pontos.last().rotulo, style = EstiloDado, color = tintaFraca)
            }
        }
    }
}
