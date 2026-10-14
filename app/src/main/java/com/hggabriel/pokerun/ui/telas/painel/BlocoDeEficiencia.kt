package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.ui.componentes.FormaDoGrafico
import com.hggabriel.pokerun.ui.componentes.GraficoCanvas
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.PontoDoGrafico
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** O mínimo de dado do bloco (docs/03 §3.17): 8 corridas com FC. */
private const val CORRIDAS_COM_FC_MINIMAS = 8

private const val SEGUNDOS_POR_HORA = 3600.0

private val EspacoEntreLinhas = 8.dp

/** Um ponto da série: a corrida, quando foi e a FC média por km/h. */
data class PontoDeEficiencia(val id: String, val instante: Instant, val valor: Double)

/** O bloco tem dado bastante para a curva, ou ainda faltam corridas com FC. */
sealed interface EficienciaCardiaca {
    data class Faltam(val corridas: Int) : EficienciaCardiaca
    data class Serie(val pontos: List<PontoDeEficiencia>) : EficienciaCardiaca
}

/**
 * A eficiência cardíaca de docs/03 §3.17, que fica com a FC digitada (decisão nº 108):
 * FC média ÷ velocidade média, em bpm por km/h, uma corrida por ponto, em ordem de tempo.
 * Quanto menor, menos o coração trabalha para a mesma velocidade.
 *
 * Só entram corridas válidas com FC: descartada (RN-31) e substituída (RN-24) não. Abaixo
 * de [CORRIDAS_COM_FC_MINIMAS], o bloco diz quantas faltam.
 */
fun eficienciaCardiaca(corridas: List<Corrida>): EficienciaCardiaca {
    val comFc = corridas
        .filter { !it.descartada && !it.substituida && it.fcMedia != null && it.km > 0 && it.duracaoSeg > 0 }
        .sortedBy { it.dataHoraInicio }
    if (comFc.size < CORRIDAS_COM_FC_MINIMAS) return EficienciaCardiaca.Faltam(CORRIDAS_COM_FC_MINIMAS - comFc.size)

    return EficienciaCardiaca.Serie(
        comFc.map { corrida ->
            val kmPorHora = corrida.km / (corrida.duracaoSeg / SEGUNDOS_POR_HORA)
            PontoDeEficiencia(corrida.id, corrida.dataHoraInicio, corrida.fcMedia!! / kmPorHora)
        },
    )
}

/**
 * O bloco da curva cardíaca (`F3-T06`): a série em `sinal`, que é a cor de dado
 * secundário (docs/02 §2.1), ou a frase do mínimo. Opcional e cortável (docs/06 §2).
 *
 * @param fuso o fuso em que os dias do eixo são lidos (RN-28).
 */
@Composable
fun BlocoDeEficiencia(estado: EficienciaCardiaca, fuso: ZoneId, modifier: Modifier = Modifier) {
    val titulo = stringResource(R.string.painel_eficiencia_titulo)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titulo.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        when (estado) {
            is EficienciaCardiaca.Faltam -> Text(
                text = pluralStringResource(R.plurals.painel_eficiencia_faltam, estado.corridas, estado.corridas),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is EficienciaCardiaca.Serie -> GraficoCanvas(
                titulo = titulo,
                pontos = estado.pontos.map { ponto ->
                    val dia = ponto.instante.atZone(fuso).toLocalDate()
                    PontoDoGrafico(
                        rotulo = "%02d/%02d".format(dia.dayOfMonth, dia.monthValue),
                        valor = ponto.valor,
                        valorEscrito = String.format(LocaleDoApp, "%.1f", ponto.valor),
                    )
                },
                unidade = stringResource(R.string.painel_eficiencia_unidade),
                forma = FormaDoGrafico.LINHA,
                secundaria = true,
                escreverMarca = { String.format(LocaleDoApp, "%.0f", it) },
            )
        }
    }
}
