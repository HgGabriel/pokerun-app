package com.hggabriel.pokerun.dados.healthconnect

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/** A janela que o passo 4 do onboarding olha para achar as origens (docs/03 §3.2). */
private const val DIAS_DA_JANELA = 30L

/**
 * O estado do Health Connect neste aparelho (docs/05 §4.4).
 *
 * **Nenhum dos três é erro.** Indisponível é o modo manual, caminho previsto e não
 * falha, e é por isso que [Indisponivel] e [PrecisaAtualizar] levam ao mesmo lugar no
 * onboarding: o cliente não conecta nos dois casos, e mandar alguém à Play Store no
 * meio do cadastro trava o passo 3 para resolver o que docs/05 §4.4 já resolve.
 */
enum class StatusDoHealthConnect { Disponivel, PrecisaAtualizar, Indisponivel }

/**
 * Um app que gravou treino no Health Connect nos últimos 30 dias.
 *
 * [pacote] é o `dataOrigin` do Health Connect e é o que vai para `fonte_canonica`
 * (RN-22). [rotulo] é o nome que o usuário reconhece, e existe porque docs/02 §7 manda
 * mostrar `Samsung Health`, nunca `com.sec.android.app.shealth`.
 */
data class OrigemDeTreino(val pacote: String, val rotulo: String, val corridas: Int)

/**
 * O único cliente do Health Connect do app (docs/05 §4).
 *
 * **Nasceu na Fase 1 com o mínimo do onboarding** (`F1-T08`, docs/03 §3.2): se o
 * aparelho tem Health Connect, se o usuário concedeu leitura, e quem andou gravando
 * treino nos últimos 30 dias. A ordem do cadastro é rígida (`EXECUCAO.md §8`, item 9)
 * e o passo 5 pede a lista de origens; sem ler o Health Connect não há lista.
 *
 * **`F2-T01` pôs aqui o contrato de leitura da ingestão** — [sessoesEntre] e
 * [medidasDa] —, no mesmo repositório e não num segundo cliente: seriam duas fontes de
 * verdade sobre a mesma permissão. O contrato **lê e não decide**. Tipo de exercício,
 * fonte canônica (RN-22) e cursor são `F2-T02`; splits, `F2-T03`; idempotência,
 * `F2-T04`.
 *
 * **Também não é o `DumpViewModel` de `F0-T09`.** Aquele é descartável e some com
 * `F0-T10`, e produção não pode depender de código marcado para exclusão. A lista de
 * permissões coincide porque a fonte das duas é o `AndroidManifest.xml`, não uma cópia
 * da outra.
 *
 * [contexto] é o da aplicação, vindo do `AppContainer`. Nunca o de uma Activity: o
 * container vive enquanto o processo viver.
 */
class SaudeRepositorio(private val contexto: Context) {

    /**
     * As permissões que o passo 3 pede, e são as seis do `AndroidManifest.xml`.
     *
     * Derivadas do tipo de registro, e não escritas à mão: as constantes
     * `HealthPermission.READ_*` são `internal` no artefato, e uma lista literal sairia
     * do lugar no dia em que um tipo mudasse de chave. `StepsRecord` e
     * `StepsCadenceRecord` colapsam em `READ_STEPS`, que é por isso que sete tipos dão
     * seis permissões.
     */
    val permissoesDeLeitura: Set<String> = setOf(
        ExerciseSessionRecord::class,
        DistanceRecord::class,
        HeartRateRecord::class,
        StepsRecord::class,
        StepsCadenceRecord::class,
        ActiveCaloriesBurnedRecord::class,
        SpeedRecord::class,
    ).mapTo(mutableSetOf(), HealthPermission::getReadPermission)

    /**
     * Se existe no aparelho alguém que atenda o caminho de atualização do Health Connect
     * (`F1-T21`, docs/03 §3.11).
     *
     * **A pergunta não é "tem Play Store instalada"**, é "algum app resolve este
     * `Intent`": o mesmo `market://` é atendido por outras lojas, e aparelho sem loja
     * nenhuma existe — é justamente onde o botão não pode aparecer. Síncrono e sem rede,
     * como [status].
     */
    fun temLojaParaAtualizar(): Boolean =
        intencaoDeAtualizar().resolveActivity(contexto.packageManager) != null

    /** Síncrono e sem rede: é uma consulta ao `PackageManager` (docs/05 §4.4). */
    fun status(): StatusDoHealthConnect = when (HealthConnectClient.getSdkStatus(contexto)) {
        HealthConnectClient.SDK_AVAILABLE -> StatusDoHealthConnect.Disponivel
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            StatusDoHealthConnect.PrecisaAtualizar
        else -> StatusDoHealthConnect.Indisponivel
    }

