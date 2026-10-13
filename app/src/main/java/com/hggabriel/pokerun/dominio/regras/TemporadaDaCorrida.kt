package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Instant

/**
 * // RN-40
 *
 * A temporada em que [instante] aconteceu, ou **nula** quando nenhuma temporada
 * publicada o cobre.
 *
 * **É a da data, não a ativa.** Na virada, a corrida de 31/12 às 23h é registrada em
 * janeiro, com a temporada seguinte já ativa, e credita em Kanto mesmo assim. Quem
 * grava corrida — o registro manual — pergunta aqui, e nunca mais a
 * `TEMPORADA_CORRENTE` que existiu até 26/09 (decisão nº 62).
 *
 * **O fuso da temporada já está dentro de `inicio` e `fim`.** Os dois são instantes, e
 * foram publicados como a meia-noite de 01/01 em São Paulo (decisão nº 100), então
 * comparar instantes **é** comparar no fuso da temporada: a corrida de 31/12 às 23h, que
 * em UTC já é 01/01, cai antes do `fim`. O erro que RN-40 existe para impedir é tirar o
 * ano do instante em UTC, ou no fuso do aparelho.
 *
 * O `fim` é exclusivo, como o `data_fim` das semanas: a meia-noite da virada já é da
 * temporada seguinte.
 */
fun temporadaDe(instante: Instant, temporadas: List<Temporada>): Temporada? =
    temporadas.firstOrNull { instante >= it.inicio && instante < it.fim }
