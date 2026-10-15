package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import kotlin.math.ceil

/** O prazo só aparece com 4 ou mais semanas de histórico (docs/03 §3.18). */
private const val SEMANAS_PARA_O_PRAZO = 4
private const val DIAS_NA_SEMANA = 7

/**
 * O painel de três linhas da `PokedexMainScreen` (docs/03 §3.18, docs/04 §5.3):
 *
 * ```
 * Você já é mais rápido que 87 Pokémon.        · 41.300 XP
 *
 * Superado esta semana   Marowak
 * Próximo alvo           Growlithe    4 dias no ritmo atual
 * Se cumprir a meta      Ninetales
 * ```
 *
 * Os campos são **números da Pokédex**, e não nomes: o nome vem da PokéAPI (D-10), e
 * quem desenha o resolve.
 *
 * @property ultrapassados quantas espécies o corredor ultrapassou. Com [recuou] é a
 *   coleção: *"Você já foi mais rápido que 120 Pokémon"*.
 * @property posicao a forma atual, que a linguagem de recuo mostra junto (docs/04 §5.3).
 * @property retomar o próximo alvo é uma espécie que a coleção já teve: *"Para retomar"* no
 *   lugar de *"conquistar"* (docs/04 §5.3).
 * @property prazoEmDias quanto falta para o próximo alvo no ritmo médio de XP por semana.
 *   Nulo antes de 4 semanas de histórico, quando a média ainda não diz nada.
 * @property aoCumprirAMeta a espécie em que o corredor cairia se fechasse a semana no
 *   plano. Nulo sem plano ativo ou sem semana corrente.
 */
data class PainelDaPokedex(
    val xp: Long,
    val posicao: Int,
    val colecaoMax: Int,
    val recuou: Boolean,
    val ultrapassados: Int,
    val superadoNaSemana: Int?,
    val proximoAlvo: Int?,
    val faltamXp: Long?,
    val retomar: Boolean,
    val prazoEmDias: Int?,
    val aoCumprirAMeta: Int?,
)

/**
 * // XP-12
 *
 * @param historico a posição ao fim de cada semana fechada (`historico` da temporada).
 *   A última linha é a base de *"superado esta semana"*.
 * @param xpDaMeta o XP que a semana ainda rende se o plano for cumprido, ou nulo.
 */
fun painelDaPokedex(
    temporada: Temporada,
    xp: Long,
    colecaoMax: Int,
    historico: List<MarcoDaSemana>,
    xpDaMeta: Long?,
): PainelDaPokedex {
    val degraus = escada(temporada)
    val posicao = posicaoNaEscada(xp, temporada)
    val colecao = maxOf(colecaoMax, posicao).coerceAtMost(degraus.size)
    val recuou = posicao < colecao

    val proximo = degraus.getOrNull(posicao)
    val faltam = proximo?.let { (it.second - xp).coerceAtLeast(0L) }

    val fimDaUltimaSemana = historico.lastOrNull()?.especie
    // Superado esta semana: a espécie da posição, se a posição subiu depois da última
    // semana fechada. Sem semana fechada, a base é o `#001`, que ninguém "superou".
    val superado = if (posicao > (fimDaUltimaSemana ?: 1)) degraus[posicao - 1].first else null

    val prazo = if (historico.size >= SEMANAS_PARA_O_PRAZO && faltam != null && xp > 0) {
        val xpPorSemana = xp.toDouble() / historico.size
        ceil(faltam / xpPorSemana * DIAS_NA_SEMANA).toInt().coerceAtLeast(1)
    } else {
        null
    }

    val aoCumprir = xpDaMeta?.let { degraus[posicaoNaEscada(xp + it, temporada) - 1].first }

    return PainelDaPokedex(
        xp = xp,
        posicao = posicao,
        colecaoMax = colecao,
        recuou = recuou,
        ultrapassados = if (recuou) colecao else posicao,
        superadoNaSemana = superado,
        proximoAlvo = proximo?.first,
        faltamXp = faltam,
        retomar = recuou && posicao + 1 <= colecao,
        prazoEmDias = prazo,
        aoCumprirAMeta = aoCumprir,
    )
}

/** O XP por km de uma sessão que cumpre o alvo: `km × 100 × 2,0` (docs/04 §3, §3.2). */
private const val XP_DA_SESSAO_CUMPRIDA_POR_KM = 100.0 * 2.0

/**
 * O XP que a semana corrente ainda rende **se o plano for cumprido** (docs/03 §3.18, a
 * linha *"Se cumprir a meta"*): os km que faltam para o `km_alvo` da semana, a 100 XP por
 * km e com o multiplicador de quem cumpre, `2,0×`. Sem bônus de pace, de longão nem de
 * recorde: é uma estimativa por baixo, e a linha diz a espécie que ela alcança, não uma
 * promessa de XP. Decisão nº 137.
 *
 * Nulo sem semana corrente (plano não iniciado ou já encerrado).
 */
fun xpDaMetaDaSemana(
    plano: com.hggabriel.pokerun.dominio.modelo.Plano,
    grade: List<com.hggabriel.pokerun.dominio.modelo.Semana>,
    corridas: List<com.hggabriel.pokerun.dominio.modelo.Corrida>,
    agora: java.time.Instant,
): Long? {
    val semana = CalendarioDoPlano.semanaDe(agora, plano, grade) ?: return null
    val feitos = CalculoDeAderencia.validasDa(semana, corridas).sumOf { it.km }
    return (maxOf(0.0, semana.kmAlvo - feitos) * XP_DA_SESSAO_CUMPRIDA_POR_KM).toLong()
}
