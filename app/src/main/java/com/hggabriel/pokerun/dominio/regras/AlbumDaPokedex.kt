package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

/** A janela do XP por km que estima o km até o próximo (docs/03 §3.18). */
private val JANELA_DO_RITMO: Duration = Duration.ofDays(28)

/**
 * O álbum da Pokédex (docs/03 §3.18): o que a coleção conta em unidades de corredor.
 *
 * @property coladaEm a corrida que levou o corredor a cada posição da coleção, da 2 à
 *   `colecaoMax`. A posição 1 é de graça (D-15) e não tem corrida.
 * @property kmPorTier o km corrido em cada página, pela ordem do tier. Tier sem km fica fora.
 */
data class AlbumDaPokedex(
    val coladaEm: Map<Int, Corrida>,
    val kmPorTier: Map<Int, Double>,
)

/**
 * Refaz a coleção corrida a corrida, na ordem da data, somando o `xpCreditado` (XP-10: é o
 * resultado do último replay). A regressão fica de fora: ela recua a posição, não descola
 * figurinha (RN-38). O km de cada corrida se divide entre as páginas na proporção do XP que
 * ela cobriu em cada uma; corrida sem XP conta na página em que o corredor estava.
 *
 * Só leitura: nada daqui é gravado, e o XP da temporada continua sendo o do replay.
 */
fun albumDaPokedex(temporada: Temporada, corridas: List<Corrida>, colecaoMax: Int): AlbumDaPokedex {
    val degraus = escada(temporada)
    val tiers = temporada.tiers.sortedBy { it.ordem }
    // O XP que põe o corredor na primeira espécie de cada tier. O do primeiro é zero.
    val inicioDoTier = buildList {
        var passadas = 0
        for (tier in tiers) {
            add(tier.ordem to if (passadas == 0) 0L else degraus[passadas].second)
            passadas += tier.especies.size
        }
    }

    val coladaEm = mutableMapOf<Int, Corrida>()
    val kmPorTier = mutableMapOf<Int, Double>()
    var acumulado = 0L
    var alcancada = 1

    for (corrida in validasDaTemporada(temporada, corridas)) {
        val antes = acumulado
        acumulado += maxOf(0L, corrida.xpCreditado)

        if (acumulado == antes) {
            val tier = tierDaPosicao(temporada, posicaoNaEscada(antes, temporada))
            kmPorTier.merge(tier, corrida.km, Double::plus)
        } else {
            inicioDoTier.forEachIndexed { i, (ordem, ini) ->
                val fim = inicioDoTier.getOrNull(i + 1)?.second ?: Long.MAX_VALUE
                val coberto = minOf(fim, acumulado) - maxOf(ini, antes)
                if (coberto > 0) kmPorTier.merge(ordem, corrida.km * coberto / (acumulado - antes), Double::plus)
            }
        }

        val posicao = minOf(posicaoNaEscada(acumulado, temporada), colecaoMax)
        for (p in alcancada + 1..posicao) coladaEm[p] = corrida
        alcancada = maxOf(alcancada, posicao)
    }
    return AlbumDaPokedex(coladaEm, kmPorTier)
}

/**
 * Quantos km faltam para o próximo, no ritmo de XP por km das últimas 4 semanas;
 * sem corrida nelas, no da temporada toda. Arredonda para cima em meio km e nunca fica
 * abaixo de 0,5: a tela diz `~6 km`, uma estimativa, não uma promessa.
 *
 * Nulo sem próximo ([faltamXp] nulo) ou sem XP corrido na temporada.
 */
fun kmAteOProximo(faltamXp: Long?, corridas: List<Corrida>, temporada: Temporada, agora: Instant): Double? {
    if (faltamXp == null) return null
    val validas = validasDaTemporada(temporada, corridas)
    val recentes = validas.filter { it.dataHoraInicio >= agora - JANELA_DO_RITMO }
    val xpPorKm = xpPorKm(recentes) ?: xpPorKm(validas) ?: return null
    return maxOf(0.5, ceil(faltamXp / xpPorKm * 2) / 2)
}

private fun xpPorKm(corridas: List<Corrida>): Double? {
    val km = corridas.sumOf { it.km }
    val xp = corridas.sumOf { maxOf(0L, it.xpCreditado) }
    return if (km > 0 && xp > 0) xp / km else null
}

/**
 * As posições que a coleção ganhou depois da última semana fechada: o selo `nova`. A base
 * é o maior marco do histórico, porque a coleção nunca recua (XP-06); sem semana fechada,
 * é a posição 1, que é de graça.
 */
fun coladasNaSemana(colecaoMax: Int, historico: List<MarcoDaSemana>): IntRange =
    (historico.maxOfOrNull { it.especie } ?: 1) + 1..colecaoMax

/**
 * As figurinhas do pacote: o que a coleção ganhou desde a [ultimaVista] na aba. Na
 * primeira visita ([ultimaVista] nulo) o pacote fica vazio, porque a cascata de estreia
 * mostra a coleção inteira.
 */
fun figurinhasDoPacote(colecaoMax: Int, ultimaVista: Int?): IntRange =
    if (ultimaVista == null) IntRange.EMPTY else ultimaVista + 1..colecaoMax

private fun validasDaTemporada(temporada: Temporada, corridas: List<Corrida>): List<Corrida> =
    corridas.filter { it.temporadaId == temporada.id && !it.descartada && !it.substituida }
        .sortedBy { it.dataHoraInicio }
