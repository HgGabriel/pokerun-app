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
 * Card de dado: `superficie` sobre `fundo`, os dois brancos, delimitado pela borda
 * decorativa de 1dp em `borda` e **sem sombra** (`elevacao-plana`). Nenhuma tela chama
 * `Card` diretamente (docs/02 §4.1).
 *
 * `CardDefaults.cardElevation()` nasce com `defaultElevation = 1.dp`, que aplica
 * sombra **e** tinta tonal. Sem o override abaixo, todo card do app nasceria com a
 * sombra que §4 reserva para o card de espécie, o FAB e o bottom sheet.
 *
 * Estados conforme a tabela de docs/02 §4.2:
 * - **pressionado** — retângulo em `estado-pressionado`, pelo `LocalIndication` do tema;
 * - **selecionado** — fundo em `superficie-afundada` mais borda em `borda-controle`.
 *   Nunca só a cor: o chamador acrescenta o ícone de marca ao [conteudo];
 * - **desabilitado** — 38% de opacidade e o clique fora, para a semântica ir junto;
 * - **foco** — contorno de 2dp em `foco`, deslocado 2dp. Nunca remover.
 *
 * Sem [aoTocar] a ficha só mostra dado: não recebe foco, não indica toque.
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
 * tinta entram por parâmetro para que [FichaDeEspecime] possa ganhar as suas quando
 * virar a Pokébola aberta (docs/02 §4.3). Hoje as duas passam as mesmas.
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
        Modifier.border(LarguraDoFoco, cores.foco, forma)
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
                containerColor = if (selecionada) cores.superficieAfundada else superficie,
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
 * Ficha de espécie: o card que leva sprite ou arte de uma espécie (docs/02 §4.3).
 *
 * Consumidores de hoje: a grade da Pokédex (`F4-T07`), o card da espécie atual
 * (`F2-T14`) e a sequência do `WeeklyCloseScreen` (`F4-T09`).
 *
 * **Não tem superfície própria.** O design system não fixa uma: a ficha usa
 * `superficie` e `texto-primario`, que já têm par medido em docs/02 §2.2, e as cores
 * de tipo de §2.5. Restrição que vem junto: nada entra numa ficha de espécie sem par
 * medido contra a superfície em que fica.
 *
 * **Ainda não é a Pokébola aberta.** A forma de docs/02 §4.3 (metade de cima em
 * `tipo-*-suave`, faixa de 4dp com botão central, `raio-especie` e `elevacao-especie`)
 * entra com o componente de card de espécie. Até lá esta ficha desenha o mesmo
 * painel que [Ficha]. Pela §4.3, a grade da Pokédex deixa de usá-la: cada célula
 * passa a ser o item da Pokédex (docs/02 §4.4).
 */
@Composable
fun FichaDeEspecime(
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    selecionada: Boolean = false,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val esquema = MaterialTheme.colorScheme

    // `superficie` e `texto-primario`: o design system não fixa superfície própria
    // para a ficha de espécie (docs/02 §4.3).
    CompositionLocalProvider(LocalContentColor provides esquema.onSurface) {
        FichaBase(
            superficie = esquema.surface,
            tinta = esquema.onSurface,
            modifier = modifier,
            aoTocar = aoTocar,
            selecionada = selecionada,
            habilitada = true,
            fonteDeInteracao = remember { MutableInteractionSource() },
            conteudo = conteudo,
        )
    }
}
