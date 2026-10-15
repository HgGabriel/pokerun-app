package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreCampos = 16.dp
private val EspacoEntreBlocos = 24.dp
private val EspacoAntesDoBotao = 32.dp

/** A folga no fim, para o botão não colar na barra inferior. */
private val FolgaDoFim = 24.dp

/**
 * O registro manual de treino (`F1-T16`, docs/03 §3.10).
 *
 * **É o alvo do FAB da Home**, e o FAB só existe no estado `Ativo` — a corrida precisa
 * de um plano para ter semana (RN-02). O caminho sem plano ativo continua tratado, com
 * a mensagem no lugar do formulário, porque o `ViewModel` não pode depender de quem o
 * abriu ter checado isso.
 *
 * **Vive dentro da casca, sob a aba `Hoje`** (docs/03 §1), então não leva
 * `safeDrawingPadding`: o `Scaffold` da casca já entrega o espaçamento.
 *
 * ### A nota sobre splits fica em cima, e não embaixo do botão
 *
 * docs/03 §3.10 pede a nota *"sem tom de desculpa"*. Ela diz o que este caminho não
 * captura — e é informação que muda o que a pessoa espera da tela, não um pedido de
 * perdão: o registro manual é a entrada de corrida do app (docs/05 §4.4). Embaixo
 * do botão ela seria lida depois de a decisão já ter sido tomada.
 *
 * ### Os erros saem pelo canal, sem exceção
 *
 * Todo campo é [CampoComErro] e a falha de tela é [BannerDeAlerta] (`F1-T06b`,
 * docs/02 §2.4). Nenhuma cor de alerta é pintada aqui, e o sinalizador de erro do
 * Material não aparece solto em lugar nenhum — `CanalDeAlertaTest` varre este arquivo
 * junto com os outros, e **ela varre o texto do arquivo inteiro**: nomear o
 * sinalizador num comentário já derruba o teste, como derrubou a primeira versão
 * deste KDoc.
 */
