package com.hggabriel.pokerun.ui.telas.pokedex

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.FichaDaEspecie
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/**
 * Uma página do detalhe: a espécie como o álbum a vê, com o que a PokéAPI já devolveu.
 *
 * @property kmFalta o `~790 km` da não alcançada, ou nulo sem corrida.
 * @property fracao quanto do XP dela já foi feito, para a barra do verso trancado.
 */
@Immutable
internal data class PaginaDaEspecie(
    val numero: Int,
    val posicao: Int,
    val total: Int,
    val colada: Boolean,
    val tier: Int,
    val nomeDoTier: String,
    val nome: String?,
    val tipos: List<TipoDePokemon>,
    val coladaEm: Corrida?,
    val kmFalta: Double?,
    val fracao: Float,
    val ficha: FichaDaEspecie?,
    val evolucao: List<List<Int>>,
)

private val LarguraDaAmpliada = 212.dp
private val AlturaDaAmpliada = 236.dp
private val BordaDaGrande = 6.dp
private val TresD = 290.dp
private val LarguraDaVizinha = 96.dp

/**
 * A frente da figurinha ampliada (docs/03 §3.19): de borda a borda em `tipo-*-suave`, com a
 * seta de voltar, a posição, a figurinha grande inclinada com o 3D saindo por cima, o nome
 * e as etiquetas. As vizinhas espiam nas laterais; tocar numa vai até ela.
 *
 * Tocar no Pokémon toca o grito e roda o 3D de novo. A não alcançada é a vaga grande, com
 * a silhueta chapada, sem tipo e sem grito.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FrenteDaEspecie(
    pagina: PaginaDaEspecie,
    anterior: FigurinhaDoAlbum?,
    proxima: FigurinhaDoAlbum?,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoVoltar: () -> Unit,
    aoIrPara: (Int) -> Unit,
    aoTocarGrito: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = LocalCoresPokerun.current
    val cor = pagina.tipos.firstOrNull()?.takeIf { pagina.colada }?.let { LocalCoresDeTipo.current[it] }
    val fundo = cor?.suave ?: cores.superficieAfundada
    val texto = if (pagina.colada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(fundo)
            .padding(bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(Espacos.alvoToque).padding(horizontal = Espacos.relacionado),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.editar_voltar), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                stringResource(R.string.pokedex_detalhe_topo, pagina.posicao, pagina.total).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // O palco: as vizinhas nas laterais e a figurinha grande no meio.
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            anterior?.let { Vizinha(it, nomes, tipos, aoIrPara, Modifier.align(Alignment.TopStart).offset(x = (-66).dp, y = 96.dp)) }
            proxima?.let { Vizinha(it, nomes, tipos, aoIrPara, Modifier.align(Alignment.TopEnd).offset(x = 66.dp, y = 96.dp)) }
            if (pagina.colada && cor != null) {
                FigurinhaAmpliada(pagina, lerp(cor.suave, cor.base, 0.16f), aoTocarGrito, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            } else if (pagina.colada) {
                FigurinhaAmpliada(pagina, cores.superficieAfundada, aoTocarGrito, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            } else {
                VagaAmpliada(pagina, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            }
        }

        Text(
            text = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero),
            style = Estilos.tituloTela.copy(fontSize = 28.sp, lineHeight = 34.sp),
            color = texto,
            modifier = Modifier.padding(horizontal = Espacos.margem).padding(top = 6.dp).semantics { heading() },
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = Espacos.margem).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            if (pagina.colada && pagina.tipos.isNotEmpty()) {
                pagina.tipos.forEach { ChipDeTipo(it, grande = true) }
            } else {
                // Na página tingida, o `Tipo ?` vai em branco, como a etiqueta do tier.
                ChipDeTipo(null, grande = true, fundoSemTipo = MaterialTheme.colorScheme.surface)
            }
            Text(
                stringResource(R.string.escada_tier_nome, pagina.tier, pagina.nomeDoTier).uppercase(LocaleDoApp),
                style = Estilos.rotulo.copy(letterSpacing = Estilos.rotulo.letterSpacing * 0.75f),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .heightIn(min = 28.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                    .padding(horizontal = Espacos.interno, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Vizinha(
    f: FigurinhaDoAlbum,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoIrPara: (Int) -> Unit,
    modifier: Modifier,
) {
    val m = modifier.width(LarguraDaVizinha).graphicsLayer { alpha = 0.9f }
    if (f.colada) {
        FigurinhaColada(f.numero, nomes[f.numero], tipos[f.numero].orEmpty(), aoTocar = { aoIrPara(f.numero) }, modifier = m)
    } else {
        FigurinhaVaga(f.numero, nomes[f.numero], aoTocar = { aoIrPara(f.numero) }, modifier = m)
    }
}

/** A figurinha grande, inclinada −2°, com borda branca de 6dp e o 3D de 290dp saindo por cima. */
@Composable
private fun FigurinhaAmpliada(pagina: PaginaDaEspecie, fundo: androidx.compose.ui.graphics.Color, aoTocarGrito: () -> Unit, modifier: Modifier) {
    var ciclo by remember { mutableIntStateOf(0) }
    val nome = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero)
    val descricao = stringResource(R.string.pokedex_tocar_grito, nome)
    val tocar = {
        ciclo++
        aoTocarGrito()
    }
    Box(
        modifier = modifier
            .size(LarguraDaAmpliada, AlturaDaAmpliada)
            .graphicsLayer { rotationZ = -2f }
            .shadow(10.dp, RoundedCornerShape(20.dp), clip = false, spotColor = MaterialTheme.colorScheme.onSurface)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(BordaDaGrande),
    ) {
        Box(Modifier.fillMaxSize().background(fundo, RoundedCornerShape(15.dp)))
        Text(
            stringResource(R.string.especie_numero, pagina.numero),
            style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily, fontSize = 13.sp)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp, top = 6.dp),
        )
        SpriteAnimado(
            numero = pagina.numero,
            umCiclo = true,
            ciclo = ciclo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .requiredSize(TresD)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = tocar)
                .clearAndSetSemantics {
                    contentDescription = descricao
                    role = Role.Button
                    onClick { tocar(); true }
                },
        )
        SeloDeSom(Modifier.align(Alignment.BottomEnd).offset(x = 16.dp, y = 16.dp))
    }
}

