package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** Contorno de foco de teclado e D-pad, deslocado do card (docs/02 §4.2). */
private val LarguraDoFoco = 2.dp

/** O deslocamento do contorno. Fica sempre reservado, senão a tela pula ao focar. */
private val FolgaDoFoco = 2.dp

private val LarguraDaBorda = 1.dp

private const val OpacidadeDesabilitada = 0.38f

/**
 * Painel do sistema: superfície branca sobre papel, borda decorativa de 1dp,
 * **elevação 0dp**. Nenhuma tela chama `Card` diretamente (docs/02 §4.1).
 *
 * `CardDefaults.cardElevation()` nasce com `defaultElevation = 1.dp`, que aplica
 * sombra **e** tinta tonal. Sem o override abaixo, todo card do app nasceria contra
 * a regra de "elevação é contraste de superfície, não sombra".
 *
 * Estados conforme a tabela de docs/02 §4.2:
 * - **pressionado** — retângulo em `leitura-toque`, pelo `LocalIndication` do tema;
 * - **selecionado** — fundo em `leitura-toque` mais borda em `borda-forte`. Nunca só
 *   a cor: o chamador acrescenta o ícone de marca ao [conteudo];
 * - **desabilitado** — 38% de opacidade e o clique fora, para a semântica ir junto;
 * - **foco** — contorno de 2dp em `leitura`, deslocado 2dp. Nunca remover.
 *
 * Sem [aoTocar] a ficha é um painel de leitura: não recebe foco, não indica toque.
 */
@Composable
fun Ficha(
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    selecionada: Boolean = false,
    habilitada: Boolean = true,
    fonteDeInteracao: MutableInteractionSource = remember { MutableInteractionSource() },
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    FichaBase(
        superficie = MaterialTheme.colorScheme.surface,
        tinta = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
        aoTocar = aoTocar,
        selecionada = selecionada,
        habilitada = habilitada,
        fonteDeInteracao = fonteDeInteracao,
        conteudo = conteudo,
    )
}

/**
 * O corpo dos dois: geometria, borda, estados e camada de toque. A superfície e a
 * tinta entram por parâmetro porque [FichaDeEspecime] as fixa e [Ficha] as herda —
 * e é essa a única diferença entre as duas (docs/02 §4.3).
 */
@Composable
private fun FichaBase(
    superficie: Color,
    tinta: Color,
    modifier: Modifier,
    aoTocar: (() -> Unit)?,
    selecionada: Boolean,
    habilitada: Boolean,
    fonteDeInteracao: MutableInteractionSource,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val cores = LocalCoresPokerun.current
    val forma = MaterialTheme.shapes.medium

    val focada by fonteDeInteracao.collectIsFocusedAsState()

    val contorno = if (focada) {
        Modifier.border(LarguraDoFoco, esquema.primary, forma)
    } else {
        Modifier
    }

    val toque = if (aoTocar != null) {
        Modifier.clickable(
            interactionSource = fonteDeInteracao,
            indication = LocalIndication.current,
            enabled = habilitada,
            onClick = aoTocar,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(contorno)
            .padding(LarguraDoFoco + FolgaDoFoco)
            .alpha(if (habilitada) 1f else OpacidadeDesabilitada)
    ) {
        Card(
            shape = forma,
            colors = CardDefaults.cardColors(
                containerColor = if (selecionada) cores.leituraToque else superficie,
                contentColor = tinta,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(
                LarguraDaBorda,
                if (selecionada) esquema.outline else esquema.outlineVariant,
            ),
        ) {
            // O toque vive aqui dentro, e não no Card: a camada de pressão precisa
            // ser desenhada DEPOIS da superfície do Card para aparecer sobre ela.
            // O overload clicável do Card também não serviria — ele fixa `ripple()`
            // internamente e ignoraria o LocalIndication do tema.
            Column(modifier = toque.fillMaxWidth(), content = conteudo)
        }
    }
}

/**
 * Ficha de espécime: **onde houver sprite, a superfície é papel** (docs/02 §4.3).
 *
 * A ficha do espécime é o papel; o app em volta é o fichário. É a mesma ideia de
 * `chassi` um nível abaixo — a Pokédex real é uma tela clara dentro de uma carcaça,
 * e a referência do anime é silhueta escura sobre fundo claro **emoldurado**, nunca
 * a tela inteira.
 *
 * Três consumidores: a grade da Pokédex (`F4-T07`), o card da espécie atual
 * (`F2-T14`) e a sequência do `WeeklyCloseScreen` (`F4-T09`).
 *
 * **Não cria superfície nova para medir.** Numa célula existem só sprite, número,
 * nome e banda de tipo: `tinta` (16,83:1) e `tinta-fraca` (6,20:1) sobre `painel` já
 * estão medidos em docs/02 §2.2, e a banda de tipo em §2.5. Restrição que vem junto:
 * **nada entra numa ficha de espécime sem par medido contra `painel`.**
 *
 * Hoje o app entrega um tema só, então fixar a superfície e herdá-la dão o mesmo
 * pixel. O que este composable garante é o **lugar** onde a fixação acontece: no dia
 * em que houver um segundo tema, o papel sob luz baixa (algo perto de `#EDEAE4`, com
 * as duas medições que §4.3 prevê) entra aqui e em nenhuma tela.
 */
@Composable
fun FichaDeEspecime(
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    selecionada: Boolean = false,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = LocalCoresPokerun.current

    // Lê do portador, NUNCA do ColorScheme. `colorScheme.onSurface` compila, passa o
    // teste de F0-T13 e hoje renderiza idêntico, porque `onSurface` É `Tinta` — mas o
    // ColorScheme é exatamente a coisa que acompanha o tema do app. No dia em que
    // houver um segundo, a ficha ficaria com papel fixo embaixo e tinta clara em cima,
    // e o ponto de fixação seria ilusório justamente no dia em que ele importa.
    CompositionLocalProvider(LocalContentColor provides cores.especimeTinta) {
        FichaBase(
            superficie = cores.especimeSuperficie,
            tinta = cores.especimeTinta,
            modifier = modifier,
            aoTocar = aoTocar,
            selecionada = selecionada,
            habilitada = true,
            fonteDeInteracao = remember { MutableInteractionSource() },
            conteudo = conteudo,
        )
    }
}
