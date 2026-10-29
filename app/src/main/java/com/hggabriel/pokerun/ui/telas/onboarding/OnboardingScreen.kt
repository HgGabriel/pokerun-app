package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Treinador
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BustoDoTreinador
import com.hggabriel.pokerun.ui.componentes.COLUNAS_DE_TREINADOR
import com.hggabriel.pokerun.ui.componentes.CampoDeBuscaDeTreinador
import com.hggabriel.pokerun.ui.componentes.IconesClarosNaBarraDeStatus
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.ResultadoDaBusca
import com.hggabriel.pokerun.ui.componentes.SpriteDoTreinador
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.filtrarKm
import com.hggabriel.pokerun.ui.componentes.gradeDeTreinadores
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Raios
import kotlinx.coroutines.launch

/** O diâmetro do indicador dentro do botão, para não empurrar a altura dele. */
private val IndicadorNoBotao = 20.dp

/** O lugar do treinador na licença aberta: o corpo inteiro de 80dp e a folga do carimbo. */
private val VagaDoTreinador = 96.dp

/** O busto do treinador na licença encolhida. */
private val BustoDaLicenca = 44.dp

private const val OPACIDADE_DO_ROTULO = 0.72f
private const val OPACIDADE_DA_DICA = 0.4f
private const val OPACIDADE_DO_BRILHO = 0.6f
private const val OPACIDADE_DA_POKEBOLA = 0.08f
private const val OPACIDADE_DA_SILHUETA = 0.22f

/** O carimbo do treinador escolhido: entra maior e assenta. */
private const val ESCALA_DO_CARIMBO = 1.25f

/** A forma da licença: sangra no topo e arredonda embaixo. */
private val FormaDaLicenca = RoundedCornerShape(bottomStart = Raios.especie, bottomEnd = Raios.especie)

/** Os gestos da tela, num pacote só, para os previews passarem nada. */
@Immutable
class AcoesDoOnboarding(
    val aoMudarNome: (String) -> Unit = {},
    val aoMudarDistancia: (String) -> Unit = {},
    val aoSairDaDistancia: () -> Unit = {},
    val aoBuscar: (String) -> Unit = {},
    val aoEscolherTreinador: (String) -> Unit = {},
    val aoEmitir: () -> Unit = {},
)

/**
 * O cadastro (`F1-T08`, docs/03 §3.2): desde 29/10/2026, a emissão da licença de treinador.
 *
 * **A licença é a peça em tinta do topo** e se preenche enquanto a pessoa digita: o nome e a
 * distância são digitados nela, e o treinador escolhido na grade entra nela com um carimbo.
 * Embaixo, a busca e a grade dos 1.500 treinadores, com os três grupos curados à vista antes
 * de qualquer tecla. Com a busca em foco ou a grade rolada, a licença encolhe para uma linha
 * e continua mostrando o que já foi preenchido. O botão de emitir só liga com a licença
 * completa ([fichaCompleta]).
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
    val termo by vm.busca.termo.collectAsStateWithLifecycle()
    val resultado by vm.busca.resultado.collectAsStateWithLifecycle()

    LaunchedEffect(estado) {
        if (estado is OnboardingUiState.Concluido) aoConcluir()
    }

    val acoes = remember(vm) {
        AcoesDoOnboarding(
            aoMudarNome = vm::nomeMudou,
            aoMudarDistancia = vm::distanciaMudou,
            aoSairDaDistancia = vm::distanciaSaiu,
            aoBuscar = vm.busca::mudou,
            aoEscolherTreinador = vm::escolherTreinador,
            aoEmitir = vm::salvarPerfil,
        )
    }
    // `resultado` entra na conta: a licença só fica completa depois que a lista chegou.
    val completa = (estado as? OnboardingUiState.Perfil)?.let { resultado.total > 0 && vm.completa(it) } ?: false

    OnboardingScreen(estado, termo, resultado, completa, acoes, modifier)
}

/**
 * A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam.
 *
 * Nenhum preview do app depende de rede nem de Firestore.
 */
