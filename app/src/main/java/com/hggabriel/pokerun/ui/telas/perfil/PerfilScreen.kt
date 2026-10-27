package com.hggabriel.pokerun.ui.telas.perfil

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Insignias
import com.hggabriel.pokerun.dominio.regras.Treinador
import androidx.compose.ui.graphics.FilterQuality
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BustoDoTreinador
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.SpriteDoTreinador
import com.hggabriel.pokerun.ui.componentes.filtrarKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.FormaEspecie
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import kotlinx.coroutines.launch
import java.time.LocalDate

private val LarguraDaCredencial = 300.dp
private val AlturaDaFoto = 218.dp
private val RenderDoParceiro = 200.dp
private val AlturaDoCordao = 120.dp
private val TopoDaCredencial = 92.dp
private val AlturaDaLinha = 60.dp

/** A credencial pende do cordão inclinada (decisão 14): um objeto, e não um cartão de dado. */
private const val INCLINACAO = -2.5f

/** Quanto a credencial gira ao trocar de parceiro, antes da mola trazê-la de volta. */
private const val BALANCO = 6f

/** O teto do giro no arraste, para ela não virar de lado. */
private const val GIRO_MAXIMO = 10f

/**
 * O Perfil (estudo de Progresso e Perfil, Revisão 1 e 3): a credencial do corredor pendurada
 * num cordão e, abaixo, a lista de três cartões com as configurações gerais.
 *
 * **É destino de cada grafo de aba** (`F1-T07c`), alcançado pelo atalho do treinador na raiz
 * de toda aba. Vive dentro da casca e não leva `safeDrawingPadding` própria.
 *
 * **Sem peça em tinta** (decisão 12): a tinta com o parceiro em 3D é a assinatura da arena e
 * da estrada. A personalidade aqui vem do objeto: o cordão, a inclinação e a faixa no tipo.
 */
@Composable
fun PerfilScreen(
    aoSairDaConta: () -> Unit,
    aoEscolherParceiro: () -> Unit,
    aoEscolherTreinador: () -> Unit,
    modifier: Modifier = Modifier,
    vm: PerfilViewModel = perfilViewModel(),
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

    PerfilScreen(
        estado = estado,
        recalculo = recalculo,
        acoes = AcoesDoPerfil(
            aoMudarNome = vm::nomeMudou,
            aoConfirmarNome = vm::confirmarNome,
            aoEscolherParceiro = aoEscolherParceiro,
            aoEscolherTreinador = aoEscolherTreinador,
            aoAbrirBaseline = vm::abrirBaseline,
            aoMudarBaseline = vm::baselineMudou,
            aoConfirmarBaseline = vm::confirmarBaseline,
            aoFecharBaseline = vm::fecharBaseline,
            aoMudarSom = vm::somNaCapturaMudou,
            aoPedirRecalculo = recalculoVm::pedirConfirmacao,
            aoConfirmarRecalculo = recalculoVm::recalcular,
            aoCancelarRecalculo = recalculoVm::cancelar,
            aoPedirParaSair = vm::pedirParaSair,
            aoConfirmarSaida = vm::sair,
            aoDesistirDeSair = vm::desistirDeSair,
        ),
        modifier = modifier,
    )
}

/** As ações da tela, num pacote: são quinze, e a lista de parâmetros ficaria ilegível. */
data class AcoesDoPerfil(
    val aoMudarNome: (String) -> Unit = {},
    val aoConfirmarNome: () -> Unit = {},
    val aoEscolherParceiro: () -> Unit = {},
    val aoEscolherTreinador: () -> Unit = {},
    val aoAbrirBaseline: () -> Unit = {},
    val aoMudarBaseline: (String) -> Unit = {},
    val aoConfirmarBaseline: () -> Unit = {},
    val aoFecharBaseline: () -> Unit = {},
    val aoMudarSom: (Boolean) -> Unit = {},
    val aoPedirRecalculo: () -> Unit = {},
    val aoConfirmarRecalculo: () -> Unit = {},
    val aoCancelarRecalculo: () -> Unit = {},
    val aoPedirParaSair: () -> Unit = {},
    val aoConfirmarSaida: () -> Unit = {},
    val aoDesistirDeSair: () -> Unit = {},
)

