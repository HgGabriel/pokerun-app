package com.hggabriel.pokerun.ui.telas.detalhesemana

import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.ui.componentes.DiaDoTreino
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import java.time.LocalDate

/**
 * O estado da `WeekDetailScreen` (`F1-T15`, docs/03 §3.9).
 *
 * Três estados, e a ficha não pede outros: a tela é somente leitura e não tem escrita
 * nenhuma. [Carregando] é a espera pela grade, [Conteudo] é a semana e [Falhou] é o item
 * 7 do piso de qualidade (docs/02 §8) — toda tela com dado remoto tem erro com ação de
 * repetir.
 *
 * **Nenhuma data aqui é `Instant`**, pelo mesmo motivo de `HomeUiState`: as fronteiras já
 * saíram do fuso do plano (RN-28) antes de chegar na tela, e um `Instant` aqui convidaria
 * o `@Composable` a formatar com o fuso do aparelho.
 */
sealed interface DetalheSemanaUiState {

    /** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
    data object Carregando : DetalheSemanaUiState

    /**
     * A semana inteira: cabeçalho, card sem anel, grade de dias e as corridas dela.
     *
     * O card é o mesmo de `F1-T09` e é montado das mesmas três peças de `ui/componentes`
     * — [feitas]/[previstas], [segmentos] e [dias]. A tela não recalcula nada: quem
     * deriva é [detalheDaSemana], que é função pura e tem teste.
     */
    data class Conteudo(
        val nomeDoPlano: String,
        val numero: Int,
        val totalDeSemanas: Int,
        val primeiroDia: LocalDate,
        /** O último dia **incluído**: `data_fim` é exclusivo, então é a véspera dele. */
        val ultimoDia: LocalDate,
        val tipo: TipoDeSemana,
        val kmAlvo: Double,
        /** Nulo na 2ª de taper e na semana da prova, que não planejam longão. */
        val longaoKm: Double?,
        /** // RN-10 — nulo onde não há longão a cumprir. Nulo **não é falso**. */
        val longaoCumprido: Boolean?,
        val feitas: Int,
        val previstas: Int,
        val segmentos: List<SegmentoDaSemana>,
        val dias: List<DiaDoTreino>,
        val corridas: List<CorridaDaSemana>,
        /** // RN-05 — semana encerrada é congelada, e a tela mostra o cadeado. */
        val congelada: Boolean,
    ) : DetalheSemanaUiState {

        /**
         * O estado vazio de docs/03 §3.9, que **não é um estado de tela**: o cabeçalho, o
         * card e a grade continuam desenhados, e o que entra é a frase com as sessões
         * previstas da semana no lugar da lista.
         */
        val vazia: Boolean get() = corridas.isEmpty()
    }

    /** Erro de leitura, com a ação de repetir (docs/02 §8, item 7). */
    data object Falhou : DetalheSemanaUiState
}

/**
 * Uma corrida da semana, na lista de docs/03 §3.9.
 *
 * **Quatro dados e nenhum a mais.** A ficha da tarefa diz *"lista de corridas"* sem
 * listar campos, e o bloco de ficha completo — FC, splits, esforço percebido, XP — é a
 * `RunDetailScreen` (docs/03 §3.13, `F2-T09`). Aqui ficam o dia, a distância, o tempo e o
 * pace, que é o que distingue duas corridas da mesma semana sem obrigar a pessoa a fazer
 * a divisão de cabeça.
 *
 * **[dia] é `LocalDate` e já saiu do fuso do plano** (RN-28): o mesmo instante é domingo
 * para quem treina em São Paulo e segunda para quem treina em Tóquio, e a semana é a do
 * plano.
 *
 * [id] fica aqui à espera da `RunDetailScreen`. Enquanto ela não existe, a linha não é
 * tocável — pelo mesmo motivo do segmento da barra em `F1-T09`: um alvo que não faz nada
 * é pior que nenhum alvo.
 */
data class CorridaDaSemana(
    val id: String,
    val dia: LocalDate,
    val km: Double,
    val duracaoSeg: Long,
)
