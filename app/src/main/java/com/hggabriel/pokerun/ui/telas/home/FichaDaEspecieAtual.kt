package com.hggabriel.pokerun.ui.telas.home

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.Elevacao
import com.hggabriel.pokerun.ui.componentes.IconeDeTipo
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RecuoDoSprite
import com.hggabriel.pokerun.ui.componentes.fundoDoTipo
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.componentes.vagaParada
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaEspecie
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** O render do HOME, maior que a cena: ele salta pelo canto de cima à direita. */
private val LadoDoSprite = 212.dp

/**
 * Quanto o topo do sprite sobe acima do card. O render do HOME tem perto de um quarto de
 * margem transparente em cima, então a arte visível passa uns 25dp do card.
 */
private val SaltoDoSprite = 78.dp

/** O espaço reservado entre o título e o card. O resto do salto passa sobre a margem transparente. */
private val EspacoAntesDoCard = 24.dp

/** Quanto o sprite vaza pela borda direita do card, sobre a goteira da tela. */
private val VazamentoDoSprite = 34.dp

private val AlturaDaCena = 184.dp

/** O texto da cena fica na metade esquerda; a direita é do sprite. */
private const val LarguraDoTexto = 0.55f

private val LadoDoTipo = 30.dp
private val AnelDoTipo = 2.dp

// ---- A pista na divisa ------------------------------------------------------

/** A pista corre com o centro na emenda entre a cena e o rodapé. */
private val AlturaDaPista = 12.dp

/** O anel branco em volta da pista, da bandeira e da chegada: separa as peças da cena. */
private val AnelDaPista = 3.dp
private val InicioDaPista = 16.dp

/** Da ponta da pista à borda direita do card: a bandeira e a chegada moram aqui. */
private val FimDaPista = 86.dp
private val LadoDoMarcador = 48.dp
private val BordaDoMarcador = 3.dp

/** O ícone da Geração III tem 32px, com o desenho centrado 4,5px abaixo do meio do quadro. */
private const val LadoDoIcone = 32
private const val DesvioDoDesenho = 4.5f

/** A escala do ícone mira 2dp por pixel e arredonda para um múltiplo inteiro, para o pixel não borrar. */
private val EscalaAlvoDoIcone = 2.dp
private val LarguraDaBandeira = 14.dp
private val AlturaDaBandeira = 32.dp
private val BordaDaBandeira = 2.dp
private val CasaDaBandeira = 4.dp
private val RaioDaBandeira = 3.dp

/** A chegada fica um passo depois da bandeira, e não colada nela. */
private val DistanciaDaChegada = 34.dp
private val LadoDaChegada = 46.dp
private val LadoDaSilhuetaNaChegada = 42.dp
private val LarguraDaPontaDoBalao = 8.dp
private val AlturaDaPontaDoBalao = 4.dp
private val MargemDoBalao = 8.dp
private const val OpacidadeDaChegada = 0.5f

/** A sombra do sprite: a própria arte em preto, deslocada e borrada. Só com blur (Android 12+). */
private val DesvioDaSombra = 14.dp
private val RaioDaSombra = 10.dp
private const val OpacidadeDaSombra = 0.35f

/** O degradê da esquerda, onde o texto mora: a tinta fecha o contraste do branco sobre a cena. */
private const val VeuForte = 0.82f
private const val VeuMeio = 0.35f

private const val OpacidadeDaMarcaDagua = 0.16f
private const val OpacidadeDoApoio = 0.9f

private val EstiloDaMarcaDagua = Estilos.tituloTela.copy(fontSize = 92.sp, lineHeight = 92.sp, letterSpacing = (-0.05).em)

/**
 * A seção da espécie atual na Home (docs/01 §2.1, docs/03 §3.3.2), no desenho do **salto
 * lateral** (sketch de 29/10/2026):
 *
 * ```
 *  Último desbloqueado         Pokédex
 *                          ╭───────╮
 *  ╭──────────────────────(  arte   )─╮   o sprite salta pelo canto de cima à direita
 *  │ Bulbasaur             ╰───────╯  │
 *  │ Tier 2 · Primeiros Passos        │   cena: o fundo do tipo primário, véu à esquerda
 *  │ (●)(●)                           │   tipos só em ícone
 *  │ 001          [faltam 100 XP]     │   número em marca d'água; o balão sobre o marcador
 *  ├━━━━━━━━━━━━(●)━━━━━━▌▦   (◐)    ─┤   a pista na divisa: marcador, bandeira, chegada
 *  │ 150 de 250 XP                    │   o placar do degrau
 *  ╰──────────────────────────────────╯
 * ```
 *
 * A pista vem do sketch "bandeira na divisa" (29/10/2026). O marcador é o ícone da Geração
 * III da espécie, e a chegada é a silhueta do próximo alvo, que não leva nome: quando ele é
 * segredo, a chegada fica vazia.
 *
 * A seção inteira é um nó só, a porta da aba Pokédex: o `Pokédex` do título não é botão.
 * Sem tipo conhecido (sem rede e sem cópia guardada), a cena fica em `superficie-afundada`
 * sob o mesmo véu. A silhueta de RN-36 é preenchimento chapado, não opacidade (docs/03 §3.18).
 */
