package com.hggabriel.pokerun.dominio.regras

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val leitor = Json { ignoreUnknownKeys = true }

/** A altura e o peso da espécie, para o verso da figurinha (docs/03 §3.19). */
data class FichaDaEspecie(val alturaM: Double, val pesoKg: Double)

/**
 * A ficha de `pokemon/{id}`: `height` em decímetros e `weight` em hectogramas. Nula sem
 * um dos dois ou com resposta que não é JSON. Como o nome, é dado de exibição.
 */
fun fichaDaEspecie(json: String): FichaDaEspecie? = try {
    val raiz = leitor.parseToJsonElement(json).jsonObject
    val altura = raiz["height"]?.jsonPrimitive?.intOrNull
    val peso = raiz["weight"]?.jsonPrimitive?.intOrNull
    if (altura == null || peso == null) null else FichaDaEspecie(altura / 10.0, peso / 10.0)
} catch (_: Exception) {
    null
}

/** O endereço de `evolution-chain` em `pokemon-species/{id}`, ou nulo. */
fun cadeiaDaEspecie(json: String): String? = try {
    (leitor.parseToJsonElement(json).jsonObject["evolution_chain"] as? JsonObject)?.get("url")?.jsonPrimitive?.content
} catch (_: Exception) {
    null
}

/**
 * A cadeia de evolução em estágios, do primeiro ao último, pelo número da espécie. Só
 * entram as espécies da temporada ([permitidos]): o ramo de outra região sai e o estágio
 * que fica vazio some. Lista vazia com resposta que não é JSON.
 */
fun estagiosDaEvolucao(json: String, permitidos: Set<Int>): List<List<Int>> = try {
    val estagios = mutableListOf<MutableList<Int>>()
    fun descer(no: JsonElement, nivel: Int) {
        val objeto = no.jsonObject
        val numero = (objeto["species"] as? JsonObject)?.get("url")?.jsonPrimitive?.content
            ?.trimEnd('/')?.substringAfterLast('/')?.toIntOrNull()
        while (estagios.size <= nivel) estagios.add(mutableListOf())
        if (numero != null && numero in permitidos) estagios[nivel].add(numero)
        objeto["evolves_to"]?.jsonArray?.forEach { descer(it, nivel + 1) }
    }
    descer(leitor.parseToJsonElement(json).jsonObject.getValue("chain"), 0)
    estagios.filter { it.isNotEmpty() }
} catch (_: Exception) {
    emptyList()
}
