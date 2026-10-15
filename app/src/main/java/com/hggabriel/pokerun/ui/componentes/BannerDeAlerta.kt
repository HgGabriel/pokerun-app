package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Tamanhos

/** O contorno do bloco de alerta, em `alerta`. Sucesso e informação não têm contorno. */
internal val ContornoDoAlerta = 1.dp

/**
 * O traço dos ícones do bloco: 1,75dp, o traço do conjunto de ícones do sistema
 * (docs/02 §4, `Tamanhos.icone`). `internal` para o teste medir a peça: o
 * `Filled.Warning` do núcleo do Material é um triângulo **sólido**, e é o caminho de
 * menor esforço para quem mexer nisto depois.
 */
internal val TracoDoIcone = 1.75.dp

private val EspacoDepoisDoRotulo = Espacos.minimo

/** O que o bloco diz: risco ou erro, confirmação, ou só informação (`Alerta.md`). */
enum class TipoDeAviso { Alerta, Sucesso, Informacao }

/** O ícone de cada tipo. Cor nunca é o único canal: cada tipo tem forma própria. */
internal enum class IconeDoAviso { Triangulo, Check, Info }

/** As peças de forma de cada tipo, separadas das cores para o teste ler a regra. */
internal data class PecasDoAviso(val icone: IconeDoAviso, val contorno: Boolean)

/**
 * docs/02 §2.4: alerta leva triângulo e contorno em `alerta`; sucesso leva check;
 * informação leva `i`. Nenhum leva filete lateral.
 */
internal fun pecasDoAviso(tipo: TipoDeAviso): PecasDoAviso = when (tipo) {
    TipoDeAviso.Alerta -> PecasDoAviso(IconeDoAviso.Triangulo, contorno = true)
    TipoDeAviso.Sucesso -> PecasDoAviso(IconeDoAviso.Check, contorno = false)
    TipoDeAviso.Informacao -> PecasDoAviso(IconeDoAviso.Info, contorno = false)
}

/**
 * O rótulo do canal de alerta, em caixa alta (docs/02 §2.4).
 *
 * Três linhas com razão de existir, e a terceira é a que motivou tirá-lo de dentro
 * do `Text`:
 *
 * - **A caixa alta é desenho, e sai daqui.** O chamador passa `Risco de lesão`, e
 *   `strings.xml` guarda a frase em caixa mista — parte dos leitores de tela soletra
 *   palavra inteira em maiúscula, então o que o TalkBack lê nunca é esta saída.
 * - **O locale é o do app, nunca o da máquina.** Em turco `sincronização` viraria
 *   `SİNCRONIZAÇÃO`, e quem executa não é quem lê.
 * - **Rótulo em branco falha.** Sem rótulo, a cor e o ícone ficariam sozinhos, e §2.4
 *   exige o rótulo junto. É o atalho óbvio, e por isso é o que precisa quebrar cedo.
 */
internal fun rotuloDoCanal(rotulo: String): String {
    require(rotulo.isNotBlank()) {
        "Aviso sem rótulo deixa a cor e o ícone sozinhos (docs/02 §2.4)"
    }
    return rotulo.uppercase(LocaleDoApp)
}

/**
 * O bloco de aviso do sistema (docs/02 §2.4, `Alerta.md`).
 *
 * **Cor nunca é o único canal.** Todo uso de `alerta` carrega, junto, o bloco em
 * `alerta-suave` com contorno em `alerta`, o ícone de triângulo e o rótulo em caixa
 * alta no estilo `rotulo`. `destaque` e `alerta` se separam por valor, mas sob
 * protanopia ficam a ΔE 13,8: quem tem dicromacia reconhece o aviso **pela forma**.
 *
 * **Sem filete lateral colorido.** O canal redundante é o ícone e o rótulo.
 *
 * Sucesso usa `sucesso-suave` e check; informação usa `superficie-afundada` e `i`.
 *
 * **O ícone é desenhado, e não um ícone do Material.** O conjunto `Outlined` mora em
 * `material-icons-extended`, que é dependência nova; o núcleo só tem glifos sólidos.
 * Um `Path` acerta o traço de 1,75dp.
 *
 * **Não bloqueia nada.** O bloco é de leitura: sem toque, sem botão, sem dispensar. O
 * `Tentar de novo` de erro de tela é um botão da própria tela, abaixo do bloco.
 *
 * @param rotulo em caixa alta, como `RISCO DE LESÃO`. A caixa alta é aplicada aqui.
 * @param descricao o que o TalkBack lê. O bloco inteiro é um nó só: rótulo e corpo lidos
 *   em sequência viram duas paradas para uma informação.
 */
