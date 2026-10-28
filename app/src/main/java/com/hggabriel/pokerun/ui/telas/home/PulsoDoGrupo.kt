package com.hggabriel.pokerun.ui.telas.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.ProgressoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.rede.EspeciesRepositorio
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.Replay
import com.hggabriel.pokerun.dominio.regras.Parceiro
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * O pulso do grupo na Home (docs/03 §3.3): quem do plano já correu nesta semana.
 *
 * **Não é ranking.** Não tem XP e a ordem não diz desempenho: é a ordem de entrada no
 * plano, fixa (docs/02 §1, princípio 2). O ranking mora na aba `Grupo`.
 */
data class PulsoDoGrupo(val membros: List<MembroDoPulso>) {
    val correram: Int get() = membros.count { it.correu }

    /** Plano de uma pessoa só: a Home mostra o avatar e `Só você neste plano`. */
    val solo: Boolean get() = membros.size <= 1

    /**
     * O mesmo pulso com a sua situação vinda da Home, que escuta as suas corridas ao
     * vivo. O pulso lê os `weekly` uma vez por abertura, e sem isto a sua corrida recém
     * registrada não acenderia o seu próprio anel até a próxima abertura.
     */
    fun comVoce(correu: Boolean): PulsoDoGrupo =
        copy(membros = membros.map { if (it.voce) it.copy(correu = correu) else it })
}

data class MembroDoPulso(
    val uid: String,
    val nome: String,
    val voce: Boolean,
    val correu: Boolean,
    /** O sprite do parceiro: o Pokémon grande do banner. */
    val sprite: String?,
    /** O treinador: a pessoa na pilha de quem correu (decisão 23). */
    val treinador: String? = null,
)

/**
 * O pulso a partir dos membros e das sessões que cada um cumpriu na semana.
 *
 * **Correu é ter ao menos uma sessão cumprida** (`weekly.sessoes_feitas ≥ 1`), a mesma
 * regra dos carimbos: corrida avulsa ou excedente não acende ninguém. `weekly` ausente é
 * quem não correu na semana.
 *
 * Quem saiu do plano (`ativo = false`) não entra. A ordem é `entrou_em`, e o uid desempata
 * para dois membros gravados no mesmo instante, que é o caso do dono e do plano criados
 * juntos num teste.
 */
internal fun pulsoDoGrupo(
    membros: List<Membro>,
    sessoesFeitas: Map<String, Int?>,
    uid: String,
    sprite: (Membro) -> String?,
): PulsoDoGrupo = PulsoDoGrupo(
    membros
        .filter { it.ativo }
        .sortedWith(compareBy<Membro>({ it.entrouEm }, { it.uid }))
        .map { membro ->
            MembroDoPulso(
                uid = membro.uid,
                nome = membro.nome,
                voce = membro.uid == uid,
                correu = (sessoesFeitas[membro.uid] ?: 0) >= 1,
                sprite = sprite(membro),
                treinador = membro.treinador,
            )
        },
)

/**
 * Lê o pulso **uma vez por plano**, e de novo só quando o plano ativo muda (RN-13). A
 * ficha do ranking proíbe listener contínuo sobre `weekly` (docs/03 §3.16), e a Home
 * segue a mesma regra: ~8 leituras por abertura, como a aba `Grupo`.
 *
 * Fora do intervalo do plano não há semana corrente e não há pulso. Falha de leitura
 * também deixa o pulso nulo: a linha some, sem erro na tela, porque ela é contexto e não
 * o dado principal da Home.
 */
class PulsoDoGrupoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val planos: PlanoRepositorio,
    private val progresso: ProgressoRepositorio,
    private val temporadas: TemporadaRepositorio,
    private val especies: EspeciesRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _pulso = MutableStateFlow<PulsoDoGrupo?>(null)
    val pulso: StateFlow<PulsoDoGrupo?> = _pulso.asStateFlow()

    private val _lendo = MutableStateFlow(false)

    /** A leitura está no ar: a Home guarda o lugar do banner com o esqueleto. */
    val lendo: StateFlow<Boolean> = _lendo.asStateFlow()

    private var planoLido: String? = null

    /** Lê o pulso do [planoId], se ainda não leu. A Home chama a cada plano que mostra. */
    fun carregar(planoId: String) {
        if (planoId == planoLido) return
        planoLido = planoId
        _pulso.value = null
        _lendo.value = true
        viewModelScope.launch {
            try {
                _pulso.value = ler(planoId)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                // A linha some; a próxima abertura tenta de novo.
                planoLido = null
            } finally {
                _lendo.value = false
            }
        }
    }

    private suspend fun ler(planoId: String): PulsoDoGrupo? {
        val uid = autenticacao.uidAtual ?: return null
        val plano = planos.buscar(planoId) ?: return null
        val grade = planos.buscarSemanas(planoId)
        val agora = relogio.instant()
        val semana = CalendarioDoPlano.semanaDe(agora, plano, grade) ?: return null
        val membros = planos.buscarMembros(planoId).filter { it.ativo }
        val chave = Replay.chaveDaSemana(planoId, semana.numero)

        val feitas = coroutineScope {
            membros.map { membro -> async { membro.uid to progresso.semanal(membro.uid, chave)?.sessoesFeitas } }
                .awaitAll()
                .toMap()
        }
        val temporada = temporadaDe(agora, temporadas.todas())
        return pulsoDoGrupo(membros, feitas, uid) { membro ->
            temporada?.let { especies.spriteDeJogo(Parceiro.doMembro(membro.parceiro, membro.avatarEspecie, it)) }
        }
    }
}