@Composable
fun CorridaManualScreen(
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
    vm: CorridaManualViewModel = corridaManualViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.registrada) {
        if (estado.registrada) {
            aoSair()
            vm.saidaConsumida()
        }
    }

    CorridaManualScreen(
        estado = estado,
        aoMudarKm = vm::kmMudou,
        aoMudarHoras = vm::horasMudaram,
        aoMudarMinutos = vm::minutosMudaram,
        aoMudarSegundos = vm::segundosMudaram,
        aoMudarFrequencia = vm::frequenciaMudou,
        aoMudarEsforco = vm::esforcoMudou,
        aoMudarTempo1kMinutos = vm::tempo1kMinutosMudou,
        aoMudarTempo1kSegundos = vm::tempo1kSegundosMudou,
        aoMudarTempo5kMinutos = vm::tempo5kMinutosMudou,
        aoMudarTempo5kSegundos = vm::tempo5kSegundosMudou,
        aoAbrirCalendario = vm::abrirCalendario,
        aoFecharCalendario = vm::fecharCalendario,
        aoEscolherData = vm::dataEscolhida,
        aoAbrirRelogio = vm::abrirRelogio,
        aoFecharRelogio = vm::fecharRelogio,
        aoEscolherHora = vm::horaEscolhida,
        aoRegistrar = vm::registrar,
        aoConfirmarRetroativo = vm::confirmarRetroativo,
        aoCancelarRetroativo = vm::cancelarRetroativo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun CorridaManualScreen(
    estado: CorridaManualUiState,
    aoMudarKm: (String) -> Unit,
    aoMudarHoras: (String) -> Unit,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
    aoMudarFrequencia: (String) -> Unit,
    aoMudarEsforco: (String) -> Unit,
    aoAbrirCalendario: () -> Unit,
    aoFecharCalendario: () -> Unit,
    aoEscolherData: (LocalDate) -> Unit,
    aoAbrirRelogio: () -> Unit,
    aoFecharRelogio: () -> Unit,
    aoEscolherHora: (LocalTime) -> Unit,
    aoRegistrar: () -> Unit,
    aoConfirmarRetroativo: () -> Unit,
    aoCancelarRetroativo: () -> Unit,
    modifier: Modifier = Modifier,
    aoMudarTempo1kMinutos: (String) -> Unit = {},
    aoMudarTempo1kSegundos: (String) -> Unit = {},
    aoMudarTempo5kMinutos: (String) -> Unit = {},
    aoMudarTempo5kSegundos: (String) -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.manual_sobrancelha)),
                titulo = stringResource(R.string.manual_titulo),
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(
                modifier = Modifier.padding(horizontal = Goteira),
                verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos),
            ) {
                NotaSobreSplits()

                if (estado.erroDeTela != null) {
                    BannerDeAlerta(
                        rotulo = stringResource(R.string.alerta_corrija),
                        texto = stringResource(estado.erroDeTela),
                    )
                }

                CampoData(estado, aoAbrirCalendario)
                CampoHora(estado, aoAbrirRelogio)

                CampoComErro(
                    valor = estado.km,
                    aoMudar = aoMudarKm,
                    rotulo = R.string.manual_campo_distancia,
                    erro = estado.erros.km,
                    sufixo = { Text(stringResource(R.string.manual_km)) },
                    // Decimal e não `Number`: 6,2 km é a resposta tanto quanto 6.
                    opcoesDeTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )

                CamposDeDuracao(
                    estado = estado,
                    aoMudarHoras = aoMudarHoras,
                    aoMudarMinutos = aoMudarMinutos,
                    aoMudarSegundos = aoMudarSegundos,
                )

                Spacer(Modifier.height(EspacoEntreBlocos - EspacoEntreCampos))

                CampoComErro(
                    valor = estado.fcMedia,
                    aoMudar = aoMudarFrequencia,
                    rotulo = R.string.manual_campo_frequencia,
                    erro = estado.erros.fcMedia,
                    apoio = R.string.manual_campo_frequencia_apoio,
                    sufixo = { Text(stringResource(R.string.manual_bpm)) },
                    opcoesDeTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )

                CampoComErro(
                    valor = estado.esforco,
                    aoMudar = aoMudarEsforco,
                    rotulo = R.string.manual_campo_esforco,
                    erro = estado.erros.esforco,
                    apoio = R.string.manual_campo_esforco_apoio,
                    opcoesDeTeclado = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )

                CamposDeMarca(
                    rotulo = R.string.manual_campo_tempo_1k,
                    minutos = estado.tempo1kMinutos,
                    segundos = estado.tempo1kSegundos,
                    erro = estado.erros.tempo1k,
                    aoMudarMinutos = aoMudarTempo1kMinutos,
                    aoMudarSegundos = aoMudarTempo1kSegundos,
                )

                CamposDeMarca(
                    rotulo = R.string.manual_campo_tempo_5k,
                    minutos = estado.tempo5kMinutos,
                    segundos = estado.tempo5kSegundos,
                    erro = estado.erros.tempo5k,
                    aoMudarMinutos = aoMudarTempo5kMinutos,
                    aoMudarSegundos = aoMudarTempo5kSegundos,
                )

                Spacer(Modifier.height(EspacoAntesDoBotao - EspacoEntreCampos))

                BotaoPrimario(
                    aoTocar = aoRegistrar,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.manual_salvar))
                }

                Spacer(Modifier.height(FolgaDoFim))
            }
        }
    }

    if (estado.escolhendoData) {
        CalendarioDoTreino(
            escolhida = estado.data,
            aoFechar = aoFecharCalendario,
            aoEscolher = aoEscolherData,
        )
    }

    if (estado.escolhendoHora) {
        RelogioDoTreino(
            escolhida = estado.hora,
            aoFechar = aoFecharRelogio,
            aoEscolher = aoEscolherHora,
        )
    }

    estado.confirmando?.let { corrida ->
        ConfirmarRetroativo(
            corrida = corrida,
            aoConfirmar = aoConfirmarRetroativo,
            aoCancelar = aoCancelarRetroativo,
        )
    }
}

