package com.hggabriel.pokerun.ui.telas.fechamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.RoteadorDaAbertura
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.QuebraDoXp
import com.hggabriel.pokerun.dominio.regras.Regressao
import com.hggabriel.pokerun.dominio.regras.SemanaAApresentar
import com.hggabriel.pokerun.dominio.regras.gradeDaPokedex
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.quebraDoXp
import com.hggabriel.pokerun.dominio.regras.semanaAApresentar
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration

/** O que a sequência do fechamento conta, já resolvido (docs/03 §3.20). */
data class FechamentoDaTela(
    val temporada: Temporada,
    val semana: SemanaAApresentar,
    /** A barra de sessões da semana que fechou, a mesma da Home (docs/03 §3.3.1). Vazia sem a grade. */
    val segmentos: List<SegmentoDaSemana>,
    val longaoKm: Double?,
    val xpDaSemana: Long,
    val quebra: QuebraDoXp,
    val sessoesFeitas: Int,
    val sessoesPrevistas: Int,
    val kmFeito: Double,
    val posicao: Int,
    val colecaoMax: Int,
    val xpTotal: Long,
    /** As espécies ultrapassadas entre a posição de antes e a de agora, no máximo [LIMITE_DAS_ULTRAPASSADAS]. */
    val ultrapassadas: List<CelulaDaPokedex>,
    val totalUltrapassadas: Int,
    val nomes: Map<Int, String>,
    val proximoAlvo: Int?,
    val faltamXp: Long?,
    /** Semanas inteiras sem correr, quando a regressão (RN-37) já agiu; senão nulo. */
    val semanasSemCorrer: Int?,
    val quedaEmPorcento: Int?,
    val proximaSemana: ResumoDaProximaSemana?,
) {
    val recuou: Boolean get() = semana.posicaoDepois < semana.posicaoAntes
    val semTreino: Boolean get() = sessoesFeitas == 0 && xpDaSemana == 0L
}

/** O que a semana seguinte pede (docs/03 §3.20, passo 5 e o vazio). */
data class ResumoDaProximaSemana(val numero: Int, val sessoes: Int, val longaoKm: Double?)

/** A `WeeklyCloseScreen` (docs/03 §3.20). */
sealed interface FechamentoUiState {
    data object Carregando : FechamentoUiState

    /** Nada a apresentar: a tela fecha sozinha. Alcançável se o estado mudou entre o roteador e aqui. */
    data object Nada : FechamentoUiState

    data class Conteudo(val fechamento: FechamentoDaTela) : FechamentoUiState

    data object Falhou : FechamentoUiState
}

/** Quantas silhuetas a sequência colore: o resto vira "e mais N". Um tier de 29 não cabe em 2 a 3 s. */
internal const val LIMITE_DAS_ULTRAPASSADAS = 9

/** Cada queda da regressão tira 2% (docs/04 §5.2), para a frase de docs/03 §3.20. */
private const val TAXA_DA_QUEDA = 0.02

/**
 * A `WeeklyCloseScreen` (`F4-T09`). **Leitura única**: o fechamento é uma foto da semana
 * que fechou, não um painel vivo, e o gatilho é a abertura (docs/03 §3.20). **Marca a
 * semana como apresentada ao ser exibida** (docs/01 §4.6), e a marcação é a única escrita.
 */
class FechamentoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val especies: EspeciesRepositorio,
    private val roteador: RoteadorDaAbertura,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow<FechamentoUiState>(FechamentoUiState.Carregando)
    val estado: StateFlow<FechamentoUiState> = _estado.asStateFlow()

    private var marcada = false

    init {
        carregar()
    }

    fun tentarDeNovo() = carregar()

    /** A estreia (docs/03 §3.20.1) pede a aba nova pelo roteador, que a casca consome. */
    fun abrirAPokedex() = roteador.pedirAPokedex()

    private fun carregar() {
        _estado.update { FechamentoUiState.Carregando }
        viewModelScope.launch {
            try {
                _estado.value = ler()
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.value = FechamentoUiState.Falhou
            }
        }
    }

    private suspend fun ler(): FechamentoUiState {
        val uid = autenticacao.uidAtual ?: return FechamentoUiState.Nada
        val agora = relogio.instant()
        val temporada = temporadaDe(agora, temporadas.todas()) ?: return FechamentoUiState.Nada
        val lido = progresso.lerProgresso(uid, temporada.id) ?: return FechamentoUiState.Nada
        val usuario = usuarios.buscar(uid) ?: return FechamentoUiState.Nada
        val semana = semanaAApresentar(lido.historico, usuario.semanaApresentada) ?: return FechamentoUiState.Nada

        val chave = semana.chave
        val semanal = progresso.semanal(uid, chave)
        val doPlano = corridas.observarDoPlano(uid, semana.planoId).first()
        val daSemana = doPlano.filter { it.semanaRef == semana.numero }
        val grade = try {
            planos.buscarSemanas(semana.planoId)
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            emptyList()
        }
        val prevista = grade.firstOrNull { it.numero == semana.numero }
        val proxima = grade.firstOrNull { it.numero == semana.numero + 1 }

        val xp = lido.xpTotal
        val posicao = posicaoNaEscada(xp, temporada)
        val colecao = maxOf(lido.colecaoMax, posicao)
        val celulas = gradeDaPokedex(temporada, posicao, colecao, xp)

        val ultrapassadas = if (semana.posicaoDepois > semana.posicaoAntes) {
            celulas.filter { it.posicao in (semana.posicaoAntes + 1)..semana.posicaoDepois }
        } else {
            emptyList()
        }
        val exibidas = ultrapassadas.takeLast(LIMITE_DAS_ULTRAPASSADAS)

        val proximo = celulas.firstOrNull { it.posicao == posicao + 1 }
        val numeros = (exibidas.map { it.numero } + listOfNotNull(proximo?.numero)).distinct()
        val nomes = numeros.mapNotNull { numero -> especies.nome(numero)?.let { numero to it } }.toMap()

        val semSemanas = Duration.between(lido.ultimaCorridaEm, agora).toDays().toInt() / 7
        val quedas = Regressao.semanasDeQueda(lido.ultimaCorridaEm, agora)
        val queda = if (quedas > 0) ((1 - Math.pow(1 - TAXA_DA_QUEDA, quedas.toDouble())) * 100).toInt() else null

        val fechamento = FechamentoDaTela(
            temporada = temporada,
            semana = semana,
            segmentos = prevista?.let { segmentosDaSemana(it, doPlano) }.orEmpty(),
            longaoKm = prevista?.longaoKm,
            xpDaSemana = semanal?.xpSemana ?: 0L,
            quebra = quebraDoXp(daSemana),
            sessoesFeitas = semanal?.sessoesFeitas ?: 0,
            sessoesPrevistas = prevista?.sessoesAlvo ?: 0,
            kmFeito = semanal?.kmFeito ?: 0.0,
            posicao = posicao,
            colecaoMax = colecao,
            xpTotal = xp,
            ultrapassadas = exibidas,
            totalUltrapassadas = ultrapassadas.size,
            nomes = nomes,
            proximoAlvo = proximo?.numero,
            faltamXp = proximo?.faltamXp,
            semanasSemCorrer = if (quedas > 0) semSemanas else null,
            quedaEmPorcento = queda,
            proximaSemana = proxima?.let { ResumoDaProximaSemana(it.numero, it.sessoesAlvo, it.longaoKm) },
        )

        marcarComoApresentada(uid, chave)
        return FechamentoUiState.Conteudo(fechamento)
    }

    /** Ao ser exibido, e uma vez: a escrita falhar não pode derrubar a tela. */
    private fun marcarComoApresentada(uid: String, chave: String) {
        if (marcada) return
        marcada = true
        viewModelScope.launch {
            try {
                usuarios.marcarSemanaApresentada(uid, chave)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                marcada = false
            }
        }
    }
}
