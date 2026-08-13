package com.hggabriel.pokerun.ui.componentes

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/*
 * Números e datas em texto (`F1-T09`).
 *
 * Nada aqui monta frase: quem junta as palavras é `strings.xml`, que é onde a varredura
 * de travessão e emoji de `F1-T20` olha. Estas funções devolvem **os pedaços** —
 * `6,2`, `agosto`, `SEG` — e a frase é o recurso de string com `%s`.
 */

/**
 * O locale das datas e dos números do app: **pt-BR fixo, e não o do aparelho**.
 *
 * A primeira versão lia `LocalConfiguration.current.locales[0]`, que é o certo num app
 * traduzido — e o emulador em inglês mostrou por que não é o certo neste: o app tem um
 * `values/strings.xml` só, em português, então a tela saía com a copy em português e o
 * mês em inglês na mesma linha (`24 A 30 DE AUGUST`), e a distância com ponto decimal
 * (`6.2 km`) num teclado que digita vírgula.
 *
 * Não há dois idiomas para escolher: há um, e o aparelho não muda isso. No dia em que
 * houver um segundo `values-xx/`, esta constante é o único lugar que precisa voltar a
 * ler a configuração.
 */
internal val LocaleDoApp: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Uma distância, sem a unidade e **sem casa decimal inútil**.
 *
 * `10 km` e não `10,0 km`; `6,2 km` e não `6,20 km`. O separador é o do locale, porque o
 * teclado decimal de um aparelho em pt-BR digita vírgula e a tela precisa devolver o que
 * a pessoa digitou.
 *
 * Uma casa é o que a distância de corrida comporta: o GPS do grupo erra mais que 100 m,
 * e a segunda casa seria precisão que o dado não tem.
 */
internal fun formatarKm(km: Double, locale: Locale = LocaleDoApp): String {
    val arredondado = Math.round(km * 10) / 10.0
    return if (arredondado == Math.floor(arredondado)) {
        String.format(locale, "%.0f", arredondado)
    } else {
        String.format(locale, "%.1f", arredondado)
    }
}

/** O nome do mês por extenso, em caixa mista: `agosto`. */
internal fun nomeDoMes(data: LocalDate, locale: Locale = LocaleDoApp): String =
    data.month.getDisplayName(TextStyle.FULL, locale)

/** O nome do dia da semana por extenso, em caixa mista: `segunda-feira`. */
internal fun nomeDoDia(dia: DayOfWeek, locale: Locale = LocaleDoApp): String =
    dia.getDisplayName(TextStyle.FULL, locale)

/**
 * O rótulo curto do dia na grade de sete quadrados: `SEG`.
 *
 * Em caixa alta porque o papel dele é o de rótulo de dado (docs/02 §3.2), e sem o ponto
 * final que o `TextStyle.SHORT` do pt-BR traz — `seg.` vira `SEG`. O ponto é abreviação
 * de prosa e não tem função numa linha de sete marcas.
 */
internal fun rotuloCurtoDoDia(dia: DayOfWeek, locale: Locale = LocaleDoApp): String =
    dia.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".").uppercase(locale)
