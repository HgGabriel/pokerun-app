package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana

/**
 * Os cinco recordes monitorados de docs/04 §3.3, **vigentes até antes de uma corrida**.
 *
 * Nulo é recorde ainda não estabelecido. É vitalício (RN-39): atravessa planos e
 * temporadas, e é o que `users/{uid}/agregados/records` guarda.
 */
data class Recordes(
    val melhorPaceSegKm: Double? = null,
    val melhor1kSeg: Long? = null,
    val melhor5kSeg: Long? = null,
    val maiorDistanciaKm: Double? = null,
    val maiorDuracaoSeg: Long? = null,
)

enum class TipoDeRecorde { MELHOR_PACE, MELHOR_1K, MELHOR_5K, MAIOR_DISTANCIA, MAIOR_DURACAO }

/**
 * O que o motor precisa saber da história para pontuar uma corrida.
 *
 * **O motor não percorre a história: recebe-a pronta.** Quem anda em ordem cronológica
 * é o replay (`F2-T07`, docs/05 §2.5), que entrega a cada corrida a média móvel e os
 * recordes vigentes até ali. Assim o motor é uma função de uma corrida só, e não há uma
 * segunda leitura do histórico que possa discordar da do replay.
 *
 * @property ordemNoHistorico a posição da corrida entre as válidas do usuário, a partir
 * de 1. É vitalícia, como os recordes que ela protege (XP-02).
 * @property mediaMovelPaceSegKm a média das 4 semanas anteriores, ou nulo sem histórico.
 * @property semana a semana do plano em que a corrida cai. Nulo é corrida avulsa (XP-05).
 * @property corridasDaSemana as corridas do mesmo plano e semana, a própria inclusive.
 * A atribuição de sessões se refaz sobre elas (docs/04 §3.2).
 */
data class ContextoDeXp(
    val ordemNoHistorico: Int,
    val recordesAnteriores: Recordes,
    val mediaMovelPaceSegKm: Double?,
    val semana: Semana?,
    val corridasDaSemana: List<Corrida> = emptyList(),
)

/**
 * As parcelas da conta de docs/04 §3, que viram `xp_detalhe` (docs/05 §1).
 *
 * [base] e [longao] ficam em `Double` porque `km × 100` com 4,6 km não é inteiro em
 * ponto flutuante; só o [total] arredonda, uma vez.
 */
data class DetalheDeXp(
    val base: Double,
    val longao: Double,
    val pace: Int,
    val multiplicador: Double,
    val recorde: Int,
    val recordesBatidos: Set<TipoDeRecorde>,
    val total: Long,
)

/**
 * A fórmula de XP de docs/04 §3, aplicada a uma corrida:
 *
 * ```
 * XP = (km × 100 + bônus_longão + bônus_pace) × multiplicador + bônus_recorde
 * ```
 *
 * Não há termo de tempo (docs/04 §3.1): ele premiaria a lentidão.
 */
object MotorDeXp {

    private const val XP_POR_KM = 100.0
    private const val XP_POR_KM_DE_LONGAO = 50.0
    /** O bônus de longão conta a partir de 60% do longão-alvo da semana. */
    private const val INICIO_DO_BONUS_DE_LONGAO = 0.6
    private const val BONUS_DE_PACE = 200
    private const val BONUS_POR_RECORDE = 500
    /** As 3 primeiras corridas são calibração (XP-02). */
    private const val CORRIDAS_DE_CALIBRACAO = 3
    /** Abaixo disso um tiro de 300 m viraria o recorde de pace (docs/04 §3.3). */
    private const val KM_MINIMO_DO_PACE = 1.0
    private const val KM_DO_1K = 1.0
    private const val KM_DO_5K = 5.0

    private const val MULTIPLICADOR_CUMPRIU = 2.0
    private const val MULTIPLICADOR_REIVINDICOU = 1.5
    private const val MULTIPLICADOR_AVULSA = 1.0

