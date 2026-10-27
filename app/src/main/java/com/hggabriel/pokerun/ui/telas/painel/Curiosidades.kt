package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private val FusoDeReserva: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * As faixas do dia do Você sabia. Quatro delas têm a marca de horário dos jogos (decisão 20):
 * `dawn-mark` antes das 9h, `lunchtime-mark` das 11h às 14h, `dusk-mark` das 17h às 19h e
 * `sleepy-time-mark` depois das 20h. As outras três não têm marca.
 */
enum class FaixaDoDia(val de: Int, val ate: Int, val marca: String?) {
    CEDO(0, 9, "dawn-mark"),
    MANHA(9, 11, null),
    ALMOCO(11, 14, "lunchtime-mark"),
    TARDE(14, 17, null),
    ENTARDECER(17, 19, "dusk-mark"),
    NOITE(19, 20, null),
    TARDE_DA_NOITE(20, 24, "sleepy-time-mark"),
    ;

    companion object {
        fun daHora(hora: Int): FaixaDoDia = entries.first { hora >= it.de && hora < it.ate }
    }
}

/** Um fato do Você sabia. A tela escolhe o número grande e a frase pela forma. */
sealed interface Curiosidade {
    /** `77%` dos seus km vieram de uma corrida só, os 10 km de 11 de outubro. */
    data class ParteDaMaior(val porcento: Int, val km: Double, val dia: LocalDate) : Curiosidade

    /** `2 de 3` das suas corridas foram aos domingos. */
    data class DiaPreferido(val dia: DayOfWeek, val corridas: Int, val total: Int) : Curiosidade

    /** `2 de 3` das suas corridas foram à noite. */
    data class HorarioPreferido(val faixa: FaixaDoDia, val corridas: Int, val total: Int) : Curiosidade

    /** `1.240 XP` foi o que rendeu a corrida de 10 km de 11 de outubro, a que mais deu. */
    data class MaiorXp(val xp: Long, val km: Double, val dia: LocalDate) : Curiosidade

    /** `22%` do seu XP veio do multiplicador do plano. */
    data class ParteDoMultiplicador(val porcento: Int) : Curiosidade
}

/**
 * Os fatos do Você sabia, calculados de `runs` e `xp_detalhe` (estudo, §c e §e). Tocar o
 * cartão troca o fato, e a troca é a recompensa.
 *
 * **Só entra fato que diz algo.** Com uma corrida, só o XP dela; a parte da maior corrida
 * pede duas; dia e horário pedem que o preferido tenha ao menos duas corridas; o multiplicador
 * só aparece quando rendeu alguma coisa. Nenhum fato é frase de "falta" (decisão 2).
 */
fun curiosidades(corridas: List<Corrida>, fusos: Map<String, ZoneId>): List<Curiosidade> {
    val validas = corridas.filter { it.valida() && it.km > 0 }
    if (validas.isEmpty()) return emptyList()
    val quando = { c: Corrida -> c.dataHoraInicio.atZone(fusos[c.planoId] ?: FusoDeReserva) }
    val total = validas.size

    return buildList {
        if (total >= 2) {
            val maior = validas.maxBy { it.km }
            val km = validas.sumOf { it.km }
            add(Curiosidade.ParteDaMaior((maior.km / km * 100).roundToInt(), maior.km, quando(maior).toLocalDate()))
        }

        validas.groupBy { quando(it).dayOfWeek }.maxByOrNull { it.value.size }
            ?.takeIf { it.value.size >= 2 }
            ?.let { (dia, delas) -> add(Curiosidade.DiaPreferido(dia, delas.size, total)) }

        validas.groupBy { FaixaDoDia.daHora(quando(it).hour) }.maxByOrNull { it.value.size }
            ?.takeIf { it.value.size >= 2 }
            ?.let { (faixa, delas) -> add(Curiosidade.HorarioPreferido(faixa, delas.size, total)) }

        validas.filter { it.xpCreditado > 0 }.maxByOrNull { it.xpCreditado }
            ?.let { add(Curiosidade.MaiorXp(it.xpCreditado, it.km, quando(it).toLocalDate())) }

        // A conta é `(base + longão + pace) × multiplicador + recorde` (docs/04 §3): o que
        // veio do multiplicador é a parte acima de 1.
        val comDetalhe = validas.mapNotNull { it.xpDetalhe }
        val xp = comDetalhe.sumOf { (it.base + it.longao + it.pace) * it.multiplicador + it.recorde }
        val doMultiplicador = comDetalhe.sumOf { (it.base + it.longao + it.pace) * (it.multiplicador - 1).coerceAtLeast(0.0) }
        if (xp > 0 && doMultiplicador > 0) {
            add(Curiosidade.ParteDoMultiplicador((doMultiplicador / xp * 100).roundToInt()))
        }
    }
}
