package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Duration
import java.time.Instant

/**
 * O progresso de uma temporada que o replay deriva das corridas (docs/05 §2.5, passo 4).
 *
 * Sem `colecao_max` e sem `tier_atual`: os dois nascem na Fase 4 (D-19).
 */
data class ProgressoNaTemporada(
    val xpTotal: Long,
    /** A posição na escada, de 1 a `total_especies`. Pode recuar (XP-06). */
    val especieAtual: Int,
    /** A base de RN-36 e RN-37. */
    val ultimaCorridaEm: Instant,
    /** O máximo histórico da posição, que nunca recua (XP-06, RN-38). */
    val colecaoMax: Int = especieAtual,
    /**
     * Quantas quedas de 2% da lacuna **corrente** o último replay já descontou
     * (`regressao_semanas`). É o que dispensa um replay a cada abertura: só se roda outro
     * quando passa uma semana inativa nova (`Regressao.pendente`).
     */
    val semanasDeQueda: Int = 0,
    /**
     * A posição ao fim de cada semana fechada (`historico`): o que o replay grava para a
     * `WeeklyCloseScreen` animar. Vazio quando quem constrói não o leu.
     */
    val historico: List<MarcoDaSemana> = emptyList(),
)

/** Tudo o que o replay recalcula. As chaves de [xpPorSemana] são `{planId}_{n}`. */
data class ResultadoDoReplay(
    val porCorrida: Map<String, DetalheDeXp>,
    val xpPorSemana: Map<String, Long>,
    val porTemporada: Map<String, ProgressoNaTemporada>,
    /** Os recordes depois da última corrida: o que `agregados/records` guarda. */
    val recordes: Recordes,
)

/** O documento `users/{uid}/weekly/{planId}_{n}` (docs/05 §1). */
data class AgregadoDaSemana(
    val planoId: String,
    val xpSemana: Long,
    val sessoesFeitas: Int,
    val kmFeito: Double,
    val longaoFeito: Double,
    val aderenciaPct: Double,
    val fechada: Boolean,
)

/** Uma linha de `historico`: a posição na escada ao fim de uma semana fechada. */
data class MarcoDaSemana(val semana: String, val especie: Int)

/** O que o replay grava num intervalo: o detalhe de cada corrida e o `weekly` das semanas dele. */
data class EscritasDoReplay(
    val corridas: Map<String, DetalheDeXp>,
    val semanas: Map<String, AgregadoDaSemana>,
)

/**
 * // XP-10
 *
 * O motor único de recálculo (docs/05 §2.5). Gravação, correção, descarte, o botão de
 * Ajustes e o backfill passam todos por aqui: nunca há cálculo pontual de XP.
 *
 * **É Kotlin puro.** Quem lê e grava no Firestore é `MotorDeReplay`, na camada de dados;
 * aqui só entra a lista de corridas e sai o que elas valem.
 */
object Replay {

    /** A janela da média móvel de pace (docs/04 §3): as 4 semanas antes da corrida. */
    private val JANELA_DA_MEDIA: Duration = Duration.ofDays(28)

