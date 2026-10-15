package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import kotlin.math.roundToLong

/**
 * Uma semana fechada que ainda não foi apresentada, e de onde a escada parte (docs/01 §4.6).
 *
 * @property chave `{planId}_{n}`, o que `semana_apresentada` guarda (docs/05 §1).
 * @property posicaoAntes a posição ao fim da última semana já apresentada, ou `1` (o `#001`)
 *   na estreia da Fase 4 (docs/03 §3.20.1): o indicador percorre do início até aqui.
 * @property estreia a primeira vez: nada foi apresentado ainda.
 */
data class SemanaAApresentar(
    val chave: String,
    val planoId: String,
    val numero: Int,
    val posicaoAntes: Int,
    val posicaoDepois: Int,
    val estreia: Boolean,
)

/**
 * A ordem da abertura (docs/01 §4.6): existe semana fechada não apresentada?
 *
 * O `historico` da temporada traz a posição ao fim de cada semana fechada, em ordem. A
 * **última** é a semana a apresentar; se ela já é a `semana_apresentada`, não há nada.
 * Se várias semanas fecharam sem o app ser aberto, a tela mostra a última com o
 * **acumulado**: a escada parte da posição da última apresentada. O app não faz trabalho
 * em segundo plano (docs/03 §3.20), então a abertura é o único gatilho.
 */
fun semanaAApresentar(historico: List<MarcoDaSemana>, semanaApresentada: String?): SemanaAApresentar? {
    val ultima = historico.lastOrNull() ?: return null
    if (ultima.semana == semanaApresentada) return null

    val indiceDaApresentada = historico.indexOfFirst { it.semana == semanaApresentada }
    val antes = if (indiceDaApresentada >= 0) historico[indiceDaApresentada].especie else 1
    val corte = ultima.semana.lastIndexOf('_')
    val numero = ultima.semana.substring(corte + 1).toIntOrNull() ?: return null
    if (corte <= 0) return null

    return SemanaAApresentar(
        chave = ultima.semana,
        planoId = ultima.semana.substring(0, corte),
        numero = numero,
        posicaoAntes = antes,
        posicaoDepois = ultima.especie,
        estreia = semanaApresentada == null,
    )
}

/**
 * O XP da semana por origem, na ordem de docs/03 §3.20 (volume, longão, pace, recordes).
 * As três primeiras parcelas já saem **com o multiplicador** aplicado, e o recorde é
 * somado depois dele (docs/04 §3): a soma das quatro é o `xp_semana`, a menos de arredondamento.
 */
data class QuebraDoXp(val volume: Long, val longao: Long, val pace: Long, val recordes: Long) {
    val total: Long get() = volume + longao + pace + recordes
}

/**
 * Soma o `xp_detalhe` das corridas válidas da semana. Corrida sem detalhe (antes do
 * primeiro replay) não entra: a tela não inventa XP.
 */
fun quebraDoXp(corridas: List<Corrida>): QuebraDoXp {
    var volume = 0.0
    var longao = 0.0
    var pace = 0.0
    var recordes = 0L
    corridas.filter { !it.descartada && !it.substituida }.forEach { corrida ->
        val parcelas = corrida.xpDetalhe ?: return@forEach
        volume += parcelas.base * parcelas.multiplicador
        longao += parcelas.longao * parcelas.multiplicador
        pace += parcelas.pace * parcelas.multiplicador
        recordes += parcelas.recorde
    }
    return QuebraDoXp(volume.roundToLong(), longao.roundToLong(), pace.roundToLong(), recordes)
}
