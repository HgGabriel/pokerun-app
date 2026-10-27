package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

private val Goteira = Espacos.margem
private val AlturaDoEsqueletoDoBanner = 156.dp
private val AlturaDoEsqueletoDoMembro = 148.dp

/**
 * O grupo do plano, aberto pelo banner do detalhe (26/10/2026, docs/03 §3.7.1).
 *
 * O mesmo banner no topo, sem a pílula; um cartão por membro na largura toda, com a fração
 * e as células de cada semana; e o convite. Lê o plano pelo mesmo `ViewModel` do detalhe,
 * numa instância própria desta entrada da pilha.
 */
@Composable
fun GrupoDoPlanoScreen(
    planoId: String,
    modifier: Modifier = Modifier,
    vm: DetalhePlanoViewModel = detalhePlanoViewModel(planoId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    GrupoDoPlanoScreen(estado = estado, aoTentarDeNovo = vm::tentarDeNovo, modifier = modifier)
}

/** A tela sem `ViewModel`, para os previews. */
@Composable
fun GrupoDoPlanoScreen(
    estado: DetalhePlanoUiState,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            // Também vive na pilha modal, sem o `Scaffold` da casca.
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
        ) {
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.detalhe_sobrancelha), stringResource(R.string.destino_grupo)),
                    titulo = (estado as? DetalhePlanoUiState.Conteudo)?.nome.orEmpty(),
                )
            }

            when (estado) {
                DetalhePlanoUiState.Carregando -> item(key = "esqueleto") { Esqueleto() }
                DetalhePlanoUiState.Falhou -> falhou(aoTentarDeNovo)
                is DetalhePlanoUiState.Conteudo -> {
                    item(key = "banner") {
                        Column(modifier = Modifier.padding(horizontal = Goteira)) {
                            Spacer(Modifier.height(20.dp))
                            BannerDoGrupo(estado, aoTocar = null)
                        }
                    }
                    items(estado.progresso, key = { it.uid }) { membro ->
                        Column(modifier = Modifier.padding(horizontal = Goteira)) {
                            Spacer(Modifier.height(Espacos.interno))
                            CartaoDoMembro(membro, estado)
                        }
                    }
                    if (estado.mostraConvite) {
                        item(key = "convite") { CodigoDeConvite(estado.codigoConvite) }
                    }
                }
            }
        }
    }
}

/** O banner e dois membros em bloco estático, sem shimmer (docs/02 §8, item 6). */
@Composable
private fun Esqueleto() {
    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Spacer(Modifier.height(20.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(AlturaDoEsqueletoDoBanner)
                .background(MaterialTheme.colorScheme.surface, FormaDoCartao),
        )
        repeat(2) {
            Spacer(Modifier.height(Espacos.interno))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(AlturaDoEsqueletoDoMembro)
                    .background(MaterialTheme.colorScheme.surface, FormaDoCartao),
            )
        }
    }
}
