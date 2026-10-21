package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import kotlin.math.abs

/*
 * A grade **depois** de gerada (DA-10, docs/01 §3.3).
 *
 * `GeradorDePlano` responde "que grade sai dos parâmetros". Este arquivo responde as
 * perguntas da revisão do rascunho e do detalhe do plano: **até quanto o dono pode subir
 * um longão, o que muda quando ele mexe, e o que acontece quando a curta do plano muda.**
 *
 * Tudo passa por `GeradorDePlano.recalcular`, a mesma conta da geração: cada semana de
 * build vale o que foi pedido, limitada a 110% do maior longão das 4 semanas anteriores.
 * São funções puras sobre `List<Semana>`, sem Compose, Firestore ou relógio.
 */

/** Abaixo de 50 m a tela mostra o mesmo número antes e depois: não é mudança para o dono. */
private const val MUDANCA_VISIVEL_KM = 0.05

/** Uma semana que mudou de longão por causa da edição de outra (docs/01 §3.3). */
data class MudancaDeLongao(val numero: Int, val deKm: Double, val paraKm: Double)

sealed interface ResultadoDaEdicao {

    /**
     * A grade nova, e as **outras** semanas cujo longão mudou de forma visível. A tela
     * lista [tambemMudam] antes de gravar: *"Também mudam: semana 4, 13,3 → 12,7 km"*.
     */
    data class Aplicada(val grade: List<Semana>, val tambemMudam: List<MudancaDeLongao>) : ResultadoDaEdicao

    /**
     * O valor passa do teto da semana, e a edição é recusada. A tela diz o teto: *"Até
     * 12,7 km nesta semana: 10% acima do maior longão das 4 semanas anteriores."*
     */
    data class AcimaDoTeto(val tetoKm: Double) : ResultadoDaEdicao

    /**
     * A semana não se edita: congelada (RN-05), de taper ou da prova (os dois derivam do
     * build), ou o valor não é uma distância. A tela nem oferece o toque; isto é a guarda.
     */
    data object NaoEditavel : ResultadoDaEdicao
}

/**
 * O teto do longão da semana [numero]: 110% do maior longão das 4 semanas anteriores, com
 * a baseline como semana 0, e nunca mais de 32 km. É o número que o diálogo mostra.
 */
fun tetoDoLongao(parametros: ParametrosDeGeracao, grade: List<Semana>, numero: Int): Double =
    GeradorDePlano.teto(parametros, grade, numero)

/**
 * // RN-05, RN-06
 *
 * Troca o longão pedido da semana [numero], ou o apaga com [longaoKm] nulo (desfazer).
 *
 * - **Acima do teto, recusa** e diz o teto. Não há alerta não bloqueante: a grade nunca
 *   sai do limite (RN-30).
 * - **Abaixo, aceita sempre.** As semanas seguintes ficam no que pediam, limitadas ao teto
 *   novo: depois de uma semana baixada, a volta é gradual.
 * - **O pedido das outras semanas sobrevive.** Ele fica em [Semana.longaoPedidoKm], e é
 *   por isso que desfazer uma edição devolve as seguintes ao que eram.
 *
 * @param congeladas as semanas que já acabaram (RN-05). Não mudam e entram no teto.
 */
fun editarLongao(
    parametros: ParametrosDeGeracao,
    grade: List<Semana>,
    numero: Int,
    longaoKm: Double?,
    congeladas: Set<Int>,
): ResultadoDaEdicao {
    val semana = grade.firstOrNull { it.numero == numero }
    if (semana == null || semana.tipo != TipoDeSemana.BUILD || numero in congeladas) {
        return ResultadoDaEdicao.NaoEditavel
    }
    if (longaoKm != null) {
        if (!(longaoKm > 0.0)) return ResultadoDaEdicao.NaoEditavel
        val teto = tetoDoLongao(parametros, grade, numero)
        if (longaoKm > teto + 1e-9) return ResultadoDaEdicao.AcimaDoTeto(teto)
    }

    val pedidos = pedidosDe(grade, congeladas).toMutableMap()
    if (longaoKm == null) pedidos.remove(numero) else pedidos[numero] = longaoKm

    val nova = GeradorDePlano.recalcular(parametros, grade, congeladas, pedidos)
    val mudaram = nova.mapNotNull { depois ->
        val antes = grade[depois.numero - 1]
        val de = antes.longaoKm
        val para = depois.longaoKm
        if (depois.numero == numero || de == null || para == null || abs(de - para) < MUDANCA_VISIVEL_KM) {
            null
        } else {
            MudancaDeLongao(depois.numero, de, para)
        }
    }
    return ResultadoDaEdicao.Aplicada(nova, mudaram)
}

/**
 * A grade com a curta do plano trocada (docs/01 §3.3). **Vale a partir da semana
 * seguinte**: [fixas] traz as congeladas e a corrente, e nenhuma corrida já feita perde o
 * multiplicador. O longão e o teto não mudam; o volume das semanas futuras se refaz.
 *
 * Quem chama grava também `params_geracao.curta_km`.
 */
fun trocarCurta(
    parametros: ParametrosDeGeracao,
    grade: List<Semana>,
    curtaKm: Double,
    fixas: Set<Int>,
): List<Semana> =
    GeradorDePlano.recalcular(parametros.copy(curtaKm = curtaKm), grade, fixas, pedidosDe(grade, fixas))

/**
 * A grade refeita com a regra corrente, mantendo [fixas] e os pedidos gravados. É a
 * migração de um plano gerado pela regra anterior (DA-10, docs/05 §2.9).
 */
fun recalcularGrade(parametros: ParametrosDeGeracao, grade: List<Semana>, fixas: Set<Int>): List<Semana> =
    GeradorDePlano.recalcular(parametros, grade, fixas, pedidosDe(grade, fixas))

private fun pedidosDe(grade: List<Semana>, fixas: Set<Int>): Map<Int, Double> =
    grade.filter { it.numero !in fixas && it.tipo == TipoDeSemana.BUILD }
        .mapNotNull { semana -> semana.longaoPedidoKm?.let { semana.numero to it } }
        .toMap()
