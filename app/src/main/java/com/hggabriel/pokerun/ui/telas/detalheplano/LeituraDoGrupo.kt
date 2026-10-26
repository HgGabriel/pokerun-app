package com.hggabriel.pokerun.ui.telas.detalheplano

import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana

/*
 * O que a `PlanDetailScreen` lê do `weekly` do grupo (R3), sem Firestore.
 *
 * A rule libera `get` a qualquer membro do plano e nega `list` na coleção de outro
 * usuário, então a leitura é documento a documento: uma por membro e por semana da janela
 * dele. Uma vez por abertura e a cada `Tentar de novo`, como o pulso do grupo, e nunca
 * listener (docs/03 §3.16).
 */

/**
 * // RN-19
 *
 * As semanas a ler de cada **outro** membro: de `entrou_na_semana` até [ateSemana]. As
 * suas não entram, porque saem das suas corridas. Janela vazia (antes de o plano
 * começar, ou de o membro entrar) não pede leitura nenhuma.
 */
internal fun semanasALer(membros: List<Membro>, uid: String, ateSemana: Int): Map<String, IntRange> =
    membros.filter { it.uid != uid }.associate { it.uid to (it.entrouNaSemana..ateSemana) }

/**
 * O `weekly` lido, pelo número da semana, no formato que [detalheDoPlano] consome. O
 * documento ausente sai do mapa, e a função pura o conta como zero.
 */
internal fun feitosDosSemanais(semanais: Map<Int, AgregadoDaSemana?>): Map<Int, FeitoNaSemana> =
    semanais.mapNotNull { (numero, agregado) ->
        agregado?.let { numero to FeitoNaSemana(it.sessoesFeitas, it.longaoFeito) }
    }.toMap()

/**
 * O que decide reler o grupo: quem está no plano, desde quando, e até que semana a janela
 * vai. Uma corrida nova sua não muda a chave, e por isso não relê o grupo.
 */
internal data class ChaveDoGrupo(val janelas: Map<String, IntRange>, val tentativa: Int)
