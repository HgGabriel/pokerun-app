package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Raios
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/** O quanto o dia correu: quilômetros somados e quantas corridas. */
data class DiaDoHeatmap(val data: LocalDate, val km: Double, val corridas: Int) {
    val degrau: Int get() = degrauDoDia(km, corridas)
}

/** A grade anual: uma coluna por semana (segunda a domingo), com nulo fora do ano. */
data class GradeDoHeatmap(val ano: Int, val colunas: List<List<DiaDoHeatmap?>>) {
    val dias: List<DiaDoHeatmap> get() = colunas.flatten().filterNotNull()
    val diasComCorrida: Int get() = dias.count { it.corridas > 0 }
    val kmNoAno: Double get() = dias.sumOf { it.km }
}

/** Quilômetros e dias de um mês, para a leitura falada. */
data class ResumoDoMes(val mes: Int, val diasComCorrida: Int, val km: Double)

/**
 * Onde um degrau acaba e o outro começa, em km por dia (decisão nº 133): fixos, e não
 * relativos aos dados de quem olha, para que a mesma cor diga a mesma coisa para todos e a
 * legenda não mude de um mês para o outro. Quatro degraus de corrida e o dia vazio.
 */
val LIMITES_DO_HEATMAP_KM: List<Int> = listOf(5, 10, 15)

private const val DIAS_NA_SEMANA = 7

/** O fuso do plano da temporada, para a corrida cujo plano não foi lido (como o histórico). */
private val FUSO_DE_RESERVA: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * O degrau de um dia: 0 sem corrida, e de 1 a 4 conforme os km somados do dia
 * ([LIMITES_DO_HEATMAP_KM]). Dia com corrida nunca fica no degrau 0, por menor que seja.
 */
fun degrauDoDia(km: Double, corridas: Int): Int {
    if (corridas <= 0) return 0
    return 1 + LIMITES_DO_HEATMAP_KM.count { km >= it }
}

/**
 * A grade anual de docs/03 §3.17: o **ano civil** (decisão nº 133), em colunas de segunda
 * a domingo, atravessando planos (RN-16): conta toda corrida válida do usuário.
 *
 * O dia de cada corrida é o do fuso do plano dela (RN-28). Descartada (RN-31) e
 * substituída (RN-24) não entram. A grade existe sem nenhuma corrida: "a grade vazia é
 * legível e convida".
 */
fun gradeDoHeatmap(corridas: List<Corrida>, fusos: Map<String, ZoneId>, ano: Int): GradeDoHeatmap {
    val primeiro = LocalDate.of(ano, 1, 1)
    val ultimo = LocalDate.of(ano, 12, 31)

    val porDia = corridas
        .filter { !it.descartada && !it.substituida }
        .groupBy { it.dataHoraInicio.atZone(fusos[it.planoId] ?: FUSO_DE_RESERVA).toLocalDate() }

    val segunda = primeiro.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val colunas = ceil((ChronoUnit.DAYS.between(segunda, ultimo) + 1) / DIAS_NA_SEMANA.toDouble()).toInt()

    return GradeDoHeatmap(
        ano = ano,
        colunas = (0 until colunas).map { coluna ->
            (0 until DIAS_NA_SEMANA).map { linha ->
                val data = segunda.plusDays((coluna * DIAS_NA_SEMANA + linha).toLong())
                if (data.isBefore(primeiro) || data.isAfter(ultimo)) {
                    null
                } else {
                    val doDia = porDia[data].orEmpty()
                    DiaDoHeatmap(data, doDia.sumOf { it.km }, doDia.size)
                }
            }
        },
    )
}

/**
 * O dia que o toque resolve: a coluna é a semana, e a linha, o dia dela. A célula do dia é
 * menor que 48dp, então o toque cai no dia **válido mais próximo** da coluna (docs/03
 * §3.17). Coluna e linha fora da grade são cortadas nas bordas.
 */
