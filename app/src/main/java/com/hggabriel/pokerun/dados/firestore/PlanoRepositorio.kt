package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val PLANOS = "plans"
private const val SEMANAS = "weeks"
private const val MEMBROS = "members"

private const val NOME = "nome"
private const val DISTANCIA_ALVO_KM = "distancia_alvo_km"
private const val DATA_PROVA = "data_prova"
private const val FUSO = "fuso"
private const val OWNER_UID = "owner_uid"
private const val CODIGO_CONVITE = "codigo_convite"
private const val ENCERRADO = "encerrado"
private const val PARAMS_GERACAO = "params_geracao"
private const val BASELINE_KM = "baseline_km"
private const val SESSOES_POR_SEMANA = "sessoes_por_semana"

private const val DATA_INICIO = "data_inicio"
private const val DATA_FIM = "data_fim"
private const val SESSOES_ALVO = "sessoes_alvo"
private const val KM_ALVO = "km_alvo"
private const val LONGAO_KM = "longao_km"
private const val TIPO = "tipo"
private const val PARCIAL = "parcial"

private const val ENTROU_EM = "entrou_em"
private const val ENTROU_NA_SEMANA = "entrou_na_semana"
private const val POSICAO_POKEDEX = "posicao_pokedex"
private const val ATIVO = "ativo"

/**
 * O agregado do plano: `plans/{planId}` e as subcoleções `weeks` e `members`
 * (`F1-T05`, docs/05 §1 e §2).
 *
 * **O documento de `invites/` não está aqui**, e é decisão de escopo, não
 * esquecimento: ele é `F1-T14`. A consequência prática está em [criar], que grava o
 * plano com o código já sorteado mas não reserva o código — a criação transacional
 * de `invites/{codigo}`, que é o que torna o código único (RN-29), envolve esta
 * chamada quando aquela tarefa chegar.
 *
 * **Nada aqui consulta a coleção `plans`.** `allow list` é `false` (RN-17): todo
 * acesso é por ID. A lista de planos de um usuário vem de `users/{uid}.planos`
 * (docs/05 §2.7), e [observarVarios] é o que a `PlansListScreen` (`F1-T12`) usa
 * para transformar aqueles IDs em planos.
 */
class PlanoRepositorio(private val firestore: FirebaseFirestore) {

    private fun plano(planoId: String) = firestore.collection(PLANOS).document(planoId)

    /**
     * Um ID de plano ainda não gravado.
     *
     * Existe porque o documento de convite carrega `plano_id` e precisa do valor
     * antes de o plano existir (`F1-T14`). IDs automáticos do Firestore são
     * alfanuméricos de 20 caracteres, o que satisfaz a restrição das rules de que
     * **`planId` não contenha `_`** — o ID de `users/{uid}/weekly` é `{planId}_{n}`
     * e a regra do leaderboard separa os dois por `_`.
     */
    fun novoId(): String = firestore.collection(PLANOS).document().id

    fun observar(planoId: String): Flow<Plano?> =
        plano(planoId).observarDocumento().map { it?.paraPlano() }

    /**
     * Os planos de uma lista de IDs — o que a `PlansListScreen` monta a partir de
     * `users/{uid}.planos` (docs/05 §2.7).
     *
     * **São N leituras diretas, e não uma consulta**, porque `plans` não aceita
     * `list` (RN-17). No caso real são 2 a 4 documentos, cada um com seu listener,
     * o que também é o que faz a lista reagir a um plano encerrado noutro aparelho.
     *
     * ID que não resolve **some da lista em vez de derrubá-la**: um plano apagado
     * pela console deixaria a tela inteira sem conteúdo, e a lista dos outros
     * continua correta.
     */
    fun observarVarios(planoIds: List<String>): Flow<List<Plano>> =
        if (planoIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(planoIds.map { observar(it) }) { planos -> planos.filterNotNull() }
        }

    /**
     * A grade, sempre ordenada por número de semana.
     *
     * **A ordenação é em Kotlin, e não no `orderBy`**, porque o número da semana é o
     * ID do documento (`weeks/{n}`) e não um campo. Ordenar por ID no Firestore é
     * ordenação lexicográfica de texto, que põe a semana 10 antes da 2 e entrega uma
     * grade fora de ordem sem erro nenhum. A grade tem 21 documentos: ordenar no
     * cliente não custa nada.
     */
    fun observarSemanas(planoId: String): Flow<List<Semana>> =
        plano(planoId).collection(SEMANAS).observarColecao()
            .map { consulta -> consulta.documents.map { it.paraSemana() }.sortedBy { it.numero } }

