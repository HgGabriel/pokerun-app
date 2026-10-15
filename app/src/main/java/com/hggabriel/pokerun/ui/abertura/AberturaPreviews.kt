package com.hggabriel.pokerun.ui.abertura

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * Quadros-chave da abertura (docs/02 §6): o repouso, que é o último quadro da splash; o
 * fim do salto; o meio da corrida; e o começo da dissolução.
 */
@Composable
private fun Quadro(abre: Float, corre: Float, dissolve: Float) {
    PokerunTheme {
        CenaDaAbertura(quadro = { QuadroDaAbertura(abre, corre, dissolve) }, icone = null)
    }
}

@Preview(name = "1 Repouso", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaRepouso() = Quadro(abre = 0f, corre = 0f, dissolve = 0f)

@Preview(name = "2 Salto", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaSalto() = Quadro(abre = 1f, corre = 0f, dissolve = 0f)

@Preview(name = "3 Corrida", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaCorrida() = Quadro(abre = 1f, corre = 0.45f, dissolve = 0f)

@Preview(name = "4 Dissolve", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaDissolve() = Quadro(abre = 1f, corre = 1f, dissolve = 0.5f)
