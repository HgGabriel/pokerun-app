package com.hggabriel.pokerun.ui.navegacao

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * **Tudo desliza, e a direção diz onde a tela mora.** Trocar de aba desliza as duas telas
 * juntas, no sentido da aba na barra; descer um nível traz a tela nova pela direita sobre a
 * de baixo, que recua um quarto, e voltar é o espelho; abrir uma tarefa a sobe de baixo, e
 * fechar a desce. Nada esmaece: a revisão de 18/10 recusou o fade como linguagem de
 * navegação.
 *
 * A regra sai da relação entre as duas telas, e não da tela: nenhum `composable<...>` declara
 * transição própria, e os dois `NavHost` recebem os mesmos quatro lambdas daqui.
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
    // As portas e a moldura: trocam entre si sem volta.
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
    AlbumEncerrado::class to Lugar.NaAba(),
    EspecieEncerrada::class to Lugar.NaAba(),
    DetalheDaCorrida::class to Lugar.NaAba(),
    Perfil::class to Lugar.NaAba(),
    RitmoDoProgresso::class to Lugar.NaAba(),
    FitasDoProgresso::class to Lugar.NaAba(),
    SelosDoProgresso::class to Lugar.NaAba(),
    CorridasDoProgresso::class to Lugar.NaAba(),
    EscolherParceiro::class to Lugar.NaAba(),
    EscolherTreinador::class to Lugar.NaAba(),
    // Na aba `Hoje` pela placa de peito; no host de fora, saindo da lista de planos.
    DetalheDoPlano::class to Lugar.NaAba(foraDaAba = PLANOS),
    // Um nível abaixo do detalhe, nas mesmas duas pilhas.
    GrupoDoPlano::class to Lugar.NaAba(foraDaAba = PLANOS),
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

/** A camada de [destino]. Rota sem lugar conhecido cai numa camada própria, de porta. */
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
    /** R1: para uma aba mais à direita na barra. */
    ABA_A_DIREITA,
    /** R1: para uma aba mais à esquerda — inclusive o voltar na raiz, que cai em `Hoje`. */
    ABA_A_ESQUERDA,
    /** R2, indo. */
    APROFUNDAR,
    /** R2, voltando: o espelho. */
    VOLTAR_UM_NIVEL,
    /** R3, abrindo. Vale também para o fechamento da semana. */
    ABRIR_TAREFA,
    /** R3, fechando. */
    FECHAR_TAREFA,
    /** R4: troca de porta de entrada, e o padrão para o que não se encaixa. */
    PORTA,
}

/** A posição da aba na barra, para o sentido do deslize. */
private fun ordemDaAba(camada: Camada): Int =
    DestinoDeTopo.entries.firstOrNull { it.name == camada.nome }?.ordinal ?: 0

/** A regra da passagem de [de] para [para]. [voltando] é o pop, inclusive o gesto. */
internal fun regraEntre(de: Camada, para: Camada, voltando: Boolean): Regra = when {
    de == para -> if (voltando) Regra.VOLTAR_UM_NIVEL else Regra.APROFUNDAR
    !voltando && (para.tipo == TipoDeCamada.TAREFA || para.tipo == TipoDeCamada.MOMENTO) -> Regra.ABRIR_TAREFA
    voltando && (de.tipo == TipoDeCamada.TAREFA || de.tipo == TipoDeCamada.MOMENTO) -> Regra.FECHAR_TAREFA
    de.tipo == TipoDeCamada.ABA && para.tipo == TipoDeCamada.ABA ->
        if (ordemDaAba(para) > ordemDaAba(de)) Regra.ABA_A_DIREITA else Regra.ABA_A_ESQUERDA
    else -> Regra.PORTA
}

/**
 * Os quatro lambdas que os `NavHost` recebem.
 *
 * **Movimento reduzido é corte seco:** toda regra vira `None`, sem quadro intermediário. O
 * gesto de voltar continua funcionando, só sem animação.
 *
 * **Quem cobre fica por cima sem nada daqui:** o `NavHost` dá ao destino do push o `zIndex` da
 * origem mais um, e ao do pop, menos um (conferido no bytecode do Navigation 2.8.4). A tela
 * nova desliza sobre a de baixo, e no voltar é ela que sai por cima.
 */
internal class TransicoesDeNavegacao(private val reduzido: Boolean) {
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
            Regra.ABA_A_DIREITA -> slideInHorizontally(ida()) { largura -> largura }
            Regra.ABA_A_ESQUERDA -> slideInHorizontally(ida()) { largura -> -largura }
            Regra.APROFUNDAR, Regra.PORTA -> slideInHorizontally(ida()) { largura -> largura }
            Regra.VOLTAR_UM_NIVEL -> slideInHorizontally(volta()) { largura -> -recuo(largura) }
            Regra.ABRIR_TAREFA -> slideInVertically(ida()) { altura -> altura }
            // A tela de baixo já está lá, inteira: quem anda é a tarefa descendo por cima.
            Regra.FECHAR_TAREFA -> EnterTransition.None
        }
    }

    /** O que a tela que sai faz sob [regra]. */
    fun sair(regra: Regra): ExitTransition {
        if (reduzido) return ExitTransition.None
        return when (regra) {
            Regra.ABA_A_DIREITA -> slideOutHorizontally(ida()) { largura -> -largura }
            Regra.ABA_A_ESQUERDA -> slideOutHorizontally(ida()) { largura -> largura }
            Regra.APROFUNDAR, Regra.PORTA -> slideOutHorizontally(ida()) { largura -> -recuo(largura) }
            Regra.VOLTAR_UM_NIVEL -> slideOutHorizontally(volta()) { largura -> largura }
            // A tela de baixo fica parada enquanto a tarefa sobe e a cobre.
            Regra.ABRIR_TAREFA -> parada(Movimento.Navegacao.DURACAO)
            Regra.FECHAR_TAREFA -> slideOutVertically(volta(Movimento.Curva.saida)) { altura -> altura }
        }
    }

    private companion object {
        fun <T> ida() = tween<T>(Movimento.Navegacao.DURACAO, easing = Movimento.Curva.padrao)

        fun <T> volta(curva: androidx.compose.animation.core.Easing = Movimento.Curva.padrao) =
            tween<T>(Movimento.Navegacao.DURACAO, easing = curva)

        /** Quanto a tela de baixo anda quando outra a cobre (`recuo-navegacao`). */
        fun recuo(largura: Int): Int = (largura * Movimento.Navegacao.RECUO).roundToInt()

        /**
         * Fica inteira durante [duracao] e some no fim. É o `KeepUntilTransitionsFinished` do
         * Compose, que no animation 1.7 é `internal`: o salto atrasado dá o mesmo efeito. Uma
         * saída que não muda nada termina no primeiro quadro, e a tarefa subiria sobre o vazio.
         */
        fun parada(duracao: Int): ExitTransition = fadeOut(snap(delayMillis = duracao))
    }
}

/** As transições do tema atual, com o movimento reduzido lido do aparelho. */
@Composable
internal fun rememberTransicoesDeNavegacao(): TransicoesDeNavegacao {
    val reduzido = LocalMovimentoReduzido.current
    return remember(reduzido) { TransicoesDeNavegacao(reduzido) }
}