/** A tela sem `ViewModel`, para os previews. */
@Composable
fun PerfilScreen(
    estado: PerfilUiState,
    recalculo: EstadoDoRecalculo,
    acoes: AcoesDoPerfil,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Sobrancelha()

            if (estado.erroDeTela != null) {
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_corrija),
                    texto = stringResource(estado.erroDeTela),
                    modifier = Modifier.padding(horizontal = Espacos.margem),
                )
            }

            PalcoDaCredencial(estado, acoes)

            Column(
                modifier = Modifier.padding(horizontal = Espacos.margem),
                verticalArrangement = Arrangement.spacedBy(Espacos.interno),
            ) {
                CartaoDoCorredor(estado, acoes)
                CartaoDoApp(estado.somNaCaptura, acoes.aoMudarSom)
                CartaoDaConta(recalculo, acoes)
                Rodape()
                Spacer(Modifier.height(Espacos.bloco))
            }
        }
    }

    if (estado.editandoBaseline) FolhaDaBaseline(estado, acoes)

    if (recalculo == EstadoDoRecalculo.Confirmando) {
        AlertDialog(
            onDismissRequest = acoes.aoCancelarRecalculo,
            title = { Text(stringResource(R.string.ajustes_recalcular_titulo)) },
            text = { Text(stringResource(R.string.ajustes_recalcular_texto)) },
            confirmButton = {
                BotaoDeTexto(aoTocar = acoes.aoConfirmarRecalculo) { Text(stringResource(R.string.ajustes_recalcular_confirmar)) }
            },
            dismissButton = {
                BotaoDeTexto(aoTocar = acoes.aoCancelarRecalculo) { Text(stringResource(R.string.editar_voltar)) }
            },
        )
    }

    if (estado.confirmandoSaida) {
        AlertDialog(
            onDismissRequest = acoes.aoDesistirDeSair,
            title = { Text(stringResource(R.string.ajustes_sair_titulo)) },
            text = { Text(stringResource(R.string.ajustes_sair_texto)) },
            confirmButton = {
                BotaoDeTexto(aoTocar = acoes.aoConfirmarSaida) { Text(stringResource(R.string.ajustes_sair_confirmar)) }
            },
            dismissButton = {
                BotaoDeTexto(aoTocar = acoes.aoDesistirDeSair) { Text(stringResource(R.string.ajustes_sair_cancelar)) }
            },
        )
    }
}

/** Só a sobrancelha: a credencial é o título da tela, e um título por cima a repetiria. */
@Composable
private fun Sobrancelha() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Espacos.alvoToque)
            .padding(horizontal = Espacos.margem),
        contentAlignment = Alignment.CenterStart,
    ) {
        RotuloDoCartao(stringResource(R.string.perfil_sobrancelha))
    }
}

// ---------------------------------------------------------------------------
// A credencial
// ---------------------------------------------------------------------------