@Composable
fun FichaDaEspecieAtual(card: CardDaEspecie, modifier: Modifier = Modifier, aoTocar: (() -> Unit)? = null) {
    val nome = card.nome
    val numero = stringResource(R.string.especie_numero, card.numero)
    val alvo = card.proximoNumero?.let { proximo ->
        card.faltamXp?.let { faltam ->
            if (card.proximoSecreto) {
                return@let stringResource(R.string.especie_faltam_segredo, String.format(LocaleDoApp, "%,d", faltam))
            }
            stringResource(
                R.string.especie_faltam,
                String.format(LocaleDoApp, "%,d", faltam),
                card.proximoNome ?: stringResource(R.string.especie_numero, proximo),
            )
        }
    }
    val tier = card.escada?.let { escada -> escada.tiers.firstOrNull { it.ordem == escada.tierAtual } }
    val linhaDoTier = tier?.let { stringResource(R.string.especie_tier, it.ordem, it.nome) }
    val fracao = card.custoDoProximo?.takeIf { it > 0 }?.let { custo ->
        card.faltamXp?.let { faltam -> (1f - faltam.toFloat() / custo).coerceIn(0f, 1f) }
    }
    val nomesDosTipos = card.tipos.map { stringResource(nomeDoTipo(it)) }
    val quem = if (nomesDosTipos.isEmpty()) {
        nome ?: numero
    } else {
        stringResource(
            R.string.especie_descricao_tipos,
            nome ?: numero,
            nomesDosTipos.reduce { a, b -> stringResource(R.string.especie_tipos_e, a, b) },
        )
    }
    val descricao = buildString {
        append(stringResource(if (card.silhueta) R.string.especie_descricao_silhueta else R.string.especie_descricao, quem))
        if (linhaDoTier != null) append(". ").append(linhaDoTier)
        if (alvo != null) append(". ").append(alvo)
    }

    val toque = if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier
    Column(
        modifier = modifier
            .then(toque)
            .clearAndSetSemantics {
                contentDescription = descricao
                if (aoTocar != null) {
                    role = Role.Button
                    onClick { aoTocar(); true }
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.especie_secao_titulo),
                style = Estilos.tituloSecao,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (aoTocar != null) {
                Text(text = stringResource(R.string.pokedex_titulo), style = Estilos.corpoForte, color = MaterialTheme.colorScheme.primary)
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(top = EspacoAntesDoCard)
                    .fillMaxWidth()
                    .shadow(Elevacao.especie, FormaEspecie, clip = false, ambientColor = MaterialTheme.colorScheme.onSurface, spotColor = MaterialTheme.colorScheme.onSurface)
                    .clip(FormaEspecie)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                val faltam = card.faltamXp
                val custo = card.custoDoProximo
                Column(modifier = Modifier.fillMaxWidth()) {
                    CenaDoTipo(card, linhaDoTier, numero)
                    if (fracao != null && faltam != null && custo != null) {
                        Placar(feitos = custo - faltam, custo = custo)
                    }
                }
                if (fracao != null && faltam != null) {
                    PistaNaDivisa(
                        fracao = fracao,
                        faltam = faltam,
                        icone = card.icone,
                        silhueta = card.silhueta,
                        spriteDoProximo = card.spriteDoProximo,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            SpriteQueSalta(
                card = card,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = VazamentoDoSprite, y = EspacoAntesDoCard - SaltoDoSprite)
                    .size(LadoDoSprite),
            )
        }
    }
}

/** A cena: o fundo do tipo primário, o véu da esquerda, o número em marca d'água, o nome e os tipos. */
@Composable
private fun CenaDoTipo(card: CardDaEspecie, linhaDoTier: String?, numero: String) {
    val tipo = card.tipos.firstOrNull()
    val chapado = tipo?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada
    val veu = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlturaDaCena)
            .clipToBounds()
            .background(chapado)
            .then(
                if (tipo != null) {
                    // Sem `sizeToIntrinsics`, senão os 512px do WebP ditariam a altura da cena.
                    Modifier.paint(painterResource(fundoDoTipo(tipo)), sizeToIntrinsics = false, contentScale = ContentScale.Crop)
                } else {
                    Modifier
                },
            )
            .drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        0f to veu.copy(alpha = VeuForte),
                        0.55f to veu.copy(alpha = VeuMeio),
                        1f to Color.Transparent,
                    ),
                )
            },
    ) {
        Text(
            text = numero.removePrefix("#"),
            style = EstiloDaMarcaDagua,
            color = Color.White.copy(alpha = OpacidadeDaMarcaDagua),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = Espacos.interno, y = 26.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(LarguraDoTexto)
                .padding(start = Espacos.margem, top = Espacos.margem),
        ) {
            Text(text = card.nome ?: numero, style = Estilos.tituloTela, color = Color.White)
            if (linhaDoTier != null) {
                Text(text = linhaDoTier, style = Estilos.apoio, color = Color.White.copy(alpha = OpacidadeDoApoio))
            }
            if (card.tipos.isNotEmpty()) {
                Spacer(Modifier.height(Espacos.interno))
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
                    card.tipos.forEach {
                        IconeDeTipo(
                            tipo = it,
                            lado = LadoDoTipo,
                            modifier = Modifier
                                .background(Color.White, FormaCirculo)
                                .padding(AnelDoTipo),
                        )
                    }
                }
            }
        }
    }
}

