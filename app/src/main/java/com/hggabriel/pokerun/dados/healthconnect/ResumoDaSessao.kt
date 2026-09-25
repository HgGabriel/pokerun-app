package com.hggabriel.pokerun.dados.healthconnect

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Uma sessão de exercício como o Health Connect a entregou, **sem filtro nenhum**
 * (`F2-T01`, docs/05 §4.3).
 *
 * É o cabeçalho: o que decide se a sessão entra, sem ler nada de dentro dela. Quem
 * filtra — tipo de exercício, fonte canônica, cursor — é `F2-T02`, sobre esta lista;
 * as medidas só são lidas, por [SaudeRepositorio.medidasDa], das que passaram. Ler
 * as seis permissões de uma caminhada do Strava para depois jogá-la fora seria o
 * custo inteiro por nada.
 *
 * [id] é o `metadata.id`, que vai para `hc_record_id`, e [idDoCliente] o
 * `clientRecordId`, que vai para `hc_client_record_id` e tem precedência quando existe
 * — o `metadata.id` muda se a origem apagar e reinserir (`EXECUCAO.md §8.1`, item 3).
 * [origem] é o `dataOrigin`, comparado com `fonte_canonica` (RN-22). [tipo] é a
 * constante crua de `ExerciseSessionRecord`, sem tradução: é `F2-T02` quem sabe quais
 * contam como corrida.
 *
 * [duracaoSeg] já desconta as pausas — ver [duracaoEmMovimento].
 */
data class SessaoDoHealthConnect(
    val id: String,
    val idDoCliente: String?,
    val origem: String,
    val tipo: Int,
    val inicio: Instant,
    val fim: Instant,
    val duracaoSeg: Long,
)

/**
 * O que existe dentro de uma sessão, campo a campo (docs/05 §4.1).
 *
 * **Todo campo é anulável, e nulo quer dizer "não veio"** — a permissão daquele tipo
 * não foi concedida, ou a origem não gravou. O usuário pode marcar só parte das caixas
 * na folha do Health Connect, e uma corrida sem FC continua sendo corrida. Nenhum zero
 * é inventado: `0 passos` é uma medida, ausente é outra coisa.
 *
 * Sem splits: são derivados de amostras de velocidade, e isso é `F2-T03`.
 */
data class MedidasDaSessao(
    val metros: Double?,
    val fcMedia: Long?,
    val fcMax: Long?,
    val fcMin: Long?,
    val caloriasAtivas: Double?,
    val passos: Long?,
    val cadenciaMedia: Double?,
)

/** Um intervalo de tempo, que é tudo que o cálculo da pausa precisa de um segmento. */
data class Intervalo(val inicio: Instant, val fim: Instant)

/**
 * A duração da sessão **em movimento**: o relógio menos as pausas.
 *
 * `duracao_seg` alimenta o pace, e o pace alimenta `bônus_pace` (`docs/04 §3`). Com o
 * relógio cru, quem parou cinco minutos num sinal teria o pace de quem correu devagar.
 * É também o que o agregado `EXERCISE_DURATION_TOTAL` da plataforma faz — que não é
 * usado porque `F0-T09` o viu voltar vazio para uma origem cujos registros a leitura
 * crua achava.
 *
 * A pausa é recortada à janela da sessão, e duas pausas sobrepostas contam o trecho
 * comum uma vez só. O resultado nunca é negativo.
 */
fun duracaoEmMovimento(inicio: Instant, fim: Instant, pausas: List<Intervalo>): Long {
    val relogio = Duration.between(inicio, fim).seconds
    if (relogio <= 0) return 0

    var parado = 0L
    var ate = inicio
    pausas
        .map { Intervalo(maxOf(it.inicio, inicio), minOf(it.fim, fim)) }
        .filter { it.inicio < it.fim }
        .sortedBy { it.inicio }
        .forEach { pausa ->
            val desde = maxOf(pausa.inicio, ate)
            if (pausa.fim > desde) {
                parado += Duration.between(desde, pausa.fim).seconds
                ate = pausa.fim
            }
        }
    return (relogio - parado).coerceAtLeast(0)
}

/** A faixa cardíaca de uma sessão, que é o que docs/05 §4.1 grava das amostras. */
data class FaixaCardiaca(val media: Long, val max: Long, val min: Long)

/**
 * Média, máxima e mínima das amostras **dentro da janela da sessão**, ou nulo se não
 * houver nenhuma.
 *
 * A janela importa porque um `HeartRateRecord` é uma série, e a série que atravessa o
 * início da corrida traz os batimentos do aquecimento junto. As amostras não são
 * gravadas (docs/05 §4.2); só a faixa sai daqui.
 */
fun faixaCardiaca(amostras: List<Pair<Instant, Long>>, inicio: Instant, fim: Instant): FaixaCardiaca? {
    val dentro = amostras.filter { (quando, _) -> quando >= inicio && quando <= fim }.map { it.second }
    if (dentro.isEmpty()) return null
    return FaixaCardiaca(
        media = dentro.average().roundToLong(),
        max = dentro.max(),
        min = dentro.min(),
    )
}

/**
 * A média das amostras dentro da janela, ou nulo se não houver nenhuma.
 *
 * É a cadência: `RATE_AVG` de `StepsCadenceRecord` não é agregado suportado pela
 * plataforma (`F0-T09`), e a média das amostras é o único caminho que existe.
 */
fun mediaNaJanela(amostras: List<Pair<Instant, Double>>, inicio: Instant, fim: Instant): Double? {
    val dentro = amostras.filter { (quando, _) -> quando >= inicio && quando <= fim }.map { it.second }
    return if (dentro.isEmpty()) null else dentro.average()
}
