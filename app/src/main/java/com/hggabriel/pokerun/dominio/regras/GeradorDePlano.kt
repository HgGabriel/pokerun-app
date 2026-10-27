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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Se um plano cabe até a data da prova (DA-10, docs/01 §3.2, "Quando o plano não cabe").
 *
 * [semanas] é o `N` do plano em todos os casos: a tela mostra o número antes de gerar.
 */
sealed interface Viabilidade {
    val semanas: Int

    /** O longão chega ao pico desejado, 10% acima da prova (com teto de 32 km). */
    data class Cabe(override val semanas: Int, val picoKm: Double, val ritmo: Double) : Viabilidade

    /**
     * O longão sobe 10% toda semana e não chega ao pico desejado, mas passa de 75% da
     * prova. A grade sai, e a revisão diz quanto falta.
     */
    data class PicoReduzido(
        override val semanas: Int,
        val picoKm: Double,
        val picoDesejadoKm: Double,
    ) : Viabilidade

    /**
     * Nem subindo 10% por semana o longão chega a 75% da prova. **A grade não sai**; a tela
     * diz o motivo e oferece as duas saídas, cada uma com um toque.
     *
     * @param picoKm o maior longão que cabe até a data escolhida.
     * @param primeiraProvaQueCabe a segunda-feira da primeira semana em que a prova cabe.
     *   Qualquer dia daquela semana serve.
     * @param maiorAlvoQueCabeKm a maior distância que cabe na data escolhida, em meio km.
     */
    data class NaoCabe(
        override val semanas: Int,
        val picoKm: Double,
        val picoMinimoKm: Double,
        val primeiraProvaQueCabe: LocalDate,
        val maiorAlvoQueCabeKm: Double,
    ) : Viabilidade

    /** Menos de 4 semanas: não há 1 de build, 2 de taper e a da prova. */
    data class CurtoDemais(override val semanas: Int) : Viabilidade
}

/**
 * Gera a grade de semanas de um plano (DA-10, docs/01 §3.2).
 *
 * **A regra em uma frase:** o longão cresce um pouco toda semana, nunca mais de 10% acima
 * do maior do último mês, até passar um pouco da distância da prova, com teto de 32 km; as
 * corridas curtas ficam na distância que o corredor escolhe, e as duas semanas antes da
 * prova aliviam o longão.
 *
 * É função pura: não conhece Android nem Firestore e não lê relógio — o instante de
 * criação chega por parâmetro.
 *
 * ### Uma função gera, edita e migra
 *
 * A grade é **a rampa, mais os pedidos do dono, mais as semanas fixas**. [recalcular] faz
 * essa conta, e [gerar] é ela sobre uma grade sem pedido e sem semana fixa. A edição
 * (`EdicaoDaGrade.kt`) e a migração do plano em produção passam pelo mesmo lugar: não há
 * segunda cópia da fórmula para divergir.
 *
 * ### `dataFim` é exclusivo
 *
 * É **o instante em que a semana acaba**, a meia-noite do dia seguinte ao último dia dela:
 * a semana da São Silvestre, de 28 a 31/12, termina em 01/01. É a única representação em
 * que *"a semana contém t"* é `dataInicio <= t < dataFim`, sem vão nem sobreposição,
 * inclusive na virada do horário de verão. RN-05 depende dela.
 */
object GeradorDePlano {

    /** O pico passa 10% da prova: a pessoa chega a ela já tendo corrido mais. */
    const val FATOR_DO_PICO = 1.1

    /** O longão-pico usual de maratona. Sem ele, a maratona pedia 46 km (DA-10, nº 1). */
    const val TETO_DO_LONGAO_KM = 32.0

    /**
     * Um longão vale no máximo 110% do maior das 4 semanas anteriores. Frandsen et al.,
     * *BJSM* 2025: o risco de lesão sobe quando uma sessão passa 10% da maior corrida dos
     * 30 dias anteriores (docs/01 §3.2).
     */
    const val PASSO_MAXIMO = 1.1

    /** Os 30 dias do estudo, em semanas de calendário. */
    const val JANELA_DO_PASSO = 4

    /** O pico precisa chegar a 75% da prova, a razão da maratona no teto (32 ÷ 42,2). */
    const val PISO_DA_PROVA = 0.75

