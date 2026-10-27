package com.hggabriel.pokerun.ui.abertura

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * Quadros-chave da abertura (docs/02 §2.7): o estado inicial, que é a splash parada da fase 1;
 * o clique, com a coroa no fundo; o pico do overshoot da entrada; o estado final da entrada, com as molas em
 * repouso; e a saída a meio, com o furo abrindo sobre a primeira tela.
 */
@Composable
private fun Quadro(entrada: Float, saida: Float, clique: CliqueDaAbertura = CliqueDaAbertura()) {
    PokerunTheme {
        CenaDaAbertura(quadro = { QuadroDaAbertura(entrada, saida) }, icone = { null }, clique = { clique })
    }
}

@Preview(name = "1 Inicial", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaInicial() = Quadro(entrada = 0f, saida = 0f)

@Preview(name = "1b Clique", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaClique() = Quadro(entrada = 0f, saida = 0f, clique = CliqueDaAbertura(coroa = 1f, botao = 1f))

@Preview(name = "2 Pico do overshoot", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaPico() = Quadro(entrada = FisicaDaAbertura.PICO_DA_ENTRADA, saida = 0f)

@Preview(name = "3 Final", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaFinal() = Quadro(entrada = 1f, saida = 0f)

@Preview(name = "4 Saída", widthDp = 360, heightDp = 640)
@Composable
private fun AberturaSaida() = Quadro(entrada = 1f, saida = 0.5f)
