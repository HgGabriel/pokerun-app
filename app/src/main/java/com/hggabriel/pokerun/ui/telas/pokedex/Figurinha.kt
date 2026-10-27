package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.componentes.IconeDeTipo
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.navegacao.LocalFigurinhaEmVoo
import com.hggabriel.pokerun.ui.navegacao.chaveDaFigurinha
import com.hggabriel.pokerun.ui.navegacao.vooCompartilhado
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Tamanhos

/** A borda branca da figurinha e o raio dela: a figurinha é papel, não ficha. */
private val BordaDaFigurinha = 3.dp
private val FormaDaFigurinha = RoundedCornerShape(12.dp)
private val FormaDaImagem = RoundedCornerShape(9.dp)
private val IconeNaFigurinha = 14.dp

/** A sombra curta de objeto colado: só a figurinha colada tem, a vaga é buraco na página. */
private val SombraDaFigurinha = 3.dp

/**
 * As medidas de uma figurinha por lugar. A da grade é a de 3 colunas a 360dp; a da fita é
 * menor, para caberem 4 na largura da capa.
 */
@Immutable
internal data class MedidaDaFigurinha(val alturaDaImagem: Dp, val sprite: Dp)

internal val NaGrade = MedidaDaFigurinha(alturaDaImagem = 78.dp, sprite = Tamanhos.spriteLista)
internal val NaFita = MedidaDaFigurinha(alturaDaImagem = 70.dp, sprite = 64.dp)

/** A largura da figurinha na fita e a da figurinha grande, a posição atual. */
internal val LarguraNaFita = 84.dp
internal val LarguraDaGrande = 118.dp
private val ImagemDaGrande = 112.dp
internal val AlturaDaGrande = ImagemDaGrande + 30.dp

/**
 * O 3D fica inteiro dentro da imagem, abaixo do número: o GIF do Let's Go enche o quadro, e o
 * que passa da borda é cortado.
 */
private val TopoDoTresD = 18.dp

/**
 * A inclinação fixa da figurinha colada, de −1,5° a +1,5° em meio grau, pelo número: a
 * mesma figurinha cai sempre do mesmo jeito, e vizinhas quase nunca repetem.
 */
internal fun inclinacaoDe(numero: Int): Float = ((numero * 37) % 7 - 3) * 0.5f

/** A descrição falada: `#058 Growlithe, Fogo, alcançado`. */
@Composable
internal fun descricaoDaFigurinha(numero: Int, nome: String?, tipos: List<TipoDePokemon>, colada: Boolean, extra: String? = null): String {
    val partes = buildList {
        add(listOfNotNull(stringResource(R.string.especie_numero, numero), nome).joinToString(" "))
        if (colada) tipos.forEach { add(stringResource(nomeDoTipo(it))) }
        add(stringResource(if (colada) R.string.pokedex_item_alcancada else R.string.pokedex_item_nao_alcancada))
        if (extra != null) add(extra)
    }
    return partes.joinToString(", ")
}

/**
 * A figurinha colada (docs/03 §3.18): o sprite de jogo sobre `tipo-*-suave`, com borda
 * branca, inclinação fixa e sombra curta de objeto. Número no canto, ícone do tipo primário
 * no outro canto e o nome embaixo.
 *
 * @param nova o selo `nova`, das coladas nesta semana.
 * @param brilho a posição do reflexo holográfico (de 0 a 1), só no Tier 7. Lida no desenho,
 *   para a rolagem não recompor a grade.
 * @param emDobro a posição atual: 2×2 na grade, com o 3D em loop.
 */
