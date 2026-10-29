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
 * O compasso, as durações e as curvas do design system (docs/02 §6).
 *
 * **Todo movimento do app anda no mesmo compasso (29/10/2026).** A unidade é a [Tempo.BATIDA],
 * de 120ms, e há só dois tempos de animação: [Tempo.PADRAO] (2 batidas) para tudo que entra,
 * sai, troca ou desliza, e [Tempo.LENTO] (4 batidas) para o que precisa ser visto acontecendo.
 * Entrada e saída duram o mesmo; muda só a curva. Item que entra em sequência espera uma batida
 * depois do anterior. Mola sem duração não entra: cada uma terminava num tempo próprio, e a tela
 * parecia um monte de peças se mexendo cada uma na sua hora.
 *
 * Nada anima em loop, a não ser o loader, o pulso da espera (`Modifier.esqueleto`) e os loops
 * que são o próprio dado (o chão no pace e o coração no bpm da arena). Os dois primeiros duram
 * batidas inteiras. Com movimento reduzido tudo vai direto ao estado final: [especificacao]
 * devolve `snap()` em vez de `tween`.
 *
 * O sinal de movimento reduzido é `Settings.Global.ANIMATOR_DURATION_SCALE == 0`:
 * `LocalAccessibilityManager` não informa essa preferência. [PokerunTheme] lê a escala
 * uma vez e a expõe por [LocalMovimentoReduzido].
 */
object Movimento {

    /** O compasso, em milissegundos. */
    object Tempo {
        /** A unidade. A resposta ao toque e o intervalo entre itens de uma sequência. */
        const val BATIDA = 120
        /** O tempo de tudo: entrar, sair, trocar de cor ou de estado, deslizar a tela, o pop. */
        const val PADRAO = 2 * BATIDA
        /** O segundo tempo, para o que precisa ser visto: encher, girar, acordar, cair. */
        const val LENTO = 4 * BATIDA

        /** [n] batidas, para sequências e loops que precisam caber no compasso. */
        fun batidas(n: Int): Int = n * BATIDA
    }

    /** As durações com nome próprio, todas em batidas. */
    object Duracao {
        /** Camada de pressionado e escala de 0,97 no toque. */
        const val TOQUE = Tempo.BATIDA
        /** Um ciclo do balanço da Pokébola: 10 batidas. */
        const val LOADER = 10 * Tempo.BATIDA
        /**
         * O clique do cronômetro na abertura, depois que o app ficou pronto: a coroa desce e
         * volta, e o botão de volta afunda. A captura dispara com o botão no fundo, e a volta
         * dele corre junto com o pop.
         */
        const val CLIQUE = Tempo.LENTO
        /**
         * Teto das molas da abertura, do pop à primeira tela. Somado a [CLIQUE], é o que a
         * abertura acrescenta ao tempo que o app já leva para ficar pronto.
         */
        const val ABERTURA = 800
    }

    object Curva {
        /** Transições de estado. `cubic-bezier(0.2, 0, 0, 1)`. */
        val padrao: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        /** Saída e dispensa. `cubic-bezier(0.3, 0, 1, 1)`. */
        val saida: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
        /** Pop do marcador de captura e do sprite revelado. `cubic-bezier(0.34, 1.56, 0.64, 1)`. */
        val captura: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    }

    /**
     * O movimento da troca de rota (`docs/design-system/transicoes.md`). Tudo desliza a
     * largura ou a altura inteira, na ida e na volta, em [DURACAO].
     */
    object Navegacao {
        /** `duracao-navegacao`: o deslize de uma tela inteira, na ida e na volta. É o [Tempo.PADRAO]. */
        const val DURACAO = Tempo.PADRAO
        /** `recuo-navegacao`: a fração da largura que a tela de baixo recua quando outra a cobre. */
        const val RECUO = 0.25f
    }

    /** O pacote e a cascata do álbum da Pokédex (docs/03 §3.18). */
    object Album {
        /** O intervalo entre uma figurinha e a seguinte quando o pacote abre: uma batida. */
        const val ENTRE_FIGURINHAS = Tempo.BATIDA
        /**
         * Uma figurinha colando, da entrada no ar ao assentar: [Tempo.LENTO]. É coisa para ser
         * vista, e com uma batida entre elas as colagens se sobrepõem em onda.
         */
        const val COLAR = Tempo.LENTO
        /** A cascata de estreia inteira, da primeira figurinha à última assentada: 16 batidas. */
        const val CASCATA = 16 * Tempo.BATIDA

        /**
         * Quando a figurinha [indice] de [total] começa a cair na cascata: espalhadas por
         * igual, para a última assentar em [CASCATA] com a colagem de [COLAR].
         */
        fun atrasoNaCascata(indice: Int, total: Int): Int =
            if (total <= 1) 0 else (indice.toLong() * (CASCATA - COLAR) / (total - 1)).toInt()
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

    /** Entrar, trocar de cor ou de estado: [Tempo.PADRAO] em `curva-padrao`. */
    fun <T> padrao(reduzido: Boolean = false): FiniteAnimationSpec<T> = especificacao(Tempo.PADRAO, Curva.padrao, reduzido)

    /** Sair: o mesmo [Tempo.PADRAO], em `curva-saida`. */
    fun <T> saida(reduzido: Boolean = false): FiniteAnimationSpec<T> = especificacao(Tempo.PADRAO, Curva.saida, reduzido)

    /** O pop que passa do alvo e assenta: [Tempo.PADRAO] em `curva-captura`. Substitui a mola. */
    fun <T> pop(reduzido: Boolean = false): FiniteAnimationSpec<T> = especificacao(Tempo.PADRAO, Curva.captura, reduzido)

    /** O que precisa ser visto acontecendo: [Tempo.LENTO] em [curva]. */
    fun <T> lento(reduzido: Boolean = false, curva: Easing = Curva.padrao): FiniteAnimationSpec<T> =
        especificacao(Tempo.LENTO, curva, reduzido)
}

/** Se o sistema pede movimento reduzido. [PokerunTheme] provê o valor lido do aparelho. */
val LocalMovimentoReduzido = staticCompositionLocalOf { false }