    /** A penúltima semana corta o longão a 60% do maior do build. */
    const val TAPER_PENULTIMA = 0.6

    /** A última semana antes da prova corta o longão a 40% do maior do build. */
    const val TAPER_ULTIMA = 0.4

    /** 1 semana de build, 2 de taper e a da prova. */
    const val SEMANAS_MINIMAS = 4

    /** Abaixo disto, duas distâncias são a mesma para a conta (arredondamento de `Double`). */
    private const val FOLGA = 1e-9

    /**
     * Se o plano cabe. A conta é a de [gerar], e a tela de criação a chama antes de
     * navegar: um plano que não cabe não chega à revisão.
     *
     * @param inicio o instante da criação. O plano começa na [primeiraSegundaDe] dele.
     * @param fuso o fuso do plano (RN-28).
     */
    fun avaliar(parametros: ParametrosDeGeracao, inicio: Instant, fuso: ZoneId): Viabilidade =
        avaliar(
            alvoKm = parametros.distanciaAlvoKm,
            baselineKm = parametros.baselineKm,
            primeiraSegunda = primeiraSegundaDe(inicio.atZone(fuso).toLocalDate()),
            diaDaProva = parametros.dataProva.atZone(fuso).toLocalDate(),
        )

    /** O mesmo que o outro [avaliar], em datas de calendário: é o que o formulário tem. */
    fun avaliar(alvoKm: Double, baselineKm: Double, primeiraSegunda: LocalDate, diaDaProva: LocalDate): Viabilidade {
        val n = contarSemanas(primeiraSegunda, diaDaProva)
        if (n < SEMANAS_MINIMAS) return Viabilidade.CurtoDemais(n)

        val rampa = rampa(alvoKm, baselineKm, n)
        val picoMinimo = PISO_DA_PROVA * alvoKm
        return when {
            rampa.picoKm < picoMinimo - FOLGA -> {
                val semanasDeBuild = max(
                    1,
                    ceil(ln(picoMinimo / rampa.partidaKm) / ln(PASSO_MAXIMO) - FOLGA).toInt(),
                )
                Viabilidade.NaoCabe(
                    semanas = n,
                    picoKm = rampa.picoKm,
                    picoMinimoKm = picoMinimo,
                    primeiraProvaQueCabe = primeiraSegunda.plusWeeks((semanasDeBuild + 3 - 1).toLong()),
                    maiorAlvoQueCabeKm = floor(rampa.picoKm / PISO_DA_PROVA * 2) / 2,
                )
            }
            rampa.picoKm < rampa.picoDesejadoKm - FOLGA ->
                Viabilidade.PicoReduzido(n, rampa.picoKm, rampa.picoDesejadoKm)
            else -> Viabilidade.Cabe(n, rampa.picoKm, rampa.ritmo)
        }
    }

    /**
     * A grade de um plano novo: as datas, os tipos e a rampa.
     *
     * Falha alto com plano que não cabe. A tela de criação chama [avaliar] antes, então
     * chegar aqui com um plano assim é defeito, e não entrada do usuário.
     *
     * @param inicio qualquer instante do dia da criação. A grade arranca na
     *   [primeiraSegundaDe] dele (DA-10, nº 7), e não no dia do cadastro.
     * @param fuso o fuso do plano, nunca o do aparelho (RN-28).
     */
    fun gerar(parametros: ParametrosDeGeracao, inicio: Instant, fuso: ZoneId): List<Semana> {
        require(parametros.sessoesPorSemana in 2..4) {
            "sessões por semana é 2, 3 ou 4 (docs/01 §3.1), veio ${parametros.sessoesPorSemana}"
        }
        require(parametros.curtaKm > 0.0) { "a curta precisa ser maior que zero, veio ${parametros.curtaKm}" }
        val viabilidade = avaliar(parametros, inicio, fuso)
        require(viabilidade is Viabilidade.Cabe || viabilidade is Viabilidade.PicoReduzido) {
            "o plano não cabe até a prova: $viabilidade"
        }

        val primeiraSegunda = primeiraSegundaDe(inicio.atZone(fuso).toLocalDate())
        val diaDaProva = parametros.dataProva.atZone(fuso).toLocalDate()
        val total = viabilidade.semanas

        val esqueleto = (1..total).map { numero ->
            val segunda = primeiraSegunda.plusWeeks((numero - 1).toLong())
            val ultimoDia = if (numero == total) diaDaProva else segunda.plusDays(6)
            Semana(
                numero = numero,
                dataInicio = segunda.atStartOfDay(fuso).toInstant(),
                dataFim = ultimoDia.plusDays(1).atStartOfDay(fuso).toInstant(),
                sessoesAlvo = if (numero == total) 1 else parametros.sessoesPorSemana,
                kmAlvo = 0.0,
                longaoKm = null,
                tipo = when {
                    numero == total -> TipoDeSemana.PROVA
                    numero > total - 3 -> TipoDeSemana.TAPER
                    else -> TipoDeSemana.BUILD
                },
                // RN-26: parcial é ter menos de sete dias. Com a semana 1 começando
                // numa segunda, só a da prova pode ser.
                parcial = ChronoUnit.DAYS.between(segunda, ultimoDia) < 6,
            )
        }
        return recalcular(parametros, esqueleto, fixas = emptySet(), pedidos = emptyMap())
    }

