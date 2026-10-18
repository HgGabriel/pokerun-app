package com.hggabriel.pokerun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Os 14 estilos do design system, um por token de `type`.
 *
 * O Quicksand nunca fica abaixo de 500 em 16sp nem de 600 em 14sp ou menos: o traço
 * fino some sob sol forte em peso 400. `rotulo` vai em caixa alta, aplicada no
 * componente.
 */
object Estilos {
    // ---- Números: Poppins 700 ----------------------------------------------

    /**
     * O número da placa de peito da Home, e só ele (docs/02 §4.5). Um no app. Acima de
     * fontScale 1,5 cai para [numeroXl]. Não tem papel do M3: é lido pelo nome.
     */
    val numeroPeito = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-0.03).em,
    )

    /** O número principal da tela: um por tela. Acima de fontScale 1,5 cai para [numeroL]. */
    val numeroXl = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    )

    /** Número de destaque secundário: XP total, posição na Pokédex. */
    val numeroL = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em,
    )

    /** Métrica em card: pace, tempo, distância, fração de sessões. */
    val numeroM = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    )

    /** Valor do bloco de ficha da corrida, XP e posição no ranking. */
    val numeroS = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    // ---- Títulos: Poppins 700 ----------------------------------------------

    /** Título de conteúdo da tela, no cabeçalho de ficha. */
    val tituloTela = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )

    /** Nome da espécie no card e no detalhe. */
    val nomeEspecie = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )

    /** Título de card e de seção. */
    val tituloSecao = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    // ---- Texto: Quicksand, com três estilos em Poppins -----------------------

    /** Texto corrido, estado vazio, valor de campo. */
    val corpo = TextStyle(
        fontFamily = FamiliaTexto,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    /** Nome de membro, linha principal de lista. */
    val corpoForte = TextStyle(
        fontFamily = FamiliaTexto,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    /** Texto secundário, espécie no ranking, nome no item da Pokédex. */
    val apoio = TextStyle(
        fontFamily = FamiliaTexto,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    /** Todo botão e chip interativo. */
    val botao = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /** Sobrancelha do cabeçalho e rótulo de dado, em caixa alta. No máximo 2 linhas. */
    val rotulo = TextStyle(
        fontFamily = FamiliaDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    )

    /** Rótulo da barra inferior, número no item da Pokédex, tipo no chip pequeno. O piso do sistema. */
    val legenda = TextStyle(
        fontFamily = FamiliaTexto,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
}

/**
 * **Os 15 papéis, todos preenchidos.** O `Typography` do Compose completa em
 * silêncio o que não recebe, com Roboto na escala Material: não gera erro, não
 * aparece em revisão de diff, aparece na tela (docs/02 §3.2). O mais visível seria
 * `labelLarge`: sem ele todo botão do app renderiza em Roboto.
 *
 * Cada papel recebe o estilo do design system que serve aos componentes do M3 que o
 * consomem. [Estilos.nomeEspecie] e [Estilos.numeroS] não têm papel e são lidos pelo
 * nome.
 */
val Typography = Typography(
    // Distância da corrida e o degrau da placa de peito em fontScale alto; o de baixo é o seguinte.
    displayLarge = Estilos.numeroXl,
    displayMedium = Estilos.numeroL,
    displaySmall = Estilos.numeroM,

    // `DatePicker` (headlineLarge) e título de `AlertDialog` (headlineSmall).
    headlineLarge = Estilos.tituloTela,
    headlineMedium = Estilos.tituloTela,
    headlineSmall = Estilos.tituloTela,

    // Título do cabeçalho de ficha (docs/02 §10.1).
    titleLarge = Estilos.tituloTela,
    // Título de card e de seção.
    titleMedium = Estilos.tituloSecao,
    // Abas internas e linha principal de lista.
    titleSmall = Estilos.corpoForte,

    // Entrada de campo e headline de `ListItem`.
    bodyLarge = Estilos.corpo,
    // Texto do `Snackbar` e do `AlertDialog`, supporting de `ListItem`.
    bodyMedium = Estilos.apoio,
    // Texto de apoio de campo.
    bodySmall = Estilos.legenda,

    // Todo botão, chip e segmented button.
    labelLarge = Estilos.botao,
    // Rótulo de dado e sobrancelha do cabeçalho de ficha.
    labelMedium = Estilos.rotulo,
    // Badge e overline de `ListItem`.
    labelSmall = Estilos.legenda,
)

/**
 * Valor de dado: splits, tempos, pace, bpm, número da Pokédex.
 *
 * É [Estilos.numeroS]. O nome fica porque cerca de 15 telas e componentes o importam.
 */
val EstiloDado = Estilos.numeroS
