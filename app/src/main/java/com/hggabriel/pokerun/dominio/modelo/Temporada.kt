package com.hggabriel.pokerun.dominio.modelo

import java.time.Instant
import java.time.ZoneId

/**
 * A definição de uma temporada (`temporadas/{temporadaId}`, docs/05 §1).
 *
 * **Definição, não progresso.** O que cada corredor acumulou vive em
 * `users/{uid}/temporadas/{id}` e é sazonal; aqui está o catálogo, que é igual para
 * todo mundo. Corridas, recordes e gráficos são vitalícios e atravessam a virada;
 * XP, coleção e tier zeram (RN-39).
 *
 * **[totalEspecies] é o antídoto de RN-43.** Nenhuma contagem de espécies ou de
 * tiers pode aparecer como constante no código: a primeira temporada tem 150 e a
 * segunda tem 235, e um literal escrito hoje quebra em janeiro num lugar que
 * ninguém vai lembrar de procurar. Toda barra de progresso, toda posição na escada
 * e todo *"faltam N"* saem daqui.
 *
 * A soma de `especies` de todos os [tiers] tem que bater com [totalEspecies], e é
 * `F4-T02` que testa isso — junto com IDs únicos e nenhum fora do intervalo válido.
 */
data class Temporada(
    /** Ex.: `kanto-2026`. */
    val id: String,
    val nome: String,
    val regiao: String,
    val inicio: Instant,
    val fim: Instant,
    /** O fuso que decide quando a temporada vira. Mesmo cuidado de `Plano.fuso` (RN-28). */
    val fuso: ZoneId,
    val ativa: Boolean,
    val totalEspecies: Int,
    val tiers: List<Tier>,
)

/**
 * Uma faixa de locomoção da temporada (docs/07).
 *
 * O tier descreve **a criatura**, não o corredor: é a velocidade natural da espécie
 * que decide onde ela entra. Por isso o rótulo nunca carrega faixa de pace — dizer
 * *"tier 3: 5:30 a 6:00 min/km"* seria mentir sobre quem está lendo (docs/04 §2).
 *
 * **[xpPorEspecie] é a curva de custo e é dado, não código.** Ela vive em Remote
 * Config indexada por temporada (`F4-T03`) exatamente para poder ser ajustada
 * durante o feature freeze, que é quando se descobre se ficou boa (DA-04).
 */
data class Tier(
    /** 1 a 8 na temporada de Kanto. A contagem vem da lista, nunca de literal (RN-43). */
    val ordem: Int,
    val nome: String,
    val descricao: String,
    /** Os IDs das espécies deste tier, na ordem da escada. */
    val especies: List<Int>,
    val xpPorEspecie: Int,
)