@Composable
private fun PalcoDaCredencial(estado: PerfilUiState, acoes: AcoesDoPerfil) {
    val reduzido = LocalMovimentoReduzido.current
    val giro = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()

    // A credencial balança uma vez quando o parceiro muda (decisão 14).
    LaunchedEffect(estado.trocasDeParceiro) {
        if (estado.trocasDeParceiro > 0 && !reduzido) {
            giro.snapTo(BALANCO)
            giro.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    val arraste = if (reduzido) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    escopo.launch {
                        giro.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    }
                },
                onHorizontalDrag = { _, delta ->
                    escopo.launch { giro.snapTo((giro.value + delta / 12f).coerceIn(-GIRO_MAXIMO, GIRO_MAXIMO)) }
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Cordao(Modifier.fillMaxWidth().height(AlturaDoCordao))
        Broches(estado.insignias, Modifier.fillMaxWidth().height(AlturaDoCordao))
        Credencial(
            estado = estado,
            acoes = acoes,
            modifier = Modifier
                .padding(top = TopoDaCredencial, bottom = Espacos.bloco)
                .graphicsLayer {
                    rotationZ = INCLINACAO + giro.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .then(arraste),
        )
    }
}

/** Duas tiras em `destaque` que descem dos cantos até o prendedor (Revisão 1). */
@Composable
private fun Cordao(modifier: Modifier) {
    val tira = MaterialTheme.colorScheme.primary
    val sombra = LocalCoresPokerun.current.destaquePressionado
    val metal = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val meio = size.width / 2
        val largura = 22.dp.toPx()
        val fim = 92.dp.toPx()
        drawLine(tira, Offset(meio - 125.dp.toPx(), -4f), Offset(meio - 17.dp.toPx(), fim), largura, StrokeCap.Butt)
        drawLine(sombra, Offset(meio + 125.dp.toPx(), -4f), Offset(meio + 17.dp.toPx(), fim), largura, StrokeCap.Butt)
        drawRoundRect(
            color = metal.copy(alpha = 0.7f),
            topLeft = Offset(meio - 19.dp.toPx(), 80.dp.toPx()),
            size = Size(38.dp.toPx(), 26.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        drawRoundRect(
            color = metal,
            topLeft = Offset(meio - 9.dp.toPx(), 98.dp.toPx()),
            size = Size(18.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }
}

/**
 * As insígnias da vida, presas no cordão como broches (decisão 19): descem pela tira da
 * esquerda e, passadas quatro, pela da direita. Sem insígnia, o cordão fica liso.
 */
@Composable
private fun Broches(insignias: List<String>, modifier: Modifier) {
    if (insignias.isEmpty()) return
    BoxWithConstraints(modifier = modifier) {
        val meio = maxWidth / 2
        insignias.take(8).forEachIndexed { i, id ->
            val lado = if (i < 4) -1 else 1
            val passo = (i % 4)
            // Ao longo da tira: de (meio ∓ 125, 0) a (meio ∓ 17, 92), um broche a cada quarto.
            val t = 0.2f + 0.2f * passo
            val x = meio + (lado * (125 - 108 * t)).dp - 14.dp
            val y = (92 * t).dp - 14.dp
            AsyncImage(
                model = Insignias.url(id),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.offset(x = x, y = y).size(28.dp),
            )
        }
    }
}

@Composable
private fun Credencial(estado: PerfilUiState, acoes: AcoesDoPerfil, modifier: Modifier) {
    val tipo = estado.parceiro?.tipos?.firstOrNull()
    val corDoTipo = tipo?.let { LocalCoresDeTipo.current[it] }
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val nomeDoParceiro = estado.parceiro?.nome ?: estado.parceiro?.numero?.let { stringResource(R.string.parceiro_numero, it) }
    val semEscolha = stringResource(R.string.perfil_sem_escolha)
    val descricao = stringResource(
        R.string.perfil_credencial_descricao,
        estado.nome,
        nomeDoParceiro ?: semEscolha,
        estado.treinador?.let(Treinador::rotulo) ?: semEscolha,
    )

    Column(
        modifier = modifier
            // 28dp e não 16: inclinada, a ponta de baixo avança ~26dp, e a 320dp ela saía da tela.
            .padding(horizontal = 28.dp)
            .widthIn(max = LarguraDaCredencial)
            .fillMaxWidth()
            .shadow(10.dp, FormaEspecie, clip = false)
            .clip(FormaEspecie)
            .background(MaterialTheme.colorScheme.surface)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
    ) {
        // A foto: o render parado do HOME sobre o pastel do tipo (decisão 13).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDaFoto)
                .background(corDoTipo?.suave ?: afundada),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // O furo do cordão.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Espacos.interno)
                    .size(width = 56.dp, height = 10.dp)
                    .clip(FormaControle)
                    .background(afundada),
            )
            estado.parceiro?.let { parceiro ->
                Text(
                    text = stringResource(R.string.parceiro_numero, parceiro.numero),
                    style = Estilos.rotulo.copy(fontSize = 13.sp),
                    color = corDoTipo?.base ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = Espacos.margem, top = 30.dp),
                )
                RenderDoParceiro(parceiro, Modifier.padding(bottom = 6.dp).size(RenderDoParceiro))
            }
            BotaoDeTroca(acoes.aoEscolherParceiro, Modifier.align(Alignment.BottomEnd).padding(10.dp))
        }

        // Os dados, como num documento.
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = Espacos.margem)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    RotuloDoCartao(
                        estado.credencial?.temporada?.let { stringResource(R.string.perfil_corredor_da_temporada, it) }
                            ?: stringResource(R.string.perfil_corredor),
                    )
                    CampoDoNome(estado.nome, acoes)
                }
                SpriteDoTreinador(
                    treinador = estado.treinador,
                    modifier = Modifier.offset(x = 8.dp, y = 6.dp),
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = Espacos.interno),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            CamposDaCredencial(estado.credencial)
        }

        // A faixa de categoria, no tipo base, como a faixa de acesso de uma credencial.
        if (estado.parceiro != null) {
            val sobre = corDoTipo?.sobre ?: MaterialTheme.colorScheme.surface
            val fundoDaFaixa = corDoTipo?.base ?: MaterialTheme.colorScheme.onSurface
            val nome = (nomeDoParceiro ?: "").uppercase(LocaleDoApp)
            val estiloDoNome = Estilos.tituloSecao.copy(fontSize = 15.sp, letterSpacing = 0.6.sp)
            val nomeDoTipo = tipo?.let { stringResource(nomeDoTipo(it)).uppercase(LocaleDoApp) }
            val recheio = Modifier.fillMaxWidth().background(fundoDaFaixa).padding(horizontal = 18.dp, vertical = 10.dp)
            // Ampliada, a faixa empilha: lado a lado, o nome partia no meio (NIDORA / N).
            if (ampliado()) {
                Column(modifier = recheio) {
                    Text(text = nome, style = estiloDoNome, color = sobre)
                    if (nomeDoTipo != null) Text(text = nomeDoTipo, style = Estilos.rotulo, color = sobre.copy(alpha = 0.85f))
                }
            } else {
                Row(modifier = recheio, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = nome, style = estiloDoNome, color = sobre, modifier = Modifier.weight(1f))
                    if (nomeDoTipo != null) Text(text = nomeDoTipo, style = Estilos.rotulo, color = sobre.copy(alpha = 0.85f))
                }
            }
        }
    }
}

