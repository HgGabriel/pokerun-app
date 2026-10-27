package com.hggabriel.pokerun.dados.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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

    private val vistasLidas = MutableStateFlow<Map<String, Set<Int>?>>(emptyMap())

    /**
     * As espécies coladas que a pessoa já abriu no detalhe nesta temporada, ou nulo antes da
     * primeira semente. A colada fora daqui leva o selo `nova` (27/10/2026: "nova" é "ainda
     * não vista", e não mais "colada nesta semana").
     */
    fun vistas(temporadaId: String): Flow<Set<Int>?> {
        if (temporadaId !in vistasLidas.value) {
            val lidas = arquivo.getStringSet(VISTAS + temporadaId, null)?.mapNotNull { it.toIntOrNull() }?.toSet()
            vistasLidas.update { it + (temporadaId to lidas) }
        }
        return vistasLidas.map { it[temporadaId] }
    }

    /** A semente da primeira visita: as coladas que já não eram novas. */
    fun semearVistas(temporadaId: String, numeros: Set<Int>) {
        if (vistasLidas.value[temporadaId] != null) return
        gravarVistas(temporadaId, numeros)
    }

    /** A pessoa abriu a colada [numero] no detalhe: o selo `nova` sai dela. */
    fun marcarVista(temporadaId: String, numero: Int) {
        vistas(temporadaId)
        val atuais = vistasLidas.value[temporadaId] ?: return
        if (numero in atuais) return
        gravarVistas(temporadaId, atuais + numero)
    }

    private fun gravarVistas(temporadaId: String, numeros: Set<Int>) {
        arquivo.edit().putStringSet(VISTAS + temporadaId, numeros.mapTo(HashSet()) { it.toString() }).apply()
        vistasLidas.update { it + (temporadaId to numeros) }
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
        const val VISTAS = "especies_vistas_"
    }
}
