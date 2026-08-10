package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Responde em que semana do plano cai um instante (RN-01, RN-02, RN-28).
 *
 * ### Por que isto é uma função e não uma query
 *
 * RN-02 manda calcular `semana_ref` **na gravação** e nunca derivar em query. O
 * motivo é RN-28: a resposta depende do fuso do plano, e uma query não carrega
 * fuso. Guardado o snapshot, a corrida fica na semana em que ela aconteceu para
 * quem treina naquele plano — inclusive depois de o corredor mudar de país e
 * depois de o aparelho trocar de fuso.
 *
 * ### O erro que esta função existe para impedir
 *
 * Uma corrida de **domingo às 22h em UTC−3** é segunda-feira 01h em UTC. Quem
 * calcula em UTC, ou no fuso do aparelho, joga essa corrida para a semana seguinte
 * — e como `semana_ref` é snapshot, ela fica lá. O sintoma é uma aderência de
 * domingo que some e reaparece na semana errada, semanas depois, sem nada na tela
 * dizendo que o problema foi fuso.
 *
 * ### Como ela evita o erro
 *
 * Convertendo o instante para **data no fuso do plano** antes de qualquer conta, e
 * contando em dias de calendário a partir da primeira segunda-feira da grade.
 * Contar em horas seria o mesmo bug com outra roupa: a semana que contém a virada
 * do horário de verão tem 167 ou 169 horas, e sete dias.
 */
object CalendarioDoPlano {

    /**
     * // RN-28
     *
     * O `semana_ref` de [instante], ou **nulo** quando ele cai fora do intervalo do
     * plano (RN-03) — a corrida entra no histórico vitalício e não conta para a
     * aderência.
     *
     * O único campo de [plano] que importa aqui é o [Plano.fuso]; [grade] traz as
     * fronteiras que `GeradorDePlano` já calculou naquele mesmo fuso.
     */
    fun semanaRef(instante: Instant, plano: Plano, grade: List<Semana>): Int? {
        if (grade.isEmpty()) return null

        // A conversão acontece aqui, uma vez, e é a linha inteira da regra: é ela
        // que decide se domingo 22h é domingo.
        val dia = instante.atZone(plano.fuso).toLocalDate()
        val primeiraSegunda = grade.first().dataInicio.atZone(plano.fuso).toLocalDate()
        val diaDaProva = ultimoDia(plano, grade)

        if (dia < primeiraSegunda || dia > diaDaProva) return null

        // Dias de calendário, não horas: a semana do horário de verão tem sete dias
        // e 167 ou 169 horas.
        return (ChronoUnit.DAYS.between(primeiraSegunda, dia) / 7).toInt() + 1
    }

    /** A própria [Semana], para quem precisa do alvo e não só do número. */
    fun semanaDe(instante: Instant, plano: Plano, grade: List<Semana>): Semana? =
        semanaRef(instante, plano, grade)?.let { ref -> grade.firstOrNull { it.numero == ref } }

    /**
     * O último dia do plano é o dia da prova, e não o domingo da última semana: a
     * 21ª vai de 28 a 31/12 e 01/01 já está fora (RN-26).
     *
     * Sai da grade, e não de `plano.dataProva`, porque `dataFim` é exclusivo por
     * decisão de `F1-T02` — subtrair um dia dele é o que devolve o último dia
     * incluído, e mantém as duas funções amarradas à mesma fronteira.
     */
    private fun ultimoDia(plano: Plano, grade: List<Semana>): LocalDate =
        grade.last().dataFim.atZone(plano.fuso).toLocalDate().minusDays(1)
}
