package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.ui.componentes.AtalhoDoPerfil
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.marcaDePokebola
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import java.time.LocalDate

private val AlturaDaCena = 178.dp
private val TopoDaEstrada = 132.dp
private val EspessuraDaEstrada = 12.dp
private val MargemDaEstrada = 24.dp
private val TamanhoDoParceiro = 118.dp
private val TamanhoDoRival = 46.dp
private const val RIVAIS_VISIVEIS = 3

/**
 * A peça em tinta do Progresso (estudo, Direção A): o número da temporada, a pílula da
 * prova e a estrada, com o parceiro em 3D no ponto de hoje, os rivais ultrapassados à beira
 * do caminho e a bandeira da prova na cor do tipo do parceiro.
 *
 * **Arrastar a estrada leva o parceiro a outra semana**, e o número passa a mostrar o km
 * acumulado até ali. A cena responde ao dedo e para quando ele para: não há loop, contagem
 * automática nem entrada animada. Com movimento reduzido, o parceiro fica no ponto de hoje
 * e o arraste só troca o número.
 */
@Composable
internal fun EstradaEmTinta(estado: PainelUiState.Conteudo, aoAbrirPerfil: () -> Unit) {
    val estrada = estado.estrada
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val corDoTipo = estado.tipoDoParceiro?.let { LocalCoresDeTipo.current[it].base } ?: MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current
    var selecionada by remember(estrada?.indiceDeHoje, estrada?.semanas?.size) { mutableIntStateOf(estrada?.indiceDeHoje ?: 0) }
    val km = estrada?.semanas?.getOrNull(selecionada)?.kmAcumulado ?: 0.0
    val emHoje = estrada == null || selecionada == estrada.indiceDeHoje
    val pilula = textoDaPilula(estado.pilula)
    val descricao = stringResource(R.string.progresso_estrada_descricao, formatarKm(estrada?.kmNaTemporada ?: 0.0), pilula)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(tinta),
    ) {
        // O brilho na cor do tipo do parceiro, atrás dele, e a Pokébola em marca d'água.
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                Brush.radialGradient(listOf(corDoTipo.copy(alpha = 0.62f), Color.Transparent), center = Offset(size.width * 0.37f, size.height * 0.72f), radius = 120.dp.toPx()),
                radius = 120.dp.toPx(),
                center = Offset(size.width * 0.37f, size.height * 0.72f),
            )
            val raio = 115.dp.toPx()
            val centro = Offset(size.width + 45.dp.toPx(), 155.dp.toPx())
            marcaDePokebola(claro.copy(alpha = 0.08f), centro, raio)
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, end = Espacos.minimo, top = Espacos.relacionado),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.destino_progresso).uppercase(LocaleDoApp),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                AtalhoDoPerfil(aoTocar = aoAbrirPerfil)
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = Espacos.margem)
                    .clearAndSetSemantics { contentDescription = descricao },
            ) {
                Text(
                    (if (emHoje) stringResource(R.string.progresso_na_temporada) else rotuloDaSemana(estrada!!.semanas[selecionada].inicio)).uppercase(LocaleDoApp),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.72f),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatarKm(km), style = Estilos.numeroXl, color = claro)
                    Spacer(Modifier.width(Espacos.minimo))
                    Text(
                        stringResource(R.string.painel_unidade_km),
                        style = Estilos.numeroM,
                        color = claro,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    pilula,
                    style = Estilos.corpoForte,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(top = Espacos.relacionado)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = Espacos.interno, vertical = Espacos.minimo),
                )
            }

            if (estrada != null) {
                Cena(
                    estado = estado,
                    estrada = estrada,
                    selecionada = if (reduzido) estrada.indiceDeHoje else selecionada,
                    corDoTipo = corDoTipo,
                    aoSelecionar = { selecionada = it },
                )
            }
            Spacer(Modifier.height(Espacos.interno))
        }
    }
}

