package com.hggabriel.pokerun.ui.telas.entrarcomcodigo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val RecheioDaFicha = 16.dp
private val FolgaDoFim = 24.dp

/** O esqueleto da busca, na forma da ficha do plano (docs/02 §8, item 6). */
private val AlturaDoEsqueleto = 88.dp

/**
 * A entrada em plano por código de convite (`F1-T14`, docs/03 §3.8).
 *
 * **Três blocos, na ordem em que a decisão acontece:** o campo do código, a prévia do
 * plano que ele resolveu, e a confirmação. Nada é gravado antes do último toque.
 *
 * **O campo não recusa caractere: ele não aceita.** `normalizarCodigo` (RN-29) sobe a
 * caixa e descarta o que não é do alfabeto, então digitar `l` ou colar com hífen não
 * produz mensagem de erro nenhuma — produz o código certo. É a diferença entre um campo
 * que corrige e um que reclama.
 *
 * **A troca do plano ativo nunca é silenciosa** (RN-13). Quem já tem um plano ativo passa
 * por um diálogo que nomeia o plano de antes e oferece as duas saídas da especificação:
 * tornar o novo ativo, ou entrar guardado. Quem não tem plano ativo entra direto, porque
 * não há troca — há campo vazio a preencher.
 *
 * **Plano encerrado aparece e não deixa entrar** (RN-27). Ele é somente leitura (RN-07),
 * então o botão sai da tela e uma frase diz por quê. Esconder o plano seria pior: o
 * código está certo, e um *"não encontrado"* mandaria a pessoa conferir letra por letra.
 */
@Composable
fun EntrarComCodigoScreen(
    aoEntrar: () -> Unit,
    modifier: Modifier = Modifier,
    vm: EntrarComCodigoViewModel = entrarComCodigoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.entrou) {
        if (estado.entrou) aoEntrar()
    }

    EntrarComCodigoScreen(
        estado = estado,
        aoMudarCodigo = vm::codigoMudou,
        aoBuscar = vm::buscar,
        aoEntrar = vm::entrar,
        aoCancelarEscolha = vm::cancelarEscolha,
        aoEscolherAtivo = vm::confirmarEntrada,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun EntrarComCodigoScreen(
    estado: EntrarComCodigoUiState,
    aoMudarCodigo: (String) -> Unit,
    aoBuscar: () -> Unit,
    aoEntrar: () -> Unit,
    aoCancelarEscolha: () -> Unit,
    aoEscolherAtivo: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            // Modal: esta tela vive no `NavHost` de fora, sem o `Scaffold` da casca para
            // descontar a barra de status. Sem isto a sobrancelha sai debaixo do relógio.
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = FolgaDoFim),
        ) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.entrar_sobrancelha)),
                titulo = stringResource(R.string.entrar_titulo),
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                CampoDoCodigo(estado, aoMudarCodigo, aoBuscar)

                Spacer(Modifier.height(EspacoEntreBlocos))

                Button(
                    onClick = aoBuscar,
                    enabled = estado.podeBuscar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDoBotao),
                ) {
                    Text(stringResource(R.string.entrar_buscar))
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))

                ResultadoDaBusca(estado, aoEntrar)

                estado.erro?.let { erro ->
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    BannerDeAlerta(
                        rotulo = stringResource(R.string.alerta_falha_ao_entrar),
                        texto = stringResource(erro),
                    )
                }
            }
        }
    }

    estado.escolhendoAtivo?.let { previa ->
        DialogoDaEscolha(
            previa = previa,
            aoCancelar = aoCancelarEscolha,
            aoEscolher = aoEscolherAtivo,
        )
    }
}

// ---------------------------------------------------------------------------
// O campo
// ---------------------------------------------------------------------------

/**
 * // RN-29
 *
 * O código é **dado**, e por isso vem em mono: são seis caracteres que a pessoa confere
 * um a um contra um papel ou uma mensagem, e é exatamente o caso em que a Plex Mono
 * separa o que a Plex Sans junta.
 *
 * `KeyboardCapitalization.Characters` porque o alfabeto é todo em caixa alta — o teclado
 * já abre no lugar certo, e `normalizarCodigo` cuida de quem digitar mesmo assim em caixa
 * baixa. `ImeAction.Search` não existe aqui de propósito: o botão é a ação, e um teclado
 * que busca com quatro caracteres pediria uma leitura garantidamente vazia.
 */
