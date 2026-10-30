package com.hggabriel.pokerun.dados.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * As preferências guardadas no aparelho (estudo de Progresso e Perfil, Revisão 3): o som e a
 * vibração na captura, **desligada por padrão**, que vale para o aparelho; e o estado do álbum
 * (coleção vista, espécies vistas, cascata de estreia), que vale por conta.
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

    // -----------------------------------------------------------------------
    // O álbum (docs/03 §3.18). Tudo por conta e por temporada desde 29/10/2026: com a chave
    // só por temporada, a coleção vista de uma conta valia para a outra no mesmo aparelho, e
    // o pacote não chegava enquanto a coleção nova não passasse da vista da outra.
    // -----------------------------------------------------------------------

    private val colecoesLidas = MutableStateFlow<Map<String, Int?>>(emptyMap())

    /**
     * A maior coleção que a aba Pokédex já mostrou à conta [uid] nesta temporada, ou nulo na
     * primeira visita. Acima dela, a coleção nova chega num pacote. Muda na hora em que o
     * pacote abre, para o badge da barra apagar junto.
     */
    fun colecaoVista(uid: String, temporadaId: String): Flow<Int?> {
        val chave = chave(uid, temporadaId)
        if (chave !in colecoesLidas.value) {
            val lida = arquivo.getInt(COLECAO_VISTA + chave, -1).takeIf { it >= 0 }
            colecoesLidas.update { it + (chave to lida) }
        }
        return colecoesLidas.map { it[chave] }
    }

    fun definirColecaoVista(uid: String, temporadaId: String, colecao: Int) {
        val chave = chave(uid, temporadaId)
        arquivo.edit().putInt(COLECAO_VISTA + chave, colecao).apply()
        colecoesLidas.update { it + (chave to colecao) }
    }

    private val vistasLidas = MutableStateFlow<Map<String, Set<Int>?>>(emptyMap())

    /**
     * As espécies coladas que a conta já abriu no detalhe nesta temporada, ou nulo antes da
     * primeira semente. A colada fora daqui leva o selo `nova` (27/10/2026: "nova" é "ainda
     * não vista", e não mais "colada nesta semana").
     */
    fun vistas(uid: String, temporadaId: String): Flow<Set<Int>?> {
        val chave = chave(uid, temporadaId)
        if (chave !in vistasLidas.value) {
            val lidas = arquivo.getStringSet(VISTAS + chave, null)?.mapNotNull { it.toIntOrNull() }?.toSet()
            vistasLidas.update { it + (chave to lidas) }
        }
        return vistasLidas.map { it[chave] }
    }

    /** A semente da primeira visita: as coladas que já não eram novas. */
    fun semearVistas(uid: String, temporadaId: String, numeros: Set<Int>) {
        if (vistasLidas.value[chave(uid, temporadaId)] != null) return
        gravarVistas(chave(uid, temporadaId), numeros)
    }

    /** A conta abriu a colada [numero] no detalhe: o selo `nova` sai dela. */
    fun marcarVista(uid: String, temporadaId: String, numero: Int) {
        vistas(uid, temporadaId)
        val chave = chave(uid, temporadaId)
        val atuais = vistasLidas.value[chave] ?: return
        if (numero in atuais) return
        gravarVistas(chave, atuais + numero)
    }

    /** O puxar da Pokédex: o selo `nova` sai de todas as [numeros] de uma vez, sem abrir uma a uma. */
    fun marcarVistas(uid: String, temporadaId: String, numeros: Set<Int>) {
        vistas(uid, temporadaId)
        val chave = chave(uid, temporadaId)
        val atuais = vistasLidas.value[chave] ?: emptySet()
        if (atuais.containsAll(numeros)) return
        gravarVistas(chave, atuais + numeros)
    }

    private fun gravarVistas(chave: String, numeros: Set<Int>) {
        arquivo.edit().putStringSet(VISTAS + chave, numeros.mapTo(HashSet()) { it.toString() }).apply()
        vistasLidas.update { it + (chave to numeros) }
    }

    /** A cascata de estreia do álbum já tocou para a conta [uid] neste aparelho: ela toca uma vez só. */
    fun cascataVista(uid: String): Boolean = arquivo.getBoolean(CASCATA_VISTA + uid, false)

    fun marcarCascataVista(uid: String) {
        arquivo.edit().putBoolean(CASCATA_VISTA + uid, true).apply()
    }

    private fun chave(uid: String, temporadaId: String) = "${uid}_$temporadaId"

    private companion object {
        const val ARQUIVO = "preferencias"
        const val SOM_NA_CAPTURA = "som_na_captura"
        const val COLECAO_VISTA = "colecao_vista_"
        const val CASCATA_VISTA = "cascata_vista_"
        const val VISTAS = "especies_vistas_"
    }
}
