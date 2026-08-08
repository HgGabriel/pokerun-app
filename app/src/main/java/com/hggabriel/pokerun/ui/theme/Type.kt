package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hggabriel.pokerun.R

/**
 * Hierarquia por LARGURA, não só por peso: display expandido contra corpo normal.
 *
 * As três famílias são locais, em `res/font`, e **nunca vão à rede**: Downloadable
 * Fonts está fora pela mesma lógica de D-10, e é pior que a PokéAPI porque uma
 * fonte que não chega não degrada — ela troca o app inteiro de tipo no primeiro
 * quadro (docs/02 §3.1).
 *
 * Quatro arquivos, e não os seis que a ficha de `F0-T08` previa: no `google/fonts`
 * a IBM Plex Sans só existe como variável, então um arquivo cobre 400, 500 e 600
 * por `FontVariation.weight`. A Plex Mono tem estáticas e são duas. Subconjunto
 * latino aplicado com `fontTools`, com os eixos e as features preservados: 644 KB
 * contra 1,4 MB das fontes cheias.
 */

/**
 * Display: Archivo no **máximo do eixo `wdth`**, 125.
 *
 * O valor não é negociável e não tem meio-termo (docs/02 §3.1): abaixo de ~112,5 a
 * expansão deixa de ser lida como largura e vira "negrito um pouco estranho", e a
 * tese de hierarquia por largura — que é a identidade do projeto — colapsa. Ou a
 * expansão é evidente, ou não vale a fonte extra.
 *
 * O opt-in de [ExperimentalTextApi] é de `Font(..., variationSettings = ...)`, que
 * ainda não estabilizou. Ele fica nas duas famílias que usam a API, e não no
 * arquivo inteiro, para que o dia em que ela estabilizar apague duas linhas em vez
 * de esconder o próximo opt-in que alguém acrescentar aqui. O plano B de
 * docs/02 §3.1 — empacotar instâncias estáticas — dispensaria a anotação e custaria
 * mais arquivo e mais APK.
 */
@OptIn(ExperimentalTextApi::class)
private val DisplayFamily = FontFamily(
    Font(
        resId = R.font.archivo_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.width(125f),
            FontVariation.weight(700),
        ),
    ),
)

/**
 * Corpo e UI: IBM Plex Sans nos três pesos, todos do mesmo arquivo variável.
 *
 * O `weight` de cada [Font] é o que o Compose casa contra o `fontWeight` do estilo;
 * o `variationSettings` é o que instancia o desenho. Os dois têm de concordar — se
 * divergirem, o Compose escolhe o arquivo por um número e desenha outro.
 */
@OptIn(ExperimentalTextApi::class)
private val CorpoFamily = FontFamily(
    Font(
        resId = R.font.ibm_plex_sans_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.ibm_plex_sans_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.ibm_plex_sans_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

/** Dado tabular: o irmão desenhado do sans, com os números alinhando em coluna. */
private val DadoFamily = FontFamily(
    Font(resId = R.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
)

/**
 * Figuras tabulares no display (docs/02 §3.3). Sem isto, `111` é bem mais estreito
 * que `888` e a contagem regressiva treme ao virar o dia. A Plex Mono não precisa:
 * ela já é monoespaçada.
 */
private const val TABULAR = "tnum"

/**
 * **Os 15 papéis, todos preenchidos.** O `Typography` do Compose completa em
 * silêncio o que não recebe, com Roboto na escala Material: não gera erro, não
 * aparece em revisão de diff, aparece na tela (docs/02 §3.2). O mais visível seria
 * `labelLarge` — sem ele todo botão do app renderiza em Roboto enquanto o texto ao
 * redor vai em Plex Sans.
 */
val Typography = Typography(
    // ---- Display: Archivo Expandido 700 ----------------------------------
    // Contagem regressiva, posição na Pokédex, distância principal.
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontFeatureSettings = TABULAR,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontFeatureSettings = TABULAR,
    ),
    // `displaySmall` é o único papel que a tabela de docs/02 §3.2 não nomeia.
    // Colapsado sobre o Display L em vez de ganhar um tamanho novo, que é o que o
    // próprio documento faz com `headlineMedium` e `headlineLarge`. Inventar um
    // degrau intermediário seria criar escala que ninguém especificou.
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontFeatureSettings = TABULAR,
    ),

    // ---- Heading: Plex Sans 600 ------------------------------------------
    // Os três no mesmo degrau: o app tem um tamanho de heading, e os dois grandes
    // existem só porque o DatePicker da CreatePlanScreen os consome.
    headlineLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),

    // ---- Title ------------------------------------------------------------
    // Título de tela, e o título do cabeçalho de ficha (docs/02 §10.1).
    titleLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // Headline de `ListItem`.
    titleMedium = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // Abas internas da `RunHistoryScreen`.
    titleSmall = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // ---- Body -------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Texto de apoio de formulário.
    bodySmall = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),

    // ---- Label ------------------------------------------------------------
    // Todo botão, chip e segmented button. É Plex Sans, não mono: o mono é a voz
    // do dado, e um botão não é dado.
    labelLarge = TextStyle(
        fontFamily = CorpoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Rótulo de dado, sobrancelha do cabeçalho de ficha e item da barra inferior.
    // Mono por escolha, não por acidente: reforça o instrumento na única navegação
    // persistente do app (docs/02 §3.2). A caixa alta se aplica no componente.
    labelMedium = TextStyle(
        fontFamily = DadoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.96.sp,  // +0.08em
    ),
    // Badge e overline de `ListItem`. O tamanho vem de docs/02 §3.2; o peso e o
    // tracking seguem a banda Label/Data da escala, porque overline também é caixa
    // alta e o documento prende o tracking à caixa alta, não ao tamanho.
    labelSmall = TextStyle(
        fontFamily = DadoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.88.sp,  // +0.08em
    ),
)

/** Dado tabular: splits, tempos, pace, bpm, número da Pokédex. */
val EstiloDado = TextStyle(
    fontFamily = DadoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)
