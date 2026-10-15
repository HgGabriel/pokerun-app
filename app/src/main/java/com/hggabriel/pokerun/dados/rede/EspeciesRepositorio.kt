package com.hggabriel.pokerun.dados.rede

import android.content.Context
import androidx.core.content.edit
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.nomeDaEspecie
import com.hggabriel.pokerun.dominio.regras.tiposDaEspecie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val PREFERENCIAS = "nomes_das_especies"
private const val PREFERENCIAS_DE_TIPO = "tipos_das_especies"
private const val PRAZO_MS = 8_000

/**
 * Nome, tipos e sprite de uma espécie, pela PokéAPI (D-10, `F2-T14`). Da PokéAPI sai
 * **só dado de exibição**: tier e curva vêm da definição da temporada (decisão nº 102).
 *
 * - **Nome:** `pokemon-species/{id}`, em inglês, guardado depois da primeira leitura.
 *   Sem rede e sem cópia guardada, nulo, e o card mostra só o número.
 * - **Tipos:** `pokemon/{id}`, na ordem do slot, guardados depois da primeira leitura.
 *   Sem rede e sem cópia guardada, lista vazia, e a ficha de espécie fica com a metade
 *   de cima em `superficie-afundada` e sem chip.
 * - **Sprite:** a arte oficial pelo número, sem chamada de API. Quem baixa e guarda em
 *   disco é o Coil.
 *
 * Nenhum trabalho em segundo plano: a busca acontece quando o card é desenhado.
 */
class EspeciesRepositorio(contexto: Context) {

    private val guardados = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    private val tiposGuardados = contexto.getSharedPreferences(PREFERENCIAS_DE_TIPO, Context.MODE_PRIVATE)

    suspend fun nome(numero: Int): String? {
        guardados.getString(numero.toString(), null)?.let { return it }
        val lido = withContext(Dispatchers.IO) {
            buscar("https://pokeapi.co/api/v2/pokemon-species/$numero/")?.let(::nomeDaEspecie)
        } ?: return null
        guardados.edit { putString(numero.toString(), lido) }
        return lido
    }

    /**
     * Guardado como os `id` do enum separados por vírgula (`planta,venenoso`). Uma lista
     * vazia não é guardada: sem rede, a próxima abertura tenta de novo.
     */
    suspend fun tipos(numero: Int): List<TipoDePokemon> {
        tiposGuardados.getString(numero.toString(), null)?.let { guardado ->
            return guardado.split(',').mapNotNull { id -> TipoDePokemon.entries.firstOrNull { it.id == id } }
        }
        val lidos = withContext(Dispatchers.IO) {
            buscar("https://pokeapi.co/api/v2/pokemon/$numero/")?.let(::tiposDaEspecie)
        }.orEmpty()
        if (lidos.isNotEmpty()) {
            tiposGuardados.edit { putString(numero.toString(), lidos.joinToString(",") { it.id }) }
        }
        return lidos
    }

    fun sprite(numero: Int): String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$numero.png"

    /** O corpo da resposta, ou nulo sem rede ou com resposta que não é 200. */
    private fun buscar(endereco: String): String? {
        val conexao = URL(endereco).openConnection() as HttpURLConnection
        return try {
            conexao.connectTimeout = PRAZO_MS
            conexao.readTimeout = PRAZO_MS
            if (conexao.responseCode != HttpURLConnection.HTTP_OK) return null
            conexao.inputStream.bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        } finally {
            conexao.disconnect()
        }
    }
}
