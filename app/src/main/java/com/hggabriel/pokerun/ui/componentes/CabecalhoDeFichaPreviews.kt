package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/**
 * As três profundidades, no mesmo quadro (docs/02 §10.1).
 *
 * Lado a lado porque o ponto do componente é serem **iguais**: a mesma pergunta, no
 * mesmo lugar, da mesma forma. Um preview por profundidade mostraria três cabeçalhos
 * bonitos e nenhuma prova de que o título cai na mesma altura nos três — que é
 * justamente o que a linha de 48dp fixa da sobrancelha existe para garantir.
 *
 * O que cada um exercita:
 *
 * | Profundidade | Exercita |
 * |---|---|
 * | Raiz de aba | um nível, engrenagem no slot de ação, título com o nome do plano |
 * | Detalhe | dois níveis mais o índice `n de N` colado ao último |
 * | Modal | dois níveis, slot de ação vazio |
 */
@Preview(name = "Três profundidades", showBackground = true, backgroundColor = 0xFFFAF9F7)
@Composable
private fun CabecalhoTresProfundidadesPreview() {
    PokerunTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CabecalhoDeAba(
                aba = "Hoje",
                titulo = "Meia de Floripa",
                aoAbrirAjustes = {},
            )

            CabecalhoDeFicha(
                sobrancelha = listOf("Hoje", "Semana"),
                titulo = "13 a 19 de outubro",
                indice = Indice(n = 3, total = 21),
            )

            CabecalhoDeFicha(
                sobrancelha = listOf("Plano", "Novo"),
                titulo = "Criar plano",
            )
        }
    }
}

/**
 * O caso apertado de docs/02 §8, item 9: `fontScale` 2,0 em 320dp de largura.
 *
 * É onde a sobrancelha e a engrenagem brigam pela mesma linha. O teto de duas linhas
 * segura a sobrancelha, e o título quebra em vez de truncar, porque ele é dado do
 * usuário.
 */
@Preview(
    name = "fontScale 2,0 em 320dp",
    showBackground = true,
    backgroundColor = 0xFFFAF9F7,
    fontScale = 2.0f,
    widthDp = 320,
)
@Composable
private fun CabecalhoFonteGrandePreview() {
    PokerunTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CabecalhoDeAba(
                aba = "Progresso",
                titulo = "Setembro de 2026",
                aoAbrirAjustes = {},
            )

            CabecalhoDeFicha(
                sobrancelha = listOf("Progresso", "Corrida"),
                titulo = "Domingo, 14 de setembro",
            )
        }
    }
}
