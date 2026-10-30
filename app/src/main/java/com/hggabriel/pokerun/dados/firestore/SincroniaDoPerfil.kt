package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dominio.modelo.Usuario

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
 * O treinador não volta a nulo pelo Perfil: o seletor só troca uma escolha por outra
 * (docs/estudo-progresso-perfil.md, d.2 regra 3). O parceiro volta, desde 29/10/2026:
 * [parceiroAutomatico] apaga a escolha, e o parceiro passa a ser o padrão da regra 2, o
 * último Pokémon da coleção. Ele e [parceiro] não andam juntos.
 */
data class MudancaDoPerfil(
    val nome: String? = null,
    val baselineKm: Double? = null,
    val parceiro: Int? = null,
    val treinador: String? = null,
    val parceiroAutomatico: Boolean = false,
)

/**
 * O valor que apaga o campo no `update`. O lote é puro e testável sem o Firestore; quem grava
 * troca a marca por `FieldValue.delete()`.
 */
internal object ApagarCampo

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
        if (mudanca.parceiroAutomatico) put(CAMPO_PARCEIRO, ApagarCampo)
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

internal const val CAMPO_AVATAR_ESPECIE = "avatar_especie"

/**
 * A foto do membro, que o puxar do Grupo envia a `members/{uid}` antes de reler (29/10/2026):
 * o nome, o parceiro escolhido (ou a marca que o apaga, e o grupo cai no avatar), o treinador e
 * o avatar da coleção. O Perfil e o replay já gravam esses campos, mas uma escrita perdida
 * deixava a foto velha no grupo até a próxima troca. Avatar nulo (leitura que falhou) fica fora:
 * a foto não apaga o que o replay gravou.
 */
internal fun fotoDoMembro(usuario: Usuario, avatar: Int?): Map<String, Any> = buildMap {
    put(CAMPO_NOME, usuario.nome)
    put(CAMPO_PARCEIRO, usuario.parceiro ?: ApagarCampo)
    usuario.treinador?.let { put(CAMPO_TREINADOR, it) }
    avatar?.let { put(CAMPO_AVATAR_ESPECIE, it) }
}
