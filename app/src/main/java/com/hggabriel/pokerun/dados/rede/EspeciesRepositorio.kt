package com.hggabriel.pokerun.dados.rede

import android.content.Context
import androidx.core.content.edit
import com.hggabriel.pokerun.dominio.regras.nomeDaEspecie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val PREFERENCIAS = "nomes_das_especies"
private const val PRAZO_MS = 8_000

/**
 * Nome e sprite de uma espécie, pela PokéAPI (D-10, `F2-T14`). Da PokéAPI saem **só**
 * nome e sprite: tier e curva vêm da definição da temporada (decisão nº 102).
 *
 * - **Nome:** `pokemon-species/{id}`, em inglês, guardado depois da primeira leitura.
 *   Sem rede e sem cópia guardada, nulo, e o card mostra só o número.
 * - **Sprite:** a arte oficial pelo número, sem chamada de API. Quem baixa e guarda em
 *   disco é o Coil.
 *
 * Nenhum trabalho em segundo plano: a busca acontece quando o card é desenhado.
 */
class EspeciesRepositorio(contexto: Context) {

    private val guardados = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)

    suspend fun nome(numero: Int): String? {
        guardados.getString(numero.toString(), null)?.let { return it }
        val lido = withContext(Dispatchers.IO) { buscar(numero) } ?: return null
        guardados.edit { putString(numero.toString(), lido) }
        return lido
    }

    fun sprite(numero: Int): String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$numero.png"

    private fun buscar(numero: Int): String? {
        val conexao = URL("https://pokeapi.co/api/v2/pokemon-species/$numero/").openConnection() as HttpURLConnection
        return try {
            conexao.connectTimeout = PRAZO_MS
            conexao.readTimeout = PRAZO_MS
            if (conexao.responseCode != HttpURLConnection.HTTP_OK) return null
            nomeDaEspecie(conexao.inputStream.bufferedReader().use { it.readText() })
        } catch (_: IOException) {
            null
        } finally {
            conexao.disconnect()
        }
    }
}