    /**
     * A conta da grade: **cada semana de build vale o que foi pedido — a rampa ou o
     * pedido do dono —, limitada a 110% do maior longão das 4 semanas anteriores** e a
     * 32 km. O taper sai do maior longão do build; a prova não muda.
     *
     * Datas, tipos e sessões vêm de [grade] e não mudam: é delas que RN-05 e a aderência
     * dependem.
     *
     * @param fixas as semanas que ficam como estão (as congeladas, e a corrente quando
     *   a mudança vale da semana seguinte). Elas entram no teto como histórico.
     * @param pedidos o longão pedido por semana de build. Semana sem pedido segue a rampa.
     */
    internal fun recalcular(
        parametros: ParametrosDeGeracao,
        grade: List<Semana>,
        fixas: Set<Int>,
        pedidos: Map<Int, Double>,
    ): List<Semana> {
        val n = grade.size
        val semanasDeBuild = n - 3
        val rampa = rampa(parametros.distanciaAlvoKm, parametros.baselineKm, n)
        val longao = DoubleArray(n + 1).also { it[0] = parametros.baselineKm }
        val nova = grade.toMutableList()

        for (k in 1..semanasDeBuild) {
            val semana = grade[k - 1]
            if (k in fixas) {
                longao[k] = semana.longaoKm ?: (semana.kmAlvo / semana.sessoesAlvo)
                continue
            }
            val pedido = pedidos[k]
            val km = min(pedido ?: rampa.longao(k), teto(longao, k))
            longao[k] = km
            nova[k - 1] = semana.comLongao(km, parametros.curtaKm, pedido)
        }

        val maiorDoBuild = (1..semanasDeBuild).maxOf { longao[it] }
        listOf(TAPER_PENULTIMA, TAPER_ULTIMA).forEachIndexed { i, fator ->
            val k = semanasDeBuild + 1 + i
            if (k !in fixas) nova[k - 1] = grade[k - 1].comLongao(fator * maiorDoBuild, parametros.curtaKm, null)
        }
        if (n !in fixas) {
            nova[n - 1] = grade[n - 1].copy(kmAlvo = parametros.distanciaAlvoKm, longaoKm = null, longaoPedidoKm = null)
        }
        return nova
    }

    /**
     * O longão de cada semana de um plano novo de [n] semanas, da 1 à prova: a conta de
     * [recalcular] sem pedido e sem semana fixa. A última é a distância da prova.
     *
     * É o que a tela de criação desenha enquanto se digita. Vale também para o plano que
     * não cabe: a curva mostra até onde o longão chega, subindo no máximo 10% por semana.
     */
    fun curvaDoLongao(alvoKm: Double, baselineKm: Double, n: Int): List<Double> {
        require(n >= SEMANAS_MINIMAS) { "o plano tem pelo menos $SEMANAS_MINIMAS semanas, veio $n" }
        val semanasDeBuild = n - 3
        val rampa = rampa(alvoKm, baselineKm, n)
        val longao = DoubleArray(n + 1).also { it[0] = baselineKm }
        for (k in 1..semanasDeBuild) longao[k] = min(rampa.longao(k), teto(longao, k))
        val maiorDoBuild = (1..semanasDeBuild).maxOf { longao[it] }
        return (1..semanasDeBuild).map { longao[it] } +
            listOf(TAPER_PENULTIMA * maiorDoBuild, TAPER_ULTIMA * maiorDoBuild, alvoKm)
    }

