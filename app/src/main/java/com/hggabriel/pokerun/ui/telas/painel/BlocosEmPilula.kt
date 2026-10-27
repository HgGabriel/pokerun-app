package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.ColunaDePilula
import com.hggabriel.pokerun.ui.componentes.GraficoDePilulas
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import kotlin.math.abs

/*
 * Os três gráficos que ficaram (decisão 21), na forma da segunda linguagem: pílulas cheias
 * sobre trilho, sem eixo e sem grade, com o valor da semana atual num balão em tinta. Abaixo
 * do mínimo de dado, o gráfico mostra as pílulas que já existem, e a legenda diz quanto falta.
 */

/** No máximo doze colunas: o que cabe a 320dp sem a pílula virar risco. */
private const val COLUNAS_VISIVEIS = 12

/** Vagas desenhadas quando não há dado nenhum: o gráfico existe desde o primeiro dia. */
private const val VAGAS_SEM_DADO = 8

/** O piso da pílula de pace, para a semana mais lenta não sumir. */
private const val PISO_DO_PACE = 0.25f

@Composable
private fun NumeroGrande(valor: String, unidade: String?) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(valor, style = Estilos.numeroL, color = MaterialTheme.colorScheme.onSurface)
        if (unidade != null) {
            Spacer(Modifier.width(Espacos.minimo))
            Text(unidade, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun Legenda(texto: String) {
    Spacer(Modifier.height(Espacos.relacionado))
    Text(texto, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun vagas(cor: Color) = List(VAGAS_SEM_DADO) { ColunaDePilula(0f, cor) }

// ---------------------------------------------------------------------------
// Volume semanal acumulado (tela Selos)
// ---------------------------------------------------------------------------

@Composable
fun CartaoDeVolume(estado: VolumeAcumulado, modifier: Modifier = Modifier) {
    val destaque = MaterialTheme.colorScheme.primary
    val claro = destaque.copy(alpha = 0.35f)
    val tinta = MaterialTheme.colorScheme.onSurface
    val (pontos, marcadores) = when (estado) {
        is VolumeAcumulado.Serie -> estado.pontos to estado.marcadores
        is VolumeAcumulado.Faltam -> estado.parciais to emptySet()
    }
    val inicio = (pontos.size - COLUNAS_VISIVEIS).coerceAtLeast(0)
    val visiveis = pontos.drop(inicio)
    val maximo = visiveis.maxOfOrNull { it.kmAcumulado }?.takeIf { it > 0 } ?: 1.0
    val colunas = visiveis.mapIndexed { i, ponto ->
        val ultima = i == visiveis.lastIndex
        val prova = (inicio + i) in marcadores
        ColunaDePilula(
            fracao = (ponto.kmAcumulado / maximo).toFloat(),
            cor = when {
                prova -> tinta
                ultima -> destaque
                else -> claro
            },
            rotulo = "%02d/%02d".format(ponto.inicioDaSemana.dayOfMonth, ponto.inicioDaSemana.monthValue),
            balao = if (ultima) "${formatarKm(ponto.kmAcumulado)} km" else null,
            rotuloForte = ultima || prova,
        )
    }.ifEmpty { vagas(claro) }
    val total = pontos.lastOrNull()?.kmAcumulado ?: 0.0
    val titulo = stringResource(R.string.painel_volume_titulo)

    Cartao(modifier = modifier) {
        RotuloDoCartao(titulo)
        Spacer(Modifier.height(Espacos.relacionado))
        NumeroGrande(formatarKm(total), stringResource(R.string.painel_unidade_km))
        Spacer(Modifier.height(Espacos.interno))
        GraficoDePilulas(colunas, descricao = stringResource(R.string.ritmo_grafico_descricao, titulo, stringResource(R.string.selos_volume_total, formatarKm(total))))
        when {
            estado is VolumeAcumulado.Faltam ->
                Legenda(pluralStringResource(R.plurals.painel_volume_faltam, estado.semanas, estado.semanas))
            marcadores.isNotEmpty() ->
                Legenda(pluralStringResource(R.plurals.painel_volume_provas, marcadores.size, marcadores.size))
        }
    }
}

// ---------------------------------------------------------------------------
// Pace em média móvel de 4 semanas (tela Ritmo)
// ---------------------------------------------------------------------------

@Composable
fun CartaoDePace(estado: PaceMovel, modifier: Modifier = Modifier) {
    val destaque = MaterialTheme.colorScheme.primary
    val claro = destaque.copy(alpha = 0.35f)
    val pontos = when (estado) {
        is PaceMovel.Serie -> estado.pontos
        is PaceMovel.Faltam -> estado.parciais
    }.takeLast(COLUNAS_VISIVEIS)
    val rapido = pontos.minOfOrNull { it.paceSegKm }
    val lento = pontos.maxOfOrNull { it.paceSegKm }
    // Mais alta é mais rápida: a pílula enche ao contrário do pace.
    val colunas = pontos.mapIndexed { i, ponto ->
        val fracao = if (rapido == null || lento == null || lento == rapido) {
            1f
        } else {
            (PISO_DO_PACE + (1 - PISO_DO_PACE) * ((lento - ponto.paceSegKm) / (lento - rapido))).toFloat()
        }
        val ultima = i == pontos.lastIndex
        ColunaDePilula(
            fracao = fracao,
            cor = if (ultima) destaque else claro,
            rotulo = "${i + 1}",
            balao = if (ultima) paceCurto(ponto.paceSegKm) else null,
            rotuloForte = ultima,
        )
    }.ifEmpty { vagas(claro) }
    val titulo = stringResource(R.string.painel_pace_titulo)
    val atual = pontos.lastOrNull()?.paceSegKm
    val primeira = pontos.firstOrNull()?.paceSegKm

    Cartao(modifier = modifier) {
        RotuloDoCartao(titulo)
        if (atual != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            NumeroGrande(paceCurto(atual), stringResource(R.string.painel_unidade_pace))
            if (primeira != null && pontos.size > 1 && primeira != atual) {
                val diferenca = paceCurto(abs(primeira - atual))
                Text(
                    stringResource(if (atual < primeira) R.string.ritmo_mais_rapido else R.string.ritmo_mais_lento, diferenca),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(Espacos.interno))
        GraficoDePilulas(colunas, descricao = stringResource(R.string.ritmo_grafico_descricao, titulo, atual?.let(::paceCurto).orEmpty()))
        if (estado is PaceMovel.Faltam) Legenda(pluralStringResource(R.plurals.painel_pace_faltam, estado.corridas, estado.corridas))
    }
}

// ---------------------------------------------------------------------------
// Eficiência cardíaca (tela Ritmo)
// ---------------------------------------------------------------------------

@Composable
fun CartaoDeEficiencia(estado: EficienciaCardiaca, modifier: Modifier = Modifier) {
    // Cinza `texto-secundario`, como a spec de 03 §3.17 pede; a última em tinta.
    val cinza = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val tinta = MaterialTheme.colorScheme.onSurface
    val pontos = when (estado) {
        is EficienciaCardiaca.Serie -> estado.pontos
        is EficienciaCardiaca.Faltam -> estado.parciais
    }.takeLast(COLUNAS_VISIVEIS - 3)
    val maximo = pontos.maxOfOrNull { it.valor }?.takeIf { it > 0 } ?: 1.0
    val colunas = pontos.mapIndexed { i, ponto ->
        val ultima = i == pontos.lastIndex
        ColunaDePilula(
            fracao = (ponto.valor / maximo).toFloat(),
            cor = if (ultima) tinta else cinza,
            rotulo = umaCasa(ponto.valor),
            rotuloForte = ultima,
        )
    }.ifEmpty { vagas(cinza) }
    val titulo = stringResource(R.string.painel_eficiencia_titulo)
    val atual = pontos.lastOrNull()?.valor

    Cartao(modifier = modifier) {
        RotuloDoCartao(titulo)
        if (atual != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            NumeroGrande(umaCasa(atual), stringResource(R.string.painel_eficiencia_unidade))
        }
        Text(stringResource(R.string.ritmo_eficiencia_apoio), style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Espacos.interno))
        GraficoDePilulas(colunas, descricao = stringResource(R.string.ritmo_grafico_descricao, titulo, atual?.let(::umaCasa).orEmpty()))
        if (estado is EficienciaCardiaca.Faltam) {
            Legenda(pluralStringResource(R.plurals.painel_eficiencia_faltam, estado.corridas, estado.corridas))
        }
    }
}

/** `7:12`: minutos e segundos de um pace em segundos por km. */
internal fun paceCurto(segundos: Double): String {
    val total = Math.round(segundos)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun umaCasa(valor: Double): String = String.format(LocaleDoApp, "%.1f", valor)
