package com.hggabriel.pokerun.ui.telas.editarcorrida

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.regras.OpcaoDeSessao
import com.hggabriel.pokerun.dominio.regras.ResumoDaCorrecao
import com.hggabriel.pokerun.dominio.regras.ValoresCorrigidos
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Os estados de docs/03 §3.14: `Editando`, `Recalculando`, `Concluido(resumo)` e erro. */
sealed interface EditarCorridaUiState {

    data object Carregando : EditarCorridaUiState

    data class Editando(
        /** Os campos são os do registro manual, e a validação também (`validarCorrida`). */
        val formulario: CorridaManualUiState,
        val dia: LocalDate,
        /** O seletor de RN-34. Vazio em corrida fora do intervalo do plano (RN-03). */
        val opcoes: List<OpcaoDeSessao>,
        val numeroDaSemana: Int?,
        val sessaoEscolhida: SessaoReivindicada?,
        val confirmandoDescarte: Boolean = false,
        @param:StringRes val erroDeTela: Int? = null,
    ) : EditarCorridaUiState

    data object Recalculando : EditarCorridaUiState

    data class Concluido(
        val descartada: Boolean,
        val dia: LocalDate,
        val mudancas: List<Mudanca>,
        val resumo: ResumoDaCorrecao,
        /** O número da Pokédex em cada posição do resumo. Nulo sem a temporada. */
        val especieAntes: Int? = null,
        val especieDepois: Int? = null,
    ) : EditarCorridaUiState

    data object Falhou : EditarCorridaUiState
}

/** Uma linha "antes → depois" do resumo de docs/01 §5.3. */
data class Mudanca(@param:StringRes val rotulo: Int, val antes: String, val depois: String)

/**
 * O formulário preenchido com a corrida, no fuso do plano (RN-28). Os textos saem na
 * forma que `validarCorrida` lê de volta: vírgula decimal, e campo vazio onde o valor é
 * zero ou não existe.
 */
fun formularioDe(corrida: Corrida, fuso: ZoneId, locale: Locale = LocaleDoApp): CorridaManualUiState {
    val quando = corrida.dataHoraInicio.atZone(fuso)
    val horas = corrida.duracaoSeg / 3600
    val minutos = (corrida.duracaoSeg % 3600) / 60
    val segundos = corrida.duracaoSeg % 60

    return CorridaManualUiState(
        data = quando.toLocalDate(),
        hora = LocalTime.of(quando.hour, quando.minute),
        km = textoDaDistancia(corrida.km, locale),
        horas = if (horas > 0) horas.toString() else "",
        minutos = minutos.toString(),
        segundos = if (segundos > 0) segundos.toString() else "",
        fcMedia = corrida.fcMedia?.toString().orEmpty(),
        esforco = corrida.esforcoPercebido?.toString().orEmpty(),
        tempo1kMinutos = corrida.tempo1kSeg?.let { (it / 60).toString() }.orEmpty(),
        tempo1kSegundos = corrida.tempo1kSeg?.let { (it % 60).takeIf { s -> s > 0 }?.toString() }.orEmpty(),
        tempo5kMinutos = corrida.tempo5kSeg?.let { (it / 60).toString() }.orEmpty(),
        tempo5kSegundos = corrida.tempo5kSeg?.let { (it % 60).takeIf { s -> s > 0 }?.toString() }.orEmpty(),
    )
}

/** Se algum campo de medição mudou. Só a sessão mudando não pede registro novo (RN-24). */
fun medicaoMudou(original: Corrida, valores: ValoresCorrigidos): Boolean =
    original.dataHoraInicio != valores.dataHoraInicio ||
        kotlin.math.abs(original.km - valores.km) > 1e-9 ||
        original.duracaoSeg != valores.duracaoSeg ||
        original.fcMedia != valores.fcMedia ||
        original.esforcoPercebido != valores.esforcoPercebido ||
        original.tempo1kSeg != valores.tempo1kSeg ||
        original.tempo5kSeg != valores.tempo5kSeg

/** As linhas "antes → depois" do resumo: distância, duração e data, quando mudaram. */
fun mudancasDa(original: Corrida, valores: ValoresCorrigidos, fuso: ZoneId, locale: Locale = LocaleDoApp): List<Mudanca> =
    buildList {
        if (kotlin.math.abs(original.km - valores.km) > 1e-9) {
            add(Mudanca(R.string.editar_resumo_distancia, formatarKm(original.km, locale), formatarKm(valores.km, locale)))
        }
        if (original.duracaoSeg != valores.duracaoSeg) {
            add(
                Mudanca(
                    R.string.editar_resumo_duracao,
                    formatarDuracao(original.duracaoSeg, locale),
                    formatarDuracao(valores.duracaoSeg, locale),
                ),
            )
        }
        val diaAntes = original.dataHoraInicio.atZone(fuso).toLocalDate()
        val diaDepois = valores.dataHoraInicio.atZone(fuso).toLocalDate()
        if (diaAntes != diaDepois) {
            add(Mudanca(R.string.editar_resumo_data, diaCurto(diaAntes), diaCurto(diaDepois)))
        }
    }

private fun diaCurto(dia: LocalDate): String = "%02d/%02d".format(dia.dayOfMonth, dia.monthValue)

private fun textoDaDistancia(km: Double, locale: Locale): String =
    String.format(locale, "%.2f", km).trimEnd('0').trimEnd(',', '.')
