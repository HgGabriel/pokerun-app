package com.hggabriel.pokerun.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.hggabriel.pokerun.R

/**
 * Duas famílias: **Poppins** (`display`) para números, títulos, botões e rótulos, e
 * **Quicksand** (`texto`) para texto corrido, apoio e legenda (docs/02 §3).
 *
 * As duas são locais, em `res/font`, e **nunca vão à rede**: Downloadable Fonts está
 * fora porque uma fonte que não chega não degrada, ela troca o app inteiro de tipo
 * no primeiro quadro. Subconjunto latino aplicado com `fontTools`, com eixos e
 * features preservados. As licenças OFL ficam em `res/raw`.
 *
 * Nenhuma das duas tem algarismos tabulares. Número em coluna se alinha à direita no
 * componente, e não por `fontFeatureSettings`.
 *
 * As famílias moram fora de Type.kt de propósito. No mesmo arquivo, `Estilos` e
 * `Typography` se inicializariam em ciclo pela classe `TypeKt`, e os papéis nasceriam
 * nulos sem erro de compilação.
 */

/** Poppins em quatro arquivos estáticos: o Google Fonts não publica a Poppins variável. */
internal val FamiliaDisplay = FontFamily(
    Font(resId = R.font.poppins_regular, weight = FontWeight.Normal),
    Font(resId = R.font.poppins_medium, weight = FontWeight.Medium),
    Font(resId = R.font.poppins_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.poppins_bold, weight = FontWeight.Bold),
)

/**
 * Quicksand nos cinco pesos do eixo `wght` (300 a 700), todos do mesmo arquivo variável.
 *
 * O `weight` de cada [Font] é o que o Compose casa contra o `fontWeight` do estilo; o
 * `variationSettings` é o que instancia o desenho. Os dois têm de concordar: se
 * divergirem, o Compose escolhe o arquivo por um número e desenha outro.
 *
 * O opt-in de [ExperimentalTextApi] é de `Font(..., variationSettings = ...)`, que
 * ainda não estabilizou. Ele fica só nesta família, para que o dia em que a API
 * estabilizar apague uma linha em vez de esconder o próximo opt-in deste arquivo.
 */
@OptIn(ExperimentalTextApi::class)
internal val FamiliaTexto = FontFamily(
    listOf(300, 400, 500, 600, 700).map { peso ->
        Font(
            resId = R.font.quicksand_variable,
            weight = FontWeight(peso),
            variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
        )
    },
)
