package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Onde cada figurinha cai na grade: linha e coluna, contadas de zero. */
internal data class Encaixe(val linha: Int, val coluna: Int)

/**
 * O encaixe denso da página (o `grid-auto-flow: dense` do esboço): cada figurinha, na ordem,
 * vai para a primeira casa livre de cima para baixo em que cabe. A figurinha em dobro ocupa
 * 2×2; quando ela não cabe no fim de uma linha, desce, e a figurinha seguinte tapa o buraco.
 * A ordem visual de um vizinho pode trocar; a ordem falada segue o número.
 *
 * Com 2 colunas, o dobro ocupa a linha inteira.
 */
internal fun encaixeDenso(emDobro: List<Boolean>, colunas: Int): List<Encaixe> {
    val ocupadas = mutableSetOf<Encaixe>()
    fun livre(linha: Int, coluna: Int, lado: Int): Boolean =
        coluna + lado <= colunas && (0 until lado).all { dl -> (0 until lado).all { dc -> Encaixe(linha + dl, coluna + dc) !in ocupadas } }

    return emDobro.map { dobro ->
        val lado = if (dobro) minOf(2, colunas) else 1
        var linha = 0
        var achada: Encaixe? = null
        while (achada == null) {
            for (coluna in 0 until colunas) {
                if (livre(linha, coluna, lado)) {
                    achada = Encaixe(linha, coluna)
                    break
                }
            }
            linha++
        }
        val casa = checkNotNull(achada)
        for (dl in 0 until lado) for (dc in 0 until lado) ocupadas += Encaixe(casa.linha + dl, casa.coluna + dc)
        casa
    }
}

private val EspacoEntreColunas = 8.dp
private val EspacoEntreLinhas = 10.dp

/**
 * A grade da página aberta: [colunas] colunas iguais e o encaixe de [encaixeDenso]. A altura
 * de cada linha é a da figurinha mais alta nela, e a figurinha em dobro mede duas linhas e
 * o espaço entre elas.
 */
@Composable
internal fun GradeDoAlbum(
    emDobro: List<Boolean>,
    colunas: Int,
    modifier: Modifier = Modifier,
    espacoColunas: Dp = EspacoEntreColunas,
    espacoLinhas: Dp = EspacoEntreLinhas,
    conteudo: @Composable () -> Unit,
) {
    Layout(content = conteudo, modifier = modifier) { medidos, restricoes ->
        val vao = espacoColunas.roundToPx()
        val entreLinhas = espacoLinhas.roundToPx()
        val largura = restricoes.maxWidth
        val celula = (largura - vao * (colunas - 1)) / colunas
        val encaixes = encaixeDenso(emDobro.take(medidos.size), colunas)
        val lado = emDobro.map { if (it) minOf(2, colunas) else 1 }

        // Primeiro as simples, que dão a altura das linhas.
        val simples = medidos.mapIndexed { i, m ->
            if (lado[i] == 1) m.measure(Constraints.fixedWidth(celula)) else null
        }
        val linhas = (encaixes.indices.maxOfOrNull { encaixes[it].linha + lado[it] } ?: 0)
        val alturaPadrao = simples.filterNotNull().maxOfOrNull { it.height } ?: celula
        val alturas = IntArray(linhas) { alturaPadrao }
        simples.forEachIndexed { i, p -> if (p != null) alturas[encaixes[i].linha] = maxOf(alturas[encaixes[i].linha], p.height) }

        val colocados = medidos.mapIndexed { i, m ->
            simples[i] ?: run {
                val n = lado[i]
                val w = celula * n + vao * (n - 1)
                val h = (0 until n).sumOf { alturas[encaixes[i].linha + it] } + entreLinhas * (n - 1)
                m.measure(Constraints.fixed(w, h))
            }
        }
        val topo = IntArray(linhas + 1)
        for (l in 0 until linhas) topo[l + 1] = topo[l] + alturas[l] + entreLinhas
        val alturaTotal = if (linhas == 0) 0 else topo[linhas] - entreLinhas

        layout(largura, alturaTotal) {
            colocados.forEachIndexed { i, p ->
                p.place(encaixes[i].coluna * (celula + vao), topo[encaixes[i].linha])
            }
        }
    }
}