    /**
     * // XP-01
     *
     * O XP da corrida e as parcelas da conta. Quem credita é o replay, a partir do início
     * da semana da corrida: o multiplicador depende das outras corridas da semana.
     */
    fun xpDaCorrida(corrida: Corrida, contexto: ContextoDeXp): DetalheDeXp {
        val base = corrida.km * XP_POR_KM
        val sessao = sessaoDa(corrida, contexto)
        val longao = bonusDeLongao(corrida, sessao, contexto.semana)
        val pace = bonusDePace(corrida, contexto.mediaMovelPaceSegKm)
        val multiplicador = multiplicador(corrida, sessao, contexto.semana)
        val batidos = recordesBatidos(corrida, contexto.recordesAnteriores)
        val recorde = bonusDeRecorde(batidos, contexto.ordemNoHistorico)

        val total = Math.round((base + longao + pace) * multiplicador) + recorde
        return DetalheDeXp(base, longao, pace, multiplicador, recorde, batidos, total)
    }

    /**
     * // RN-34
     *
     * A sessão que a corrida ocupa depois da atribuição da semana, ou nulo se sobrou.
     *
     * **Sai de [segmentosDaSemana], e não de uma segunda implementação** (ficha de
     * `F2-T06`): a barra da Home e o multiplicador têm de concordar sobre quem é o longão.
     * A escolha gravada pelo usuário vem primeiro; a automática se arranja em volta dela.
     */
    private fun sessaoDa(corrida: Corrida, contexto: ContextoDeXp): SessaoReivindicada? {
        val semana = contexto.semana ?: return null
        if (corrida.semanaRef != semana.numero) return null
        val daSemana = contexto.corridasDaSemana.filter { it.id != corrida.id } + corrida
        return segmentosDaSemana(semana, daSemana).firstOrNull { it.corridaId == corrida.id }?.sessao
    }

    /**
     * // XP-05
     *
     * `+50 XP` por km acima de 60% do longão-alvo, **só para a corrida que ocupa o
     * longão**. Avulsa, semana sem longão (2ª de taper e prova) e corrida em sessão curta
     * não recebem.
     */
    private fun bonusDeLongao(corrida: Corrida, sessao: SessaoReivindicada?, semana: Semana?): Double {
        if (sessao != SessaoReivindicada.Longao) return 0.0
        val alvo = semana?.longaoKm ?: return 0.0
        return maxOf(0.0, corrida.km - INICIO_DO_BONUS_DE_LONGAO * alvo) * XP_POR_KM_DE_LONGAO
    }

    /** `+200` se o pace for **estritamente** melhor que a média móvel de 4 semanas. */
    private fun bonusDePace(corrida: Corrida, media: Double?): Int {
        if (media == null) return 0
        return if (paceDe(corrida) < media) BONUS_DE_PACE else 0
    }

    /**
     * // XP-03
     *
     * `2,0×` se a sessão ocupada foi cumprida (90% do alvo, o limiar de RN-10), `1,5×` se
     * ficou abaixo, `1,0×` se a corrida é avulsa ou sobrou fora das N sessões.
     * Nunca olha o dia da semana.
     */
    private fun multiplicador(corrida: Corrida, sessao: SessaoReivindicada?, semana: Semana?): Double {
        if (sessao == null || semana == null) return MULTIPLICADOR_AVULSA
        val alvo = alvoDaSessao(sessao, semana)
        return if (corrida.km >= CalculoDeAderencia.PISO_DO_LONGAO * alvo) {
            MULTIPLICADOR_CUMPRIU
        } else {
            MULTIPLICADOR_REIVINDICOU
        }
    }

    /**
     * A distância-alvo de uma sessão. A curta divide o que sobra do volume depois do
     * longão; sem longão, o volume se divide igual entre as sessões. É a mesma conta do
     * gerador (`GeradorDePlano.volume`), lida ao contrário, e sobrevive à edição do
     * longão porque o volume se refaz junto com ele.
     */
    internal fun alvoDaSessao(sessao: SessaoReivindicada, semana: Semana): Double {
        val longao = semana.longaoKm
        return when {
            sessao == SessaoReivindicada.Longao && longao != null -> longao
            longao != null -> (semana.kmAlvo - longao) / (semana.sessoesAlvo - 1)
            else -> semana.kmAlvo / semana.sessoesAlvo
        }
    }

