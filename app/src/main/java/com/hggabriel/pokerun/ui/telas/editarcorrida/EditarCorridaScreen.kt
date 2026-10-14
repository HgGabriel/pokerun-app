package com.hggabriel.pokerun.ui.telas.editarcorrida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.regras.OpcaoDeSessao
import com.hggabriel.pokerun.dominio.regras.ResumoDaCorrecao
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.telas.corridamanual.CalendarioDoTreino
import com.hggabriel.pokerun.ui.telas.corridamanual.CampoData
import com.hggabriel.pokerun.ui.telas.corridamanual.CampoHora
import com.hggabriel.pokerun.ui.telas.corridamanual.CamposDeDuracao
import com.hggabriel.pokerun.ui.telas.corridamanual.CamposDeMarca
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualUiState
import com.hggabriel.pokerun.ui.telas.corridamanual.RelogioDoTreino
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val Goteira = 16.dp
private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreCampos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val FolgaDoFim = 24.dp

/**
 * A `RunEditScreen` (`F2-T10`, docs/03 §3.14), o centro de agência do usuário: corrigir,
 * descartar e escolher a sessão que a corrida cumpre. Sem janela de tempo (RN-32).
 *
 * Fica na pilha modal (docs/03 §1), e por isso leva `safeDrawingPadding` própria. Os
 * campos são os do registro manual, com a mesma validação.
 */
@Composable
fun EditarCorridaScreen(
    corridaId: String,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
    vm: EditarCorridaViewModel = editarCorridaViewModel(corridaId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    EditarCorridaScreen(
        estado = estado,
        aoMudarFormulario = vm::mudarFormulario,
        aoEscolherSessao = vm::escolherSessao,
        aoSalvar = vm::salvar,
        aoPedirDescarte = vm::pedirDescarte,
        aoConfirmarDescarte = vm::confirmarDescarte,
        aoCancelarDescarte = vm::cancelarDescarte,
        aoTentarDeNovo = vm::tentarDeNovo,
        aoSair = aoSair,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun EditarCorridaScreen(
    estado: EditarCorridaUiState,
    aoMudarFormulario: ((CorridaManualUiState) -> CorridaManualUiState) -> Unit,
    aoEscolherSessao: (SessaoReivindicada) -> Unit,
    aoSalvar: () -> Unit,
    aoPedirDescarte: () -> Unit,
    aoConfirmarDescarte: () -> Unit,
    aoCancelarDescarte: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.editar_sobrancelha)),
                titulo = when (estado) {
                    is EditarCorridaUiState.Editando -> textoDoDia(estado.dia)
                    is EditarCorridaUiState.Concluido -> stringResource(
                        if (estado.descartada) R.string.editar_titulo_descartada else R.string.editar_titulo_corrigida,
                    )
                    else -> ""
                },
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(
                modifier = Modifier.padding(horizontal = Goteira),
                verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos),
            ) {
                when (estado) {
                    EditarCorridaUiState.Carregando -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    EditarCorridaUiState.Recalculando -> Recalculando()
                    EditarCorridaUiState.Falhou -> Falhou(aoTentarDeNovo)
                    is EditarCorridaUiState.Concluido -> Concluido(estado, aoSair)
                    is EditarCorridaUiState.Editando -> Editando(
                        estado = estado,
                        aoMudarFormulario = aoMudarFormulario,
                        aoEscolherSessao = aoEscolherSessao,
                        aoSalvar = aoSalvar,
                        aoPedirDescarte = aoPedirDescarte,
                    )
                }
                Spacer(Modifier.height(FolgaDoFim))
            }
        }
    }

    val editando = estado as? EditarCorridaUiState.Editando ?: return
    val f = editando.formulario
    if (f.escolhendoData) {
        CalendarioDoTreino(
            escolhida = f.data,
            aoFechar = { aoMudarFormulario { it.copy(escolhendoData = false) } },
            aoEscolher = { data: LocalDate ->
                aoMudarFormulario { it.copy(data = data, escolhendoData = false, erros = it.erros.copy(dataHora = null)) }
            },
        )
    }
    if (f.escolhendoHora) {
        RelogioDoTreino(
            escolhida = f.hora,
            aoFechar = { aoMudarFormulario { it.copy(escolhendoHora = false) } },
            aoEscolher = { hora: LocalTime ->
                aoMudarFormulario { it.copy(hora = hora, escolhendoHora = false, erros = it.erros.copy(dataHora = null)) }
            },
        )
    }
    if (editando.confirmandoDescarte) {
        AlertDialog(
            onDismissRequest = aoCancelarDescarte,
            title = { Text(stringResource(R.string.editar_descartar_titulo)) },
            text = { Text(stringResource(R.string.editar_descartar_texto)) },
            confirmButton = { TextButton(onClick = aoConfirmarDescarte) { Text(stringResource(R.string.editar_descartar_confirmar)) } },
            dismissButton = { TextButton(onClick = aoCancelarDescarte) { Text(stringResource(R.string.editar_descartar_voltar)) } },
        )
    }
}

