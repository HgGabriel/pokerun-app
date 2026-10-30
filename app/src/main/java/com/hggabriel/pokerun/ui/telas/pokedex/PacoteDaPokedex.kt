package com.hggabriel.pokerun.ui.telas.pokedex

import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.local.PreferenciasDoAparelho
import com.hggabriel.pokerun.dominio.regras.figurinhasDoPacote
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock

/**
 * Quantas figurinhas esperam no pacote do álbum da temporada de hoje (29/10/2026).
 *
 * É a mesma conta da aba Pokédex (coleção de agora menos a coleção vista), lida fora dela: a
 * barra põe o número no badge da aba, e a arena do registro sabe se a corrida deixou um
 * pacote. Assim, o que a corrida liberou aparece onde mudou, e some no mesmo quadro em que o
 * pacote abre.
 *
 * Na primeira visita não há pacote (a coleção vista é nula): o que já estava colado chega na
 * cascata de estreia.
 */
class PacoteDaPokedex(
    private val autenticacao: AutenticacaoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val preferencias: PreferenciasDoAparelho,
    private val relogio: Clock = Clock.systemUTC(),
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun quantas(): Flow<Int> {
        val uid = autenticacao.uidAtual ?: return flowOf(0)
        return flow { emit(temporadaDe(relogio.instant(), temporadas.todas())) }
            .flatMapLatest { temporada ->
                if (temporada == null) {
                    flowOf(0)
                } else {
                    combine(progresso.observarProgresso(uid, temporada.id), preferencias.colecaoVista(uid, temporada.id)) { lido, vista ->
                        val colecao = maxOf(lido?.colecaoMax ?: 1, posicaoNaEscada(lido?.xpTotal ?: 0L, temporada))
                        // A marca de partida sai daqui, e não só da primeira visita à aba: sem ela, a
                        // primeira corrida de quem ainda não abriu a Pokédex chegava sem pacote, e a
                        // visita seguinte tomava a coleção nova como vista. Só com progresso lido.
                        if (vista == null && lido != null) preferencias.definirColecaoVista(uid, temporada.id, colecao)
                        figurinhasDoPacote(colecao, vista).count()
                    }
                }
            }
            .distinctUntilChanged()
            .catch { emit(0) }
    }
}