/** O render do HOME, com a arte oficial de reserva. */
@Composable
private fun RenderDoParceiro(parceiro: ParceiroDaCredencial, modifier: Modifier) {
    var falhou by remember(parceiro.render) { mutableStateOf(false) }
    AsyncImage(
        model = if (falhou) parceiro.reserva else parceiro.render,
        contentDescription = null,
        onError = { falhou = true },
        modifier = modifier,
    )
}

/** O botão redondo em `destaque` no canto da foto: abre o seletor de parceiro. */
@Composable
private fun BotaoDeTroca(aoTocar: () -> Unit, modifier: Modifier) {
    val descricao = stringResource(R.string.perfil_trocar_parceiro)
    val claro = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .size(Espacos.alvoToque)
            .clip(FormaCirculo)
            .clickable(role = Role.Button, onClick = aoTocar)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(36.dp)
                .shadow(3.dp, FormaCirculo)
                .background(MaterialTheme.colorScheme.primary, FormaCirculo),
        ) {
            val u = size.width / 24f
            val traco = 2.6f * u
            // As duas setas da troca: em cima para a direita, embaixo para a esquerda.
            drawLine(claro, Offset(7 * u, 10 * u), Offset(17 * u, 10 * u), traco, StrokeCap.Round)
            drawLine(claro, Offset(17 * u, 10 * u), Offset(14 * u, 7 * u), traco, StrokeCap.Round)
            drawLine(claro, Offset(17 * u, 14 * u), Offset(7 * u, 14 * u), traco, StrokeCap.Round)
            drawLine(claro, Offset(7 * u, 14 * u), Offset(10 * u, 17 * u), traco, StrokeCap.Round)
        }
    }
}

/**
 * O nome digitado é o título da credencial, editado no lugar, como o nome do plano na
 * montanha. Salva ao sair do campo ou no "concluído" do teclado (decisão 7).
 */
@Composable
private fun CampoDoNome(nome: String, acoes: AcoesDoPerfil) {
    val foco = LocalFocusManager.current
    val estilo = Estilos.tituloTela.copy(fontSize = 30.sp, lineHeight = 36.sp, color = MaterialTheme.colorScheme.onSurface)
    val rotulo = stringResource(R.string.perfil_campo_nome)
    BasicTextField(
        value = nome,
        onValueChange = acoes.aoMudarNome,
        singleLine = true,
        textStyle = estilo,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused) acoes.aoConfirmarNome() }
            .semantics { contentDescription = rotulo },
        decorationBox = { campo ->
            Box {
                if (nome.isEmpty()) {
                    Text(stringResource(R.string.perfil_nome_vazio), style = estilo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                campo()
            }
        },
    )
}

