package com.hggabriel.pokerun.ui.telas.dump

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

/** Os 30 dias que a ficha de `F0-T09` manda ler. */
private const val DIAS_DA_JANELA = 30L

/** A etiqueta do `logcat`, para `adb logcat -s PokerunDump`. */
private const val ETIQUETA = "PokerunDump"

/**
 * As permissões que o dump pede, e são as mesmas do `AndroidManifest.xml`.
 *
 * Derivadas do tipo de registro, e não escritas à mão: as constantes
 * `HealthPermission.READ_*` existem no bytecode mas são `internal`, e a lista
 * literal sairia do lugar no dia em que um tipo mudasse de chave. `StepsRecord` e
 * `StepsCadenceRecord` colapsam em `READ_STEPS`, que é por isso que o conjunto tem
 * seis entradas para sete tipos.
 */
val PERMISSOES_DO_DUMP: Set<String> = setOf(
    ExerciseSessionRecord::class,
    DistanceRecord::class,
    HeartRateRecord::class,
    StepsRecord::class,
    StepsCadenceRecord::class,
    ActiveCaloriesBurnedRecord::class,
    SpeedRecord::class,
).mapTo(mutableSetOf()) { HealthPermission.getReadPermission(it) }

/**
 * O motor do dump do Health Connect (`F0-T09`). **Descartável.**
 *
 * Ele não é o cliente de ingestão: `F2-T01` a `F2-T04` constroem aquele, com filtro
 * de fonte canônica (RN-22) e idempotência de três chaves. Este aqui é o oposto —
 * **não filtra nada e não descarta nada**, porque o entregável de `F0-T10` é
 * descobrir o que cada aparelho tem, e uma origem descartada é exatamente a célula
 * que ficaria vazia sem ninguém saber por quê.
 *
 * A leitura acontece no toque do botão, e isso não contradiz RN-25 ("só na abertura
 * do app"): a regra existe para proibir leitura em segundo plano, e aqui não há
 * serviço, alarme nem `WorkManager`.
 *
 * **Um agregado por grupo de campo, cada um com seu próprio `catch`.** Uma chamada
 * só com as seis métricas seria mais barata e falharia inteira quando faltasse uma
 * permissão, e a saída diria "nada" onde a verdade é "tudo menos calorias".
 */
class DumpViewModel(app: Application) : AndroidViewModel(app) {

    private val _estado = MutableStateFlow(DumpUiState())
    val estado: StateFlow<DumpUiState> = _estado.asStateFlow()

    init {
        conferirDisponibilidade()
    }

    /** `getSdkStatus()` mais as permissões já concedidas. Barato, roda na entrada. */
    fun conferirDisponibilidade() {
        val contexto = getApplication<Application>()
        val status = when (HealthConnectClient.getSdkStatus(contexto)) {
            HealthConnectClient.SDK_AVAILABLE -> StatusDoSdk.Disponivel
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StatusDoSdk.PrecisaAtualizar
            else -> StatusDoSdk.Indisponivel
        }
        if (status != StatusDoSdk.Disponivel) {
            _estado.value = _estado.value.copy(
                status = status,
                permissoesFaltando = PERMISSOES_DO_DUMP,
            )
            return
        }
        viewModelScope.launch {
            val concedidas = try {
                HealthConnectClient.getOrCreate(contexto)
                    .permissionController
                    .getGrantedPermissions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(status = status, falha = descrever(e))
                return@launch
            }
            _estado.value = _estado.value.copy(
                status = status,
                permissoesFaltando = PERMISSOES_DO_DUMP - concedidas,
                falha = null,
            )
        }
    }

