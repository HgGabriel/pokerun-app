package com.hggabriel.pokerun.dados.rede

import android.content.Context
import androidx.core.content.edit
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.FichaDaEspecie
import com.hggabriel.pokerun.dominio.regras.cadeiaDaEspecie
import com.hggabriel.pokerun.dominio.regras.estagiosDaEvolucao
import com.hggabriel.pokerun.dominio.regras.fichaDaEspecie
import com.hggabriel.pokerun.dominio.regras.nomeDaEspecie
import com.hggabriel.pokerun.dominio.regras.tiposDaEspecie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
 * - **Ficha e evolução:** `pokemon/{id}`, `pokemon-species/{id}` e `evolution-chain/{id}`,
 *   para o verso da figurinha. A resposta crua fica em disco depois da primeira leitura.
 * - **Grito:** o `.ogg` de `cries.latest` pelo número, sem chamada de API.
 *
 * Nenhum trabalho em segundo plano: a busca acontece quando o card é desenhado.
 */
class EspeciesRepositorio(contexto: Context) {

    private val respostas = File(contexto.filesDir, "pokeapi")

    private val guardados = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    private val tiposGuardados = contexto.getSharedPreferences(PREFERENCIAS_DE_TIPO, Context.MODE_PRIVATE)

    /** O nome já guardado no aparelho, sem ir à rede: para a tela abrir pronta. */
    fun nomeGuardado(numero: Int): String? = guardados.getString(numero.toString(), null)

    /** Os tipos já guardados no aparelho, sem ir à rede. Lista vazia se nunca vieram. */
    fun tiposGuardados(numero: Int): List<TipoDePokemon> =
        tiposGuardados.getString(numero.toString(), null)
            ?.split(',')?.mapNotNull { id -> TipoDePokemon.entries.firstOrNull { it.id == id } }
            .orEmpty()

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

    /** Altura e peso, de `pokemon/{id}`. Nulo sem rede e sem cópia em disco. */
    suspend fun ficha(numero: Int): FichaDaEspecie? =
        lerGuardado("pokemon_$numero", "https://pokeapi.co/api/v2/pokemon/$numero/")?.let(::fichaDaEspecie)

    /**
     * Os estágios da evolução, só com as espécies da temporada ([permitidos]). Lista vazia
     * sem rede e sem cópia em disco: o verso esconde o cartão.
     */
    suspend fun evolucao(numero: Int, permitidos: Set<Int>): List<List<Int>> {
        val especie = lerGuardado("especie_$numero", "https://pokeapi.co/api/v2/pokemon-species/$numero/")
        val endereco = especie?.let(::cadeiaDaEspecie) ?: return emptyList()
        val chave = "cadeia_" + endereco.trimEnd('/').substringAfterLast('/')
        return lerGuardado(chave, endereco)?.let { estagiosDaEvolucao(it, permitidos) }.orEmpty()
    }

    /** O grito da espécie, o `cries.latest` da PokéAPI. Quem toca e guarda é o player. */
    fun grito(numero: Int): String = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/$numero.ogg"

    /**
     * A resposta pela [chave] em disco, ou da rede na primeira vez. A escrita vai para um
     * arquivo temporário e depois troca de nome, para nunca deixar um JSON pela metade.
     */
    private suspend fun lerGuardado(chave: String, endereco: String): String? = withContext(Dispatchers.IO) {
        val arquivo = File(respostas, "$chave.json")
        if (arquivo.exists()) return@withContext arquivo.readText()
        val lido = buscar(endereco) ?: return@withContext null
        try {
            respostas.mkdirs()
            val temporario = File(respostas, "$chave.tmp")
            temporario.writeText(lido)
            temporario.renameTo(arquivo)
        } catch (_: IOException) {
            // Sem disco, a leitura vale para esta abertura e a próxima busca de novo.
        }
        lido
    }

    fun sprite(numero: Int): String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$numero.png"

    /**
     * O render 3D do Pokémon HOME, para a espécie em destaque na Home (26/10/2026). Nem toda
     * espécie tem: quem usa leva [sprite], a arte oficial, como reserva.
     */
    fun spriteDoHome(numero: Int): String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$numero.png"

    /**
     * O ícone de 32px da Geração III, o marcador da pista na Home (29/10/2026). Existe até o
     * número 386, e pede escala inteira e sem filtro para o pixel não borrar.
     */
    fun iconeDeJogo(numero: Int): String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-iii/icons/$numero.png"

    /**
     * O sprite de jogo pelo número (docs/02 §4.4): 96px, sem filtro de suavização, para o
     * item da Pokédex (`spriteLista` 72dp) e o avatar do ranking.
     */
    fun spriteDeJogo(numero: Int): String = spriteDeJogoDe(numero)

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

private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

/** O sprite de jogo pelo número: 96px, parado. É a reserva do sprite animado. */
fun spriteDeJogoDe(numero: Int): String = "$SPRITES/$numero.png"

/**
 * Os sprites animados da arena do registro de corrida, do melhor para o pior (26/10/2026).
 * Quem usa tenta em ordem e passa ao seguinte quando a espécie não existe naquela fonte.
 *
 * Os quatro primeiros são os modelos 3D animados dos jogos: Let's Go em GIF de 384px (só Kanto),
 * Sword/Shield, BDSP e Legends Arceus em WebP animado de 512px. Depois, o render parado do HOME
 * (512px), o GIF do Showdown, que é pixel art pequena, e a arte oficial no fim.
 */
fun spritesAnimadosDe(numero: Int): List<String> = listOf(
    "$SPRITES/versions/generation-vii/lets-go-pikachu-lets-go-eevee/$numero.gif",
    "$SPRITES/versions/generation-viii/sword-shield/$numero.webp",
    "$SPRITES/versions/generation-viii/brilliant-diamond-shining-pearl/$numero.webp",
    "$SPRITES/versions/generation-viii/legends-arceus/$numero.webp",
    "$SPRITES/other/home/$numero.png",
    "$SPRITES/other/showdown/$numero.gif",
    "$SPRITES/other/official-artwork/$numero.png",
)

/** Se o sprite é pixel art, que se amplia sem suavização. */
fun spriteEmPixel(url: String): Boolean = "/showdown/" in url
