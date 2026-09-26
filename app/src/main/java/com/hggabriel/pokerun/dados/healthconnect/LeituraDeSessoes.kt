package com.hggabriel.pokerun.dados.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import java.time.Instant
import kotlin.math.roundToInt

/*
 * O que a importação faz com cada sessão depois de lida (`F2-T02`, docs/05 §4.3).
 *
 * `SaudeRepositorio.sessoesEntre` lê e não decide (`F2-T01`). As decisões moram aqui,
 * em funções puras, porque são regra e têm teste: o tipo de exercício e a fonte
 * canônica (RN-22, decisão nº 93), de onde a leitura começa, e os snapshots com que a
 * sessão vira `Corrida` (RN-03, RN-14, RN-40).
 */

/**
 * Os tipos que a importação aceita, com o token que cada um grava em `tipo_exercicio`.
 *
 * **Só corrida** (decisão nº 93). Caminhada e bicicleta não chegam à revisão; o
 * descarte de RN-31 fica para o relógio que marcou errado. O token de rua é o mesmo
 * `RUNNING` que o registro manual grava, para uma corrida não mudar de tipo conforme
 * o caminho por onde entrou.
 */
private val TOKENS_DE_CORRIDA: Map<Int, String> = mapOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "RUNNING",
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "RUNNING_TREADMILL",
)

/**
 * O que sobra de uma leitura depois do filtro.
 *
 * [deOutraOrigem] é só uma contagem, e de propósito: essas corridas alimentam o aviso da
 * `ImportReviewScreen` (`F2-T05`) e **nunca são importadas** — não existe `[Importar
 * assim mesmo]` (decisão nº 93). Guardar as sessões convidaria alguém a importá-las.
 */
data class Triagem(val daFonte: List<SessaoDoHealthConnect>, val deOutraOrigem: Int)

/**
 * // RN-22
 *
 * Separa as corridas da [fonteCanonica] das de outra origem. O que não é corrida sai
 * das duas: uma caminhada do Strava não viria nem se o Strava fosse a fonte, e contá-la
 * no aviso diria que falta uma corrida que nunca existiu.
 */
fun triar(sessoes: List<SessaoDoHealthConnect>, fonteCanonica: String): Triagem {
    val corridas = sessoes.filter { it.tipo in TOKENS_DE_CORRIDA }
    val (daFonte, deOutra) = corridas.partition { it.origem == fonteCanonica }
    return Triagem(daFonte = daFonte, deOutraOrigem = deOutra.size)
}

/**
 * De onde a leitura começa: o cursor `hc_sincronizado_ate`, ou, com ele nulo, o início
 * do plano ativo — a segunda-feira da primeira semana da [grade].
 *
 * **Nulo quando não há de onde ler**: sem cursor e sem plano. Não é o caso de ler desde
 * sempre — o cursor nulo quer dizer "nunca sincronizou", e a primeira leitura existe para
 * trazer o que o plano usa, não o histórico inteiro do aparelho.
 */
fun inicioDaLeitura(cursor: Instant?, grade: List<Semana>): Instant? =
    cursor ?: grade.firstOrNull()?.dataInicio

/**
 * // RN-14
 *
 * A [sessao] como a `Corrida` que a revisão vai oferecer, ou **nula** quando ela não pode
 * virar uma.
 *
 * Os snapshots saem aqui, no momento da importação: o plano ativo, a semana dele no fuso
 * do plano (nula fora do intervalo, RN-03) e a temporada da data (RN-40).
 * `CorridaRepositorio.registrar` recalcula `semana_ref` do mesmo jeito ao gravar; o daqui
 * é o que a revisão mostra antes disso.
 *
 * **Dois motivos para nulo**, e nenhum é erro de leitura:
 * - **sem distância**, porque `km` é campo sempre presente (docs/05 §4.1) e zero viraria
 *   pace infinito no replay;
 * - **sem temporada publicada que cubra a data** — o console ainda não publicou a do ano
 *   seguinte. Gravar sem temporada não credita XP em lugar nenhum, e gravar na última
 *   credita no lugar errado (decisão nº 103).
 *
 * `xp_creditado` nasce zerado e `sessao_reivindicada` nula: quem escreve os dois é o
 * replay de `F2-T07` (XP-10, RN-34).
 */
fun corridaImportada(
    sessao: SessaoDoHealthConnect,
    medidas: MedidasDaSessao,
    plano: Plano,
    grade: List<Semana>,
    temporadas: List<Temporada>,
): Corrida? {
    val token = TOKENS_DE_CORRIDA[sessao.tipo] ?: return null
    val metros = medidas.metros ?: return null
    val temporada = temporadaDe(sessao.inicio, temporadas) ?: return null

    return Corrida(
        id = "",
        dataHoraInicio = sessao.inicio,
        km = metros / 1_000.0,
        duracaoSeg = sessao.duracaoSeg,
        tipoExercicio = token,
        origem = OrigemDaCorrida.HC,
        planoId = plano.id,
        semanaRef = CalendarioDoPlano.semanaRef(sessao.inicio, plano, grade),
        temporadaId = temporada.id,
        fcMedia = medidas.fcMedia?.toInt(),
        fcMax = medidas.fcMax?.toInt(),
        fcMin = medidas.fcMin?.toInt(),
        caloriasAtivas = medidas.caloriasAtivas?.roundToInt(),
        passos = medidas.passos?.toInt(),
        cadenciaMedia = medidas.cadenciaMedia?.roundToInt(),
        hcRecordId = sessao.id,
        hcClientRecordId = sessao.idDoCliente,
    )
}
