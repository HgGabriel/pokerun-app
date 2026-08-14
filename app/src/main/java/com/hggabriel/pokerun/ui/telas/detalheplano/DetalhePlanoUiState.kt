package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.regras.SaltoDeVolume
import java.time.LocalDate

/**
 * O estado da `PlanDetailScreen` (`F1-T13`, docs/03 §3.7).
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
 *   lê `plans/{id}`, e documento que existe não é rascunho. Não há campo no schema que o
 *   diga (docs/05 §1). Desenhá-lo seria um estado sem caminho para exercitá-lo.
 */
sealed interface DetalhePlanoUiState {

    /** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
    data object Carregando : DetalhePlanoUiState

    /**
     * O plano inteiro: cabeçalho, grade, membros e código.
     *
     * As três permissões da tela são [podeEditar], [podeEncerrar] e nada mais — e são
     * propriedades do estado, e não condições escritas dentro do `@Composable`, porque é
     * assim que RN-05, RN-06 e RN-27 ganham teste.
     */
    data class Conteudo(
        val nome: String,
        val dataDaProva: LocalDate,
        val situacao: SituacaoDoPlano,
        /** O numerador da aderência acumulada, com o teto por semana de RN-08. */
        val sessoesFeitas: Int,
        /** O denominador: da semana em que o usuário entrou (RN-19) até a corrente. */
        val sessoesPrevistas: Int,
        val semanas: List<Semana>,
        /** // RN-05 — os números das semanas que já acabaram. Cadeado na lista. */
        val congeladas: Set<Int>,
        val membros: List<Membro>,
        val codigoConvite: String,
        /** // RN-06 */
        val ehDono: Boolean,
        /** // RN-30 — o maior salto acima de 15%, e só para quem edita. */
        val alerta: SaltoDeVolume? = null,
        val editando: EdicaoDoLongao? = null,
        val confirmandoEncerrar: Boolean = false,
        @param:StringRes val erro: Int? = null,
    ) : DetalhePlanoUiState {

        /** // RN-07 */
        val encerrado: Boolean get() = situacao == SituacaoDoPlano.ENCERRADO

        /**
         * // RN-05, RN-06, RN-07
         *
         * As três regras de permissão da tela, na ordem em que negam:
         *
         * - **RN-06** — quem não é dono não edita a estrutura de plano nenhum.
         * - **RN-07** — plano encerrado é somente leitura, e não reabre (RN-27).
         * - **RN-05** — semana que já acabou é congelada, e nem o dono a edita. A rule de
         *   `weeks/{n}` faz a mesma conta com `request.time`, então oferecer o toque aqui
         *   seria oferecer uma escrita que o servidor nega.
         *
         * A quarta condição não é regra de negócio e sim da grade: semana sem longão
         * previsto não tem o que editar, porque o volume dela não deriva de um
         * (`editarLongao`, docs/01 §3.3).
         */
        fun podeEditar(semana: Semana): Boolean =
            ehDono &&
                !encerrado &&
                semana.numero !in congeladas &&
                semana.longaoKm != null

        /**
         * // RN-27
         *
         * Encerrar é do dono, com confirmação, e some depois de encerrado: o plano **não
         * reabre**, e a própria rule de `plans` exige `encerrado == false` para aceitar
         * a escrita — a que encerra é a última que o documento aceita.
         */
        val podeEncerrar: Boolean get() = ehDono && !encerrado
    }

    /** Erro de leitura, com a ação de repetir (docs/02 §8, item 7). */
    data object Falhou : DetalhePlanoUiState
}

/**
 * O diálogo de edição do longão do dono (docs/01 §3.3).
 *
 * É irmão do de `F1-T11` e não o mesmo tipo: lá ele vive num rascunho em memória, aqui
 * cada confirmação é uma escrita em `weeks/{n}`. O que as duas telas compartilham é o que
 * importa — `editarLongao` e `alertaDeVolume`, as regras — e não o portador do texto
 * digitado.
 */
data class EdicaoDoLongao(
    val numero: Int,
    val texto: String,
    @param:StringRes val erro: Int? = null,
)
