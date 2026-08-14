package com.hggabriel.pokerun.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano

private val Recheio = 6.dp
private val RecheioVertical = 2.dp
private val LarguraDaBorda = 1.dp

/**
 * A marca da situação de um plano: `ATIVO`, `GUARDADO` ou `ENCERRADO`.
 *
 * **Dois consumidores desde o primeiro dia:** a linha da `PlansListScreen` (`F1-T12`) e o
 * cabeçalho da `PlanDetailScreen` (`F1-T13`). Nasceu privada na primeira e mudou de casa
 * na segunda — duas cópias divergiriam no dia em que a paleta mudasse, e a marca é
 * justamente o que diz ao usuário para onde as corridas dele estão indo.
 *
 * **É tag, não chip**, pelo mesmo motivo da tag de tipo da grade de semanas: não
 * seleciona, não filtra e não recebe toque, então a borda é a decorativa de 1dp em
 * `borda` (docs/02 §2.3). `borda-forte` ali convidaria alguém a tocar.
 *
 * **A cor não é o único canal** (docs/02 §4.2): a palavra está escrita. `leitura` no
 * ativo, `tinta-fraca` no resto — quem não distingue as duas lê `ATIVO` do mesmo jeito.
 */
@Composable
fun MarcaDeSituacao(situacao: SituacaoDoPlano, modifier: Modifier = Modifier) {
    val ativo = situacao == SituacaoDoPlano.ATIVO

    Text(
        text = stringResource(rotuloDaSituacao(situacao)).uppercase(LocaleDoApp),
        style = MaterialTheme.typography.labelSmall,
        color = if (ativo) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .border(
                width = LarguraDaBorda,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = Recheio, vertical = RecheioVertical),
    )
}

/**
 * O rótulo da situação em texto, para quem precisa dele fora da marca — a descrição
 * acessível da linha, que junta nome, situação e dados numa frase só.
 */
@StringRes
internal fun rotuloDaSituacao(situacao: SituacaoDoPlano): Int = when (situacao) {
    SituacaoDoPlano.ATIVO -> R.string.planos_marca_ativo
    SituacaoDoPlano.DORMENTE -> R.string.planos_marca_dormente
    SituacaoDoPlano.ENCERRADO -> R.string.planos_marca_encerrado
}
