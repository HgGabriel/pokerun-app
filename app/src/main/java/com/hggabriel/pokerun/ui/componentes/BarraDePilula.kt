package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaChip
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/**
 * A pílula de comparação: trilho em `superficie-afundada` e o cheio em [cor] (tinta por
 * padrão). Nasceu no ranking da aba Grupo e mora aqui desde que o Progresso passou a usá-la
 * (estudo de Progresso e Perfil, F3). O dado enche a pílula como um tubo.
 */
@Composable
fun BarraDePilula(
    fracao: Float,
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.onSurface,
    altura: Dp = 8.dp,
) {
    val trilho = LocalCoresPokerun.current.superficieAfundada
    Canvas(modifier = modifier.fillMaxWidth().height(altura).clearAndSetSemantics {}) {
        val raio = CornerRadius(size.height / 2)
        drawRoundRect(trilho, cornerRadius = raio)
        if (fracao > 0f) {
            val largura = (size.width * fracao.coerceAtMost(1f)).coerceAtLeast(size.height)
            drawRoundRect(cor, size = Size(largura, size.height), cornerRadius = raio)
        }
    }
}

/**
 * Uma coluna do [GraficoDePilulas].
 *
 * @property fracao quanto a pílula enche, de 0 a 1.
 * @property balao o valor num balão em tinta acima da coluna: a semana atual ou a última.
 */
data class ColunaDePilula(
    val fracao: Float,
    val cor: Color,
    val rotulo: String? = null,
    val balao: String? = null,
    val rotuloForte: Boolean = false,
)

private val AlturaDoBalao = 26.dp
private val LarguraDaPilula = 18.dp

/**
 * Pílulas cheias sobre trilho, sem eixo e sem grade (decisão 21): a mesma linguagem do
 * trilho do longão. Volume, pace e eficiência cardíaca usam a mesma figura, cada um com a
 * sua cor e o seu sentido. O gráfico é um nó só para o TalkBack, com a [descricao].
 */
@Composable
fun GraficoDePilulas(
    colunas: List<ColunaDePilula>,
    descricao: String,
    modifier: Modifier = Modifier,
    altura: Dp = 112.dp,
) {
    val trilho = LocalCoresPokerun.current.superficieAfundada
    val tinta = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descricao },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        colunas.forEach { coluna ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.heightIn(min = AlturaDoBalao), contentAlignment = Alignment.Center) {
                    if (coluna.balao != null) {
                        Text(
                            text = coluna.balao,
                            style = naFigura(Estilos.legenda),
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .background(tinta, FormaChip)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Canvas(modifier = Modifier.width(LarguraDaPilula).height(altura)) {
                    val raio = CornerRadius(size.width / 2)
                    drawRoundRect(trilho, cornerRadius = raio)
                    val f = coluna.fracao.coerceIn(0f, 1f)
                    if (f > 0f) {
                        val h = (size.height * f).coerceAtLeast(size.width)
                        drawRoundRect(coluna.cor, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = raio)
                    }
                }
                if (coluna.rotulo != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = coluna.rotulo,
                        style = naFigura(Estilos.legenda),
                        color = if (coluna.rotuloForte) tinta else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
