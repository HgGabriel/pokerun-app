package com.hggabriel.pokerun.ui.telas.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A corrida que acabou de ser registrada, à espera de a Home responder (docs/03 §3.3).
 *
 * @property calculado se a simulação do ganho já voltou. Antes disso a Home só enche o
 *   segmento; o snackbar espera o número.
 * @property xp o `+N XP`, ou nulo quando a simulação falhou, demorou ou não achou ganho.
 */
data class CorridaRegistrada(
    val corridaId: String,
    val calculado: Boolean = false,
    val xp: Long? = null,
    /**
     * A pessoa ainda está no resultado da arena, que mostra o ganho quando ele chega: a Home
     * carimba a vaga e não repete o número. Quem sai da arena antes do número recebe o
     * snackbar na Home (29/10/2026).
     */
    val naArena: Boolean = true,
)

/**
 * O recado do registro manual para a Home.
 *
 * **Vive no container, e não num `ViewModel`,** porque quem escreve e quem lê não estão
 * vivos ao mesmo tempo: a `ManualRunScreen` sai antes de o ganho ser calculado, e a Home
 * só volta a compor depois que ela sai. É o mesmo desenho do pedido da Pokédex no
 * `RoteadorDaAbertura`.
 *
 * As duas etapas existem por causa do tempo de cada resposta. O ID chega na hora, e com
 * ele o segmento enche assim que a Home aparece; o XP chega quando a simulação volta, e
 * só então o snackbar sai. Toda escrita confere o ID: um cálculo atrasado de uma corrida
 * anterior não sobrescreve o aviso da corrida nova.
 */
class AvisoDeCorrida {

    private val atualMutavel = MutableStateFlow<CorridaRegistrada?>(null)

    val atual: StateFlow<CorridaRegistrada?> = atualMutavel.asStateFlow()

    fun registrada(corridaId: String) {
        atualMutavel.value = CorridaRegistrada(corridaId)
    }

    fun ganhoCalculado(corridaId: String, xp: Long?) = atualMutavel.update { aviso ->
        if (aviso?.corridaId == corridaId) aviso.copy(calculado = true, xp = xp) else aviso
    }

    /** A pessoa saiu da arena. Se o ganho ainda não tinha chegado, a Home é quem o mostra. */
    fun saiuDaArena(corridaId: String) = atualMutavel.update { aviso ->
        if (aviso?.corridaId == corridaId && !aviso.calculado) aviso.copy(naArena = false) else aviso
    }

    /** A Home já respondeu: o snackbar saiu e o segmento encheu. */
    fun consumir(corridaId: String) = atualMutavel.update { aviso ->
        if (aviso?.corridaId == corridaId) null else aviso
    }
}
