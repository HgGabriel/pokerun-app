package com.hggabriel.pokerun.ui.telas.perfil

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.LocalDate
import java.time.ZoneId

/**
 * Os campos da credencial do corredor (estudo de Progresso e Perfil, Revisão 1 e 3).
 *
 * @property desde o dia da primeira corrida válida. Nulo sem corrida.
 * @property naVidaKm a soma das corridas válidas, de todas as temporadas (RN-39).
 * @property colecao a `colecao_max` da temporada corrente, de [totalEspecies].
 * @property prova o dia da prova do plano ativo, no fuso do plano. Nulo sem plano ativo.
 * @property temporada o nome da temporada corrente (`Kanto 2026`), o que o catálogo guarda
 *   e nenhuma tela lia.
 */
data class DadosDaCredencial(
    val desde: LocalDate?,
    val naVidaKm: Double,
    val colecao: Int?,
    val totalEspecies: Int?,
    val prova: LocalDate?,
    val temporada: String?,
)

/**
 * Corrida descartada ou substituída não conta: é a mesma regra do replay e do histórico.
 * Sem corrida, `desde` fica nulo e a credencial diz que ainda não há corrida, em vez de
 * inventar uma data de cadastro que o app não guarda.
 */
fun dadosDaCredencial(
    corridas: List<Corrida>,
    temporada: Temporada?,
    colecaoCorrente: Int?,
    planoAtivo: Plano?,
    fuso: ZoneId,
): DadosDaCredencial {
    val validas = corridas.filter { !it.descartada && !it.substituida }
    return DadosDaCredencial(
        desde = validas.minOfOrNull { it.dataHoraInicio }?.atZone(fuso)?.toLocalDate(),
        naVidaKm = validas.sumOf { it.km },
        colecao = temporada?.let { colecaoCorrente ?: 1 },
        totalEspecies = temporada?.totalEspecies,
        prova = planoAtivo?.let { it.dataProva.atZone(it.fuso).toLocalDate() },
        temporada = temporada?.nome,
    )
}
