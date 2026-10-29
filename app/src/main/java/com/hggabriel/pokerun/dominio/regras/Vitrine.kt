package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import java.time.Instant

/**
 * O que o grupo vê da vida de corrida de um membro, fora do plano: o total em km, a primeira
 * e a última corrida (cartão do treinador, 27/10/2026).
 *
 * A rule fecha `users/{uid}/runs` para os outros, então o replay do dono do uid copia estes
 * três números em `members/{uid}`, no mesmo lote do avatar. Corrida descartada ou substituída
 * não conta: é a regra do replay e da credencial do Perfil.
 */
data class Vitrine(
    val naVidaKm: Double,
    val primeiraCorrida: Instant?,
    val ultimaCorrida: Instant?,
)

fun vitrineDe(corridas: List<Corrida>): Vitrine {
    val validas = corridas.filter { !it.descartada && !it.substituida }
    return Vitrine(
        naVidaKm = validas.sumOf { it.km },
        primeiraCorrida = validas.minOfOrNull { it.dataHoraInicio },
        ultimaCorrida = validas.maxOfOrNull { it.dataHoraInicio },
    )
}