/**
 * A nota de docs/03 §3.10, **sem tom de desculpa**.
 *
 * Em `bodyMedium` e em `texto-secundario`, e não num bloco de alerta: ela não avisa
 * de risco nenhum. É o que a tela não captura, dito uma vez, antes de a pessoa
 * preencher.
 */
@Composable
private fun NotaSobreSplits() {
    Text(
        text = stringResource(R.string.manual_nota_splits),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A data do treino: o mesmo `CampoComErro` dos outros, em `somenteLeitura`, com a
 * camada de toque que `F1-T06c` levou para dentro do componente.
 *
 * **Data digitada à mão continua fora**, pelo motivo de `F1-T10`: `03/04` é março ou
 * abril conforme quem digita.
 */
@Composable
internal fun CampoData(estado: CorridaManualUiState, aoAbrir: () -> Unit) {
    val texto = estado.data?.let {
        stringResource(R.string.manual_data_escolhida, it.dayOfMonth, nomeDoMes(it), it.year)
    } ?: ""

    CampoComErro(
        valor = texto,
        aoMudar = {},
        rotulo = R.string.manual_campo_data,
        // O erro é um só para os dois seletores, e o bloco sai **embaixo da hora**: eles
        // respondem uma pergunta só, e repetir a frase nos dois campos a diria duas
        // vezes. O contorno acende aqui também — sem ele, quem esqueceu a data veria a
        // hora acusada, que é o defeito que o emulador pegou em 18/08.
        erro = estado.erros.dataHora,
        explicaOErro = false,
        vazio = R.string.manual_campo_data_vazio,
        somenteLeitura = true,
        aoTocar = aoAbrir,
    )
}

@Composable
internal fun CampoHora(estado: CorridaManualUiState, aoAbrir: () -> Unit) {
    val texto = estado.hora?.let {
        stringResource(R.string.manual_hora_escolhida, it.hour, it.minute)
    } ?: ""

    CampoComErro(
        valor = texto,
        aoMudar = {},
        rotulo = R.string.manual_campo_hora,
        erro = estado.erros.dataHora,
        vazio = R.string.manual_campo_hora_vazio,
        somenteLeitura = true,
        aoTocar = aoAbrir,
    )
}

/**
 * A duração em três campos (docs/03 §3.10: *"duração (h/m/s)"*).
 *
 * **Empilha acima de [ESCALA_QUE_EMPILHA]**, como a grade de dias de `F1-T09` e pelo
 * mesmo motivo: três campos lado a lado em 320dp a `fontScale` 2,0 não cabem, e o
 * rótulo `min` sozinho já ocupa a largura útil de cada um. Empilhados eles ficam
 * legíveis e a tela rola.
 *
 * O erro é um só para os três, e sai embaixo do último: eles respondem uma pergunta.
 */
@Composable
internal fun CamposDeDuracao(
    estado: CorridaManualUiState,
    aoMudarHoras: (String) -> Unit,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
) {
    val empilhado = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

    Column {
        Text(
            text = stringResource(R.string.manual_campo_duracao),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreCampos / 2))

        if (empilhado) {
            Column(verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos / 2)) {
                CampoDeTempo(estado.horas, R.string.manual_campo_horas, estado.erros.duracao, aoMudarHoras, explica = false)
                CampoDeTempo(estado.minutos, R.string.manual_campo_minutos, estado.erros.duracao, aoMudarMinutos, explica = false)
                // Empilhados, o erro sai embaixo do último campo, que é onde o
                // `CampoComErro` já sabe pô-lo.
                CampoDeTempo(
                    estado.segundos,
                    R.string.manual_campo_segundos,
                    estado.erros.duracao,
                    aoMudarSegundos,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(EspacoEntreCampos / 2)) {
                CampoDeTempo(
                    estado.horas,
                    R.string.manual_campo_horas,
                    estado.erros.duracao,
                    aoMudarHoras,
                    Modifier.weight(1f),
                    explica = false,
                )
                CampoDeTempo(
                    estado.minutos,
                    R.string.manual_campo_minutos,
                    estado.erros.duracao,
                    aoMudarMinutos,
                    Modifier.weight(1f),
                    explica = false,
                )
                CampoDeTempo(
                    estado.segundos,
                    R.string.manual_campo_segundos,
                    estado.erros.duracao,
                    aoMudarSegundos,
                    Modifier.weight(1f),
                    explica = false,
                )
            }

            // Lado a lado, o erro não pode sair dentro de uma das três colunas: ele
            // ficaria com um terço da largura, e o bloco de docs/02 §2.4 tem triângulo e
            // rótulo antes do texto. Os três campos acendem o contorno, e o bloco sai uma
            // vez, embaixo da linha inteira: é o mesmo `BannerDeAlerta` do `CampoComErro`.
            if (estado.erros.duracao != null) {
                Spacer(Modifier.height(EspacoEntreCampos / 4))
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_corrija),
                    texto = stringResource(estado.erros.duracao),
                )
            }
        }
    }
}

