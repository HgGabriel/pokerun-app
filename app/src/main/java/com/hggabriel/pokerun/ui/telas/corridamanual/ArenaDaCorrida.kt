package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.filtrarKm
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.telas.home.CardDaEspecie
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * A arena do registro de corrida (26/10/2026): a metade de cima da tela, em tinta, com o
 * Pokémon da temporada animado em 3D e o placar que é o próprio formulário. A distância e o tempo
 * se digitam ali, em número grande, e o pace sai na hora, junto com a espécie que corre
 * naquele ritmo ao lado do seu Pokémon.
 */

// ---------------------------------------------------------------------------
// As regras da arena
// ---------------------------------------------------------------------------

/**
 * Os ritmos, do mais rápido ao mais lento: o pace abaixo de [tetoSeg] (segundos por km) é
 * o ritmo daquela espécie. O último não tem teto.
 */
internal enum class Ritmo(val tetoSeg: Long, val numero: Int, @param:StringRes val nome: Int) {
    JOLTEON(255, 135, R.string.manual_ritmo_jolteon),
    RAPIDASH(300, 78, R.string.manual_ritmo_rapidash),
    PONYTA(345, 77, R.string.manual_ritmo_ponyta),
    GROWLITHE(390, 58, R.string.manual_ritmo_growlithe),
    PIKACHU(450, 25, R.string.manual_ritmo_pikachu),
    PSYDUCK(540, 54, R.string.manual_ritmo_psyduck),
    SLOWPOKE(Long.MAX_VALUE, 79, R.string.manual_ritmo_slowpoke),
}

internal fun ritmoDe(paceSeg: Long): Ritmo = Ritmo.entries.first { paceSeg < it.tetoSeg }

/**
 * O pace ao vivo, em segundos por km, ou nulo enquanto falta distância ou tempo. Usa as
 * mesmas leituras da validação, então o que a arena mostra é o que a gravação aceita.
 */
internal fun paceAoVivo(km: String, horas: String, minutos: String, segundos: String): Long? {
    val distancia = distanciaEmKm(km) ?: return null
    val duracao = duracaoEmSegundos(horas, minutos, segundos)?.takeIf { it > 0 } ?: return null
    return Math.round(duracao / distancia)
}

/** O pace em `m:ss`. */
internal fun paceEmTexto(paceSeg: Long): String = "%d:%02d".format(paceSeg / 60, paceSeg % 60)

/** Quanto o chão corre, em dp por segundo: mais rápido o pace, mais rápido o chão. */
internal fun velocidadeDoChao(paceSeg: Long?): Float =
    if (paceSeg == null || paceSeg <= 0) 0f else (VELOCIDADE_DE_REFERENCIA * PACE_DE_REFERENCIA / paceSeg).coerceIn(80f, 1400f)

private const val VELOCIDADE_DE_REFERENCIA = 420f
private const val PACE_DE_REFERENCIA = 330f
private const val VELOCIDADE_MAXIMA = 1400f

// ---------------------------------------------------------------------------
// As medidas
// ---------------------------------------------------------------------------

private val FormaDaArena = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
private val AlturaDaCena = 188.dp
private val SpriteDoTreinador = 172.dp
private val SpriteDoParceiro = 108.dp

/** A altura do chão na cena, em fração da altura. */
private const val CHAO = 0.84f

private val EstiloDaDistancia = Estilos.numeroPeito.copy(fontSize = 88.sp, lineHeight = 92.sp)

/** As linhas de velocidade: posição, comprimento, profundidade e brilho, fixos. */
private class LinhaDeVelocidade(val x: Float, val y: Float, val comprimento: Float, val profundidade: Float, val alfa: Float, val espessura: Dp)

private val Linhas = List(16) { i ->
    val semente = (i * 7919 + 13) % 997 / 997f
    val outra = (i * 104729 + 31) % 991 / 991f
    LinhaDeVelocidade(
        x = semente,
        y = 0.12f + 0.84f * outra,
        comprimento = 0.08f + 0.22f * ((i * 37) % 11 / 11f),
        profundidade = 0.45f + 0.9f * semente,
        alfa = 0.1f + 0.28f * outra,
        espessura = if (i % 3 == 0) 3.dp else 1.5.dp,
    )
}

/** As cores de uma [CaixaDeNumero]: a arena é escura, os cartões são claros. */
internal data class CoresDaCaixa(val fundo: Color, val texto: Color, val marcador: Color, val unidade: Color)