    /**
     * // RN-33
     *
     * Recalcula tudo a partir das corridas, em ordem cronológica.
     *
     * **Sempre desde a primeira corrida**, mesmo quando o chamador pediu um intervalo.
     * A média móvel, a ordem que protege as três corridas de calibração (XP-02) e os
     * recordes dependem de tudo o que veio antes, e o corpus é pequeno: um ano de treino
     * são uns 150 documentos. O intervalo limita o que se **grava** ([escritas]), não o
     * que se calcula. Como o cálculo é determinístico, a parte anterior ao intervalo dá
     * o mesmo que já está gravado.
     *
     * Descartada (RN-31) e substituída (RN-24) não entram em nada.
     *
     * **[agora] é obrigatório** (`F4-T10`): a regressão por inatividade (XP-08) depende
     * de quanto tempo faz que o corredor parou, e um replay sem relógio daria o XP de
     * ontem. A queda entra no XP da temporada, na espécie atual e no `historico`, e por
     * isso o replay **regrava** esses campos para quem já tinha progresso. O que não muda:
     * `xp_creditado` e `weekly`. [colecaoAnterior] é a `colecao_max` gravada por temporada:
     * ela só sobe (RN-38).
     */
    fun recalcular(
        corridas: List<Corrida>,
        grades: Map<String, List<Semana>>,
        temporadas: List<Temporada>,
        agora: Instant,
        colecaoAnterior: Map<String, Int> = emptyMap(),
    ): ResultadoDoReplay {
        // Empate de horário desempata pelo id: a ordem em que o Firestore devolve as
        // corridas não é garantida, e o resultado não pode depender dela.
        val validas = corridas
            .filter { !it.descartada && !it.substituida }
            .sortedWith(compareBy<Corrida> { it.dataHoraInicio }.thenBy { it.id })

        val porSemana = validas
            .filter { it.semanaRef != null }
            .groupBy { chaveDaSemana(it.planoId, it.semanaRef!!) }

        val porCorrida = linkedMapOf<String, DetalheDeXp>()
        var recordes = Recordes()

        validas.forEachIndexed { indice, corrida ->
            val semana = corrida.semanaRef?.let { n -> grades[corrida.planoId]?.firstOrNull { it.numero == n } }
            val contexto = ContextoDeXp(
                ordemNoHistorico = indice + 1,
                recordesAnteriores = recordes,
                mediaMovelPaceSegKm = mediaMovel(validas.subList(0, indice), corrida.dataHoraInicio),
                semana = semana,
                corridasDaSemana = semana?.let { porSemana[chaveDaSemana(corrida.planoId, it.numero)] }.orEmpty(),
            )
            porCorrida[corrida.id] = MotorDeXp.xpDaCorrida(corrida, contexto)
            recordes = MotorDeXp.recordesDepois(corrida, recordes)
        }

        val xpPorSemana = porSemana.mapValues { (_, daSemana) -> daSemana.sumOf { porCorrida.getValue(it.id).total } }

        val porTemporada = validas.groupBy { it.temporadaId }.mapNotNull { (id, daTemporada) ->
            val temporada = temporadas.firstOrNull { it.id == id } ?: return@mapNotNull null
            val trajetoria = Regressao.trajetoria(
                entradas = daTemporada.map { it.dataHoraInicio to porCorrida.getValue(it.id).total },
                ate = agora,
                temporada = temporada,
            )
            id to ProgressoNaTemporada(
                xpTotal = trajetoria.xp,
                especieAtual = posicaoNaEscada(trajetoria.xp, temporada),
                ultimaCorridaEm = daTemporada.maxOf { it.dataHoraInicio },
                // XP-06: o máximo guardado nunca desce, nem na correção (RN-38).
                colecaoMax = maxOf(colecaoAnterior[id] ?: 0, trajetoria.posicaoMaxima),
                semanasDeQueda = Regressao.semanasDeQueda(daTemporada.maxOf { it.dataHoraInicio }, agora),
            )
        }.toMap()

        return ResultadoDoReplay(porCorrida, xpPorSemana, porTemporada, recordes)
    }

    /**
     * O que gravar quando o replay foi pedido para `[desde, ate]`.
     *
     * As corridas válidas do intervalo recebem o detalhe novo. As semanas são as de
     * **toda** corrida do intervalo, válida ou não: a semana que perdeu a única corrida
     * para um descarte precisa ser regravada com zero, senão o ranking mostra XP de uma
     * corrida que saiu (RN-31).
     */
    fun escritas(
        corridas: List<Corrida>,
        resultado: ResultadoDoReplay,
        grades: Map<String, List<Semana>>,
        desde: Instant,
        ate: Instant,
    ): EscritasDoReplay {
        val doIntervalo = corridas.filter { it.dataHoraInicio >= desde && it.dataHoraInicio <= ate }
        val semanas = doIntervalo
            .mapNotNull { c -> c.semanaRef?.let { n -> grades[c.planoId]?.firstOrNull { it.numero == n }?.let { c.planoId to it } } }
            .distinct()
            .associate { (planoId, semana) ->
                chaveDaSemana(planoId, semana.numero) to agregado(planoId, semana, corridas, resultado, ate)
            }
        return EscritasDoReplay(
            corridas = doIntervalo.mapNotNull { c -> resultado.porCorrida[c.id]?.let { c.id to it } }.toMap(),
            semanas = semanas,
        )
    }

