package com.hggabriel.pokerun.ui.telas.membro

import com.hggabriel.pokerun.ui.componentes.AtualizarAoPuxar
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import com.hggabriel.pokerun.ui.componentes.FracaoDeSessoes
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.telas.perfil.AcoesDoPerfil
import com.hggabriel.pokerun.ui.telas.perfil.BarraDeVoltar
import com.hggabriel.pokerun.ui.telas.perfil.PalcoDaCredencial
import com.hggabriel.pokerun.ui.telas.perfil.dataCurtaComAno
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import java.time.LocalDate

private val AlturaDaSessao = 10.dp
private val CelulaDaPokedex = 44.dp
private val SpriteDaPokedex = 56.dp

/** A silhueta da próxima espécie: o sprite pintado de tinta, como na Pokédex. */
private const val OPACIDADE_DA_SILHUETA = 0.18f

/**
 * O cartão do treinador de outro membro do grupo (27/10/2026), aberto pela linha do ranking.
 *
 * É o Perfil dele em modo leitura: a mesma credencial pendurada no cordão (nome, treinador,
 * parceiro, desde, coleção, km na vida e prova) e, embaixo, dois cartões: a semana dele no
 * plano e a Pokédex da temporada. O dado mora na figura, e a tela não explica nada.
 */
@Composable
fun MembroScreen(
    planoId: String,
    uid: String,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MembroViewModel = membroViewModel(planoId, uid),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    MembroScreen(estado = estado, aoVoltar = aoVoltar, aoTentarDeNovo = vm::tentarDeNovo, modifier = modifier)
}

/** A tela sem `ViewModel`, para os previews. */
@Composable
fun MembroScreen(
    estado: MembroUiState,
    aoVoltar: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        AtualizarAoPuxar(aoAtualizar = aoTentarDeNovo) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                BarraDeVoltar(aoVoltar, rotulo = stringResource(R.string.membro_sobrancelha))
                when (estado) {
                    MembroUiState.Carregando -> CartoesEmEspera(
                        listOf(420.dp, 160.dp, 140.dp),
                        Modifier.padding(horizontal = Espacos.margem),
                    )
                    MembroUiState.Falhou -> Falhou(aoTentarDeNovo)
                    is MembroUiState.Conteudo -> Conteudo(estado)
                }
            }
        }
    }
}

@Composable
private fun Conteudo(estado: MembroUiState.Conteudo) {
    PalcoDaCredencial(estado.credencial, AcoesDoPerfil(), editavel = false)
    Column(
        modifier = Modifier.padding(horizontal = Espacos.margem),
        verticalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        CartaoDaSemana(estado.semana, estado.noGrupoDesde, estado.ultimaCorrida)
        estado.pokedex?.let { CartaoDaPokedex(it) }
        Spacer(Modifier.height(Espacos.bloco))
    }
}

/**
 * A semana do membro: o km em número grande e o XP ao lado, uma pílula por sessão prevista
 * (cheia em `destaque` quando feita), o longão como tubo e, no pé, há quanto tempo ele está
 * no grupo e quando correu pela última vez.
 */
