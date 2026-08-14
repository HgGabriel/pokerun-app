package com.hggabriel.pokerun.ui.telas.detalhesemana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
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
 * O motor do detalhe da semana (`F1-T15`, docs/03 §3.9).
 *
 * ### Três listeners, e nenhuma escrita
 *
 * `plans/{id}` dá o nome e o fuso, `weeks` dá a grade e `runs` filtradas pelo plano dão o
 * que a pessoa correu. A tela é somente leitura: a edição do longão é do dono e mora na
 * `PlanDetailScreen` (RN-06), e registrar corrida é a `ManualRunScreen`. Não há
 * `users/{uid}` aqui porque nada nesta tela depende de qual plano é o ativo — a semana
 * de um plano guardado se lê igual.
 *
 * ### O plano vem inteiro, e a semana é escolhida depois
 *
 * A rota carrega `planoId` e `numero`, e não a semana. Um argumento de rota sobrevive a
 * morte de processo, e serializar a semana faria a tela renascer com a grade de antes de
 * o dono editar o longão — que é exatamente o dado que esta tela mostra.
 *
 * ### O relógio é lido na combinação, e não guardado
 *
 * `Instant.now()` a cada emissão é o mesmo padrão da Home e do detalhe do plano: quem
 * decide o cadeado (RN-05) e o quadrado de hoje é [detalheDaSemana], que recebe o
 * instante por parâmetro e tem teste. Nenhuma conta de data acontece aqui.
 */
class DetalheSemanaViewModel(
    private val planoId: String,
    private val numero: Int,
    private val autenticacao: AutenticacaoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
) : ViewModel() {

    /** Reassinar a cadeia é o que o botão de repetir de docs/02 §8, item 7 precisa fazer. */
    private val tentativas = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalheSemanaUiState> = tentativas
        .flatMapLatest { remoto() }
        .catch { emit(DetalheSemanaUiState.Falhou) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ASSINATURA_SOBREVIVE_MS),
            initialValue = DetalheSemanaUiState.Carregando,
        )

    fun tentarDeNovo() = tentativas.update { it + 1 }

    private fun remoto(): Flow<DetalheSemanaUiState> {
        // Sem sessão não há semana. Esta tela vive dentro da casca, e quem devolve ao
        // cadastro é a Home embaixo dela; aqui isso é rede de segurança.
        val uid = autenticacao.uidAtual ?: return flowOf(DetalheSemanaUiState.Falhou)

        return combine(
            planos.observar(planoId),
            planos.observarSemanas(planoId),
            corridas.observarDoPlano(uid, planoId),
        ) { plano, grade, corridasDoPlano ->
            detalheDaSemana(
                plano = plano,
                grade = grade,
                corridas = corridasDoPlano,
                numero = numero,
                agora = Instant.now(),
            )
        }
    }
}
