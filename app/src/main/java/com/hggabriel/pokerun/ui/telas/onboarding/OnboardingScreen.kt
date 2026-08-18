package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.healthconnect.OrigemDeTreino
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A goteira do corpo, a mesma do cabeçalho de ficha, para o texto cair na mesma coluna. */
private val Goteira = 16.dp

/** O piso de toque de docs/02 §8, item 2. Botão do Material nasce com 40dp. */
private val AlturaDoBotao = 48.dp

/** O diâmetro do indicador dentro do botão, para não empurrar a altura dele. */
private val IndicadorNoBotao = 20.dp

private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val RecheioDaFicha = 16.dp

/** Quantos blocos o esqueleto do passo 4 desenha. Uma pessoa tem 1 ou 2 origens. */
private const val BLOCOS_DO_ESQUELETO = 2

/** Altura de um bloco do esqueleto: a de uma linha de origem, com rótulo e contagem. */
private val AlturaDoEsqueleto = 72.dp

/**
 * O cadastro (`F1-T08`, docs/03 §3.2).
 *
 * **Cinco passos em ordem rígida:** nome, distância confortável, permissão do Health
 * Connect, leitura dos últimos 30 dias e escolha da fonte canônica. A ordem não é
 * apresentação: não dá para listar quem grava no Health Connect antes de pedir
 * permissão e ler dele (`EXECUCAO.md §8`, item 9). Quem a executa é
 * `PassosDoOnboarding.kt`, com teste.
 *
 * **Dois dos cinco podem não acontecer.** Sem Health Connect no aparelho, os passos 3 a
 * 5 somem e o cadastro acaba no passo 2, sem tela de erro — indisponível é o modo
 * manual, caminho previsto de docs/05 §4.4. E `Nenhuma corrida encontrada` no passo 4
 * também não é falha: é o aparelho sem treino gravado, e a saída é
 * `Continuar sem sincronização`.
 *
 * **Não há botão de voltar entre os passos**, e a ausência é deliberada: a rota é uma
 * porta (`NavegacaoDoApp`), o perfil é gravado ao fim do passo 2 e os passos seguintes
 * não têm o que desfazer. Voltar do passo 4 para o 3 pediria de novo uma permissão que
 * o sistema já respondeu.
 */
@Composable
fun OnboardingScreen(
    aoConcluir: () -> Unit,
    modifier: Modifier = Modifier,
    vm: OnboardingViewModel = onboardingViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado) {
        if (estado is OnboardingUiState.Concluido) aoConcluir()
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { vm.permissaoRespondida() }

    OnboardingScreen(
        estado = estado,
        aoMudarNome = vm::nomeMudou,
        aoMudarDistancia = vm::distanciaMudou,
        aoSalvarPerfil = vm::salvarPerfil,
        aoConcederPermissao = { pedirPermissao.launch(vm.permissoesDeSaude) },
        aoTentarDeNovo = vm::tentarLerOrigens,
        aoEscolherOrigem = vm::escolherOrigem,
        aoUsarOrigem = vm::salvarFonte,
        aoSeguirSemSincronizacao = vm::seguirSemSincronizacao,
        modifier = modifier,
    )
}

/**
 * A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam.
 *
 * Nenhum preview do app depende de rede, de Firestore ou de Health Connect.
 */
