package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaChip
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/*
 * Os controles de seleção do app: o segmentado e o chip de filtro (docs/02 §4.2).
 *
 * **Existem pelo mesmo motivo de `Botoes.kt`.** O `SegmentedButton` e o `FilterChip` do
 * M3 pintam o selecionado com `secondaryContainer`, que no app é `destaque-suave`, a cor do
 * destino ativo da barra inferior; e o `itemShape` do segmentado parte da pílula, que é
 * a forma que §4 tira do app. As duas coisas compilam e passam na revisão: foram vistas no
 * aparelho em 17/10/2026, no seletor Painel/Histórico e nos filtros do histórico.
 *
 * Aqui a tabela de §4.2 vale uma vez: **selecionado é `superficie-afundada` mais o check**,
 * porque a cor sozinha não chega a quem não a distingue; o contorno é `borda-controle`
 * (§2.3), selecionado ou não; o toque é a camada no retângulo, sem ripple. As telas chamam
 * estes, e `SelecaoTest` reprova a chamada direta ao componente do M3.
 */

/** O alvo de toque mínimo de docs/02 §8, item 2. */
private val AlturaDoAlvo = 48.dp

private val LarguraDoContorno = 1.dp

/**
 * Uma escolha entre poucas opções do mesmo nível, em segmentos de `raio-controle`.
 *
 * O check do selecionado é o ícone padrão do M3, que já entra no segmento ativo; o fundo
 * em `superficie-afundada` é o segundo canal.
 */
@Composable
fun <T> SegmentosDeEscolha(
    opcoes: List<T>,
    escolhida: T,
    aoEscolher: (T) -> Unit,
    modifier: Modifier = Modifier,
    rotulo: @Composable (T) -> String,
) {
    val esquema = MaterialTheme.colorScheme
    val cores = LocalCoresPokerun.current
    val coresDoSegmento = SegmentedButtonDefaults.colors(
        activeContainerColor = cores.superficieAfundada,
        activeContentColor = esquema.onSurface,
        activeBorderColor = esquema.outline,
        inactiveContainerColor = esquema.surface,
        inactiveContentColor = esquema.onSurface,
        inactiveBorderColor = esquema.outline,
    )

    SemRipple {
        SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
            opcoes.forEachIndexed { indice, opcao ->
                val fonte = remember { MutableInteractionSource() }
                val forma = SegmentedButtonDefaults.itemShape(
                    index = indice,
                    count = opcoes.size,
                    baseShape = FormaControle,
                )
                SegmentedButton(
                    selected = opcao == escolhida,
                    onClick = { aoEscolher(opcao) },
                    shape = forma,
                    colors = coresDoSegmento,
                    interactionSource = fonte,
                    modifier = Modifier
                        .heightIn(min = AlturaDoAlvo)
                        .camadaDeToque(fonte, forma),
                ) {
                    Text(rotulo(opcao))
                }
            }
        }
    }
}

/**
 * Um filtro que liga e desliga, em `raio-chip`. Selecionado leva o check à esquerda e o
 * fundo em `superficie-afundada`.
 */
@Composable
fun ChipDeFiltro(
    selecionado: Boolean,
    aoTocar: () -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
) {
    val esquema = MaterialTheme.colorScheme
    val cores = LocalCoresPokerun.current
    val fonte = remember { MutableInteractionSource() }

    SemRipple {
        FilterChip(
            selected = selecionado,
            onClick = aoTocar,
            label = { Text(rotulo) },
            leadingIcon = if (selecionado) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            shape = FormaChip,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = esquema.surface,
                labelColor = esquema.onSurface,
                selectedContainerColor = cores.superficieAfundada,
                selectedLabelColor = esquema.onSurface,
                selectedLeadingIconColor = esquema.onSurface,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selecionado,
                borderColor = esquema.outline,
                selectedBorderColor = esquema.outline,
                borderWidth = LarguraDoContorno,
                selectedBorderWidth = LarguraDoContorno,
            ),
            interactionSource = fonte,
            modifier = modifier
                .heightIn(min = AlturaDoAlvo)
                .camadaDeToque(fonte, FormaChip),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Segmentos e chips", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SelecaoPreview() {
    PokerunTheme {
        Column(Modifier.padding(Espacos.margem)) {
            SegmentosDeEscolha(listOf("Painel", "Histórico"), "Painel", {}) { it }
            Spacer(Modifier.size(Espacos.bloco))
            Row {
                ChipDeFiltro(selecionado = true, aoTocar = {}, rotulo = "Todas")
                Spacer(Modifier.size(Espacos.relacionado))
                ChipDeFiltro(selecionado = false, aoTocar = {}, rotulo = "Plano atual")
            }
        }
    }
}
