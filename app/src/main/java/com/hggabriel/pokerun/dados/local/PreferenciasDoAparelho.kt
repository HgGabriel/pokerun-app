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

    private companion object {
        const val ARQUIVO = "preferencias"
        const val SOM_NA_CAPTURA = "som_na_captura"
    }
}
