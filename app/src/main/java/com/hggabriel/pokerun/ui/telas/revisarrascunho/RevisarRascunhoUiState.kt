package com.hggabriel.pokerun.ui.telas.revisarrascunho

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.SaltoDeVolume

/**
 * O estado da `PlanDraftReviewScreen` (`F1-T11`, docs/03 §3.6).
 *
 * **É um estado só, e não uma máquina**, pelo mesmo motivo de `F1-T10`: a grade nasce
 * pronta de uma função pura no momento em que a tela abre, sem ida ao servidor. Não há
 * `Carregando` porque não há o que carregar — [GeradorDePlano][com.hggabriel.pokerun.dominio.regras.GeradorDePlano]
 * responde na composição. O que existe de assíncrono é a escrita, e ela cabe em
 * [salvando] mais [erro].
 *
 * **[grade] é a fonte da verdade da tela e a que vai para o Firestore.** Editar o longão
 * troca a lista inteira por uma nova, e [alerta] é recalculado junto: RN-30 roda sobre a
 * **grade corrente**, e um alerta guardado de uma versão anterior seria pior que nenhum.
 */
data class RevisarRascunhoUiState(
    val nomeDoPlano: String = "",
    val grade: List<Semana> = emptyList(),
    /** O maior salto acima de 15% (RN-30), ou nulo. **Não bloqueia** — ver [podeCriar]. */
    val alerta: SaltoDeVolume? = null,
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
     * // RN-30
     *
     * **[alerta] não entra nesta conta, e é a metade da regra que só o código prova.**
     * RN-30 é explícito: o aviso de 15% *"nunca bloqueia a gravação"*. Quem quiser uma
     * grade agressiva, grava — o app avisa e sai da frente.
     *
     * [online] entra porque a reserva do código de convite é transacional (RN-29) e não
     * resolve no cache: offline a escrita **fica pendurada** em vez de falhar
     * (docs/05 §2.6). [salvando] entra para o segundo toque não gravar dois planos.
     */
    val podeCriar: Boolean get() = grade.isNotEmpty() && online && !salvando
}

/**
 * O diálogo de edição do longão (docs/01 §3.3).
 *
 * **Só o longão é editável.** O volume deriva dele pela fórmula da geração, e um segundo
 * campo faria os dois divergirem sem ninguém saber qual manda.
 *
 * [texto] é o que está digitado, não o número: o campo aceita vírgula e ponto, e a
 * conversão é de `distanciaEmKm`, a mesma das outras três distâncias do app.
 */
data class EdicaoDoLongao(
    val numero: Int,
    val texto: String,
    @param:StringRes val erro: Int? = null,
)
