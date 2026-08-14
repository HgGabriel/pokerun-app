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
     * // RN-05
     *
     * Uma semana encerrada é congelada: edições no plano afetam apenas semanas
     * futuras (RN-06). Encerrada é ter chegado ao fim, e [Semana.dataFim] é
     * **exclusivo** — ele é a meia-noite que já pertence à semana seguinte —, então
     * a comparação é `>=` e não sobra buraco nem sobreposição entre as semanas.
     *
     * **Isto é derivado, e não um campo.** O schema chegou a ter `congelada:
     * Boolean`, e ninguém era dono da escrita: enquanto o dono não abrisse o app,
     * uma semana passada continuaria destravada no banco e o cadeado da
     * `WeekDetailScreen` mentiria. Derivar tira a pergunta "quem grava?" do caminho
     * e faz RN-05 valer mesmo num plano que ninguém abre há um mês. A rule de
     * `weeks/{n}` faz a mesma conta com `request.time`.
     *
     * **O fuso não aparece aqui, e é de propósito.** [Semana.dataFim] já foi gerado
     * no fuso do plano por `GeradorDePlano`, então comparar dois instantes devolve
     * a mesma resposta seja quem for que pergunte, de onde for (RN-28). Converter
     * de novo aqui reabriria a porta que `Plano.fuso` fechou.
     */
    fun congelada(semana: Semana, agora: Instant): Boolean = agora >= semana.dataFim

    /**
     * // RN-27
     *
     * Um plano encerrado, pelos **dois** caminhos: o dono encerrando à mão
     * ([Plano.encerrado]) e o fim da semana da prova, que não passa por ninguém.
     *
     * **A fronteira sai de `plans/{id}` sozinho**, sem a subcoleção: é a meia-noite do
     * dia seguinte ao da prova, no fuso do plano. Isso existe porque a
     * `PlansListScreen` não lê as semanas de N planos — ler as 21 semanas de cada um
     * para saber a situação de uma linha custaria uma consulta por plano numa tela que
     * hoje gasta N leituras diretas (docs/05 §2.7).
     *
     * **É a mesma fronteira que [congelada] daria na última semana**, e não por acaso:
     * `GeradorDePlano` fecha a semana da prova exatamente no dia da prova mais um
     * (RN-26), então `agora >= grade.last().dataFim` e a conta daqui coincidem por
     * construção. A Home e a `PlanDetailScreen` continuam usando a grade, que elas já
     * têm em mãos, e o teste de equivalência de `F1-T12b` é o que mantém as duas contas
     * honestas: se `data_fim` mudar de semântica, ele quebra antes de a lista dizer
     * `ATIVO` num plano que a Home dá por encerrado.
     *
     * **Nada aqui grava `encerrado = true`.** O campo continua sendo do dono; o
     * encerramento por data é derivado, pelo mesmo motivo de [congelada] — um booleano
     * que ninguém é dono de escrever mente enquanto o app não abre.
     */
    fun planoEncerrado(plano: Plano, agora: Instant): Boolean =
        plano.encerrado || agora >= fimDoPlano(plano)

    /**
     * A meia-noite que já pertence ao dia seguinte ao da prova, no fuso do plano
     * (RN-28). Exclusiva, como [Semana.dataFim]: o dia da prova inteiro é do plano, e
     * quem corre de manhã ainda registra a corrida à noite.
     */
    private fun fimDoPlano(plano: Plano): Instant =
        plano.dataProva
            .atZone(plano.fuso)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(plano.fuso)
            .toInstant()

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
