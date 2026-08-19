package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/*
 * As validações do registro manual (`F1-T16`, docs/03 §3.10, RN-04 e RN-28).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `CamposDoPlano.kt` e de
 * `PainelDeHoje.kt`: são a parte da tela que se prova sem aparelho — o relógio e o
 * fuso chegam por parâmetro — e são as que a revisão de olho deixa passar.
 *
 * **A corrida é append-only** (RN-24). Um valor errado que passe daqui não é
 * corrigido depois: ele vira um documento que fica, e a correção é um segundo
 * documento apontando para ele. É o que torna a validação de entrada mais cara de
 * pular aqui do que num formulário comum.
 */

/**
 * // RN-04
 *
 * Quantos dias de registro retroativo são livres. Passando deles, a gravação exige
 * confirmação explícita — o diálogo de [CorridaManualUiState.confirmando].
 *
 * **Sete é o número da regra**, e o intervalo é fechado: *"livre até 7 dias"* inclui
 * o sétimo.
 */
private const val DIAS_LIVRES = 7

/** Até dois dígitos: horas de corrida. `100` num campo de horas é dedo escorregado. */
private val FORMA_DE_HORAS = Regex("""\d{1,2}""")

/** Até dois dígitos, e o valor ainda é conferido contra 59. */
private val FORMA_DE_MINUTO_OU_SEGUNDO = Regex("""\d{1,2}""")

/**
 * Até três dígitos, a forma dos dois campos opcionais.
 *
 * É conferência de **forma**, e não de fisiologia: o app não sabe a FC máxima de quem
 * digita, e recusar 195 bpm de um jovem de 20 anos seria inventar regra que a spec não
 * tem. O que ela pega é `1520`, que é o `152` com um dedo a mais. Quem tem faixa
 * declarada na spec é o esforço percebido, e a faixa dele é [ESFORCO].
 */
private val ATE_TRES_DIGITOS = Regex("""\d{1,3}""")

/** A escala de docs/03 §3.10 e docs/05 §4.1, ao pé da letra. */
private val ESFORCO = 1..10

/**
 * O que o formulário acusa, campo a campo.
 *
 * **Saem todos juntos**, como em `ErrosDoPlano`: acusar em série faria o usuário
 * descobrir o terceiro erro no terceiro toque no botão. Toda mensagem é id de
 * recurso, nunca texto — microcopy mora em `strings.xml`, que é onde a varredura de
 * `F1-T20` olha.
 *
 * [dataHora] é **um** erro para os dois seletores porque eles respondem uma pergunta
 * só: quando o treino aconteceu. Duas linhas de alerta embaixo de dois campos que a
 * pessoa preenche em sequência diriam a mesma coisa duas vezes.
 */
data class ErrosDaCorrida(
    @param:StringRes val dataHora: Int? = null,
    @param:StringRes val km: Int? = null,
    @param:StringRes val duracao: Int? = null,
    @param:StringRes val fcMedia: Int? = null,
    @param:StringRes val esforco: Int? = null,
) {
    val algum: Boolean
        get() = dataHora != null || km != null || duracao != null ||
            fcMedia != null || esforco != null
}

/** O resultado de [validarCorrida]: ou os campos convertidos, ou o que falta neles. */
sealed interface ValidacaoDaCorrida {

    /**
     * Tudo válido e já convertido — é isto que vira o documento de `users/{uid}/runs`.
     *
     * [dataHoraInicio] já saiu do fuso do plano (RN-28), e é dele que
     * `CorridaRepositorio.registrar` deriva `semana_ref` (RN-02). Nada aqui é
     * recalculado na tela.
     */
    data class Ok(
        val dataHoraInicio: Instant,
        /**
         * O mesmo momento como data de calendário **no fuso do plano** — é a data que
         * a pessoa escolheu, e a que o diálogo de RN-04 nomeia.
         *
         * Fica aqui e não é derivada de [dataHoraInicio] na tela porque a tela não tem
         * o fuso do plano: reler o instante em UTC devolveria o dia seguinte para toda
         * corrida noturna de quem treina a oeste de Greenwich.
         */
        val dia: LocalDate,
        val km: Double,
        val duracaoSeg: Long,
        val fcMedia: Int?,
        val esforcoPercebido: Int?,
        /** // RN-04 — o registro passou dos [DIAS_LIVRES] e o diálogo tem de aparecer. */
        val exigeConfirmacao: Boolean,
    ) : ValidacaoDaCorrida

    data class Falhou(val erros: ErrosDaCorrida) : ValidacaoDaCorrida
}

/**
 * // RN-04
 *
 * Valida o formulário inteiro e diz, junto, se a gravação precisa de confirmação.
 *
 * ### A confirmação sai da mesma passada da validação, e não do botão
 *
 * RN-04 não é um erro de campo: a data de duas semanas atrás é **válida**, e o que
 * ela exige é uma pergunta a mais antes de gravar. Devolvê-la em
 * [ValidacaoDaCorrida.Ok] mantém o `ViewModel` sem nenhuma conta de data — que é o
 * que faz esta regra ter teste.
 *
 * ### Dias de calendário, no fuso do plano
 *
 * A mesma escolha de `CalendarioDoPlano`, e pelo mesmo motivo: contar em `7 × 24h`
 * diverge de "sete dias" sempre que a corrida foi de manhã e o app é aberto de
 * noite — 185 horas de distância dentro do sétimo dia —, e o sintoma seria um
 * diálogo de confirmação na frente de quem está dentro da regra.
 *
 * O fuso é o **do plano** (RN-28) dos dois lados da conta: ele interpreta a data
 * digitada e decide que dia é hoje. Uma corrida de domingo 22h em São Paulo é
 * domingo, e continua sendo depois de o corredor viajar.
 *
 * @param data nulo é *"ainda não respondeu"*, como em `F1-T10`. Tratar como hoje
 *   inventaria uma resposta, e a data decide a semana da corrida (RN-01).
 * @param hora idem, e com um agravante: meia-noite é justamente a fronteira que
 *   separa duas semanas de treino.
 * @param agora o relógio, por parâmetro. É o que torna esta função testável.
 */