    /**
     * // XP-02
     *
     * `+500` por recorde batido, **a partir da 4ª corrida**. As três primeiras ainda
     * estabelecem os recordes: só não pagam por eles.
     */
    private fun bonusDeRecorde(batidos: Set<TipoDeRecorde>, ordem: Int): Int =
        if (ordem <= CORRIDAS_DE_CALIBRACAO) 0 else batidos.size * BONUS_POR_RECORDE

    /**
     * Os recordes que a corrida bate. **Estabelecer o primeiro valor conta como bater**:
     * docs/04 §3.3 diz que na primeira corrida os cinco "cairiam de uma vez".
     */
    fun recordesBatidos(corrida: Corrida, anteriores: Recordes): Set<TipoDeRecorde> = buildSet {
        if (corrida.km >= KM_MINIMO_DO_PACE && menor(paceDe(corrida), anteriores.melhorPaceSegKm)) {
            add(TipoDeRecorde.MELHOR_PACE)
        }
        tempoDe1k(corrida)?.let { if (menor(it.toDouble(), anteriores.melhor1kSeg?.toDouble())) add(TipoDeRecorde.MELHOR_1K) }
        tempoDe5k(corrida)?.let { if (menor(it.toDouble(), anteriores.melhor5kSeg?.toDouble())) add(TipoDeRecorde.MELHOR_5K) }
        if (corrida.km > 0 && maior(corrida.km, anteriores.maiorDistanciaKm)) add(TipoDeRecorde.MAIOR_DISTANCIA)
        if (corrida.duracaoSeg > 0 && maior(corrida.duracaoSeg.toDouble(), anteriores.maiorDuracaoSeg?.toDouble())) {
            add(TipoDeRecorde.MAIOR_DURACAO)
        }
    }

    /** Os recordes depois da corrida: o que o replay passa à seguinte. */
    fun recordesDepois(corrida: Corrida, anteriores: Recordes): Recordes {
        val batidos = recordesBatidos(corrida, anteriores)
        return Recordes(
            melhorPaceSegKm = if (TipoDeRecorde.MELHOR_PACE in batidos) paceDe(corrida) else anteriores.melhorPaceSegKm,
            melhor1kSeg = if (TipoDeRecorde.MELHOR_1K in batidos) tempoDe1k(corrida) else anteriores.melhor1kSeg,
            melhor5kSeg = if (TipoDeRecorde.MELHOR_5K in batidos) tempoDe5k(corrida) else anteriores.melhor5kSeg,
            maiorDistanciaKm = if (TipoDeRecorde.MAIOR_DISTANCIA in batidos) corrida.km else anteriores.maiorDistanciaKm,
            maiorDuracaoSeg = if (TipoDeRecorde.MAIOR_DURACAO in batidos) corrida.duracaoSeg else anteriores.maiorDuracaoSeg,
        )
    }

    /** Segundos por km. Derivado, nunca armazenado (docs/05 §4.2). */
    fun paceDe(corrida: Corrida): Double = corrida.duracaoSeg / corrida.km

    /** O tempo digitado só vale se a corrida alcançou a marca. */
    private fun tempoDe1k(corrida: Corrida): Long? = corrida.tempo1kSeg?.takeIf { corrida.km >= KM_DO_1K && it > 0 }

    private fun tempoDe5k(corrida: Corrida): Long? = corrida.tempo5kSeg?.takeIf { corrida.km >= KM_DO_5K && it > 0 }

    private fun menor(valor: Double, recorde: Double?): Boolean = recorde == null || valor < recorde

    private fun maior(valor: Double, recorde: Double?): Boolean = recorde == null || valor > recorde
}
