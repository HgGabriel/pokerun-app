package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlin.math.roundToInt
import kotlin.reflect.KClass

/*
 * As transições entre rotas, num lugar só (`docs/design-system/transicoes.md`).
 *
 * **A direção diz onde a tela mora.** Trocar de aba não desloca nada; descer um nível anda na
 * horizontal e voltar é o espelho; abrir uma tarefa sobe por cima e fechar a desce; porta de
 * entrada e o fechamento da semana só dissolvem. A regra sai da relação entre as duas
 * telas, e não da tela: nenhum `composable<...>` declara transição própria, e os dois
 * `NavHost` recebem os mesmos quatro lambdas daqui.
 *
 * **A relação é entre camadas.** Camada é o conjunto de rotas que trocam entre si pela regra
 * de aprofundar — cada aba é uma, cada fluxo modal é outro. Mudar de camada é o que dispara
 * as outras regras.
 */

internal enum class TipoDeCamada { PORTA, ABA, TAREFA, MOMENTO }

internal data class Camada(val tipo: TipoDeCamada, val nome: String)

/** Onde uma rota mora: numa camada fixa, ou na aba cujo grafo a contém. */
internal sealed interface Lugar {
    data class Fixo(val camada: Camada) : Lugar

    /** Dentro de uma aba. [foraDaAba] vale quando a mesma rota também existe no host de fora. */
    data class NaAba(val foraDaAba: Camada? = null) : Lugar
}

private val PLANOS = Camada(TipoDeCamada.TAREFA, "planos")
private val CRIACAO = Camada(TipoDeCamada.TAREFA, "criacao")

/**
 * Toda rota de `Rotas.kt`, com o lugar dela. `TransicoesDeNavegacaoTest` falha se uma rota
 * nova entrar lá sem entrar aqui.
 */
internal val LUGAR_DA_ROTA: Map<KClass<out Any>, Lugar> = mapOf(
    // As portas e a moldura: trocam entre si sem volta espacial (R4).
    Login::class to Lugar.Fixo(Camada(TipoDeCamada.PORTA, "login")),
    Onboarding::class to Lugar.Fixo(Camada(TipoDeCamada.PORTA, "onboarding")),
    Casca::class to Lugar.Fixo(Camada(TipoDeCamada.PORTA, "casca")),
    // Os grafos das abas e o que mora dentro deles.
    AbaHoje::class to Lugar.NaAba(),
    Hoje::class to Lugar.NaAba(),
    AbaProgresso::class to Lugar.NaAba(),
    Progresso::class to Lugar.NaAba(),
    AbaGrupo::class to Lugar.NaAba(),
    Grupo::class to Lugar.NaAba(),
    AbaPokedex::class to Lugar.NaAba(),
    Pokedex::class to Lugar.NaAba(),
    DetalheDaSemana::class to Lugar.NaAba(),
    DetalheDaEspecie::class to Lugar.NaAba(),
    DetalheDaCorrida::class to Lugar.NaAba(),
    Ajustes::class to Lugar.NaAba(),
    // Na aba `Hoje` pela placa de peito; no host de fora, saindo da lista de planos.
    DetalheDoPlano::class to Lugar.NaAba(foraDaAba = PLANOS),
    // Mora dentro de `Hoje`, mas é tarefa aberta pelo FAB: sobe, não aprofunda.
    CorridaManual::class to Lugar.Fixo(Camada(TipoDeCamada.TAREFA, "registro")),
    // A pilha modal.
    ListaDePlanos::class to Lugar.Fixo(PLANOS),
    CriarPlano::class to Lugar.Fixo(CRIACAO),
    RevisarRascunho::class to Lugar.Fixo(CRIACAO),
    EntrarComCodigo::class to Lugar.Fixo(Camada(TipoDeCamada.TAREFA, "codigo")),
    EditarCorrida::class to Lugar.Fixo(Camada(TipoDeCamada.TAREFA, "edicao")),
    FechamentoDaSemana::class to Lugar.Fixo(Camada(TipoDeCamada.MOMENTO, "fechamento")),
)

