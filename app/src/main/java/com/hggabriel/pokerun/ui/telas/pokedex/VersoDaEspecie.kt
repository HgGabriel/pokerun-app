package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.formatarPace
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/**
 * O verso da figurinha (docs/03 §3.19), em cartões brancos. Colada: a corrida que a colou,
 * a linha de velocidade, a evolução e altura e peso. Não alcançada: o km que falta, a linha
 * de velocidade e os outros dois trancados até colar.
 *
 * @param colada quais números estão colados, para a evolução mostrar vaga onde é vaga.
 */
@Composable
internal fun VersoDaEspecie(
    pagina: PaginaDaEspecie,
    fuso: java.time.ZoneId,
    colada: (Int) -> Boolean,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoAbrirCorrida: (String) -> Unit,
    aoIrPara: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nome = pagina.nome ?: stringResource(R.string.especie_numero, pagina.numero)
    val linha = linhaDeVelocidade(pagina.numero)?.let { stringResource(it, nome) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Espacos.margem),
        verticalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        if (pagina.colada) {
            pagina.coladaEm?.let { ColadaEm(it, fuso, aoAbrirCorrida) }
            linha?.let { LinhaDeVelocidade(it) }
            if (pagina.evolucao.flatten().size > 1) Evolucao(pagina, colada, nomes, tipos, aoIrPara)
            pagina.ficha?.let { ficha ->
                Cartao {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacos.interno)) {
                        Medida(stringResource(R.string.pokedex_altura), stringResource(R.string.pokedex_metros, decimal(ficha.alturaM)), Modifier.weight(1f))
                        Medida(stringResource(R.string.pokedex_peso), stringResource(R.string.pokedex_quilos, decimal(ficha.pesoKg)), Modifier.weight(1f))
                    }
                }
            }
        } else {
            Falta(pagina)
            linha?.let { LinhaDeVelocidade(it) }
            Trancado(stringResource(R.string.pokedex_trancado_evolucao), 84.dp)
            Trancado(stringResource(R.string.pokedex_trancado_ficha), 72.dp)
        }
    }
}

private fun decimal(valor: Double): String =
    if (valor == Math.floor(valor)) String.format(LocaleDoApp, "%.0f", valor) else String.format(LocaleDoApp, "%.1f", valor)

/** A corrida que colou a figurinha: o dia, o km e o pace. Tocar abre a corrida. */
@Composable
private fun ColadaEm(corrida: com.hggabriel.pokerun.dominio.modelo.Corrida, fuso: java.time.ZoneId, aoAbrirCorrida: (String) -> Unit) {
    val dia = diaDaCorrida(corrida, fuso)
    val km = formatarKm(corrida.km)
    val pace = formatarPace(corrida.km, corrida.duracaoSeg)
    val descricao = stringResource(R.string.pokedex_colada_descricao, dia, km, pace ?: "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { aoAbrirCorrida(corrida.id) }
            .clearAndSetSemantics {
                contentDescription = descricao
                role = Role.Button
                onClick { aoAbrirCorrida(corrida.id); true }
            }
            .padding(Espacos.margem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RotuloDoCartao(stringResource(R.string.pokedex_colada_em))
            Text(dia, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = Espacos.minimo)) {
                Valor(km, stringResource(R.string.painel_unidade_km))
                if (pace != null) Valor(pace, stringResource(R.string.pokedex_por_km))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Valor(numero: String, unidade: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(numero, style = Estilos.numeroM, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(3.dp))
        Text(unidade, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun LinhaDeVelocidade(texto: String) {
    Cartao {
        Text(texto, style = Estilos.tituloSecao, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Medida(rotulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        RotuloDoCartao(rotulo)
        Text(valor, style = Estilos.numeroS, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * A evolução: minifigurinhas com setas, estágio a estágio. A espécie aberta leva anel em
 * tinta; tocar noutra vai até a página dela.
 */
@Composable
private fun Evolucao(
    pagina: PaginaDaEspecie,
    colada: (Int) -> Boolean,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoIrPara: (Int) -> Unit,
) {
    val todas = pagina.evolucao.flatten()
    val lado = if (todas.size <= 3) 72.dp else 52.dp
    Cartao {
        RotuloDoCartao(stringResource(R.string.pokedex_evolucao))
        Spacer(Modifier.height(Espacos.relacionado))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pagina.evolucao.forEachIndexed { i, estagio ->
                if (i > 0) Seta()
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
                    estagio.forEach { n ->
                        Elo(n, n == pagina.numero, colada(n), nomes[n], tipos[n]?.firstOrNull(), lado) { aoIrPara(n) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Elo(numero: Int, aqui: Boolean, colada: Boolean, nome: String?, tipo: TipoDePokemon?, lado: Dp, aoTocar: () -> Unit) {
    val cores = LocalCoresPokerun.current
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    val forma = RoundedCornerShape(12.dp)
    val descricao = descricaoDaFigurinha(numero, nome, listOfNotNull(tipo), colada)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(enabled = !aqui, onClick = aoTocar)
            .clearAndSetSemantics {
                contentDescription = descricao
                if (!aqui) {
                    role = Role.Button
                    onClick { aoTocar(); true }
                }
            },
    ) {
        // O anel da espécie aberta fica por fora: as minis da cadeia têm todas o mesmo tamanho.
        Box(
            modifier = Modifier
                .then(if (aqui) Modifier.border(2.dp, tinta, RoundedCornerShape(16.dp)) else Modifier)
                .padding(4.dp)
                .size(lado, lado + lado / 6)
                .background(if (colada) claro else cores.superficieAfundada, forma)
                .then(if (colada) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma) else Modifier)
                .padding(3.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (colada) tipo?.let { LocalCoresDeTipo.current[it].suave } ?: cores.superficieAfundada else cores.superficieAfundada),
            contentAlignment = Alignment.Center,
        ) {
            val filtro = remember(colada) { if (colada) null else ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) }
            SpriteDeJogo(numero, Modifier.requiredSize(lado + 8.dp), filtro)
        }
        Text(
            nome.orEmpty(),
            style = naFigura(Estilos.legenda),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun Seta() {
    val cor = MaterialTheme.colorScheme.outline
    Canvas(Modifier.size(20.dp)) {
        val u = size.width / 24f
        val traco = 1.75f * u
        drawLine(cor, Offset(5 * u, 12 * u), Offset(19 * u, 12 * u), traco, StrokeCap.Round)
        drawLine(cor, Offset(13 * u, 6 * u), Offset(19 * u, 12 * u), traco, StrokeCap.Round)
        drawLine(cor, Offset(13 * u, 18 * u), Offset(19 * u, 12 * u), traco, StrokeCap.Round)
    }
}

/** O que falta para a não alcançada: `~790 km` e a barra do XP já feito. */
@Composable
private fun Falta(pagina: PaginaDaEspecie) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.pokedex_falta))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacos.interno),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            pagina.kmFalta?.let { Valor(stringResource(R.string.pokedex_aproximado, formatarKm(it)), stringResource(R.string.painel_unidade_km)) }
            BarraDePilula(pagina.fracao, cor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        }
    }
}

/** O cartão trancado do verso da vaga: libera ao colar. */
@Composable
private fun Trancado(descricao: String, altura: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .graphicsLayer { }
            .clearAndSetSemantics { contentDescription = descricao },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
