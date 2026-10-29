package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.filtrarKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.telas.home.CardDaEspecie
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import kotlin.math.roundToInt

/*
 * A montanha do plano (27/10/2026): a peça em tinta da criação. O nome se digita como o
 * título, e a curva do longão sobe semana a semana enquanto a data e as distâncias chegam,
 * com o Pokémon da temporada de pé no pico. É o plano antes de existir, e o motivo de um
 * "não cabe" à vista: a linha do mínimo acima do pico.
 */

private val FormaDaMontanha = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
private val AlturaDaCena = 236.dp

/** Onde a escala começa, a partir do topo da cena: a folga do Pokémon de pé no pico. */
private val TopoDaEscala = 104.dp
private val FolgaDaBase = 12.dp
private val SpriteNoPico = 96.dp
private const val LARGURA_DA_COLUNA = 0.66f
private const val VAO = 3f

/**
 * A silhueta da montanha antes do dado: uma rampa genérica de 12 semanas, só para a cena
 * não abrir vazia. Fica apagada, e o Pokémon em cinza.
 */
private val Fantasma = List(9) { 0.42f + 0.58f * it / 8f } + listOf(0.6f, 0.4f, 0.86f)

@Composable
internal fun MontanhaDoPlano(
    estado: CriarPlanoUiState,
    especie: CardDaEspecie?,
    aoMudarNome: (String) -> Unit,
    ancora: FocusRequester,
    proximo: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val brilho = especie?.tipos?.firstOrNull()?.let { LocalCoresDeTipo.current[it].base }
        ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDaMontanha)
            .background(tinta)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = Espacos.bloco),
    ) {
        Column(
            modifier = Modifier
                .focusRequester(ancora)
                .focusable()
                .padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.margem),
        ) {
            SobrancelhaDoBanner(stringResource(R.string.criar_plano_novo))
            CampoDoNome(
                nome = estado.nome,
                erro = estado.erros.nome != null,
                brilho = brilho,
                aoMudar = aoMudarNome,
                proximo = proximo,
            )
        }

        Cena(estado = estado, especie = especie, brilho = brilho)

        Spacer(Modifier.height(Espacos.relacionado))
        Placar(estado)
    }
}

/** O nome do plano, digitado no tamanho do título. O traço embaixo vira `destaque` no erro. */
@Composable
private fun CampoDoNome(
    nome: String,
    erro: Boolean,
    brilho: Color,
    aoMudar: (String) -> Unit,
    proximo: FocusRequester,
) {
    val claro = MaterialTheme.colorScheme.surface
    val rotulo = stringResource(R.string.criar_campo_nome)
    BasicTextField(
        value = nome,
        onValueChange = aoMudar,
        textStyle = Estilos.tituloTela.copy(color = claro),
        singleLine = true,
        cursorBrush = SolidColor(brilho),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { proximo.requestFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = rotulo },
        decorationBox = { campo ->
            Column {
                Box {
                    if (nome.isEmpty()) Text(text = rotulo, style = Estilos.tituloTela, color = claro.copy(alpha = 0.3f))
                    campo()
                }
                Spacer(Modifier.height(Espacos.minimo))
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 4.dp)
                        .background(
                            if (erro) MaterialTheme.colorScheme.primary else claro.copy(alpha = 0.16f),
                            RoundedCornerShape(percent = 50),
                        ),
                )
            }
        },
    )
}

/**
 * A cena: uma coluna por semana, em pílula sobre o trilho. O build em branco, o pico cheio,
 * o polimento apagado e a prova na cor do tipo. Com o plano que não cabe, a linha do mínimo
 * em `destaque` passa acima do pico. As colunas crescem com mola quando a curva muda; com
 * movimento reduzido, vão direto.
 */
