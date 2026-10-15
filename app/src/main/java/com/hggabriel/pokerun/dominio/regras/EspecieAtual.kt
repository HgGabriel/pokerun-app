package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant

/** Aos 14 dias sem corrida o sprite esvanece para silhueta (RN-36, XP-07). */
private val DIAS_ATE_ESVANECER: Duration = Duration.ofDays(14)

/**
 * O que o card da espécie atual mostra (docs/01 §2.1, D-19).
 *
 * @property posicao a posição na escada, de 1 a `total_especies` (decisão nº 118).
 * @property numero o número da Pokédex da espécie naquela posição, que dá nome e sprite.
 * @property proximoNumero a próxima espécie da escada (XP-12). Nulo na última.
 * @property faltamXp o XP que falta para ultrapassá-la. Nulo na última.
 */
data class EspecieAtual(
    val posicao: Int,
    val numero: Int,
    val proximoNumero: Int?,
    val faltamXp: Long?,
    val silhueta: Boolean,
)

/**
 * A escada da temporada: o número de cada espécie, na ordem dos tiers e, dentro de cada
 * um, na ordem da lista, com o XP acumulado para ultrapassá-la (docs/04 §4).
 */
private fun escada(temporada: Temporada): List<Pair<Int, Long>> {
    var acumulado = 0L
    return temporada.tiers.sortedBy { it.ordem }.flatMap { tier ->
        tier.especies.map { numero ->
            acumulado += tier.xpPorEspecie
            numero to acumulado
        }
    }
}

/**
 * // XP-12
 *
 * A espécie atual de quem tem [xp] na temporada. A posição é a de `posicaoNaEscada`, a
 * mesma que o replay grava; o alvo é a espécie seguinte da escada, e o que falta é o
 * custo acumulado dela menos o XP. A silhueta segue RN-36: 14 dias desde a última
 * corrida. Quem não correu na temporada não esvanece: ainda não parou de nada.
 */
fun especieAtual(xp: Long, ultimaCorridaEm: Instant?, temporada: Temporada, agora: Instant): EspecieAtual {
    val degraus = escada(temporada)
    val posicao = posicaoNaEscada(xp, temporada)
    val proximo = degraus.getOrNull(posicao)
    return EspecieAtual(
        posicao = posicao,
        numero = degraus[posicao - 1].first,
        proximoNumero = proximo?.first,
        faltamXp = proximo?.let { it.second - xp },
        silhueta = ultimaCorridaEm != null && Duration.between(ultimaCorridaEm, agora) >= DIAS_ATE_ESVANECER,
    )
}

/** O número da Pokédex numa posição da escada: o avatar do ranking a partir dela. */
fun numeroNaEscada(posicao: Int, temporada: Temporada): Int {
    val degraus = escada(temporada)
    return degraus[(posicao - 1).coerceIn(0, degraus.lastIndex)].first
}

private val leitor = Json { ignoreUnknownKeys = true }

/**
 * O nome da espécie em inglês, de `pokemon-species/{id}` (D-10, ficha de `F2-T14`): o
 * `name` de `pokemon/{id}` é slug (`mr-mime`). Nulo sem registro em inglês ou com
 * resposta que não é JSON.
 */
fun nomeDaEspecie(json: String): String? = try {
    leitor.parseToJsonElement(json).jsonObject["names"]?.jsonArray
        ?.map { it.jsonObject }
        ?.firstOrNull { (it["language"] as? JsonObject)?.get("name")?.jsonPrimitive?.content == "en" }
        ?.get("name")?.jsonPrimitive?.content
} catch (_: Exception) {
    null
}

/**
 * Os tipos da espécie, de `pokemon/{id}`, na ordem do `slot`: o primeiro é o primário,
 * que pinta a metade de cima da ficha de espécie (docs/02 §4.3). Tipo que o app não
 * conhece fica de fora. Lista vazia sem `types` ou com resposta que não é JSON.
 *
 * Como o nome, o tipo é dado de exibição. Tier e curva continuam vindo da definição da
 * temporada, nunca da PokéAPI (decisão nº 102).
 */
fun tiposDaEspecie(json: String): List<TipoDePokemon> = try {
    leitor.parseToJsonElement(json).jsonObject["types"]?.jsonArray
        ?.map { it.jsonObject }
        ?.sortedBy { it["slot"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE }
        ?.mapNotNull { (it["type"] as? JsonObject)?.get("name")?.jsonPrimitive?.content }
        ?.mapNotNull { TipoDePokemon.daPokeApi(it) }
        .orEmpty()
} catch (_: Exception) {
    emptyList()
}