// ---------------------------------------------------------------------------
// A arena
// ---------------------------------------------------------------------------

@Composable
internal fun ArenaDaCorrida(
    estado: CorridaManualUiState,
    especie: CardDaEspecie?,
    aoMudarKm: (String) -> Unit,
    aoMudarHoras: (String) -> Unit,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val brilho = especie?.tipos?.firstOrNull()?.let { LocalCoresDeTipo.current[it].base }
        ?: MaterialTheme.colorScheme.primary
    val pace = paceAoVivo(estado.km, estado.horas, estado.minutos, estado.segundos)
    // O "próximo" da distância vai direto para as horas, e não para o atalho do longão.
    val horas = remember { FocusRequester() }
    // Onde o foco descansa quando o tempo termina: limpar o foco o devolveria à distância.
    val ancora = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDaArena)
            .background(tinta)
            .padding(bottom = Espacos.bloco),
    ) {
        Column(
            modifier = Modifier
                .focusRequester(ancora)
                .focusable()
                .padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.margem),
        ) {
            val sobrancelha = listOfNotNull(
                stringResource(R.string.manual_sobrancelha),
                estado.semana?.let { stringResource(R.string.manual_arena_semana, it) },
            ).joinToString(" · ")
            SobrancelhaDoBanner(sobrancelha)
            Text(text = stringResource(R.string.manual_titulo), style = Estilos.tituloTela, color = claro)
        }

        Cena(
            especie = especie,
            brilho = brilho,
            pace = pace,
            acordado = estado.km.isNotBlank(),
        )

        PlacarDaDistancia(
            km = estado.km,
            erro = estado.erros.km != null,
            longao = estado.longaoDaSemana,
            brilho = brilho,
            aoMudarKm = aoMudarKm,
            horas = horas,
        )

        Spacer(Modifier.height(Espacos.bloco))

        PlacarDoTempo(
            estado = estado,
            brilho = brilho,
            horas = horas,
            aoConcluir = { ancora.requestFocus() },
            aoMudarHoras = aoMudarHoras,
            aoMudarMinutos = aoMudarMinutos,
            aoMudarSegundos = aoMudarSegundos,
        )
    }
}

/**
 * A cena: o chão correndo na velocidade do pace, o brilho do tipo atrás do Pokémon da
 * temporada e, com pace, a espécie daquele ritmo correndo junto. Sem pace, tudo parado.
 */
