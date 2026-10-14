package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import java.time.Instant

/** Os campos editáveis de docs/03 §3.14, e a sessão escolhida. */
data class ValoresCorrigidos(
    val dataHoraInicio: Instant,
    val km: Double,
    val duracaoSeg: Long,
    val fcMedia: Int?,
    val esforcoPercebido: Int?,
    val tempo1kSeg: Long?,
    val tempo5kSeg: Long?,
    val sessao: SessaoReivindicada?,
)

/** O par que RN-24 grava no mesmo lote: o registro novo e o antigo marcado. */
data class CorrecaoDeCorrida(val nova: Corrida, val antiga: Corrida)

/** Uma linha do seletor de sessão de docs/03 §3.14. */
data class OpcaoDeSessao(
    val sessao: SessaoReivindicada,
    val alvoKm: Double?,
    val multiplicador: Double,
    val habilitada: Boolean,
    val escolhida: Boolean,
)

/** O resumo do replay de docs/01 §5.3, sem a linha `Coleção` na Fase 2 (D-19). */
data class ResumoDaCorrecao(
    val xpAntes: Long,
    val xpDepois: Long,
    val posicaoAntes: Int,
    val posicaoDepois: Int,
    val recordesQueMudaram: Set<TipoDeRecorde>,
)

object EdicaoDeCorrida {
    fun corrigir(original: Corrida, valores: ValoresCorrigidos): CorrecaoDeCorrida =
        CorrecaoDeCorrida(original, original)

    fun descartar(corrida: Corrida): Corrida = corrida

    fun reivindicar(corrida: Corrida, sessao: SessaoReivindicada?): Corrida = corrida

    fun edicaoPermitida(corrida: Corrida, plano: Plano?, agora: Instant): Boolean = false

    fun opcoesDeSessao(corrida: Corrida, semana: Semana, corridasDaSemana: List<Corrida>): List<OpcaoDeSessao> =
        emptyList()

    fun resumo(
        antes: ProgressoNaTemporada?,
        depois: ProgressoNaTemporada?,
        recordesAntes: Recordes,
        recordesDepois: Recordes,
    ): ResumoDaCorrecao = ResumoDaCorrecao(0, 0, 0, 0, emptySet())
}
