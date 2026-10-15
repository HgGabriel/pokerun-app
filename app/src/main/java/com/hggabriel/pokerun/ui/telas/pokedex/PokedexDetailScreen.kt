package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.FichaDeEspecime
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Tamanhos
import androidx.compose.ui.unit.dp

private val Goteira = 16.dp

/** O que a tela mostra de uma espécie (docs/03 §3.19). */
data class DetalheDaEspecie(
    val celula: CelulaDaPokedex,
    val nome: String?,
    val tipos: List<TipoDePokemon>,
    val arte: String,
    /** A frase da conquista, já com a semana, ou nulo se a espécie ainda não foi alcançada. */
    val conquista: ConquistaDaEspecie?,
)

/** Quando a espécie entrou na coleção: a semana do plano, e o período dela se já se sabe. */
data class ConquistaDaEspecie(val numeroDaSemana: Int, val primeiroDia: java.time.LocalDate?, val ultimoDia: java.time.LocalDate?)

/**
 * A linha de velocidade da criatura (docs/07, `03 §3.19`): só as espécies de que o
 * documento de curadoria escreve uma. O resto das espécies não tem linha, e a tela não
 * inventa uma. Devolve o recurso, que leva o nome da espécie.
 */
@StringRes
fun linhaDeVelocidade(numero: Int): Int? = when (numero) {
    18 -> R.string.velocidade_018
    22 -> R.string.velocidade_022
    51 -> R.string.velocidade_051
    53 -> R.string.velocidade_053
    59 -> R.string.velocidade_059
    78 -> R.string.velocidade_078
    85 -> R.string.velocidade_085
    121 -> R.string.velocidade_121
    135 -> R.string.velocidade_135
    142 -> R.string.velocidade_142
    149 -> R.string.velocidade_149
    else -> null
}

/**
 * A `PokedexDetailScreen` (`F4-T08`, docs/03 §3.19): a ficha de espécie como herói, com a
 * arte oficial em `arteDetalhe` (232dp), número, nome, tier e a linha de velocidade da
 * criatura; se alcançada, a semana; se não, **quanto XP falta**.
 *
 * O `contentDescription` do herói diz nome e situação (`Growlithe, ainda não alcançado`).
 * A silhueta da espécie não alcançada é preenchimento chapado, não opacidade (docs/03
 * §3.18). A tela não grava nada.
 */
@Composable
fun PokedexDetailScreen(numero: Int, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: PokedexDetalheViewModel = viewModel(key = "especie-$numero") {
        PokedexDetalheViewModel(
            numero,
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.planoRepositorio,
            container.especiesRepositorio,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    PokedexDetailScreen(estado = estado, numero = numero, aoTentarDeNovo = vm::tentarDeNovo, modifier = modifier)
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
internal fun PokedexDetailScreen(
    estado: DetalheUiState,
    numero: Int,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conteudo = estado as? DetalheUiState.Conteudo
    val titulo = conteudo?.especie?.let { it.nome ?: stringResource(R.string.especie_numero, it.celula.numero) }
        ?: stringResource(R.string.especie_numero, numero)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Espacos.bloco)) {
            item {
                CabecalhoDeFicha(sobrancelha = listOf(stringResource(R.string.pokedex_titulo)), titulo = titulo)
                Spacer(Modifier.height(Espacos.bloco))
            }
            when (estado) {
                DetalheUiState.Carregando -> Unit
                DetalheUiState.Falhou -> item {
                    Column(modifier = Modifier.padding(horizontal = Goteira)) {
                        Text(
                            text = stringResource(R.string.pokedex_detalhe_erro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(Espacos.bloco))
                        BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_erro_repetir))
                        }
                    }
                }
                is DetalheUiState.Conteudo -> item { Heroi(estado.especie) }
            }
        }
    }
}