@Composable
private fun Cena(especie: CardDaEspecie?, brilho: Color, pace: Long?, acordado: Boolean) {
    val reduzido = LocalMovimentoReduzido.current
    val claro = MaterialTheme.colorScheme.surface
    val velocidade by animateFloatAsState(
        targetValue = velocidadeDoChao(pace),
        animationSpec = tween(600),
        label = "velocidade do chão",
    )
    val deslocamento = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reduzido) {
        if (reduzido) return@LaunchedEffect
        var anterior = withFrameNanos { it }
        while (true) {
            withFrameNanos { agora ->
                deslocamento.floatValue = (deslocamento.floatValue + velocidade * (agora - anterior) / 1e9f) % 100_000f
                anterior = agora
            }
        }
    }
    val ritmo = pace?.let(::ritmoDe)
    val descricao = if (pace != null && ritmo != null) {
        stringResource(R.string.manual_ritmo_descricao, paceEmTexto(pace), stringResource(ritmo.nome))
    } else {
        ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDaCena),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val chao = size.height * CHAO
            val intensidade = (velocidade / VELOCIDADE_MAXIMA * 2.2f).coerceIn(0f, 1f)
            val centro = Offset(size.width / 2 + 20.dp.toPx(), chao - 56.dp.toPx())
            val raio = size.height * 0.78f
            drawCircle(
                brush = Brush.radialGradient(listOf(brilho.copy(alpha = 0.55f), Color.Transparent), centro, raio),
                radius = raio,
                center = centro,
            )
            drawRect(claro.copy(alpha = 0.05f), topLeft = Offset(0f, chao), size = Size(size.width, size.height - chao))
            drawLine(claro.copy(alpha = 0.2f), Offset(0f, chao), Offset(size.width, chao), 1.5.dp.toPx())

            val d = deslocamento.floatValue * density
            if (intensidade > 0f) {
                Linhas.forEach { linha ->
                    val comprimento = linha.comprimento * size.width * (0.5f + intensidade)
                    val passo = size.width + comprimento
                    val x = (linha.x * passo + d * linha.profundidade) % passo - comprimento
                    val y = linha.y * (chao - 8.dp.toPx())
                    drawLine(
                        color = claro.copy(alpha = linha.alfa * intensidade),
                        start = Offset(x, y),
                        end = Offset(x + comprimento, y),
                        strokeWidth = linha.espessura.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            // As marcas do chão andam com o chão, e ficam paradas sem pace.
            val vao = 44.dp.toPx()
            var x = d % vao - vao
            val yDaMarca = chao + (size.height - chao) / 2
            while (x < size.width) {
                drawLine(claro.copy(alpha = 0.14f), Offset(x, yDaMarca), Offset(x + 14.dp.toPx(), yDaMarca), 2.dp.toPx(), StrokeCap.Round)
                x += vao
            }
        }

        // O parceiro de ritmo vem atrás, menor e mais alto: está mais longe.
        AnimatedVisibility(
            visible = ritmo != null,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Espacos.bloco, bottom = AlturaDaCena * (1 - CHAO) + 18.dp),
        ) {
            if (ritmo != null) SpriteAnimado(numero = ritmo.numero, modifier = Modifier.size(SpriteDoParceiro))
        }

        if (especie != null) {
            SpriteAnimado(
                numero = especie.numero,
                apagado = especie.silhueta && !acordado,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = 20.dp)
                    .padding(bottom = AlturaDaCena * (1 - CHAO) - 6.dp)
                    .size(SpriteDoTreinador),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = Espacos.margem, top = Espacos.interno)
                .clearAndSetSemantics { if (descricao.isNotEmpty()) contentDescription = descricao },
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pace?.let(::paceEmTexto) ?: stringResource(R.string.manual_pace_vazio),
                    style = Estilos.numeroL,
                    color = if (pace != null) claro else claro.copy(alpha = 0.3f),
                )
                Spacer(Modifier.width(Espacos.minimo))
                Text(
                    text = stringResource(R.string.manual_por_km),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            AnimatedVisibility(visible = ritmo != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .padding(top = Espacos.minimo)
                        .background(brilho, RoundedCornerShape(percent = 50))
                        .padding(horizontal = Espacos.interno, vertical = Espacos.minimo),
                ) {
                    Text(
                        text = ritmo?.let { stringResource(R.string.manual_ritmo_de, stringResource(it.nome)) }.orEmpty()
                            .uppercase(),
                        style = Estilos.rotulo,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** A distância, digitada no número grande do meio da arena. */
@Composable
private fun PlacarDaDistancia(
    km: String,
    erro: Boolean,
    longao: Double?,
    brilho: Color,
    aoMudarKm: (String) -> Unit,
    horas: FocusRequester,
) {
    val claro = MaterialTheme.colorScheme.surface
    val rotulo = stringResource(R.string.manual_campo_distancia)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espacos.margem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            value = km,
            onValueChange = { novo -> aoMudarKm(filtrarKm(novo)) },
            textStyle = EstiloDaDistancia.copy(color = claro, textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = SolidColor(brilho),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { horas.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = rotulo },
            decorationBox = { campo ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (km.isEmpty()) {
                        Text(
                            text = "0",
                            style = EstiloDaDistancia,
                            color = claro.copy(alpha = 0.22f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    campo()
                }
            },
        )
        // O traço embaixo do número: em destaque quando a distância não passou.
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 4.dp)
                .background(
                    if (erro) MaterialTheme.colorScheme.primary else claro.copy(alpha = 0.16f),
                    RoundedCornerShape(percent = 50),
                ),
        )
        Spacer(Modifier.height(Espacos.relacionado))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.manual_quilometros).uppercase(),
                style = Estilos.rotulo,
                color = claro.copy(alpha = 0.6f),
            )
            if (longao != null) {
                Spacer(Modifier.width(Espacos.interno))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .border(1.dp, claro.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
                        .clickable { aoMudarKm(formatarKm(longao)) }
                        .padding(horizontal = Espacos.interno, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.manual_atalho_longao, formatarKm(longao)).uppercase(),
                        style = Estilos.rotulo,
                        color = claro,
                    )
                }
            }
        }
    }
}