@Composable
private fun Cena(
    estado: PainelUiState.Conteudo,
    estrada: EstradaDaTemporada,
    selecionada: Int,
    corDoTipo: Color,
    aoSelecionar: (Int) -> Unit,
) {
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    val densidade = LocalDensity.current
    val n = estrada.semanas.size.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDaCena)
            .pointerInput(n) {
                val inicio = MargemDaEstrada.toPx()
                val largura = size.width - inicio * 2
                fun indice(x: Float) = (((x - inicio) / largura) * n).toInt().coerceIn(0, n - 1)
                detectHorizontalDragGestures(
                    onDragStart = { aoSelecionar(indice(it.x)) },
                    onHorizontalDrag = { mudanca, _ -> aoSelecionar(indice(mudanca.position.x)) },
                )
            }
            .pointerInput(n) {
                val inicio = MargemDaEstrada.toPx()
                val largura = size.width - inicio * 2
                detectTapGestures { aoSelecionar((((it.x - inicio) / largura) * n).toInt().coerceIn(0, n - 1)) }
            },
    ) {
        val larguraTotal = maxWidth
        val larguraDaEstrada = larguraTotal - MargemDaEstrada * 2
        val passo = larguraDaEstrada / n
        fun centro(i: Int): Dp = MargemDaEstrada + passo * i + passo / 2

        Canvas(modifier = Modifier.matchParentSize()) {
            val y = TopoDaEstrada.toPx()
            val esp = EspessuraDaEstrada.toPx()
            val x0 = MargemDaEstrada.toPx()
            val largura = size.width - x0 * 2
            val raio = CornerRadius(esp / 2)
            drawRoundRect(claro.copy(alpha = 0.08f), Offset(x0, y), Size(largura, esp), raio)
            val ate = largura * (estrada.indiceDeHoje + 1) / n
            drawRoundRect(claro.copy(alpha = 0.30f), Offset(x0, y), Size(ate, esp), raio)
            // Um traço por semana.
            for (i in 1 until n) {
                val x = x0 + largura * i / n
                drawRect(tinta.copy(alpha = 0.55f), Offset(x - 1f, y), Size(2.dp.toPx(), esp))
            }
            if (estrada.comProva) {
                val xf = x0 + largura - 4.dp.toPx()
                drawRoundRect(claro, Offset(xf, 70.dp.toPx()), Size(3.dp.toPx(), 68.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                val bandeira = Path().apply {
                    moveTo(xf + 3.dp.toPx(), 72.dp.toPx())
                    relativeLineTo(22.dp.toPx(), 0f)
                    relativeLineTo(-6.dp.toPx(), 9.dp.toPx())
                    relativeLineTo(6.dp.toPx(), 9.dp.toPx())
                    relativeLineTo(-22.dp.toPx(), 0f)
                    close()
                }
                drawPath(bandeira, corDoTipo)
            }
        }

        // Os rivais ultrapassados: os três mais recentes, e o resto numa pílula `+N`.
        val passados = estrada.rivais.filter { it.indice <= selecionada }
        val ultimos = passados.flatMap { it.especies }.takeLast(RIVAIS_VISIVEIS)
        val resto = passados.sumOf { it.especies.size } - ultimos.size
        val semanaDosRivais = passados.lastOrNull()?.indice ?: 0
        val xRivais = (centro(semanaDosRivais) - TamanhoDoRival * 1.5f).coerceAtLeast(0.dp)
        ultimos.forEachIndexed { i, numero ->
            AsyncImage(
                model = spriteDeJogoDe(numero),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .offset(x = xRivais + 22.dp * i, y = 88.dp + 2.dp * i)
                    .size(TamanhoDoRival),
            )
        }
        if (resto > 0) {
            Text(
                "+$resto",
                style = naFigura(Estilos.legenda),
                color = claro,
                modifier = Modifier
                    .offset(x = (xRivais - 16.dp).coerceAtLeast(4.dp), y = 62.dp)
                    .background(claro.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        // O parceiro, em 3D, no ponto da semana escolhida.
        estado.parceiro?.let { numero ->
            SpriteAnimado(
                numero = numero,
                modifier = Modifier
                    .offset(x = centro(selecionada) - TamanhoDoParceiro / 2, y = TopoDaEstrada - TamanhoDoParceiro + 8.dp)
                    .size(TamanhoDoParceiro),
            )
        }

        // Com o parceiro arrastado até a bandeira, o rótulo da prova sairia por cima dele.
        if (estrada.comProva && selecionada < n - 2) {
            estado.prova?.let { prova ->
                Text(
                    "${formatarKm(prova.distanciaKm)} km",
                    style = naFigura(Estilos.corpoForte.copy(fontSize = 13.sp)),
                    color = claro,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 40.dp),
                )
            }
        }

        // As datas: o começo, hoje e o fim.
        val estilo = naFigura(Estilos.rotulo.copy(fontSize = 11.sp))
        Box(modifier = Modifier.fillMaxWidth().offset(y = 150.dp)) {
            Text(
                dataCurta(estrada.inicio),
                style = estilo,
                color = claro.copy(alpha = 0.64f),
                modifier = Modifier.offset(x = MargemDaEstrada),
            )
            Text(
                stringResource(R.string.progresso_hoje),
                style = estilo,
                color = claro,
                modifier = Modifier.offset(x = (centro(estrada.indiceDeHoje) - 14.dp).coerceAtLeast(MargemDaEstrada + 48.dp)),
            )
            Text(
                dataCurta(estrada.fim),
                style = estilo,
                color = claro.copy(alpha = 0.64f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = MargemDaEstrada),
            )
        }
    }
}

private fun dataCurta(data: LocalDate): String = "${data.dayOfMonth} ${nomeCurtoDoMes(data)}"

/** `ATÉ 2 NOV`: a semana arrastada, pelo domingo dela. */
@Composable
private fun rotuloDaSemana(inicio: LocalDate): String = stringResource(R.string.progresso_ate, dataCurta(inicio.plusDays(6)))

/** O texto da pílula da estrada (decisão 17). */
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