/** A camada de [destino]. Rota sem lugar conhecido cai numa camada própria, que dissolve (R4). */
internal fun camadaDe(destino: NavDestination): Camada {
    val lugar = LUGAR_DA_ROTA.entries.firstOrNull { destino.hasRoute(it.key) }?.value
    if (lugar is Lugar.Fixo) return lugar.camada
    val aba = DestinoDeTopo.entries.firstOrNull { topo ->
        destino.hierarchy.any { it.hasRoute(topo.grafo::class) }
    }
    return when {
        aba != null -> Camada(TipoDeCamada.ABA, aba.name)
        lugar is Lugar.NaAba && lugar.foraDaAba != null -> lugar.foraDaAba
        else -> Camada(TipoDeCamada.PORTA, destino.route ?: "desconhecida")
    }
}

/** As regras de `transicoes.md` §3, com o sentido. */
internal enum class Regra {
    /** R1: topo ↔ topo, nos dois sentidos. */
    TROCAR_DE_ABA,
    /** R2, indo. */
    APROFUNDAR,
    /** R2, voltando: o espelho. */
    VOLTAR_UM_NIVEL,
    /** R3, abrindo. */
    ABRIR_TAREFA,
    /** R3, fechando. */
    FECHAR_TAREFA,
    /** R5, abrindo. */
    ABRIR_MOMENTO,
    /** R5, fechando. */
    FECHAR_MOMENTO,
    /** R4, e o padrão neutro para o que não se encaixa. */
    PORTA,
}

/** A regra da passagem de [de] para [para]. [voltando] é o pop, inclusive o gesto. */
internal fun regraEntre(de: Camada, para: Camada, voltando: Boolean): Regra = when {
    de == para -> if (voltando) Regra.VOLTAR_UM_NIVEL else Regra.APROFUNDAR
    !voltando && para.tipo == TipoDeCamada.TAREFA -> Regra.ABRIR_TAREFA
    !voltando && para.tipo == TipoDeCamada.MOMENTO -> Regra.ABRIR_MOMENTO
    voltando && de.tipo == TipoDeCamada.TAREFA -> Regra.FECHAR_TAREFA
    voltando && de.tipo == TipoDeCamada.MOMENTO -> Regra.FECHAR_MOMENTO
    de.tipo == TipoDeCamada.ABA && para.tipo == TipoDeCamada.ABA -> Regra.TROCAR_DE_ABA
    else -> Regra.PORTA
}

/**
 * Os quatro lambdas que os `NavHost` recebem.
 *
 * **Movimento reduzido é corte seco:** toda regra vira `None`, sem quadro intermediário. O
 * gesto de voltar continua funcionando, só sem animação.
 *
 * **A tarefa fica por cima no voltar sem nada daqui:** o `NavHost` dá ao destino do pop o
 * `zIndex` da origem menos um (conferido no bytecode do Navigation 2.8.4).
 *
 * @param deslocamentoPx [Movimento.Navegacao.APROFUNDAR] em pixels.
 */