@Composable
private fun CampoDoCodigo(
    estado: EntrarComCodigoUiState,
    aoMudar: (String) -> Unit,
    aoBuscar: () -> Unit,
) {
    OutlinedTextField(
        value = estado.codigo,
        onValueChange = aoMudar,
        label = { Text(stringResource(R.string.entrar_campo)) },
        singleLine = true,
        textStyle = EstiloDado,
        supportingText = { Text(stringResource(R.string.entrar_apoio)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        // O `Done` do teclado busca, e o `podeBuscar` do `ViewModel` é quem decide se há
        // o que buscar: a guarda não pode depender de qual dos dois caminhos a pessoa
        // usou. Sem isto, quem digita o sexto caractere e fecha o teclado fica olhando
        // para um botão que ela já achou que tinha tocado.
        keyboardActions = KeyboardActions(onDone = { aoBuscar() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Os cinco estados de docs/03 §3.8
// ---------------------------------------------------------------------------

@Composable
private fun ResultadoDaBusca(estado: EntrarComCodigoUiState, aoEntrar: () -> Unit) {
    when (val resultado = estado.resultado) {
        Resultado.Idle -> Unit
        Resultado.Buscando -> Esqueleto()
        Resultado.NaoEncontrado -> Aviso(stringResource(R.string.entrar_nao_encontrado))
        is Resultado.JaMembro ->
            Aviso(stringResource(R.string.entrar_ja_membro, resultado.nome))
        is Resultado.Encontrado ->
            PlanoEncontrado(previa = resultado.previa, entrando = estado.entrando, aoEntrar = aoEntrar)
    }
}

/**
 * A prévia do plano, na mesma forma da linha da lista de planos: nome, data da prova no
 * fuso do plano (RN-28) e distância. É o que docs/03 §3.8 chama de *"prévia do plano"*, e
 * é o suficiente para alguém reconhecer se o convite é o que esperava.
 */
@Composable
private fun PlanoEncontrado(previa: PreviaDaEntrada, entrando: Boolean, aoEntrar: () -> Unit) {
    val locale = LocaleDoApp
    val dados = stringResource(
        R.string.planos_dados,
        previa.dataDaProva.dayOfMonth,
        nomeDoMes(previa.dataDaProva, locale),
        previa.dataDaProva.year,
        formatarKm(previa.distanciaAlvoKm, locale),
    )

    Ficha {
        Column(
            modifier = Modifier
                .padding(RecheioDaFicha)
                .clearAndSetSemantics {
                    contentDescription = "${previa.nome}. $dados"
                },
        ) {
            Text(
                text = previa.nome,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(EspacoEntreLinhas))
            Text(
                text = dados,
                style = EstiloDado,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(EspacoEntreBlocos))

    // RN-27: plano encerrado não recebe corridas (RN-07), então não há entrada a
    // oferecer. A frase fica no lugar do botão, e não ao lado dele.
    if (previa.podeEntrar) {
        Button(
            onClick = aoEntrar,
            enabled = !entrando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDoBotao),
        ) {
            Text(
                stringResource(
                    if (entrando) R.string.entrar_entrando else R.string.entrar_confirmar,
                ),
            )
        }
    } else {
        Aviso(stringResource(R.string.entrar_encerrado))
    }
}

@Composable
private fun Aviso(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Esqueleto na forma do conteúdo (docs/02 §8, item 6), nunca spinner centralizado. */
@Composable
private fun Esqueleto() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDoEsqueleto)
            .background(
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            ),
    )
}

// ---------------------------------------------------------------------------
// A escolha de RN-13
// ---------------------------------------------------------------------------

/**
 * // RN-13
 *
 * O diálogo obrigatório de docs/03 §3.8. **Ele não tem "cancelar" como terceiro botão
 * disfarçado:** as duas ações são as duas maneiras de entrar, e sair sem entrar é o
 * toque fora ou o voltar do sistema.
 *
 * A frase nomeia o plano de antes porque a pergunta que a pessoa faz não é *"quero
 * este?"*, e sim *"o que acontece com o outro?"*. Quando o nome não pôde ser lido, a
 * frase existe sem ele — inventar um nome vazio seria pior que a ausência.
 */
@Composable
private fun DialogoDaEscolha(
    previa: PreviaDaEntrada,
    aoCancelar: () -> Unit,
    aoEscolher: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.entrar_escolha_titulo)) },
        text = {
            Text(
                text = previa.nomeDoPlanoAtivo?.let {
                    stringResource(R.string.entrar_escolha_texto, it)
                } ?: stringResource(R.string.entrar_escolha_texto_sem_nome),
            )
        },
        confirmButton = {
            TextButton(onClick = { aoEscolher(true) }) {
                Text(stringResource(R.string.entrar_escolha_ativo))
            }
        },
        dismissButton = {
            TextButton(onClick = { aoEscolher(false) }) {
                Text(stringResource(R.string.entrar_escolha_dormente))
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
private fun entrarComCodigoViewModel(): EntrarComCodigoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        EntrarComCodigoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.conviteRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun EntrarPreview(estado: EntrarComCodigoUiState) {
    PokerunTheme {
        EntrarComCodigoScreen(
            estado = estado,
            aoMudarCodigo = {},
            aoBuscar = {},
            aoEntrar = {},
            aoCancelarEscolha = {},
            aoEscolherAtivo = {},
        )
    }
}

private val PREVIA_DE_EXEMPLO = PreviaDaEntrada(
    planoId = "a",
    nome = "São Silvestre 2026",
    dataDaProva = LocalDate.of(2026, 12, 31),
    distanciaAlvoKm = 15.0,
    encerrado = false,
    exigeEscolha = false,
    nomeDoPlanoAtivo = null,
)

@Preview(name = "Idle", showBackground = true)
@Composable
private fun IdlePreview() = EntrarPreview(EntrarComCodigoUiState())

@Preview(name = "Buscando", showBackground = true)
@Composable
private fun BuscandoPreview() = EntrarPreview(
    EntrarComCodigoUiState(codigo = "FYQJE6", resultado = Resultado.Buscando),
)

@Preview(name = "Encontrado", showBackground = true)
@Composable
private fun EncontradoPreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.Encontrado(PREVIA_DE_EXEMPLO),
    ),
)

@Preview(name = "Encontrado e encerrado", showBackground = true)
@Composable
private fun EncerradoPreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.Encontrado(PREVIA_DE_EXEMPLO.copy(encerrado = true)),
    ),
)

@Preview(name = "Não encontrado", showBackground = true)
@Composable
private fun NaoEncontradoPreview() = EntrarPreview(
    EntrarComCodigoUiState(codigo = "FYQJE6", resultado = Resultado.NaoEncontrado),
)

@Preview(name = "Já membro", showBackground = true)
@Composable
private fun JaMembroPreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.JaMembro("São Silvestre 2026"),
    ),
)

@Preview(name = "Escolhendo o ativo", showBackground = true)
@Composable
private fun EscolhendoPreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.Encontrado(PREVIA_DE_EXEMPLO),
        escolhendoAtivo = PREVIA_DE_EXEMPLO.copy(
            exigeEscolha = true,
            nomeDoPlanoAtivo = "Meia de Interlagos",
        ),
    ),
)

@Preview(name = "Erro de rede", showBackground = true)
@Composable
private fun ErroPreview() = EntrarPreview(
    EntrarComCodigoUiState(codigo = "FYQJE6", erro = R.string.entrar_erro_buscar),
)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.Encontrado(PREVIA_DE_EXEMPLO),
    ),
)

@Preview(name = "320dp", showBackground = true, widthDp = 320)
@Composable
private fun EstreitoPreview() = EntrarPreview(
    EntrarComCodigoUiState(
        codigo = "FYQJE6",
        resultado = Resultado.Encontrado(PREVIA_DE_EXEMPLO),
    ),
)
