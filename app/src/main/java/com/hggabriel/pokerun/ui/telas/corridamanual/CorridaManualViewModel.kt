package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TEMPORADA_CORRENTE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/** O tipo de exercício de uma corrida digitada à mão. É o mesmo token do Health Connect. */
private const val CORRIDA_A_PE = "RUNNING"

/**
 * O motor do registro manual (`F1-T16`, docs/03 §3.10).
 *
 * ### O plano ativo é lido uma vez, e não observado
 *
 * A tela é um formulário que vive alguns segundos. O que ela precisa do plano são duas
 * coisas que não mudam nesse intervalo — o fuso (RN-28) e a grade, de onde
 * `semana_ref` sai (RN-02) —, e um listener aberto durante o preenchimento só
 * traria o custo de reassinar a cada rotação. A Home embaixo continua observando.
 *
 * ### A navegação não espera a escrita, e isso é a decisão nº 4
 *
 * `CorridaRepositorio.registrar` resolve na **confirmação do servidor**, então
 * `await` offline fica pendurado até a rede voltar. Amarrar o fechamento da tela ao
 * retorno dele travaria o registro exatamente de quem correu longe de sinal — e o
 * documento já está no cache local desde o primeiro instante, então a Home embaixo já
 * mostra a corrida. O estado terminal é ligado **antes** do `await`; o `catch` que vem
 * depois só existe para o caso de o servidor recusar, e aí a mensagem é a única coisa
 * que a tela ainda pode fazer.
 *
 * ### RN-04 não mora aqui
 *
 * Quem decide se o registro é retroativo demais é [validarCorrida], que recebe o
 * relógio e o fuso por parâmetro e tem teste. Aqui só fica o que a resposta dela
 * dispara: abrir o diálogo, ou gravar.
 */
class CorridaManualViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val relogio: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _estado = MutableStateFlow(CorridaManualUiState())
    val estado: StateFlow<CorridaManualUiState> = _estado.asStateFlow()

    /**
     * O plano ativo com a grade dele, ou nulo enquanto a leitura não voltou.
     *
     * Guardado porque as duas metades da gravação precisam dele: a validação usa o
     * fuso e `registrar` exige o plano e a grade na assinatura — é assim que
     * `F1-T05` garante que ninguém grave `semana_ref` sem chamar `CalendarioDoPlano`.
     */
    private var planoAtivo: Pair<Plano, List<Semana>>? = null

    init {
        carregarPlanoAtivo()
    }

    // -----------------------------------------------------------------------
    // Os campos
    // -----------------------------------------------------------------------

    /** Digitar limpa o erro daquele campo: o usuário está consertando. */
    fun kmMudou(texto: String) = _estado.update {
        it.copy(km = texto, erros = it.erros.copy(km = null))
    }

    fun horasMudaram(texto: String) = _estado.update {
        it.copy(horas = texto, erros = it.erros.copy(duracao = null))
    }

    fun minutosMudaram(texto: String) = _estado.update {
        it.copy(minutos = texto, erros = it.erros.copy(duracao = null))
    }

    fun segundosMudaram(texto: String) = _estado.update {
        it.copy(segundos = texto, erros = it.erros.copy(duracao = null))
    }

    fun frequenciaMudou(texto: String) = _estado.update {
        it.copy(fcMedia = texto, erros = it.erros.copy(fcMedia = null))
    }

    fun esforcoMudou(texto: String) = _estado.update {
        it.copy(esforco = texto, erros = it.erros.copy(esforco = null))
    }

    fun abrirCalendario() = _estado.update { it.copy(escolhendoData = true) }

    fun fecharCalendario() = _estado.update { it.copy(escolhendoData = false) }

    fun dataEscolhida(data: LocalDate) = _estado.update {
        it.copy(data = data, escolhendoData = false, erros = it.erros.copy(dataHora = null))
    }

    fun abrirRelogio() = _estado.update { it.copy(escolhendoHora = true) }

    fun fecharRelogio() = _estado.update { it.copy(escolhendoHora = false) }

    fun horaEscolhida(hora: LocalTime) = _estado.update {
        it.copy(hora = hora, escolhendoHora = false, erros = it.erros.copy(dataHora = null))
    }

    // -----------------------------------------------------------------------
    // Registrar
    // -----------------------------------------------------------------------

    /**
     * Valida os campos e, passando, grava — ou abre o diálogo de RN-04 antes.
     *
     * A validação é no toque e não na digitação, como em `F1-T10`: acusar
     * *"escreva a distância"* na primeira letra apagada é ruído.
     */
    fun registrar() {
        val plano = planoAtivo
        if (plano == null) {
            _estado.update { it.copy(erroDeTela = R.string.manual_erro_sem_plano) }
            return
        }

        val atual = _estado.value
        val resultado = validarCorrida(
            data = atual.data,
            hora = atual.hora,
            km = atual.km,
            horas = atual.horas,
            minutos = atual.minutos,
            segundos = atual.segundos,
            fcMedia = atual.fcMedia,
            esforco = atual.esforco,
            fuso = plano.first.fuso,
            agora = relogio.instant(),
        )

        when (resultado) {
            is ValidacaoDaCorrida.Falhou ->
                _estado.update { it.copy(erros = resultado.erros, erroDeTela = null) }

            is ValidacaoDaCorrida.Ok -> if (resultado.exigeConfirmacao) {
                // RN-04. A corrida validada fica guardada: revalidar no "sim" faria a
                // mesma entrada ser conferida contra outro relógio.
                _estado.update {
                    it.copy(erros = ErrosDaCorrida(), erroDeTela = null, confirmando = resultado)
                }
            } else {
                _estado.update { it.copy(erros = ErrosDaCorrida(), erroDeTela = null) }
                gravar(resultado)
            }
        }
    }

    /** // RN-04 — o "sim" do diálogo grava a corrida que já estava validada. */
    fun confirmarRetroativo() {
        val guardada = _estado.value.confirmando ?: return

        _estado.update { it.copy(confirmando = null) }
        gravar(guardada)
    }

    fun cancelarRetroativo() = _estado.update { it.copy(confirmando = null) }

    /** Consome o destino depois de sair, para o voltar não fechar a tela de novo. */
    fun saidaConsumida() = _estado.update { it.copy(registrada = false) }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    private fun gravar(corrida: ValidacaoDaCorrida.Ok) {
        val uid = autenticacao.uidAtual ?: return
        val (plano, grade) = planoAtivo ?: return

        // Ligado **antes** da escrita. Ver a decisão nº 4 no KDoc da classe.
        _estado.update { it.copy(registrada = true) }

        viewModelScope.launch {
            try {
                corridas.registrar(uid, corrida.paraDocumento(plano.id), plano, grade)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // A tela já saiu. A mensagem existe para quem ainda estiver nela — o
                // servidor pode recusar antes de a navegação acontecer — e é a única
                // coisa que ainda dá para fazer aqui.
                _estado.update { it.copy(registrada = false, erroDeTela = R.string.manual_erro_salvar) }
            }
        }
    }

    private fun carregarPlanoAtivo() {
        val uid = autenticacao.uidAtual ?: return

        viewModelScope.launch {
            try {
                val planoId = usuarios.observar(uid).first()?.planoAtivoId
                if (planoId == null) {
                    _estado.update { it.copy(erroDeTela = R.string.manual_erro_sem_plano) }
                    return@launch
                }

                val (plano, grade) = combine(
                    planos.observar(planoId),
                    planos.observarSemanas(planoId),
                ) { plano, grade -> plano to grade }.first()

                if (plano == null) {
                    _estado.update { it.copy(erroDeTela = R.string.manual_erro_sem_plano) }
                    return@launch
                }

                planoAtivo = plano to grade
                _estado.update { it.copy(erroDeTela = null) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.update { it.copy(erroDeTela = R.string.manual_erro_salvar) }
            }
        }
    }
}

/**
 * O documento que vai para `users/{uid}/runs`.
 *
 * **`semana_ref` nasce nulo de propósito**: quem o calcula é
 * `CorridaRepositorio.registrar`, no fuso do plano (RN-02, RN-28), e o KDoc dele diz
 * que o que vier daqui é ignorado. Preencher aqui seria a segunda conta da mesma
 * resposta.
 *
 * `splits_km` fica vazio — corrida manual não tem, e docs/05 §4.1 diz que toda tela
 * tolera a ausência. `xp_creditado` nasce zerado porque quem escreve XP é o replay de
 * `F2-T07` (XP-10), e `sessao_reivindicada` é atribuída pelo mesmo motivo (RN-34).
 */
private fun ValidacaoDaCorrida.Ok.paraDocumento(planoId: String) = Corrida(
    id = "",
    dataHoraInicio = dataHoraInicio,
    km = km,
    duracaoSeg = duracaoSeg,
    tipoExercicio = CORRIDA_A_PE,
    origem = OrigemDaCorrida.MANUAL,
    planoId = planoId,
    semanaRef = null,
    temporadaId = TEMPORADA_CORRENTE,
    fcMedia = fcMedia,
    esforcoPercebido = esforcoPercebido,
)
