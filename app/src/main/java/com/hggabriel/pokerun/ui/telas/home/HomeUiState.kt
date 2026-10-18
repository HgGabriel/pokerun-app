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
        val placa: DadosDaPlaca,
        val comecaEm: LocalDate,
        val primeiraSemana: ResumoDaSemana,
    ) : HomeUiState {
        val nomeDoPlano: String get() = placa.nomeDoPlano
    }

    /**
     * O painel do plano corrente: a placa de peito, o card da semana e a última corrida.
     *
     * **[planoId] existe para o card ter porta.** docs/03 §1 desenha a aresta
     * `HomeScreen → WeekDetailScreen`, e a rota dela carrega plano e número — o número
     * já estava no card, o plano não. Sem ele o toque não teria para onde ir.
     *
     * [ultimaCorrida] é nula enquanto o plano não tem corrida: a linha some, e não
     * mostra zeros.
     */
    data class Ativo(
        val planoId: String,
        val placa: DadosDaPlaca,
        val semana: CardDaSemana,
        val ultimaCorrida: UltimaCorrida? = null,
    ) : HomeUiState {
        val nomeDoPlano: String get() = placa.nomeDoPlano
        val diasAteAProva: Int get() = placa.diasAteAProva
    }

    /**
     * // RN-07
     *
     * Plano encerrado é somente-leitura e **não reabre** (RN-27). A Home oferece as
     * mesmas duas saídas de [SemPlano], porque é o que resta a fazer daqui. A placa
     * fica, carimbada `CONCLUÍDO`, e é a porta do resumo congelado (D-05).
     */
    data class Encerrado(val placa: DadosDaPlaca) : HomeUiState {
        val nomeDoPlano: String get() = placa.nomeDoPlano
    }

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
 * [feitas] e [previstas] são a fração — `2 de 3`, nunca `67%`. [segmentos] é a
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

/**
 * A placa de peito (docs/02 §4.5): a faixa diz [nomeDoPlano] e [dataDaProva], o número
 * diz [diasAteAProva], a tarja diz a semana e desenha o [arco].
 *
 * [dataDaProva] sai da grade, e não de `plano.dataProva`, pelo mesmo motivo da contagem
 * (ver `diasAteAProva`): as duas presas à mesma fronteira não discordam nunca.
 */
data class DadosDaPlaca(
    val planoId: String,
    val nomeDoPlano: String,
    val dataDaProva: LocalDate,
    val diasAteAProva: Int,
    val arco: List<BarraDoArco>,
) {
    /** A semana corrente, ou nula fora do intervalo do plano. */
    val semanaAtual: Int? get() = arco.firstOrNull { it.situacao == SituacaoDaBarra.ATUAL }?.numero
}

/**
 * Uma semana do arco do longão. [longaoKm] é nulo na semana que não planeja longão (a
 * 2ª de taper, RN-10), e a barra vira um traço em vez de sumir: o buraco leria como
 * semana faltando.
 */
data class BarraDoArco(
    val numero: Int,
    val longaoKm: Double?,
    val taper: Boolean,
    val situacao: SituacaoDaBarra,
)

/** Onde a semana fica em relação a hoje. Decide a cor da barra (docs/02 §4.5). */
enum class SituacaoDaBarra { PASSADA, ATUAL, FUTURA }

/**
 * A linha `5,8 km · 6:12 /km · 35:58` de docs/03 §3.3. Sem o dia, que o carimbo já
 * diz. [id] é a porta da `RunDetailScreen`.
 */
data class UltimaCorrida(
    val id: String,
    val km: Double,
    val duracaoSeg: Long,
)
