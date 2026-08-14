package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.hggabriel.pokerun.dominio.regras.sortearCodigoDeConvite
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private const val CONVITES = "invites"
private const val PLANO_ID = "plano_id"

/**
 * Quantos sorteios antes de desistir.
 *
 * O alfabeto de 31 em 6 posições dá 888 milhões de códigos, e o app é de oito pessoas:
 * a chance de duas colisões seguidas é indistinguível de zero. O laço existe para o caso
 * que a regra prevê, não para o que ela espera — e desistir depois de cinco é o que
 * impede uma falha de rede persistente de virar laço infinito.
 */
private const val TENTATIVAS_DE_RESERVA = 5

/**
 * A coleção `invites/{codigo}` (`F1-T14`, docs/05 §1, RN-29).
 *
 * **O código é o ID do documento, e é isso que o torna único.** Não há campo `codigo`
 * nem consulta: `invites` é `allow list: if false`, e um `create` sobre um ID que já
 * existe falha sozinho. A unicidade é estrutural, do servidor, e não depende de ninguém
 * lembrar de checar antes.
 *
 * **Resolução por leitura direta pelo ID, nunca por consulta em `plans`** (RN-29). Uma
 * *query* por `codigo_convite` transformaria seis caracteres num alvo de força bruta com
 * resposta paginada; ler `invites/{codigo}` responde uma coisa só sobre um código só.
 *
 * **Nada aqui resolve offline**, e é decisão da especificação (docs/05 §2.6): a reserva
 * é transacional e a resolução vai ao servidor de propósito. Ver [planoDoCodigo].
 */
class ConviteRepositorio(private val firestore: FirebaseFirestore) {

    private fun convite(codigo: String) = firestore.collection(CONVITES).document(codigo)

    /**
     * // RN-29
     *
     * O plano de um código, ou `null` se o código não existe.
     *
     * **A leitura é `Source.SERVER`, e a diferença é a mensagem que o usuário lê.** O
     * `get()` padrão cai no cache quando o servidor não responde, e o cache de um app que
     * nunca viu aquele convite responde *"não existe"* — a tela diria *"nenhum plano com
     * este código"* para quem só está sem rede, e a pessoa iria conferir letra por letra
     * um código correto. Indo ao servidor, a falta de rede vira exceção e a tela mostra
     * erro de conexão, que é a verdade.
     */
    suspend fun planoDoCodigo(codigo: String): String? =
        convite(codigo).get(Source.SERVER).await()
            .takeIf { it.exists() }
            ?.getString(PLANO_ID)

    /**
     * // RN-29
     *
     * Sorteia e **reserva** um código para [planoId], devolvendo o que ficou gravado.
     *
     * A transação é o que dá sentido ao laço: ela lê o documento e só escreve se ele não
     * existir, então uma colisão volta como "não reservou" e dispara outro sorteio, em
     * vez de sobrescrever o convite de outro plano. A rule sozinha já negaria a
     * sobrescrita (`update` é `false` em `invites`), mas ali a negativa chegaria como
     * `PERMISSION_DENIED` — indistinguível de uma regra quebrada, e é justamente o tipo
     * de erro que se diagnostica errado às onze da noite.
     *
     * **Chamada antes de o plano existir**, com o ID vindo de `PlanoRepositorio.novoId`:
     * assim o código gravado em `plans/{id}.codigo_convite` é sempre um código reservado.
     * A ordem inversa deixaria uma janela em que dois planos carregam o mesmo código.
     *
     * O preço da ordem escolhida é um convite órfão quando a criação do plano falha
     * depois da reserva. Ele é inofensivo e a `JoinPlanScreen` já o trata: convite que
     * aponta para plano inexistente é `NaoEncontrado`. Apagá-lo não é opção — `invites`
     * é `allow delete: if false`.
     */
    suspend fun reservarCodigo(planoId: String, aleatorio: Random = Random.Default): String {
        repeat(TENTATIVAS_DE_RESERVA) {
            val codigo = sortearCodigoDeConvite(aleatorio)
            val documento = convite(codigo)

            val reservou = firestore.runTransaction { transacao ->
                if (transacao.get(documento).exists()) {
                    false
                } else {
                    transacao.set(documento, mapOf(PLANO_ID to planoId))
                    true
                }
            }.await()

            if (reservou) return codigo
        }

        error("$TENTATIVAS_DE_RESERVA códigos sorteados e todos já existiam (RN-29)")
    }
}
