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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/**
 * Uma vaga do álbum como a página a desenha.
 *
 * @property colada a figurinha está na página. Fica falsa até o pacote abrir, mesmo que a
 *   coleção já a tenha: o pacote é que cola.
 * @property atual a posição do corredor, a figurinha em dobro.
 * @property proximo a vaga marcada pelo km.
 * @property nova colada nesta semana: o selo `nova`.
 */
@Immutable
internal data class FigurinhaDoAlbum(
    val numero: Int,
    val posicao: Int,
    val colada: Boolean,
    val atual: Boolean = false,
    val proximo: Boolean = false,
    val nova: Boolean = false,
)

/**
 * Uma página do álbum: um tier da temporada.
 *
 * @property km o km corrido no tier, ou nulo antes da primeira figurinha colada nele.
 * @property lacrada o tier que a temporada não deixa alcançar (XP-11): vira envelope.
 */
@Immutable
internal data class PaginaDoAlbum(
    val ordem: Int,
    val nome: String,
    val figurinhas: List<FigurinhaDoAlbum>,
    val km: Double?,
    val lacrada: Boolean = false,
) {
    val coladas: Int get() = figurinhas.count { it.colada }
    val total: Int get() = figurinhas.size
}

/** As inclinações do maço, da primeira à quarta mini. */
private val InclinacoesDoMaco = listOf(-4f, 3f, -2f, 5f)
private const val MINIS_NO_MACO = 4

/** O rótulo do tier em caixa alta: `TIER 4 · RITMO SUSTENTADO`. */
@Composable
private fun rotuloDoTier(pagina: PaginaDoAlbum): String =
    stringResource(R.string.escada_tier_nome, pagina.ordem, pagina.nome).uppercase(LocaleDoApp)

/** `21 de 21 · 18 km`, ou só `0 de 25` antes da primeira. */
@Composable
private fun legendaDaPagina(pagina: PaginaDoAlbum): String {
    val de = stringResource(R.string.pokedex_de, pagina.coladas, pagina.total)
    val km = pagina.km?.takeIf { pagina.coladas > 0 && it > 0 }?.let { stringResource(R.string.pokedex_km, formatarKm(it)) }
    return listOfNotNull(de, km).joinToString(" · ")
}

/**
 * A página fechada: o rótulo do tier, a legenda e o maço de 4 minifigurinhas com `+17`.
 * Nas páginas completas, o maço é de coladas; nas não começadas, de vagas. O toque abre a
 * página no lugar.
 */
@Composable
internal fun PaginaFechada(
    pagina: PaginaDoAlbum,
    tipos: Map<Int, List<TipoDePokemon>>,
    aoAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotulo = rotuloDoTier(pagina)
    val legenda = legendaDaPagina(pagina)
    val acao = stringResource(R.string.pokedex_abrir_pagina)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = aoAbrir)
            .clearAndSetSemantics {
                contentDescription = "$rotulo, $legenda"
                role = Role.Button
                onClick(label = acao) { aoAbrir(); true }
            }
            .heightIn(min = 76.dp)
            .padding(start = Espacos.margem, end = Espacos.interno, top = Espacos.interno, bottom = Espacos.interno),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(rotulo, style = Estilos.rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Spacer(Modifier.height(2.dp))
            Text(legenda, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Maco(pagina, tipos)
    }
}