    /** Lê os 30 dias e monta o texto. É o que a definição de pronto chama de "imprime". */
    fun lerJanela() {
        if (_estado.value.lendo) return
        _estado.value = _estado.value.copy(lendo = true, falha = null)
        viewModelScope.launch {
            try {
                val cliente = HealthConnectClient.getOrCreate(getApplication())
                val texto = montarRelatorio(cliente)
                texto.lineSequence().forEach { Log.i(ETIQUETA, it) }
                _estado.value = _estado.value.copy(lendo = false, relatorio = texto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(lendo = false, falha = descrever(e))
            }
        }
    }

    // -----------------------------------------------------------------------
    // O relatório
    // -----------------------------------------------------------------------

    private suspend fun montarRelatorio(cliente: HealthConnectClient): String {
        val fim = Instant.now()
        val inicio = fim.minus(Duration.ofDays(DIAS_DA_JANELA))
        val sessoes = lerTudo(cliente, ExerciseSessionRecord::class, inicio, fim)
            .sortedBy { it.startTime }
        val concedidas = PERMISSOES_DO_DUMP.size - _estado.value.permissoesFaltando.size

        val saida = StringBuilder()
        saida.appendLine("PokéRun · dump do Health Connect (F0-T09)")
        saida.appendLine(linha("gerado_em", momento(fim)))
        saida.appendLine(linha("aparelho", "${Build.MANUFACTURER} ${Build.MODEL}"))
        saida.appendLine(linha("android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"))
        saida.appendLine(linha("fuso", ZoneId.systemDefault().id))
        saida.appendLine(linha("sdk_status", nomeDoStatus()))
        saida.appendLine(linha("permissoes", "$concedidas de ${PERMISSOES_DO_DUMP.size}"))
        saida.appendLine(linha("janela", "${momento(inicio)} a ${momento(fim)}"))
        saida.appendLine(linha("sessoes", sessoes.size.toString()))

        if (sessoes.isEmpty()) {
            saida.appendLine()
            saida.appendLine("Nenhuma sessão de exercício nos últimos $DIAS_DA_JANELA dias.")
            saida.appendLine("Isto é um resultado, não uma falha: para F0-T10 significa que este")
            saida.appendLine("aparelho não entrega treino nenhum pelo Health Connect.")
            return saida.toString()
        }

        sessoes.forEachIndexed { indice, sessao ->
            saida.appendLine()
            saida.appendLine("--- sessão ${indice + 1} de ${sessoes.size} ---")
            saida.append(descreverSessao(cliente, sessao))
        }
        return saida.toString()
    }

    /**
     * **Cada campo sai duas vezes: o agregado e o bruto.** Não é zelo.
     *
     * O emulador mostrou que o Health Connect devolve agregado vazio para
     * `DISTANCE_TOTAL`, `COUNT_TOTAL`, `ACTIVE_CALORIES_TOTAL` e
     * `EXERCISE_DURATION_TOTAL` de uma origem cujos registros a leitura crua acha
     * sem dificuldade, enquanto `BPM_*`, que vem de amostras, responde normalmente.
     * Só com o agregado, o dump escreveria `(ausente)` em campo que o aparelho tem,
     * e `F0-T10` leria isso como "esta pessoa não entrega distância" quando a
     * verdade é "a plataforma não soma esta origem".
     *
     * **A célula vazia de `F0-T10` só vale se as duas colunas estiverem vazias.**
     */
    private suspend fun descreverSessao(
        cliente: HealthConnectClient,
        sessao: ExerciseSessionRecord,
    ): String {
        val origem = sessao.metadata.dataOrigin
        val inicio = sessao.startTime
        val fim = sessao.endTime
        val saida = StringBuilder()

        saida.appendLine(linha("dataOrigin", origem.packageName))
        saida.appendLine(linha("metadata.id", sessao.metadata.id))
        saida.appendLine(linha("clientRecordId", sessao.metadata.clientRecordId ?: AUSENTE))
        saida.appendLine(
            linha("tipo_exercicio", "${sessao.exerciseType} (${nomeDoExercicio(sessao.exerciseType)})")
        )
        saida.appendLine(linha("inicio", momento(inicio)))
        saida.appendLine(linha("fim", momento(fim)))

        // Duração de relógio: subtração dos dois campos acima, sempre disponível.
        // É o piso contra o qual se lê o agregado.
        saida.appendLine(linha("duracao_relogio_seg", Duration.between(inicio, fim).seconds.toString()))

        val duracao = agregar(
            cliente, setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), inicio, fim, origem
        )
        saida.appendLine(
            linha("duracao_agregada_seg", duracao.texto { it[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.seconds })
        )

        val distancia = agregar(cliente, setOf(DistanceRecord.DISTANCE_TOTAL), inicio, fim, origem)
        saida.appendLine(
            linha("distancia_agregada_m", distancia.texto { r ->
                r[DistanceRecord.DISTANCE_TOTAL]?.let { "%.1f".format(it.inMeters) }
            })
        )

        val distancias = tentar { lerTudo(cliente, DistanceRecord::class, inicio, fim, origem) }
        saida.appendLine(
            linha("distancia_bruta_m", distancias.texto { registros ->
                if (registros.isEmpty()) null
                else "%.1f em %s".format(registros.sumOf { it.distance.inMeters }, contar(registros.size))
            })
        )

        val fc = agregar(
            cliente,
            setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN),
            inicio, fim, origem,
        )
        saida.appendLine(
            linha("fc_media_max_min", fc.texto { r ->
                val media = r[HeartRateRecord.BPM_AVG]
                val maxima = r[HeartRateRecord.BPM_MAX]
                val minima = r[HeartRateRecord.BPM_MIN]
                if (media == null && maxima == null && minima == null) null
                else "${media ?: AUSENTE} / ${maxima ?: AUSENTE} / ${minima ?: AUSENTE}"
            })
        )

        val batimentos = tentar { lerTudo(cliente, HeartRateRecord::class, inicio, fim, origem) }
        saida.appendLine(linha("fc_amostras", batimentos.texto { amostrasDe(it) { r -> r.samples.size } }))

        // `RATE_AVG` de cadência não é agregado suportado pela plataforma: o
        // emulador respondeu "Unsupported aggregation type
        // StepsCadenceSeries_rate_avg". A média sai das amostras, que é o único
        // caminho que existe, e a contagem diz se ela é confiável.
        val cadencia = tentar { lerTudo(cliente, StepsCadenceRecord::class, inicio, fim, origem) }
        saida.appendLine(
            linha("cadencia_ppm", cadencia.texto { registros ->
                val amostras = registros.flatMap { it.samples }
                if (amostras.isEmpty()) null
                else "%.1f em %d amostras / %s".format(
                    amostras.sumOf { it.rate } / amostras.size, amostras.size, contar(registros.size),
                )
            })
        )

        val passos = agregar(cliente, setOf(StepsRecord.COUNT_TOTAL), inicio, fim, origem)
        saida.appendLine(linha("passos_agregado", passos.texto { it[StepsRecord.COUNT_TOTAL] }))

        val passosBrutos = tentar { lerTudo(cliente, StepsRecord::class, inicio, fim, origem) }
        saida.appendLine(
            linha("passos_bruto", passosBrutos.texto { registros ->
                if (registros.isEmpty()) null
                else "${registros.sumOf { it.count }} em ${contar(registros.size)}"
            })
        )

        val calorias = agregar(
            cliente, setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), inicio, fim, origem
        )
        saida.appendLine(
            linha("calorias_agregada_kcal", calorias.texto { r ->
                r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let { "%.1f".format(it.inKilocalories) }
            })
        )

