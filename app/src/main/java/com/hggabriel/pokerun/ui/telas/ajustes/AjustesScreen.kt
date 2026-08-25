package com.hggabriel.pokerun.ui.telas.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.healthconnect.OrigemDeTreino
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val IndicadorNoBotao = 20.dp
private val RecheioDaFicha = 16.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreCampos = 16.dp
private val EspacoEntreBlocos = 24.dp
private val EspacoAntesDaSecao = 32.dp

/** A folga no fim, para o último botão não colar na barra inferior. */
private val FolgaDoFim = 24.dp

/**
 * Os ajustes de perfil e de origem (`F1-T17`, docs/03 §3.11).
 *
 * **É destino de cada grafo de aba** (`F1-T07c`, decisão nº 60), alcançado pela
 * engrenagem que docs/02 §10.1 põe na raiz de toda aba. Empilha sobre a raiz da aba em
 * que foi aberta, e o voltar devolve àquela raiz — por isso não há botão de voltar
 * desenhado aqui.
 *
 * **Vive dentro da casca**, então não leva `safeDrawingPadding`: o `Scaffold` da casca
 * já entrega o espaçamento.
 *
 * ### As quatro opções da ficha, e a que não está aqui
 *
 * Editar nome e `baseline_km`, trocar a fonte canônica, refazer as permissões e sair
 * estão. **O botão de recalcular agregados não** — a implementação é `F3-T08`, sobre o
 * motor de replay de `F2-T07`, e a ficha de `F1-T17` diz que na Fase 1 ele pode não
 * existir ainda. Ver o KDoc de [AjustesUiState].
 *
 * ### Os erros saem pelo canal, sem exceção
 *
 * Todo campo é [CampoComErro] e a falha de tela é [BannerDeAlerta] (`F1-T06b`,
 * docs/02 §2.4). Nenhuma cor de alerta é pintada aqui, e `CanalDeAlertaTest` varre o
 * texto deste arquivo inteiro — nomear o sinalizador do Material até em comentário já
 * derruba o teste.
 */