@Composable
private fun Heroi(especie: DetalheDaEspecie) {
    val cores = LocalCoresPokerun.current
    val celula = especie.celula
    val alcancada = celula.estado != EstadoDaCelula.NAO_ALCANCADA
    val numero = stringResource(R.string.especie_numero, celula.numero)
    val nome = especie.nome ?: numero
    val linha = linhaDeVelocidade(celula.numero)?.let { stringResource(it, nome) }
    var semArte by remember(especie.arte) { mutableStateOf(false) }

    val situacao = stringResource(
        when (celula.estado) {
            EstadoDaCelula.ATUAL -> R.string.pokedex_item_atual
            EstadoDaCelula.ALCANCADA -> R.string.pokedex_item_alcancada
            EstadoDaCelula.NAO_ALCANCADA -> R.string.pokedex_item_nao_alcancada
        },
    )
    val falta = String.format(LocaleDoApp, "%,d", celula.faltamXp)
    val frase = when {
        !alcancada -> stringResource(R.string.pokedex_detalhe_faltam, falta)
        especie.conquista != null -> {
            val c = especie.conquista
            val periodo = if (c.primeiroDia != null && c.ultimoDia != null) {
                stringResource(
                    R.string.pokedex_detalhe_conquista_periodo,
                    c.numeroDaSemana,
                    c.primeiroDia.dayOfMonth,
                    c.ultimoDia.dayOfMonth,
                    com.hggabriel.pokerun.ui.componentes.nomeDoMes(c.ultimoDia, LocaleDoApp),
                )
            } else {
                stringResource(R.string.pokedex_detalhe_conquista, c.numeroDaSemana)
            }
            periodo
        }
        else -> stringResource(R.string.pokedex_detalhe_alcancada)
    }
    val nomesDosTipos = especie.tipos.map { stringResource(nomeDoTipo(it)) }
    val tierEscrito = stringResource(R.string.escada_tier_nome, celula.tier, celula.nomeDoTier)
    val descricao = buildString {
        append(nome).append(", ").append(situacao)
        if (alcancada && nomesDosTipos.isNotEmpty()) append(", ").append(nomesDosTipos.joinToString(" e "))
        append(". ").append(tierEscrito)
        if (linha != null) append(". ").append(linha)
        append(". ").append(frase)
    }

    FichaDeEspecime(
        arte = {
            if (semArte) {
                Spacer(Modifier.size(Tamanhos.arteDetalhe))
            } else {
                AsyncImage(
                    model = especie.arte,
                    contentDescription = null,
                    modifier = Modifier.size(Tamanhos.arteDetalhe),
                    colorFilter = if (alcancada) null else ColorFilter.tint(cores.silhueta, BlendMode.SrcIn),
                    onError = { semArte = true },
                )
            }
        },
        tipo = if (alcancada) especie.tipos.firstOrNull() else null,
        capturada = alcancada,
        modifier = Modifier.padding(horizontal = Goteira).clearAndSetSemantics { contentDescription = descricao },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Espacos.margem),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            Text(
                text = stringResource(R.string.especie_numero_nome, celula.numero, nome),
                style = Estilos.nomeEspecie,
                color = if (alcancada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.escada_tier_nome, celula.tier, celula.nomeDoTier),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (alcancada && especie.tipos.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
                    especie.tipos.forEach { ChipDeTipo(it) }
                }
            }
            if (linha != null) {
                Text(
                    text = linha,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = frase,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private fun exemplo(estado: EstadoDaCelula, faltam: Long = 0L, conquista: ConquistaDaEspecie? = null) = DetalheUiState.Conteudo(
    DetalheDaEspecie(
        celula = CelulaDaPokedex(
            posicao = 87, numero = 78, tier = 7, nomeDoTier = "Velocistas Supremos",
            estado = estado, xpParaAlcancar = 41_300L, faltamXp = faltam,
        ),
        nome = "Rapidash",
        tipos = listOf(TipoDePokemon.FOGO),
        arte = "",
        conquista = conquista,
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DetalheAlcancadaPreview() = PokerunTheme {
    PokedexDetailScreen(
        exemplo(
            EstadoDaCelula.ALCANCADA,
            conquista = ConquistaDaEspecie(4, java.time.LocalDate.of(2026, 9, 22), java.time.LocalDate.of(2026, 9, 28)),
        ),
        numero = 78, aoTentarDeNovo = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DetalheNaoAlcancadaPreview() = PokerunTheme {
    PokedexDetailScreen(exemplo(EstadoDaCelula.NAO_ALCANCADA, faltam = 12_400L), numero = 78, aoTentarDeNovo = {})
}

@Preview(showBackground = true, widthDp = 320, heightDp = 900, fontScale = 2.0f)
@Composable
private fun DetalheFonteGrandePreview() = PokerunTheme {
    PokedexDetailScreen(exemplo(EstadoDaCelula.NAO_ALCANCADA, faltam = 12_400L), numero = 78, aoTentarDeNovo = {})
}