@Composable
private fun Cena(estado: CriarPlanoUiState, especie: CardDaEspecie?, brilho: Color) {
    val previa = estado.previa
    val claro = MaterialTheme.colorScheme.surface
    val vermelho = MaterialTheme.colorScheme.primary
    val reduzido = LocalMovimentoReduzido.current
    val densidade = LocalDensity.current
    val medidor = rememberTextMeasurer()
    val semEscala = Density(densidade.density, fontScale = 1f)

    val maximo = previa?.let { maxOf(it.longoes.max(), if (it.cabe) 0.0 else it.minimoKm) } ?: 1.0
    val alvos: List<Float> = previa?.longoes?.map { (it / maximo).toFloat() } ?: Fantasma
    val indiceDoPico = previa?.indiceDoPico ?: (Fantasma.size - 4)
    val especificacao = Movimento.lento<Float>(reduzido)
    val alturas = alvos.mapIndexed { i, alvo ->
        key(i) { animateFloatAsState(alvo, especificacao, label = "coluna").value }
    }
    val minimo by animateFloatAsState(
        targetValue = previa?.takeIf { !it.cabe }?.let { (it.minimoKm / maximo).toFloat() } ?: 0f,
        animationSpec = especificacao,
        label = "mínimo",
    )

    val rotuloDaProva = estado.dataProva?.let { "${it.dayOfMonth} ${nomeCurtoDoMes(it)}".uppercase(LocaleDoApp) }
    val textoDoMinimo = previa?.takeIf { !it.cabe }?.let {
        stringResource(R.string.criar_precisa_de, formatarKm(it.minimoKm)).uppercase(LocaleDoApp)
    }
    val descricao = if (previa != null) {
        stringResource(
            R.string.criar_montanha_descricao,
            formatarKm(previa.longoes.first()),
            formatarKm(previa.picoKm),
            previa.indiceDoPico + 1,
            formatarKm(previa.longoes.last()),
            previa.semanas,
        )
    } else {
        stringResource(R.string.criar_montanha_vazia)
    }
    val estiloDoRotulo = Estilos.rotulo.copy(color = claro.copy(alpha = 0.6f))
    val estiloDoMinimo = Estilos.rotulo.copy(color = Color.White)

    val largura = remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDaCena)
            .padding(horizontal = Espacos.margem)
            .onSizeChanged { largura.intValue = it.width }
            .clearAndSetSemantics { contentDescription = descricao },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(AlturaDaCena)) {
            val n = alturas.size
            val vao = VAO * density
            val coluna = (size.width - vao * (n - 1)) / n
            val pilula = coluna * LARGURA_DA_COLUNA
            val base = size.height - FolgaDaBase.toPx()
            val topo = TopoDaEscala.toPx()
            val util = base - topo
            fun esquerda(i: Int) = i * (coluna + vao) + (coluna - pilula) / 2
            fun yDe(fracao: Float) = base - util * fracao.coerceIn(0f, 1f)

            // O brilho do tipo, atrás do pico: é onde o Pokémon fica.
            val centro = Offset(esquerda(indiceDoPico) + pilula / 2, yDe(alturas[indiceDoPico]) - 40.dp.toPx())
            val raio = size.height * 0.62f
            drawCircle(
                brush = Brush.radialGradient(listOf(brilho.copy(alpha = if (previa != null) 0.5f else 0.18f), Color.Transparent), centro, raio),
                radius = raio,
                center = centro,
            )

            alturas.forEachIndexed { i, fracao ->
                val x = esquerda(i)
                drawRoundRect(
                    color = claro.copy(alpha = 0.06f),
                    topLeft = Offset(x, topo),
                    size = Size(pilula, util),
                    cornerRadius = CornerRadius(pilula / 2),
                )
                val y = yDe(fracao)
                val alto = (base - y).coerceAtLeast(pilula)
                val prova = i == n - 1
                val polimento = i == n - 2 || i == n - 3
                val (cor, cima, baixo) = when {
                    previa == null -> Triple(claro, 0.16f, 0.08f)
                    prova -> Triple(brilho, 1f, 0.7f)
                    i == indiceDoPico -> Triple(claro, 1f, 0.85f)
                    polimento -> Triple(claro, 0.32f, 0.14f)
                    else -> Triple(claro, 0.82f, 0.32f)
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to cor.copy(alpha = cima),
                        1f to cor.copy(alpha = baixo),
                        startY = base - alto,
                        endY = base,
                    ),
                    topLeft = Offset(x, base - alto),
                    size = Size(pilula, alto),
                    cornerRadius = CornerRadius(pilula / 2),
                )
            }

            // O dia da prova, acima da coluna dela.
            if (previa != null && rotuloDaProva != null) {
                val layout = medidor.measure(rotuloDaProva, estiloDoRotulo, density = semEscala)
                val x = (esquerda(n - 1) + pilula - layout.size.width).coerceAtLeast(0f)
                val y = yDe(alturas[n - 1]) - layout.size.height - 6.dp.toPx()
                drawText(layout, topLeft = Offset(x, y))
            }

            // O plano que não cabe: a linha cheia do mínimo, com a pílula do quanto falta.
            if (minimo > 0f && textoDoMinimo != null) {
                val y = yDe(minimo)
                drawLine(vermelho, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                val layout = medidor.measure(textoDoMinimo, estiloDoMinimo, density = semEscala)
                val px = 8.dp.toPx()
                val py = 4.dp.toPx()
                val altura = layout.size.height + 2 * py
                drawRoundRect(
                    color = vermelho,
                    topLeft = Offset(0f, y - altura / 2),
                    size = Size(layout.size.width + 2 * px, altura),
                    cornerRadius = CornerRadius(altura / 2),
                )
                drawText(layout, topLeft = Offset(px, y - altura / 2 + py))
            }
        }

        // O Pokémon de pé no topo do pico: anda junto com a coluna.
        if (especie != null && largura.intValue > 0) {
            val n = alturas.size
            val vaoPx = with(densidade) { VAO.dp.toPx() }
            val colunaPx = (largura.intValue - vaoPx * (n - 1)) / n
            val sprite = with(densidade) { SpriteNoPico.toPx() }
            val base = with(densidade) { (AlturaDaCena - FolgaDaBase).toPx() }
            val util = base - with(densidade) { TopoDaEscala.toPx() }
            val fracao = alturas[indiceDoPico].coerceIn(0f, 1f)
            SpriteAnimado(
                numero = especie.numero,
                apagado = previa == null,
                modifier = Modifier
                    .size(SpriteNoPico)
                    .offset {
                        val centro = indiceDoPico * (colunaPx + vaoPx) + colunaPx / 2
                        IntOffset(
                            x = (centro - sprite / 2).roundToInt(),
                            y = (base - util * fracao - sprite + 6.dp.toPx()).roundToInt(),
                        )
                    },
            )
        }
    }
}