@Composable
fun OnboardingScreen(
    estado: OnboardingUiState,
    termo: String,
    resultado: ResultadoDaBusca,
    completa: Boolean,
    acoes: AcoesDoOnboarding,
    modifier: Modifier = Modifier,
) {
    // `Salvando` mantém a licença por baixo, e é só o botão que troca de conteúdo
    // (docs/02 §8, item 6). Em `Concluido` a tela já está saindo.
    val passo = when (estado) {
        is OnboardingUiState.Perfil -> estado
        is OnboardingUiState.Salvando -> estado.passo
        OnboardingUiState.Concluido -> null
    }
    val salvando = estado is OnboardingUiState.Salvando

    IconesClarosNaBarraDeStatus()

    val grade = rememberLazyGridState()
    var buscaComFoco by remember { mutableStateOf(false) }
    val rolada by remember { derivedStateOf { grade.firstVisibleItemIndex > 0 } }
    val escopo = rememberCoroutineScope()
    val foco = LocalFocusManager.current
    // Escolher fecha o teclado: a licença volta a abrir e mostra o carimbo do treinador.
    val aoEscolher = remember(acoes, foco) {
        { nome: String ->
            foco.clearFocus()
            acoes.aoEscolherTreinador(nome)
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        if (passo == null) return@Surface
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Licenca(
                passo = passo,
                compacta = buscaComFoco || rolada,
                habilitada = !salvando,
                acoes = acoes,
                aoAbrir = {
                    foco.clearFocus()
                    escopo.launch { grade.animateScrollToItem(0) }
                },
            )

            Column(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                ErroDaLicenca(passo)
                Spacer(Modifier.height(Espacos.interno))
                CampoDeBuscaDeTreinador(termo, resultado.total, acoes.aoBuscar, aoMudarFoco = { buscaComFoco = it })
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUNAS_DE_TREINADOR),
                state = grade,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = Espacos.margem, end = Espacos.margem, top = Espacos.relacionado, bottom = Espacos.margem),
                horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
                verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
            ) {
                gradeDeTreinadores(resultado.achados, passo.treinador, aoEscolher)
            }

            BarraDeEmissao(completa = completa, salvando = salvando, erro = passo.erroAoGravar, aoEmitir = acoes.aoEmitir)
        }
    }
}

// ---------------------------------------------------------------------------
// A licença
// ---------------------------------------------------------------------------

/**
 * A licença em tinta: o brilho em `destaque` atrás do treinador e a Pokébola em marca d'água,
 * como no `BannerEscuro`, mas sangrando sob a barra de status, como a arena do registro.
 */
@Composable
private fun Licenca(
    passo: OnboardingUiState.Perfil,
    compacta: Boolean,
    habilitada: Boolean,
    acoes: AcoesDoOnboarding,
    aoAbrir: () -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val brilho = MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current

    // O carimbo vive aqui, e não na figura: encolher e abrir a licença não carimba de novo.
    val carimbo = remember { Animatable(1f) }
    LaunchedEffect(passo.treinador) {
        if (passo.treinador != null && !reduzido) {
            carimbo.snapTo(ESCALA_DO_CARIMBO)
            carimbo.animateTo(1f, Movimento.pop())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaDaLicenca)
            .background(tinta)
            .drawBehind {
                drawRect(Brush.horizontalGradient(0.35f to Color.Transparent, 1f to brilho.copy(alpha = OPACIDADE_DO_BRILHO)))
                val raio = minOf(size.height * 0.62f, 120.dp.toPx())
                val centro = Offset(size.width - raio * 0.45f, size.height - raio * 0.55f)
                val traco = raio * 0.14f
                val marca = claro.copy(alpha = OPACIDADE_DA_POKEBOLA)
                drawCircle(marca, raio, centro, style = Stroke(traco))
                drawLine(marca, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), traco)
                drawCircle(marca, raio * 0.3f, centro, style = Stroke(traco))
            }
            .statusBarsPadding()
            .animateContentSize(Movimento.padrao(reduzido)),
    ) {
        if (compacta) {
            LicencaEncolhida(passo, aoAbrir, escala = { carimbo.value })
        } else {
            LicencaAberta(passo, habilitada, acoes, escala = { carimbo.value })
        }
    }
}

@Composable
private fun LicencaAberta(
    passo: OnboardingUiState.Perfil,
    habilitada: Boolean,
    acoes: AcoesDoOnboarding,
    escala: () -> Float,
) {
    val claro = MaterialTheme.colorScheme.surface
    Column(modifier = Modifier.padding(start = Espacos.margem, end = Espacos.relacionado, top = Espacos.relacionado, bottom = Espacos.margem)) {
        RotuloDaLicenca(stringResource(R.string.onboarding_licenca))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                CampoDoNome(passo.nome, acoes.aoMudarNome, habilitada)
                Spacer(Modifier.height(Espacos.interno))
                RotuloDaLicenca(stringResource(R.string.onboarding_campo_distancia))
                CampoDaDistancia(passo.distancia, acoes.aoMudarDistancia, acoes.aoSairDaDistancia, habilitada)
            }
            TreinadorDaLicenca(passo.treinador, escala, claro)
        }
    }
}

