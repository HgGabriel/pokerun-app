package com.hggabriel.pokerun.ui.telas.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** O piso de toque de docs/02 §8, item 2. `ButtonDefaults.MinHeight` é 40dp. */
private val AlturaDoBotao = 48.dp

/** O diâmetro do indicador dentro do botão, para não empurrar a altura dele. */
private val IndicadorNoBotao = 20.dp

/**
 * A porta de entrada do app (`F1-T06`, docs/03 §3.1).
 *
 * **Mínima por decisão de escopo**, não por falta de tempo: a Fase 1 tem 11 telas em
 * 14 dias e esta é uma marca, uma frase e um botão. Não enriquecer.
 *
 * Duas ausências que valem explicar, porque parecem esquecimento:
 *
 * - **Não há logo.** A arte é `F0-T15`, que é 👤 e depende de ilustração; até lá a
 *   marca é a palavra, no degrau de display. Trocar por um símbolo qualquer seria
 *   inventar identidade visual.
 * - **Não há skeleton.** docs/02 §8, item 6 proíbe spinner centralizado *em estado
 *   de carregamento de conteúdo* — a forma do conteúdo é que deve aparecer. Aqui não
 *   há conteúdo carregando: o que espera é uma ação do usuário, e o indicador vive
 *   dentro do próprio botão que ele tocou, que é onde a resposta é esperada.
 *
 * O erro não tem botão próprio. O mesmo botão é o "repetir" de docs/02 §8, item 7 —
 * dois controles para a mesma ação só fariam o usuário escolher entre sinônimos.
 */
@Composable
fun LoginScreen(
    aoEntrarComPerfil: (uid: String) -> Unit,
    aoEntrarSemPerfil: (uid: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: LoginViewModel = loginViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    // A folha de contas precisa da Activity viva, não do contexto da aplicação.
    val activity = LocalContext.current.comoActivity()

    LaunchedEffect(estado) {
        val autenticado = estado as? LoginUiState.Autenticado ?: return@LaunchedEffect
        if (autenticado.temPerfil) {
            aoEntrarComPerfil(autenticado.uid)
        } else {
            aoEntrarSemPerfil(autenticado.uid)
        }
    }

    LoginScreen(
        estado = estado,
        aoTocarEntrar = { activity?.let(vm::entrar) },
        modifier = modifier,
    )
}

/**
 * A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam.
 *
 * A separação existe para que os quatro estados sejam desenháveis sem Firebase por
 * perto — nenhum preview do app pode depender de rede.
 */
@Composable
fun LoginScreen(
    estado: LoginUiState,
    aoTocarEntrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                // A tela rola desde `F1-T06b`, e não por precaução: a `fontScale` 2,0
                // em 320dp o bloco de erro passava da borda de baixo e a última linha
                // — *"Adicione uma nos Ajustes do sistema e volte"*, que é a instrução
                // de recuperação — ficava fora do alcance. Sem rolagem não havia como
                // chegar nela. Com `Arrangement.Center` a tela continua centrada
                // enquanto couber, e só passa a rolar quando não couber.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = aoTocarEntrar,
                enabled = estado !is LoginUiState.Entrando,
                // `enabled = false` está certo — o botão não aceita toque enquanto a
                // folha de contas está aberta, e o TalkBack precisa saber disso —,
                // mas a paleta desabilitada do Material, não. Ela pinta o botão de
                // um cinza que não é token deste projeto, e o `Entrando` apareceu
                // cinza no emulador, sem contraste medido contra `Papel`.
                //
                // Os 38% de opacidade de docs/02 §4.2 também não servem aqui: eles
                // são para controle **indisponível**, e este está **ocupado**. Um
                // indicador de progresso a 38% é justamente o que ninguém enxerga.
                // O botão mantém a cor e troca só o conteúdo.
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AlturaDoBotao),
            ) {
                if (estado is LoginUiState.Entrando) {
                    // O rótulo continua no lugar por baixo da semântica: quem usa
                    // leitor de tela ouve a ação, não "indicador de progresso".
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(IndicadorNoBotao)
                            .clearAndSetSemantics {},
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = stringResource(R.string.login_entrar))
                }
            }

            if (estado is LoginUiState.Erro) {
                Spacer(Modifier.height(16.dp))
                // O erro sai pelo canal de docs/02 §2.4 desde `F1-T06b`. Antes era um
                // `Text` em `alerta` e centralizado: cor sozinha, sem nenhuma das três
                // peças, e centralizar duas linhas de recuperação ainda cortava a
                // leitura. O bloco alinha à esquerda porque o filete mora lá.
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_falha_ao_entrar),
                    texto = stringResource(estado.mensagem),
                )
            }
        }
    }
}

/**
 * A `Activity` por trás do [LocalContext], ou nulo num preview.
 *
 * **Não é `LocalActivity`** porque ele só existe a partir de `activity-compose`
 * 1.9, e a versão do projeto é 1.8.0 — subir dependência é escopo da lista da Fase 0
 * (`F0-T03`), não de uma tela. Desembrulhar o `ContextWrapper` é o caminho de sempre
 * e não acrescenta nada ao build.
 */
private tailrec fun Context.comoActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.comoActivity()
    else -> null
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e **nunca** enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun loginViewModel(): LoginViewModel {
    val container = LocalAppContainer.current
    return androidx.lifecycle.viewmodel.compose.viewModel {
        LoginViewModel(container.autenticacaoRepositorio, container.usuarioRepositorio)
    }
}

@Preview(name = "Ocioso", showBackground = true)
@Composable
private fun LoginOciosoPreview() {
    PokerunTheme { LoginScreen(estado = LoginUiState.Ocioso, aoTocarEntrar = {}) }
}

@Preview(name = "Entrando", showBackground = true)
@Composable
private fun LoginEntrandoPreview() {
    PokerunTheme { LoginScreen(estado = LoginUiState.Entrando, aoTocarEntrar = {}) }
}

@Preview(name = "Erro", showBackground = true)
@Composable
private fun LoginErroPreview() {
    PokerunTheme {
        LoginScreen(
            estado = LoginUiState.Erro(R.string.login_erro_sem_conta),
            aoTocarEntrar = {},
        )
    }
}

@Preview(name = "Erro em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun LoginFonteGrandePreview() {
    PokerunTheme {
        LoginScreen(
            estado = LoginUiState.Erro(R.string.login_erro_generico),
            aoTocarEntrar = {},
        )
    }
}
