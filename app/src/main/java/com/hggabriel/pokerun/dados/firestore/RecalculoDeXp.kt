package com.hggabriel.pokerun.dados.firestore

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/**
 * // XP-01
 *
 * Toda gravação de corrida dispara o replay a partir do início da semana dela (docs/05
 * §2.5). Esta classe costura as duas coisas **num escopo que vive com o processo**, e
 * não com a tela.
 *
 * A tela de registro sai antes de o servidor confirmar (decisão nº 4), e o `ViewModel`
 * dela morre junto. Um replay lançado no `viewModelScope` seria cancelado no meio, e a
 * corrida ficaria gravada sem XP até alguém apertar o recálculo.
 *
 * Falha do replay não volta para a tela: a corrida já está gravada, que é o que o
 * usuário pediu. O reparo é o recálculo (docs/05 §2.6), e o backfill da abertura.
 */
class RecalculoDeXp(
    private val motor: MotorDeReplay,
    private val escopo: CoroutineScope,
    private val relogio: Clock = Clock.systemUTC(),
) {

    /**
     * Roda [escrita] no escopo do app e, se ela der certo, o replay de [desde] até agora.
     * O `Deferred` devolvido é o da escrita: quem chama pode esperar por ele para mostrar
     * erro, e cancelar a espera não cancela nem a escrita nem o replay.
     */
    fun depoisDe(uid: String, desde: Instant, escrita: suspend () -> Unit): Deferred<Unit> {
        val gravacao = escopo.async { escrita() }
        escopo.launch {
            try {
                gravacao.await()
                motor.replay(uid, desde, relogio.instant())
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // Escrita recusada já chega à tela pelo `Deferred`. Replay que falhou
                // fica para o próximo replay, que recalcula tudo desde o começo.
            }
        }
        return gravacao
    }
}
