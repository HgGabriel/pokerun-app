package com.hggabriel.pokerun.dados.firestore

private const val USUARIOS = "users"
private const val PLANOS = "plans"
private const val MEMBROS = "members"

internal const val CAMPO_NOME = "nome"
internal const val CAMPO_BASELINE_KM = "baseline_km"
internal const val CAMPO_PARCEIRO = "parceiro"
internal const val CAMPO_TREINADOR = "treinador"

/**
 * O que o Perfil mudou. Campo nulo é campo que não mudou e fica fora do lote.
 *
 * O parceiro e o treinador não voltam a nulo pelo Perfil: o seletor só troca uma escolha
 * por outra (docs/estudo-progresso-perfil.md, d.2 regra 3).
 */
data class MudancaDoPerfil(
    val nome: String? = null,
    val baselineKm: Double? = null,
    val parceiro: Int? = null,
    val treinador: String? = null,
)

/** Uma escrita do lote: o caminho do documento, em segmentos, e os campos do `update`. */
internal data class EscritaDoPerfil(val caminho: List<String>, val campos: Map<String, Any>)

/**
 * O lote que o Perfil grava (estudo, §b e §e).
 *
 * **O nome, o parceiro e o treinador são copiados em `members/{uid}` de todo plano**, no
 * mesmo lote de `users/{uid}`. Toda tela de grupo lê a cópia do membro, e a rule fecha
 * `users/` para os outros. Antes desta função, o nome novo ficava só em `users/{uid}`, e o
 * grupo mostrava o nome da criação para sempre.
 *
 * A baseline fica só em `users/{uid}`: ela semeia o próximo plano e ninguém mais a lê.
 *
 * É `update`, e não `set` com merge: um merge num plano sem o documento de membro criaria
 * um membro sem os campos que a lista de membros exige (o mesmo motivo de `MotorDeReplay`).
 */
internal fun loteDoPerfil(uid: String, planos: List<String>, mudanca: MudancaDoPerfil): List<EscritaDoPerfil> {
    val doUsuario = buildMap<String, Any> {
        mudanca.nome?.let { put(CAMPO_NOME, it) }
        mudanca.baselineKm?.let { put(CAMPO_BASELINE_KM, it) }
        mudanca.parceiro?.let { put(CAMPO_PARCEIRO, it) }
        mudanca.treinador?.let { put(CAMPO_TREINADOR, it) }
    }
    if (doUsuario.isEmpty()) return emptyList()

    val doMembro = doUsuario - CAMPO_BASELINE_KM
    val membros = if (doMembro.isEmpty()) {
        emptyList()
    } else {
        planos.distinct().map { planoId -> EscritaDoPerfil(listOf(PLANOS, planoId, MEMBROS, uid), doMembro) }
    }
    return listOf(EscritaDoPerfil(listOf(USUARIOS, uid), doUsuario)) + membros
}