/**
 * O render do HOME com a sombra dele. O render primeiro; se falhar, a arte oficial; se ela
 * também falhar, a vaga parada.
 */
@Composable
private fun SpriteQueSalta(card: CardDaEspecie, modifier: Modifier) {
    val cores = LocalCoresPokerun.current
    var imagem by remember(card.sprite, card.spriteReserva) { mutableStateOf<String?>(card.sprite) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val atual = imagem
        if (atual == null) {
            Spacer(Modifier.size(LadoDoSprite).vagaParada(FormaCirculo, recuo = RecuoDoSprite))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SombraDoSprite(atual)
            ImagemDaRede(
                model = atual,
                modifier = Modifier.matchParentSize(),
                colorFilter = if (card.silhueta) ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) else null,
                aoFalhar = { imagem = card.spriteReserva.takeIf { it != atual } },
            )
        }
    }
}

@Composable
private fun BoxScope.SombraDoSprite(modelo: String) {
    AsyncImage(
        model = modelo,
        contentDescription = null,
        colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn),
        alpha = OpacidadeDaSombra,
        modifier = Modifier
            .matchParentSize()
            .offset(y = DesvioDaSombra)
            .blur(RaioDaSombra, BlurredEdgeTreatment.Unbounded),
    )
}

/** O placar do degrau, à esquerda, abaixo da pista: `150 de 250 XP`, com o feito em Poppins. */
@Composable
private fun Placar(feitos: Long, custo: Long) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val resto = stringResource(R.string.especie_placar_resto, String.format(LocaleDoApp, "%,d", custo))
    val texto = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = Estilos.tituloSecao.fontFamily, fontWeight = FontWeight.Bold, color = tinta)) {
            append(String.format(LocaleDoApp, "%,d", feitos))
        }
        append(" ")
        append(resto)
    }
    Text(
        text = texto,
        style = Estilos.legenda,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = Espacos.margem,
            end = Espacos.margem,
            // Abaixo da metade de baixo do marcador, que desce da divisa.
            top = LadoDoMarcador / 2 + Espacos.relacionado / 2,
            bottom = Espacos.interno,
        ),
    )
}

/**
 * A pista sobre a emenda entre a cena e o rodapé: o trilho cheio até a [fracao], o marcador
 * da espécie com o balão do que falta, a bandeira na ponta e, um passo depois, a silhueta do
 * próximo alvo. Ocupa o card inteiro e posiciona as peças pela altura da cena.
 */
@Composable
private fun PistaNaDivisa(
    fracao: Float,
    faltam: Long,
    icone: String?,
    silhueta: Boolean,
    spriteDoProximo: String?,
    modifier: Modifier,
) {
    val cores = LocalCoresPokerun.current
    val esquema = MaterialTheme.colorScheme
    val balao = stringResource(R.string.especie_balao, String.format(LocaleDoApp, "%,d", faltam))
    Layout(
        modifier = modifier,
        content = {
            Box(modifier = Modifier.background(esquema.surface, FormaCirculo).padding(AnelDaPista)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AlturaDaPista)
                        .clip(FormaCirculo)
                        .background(cores.superficieAfundada),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fracao)
                            .fillMaxHeight()
                            .clip(FormaCirculo)
                            .background(esquema.primary),
                    )
                }
            }
            Bandeira()
            Chegada(spriteDoProximo)
            Marcador(icone, silhueta)
            Balao(balao)
        },
    ) { medidas, restricoes ->
        val largura = restricoes.maxWidth
        val divisa = AlturaDaCena.roundToPx()
        val anel = AnelDaPista.roundToPx()
        val inicio = InicioDaPista.roundToPx()
        val fim = largura - FimDaPista.roundToPx()
        val trilho = (fim - inicio).coerceAtLeast(0)
        val livre = Constraints()
        val pTrilho = medidas[0].measure(Constraints.fixedWidth(trilho + 2 * anel))
        val pBandeira = medidas[1].measure(livre)
        val pChegada = medidas[2].measure(livre)
        val pMarcador = medidas[3].measure(livre)
        val pBalao = medidas[4].measure(livre)
        val xDoMarcador = inicio + (trilho * fracao).roundToInt()
        val raio = pMarcador.width / 2
        val margem = MargemDoBalao.roundToPx()
        layout(largura, restricoes.maxHeight) {
            pTrilho.place(inicio - anel, divisa - pTrilho.height / 2)
            pBandeira.place(fim - anel, divisa - pBandeira.height / 2)
            pChegada.place(fim + DistanciaDaChegada.roundToPx() - anel, divisa - pChegada.height / 2)
            pMarcador.place(xDoMarcador - raio, divisa - pMarcador.height / 2)
            pBalao.place(
                (xDoMarcador - pBalao.width / 2).coerceIn(margem, (largura - pBalao.width - margem).coerceAtLeast(margem)),
                divisa - raio - pBalao.height,
            )
        }
    }
}

