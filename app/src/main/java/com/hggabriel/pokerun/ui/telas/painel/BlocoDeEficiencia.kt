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
    /** [parciais] são as corridas com FC que já existem (Revisão 3). */
    data class Faltam(val corridas: Int, val parciais: List<PontoDeEficiencia> = emptyList()) : EficienciaCardiaca
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
    val pontos = comFc.map { corrida ->
        val kmPorHora = corrida.km / (corrida.duracaoSeg / SEGUNDOS_POR_HORA)
        PontoDeEficiencia(corrida.id, corrida.dataHoraInicio, corrida.fcMedia!! / kmPorHora)
    }
    if (comFc.size < CORRIDAS_COM_FC_MINIMAS) return EficienciaCardiaca.Faltam(CORRIDAS_COM_FC_MINIMAS - comFc.size, pontos)

    return EficienciaCardiaca.Serie(pontos)
}
