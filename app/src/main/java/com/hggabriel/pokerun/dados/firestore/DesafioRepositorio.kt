package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.regras.Desafio
import kotlinx.coroutines.tasks.await

private const val PLANOS = "plans"
private const val DESAFIOS = "desafios"

private const val TIPO = "tipo"
private const val META = "meta"
private const val ACUMULADO = "acumulado"
private const val CONCLUIDO = "concluido"
private const val INSIGNIA_ID = "insignia_id"

/**
 * O desafio coletivo de cada semana: `plans/{planId}/desafios/{semanaRef}` (docs/05 §1,
 * `F2-T13`). A rule deixa qualquer membro ler e escrever, porque o progresso é do grupo
 * (docs/05 §3). Uma leitura da coleção inteira: um plano tem no máximo 21 semanas.
 */
class DesafioRepositorio(private val firestore: FirebaseFirestore) {

    private fun desafios(planoId: String) = firestore.collection(PLANOS).document(planoId).collection(DESAFIOS)

    suspend fun buscarTodos(planoId: String): Map<Int, Desafio> =
        desafios(planoId).get().await().documents.associate { it.id.toInt() to it.paraDesafio() }

    suspend fun gravar(planoId: String, semanaRef: Int, desafio: Desafio) {
        desafios(planoId).document(semanaRef.toString()).set(desafio.paraDocumento()).await()
    }
}

internal fun Desafio.paraDocumento(): Map<String, Any?> = mapOf(
    TIPO to tipo,
    META to meta,
    ACUMULADO to acumulado,
    CONCLUIDO to concluido,
    INSIGNIA_ID to insigniaId,
)

internal fun DocumentSnapshot.paraDesafio(): Desafio = Desafio(
    tipo = exigirTexto(TIPO),
    meta = exigirDecimal(META),
    acumulado = exigirDecimal(ACUMULADO),
    concluido = exigirBooleano(CONCLUIDO),
    insigniaId = getString(INSIGNIA_ID),
)