@Composable
private fun Editando(
    estado: EditarCorridaUiState.Editando,
    aoMudarFormulario: ((CorridaManualUiState) -> CorridaManualUiState) -> Unit,
    aoEscolherSessao: (SessaoReivindicada) -> Unit,
    aoSalvar: () -> Unit,
    aoPedirDescarte: () -> Unit,
) {
    val f = estado.formulario

    // O aviso de docs/03 §3.14, no topo: a correção não apaga nada (RN-24).
    Text(
        text = stringResource(R.string.editar_aviso),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (estado.erroDeTela != null) {
        BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erroDeTela))
    }

    CampoData(f) { aoMudarFormulario { it.copy(escolhendoData = true) } }
    CampoHora(f) { aoMudarFormulario { it.copy(escolhendoHora = true) } }
    CampoComErro(
        valor = f.km,
        aoMudar = { texto -> aoMudarFormulario { it.copy(km = texto, erros = it.erros.copy(km = null)) } },
        rotulo = R.string.manual_campo_distancia,
        erro = f.erros.km,
        sufixo = { Text(stringResource(R.string.manual_km)) },
        opcoesDeTeclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    )
    CamposDeDuracao(
        estado = f,
        aoMudarHoras = { t -> aoMudarFormulario { it.copy(horas = t, erros = it.erros.copy(duracao = null)) } },
        aoMudarMinutos = { t -> aoMudarFormulario { it.copy(minutos = t, erros = it.erros.copy(duracao = null)) } },
        aoMudarSegundos = { t -> aoMudarFormulario { it.copy(segundos = t, erros = it.erros.copy(duracao = null)) } },
    )
    CampoComErro(
        valor = f.fcMedia,
        aoMudar = { t -> aoMudarFormulario { it.copy(fcMedia = t, erros = it.erros.copy(fcMedia = null)) } },
        rotulo = R.string.manual_campo_frequencia,
        erro = f.erros.fcMedia,
        apoio = R.string.manual_campo_frequencia_apoio,
        sufixo = { Text(stringResource(R.string.manual_bpm)) },
        opcoesDeTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
    CampoComErro(
        valor = f.esforco,
        aoMudar = { t -> aoMudarFormulario { it.copy(esforco = t, erros = it.erros.copy(esforco = null)) } },
        rotulo = R.string.manual_campo_esforco,
        erro = f.erros.esforco,
        apoio = R.string.manual_campo_esforco_apoio,
        opcoesDeTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
    CamposDeMarca(
        rotulo = R.string.manual_campo_tempo_1k,
        minutos = f.tempo1kMinutos,
        segundos = f.tempo1kSegundos,
        erro = f.erros.tempo1k,
        aoMudarMinutos = { t -> aoMudarFormulario { it.copy(tempo1kMinutos = t, erros = it.erros.copy(tempo1k = null)) } },
        aoMudarSegundos = { t -> aoMudarFormulario { it.copy(tempo1kSegundos = t, erros = it.erros.copy(tempo1k = null)) } },
    )
    CamposDeMarca(
        rotulo = R.string.manual_campo_tempo_5k,
        minutos = f.tempo5kMinutos,
        segundos = f.tempo5kSegundos,
        erro = f.erros.tempo5k,
        aoMudarMinutos = { t -> aoMudarFormulario { it.copy(tempo5kMinutos = t, erros = it.erros.copy(tempo5k = null)) } },
        aoMudarSegundos = { t -> aoMudarFormulario { it.copy(tempo5kSegundos = t, erros = it.erros.copy(tempo5k = null)) } },
    )

    if (estado.opcoes.isNotEmpty()) {
        SeletorDeSessao(estado, aoEscolherSessao)
    }

    Button(
        onClick = aoSalvar,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.editar_salvar))
    }
    // RN-31: o caminho para duplicata, tipo errado e corrida fantasma.
    OutlinedButton(
        onClick = aoPedirDescarte,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.editar_descartar))
    }
}