@Composable
internal fun FigurinhaColada(
    numero: Int,
    nome: String?,
    tipos: List<TipoDePokemon>,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    medida: MedidaDaFigurinha = NaGrade,
    nova: Boolean = false,
    brilho: (() -> Float)? = null,
    emDobro: Boolean = false,
    batida: Int? = null,
    lugar: String? = null,
) {
    val tipo = tipos.firstOrNull()
    val suave = tipo?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada
    val descricao = descricaoDaFigurinha(numero, nome, tipos, colada = true)
    val voo = LocalFigurinhaEmVoo.current
    val aoTocar = {
        if (lugar != null) voo.marcar(numero, lugar)
        aoTocar()
    }

    Box(
        modifier = modifier
            .vooCompartilhado(chaveDaFigurinha(numero), lugar != null && voo.numero == numero && voo.lugar == lugar)
            .batida(batida)
            .graphicsLayer { rotationZ = if (emDobro) -1f else inclinacaoDe(numero) }
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { aoTocar(); true }
            },
    ) {
        Column(
            modifier = Modifier
                .then(if (emDobro) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .shadow(SombraDaFigurinha, FormaDaFigurinha, clip = false, spotColor = MaterialTheme.colorScheme.onSurface)
                .background(MaterialTheme.colorScheme.surface, FormaDaFigurinha)
                .clickable(onClick = aoTocar)
                .padding(if (emDobro) BordaDaFigurinha + 1.dp else BordaDaFigurinha),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (emDobro) Modifier.weight(1f) else Modifier.height(medida.alturaDaImagem))
                    .then(if (brilho != null) Modifier.reflexo(brilho) else Modifier)
                    .background(suave, FormaDaImagem),
                contentAlignment = Alignment.Center,
            ) {
                if (emDobro) {
                    SpriteAnimado(
                        numero = numero,
                        paraNoReduzido = true,
                        modifier = Modifier.fillMaxSize().padding(start = 4.dp, end = 4.dp, top = TopoDoTresD + 4.dp, bottom = 4.dp),
                    )
                } else {
                    SpriteDeJogo(numero, Modifier.size(medida.sprite))
                }
                NumeroDaFigurinha(numero, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.align(Alignment.TopStart))
                if (tipo != null) {
                    IconeDeTipo(tipo, Modifier.align(Alignment.TopEnd).padding(5.dp), lado = IconeNaFigurinha)
                }
            }
            NomeDaFigurinha(nome, MaterialTheme.colorScheme.onSurface, emDobro, FontWeight.Bold)
        }
        if (nova) SeloNova(Modifier.align(Alignment.TopCenter))
    }
}

/**
 * A vaga (docs/03 §3.18): bloco cheio, reto e sem sombra, com a silhueta chapada e o número e
 * o nome visíveis, que nunca se escondem. A vaga do próximo leva contorno em tinta e o km.
 *
 * @param sobreTinta a vaga na fita da capa: buraco claro na tinta, silhueta branca.
 * @param falta o `~6 km` da vaga do próximo, ou nulo.
 */
@Composable
internal fun FigurinhaVaga(
    numero: Int,
    nome: String?,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    medida: MedidaDaFigurinha = NaGrade,
    sobreTinta: Boolean = false,
    proximo: Boolean = false,
    falta: String? = null,
    emDobro: Boolean = false,
    descricaoExtra: String? = null,
    lugar: String? = null,
) {
    val voo = LocalFigurinhaEmVoo.current
    val aoTocar = {
        if (lugar != null) voo.marcar(numero, lugar)
        aoTocar()
    }
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val fundo = if (sobreTinta) claro.copy(alpha = 0.07f) else LocalCoresPokerun.current.superficieAfundada
    val texto = if (sobreTinta) claro.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
    val silhueta = if (sobreTinta) claro.copy(alpha = 0.18f) else LocalCoresPokerun.current.silhueta
    val contorno = if (sobreTinta) claro else tinta
    val descricao = descricaoDaFigurinha(numero, nome, emptyList(), colada = false, extra = descricaoExtra)

    Box(
        modifier = modifier
            .vooCompartilhado(chaveDaFigurinha(numero), lugar != null && voo.numero == numero && voo.lugar == lugar)
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { aoTocar(); true }
            },
    ) {
        Column(
            modifier = Modifier
                .then(if (emDobro) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .background(fundo, FormaDaFigurinha)
                .then(if (proximo) Modifier.border(2.dp, contorno, FormaDaFigurinha) else Modifier)
                .clickable(onClick = aoTocar)
                .padding(BordaDaFigurinha),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (emDobro) Modifier.weight(1f) else Modifier.height(medida.alturaDaImagem)),
                contentAlignment = Alignment.Center,
            ) {
                val filtro = remember(silhueta) { ColorFilter.tint(silhueta, BlendMode.SrcIn) }
                if (emDobro) {
                    SpriteAnimado(numero = numero, paraNoReduzido = true, silhueta = filtro, modifier = Modifier.size(170.dp))
                } else {
                    SpriteDeJogo(numero, Modifier.size(medida.sprite), filtro)
                }
                NumeroDaFigurinha(numero, texto, Modifier.align(Alignment.TopStart))
                if (falta != null) {
                    PilulaDaFalta(falta, sobreTinta, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                }
            }
            NomeDaFigurinha(nome, texto, emDobro, FontWeight.SemiBold)
        }
    }
}

