package com.hggabriel.pokerun.ui.telas.editarcorrida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.RecalculoDeXp
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.EdicaoDeCorrida
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.ResultadoDoReplay
import com.hggabriel.pokerun.dominio.regras.ValoresCorrigidos
import com.hggabriel.pokerun.dominio.regras.numeroNaEscada
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualUiState
import com.hggabriel.pokerun.ui.telas.corridamanual.ValidacaoDaCorrida
import com.hggabriel.pokerun.ui.telas.corridamanual.validarCorrida
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/**
 * A `RunEditScreen` (`F2-T10`, docs/03 §3.14): corrigir, descartar e escolher a sessão.
 *
 * Grava pelo `CorridaRepositorio` e recalcula pelo replay a partir do início da semana
 * da corrida (RN-33), no escopo do app (`RecalculoDeXp.comResumo`): sair da tela no meio
 * não deixa a escrita sem replay.
 */
class EditarCorridaViewModel(
    private val corridaId: String,
    private val autenticacao: AutenticacaoRepositorio,
    private val planos: PlanoRepositorio,
    private val corridas: CorridaRepositorio,
    private val recalculo: RecalculoDeXp,
    /** Para o resumo mostrar a espécie de cada posição da escada. Sem ela, o resumo fica no número. */
    private val temporadas: TemporadaRepositorio? = null,
    private val relogio: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _estado = MutableStateFlow<EditarCorridaUiState>(EditarCorridaUiState.Carregando)
    val estado: StateFlow<EditarCorridaUiState> = _estado.asStateFlow()

    private var original: Corrida? = null
    private var plano: Plano? = null
    private var temporada: Temporada? = null
    private var grade: List<Semana> = emptyList()
    private var formularioInicial: CorridaManualUiState? = null
    private var sessaoInicial: SessaoReivindicada? = null

    init {
        carregar()
    }

    fun tentarDeNovo() = carregar()

    private fun carregar() {
        _estado.value = EditarCorridaUiState.Carregando
        viewModelScope.launch {
            try {
                val uid = autenticacao.uidAtual ?: error("sem sessão")
                val corrida = corridas.observar(uid, corridaId).first() ?: error("corrida inexistente")
                val doPlano = planos.buscar(corrida.planoId) ?: error("plano inexistente")
                val semanas = planos.buscarSemanas(corrida.planoId)
                val semana = semanas.firstOrNull { it.numero == corrida.semanaRef }
                val daSemana = corridas.observarDoPlano(uid, corrida.planoId).first()
                    .filter { it.semanaRef == corrida.semanaRef }
                val opcoes = semana?.let { EdicaoDeCorrida.opcoesDeSessao(corrida, it, daSemana) }.orEmpty()
                val formulario = formularioDe(corrida, doPlano.fuso)

                original = corrida
                plano = doPlano
                // A temporada só desenha o resumo: se ela falhar, a edição abre do mesmo jeito.
                temporada = try {
                    temporadas?.todas()?.firstOrNull { it.id == corrida.temporadaId }
                } catch (cancelamento: CancellationException) {
                    throw cancelamento
                } catch (erro: Exception) {
                    null
                }
                grade = semanas
                formularioInicial = formulario
                sessaoInicial = opcoes.firstOrNull { it.escolhida }?.sessao
                _estado.value = EditarCorridaUiState.Editando(
                    formulario = formulario,
                    dia = formulario.data!!,
                    opcoes = opcoes,
                    numeroDaSemana = semana?.numero,
                    sessaoEscolhida = sessaoInicial,
                )
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.value = EditarCorridaUiState.Falhou
            }
        }
    }

    /** Toda mudança de campo passa por aqui, como no registro manual. */
    fun mudarFormulario(mudanca: (CorridaManualUiState) -> CorridaManualUiState) = editando { atual ->
        atual.copy(formulario = mudanca(atual.formulario), erroDeTela = null)
    }

    fun escolherSessao(sessao: SessaoReivindicada) = editando { it.copy(sessaoEscolhida = sessao) }

    fun pedirDescarte() = editando { it.copy(confirmandoDescarte = true) }

    fun cancelarDescarte() = editando { it.copy(confirmandoDescarte = false) }

    // -----------------------------------------------------------------------
    // Salvar
    // -----------------------------------------------------------------------

    fun salvar() {
        val atual = _estado.value as? EditarCorridaUiState.Editando ?: return
        val corrida = original ?: return
        val doPlano = plano ?: return
        val f = atual.formulario

        val validacao = validarCorrida(
            data = f.data,
            hora = f.hora,
            km = f.km,
            horas = f.horas,
            minutos = f.minutos,
            segundos = f.segundos,
            fcMedia = f.fcMedia,
            esforco = f.esforco,
            tempo1kMinutos = f.tempo1kMinutos,
            tempo1kSegundos = f.tempo1kSegundos,
            tempo5kMinutos = f.tempo5kMinutos,
            tempo5kSegundos = f.tempo5kSegundos,
            fuso = doPlano.fuso,
            agora = relogio.instant(),
        )
        // RN-04 não vale aqui: a corrida já está registrada, e a correção é livre (RN-32).
        val ok = when (validacao) {
            is ValidacaoDaCorrida.Falhou -> {
                editando { it.copy(formulario = it.formulario.copy(erros = validacao.erros)) }
                return
            }
            is ValidacaoDaCorrida.Ok -> validacao
        }

        // O formulário tem hora e minuto; a corrida, segundos. Quem não mexeu na data e na
        // hora não pode ganhar um registro novo por causa dos segundos.
        val inicial = formularioInicial
        val quando = if (inicial != null && f.data == inicial.data && f.hora == inicial.hora) {
            corrida.dataHoraInicio
        } else {
            ok.dataHoraInicio
        }
        // A atribuição automática nunca é gravada (docs/04 §3.2): manter a opção que veio
        // marcada, sem escolha anterior, é continuar sem escolha.
        val sessao = if (atual.sessaoEscolhida == sessaoInicial && corrida.sessaoReivindicada == null) {
            null
        } else {
            atual.sessaoEscolhida
        }
        val valores = ValoresCorrigidos(
            dataHoraInicio = quando,
            km = ok.km,
            duracaoSeg = ok.duracaoSeg,
            fcMedia = ok.fcMedia,
            esforcoPercebido = ok.esforcoPercebido,
            tempo1kSeg = ok.tempo1kSeg,
            tempo5kSeg = ok.tempo5kSeg,
            sessao = sessao,
        )

        val mediu = medicaoMudou(corrida, valores)
        if (!mediu && sessao == corrida.sessaoReivindicada) {
            // Nada mudou: não há o que gravar nem recalcular.
            _estado.value = concluido(corrida, valores, descartada = false, antes = null, depois = null)
            return
        }

        executar(
            desde = desdeDe(corrida, valores.dataHoraInicio),
            escrita = { uid ->
                if (mediu) {
                    val correcao = EdicaoDeCorrida.corrigir(corrida, valores)
                    corridas.corrigir(uid, corrida, correcao.nova, doPlano, grade)
                } else {
                    corridas.reivindicar(uid, corrida.id, sessao)
                }
            },
            aoConcluir = { antes, depois -> concluido(corrida, valores, descartada = false, antes = antes, depois = depois) },
        )
    }

    // -----------------------------------------------------------------------
    // Descartar
    // -----------------------------------------------------------------------

    fun confirmarDescarte() {
        val corrida = original ?: return
        editando { it.copy(confirmandoDescarte = false) }
        val valores = ValoresCorrigidos(
            corrida.dataHoraInicio, corrida.km, corrida.duracaoSeg, corrida.fcMedia,
            corrida.esforcoPercebido, corrida.tempo1kSeg, corrida.tempo5kSeg, corrida.sessaoReivindicada,
        )
        executar(
            desde = desdeDe(corrida, corrida.dataHoraInicio),
            escrita = { uid -> corridas.descartar(uid, corrida.id) },
            aoConcluir = { antes, depois -> concluido(corrida, valores, descartada = true, antes = antes, depois = depois) },
        )
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    /**
     * `Recalculando` enquanto a escrita e o replay correm no escopo do app; depois, o
     * estado que [aoConcluir] monta com o antes e o depois. Falha volta ao formulário,
     * com o erro de tela.
     */
    private fun executar(
        desde: Instant,
        escrita: suspend (String) -> Unit,
        aoConcluir: (ResultadoDoReplay, ResultadoDoReplay) -> EditarCorridaUiState,
    ) {
        val uid = autenticacao.uidAtual ?: return
        val editando = _estado.value as? EditarCorridaUiState.Editando
        _estado.value = EditarCorridaUiState.Recalculando
        viewModelScope.launch {
            try {
                val (antes, depois) = recalculo.comResumo(uid, desde) { escrita(uid) }.await()
                _estado.value = aoConcluir(antes, depois)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.value = editando?.copy(erroDeTela = R.string.editar_erro_salvar) ?: EditarCorridaUiState.Falhou
            }
        }
    }

    private fun concluido(
        corrida: Corrida,
        valores: ValoresCorrigidos,
        descartada: Boolean,
        antes: ResultadoDoReplay?,
        depois: ResultadoDoReplay?,
    ): EditarCorridaUiState {
        val fuso = plano?.fuso ?: return EditarCorridaUiState.Falhou
        val temporadaId = corrida.temporadaId
        val resumo = EdicaoDeCorrida.resumo(
            antes?.porTemporada?.get(temporadaId),
            (depois ?: antes)?.porTemporada?.get(temporadaId),
            antes?.recordes ?: Recordes(),
            (depois ?: antes)?.recordes ?: Recordes(),
        )
        val lida = temporada
        return EditarCorridaUiState.Concluido(
            descartada = descartada,
            dia = corrida.dataHoraInicio.atZone(fuso).toLocalDate(),
            mudancas = if (descartada) emptyList() else mudancasDa(corrida, valores, fuso),
            resumo = resumo,
            especieAntes = lida?.let { numeroNaEscada(resumo.posicaoAntes, it) },
            especieDepois = lida?.let { numeroNaEscada(resumo.posicaoDepois, it) },
        )
    }

    /** RN-33: do início da semana da corrida, a mais cedo entre a data antiga e a nova. */
    private fun desdeDe(corrida: Corrida, nova: Instant): Instant {
        val doPlano = plano ?: return minOf(corrida.dataHoraInicio, nova)
        fun inicio(instante: Instant) = CalendarioDoPlano.semanaDe(instante, doPlano, grade)?.dataInicio ?: instante
        return minOf(inicio(corrida.dataHoraInicio), inicio(nova))
    }

    private fun editando(mudanca: (EditarCorridaUiState.Editando) -> EditarCorridaUiState.Editando) =
        _estado.update { atual -> if (atual is EditarCorridaUiState.Editando) mudanca(atual) else atual }
}
