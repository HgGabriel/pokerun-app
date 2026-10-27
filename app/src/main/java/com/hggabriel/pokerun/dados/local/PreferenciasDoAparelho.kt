package com.hggabriel.pokerun.dados.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * As preferências que valem por aparelho, e não por conta (estudo de Progresso e Perfil,
 * Revisão 3). Hoje é uma só: o som e a vibração na captura, **desligada por padrão**.
 *
 * `SharedPreferences`, como o badge da aba Pokédex: uma chave booleana não paga uma
 * dependência nova (DataStore).
 */
class PreferenciasDoAparelho(contexto: Context) {

    private val arquivo = contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    private val _somNaCaptura = MutableStateFlow(arquivo.getBoolean(SOM_NA_CAPTURA, false))
    val somNaCaptura: StateFlow<Boolean> = _somNaCaptura.asStateFlow()

    fun definirSomNaCaptura(ligado: Boolean) {
        arquivo.edit().putBoolean(SOM_NA_CAPTURA, ligado).apply()
        _somNaCaptura.value = ligado
    }

    /**
     * A maior coleção que a aba Pokédex já mostrou nesta temporada, ou nulo na primeira
     * visita. Acima dela, a coleção nova chega num pacote (docs/03 §3.18).
     */
    fun colecaoVista(temporadaId: String): Int? =
        arquivo.getInt(COLECAO_VISTA + temporadaId, -1).takeIf { it >= 0 }

    fun definirColecaoVista(temporadaId: String, colecao: Int) {
        arquivo.edit().putInt(COLECAO_VISTA + temporadaId, colecao).apply()
    }

    /** A cascata de estreia do álbum já tocou neste aparelho: ela toca uma vez só. */
    val cascataVista: Boolean get() = arquivo.getBoolean(CASCATA_VISTA, false)

    fun marcarCascataVista() {
        arquivo.edit().putBoolean(CASCATA_VISTA, true).apply()
    }

    private companion object {
        const val ARQUIVO = "preferencias"
        const val SOM_NA_CAPTURA = "som_na_captura"
        const val COLECAO_VISTA = "colecao_vista_"
        const val CASCATA_VISTA = "cascata_vista"
    }
}