internal class TransicoesDeNavegacao(
    private val deslocamentoPx: Int,
    private val reduzido: Boolean,
) {
    val entrada: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { entrar(regra(voltando = false)) }
    val saida: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { sair(regra(voltando = false)) }
    val entradaNoVoltar: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { entrar(regra(voltando = true)) }
    val saidaNoVoltar: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { sair(regra(voltando = true)) }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.regra(voltando: Boolean): Regra =
        regraEntre(camadaDe(initialState.destination), camadaDe(targetState.destination), voltando)

    /** O que a tela que chega faz sob [regra]. */
    fun entrar(regra: Regra): EnterTransition {
        if (reduzido) return EnterTransition.None
        return when (regra) {
            Regra.TROCAR_DE_ABA -> acender(Movimento.Duracao.CURTA)
            Regra.APROFUNDAR -> slideInHorizontally(tween(IR, easing = Movimento.Curva.padrao)) { deslocamentoPx } +
                acender(IR)
            Regra.VOLTAR_UM_NIVEL -> slideInHorizontally(tween(VOLTAR, easing = Movimento.Curva.padrao)) { -deslocamentoPx } +
                acender(VOLTAR)
            Regra.ABRIR_TAREFA -> slideInVertically(tween(IR, easing = Movimento.Curva.padrao)) { altura -> camada(altura) } +
                fadeIn(tween(IR, easing = Movimento.Curva.padrao))
            Regra.ABRIR_MOMENTO, Regra.PORTA -> fadeIn(tween(IR, easing = Movimento.Curva.padrao))
            // A tela de baixo já está lá: quem anima é a tarefa ou o momento saindo por cima.
            Regra.FECHAR_TAREFA, Regra.FECHAR_MOMENTO -> EnterTransition.None
        }
    }

    /** O que a tela que sai faz sob [regra]. */
    fun sair(regra: Regra): ExitTransition {
        if (reduzido) return ExitTransition.None
        return when (regra) {
            Regra.TROCAR_DE_ABA -> esmaecer(Movimento.Duracao.CURTA)
            Regra.APROFUNDAR -> slideOutHorizontally(tween(IR, easing = Movimento.Curva.padrao)) { -deslocamentoPx } +
                esmaecer(IR)
            Regra.VOLTAR_UM_NIVEL -> slideOutHorizontally(tween(VOLTAR, easing = Movimento.Curva.padrao)) { deslocamentoPx } +
                esmaecer(VOLTAR)
            // A tela de baixo fica parada até a tarefa ou o momento cobri-la.
            Regra.ABRIR_TAREFA, Regra.ABRIR_MOMENTO -> parada(IR)
            Regra.FECHAR_TAREFA -> slideOutVertically(tween(VOLTAR, easing = Movimento.Curva.saida)) { altura -> camada(altura) } +
                fadeOut(tween(VOLTAR, easing = Movimento.Curva.saida))
            Regra.FECHAR_MOMENTO, Regra.PORTA -> fadeOut(tween(VOLTAR, easing = Movimento.Curva.saida))
        }
    }

    private fun camada(altura: Int): Int = (altura * Movimento.Navegacao.CAMADA).roundToInt()

    private companion object {
        val IR = Movimento.Duracao.MEDIA
        val VOLTAR = Movimento.Duracao.saida(Movimento.Duracao.MEDIA)

        /** A tela que sai esmaece no primeiro trecho de [duracao] (`fracao-esmaecer`). */
        fun esmaecer(duracao: Int): ExitTransition =
            fadeOut(tween(Movimento.Navegacao.esmaecer(duracao), easing = Movimento.Curva.saida))

        /** A que chega acende no restante, sem as duas se sobreporem. */
        fun acender(duracao: Int): EnterTransition {
            val atraso = Movimento.Navegacao.esmaecer(duracao)
            return fadeIn(tween(duracao - atraso, delayMillis = atraso, easing = Movimento.Curva.padrao))
        }

        /**
         * Fica inteira durante [duracao] e some no fim. É o `KeepUntilTransitionsFinished` do
         * Compose, que no animation 1.7 é `internal`: o salto atrasado dá o mesmo efeito.
         */
        fun parada(duracao: Int): ExitTransition = fadeOut(snap(delayMillis = duracao))
    }
}

/** As transições do tema atual: deslocamento na densidade do aparelho, e o movimento reduzido. */
@Composable
internal fun rememberTransicoesDeNavegacao(): TransicoesDeNavegacao {
    val reduzido = LocalMovimentoReduzido.current
    val deslocamento = with(LocalDensity.current) { Movimento.Navegacao.APROFUNDAR.roundToPx() }
    return remember(reduzido, deslocamento) { TransicoesDeNavegacao(deslocamento, reduzido) }
}