    fun observarMembros(planoId: String): Flow<List<Membro>> =
        plano(planoId).collection(MEMBROS).observarColecao()
            .map { consulta -> consulta.documents.map { it.paraMembro() } }

    /**
     * Grava o plano, a grade inteira e o documento do dono.
     *
     * **São duas idas ao servidor, e não um `WriteBatch` só.** A rule de
     * `weeks/{n}` exige `dono(planId)`, que é um `get()` no documento do plano — e
     * `get()` enxerga o estado já commitado, nunca uma escrita da mesma remessa. Um
     * lote único com o plano e as 21 semanas juntos é negado inteiro, e a mensagem
     * que volta não diz isso. O plano vai primeiro, e só então a grade.
     *
     * A janela entre as duas é real: uma queda de rede no meio deixa um plano sem
     * semanas. Quem trata é a tela de criação (`F1-T10`), que já bloqueia sem
     * conexão porque a unicidade do convite não resolve offline (docs/05 §2.6).
     */
    suspend fun criar(plano: Plano, grade: List<Semana>, dono: Membro) {
        val documento = plano(plano.id)
        documento.set(plano.paraDocumento()).await()

        val remessa = firestore.batch()
        grade.forEach { semana ->
            val alvo = documento.collection(SEMANAS).document(semana.numero.toString())
            remessa.set(alvo, semana.paraDocumento())
        }
        remessa.set(documento.collection(MEMBROS).document(dono.uid), dono.paraDocumento())
        remessa.commit().await()
    }

    /**
     * // RN-06
     *
     * A edição do dono muda o longão e **deriva** o volume pela fórmula de
     * docs/01 §3.2 — um campo controla, o outro sai dele (docs/03 §3.6). Por isso a
     * escrita é dos dois campos, e não um `set` do documento inteiro: reescrever a
     * semana a partir de um modelo em memória arrastaria junto as fronteiras de
     * data, e é delas que RN-05 depende para saber que a semana já fechou.
     *
     * A trava de RN-05 é da rule, que compara `request.time` com `data_fim`. A tela
     * decide o cadeado com `CalendarioDoPlano.congelada`, e as duas fazem a mesma
     * conta sobre o mesmo campo.
     */
    suspend fun atualizarLongao(planoId: String, numero: Int, longaoKm: Double?, kmAlvo: Double) {
        plano(planoId).collection(SEMANAS).document(numero.toString())
            .update(mapOf(LONGAO_KM to longaoKm, KM_ALVO to kmAlvo))
            .await()
    }

    /** Entrar num plano é criar o próprio documento em `members/{uid}`. */
    suspend fun entrar(planoId: String, membro: Membro) {
        plano(planoId).collection(MEMBROS).document(membro.uid).set(membro.paraDocumento()).await()
    }

    /**
     * // RN-27
     *
     * Encerrar não apaga nada: congela km, aderência, ranking e posições, e **não
     * reabre**. É por isso que a rule de `update` em `plans` exige
     * `encerrado == false` — a própria escrita que encerra é a última que o
     * documento aceita.
     */
    suspend fun encerrar(planoId: String) {
        plano(planoId).update(ENCERRADO, true).await()
    }
}

internal fun Plano.paraDocumento(): Map<String, Any?> = mapOf(
    NOME to nome,
    DISTANCIA_ALVO_KM to distanciaAlvoKm,
    DATA_PROVA to dataProva.paraTimestamp(),
    FUSO to fuso.paraDocumento(),
    OWNER_UID to ownerUid,
    CODIGO_CONVITE to codigoConvite,
    ENCERRADO to encerrado,
    PARAMS_GERACAO to mapOf(
        DATA_PROVA to parametros.dataProva.paraTimestamp(),
        DISTANCIA_ALVO_KM to parametros.distanciaAlvoKm,
        BASELINE_KM to parametros.baselineKm,
        SESSOES_POR_SEMANA to parametros.sessoesPorSemana,
    ),
)

