package com.hggabriel.pokerun.ui.telas.detalhecorrida

import androidx.compose.runtime.Immutable
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.MotorDeXp
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.dominio.regras.albumDaPokedex
import com.hggabriel.pokerun.dominio.regras.numeroNaEscada
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.ritmoDe
import java.time.LocalDate
import java.time.LocalTime

/** Os estados da `RunDetailScreen` (docs/03 §3.13). */
sealed interface DetalheCorridaUiState {

    data object Carregando : DetalheCorridaUiState

    /**
     * A corrida inteira, no placar e nos cartões de baixo.
     *
     * @property paceSeg segundos por km, arredondado como o pace que a tela mostra. Nulo sem
     *   distância ou sem duração: o pace e a medalha do ritmo somem.
     * @property recordes os recordes que a corrida bateu, só quando a conta pagou por eles: as
     *   três primeiras corridas estabelecem recorde sem bônus, e a tela não as marca.
     * @property especie o número da espécie que a corrida deixou: a última figurinha que ela
     *   colou ou, sem figurinha, a posição em que o corredor estava. Nulo enquanto a temporada
     *   não chega.
     * @property figurinhas os números das figurinhas que esta corrida colou, na ordem da escada.
     */
    @Immutable
    data class Conteudo(
        val corridaId: String,
        /** O dia da corrida **no fuso do plano** (RN-28). */
        val dia: LocalDate,
        /** O ano só aparece no título quando não é o de hoje. */
        val mostraAno: Boolean,
        val inicio: LocalTime,
        val fim: LocalTime,
        val km: Double,
        val duracaoSeg: Long,
        val paceSeg: Long?,
        val fcMedia: Int?,
        val esforco: Int?,
        val tempo1kSeg: Long?,
        val tempo5kSeg: Long?,
        val nomeDoPlano: String,
        /** Nulo é corrida fora do intervalo do plano (RN-03). */
        val semanaRef: Int?,
        val sessao: SessaoReivindicada?,
        val xpCreditado: Long,
        /** A conta aberta. Nula enquanto o replay não gravou `xp_detalhe`. */
        val conta: ParcelasDeXp?,
        val recordes: Set<TipoDeRecorde>,
        /** Descartada (RN-31) ou substituída (RN-24): fora dos cálculos. */
        val foraDosCalculos: Boolean,
        val especie: Int?,
        val figurinhas: List<Int>,
    ) : DetalheCorridaUiState {
        val ritmo: Ritmo? get() = paceSeg?.let(::ritmoDe)
    }

    data object Falhou : DetalheCorridaUiState
}

/**
 * O detalhe de uma corrida, ou [DetalheCorridaUiState.Falhou] se ela ou o plano dela não
 * existem.
 *
 * **Campo ausente não vira `—`: o bloco some** (docs/03 §3.13.1). O pace é derivado, nunca
 * armazenado (docs/05 §4.2). A FC média, o esforço e os trechos só aparecem quando foram
 * digitados.
 *
 * A [temporada], o [historico] e o [progresso] chegam depois da corrida e podem não chegar
 * (sem rede): sem eles a tela mostra a corrida sem a espécie, sem as figurinhas e sem os
 * recordes, e nada falha.
 */
fun detalheDaCorrida(
    corrida: Corrida?,
    plano: Plano?,
    temporada: Temporada? = null,
    historico: List<Corrida>? = null,
    progresso: ProgressoNaTemporada? = null,
    hoje: LocalDate? = null,
): DetalheCorridaUiState {
    if (corrida == null || plano == null) return DetalheCorridaUiState.Falhou

    val inicio = corrida.dataHoraInicio.atZone(plano.fuso)
    val fora = corrida.descartada || corrida.substituida
    val album = if (historico != null && !fora) albumDaCorrida(corrida, temporada, historico, progresso) else null
    val pagouRecorde = (corrida.xpDetalhe?.recorde ?: 0) > 0

    return DetalheCorridaUiState.Conteudo(
        corridaId = corrida.id,
        dia = inicio.toLocalDate(),
        mostraAno = inicio.year != (hoje ?: LocalDate.now(plano.fuso)).year,
        inicio = inicio.toLocalTime(),
        fim = inicio.plusSeconds(corrida.duracaoSeg.coerceAtLeast(0)).toLocalTime(),
        km = corrida.km,
        duracaoSeg = corrida.duracaoSeg,
        paceSeg = paceDe(corrida.km, corrida.duracaoSeg),
        fcMedia = corrida.fcMedia,
        esforco = corrida.esforcoPercebido,
        tempo1kSeg = corrida.tempo1kSeg?.takeIf { it > 0 },
        tempo5kSeg = corrida.tempo5kSeg?.takeIf { it > 0 },
        nomeDoPlano = plano.nome,
        semanaRef = corrida.semanaRef,
        sessao = corrida.sessaoReivindicada,
        xpCreditado = corrida.xpCreditado,
        conta = corrida.xpDetalhe,
        recordes = if (pagouRecorde && historico != null) recordesDaCorrida(corrida, historico) else emptySet(),
        foraDosCalculos = fora,
        especie = album?.especie,
        figurinhas = album?.figurinhas.orEmpty(),
    )
}