/**
 * O tempo de melhor 1 km ou de melhor 5 km, em minutos e segundos (decisão nº 110).
 *
 * Sem splits, é a única fonte desses dois recordes (docs/04 §3.3). Opcional, como a
 * frequência: o apoio diz quando preencher. Empilha na mesma escala da duração e pelo
 * mesmo motivo, e o erro sai embaixo da linha inteira.
 */
@Composable
internal fun CamposDeMarca(
    rotulo: Int,
    minutos: String,
    segundos: String,
    erro: Int?,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
) {
    val empilhado = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

    Column {
        Text(
            text = stringResource(rotulo),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.manual_campo_tempo_marca_apoio),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreCampos / 2))

        if (empilhado) {
            Column(verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos / 2)) {
                CampoDeTempo(minutos, R.string.manual_campo_minutos, erro, aoMudarMinutos, explica = false)
                CampoDeTempo(segundos, R.string.manual_campo_segundos, erro, aoMudarSegundos)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(EspacoEntreCampos / 2)) {
                CampoDeTempo(minutos, R.string.manual_campo_minutos, erro, aoMudarMinutos, Modifier.weight(1f), explica = false)
                CampoDeTempo(segundos, R.string.manual_campo_segundos, erro, aoMudarSegundos, Modifier.weight(1f), explica = false)
            }
            if (erro != null) {
                Spacer(Modifier.height(EspacoEntreCampos / 4))
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_corrija),
                    texto = stringResource(erro),
                )
            }
        }
    }
}

/**
 * Um dos campos de tempo. O teclado é numérico e o erro é opcional.
 *
 * Com [explica] falso o campo acende o contorno de erro sem desenhar o bloco: o erro de
 * um grupo de campos (duração, melhor km) acende todos e se explica uma vez só.
 */
