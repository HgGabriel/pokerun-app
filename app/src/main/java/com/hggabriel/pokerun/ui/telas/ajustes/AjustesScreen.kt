package com.hggabriel.pokerun.ui.telas.ajustes

import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val IndicadorNoBotao = 20.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreCampos = 16.dp
private val EspacoAntesDaSecao = 32.dp

/** A folga no fim, para o último botão não colar na barra inferior. */
private val FolgaDoFim = 24.dp

/**
 * Os ajustes de perfil (`F1-T17`, docs/03 §3.11).
 *
 * **É destino de cada grafo de aba** (`F1-T07c`, decisão nº 60), alcançado pela
 * engrenagem que docs/02 §10.1 põe na raiz de toda aba. Empilha sobre a raiz da aba em
 * que foi aberta, e o voltar devolve àquela raiz — por isso não há botão de voltar
 * desenhado aqui.
 *
 * **Vive dentro da casca**, então não leva `safeDrawingPadding`: o `Scaffold` da casca
 * já entrega o espaçamento.
 *
 * ### As opções da tela, e a que não está aqui
 *
 * Editar nome e `baseline_km` e sair estão. **O botão de recalcular agregados não** — a
 * implementação é `F3-T08`, sobre o motor de replay de `F2-T07`. Ver o KDoc de
 * [AjustesUiState].
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
    recalculoVm: RecalculoViewModel = recalculoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val recalculo by recalculoVm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.saiuDaConta) {
        if (estado.saiuDaConta) {
            aoSairDaConta()
            vm.saidaConsumida()
        }
    }

    AjustesScreen(
        estado = estado,
        aoMudarNome = vm::nomeMudou,
        aoMudarBaseline = vm::baselineMudou,
        aoSalvarPerfil = vm::salvarPerfil,
        aoPedirParaSair = vm::pedirParaSair,
        aoConfirmarSaida = vm::sair,
        aoDesistirDeSair = vm::desistirDeSair,
        modifier = modifier,
        recalculo = recalculo,
        aoPedirRecalculo = recalculoVm::pedirConfirmacao,
        aoConfirmarRecalculo = recalculoVm::recalcular,
        aoCancelarRecalculo = recalculoVm::cancelar,
    )
}

@Composable
fun AjustesScreen(
    estado: AjustesUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarBaseline: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
    aoPedirParaSair: () -> Unit,
    aoConfirmarSaida: () -> Unit,
    aoDesistirDeSair: () -> Unit,
    modifier: Modifier = Modifier,
    recalculo: EstadoDoRecalculo? = null,
    aoPedirRecalculo: () -> Unit = {},
    aoConfirmarRecalculo: () -> Unit = {},
    aoCancelarRecalculo: () -> Unit = {},
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

                if (recalculo != null) {
                    Spacer(Modifier.height(EspacoAntesDaSecao - EspacoEntreCampos))
                    HorizontalDivider()
                    BlocoDeRecalculo(recalculo, aoPedirRecalculo)
                }

                Spacer(Modifier.height(EspacoAntesDaSecao - EspacoEntreCampos))
                HorizontalDivider()

                BotaoSecundario(
                    aoTocar = aoPedirParaSair,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.ajustes_sair))
                }

                Spacer(Modifier.height(FolgaDoFim))
            }
        }
    }

    if (recalculo == EstadoDoRecalculo.Confirmando) {
        AlertDialog(
            onDismissRequest = aoCancelarRecalculo,
            title = { Text(stringResource(R.string.ajustes_recalcular_titulo)) },
            text = { Text(stringResource(R.string.ajustes_recalcular_texto)) },
            confirmButton = {
                BotaoDeTexto(aoTocar = aoConfirmarRecalculo) {
                    Text(stringResource(R.string.ajustes_recalcular_confirmar))
                }
            },
            dismissButton = {
                BotaoDeTexto(aoTocar = aoCancelarRecalculo) {
                    Text(stringResource(R.string.editar_voltar))
                }
            },
        )
    }

    if (estado.confirmandoSaida) {
        AlertDialog(
            onDismissRequest = aoDesistirDeSair,
            title = { Text(stringResource(R.string.ajustes_sair_titulo)) },
            text = { Text(stringResource(R.string.ajustes_sair_texto)) },
            confirmButton = {
                BotaoDeTexto(aoTocar = aoConfirmarSaida) {
                    Text(stringResource(R.string.ajustes_sair_confirmar))
                }
            },
            dismissButton = {
                BotaoDeTexto(aoTocar = aoDesistirDeSair) {
                    Text(stringResource(R.string.ajustes_sair_cancelar))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// O recálculo (F3-T08)
// ---------------------------------------------------------------------------

/**
 * O botão **Recalcular Agregados** de docs/03 §3.11, com a confirmação antes, o progresso
 * durante e o resumo depois: quantas corridas e o XP da temporada antes e depois (ficha
 * de `F3-T08`). O progresso é barra, nunca spinner centralizado (docs/02 §8, item 6).
 */
