package com.hggabriel.pokerun.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Durações e curvas do design system (docs/02 §6).
 *
 * Nada anima em loop, a não ser o loader. Com movimento reduzido tudo vai direto ao
 * estado final: [especificacao] devolve `snap()` em vez de `tween`.
 *
 * O sinal de movimento reduzido é `Settings.Global.ANIMATOR_DURATION_SCALE == 0`:
 * `LocalAccessibilityManager` não informa essa preferência. [PokerunTheme] lê a escala
 * uma vez e a expõe por [LocalMovimentoReduzido].
 */
object Movimento {

    /** Durações em milissegundos. */
    object Duracao {
        /** Camada de pressionado e escala de 0,97 no toque. */
        const val TOQUE = 100
        /** Troca de destino na barra inferior, seleção de chip. */
        const val CURTA = 160
        /** Pop do marcador de captura, silhueta revelando cor. */
        const val MEDIA = 280
        /** Barra de XP e barra do desafio preenchendo ao mudar de valor. */
        const val PREENCHIMENTO = 560
        /** Um ciclo do balanço da Pokébola. */
        const val LOADER = 1200
        /**
         * A splash do sistema: espera a janela aparecer, e a coroa e o botão de volta do
         * cronômetro clicam uma vez. Vive na AVD (`splash_icone_animado.xml`) e em
         * `windowSplashScreenAnimationDuration`. É o teto do Android para o ícone animado.
         */
        const val SPLASH = 1000

        /** A saída e a dispensa duram 60% da entrada. */
        fun saida(entrada: Int): Int = entrada * 6 / 10
    }

    object Curva {
        /** Transições de estado. `cubic-bezier(0.2, 0, 0, 1)`. */
        val padrao: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        /** Saída e dispensa. `cubic-bezier(0.3, 0, 1, 1)`. */
        val saida: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
        /** Pop do marcador de captura e do sprite revelado. `cubic-bezier(0.34, 1.56, 0.64, 1)`. */
        val captura: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    }

    /** `true` quando o sistema pede movimento reduzido: escala de animação zero. */
    fun reduzido(escalaDoAnimador: Float): Boolean = escalaDoAnimador == 0f

    /**
     * A escala de animação do sistema. Devolve 1 quando não consegue ler, que é o
     * padrão do aparelho e o que o preview do Android Studio precisa.
     */
    fun escalaDoAnimador(contexto: Context): Float = runCatching {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)

    /** A animação de [duracao] ms em [curva], ou o salto direto ao fim com movimento reduzido. */
    fun <T> especificacao(duracao: Int, curva: Easing, reduzido: Boolean): FiniteAnimationSpec<T> =
        if (reduzido) snap() else tween(durationMillis = duracao, easing = curva)
}

/** Se o sistema pede movimento reduzido. [PokerunTheme] provê o valor lido do aparelho. */
val LocalMovimentoReduzido = staticCompositionLocalOf { false }
