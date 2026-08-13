package com.hggabriel.pokerun.ui.telas.criarplano

import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import java.time.LocalDate

/**
 * O estado da `CreatePlanScreen` (`F1-T10`, docs/03 §3.5).
 *
 * **É um estado só, e não uma máquina**, porque a tela é um formulário e nada mais: ela
 * não grava, não lê e não espera servidor. Quem grava o plano é a revisão do rascunho
 * (`F1-T11`), depois de o usuário conferir a grade — a rota `RevisarRascunho` carrega os
 * parâmetros de entrada, e não a grade gerada (`Rotas.kt`).
 *
 * Isso é o que dispensa `Carregando`, `Salvando` e `Falhou` aqui. O único estado externo
 * é [online], que não é do formulário: é do aparelho.
 *
 * [baselinePreenchida] existe para o campo não ser sobrescrito duas vezes. O valor chega
 * de `users/{uid}.baseline_km` numa leitura assíncrona, e sem a marca cada emissão do
 * perfil apagaria o que o usuário estivesse digitando naquele campo.
 */
data class CriarPlanoUiState(
    val nome: String = "",
    val dataProva: LocalDate? = null,
    val alvo: String = "",
    val baseline: String = "",
    val sessoesPorSemana: Int = SESSOES_PADRAO,
    val erros: ErrosDoPlano = ErrosDoPlano(),
    /**
     * O total de semanas que a data escolhida dá, ou nulo enquanto ela não serve. Fica no
     * estado, e não numa chamada ao `ViewModel` durante a composição: a tela desenha o que
     * o estado diz, e nada mais.
     */
    val semanas: Int? = null,
    val online: Boolean = true,
    val baselinePreenchida: Boolean = false,
    /** O calendário está aberto. Fica no estado para sobreviver à rotação. */
    val escolhendoData: Boolean = false,
    /** Terminal: os campos passaram e a tela sai para a revisão. Ver [RevisarRascunho]. */
    val rascunho: RevisarRascunho? = null,
) {
    /**
     * As opções do seletor de sessões (docs/01 §3.1). **Não existe campo de semana leve
     * nem de volume** (D-14): o volume deriva do longão, e um segundo campo faria os dois
     * divergirem.
     */
    companion object {
        val SESSOES = listOf(2, 3, 4)

        /** 3 é o meio da faixa e o exemplo que docs/01 §3.2 usa para a grade inteira. */
        const val SESSOES_PADRAO = 3
    }
}
