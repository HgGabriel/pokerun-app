package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** O contorno que substitui a mudança de superfície. Ver [SnackbarPokerun]. */
private val LarguraDoContorno = 1.dp

/**
 * O Snackbar do app: superfície clara **com contorno**. Nenhuma tela chama o
 * `Snackbar` do Material 3 diretamente (docs/02 §2.2).
 *
 * O esquema já entrega as três cores certas — `inverseSurface = superficie`,
 * `inverseOnSurface = texto-primario` e `inversePrimary = destaque`, as três medidas —,
 * e o `Snackbar` do M3 as lê sozinho. O que ele **não** faz sozinho é o requisito que
 * veio junto da decisão: `superficie` sobre `fundo` é 1:1, os dois brancos, então o
 * Snackbar não se separa do fundo. Sem o contorno de 1dp em `borda-controle` (4,51:1,
 * piso de WCAG 1.4.11) ele fica sem delimitação nenhuma. É o caso de docs/02 §2.3:
 * onde a superfície não delimita, o contorno delimita.
 *
 * O Snackbar escuro não saiu por gosto — com `inverseSurface = texto-primario`, a ação
 * em `destaque` mediria 3,69:1 e reprovaria AA. O porquê completo está no comentário do esquema, em
 * `ui/theme/Theme.kt`.
 */
@Composable
fun SnackbarPokerun(dados: SnackbarData, modifier: Modifier = Modifier) {
    val forma = MaterialTheme.shapes.extraSmall

    Snackbar(
        snackbarData = dados,
        modifier = modifier.border(LarguraDoContorno, MaterialTheme.colorScheme.outline, forma),
        shape = forma,
    )
}

/**
 * O hospedeiro que as telas passam ao `Scaffold`. Existe para que o contorno não
 * seja opt-in: um `SnackbarHost(estado)` puro renderiza o Snackbar do Material 3
 * sem ele, e a falta não aparece em revisão de diff.
 */
@Composable
fun HospedeiroDeSnackbar(estado: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = estado, modifier = modifier) { dados -> SnackbarPokerun(dados) }
}
