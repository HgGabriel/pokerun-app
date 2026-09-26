package com.hggabriel.pokerun.dados.healthconnect

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import java.time.Instant

data class Triagem(val daFonte: List<SessaoDoHealthConnect>, val deOutraOrigem: Int)

fun triar(sessoes: List<SessaoDoHealthConnect>, fonteCanonica: String): Triagem =
    Triagem(emptyList(), -1)

fun inicioDaLeitura(cursor: Instant?, grade: List<Semana>): Instant? = null

fun corridaImportada(
    sessao: SessaoDoHealthConnect,
    medidas: MedidasDaSessao,
    plano: Plano,
    grade: List<Semana>,
    temporadas: List<Temporada>,
): Corrida? = null
