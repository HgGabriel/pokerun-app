package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import kotlinx.coroutines.tasks.await

private const val USUARIOS = "users"
private const val TEMPORADAS_DO_USUARIO = "temporadas"
private const val SEMANAIS = "weekly"

private const val PLANO_ID = "plano_id"
private const val XP_SEMANA = "xp_semana"
private const val SESSOES_FEITAS = "sessoes_feitas"
private const val KM_FEITO = "km_feito"
private const val LONGAO_FEITO = "longao_feito"
private const val ADERENCIA_PCT = "aderencia_pct"
private const val FECHADA = "fechada"

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
    suspend fun existeProgresso(uid: String, temporadaId: String): Boolean =
        progresso(uid, temporadaId).get().await().exists()

    /**
     * Os `weekly` do usuário num plano, pelo número da semana. A consulta é sobre a
     * própria coleção, que a rule libera ao dono (`list: if eu(uid)`), com um filtro de
     * igualdade só: não pede índice.
     */
    suspend fun semanaisDoPlano(uid: String, planoId: String): Map<Int, AgregadoDaSemana> =
        firestore.collection(USUARIOS).document(uid).collection(SEMANAIS)
            .whereEqualTo(PLANO_ID, planoId).get().await().documents
            .associate { it.id.substringAfterLast('_').toInt() to it.paraAgregadoDaSemana() }
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
