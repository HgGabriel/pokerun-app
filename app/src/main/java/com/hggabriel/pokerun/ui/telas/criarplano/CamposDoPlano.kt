package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import com.hggabriel.pokerun.dominio.regras.Viabilidade
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import java.time.LocalDate

/*
 * As validações do formulário de criação (`F1-T10`, docs/03 §3.5, DA-10).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `PassosDoOnboarding.kt` e de
 * `PainelDeHoje.kt`: são a parte da tela que se prova sem aparelho — o dia de hoje chega
 * por parâmetro — e são justamente as que a revisão de olho deixa passar.
 *
 * **Elas existem para impedir que o `GeradorDePlano` receba entrada que ele recusa.** Ele
 * falha alto com `require`, então uma tela que deixasse passar um plano que não cabe
 * derrubaria o app em vez de mostrar as saídas.
 */

/**
 * O que o formulário acusa, campo a campo.
 *
 * **Os cinco saem juntos**, e não um por vez: acusar em série faria o usuário descobrir
 * o terceiro erro no terceiro toque no botão. Toda mensagem é id de recurso, nunca texto
 * — microcopy mora em `strings.xml`, que é onde a varredura de `F1-T20` olha.
 */
data class ErrosDoPlano(
    @param:StringRes val nome: Int? = null,
    @param:StringRes val data: Int? = null,
    @param:StringRes val alvo: Int? = null,
    @param:StringRes val baseline: Int? = null,
    @param:StringRes val curta: Int? = null,
) {
    val algum: Boolean get() = nome != null || data != null || alvo != null || baseline != null || curta != null
}

/** O resultado de [validarRascunho]. */
sealed interface ValidacaoDoPlano {

    /**
     * Tudo válido, e o plano cabe: é isto que a rota `RevisarRascunho` carrega.
     *
     * [semanas] vem junto porque a tela mostra *"10 semanas até a prova"* antes de gerar,
     * e recalcular a mesma conta na hora de desenhar abriria a porta para as duas
     * divergirem.
     */
    data class Ok(
        val nome: String,
        val dataProva: LocalDate,
        val alvoKm: Double,
        val baselineKm: Double,
        val curtaKm: Double,
        val semanas: Int,
    ) : ValidacaoDoPlano

    data class Falhou(val erros: ErrosDoPlano) : ValidacaoDoPlano

    /**
     * Os campos estão certos, e **o plano não cabe** até a prova (docs/01 §3.2). A tela
     * diz o motivo e oferece as duas saídas; o rascunho não sai.
     */
    data class NaoCabe(val motivo: Viabilidade.NaoCabe) : ValidacaoDoPlano
}

/**
 * As validações de docs/03 §3.5, mais a forma dos três campos de distância, mais a
 * pergunta da DA-10: o plano cabe até a prova?
 *
 * ### A contagem parte da próxima segunda-feira
 *
 * É lá que o plano começa (DA-10, nº 7). Quem responde é [GeradorDePlano], que é o mesmo
 * que vai gerar a grade — duas leituras da palavra "início" é o defeito que isso evita.
 *
 * ### Data no futuro é a primeira porta, o mínimo de semanas é a segunda, caber é a
 * terceira
 *
 * As duas primeiras caem no mesmo campo e a mensagem é diferente: *"escolha uma data"*
 * não ajuda quem escolheu a semana que vem. A terceira só roda com os cinco campos
 * certos, porque fala de três deles ao mesmo tempo.
 *
 * **A distância da prova pode ser menor que a confortável** (DA-10, nº 6): é o plano de
 * quem já corre a distância e quer constância até a prova.
 *
 * @param dataProva nulo é *"ainda não respondeu"*: o `DatePicker` abre vazio.
 * @param hoje o relógio, por parâmetro. É o que torna esta função testável.
 */