@Composable
private fun CamposDaCredencial(dados: DadosDaCredencial?) {
    val vazio = stringResource(R.string.perfil_sem_corrida)
    val desde = stringResource(R.string.perfil_desde) to (dados?.desde?.let(::dataCurtaComAno) ?: vazio)
    val colecao = stringResource(R.string.perfil_colecao) to (dados?.colecao?.toString() ?: vazio)
    val deTotal = dados?.totalEspecies?.let { stringResource(R.string.perfil_de_total, it) }
    val vida = stringResource(R.string.perfil_na_vida) to formatarKm(dados?.naVidaKm ?: 0.0)
    val km = stringResource(R.string.ajustes_km)
    val prova = stringResource(R.string.perfil_prova) to (dados?.prova?.let(::dataCurta) ?: stringResource(R.string.perfil_sem_plano))
    // (rótulo, valor, unidade), na ordem do documento.
    val campos = listOf(Triple(desde, null, 0), Triple(colecao, deTotal, 1), Triple(vida, km, 2), Triple(prova, null, 3))
    Column(
        modifier = Modifier.padding(top = Espacos.interno),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Com a fonte ampliada, um campo por linha: em duas colunas o rótulo COLEÇÃO partia
        // no meio da palavra a 320dp (visto no aparelho, 27/10).
        if (ampliado()) {
            campos.forEach { (par, unidade, _) -> CampoDaCredencial(par.first, par.second, Modifier.fillMaxWidth(), unidade) }
        } else {
            campos.chunked(2).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.margem)) {
                    linha.forEach { (par, unidade, _) -> CampoDaCredencial(par.first, par.second, Modifier.weight(1f), unidade) }
                }
            }
        }
    }
}

/** O limiar em que a tela empilha, o mesmo da barra de sessões e da placa de peito. */
private const val ESCALA_QUE_EMPILHA = 1.3f

@Composable
private fun ampliado(): Boolean = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

@Composable
private fun CampoDaCredencial(rotulo: String, valor: String, modifier: Modifier, unidade: String? = null) {
    Column(modifier = modifier) {
        RotuloDoCartao(rotulo)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = valor, style = Estilos.tituloSecao.copy(fontSize = 18.sp, lineHeight = 24.sp), color = MaterialTheme.colorScheme.onSurface)
            if (unidade != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unidade,
                    style = Estilos.legenda,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

/** `11 out 2026`. */
internal fun dataCurtaComAno(data: LocalDate): String = "${data.dayOfMonth} ${nomeCurtoDoMes(data)} ${data.year}"

/** `31 dez`. */
internal fun dataCurta(data: LocalDate): String = "${data.dayOfMonth} ${nomeCurtoDoMes(data)}"

// ---------------------------------------------------------------------------
// A lista (decisão 15)
// ---------------------------------------------------------------------------

@Composable
private fun CartaoDoCorredor(estado: PerfilUiState, acoes: AcoesDoPerfil) {
    Cartao(recheio = androidx.compose.foundation.layout.PaddingValues(start = Espacos.margem, end = Espacos.margem, top = Espacos.interno, bottom = Espacos.minimo)) {
        RotuloDoCartao(stringResource(R.string.perfil_cartao_corredor), Modifier.padding(vertical = Espacos.minimo))
        LinhaDoPerfil(
            titulo = stringResource(R.string.perfil_treinador),
            valor = estado.treinador?.let(Treinador::rotulo),
            aoTocar = acoes.aoEscolherTreinador,
            primeira = true,
            icone = { BustoDoTreinador(estado.treinador, 28.dp) },
        )
        LinhaDoPerfil(
            titulo = stringResource(R.string.perfil_parceiro),
            valor = estado.parceiro?.nome,
            aoTocar = acoes.aoEscolherParceiro,
            icone = { IconeDePokebola() },
        )
        LinhaDoPerfil(
            titulo = stringResource(R.string.perfil_baseline),
            apoio = stringResource(R.string.perfil_baseline_regra),
            valor = estado.baselineKm?.let { "${formatarKm(it)} ${stringResource(R.string.ajustes_km)}" },
            aoTocar = acoes.aoAbrirBaseline,
            icone = { IconeDeMontanha() },
        )
    }
}

@Composable
private fun CartaoDoApp(somNaCaptura: Boolean, aoMudar: (Boolean) -> Unit) {
    Cartao(recheio = androidx.compose.foundation.layout.PaddingValues(start = Espacos.margem, end = Espacos.margem, top = Espacos.interno, bottom = Espacos.minimo)) {
        RotuloDoCartao(stringResource(R.string.perfil_cartao_app), Modifier.padding(vertical = Espacos.minimo))
        val titulo = stringResource(R.string.perfil_som_na_captura)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDaLinha)
                .clickable(role = Role.Switch) { aoMudar(!somNaCaptura) }
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconeDeSom()
            Text(text = titulo, style = Estilos.corpoForte.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            val tinta = MaterialTheme.colorScheme.onSurface
            val afundada = LocalCoresPokerun.current.superficieAfundada
            Switch(
                checked = somNaCaptura,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = tinta,
                    checkedBorderColor = tinta,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = afundada,
                    uncheckedBorderColor = afundada,
                ),
            )
        }
    }
}

