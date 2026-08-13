package com.hggabriel.pokerun.ui.navegacao

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R

/** O filete que marca o item selecionado (docs/03 §1). */
private val AlturaDoFilete = 2.dp

private val TamanhoDoIcone = 24.dp

/**
 * Os três destinos de topo das Fases 1 a 3 (docs/03 §2).
 *
 * **A quarta aba, `Pokédex`, só entra na Fase 4** (`F4-T11`). Acrescentar um destino
 * em outubro é aceitável; mostrar uma aba vazia e bloqueada durante dois meses não é.
 *
 * **Os glifos continuam provisórios, e a razão não mudou desde `F0-T17`:**
 * `material-icons-core` não tem gráfico nem grupo, e `material-icons-extended` é
 * dependência fora da lista da Fase 0 — o único tipo de decisão que ainda para a
 * sessão (`EXECUCAO.md §7`). `Home`, `DateRange` e `Person` são o que existe, e a
 * troca é barata: dois campos deste enum.
 */
enum class DestinoDeTopo(
    @param:StringRes val rotulo: Int,
    /** O **grafo** da aba, não a tela raiz: é ele que carrega a pilha. */
    val grafo: Any,
) {
    HOJE(R.string.destino_hoje, AbaHoje),
    PROGRESSO(R.string.destino_progresso, AbaProgresso),
    GRUPO(R.string.destino_grupo, AbaGrupo);

    val iconeDeContorno
        @Composable get() = when (this) {
            HOJE -> Icons.Outlined.Home
            PROGRESSO -> Icons.Outlined.DateRange
            GRUPO -> Icons.Outlined.Person
        }

    val iconePreenchido
        @Composable get() = when (this) {
            HOJE -> Icons.Filled.Home
            PROGRESSO -> Icons.Filled.DateRange
            GRUPO -> Icons.Filled.Person
        }
}

/**
 * A barra inferior, com o estado selecionado em **três canais** (docs/03 §1).
 *
 * | Canal | Não selecionado | Selecionado |
 * |---|---|---|
 * | Ícone | contorno | preenchido |
 * | Rótulo | `tinta-fraca` | `leitura` |
 * | Marca | nenhuma | filete de 2dp em `leitura`, no topo do item |
 *
 * Três canais e não só cor porque um deles sozinho falha para alguém: a cor some no
 * daltonismo e sob sol aberto, e o preenchimento do ícone é sutil em glifo pequeno.
 *
 * **A pílula do Material está desligada de propósito.** `indicatorColor` transparente
 * não é escolher uma cor fora dos tokens — é remover uma forma. A pílula padrão nasce
 * do `secondaryContainer`, e o filete é o mesmo vocabulário do cabeçalho de ficha e
 * das marcas da escada: a barra passa a parecer a régua de um aparelho, e não uma
 * barra do Material.
 */
@Composable
fun BarraDeDestinos(
    destinoAtual: DestinoDeTopo,
    aoEscolher: (DestinoDeTopo) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val marca = MaterialTheme.colorScheme.primary

        DestinoDeTopo.entries.forEach { destino ->
            val selecionado = destino == destinoAtual

            NavigationBarItem(
                selected = selecionado,
                onClick = { aoEscolher(destino) },
                icon = {
                    Icon(
                        imageVector = if (selecionado) {
                            destino.iconePreenchido
                        } else {
                            destino.iconeDeContorno
                        },
                        // O rótulo já nomeia o item para o TalkBack. Repetir aqui
                        // faria o leitor de tela dizer o nome duas vezes.
                        contentDescription = null,
                        modifier = Modifier.size(TamanhoDoIcone),
                    )
                },
                label = { Text(stringResource(destino.rotulo)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.drawBehind {
                    if (selecionado) {
                        drawRect(
                            color = marca,
                            size = Size(size.width, AlturaDoFilete.toPx()),
                        )
                    }
                },
            )
        }
    }
}