    /**
     * // RN-18
     *
     * O `weekly` de uma semana. As contas são as de `CalculoDeAderencia`, as mesmas da
     * Home e do detalhe do plano: o ranking e o card não podem discordar. `xp_semana`
     * ordena o ranking; a aderência sai em porcentagem, ao lado dele.
     */
    private fun agregado(
        planoId: String,
        semana: Semana,
        corridas: List<Corrida>,
        resultado: ResultadoDoReplay,
        ate: Instant,
    ): AgregadoDaSemana {
        val doPlano = corridas.filter { it.planoId == planoId }
        val validas = CalculoDeAderencia.validasDa(semana, doPlano)
        return AgregadoDaSemana(
            planoId = planoId,
            xpSemana = resultado.xpPorSemana[chaveDaSemana(planoId, semana.numero)] ?: 0L,
            sessoesFeitas = CalculoDeAderencia.sessoesFeitas(semana, doPlano),
            kmFeito = validas.sumOf { it.km },
            longaoFeito = validas.maxOfOrNull { it.km } ?: 0.0,
            aderenciaPct = CalculoDeAderencia.daSemana(semana, doPlano) * 100,
            fechada = CalendarioDoPlano.congelada(semana, ate),
        )
    }

    /**
     * O `historico` da temporada: a posição na escada ao fim de cada semana fechada, em
     * ordem. É o que a `WeeklyCloseScreen` anima, do `#001` até a posição acumulada
     * (docs/03 §3.20.1).
     *
     * As semanas são as dos planos em que o usuário tem corrida, dentro da temporada. A
     * semana sem corrida repete a posição: ela também fechou.
     */
    fun historico(
        temporada: Temporada,
        corridas: List<Corrida>,
        resultado: ResultadoDoReplay,
        grades: Map<String, List<Semana>>,
        ate: Instant,
    ): List<MarcoDaSemana> {
        val daTemporada = corridas.filter { it.id in resultado.porCorrida && it.temporadaId == temporada.id }
        return semanasDoHistorico(temporada, grades, ate)
            .map { (planoId, semana) ->
                // Com a regressão: a posição ao fim da semana já descontou as quedas até ali.
                val xp = Regressao.trajetoria(
                    entradas = daTemporada.filter { it.dataHoraInicio < semana.dataFim }
                        .map { it.dataHoraInicio to resultado.porCorrida.getValue(it.id).total },
                    ate = semana.dataFim,
                    temporada = temporada,
                ).xp
                MarcoDaSemana(chaveDaSemana(planoId, semana.numero), posicaoNaEscada(xp, temporada))
            }
    }

    /**
     * As semanas que entram no `historico` em [ate]: as fechadas (RN-05) que tocam a
     * temporada, por ordem de fechamento. Uma regra só para [historico] e
     * [semanasForaDoHistorico].
     */
    private fun semanasDoHistorico(
        temporada: Temporada,
        grades: Map<String, List<Semana>>,
        ate: Instant,
    ): List<Pair<String, Semana>> = grades
        .flatMap { (planoId, grade) -> grade.map { planoId to it } }
        .filter { (_, semana) ->
            CalendarioDoPlano.congelada(semana, ate) &&
                semana.dataFim > temporada.inicio && semana.dataInicio < temporada.fim
        }
        .sortedWith(compareBy({ it.second.dataFim }, { it.first }))

