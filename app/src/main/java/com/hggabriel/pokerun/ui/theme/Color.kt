package com.hggabriel.pokerun.ui.theme

import androidx.compose.ui.graphics.Color
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon

/**
 * Tokens de cor do PokéRun: interface branca, tinta quase preta e vermelho Pokébola
 * (docs/02 §2). Os valores vêm do `tokens.json` do design system e só mudam lá.
 *
 * Tema claro fixo (D-13). Não existe variante escura nem cor dinâmica.
 *
 * Contraste medido (WCAG 2.1) sobre branco e sobre [SuperficieAfundada]. Todo token
 * de texto passa AA (4,5:1) nas duas, menos [Destaque], que só vira texto sobre
 * branco (docs/02 §2.2).
 *
 * NENHUMA TELA LÊ UM `val` DAQUI. O acesso é `MaterialTheme.colorScheme.*`,
 * `LocalCoresPokerun.current.*` ou `LocalCoresDeTipo.current[tipo]`. Há teste que
 * falha se um import escapar.
 */

// --- Superfícies -------------------------------------------------------------

/** Fundo de toda tela. Nunca recebe cor de tipo. */
val Fundo = Color(0xFFFFFFFF)

/** Cards, campos, barra inferior, linhas de lista. Mesmo branco: o card se delimita pela [Borda]. */
val Superficie = Color(0xFFFFFFFF)

/** Cinza-azulado frio: trilha de barra, skeleton, espécie não alcançada, dia sem corrida. */
val SuperficieAfundada = Color(0xFFF0F3F8)

/** Divisores e contorno decorativo de card. 1,24:1. Nunca é o único limite de um controle. */
val Borda = Color(0xFFE2E7EF)

/** Contorno de campo, botão secundário e segmento pendente. 4,51:1 / 4,06:1 (WCAG 1.4.11). */
val BordaControle = Color(0xFF6E7787)

// --- Tinta -------------------------------------------------------------------

/** Texto, números, ícone ativo e preenchimento neutro de barra. 18,04:1 / 16,22:1. */
val TextoPrimario = Color(0xFF14161F)

/** Rótulo, unidade, apoio e ícone inativo. 7,85:1 / 7,06:1. */
val TextoSecundario = Color(0xFF4A5262)

/** Preenchimento chapado da silhueta, pela máscara do próprio sprite. Aponta para [TextoSecundario]. */
val Silhueta = TextoSecundario

/** Anel de foco: 2dp sólido, deslocado 2dp. Aponta para [TextoPrimario]. */
val Foco = TextoPrimario

// --- Destaque: você e a sua ação ----------------------------------------------

/**
 * Vermelho Pokébola. Ação principal, progresso do usuário e a marca. 4,89:1 sobre
 * branco. Como texto, só sobre branco: sobre [SuperficieAfundada] mede 4,39:1.
 */
val Destaque = Color(0xFFDD1F26)

/** Botão primário pressionado. Branco sobre ele: 6,23:1. */
val DestaquePressionado = Color(0xFFC40218)

/** Texto e ícone sobre [Destaque] (4,89:1) e [DestaquePressionado] (6,23:1). */
val TextoSobreDestaque = Color(0xFFFFFFFF)

/** Linha "você" e destino ativo da barra inferior. Texto sobre ele em [TextoPrimario] (15,47:1). */
val DestaqueSuave = Color(0xFFFEE9E6)

// --- Estados funcionais ------------------------------------------------------

/**
 * Risco de lesão e erro. 11,11:1 sobre branco, 9,72:1 sobre [AlertaSuave].
 *
 * Separado de [Destaque] por valor, não só por matiz: ΔE 59,9 na visão normal, 32,8
 * em deuteranopia e 13,8 em protanopia (Viénot 1999). Cor nunca é o único canal do
 * aviso: vem sempre com triângulo e rótulo em caixa alta (docs/02 §2.4).
 */
val Alerta = Color(0xFF5A3200)

/** Fundo do bloco de alerta. [TextoPrimario] sobre ele: 15,78:1. */
val AlertaSuave = Color(0xFFFEEDDD)

/** Confirmação, sempre com check. 5,39:1 sobre branco, 4,88:1 sobre [SucessoSuave]. */
val Sucesso = Color(0xFF0B7A4B)