/**
 * A licença numa linha: o rótulo, o nome, a distância e o busto. Um nó só para o TalkBack, e
 * o toque a abre de novo (sobe a grade e tira o foco da busca).
 */
@Composable
private fun LicencaEncolhida(passo: OnboardingUiState.Perfil, aoAbrir: () -> Unit, escala: () -> Float) {
    val claro = MaterialTheme.colorScheme.surface
    val semEscolha = stringResource(R.string.perfil_sem_escolha)
    val nome = passo.nome.trim().ifEmpty { stringResource(R.string.perfil_nome_vazio) }
    val falada = stringResource(
        R.string.onboarding_licenca_falada,
        nome,
        passo.distancia.ifBlank { "0" },
        passo.treinador?.let(Treinador::rotulo) ?: semEscolha,
    )
    val editar = stringResource(R.string.onboarding_editar_licenca)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = editar, onClick = aoAbrir)
            .clearAndSetSemantics {
                contentDescription = falada
                role = Role.Button
            }
            .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RotuloDaLicenca(stringResource(R.string.onboarding_licenca))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = nome,
                    style = Estilos.tituloSecao.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = if (passo.nome.isBlank()) claro.copy(alpha = OPACIDADE_DA_DICA) else claro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (passo.distancia.isNotBlank()) {
                    Spacer(Modifier.width(Espacos.relacionado))
                    Text(passo.distancia, style = Estilos.numeroS, color = claro)
                    Text(
                        text = stringResource(R.string.onboarding_campo_distancia_unidade),
                        style = Estilos.legenda,
                        color = claro.copy(alpha = OPACIDADE_DO_ROTULO),
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(Espacos.interno))
        BustoDoTreinador(
            treinador = passo.treinador,
            tamanho = BustoDaLicenca,
            fundo = claro,
            modifier = Modifier.graphicsLayer {
                scaleX = escala()
                scaleY = escala()
            },
        )
    }
}

@Composable
private fun RotuloDaLicenca(texto: String) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = Estilos.rotulo,
        color = MaterialTheme.colorScheme.surface.copy(alpha = OPACIDADE_DO_ROTULO),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** O nome digitado no lugar, em 30sp, como o nome da credencial do Perfil. */
@Composable
private fun CampoDoNome(nome: String, aoMudar: (String) -> Unit, habilitado: Boolean) {
    val claro = MaterialTheme.colorScheme.surface
    val estilo = Estilos.tituloTela.copy(fontSize = 30.sp, lineHeight = 36.sp, color = claro)
    val rotulo = stringResource(R.string.onboarding_campo_nome)
    BasicTextField(
        value = nome,
        onValueChange = aoMudar,
        enabled = habilitado,
        singleLine = true,
        textStyle = estilo,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = rotulo },
        decorationBox = { campo ->
            Box {
                if (nome.isEmpty()) {
                    Text(stringResource(R.string.perfil_nome_vazio), style = estilo, color = claro.copy(alpha = OPACIDADE_DA_DICA))
                }
                campo()
            }
        },
    )
}

/**
 * A distância digitada no lugar, em `numero-l` (docs/02 §1: quando o número é a entrada, ele
 * é o maior texto). O campo tem a largura do que foi digitado, e o `km` vem colado.
 */
@Composable
private fun CampoDaDistancia(distancia: String, aoMudar: (String) -> Unit, aoSair: () -> Unit, habilitado: Boolean) {
    val claro = MaterialTheme.colorScheme.surface
    val foco = LocalFocusManager.current
    val estilo = Estilos.numeroL.copy(color = claro)
    val rotulo = stringResource(R.string.onboarding_campo_distancia)
    val medidor = rememberTextMeasurer()
    val largura = with(LocalDensity.current) {
        medidor.measure(distancia.ifEmpty { "0" }, estilo).size.width.toDp() + 4.dp
    }
    var teveFoco by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = distancia,
            onValueChange = { aoMudar(filtrarKm(it)) },
            enabled = habilitado,
            singleLine = true,
            textStyle = estilo,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            // Decimal: a resposta é 7,5 tanto quanto 7, e quem corre 800 m responde 0,8.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
            modifier = Modifier
                .widthIn(min = largura)
                .width(largura)
                .onFocusChanged {
                    if (it.isFocused) {
                        teveFoco = true
                    } else if (teveFoco) {
                        aoSair()
                    }
                }
                .semantics { contentDescription = rotulo },
            decorationBox = { campo ->
                Box {
                    if (distancia.isEmpty()) Text("0", style = estilo, color = claro.copy(alpha = OPACIDADE_DA_DICA))
                    campo()
                }
            },
        )
        Text(
            text = stringResource(R.string.onboarding_campo_distancia_unidade),
            style = Estilos.corpoForte,
            color = claro.copy(alpha = OPACIDADE_DO_ROTULO),
            modifier = Modifier.padding(start = Espacos.minimo, bottom = 6.dp),
        )
    }
}