@Composable
private fun CartaoDaConta(recalculo: EstadoDoRecalculo, acoes: AcoesDoPerfil) {
    Cartao(recheio = androidx.compose.foundation.layout.PaddingValues(start = Espacos.margem, end = Espacos.margem, top = Espacos.interno, bottom = Espacos.minimo)) {
        RotuloDoCartao(stringResource(R.string.perfil_cartao_conta), Modifier.padding(vertical = Espacos.minimo))
        val apoio = when (recalculo) {
            EstadoDoRecalculo.Recalculando -> stringResource(R.string.ajustes_recalcular_andamento)
            is EstadoDoRecalculo.Concluido -> pluralStringResource(
                R.plurals.ajustes_recalcular_resumo,
                recalculo.resumo.corridas,
                recalculo.resumo.corridas,
                String.format(LocaleDoApp, "%,d", recalculo.resumo.xpAntes),
                String.format(LocaleDoApp, "%,d", recalculo.resumo.xpDepois),
            )
            EstadoDoRecalculo.Falhou -> stringResource(R.string.ajustes_recalcular_erro)
            else -> stringResource(R.string.perfil_recalcular_apoio)
        }
        LinhaDoPerfil(
            titulo = stringResource(R.string.ajustes_recalcular),
            apoio = apoio,
            aoTocar = if (recalculo == EstadoDoRecalculo.Recalculando) null else acoes.aoPedirRecalculo,
            primeira = true,
            icone = { IconeDeLinha(Icons.Default.Refresh) },
        )
        if (recalculo == EstadoDoRecalculo.Recalculando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = Espacos.relacionado))
        }
        LinhaDoPerfil(
            titulo = stringResource(R.string.ajustes_sair),
            aoTocar = acoes.aoPedirParaSair,
            chevron = false,
            icone = { IconeDeLinha(Icons.AutoMirrored.Filled.ExitToApp) },
        )
    }
}

/**
 * Uma linha de 60dp da lista: ícone, título, apoio, valor e chevron. A linha inteira é o
 * alvo, e o TalkBack a lê como um nó.
 */
