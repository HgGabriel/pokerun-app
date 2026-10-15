package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dominio.regras.Regressao
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/**
 * O backfill da Fase 2 (docs/06 §2): na primeira abertura com o XP, um replay desde a
 * primeira corrida. Sem ele, as corridas da Fase 1 ficam sem XP e o card da espécie
 * abre todo mundo no `#001`.
 *
 * **O marcador é a ausência do progresso da temporada corrente**, e não um campo novo:
 * o replay grava `users/{uid}/temporadas/{id}` assim que há uma corrida na temporada,
 * então o documento existir é a prova de que o backfill já rodou. Quem ainda não correu
 * nesta temporada repete a conferência a cada abertura, ao custo de uma leitura.
 *
 * **É também o gatilho da regressão por inatividade** (`F4-T10`, RN-37): sem serviço em
 * segundo plano, a queda de 2% por semana se materializa na abertura, quando o progresso
 * grava menos quedas do que a lacuna desde a última corrida já impõe.
 *
 * Roda no escopo do app, e a falha fica para a próxima abertura.
 */
class BackfillDeXp(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val motor: MotorDeReplay,
    private val escopo: CoroutineScope,
    private val relogio: Clock = Clock.systemUTC(),
) {

    fun seNecessario() {
        val uid = autenticacao.uidAtual ?: return
        escopo.launch {
            try {
                val agora = relogio.instant()
                val temporada = temporadaDe(agora, temporadas.todas()) ?: return@launch
                val atual = progresso.lerProgresso(uid, temporada.id)
                // Sem progresso: o backfill. Com ele: só quando passou uma semana inativa
                // que o replay anterior não descontou (`F4-T10`, RN-37).
                if (atual != null && !Regressao.pendente(atual.ultimaCorridaEm, atual.semanasDeQueda, agora)) {
                    return@launch
                }
                motor.replay(uid, desde = Instant.EPOCH, ate = agora)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // Sem rede ou com a rule recusando uma remessa: a próxima abertura tenta.
            }
        }
    }
}
