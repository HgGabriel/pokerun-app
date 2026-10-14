package com.hggabriel.pokerun.ui.telas.grupo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.DesafioRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.DesafioColetivo
import com.hggabriel.pokerun.dominio.regras.LinhaDoRanking
import com.hggabriel.pokerun.dominio.regras.Ranking
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.numeroNaEscada
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/** A `SocialLeaderboardScreen` (docs/03 §3.16). */
sealed interface GrupoUiState {

    data object Carregando : GrupoUiState

    /** Sem plano ativo não há grupo: o ranking é do plano (RN-12). */
    data object SemPlano : GrupoUiState

    data class Conteudo(
        val nomeDoPlano: String,
        val codigoConvite: String,
        val linhas: List<LinhaDoGrupo>,
        val desafio: BlocoDoDesafio? = null,
    ) : GrupoUiState {
        /** O vazio de membro único de docs/03 §3.16. */
        val sozinho: Boolean get() = linhas.size <= 1
    }

    data object Falhou : GrupoUiState
}

/** O desafio coletivo da semana e as insígnias do grupo (`F2-T13`, docs/03 §3.16). */
data class BlocoDoDesafio(
    val meta: Double,
    val acumulado: Double,
    val concluido: Boolean,
    val insigniaDaSemana: String?,
    val insignias: List<String>,
)

/** Uma linha do ranking com o avatar já resolvido em número, nome e sprite. */
data class LinhaDoGrupo(
    val ranking: LinhaDoRanking,
    val numeroDaEspecie: Int?,
    val nomeDaEspecie: String?,
    val sprite: String?,
)

/**
 * O ranking do grupo (`F2-T12`). Lê **uma vez** ao abrir, e de novo no repetir: a ficha
 * proíbe listener contínuo sobre `weekly`. O avatar é a posição na escada que o replay
 * de cada membro grava em `members/{uid}.avatar_especie`; o número sai da escada da
 * temporada corrente, e o nome e o sprite, da PokéAPI (D-10). O esvanecimento de RN-36
 * não vale aqui (docs/03 §3.16.1).
 */
class GrupoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val progresso: ProgressoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val especies: EspeciesRepositorio,
    private val desafios: DesafioRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow<GrupoUiState>(GrupoUiState.Carregando)
    val estado: StateFlow<GrupoUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun tentarDeNovo() = carregar()

    private fun carregar() {
        _estado.update { GrupoUiState.Carregando }
        viewModelScope.launch {
            try {
                _estado.value = ler()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.value = GrupoUiState.Falhou
            }
        }
    }

    private suspend fun ler(): GrupoUiState {
        val uid = autenticacao.uidAtual ?: return GrupoUiState.Falhou
        val planoId = usuarios.buscar(uid)?.planoAtivoId ?: return GrupoUiState.SemPlano
        val plano = planos.buscar(planoId) ?: return GrupoUiState.SemPlano
        val grade = planos.buscarSemanas(planoId)
        val agora = relogio.instant()
        // A semana corrente; fora da grade (antes do início ou depois da prova), a última.
        val semana = CalendarioDoPlano.semanaDe(agora, plano, grade) ?: grade.lastOrNull() ?: return GrupoUiState.Falhou
        val membros = planos.observarMembros(planoId).first()
        val chave = Replay.chaveDaSemana(planoId, semana.numero)

        val semanais = coroutineScope {
            membros.map { membro -> async { membro.uid to progresso.semanal(membro.uid, chave) } }.awaitAll().toMap()
        }
        val temporada = temporadaDe(agora, temporadas.todas())
        val ranking = Ranking.ordenar(membros, semanais, semana.sessoesAlvo)

        val linhas = ranking.map { linha ->
            val numero = temporada?.let { numeroNaEscada(linha.avatarEspecie ?: 1, it) }
            LinhaDoGrupo(
                ranking = linha,
                numeroDaEspecie = numero,
                nomeDaEspecie = numero?.let { especies.nome(it) },
                sprite = numero?.let { especies.sprite(it) },
            )
        }
        return GrupoUiState.Conteudo(plano.nome, plano.codigoConvite, linhas, desafio(planoId, semana, membros, semanais))
    }

    /**
     * O desafio da semana, refeito sobre os `weekly` que o ranking já leu (decisão nº 129).
     * Quando muda, é gravado em `desafios/{semanaRef}`, sem segurar a tela: a rule deixa
     * qualquer membro escrever. Falha na leitura deixa a tela sem o bloco, e não sem ranking.
     */
    private suspend fun desafio(
        planoId: String,
        semana: Semana,
        membros: List<Membro>,
        semanais: Map<String, AgregadoDaSemana?>,
    ): BlocoDoDesafio? {
        val todos = try {
            desafios.buscarTodos(planoId)
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            return null
        }
        val ativos = membros.filter { it.ativo }
        val existente = todos[semana.numero]
        val atual = DesafioColetivo.atualizar(
            existente = existente,
            semana = semana,
            membrosAtivos = ativos.size,
            semanais = ativos.map { semanais[it.uid] },
            concluidosAntes = todos.filterKeys { it < semana.numero }.values.count { it.concluido },
        )
        if (atual != existente) {
            viewModelScope.launch {
                try {
                    desafios.gravar(planoId, semana.numero, atual)
                } catch (cancelamento: CancellationException) {
                    throw cancelamento
                } catch (_: Exception) {
                    // A próxima abertura do ranking refaz e grava de novo.
                }
            }
        }
        return BlocoDoDesafio(
            meta = atual.meta,
            acumulado = atual.acumulado,
            concluido = atual.concluido,
            insigniaDaSemana = atual.insigniaId,
            insignias = DesafioColetivo.conquistadas(todos.values + atual),
        )
    }
}