/** O pace arredondado ao segundo, como `formatarPace`. Nulo sem distância ou sem duração. */
internal fun paceDe(km: Double, duracaoSeg: Long): Long? =
    if (km <= 0.0 || duracaoSeg <= 0L) null else Math.round(duracaoSeg / km)

/**
 * A distância em km inteiros e metros: `12 km 400 m`, no lugar de `12,40`. A vírgula some, e
 * cada parte tem a unidade dela. Arredonda ao metro uma vez, para `12,9996` não virar
 * `12 km 1000 m`.
 */
internal fun partesDaDistancia(km: Double): Pair<Int, Int> {
    val metros = Math.round(km.coerceAtLeast(0.0) * 1000)
    return (metros / 1000).toInt() to (metros % 1000).toInt()
}

/** A espécie e as figurinhas de uma corrida no álbum. */
internal data class AlbumDaCorrida(val especie: Int?, val figurinhas: List<Int>)

/**
 * O que a corrida fez no álbum da temporada dela, pela mesma conta da Pokédex
 * ([albumDaPokedex]): as posições que ela colou e a espécie em que deixou o corredor.
 * Corrida de outra temporada que não a lida não tem álbum.
 */
internal fun albumDaCorrida(
    corrida: Corrida,
    temporada: Temporada?,
    historico: List<Corrida>,
    progresso: ProgressoNaTemporada?,
): AlbumDaCorrida? {
    if (temporada == null || corrida.temporadaId != temporada.id) return null
    // O histórico pode chegar do cache sem a corrida recém-gravada.
    val corridas = if (historico.any { it.id == corrida.id }) historico else historico + corrida
    val xp = progresso?.xpTotal ?: 0L
    val colecao = maxOf(progresso?.colecaoMax ?: 1, posicaoNaEscada(xp, temporada))
    val coladas = albumDaPokedex(temporada, corridas, colecao).coladaEm
        .filterValues { it.id == corrida.id }
        .keys
        .sorted()
    val posicao = coladas.lastOrNull() ?: posicaoAte(corrida, temporada, corridas, colecao)
    return AlbumDaCorrida(
        especie = numeroNaEscada(posicao, temporada),
        figurinhas = coladas.map { numeroNaEscada(it, temporada) },
    )
}

/** A maior posição da coleção até a corrida, inclusive: a espécie em que ela deixou o corredor. */
private fun posicaoAte(corrida: Corrida, temporada: Temporada, corridas: List<Corrida>, colecao: Int): Int {
    var acumulado = 0L
    var alcancada = 1
    corridas
        .filter { it.temporadaId == temporada.id && !it.descartada && !it.substituida }
        .filter { it.dataHoraInicio <= corrida.dataHoraInicio }
        .sortedBy { it.dataHoraInicio }
        .forEach { c ->
            acumulado += maxOf(0L, c.xpCreditado)
            alcancada = maxOf(alcancada, minOf(posicaoNaEscada(acumulado, temporada), colecao))
        }
    return alcancada
}

/**
 * Os recordes que a corrida bateu, refeitos na ordem do replay (data, depois id): os
 * recordes são vitalícios (RN-39), e o histórico inteiro entra. A tela não guarda isso.
 */
internal fun recordesDaCorrida(corrida: Corrida, historico: List<Corrida>): Set<TipoDeRecorde> {
    if (corrida.descartada || corrida.substituida) return emptySet()
    val validas = (historico.filter { it.id != corrida.id } + corrida)
        .filter { !it.descartada && !it.substituida }
        .sortedWith(compareBy<Corrida> { it.dataHoraInicio }.thenBy { it.id })
    var recordes = Recordes()
    for (c in validas) {
        if (c.id == corrida.id) return MotorDeXp.recordesBatidos(c, recordes)
        recordes = MotorDeXp.recordesDepois(c, recordes)
    }
    return emptySet()
}