    /**
     * As semanas já fechadas em [agora] que o [gravado] ainda não tem.
     *
     * O `historico` só muda num replay, e o replay roda quando alguém grava ou edita uma
     * corrida, pede o recálculo, ou acumula uma semana inativa. Uma semana que fecha sem
     * nenhum desses eventos fica de fora, e com ela o fechamento e a liberação da Pokédex.
     * A abertura usa esta conta para pedir o replay que falta.
     *
     * @param grades só os planos em que o usuário tem corrida: são os que o replay lê.
     */
    fun semanasForaDoHistorico(
        temporada: Temporada,
        grades: Map<String, List<Semana>>,
        gravado: List<MarcoDaSemana>,
        agora: Instant,
    ): List<Pair<String, Semana>> {
        val chaves = gravado.mapTo(HashSet()) { it.semana }
        return semanasDoHistorico(temporada, grades, agora)
            .filter { (planoId, semana) -> chaveDaSemana(planoId, semana.numero) !in chaves }
    }

    /** O ID do documento de `weekly`. A rule do leaderboard lê o plano dele (docs/05 §3). */
    fun chaveDaSemana(planoId: String, numero: Int): String = "${planoId}_$numero"

    /**
     * Quanto o XP da temporada subiu de [antes] para [depois]: o `+N XP` da resposta ao
     * registro (docs/03 §3.3).
     *
     * **É a diferença do total, e não o XP da corrida.** O replay refaz a semana inteira
     * (XP-01): a corrida nova pode tomar o longão de outra e mudar o multiplicador dela, e
     * uma corrida depois de 21 dias parado devolve o que a regressão tinha descontado.
     * O que a pessoa ganhou é o que o total mudou.
     *
     * Nulo quando não há o que mostrar: a temporada não está no resultado, ou o total não
     * subiu. Um `+0 XP` diria que a corrida não contou, e ela contou para a semana.
     */
    fun ganhoNaTemporada(antes: ResultadoDoReplay, depois: ResultadoDoReplay, temporadaId: String): Long? {
        val total = depois.porTemporada[temporadaId]?.xpTotal ?: return null
        val ganho = total - (antes.porTemporada[temporadaId]?.xpTotal ?: 0L)
        return ganho.takeIf { it > 0 }
    }

    /**
     * A média móvel de pace das 4 semanas antes de [instante], em segundos por km.
     *
     * **Ponderada pela distância**: tempo total sobre quilômetros totais. A média simples
     * dos paces deixaria um trote de 1 km pesar o mesmo que um longão de 15. Nula sem
     * corrida na janela, e então não há bônus de pace.
     */
    fun mediaMovel(anteriores: List<Corrida>, instante: Instant): Double? {
        val janela = anteriores.filter {
            it.dataHoraInicio < instante && it.dataHoraInicio >= instante.minus(JANELA_DA_MEDIA) && it.km > 0
        }
        if (janela.isEmpty()) return null
        return janela.sumOf { it.duracaoSeg }.toDouble() / janela.sumOf { it.km }
    }
}

/**
 * // XP-06
 *
 * A posição na escada para um XP: quantas espécies o XP já ultrapassou, **com piso em
 * 1**, porque todo corredor começa no `#001` (D-15).
 *
 * A escada é a ordem dos tiers e, dentro de cada um, a ordem das espécies; cada espécie
 * custa o `xp_por_especie` do tier dela (docs/04 §4). Com 1.000 XP passam 10 espécies do
 * Tier 1, e a posição é 10: é a mesma contagem que a coleção usa, *"você já foi mais
 * rápido que 120 Pokémon"* para a coleção 120 (docs/04 §5.3).
 *
 * O teto é [Temporada.totalEspecies], nunca um literal (RN-43).
 */
fun posicaoNaEscada(xp: Long, temporada: Temporada): Int {
    var restante = xp
    var passadas = 0
    for (tier in temporada.tiers.sortedBy { it.ordem }) {
        repeat(tier.especies.size) {
            if (restante < tier.xpPorEspecie) return maxOf(1, passadas)
            restante -= tier.xpPorEspecie
            passadas++
        }
    }
    return maxOf(1, minOf(passadas, temporada.totalEspecies))
}