/** Fundo do bloco de sucesso. */
val SucessoSuave = Color(0xFFDAFCE6)

// --- Camadas -----------------------------------------------------------------

/** Camada de toque neutra: [TextoPrimario] a 8%, no retângulo do alvo. */
val EstadoPressionado = TextoPrimario.copy(alpha = 0.08f)

/**
 * Fundo de bottom sheet e diálogo: [TextoPrimario] a 55%.
 *
 * Os componentes do M3 trocam o alfa por conta própria (`scrim.copy(alpha = 0.32f)`):
 * quem quiser os 55% passa `colorScheme.scrim` explicitamente.
 */
val Scrim = TextoPrimario.copy(alpha = 0.55f)

/** Traço do padrão de Pokébolas de fundo: [TextoPrimario] a 5%. */
val PadraoPokebola = TextoPrimario.copy(alpha = 0.05f)

// --- Os 18 tipos (docs/02 §2.5) ----------------------------------------------
//
// base: chip e ícone de tipo. sobre: texto e ícone sobre a base. suave: fundo da área
// do sprite. As bases ficam em duas faixas de luminância: Y ≤ 0,16 recebe branco e
// Y ≈ 0,25 recebe tinta. Nenhuma cai em 0,18–0,23, onde nem um nem outro passa 4,5:1.

private val SobreClaro = Color(0xFFFFFFFF)
private val SobreEscuro = Color(0xFF14161F)

internal val CoresDeTipoClaro = CoresDeTipo(
    mapOf(
        TipoDePokemon.NORMAL to CorDeTipo(Color(0xFF8B8B67), SobreEscuro, Color(0xFFF1F2DC)),
        TipoDePokemon.FOGO to CorDeTipo(Color(0xFFD86A0C), SobreEscuro, Color(0xFFFFEBE0)),
        TipoDePokemon.AGUA to CorDeTipo(Color(0xFF4367C4), SobreClaro, Color(0xFFDDE8FF)),
        TipoDePokemon.PLANTA to CorDeTipo(Color(0xFF347301), SobreClaro, Color(0xFFCFF5BF)),
        TipoDePokemon.ELETRICO to CorDeTipo(Color(0xFFA48705), SobreEscuro, Color(0xFFFFF0BD)),
        TipoDePokemon.GELO to CorDeTipo(Color(0xFF559394), SobreEscuro, Color(0xFFD4F6F7)),
        TipoDePokemon.LUTADOR to CorDeTipo(Color(0xFF9F0308), SobreClaro, Color(0xFFFEE0DB)),
        TipoDePokemon.VENENOSO to CorDeTipo(Color(0xFF9D3D9E), SobreClaro, Color(0xFFFEDBFD)),
        TipoDePokemon.TERRESTRE to CorDeTipo(Color(0xFF886C04), SobreClaro, Color(0xFFF7E7BA)),
        TipoDePokemon.VOADOR to CorDeTipo(Color(0xFF947BD9), SobreEscuro, Color(0xFFF2EFFE)),
        TipoDePokemon.PSIQUICO to CorDeTipo(Color(0xFFCA2862), SobreClaro, Color(0xFFFEDEE4)),
        TipoDePokemon.INSETO to CorDeTipo(Color(0xFF849204), SobreEscuro, Color(0xFFECF7BB)),
        TipoDePokemon.PEDRA to CorDeTipo(Color(0xFF615101), SobreClaro, Color(0xFFF1E8C3)),
        TipoDePokemon.FANTASMA to CorDeTipo(Color(0xFF5E4685), SobreClaro, Color(0xFFEBE3FF)),
        TipoDePokemon.DRAGAO to CorDeTipo(Color(0xFF6C32F3), SobreClaro, Color(0xFFE6E5FE)),
        TipoDePokemon.SOMBRIO to CorDeTipo(Color(0xFF5E3E28), SobreClaro, Color(0xFFF9E3D5)),
        TipoDePokemon.ACO to CorDeTipo(Color(0xFF8786A4), SobreEscuro, Color(0xFFEEEEFF)),
        TipoDePokemon.FADA to CorDeTipo(Color(0xFFC27185), SobreEscuro, Color(0xFFFFEAEE)),
    ),
)
