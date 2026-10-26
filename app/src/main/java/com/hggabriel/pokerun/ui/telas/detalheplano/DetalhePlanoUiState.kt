package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import com.hggabriel.pokerun.ui.componentes.TempoDaSemana
import java.time.LocalDate

/**
 * O estado da `PlanDetailScreen` (`F1-T13`, r2 de 26/10/2026, docs/03 §3.7).
 *
 * A ficha lista seis estados: `Loading`, `Rascunho`, `Ativo`, `Dormente`,
 * `Encerrado(resumo)` e `Error`. Aqui são **três**, e a diferença é deliberada:
 *
 * - **`Ativo`, `Dormente` e `Encerrado` não são estados de tela, são situações do mesmo
 *   plano** — o layout é idêntico e o que muda é o que se pode fazer. Virarem um campo
 *   ([Conteudo.situacao]) em vez de três `data class` iguais tira a chance de as três
 *   divergirem no dia em que a tela ganhar uma linha nova.
 * - **`Rascunho` não é alcançável e não é representável.** Um rascunho é uma grade em
 *   memória na `PlanDraftReviewScreen`, antes de `criar`; esta tela recebe um `planoId` e
 *   lê `plans/{id}`, e documento que existe não é rascunho.
 */
sealed interface DetalhePlanoUiState {

    /** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
    data object Carregando : DetalhePlanoUiState

    /**
     * O plano inteiro: número principal, gráfico do longão, a semana selecionada, o grupo
     * e o código.
     *
     * As permissões da tela são [podeEditar], [podeMudarCurta] e [podeEncerrar] — e são
     * propriedades do estado, e não condições escritas dentro do `@Composable`, porque é
     * assim que RN-05, RN-06 e RN-27 ganham teste.
     */
    data class Conteudo(
        val nome: String,
        val dataDaProva: LocalDate,
        val situacao: SituacaoDoPlano,
        val semanas: List<Semana>,
        /** // RN-05 — os números das semanas que já acabaram. */
        val congeladas: Set<Int>,
        /**
         * As semanas que já começaram: as congeladas e a corrente. A troca da curta vale
         * a partir da semana seguinte, então elas ficam (docs/01 §3.3).
         */
        val comecadas: Set<Int>,
        /** O dono primeiro, depois na ordem de entrada. */
        val membros: List<Membro>,
        val codigoConvite: String,
        /** // RN-06 */
        val ehDono: Boolean,
        /** O que gerou a grade. A edição refaz as semanas com a mesma regra (docs/01 §3.3). */
        val parametros: ParametrosDeGeracao,
        /** O número grande do topo, que muda com a situação (D2, S2). */
        val numeroPrincipal: NumeroPrincipal,
        /** A semana em curso. Nula antes de o plano começar e no plano encerrado. */
        val semanaCorrente: Int?,
        /**
         * A última semana que a fração de cada membro conta (RN-19): a corrente enquanto o
         * plano corre, a última depois do fim. É também até onde se lê o `weekly` do grupo.
         */
        val ateSemana: Int,
        /** Cada semana da grade com o período, o tempo e os seus carimbos, na ordem. */
        val linhaDoTempo: List<SemanaDoPlano>,
        /** O progresso de cada membro, na ordem de [membros]. */
        val progresso: List<ProgressoDoMembro>,
        /** A semana do card e do destaque no gráfico. Sempre dentro de `1..N`. */
        val semanaSelecionada: Int,
        /** O sprite e o tipo de cada membro, pelo uid. Chegam depois, e faltar não é erro. */
        val avatares: Map<String, AvatarDoMembro> = emptyMap(),
        /** O que a última edição mexeu além da semana editada. */
        val tambemMudaram: List<MudancaDeLongao> = emptyList(),
        val editando: EdicaoDoLongao? = null,
        val editandoCurta: EdicaoDaCurta? = null,
        val confirmandoEncerrar: Boolean = false,
        @param:StringRes val erro: Int? = null,
    ) : DetalhePlanoUiState {

        /** // RN-07 */
        val encerrado: Boolean get() = situacao == SituacaoDoPlano.ENCERRADO

        /** O seu progresso: a linha do grupo de quem está olhando. */
        val voce: ProgressoDoMembro? get() = progresso.firstOrNull { it.voce }

        /** // RN-08, RN-19 — a sua fração, a mesma conta da aderência acumulada de antes da r2. */
        val sessoesFeitas: Int get() = voce?.feitas ?: 0

        val sessoesPrevistas: Int get() = voce?.previstas ?: 0

        /**
         * // D7, S1
         *
         * As semanas em que **algum membro** já cumpriu sessão. O longão é o mesmo para
         * todos, então a corrida de qualquer um trava a semana.
         */
        val semanasCorridas: Set<Int>
            get() = progresso.flatMapTo(mutableSetOf()) { membro ->
                membro.semanas.orEmpty().filter { (it.sessoesFeitas ?: 0) >= 1 }.map { it.numero }
            }

        /**
         * // RN-05, RN-06, RN-07, D7
         *
         * As regras de permissão da edição do longão, na ordem em que negam:
         *
         * - **RN-06** — quem não é dono não edita a estrutura de plano nenhum.
         * - **RN-07** — plano encerrado é somente leitura, e não reabre (RN-27).
         * - **RN-05** — semana que já acabou é congelada, e nem o dono a edita. A rule de
         *   `weeks/{n}` faz a mesma conta com `request.time`, então oferecer o toque aqui
         *   seria oferecer uma escrita que o servidor nega.
         * - **Da grade:** só semana de build se edita. O taper sai do maior longão do build,
         *   e a prova não planeja longão (docs/01 §3.3).
         * - **D7:** semana em que algum membro já cumpriu sessão não tem o longão mudado.
         *   A regra é da tela, e não da rule: mudar o alvo depois de alguém correr para ele
         *   reescreveria o que aquela corrida cumpriu.
         */
        fun podeEditar(semana: Semana): Boolean =
            ehDono &&
                !encerrado &&
                semana.numero !in congeladas &&
                semana.tipo == TipoDeSemana.BUILD &&
                semana.numero !in semanasCorridas

        /** // RN-06, RN-07 — a curta é do plano, e só o dono a troca enquanto ele corre. */
        val podeMudarCurta: Boolean get() = ehDono && !encerrado

        /**
         * // RN-27
         *
         * Encerrar é do dono, com confirmação, e some depois de encerrado: o plano **não
         * reabre**, e a própria rule de `plans` exige `encerrado == false` para aceitar
         * a escrita — a que encerra é a última que o documento aceita.
         */
        val podeEncerrar: Boolean get() = ehDono && !encerrado

        /** D3: o convite vale para dono e membro, ativo e guardado. Some só no encerrado. */
        val mostraConvite: Boolean get() = !encerrado
    }

