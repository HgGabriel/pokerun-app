package com.hggabriel.pokerun.ui.telas.listadeplanos

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.Plano
import java.time.LocalDate

/**
 * O estado da `PlansListScreen` (`F1-T12`, docs/03 §3.4).
 *
 * Os três da ficha estão aqui com os nomes dela: [Carregando], [Lista] e [Vazio]. O
 * quarto existe pelo mesmo motivo do sétimo estado da Home: docs/02 §8, item 7 exige
 * estado de erro **com ação de repetir** em toda tela com dado remoto, e a lista lê dois
 * documentos remotos — `users/{uid}` e cada `plans/{id}`.
 *
 * **Nenhuma data aqui é `Instant`.** A data da prova já saiu do fuso do plano (RN-28)
 * dentro de [itensDePlano]; um `Instant` aqui convidaria o `@Composable` a formatá-lo
 * com o fuso do aparelho, que é o bug que RN-28 existe para impedir.
 */
sealed interface ListaDePlanosUiState {

    /** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
    data object Carregando : ListaDePlanosUiState

    /**
     * O estado vazio de docs/03 §3.4, com **a mesma copy da `HomeScreen · SemPlano`** —
     * a especificação manda repetir a copy, e a tela repete o recurso de string, que é o
     * que impede as duas de divergirem numa revisão futura.
     */
    data object Vazio : ListaDePlanosUiState

    /**
     * Os planos do usuário, na ordem dos três grupos de [itensDePlano].
     *
     * [confirmando] é o plano que o diálogo de RN-13 está perguntando se vira ativo.
     * Nulo é diálogo fechado, e **é o único caminho até a escrita**: sem ele o botão
     * `[Tornar ativo]` trocaria o plano no toque, que é exatamente o "troca silenciosa"
     * que a regra proíbe.
     */
    data class Lista(
        val itens: List<ItemDePlano>,
        val confirmando: ItemDePlano? = null,
        @param:StringRes val erro: Int? = null,
    ) : ListaDePlanosUiState {

        /**
         * // RN-12
         *
         * O plano que recebe as corridas hoje, ou nulo. É ele que o diálogo nomeia como
         * o plano que **sai**: as corridas já registradas continuam nele (RN-14), e ele
         * segue visível para consulta (RN-15).
         */
        val ativo: ItemDePlano? get() = itens.firstOrNull { it.situacao == SituacaoDoPlano.ATIVO }
    }

    /** Erro de leitura, com a ação de repetir (docs/02 §8, item 7). */
    data object Falhou : ListaDePlanosUiState
}

/**
 * Uma linha da lista.
 *
 * [dataDaProva] já vem convertida **no fuso do plano** (RN-28), e é por isso que ela é
 * `LocalDate` e não `Instant`.
 */
data class ItemDePlano(
    val id: String,
    val nome: String,
    val dataDaProva: LocalDate,
    val distanciaAlvoKm: Double,
    val situacao: SituacaoDoPlano,
) {
    /**
     * // RN-13
     *
     * `[Tornar ativo]` só existe em plano dormente. No ativo não teria efeito, e no
     * encerrado seria uma porta para um plano que não recebe corrida nenhuma (RN-07) —
     * apontar `plano_ativo_id` para ele deixaria a Home no estado `Encerrado`, que é um
     * beco sem saída oferecido por um botão.
     */
    val podeTornarAtivo: Boolean get() = situacao == SituacaoDoPlano.DORMENTE
}

/**
 * As três situações que a lista distingue (docs/03 §3.4).
 *
 * [ATIVO] é o de RN-12, marcado em `leitura`; [DORMENTE] é o de RN-15, visível para
 * consulta e sem receber corridas, em `tinta-fraca`; [ENCERRADO] é o de RN-07, agrupado
 * ao final (D-05).
 */
enum class SituacaoDoPlano {
    ATIVO,
    DORMENTE,
    ENCERRADO,
}

/**
 * // RN-12
 *
 * Classifica e ordena os planos do usuário nos três grupos de docs/03 §3.4.
 *
 * **`encerrado` ganha de `plano_ativo_id`.** Um plano encerrado apontado como ativo
 * existe de verdade — RN-27 encerra o plano ao fim da semana da prova sem tocar no
 * documento do usuário —, e ele vai para o grupo do fim. Marcá-lo em `leitura` no meio
 * da lista diria que ele ainda recebe corridas, quando RN-07 já o congelou; a Home tem
 * um estado próprio para esse caso e é lá que ele se resolve.
 *
 * **A ordem dentro de cada grupo é a de chegada**, que é a ordem de `users/{uid}.planos`
 * (docs/05 §2.7) — o array cresce por `arrayUnion`, então ele é o histórico de entrada
 * do corredor. A especificação não pede ordenação nenhuma dentro do grupo, e ordenar por
 * data da prova embaralharia a lista a cada plano que encerra.
 *
 * @param planos os planos já lidos por ID (nunca por consulta: `plans` não aceita
 *   `list`, RN-17).
 * @param planoAtivoId o `plano_ativo_id` de `users/{uid}`, nulo para quem ainda não
 *   tornou nenhum ativo.
 */
fun itensDePlano(planos: List<Plano>, planoAtivoId: String?): List<ItemDePlano> =
    planos
        .map { plano ->
            ItemDePlano(
                id = plano.id,
                nome = plano.nome,
                // RN-28: o fuso é o do plano, e cada linha lê o seu. Dois planos criados
                // em fusos diferentes convivem na mesma conta.
                dataDaProva = plano.dataProva.atZone(plano.fuso).toLocalDate(),
                distanciaAlvoKm = plano.distanciaAlvoKm,
                situacao = when {
                    plano.encerrado -> SituacaoDoPlano.ENCERRADO
                    plano.id == planoAtivoId -> SituacaoDoPlano.ATIVO
                    else -> SituacaoDoPlano.DORMENTE
                },
            )
        }
        // `sortedBy` é estável, então a ordem de chegada sobrevive dentro do grupo.
        .sortedBy { it.situacao.ordinal }
