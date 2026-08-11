package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val USUARIOS = "users"

private const val NOME = "nome"
private const val PLANO_ATIVO_ID = "plano_ativo_id"
private const val PLANOS = "planos"
private const val FONTE_CANONICA = "fonte_canonica"
private const val BASELINE_KM = "baseline_km"

/**
 * O perfil do corredor: `users/{uid}` (`F1-T05`, docs/05 §1).
 *
 * **Documento ausente é o estado de quem ainda não fez o onboarding**, e [observar]
 * devolve `null` para ele sem erro nenhum. É esse `null` que a navegação de `F1-T07`
 * usa para mandar o recém-autenticado à `OnboardingScreen` em vez da `HomeScreen`.
 *
 * O que **não** mora aqui: `runs`, `weekly`, `agregados` e `temporadas` são
 * subcoleções de `users/{uid}` mas são agregados próprios — corridas são
 * [CorridaRepositorio], e os outros três são `F2-T08`.
 */
class UsuarioRepositorio(private val firestore: FirebaseFirestore) {

    private fun usuario(uid: String) = firestore.collection(USUARIOS).document(uid)

    fun observar(uid: String): Flow<Usuario?> =
        usuario(uid).observarDocumento().map { it?.paraUsuario() }

    /** O passo que fecha o onboarding: nome e baseline (docs/01 §3.1). */
    suspend fun salvar(usuario: Usuario) {
        usuario(usuario.uid).set(usuario.paraDocumento()).await()
    }

    /**
     * // RN-13
     *
     * Trocar o plano ativo é decisão explícita, com confirmação, e **nunca efeito
     * colateral** de entrar noutro plano: quem já tem plano ativo e usa um código de
     * convite escolhe entre tornar o novo ativo ou entrar como dormente
     * (docs/03 §3.8). O repositório expõe a troca como uma escrita própria por isso
     * — embutida no `salvar` de outra tela, ela viraria silenciosa.
     *
     * Aceita `null`: sair do último plano deixa o usuário sem plano ativo, que é o
     * estado vazio da `HomeScreen`.
     */
    suspend fun definirPlanoAtivo(uid: String, planoId: String?) {
        usuario(uid).update(PLANO_ATIVO_ID, planoId).await()
    }

    /**
     * // RN-22
     *
     * A fonte canônica é o último passo do onboarding e só existe depois de pedir
     * permissão e ler as origens do Health Connect — a ordem é rígida (docs/01) e é
     * por isso que ela é uma escrita separada de [salvar], que roda dois passos
     * antes. Continua nula para quem não tem Health Connect (docs/05 §4.4).
     */
    suspend fun definirFonteCanonica(uid: String, pacote: String) {
        usuario(uid).update(FONTE_CANONICA, pacote).await()
    }

    /**
     * // D-04
     *
     * Acrescenta um plano à lista do usuário — ao criar (`F1-T10`) e ao entrar por
     * convite (`F1-T14`). É o único caminho pelo qual a `PlansListScreen` descobre
     * que o plano existe, porque `plans` não aceita consulta (docs/05 §2.7).
     *
     * `arrayUnion` e não leitura-modificação-escrita: a operação é idempotente do
     * lado do servidor, então entrar duas vezes no mesmo plano — dois toques no
     * botão, um retry de rede — não duplica a entrada nem exige transação.
     */
    suspend fun acrescentarPlano(uid: String, planoId: String) {
        usuario(uid).update(PLANOS, FieldValue.arrayUnion(planoId)).await()
    }
}

internal fun Usuario.paraDocumento(): Map<String, Any?> = mapOf(
    NOME to nome,
    PLANO_ATIVO_ID to planoAtivoId,
    PLANOS to planos,
    FONTE_CANONICA to fonteCanonica,
    BASELINE_KM to baselineKm,
)

internal fun DocumentSnapshot.paraUsuario(): Usuario = Usuario(
    uid = id,
    nome = exigirTexto(NOME),
    planoAtivoId = getString(PLANO_ATIVO_ID),
    // Vazia é o estado de quem acabou de fazer o onboarding e ainda não criou nem
    // entrou em plano nenhum — a `HomeScreen · SemPlano` (docs/03 §3.4).
    planos = listaDeTextos(PLANOS),
    // Nula significa "nada a filtrar porque nada é importado", nunca "aceite tudo".
    fonteCanonica = getString(FONTE_CANONICA),
    baselineKm = exigirDecimal(BASELINE_KM),
)