fun diaDaColuna(grade: GradeDoHeatmap, coluna: Int, linha: Int): DiaDoHeatmap? {
    if (grade.colunas.isEmpty()) return null
    val dias = grade.colunas[coluna.coerceIn(0, grade.colunas.lastIndex)]
    val alvo = linha.coerceIn(0, DIAS_NA_SEMANA - 1)
    return dias.indices
        .filter { dias[it] != null }
        .minByOrNull { kotlin.math.abs(it - alvo) }
        ?.let { dias[it] }
}

/** Os doze meses do ano da grade, com dias corridos e quilômetros. */
fun resumoDosMeses(grade: GradeDoHeatmap): List<ResumoDoMes> {
    val porMes = grade.dias.groupBy { it.data.monthValue }
    return (1..12).map { mes ->
        val dias = porMes[mes].orEmpty()
        ResumoDoMes(mes, dias.count { it.corridas > 0 }, dias.sumOf { it.km })
    }
}

private val EspacoEntreLinhas = 8.dp
private val LadoDaLegenda = 16.dp
private const val FRACAO_DA_CELULA = 0.8f

/**
 * O heatmap de calendário (`F3-T02`, docs/03 §3.17), a grade anual no topo do painel.
 *
 * - **Cor:** dia sem corrida em `superficie-afundada`, dia corrido em quatro degraus até
 *   `destaque` (`intensidade-1` a `3` no portador, docs/02 §2.6), célula com `raio-dado`.
 * - **Legenda obrigatória** `menos … mais`, com o intervalo de km de cada degrau.
 * - **Leitura por toque:** `14 de setembro · 12,4 km · 1 corrida`. O alvo é a coluna da
 *   semana e o toque resolve para o dia mais próximo; arrastar o dedo varre as semanas.
 * - **O desenho não fala:** a semântica é o resumo do ano e dos meses, em texto
 *   (docs/02 §8, item 5).
 *
 * @param hoje o dia corrente: dia depois dele não tem leitura de corrida.
 */
