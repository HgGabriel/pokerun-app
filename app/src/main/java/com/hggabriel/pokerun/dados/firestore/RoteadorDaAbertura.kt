package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dominio.regras.semanaAApresentar
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock

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
            val temporada = temporadaDe(relogio.instant(), temporadas.todas()) ?: return false
            val lido = progresso.lerProgresso(uid, temporada.id) ?: return false
            val usuario = usuarios.buscar(uid) ?: return false
            semanaAApresentar(lido.historico, usuario.semanaApresentada) != null
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: Exception) {
            false
        }
    }
}
