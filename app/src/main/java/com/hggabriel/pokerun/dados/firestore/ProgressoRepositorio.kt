package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import com.hggabriel.pokerun.dominio.regras.MarcoDaSemana
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import com.hggabriel.pokerun.dominio.regras.Recordes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val USUARIOS = "users"
private const val TEMPORADAS_DO_USUARIO = "temporadas"
private const val SEMANAIS = "weekly"
private const val AGREGADOS = "agregados"
private const val RECORDES = "records"
private const val MELHOR_PACE = "melhor_pace_seg_km"
private const val MELHOR_1K = "melhor_1k"
private const val MELHOR_5K = "melhor_5k"
private const val MAIOR_DISTANCIA = "maior_distancia"
private const val MAIOR_DURACAO = "maior_duracao_seg"

private const val PLANO_ID = "plano_id"
private const val XP_SEMANA = "xp_semana"
private const val SESSOES_FEITAS = "sessoes_feitas"
private const val KM_FEITO = "km_feito"
private const val LONGAO_FEITO = "longao_feito"
private const val ADERENCIA_PCT = "aderencia_pct"
private const val FECHADA = "fechada"
private const val XP_TOTAL = "xp_total"
private const val ESPECIE_ATUAL = "especie_atual"
private const val ULTIMA_CORRIDA_EM = "ultima_corrida_em"
private const val REGRESSAO_SEMANAS = "regressao_semanas"
private const val COLECAO_MAX = "colecao_max"
private const val HISTORICO = "historico"

/**
 * A leitura dos agregados que o replay escreve: o progresso sazonal em
 * `users/{uid}/temporadas/{id}`, o `weekly` e `agregados/records` (docs/05 §1).
 *
 * Quem **escreve** é só `MotorDeReplay` (XP-10). Aqui só se lê.
 */
class ProgressoRepositorio(private val firestore: FirebaseFirestore) {

    private fun progresso(uid: String, temporadaId: String) =
        firestore.collection(USUARIOS).document(uid).collection(TEMPORADAS_DO_USUARIO).document(temporadaId)

    /** Se o replay já gravou o progresso desta temporada. É o marcador do backfill. */
    /**
     * O progresso da temporada, ou nulo antes do primeiro replay: quem ainda não correu
     * nela está no `#001` com zero XP (D-15). É a fonte do card da espécie atual
     * (`F2-T14`) e, pelo `avatar_especie`, do ranking.
     */
    fun observarProgresso(uid: String, temporadaId: String): Flow<ProgressoNaTemporada?> =
        progresso(uid, temporadaId).observarDocumento().map { it?.paraProgresso() }

    /**
     * Os recordes vitalícios (RN-39), de `agregados/records` (`F3-T03`). Nulo antes do
     * primeiro replay. Recorde ainda não estabelecido vem nulo no próprio documento.
     */
    fun observarRecordes(uid: String): Flow<Recordes?> =
        firestore.collection(USUARIOS).document(uid).collection(AGREGADOS).document(RECORDES)
            .observarDocumento()
            .map { documento ->
                documento?.let {
                    Recordes(
                        melhorPaceSegKm = it.getLong(MELHOR_PACE)?.toDouble(),
                        melhor1kSeg = it.getLong(MELHOR_1K),
                        melhor5kSeg = it.getLong(MELHOR_5K),
                        maiorDistanciaKm = it.getDouble(MAIOR_DISTANCIA),
                        maiorDuracaoSeg = it.getLong(MAIOR_DURACAO),
                    )
                }
            }

    suspend fun existeProgresso(uid: String, temporadaId: String): Boolean =
        progresso(uid, temporadaId).get().await().exists()

    /** O progresso da temporada, lido uma vez, ou nulo se o replay nunca gravou (`F4-T10`). */
    suspend fun lerProgresso(uid: String, temporadaId: String): ProgressoNaTemporada? =
        progresso(uid, temporadaId).get().await().takeIf { it.exists() }?.paraProgresso()

    /**
     * Os `weekly` do usuário num plano, pelo número da semana. A consulta é sobre a
     * própria coleção, que a rule libera ao dono (`list: if eu(uid)`), com um filtro de
     * igualdade só: não pede índice.
     */
    /**
     * O `weekly` de um membro numa semana, ou nulo se ele não correu nela. Leitura avulsa,
     * e não listener: o ranking custa ~8 leituras e as verificações das rules (ficha de
     * `F2-T12`). A rule lê o plano do ID do documento, então o documento ausente da semana
     * 1 também é legível (docs/05 §3).
     */
    suspend fun semanal(uid: String, chave: String): AgregadoDaSemana? =
        firestore.collection(USUARIOS).document(uid).collection(SEMANAIS).document(chave).get().await()
            .takeIf { it.exists() }?.paraAgregadoDaSemana()

    suspend fun semanaisDoPlano(uid: String, planoId: String): Map<Int, AgregadoDaSemana> =
        firestore.collection(USUARIOS).document(uid).collection(SEMANAIS)
            .whereEqualTo(PLANO_ID, planoId).get().await().documents
            .associate { it.id.substringAfterLast('_').toInt() to it.paraAgregadoDaSemana() }
}

internal fun DocumentSnapshot.paraProgresso(): ProgressoNaTemporada {
    val especie = exigirInteiro(ESPECIE_ATUAL)
    return ProgressoNaTemporada(
        xpTotal = exigirLongo(XP_TOTAL),
        especieAtual = especie,
        ultimaCorridaEm = exigirInstante(ULTIMA_CORRIDA_EM),
        // Exceção declarada à nº 3: `colecao_max` nasce na Fase 4. Documento gravado antes
        // dela lê como a própria posição, e não como uma coleção que recuou.
        colecaoMax = maxOf(getLong(COLECAO_MAX)?.toInt() ?: especie, especie),
        // Exceção declarada à nº 3: documento gravado antes de `F4-T10` não tem o campo, e
        // lê como nenhuma queda descontada.
        semanasDeQueda = getLong(REGRESSAO_SEMANAS)?.toInt() ?: 0,
        historico = (get(HISTORICO) as? List<*>).orEmpty().mapNotNull { linha ->
            val mapa = linha as? Map<*, *> ?: return@mapNotNull null
            val semana = mapa["semana"] as? String ?: return@mapNotNull null
            val posicao = (mapa["especie"] as? Number)?.toInt() ?: return@mapNotNull null
            MarcoDaSemana(semana, posicao)
        },
    )
}

internal fun DocumentSnapshot.paraAgregadoDaSemana(): AgregadoDaSemana = AgregadoDaSemana(
    planoId = exigirTexto(PLANO_ID),
    xpSemana = exigirLongo(XP_SEMANA),
    sessoesFeitas = exigirInteiro(SESSOES_FEITAS),
    kmFeito = exigirDecimal(KM_FEITO),
    longaoFeito = exigirDecimal(LONGAO_FEITO),
    aderenciaPct = exigirDecimal(ADERENCIA_PCT),
    fechada = exigirBooleano(FECHADA),
)
