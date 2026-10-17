package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import kotlin.math.ceil

/**
 * A geometria do padrão de Pokébolas de fundo (docs/02 §4.4).
 *
 * **Os números repetem o `padraoSvg` do design system** (`docs/design-system/componentes/
 * bundle.js`): um ladrilho de 56dp com duas Pokébolas em contorno na diagonal, cada uma
 * com o aro, a faixa partida e o botão. Mudou lá, mude aqui e em `PadraoDePokebolasTest`.
 *
 * O desenho é o da Pokébola de contorno do sistema, menor e mais fino: a faixa sai do aro
 * e para antes do botão, e o vão que sobra faz o papel da fronteira de cor, como no ícone
 * monocromático (§2.7).
 */
internal object GeometriaDoPadrao {
    /** O lado do ladrilho que se repete. */
    val ladrilho = 56.dp

    /** Os centros das duas Pokébolas, a partir do canto do ladrilho. */
    val centros = listOf(DpOffset(14.dp, 14.dp), DpOffset(42.dp, 42.dp))

    /** O raio do aro. */
    val raio = 9.dp

    /** O raio do botão central. */
    val raioDoBotao = 2.6.dp

    /** Onde a faixa para, contado do centro: 0,4dp de vão em volta do botão. */
    val fimDaFaixa = 3.dp

    /** A largura do traço. A cor é `padrao-pokebola`, tinta a 5%. */
    val traco = 1.5.dp
}

/**
 * Desenha o padrão de Pokébolas atrás do conteúdo, ladrilhado a partir do canto de cima e
 * preso aos limites do elemento.
 *
 * **Onde ele aparece:** no cabeçalho da Pokédex e no [EstadoVazio], e só neles.
 * **Custo de fugir:** em todo canto ele vira papel de parede, e o app lê como fan app
 * (§4.4). Um lugar novo ganha linha em docs/02 §12, e `PadraoDePokebolasTest` muda no
 * mesmo commit.
 *
 * **É decorativo:** não tem semântica, não anima e não carrega informação, por isso fica
 * fora dos 3:1 de §8, item 1 (§4.2). O ponto mais escuro do traço sobre branco é mais claro
 * que `superficie-afundada`, o segundo chão que §2.2 já mede: todo texto que passa lá passa
 * aqui. O texto em `destaque` é a exceção de §2.2, e o botão primário é opaco.
 *
 * O caminho do ladrilho é montado uma vez por tamanho, no `drawWithCache`; cada quadro só
 * o translada. A cor entra por parâmetro para o modifier não ler `CompositionLocal`: quem
 * chama lê `LocalCoresPokerun.current.padraoPokebola`.
 */
fun Modifier.padraoDePokebolas(cor: Color): Modifier = drawWithCache {
    val g = GeometriaDoPadrao
    val lado = g.ladrilho.toPx()
    val raio = g.raio.toPx()
    val raioDoBotao = g.raioDoBotao.toPx()
    val fimDaFaixa = g.fimDaFaixa.toPx()

    val pokebolas = Path().apply {
        g.centros.forEach { centro ->
            val c = Offset(centro.x.toPx(), centro.y.toPx())
            addOval(Rect(c, raio))
            addOval(Rect(c, raioDoBotao))
            moveTo(c.x - raio, c.y)
            lineTo(c.x - fimDaFaixa, c.y)
            moveTo(c.x + fimDaFaixa, c.y)
            lineTo(c.x + raio, c.y)
        }
    }
    val traco = Stroke(width = g.traco.toPx())
    val colunas = ceil(size.width / lado).toInt()
    val linhas = ceil(size.height / lado).toInt()

    onDrawBehind {
        clipRect {
            for (linha in 0 until linhas) {
                for (coluna in 0 until colunas) {
                    translate(coluna * lado, linha * lado) {
                        drawPath(pokebolas, cor, style = traco)
                    }
                }
            }
        }
    }
}

/**
 * O estado vazio (docs/02 §4.2): o motivo, o que falta e a ação, numa [Ficha] sobre o
 * padrão de Pokébolas. É o `.pr-vazio` do design system.
 *
 * **A ficha dá ao padrão uma borda onde acabar.** Solto na tela, o ladrilho cortado na
 * altura do texto lê como defeito; dentro da borda de 1dp ele lê como fundo do bloco. O
 * recheio é `bloco` em cima e embaixo e `margem` dos lados, como na referência, para o
 * padrão ter altura e o texto não encostar na borda.
 *
 * Quem usa passa só o texto e a ação, sem borda nem recheio. A ação dentro do bloco não é
 * card dentro de card (§9.1): o botão é controle, e a ficha é a única caixa.
 */
@Composable
fun EstadoVazio(
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cor = LocalCoresPokerun.current.padraoPokebola
    Ficha(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padraoDePokebolas(cor)
                .padding(horizontal = Espacos.margem, vertical = Espacos.bloco),
            content = conteudo,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Vazio com ação", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EstadoVazioPreview() {
    PokerunTheme {
        EstadoVazio(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nenhuma corrida ainda. Registre a primeira e ela aparece aqui.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Espacos.bloco))
            BotaoPrimario(aoTocar = {}, modifier = Modifier.fillMaxWidth()) { Text("Registrar corrida") }
        }
    }
}

@Preview(name = "Vazio só com texto", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EstadoVazioSoTextoPreview() {
    PokerunTheme {
        EstadoVazio(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nenhuma corrida registrada nesta semana.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun EstadoVazioFonteGrandePreview() = EstadoVazioPreview()