/**
 * A posição atual na fita da capa: figurinha grande (118dp) com o 3D saindo por cima. Se o
 * pacote ainda não abriu, é vaga grande, com a silhueta do 3D.
 */
@Composable
internal fun FigurinhaGrande(
    numero: Int,
    nome: String?,
    tipos: List<TipoDePokemon>,
    colada: Boolean,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    nova: Boolean = false,
    batida: Int? = null,
) {
    val voo = LocalFigurinhaEmVoo.current
    val aoTocar = {
        voo.marcar(numero, "fita")
        aoTocar()
    }
    val tipo = tipos.firstOrNull()
    val claro = MaterialTheme.colorScheme.surface
    val suave = tipo?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada
    val descricao = descricaoDaFigurinha(numero, nome, tipos, colada, stringResource(R.string.pokedex_item_atual))

    Box(
        modifier = modifier
            .vooCompartilhado(chaveDaFigurinha(numero), voo.numero == numero && voo.lugar == "fita")
            .size(width = LarguraDaGrande, height = AlturaDaGrande)
            .batida(if (colada) batida else null)
            .graphicsLayer { rotationZ = if (colada) -2f else 0f }
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { aoTocar(); true }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (colada) {
                        Modifier.shadow(SombraDaFigurinha, FormaDaFigurinha, clip = false).background(claro, FormaDaFigurinha)
                    } else {
                        Modifier.background(claro.copy(alpha = 0.07f), FormaDaFigurinha)
                    },
                )
                .clickable(onClick = aoTocar)
                .padding(BordaDaFigurinha),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(if (colada) Modifier.background(suave, FormaDaImagem) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                val silhueta = remember(colada) { if (colada) null else ColorFilter.tint(Color.White.copy(alpha = 0.18f), BlendMode.SrcIn) }
                SpriteAnimado(
                    numero = numero,
                    paraNoReduzido = true,
                    silhueta = silhueta,
                    modifier = Modifier.fillMaxSize().padding(start = 2.dp, end = 2.dp, top = TopoDoTresD, bottom = 2.dp),
                )
                NumeroDaFigurinha(
                    numero,
                    if (colada) MaterialTheme.colorScheme.onSurfaceVariant else claro.copy(alpha = 0.6f),
                    Modifier.align(Alignment.TopStart),
                )
                if (colada && tipo != null) {
                    IconeDeTipo(tipo, Modifier.align(Alignment.TopEnd).padding(5.dp), lado = IconeNaFigurinha)
                }
            }
            NomeDaFigurinha(
                nome,
                if (colada) MaterialTheme.colorScheme.onSurface else claro.copy(alpha = 0.6f),
                emDobro = false,
                peso = FontWeight.Bold,
            )
        }
        if (nova) SeloNova(Modifier.align(Alignment.TopCenter))
    }
}

/**
 * A minifigurinha do maço da página fechada e da evolução: 36×44, inclinada. A vaga é bloco
 * claro com a silhueta, recortado da figurinha de cima por um anel branco. O sprite é maior
 * que a mini e fica recortado dentro dela: sem o recorte, a silhueta vazava sobre a vizinha.
 */