/** O balão em tinta com a ponta para o marcador. */
@Composable
private fun Balao(texto: String) {
    val tinta = MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = texto,
            style = Estilos.legenda,
            color = MaterialTheme.colorScheme.surface,
            maxLines = 1,
            modifier = Modifier
                .background(tinta, FormaCirculo)
                .padding(horizontal = Espacos.relacionado, vertical = Espacos.relacionado / 2),
        )
        Canvas(modifier = Modifier.size(LarguraDaPontaDoBalao, AlturaDaPontaDoBalao)) {
            drawPath(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                },
                color = tinta,
            )
        }
    }
}

/**
 * O marcador: o ícone da Geração III num disco com aro em `destaque`. O ícone é pixel art de
 * 32px; a escala é um múltiplo inteiro de pixels e sem filtro, para cada pixel sair quadrado.
 */
@Composable
private fun Marcador(icone: String?, silhueta: Boolean) {
    val cores = LocalCoresPokerun.current
    val densidade = LocalDensity.current
    val escala = with(densidade) { EscalaAlvoDoIcone.toPx().toInt().coerceAtLeast(1) }
    val quadro = with(densidade) { (LadoDoIcone * escala).toDp() }
    val desvio = with(densidade) { (DesvioDoDesenho * escala).toDp() }
    Box(
        modifier = Modifier
            .size(LadoDoMarcador)
            .clip(FormaCirculo)
            .background(MaterialTheme.colorScheme.surface)
            .border(BordaDoMarcador, MaterialTheme.colorScheme.primary, FormaCirculo),
        contentAlignment = Alignment.Center,
    ) {
        if (icone != null) {
            AsyncImage(
                model = icone,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                colorFilter = if (silhueta) ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) else null,
                modifier = Modifier
                    .requiredSize(quadro)
                    .offset(y = -desvio),
            )
        }
    }
}

/** A bandeira quadriculada de pé na ponta da pista, com o anel branco que a separa da cena. */
@Composable
private fun Bandeira() {
    val tinta = MaterialTheme.colorScheme.onSurface
    val branco = MaterialTheme.colorScheme.surface
    val forma = RoundedCornerShape(RaioDaBandeira)
    Box(modifier = Modifier.background(branco, RoundedCornerShape(RaioDaBandeira + AnelDaPista)).padding(AnelDaPista)) {
        Box(
            modifier = Modifier
                .size(LarguraDaBandeira, AlturaDaBandeira)
                .clip(forma)
                .drawBehind {
                    val casa = CasaDaBandeira.toPx()
                    drawRect(branco)
                    var linha = 0
                    while (linha * casa < size.height) {
                        var coluna = 0
                        while (coluna * casa < size.width) {
                            if ((linha + coluna) % 2 == 0) {
                                drawRect(tinta, Offset(coluna * casa, linha * casa), Size(casa, casa))
                            }
                            coluna++
                        }
                        linha++
                    }
                }
                .border(BordaDaBandeira, tinta, forma),
        )
    }
}

/** A chegada: a silhueta do próximo alvo num disco, sem nome. Vazia quando ele é segredo. */
@Composable
private fun Chegada(sprite: String?) {
    val cores = LocalCoresPokerun.current
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, FormaCirculo)
            .padding(AnelDaPista)
            .size(LadoDaChegada)
            .background(cores.superficieAfundada, FormaCirculo),
        contentAlignment = Alignment.Center,
    ) {
        if (sprite != null) {
            AsyncImage(
                model = sprite,
                contentDescription = null,
                colorFilter = ColorFilter.tint(cores.silhueta, BlendMode.SrcIn),
                alpha = OpacidadeDaChegada,
                modifier = Modifier.size(LadoDaSilhuetaNaChegada),
            )
        }
    }
}
