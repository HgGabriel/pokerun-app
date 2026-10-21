package com.hggabriel.pokerun.ui.telas.revisarrascunho

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.dominio.regras.Viabilidade

/**
 * O estado da `PlanDraftReviewScreen` (`F1-T11`, docs/03 §3.6, DA-10).
 *
 * **É um estado só, e não uma máquina**, pelo mesmo motivo de `F1-T10`: a grade nasce
 * pronta de uma função pura no momento em que a tela abre, sem ida ao servidor. O que
 * existe de assíncrono é a escrita, e ela cabe em [salvando] mais [erro].
 *
 * **[grade] é a fonte da verdade da tela e a que vai para o Firestore.** Editar o longão
 * troca a lista inteira por uma nova, e [tambemMudaram] diz quais outras semanas a edição
 * moveu (docs/01 §3.3).
 */
data class RevisarRascunhoUiState(
    val nomeDoPlano: String = "",
    val grade: List<Semana> = emptyList(),
    /**
     * O plano cabe, mas o longão não chega ao pico desejado (docs/01 §3.2). A tela diz
     * quanto falta; não bloqueia.
     */
    val picoReduzido: Viabilidade.PicoReduzido? = null,
    /** O que a última edição mexeu além da semana editada. Vazio quando nada mais mudou. */
    val tambemMudaram: List<MudancaDeLongao> = emptyList(),
    /** O diálogo do longão está aberto nesta semana. Nulo é fechado. */
    val editando: EdicaoDoLongao? = null,
    /**
     * // RN-13
     *
     * O usuário já tem um plano ativo, então o plano novo nasce **dormente**. A troca do
     * ativo é decisão explícita e nunca efeito colateral de criar outro plano: quem faz é
     * a `PlansListScreen` (`F1-T12`), com confirmação. A tela só avisa que vai ser assim.
     */
    val jaTemPlanoAtivo: Boolean = false,
    val online: Boolean = true,
    val salvando: Boolean = false,
    @param:StringRes val erro: Int? = null,
    /** Terminal: o plano está no Firestore e a tela sai. */
    val criado: Boolean = false,
) {
    /**
     * [online] entra porque a reserva do código de convite é transacional (RN-29) e não
     * resolve no cache: offline a escrita **fica pendurada** em vez de falhar
     * (docs/05 §2.6). [salvando] entra para o segundo toque não gravar dois planos.
     */
    val podeCriar: Boolean get() = grade.isNotEmpty() && online && !salvando
}

/**
 * O diálogo de edição do longão (docs/01 §3.3).
 *
 * **Só o longão é editável**, e só nas semanas de build. [tetoKm] é o máximo que a semana
 * aceita: o diálogo o mostra antes de o dono digitar, e recusa acima dele. [editada] diz
 * que a semana tem um pedido do dono, e acende o "voltar à rampa".
 *
 * [texto] é o que está digitado, não o número: o campo aceita vírgula e ponto, e a
 * conversão é de `distanciaEmKm`, a mesma das outras distâncias do app.
 */
data class EdicaoDoLongao(
    val numero: Int,
    val texto: String,
    val tetoKm: Double,
    val editada: Boolean = false,
    @param:StringRes val erro: Int? = null,
)
