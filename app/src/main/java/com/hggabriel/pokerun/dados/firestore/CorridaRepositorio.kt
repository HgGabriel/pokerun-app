package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val USUARIOS = "users"
private const val CORRIDAS = "runs"

private const val DATA_HORA_INICIO = "data_hora_inicio"
private const val KM = "km"
private const val DURACAO_SEG = "duracao_seg"
private const val FC_MEDIA = "fc_media"
private const val FC_MAX = "fc_max"
private const val FC_MIN = "fc_min"
private const val CALORIAS_ATIVAS = "calorias_ativas"
private const val PASSOS = "passos"
private const val CADENCIA_MEDIA = "cadencia_media"
private const val TIPO_EXERCICIO = "tipo_exercicio"
private const val SPLITS_KM = "splits_km"
private const val ESFORCO_PERCEBIDO = "esforco_percebido"
private const val ORIGEM = "origem"
private const val HC_RECORD_ID = "hc_record_id"
private const val HC_CLIENT_RECORD_ID = "hc_client_record_id"
private const val PLANO_ID = "plano_id"
private const val SEMANA_REF = "semana_ref"
private const val TEMPORADA_ID = "temporada_id"
private const val SESSAO_REIVINDICADA = "sessao_reivindicada"
private const val XP_CREDITADO = "xp_creditado"
private const val SUBSTITUI_RUN_ID = "substitui_run_id"
private const val SUBSTITUIDA = "substituida"
private const val DESCARTADA = "descartada"

/**
 * O histórico de treinos: `users/{uid}/runs` (`F1-T05`, docs/05 §1 e §2.1).
 *
 * **As corridas não pertencem ao plano.** Elas moram sob o usuário justamente para
 * que encerrar ou apagar um plano não apague treino nenhum (docs/05 §2.1); o
 * `plano_id` que carregam é snapshot, e é por isso que [observarDoPlano] filtra por
 * campo em vez de descer por uma subcoleção do plano.
 *
 * **Nada aqui apaga nem sobrescreve corrida** (RN-24). Correção é [corrigir], que
 * grava registro novo. A rule de `update` em `runs` aceita quatro chaves e nenhuma
 * delas é medição — `substituida`, `descartada` e `sessao_reivindicada` são lápides,
 * e `xp_creditado` é resultado de replay. É ela que transforma a regra em erro de
 * escrita em vez de convenção.
 *
 * O descarte (`descartada = true`, RN-31) e a troca de sessão reivindicada (RN-34)
 * **não estão aqui**: são `F2-T10`, junto da tela que os dispara.
 */
class CorridaRepositorio(private val firestore: FirebaseFirestore) {

    private fun corridas(uid: String) =
        firestore.collection(USUARIOS).document(uid).collection(CORRIDAS)

    /**
     * As corridas de um plano, em ordem cronológica.
     *
     * **A ordenação é em Kotlin de propósito.** Um `whereEqualTo` num campo somado a
     * um `orderBy` noutro exige índice composto declarado, e o projeto não tem
     * arquivo de índices para manter. O corpus é pequeno por construção — um plano
     * de 21 semanas com 4 sessões não passa de ~84 documentos —, e é isso que a
     * aderência (`F1-T04`) e o replay (`F2-T07`) consomem inteiro de qualquer jeito.
     */
    fun observarDoPlano(uid: String, planoId: String): Flow<List<Corrida>> =
        corridas(uid).whereEqualTo(PLANO_ID, planoId).observarColecao()
            .map { consulta ->
                consulta.documents.map { it.paraCorrida() }.sortedBy { it.dataHoraInicio }
            }

    /**
     * O histórico vitalício, do mais recente para o mais antigo — a ordem da
     * `RunHistoryScreen` (`F2-T11`).
     *
     * Atravessa planos e temporadas: corridas, recordes e gráficos são vitalícios;
     * XP, coleção e tier é que são sazonais (RN-39).
     */
    fun observarHistorico(uid: String): Flow<List<Corrida>> =
        corridas(uid).orderBy(DATA_HORA_INICIO, Query.Direction.DESCENDING).observarColecao()
            .map { consulta -> consulta.documents.map { it.paraCorrida() } }

    /**
     * // RN-14
     *
     * Grava a corrida e devolve o ID do documento criado.
     *
     * **O `semana_ref` é calculado aqui, e o que vier em [corrida] é ignorado.**
     * A aderência não recalcula a semana: ela lê o campo (`F1-T04`). Um `semana_ref`
     * gravado errado não derruba teste de regra nenhum, porque os testes recebem o
     * campo pronto — então quem grava é quem tem de chamar [CalendarioDoPlano], e a
     * única forma de garantir isso é a assinatura exigir o plano e a grade em vez de
     * confiar no chamador. O cálculo sai no fuso do plano (RN-28), que chega dentro
     * de [plano].
     *
     * Nulo é resultado previsto: corrida fora do intervalo do plano entra no
     * histórico e não conta para semana nenhuma (RN-03).
     */
    suspend fun registrar(uid: String, corrida: Corrida, plano: Plano, grade: List<Semana>): String {
        val documento = corridas(uid).document()
        documento.set(corrida.comSnapshotDoPlano(plano, grade).paraDocumento()).await()
        return documento.id
    }