@Composable
private fun BlocoDeRecalculo(estado: EstadoDoRecalculo, aoPedir: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EspacoEntreCampos / 2)) {
        TituloDaSecao(R.string.ajustes_recalcular_secao)
        Text(
            text = stringResource(R.string.ajustes_recalcular_apoio),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (estado) {
            EstadoDoRecalculo.Recalculando -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.ajustes_recalcular_andamento),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            is EstadoDoRecalculo.Concluido -> Text(
                text = pluralStringResource(
                    R.plurals.ajustes_recalcular_resumo,
                    estado.resumo.corridas,
                    estado.resumo.corridas,
                    String.format(LocaleDoApp, "%,d", estado.resumo.xpAntes),
                    String.format(LocaleDoApp, "%,d", estado.resumo.xpDepois),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            EstadoDoRecalculo.Falhou -> Text(
                text = stringResource(R.string.ajustes_recalcular_erro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Unit
        }
        if (estado != EstadoDoRecalculo.Recalculando) {
            BotaoSecundario(
                aoTocar = aoPedir,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(stringResource(R.string.ajustes_recalcular))
            }
        }
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
        // A unidade é sufixo e não parte do rótulo, como no cadastro e na criação de
        // plano. **O emulador mostrou o custo de divergir:** com `(km)` no rótulo, a
        // `fontScale` 2,0 a 320dp quebrava o rótulo em duas linhas e a segunda cruzava
        // o contorno do campo.
        sufixo = { Text(stringResource(R.string.ajustes_km)) },
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

/**
 * O botão de ação, com o indicador **dentro dele** e não numa tela própria (docs/02 §8,
 * item 6). É cópia do desenho do cadastro de propósito: promover o componente mexeria
 * numa tela de fase anterior de passagem (`EXECUCAO.md §6`).
 */
@Composable
private fun BotaoPrincipal(
    rotulo: Int,
    aoTocar: () -> Unit,
    ocupado: Boolean,
    habilitado: Boolean = true,
) {
    BotaoPrimario(
        aoTocar = aoTocar,
        habilitado = habilitado,
        ocupado = ocupado,
        modifier = Modifier
            .fillMaxWidth(),
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
private fun recalculoViewModel(): RecalculoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        RecalculoViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.recalculoDeXp,
        )
    }
}

@Composable
private fun ajustesViewModel(): AjustesViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        AjustesViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun AjustesPreview(estado: AjustesUiState) {
    PokerunTheme {
        AjustesScreen(
            estado = estado,
            aoMudarNome = {},
            aoMudarBaseline = {},
            aoSalvarPerfil = {},
            aoPedirParaSair = {},
            aoConfirmarSaida = {},
            aoDesistirDeSair = {},
        )
    }
}

private val COM_PERFIL = AjustesUiState(
    carregando = false,
    nome = "Hiago",
    baseline = "7,5",
)

@Preview(name = "perfil", showBackground = true)
@Composable
private fun AjustesComPerfilPreview() = AjustesPreview(COM_PERFIL)

@Preview(name = "perfil acusado", showBackground = true)
@Composable
private fun AjustesComErroPreview() = AjustesPreview(
    COM_PERFIL.copy(
        nome = "",
        baseline = "1e3",
        erros = ErrosDoPerfil(
            nome = R.string.ajustes_erro_nome,
            baseline = R.string.ajustes_erro_baseline,
        ),
        podeSalvar = true,
    ),
)

@Preview(name = "perfil em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun AjustesAmpliadoPreview() = AjustesPreview(COM_PERFIL)