@Composable
fun BannerDeAlerta(
    rotulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    tipo: TipoDeAviso = TipoDeAviso.Alerta,
    descricao: String = "$rotulo. $texto",
) {
    val cores = LocalCoresPokerun.current
    val esquema = MaterialTheme.colorScheme
    val pecas = pecasDoAviso(tipo)

    val (fundo, tinta) = when (tipo) {
        TipoDeAviso.Alerta -> cores.alertaSuave to cores.alerta
        TipoDeAviso.Sucesso -> cores.sucessoSuave to cores.sucesso
        TipoDeAviso.Informacao -> cores.superficieAfundada to esquema.onSurface
    }

    val contorno = if (pecas.contorno) {
        Modifier.border(ContornoDoAlerta, tinta, FormaControle)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(fundo, FormaControle)
            .then(contorno)
            .padding(Espacos.interno)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.Top,
    ) {
        IconeDeAviso(pecas.icone, tinta)
        Spacer(Modifier.width(Espacos.relacionado))
        Column(verticalArrangement = Arrangement.spacedBy(EspacoDepoisDoRotulo)) {
            Text(
                text = rotuloDoCanal(rotulo),
                style = MaterialTheme.typography.labelMedium,
                color = tinta,
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurface,
            )
        }
    }
}

/**
 * O ícone do bloco, em traço de 1,75dp, no tamanho do ícone de tipo (16dp).
 *
 * O triângulo vai sem exclamação dentro: a 16dp o glifo interno vira um borrão, e quem
 * carrega a informação é o rótulo ao lado. A forma que precisa ser reconhecível é a do
 * triângulo.
 */
@Composable
private fun IconeDeAviso(icone: IconeDoAviso, cor: Color) {
    Canvas(modifier = Modifier.size(Tamanhos.iconeTipo)) {
        val traco = TracoDoIcone.toPx()
        val estilo = Stroke(width = traco, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icone) {
            IconeDoAviso.Triangulo -> triangulo(cor, estilo)
            IconeDoAviso.Check -> check(cor, estilo)
            IconeDoAviso.Info -> info(cor, estilo)
        }
    }
}

private fun DrawScope.triangulo(cor: Color, estilo: Stroke) {
    // Meio traço de folga em cada lado, senão a linha é cortada pela borda do Canvas.
    val folga = estilo.width / 2
    val largura = size.width - estilo.width
    val altura = size.height - estilo.width
    val caminho = Path().apply {
        moveTo(folga + largura / 2, folga)
        lineTo(folga + largura, folga + altura)
        lineTo(folga, folga + altura)
        close()
    }
    drawPath(path = caminho, color = cor, style = estilo)
}

private fun DrawScope.check(cor: Color, estilo: Stroke) {
    val caminho = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.52f)
        lineTo(size.width * 0.40f, size.height * 0.77f)
        lineTo(size.width * 0.85f, size.height * 0.27f)
    }
    drawPath(path = caminho, color = cor, style = estilo)
}

private fun DrawScope.info(cor: Color, estilo: Stroke) {
    val raio = (size.minDimension - estilo.width) / 2
    drawCircle(color = cor, radius = raio, style = estilo)
    drawCircle(color = cor, radius = estilo.width * 0.6f, center = Offset(center.x, size.height * 0.30f))
    drawLine(
        color = cor,
        start = Offset(center.x, size.height * 0.46f),
        end = Offset(center.x, size.height * 0.72f),
        strokeWidth = estilo.width,
        cap = StrokeCap.Round,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BannerPreview() {
    PokerunTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BannerDeAlerta(
                rotulo = "Risco de lesão",
                texto = "Salto de 22% entre as semanas 6 e 7. Aumentos acima de 15% elevam risco de lesão.",
            )
            BannerDeAlerta(
                rotulo = "Guardado",
                texto = "A corrida entrou na semana 9.",
                tipo = TipoDeAviso.Sucesso,
            )
            BannerDeAlerta(
                rotulo = "Semana congelada",
                texto = "O plano volta a contar quando você retomar.",
                tipo = TipoDeAviso.Informacao,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, fontScale = 2.0f, widthDp = 320)
@Composable
private fun BannerFonteGrandePreview() = BannerPreview()