    /**
     * // RN-24
     *
     * Corrige uma corrida sem sobrescrevê-la: grava um registro novo apontando para
     * o antigo por `substitui_run_id` e marca o antigo com `substituida = true`. Os
     * dois vão no mesmo lote — um registro corrigido que não fosse marcado apareceria
     * duas vezes em toda soma, e a marca sem o registro novo apagaria a corrida.
     *
     * A remessa mexe **só** em `substituida` no documento antigo. Qualquer outra
     * chave afetada é negada pela rule, e é assim que RN-24 deixa de depender de
     * disciplina de quem escreve o código.
     */
    suspend fun corrigir(
        uid: String,
        original: Corrida,
        correcao: Corrida,
        plano: Plano,
        grade: List<Semana>,
    ): String {
        val novo = corridas(uid).document()
        val remessa = firestore.batch()

        remessa.set(
            novo,
            correcao.comSnapshotDoPlano(plano, grade)
                .copy(substituiRunId = original.id, substituida = false)
                .paraDocumento(),
        )
        remessa.update(corridas(uid).document(original.id), SUBSTITUIDA, true)

        remessa.commit().await()
        return novo.id
    }
}

/**
 * // RN-28
 *
 * O par de snapshots que a corrida carrega para sempre: o plano em que ela caiu e a
 * semana daquele plano, derivada **no fuso do plano**. Referência congelada, não
 * viva (RN-14) — trocar de plano ativo depois não move corrida nenhuma de lugar.
 */
private fun Corrida.comSnapshotDoPlano(plano: Plano, grade: List<Semana>): Corrida = copy(
    planoId = plano.id,
    semanaRef = CalendarioDoPlano.semanaRef(dataHoraInicio, plano, grade),
)

internal fun Corrida.paraDocumento(): Map<String, Any?> = mapOf(
    DATA_HORA_INICIO to dataHoraInicio.paraTimestamp(),
    KM to km,
    DURACAO_SEG to duracaoSeg,
    TIPO_EXERCICIO to tipoExercicio,
    ORIGEM to origem.paraDocumento(),
    PLANO_ID to planoId,
    SEMANA_REF to semanaRef,
    TEMPORADA_ID to temporadaId,
    FC_MEDIA to fcMedia,
    FC_MAX to fcMax,
    FC_MIN to fcMin,
    CALORIAS_ATIVAS to caloriasAtivas,
    PASSOS to passos,
    CADENCIA_MEDIA to cadenciaMedia,
    SPLITS_KM to splitsKm,
    ESFORCO_PERCEBIDO to esforcoPercebido,
    HC_RECORD_ID to hcRecordId,
    HC_CLIENT_RECORD_ID to hcClientRecordId,
    SESSAO_REIVINDICADA to sessaoReivindicada?.token,
    // Resultado do último replay, para auditoria. Quem escreve de verdade é o motor
    // de `F2-T07`; aqui ele nasce zerado e nunca é usado como fonte (XP-10).
    XP_CREDITADO to xpCreditado,
    SUBSTITUI_RUN_ID to substituiRunId,
    SUBSTITUIDA to substituida,
    DESCARTADA to descartada,
)

internal fun DocumentSnapshot.paraCorrida(): Corrida = Corrida(
    id = id,
    dataHoraInicio = exigirInstante(DATA_HORA_INICIO),
    km = exigirDecimal(KM),
    duracaoSeg = exigirLongo(DURACAO_SEG),
    tipoExercicio = exigirTexto(TIPO_EXERCICIO),
    origem = origemDoDocumento(exigirTexto(ORIGEM)),
    planoId = exigirTexto(PLANO_ID),
    // Nulo é corrida fora do intervalo do plano (RN-03), não campo faltando.
    semanaRef = inteiroOuNulo(SEMANA_REF),
    temporadaId = exigirTexto(TEMPORADA_ID),
    fcMedia = inteiroOuNulo(FC_MEDIA),
    fcMax = inteiroOuNulo(FC_MAX),
    fcMin = inteiroOuNulo(FC_MIN),
    caloriasAtivas = inteiroOuNulo(CALORIAS_ATIVAS),
    passos = inteiroOuNulo(PASSOS),
    cadenciaMedia = inteiroOuNulo(CADENCIA_MEDIA),
    splitsKm = listaDeSegundos(SPLITS_KM),
    esforcoPercebido = inteiroOuNulo(ESFORCO_PERCEBIDO),
    hcRecordId = getString(HC_RECORD_ID),
    hcClientRecordId = getString(HC_CLIENT_RECORD_ID),
    sessaoReivindicada = getString(SESSAO_REIVINDICADA)?.let { sessaoDoDocumento(it) },
    xpCreditado = exigirLongo(XP_CREDITADO),
    substituiRunId = getString(SUBSTITUI_RUN_ID),
    substituida = exigirBooleano(SUBSTITUIDA),
    descartada = exigirBooleano(DESCARTADA),
)