    /**
     * Se dá para listar as origens, que é a única pergunta que o passo 4 faz.
     *
     * **É `READ_EXERCISE` sozinha, e não o conjunto inteiro.** O usuário pode conceder
     * parte das caixas, e com a sessão liberada a lista de origens sai completa: as
     * outras cinco permissões são campos de dentro de cada treino, escopo da ingestão
     * de `F2-T01`. Exigir as seis aqui mandaria de volta ao passo 3 alguém que já
     * concedeu o que este passo precisa.
     */
    suspend fun podeLerTreinos(): Boolean {
        if (status() != StatusDoHealthConnect.Disponivel) return false
        val concedidas = HealthConnectClient.getOrCreate(contexto)
            .permissionController
            .getGrantedPermissions()
        return HealthPermission.getReadPermission(ExerciseSessionRecord::class) in concedidas
    }

    /**
     * Os apps que gravaram treino na janela, com quantos cada um gravou (passo 4).
     *
     * Conta **sessões de exercício**, sem filtrar por tipo. A ingestão filtra — só
     * corrida entra desde a decisão nº 93 (`F2-T02`) —, mas esta lista responde outra
     * pergunta: quem grava treino neste aparelho. Filtrar aqui esconderia do passo 5 a
     * origem de quem não correu com ela nos últimos 30 dias.
     *
     * Ordenado pela contagem, do maior para o menor: a origem que mais grava é quase
     * sempre a que o usuário quer, e fica no topo em vez de sair na ordem em que a
     * plataforma devolveu.
     *
     * **Propaga a exceção.** A tela precisa separar "não deu para ler" de "não há
     * origem nenhuma", e as duas viram lista vazia se o erro morrer aqui.
     */
    suspend fun origensRecentes(agora: Instant = Instant.now()): List<OrigemDeTreino> {
        val inicio = agora.minus(Duration.ofDays(DIAS_DA_JANELA))
        return lerTudo(ExerciseSessionRecord::class, inicio, agora)
            .groupingBy { it.metadata.dataOrigin.packageName }
            .eachCount()
            .map { (pacote, corridas) -> OrigemDeTreino(pacote, rotuloDoApp(pacote), corridas) }
            .sortedWith(compareByDescending<OrigemDeTreino> { it.corridas }.thenBy { it.rotulo })
    }

    /**
     * Toda sessão de exercício que começa em `[desde, ate)`, de qualquer origem e de
     * qualquer tipo, na ordem do início (`F2-T01`).
     *
     * **Sem filtro nenhum, e de propósito:** `F2-T02` precisa das corridas de outra
     * origem para contá-las no aviso da `ImportReviewScreen` (decisão nº 93), e um leitor
     * que já descartasse não teria o que contar.
     *
     * **É o início da sessão que decide se ela está na janela**, e não a sobreposição que
     * o filtro do Health Connect usa. A janela de `F2-T02` vai do cursor até agora, e a
     * corrida que atravessa o cursor sairia nas duas leituras vizinhas; pelo início, cada
     * sessão pertence a exatamente uma.
     *
     * Health Connect indisponível devolve a lista vazia sem tocar no cliente: é o modo
     * manual (docs/05 §4.4), caminho previsto e não falha. **Falha de leitura propaga**,
     * como em [origensRecentes] — "não deu para ler" e "não há corrida nova" não podem
     * virar a mesma lista vazia.
     */
    suspend fun sessoesEntre(desde: Instant, ate: Instant): List<SessaoDoHealthConnect> {
        if (status() != StatusDoHealthConnect.Disponivel || !desde.isBefore(ate)) return emptyList()
        return lerTudo(ExerciseSessionRecord::class, desde, ate)
            .filter { it.startTime >= desde && it.startTime < ate }
            .sortedBy { it.startTime }
            .map { sessao ->
                SessaoDoHealthConnect(
                    id = sessao.metadata.id,
                    idDoCliente = sessao.metadata.clientRecordId,
                    origem = sessao.metadata.dataOrigin.packageName,
                    tipo = sessao.exerciseType,
                    inicio = sessao.startTime,
                    fim = sessao.endTime,
                    duracaoSeg = duracaoEmMovimento(
                        sessao.startTime,
                        sessao.endTime,
                        sessao.segments
                            .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
                            .map { Intervalo(it.startTime, it.endTime) },
                    ),
                )
            }
    }

