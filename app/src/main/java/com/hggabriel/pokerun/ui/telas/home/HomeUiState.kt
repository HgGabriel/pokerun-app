package com.hggabriel.pokerun.ui.telas.home

import com.hggabriel.pokerun.ui.componentes.DiaDoTreino
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import java.time.LocalDate

/**
 * O estado da `HomeScreen` (`F1-T09`, docs/03 §3.3).
 *
 * Os cinco da ficha estão aqui com os nomes dela: [Carregando], [SemPlano],
 * [NaoIniciado], [Ativo] e [Encerrado]. Os outros dois existem por motivo nomeado:
 *
 * - **[SemPerfil]** é o caso que `F1-T08` deixou para cá. Quem for morto entre
 *   autenticar e terminar o passo 2 do cadastro abre direto na casca, porque a
 *   abertura com sessão não checa perfil, e os cinco estados acima pressupõem perfil.
 *   É terminal como o `Concluido` do onboarding: a tela não muda de aparência, ela sai.
 * - **[Falhou]** é o item 7 do piso de qualidade (docs/02 §8): toda tela com dado
 *   remoto tem estado de erro **com ação de repetir**. A ficha lista cinco estados e
 *   nenhum deles cobre o listener do Firestore caindo.
 *
 * **Nenhuma data aqui é `Instant`.** As fronteiras já saíram do fuso do plano (RN-28)
 * antes de chegar na tela; um `Instant` aqui convidaria o `@Composable` a formatar com
 * o fuso do aparelho, que é o bug que RN-28 existe para impedir.
 */
sealed interface HomeUiState {

    /** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
    data object Carregando : HomeUiState

    /** Terminal: não há `users/{uid}` e a tela devolve ao cadastro. Ver [HomeUiState]. */
    data object SemPerfil : HomeUiState

    /** O estado vazio de docs/03 §3.3: `[Criar plano]` e `[Entrar com código]`. */
    data object SemPlano : HomeUiState

    /**
     * O plano existe e a primeira semana ainda não começou.
     *
     * Não é um `Ativo` com zero de aderência: a semana 1 ainda não abriu, e uma fração
     * `0 de 3` num plano que começa segunda leria como falta.
     */
    data class NaoIniciado(
        val nomeDoPlano: String,
        val comecaEm: LocalDate,
        val primeiraSemana: ResumoDaSemana,
    ) : HomeUiState

    /** O painel do plano corrente: contagem regressiva e card da semana. */
    data class Ativo(
        val nomeDoPlano: String,
        val diasAteAProva: Int,
        val semana: CardDaSemana,
    ) : HomeUiState

    /**
     * // RN-07
     *
     * Plano encerrado é somente-leitura e **não reabre** (RN-27). A Home oferece as
     * mesmas duas saídas de [SemPlano], porque é o que resta a fazer daqui.
     */
    data class Encerrado(val nomeDoPlano: String) : HomeUiState

    /** Erro de leitura, com a ação de repetir (docs/02 §8, item 7). */
    data object Falhou : HomeUiState
}

/**
 * A linha `Semana 1: 3 sessões · 10 km · longão de 5 km` de docs/03 §3.3.
 *
 * [longaoKm] é nulo nas duas semanas que não planejam longão, e a linha encolhe em vez
 * de mostrar `longão de null km`.
 */
data class ResumoDaSemana(
    val numero: Int,
    val sessoes: Int,
    val kmAlvo: Double,
    val longaoKm: Double?,
)

/**
 * O card da semana, **sem anel** (docs/03 §3.3.1, docs/02 §9.1.1).
 *
 * [feitas] e [previstas] são a fração em mono — `2 de 3`, nunca `67%`. [segmentos] é a
 * barra, [dias] é a grade: a barra diz *quantas*, a grade diz *quando*.
 *
 * [longaoCumprido] é nulo quando a semana não planeja longão, e nulo **não é falso**:
 * um X na 2ª de taper puniria quem seguiu o plano à risca (RN-10).
 */
data class CardDaSemana(
    val numero: Int,
    val totalDeSemanas: Int,
    val primeiroDia: LocalDate,
    val ultimoDia: LocalDate,
    val feitas: Int,
    val previstas: Int,
    val longaoKm: Double?,
    val longaoCumprido: Boolean?,
    val segmentos: List<SegmentoDaSemana>,
    val dias: List<DiaDoTreino>,
)
