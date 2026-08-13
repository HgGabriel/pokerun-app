package com.hggabriel.pokerun.ui.telas.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant

/** Quanto tempo o estado sobrevive a uma rotação antes de os listeners caírem. */
private const val ASSINATURA_SOBREVIVE_MS = 5_000L

/**
 * O motor da Home (`F1-T09`, docs/03 §3.3).
 *
 * ### Três listeners, e o painel sai da combinação dos três
 *
 * `users/{uid}` diz qual é o plano ativo; `plans/{id}` e a subcoleção `weeks` dizem o
 * que ele é; `runs` filtradas por `plano_id` dizem o que foi corrido. Quem transforma
 * isso em estado de tela é [painelDeHoje], que é função pura e tem teste — aqui só mora
 * a costura.
 *
 * **O `flatMapLatest` é o que faz a troca de plano ativo (RN-13) chegar sozinha.**
 * Trocado o `plano_ativo_id` em outro aparelho, o listener de `users/{uid}` emite, os
 * três de baixo são descartados e assinados de novo no plano novo. Sem ele, a Home
 * continuaria mostrando o plano antigo até alguém reabrir o app.
 *
 * ### O relógio é lido a cada emissão, e não em segundo plano
 *
 * `Instant.now()` entra na combinação, então a contagem regressiva é a do momento em que
 * o dado chegou. **Não há timer**: a Home não se atualiza sozinha à meia-noite, e isso
 * está de acordo com RN-25 — o app lê e recalcula na abertura, nunca em segundo plano.
 *
 * ### Sair da conta não passa por aqui
 *
 * Sem `uidAtual` o estado é [HomeUiState.SemPerfil] e a tela devolve ao cadastro. O
 * caminho de verdade de quem sai da conta é o botão de `SettingsScreen` (`F1-T17`), que
 * é quem troca a porta de entrada; aqui isso é rede de segurança, não fluxo.
 */
class HomeViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
) : ViewModel() {

    /**
     * O contador de tentativas. Mexer nele reassina a cadeia inteira, que é o que o
     * botão de repetir de docs/02 §8, item 7 precisa fazer — o `catch` de um `Flow`
     * encerra a coleta, e sem uma nova assinatura o erro seria permanente.
     */
    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<HomeUiState> = tentativas
        .flatMapLatest { painel() }
        .catch { emit(HomeUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = HomeUiState.Carregando,
        )

    /** Reassina os listeners depois de uma falha de leitura. */
    fun tentarDeNovo() {
        tentativas.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun painel(): Flow<HomeUiState> {
        val uid = autenticacao.uidAtual ?: return flowOf(HomeUiState.SemPerfil)

        return usuarios.observar(uid).flatMapLatest { usuario ->
            val planoId = usuario?.planoAtivoId
            when {
                // Documento ausente é quem foi morto entre autenticar e terminar o
                // passo 2 do cadastro. A abertura com sessão vai direto para a casca
                // (`NavegacaoDoApp`), então é aqui que o caso é percebido.
                usuario == null -> flowOf(HomeUiState.SemPerfil)
                planoId == null -> flowOf(HomeUiState.SemPlano)
                else -> combine(
                    planos.observar(planoId),
                    planos.observarSemanas(planoId),
                    corridas.observarDoPlano(uid, planoId),
                ) { plano, grade, corridasDoPlano ->
                    painelDeHoje(plano, grade, corridasDoPlano, Instant.now())
                }
            }
        }
    }
}