@Composable
fun BlocoDeHeatmap(grade: GradeDoHeatmap, hoje: LocalDate, modifier: Modifier = Modifier) {
    val locale = LocaleDoApp
    var selecionado by rememberSaveable { mutableStateOf<String?>(null) }
    val dataSelecionada = selecionado?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    val titulo = stringResource(R.string.painel_heatmap_titulo, grade.ano)
    val descricao = descricaoFalada(grade, locale, titulo)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titulo.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))

        GradeDesenhada(
            grade = grade,
            hoje = hoje,
            selecionada = dataSelecionada,
            aoSelecionar = { selecionado = it.toString() },
            descricao = descricao,
        )

        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = leituraDoDia(grade, dataSelecionada, hoje, locale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (grade.diasComCorrida == 0) {
            Spacer(Modifier.height(EspacoEntreLinhas))
            Text(
                text = stringResource(R.string.painel_heatmap_vazio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(EspacoEntreLinhas))
        Legenda()
    }
}

@Composable
private fun GradeDesenhada(
    grade: GradeDoHeatmap,
    hoje: LocalDate,
    selecionada: LocalDate?,
    aoSelecionar: (LocalDate) -> Unit,
    descricao: String,
) {
    val cores = LocalCoresPokerun.current
    val degraus = listOf(
        cores.superficieAfundada,
        cores.intensidade1,
        cores.intensidade2,
        cores.intensidade3,
        MaterialTheme.colorScheme.primary,
    )
    val contorno = MaterialTheme.colorScheme.onSurface
    val tintaFraca = MaterialTheme.colorScheme.onSurfaceVariant
    val medidor = rememberTextMeasurer()
    val estiloDoMes = MaterialTheme.typography.labelSmall.copy(color = tintaFraca)
    val densidade = LocalDensity.current
    val locale = LocaleDoApp

    val faixaDosMeses = with(densidade) { medidor.measure("jan", estiloDoMes).size.height.toDp() } + EspacoEntreLinhas / 2

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val colunas = grade.colunas.size
        val passo = maxWidth / colunas
        val altura = faixaDosMeses + passo * DIAS_NA_SEMANA

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(altura)
                .clearAndSetSemantics { contentDescription = descricao }
                .pointerInput(grade) {
                    val lado = size.width.toFloat() / colunas
                    detectTapGestures { toque -> resolver(toque, grade, lado, faixaDosMeses.toPx(), aoSelecionar) }
                }
                .pointerInput(grade) {
                    val lado = size.width.toFloat() / colunas
                    detectHorizontalDragGestures(
                        onDragStart = { toque -> resolver(toque, grade, lado, faixaDosMeses.toPx(), aoSelecionar) },
                        onHorizontalDrag = { mudanca, _ ->
                            resolver(mudanca.position, grade, lado, faixaDosMeses.toPx(), aoSelecionar)
                        },
                    )
                },
        ) {
            val lado = size.width / colunas
            val celula = lado * FRACAO_DA_CELULA
            val folga = (lado - celula) / 2
            val topo = faixaDosMeses.toPx()
            val raio = CornerRadius(Raios.dado.toPx())

            var fimDoUltimoMes = 0f
            grade.colunas.forEachIndexed { indice, dias ->
                val x = lado * indice
                dias.forEachIndexed { linha, dia ->
                    if (dia == null) return@forEachIndexed
                    val origem = Offset(x + folga, topo + lado * linha + folga)
                    val futuro = dia.data.isAfter(hoje)
                    val cor: Color = if (futuro) degraus[0] else degraus[dia.degrau]
                    drawRoundRect(cor, origem, Size(celula, celula), raio)
                    if (dia.data == selecionada) {
                        drawRoundRect(
                            contorno,
                            origem - Offset(folga, folga),
                            Size(celula + folga * 2, celula + folga * 2),
                            raio,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
                // O rótulo do mês fica na coluna do dia 1, e só se não pisar no anterior.
                val primeiroDoMes = dias.filterNotNull().firstOrNull { it.data.dayOfMonth == 1 }
                if (primeiroDoMes != null) {
                    val texto = medidor.measure(nomeDoMes(primeiroDoMes.data, locale).take(3), estiloDoMes)
                    if (x >= fimDoUltimoMes) {
                        drawText(texto, topLeft = Offset(x, 0f))
                        fimDoUltimoMes = x + texto.size.width + 4.dp.toPx()
                    }
                }
            }
        }
    }
}

/** Do toque em pixels para o dia: a coluna pelo x, a linha pelo y abaixo dos meses. */
private fun resolver(
    toque: Offset,
    grade: GradeDoHeatmap,
    lado: Float,
    topo: Float,
    aoSelecionar: (LocalDate) -> Unit,
) {
    val coluna = floor(toque.x / lado).toInt()
    val linha = floor((toque.y - topo) / lado).toInt()
    diaDaColuna(grade, coluna, linha)?.let { aoSelecionar(it.data) }
}

@Composable
private fun Legenda() {
    val cores = LocalCoresPokerun.current
    val degraus = listOf(
        cores.superficieAfundada,
        cores.intensidade1,
        cores.intensidade2,
        cores.intensidade3,
        MaterialTheme.colorScheme.primary,
    )
    val faixas = faixasDaLegenda()
    val descricao = faixas.joinToString("; ")

    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = descricao }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.painel_heatmap_menos),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.painel_heatmap_unidade),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.painel_heatmap_mais),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Spacer(Modifier.height(EspacoEntreLinhas / 2))
        // Acima de `ESCALA_QUE_EMPILHA` o rótulo não cabe na quinta parte da largura e
        // quebraria no meio da palavra: a legenda desce para uma linha por degrau.
        if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
            Column(verticalArrangement = Arrangement.spacedBy(EspacoEntreLinhas / 2)) {
                degraus.forEachIndexed { indice, cor ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(LadoDaLegenda).background(cor, RoundedCornerShape(Raios.dado)))
                        Spacer(Modifier.width(EspacoEntreLinhas))
                        Text(
                            text = faixas[indice],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                degraus.forEachIndexed { indice, cor ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Spacer(Modifier.size(LadoDaLegenda).background(cor, RoundedCornerShape(Raios.dado)))
                        Text(
                            text = faixas[indice],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** O intervalo de km de cada degrau, do vazio ao mais forte. */
@Composable
private fun faixasDaLegenda(): List<String> {
    val l = LIMITES_DO_HEATMAP_KM
    return listOf(
        stringResource(R.string.painel_heatmap_faixa_zero),
        stringResource(R.string.painel_heatmap_faixa_ate, l[0]),
        stringResource(R.string.painel_heatmap_faixa_entre, l[0], l[1]),
        stringResource(R.string.painel_heatmap_faixa_entre, l[1], l[2]),
        stringResource(R.string.painel_heatmap_faixa_acima, l[2]),
    )
}

@Composable
private fun leituraDoDia(grade: GradeDoHeatmap, data: LocalDate?, hoje: LocalDate, locale: Locale): String {
    if (data == null) return stringResource(R.string.painel_heatmap_toque)
    val dia = grade.dias.firstOrNull { it.data == data } ?: return stringResource(R.string.painel_heatmap_toque)
    val quando = stringResource(R.string.painel_heatmap_data, dia.data.dayOfMonth, nomeDoMes(dia.data, locale))
    return when {
        dia.data.isAfter(hoje) -> quando
        dia.corridas == 0 -> stringResource(R.string.painel_heatmap_leitura_sem_corrida, quando)
        else -> stringResource(
            R.string.painel_heatmap_leitura,
            quando,
            formatarKm(dia.km, locale),
            pluralStringResource(R.plurals.painel_heatmap_corridas, dia.corridas, dia.corridas),
        )
    }
}

@Composable
private fun descricaoFalada(grade: GradeDoHeatmap, locale: Locale, titulo: String): String {
    // `map` é inline: o `stringResource` cabe dentro dele, e dentro do `joinToString` não.
    val meses = resumoDosMeses(grade).filter { it.diasComCorrida > 0 }.map { mes ->
        stringResource(
            R.string.painel_heatmap_mes_falado,
            nomeDoMes(LocalDate.of(grade.ano, mes.mes, 1), locale),
            pluralStringResource(R.plurals.painel_heatmap_dias, mes.diasComCorrida, mes.diasComCorrida),
            formatarKm(mes.km, locale),
        )
    }.joinToString("; ")
    val resumo = stringResource(
        R.string.painel_heatmap_resumo_falado,
        titulo,
        pluralStringResource(R.plurals.painel_heatmap_dias, grade.diasComCorrida, grade.diasComCorrida),
        formatarKm(grade.kmNoAno, locale),
    )
    return if (meses.isEmpty()) resumo else "$resumo $meses."
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val HojeDoPreview: LocalDate = LocalDate.of(2026, 10, 15)

private fun gradeDeExemplo(): GradeDoHeatmap {
    val vazia = gradeDoHeatmap(emptyList(), emptyMap(), 2026)
    val kmPorDia = listOf(3.2, 6.0, 0.0, 12.4, 0.0, 17.5, 8.1)
    return vazia.copy(
        colunas = vazia.colunas.mapIndexed { coluna, dias ->
            dias.mapIndexed { linha, dia ->
                val km = if (dia != null && !dia.data.isAfter(HojeDoPreview) && (coluna + linha) % 3 != 0) {
                    kmPorDia[(coluna * 3 + linha) % kmPorDia.size]
                } else {
                    0.0
                }
                dia?.copy(km = km, corridas = if (km > 0) 1 else 0)
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HeatmapCheioPreview() {
    PokerunTheme { BlocoDeHeatmap(gradeDeExemplo(), HojeDoPreview) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HeatmapVazioPreview() {
    PokerunTheme { BlocoDeHeatmap(gradeDoHeatmap(emptyList(), emptyMap(), 2026), HojeDoPreview) }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 2.0f)
@Composable
private fun HeatmapFonteGrandePreview() {
    PokerunTheme { BlocoDeHeatmap(gradeDeExemplo(), HojeDoPreview) }
}
