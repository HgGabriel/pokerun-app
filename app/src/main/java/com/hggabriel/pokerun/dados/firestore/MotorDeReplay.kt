package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.hggabriel.pokerun.dominio.regras.DetalheDeXp
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.ResultadoDoReplay
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant

private const val USUARIOS = "users"
private const val CORRIDAS = "runs"
private const val SEMANAIS = "weekly"
private const val TEMPORADAS_DO_USUARIO = "temporadas"
private const val PLANOS = "plans"
private const val MEMBROS = "members"

private const val XP_CREDITADO = "xp_creditado"
private const val XP_DETALHE = "xp_detalhe"
private const val PLANO_ID = "plano_id"
private const val XP_SEMANA = "xp_semana"
private const val XP_TOTAL = "xp_total"
private const val ESPECIE_ATUAL = "especie_atual"
private const val ULTIMA_CORRIDA_EM = "ultima_corrida_em"
private const val AVATAR_ESPECIE = "avatar_especie"

/** Abaixo do teto de 500 operações por lote do Firestore, com folga. */
private const val OPERACOES_POR_LOTE = 400

/**
 * // XP-10
 *
 * O replay de docs/05 §2.5 com as duas pontas no Firestore: lê as corridas, a grade de
 * cada plano em que elas caíram e o catálogo de temporadas; recalcula com [Replay]; e
 * grava o que caiu em `[desde, ate]`.
 *
 * **Três remessas, nesta ordem, e uma não segura a outra:**
 * 1. os agregados do usuário: `weekly` e o progresso da temporada;
 * 2. `xp_creditado` e `xp_detalhe` das corridas. Enquanto a rule de `runs` publicada não
 *    listar `xp_detalhe` (`F2-T07`), o servidor recusa esta remessa inteira, e ela não
 *    pode levar os agregados junto;
 * 3. o `avatar_especie` de `members/{uid}` em cada plano do usuário. É `update`, e não
 *    `set` com merge: um merge num membro inexistente criaria um documento sem os campos
 *    que a lista de membros exige.
 *
 * A primeira falha é relançada no fim, depois de as três terem sido tentadas.
 */
class MotorDeReplay(
    private val firestore: FirebaseFirestore,
    private val corridas: CorridaRepositorio,
    private val planos: PlanoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val usuarios: UsuarioRepositorio,
) {

    suspend fun replay(uid: String, desde: Instant, ate: Instant): ResultadoDoReplay {
        val todas = corridas.buscarTodas(uid)
        val grades = todas.map { it.planoId }.distinct().associateWith { planos.buscarSemanas(it) }
        val catalogo = temporadas.todas()

        val resultado = Replay.recalcular(todas, grades, catalogo)
        val escritas = Replay.escritas(todas, resultado, desde, ate)
        val usuario = usuario(uid)

        var falha: Exception? = null
        suspend fun tentar(bloco: suspend () -> Unit) {
            try {
                bloco()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                if (falha == null) falha = erro
            }
        }

        tentar {
            val operacoes = buildList<(WriteBatch) -> Unit> {
                escritas.semanas.forEach { (chave, xp) ->
                    val planoId = chave.substringBeforeLast('_')
                    add { it.set(usuario.collection(SEMANAIS).document(chave), mapOf(PLANO_ID to planoId, XP_SEMANA to xp), SetOptions.merge()) }
                }
                resultado.porTemporada.forEach { (id, progresso) ->
                    val campos = mapOf(
                        XP_TOTAL to progresso.xpTotal,
                        ESPECIE_ATUAL to progresso.especieAtual,
                        ULTIMA_CORRIDA_EM to progresso.ultimaCorridaEm.paraTimestamp(),
                    )
                    add { it.set(usuario.collection(TEMPORADAS_DO_USUARIO).document(id), campos, SetOptions.merge()) }
                }
            }
            gravarEmLotes(operacoes)
        }

        tentar {
            gravarEmLotes(
                escritas.corridas.map { (id, detalhe) ->
                    { lote: WriteBatch ->
                        lote.update(usuario.collection(CORRIDAS).document(id), XP_CREDITADO, detalhe.total, XP_DETALHE, detalhe.paraDocumento())
                    }
                },
            )
        }

        tentar {
            val atual = temporadaDe(ate, catalogo)?.let { resultado.porTemporada[it.id]?.especieAtual ?: 1 }
            if (atual != null) {
                val planosDoUsuario = usuarios.buscar(uid)?.planos.orEmpty()
                gravarEmLotes(
                    planosDoUsuario.map { planoId ->
                        { lote: WriteBatch ->
                            lote.update(firestore.collection(PLANOS).document(planoId).collection(MEMBROS).document(uid), AVATAR_ESPECIE, atual)
                        }
                    },
                )
            }
        }

        falha?.let { throw it }
        return resultado
    }

    private fun usuario(uid: String) = firestore.collection(USUARIOS).document(uid)

    private suspend fun gravarEmLotes(operacoes: List<(WriteBatch) -> Unit>) {
        operacoes.chunked(OPERACOES_POR_LOTE).forEach { parte ->
            val lote = firestore.batch()
            parte.forEach { it(lote) }
            lote.commit().await()
        }
    }
}

/** As parcelas da conta, com os nomes de docs/05 §1. */
internal fun DetalheDeXp.paraDocumento(): Map<String, Any> = mapOf(
    "base" to base,
    "longao" to longao,
    "pace" to pace,
    "multiplicador" to multiplicador,
    "recorde" to recorde,
)
