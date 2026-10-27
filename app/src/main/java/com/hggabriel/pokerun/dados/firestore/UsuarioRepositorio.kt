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
private const val SEMANA_APRESENTADA = "semana_apresentada"
private const val PLANOS = "planos"
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

    /**
     * Uma leitura só, para decidir entre `HomeScreen` e `OnboardingScreen` logo
     * depois de autenticar (`F1-T06`, docs/03 §3.1).
     *
     * **É `get()` e não a primeira emissão de [observar]**, e a diferença aparece
     * exatamente no caso que importa. O listener entrega o cache primeiro, e num
     * aparelho recém-formatado o cache está vazio: a primeira emissão seria `null`
     * e mandaria para o onboarding alguém que já tem perfil há meses. O `get()`
     * pergunta ao servidor.
     *
     * Falha sem rede, e isso é aceitável aqui: a autenticação que acabou de
     * acontecer também precisou de rede.
     */
    suspend fun buscar(uid: String): Usuario? =
        usuario(uid).get().await().takeIf { it.exists() }?.paraUsuario()

    /** O passo que fecha o onboarding: nome e baseline (docs/01 §3.1). */
    suspend fun salvar(usuario: Usuario) {
        usuario(usuario.uid).set(usuario.paraDocumento()).await()
    }

    /**
     * Marca o fechamento como apresentado (`F4-T09`, docs/01 §4.6). `update` de **um
     * campo**: `set` apagaria `plano_ativo_id` e `planos` (a mesma razão de
     * [atualizarPerfil], decisão nº 73).
     */
    suspend fun marcarSemanaApresentada(uid: String, chave: String) {
        usuario(uid).update(SEMANA_APRESENTADA, chave).await()
    }

    /**
     * Grava o que o Perfil mudou, num lote com `users/{uid}` e o `members/{uid}` de cada
     * plano de [planos] ([loteDoPerfil]).
     *
     * **Não espera o servidor.** O lote entra na fila local do Firestore e a tela segue:
     * sem rede, ele sobe quando a conexão voltar, e o cache já devolve o valor novo a quem
     * lê. Antes, o `await` deixava o botão girando sem fim no aparelho sem sinal.
     *
     * **`update` e não [salvar]**, que é `set` do documento inteiro: um `set` montado a partir
     * de um [Usuario] lido antes da edição apagaria o que outra tela gravou no meio
     * (`plano_ativo_id`, `planos`). Trocar a baseline não mexe em plano que já existe: ela
     * semeia o **próximo** (RN-05).
     *
     * **Se o servidor recusar o lote inteiro**, cada escrita é refeita sozinha. O caso
     * conhecido é um plano de `users.planos` sem o documento de membro: o `update` dele
     * falha, e num lote só a falha levaria junto o `users/{uid}` e os outros planos.
     */
    fun atualizarPerfil(uid: String, planos: List<String>, mudanca: MudancaDoPerfil) {
        val escritas = loteDoPerfil(uid, planos, mudanca)
        if (escritas.isEmpty()) return
        val lote = firestore.batch()
        escritas.forEach { lote.update(documento(it.caminho), it.campos) }
        lote.commit().addOnFailureListener {
            escritas.forEach { escrita -> documento(escrita.caminho).update(escrita.campos) }
        }
    }

    private fun documento(caminho: List<String>) = firestore.document(caminho.joinToString("/"))

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
    BASELINE_KM to baselineKm,
) + listOfNotNull(
    // Fora do mapa quando nulos: o cadastro nunca escolhe parceiro nem treinador.
    parceiro?.let { CAMPO_PARCEIRO to it },
    treinador?.let { CAMPO_TREINADOR to it },
)

internal fun DocumentSnapshot.paraUsuario(): Usuario = Usuario(
    uid = id,
    nome = exigirTexto(NOME),
    planoAtivoId = getString(PLANO_ATIVO_ID),
    // Vazia é o estado de quem acabou de fazer o onboarding e ainda não criou nem
    // entrou em plano nenhum — a `HomeScreen · SemPlano` (docs/03 §3.4).
    planos = listaDeTextos(PLANOS),
    baselineKm = exigirDecimal(BASELINE_KM),
    // Ausente até a primeira apresentação (`F4-T09`): é o caso normal, e não um erro.
    semanaApresentada = getString(SEMANA_APRESENTADA),
    // Ausentes até a primeira escolha no Perfil (estudo de Progresso e Perfil, F1).
    parceiro = inteiroOuNulo(CAMPO_PARCEIRO),
    treinador = getString(CAMPO_TREINADOR),
)
