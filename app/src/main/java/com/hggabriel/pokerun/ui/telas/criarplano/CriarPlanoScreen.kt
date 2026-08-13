package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val RecheioDaFicha = 16.dp

/**
 * O formulário de criação de plano (`F1-T10`, docs/03 §3.5).
 *
 * **Quatro campos, e nenhum a mais** (D-02, D-14): nome, data da prova, distância-alvo,
 * distância confortável e sessões por semana — os quatro parâmetros viram cinco valores
 * porque o primeiro pede duas coisas. **Não existe campo de semana leve nem de volume**:
 * o volume deriva do longão pela fórmula de docs/01 §3.2, e um segundo campo faria os
 * dois divergirem sem ninguém saber qual manda.
 *
 * **A distância confortável chega pré-preenchida** com o `baseline_km` do cadastro. A
 * pergunta já foi feita uma vez, e repeti-la em branco é o jeito mais rápido de alguém
 * responder diferente das duas vezes.
 *
 * **A tela não grava nada.** `Gerar plano` valida e sai para a revisão do rascunho
 * (`F1-T11`), que é quem escreve depois de o usuário conferir a grade. É por isso que
 * não há estado de carregamento aqui.
 *
 * **Sem rede, o botão fica indisponível e a tela diz por quê.** A reserva do código de
 * convite é transacional (RN-29) e não resolve no cache; somada à decisão de `F1-T05` de
 * resolver a escrita na confirmação do servidor, uma criação offline fica pendurada em
 * vez de falhar. Bloquear aqui é mais barato que preencher quatro campos à toa.
 */
@Composable
fun CriarPlanoScreen(
    aoGerar: (RevisarRascunho) -> Unit,
    modifier: Modifier = Modifier,
    vm: CriarPlanoViewModel = criarPlanoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.rascunho) {
        val rascunho = estado.rascunho ?: return@LaunchedEffect
        vm.rascunhoConsumido()
        aoGerar(rascunho)
    }

    CriarPlanoScreen(
        estado = estado,
        aoMudarNome = vm::nomeMudou,
        aoMudarAlvo = vm::alvoMudou,
        aoMudarBaseline = vm::baselineMudou,
        aoMudarSessoes = vm::sessoesMudaram,
        aoAbrirCalendario = vm::abrirCalendario,
        aoFecharCalendario = vm::fecharCalendario,
        aoEscolherData = vm::dataEscolhida,
        aoGerarPlano = vm::gerarPlano,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarPlanoScreen(
    estado: CriarPlanoUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarAlvo: (String) -> Unit,
    aoMudarBaseline: (String) -> Unit,
    aoMudarSessoes: (Int) -> Unit,
    aoAbrirCalendario: () -> Unit,
    aoFecharCalendario: () -> Unit,
    aoEscolherData: (LocalDate) -> Unit,
    aoGerarPlano: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.criar_sobrancelha)),
                titulo = stringResource(R.string.criar_titulo),
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Ficha {
                    Column(modifier = Modifier.padding(RecheioDaFicha)) {
                        CampoNome(estado, aoMudarNome)
                        Spacer(Modifier.height(EspacoEntreBlocos))
                        CampoData(estado, aoAbrirCalendario)
                        Spacer(Modifier.height(EspacoEntreBlocos))
                        CampoDistancia(
                            valor = estado.alvo,
                            rotulo = R.string.criar_campo_alvo,
                            erro = estado.erros.alvo,
                            imeAction = ImeAction.Next,
                            aoMudar = aoMudarAlvo,
                        )
                        Spacer(Modifier.height(EspacoEntreBlocos))
                        CampoDistancia(
                            valor = estado.baseline,
                            rotulo = R.string.criar_campo_baseline,
                            apoio = R.string.criar_campo_baseline_apoio,
                            erro = estado.erros.baseline,
                            imeAction = ImeAction.Done,
                            aoMudar = aoMudarBaseline,
                        )
                        Spacer(Modifier.height(EspacoEntreBlocos))
                        CampoSessoes(estado.sessoesPorSemana, aoMudarSessoes)
                    }
                }

                if (estado.semanas != null) {
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    // O tamanho do plano aparece antes de gerar. É o dado que muda com a
                    // data e o único que o usuário não tem como estimar de cabeça.
                    Text(
                        text = pluralStringResource(R.plurals.criar_resumo_semanas, estado.semanas, estado.semanas),
                        style = EstiloDado,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!estado.online) {
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    Text(
                        text = stringResource(R.string.criar_offline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))

                Button(
                    onClick = aoGerarPlano,
                    enabled = estado.online,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDoBotao),
                ) {
                    Text(stringResource(R.string.criar_gerar))
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }
        }
    }

    if (estado.escolhendoData) {
        CalendarioDaProva(
            escolhida = estado.dataProva,
            aoFechar = aoFecharCalendario,
            aoEscolher = aoEscolherData,
        )
    }
}

// ---------------------------------------------------------------------------
// Os campos
// ---------------------------------------------------------------------------