/**
 * O treinador na licença aberta: a silhueta clara antes da escolha; escolhido, o corpo inteiro
 * de 80dp com o rótulo embaixo, carimbado na entrada.
 */
@Composable
private fun TreinadorDaLicenca(treinador: String?, escala: () -> Float, claro: Color) {
    val rotulo = treinador?.let(Treinador::rotulo)
    val falado = stringResource(R.string.onboarding_treinador_falado, rotulo ?: stringResource(R.string.perfil_sem_escolha))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(VagaDoTreinador)
            .clearAndSetSemantics { contentDescription = falado },
    ) {
        Box(
            modifier = Modifier
                .size(VagaDoTreinador)
                .graphicsLayer {
                    scaleX = escala()
                    scaleY = escala()
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (treinador == null) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = claro.copy(alpha = OPACIDADE_DA_SILHUETA),
                    modifier = Modifier.size(80.dp),
                )
            } else {
                SpriteDoTreinador(treinador)
            }
        }
        Text(
            text = rotulo.orEmpty(),
            style = Estilos.legenda,
            color = claro,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * O erro da licença, embaixo dela: a distância inválida depois que o campo perdeu o foco, ou
 * o que a segunda trava do `ViewModel` acusou. Nunca no meio da digitação.
 */
@Composable
private fun ErroDaLicenca(passo: OnboardingUiState.Perfil) {
    val invalida = passo.distanciaTocada && passo.distancia.isNotBlank() && distanciaEmKm(passo.distancia) == null
    val erro = passo.erroNoNome ?: passo.erroNaDistancia ?: R.string.onboarding_erro_distancia.takeIf { invalida } ?: return
    Spacer(Modifier.height(Espacos.interno))
    BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(erro))
}

// ---------------------------------------------------------------------------
// A emissão
// ---------------------------------------------------------------------------

/**
 * O botão de emitir, preso embaixo e acima do teclado. Desligado até a licença ficar completa;
 * gravando, fica `ocupado` com o indicador por dentro. O erro de gravação fica logo acima dele,
 * pelo canal de alerta (docs/02 §2.4): nenhum campo está errado quando a rede cai.
 */
@Composable
private fun BarraDeEmissao(completa: Boolean, salvando: Boolean, erro: Int?, aoEmitir: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Espacos.margem, vertical = Espacos.interno),
    ) {
        if (erro != null) {
            BannerDeAlerta(rotulo = stringResource(R.string.alerta_falha_ao_salvar), texto = stringResource(erro))
            Spacer(Modifier.height(Espacos.interno))
        }
        BotaoPrimario(
            aoTocar = aoEmitir,
            habilitado = completa,
            ocupado = salvando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (salvando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(IndicadorNoBotao)
                        .clearAndSetSemantics {},
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(text = stringResource(R.string.onboarding_emitir))
            }
        }
    }
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
            container.treinadoresRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingPreview(estado: OnboardingUiState, completa: Boolean = false) {
    PokerunTheme {
        OnboardingScreen(
            estado = estado,
            termo = "",
            resultado = ResultadoDaBusca(total = 1_500),
            completa = completa,
            acoes = AcoesDoOnboarding(),
        )
    }
}

@Preview(name = "licença em branco", showBackground = true)
@Composable
private fun LicencaEmBrancoPreview() = OnboardingPreview(OnboardingUiState.Perfil())

@Preview(name = "licença completa", showBackground = true)
@Composable
private fun LicencaCompletaPreview() = OnboardingPreview(
    OnboardingUiState.Perfil(nome = "Hiago", distancia = "7,5", treinador = "jogger"),
    completa = true,
)

@Preview(name = "distância inválida", showBackground = true)
@Composable
private fun DistanciaInvalidaPreview() = OnboardingPreview(
    OnboardingUiState.Perfil(nome = "Hiago", distancia = "0", distanciaTocada = true),
)

@Preview(name = "emitindo", showBackground = true)
@Composable
private fun EmitindoPreview() = OnboardingPreview(
    OnboardingUiState.Salvando(OnboardingUiState.Perfil(nome = "Hiago", distancia = "7,5", treinador = "red")),
    completa = true,
)

@Preview(name = "licença em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun LicencaFonteGrandePreview() = OnboardingPreview(OnboardingUiState.Perfil(nome = "Hiago", distancia = "7,5"))
