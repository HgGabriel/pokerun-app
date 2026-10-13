package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.Ficha
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

/**
 * O cadastro (`F1-T08`, docs/03 §3.2).
 *
 * **Um passo só:** nome e maior distância confortável. Salvar grava `users/{uid}` e a
 * tela sai para a casca. As corridas entram depois, pelo registro manual (D-20).
 *
 * **Não há botão de voltar**, e a ausência é deliberada: a rota é uma porta
 * (`NavegacaoDoApp`), e antes de o perfil ser gravado não há o que desfazer.
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

    OnboardingScreen(
        estado = estado,
        aoMudarNome = vm::nomeMudou,
        aoMudarDistancia = vm::distanciaMudou,
        aoSalvarPerfil = vm::salvarPerfil,
        modifier = modifier,
    )
}

/**
 * A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam.
 *
 * Nenhum preview do app depende de rede nem de Firestore.
 */
@Composable
fun OnboardingScreen(
    estado: OnboardingUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarDistancia: (String) -> Unit,
    aoSalvarPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O perfil desenhado: `Salvando` o mantém por baixo, e é só o botão que troca de
    // conteúdo (docs/02 §8, item 6). Em `Concluido` a tela já está saindo, e desenhar
    // qualquer coisa seria um quadro piscando.
    val perfil = when (estado) {
        is OnboardingUiState.Perfil -> estado
        is OnboardingUiState.Salvando -> estado.passo
        OnboardingUiState.Concluido -> null
    }
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
            // Sem índice `n de N` na sobrancelha: a regra de docs/02 §10.3 pede um total
            // que o usuário reconheça, e o cadastro é um passo só.
            CabecalhoDeFicha(
                sobrancelha = listOf(
                    stringResource(R.string.onboarding_sobrancelha),
                    stringResource(R.string.onboarding_nivel_perfil),
                ),
                titulo = stringResource(R.string.onboarding_perfil_titulo),
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                if (perfil != null) {
                    CorpoDoPerfil(
                        passo = perfil,
                        salvando = salvando,
                        aoMudarNome = aoMudarNome,
                        aoMudarDistancia = aoMudarDistancia,
                        aoSalvarPerfil = aoSalvarPerfil,
                    )
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }
        }
    }
}

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

/**
 * O botão de ação, com o indicador por dentro quando a escrita está em curso.
 *
 * `enabled = false` está certo enquanto grava — o botão não aceita um segundo toque, e
 * o TalkBack precisa saber —, mas a paleta desabilitada do Material não: ela pinta o
 * botão de um cinza que não é token deste projeto. É o mesmo defeito que o emulador
 * pegou na `LoginScreen` em 13/08. Ocupado mantém a cor e troca só o conteúdo.
 */
@Composable
private fun BotaoPrincipal(
    rotulo: Int,
    aoTocar: () -> Unit,
    ocupado: Boolean,
) {
    Button(
        onClick = aoTocar,
        enabled = !ocupado,
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
        )
    }
}

@Preview(name = "perfil", showBackground = true)
@Composable
private fun PerfilPreview() = OnboardingPreview(OnboardingUiState.Perfil())

@Preview(name = "perfil com erro", showBackground = true)
@Composable
private fun PerfilComErroPreview() = OnboardingPreview(
    OnboardingUiState.Perfil(
        nome = "",
        distancia = "cinco",
        erroNoNome = R.string.onboarding_erro_nome,
        erroNaDistancia = R.string.onboarding_erro_distancia,
    ),
)

@Preview(name = "salvando", showBackground = true)
@Composable
private fun SalvandoPerfilPreview() = OnboardingPreview(
    OnboardingUiState.Salvando(OnboardingUiState.Perfil(nome = "Hiago", distancia = "7,5")),
)

@Preview(name = "perfil em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun PerfilFonteGrandePreview() = OnboardingPreview(OnboardingUiState.Perfil())
