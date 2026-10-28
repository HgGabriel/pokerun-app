package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
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
private const val CURTA_KM = "curta_km"

private const val DATA_INICIO = "data_inicio"
private const val DATA_FIM = "data_fim"
private const val SESSOES_ALVO = "sessoes_alvo"
private const val KM_ALVO = "km_alvo"
private const val LONGAO_KM = "longao_km"
private const val LONGAO_PEDIDO_KM = "longao_pedido_km"
private const val TIPO = "tipo"
private const val PARCIAL = "parcial"

private const val ENTROU_EM = "entrou_em"
private const val ENTROU_NA_SEMANA = "entrou_na_semana"
private const val POSICAO_POKEDEX = "posicao_pokedex"
private const val AVATAR_ESPECIE = "avatar_especie"
private const val ATIVO = "ativo"

/**
 * O agregado do plano: `plans/{planId}` e as subcoleções `weeks` e `members`
 * (`F1-T05`, docs/05 §1 e §2).
 *
 * **O documento de `invites/` não está aqui**, e é decisão de escopo: ele é
 * [ConviteRepositorio], porque a coleção é raiz e não subcoleção do plano. Quem cria
 * um plano reserva o código lá **antes** de chamar [criar], usando o ID de [novoId] —
 * assim o `codigo_convite` gravado aqui já é um código único (RN-29).
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
     * Uma leitura só, para a prévia do plano que um código de convite resolveu
     * (`F1-T14`, docs/03 §3.8). `null` é plano que não existe.
     *
     * **É `Source.SERVER`, e não a primeira emissão de [observar]**, pelo mesmo motivo
     * de `ConviteRepositorio.planoDoCodigo`: o cache de quem nunca viu aquele plano
     * responde "não existe", e a tela diria *"nenhum plano com este código"* para quem
     * só está sem rede. Aqui isso é pior do que parece, porque o convite acabou de
     * resolver — a mensagem acusaria o código de errado logo depois de ele ter dado
     * certo.
     */
    suspend fun buscar(planoId: String): Plano? =
        plano(planoId).get(Source.SERVER).await().takeIf { it.exists() }?.paraPlano()

    /**
     * Se [uid] já é membro de [planoId] (`F1-T14`, docs/03 §3.8).
     *
     * **A rule deixa qualquer um ler o próprio `members/{uid}`** (`eu(uid)`), e a
     * correção de 05/08 existe exatamente para isto: sem ela, quem ainda não é membro
     * levava `PERMISSION_DENIED` e a tela não conseguia separar `JaMembro` de erro de
     * rede.
     *
     * `Source.SERVER` porque um falso "não é membro" faria a entrada gravar por cima do
     * documento existente, e junto dele o `entrou_em` e o `entrou_na_semana` de quem já
     * treina no plano há semanas. RN-19 sai do segundo campo.
     */
    suspend fun souMembro(planoId: String, uid: String): Boolean =
        plano(planoId).collection(MEMBROS).document(uid).get(Source.SERVER).await().exists()

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

    /**
     * A grade numa leitura só, para quem acabou de entrar no plano (`F1-T14`).
     *
     * **Existe por causa de RN-19**, e a ordem das operações é forçada pela rule: a
     * leitura de `weeks` exige `souMembro(planId)`, então ninguém consegue saber em que
     * semana está entrando **antes** de já ser membro. Quem entra grava o documento,
     * lê a grade aqui e só então resolve o `entrou_na_semana`.
     *
     * **`Source.SERVER` e não [observarSemanas]**: a primeira emissão de um listener vem
     * do cache, e o cache de um plano que o aparelho nunca leu está vazio. Uma grade
     * vazia daria `semanaRef` nula e o membro começaria a contar aderência da semana 1,
     * que é o denominador inteiro do plano — o erro que RN-19 existe para impedir.
     */
    suspend fun buscarSemanas(planoId: String): List<Semana> =
        plano(planoId).collection(SEMANAS).get(Source.SERVER).await()
            .documents.map { it.paraSemana() }.sortedBy { it.numero }

    /**
     * A grade de um plano que o usuário pode não ver mais: vazia quando a rule recusa.
     *
     * É a leitura do replay, que busca a grade de **todo** plano em que há corrida do
     * usuário. Sair de um plano apaga o documento de membro, e a rule de `weeks` passa a
     * recusar a leitura. Com [buscarSemanas], essa recusa derrubava o replay inteiro e
     * nenhuma corrida nova creditava XP (27/10). Sem grade, as corridas daquele plano
     * contam como avulsas. Outras falhas, como rede, continuam subindo.
     */
    suspend fun buscarSemanasSeAcessivel(planoId: String): List<Semana> = try {
        buscarSemanas(planoId)
    } catch (erro: FirebaseFirestoreException) {
        if (erro.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw erro
        emptyList()
    }

    /** O documento de membro de um usuário, ou nulo se ele não é membro. */
    suspend fun buscarMembro(planoId: String, uid: String): Membro? =
        plano(planoId).collection(MEMBROS).document(uid).get().await()
            .takeIf { it.exists() }?.paraMembro()

    /**
     * Os membros numa leitura só, **do servidor**, para quem lê uma vez ao abrir.
     *
     * `observarMembros(...).first()` devolvia a primeira emissão do listener, que vem do
     * cache local quando o aparelho já leu o plano. O nome, o treinador e o parceiro que o
     * outro membro trocou no Perfil nunca chegavam ao ranking nem ao pulso da Home (27/10).
     * Sem rede, cai no cache: o grupo antigo é melhor que tela de erro.
     */
    suspend fun buscarMembros(planoId: String): List<Membro> {
        val membros = plano(planoId).collection(MEMBROS)
        val consulta = try {
            membros.get(Source.SERVER).await()
        } catch (erro: FirebaseFirestoreException) {
            if (erro.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw erro
            membros.get(Source.CACHE).await()
        }
        return consulta.documents.map { it.paraMembro() }
    }

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
     * Grava as semanas que uma edição do dono mudou (docs/01 §3.3). Uma edição mexe em
     * mais de uma semana — as seguintes podem descer até o teto novo, e o taper sai do
     * maior longão do build —, e **a remessa é uma só**: metade gravada deixaria uma
     * grade que nenhuma regra gera.
     *
     * A escrita é de três campos por semana, e não um `set` do documento: reescrever a
     * semana a partir de um modelo em memória arrastaria junto as fronteiras de data, e
     * é delas que RN-05 depende. A trava de RN-05 é da rule, que compara `request.time`
     * com `data_fim`; quem chama passa só semanas não congeladas.
     */
    suspend fun atualizarSemanas(planoId: String, semanas: List<Semana>) {
        if (semanas.isEmpty()) return
        val remessa = firestore.batch()
        semanas.forEach { semana ->
            remessa.update(
                plano(planoId).collection(SEMANAS).document(semana.numero.toString()),
                mapOf(
                    LONGAO_KM to semana.longaoKm,
                    KM_ALVO to semana.kmAlvo,
                    LONGAO_PEDIDO_KM to semana.longaoPedidoKm,
                ),
            )
        }
        remessa.commit().await()
    }

    /**
     * // RN-06
     *
     * Troca a curta do plano e grava o volume das semanas que ela refaz (docs/01 §3.3),
     * numa remessa só. A rule de `plans` aceita a escrita do dono enquanto o plano não
     * encerrou (RN-07).
     */
    suspend fun atualizarCurta(planoId: String, curtaKm: Double, semanas: List<Semana>) {
        val remessa = firestore.batch()
        remessa.update(plano(planoId), "$PARAMS_GERACAO.$CURTA_KM", curtaKm)
        semanas.forEach { semana ->
            remessa.update(
                plano(planoId).collection(SEMANAS).document(semana.numero.toString()),
                mapOf(LONGAO_KM to semana.longaoKm, KM_ALVO to semana.kmAlvo),
            )
        }
        remessa.commit().await()
    }

    /**
     * Entrar num plano é criar o próprio documento em `members/{uid}`.
     *
     * **É `set` com merge, e por isso serve também para corrigir o próprio documento** — a
     * rule aceita `update: if eu(uid)`. Quem entra por convite chama duas vezes: a primeira
     * cria a membresia, que é o que destrava a leitura de `weeks`, e a segunda grava o
     * `entrou_na_semana` de RN-19, que só pode ser calculado depois. A ordem é imposta
     * pela rule, não escolhida — ver [buscarSemanas].
     *
     * O merge existe porque o replay grava `avatar_especie` e `posicao_pokedex` por `update`
     * entre as duas chamadas: um `set` sem merge apagava os dois, e o grupo mostrava a
     * posição 1 da escada (o Caterpie) para quem já estava adiante (estudo, §b).
     */
    suspend fun entrar(planoId: String, membro: Membro) {
        plano(planoId).collection(MEMBROS).document(membro.uid)
            .set(membro.paraDocumento(), SetOptions.merge()).await()
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
        CURTA_KM to parametros.curtaKm,
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
 * tipo. Os valores são lidos um a um, com o mesmo critério de falhar alto do resto do
 * pacote: a grade editada deixa de ser dedutível dos campos do plano, então este bloco é
 * a única memória da entrada do formulário.
 *
 * **`curta_km` é a exceção.** Ele entrou em 21/10/2026 (DA-10), e plano gravado antes
 * não o tem até a migração de docs/05 §2.9 passar por ele. Sem o campo, a curta é a da
 * semana 0 da regra anterior — metade da baseline —, para o app abrir o plano em vez de
 * cair. A regra corrente não depende disso: ela só usa a curta ao recalcular semanas.
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
        curtaKm = (bruto[CURTA_KM] as? Number)?.toDouble()
            ?: ((valor(BASELINE_KM) as Number).toDouble() / 2),
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
    LONGAO_PEDIDO_KM to longaoPedidoKm,
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
    // Nulo na semana da prova, e na 2ª de taper dos planos gerados antes de 21/10/2026.
    longaoKm = decimalOuNulo(LONGAO_KM),
    tipo = tipoDeSemanaDoDocumento(exigirTexto(TIPO)),
    parcial = exigirBooleano(PARCIAL),
    // Nulo quando a semana segue a rampa (docs/01 §3.3).
    longaoPedidoKm = decimalOuNulo(LONGAO_PEDIDO_KM),
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
) + listOfNotNull(
    // O avatar nasce com o documento (estudo, §b): antes ele só vinha do replay, e o grupo
    // mostrava a posição 1 da escada até a primeira corrida depois de entrar. Nulo fica
    // fora do mapa, para o merge de [PlanoRepositorio.entrar] não apagar o que o replay gravou.
    avatarEspecie?.let { AVATAR_ESPECIE to it },
    parceiro?.let { CAMPO_PARCEIRO to it },
    treinador?.let { CAMPO_TREINADOR to it },
)

internal fun DocumentSnapshot.paraMembro(): Membro = Membro(
    uid = id,
    nome = exigirTexto(NOME),
    entrouEm = exigirInstante(ENTROU_EM),
    entrouNaSemana = exigirInteiro(ENTROU_NA_SEMANA),
    // Nulo até `F4-T12` denormalizar a posição para o leaderboard.
    posicaoPokedex = inteiroOuNulo(POSICAO_POKEDEX),
    ativo = exigirBooleano(ATIVO),
    // Nasce com o documento desde 27/10/2026, e o replay o atualiza (`F2-T07`).
    avatarEspecie = inteiroOuNulo(AVATAR_ESPECIE),
    // Ausentes até a pessoa escolher no Perfil (estudo de Progresso e Perfil, F1).
    parceiro = inteiroOuNulo(CAMPO_PARCEIRO),
    treinador = getString(CAMPO_TREINADOR),
)
