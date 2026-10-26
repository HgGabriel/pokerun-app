package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.MarcoDaSemana
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.semanaAApresentar
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Instant

/**
 * O roteador da abertura (`F4-T09`, docs/01 §4.6):
 *
 * ```
 * Abertura do app
 *   └─ É a 1ª abertura da semana E existe semana fechada não apresentada?
 *          ├─ Não  → HomeScreen
 *          └─ Sim  → WeeklyCloseScreen → HomeScreen
 * ```
 *
 * "1ª abertura da semana" não é um estado a guardar: a semana fecha no domingo à meia-noite
 * do plano, e o marcador `semana_apresentada` só avança quando a tela é exibida. Depois de
 * apresentada, o mesmo `historico` não tem semana nova até o domingo seguinte.
 *
 * **Falha de leitura abre a Home.** O fechamento é um momento, e não pode travar a porta
 * de um app que só quer mostrar a semana (docs/03 §3.20: nada em segundo plano).
 *
 * Também carrega o pedido da estreia (docs/03 §3.20.1), `Abrir a Pokédex`: a tela do
 * fechamento está fora da casca, e a casca é quem troca de aba.
 */
class RoteadorDaAbertura(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val progresso: ProgressoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val motor: MotorDeReplay,
    private val relogio: Clock = Clock.systemUTC(),
) {

    private val pedidoDaPokedex = MutableStateFlow(false)

    /** `true` depois de a estreia pedir a aba Pokédex, até a casca consumir o pedido. */
    val pedidoDeAbrirAPokedex: StateFlow<Boolean> = pedidoDaPokedex.asStateFlow()

    fun pedirAPokedex() {
        pedidoDaPokedex.value = true
    }

    fun consumirPedidoDaPokedex() {
        pedidoDaPokedex.value = false
    }

    /** Há semana fechada que a tela cheia ainda não apresentou? */
    suspend fun precisaDoFechamento(): Boolean {
        val uid = autenticacao.uidAtual ?: return false
        return try {
            val agora = relogio.instant()
            val temporada = temporadaDe(agora, temporadas.todas()) ?: return false
            val lido = progresso.lerProgresso(uid, temporada.id) ?: return false
            val usuario = usuarios.buscar(uid) ?: return false
            val historico = historicoEmDia(uid, usuario.planos, lido, temporada, agora)
            semanaAApresentar(historico, usuario.semanaApresentada) != null
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            false
        }
    }

    /**
     * O `historico` com as semanas que fecharam depois do último replay.
     *
     * A semana fecha no domingo à meia-noite, mas o `historico` só muda num replay. Quem
     * correu na quinta e não correu mais chegava à segunda com a semana fora dele: sem o
     * fechamento e, na 1ª semana, sem a Pokédex. Aqui a abertura roda o replay que falta,
     * e espera por ele, para a tela abrir na mesma abertura.
     *
     * Duas peneiras antes do replay, para não pagar escrita a cada abertura:
     * 1. a semana fechou depois da última corrida. A que fechou antes entrou no replay do
     *    registro daquela corrida;
     * 2. o plano tem corrida do usuário. O replay só lê esses planos, e a semana de um
     *    plano sem corrida nunca entra no `historico`.
     *
     * Qualquer falha devolve o gravado: o fechamento fica para a próxima abertura.
     */
    private suspend fun historicoEmDia(
        uid: String,
        planosDoUsuario: List<String>,
        lido: ProgressoNaTemporada,
        temporada: Temporada,
        agora: Instant,
    ): List<MarcoDaSemana> = try {
        val grades = planosDoUsuario.associateWith { planos.buscarSemanas(it) }
        val candidatas = Replay.semanasForaDoHistorico(temporada, grades, lido.historico, agora)
            .filter { (_, semana) -> semana.dataFim > lido.ultimaCorridaEm }
        val comCorrida = if (candidatas.isEmpty()) emptySet() else corridas.buscarTodas(uid).mapTo(HashSet()) { it.planoId }
        val faltam = candidatas.filter { (planoId, _) -> planoId in comCorrida }
        if (faltam.isEmpty()) {
            lido.historico
        } else {
            motor.replay(uid, desde = faltam.minOf { it.second.dataInicio }, ate = agora)
            progresso.lerProgresso(uid, temporada.id)?.historico ?: lido.historico
        }
    } catch (cancelamento: CancellationException) {
        throw cancelamento
    } catch (_: Exception) {
        lido.historico
    }
}
