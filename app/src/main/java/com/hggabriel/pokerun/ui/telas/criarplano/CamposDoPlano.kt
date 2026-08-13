package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import java.time.LocalDate

/*
 * As três validações do formulário de criação (`F1-T10`, docs/03 §3.5).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `PassosDoOnboarding.kt` e de
 * `PainelDeHoje.kt`: são a parte da tela que se prova sem aparelho — o dia de hoje chega
 * por parâmetro — e são justamente as que a revisão de olho deixa passar.
 *
 * **As três existem para impedir que o `GeradorDePlano` receba entrada que ele recusa.**
 * Ele falha alto com `require`, então uma tela que deixasse passar 7 semanas ou uma
 * baseline maior que o alvo derrubaria o app em vez de mostrar uma mensagem.
 */

/** O mínimo que a grade comporta (docs/03 §3.5). É o mesmo `require` do gerador. */
private const val SEMANAS_MINIMAS = 8

/**
 * O que o formulário acusa, campo a campo.
 *
 * **Os quatro saem juntos**, e não um por vez: acusar em série faria o usuário descobrir
 * o terceiro erro no terceiro toque no botão. Toda mensagem é id de recurso, nunca texto
 * — microcopy mora em `strings.xml`, que é onde a varredura de `F1-T20` olha.
 */
data class ErrosDoPlano(
    @param:StringRes val nome: Int? = null,
    @param:StringRes val data: Int? = null,
    @param:StringRes val alvo: Int? = null,
    @param:StringRes val baseline: Int? = null,
) {
    val algum: Boolean get() = nome != null || data != null || alvo != null || baseline != null
}

/** O resultado de [validarRascunho]: ou os campos convertidos, ou o que falta neles. */
sealed interface ValidacaoDoPlano {

    /**
     * Tudo válido, e já convertido: é isto que a rota `RevisarRascunho` carrega.
     *
     * [semanas] vem junto porque a tela mostra *"21 semanas até a prova"* antes de
     * gerar, e recalcular a mesma conta na hora de desenhar abriria a porta para as
     * duas divergirem.
     */
    data class Ok(
        val nome: String,
        val dataProva: LocalDate,
        val alvoKm: Double,
        val baselineKm: Double,
        val semanas: Int,
    ) : ValidacaoDoPlano

    data class Falhou(val erros: ErrosDoPlano) : ValidacaoDoPlano
}

/**
 * As três validações de docs/03 §3.5, mais a forma dos dois campos de distância.
 *
 * ### A contagem parte da segunda-feira da semana corrente
 *
 * E não do dia em que a pessoa está preenchendo (RN-01, docs/01 §3.2). Num domingo a
 * diferença é de **seis dias**, quase uma semana inteira do denominador: contando de
 * hoje, uma prova a 8 semanas de distância seria recusada por "mínimo de 8 semanas",
 * numa tela que a spec manda aceitar. Quem responde é [GeradorDePlano], que é o mesmo
 * que vai gerar a grade — duas leituras da palavra "início" é o defeito que isso evita.
 *
 * ### Data no futuro é a primeira porta, e o mínimo de semanas é a segunda
 *
 * As duas caem no mesmo campo e a mensagem é diferente: *"escolha uma data"* não ajuda
 * quem escolheu a semana que vem. A ordem importa — sem a primeira, uma prova no ano
 * passado passaria pela contagem com número negativo.
 *
 * @param dataProva nulo é *"ainda não respondeu"*: o `DatePicker` abre vazio, e tratar
 *   nulo como hoje inventaria uma resposta que o usuário não deu.
 * @param hoje o relógio, por parâmetro. É o que torna esta função testável.
 */
internal fun validarRascunho(
    nome: String,
    dataProva: LocalDate?,
    alvo: String,
    baseline: String,
    hoje: LocalDate,
): ValidacaoDoPlano {
    val nomeLimpo = nome.trim().ifBlank { null }
    val alvoKm = distanciaEmKm(alvo)
    val baselineKm = distanciaEmKm(baseline)
    val semanas = dataProva?.let {
        GeradorDePlano.contarSemanas(GeradorDePlano.primeiraSegundaDe(hoje), it)
    }

    val erros = ErrosDoPlano(
        nome = R.string.criar_erro_nome.takeIf { nomeLimpo == null },
        data = when {
            dataProva == null -> R.string.criar_erro_data_ausente
            !dataProva.isAfter(hoje) -> R.string.criar_erro_data_passada
            semanas!! < SEMANAS_MINIMAS -> R.string.criar_erro_data_curta
            else -> null
        },
        alvo = when {
            alvoKm == null -> R.string.criar_erro_distancia
            // A comparação só faz sentido com as duas lidas. Com a baseline fora de
            // forma, o campo dela já está aceso e acender este também seria acusar o
            // usuário de um erro que ele não cometeu.
            baselineKm != null && alvoKm <= baselineKm -> R.string.criar_erro_alvo_menor
            else -> null
        },
        baseline = R.string.criar_erro_distancia.takeIf { baselineKm == null },
    )

    if (erros.algum) return ValidacaoDoPlano.Falhou(erros)

    return ValidacaoDoPlano.Ok(
        nome = nomeLimpo!!,
        dataProva = dataProva!!,
        alvoKm = alvoKm!!,
        baselineKm = baselineKm!!,
        semanas = semanas!!,
    )
}

/**
 * Quantas semanas o plano teria com [dataProva], ou **nulo** enquanto a data não serve.
 *
 * É o número que a tela mostra antes de gerar — o único dado do formulário que o usuário
 * não tem como estimar de cabeça, e o que muda quando ele mexe no calendário.
 *
 * **Não é [validarRascunho] com valores de mentira.** A primeira versão chamava a
 * validação inteira passando `alvo = "1"` e `baseline = "1"` só para preencher os campos
 * que não importam aqui — e aqueles dois valores violam a validação de alvo maior que a
 * distância confortável, então a função devolvia nulo sempre e a linha nunca aparecia na
 * tela. O emulador pegou; nenhum teste pegaria, porque nenhum teste pedia o número.
 *
 * Nulo cobre os três casos em que não há o que mostrar: data não escolhida, data no
 * passado e plano curto demais. Nos dois últimos quem fala é a mensagem de erro do
 * campo, e um número ao lado dela seria ruído.
 */
internal fun semanasAte(hoje: LocalDate, dataProva: LocalDate?): Int? {
    if (dataProva == null || !dataProva.isAfter(hoje)) return null
    val total = GeradorDePlano.contarSemanas(GeradorDePlano.primeiraSegundaDe(hoje), dataProva)
    return total.takeIf { it >= SEMANAS_MINIMAS }
}
