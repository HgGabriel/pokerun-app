package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** O filete vertical da borda esquerda. 3dp é o valor de docs/02 §2.4. */
private val LarguraDoFilete = 3.dp

/** O traço do triângulo. 1,5dp, também de §2.4. */
private val TracoDoTriangulo = 1.5.dp

private val LadoDoTriangulo = 16.dp
private val Recheio = 12.dp
private val EspacoDepoisDoIcone = 8.dp
private val EspacoDepoisDoRotulo = 4.dp

/**
 * O bloco de aviso do sistema (docs/02 §2.4).
 *
 * **Cor nunca é o único canal.** Todo uso de `alerta` carrega, obrigatoriamente e junto,
 * três coisas — e as três estão aqui: filete vertical de 3dp na borda esquerda, ícone de
 * triângulo em traço de 1,5dp e rótulo em mono caixa alta. A regra existe porque o token
 * `alerta` foi remedido em 06/08 justamente por colapsar contra `leitura` sob
 * deuteranopia: quem tem dicromacia precisa reconhecer o aviso **pela forma** antes de
 * reconhecer pela cor, e ~8% dos homens têm — num grupo de oito, é provável.
 *
 * **O triângulo é desenhado, e não um ícone do Material.** O conjunto `Outlined` mora em
 * `material-icons-extended`, que é dependência nova e a Fase 0 fechou a lista; o
 * `Filled.Warning` do núcleo é um triângulo **sólido**, e §2.4 pede traço de 1,5dp. Um
 * `Path` de três pontos custa dez linhas e acerta a espessura exata.
 *
 * **Não bloqueia nada.** O banner é um bloco de leitura: sem toque, sem botão, sem
 * dispensar. O primeiro consumidor é RN-30 na revisão do rascunho; `F1-T13` usa o mesmo
 * na edição do dono, e a Fase 2 usa em falha de sincronização.
 *
 * @param rotulo em mono caixa alta, como `RISCO DE LESÃO`. A caixa alta é aplicada aqui.
 * @param descricao o que o TalkBack lê. O bloco inteiro é um nó só: rótulo e corpo lidos
 *   em sequência viram duas paradas para uma informação.
 */
@Composable
fun BannerDeAlerta(
    rotulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    descricao: String = "$rotulo. $texto",
) {
    val esquema = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(esquema.errorContainer, MaterialTheme.shapes.extraSmall)
            .clearAndSetSemantics { contentDescription = descricao },
    ) {
        // O filete acompanha a altura do bloco, e não a da primeira linha: com três
        // linhas de texto ele precisa correr o bloco inteiro para ser o canal de forma
        // que §2.4 exige.
        Spacer(
            modifier = Modifier
                .width(LarguraDoFilete)
                .fillMaxHeight()
                .background(esquema.error),
        )

        Row(
            modifier = Modifier.padding(Recheio),
            verticalAlignment = Alignment.Top,
        ) {
            TrianguloDeAlerta()
            Spacer(Modifier.width(EspacoDepoisDoIcone))
            Column(verticalArrangement = Arrangement.spacedBy(EspacoDepoisDoRotulo)) {
                Text(
                    text = rotulo.uppercase(LocaleDoApp),
                    style = MaterialTheme.typography.labelMedium,
                    color = esquema.error,
                )
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurface,
                )
            }
        }
    }
}

/**
 * O triângulo de aviso, em traço de 1,5dp.
 *
 * Sem exclamação dentro: a 16dp o glifo interno vira um borrão de 1,5dp de largura, e
 * quem carrega a informação é o rótulo em caixa alta ao lado. A forma que precisa ser
 * reconhecível é a do triângulo.
 */
@Composable
private fun TrianguloDeAlerta() {
    val cor = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.size(LadoDoTriangulo)) {
        val traco = TracoDoTriangulo.toPx()
        // Meio traço de folga em cada lado, senão a linha é cortada pela borda do Canvas.
        val folga = traco / 2
        val largura = size.width - traco
        val altura = size.height - traco

        val caminho = Path().apply {
            moveTo(folga + largura / 2, folga)
            lineTo(folga + largura, folga + altura)
            lineTo(folga, folga + altura)
            close()
        }

        drawPath(path = caminho, color = cor, style = Stroke(width = traco))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF9F7)
@Composable
private fun BannerPreview() {
    PokerunTheme {
        BannerDeAlerta(
            rotulo = "Risco de lesão",
            texto = "Salto de 22% entre as semanas 6 e 7. Aumentos acima de 15% elevam risco de lesão.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF9F7, fontScale = 2.0f, widthDp = 320)
@Composable
private fun BannerFonteGrandePreview() = BannerPreview()