    /**
     * O teto do longão da semana [numero] sobre uma grade: 110% do maior longão das 4
     * semanas anteriores, com a baseline como semana 0, e nunca mais de 32 km.
     */
    internal fun teto(parametros: ParametrosDeGeracao, grade: List<Semana>, numero: Int): Double {
        val longao = DoubleArray(numero).also { it[0] = parametros.baselineKm }
        for (k in 1 until numero) {
            val semana = grade[k - 1]
            longao[k] = semana.longaoKm ?: (semana.kmAlvo / semana.sessoesAlvo)
        }
        return teto(longao, numero)
    }

    private fun teto(longao: DoubleArray, k: Int): Double {
        var maior = 0.0
        for (j in max(0, k - JANELA_DO_PASSO) until k) maior = max(maior, longao[j])
        return min(PASSO_MAXIMO * maior, TETO_DO_LONGAO_KM)
    }

    /**
     * O volume da semana: o longão mais as curtas, e **a curta nunca passa do longão**.
     * `MotorDeXp.alvoDaSessao` faz a conta ao contrário, `(km_alvo − longão) ÷ (S − 1)`.
     */
    fun volume(longaoKm: Double, curtaKm: Double, sessoes: Int): Double =
        longaoKm + (sessoes - 1) * min(curtaKm, longaoKm)

    private fun Semana.comLongao(km: Double, curtaKm: Double, pedido: Double?): Semana =
        copy(longaoKm = km, kmAlvo = volume(km, curtaKm, sessoesAlvo), longaoPedidoKm = pedido)

    /**
     * A rampa geométrica de um plano de [n] semanas: o mesmo salto relativo em toda
     * semana de build (DA-10, nº 2), saindo da baseline (a semana 0) e chegando ao pico na
     * semana `N − 3`.
     */
    private fun rampa(alvoKm: Double, baselineKm: Double, n: Int): Rampa {
        val semanasDeBuild = n - 3
        val desejado = min(FATOR_DO_PICO * alvoKm, TETO_DO_LONGAO_KM)
        // Quem já corre mais que o pico mantém o longão no pico (DA-10, nº 6).
        val partida = min(baselineKm, desejado)
        val pico = min(desejado, partida * PASSO_MAXIMO.pow(semanasDeBuild))
        return Rampa(semanasDeBuild, partida, desejado, pico, (pico / partida).pow(1.0 / semanasDeBuild))
    }

    private data class Rampa(
        val semanasDeBuild: Int,
        val partidaKm: Double,
        val picoDesejadoKm: Double,
        val picoKm: Double,
        val ritmo: Double,
    ) {
        /** A semana 1 já é um passo além da baseline; a última de build é o pico, exato. */
        fun longao(k: Int): Double = if (k == semanasDeBuild) picoKm else partidaKm * ritmo.pow(k)
    }

    /**
     * // RN-01
     *
     * A segunda-feira em que o plano começa: **a primeira que cai hoje ou depois**
     * (DA-10, nº 7). Plano criado numa segunda começa nela; em outro dia, na segunda
     * seguinte, e a semana 1 nunca é parcial.
     *
     * Público porque a tela de criação conta as semanas a partir do mesmo ponto que a
     * geração. Duas leituras da palavra "início" fariam a tela aceitar um plano que o
     * gerador recusa.
     */
    fun primeiraSegundaDe(dia: LocalDate): LocalDate =
        dia.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

    /**
     * `N` = semanas de segunda a domingo da primeira segunda até o dia da prova,
     * **inclusive** (docs/01 §3.2). A última pode ter menos de sete dias. Prova antes da
     * primeira segunda dá zero.
     */
    fun contarSemanas(primeiraSegunda: LocalDate, diaDaProva: LocalDate): Int {
        if (diaDaProva.isBefore(primeiraSegunda)) return 0
        val dias = ChronoUnit.DAYS.between(primeiraSegunda, diaDaProva)
        return (dias / 7).toInt() + 1
    }
}
