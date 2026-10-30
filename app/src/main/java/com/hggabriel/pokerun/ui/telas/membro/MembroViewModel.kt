package com.hggabriel.pokerun.ui.telas.membro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.Parceiro
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.escada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.ui.telas.perfil.DadosDaCredencial
import com.hggabriel.pokerun.ui.telas.perfil.ParceiroDaCredencial
import com.hggabriel.pokerun.ui.telas.perfil.PerfilUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** A semana do membro no plano, do `weekly` dele: o grupo lê esse documento por `get`. */
data class SemanaDoMembro(
    val numero: Int,
    val sessoesFeitas: Int,
    val sessoesPrevistas: Int,
    val kmFeito: Double,
    val longaoFeito: Double,
    /** Nulo na semana da prova, que não tem longão. */
    val longaoAlvo: Double?,
    val xp: Long,
)

/** A Pokédex do membro na temporada corrente: a escada até a `colecao_max` e a próxima. */
data class PokedexDoMembro(
    val temporada: String,
    val colecao: List<Int>,
    val proxima: Int?,
    val total: Int,
)

sealed interface MembroUiState {
    data object Carregando : MembroUiState
    data object Falhou : MembroUiState

    data class Conteudo(
        /** O estado que a credencial do Perfil desenha, montado com o que o grupo lê. */
        val credencial: PerfilUiState,
        val semana: SemanaDoMembro?,
        val pokedex: PokedexDoMembro?,
        val noGrupoDesde: LocalDate,
        val ultimaCorrida: LocalDate?,
    ) : MembroUiState
}

/**
 * O cartão do treinador de um membro (27/10/2026). Lê **uma vez** ao abrir, como o ranking:
 * `members/{uid}` (nome, parceiro, treinador, coleção e a vitrine que o replay do dono
 * copia), o `weekly` da semana corrente e o catálogo da temporada. Nada aqui lê `users/`,
 * que a rule fecha para o grupo.
 */
class MembroViewModel(
    private val planoId: String,
    private val uid: String,
    private val planos: PlanoRepositorio,
    private val progresso: ProgressoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow<MembroUiState>(MembroUiState.Carregando)
    val estado: StateFlow<MembroUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun tentarDeNovo() = carregar()

    // Com conteúdo na tela, reler (o puxar) não volta ao esqueleto, e a falha mantém o que já
    // estava lá: só a primeira leitura mostra a espera e o erro.
    private fun carregar() {
        _estado.update { if (it is MembroUiState.Conteudo) it else MembroUiState.Carregando }
        viewModelScope.launch {
            try {
                _estado.value = ler()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.update { if (it is MembroUiState.Conteudo) it else MembroUiState.Falhou }
            }
        }
    }

    private suspend fun ler(): MembroUiState {
        val membro = planos.buscarMembro(planoId, uid) ?: return MembroUiState.Falhou
        val plano = planos.buscar(planoId) ?: return MembroUiState.Falhou
        val agora = relogio.instant()
        val fuso = plano.fuso
        val grade = planos.buscarSemanas(planoId)
        val semana = CalendarioDoPlano.semanaDe(agora, plano, grade) ?: grade.lastOrNull()
        val semanal = semana?.let { progresso.semanal(uid, Replay.chaveDaSemana(planoId, it.numero)) }
        val temporada = temporadaDe(agora, temporadas.todas())

        val parceiro = temporada?.let { Parceiro.doMembro(membro.parceiro, membro.avatarEspecie, it) }?.let { numero ->
            ParceiroDaCredencial(
                numero = numero,
                nome = especies.nome(numero),
                tipos = especies.tipos(numero),
                render = especies.spriteDoHome(numero),
                reserva = especies.sprite(numero),
            )
        }
        val colecao = temporada?.let { membro.avatarEspecie ?: 1 }

        val credencial = PerfilUiState(
            carregando = false,
            nome = membro.nome,
            treinador = membro.treinador,
            parceiro = parceiro,
            credencial = DadosDaCredencial(
                desde = membro.primeiraCorridaEm?.atZone(fuso)?.toLocalDate(),
                naVidaKm = membro.naVidaKm ?: 0.0,
                colecao = colecao,
                totalEspecies = temporada?.totalEspecies,
                prova = plano.dataProva.atZone(fuso).toLocalDate(),
                temporada = temporada?.nome,
            ),
        )

        val pokedex = temporada?.let { t ->
            val numeros = escada(t).map { it.first }
            val ate = (colecao ?: 1).coerceIn(0, numeros.size)
            PokedexDoMembro(t.nome, numeros.take(ate), numeros.getOrNull(ate), numeros.size)
        }

        return MembroUiState.Conteudo(
            credencial = credencial,
            semana = semana?.let {
                SemanaDoMembro(
                    numero = it.numero,
                    sessoesFeitas = semanal?.sessoesFeitas ?: 0,
                    sessoesPrevistas = it.sessoesAlvo,
                    kmFeito = semanal?.kmFeito ?: 0.0,
                    longaoFeito = semanal?.longaoFeito ?: 0.0,
                    longaoAlvo = it.longaoKm,
                    xp = semanal?.xpSemana ?: 0L,
                )
            },
            pokedex = pokedex,
            noGrupoDesde = membro.entrouEm.atZone(fuso).toLocalDate(),
            ultimaCorrida = membro.ultimaCorridaEm?.atZone(fuso)?.toLocalDate(),
        )
    }
}
