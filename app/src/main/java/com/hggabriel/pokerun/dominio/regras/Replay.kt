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
     */
    fun recalcular(
        corridas: List<Corrida>,
        grades: Map<String, List<Semana>>,
        temporadas: List<Temporada>,
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
            val xp = daTemporada.sumOf { porCorrida.getValue(it.id).total }
            id to ProgressoNaTemporada(
                xpTotal = xp,
                especieAtual = posicaoNaEscada(xp, temporada),
                ultimaCorridaEm = daTemporada.maxOf { it.dataHoraInicio },
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
        return EscritasDoReplay(
            corridas = doIntervalo.mapNotNull { c -> resultado.porCorrida[c.id]?.let { c.id to it } }.toMap(),
            semanas = emptyMap(),
        )
    }

    fun historico(
        temporada: Temporada,
        corridas: List<Corrida>,
        resultado: ResultadoDoReplay,
        grades: Map<String, List<Semana>>,
        ate: Instant,
    ): List<MarcoDaSemana> = emptyList()

    /** O ID do documento de `weekly`. A rule do leaderboard lê o plano dele (docs/05 §3). */
    fun chaveDaSemana(planoId: String, numero: Int): String = "${planoId}_$numero"

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