@Composable
private fun LinhaDoPerfil(
    titulo: String,
    aoTocar: (() -> Unit)?,
    icone: @Composable () -> Unit,
    valor: String? = null,
    apoio: String? = null,
    primeira: Boolean = false,
    chevron: Boolean = true,
) {
    if (!primeira) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDaLinha)
            .then(if (aoTocar != null) Modifier.clickable(role = Role.Button, onClick = aoTocar) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Espacos.relacionado),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icone() }
        val empilha = ampliado()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = Estilos.corpoForte.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            if (apoio != null) {
                Text(text = apoio, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Ampliada, a linha não tem largura para título e valor lado a lado: o título
            // partia letra a letra a 320dp. O valor desce para baixo do título.
            if (empilha && valor != null) {
                Text(text = valor, style = Estilos.tituloSecao.copy(fontSize = 16.sp, lineHeight = 22.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (!empilha && valor != null) {
            Text(
                text = valor,
                style = Estilos.tituloSecao.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 140.dp),
            )
        }
        if (chevron && aoTocar != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IconeDeLinha(icone: ImageVector) {
    Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
}

/** A Pokébola em traço, o ícone do parceiro. */
@Composable
private fun IconeDePokebola() {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.size(24.dp)) {
        val u = size.width / 24f
        val traco = 2 * u
        val centro = Offset(12 * u, 12 * u)
        drawCircle(tinta, 9 * u, centro, style = androidx.compose.ui.graphics.drawscope.Stroke(traco))
        drawLine(tinta, Offset(3 * u, 12 * u), Offset(21 * u, 12 * u), traco)
        drawCircle(claro, 3 * u, centro)
        drawCircle(tinta, 3 * u, centro, style = androidx.compose.ui.graphics.drawscope.Stroke(traco))
    }
}

/** O arco da montanha, o ícone da baseline (a mesma figura da criação de plano). */
@Composable
private fun IconeDeMontanha() {
    val tinta = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.size(24.dp)) {
        val u = size.width / 24f
        val caminho = androidx.compose.ui.graphics.Path().apply {
            moveTo(4 * u, 18 * u)
            cubicTo(7 * u, 10 * u, 17 * u, 10 * u, 20 * u, 18 * u)
        }
        drawPath(caminho, tinta, style = androidx.compose.ui.graphics.drawscope.Stroke(2 * u, cap = StrokeCap.Round))
        drawCircle(tinta, 2 * u, Offset(12 * u, 11 * u))
    }
}

/** O alto-falante em traço, o ícone do som. */
@Composable
private fun IconeDeSom() {
    val tinta = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.size(24.dp)) {
        val u = size.width / 24f
        val corpo = androidx.compose.ui.graphics.Path().apply {
            moveTo(4 * u, 9 * u); lineTo(8 * u, 9 * u); lineTo(13 * u, 5 * u); lineTo(13 * u, 19 * u)
            lineTo(8 * u, 15 * u); lineTo(4 * u, 15 * u); close()
        }
        drawPath(corpo, tinta, style = androidx.compose.ui.graphics.drawscope.Stroke(2 * u))
        val onda = androidx.compose.ui.graphics.Path().apply {
            moveTo(17 * u, 9 * u)
            quadraticTo(18.6f * u, 12 * u, 17 * u, 15 * u)
        }
        drawPath(onda, tinta, style = androidx.compose.ui.graphics.drawscope.Stroke(2 * u, cap = StrokeCap.Round))
    }
}

@Composable
private fun Rodape() {
    val contexto = LocalContext.current
    val versao = remember(contexto) {
        runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    Text(
        text = stringResource(R.string.perfil_rodape, versao),
        style = Estilos.legenda,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Espacos.minimo),
    )
}

// ---------------------------------------------------------------------------
// A folha da baseline: o placar é o formulário
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolhaDaBaseline(estado: PerfilUiState, acoes: AcoesDoPerfil) {
    val folha = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = acoes.aoFecharBaseline,
        sheetState = folha,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacos.margem)
                .padding(bottom = Espacos.bloco),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            RotuloDoCartao(stringResource(R.string.perfil_baseline))
            Row(verticalAlignment = Alignment.Bottom) {
                val estilo = Estilos.numeroXl.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = estado.baselineDigitada,
                    onValueChange = { acoes.aoMudarBaseline(filtrarKm(it)) },
                    singleLine = true,
                    textStyle = estilo,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { acoes.aoConfirmarBaseline() }),
                    modifier = Modifier.width(larguraDoPlacar(estado.baselineDigitada)),
                )
                Text(
                    text = stringResource(R.string.ajustes_km),
                    style = Estilos.numeroM,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Espacos.relacionado, bottom = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.perfil_baseline_regra),
                style = Estilos.legenda,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (estado.erroDaBaseline != null) {
                BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erroDaBaseline))
            }
            BotaoPrimario(aoTocar = acoes.aoConfirmarBaseline, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.perfil_baseline_pronto))
            }
        }
    }
}

/** A largura do placar acompanha os dígitos, para o `km` ficar colado ao número. */
private fun larguraDoPlacar(texto: String) = (40 * texto.length.coerceAtLeast(1) + 12).dp

// ---------------------------------------------------------------------------
// Fábricas
// ---------------------------------------------------------------------------

@Composable
private fun perfilViewModel(): PerfilViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        PerfilViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.corridaRepositorio,
            container.planoRepositorio,
            container.especiesRepositorio,
            container.preferenciasDoAparelho,
            container.desafioRepositorio,
        )
    }
}

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