        val caloriasBrutas = tentar { lerTudo(cliente, ActiveCaloriesBurnedRecord::class, inicio, fim, origem) }
        saida.appendLine(
            linha("calorias_bruta_kcal", caloriasBrutas.texto { registros ->
                if (registros.isEmpty()) null
                else "%.1f em %s".format(registros.sumOf { it.energy.inKilocalories }, contar(registros.size))
            })
        )

        // A linha que decide um visualizador inteiro da Fase 3: sem amostra de
        // velocidade não há split por km, e o bloco degrada de forma prevista
        // (docs/05 §4.1, a nota sobre não existir `SplitRecord`).
        val velocidade = tentar { lerTudo(cliente, SpeedRecord::class, inicio, fim, origem) }
        saida.appendLine(linha("amostras_speed", velocidade.texto { amostrasDe(it) { r -> r.samples.size } }))

        return saida.toString()
    }

    // -----------------------------------------------------------------------
    // Acesso ao Health Connect
    // -----------------------------------------------------------------------

    /**
     * Percorre as páginas até o fim. Uma sessão longa passa das mil amostras por
     * página, e parar na primeira produziria uma contagem de amostras menor que a
     * real, que é justamente o número de que `F0-T10` depende para decidir se
     * splits existem.
     */
    private suspend fun <T : Record> lerTudo(
        cliente: HealthConnectClient,
        tipo: KClass<T>,
        inicio: Instant,
        fim: Instant,
        origem: DataOrigin? = null,
    ): List<T> {
        val acumulado = mutableListOf<T>()
        var pagina: String? = null
        do {
            val resposta = cliente.readRecords(
                ReadRecordsRequest(
                    recordType = tipo,
                    timeRangeFilter = TimeRangeFilter.between(inicio, fim),
                    dataOriginFilter = origem?.let { setOf(it) } ?: emptySet(),
                    pageToken = pagina,
                )
            )
            acumulado += resposta.records
            pagina = resposta.pageToken
        } while (pagina != null)
        return acumulado
    }

    private suspend fun agregar(
        cliente: HealthConnectClient,
        metricas: Set<AggregateMetric<*>>,
        inicio: Instant,
        fim: Instant,
        origem: DataOrigin,
    ): Tentativa<AggregationResult> = tentar {
        cliente.aggregate(
            AggregateRequest(
                metrics = metricas,
                timeRangeFilter = TimeRangeFilter.between(inicio, fim),
                dataOriginFilter = setOf(origem),
            )
        )
    }

    private fun nomeDoStatus(): String = when (_estado.value.status) {
        StatusDoSdk.Disponivel -> "SDK_AVAILABLE"
        StatusDoSdk.PrecisaAtualizar -> "SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED"
        StatusDoSdk.Indisponivel -> "SDK_UNAVAILABLE"
        StatusDoSdk.NaoConsultado -> "(nao consultado)"
    }
}

