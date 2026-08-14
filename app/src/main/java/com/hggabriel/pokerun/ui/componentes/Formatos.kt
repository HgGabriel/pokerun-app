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

/**
 * Uma distância em quilômetros digitada num formulário, ou nulo se o que veio não é
 * uma (docs/01 §3.1).
 *
 * **Vírgula e ponto valem o mesmo**: o teclado decimal de um aparelho em pt-BR entrega
 * vírgula, e recusá-la é recusar o que o aparelho digita.
 *
 * A forma é conferida por [FORMA] antes de qualquer conversão, e não por
 * `toDoubleOrNull` sozinho, que aceita `1e3`, `Infinity` e `NaN` sem reclamar. O
 * teclado decimal não digita nenhum dos três, mas colar passa por cima do teclado — e
 * uma `baseline_km` de 1.000 gera as 21 semanas inteiras erradas, em silêncio.
 *
 * Zero também não passa: o gerador interpola **de** `baseline_km` até o alvo
 * (docs/01 §3.2), e partir de zero desfigura a grade toda.
 *
 * **Nasceu em `F1-T08` e mudou de pacote em `F1-T10`**, quando a `CreatePlanScreen`
 * passou a pedir duas distâncias pelas mesmas regras. Uma segunda cópia do regex é
 * exatamente a divergência silenciosa que ele existe para impedir: bastaria uma das
 * telas aceitar `1e3` para as 21 semanas saírem erradas por um caminho só.
 */
internal fun distanciaEmKm(texto: String): Double? {
    val limpo = texto.trim()
    if (!FORMA.matches(limpo)) return null
    val km = limpo.replace(',', '.').toDoubleOrNull() ?: return null
    return km.takeIf { it > 0.0 }
}

/**
 * Até três dígitos e até duas casas decimais.
 *
 * O teto de três dígitos não é validação de negócio inventada: é o que separa distância
 * de dedo escorregado. Nenhum ser humano responde 1.000 km à pergunta "qual a maior
 * distância que você corre hoje", e a São Silvestre tem 15.
 */
private val FORMA = Regex("""\d{1,3}([.,]\d{1,2})?""")

/**
 * Uma duração em `h:mm:ss`, ou `mm:ss` quando não chega a uma hora.
 *
 * `48:30` e não `0:48:30`: a hora só aparece quando existe, porque quase toda corrida do
 * grupo fica abaixo dela e um zero fixo à esquerda rouba a leitura do minuto. O formato é
 * o do bloco de ficha de docs/03 §3.13.1, que escreve `1:04:22`.
 *
 * Os campos internos são sempre de dois dígitos — `1:4:2` não é tempo, é três números.
 */
internal fun formatarDuracao(segundos: Long, locale: Locale = LocaleDoApp): String {
    val total = segundos.coerceAtLeast(0)
    val horas = total / 3600
    val minutos = (total % 3600) / 60
    val resto = total % 60

    return if (horas > 0) {
        String.format(locale, "%d:%02d:%02d", horas, minutos, resto)
    } else {
        String.format(locale, "%d:%02d", minutos, resto)
    }
}

/**
 * O pace em `min:seg` por quilômetro, ou **nulo** quando não há pace a mostrar.
 *
 * `min = km × pace` é a fórmula do projeto lida ao contrário: aqui o pace sai da corrida
 * gravada, e não do plano. Sem distância não existe pace, e devolver `0:00` seria um dado
 * inventado numa linha em que todos os outros são medidos — o nulo faz a tela encolher.
 *
 * **Arredonda para o segundo**, e não trunca: `5:10,6` é `5:11`. Um segundo por
 * quilômetro é meio minuto numa São Silvestre, e truncar erraria sempre para o mesmo
 * lado.
 */
internal fun formatarPace(km: Double, segundos: Long, locale: Locale = LocaleDoApp): String? {
    if (km <= 0.0 || segundos <= 0L) return null

    val porKm = Math.round(segundos / km)
    return String.format(locale, "%d:%02d", porKm / 60, porKm % 60)
}