/** A vaga grande da não alcançada: bloco cheio com a silhueta chapada do render do HOME. */
@Composable
private fun VagaAmpliada(pagina: PaginaDaEspecie, modifier: Modifier) {
    val cores = LocalCoresPokerun.current
    val nome = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero)
    val descricao = pagina.kmFalta?.let { stringResource(R.string.pokedex_vaga_descricao_km, nome, formatarKm(it)) }
        ?: stringResource(R.string.pokedex_vaga_descricao, nome)
    Box(
        modifier = modifier
            .size(LarguraDaAmpliada, AlturaDaAmpliada)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clearAndSetSemantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        val filtro = remember(cores.silhueta) { ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) }
        AsyncImage(
            model = com.hggabriel.pokerun.dados.rede.spritesAnimadosDe(pagina.numero)[4],
            contentDescription = null,
            colorFilter = filtro,
            modifier = Modifier.size(210.dp),
        )
        Text(
            stringResource(R.string.especie_numero, pagina.numero),
            style = naFigura(Estilos.legenda.copy(fontFamily = Estilos.rotulo.fontFamily, fontSize = 13.sp)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp),
        )
    }
}

/** O selo de som no canto da figurinha: diz que o Pokémon tem voz. Decorativo. */
@Composable
private fun SeloDeSom(modifier: Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(4.dp, FormaCirculo)
            .background(MaterialTheme.colorScheme.surface, FormaCirculo),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val u = size.width / 24f
            val alto = Path().apply {
                moveTo(4 * u, 9 * u); lineTo(8 * u, 9 * u); lineTo(13 * u, 5 * u)
                lineTo(13 * u, 19 * u); lineTo(8 * u, 15 * u); lineTo(4 * u, 15 * u); close()
            }
            drawPath(alto, tinta)
            val traco = Stroke(1.75f * u, cap = StrokeCap.Round)
            drawArc(tinta, -45f, 90f, false, Offset(11.5f * u, 8.5f * u), androidx.compose.ui.geometry.Size(7f * u, 7f * u), style = traco)
            drawArc(tinta, -45f, 90f, false, Offset(9.5f * u, 4f * u), androidx.compose.ui.geometry.Size(13f * u, 16f * u), style = traco)
        }
    }
}

/**
 * Toca o grito da espécie (`cries.latest`): o primeiro som de conteúdo do app, resposta a
 * um toque explícito. Só toca com a campainha no modo normal; no silencioso e no vibrar,
 * fica calado. Um grito novo corta o anterior.
 */
@Composable
internal fun rememberTocadorDeGrito(): (String) -> Unit {
    val contexto = LocalContext.current
    val tocador = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) {
        onDispose {
            tocador[0]?.release()
            tocador[0] = null
        }
    }
    return remember(contexto) {
        { endereco ->
            val audio = contexto.getSystemService(AudioManager::class.java)
            if (audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                tocador[0]?.release()
                tocador[0] = try {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        setDataSource(endereco)
                        setOnPreparedListener { it.start() }
                        setOnErrorListener { _, _, _ -> true }
                        prepareAsync()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

/** `Quinta, 23 de out`, no fuso da temporada. */
internal fun diaDaCorrida(corrida: Corrida, fuso: java.time.ZoneId): String {
    val dia = corrida.dataHoraInicio.atZone(fuso).toLocalDate()
    val semana = com.hggabriel.pokerun.ui.componentes.nomeDoDia(dia.dayOfWeek).substringBefore('-')
        .replaceFirstChar { it.titlecase(LocaleDoApp) }
    return "$semana, ${dia.dayOfMonth} de ${com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes(dia)}"
}
