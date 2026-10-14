package com.hggabriel.pokerun.ui.telas.detalhecorrida

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarPace
import java.time.LocalDate
import java.util.Locale

/** Os estados da `RunDetailScreen` (docs/03 §3.13). */
sealed interface DetalheCorridaUiState {

    data object Carregando : DetalheCorridaUiState

    data class Conteudo(
        val corridaId: String,
        /** O dia da corrida **no fuso do plano** (RN-28). */
        val dia: LocalDate,
        /** O número grande, acima do bloco (docs/03 §3.13.1). */
        val distancia: String,
        val linhas: List<LinhaDaFicha>,
        val nomeDoPlano: String,
        /** Nulo é corrida fora do intervalo do plano (RN-03). */
        val semanaRef: Int?,
        val xpCreditado: Long,
        /** A conta aberta. Nula enquanto o replay não gravou `xp_detalhe`. */
        val conta: ParcelasDeXp?,
        /** Descartada (RN-31) ou substituída (RN-24): fora dos cálculos. */
        val foraDosCalculos: Boolean,
    ) : DetalheCorridaUiState

    data object Falhou : DetalheCorridaUiState
}

/** As linhas do bloco de ficha de docs/03 §3.13.1, na ordem dele. */
enum class TipoDeLinha { DISTANCIA, DURACAO, PACE, FC_MEDIA, ESFORCO }

data class LinhaDaFicha(val tipo: TipoDeLinha, val valor: String)

/**
 * O detalhe de uma corrida, ou [DetalheCorridaUiState.Falhou] se ela ou o plano dela
 * não existem.
 *
 * **Campo ausente não vira `—`: a linha some** (docs/03 §3.13.1). O pace é derivado,
 * nunca armazenado (docs/05 §4.2), e some junto com a distância zerada. A FC média e o
 * esforço só aparecem quando foram digitados.
 */
fun detalheDaCorrida(corrida: Corrida?, plano: Plano?, locale: Locale = LocaleDoApp): DetalheCorridaUiState {
    if (corrida == null || plano == null) return DetalheCorridaUiState.Falhou

    val distancia = String.format(locale, "%.2f", corrida.km)
    val linhas = buildList {
        add(LinhaDaFicha(TipoDeLinha.DISTANCIA, distancia))
        add(LinhaDaFicha(TipoDeLinha.DURACAO, formatarDuracao(corrida.duracaoSeg, locale)))
        formatarPace(corrida.km, corrida.duracaoSeg, locale)?.let { add(LinhaDaFicha(TipoDeLinha.PACE, it)) }
        corrida.fcMedia?.let { add(LinhaDaFicha(TipoDeLinha.FC_MEDIA, it.toString())) }
        corrida.esforcoPercebido?.let { add(LinhaDaFicha(TipoDeLinha.ESFORCO, it.toString())) }
    }

    return DetalheCorridaUiState.Conteudo(
        corridaId = corrida.id,
        dia = corrida.dataHoraInicio.atZone(plano.fuso).toLocalDate(),
        distancia = distancia,
        linhas = linhas,
        nomeDoPlano = plano.nome,
        semanaRef = corrida.semanaRef,
        xpCreditado = corrida.xpCreditado,
        conta = corrida.xpDetalhe,
        foraDosCalculos = corrida.descartada || corrida.substituida,
    )
}