/** O tempo em três caixas, `h : min : s`. Os minutos completos pulam para os segundos. */
@Composable
private fun PlacarDoTempo(
    estado: CorridaManualUiState,
    brilho: Color,
    horas: FocusRequester,
    aoConcluir: () -> Unit,
    aoMudarHoras: (String) -> Unit,
    aoMudarMinutos: (String) -> Unit,
    aoMudarSegundos: (String) -> Unit,
) {
    val claro = MaterialTheme.colorScheme.surface
    val cores = CoresDaCaixa(
        fundo = claro.copy(alpha = 0.08f),
        texto = claro,
        marcador = claro.copy(alpha = 0.22f),
        unidade = claro.copy(alpha = 0.55f),
    )
    val erro = estado.erros.duracao != null
    val segundos = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.manual_tempo).uppercase(),
            style = Estilos.rotulo,
            color = claro.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(Espacos.relacionado))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center) {
            CaixaDeNumero(
                valor = estado.horas,
                aoMudar = aoMudarHoras,
                marcador = "0",
                unidade = stringResource(R.string.manual_campo_horas),
                descricao = stringResource(R.string.manual_campo_duracao) + ", " + stringResource(R.string.manual_campo_horas),
                cores = cores,
                cursor = brilho,
                erro = erro,
                foco = horas,
            )
            DoisPontos(cores.unidade)
            CaixaDeNumero(
                valor = estado.minutos,
                aoMudar = aoMudarMinutos,
                marcador = "00",
                unidade = stringResource(R.string.manual_campo_minutos),
                descricao = stringResource(R.string.manual_campo_duracao) + ", " + stringResource(R.string.manual_campo_minutos),
                cores = cores,
                cursor = brilho,
                erro = erro,
                aoCompletar = { segundos.requestFocus() },
            )
            DoisPontos(cores.unidade)
            CaixaDeNumero(
                valor = estado.segundos,
                aoMudar = aoMudarSegundos,
                marcador = "00",
                unidade = stringResource(R.string.manual_campo_segundos),
                descricao = stringResource(R.string.manual_campo_duracao) + ", " + stringResource(R.string.manual_campo_segundos),
                cores = cores,
                cursor = brilho,
                erro = erro,
                foco = segundos,
                acao = ImeAction.Done,
                aoConcluir = aoConcluir,
            )
        }
    }
}

@Composable
private fun DoisPontos(cor: Color) {
    Text(
        text = ":",
        style = Estilos.numeroL,
        color = cor,
        modifier = Modifier.padding(horizontal = Espacos.minimo, vertical = 10.dp),
    )
}

/**
 * Uma caixa de número de até [maximo] dígitos, com a [unidade] embaixo. Só aceita dígito.
 * Com [aoCompletar], o último dígito passa o foco adiante.
 */
@Composable
internal fun CaixaDeNumero(
    valor: String,
    aoMudar: (String) -> Unit,
    marcador: String,
    unidade: String?,
    descricao: String,
    cores: CoresDaCaixa,
    cursor: Color,
    erro: Boolean,
    modifier: Modifier = Modifier,
    largura: Dp = 76.dp,
    altura: Dp = 64.dp,
    estilo: TextStyle = Estilos.numeroL,
    maximo: Int = 2,
    foco: FocusRequester? = null,
    aoCompletar: (() -> Unit)? = null,
    acao: ImeAction = ImeAction.Next,
    aoConcluir: (() -> Unit)? = null,
) {
    val gerenteDeFoco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = valor,
            onValueChange = { novo ->
                val limpo = novo.filter(Char::isDigit).take(maximo)
                aoMudar(limpo)
                if (limpo.length == maximo && valor.length < maximo) aoCompletar?.invoke()
            },
            textStyle = estilo.copy(color = cores.texto, textAlign = TextAlign.Center),
            singleLine = true,
            cursorBrush = SolidColor(cursor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = acao),
            keyboardActions = KeyboardActions(
                onDone = {
                    teclado?.hide()
                    if (aoConcluir != null) aoConcluir() else gerenteDeFoco.clearFocus()
                },
            ),
            modifier = Modifier
                .size(width = largura, height = altura)
                .then(if (foco != null) Modifier.focusRequester(foco) else Modifier)
                .semantics { contentDescription = descricao },
            decorationBox = { campo ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cores.fundo, FormaControle)
                        .then(
                            if (erro) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, FormaControle) else Modifier,
                        ),
                ) {
                    if (valor.isEmpty()) {
                        Text(text = marcador, style = estilo, color = cores.marcador, textAlign = TextAlign.Center)
                    }
                    campo()
                }
            },
        )
        if (unidade != null) {
            Spacer(Modifier.height(Espacos.minimo))
            Text(text = unidade, style = Estilos.legenda, color = cores.unidade)
        }
    }
}