@Composable
fun OnboardingScreen(
    estado: OnboardingUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarDistancia: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
    aoConcederPermissao: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoEscolherOrigem: (String) -> Unit,
    aoUsarOrigem: () -> Unit,
    aoSeguirSemSincronizacao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O passo desenhado: `Salvando` mantém por baixo o passo que está esperando o
    // servidor, e é só o botão que troca de conteúdo (docs/02 §8, item 6).
    val passo = (estado as? OnboardingUiState.Salvando)?.passo ?: estado
    val salvando = estado is OnboardingUiState.Salvando

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CabecalhoDeFicha(
                sobrancelha = listOf(
                    stringResource(R.string.onboarding_sobrancelha),
                    stringResource(nivelDoPasso(passo)),
                ),
                titulo = stringResource(tituloDoPasso(passo)),
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                when (passo) {
                    is OnboardingUiState.Perfil -> CorpoDoPerfil(
                        passo = passo,
                        salvando = salvando,
                        aoMudarNome = aoMudarNome,
                        aoMudarDistancia = aoMudarDistancia,
                        aoSalvarPerfil = aoSalvarPerfil,
                    )

                    is OnboardingUiState.SolicitandoPermissao -> CorpoDaPermissao(
                        passo = passo,
                        aoConcederPermissao = aoConcederPermissao,
                        aoSeguirSemSincronizacao = aoSeguirSemSincronizacao,
                    )

                    OnboardingUiState.LendoOrigens -> CorpoDaLeitura()

                    is OnboardingUiState.EscolhendoFonte -> CorpoDaFonte(
                        passo = passo,
                        salvando = salvando,
                        aoTentarDeNovo = aoTentarDeNovo,
                        aoEscolherOrigem = aoEscolherOrigem,
                        aoUsarOrigem = aoUsarOrigem,
                        aoSeguirSemSincronizacao = aoSeguirSemSincronizacao,
                    )

                    // Terminais: a tela já está saindo, e `Salvando` foi desembrulhado
                    // no início. Desenhar qualquer coisa aqui seria um quadro piscando.
                    OnboardingUiState.Concluido, is OnboardingUiState.Salvando -> Unit
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Passos 1 e 2 — o perfil
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.CorpoDoPerfil(
    passo: OnboardingUiState.Perfil,
    salvando: Boolean,
    aoMudarNome: (String) -> Unit,
    aoMudarDistancia: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
) {
    Ficha {
        Column(modifier = Modifier.padding(RecheioDaFicha)) {
            CampoComErro(
                valor = passo.nome,
                aoMudar = aoMudarNome,
                rotulo = R.string.onboarding_campo_nome,
                erro = passo.erroNoNome,
                habilitado = !salvando,
                opcoesDeTeclado = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(EspacoEntreBlocos))

            CampoComErro(
                valor = passo.distancia,
                aoMudar = aoMudarDistancia,
                rotulo = R.string.onboarding_campo_distancia,
                erro = passo.erroNaDistancia,
                apoio = R.string.onboarding_campo_distancia_apoio,
                habilitado = !salvando,
                sufixo = { Text(stringResource(R.string.onboarding_campo_distancia_unidade)) },
                // Decimal e não `Number`: a resposta é 7,5 tanto quanto 7, e o teclado
                // sem separador obrigaria a arredondar quem corre 800 m.
                opcoesDeTeclado = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }

    Spacer(Modifier.height(EspacoDepoisDoCabecalho))

    BotaoPrincipal(
        rotulo = R.string.onboarding_salvar_perfil,
        aoTocar = aoSalvarPerfil,
        ocupado = salvando,
    )
    MensagemDeErro(passo.erroAoGravar)
}

// ---------------------------------------------------------------------------
// Passo 3 — a permissão
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.CorpoDaPermissao(
    passo: OnboardingUiState.SolicitandoPermissao,
    aoConcederPermissao: () -> Unit,
    aoSeguirSemSincronizacao: () -> Unit,
) {
    Ficha {
        Text(
            text = stringResource(R.string.onboarding_permissao_corpo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(RecheioDaFicha),
        )
    }

    Spacer(Modifier.height(EspacoDepoisDoCabecalho))

    BotaoPrincipal(
        rotulo = R.string.onboarding_permissao_conceder,
        aoTocar = aoConcederPermissao,
        ocupado = false,
    )

    if (passo.negada) {
        Spacer(Modifier.height(EspacoEntreBlocos))
        // Em `tinta-fraca`, e não em `alerta`: negar a permissão é resposta do usuário,
        // não falha do app. O canal de alerta de docs/02 §2.4 é para risco.
        Text(
            text = stringResource(R.string.onboarding_permissao_negada),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SaidaSemSincronizacao(aoSeguirSemSincronizacao)
}

// ---------------------------------------------------------------------------
// Passo 4 — a leitura
// ---------------------------------------------------------------------------

/**
 * O esqueleto na forma do conteúdo (docs/02 §8, item 6), e não um spinner centralizado.
 *
 * Blocos estáticos em `borda`, sem brilho varrendo: shimmer é decoração animada e
 * contradiz "um momento animado por semana" (docs/02 §9.1.1).
 */
@Composable
private fun ColumnScope.CorpoDaLeitura() {
    repeat(BLOCOS_DO_ESQUELETO) { indice ->
        if (indice > 0) Spacer(Modifier.height(EspacoEntreBlocos))
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
}

// ---------------------------------------------------------------------------
// Passo 5 — a fonte canônica
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.CorpoDaFonte(
    passo: OnboardingUiState.EscolhendoFonte,
    salvando: Boolean,
    aoTentarDeNovo: () -> Unit,
    aoEscolherOrigem: (String) -> Unit,
    aoUsarOrigem: () -> Unit,
    aoSeguirSemSincronizacao: () -> Unit,
) {
    when {
        passo.falhouALeitura -> {
            Text(
                text = stringResource(R.string.onboarding_fonte_falhou),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            BotaoPrincipal(
                rotulo = R.string.onboarding_fonte_repetir,
                aoTocar = aoTentarDeNovo,
                ocupado = false,
            )
        }

        passo.origens.isEmpty() -> {
            // A copy é a de docs/03 §3.2, ao pé da letra. Estado vazio, não erro.
            Text(
                text = stringResource(R.string.onboarding_fonte_vazia),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        else -> {
            Text(
                text = stringResource(R.string.onboarding_fonte_corpo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoEntreBlocos))

            passo.origens.forEach { origem ->
                LinhaDeOrigem(
                    origem = origem,
                    selecionada = origem.pacote == passo.escolhida,
                    habilitada = !salvando,
                    aoTocar = { aoEscolherOrigem(origem.pacote) },
                )
            }

            Spacer(Modifier.height(EspacoEntreBlocos))
            BotaoPrincipal(
                rotulo = R.string.onboarding_fonte_usar,
                aoTocar = aoUsarOrigem,
                ocupado = salvando,
                habilitado = passo.escolhida != null,
            )
            MensagemDeErro(passo.erroAoGravar)
        }
    }

    SaidaSemSincronizacao(aoSeguirSemSincronizacao)
}

/**
 * Uma origem da lista: o nome que o usuário reconhece e quantas corridas ela gravou.
 *
 * **Selecionada não é só a cor** (docs/02 §4.2): a `Ficha` já troca fundo e borda, e a
 * marca de seleção entra aqui. Para o TalkBack, a linha inteira é um item selecionável
 * — `selected` no nó pai —, e o glifo não é anunciado duas vezes.
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
                        R.plurals.onboarding_fonte_corridas,
                        origem.corridas,
                        origem.corridas,
                    ),
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selecionada) {
                Icon(
                    imageVector = Icons.Default.Check,
                    // O `selected` do nó pai já diz ao TalkBack o que a marca significa.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Peças comuns aos passos
// ---------------------------------------------------------------------------

/**
 * O botão de ação do passo, com o indicador por dentro quando a escrita está em curso.
 *
 * `enabled = false` está certo enquanto grava — o botão não aceita um segundo toque, e
 * o TalkBack precisa saber —, mas a paleta desabilitada do Material não: ela pinta o
 * botão de um cinza que não é token deste projeto. É o mesmo defeito que o emulador
 * pegou na `LoginScreen` em 13/08. Ocupado mantém a cor e troca só o conteúdo;
 * **indisponível**, que é outra coisa, cai nos 38% de docs/02 §4.2 por meio do
 * `enabled` do Material.
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

/**
 * `Continuar sem sincronização` (docs/03 §3.2).
 *
 * Fica em todos os passos do Health Connect, e não só no vazio: o Android bloqueia
 * permissão de saúde depois de duas negativas e a folha para de abrir, então sem uma
 * saída visível o cadastro travaria num botão que deixou de fazer efeito.
 */
@Composable
private fun ColumnScope.SaidaSemSincronizacao(aoTocar: () -> Unit) {
    Spacer(Modifier.height(EspacoEntreBlocos))
    TextButton(
        onClick = aoTocar,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(text = stringResource(R.string.onboarding_sem_sincronizacao))
    }
}

/**
 * O erro de gravação, embaixo do botão que o disparou. Nada quando não houver.
 *
 * **Não é erro de campo**, e por isso não passa por `CampoComErro`: nenhum dos dois
 * campos está errado quando a rede cai. É falha de tela, e o canal de docs/02 §2.4
 * para falha de tela é o [BannerDeAlerta] direto.
 */
@Composable
private fun ColumnScope.MensagemDeErro(mensagem: Int?) {
    if (mensagem == null) return
    Spacer(Modifier.height(EspacoEntreBlocos))
    BannerDeAlerta(
        rotulo = stringResource(R.string.alerta_falha_ao_salvar),
        texto = stringResource(mensagem),
    )
}

// ---------------------------------------------------------------------------
// O cabeçalho de cada passo (docs/02 §10.1)
// ---------------------------------------------------------------------------

/**
 * O segundo nível da sobrancelha: a profundidade em que o usuário está.
 *
 * **Sem índice `n de N`.** A regra de docs/02 §10.3 exige um total que o usuário
 * reconheça, e aqui o total muda com o aparelho: quem não tem Health Connect vê um
 * passo, quem tem vê três. `PASSO 1 DE 3` para uns e `1 DE 1` para outros seria número
 * ornamental, que é exatamente o que a regra proíbe.
 */
private fun nivelDoPasso(passo: OnboardingUiState): Int = when (passo) {
    is OnboardingUiState.Perfil -> R.string.onboarding_nivel_perfil
    is OnboardingUiState.SolicitandoPermissao, OnboardingUiState.LendoOrigens ->
        R.string.onboarding_nivel_saude
    else -> R.string.onboarding_nivel_origem
}

private fun tituloDoPasso(passo: OnboardingUiState): Int = when (passo) {
    is OnboardingUiState.Perfil -> R.string.onboarding_perfil_titulo
    is OnboardingUiState.SolicitandoPermissao -> R.string.onboarding_permissao_titulo
    OnboardingUiState.LendoOrigens -> R.string.onboarding_lendo_titulo
    else -> R.string.onboarding_fonte_titulo
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun onboardingViewModel(): OnboardingViewModel {
    val container = LocalAppContainer.current
    return androidx.lifecycle.viewmodel.compose.viewModel {
        OnboardingViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.saudeRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingPreview(estado: OnboardingUiState) {
    PokerunTheme {
        OnboardingScreen(
            estado = estado,
            aoMudarNome = {},
            aoMudarDistancia = {},
            aoSalvarPerfil = {},
            aoConcederPermissao = {},
            aoTentarDeNovo = {},
            aoEscolherOrigem = {},
            aoUsarOrigem = {},
            aoSeguirSemSincronizacao = {},
        )
    }
}

private val ORIGENS_DE_EXEMPLO = listOf(
    OrigemDeTreino("com.sec.android.app.shealth", "Samsung Health", 12),
    OrigemDeTreino("com.strava", "Strava", 8),
    OrigemDeTreino("com.dev.sem.rotulo", "com.dev.sem.rotulo", 1),
)

@Preview(name = "1 e 2 · perfil", showBackground = true)
@Composable
private fun PerfilPreview() = OnboardingPreview(OnboardingUiState.Perfil())

@Preview(name = "1 e 2 · perfil com erro", showBackground = true)
@Composable
private fun PerfilComErroPreview() = OnboardingPreview(
    OnboardingUiState.Perfil(
        nome = "",
        distancia = "cinco",
        erroNoNome = R.string.onboarding_erro_nome,
        erroNaDistancia = R.string.onboarding_erro_distancia,
    ),
)

@Preview(name = "1 e 2 · salvando", showBackground = true)
@Composable
private fun SalvandoPerfilPreview() = OnboardingPreview(
    OnboardingUiState.Salvando(OnboardingUiState.Perfil(nome = "Hiago", distancia = "7,5")),
)

@Preview(name = "3 · permissão", showBackground = true)
@Composable
private fun PermissaoPreview() = OnboardingPreview(OnboardingUiState.SolicitandoPermissao())

@Preview(name = "3 · permissão negada", showBackground = true)
@Composable
private fun PermissaoNegadaPreview() =
    OnboardingPreview(OnboardingUiState.SolicitandoPermissao(negada = true))

@Preview(name = "4 · lendo", showBackground = true)
@Composable
private fun LendoPreview() = OnboardingPreview(OnboardingUiState.LendoOrigens)

@Preview(name = "5 · origens", showBackground = true)
@Composable
private fun FontePreview() = OnboardingPreview(
    OnboardingUiState.EscolhendoFonte(origens = ORIGENS_DE_EXEMPLO, escolhida = "com.strava"),
)

@Preview(name = "5 · nenhuma origem", showBackground = true)
@Composable
private fun FonteVaziaPreview() = OnboardingPreview(OnboardingUiState.EscolhendoFonte())

@Preview(name = "5 · falha de leitura", showBackground = true)
@Composable
private fun FonteComFalhaPreview() =
    OnboardingPreview(OnboardingUiState.EscolhendoFonte(falhouALeitura = true))

@Preview(name = "5 · origens em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteFonteGrandePreview() = OnboardingPreview(
    OnboardingUiState.EscolhendoFonte(origens = ORIGENS_DE_EXEMPLO),
)

@Preview(name = "1 e 2 · perfil em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun PerfilFonteGrandePreview() = OnboardingPreview(OnboardingUiState.Perfil())