@Composable
private fun CampoDeTempo(
    valor: String,
    rotulo: Int,
    erro: Int?,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    explica: Boolean = true,
) {
    CampoComErro(
        valor = valor,
        aoMudar = aoMudar,
        rotulo = rotulo,
        erro = erro,
        explicaOErro = explica,
        opcoesDeTeclado = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

/**
 * O calendário do treino.
 *
 * **O `DatePicker` fala em milissegundos UTC**, e a conversão é em `ZoneOffset.UTC`
 * nos dois sentidos — o que ele devolve é uma data de calendário disfarçada de
 * instante, e converter com o fuso do aparelho move a data em um dia para quem está a
 * oeste de Greenwich. Quem transforma a data escolhida em instante de verdade, no fuso
 * do plano, é [validarCorrida].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarioDoTreino(
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
            BotaoDeTexto(
                aoTocar = {
                    estadoDoCalendario.selectedDateMillis?.let { millis ->
                        aoEscolher(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
                habilitado = estadoDoCalendario.selectedDateMillis != null,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoFechar) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DatePicker(state = estadoDoCalendario)
    }
}

/**
 * O relógio do treino, em 24h.
 *
 * `is24Hour` fixo pelo mesmo motivo de `LocaleDoApp` ser fixo: o app é um só, em
 * português, e `6:30 PM` num aparelho configurado em inglês seria a hora escrita numa
 * convenção que o resto da tela não usa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RelogioDoTreino(
    escolhida: LocalTime?,
    aoFechar: () -> Unit,
    aoEscolher: (LocalTime) -> Unit,
) {
    val estadoDoRelogio = rememberTimePickerState(
        initialHour = escolhida?.hour ?: 6,
        initialMinute = escolhida?.minute ?: 0,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            BotaoDeTexto(
                aoTocar = {
                    aoEscolher(LocalTime.of(estadoDoRelogio.hour, estadoDoRelogio.minute))
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoFechar) { Text(stringResource(android.R.string.cancel)) }
        },
        text = { TimePicker(state = estadoDoRelogio) },
    )
}

/**
 * // RN-04
 *
 * O diálogo do registro retroativo.
 *
 * **Ele nomeia a data**, e não pergunta *"tem certeza?"*: a pergunta sem o dado é a que
 * todo mundo confirma sem ler. O texto diz a consequência que a pessoa não vê — a
 * corrida entra na semana em que aconteceu (RN-02), e não na semana corrente, então ela
 * não muda a fração de hoje na Home.
 */
@Composable
private fun ConfirmarRetroativo(
    corrida: ValidacaoDaCorrida.Ok,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit,
) {
    val dia = corrida.dia

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = {
            Text(
                stringResource(
                    R.string.manual_retroativo_titulo,
                    stringResource(
                        R.string.manual_data_escolhida,
                        dia.dayOfMonth,
                        nomeDoMes(dia),
                        dia.year,
                    ),
                ),
            )
        },
        text = { Text(stringResource(R.string.manual_retroativo_texto)) },
        confirmButton = {
            BotaoDeTexto(aoTocar = aoConfirmar) {
                Text(stringResource(R.string.manual_retroativo_confirmar))
            }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoCancelar) {
                Text(stringResource(R.string.manual_retroativo_cancelar))
            }
        },
    )
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun corridaManualViewModel(): CorridaManualViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        CorridaManualViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.temporadaRepositorio,
            container.recalculoDeXp,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun CorridaManualPreview(estado: CorridaManualUiState) {
    PokerunTheme {
        CorridaManualScreen(
            estado = estado,
            aoMudarKm = {},
            aoMudarHoras = {},
            aoMudarMinutos = {},
            aoMudarSegundos = {},
            aoMudarFrequencia = {},
            aoMudarEsforco = {},
            aoAbrirCalendario = {},
            aoFecharCalendario = {},
            aoEscolherData = {},
            aoAbrirRelogio = {},
            aoFecharRelogio = {},
            aoEscolherHora = {},
            aoRegistrar = {},
            aoConfirmarRetroativo = {},
            aoCancelarRetroativo = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CorridaManualVaziaPreview() {
    CorridaManualPreview(CorridaManualUiState())
}

@Preview(showBackground = true)
@Composable
private fun CorridaManualPreenchidaPreview() {
    CorridaManualPreview(
        CorridaManualUiState(
            data = LocalDate.of(2026, 8, 18),
            hora = LocalTime.of(6, 30),
            km = "8,4",
            minutos = "45",
            segundos = "12",
            fcMedia = "152",
            esforco = "7",
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun CorridaManualComErrosPreview() {
    CorridaManualPreview(
        CorridaManualUiState(
            km = "abc",
            minutos = "90",
            erros = ErrosDaCorrida(
                dataHora = R.string.manual_erro_quando_ausente,
                km = R.string.manual_erro_distancia,
                duracao = R.string.manual_erro_duracao_forma,
            ),
        ),
    )
}

@Preview(showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun CorridaManualFonteGrandePreview() {
    CorridaManualPreview(
        CorridaManualUiState(
            data = LocalDate.of(2026, 8, 18),
            hora = LocalTime.of(6, 30),
            km = "8,4",
            minutos = "45",
        ),
    )
}