/** O tamanho do plano em dois números: semanas e o maior longão. `0` apagado sem dado. */
@Composable
private fun Placar(estado: CriarPlanoUiState) {
    val claro = MaterialTheme.colorScheme.surface
    val semanas = estado.previa?.semanas ?: estado.semanas
    val pico = estado.previa?.picoKm
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Espacos.margem)) {
        Numero(
            valor = semanas?.toString(),
            unidade = null,
            rotulo = stringResource(R.string.criar_semanas),
            claro = claro,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .padding(vertical = Espacos.minimo)
                .size(width = 1.dp, height = 48.dp)
                .background(claro.copy(alpha = 0.14f)),
        )
        Numero(
            valor = pico?.let { formatarKm(it) },
            unidade = stringResource(R.string.criar_km),
            rotulo = stringResource(R.string.criar_maior_longao),
            claro = claro,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Numero(valor: String?, unidade: String?, rotulo: String, claro: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = valor ?: "0",
                style = Estilos.numeroL,
                color = if (valor != null) claro else claro.copy(alpha = 0.3f),
            )
            if (unidade != null) {
                Spacer(Modifier.width(Espacos.minimo))
                Text(
                    text = unidade,
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Text(text = rotulo.uppercase(LocaleDoApp), style = Estilos.rotulo, color = claro.copy(alpha = 0.6f))
    }
}

// ---------------------------------------------------------------------------
// O campo de distância dos cartões
// ---------------------------------------------------------------------------

private val LarguraDoKm = 112.dp
private val AlturaDoKm = 60.dp

/**
 * Uma distância digitada em número grande, numa caixa de `superficie-afundada`, com o `km`
 * dentro. Aceita só a forma de `distanciaEmKm` (o filtro é o do registro de corrida).
 */
@Composable
internal fun CampoDeKm(
    valor: String,
    aoMudar: (String) -> Unit,
    descricao: String,
    erro: Boolean,
    foco: FocusRequester,
    acao: ImeAction,
    aoAcao: () -> Unit,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val forma = FormaControle
    BasicTextField(
        value = valor,
        onValueChange = { aoMudar(filtrarKm(it)) },
        textStyle = Estilos.numeroL.copy(color = tinta, textAlign = TextAlign.End),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = acao,
        ),
        keyboardActions = KeyboardActions(onNext = { aoAcao() }, onDone = { aoAcao() }),
        modifier = Modifier
            .size(width = LarguraDoKm, height = AlturaDoKm)
            .focusRequester(foco)
            .semantics { contentDescription = descricao },
        decorationBox = { campo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(LocalCoresPokerun.current.superficieAfundada, forma)
                    .then(if (erro) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, forma) else Modifier)
                    .padding(horizontal = Espacos.interno),
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (valor.isEmpty()) {
                        Text(text = "0", style = Estilos.numeroL, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                    campo()
                }
                Spacer(Modifier.width(Espacos.minimo))
                Text(
                    text = stringResource(R.string.criar_km),
                    style = Estilos.rotulo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
    )
}