@Composable
internal fun MiniFigurinha(numero: Int, tipo: TipoDePokemon?, vaga: Boolean, inclinacao: Float, modifier: Modifier = Modifier) {
    val claro = MaterialTheme.colorScheme.surface
    val cores = LocalCoresPokerun.current
    val forma = RoundedCornerShape(7.dp)
    val suave = tipo?.let { LocalCoresDeTipo.current[it].suave } ?: cores.superficieAfundada
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 44.dp)
            .graphicsLayer { rotationZ = inclinacao }
            .then(
                if (vaga) {
                    Modifier.border(2.dp, claro, forma).background(cores.superficieAfundada, forma)
                } else {
                    Modifier.shadow(2.dp, forma).background(claro, forma)
                },
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (vaga) Color.Transparent else suave),
        contentAlignment = Alignment.Center,
    ) {
        val filtro = remember(vaga) { if (vaga) ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) else null }
        // O sprite de 96px tem margem transparente: a 40dp, centrado, o Pokémon inteiro cabe na mini.
        SpriteDeJogo(numero, Modifier.requiredSize(40.dp), filtro)
    }
}

/** O sprite de jogo de 96px, ampliado sem suavização (docs/02 §4.4). */
@Composable
internal fun SpriteDeJogo(numero: Int, modifier: Modifier = Modifier, filtro: ColorFilter? = null) {
    AsyncImage(
        model = spriteDeJogoDe(numero),
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = filtro,
        modifier = modifier,
    )
}

@Composable
private fun NumeroDaFigurinha(numero: Int, cor: Color, modifier: Modifier) {
    Text(
        text = stringResource(R.string.especie_numero, numero),
        style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily)),
        color = cor,
        modifier = modifier.padding(start = 6.dp, top = 3.dp),
    )
}

@Composable
private fun NomeDaFigurinha(nome: String?, cor: Color, emDobro: Boolean, peso: FontWeight) {
    Text(
        // Sem nome ainda (sem rede), a linha fica, para as figurinhas da linha terem a mesma altura.
        text = nome.orEmpty(),
        style = naFigura(if (emDobro) Estilos.tituloSecao else Estilos.apoio.copy(fontWeight = peso)),
        color = cor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 2.dp),
    )
}

/** O selo `nova`: pílula em `destaque` na borda de cima, recortada da página por um anel branco. */
@Composable
private fun SeloNova(modifier: Modifier) {
    val claro = MaterialTheme.colorScheme.surface
    Text(
        text = stringResource(R.string.pokedex_nova),
        style = naFigura(Estilos.legenda),
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .offset(y = (-9).dp)
            .border(2.dp, claro, RoundedCornerShape(50))
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/** O `~6 km` da vaga do próximo: pílula em tinta (na fita, em branco com o texto em tinta). */
@Composable
private fun PilulaDaFalta(texto: String, sobreTinta: Boolean, modifier: Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    Text(
        text = texto,
        style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily)),
        color = if (sobreTinta) tinta else claro,
        maxLines = 1,
        modifier = modifier
            .background(if (sobreTinta) claro else tinta, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * O reflexo holográfico do Tier 7, o topo alcançável (docs/02 §9.1, "Aurora e brilho"):
 * uma faixa diagonal amarela, rosa e azul em `ColorDodge` sobre a imagem, na posição que
 * [posicao] dá. A leitura fica no desenho.
 */
@Composable
private fun Modifier.reflexo(posicao: () -> Float): Modifier {
    val cores = LocalCoresPokerun.current
    val ouro = cores.brilhoOuro.copy(alpha = 0.75f)
    val rosa = cores.brilhoRosa.copy(alpha = 0.75f)
    val azul = cores.brilhoAzul.copy(alpha = 0.75f)
    return this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .clip(FormaDaImagem)
    .drawWithContent {
        drawContent()
        val faixa = size.width * 2.6f
        val inicio = -(faixa - size.width) * posicao().coerceIn(0f, 1f)
        val pincel = Brush.linearGradient(
            0.20f to Color.Transparent,
            0.32f to Color.Transparent,
            0.42f to ouro,
            0.50f to rosa,
            0.58f to azul,
            0.70f to Color.Transparent,
            start = Offset(inicio, 0f),
            end = Offset(inicio + faixa, faixa * 0.47f),
        )
        drawRect(pincel, alpha = 0.75f, blendMode = BlendMode.ColorDodge)
    }
}