@Composable
private fun CampoNome(estado: CriarPlanoUiState, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = estado.nome,
        onValueChange = aoMudar,
        label = { Text(stringResource(R.string.criar_campo_nome)) },
        singleLine = true,
        isError = estado.erros.nome != null,
        supportingText = {
            Text(stringResource(estado.erros.nome ?: R.string.criar_campo_nome_apoio))
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * A data é um **campo**, não um link.
 *
 * A primeira versão era um rótulo com um `TextButton` embaixo, e no emulador ela apareceu
 * como um texto âmbar centralizado entre dois campos com moldura — o único elemento da
 * ficha sem caixa, e o único centralizado. Lia como coisa quebrada, não como campo a
 * preencher.
 *
 * O que ficou é o mesmo `OutlinedTextField` dos outros três, em `readOnly`, com uma
 * camada de toque por cima: a moldura, o rótulo flutuante, o texto de erro e a altura são
 * os mesmos, e o teclado nunca abre. **Digitar data à mão continua fora**, e é o ponto do
 * componente: `03/04` é março ou abril conforme quem digita, e o `DatePicker` já resolve
 * teclado, fuso e TalkBack.
 */
@Composable
private fun CampoData(estado: CriarPlanoUiState, aoAbrir: () -> Unit) {
    val escolhida = estado.dataProva
    val texto = escolhida?.let {
        stringResource(R.string.criar_data_escolhida, it.dayOfMonth, nomeDoMes(it), it.year)
    } ?: ""

    Box {
        OutlinedTextField(
            value = texto,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.criar_campo_data)) },
            placeholder = { Text(stringResource(R.string.criar_campo_data_vazio)) },
            singleLine = true,
            isError = estado.erros.data != null,
            supportingText = estado.erros.data?.let { { Text(stringResource(it)) } },
            modifier = Modifier.fillMaxWidth(),
        )

        // A camada de toque cobre o campo inteiro e engole o foco, para o cursor não
        // piscar num campo que não aceita digitação. `matchParentSize` acompanha a
        // altura, que muda quando o texto de erro aparece.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = aoAbrir,
                ),
        )
    }
}

@Composable
private fun CampoDistancia(
    valor: String,
    rotulo: Int,
    erro: Int?,
    imeAction: ImeAction,
    aoMudar: (String) -> Unit,
    apoio: Int? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(stringResource(rotulo)) },
        suffix = { Text(stringResource(R.string.criar_km)) },
        singleLine = true,
        isError = erro != null,
        supportingText = (erro ?: apoio)?.let { { Text(stringResource(it)) } },
        // Decimal e não `Number`: a resposta é 7,5 tanto quanto 7, e o teclado sem
        // separador obrigaria a arredondar quem corre 800 m.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 2, 3 ou 4 sessões (docs/01 §3.1). É seletor e não campo livre: o gerador recusa
 * qualquer outro número com `require`, e um campo de texto convidaria o usuário a
 * descobrir isso pelo travamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoSessoes(escolhida: Int, aoMudar: (Int) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.criar_campo_sessoes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreBlocos / 2))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CriarPlanoUiState.SESSOES.forEachIndexed { indice, sessoes ->
                SegmentedButton(
                    selected = sessoes == escolhida,
                    onClick = { aoMudar(sessoes) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = indice,
                        count = CriarPlanoUiState.SESSOES.size,
                    ),
                ) {
                    Text(sessoes.toString())
                }
            }
        }
    }
}

/**
 * O calendário da prova.
 *
 * **O `DatePicker` fala em milissegundos UTC**, e converter isso com o fuso do aparelho
 * move a data em um dia para quem está a oeste de Greenwich: a meia-noite UTC do dia 31
 * é o dia 30 às 21h em São Paulo. A conversão é em `ZoneOffset.UTC` nos dois sentidos,
 * porque o que o componente devolve é uma **data de calendário** disfarçada de instante,
 * e não um momento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioDaProva(
    escolhida: LocalDate?,
    aoFechar: () -> Unit,
    aoEscolher: (LocalDate) -> Unit,
) {
    val estadoDoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = escolhida
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            TextButton(
                onClick = {
                    estadoDoCalendario.selectedDateMillis?.let { millis ->
                        aoEscolher(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
                enabled = estadoDoCalendario.selectedDateMillis != null,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DatePicker(state = estadoDoCalendario)
    }
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun criarPlanoViewModel(): CriarPlanoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        CriarPlanoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.conectividadeRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun CriarPlanoPreview(estado: CriarPlanoUiState) {
    PokerunTheme {
        CriarPlanoScreen(
            estado = estado,
            aoMudarNome = {},
            aoMudarAlvo = {},
            aoMudarBaseline = {},
            aoMudarSessoes = {},
            aoAbrirCalendario = {},
            aoFecharCalendario = {},
            aoEscolherData = {},
            aoGerarPlano = {},
        )
    }
}

private val PREENCHIDO = CriarPlanoUiState(
    nome = "São Silvestre",
    dataProva = LocalDate.of(2026, 12, 31),
    alvo = "15",
    baseline = "5",
    baselinePreenchida = true,
    semanas = 21,
)

@Preview(name = "Vazio, com a baseline do cadastro", showBackground = true)
@Composable
private fun VazioPreview() =
    CriarPlanoPreview(CriarPlanoUiState(baseline = "5", baselinePreenchida = true))

@Preview(name = "Preenchido", showBackground = true)
@Composable
private fun PreenchidoPreview() = CriarPlanoPreview(PREENCHIDO)

@Preview(name = "Com os quatro erros", showBackground = true)
@Composable
private fun ComErrosPreview() = CriarPlanoPreview(
    CriarPlanoUiState(
        alvo = "3",
        baseline = "5",
        erros = ErrosDoPlano(
            nome = R.string.criar_erro_nome,
            data = R.string.criar_erro_data_curta,
            alvo = R.string.criar_erro_alvo_menor,
        ),
    ),
)

@Preview(name = "Offline", showBackground = true)
@Composable
private fun OfflinePreview() = CriarPlanoPreview(PREENCHIDO.copy(online = false))

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = CriarPlanoPreview(PREENCHIDO)