@Composable
private fun CartaoDaSemana(semana: SemanaDoMembro?, noGrupoDesde: LocalDate, ultimaCorrida: LocalDate?) {
    Cartao {
        if (semana != null) {
            RotuloDoCartao(stringResource(R.string.membro_semana_rotulo, semana.numero))
            Spacer(Modifier.height(Espacos.relacionado))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = formatarKm(semana.kmFeito), style = Estilos.numeroL, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.ajustes_km),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.grupo_xp, String.format(LocaleDoApp, "%,d", semana.xp)),
                    style = Estilos.numeroM,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(Espacos.interno))
            Sessoes(semana.sessoesFeitas, semana.sessoesPrevistas)
            Spacer(Modifier.height(Espacos.relacionado))
            FracaoDeSessoes(semana.sessoesFeitas, semana.sessoesPrevistas)
            semana.longaoAlvo?.let { alvo ->
                Spacer(Modifier.height(Espacos.interno))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RotuloDoCartao(stringResource(R.string.membro_longao), Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.membro_longao_progresso, formatarKm(semana.longaoFeito), formatarKm(alvo)),
                        style = Estilos.apoio,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Espacos.relacionado))
                BarraDePilula(
                    fracao = if (alvo > 0) (semana.longaoFeito / alvo).toFloat() else 0f,
                    cor = MaterialTheme.colorScheme.primary,
                    altura = AlturaDaSessao,
                )
            }
            Spacer(Modifier.height(Espacos.margem))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.margem)) {
            Campo(stringResource(R.string.membro_no_grupo), dataCurtaComAno(noGrupoDesde), Modifier.weight(1f))
            Campo(
                stringResource(R.string.membro_ultima_corrida),
                ultimaCorrida?.let(::dataCurtaComAno) ?: stringResource(R.string.perfil_sem_corrida),
                Modifier.weight(1f),
            )
        }
    }
}

/** Uma pílula por sessão prevista; as feitas enchem em `destaque`, da esquerda. */
@Composable
private fun Sessoes(feitas: Int, previstas: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(previstas.coerceAtLeast(1)) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AlturaDaSessao)
                    .clip(FormaCirculo)
                    .background(
                        if (i < feitas) MaterialTheme.colorScheme.primary else LocalCoresPokerun.current.superficieAfundada,
                    ),
            )
        }
    }
}

@Composable
private fun Campo(rotulo: String, valor: String, modifier: Modifier) {
    Column(modifier = modifier) {
        RotuloDoCartao(rotulo)
        Text(text = valor, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * A Pokédex do membro na temporada: a pílula da coleção e os sprites da escada até a
 * `colecao_max`, com a próxima espécie em silhueta no fim. A escada é pública, então a
 * coleção inteira sai de um número só.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDaPokedex(pokedex: PokedexDoMembro) {
    val descricao = stringResource(R.string.membro_pokedex_descricao, pokedex.colecao.size, pokedex.total)
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotuloDoCartao(stringResource(R.string.membro_pokedex_rotulo, pokedex.temporada), Modifier.weight(1f))
            Text(
                text = "${pokedex.colecao.size}/${pokedex.total}",
                style = Estilos.corpoForte,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Espacos.relacionado))
        BarraDePilula(fracao = pokedex.colecao.size.toFloat() / pokedex.total.coerceAtLeast(1), cor = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Espacos.interno))
        FlowRow(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = descricao },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            pokedex.colecao.forEach { numero -> Figurinha(numero, silhueta = false) }
            pokedex.proxima?.let { Figurinha(it, silhueta = true) }
        }
    }
}

@Composable
private fun Figurinha(numero: Int, silhueta: Boolean) {
    Box(
        modifier = Modifier
            .size(CelulaDaPokedex)
            .clip(FormaControle)
            .background(LocalCoresPokerun.current.superficieAfundada),
        contentAlignment = Alignment.Center,
    ) {
        ImagemDaRede(
            model = spriteDeJogoDe(numero),
            filterQuality = FilterQuality.None,
            recuoDaEspera = 0.3f,
            colorFilter = if (silhueta) {
                ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = OPACIDADE_DA_SILHUETA), BlendMode.SrcIn)
            } else {
                null
            },
            modifier = Modifier.requiredSize(SpriteDaPokedex),
        )
    }
}

@Composable
private fun Falhou(aoTentarDeNovo: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Espacos.margem)) {
        Text(
            text = stringResource(R.string.membro_erro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Espacos.bloco))
        BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_erro_repetir))
        }
    }
}

@Composable
private fun membroViewModel(planoId: String, uid: String): MembroViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "membro-$planoId-$uid") {
        MembroViewModel(
            planoId,
            uid,
            container.planoRepositorio,
            container.progressoRepositorio,
            container.temporadaRepositorio,
            container.especiesRepositorio,
        )
    }
}
