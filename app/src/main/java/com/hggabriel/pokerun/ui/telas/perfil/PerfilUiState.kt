package com.hggabriel.pokerun.ui.telas.perfil

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon

/**
 * O parceiro como a credencial o mostra: o número, o nome e os tipos da PokéAPI, e o render
 * parado do HOME (decisão 13: o parceiro em três alturas).
 */
data class ParceiroDaCredencial(
    val numero: Int,
    val nome: String?,
    val tipos: List<TipoDePokemon>,
    val render: String,
    val reserva: String,
)

/**
 * O estado do Perfil (estudo de Progresso e Perfil, Revisão 3): a credencial no topo e a
 * lista de três cartões abaixo.
 *
 * **É um estado só, e não uma máquina**: cada linha da lista é uma opção independente.
 */
data class PerfilUiState(
    val carregando: Boolean = true,
    /** O que o campo de nome mostra: a edição em curso, ou o nome gravado. */
    val nome: String = "",
    val treinador: String? = null,
    val parceiro: ParceiroDaCredencial? = null,
    val credencial: DadosDaCredencial? = null,
    /** As insígnias da vida, presas no cordão como broches (decisão 19). */
    val insignias: List<String> = emptyList(),
    val baselineKm: Double? = null,
    val somNaCaptura: Boolean = false,
    val versao: String = "",

    // A folha da baseline: aberta, o texto do placar e o erro dele.
    val editandoBaseline: Boolean = false,
    val baselineDigitada: String = "",
    @param:StringRes val erroDaBaseline: Int? = null,

    /** O diálogo de sair. Fica no estado para sobreviver à rotação. */
    val confirmandoSaida: Boolean = false,
    /** Terminal: a sessão foi encerrada e a tela sai. */
    val saiuDaConta: Boolean = false,
    /** Falha de leitura do perfil. Sai pelo `BannerDeAlerta`. */
    @param:StringRes val erroDeTela: Int? = null,
    /**
     * Sobe a cada troca de parceiro que esta tela vê, para a credencial balançar uma vez
     * (decisão 14). A primeira leitura não conta.
     */
    val trocasDeParceiro: Int = 0,
)
