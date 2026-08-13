package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.ConectividadeRepositorio
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * O motor do formulário de criação (`F1-T10`, docs/03 §3.5).
 *
 * ### Ele não grava nada, e isso é escopo e não esquecimento
 *
 * O plano nasce na revisão do rascunho (`F1-T11`), depois de o usuário conferir a grade e
 * poder puxar um longão. A rota `RevisarRascunho` carrega os **quatro parâmetros de
 * entrada**, e não a grade: o gerador é função pura, então os mesmos parâmetros devolvem
 * a mesma grade — e um argumento de rota sobrevive a morte de processo, o que faz a tela
 * de revisão renascer idêntica de graça (`Rotas.kt`).
 *
 * ### O fuso entra aqui, e é o do aparelho de propósito
 *
 * `plans/{id}.fuso` é o fuso **do plano**, e ele é decidido uma vez: onde o plano foi
 * criado. Depois disso RN-28 manda todo cálculo usar aquele valor e nunca mais o do
 * aparelho — é o que mantém a corrida de domingo às 22h em domingo depois de o corredor
 * viajar. Este é o único ponto do app onde `ZoneId.systemDefault()` é a resposta certa.
 *
 * ### A distância confortável chega preenchida
 *
 * Vem de `users/{uid}.baseline_km`, do cadastro (docs/03 §3.5: *"não perguntar duas
 * vezes"*). A leitura é assíncrona e o preenchimento acontece **uma vez**: sem a marca em
 * [CriarPlanoUiState.baselinePreenchida], uma segunda emissão do perfil apagaria o que o
 * usuário estivesse digitando naquele campo.
 */
class CriarPlanoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val conectividade: ConectividadeRepositorio,
    private val relogio: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _estado = MutableStateFlow(CriarPlanoUiState())
    val estado: StateFlow<CriarPlanoUiState> = _estado.asStateFlow()

    /** O fuso do plano é o de onde ele foi criado. Ver o KDoc da classe. */
    private val fuso: ZoneId get() = relogio.zone

    init {
        preencherBaseline()
        acompanharConexao()
    }

    // -----------------------------------------------------------------------
    // Os quatro campos
    // -----------------------------------------------------------------------

    /** Digitar limpa o erro daquele campo: o usuário está consertando. */
    fun nomeMudou(texto: String) = _estado.update {
        it.copy(nome = texto, erros = it.erros.copy(nome = null))
    }

    fun alvoMudou(texto: String) = _estado.update {
        it.copy(alvo = texto, erros = it.erros.copy(alvo = null))
    }

    fun baselineMudou(texto: String) = _estado.update {
        // Marcada como preenchida: a partir daqui o valor é do usuário, e uma emissão
        // atrasada do perfil não pode mais sobrescrevê-lo.
        it.copy(baseline = texto, baselinePreenchida = true, erros = it.erros.copy(baseline = null))
    }

    fun sessoesMudaram(sessoes: Int) = _estado.update { it.copy(sessoesPorSemana = sessoes) }

    fun abrirCalendario() = _estado.update { it.copy(escolhendoData = true) }

    fun fecharCalendario() = _estado.update { it.copy(escolhendoData = false) }

    fun dataEscolhida(data: LocalDate) = _estado.update {
        it.copy(
            dataProva = data,
            semanas = semanasAte(LocalDate.now(relogio), data),
            escolhendoData = false,
            erros = it.erros.copy(data = null),
        )
    }

    // -----------------------------------------------------------------------
    // Gerar
    // -----------------------------------------------------------------------

    /**
     * Valida os quatro campos e, passando, sai para a revisão do rascunho.
     *
     * A validação é no toque e não na digitação: acusar *"dê um nome ao plano"* na
     * primeira letra apagada é ruído, e o campo ainda está sendo preenchido.
     */
    fun gerarPlano() {
        val atual = _estado.value
        val resultado = validarRascunho(
            nome = atual.nome,
            dataProva = atual.dataProva,
            alvo = atual.alvo,
            baseline = atual.baseline,
            hoje = LocalDate.now(relogio),
        )

        _estado.value = when (resultado) {
            is ValidacaoDoPlano.Falhou -> atual.copy(erros = resultado.erros)
            is ValidacaoDoPlano.Ok -> atual.copy(
                erros = ErrosDoPlano(),
                rascunho = RevisarRascunho(
                    nome = resultado.nome,
                    fuso = fuso.id,
                    dataProvaEpochDia = resultado.dataProva.toEpochDay(),
                    distanciaAlvoKm = resultado.alvoKm,
                    baselineKm = resultado.baselineKm,
                    sessoesPorSemana = atual.sessoesPorSemana,
                ),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    private fun preencherBaseline() {
        val uid = autenticacao.uidAtual ?: return
        viewModelScope.launch {
            val perfil = try {
                usuarios.buscar(uid)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // Falhar ao ler o perfil não custa a criação: o campo fica vazio e o
                // usuário responde de novo. É o pior caso, não um erro de tela.
                null
            } ?: return@launch

            _estado.update {
                if (it.baselinePreenchida) {
                    it
                } else {
                    it.copy(baseline = formatarKm(perfil.baselineKm), baselinePreenchida = true)
                }
            }
        }
    }

    /**
     * A tela destrava sozinha quando a rede volta — o `Flow` continua emitindo, então
     * não há botão de "tentar de novo" para um estado que se conserta sem toque nenhum.
     */
    private fun acompanharConexao() {
        viewModelScope.launch {
            try {
                conectividade.online().collect { online ->
                    _estado.update { it.copy(online = online) }
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // A dúvida joga a favor do usuário: sem resposta do sistema, deixa
                // tentar. O pior caso é a criação falhar com a mensagem dela.
                _estado.update { it.copy(online = true) }
            }
        }
    }

    /** Consome o destino depois de navegar, para o voltar não sair de novo na hora. */
    fun rascunhoConsumido() = _estado.update { it.copy(rascunho = null) }
}
