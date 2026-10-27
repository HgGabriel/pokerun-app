package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import java.time.Instant

/**
 * O avatar com que o documento de membro nasce (estudo de Progresso e Perfil, §b).
 *
 * É a mesma conta do replay: a `colecao_max` da temporada corrente (docs/03 §3.16.1). Antes,
 * o documento nascia sem `avatar_especie`, e o grupo mostrava a posição 1 da escada (o
 * Caterpie) até a próxima corrida de quem acabou de entrar.
 */
class IdentidadeDoMembro(
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
) {

    /**
     * Nulo quando a leitura falha ou não há temporada corrente. Nulo não entra no documento,
     * e o replay grava o campo depois: a falha aqui nunca derruba a criação nem a entrada.
     */
    suspend fun avatar(uid: String, agora: Instant): Int? = try {
        temporadaDe(agora, temporadas.todas())?.let { temporada ->
            progresso.lerProgresso(uid, temporada.id)?.colecaoMax ?: 1
        }
    } catch (cancelamento: CancellationException) {
        throw cancelamento
    } catch (erro: Exception) {
        null
    }
}
