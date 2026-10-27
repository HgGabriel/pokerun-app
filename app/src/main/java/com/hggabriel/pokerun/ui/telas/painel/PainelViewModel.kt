package com.hggabriel.pokerun.ui.telas.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.dominio.regras.Parceiro
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/** O dia de hoje conta no fuso da temporada, que é o do ano civil de `kanto-2026` (nº 100). */
private val FUSO_DA_TEMPORADA: ZoneId = ZoneId.of("America/Sao_Paulo")

private data class Lidos(
    val corridas: List<Corrida>,
    val recordes: Recordes?,
    val usuario: Usuario?,
    val colecoes: Map<String, Int>,
    val catalogo: List<Temporada>,
)

/**
 * O Progresso (`F3-T09`, estudo de Progresso e Perfil): observa o histórico vitalício, os
 * recordes, o perfil (plano ativo e parceiro), a coleção e a grade do plano ativo. Nenhuma
 * escrita: o painel só lê (docs/05 §1).
 *
 * A raiz e as quatro telas das portas usam este mesmo `ViewModel`, cada uma na sua entrada
 * da pilha: a leitura repetida sai do cache do Firestore.
 */
class PainelViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val corridas: CorridaRepositorio,
    private val planos: PlanoRepositorio,
    private val progresso: ProgressoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.system(FUSO_DA_TEMPORADA),
) : ViewModel() {

    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<PainelUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(PainelUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = PainelUiState.Carregando,
        )

    fun tentarDeNovo() = tentativas.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun remoto(): Flow<PainelUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(PainelUiState.Falhou)
        val catalogo = flow { emit(temporadas.todas()) }

        return combine(
            corridas.observarHistorico(uid),
            progresso.observarRecordes(uid),
            usuarios.observar(uid),
            progresso.observarColecoes(uid),
            catalogo,
        ) { lista, recordes, usuario, colecoes, todas -> Lidos(lista, recordes, usuario, colecoes, todas) }
            .flatMapLatest { lidos ->
                val ativoId = lidos.usuario?.planoAtivoId
                val ids = (lidos.corridas.map { it.planoId } + listOfNotNull(ativoId)).distinct()
                // A grade do plano ativo alimenta o cartão da prova. Falhar nela tira o
                // cartão, e não a tela.
                val grade = if (ativoId == null) flowOf(emptyList()) else planos.observarSemanas(ativoId).catch { emit(emptyList()) }
                combine(planos.observarVarios(ids), grade) { lidosPlanos, semanas -> Triple(lidos, lidosPlanos, semanas) }
            }
            .mapLatest { (lidos, lidosPlanos, grade) ->
                val hoje = LocalDate.now(relogio)
                val temporada = temporadaDe(relogio.instant(), lidos.catalogo)
                val ativo = lidos.usuario?.planoAtivoId?.let { id -> lidosPlanos.firstOrNull { it.id == id } }
                val semanais = ativo?.let { semEsperarFalha(emptyMap()) { progresso.semanaisDoPlano(uid, it.id) } }.orEmpty()
                val parceiro = temporada?.let { Parceiro.efetivo(lidos.usuario?.parceiro, lidos.colecoes[it.id], it) }
                    ?: lidos.usuario?.parceiro
                val tipo = parceiro?.let { semEsperarFalha(null) { especies.tipos(it).firstOrNull() } }
                val montado = montarProgresso(
                    EntradaDoProgresso(
                        corridas = lidos.corridas,
                        planos = lidosPlanos,
                        recordes = lidos.recordes,
                        temporada = temporada,
                        catalogo = lidos.catalogo,
                        planoAtivo = ativo,
                        grade = grade,
                        semanais = semanais,
                        parceiro = parceiro,
                        tipoDoParceiro = tipo,
                        hoje = hoje,
                    ),
                )
                // O nome da espécie de cada fita, da PokéAPI ou da cópia guardada.
                montado.copy(
                    fitas = montado.fitas.map { fita ->
                        fita.especie?.let { numero -> fita.copy(nomeDaEspecie = semEsperarFalha(null) { especies.nome(numero) }) } ?: fita
                    },
                )
            }
    }

    /** Leitura de enfeite: a falha devolve [reserva], e a tela segue sem aquele pedaço. */
    private suspend fun <T> semEsperarFalha(reserva: T, bloco: suspend () -> T): T = try {
        bloco()
    } catch (cancelamento: CancellationException) {
        throw cancelamento
    } catch (_: Exception) {
        reserva
    }
}