internal fun validarRascunho(
    nome: String,
    dataProva: LocalDate?,
    alvo: String,
    baseline: String,
    curta: String,
    hoje: LocalDate,
): ValidacaoDoPlano {
    val nomeLimpo = nome.trim().ifBlank { null }
    val alvoKm = distanciaEmKm(alvo)
    val baselineKm = distanciaEmKm(baseline)
    val curtaKm = distanciaEmKm(curta)
    val primeiraSegunda = GeradorDePlano.primeiraSegundaDe(hoje)
    val semanas = dataProva?.let { GeradorDePlano.contarSemanas(primeiraSegunda, it) }

    val erros = ErrosDoPlano(
        nome = R.string.criar_erro_nome.takeIf { nomeLimpo == null },
        data = when {
            dataProva == null -> R.string.criar_erro_data_ausente
            !dataProva.isAfter(hoje) -> R.string.criar_erro_data_passada
            semanas!! < GeradorDePlano.SEMANAS_MINIMAS -> R.string.criar_erro_data_curta
            else -> null
        },
        alvo = R.string.criar_erro_distancia.takeIf { alvoKm == null },
        baseline = R.string.criar_erro_distancia.takeIf { baselineKm == null },
        curta = R.string.criar_erro_distancia.takeIf { curtaKm == null },
    )

    if (erros.algum) return ValidacaoDoPlano.Falhou(erros)

    val viabilidade = GeradorDePlano.avaliar(alvoKm!!, baselineKm!!, primeiraSegunda, dataProva!!)
    if (viabilidade is Viabilidade.NaoCabe) return ValidacaoDoPlano.NaoCabe(viabilidade)

    return ValidacaoDoPlano.Ok(
        nome = nomeLimpo!!,
        dataProva = dataProva,
        alvoKm = alvoKm,
        baselineKm = baselineKm,
        curtaKm = curtaKm!!,
        semanas = semanas!!,
    )
}

/**
 * Quantas semanas o plano teria com [dataProva], ou **nulo** enquanto a data não serve.
 *
 * É o número que a tela mostra antes de gerar — o único dado do formulário que o usuário
 * não tem como estimar de cabeça, e o que muda quando ele mexe no calendário.
 *
 * Nulo cobre os três casos em que não há o que mostrar: data não escolhida, data no
 * passado e plano curto demais. Nos dois últimos quem fala é a mensagem de erro do
 * campo, e um número ao lado dela seria ruído.
 */
internal fun semanasAte(hoje: LocalDate, dataProva: LocalDate?): Int? {
    if (dataProva == null || !dataProva.isAfter(hoje)) return null
    val total = GeradorDePlano.contarSemanas(GeradorDePlano.primeiraSegundaDe(hoje), dataProva)
    return total.takeIf { it >= GeradorDePlano.SEMANAS_MINIMAS }
}

/**
 * A curva do plano enquanto ele se escreve: o longão de cada semana, da 1 à prova, que a
 * montanha da tela desenha (27/10/2026).
 *
 * @property longoes um por semana; o último é a prova.
 * @property picoKm o maior longão do build, onde o Pokémon fica.
 * @property minimoKm 75% da prova: abaixo disto o plano não cabe (docs/01 §3.2).
 */
data class PreviaDoPlano(val longoes: List<Double>, val picoKm: Double, val minimoKm: Double) {
    val semanas: Int get() = longoes.size

    /** A semana do pico, a última do build, contada de zero. */
    val indiceDoPico: Int get() = longoes.size - 4

    val cabe: Boolean get() = picoKm >= minimoKm - 1e-9
}

/**
 * A prévia, ou **nulo** enquanto falta algo para desenhá-la: a data que serve (a mesma
 * porta de [semanasAte]), a distância da prova e a confortável. A curta e o nome não
 * mudam a curva.
 */
internal fun previaDoPlano(hoje: LocalDate, dataProva: LocalDate?, alvo: String, baseline: String): PreviaDoPlano? {
    val semanas = semanasAte(hoje, dataProva) ?: return null
    val alvoKm = distanciaEmKm(alvo) ?: return null
    val baselineKm = distanciaEmKm(baseline) ?: return null
    val longoes = GeradorDePlano.curvaDoLongao(alvoKm, baselineKm, semanas)
    return PreviaDoPlano(
        longoes = longoes,
        picoKm = longoes[semanas - 4],
        minimoKm = GeradorDePlano.PISO_DA_PROVA * alvoKm,
    )
}

/** Os atalhos da distância da prova: as quatro provas de rua mais corridas. */
internal val ATALHOS_DA_PROVA = listOf("5K" to "5", "10K" to "10", "21K" to "21,1", "42K" to "42,2")
