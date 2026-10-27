package com.hggabriel.pokerun.dados.local

import android.content.Context
import com.hggabriel.pokerun.dominio.regras.Treinador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A lista dos nomes de treinador do Showdown, embutida em `assets/treinadores.txt`
 * (estudo de Progresso e Perfil, Revisão 3). O arquivo leva só os nomes: o sprite vem do
 * servidor do Showdown e fica no cache de disco do Coil.
 *
 * A lista foi gerada da listagem `play.pokemonshowdown.com/sprites/trainers/` em 27/10/2026,
 * com 1.500 nomes. Ler o HTML da listagem em tempo real seria frágil: é a página de um site,
 * e não uma API.
 */
class TreinadoresRepositorio(private val contexto: Context) {

    @Volatile
    private var lida: List<String>? = null

    suspend fun lista(): List<String> = lida ?: withContext(Dispatchers.IO) {
        contexto.assets.open(ARQUIVO).bufferedReader(Charsets.UTF_8).use { Treinador.lerLista(it.readText()) }
    }.also { lida = it }

    private companion object {
        const val ARQUIVO = "treinadores.txt"
    }
}
