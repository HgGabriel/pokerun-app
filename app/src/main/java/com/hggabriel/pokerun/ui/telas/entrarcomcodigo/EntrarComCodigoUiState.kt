package com.hggabriel.pokerun.ui.telas.entrarcomcodigo

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.regras.codigoCompleto

/**
 * O estado da `JoinPlanScreen` (`F1-T14`, docs/03 §3.8).
 *
 * **Os cinco estados da ficha estão em [Resultado]**, com os nomes dela: `Idle`,
 * `Buscando`, `Encontrado`, `NaoEncontrado` e `JaMembro`. Eles descrevem o que o
 * **código** virou, e não a tela inteira — o que a pessoa digitou precisa sobreviver a
 * todos eles, ou uma busca sem resultado limparia o campo e obrigaria a digitar de novo
 * os seis caracteres.
 *
 * [erro] é o sexto estado que docs/02 §8, item 7 exige de toda tela com dado remoto, e
 * ele é diferente de [Resultado.NaoEncontrado] de propósito: *"não existe plano com este
 * código"* manda conferir as letras, e *"não deu para procurar"* manda conferir a
 * conexão. Trocar um pelo outro faz a pessoa caçar um erro que não é dela.
 */
data class EntrarComCodigoUiState(
    val codigo: String = "",
    val resultado: Resultado = Resultado.Idle,
    /** A escrita em curso. Diferente de [Resultado.Buscando], que é a leitura. */
    val entrando: Boolean = false,
    /**
     * // RN-13
     *
     * A prévia sobre a qual o diálogo de escolha está aberto, ou nulo. **É o único
     * caminho até a troca do plano ativo** quando já existe um: sem ele o botão de
     * entrar trocaria o primário no toque, que é a troca silenciosa que a regra proíbe.
     */
    val escolhendoAtivo: PreviaDaEntrada? = null,
    @param:StringRes val erro: Int? = null,
    /** Entrou de verdade. A tela sai daqui e volta para de onde veio. */
    val entrou: Boolean = false,
) {
    /**
     * // RN-29
     *
     * Buscar com menos de seis caracteres é uma leitura faturada garantidamente vazia,
     * e o campo já não aceita nada fora do alfabeto (`normalizarCodigo`).
     */
    val podeBuscar: Boolean
        get() = codigoCompleto(codigo) && resultado !is Resultado.Buscando && !entrando

    /** A prévia do plano encontrado, quando há uma. */
    val previa: PreviaDaEntrada? get() = (resultado as? Resultado.Encontrado)?.previa
}

/** Os cinco estados de docs/03 §3.8, com os nomes da ficha. */
sealed interface Resultado {

    /** Nada digitado ainda, ou o texto mudou depois da última busca. */
    data object Idle : Resultado

    /** Esperando o servidor. É leitura direta em `invites/{codigo}`, nunca consulta. */
    data object Buscando : Resultado

    data class Encontrado(val previa: PreviaDaEntrada) : Resultado

    /**
     * O código não resolve. Cobre dois casos que o usuário não distingue e nem
     * precisa: não existe documento em `invites/{codigo}`, ou ele aponta para um plano
     * que não existe mais.
     */
    data object NaoEncontrado : Resultado

    /**
     * Já é membro. O nome vem junto porque *"você já está neste plano"* sem dizer qual
     * plano é uma resposta pior do que parece: o código pode ter vindo de outra pessoa,
     * de outro plano, e o que a pessoa quer saber é qual dos dois ela já tem.
     */
    data class JaMembro(val nome: String) : Resultado
}