    /** Erro de leitura, com a ação de repetir (docs/02 §8, item 7). */
    data object Falhou : DetalhePlanoUiState
}

/**
 * O número grande do topo (D2, S2): os dias até a prova enquanto o plano corre, e o
 * resultado depois.
 */
sealed interface NumeroPrincipal {
    /** Antes do dia da prova: a mesma conta da placa de peito da Home. */
    data class Dias(val dias: Int) : NumeroPrincipal

    /** No dia da prova, como a placa: `HOJE`. */
    data object DiaDaProva : NumeroPrincipal

    /** Encerrado, com a corrida que cumpriu a sessão da prova. */
    data class TempoNaProva(val duracaoSeg: Long) : NumeroPrincipal

    /** Encerrado sem essa corrida: a sua fração de sessões (S2). */
    data class Sessoes(val feitas: Int, val previstas: Int) : NumeroPrincipal
}

/**
 * Uma semana da grade, pronta para o gráfico e para o card.
 *
 * @property planejadoKm o longão planejado, ou a distância da prova na semana da prova.
 *   Nulo só em plano antigo, de antes da DA-10, com a 2ª de taper sem longão.
 * @property segmentos as **suas** vagas e carimbos, com o dia no fuso do plano.
 */
data class SemanaDoPlano(
    val semana: Semana,
    val primeiroDia: LocalDate,
    val ultimoDia: LocalDate,
    val tempo: TempoDaSemana,
    val planejadoKm: Double?,
    val segmentos: List<SegmentoDaSemana>,
) {
    val numero: Int get() = semana.numero
}

/**
 * O progresso de um membro no plano (R3).
 *
 * @property semanas uma por semana da grade, na ordem. **Nula quando a leitura do `weekly`
 *   da outra pessoa falhou** (S3): a linha fica só com avatar e nome, e o traço dela some do
 *   gráfico. A sua nunca é nula, porque sai das suas corridas.
 */
data class ProgressoDoMembro(
    val uid: String,
    val nome: String,
    val dono: Boolean,
    val voce: Boolean,
    val entrouNaSemana: Int,
    val semanas: List<ProgressoNaSemana>?,
) {
    /** // RN-08 — o numerador, com o teto por semana. */
    val feitas: Int
        get() = semanas.orEmpty().sumOf { s -> s.sessoesFeitas?.coerceAtMost(s.sessoesAlvo) ?: 0 }

    /** // RN-19 — o denominador: só as semanas da janela do membro. */
    val previstas: Int
        get() = semanas.orEmpty().filter { it.sessoesFeitas != null }.sumOf { it.sessoesAlvo }

    fun na(numero: Int): ProgressoNaSemana? = semanas?.firstOrNull { it.numero == numero }
}

/**
 * O que um membro fez numa semana.
 *
 * @property sessoesFeitas nulo fora da janela do membro: antes de ele entrar, ou depois da
 *   semana até onde a fração conta. Dentro dela, zero é semana sem sessão, e conta (D1).
 * @property longaoFeitoKm o longão que ele correu, ou nulo quando não correu nenhum.
 */
data class ProgressoNaSemana(
    val numero: Int,
    val sessoesAlvo: Int,
    val sessoesFeitas: Int?,
    val longaoFeitoKm: Double?,
)

/** O avatar de um membro: o sprite de jogo e o tipo, que dá o fundo `tipo-*-suave`. */
data class AvatarDoMembro(val sprite: String?, val tipo: TipoDePokemon?)

/**
 * O diálogo de edição do longão do dono (docs/01 §3.3).
 *
 * É irmão do de `F1-T11` e não o mesmo tipo: lá ele vive num rascunho em memória, aqui
 * cada confirmação é uma escrita em `weeks/{n}`. O que as duas telas compartilham é a
 * regra — `editarLongao` e `tetoDoLongao` — e não o portador do texto digitado.
 */
data class EdicaoDoLongao(
    val numero: Int,
    val texto: String,
    val tetoKm: Double,
    val editada: Boolean = false,
    @param:StringRes val erro: Int? = null,
)

/** O diálogo da curta do plano (docs/01 §3.3). */
data class EdicaoDaCurta(
    val texto: String,
    @param:StringRes val erro: Int? = null,
)