// ---------------------------------------------------------------------------
// A captura
// ---------------------------------------------------------------------------

/**
 * A corrida registrada vira captura: o véu em tinta, a Pokébola cai, balança três vezes e
 * dá o clique, com as faíscas. Dura pouco mais de um segundo e meio, e só depois a tela
 * sai. Com movimento reduzido, a tela sai na hora.
 */
@Composable
internal fun CapturaDaCorrida(aoTerminar: () -> Unit, efeitos: Set<EfeitoDaCaptura> = emptySet()) {
    val reduzido = LocalMovimentoReduzido.current
    // A chave "Som e vibração na captura" do Perfil: vazia, nada toca nem vibra.
    val contexto = LocalContext.current
    val vista = LocalView.current
    val som = remember(contexto) { SomDaCaptura(contexto) }
    DisposableEffect(som) { onDispose { som.liberar() } }
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val vermelho = MaterialTheme.colorScheme.primary
    val faisca = LocalCoresDeTipo.current[com.hggabriel.pokerun.dominio.modelo.TipoDePokemon.ELETRICO].base
    val veu = remember { Animatable(0f) }
    val queda = remember { Animatable(0f) }
    val giro = remember { Animatable(0f) }
    val clique = remember { Animatable(0f) }
    val descricao = stringResource(R.string.manual_registrada)

    LaunchedEffect(Unit) {
        if (reduzido) {
            som.executar(efeitos, vista)
            aoTerminar()
            return@LaunchedEffect
        }
        launch { veu.animateTo(1f, tween(Movimento.Duracao.CURTA)) }
        queda.animateTo(1f, tween(420, easing = Movimento.Curva.captura))
        delay(120)
        repeat(3) { i ->
            val amplitude = 24f - i * 6f
            giro.animateTo(-amplitude, tween(80))
            giro.animateTo(amplitude, tween(120))
            giro.animateTo(0f, tween(80))
            delay(140)
        }
        som.executar(efeitos, vista)
        clique.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        delay(180)
        aoTerminar()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics {
                contentDescription = descricao
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(tinta.copy(alpha = 0.86f * veu.value))
            val raio = 48.dp.toPx()
            val centro = Offset(size.width / 2, lerp(-raio * 2, size.height * 0.42f, queda.value))
            val c = clique.value
            if (c > 0f) {
                drawCircle(claro.copy(alpha = 0.35f * (1 - c)), raio * (1.1f + 1.4f * c), centro)
                repeat(10) { i ->
                    val angulo = Math.toRadians(i * 36.0 + 18.0)
                    val de = raio * (1.25f + 0.6f * c)
                    val ate = raio * (1.45f + 1.3f * c)
                    val dx = kotlin.math.cos(angulo).toFloat()
                    val dy = kotlin.math.sin(angulo).toFloat()
                    drawLine(
                        color = (if (i % 2 == 0) faisca else claro).copy(alpha = 1 - c),
                        start = centro + Offset(dx * de, dy * de),
                        end = centro + Offset(dx * ate, dy * ate),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            rotate(giro.value, pivot = Offset(centro.x, centro.y + raio)) {
                pokebola(centro, raio, vermelho, claro, tinta, botaoAceso = c)
            }
        }
    }
}

/** A Pokébola desenhada: metade em [vermelho], metade em [claro], a faixa e o botão em [tinta]. */
internal fun DrawScope.pokebola(
    centro: Offset,
    raio: Float,
    vermelho: Color,
    claro: Color,
    tinta: Color,
    botaoAceso: Float = 0f,
) {
    val canto = centro - Offset(raio, raio)
    val tamanho = Size(raio * 2, raio * 2)
    drawArc(vermelho, 180f, 180f, useCenter = true, topLeft = canto, size = tamanho)
    drawArc(claro, 0f, 180f, useCenter = true, topLeft = canto, size = tamanho)
    drawLine(tinta, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), raio * 0.14f)
    drawCircle(tinta, raio, centro, style = Stroke(raio * 0.09f))
    drawCircle(tinta, raio * 0.32f, centro)
    drawCircle(claro, raio * 0.2f, centro)
    if (botaoAceso > 0f) drawCircle(vermelho.copy(alpha = botaoAceso), raio * 0.14f, centro)
    drawCircle(Color.White.copy(alpha = 0.35f), raio * 0.16f, Offset(centro.x - raio * 0.45f, centro.y - raio * 0.5f))
}
