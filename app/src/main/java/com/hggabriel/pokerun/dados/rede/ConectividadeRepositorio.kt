package com.hggabriel.pokerun.dados.rede

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Se o aparelho tem internet **agora** (`F1-T10`).
 *
 * ### Por que uma tela precisa disto, num app que é offline-first no resto
 *
 * O Firestore resolve escrita offline sozinho, e é por isso que nenhuma outra tela
 * pergunta. Criar plano é a exceção: a reserva do código de convite é um `create`
 * transacional em `invites/{codigo}` (RN-29), e transação **não** resolve no cache — ela
 * precisa do servidor para saber se o código já existe. Somada à decisão de `F1-T05` de
 * resolver a escrita na confirmação do servidor, uma criação offline não falha: ela
 * **fica pendurada**, e o usuário olha um botão ocupado até desistir.
 *
 * A `CreatePlanScreen` bloqueia antes disso e diz o motivo (docs/03 §3.5).
 *
 * ### `NET_CAPABILITY_VALIDATED`, e não só "tem rede"
 *
 * `hasCapability(INTERNET)` é verdadeiro num Wi-Fi de hotel com portal cativo, que é
 * exatamente o lugar onde a escrita ficaria pendurada. `VALIDATED` é o sistema dizendo
 * que a sondagem dele saiu e voltou.
 *
 * **Isto é dica, não garantia.** A rede pode cair entre o `true` e o commit. O que a
 * checagem compra é o caso comum — avião, metrô, dado móvel desligado — e não uma
 * promessa de entrega.
 */
class ConectividadeRepositorio(private val contexto: Context) {

    private val gerenciador: ConnectivityManager?
        get() = contexto.getSystemService()

    /**
     * `true` enquanto houver rede validada. Emite de novo a cada mudança, então a tela
     * destrava sozinha quando o Wi-Fi volta — sem botão de "tentar de novo".
     */
    fun online(): Flow<Boolean> = callbackFlow {
        val cm = gerenciador
        if (cm == null) {
            // Sem `ConnectivityManager` não há como responder, e responder "offline"
            // travaria a tela para sempre. A dúvida joga a favor do usuário: ele tenta,
            // e o pior caso é o erro que a criação já sabe mostrar.
            trySend(true)
            close()
            return@callbackFlow
        }

        trySend(temRedeValidada(cm))

        val ouvinte = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(temRedeValidada(cm))
            }

            override fun onLost(network: Network) {
                trySend(temRedeValidada(cm))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capacidades: NetworkCapabilities,
            ) {
                trySend(temRedeValidada(cm))
            }
        }

        val pedido = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(pedido, ouvinte)

        awaitClose { cm.unregisterNetworkCallback(ouvinte) }
    }.distinctUntilChanged()

    private fun temRedeValidada(cm: ConnectivityManager): Boolean {
        val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