internal fun DocumentSnapshot.paraPlano(): Plano = Plano(
    id = id,
    nome = exigirTexto(NOME),
    distanciaAlvoKm = exigirDecimal(DISTANCIA_ALVO_KM),
    dataProva = exigirInstante(DATA_PROVA),
    fuso = fusoDoDocumento(exigirTexto(FUSO)),
    ownerUid = exigirTexto(OWNER_UID),
    codigoConvite = exigirTexto(CODIGO_CONVITE),
    encerrado = exigirBooleano(ENCERRADO),
    parametros = parametrosDeGeracao(),
)

/**
 * `params_geracao` é um mapa aninhado (D-02) e o SDK o devolve como `Map<*, *>` sem
 * tipo. Os quatro valores são lidos um a um, com o mesmo critério de falhar alto do
 * resto do pacote: a grade editada deixa de ser dedutível dos campos do plano, então
 * este bloco é a única memória da entrada do formulário.
 */
private fun DocumentSnapshot.parametrosDeGeracao(): ParametrosDeGeracao {
    val bruto = get(PARAMS_GERACAO) as? Map<*, *>
        ?: error("campo '$PARAMS_GERACAO' ausente ou fora de forma em ${reference.path} (D-02)")

    fun valor(campo: String): Any = bruto[campo]
        ?: error("campo '$PARAMS_GERACAO.$campo' ausente em ${reference.path} (D-02)")

    return ParametrosDeGeracao(
        dataProva = (valor(DATA_PROVA) as Timestamp).paraInstant(),
        distanciaAlvoKm = (valor(DISTANCIA_ALVO_KM) as Number).toDouble(),
        baselineKm = (valor(BASELINE_KM) as Number).toDouble(),
        sessoesPorSemana = (valor(SESSOES_POR_SEMANA) as Number).toInt(),
    )
}

internal fun Semana.paraDocumento(): Map<String, Any?> = mapOf(
    DATA_INICIO to dataInicio.paraTimestamp(),
    // `data_fim` é exclusivo: a meia-noite do dia seguinte ao último dia da semana.
    // A semântica é a de `GeradorDePlano`, e é dela que `CalendarioDoPlano` depende.
    DATA_FIM to dataFim.paraTimestamp(),
    SESSOES_ALVO to sessoesAlvo,
    KM_ALVO to kmAlvo,
    LONGAO_KM to longaoKm,
    TIPO to tipo.paraDocumento(),
    PARCIAL to parcial,
)

internal fun DocumentSnapshot.paraSemana(): Semana = Semana(
    numero = id.toIntOrNull()
        ?: error("o ID de ${reference.path} não é o número da semana (docs/05 §1: weeks/{n})"),
    dataInicio = exigirInstante(DATA_INICIO),
    dataFim = exigirInstante(DATA_FIM),
    sessoesAlvo = exigirInteiro(SESSOES_ALVO),
    kmAlvo = exigirDecimal(KM_ALVO),
    // Nulo em duas situações previstas: a 2ª semana de taper e a da prova.
    longaoKm = decimalOuNulo(LONGAO_KM),
    tipo = tipoDeSemanaDoDocumento(exigirTexto(TIPO)),
    parcial = exigirBooleano(PARCIAL),
)

internal fun Membro.paraDocumento(): Map<String, Any?> = mapOf(
    // Denormalizado de propósito: sem ele o leaderboard de 8 pessoas custaria 8
    // leituras extras em `users/` (docs/05 §1).
    NOME to nome,
    ENTROU_EM to entrouEm.paraTimestamp(),
    // RN-19: é daqui que sai o denominador da aderência de quem entrou no meio do
    // plano. Gravado errado, ninguém percebe — os testes de `F1-T04` recebem o
    // campo pronto.
    ENTROU_NA_SEMANA to entrouNaSemana,
    POSICAO_POKEDEX to posicaoPokedex,
    ATIVO to ativo,
)

internal fun DocumentSnapshot.paraMembro(): Membro = Membro(
    uid = id,
    nome = exigirTexto(NOME),
    entrouEm = exigirInstante(ENTROU_EM),
    entrouNaSemana = exigirInteiro(ENTROU_NA_SEMANA),
    // Nulo até `F4-T12` denormalizar a posição para o leaderboard.
    posicaoPokedex = inteiroOuNulo(POSICAO_POKEDEX),
    ativo = exigirBooleano(ATIVO),
)
