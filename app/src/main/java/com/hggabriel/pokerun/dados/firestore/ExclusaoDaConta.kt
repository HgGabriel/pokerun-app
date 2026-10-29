package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.util.Date

/*
 * A exclusão da conta (29/10/2026), sem servidor e sem tarefa agendada.
 *
 * O pedido grava `excluir_em` (o pedido mais 30 dias) em todo documento da pessoa. Uma
 * política de TTL do Firestore, uma por grupo de coleção, apaga cada documento depois dessa
 * data: é o próprio banco que apaga, e não um cron. Até lá, entrar de novo mostra a conta em
 * exclusão, com duas saídas: recuperar (tira o campo de tudo) ou excluir permanentemente
 * ([ExclusaoDaContaRepositorio.excluirDeVez], que apaga tudo na hora).
 *
 * As políticas de TTL são configuração do projeto, feitas uma vez no console, no campo
 * `excluir_em` dos grupos `users`, `runs`, `weekly`, `agregados`, `temporadas` e `members`.
 * O TTL não desce às subcoleções sozinho, por isso o campo vai em cada documento.
 */

/** O campo que as políticas de TTL leem. Tem de ser `Timestamp`. */
internal const val CAMPO_EXCLUIR_EM = "excluir_em"

private val RETENCAO: Duration = Duration.ofDays(30)
private const val SEGUNDOS_POR_DIA = 86_400L
private const val PALAVRA_DE_CONFIRMACAO = "EXCLUIR"

/** O lote do Firestore aceita até 500 escritas. */
private const val ESCRITAS_POR_LOTE = 450

/** Quando a conta pedida em [pedidoEm] deixa de existir. */
fun prazoDaExclusao(pedidoEm: Instant): Instant = pedidoEm.plus(RETENCAO)

/** Os dias que faltam, arredondados para cima: faltando uma hora, ainda é um dia. */
fun diasAteExcluir(excluirEm: Instant, agora: Instant): Int {
    val segundos = Duration.between(agora, excluirEm).seconds
    if (segundos <= 0) return 0
    return ((segundos + SEGUNDOS_POR_DIA - 1) / SEGUNDOS_POR_DIA).toInt()
}

/** Passou do prazo: o TTL já pode ter apagado os dados, e o login também sai. */
fun prazoVencido(excluirEm: Instant, agora: Instant): Boolean = !agora.isBefore(excluirEm)

/** A segunda etapa: a pessoa digita `EXCLUIR`, sem importar a caixa. */
fun confirmouAExclusao(texto: String): Boolean = texto.trim().equals(PALAVRA_DE_CONFIRMACAO, ignoreCase = true)

/**
 * Marca e desmarca a conta. **`users/{uid}` é o último a ser marcado e o último a ser
 * desmarcado**: ele é o que o login lê, então "marcado" no usuário quer dizer que o resto
 * já foi. Se o meio falhar, o pedido pode ser refeito sem deixar dado sem prazo.
 */
class ExclusaoDaContaRepositorio(private val firestore: FirebaseFirestore) {

    /** Marca tudo com o prazo e devolve o prazo. Quem chama encerra a sessão depois. */
    suspend fun pedir(uid: String, planos: List<String>, agora: Instant): Instant {
        val prazo = prazoDaExclusao(agora)
        gravar(uid, planos, Timestamp(Date.from(prazo)))
        return prazo
    }

    /** Tira o prazo de tudo: a conta volta como estava. */
    suspend fun reativar(uid: String, planos: List<String>) {
        gravar(uid, planos, FieldValue.delete())
    }

    /**
     * Apaga tudo agora, sem esperar o TTL: as subcoleções, o membro de cada plano e, por último,
     * `users/{uid}`. A rule só deixa apagar corrida que já tem `excluir_em`, e toda corrida da
     * conta em exclusão tem. O login do Firebase sai depois, por `AutenticacaoRepositorio`.
     */
    suspend fun excluirDeVez(uid: String, planos: List<String>) {
        val usuario = firestore.collection(USUARIOS).document(uid)
        val filhos = SUBCOLECOES.flatMap { nome -> usuario.collection(nome).get().await().documents.map { it.reference } }
        filhos.chunked(ESCRITAS_POR_LOTE).forEach { parte ->
            val lote = firestore.batch()
            parte.forEach { lote.delete(it) }
            lote.commit().await()
        }
        planos.distinct().forEach { planoId ->
            try {
                membro(planoId, uid).delete().await()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // Sem o documento de membro, não há o que apagar nesse plano.
            }
        }
        usuario.delete().await()
    }

    private suspend fun gravar(uid: String, planos: List<String>, valor: Any) {
        val usuario = firestore.collection(USUARIOS).document(uid)
        val filhos = SUBCOLECOES.flatMap { nome -> usuario.collection(nome).get().await().documents.map { it.reference } }
        filhos.chunked(ESCRITAS_POR_LOTE).forEach { parte ->
            val lote = firestore.batch()
            parte.forEach { lote.update(it, CAMPO_EXCLUIR_EM, valor) }
            lote.commit().await()
        }
        // O membro de cada plano vai sozinho: um plano sem o documento de membro faria o lote
        // inteiro falhar (o mesmo caso de `UsuarioRepositorio.atualizarPerfil`).
        planos.distinct().forEach { planoId -> atualizarSePuder(membro(planoId, uid), valor) }
        usuario.update(CAMPO_EXCLUIR_EM, valor).await()
    }

    private fun membro(planoId: String, uid: String): DocumentReference =
        firestore.collection(PLANOS).document(planoId).collection(MEMBROS).document(uid)

    private suspend fun atualizarSePuder(documento: DocumentReference, valor: Any) {
        try {
            documento.update(CAMPO_EXCLUIR_EM, valor).await()
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            // Sem o documento de membro, não há o que marcar nesse plano.
        }
    }

    private companion object {
        const val USUARIOS = "users"
        const val PLANOS = "plans"
        const val MEMBROS = "members"
        val SUBCOLECOES = listOf("runs", "weekly", "agregados", "temporadas")
    }
}
