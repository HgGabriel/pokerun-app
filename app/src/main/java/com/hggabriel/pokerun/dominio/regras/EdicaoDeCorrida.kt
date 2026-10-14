package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
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

/**
 * A agência do usuário sobre o próprio histórico (docs/01 §5, docs/03 §3.14): corrigir,
 * descartar e escolher a sessão que a corrida cumpre. Funções puras; quem grava é
 * `CorridaRepositorio`, e quem recalcula depois é o replay (RN-33).
 */
object EdicaoDeCorrida {

    /**
     * // RN-24
     *
     * O registro novo e o antigo marcado. A medição nova vai num documento novo, que
     * aponta para o antigo por `substitui_run_id`; o antigo só ganha `substituida`.
     * `plano_id` e `temporada_id` seguem os do original, e `semana_ref` sai de novo no
     * repositório, no fuso do plano, porque a data pode ter mudado (RN-28). O XP nasce
     * zerado: quem o escreve é o replay (XP-10).
     */
    fun corrigir(original: Corrida, valores: ValoresCorrigidos): CorrecaoDeCorrida =
        CorrecaoDeCorrida(
            nova = original.copy(
                id = "",
                dataHoraInicio = valores.dataHoraInicio,
                km = valores.km,
                duracaoSeg = valores.duracaoSeg,
                fcMedia = valores.fcMedia,
                esforcoPercebido = valores.esforcoPercebido,
                tempo1kSeg = valores.tempo1kSeg,
                tempo5kSeg = valores.tempo5kSeg,
                sessaoReivindicada = valores.sessao,
                xpCreditado = 0L,
                xpDetalhe = null,
                substituiRunId = original.id,
                substituida = false,
                descartada = false,
            ),
            antiga = original.copy(substituida = true),
        )

    /** // RN-31 — sai de todos os cálculos e continua na base. */
    fun descartar(corrida: Corrida): Corrida = corrida.copy(descartada = true)

    /**
     * // RN-34
     *
     * Grava a escolha do usuário, inclusive `Avulsa`. Nulo devolve a corrida à
     * atribuição automática, que nunca é gravada (docs/04 §3.2).
     */
    fun reivindicar(corrida: Corrida, sessao: SessaoReivindicada?): Corrida = corrida.copy(sessaoReivindicada = sessao)

    /**
     * // RN-32
     *
     * Correção e descarte são livres a qualquer momento, inclusive em semana congelada e
     * plano encerrado: RN-05 e RN-07 travam a estrutura do plano, não o histórico.
     */
    @Suppress("UNUSED_PARAMETER")
    fun edicaoPermitida(corrida: Corrida, plano: Plano?, agora: Instant): Boolean = true

    /**
     * O seletor de docs/03 §3.14: avulsa, o longão (quando a semana tem) e as curtas, cada
     * um com o alvo e o multiplicador que a corrida teria ali.
     *
     * **Slot escolhido pelo usuário para outra corrida fica desabilitado**; o da
     * atribuição automática não, porque ela se refaz em volta da escolha. Sem escolha
     * gravada, a opção marcada é a da atribuição automática, e a corrida que sobrou fora
     * das N sessões aparece como avulsa, que é o que ela vale.
     */
    fun opcoesDeSessao(corrida: Corrida, semana: Semana, corridasDaSemana: List<Corrida>): List<OpcaoDeSessao> {
        val outras = corridasDaSemana.filter { it.id != corrida.id && !it.descartada && !it.substituida }
        val escolhidasPorOutras = outras.mapNotNull { it.sessaoReivindicada }.toSet()
        val atual = corrida.sessaoReivindicada
            ?: segmentosDaSemana(semana, outras + corrida).firstOrNull { it.corridaId == corrida.id }?.sessao
            ?: SessaoReivindicada.Avulsa

        val curtas = if (semana.longaoKm != null) semana.sessoesAlvo - 1 else semana.sessoesAlvo
        val sessoes = buildList {
            add(SessaoReivindicada.Avulsa)
            if (semana.longaoKm != null) add(SessaoReivindicada.Longao)
            (1..curtas).forEach { add(SessaoReivindicada.Curta(it)) }
        }

        return sessoes.map { sessao ->
            val hipotese = corrida.copy(sessaoReivindicada = sessao)
            val contexto = ContextoDeXp(
                ordemNoHistorico = 1,
                recordesAnteriores = Recordes(),
                mediaMovelPaceSegKm = null,
                semana = semana,
                corridasDaSemana = outras + hipotese,
            )
            OpcaoDeSessao(
                sessao = sessao,
                alvoKm = if (sessao == SessaoReivindicada.Avulsa) null else MotorDeXp.alvoDaSessao(sessao, semana),
                multiplicador = MotorDeXp.xpDaCorrida(hipotese, contexto).multiplicador,
                habilitada = sessao == SessaoReivindicada.Avulsa || sessao !in escolhidasPorOutras,
                escolhida = sessao == atual,
            )
        }
    }

    /**
     * O resumo de docs/01 §5.3, a partir do progresso da temporada antes e depois do
     * replay. Sem progresso antes, o usuário estava no `#001` com zero XP (D-15).
     */
    fun resumo(
        antes: ProgressoNaTemporada?,
        depois: ProgressoNaTemporada?,
        recordesAntes: Recordes,
        recordesDepois: Recordes,
    ): ResumoDaCorrecao = ResumoDaCorrecao(
        xpAntes = antes?.xpTotal ?: 0L,
        xpDepois = depois?.xpTotal ?: 0L,
        posicaoAntes = antes?.especieAtual ?: 1,
        posicaoDepois = depois?.especieAtual ?: 1,
        recordesQueMudaram = buildSet {
            if (recordesAntes.melhorPaceSegKm != recordesDepois.melhorPaceSegKm) add(TipoDeRecorde.MELHOR_PACE)
            if (recordesAntes.melhor1kSeg != recordesDepois.melhor1kSeg) add(TipoDeRecorde.MELHOR_1K)
            if (recordesAntes.melhor5kSeg != recordesDepois.melhor5kSeg) add(TipoDeRecorde.MELHOR_5K)
            if (recordesAntes.maiorDistanciaKm != recordesDepois.maiorDistanciaKm) add(TipoDeRecorde.MAIOR_DISTANCIA)
            if (recordesAntes.maiorDuracaoSeg != recordesDepois.maiorDuracaoSeg) add(TipoDeRecorde.MAIOR_DURACAO)
        },
    )
}
