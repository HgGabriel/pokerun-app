package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import java.time.LocalDate
import java.time.ZoneId

/**
 * A fita de cada recorde (decisão 18): a conquista individual do Pokémon nos jogos. A arte
 * vem do mesmo repositório dos sprites (`sprites/ribbons/generation-viii/`), pelo nome do
 * arquivo.
 */
enum class FitaDeRecorde(val tipo: TipoDeRecorde, val arquivo: String) {
    MAIOR_DISTANCIA(TipoDeRecorde.MAIOR_DISTANCIA, "footprint-ribbon"),
    MAIOR_DURACAO(TipoDeRecorde.MAIOR_DURACAO, "effort-ribbon"),
    MELHOR_PACE(TipoDeRecorde.MELHOR_PACE, "record-ribbon"),
    MELHOR_1K(TipoDeRecorde.MELHOR_1K, "royal-ribbon"),
    MELHOR_5K(TipoDeRecorde.MELHOR_5K, "champion-ribbon"),
    ;

    val url: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/ribbons/generation-viii/$arquivo.png"
}

/**
 * Uma fita na tela Fitas. Vaga é a que ainda não tem recorde: a silhueta da própria fita.
 *
 * @property valor o número do recorde: km na distância, segundos na duração, no pace, no 1 km
 *   e nos 5 km.
 * @property dia o dia da corrida que bateu o recorde, e [especie] o Pokémon que a pessoa era
 *   naquela semana: os dois saem do recálculo no aparelho (decisão 10).
 */
data class FitaDoRecorde(
    val fita: FitaDeRecorde,
    val valor: Double?,
    val dia: LocalDate? = null,
    val especie: Int? = null,
    /** O nome da [especie], da PokéAPI ou da cópia guardada. Nulo até ela responder. */
    val nomeDaEspecie: String? = null,
) {
    val vaga: Boolean get() = valor == null
}

/** As cinco fitas, na ordem da tela, com o valor de `agregados/records`. */
fun fitasDosRecordes(
    corridas: List<Corrida>,
    recordes: Recordes?,
    temporada: Temporada?,
    fusos: Map<String, ZoneId>,
): List<FitaDoRecorde> = FitaDeRecorde.entries.map { fita ->
    val valor = when (fita) {
        FitaDeRecorde.MAIOR_DISTANCIA -> recordes?.maiorDistanciaKm
        FitaDeRecorde.MAIOR_DURACAO -> recordes?.maiorDuracaoSeg?.toDouble()
        FitaDeRecorde.MELHOR_PACE -> recordes?.melhorPaceSegKm
        FitaDeRecorde.MELHOR_1K -> recordes?.melhor1kSeg?.toDouble()
        FitaDeRecorde.MELHOR_5K -> recordes?.melhor5kSeg?.toDouble()
    }
    FitaDoRecorde(fita, valor)
}
