package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Semana

/**
 * A aderência da semana e do plano (RN-08, RN-10, RN-11, RN-19, RN-26).
 *
 * **Conta sessões, nunca quilômetros** (D-06). A escolha é uma defesa contra erro de
 * medição: metade do grupo corre com relógio sem GPS próprio, e uma aderência
 * baseada em distância transformaria hardware ruim em falta de disciplina. É
 * também o que torna o número comparável entre oito pessoas com oito aparelhos
 * diferentes.
 *
 * O longão é a exceção que confirma: ele tem alvo em quilômetros, mas é
 * **indicador de tela** e fica fora da conta (RN-10).
 */
object CalculoDeAderencia {

    /** O longão conta como cumprido a partir de 90% do alvo (RN-10). */
    private const val PISO_DO_LONGAO = 0.9

    /**
     * // RN-08
     *
     * `sessões feitas ÷ sessões previstas`, no intervalo de 0 a 1.
     *
     * O teto vem de [sessoesFeitas], que nunca passa de [Semana.sessoesAlvo]: a
     * semana de 3 previstas com 4 corridas dá 100%, e não 133%.
     */
    fun daSemana(semana: Semana, corridas: List<Corrida>): Double =
        sessoesFeitas(semana, corridas).toDouble() / semana.sessoesAlvo

    /**
     * Quantas sessões previstas a semana teve cumpridas, **no máximo
     * [Semana.sessoesAlvo]**.
     *
     * O teto é por semana porque a sessão é um **slot**: RN-34 diz que cada sessão
     * prevista aceita no máximo uma corrida, então a 4ª corrida de uma semana de 3
     * não reivindica nada. É o que impede uma semana cheia de compensar uma semana
     * vazia no acumulado do plano.
     *
     * Corrida descartada (RN-31) e corrida substituída (RN-24) ficam de fora. A
     * primeira é duplicata, bicicleta marcada como corrida ou corrida fantasma; a
     * segunda já foi contada pela correção que a substituiu, e somar as duas
     * transformaria uma edição em duas sessões.
     */
    fun sessoesFeitas(semana: Semana, corridas: List<Corrida>): Int =
        minOf(validasDa(semana, corridas).size, semana.sessoesAlvo)

    /**
     * // RN-10
     *
     * Se o longão da semana foi cumprido: a **maior** corrida da semana chegou a
     * pelo menos 90% do [Semana.longaoKm].
     *
     * **Nulo quando a semana não planeja longão** — a 2ª de taper e a semana da
     * prova. Ali não há o que cumprir, e devolver `false` faria a
     * `WeekDetailScreen` mostrar um X vermelho para quem seguiu o plano à risca.
     *
     * Nunca entra na aderência (D-06). É indicador de tela.
     */
    fun longaoCumprido(semana: Semana, corridas: List<Corrida>): Boolean? {
        val alvo = semana.longaoKm ?: return null
        val maior = validasDa(semana, corridas).maxOfOrNull { it.km } ?: 0.0
        return maior >= PISO_DO_LONGAO * alvo
    }

    /**
     * // RN-19
     *
     * A aderência acumulada de um membro, da semana em que ele entrou até
     * [ateSemana], inclusive.
     *
     * Quem entra na semana 8 **não carrega as sete semanas em que não estava no
     * plano**: o denominador começa onde ele começou. Sem isso, entrar depois seria
     * uma dívida impagável, e o grupo perde exatamente as pessoas que chegaram por
     * último.
     *
     * Soma slots cumpridos sobre slots previstos, e não a média das aderências
     * semanais. As duas leituras coincidem quando as semanas têm o mesmo número de
     * sessões previstas e divergem na semana da prova, que prevê uma só: pela soma,
     * ela pesa o que vale; pela média, ela pesaria como uma semana inteira.
     *
     * Semana perdida entra como zero e não gera penalidade além disso (RN-11).
     */
    fun doPlano(
        grade: List<Semana>,
        corridas: List<Corrida>,
        membro: Membro,
        ateSemana: Int,
    ): Double {
        val janela = grade.filter { it.numero in membro.entrouNaSemana..ateSemana }
        val previstas = janela.sumOf { it.sessoesAlvo }
        if (previstas == 0) return 0.0
        return janela.sumOf { sessoesFeitas(it, corridas) }.toDouble() / previstas
    }

    /**
     * As corridas que contam para [semana].
     *
     * O filtro é por `semana_ref`, que é **snapshot gravado na gravação** (RN-02) e
     * já saiu do fuso do plano (RN-28). Recalcular a semana aqui reintroduziria o
     * bug de fuso num lugar onde ninguém iria procurar por ele.
     *
     * **Público desde `F1-T09`, e por reúso e não por conveniência:** a barra de
     * sessões e a grade de dias do card da semana precisam exatamente deste
     * conjunto. Uma segunda cópia do filtro faria a barra mostrar uma corrida
     * descartada que a fração ao lado não conta — duas respostas para a mesma
     * pergunta, na mesma altura da tela.
     */
    fun validasDa(semana: Semana, corridas: List<Corrida>): List<Corrida> =
        corridas.filter {
            it.semanaRef == semana.numero && !it.descartada && !it.substituida
        }
}
