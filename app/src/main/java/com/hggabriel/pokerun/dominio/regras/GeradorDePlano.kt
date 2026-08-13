package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Gera a grade de semanas de um plano (docs/01 §3.2, D-02).
 *
 * Quatro parâmetros do formulário entram, uma lista de [Semana] sai. É função pura:
 * não conhece Android, não conhece Firestore e não lê relógio — o instante de
 * início chega por parâmetro para que o teste possa fixá-lo.
 *
 * **A grade é gerada uma vez e depois editada à mão** (D-02). Editar o `longao_km`
 * de uma semana recalcula o `km_alvo` dela pela mesma fórmula daqui (docs/01 §3.3),
 * e é por isso que [ParametrosDeGeracao] fica guardado no plano: depois da primeira
 * edição, a grade deixa de ser dedutível dos campos do plano.
 *
 * ### `dataFim` é exclusivo
 *
 * A especificação não define a semântica de `data_fim` (docs/05 §1 lista o campo e
 * mais nada). Aqui ele é **o instante em que a semana acaba**, ou seja, a
 * meia-noite do dia seguinte ao último dia dela: a semana 21 de 28 a 31/12 termina
 * em 01/01. É a única representação em que *"a semana contém o instante t"* é
 * `dataInicio <= t < dataFim` — total, sem buraco e sem sobreposição, inclusive na
 * virada do horário de verão. A alternativa, guardar o último instante do último
 * dia, deixa um vão de menos de um microssegundo no fim de todo domingo, e um vão
 * ali é uma corrida sem semana. `F1-T03` depende desta escolha.
 */
object GeradorDePlano {

    /** O longão do pico vale 10% a mais que a distância da prova (docs/01 §3.2). */
    private const val FATOR_DO_PICO = 1.1

    /** A sessão curta é metade do longão da semana. */
    private const val FATOR_DA_CURTA = 0.5

    /** A 1ª semana de taper corta o longão a 60%. */
    private const val FATOR_DO_TAPER = 0.6

    /** A 2ª semana de taper vale 40% do volume pico, e não tem longão. */
    private const val FATOR_DO_SEGUNDO_TAPER = 0.4

    /**
     * O menor plano que a grade comporta. `docs/06 §3` e a ficha de `F1-T02`
     * chamam 8 de "N mínimo", e abaixo disso a estrutura deixa de fazer sentido:
     * com `N = 4` sobra uma semana de build, e o longão dela teria de ser a
     * baseline e o pico ao mesmo tempo. Falhar alto é melhor que devolver uma
     * grade plausível e sem sentido — a tela que impede o usuário de chegar aqui
     * é `F1-T10`.
     */
    private const val SEMANAS_MINIMAS = 8