@Composable
fun AjustesScreen(
    aoSairDaConta: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AjustesViewModel = ajustesViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.saiuDaConta) {
        if (estado.saiuDaConta) {
            aoSairDaConta()
            vm.saidaConsumida()
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { vm.permissaoRespondida() }

    AjustesScreen(
        estado = estado,
        aoMudarNome = vm::nomeMudou,
        aoMudarBaseline = vm::baselineMudou,
        aoSalvarPerfil = vm::salvarPerfil,
        aoPedirPermissao = { pedirPermissao.launch(vm.permissoesDeSaude) },
        aoTentarDeNovo = vm::tentarLerOrigens,
        aoEscolherOrigem = vm::escolherOrigem,
        aoUsarOrigem = vm::salvarFonte,
        aoPedirParaSair = vm::pedirParaSair,
        aoConfirmarSaida = vm::sair,
        aoDesistirDeSair = vm::desistirDeSair,
        modifier = modifier,
    )
}

@Composable
fun AjustesScreen(
    estado: AjustesUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarBaseline: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
    aoPedirPermissao: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoEscolherOrigem: (String) -> Unit,
    aoUsarOrigem: () -> Unit,
    aoPedirParaSair: () -> Unit,
    aoConfirmarSaida: () -> Unit,
    aoDesistirDeSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.ajustes_sobrancelha)),
                titulo = stringResource(R.string.ajustes_titulo),
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(
                modifier = Modifier.padding(horizontal = Goteira),
                verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos),
            ) {
                if (estado.erroDeTela != null) {
                    BannerDeAlerta(
                        rotulo = stringResource(R.string.alerta_corrija),
                        texto = stringResource(estado.erroDeTela),
                    )
                }

                BlocoDoPerfil(
                    estado = estado,
                    aoMudarNome = aoMudarNome,
                    aoMudarBaseline = aoMudarBaseline,
                    aoSalvarPerfil = aoSalvarPerfil,
                )

                BlocoDaOrigem(
                    estado = estado,
                    aoPedirPermissao = aoPedirPermissao,
                    aoTentarDeNovo = aoTentarDeNovo,
                    aoEscolherOrigem = aoEscolherOrigem,
                    aoUsarOrigem = aoUsarOrigem,
                )

                Spacer(Modifier.height(EspacoAntesDaSecao - EspacoEntreCampos))
                HorizontalDivider()

                OutlinedButton(
                    onClick = aoPedirParaSair,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDoBotao),
                ) {
                    Text(stringResource(R.string.ajustes_sair))
                }

                Spacer(Modifier.height(FolgaDoFim))
            }
        }
    }

    if (estado.confirmandoSaida) {
        AlertDialog(
            onDismissRequest = aoDesistirDeSair,
            title = { Text(stringResource(R.string.ajustes_sair_titulo)) },
            text = { Text(stringResource(R.string.ajustes_sair_texto)) },
            confirmButton = {
                TextButton(onClick = aoConfirmarSaida) {
                    Text(stringResource(R.string.ajustes_sair_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = aoDesistirDeSair) {
                    Text(stringResource(R.string.ajustes_sair_cancelar))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// O perfil
// ---------------------------------------------------------------------------

@Composable
private fun BlocoDoPerfil(
    estado: AjustesUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarBaseline: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
) {
    TituloDaSecao(R.string.ajustes_perfil_titulo)

    CampoComErro(
        valor = estado.nome,
        aoMudar = aoMudarNome,
        rotulo = R.string.ajustes_campo_nome,
        erro = estado.erros.nome,
        habilitado = !estado.carregando && !estado.salvandoPerfil,
        opcoesDeTeclado = KeyboardOptions(imeAction = ImeAction.Next),
    )

    CampoComErro(
        valor = estado.baseline,
        aoMudar = aoMudarBaseline,
        rotulo = R.string.ajustes_campo_baseline,
        erro = estado.erros.baseline,
        // A nota diz o que trocar a baseline faz e o que ela **não** faz: a grade dos
        // planos que já existem é congelada semana a semana (RN-05), e sem a frase a
        // pessoa esperaria o plano corrente se refazer sozinho.
        apoio = R.string.ajustes_baseline_apoio,
        habilitado = !estado.carregando && !estado.salvandoPerfil,
        opcoesDeTeclado = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
    )

    BotaoPrincipal(
        rotulo = R.string.ajustes_salvar,
        aoTocar = aoSalvarPerfil,
        ocupado = estado.salvandoPerfil,
        habilitado = estado.podeSalvar,
    )

    if (estado.perfilSalvo) {
        Text(
            text = stringResource(R.string.ajustes_salvo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// A origem dos treinos (RN-22)
// ---------------------------------------------------------------------------

@Composable
private fun BlocoDaOrigem(
    estado: AjustesUiState,
    aoPedirPermissao: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoEscolherOrigem: (String) -> Unit,
    aoUsarOrigem: () -> Unit,
) {
    Spacer(Modifier.height(EspacoAntesDaSecao - EspacoEntreCampos))
    HorizontalDivider()
    TituloDaSecao(R.string.ajustes_origem_titulo)

    when (estado.bloco) {
        BlocoDeOrigem.Indisponivel -> Corpo(R.string.ajustes_origem_indisponivel)

        BlocoDeOrigem.SemPermissao -> {
            Corpo(R.string.ajustes_origem_sem_permissao)
            BotaoPrincipal(
                rotulo = R.string.ajustes_origem_conceder,
                aoTocar = aoPedirPermissao,
                ocupado = false,
            )
        }

        BlocoDeOrigem.PodeEscolher -> {
            when {
                estado.lendoOrigens -> CircularProgressIndicator(
                    modifier = Modifier.height(IndicadorNoBotao),
                    strokeWidth = 2.dp,
                )

                estado.falhouALeitura -> {
                    Corpo(R.string.ajustes_origem_falhou)
                    BotaoPrincipal(
                        rotulo = R.string.ajustes_origem_repetir,
                        aoTocar = aoTentarDeNovo,
                        ocupado = false,
                    )
                }

                estado.origens.isEmpty() -> Corpo(R.string.ajustes_origem_vazia)

                else -> {
                    Corpo(R.string.ajustes_origem_corpo)

                    Text(
                        text = estado.rotuloDaFonteAtual
                            ?.let { stringResource(R.string.ajustes_origem_atual, it) }
                            ?: stringResource(R.string.ajustes_origem_nenhuma),
                        style = EstiloDado,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(EspacoEntreBlocos - EspacoEntreCampos))

                    estado.origens.forEach { origem ->
                        LinhaDeOrigem(
                            origem = origem,
                            selecionada = origem.pacote == estado.escolhida,
                            habilitada = !estado.salvandoFonte,
                            aoTocar = { aoEscolherOrigem(origem.pacote) },
                        )
                    }

                    BotaoPrincipal(
                        rotulo = R.string.ajustes_origem_usar,
                        aoTocar = aoUsarOrigem,
                        ocupado = estado.salvandoFonte,
                        // Nada a gravar quando a escolha já é a que está no documento.
                        habilitado = estado.escolhida != null &&
                            estado.escolhida != estado.fonteAtual,
                    )
                }
            }

            // Fica fora do `when` interno porque vale nos quatro caminhos: quem já
            // concedeu pode ter concedido de menos, e é por aqui que a folha reabre.
            TextButton(onClick = aoPedirPermissao) {
                Text(stringResource(R.string.ajustes_origem_refazer))
            }
        }
    }
}

/**
 * Uma origem da lista: o nome que o usuário reconhece e quantas corridas ela gravou.
 *
 * **Selecionada não é só a cor** (docs/02 §4.2): a [Ficha] troca fundo e borda, e para o
 * TalkBack a linha inteira é um item selecionável. É a mesma linha do passo 5 do
 * cadastro, e ela é a segunda cópia de propósito — promover o componente mexeria numa
 * tela de fase anterior de passagem (`EXECUCAO.md §6`).
 */
@Composable
private fun LinhaDeOrigem(
    origem: OrigemDeTreino,
    selecionada: Boolean,
    habilitada: Boolean,
    aoTocar: () -> Unit,
) {
    Ficha(
        aoTocar = aoTocar,
        selecionada = selecionada,
        habilitada = habilitada,
        modifier = Modifier.semantics { selected = selecionada },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDoBotao)
                .padding(RecheioDaFicha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = origem.rotulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.ajustes_origem_corridas,
                        origem.corridas,
                        origem.corridas,
                    ),
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Peças comuns
// ---------------------------------------------------------------------------

@Composable
private fun TituloDaSecao(rotulo: Int) {
    Text(
        text = stringResource(rotulo),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun Corpo(texto: Int) {
    Text(
        text = stringResource(texto),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/**
 * O botão de ação, com o indicador **dentro dele** e não numa tela própria (docs/02 §8,
 * item 6). Terceira cópia do mesmo desenho, pela razão de [LinhaDeOrigem].
 */
@Composable
private fun BotaoPrincipal(
    rotulo: Int,
    aoTocar: () -> Unit,
    ocupado: Boolean,
    habilitado: Boolean = true,
) {
    Button(
        onClick = aoTocar,
        enabled = habilitado && !ocupado,
        colors = if (ocupado) {
            ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        if (ocupado) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(IndicadorNoBotao)
                    .clearAndSetSemantics {},
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text = stringResource(rotulo))
        }
    }
}

// ---------------------------------------------------------------------------
// Fábrica
// ---------------------------------------------------------------------------

@Composable
private fun ajustesViewModel(): AjustesViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        AjustesViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.saudeRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val ORIGENS_DE_EXEMPLO = listOf(
    OrigemDeTreino("com.sec.android.app.shealth", "Samsung Health", 12),
    OrigemDeTreino("com.strava", "Strava", 8),
)

@Composable
private fun AjustesPreview(estado: AjustesUiState) {
    PokerunTheme {
        AjustesScreen(
            estado = estado,
            aoMudarNome = {},
            aoMudarBaseline = {},
            aoSalvarPerfil = {},
            aoPedirPermissao = {},
            aoTentarDeNovo = {},
            aoEscolherOrigem = {},
            aoUsarOrigem = {},
            aoPedirParaSair = {},
            aoConfirmarSaida = {},
            aoDesistirDeSair = {},
        )
    }
}

private val COM_ORIGENS = AjustesUiState(
    carregando = false,
    nome = "Hiago",
    baseline = "7,5",
    bloco = BlocoDeOrigem.PodeEscolher,
    origens = ORIGENS_DE_EXEMPLO,
    fonteAtual = "com.strava",
    escolhida = "com.strava",
)

@Preview(name = "origens", showBackground = true)
@Composable
private fun AjustesComOrigensPreview() = AjustesPreview(COM_ORIGENS)

@Preview(name = "sem Health Connect", showBackground = true)
@Composable
private fun AjustesSemHealthConnectPreview() = AjustesPreview(
    COM_ORIGENS.copy(bloco = BlocoDeOrigem.Indisponivel, origens = emptyList()),
)

@Preview(name = "sem permissão", showBackground = true)
@Composable
private fun AjustesSemPermissaoPreview() = AjustesPreview(
    COM_ORIGENS.copy(bloco = BlocoDeOrigem.SemPermissao, origens = emptyList()),
)

@Preview(name = "perfil acusado", showBackground = true)
@Composable
private fun AjustesComErroPreview() = AjustesPreview(
    COM_ORIGENS.copy(
        nome = "",
        baseline = "1e3",
        erros = ErrosDoPerfil(
            nome = R.string.ajustes_erro_nome,
            baseline = R.string.ajustes_erro_baseline,
        ),
        podeSalvar = true,
    ),
)

@Preview(name = "origens em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun AjustesAmpliadoPreview() = AjustesPreview(COM_ORIGENS)