internal fun validarCorrida(
    data: LocalDate?,
    hora: LocalTime?,
    km: String,
    horas: String,
    minutos: String,
    segundos: String,
    fcMedia: String,
    esforco: String,
    fuso: ZoneId,
    agora: Instant,
): ValidacaoDaCorrida {
    val quando = if (data != null && hora != null) {
        data.atTime(hora).atZone(fuso).toInstant()
    } else {
        null
    }

    val distancia = distanciaEmKm(km)
    val duracaoSeg = duracaoEmSegundos(horas, minutos, segundos)
    val frequencia = numeroOpcional(fcMedia, ATE_TRES_DIGITOS)
    val esforcoPercebido = numeroOpcional(esforco, ATE_TRES_DIGITOS)

    val erros = ErrosDaCorrida(
        dataHora = when {
            quando == null -> R.string.manual_erro_quando_ausente
            // A beirada é do lado de dentro: quem termina de correr e registra na hora
            // digita a hora que acabou de passar.
            quando.isAfter(agora) -> R.string.manual_erro_quando_futuro
            else -> null
        },
        km = R.string.manual_erro_distancia.takeIf { distancia == null },
        duracao = when {
            duracaoSeg == null -> R.string.manual_erro_duracao_forma
            duracaoSeg <= 0L -> R.string.manual_erro_duracao_zerada
            else -> null
        },
        fcMedia = R.string.manual_erro_frequencia.takeIf {
            fcMedia.isNotBlank() && (frequencia == null || frequencia <= 0)
        },
        esforco = R.string.manual_erro_esforco.takeIf {
            esforco.isNotBlank() && (esforcoPercebido == null || esforcoPercebido !in ESFORCO)
        },
    )

    if (erros.algum) return ValidacaoDaCorrida.Falhou(erros)

    return ValidacaoDaCorrida.Ok(
        dataHoraInicio = quando!!,
        dia = data!!,
        km = distancia!!,
        duracaoSeg = duracaoSeg!!,
        fcMedia = frequencia,
        esforcoPercebido = esforcoPercebido,
        exigeConfirmacao = retroativaDemais(quando, agora, fuso),
    )
}

/**
 * // RN-04
 *
 * Se [quando] está a mais de [DIAS_LIVRES] **dias de calendário** de [agora], os dois
 * lidos no fuso do plano.
 */
internal fun retroativaDemais(quando: Instant, agora: Instant, fuso: ZoneId): Boolean {
    val diaDaCorrida = quando.atZone(fuso).toLocalDate()
    val hoje = agora.atZone(fuso).toLocalDate()

    return ChronoUnit.DAYS.between(diaDaCorrida, hoje) > DIAS_LIVRES
}

/**
 * A duração em segundos, ou **nulo** quando algum dos três campos não é um número na
 * forma que o rótulo dele promete.
 *
 * **Campo vazio vale zero**, e é o caso comum: 45 minutos é `45` no campo do meio e
 * nada nos outros dois. Exigir `0` nos vazios seria pedir três respostas para um dado
 * só.
 *
 * **Minuto e segundo param em 59.** Aceitar `90` no campo de minutos e somar em
 * silêncio grava 1h30 sob um rótulo que diz outra coisa — e a corrida é append-only
 * (RN-24), então a pessoa não conserta o documento, ela grava um segundo.
 *
 * Zero total não é erro **desta** função: ela responde a forma, e quem decide que
 * corrida sem duração não existe é [validarCorrida], que tem a mensagem certa para
 * isso.
 */
internal fun duracaoEmSegundos(horas: String, minutos: String, segundos: String): Long? {
    val h = campoDeTempo(horas, FORMA_DE_HORAS, teto = 99) ?: return null
    val m = campoDeTempo(minutos, FORMA_DE_MINUTO_OU_SEGUNDO, teto = 59) ?: return null
    val s = campoDeTempo(segundos, FORMA_DE_MINUTO_OU_SEGUNDO, teto = 59) ?: return null

    return h * 3600L + m * 60L + s
}

/** Um campo de tempo: vazio é zero, fora da forma ou acima do teto é nulo. */
private fun campoDeTempo(texto: String, forma: Regex, teto: Int): Int? {
    val limpo = texto.trim()
    if (limpo.isEmpty()) return 0
    if (!forma.matches(limpo)) return null

    return limpo.toIntOrNull()?.takeIf { it <= teto }
}

/**
 * Um dos dois campos opcionais como inteiro, ou nulo — e **nulo tem dois
 * significados** que quem chama precisa separar: campo em branco, que é a resposta
 * legítima de quem não usa relógio, e texto que não é número, que é erro.
 *
 * Quem faz essa separação é [validarCorrida], olhando o texto original. Devolver um
 * tipo com três estados para dois campos opcionais custaria mais do que a condição.
 */
private fun numeroOpcional(texto: String, forma: Regex): Int? {
    val limpo = texto.trim()
    if (limpo.isEmpty() || !forma.matches(limpo)) return null

    return limpo.toIntOrNull()
}