    /**
     * // RN-26
     *
     * @param inicio qualquer instante da semana em que o plano começa. A grade
     *   arranca na **segunda-feira daquela semana** (RN-01), não no dia do cadastro.
     * @param fuso o fuso do plano, nunca o do aparelho (RN-28). É ele que decide em
     *   que dia cai cada instante, e trocá-lo por `systemDefault()` move a fronteira
     *   das semanas para quem viajar.
     */
    fun gerar(
        parametros: ParametrosDeGeracao,
        inicio: Instant,
        fuso: ZoneId,
    ): List<Semana> {
        require(parametros.sessoesPorSemana in 2..4) {
            "sessões por semana é 2, 3 ou 4 (docs/01 §3.1), veio ${parametros.sessoesPorSemana}"
        }

        val primeiraSegunda = primeiraSegundaDe(inicio.atZone(fuso).toLocalDate())
        val diaDaProva = parametros.dataProva.atZone(fuso).toLocalDate()

        val total = contarSemanas(primeiraSegunda, diaDaProva)
        require(total >= SEMANAS_MINIMAS) {
            "um plano precisa de pelo menos $SEMANAS_MINIMAS semanas, a prova cabe em $total"
        }

        val ultimaDeBuild = total - 3
        val longaoDoPico = FATOR_DO_PICO * parametros.distanciaAlvoKm
        val volumeDoPico = volume(longaoDoPico, parametros.sessoesPorSemana)

        return (1..total).map { numero ->
            val segunda = primeiraSegunda.plusWeeks((numero - 1).toLong())
            val ultimoDia = if (numero == total) diaDaProva else segunda.plusDays(6)

            val longao: Double? = when {
                numero <= ultimaDeBuild ->
                    interpolar(parametros.baselineKm, longaoDoPico, numero, ultimaDeBuild)
                numero == total - 2 -> FATOR_DO_TAPER * longaoDoPico
                // A 2ª de taper e a semana da prova não planejam longão.
                else -> null
            }

            Semana(
                numero = numero,
                dataInicio = segunda.atStartOfDay(fuso).toInstant(),
                dataFim = ultimoDia.plusDays(1).atStartOfDay(fuso).toInstant(),
                sessoesAlvo = if (numero == total) 1 else parametros.sessoesPorSemana,
                kmAlvo = when {
                    numero == total -> parametros.distanciaAlvoKm
                    numero == total - 1 -> FATOR_DO_SEGUNDO_TAPER * volumeDoPico
                    else -> volume(longao!!, parametros.sessoesPorSemana)
                },
                longaoKm = longao,
                tipo = when {
                    numero == total -> TipoDeSemana.PROVA
                    numero > ultimaDeBuild -> TipoDeSemana.TAPER
                    else -> TipoDeSemana.BUILD
                },
                // RN-26: parcial é ter menos de sete dias, e não "ser a semana da
                // prova". Quando a prova cai num domingo, a última semana fecha
                // inteira. O comentário de docs/05 §1 descreve o caso da São
                // Silvestre, que é quinta-feira; a regra é a de RN-26.
                parcial = ChronoUnit.DAYS.between(segunda, ultimoDia) < 6,
            )
        }
    }

    /**
     * // RN-01
     *
     * A segunda-feira da semana de [dia]. **A grade arranca aqui, e não no dia do
     * cadastro** — a semana de treino é estritamente de segunda a domingo.
     *
     * Público desde `F1-T10`: a `CreatePlanScreen` valida o mínimo de 8 semanas antes
     * de deixar o usuário seguir, e ela precisa contar a partir do mesmo ponto que a
     * geração. Duas leituras diferentes da palavra "início" fariam a tela recusar num
     * domingo um plano que o gerador aceita — seis dias de diferença, quase uma semana
     * inteira do denominador.
     */
    fun primeiraSegundaDe(dia: LocalDate): LocalDate =
        dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * `N` = semanas segunda-a-domingo da primeira segunda até o dia da prova,
     * **inclusive** (docs/01 §3.2). A última pode ter menos de sete dias.
     *
     * Público pelo mesmo motivo de [primeiraSegundaDe]: é esta conta que decide se o
     * plano tem as 8 semanas mínimas, e ela precisa ser a mesma nos dois lados.
     */
    fun contarSemanas(primeiraSegunda: LocalDate, diaDaProva: LocalDate): Int {
        val dias = ChronoUnit.DAYS.between(primeiraSegunda, diaDaProva)
        return (dias / 7).toInt() + 1
    }

    /**
     * Interpolação **linear** de [de] até [ate], com [ate] caindo exatamente em
     * [ultimoPasso].
     *
     * Linear e nunca composta: a spec calcula que 18 semanas a 10% compostos
     * multiplicariam o volume por 5,5×. Uma curva composta acerta as duas pontas e
     * erra o meio inteiro, que é a forma mais difícil de perceber a olho.
     */
    private fun interpolar(de: Double, ate: Double, passo: Int, ultimoPasso: Int): Double =
        de + (ate - de) * (passo - 1) / (ultimoPasso - 1)

    /**
     * O volume deriva do longão, nunca o contrário (docs/01 §3.2): `1,5 ×` com 2
     * sessões, `2,0 ×` com 3 e `2,5 ×` com 4 — este último idêntico ao antigo
     * `longão ÷ 0,4`.
     */
    private fun volume(longao: Double, sessoes: Int): Double =
        longao + (sessoes - 1) * FATOR_DA_CURTA * longao
}
