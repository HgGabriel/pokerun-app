package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

/** A trajetória de XP de uma temporada: o que sobrou e a maior posição que já ocupou. */
data class Trajetoria(val xp: Long, val posicaoMaxima: Int)

/**
 * // XP-08
 *
 * A regressão por inatividade (docs/04 §5.2). **Só o agregado da temporada decai**: o
 * `xp_creditado` de cada corrida e o `weekly` (que ordena o ranking, RN-18) continuam
 * sendo o que a corrida valeu.
 *
 * - Até 20 dias sem correr, nada. **A partir do dia 21**, cai 2% por semana inativa, e a
 *   primeira queda é no próprio dia 21.
 * - **Nunca abaixo do piso do tier atual** (XP-08): quem chegou ao Tier 5 não volta ao 4
 *   por inatividade. O piso vale **só** aqui: correção de dado é verdade (XP-09), e o
 *   replay a aplica sem piso.
 * - É dependente de caminho (XP-10): a lacuna entre duas corridas cai antes de a segunda
 *   somar, e a lacuna até hoje cai no fim. Por isso mora no replay, nunca em cálculo
 *   pontual.
 */
object Regressao {

    private const val DIAS_ATE_A_QUEDA = 21L
    private const val DIAS_POR_SEMANA = 7L
    private const val TAXA_DA_QUEDA = 0.02

    /** Quantas quedas de 2% uma lacuna sem correr já impôs. 20 dias: zero; 21: uma; 28: duas. */
    fun semanasDeQueda(ultimaCorridaEm: Instant, agora: Instant): Int {
        val dias = Duration.between(ultimaCorridaEm, agora).toDays()
        if (dias < DIAS_ATE_A_QUEDA) return 0
        return ((dias - DIAS_ATE_A_QUEDA) / DIAS_POR_SEMANA + 1).toInt()
    }

    /**
     * Se já passou uma semana inativa que o último replay não descontou: o gatilho da
     * regressão (RN-37), que roda ao entrar na casca e não faz trabalho em segundo plano.
     */
    fun pendente(ultimaCorridaEm: Instant, semanasAplicadas: Int, agora: Instant): Boolean =
        semanasDeQueda(ultimaCorridaEm, agora) > semanasAplicadas

    /**
     * O custo acumulado de tudo que vem antes do tier em que o [xp] está: o fim do tier
     * anterior. O tier é o da espécie da posição atual, então quem está no **fim** do Tier
     * 5 (75.800) tem piso 38.100, como diz a tabela de docs/04 §5.2, e quem acabou de
     * ultrapassar a primeira espécie do Tier 6 tem piso 75.800.
     */
    fun pisoDoTier(xp: Long, temporada: Temporada): Long {
        val posicao = posicaoNaEscada(xp, temporada)
        var passadas = 0
        var acumulado = 0L
        for (tier in temporada.tiers.sortedBy { it.ordem }) {
            if (posicao <= passadas + tier.especies.size) return acumulado
            passadas += tier.especies.size
            acumulado += tier.especies.size.toLong() * tier.xpPorEspecie
        }
        return acumulado
    }

    /**
     * [semanas] quedas de 2%, **com o piso fixado no tier de onde a queda partiu**: se o
     * piso fosse recalculado a cada passo, o XP que pousou no limite do tier de baixo
     * passaria a ser "do tier de baixo" e seguiria descendo.
     */
    fun aplicar(xp: Long, semanas: Int, temporada: Temporada): Long {
        if (semanas <= 0) return xp
        val piso = pisoDoTier(xp, temporada)
        var atual = xp
        repeat(semanas) {
            if (atual <= piso) return piso.coerceAtMost(atual)
            // A perda é de pelo menos 1 XP: com o arredondamento, 2% de menos de 25 XP seria
            // zero e o XP ficaria parado acima de um piso que nunca alcança.
            val perda = maxOf(1L, (atual * TAXA_DA_QUEDA).roundToLong())
            atual = maxOf(piso, atual - perda)
        }
        return atual
    }

    /**
     * O XP e a maior posição da temporada, depois de percorrer as corridas em ordem: cada
     * lacuna cai antes de a corrida seguinte somar, e a última cai até [ate].
     *
     * @param entradas o instante e o XP creditado de cada corrida **da temporada**, em
     *   ordem cronológica.
     */
    fun trajetoria(entradas: List<Pair<Instant, Long>>, ate: Instant, temporada: Temporada): Trajetoria {
        var xp = 0L
        var anterior: Instant? = null
        var maxima = 1
        for ((instante, credito) in entradas) {
            anterior?.let { xp = aplicar(xp, semanasDeQueda(it, instante), temporada) }
            xp += credito
            maxima = maxOf(maxima, posicaoNaEscada(xp, temporada))
            anterior = instante
        }
        anterior?.let { xp = aplicar(xp, semanasDeQueda(it, ate), temporada) }
        return Trajetoria(xp, maxima)
    }
}
