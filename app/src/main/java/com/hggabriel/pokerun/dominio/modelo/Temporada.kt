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
 * A soma de `especies` de todos os [tiers] tem que bater com [totalEspecies], com IDs
 * únicos e nenhum fora do intervalo válido: é `problemasDaTemporada` (`F2-T00`).
 */
/**
 * // RN-40
 *
 * O `temporada_id` que uma corrida gravada hoje carrega.
 *
 * **É snapshot, e a corrida credita XP na temporada em que aconteceu** — em 2026 essa
 * temporada é Kanto, e docs/05 §1 escreve o próprio ID (`temporadas/{temporadaId}
 * // "kanto-2026"`). Isto não é um literal inventado: é o valor que `F4-T01` vai gravar
 * no catálogo.
 *
 * **Existe porque o catálogo ainda não existe.** A temporada ativa é a que tem
 * `ativa = true` em `temporadas/{id}`, e essa coleção é criada em `F4-T01` — enquanto
 * ela não existe, não há de onde ler. A alternativa era gravar o campo vazio e deixar
 * `F4-T05` inferir a temporada de cada corrida por `data_hora_inicio` no backfill, o
 * que custaria uma varredura sobre documentos que este `const` já responde certo.
 *
 * **Não viola RN-43.** O que a regra proíbe é constante de **contagem** de espécies ou
 * de tiers, que muda de 150 para 235 na virada; um ID de temporada é a chave do
 * documento que traz aquela contagem.
 *
 * **Dono da remoção: `F4-T01`.** Quando o catálogo existir, quem grava corrida lê a
 * temporada ativa e esta constante sai — inclusive de `F2-T02`, a importação, que é o
 * segundo lugar do app a gravar `runs`.
 */
const val TEMPORADA_CORRENTE = "kanto-2026"

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
 * **[xpPorEspecie] é a curva de custo e é dado, não código.** Ela vive só na
 * definição da temporada, no Firestore (decisão nº 88), exatamente para poder ser
 * ajustada pelo console durante o feature freeze, que é quando se descobre se ficou
 * boa (DA-04).
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
