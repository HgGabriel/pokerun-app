package com.hggabriel.pokerun.dados.healthconnect

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

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
 * O mínimo de Health Connect que o onboarding precisa (`F1-T08`, docs/03 §3.2).
 *
 * **Não é o cliente de ingestão.** Aquele é `F2-T01` a `F2-T04`, com filtro de fonte
 * canônica (RN-22), derivação de splits e idempotência de três chaves; nada disso está
 * aqui. Este responde três perguntas e só três, que são exatamente as dos passos 3 a 5
 * do cadastro: o aparelho tem Health Connect, o usuário concedeu leitura, e quem andou
 * gravando treino nos últimos 30 dias.
 *
 * **Ele nasce na Fase 1 porque o onboarding não espera a Fase 2.** A ordem do cadastro
 * é rígida (`EXECUCAO.md §8`, item 9) e o passo 5 pede a lista de origens com
 * contagem; sem ler o Health Connect não há lista. `F2-T01` constrói o cliente de
 * produção em cima disto, não ao lado.
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
     * Conta **sessões de exercício**, sem filtrar por tipo. A ingestão também não
     * filtra: docs/05 §4.1 captura `tipo_exercicio` em vez de recusar, e bicicleta
     * marcada como corrida é descarte do usuário (RN-31), não do leitor. Filtrar aqui
     * esconderia do passo 5 justamente a origem que grava tudo.
     *
     * Ordenado pela contagem, do maior para o menor: a origem que mais grava é quase
     * sempre a que o usuário quer, e fica no topo em vez de sair na ordem em que a
     * plataforma devolveu.
     *
     * **Propaga a exceção.** A tela precisa separar "não deu para ler" de "não há
     * origem nenhuma", e as duas viram lista vazia se o erro morrer aqui.
     */
    suspend fun origensRecentes(agora: Instant = Instant.now()): List<OrigemDeTreino> {
        val cliente = HealthConnectClient.getOrCreate(contexto)
        val inicio = agora.minus(Duration.ofDays(DIAS_DA_JANELA))

        val porPacote = mutableMapOf<String, Int>()
        var pagina: String? = null
        do {
            val resposta = cliente.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(inicio, agora),
                    pageToken = pagina,
                ),
            )
            resposta.records.forEach { sessao ->
                val pacote = sessao.metadata.dataOrigin.packageName
                porPacote[pacote] = (porPacote[pacote] ?: 0) + 1
            }
            pagina = resposta.pageToken
        } while (pagina != null)

        return porPacote
            .map { (pacote, corridas) -> OrigemDeTreino(pacote, rotuloDoApp(pacote), corridas) }
            .sortedWith(compareByDescending<OrigemDeTreino> { it.corridas }.thenBy { it.rotulo })
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