/**
 * O seletor de RN-34 (docs/03 §3.14): cada opção com o alvo e o multiplicador. O slot
 * escolhido pelo usuário para outra corrida aparece desabilitado.
 */
@Composable
private fun SeletorDeSessao(estado: EditarCorridaUiState.Editando, aoEscolher: (SessaoReivindicada) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        Text(
            text = stringResource(R.string.editar_sessao_rotulo).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        estado.opcoes.forEach { opcao ->
            LinhaDaOpcao(
                opcao = opcao,
                texto = textoDaOpcao(opcao, estado.numeroDaSemana),
                escolhida = opcao.sessao == estado.sessaoEscolhida,
                aoEscolher = { aoEscolher(opcao.sessao) },
            )
        }
    }
}

@Composable
private fun LinhaDaOpcao(opcao: OpcaoDeSessao, texto: String, escolhida: Boolean, aoEscolher: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao)
            .selectable(selected = escolhida, enabled = opcao.habilitada, role = Role.RadioButton, onClick = aoEscolher),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = escolhida, onClick = null, enabled = opcao.habilitada)
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (opcao.habilitada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = EspacoEntreLinhas),
        )
        Text(
            text = stringResource(R.string.editar_sessao_multiplicador, String.format(LocaleDoApp, "%.1f", opcao.multiplicador)),
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun textoDaOpcao(opcao: OpcaoDeSessao, semana: Int?): String {
    val alvo = opcao.alvoKm?.let { formatarKm(it, LocaleDoApp) }.orEmpty()
    return when (val sessao = opcao.sessao) {
        SessaoReivindicada.Avulsa -> stringResource(R.string.editar_sessao_avulsa)
        SessaoReivindicada.Longao -> stringResource(R.string.editar_sessao_longao, semana ?: 0, alvo)
        is SessaoReivindicada.Curta -> stringResource(R.string.editar_sessao_curta, sessao.indice, alvo)
    }
}

@Composable
private fun Recalculando() {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(
        text = stringResource(R.string.editar_recalculando),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/**
 * O resumo de docs/01 §5.3: o que mudou, e a conta recalculada a partir dessa data.
 * Na Fase 2 não há a linha `Coleção` (D-19).
 */
@Composable
private fun Concluido(estado: EditarCorridaUiState.Concluido, aoSair: () -> Unit) {
    val locale = LocaleDoApp
    val resumo = estado.resumo

    estado.mudancas.forEach { mudanca ->
        LinhaDoResumo(stringResource(mudanca.rotulo), stringResource(R.string.editar_resumo_seta, mudanca.antes, mudanca.depois))
    }

    Text(
        text = stringResource(R.string.editar_resumo_recalculado).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val diferenca = resumo.xpDepois - resumo.xpAntes
    LinhaDoResumo(
        stringResource(R.string.editar_resumo_xp),
        stringResource(
            R.string.editar_resumo_xp_valor,
            String.format(locale, "%,d", resumo.xpAntes),
            String.format(locale, "%,d", resumo.xpDepois),
            (if (diferenca >= 0) "+" else "−") + String.format(locale, "%,d", kotlin.math.abs(diferenca)),
        ),
    )
    LinhaDoResumo(
        stringResource(R.string.editar_resumo_posicao),
        stringResource(R.string.editar_resumo_posicao_valor, resumo.posicaoAntes, resumo.posicaoDepois),
    )
    if (resumo.recordesQueMudaram.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.editar_resumo_recordes,
                resumo.recordesQueMudaram.map { stringResource(nomeDoRecorde(it)) }.joinToString(", "),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    Button(
        onClick = aoSair,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.editar_voltar))
    }
}

@Composable
private fun LinhaDoResumo(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = valor, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Falhou(aoTentarDeNovo: () -> Unit) {
    Text(
        text = stringResource(R.string.editar_erro_corpo),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Button(
        onClick = aoTentarDeNovo,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.home_erro_repetir))
    }
}

private fun nomeDoRecorde(tipo: TipoDeRecorde): Int = when (tipo) {
    TipoDeRecorde.MELHOR_PACE -> R.string.recorde_melhor_pace
    TipoDeRecorde.MELHOR_1K -> R.string.recorde_melhor_1k
    TipoDeRecorde.MELHOR_5K -> R.string.recorde_melhor_5k
    TipoDeRecorde.MAIOR_DISTANCIA -> R.string.recorde_maior_distancia
    TipoDeRecorde.MAIOR_DURACAO -> R.string.recorde_maior_duracao
}

@Composable
private fun textoDoDia(dia: LocalDate): String =
    stringResource(R.string.corrida_detalhe_dia, dia.dayOfMonth, nomeDoMes(dia, LocaleDoApp), dia.year)

@Composable
private fun editarCorridaViewModel(corridaId: String): EditarCorridaViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "editar-$corridaId") {
        EditarCorridaViewModel(
            corridaId,
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.recalculoDeXp,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun EditarPreview(estado: EditarCorridaUiState) {
    PokerunTheme {
        EditarCorridaScreen(
            estado = estado,
            aoMudarFormulario = {},
            aoEscolherSessao = {},
            aoSalvar = {},
            aoPedirDescarte = {},
            aoConfirmarDescarte = {},
            aoCancelarDescarte = {},
            aoTentarDeNovo = {},
            aoSair = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditandoPreview() = EditarPreview(
    EditarCorridaUiState.Editando(
        formulario = CorridaManualUiState(data = LocalDate.of(2026, 9, 12), hora = LocalTime.of(6, 30), km = "8,4", minutos = "45"),
        dia = LocalDate.of(2026, 9, 12),
        opcoes = listOf(
            OpcaoDeSessao(SessaoReivindicada.Avulsa, null, 1.0, habilitada = true, escolhida = false),
            OpcaoDeSessao(SessaoReivindicada.Longao, 12.4, 2.0, habilitada = true, escolhida = true),
            OpcaoDeSessao(SessaoReivindicada.Curta(1), 6.2, 2.0, habilitada = false, escolhida = false),
        ),
        numeroDaSemana = 12,
        sessaoEscolhida = SessaoReivindicada.Longao,
    ),
)

@Preview(showBackground = true)
@Composable
private fun ConcluidoPreview() = EditarPreview(
    EditarCorridaUiState.Concluido(
        descartada = false,
        dia = LocalDate.of(2026, 9, 12),
        mudancas = listOf(Mudanca(R.string.editar_resumo_distancia, "8,4", "5,2")),
        resumo = ResumoDaCorrecao(74_200, 71_100, 118, 116, setOf(TipoDeRecorde.MELHOR_5K)),
    ),
)