// ---------------------------------------------------------------------------
// Formatação
// ---------------------------------------------------------------------------

private const val AUSENTE = "(ausente)"

/** Largura da coluna de rótulo. Alinhado, o dump lê como tabela no `logcat`. */
private const val COLUNA = 24

/** "1 registro" e "2 registros". O dump é lido oito vezes por uma pessoa só. */
private fun contar(quantos: Int): String = if (quantos == 1) "1 registro" else "$quantos registros"

/** "N amostras em M registros", ou nada quando não houve registro nenhum. */
private inline fun <T> amostrasDe(registros: List<T>, quantas: (T) -> Int): String? =
    if (registros.isEmpty()) null
    else "${registros.sumOf(quantas)} amostras em ${contar(registros.size)}"

private fun linha(rotulo: String, valor: String): String = rotulo.padEnd(COLUNA) + ": " + valor

private val FORMATO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

/**
 * Hora local **com o deslocamento junto**. O fuso do aparelho não serve para
 * calcular `semana_ref` (RN-28), mas serve para ler um dump, e o deslocamento
 * impresso é o que permite reconstruir o instante sem adivinhar qual era.
 */
private fun momento(instante: Instant): String =
    FORMATO.format(instante.atZone(ZoneId.systemDefault()))

/**
 * O resultado de uma leitura que pode falhar sozinha.
 *
 * `runCatching` resolveria em uma linha e **engole `CancellationException`**, que
 * numa corrotina é o mecanismo de cancelamento. O dump roda inteiro dentro de uma.
 */
private class Tentativa<T>(val valor: T?, val erro: String?)

private inline fun <T, R> Tentativa<T>.texto(bloco: (T) -> R?): String = when {
    erro != null -> "(falhou: $erro)"
    valor == null -> AUSENTE
    else -> bloco(valor)?.toString() ?: AUSENTE
}

private suspend fun <T> tentar(bloco: suspend () -> T): Tentativa<T> =
    try {
        Tentativa(bloco(), null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Tentativa(null, descrever(e))
    }

private fun descrever(e: Exception): String =
    "${e::class.simpleName}: ${e.message ?: "sem mensagem"}"

/**
 * Só os tipos que aparecem num grupo de corrida, mais o guarda-chuva.
 *
 * A tabela do Health Connect tem oitenta valores e nenhum uso aqui: o que `F0-T10`
 * precisa distinguir é corrida de caminhada e de bicicleta, que é o erro de
 * classificação que RN-31 manda descartar. O resto sai como número, e número cru é
 * resposta honesta.
 */
private fun nomeDoExercicio(tipo: Int): String = when (tipo) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "RUNNING"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "RUNNING_TREADMILL"
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "WALKING"
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "HIKING"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "BIKING"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "BIKING_STATIONARY"
    ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "OTHER_WORKOUT"
    else -> "outro"
}