@Composable
private fun Maco(pagina: PaginaDoAlbum, tipos: Map<Int, List<TipoDePokemon>>) {
    // O maço mostra o que a página tem: coladas primeiro; sem nenhuma, as vagas do começo.
    val mostradas = (pagina.figurinhas.filter { it.colada } + pagina.figurinhas.filterNot { it.colada }).take(MINIS_NO_MACO)
    // As minis se sobrepõem 8dp: é um maço, não uma fila.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        modifier = Modifier.padding(start = 14.dp),
    ) {
        mostradas.forEachIndexed { i, f ->
            MiniFigurinha(
                numero = f.numero,
                tipo = tipos[f.numero]?.firstOrNull(),
                vaga = !f.colada,
                inclinacao = InclinacoesDoMaco[i],
            )
        }
        val resto = pagina.total - mostradas.size
        if (resto > 0) {
            Text(
                text = stringResource(R.string.pokedex_mais, resto),
                style = naFigura(Estilos.legenda),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

/**
 * A página aberta: o cabeçalho com `16 de 25`, o km do tier sobre a barra e a grade.
 *
 * @param falta o `~6 km` da vaga do próximo, quando ela está nesta página.
 * @param brilho a posição do reflexo do Tier 7, ou nulo nas outras páginas.
 */
@Composable
internal fun PaginaAberta(
    pagina: PaginaDoAlbum,
    nomes: Map<Int, String>,
    tipos: Map<Int, List<TipoDePokemon>>,
    colunas: Int,
    falta: String?,
    aoAbrirEspecie: (Int) -> Unit,
    modifier: Modifier = Modifier,
    brilho: (() -> Float)? = null,
) {
    val rotulo = rotuloDoTier(pagina)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Espacos.margem)
            .semantics { isTraversalGroup = true },
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Espacos.interno)) {
            Text(rotulo, style = Estilos.rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.weight(1f).padding(top = 4.dp))
            Text(
                text = stringResource(R.string.pokedex_de, pagina.coladas, pagina.total),
                style = Estilos.numeroS,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacos.interno),
            modifier = Modifier.padding(top = 6.dp, bottom = Espacos.margem),
        ) {
            val km = pagina.km?.takeIf { pagina.coladas > 0 && it > 0 }
            if (km != null) {
                Text(stringResource(R.string.pokedex_km, formatarKm(km)), style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface)
            }
            BarraDePilula(
                fracao = if (pagina.total == 0) 0f else pagina.coladas.toFloat() / pagina.total,
                cor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
        GradeDoAlbum(emDobro = pagina.figurinhas.map { it.atual }, colunas = colunas) {
            pagina.figurinhas.forEachIndexed { i, f ->
                // A grade densa pode trocar a ordem visual; a falada segue o número.
                val ordem = Modifier.semantics { traversalIndex = i.toFloat() }
                if (f.colada) {
                    FigurinhaColada(
                        numero = f.numero,
                        nome = nomes[f.numero],
                        tipos = tipos[f.numero].orEmpty(),
                        aoTocar = { aoAbrirEspecie(f.numero) },
                        nova = f.nova,
                        brilho = brilho,
                        emDobro = f.atual,
                        modifier = ordem,
                    )
                } else {
                    FigurinhaVaga(
                        numero = f.numero,
                        nome = nomes[f.numero],
                        aoTocar = { aoAbrirEspecie(f.numero) },
                        proximo = f.proximo,
                        falta = if (f.proximo) falta else null,
                        emDobro = f.atual,
                        modifier = ordem,
                    )
                }
            }
        }
    }
}

/**
 * O Tier 8, lacrado (XP-11): um envelope com a aba triangular em `superficie-afundada`, o
 * lacre em tinta com cadeado e uma janela com as 5 silhuetas, com número e nome legíveis.
 * Não abre e não promete vaga.
 */
@Composable
internal fun EnvelopeLacrado(pagina: PaginaDoAlbum, nomes: Map<Int, String>, modifier: Modifier = Modifier) {
    val cores = LocalCoresPokerun.current
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val rotulo = rotuloDoTier(pagina)
    val descricao = stringResource(R.string.pokedex_envelope, rotulo)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(claro)
            .clearAndSetSemantics { contentDescription = descricao },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
            val aba = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(aba, cores.superficieAfundada)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp)
                .size(42.dp)
                .border(4.dp, claro, RoundedCornerShape(50))
                .padding(4.dp)
                .background(tinta, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = claro, modifier = Modifier.size(16.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, end = Espacos.margem, top = 86.dp, bottom = Espacos.margem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(rotulo, style = Estilos.rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Espacos.interno))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cores.superficieAfundada, RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp, vertical = Espacos.interno),
            ) {
                val filtro = remember(cores.silhueta) { ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) }
                pagina.figurinhas.forEach { f ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        SpriteDeJogo(f.numero, Modifier.size(52.dp), filtro)
                        Text(
                            stringResource(R.string.especie_numero, f.numero),
                            style = naFigura(Estilos.legenda),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            nomes[f.numero].orEmpty(),
                            style = naFigura(Estilos.legenda.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