    /**
     * As medidas de dentro de uma sessão (docs/05 §4.1), lidas **só da origem dela** e
     * **só na janela dela**.
     *
     * **Leitura crua, e nunca o agregado da plataforma.** `F0-T09` viu o agregado de
     * distância, passos, calorias e duração voltar vazio para uma origem cujos registros
     * a leitura crua achava. Registros de intervalo (distância, passos, calorias) são
     * somados como vieram; séries de amostras (FC, cadência) são recortadas à janela,
     * porque a série que atravessa o início traz o aquecimento junto.
     *
     * **Cada campo depende da sua permissão**, conferida antes de ler: o que não foi
     * concedido sai nulo, sem exceção e sem derrubar os outros campos. Falha de leitura
     * de um tipo concedido propaga.
     */
    suspend fun medidasDa(sessao: SessaoDoHealthConnect): MedidasDaSessao {
        val concedidas = HealthConnectClient.getOrCreate(contexto)
            .permissionController
            .getGrantedPermissions()
        val origem = DataOrigin(sessao.origem)
        val inicio = sessao.inicio
        val fim = sessao.fim

        suspend fun <T : Record> seConcedida(tipo: KClass<T>): List<T>? =
            if (HealthPermission.getReadPermission(tipo) in concedidas) {
                lerTudo(tipo, inicio, fim, origem)
            } else {
                null
            }

        val distancias = seConcedida(DistanceRecord::class)
        val batimentos = seConcedida(HeartRateRecord::class)
            ?.flatMap { serie -> serie.samples.map { it.time to it.beatsPerMinute } }
        val calorias = seConcedida(ActiveCaloriesBurnedRecord::class)
        val passos = seConcedida(StepsRecord::class)
        val cadencia = seConcedida(StepsCadenceRecord::class)
            ?.flatMap { serie -> serie.samples.map { it.time to it.rate } }

        val faixa = batimentos?.let { faixaCardiaca(it, inicio, fim) }
        return MedidasDaSessao(
            metros = distancias?.takeIf { it.isNotEmpty() }?.sumOf { it.distance.inMeters },
            fcMedia = faixa?.media,
            fcMax = faixa?.max,
            fcMin = faixa?.min,
            caloriasAtivas = calorias?.takeIf { it.isNotEmpty() }
                ?.sumOf { it.energy.inKilocalories },
            passos = passos?.takeIf { it.isNotEmpty() }?.sumOf { it.count },
            cadenciaMedia = cadencia?.let { mediaNaJanela(it, inicio, fim) },
        )
    }

    /**
     * Percorre as páginas até o fim. Parar na primeira perderia sessão numa janela longa
     * — a primeira leitura, com cursor nulo, vai desde o início do plano — e amostra
     * numa corrida longa.
     */
    private suspend fun <T : Record> lerTudo(
        tipo: KClass<T>,
        inicio: Instant,
        fim: Instant,
        origem: DataOrigin? = null,
    ): List<T> {
        val cliente = HealthConnectClient.getOrCreate(contexto)
        val acumulado = mutableListOf<T>()
        var pagina: String? = null
        do {
            val resposta = cliente.readRecords(
                ReadRecordsRequest(
                    recordType = tipo,
                    timeRangeFilter = TimeRangeFilter.between(inicio, fim),
                    dataOriginFilter = origem?.let { setOf(it) } ?: emptySet(),
                    pageToken = pagina,
                ),
            )
            acumulado += resposta.records
            pagina = resposta.pageToken
        } while (pagina != null)
        return acumulado
    }

    /**
     * O nome que o usuário reconhece, ou o package name quando não der.
     *
     * O bloco `<queries>` do manifesto abre a visibilidade dos apps que integram o
     * Health Connect; fora dela, o filtro de pacotes do Android 11 devolve
     * `NameNotFoundException` mesmo para app instalado. O package name é uma saída feia
     * e honesta: melhor `com.sec.android.app.shealth` do que uma linha em branco na
     * lista que o usuário precisa escolher.
     */
    private fun rotuloDoApp(pacote: String): String = try {
        contexto.packageManager.getApplicationLabel(infoDoApp(pacote)).toString()
    } catch (naoAchou: PackageManager.NameNotFoundException) {
        pacote
    }

    private fun infoDoApp(pacote: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contexto.packageManager
                .getApplicationInfo(pacote, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            contexto.packageManager.getApplicationInfo(pacote, 0)
        }
}

/** O pacote do Health Connect, que o manifesto já declara em `<queries>`. */
private const val PACOTE_DO_HEALTH_CONNECT = "com.google.android.apps.healthdata"

/**
 * A intenção de abrir a ficha do Health Connect numa loja (`F1-T21`).
 *
 * `market://` e não a URL da web: a URL abre o navegador mesmo havendo loja, e o que a
 * pessoa precisa é do botão de atualizar, não da página. Quem resolve este `Intent` é
 * decidido pelo aparelho — ver `SaudeRepositorio.temLojaParaAtualizar`.
 */
internal fun intencaoDeAtualizar(): Intent = Intent(
    Intent.ACTION_VIEW,
    "market://details?id=$PACOTE_DO_HEALTH_CONNECT".toUri(),
)
