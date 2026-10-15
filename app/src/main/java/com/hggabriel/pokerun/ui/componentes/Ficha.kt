package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaEspecie
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
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
 * O corpo dos dois: borda, estados e camada de toque. A forma e a sombra entram por
 * parâmetro porque a [FichaDeEspecime] tem `raio-especie` e `elevacao-especie`; o card
 * de dado fica com `raio-card` e sem sombra.
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
    forma: Shape = MaterialTheme.shapes.medium,
    sombra: Dp = 0.dp,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val cores = LocalCoresPokerun.current

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
            // Card de dado sem sombra (`elevacao-plana`). Só a ficha de espécie passa
            // [sombra], e ela sai em `texto-primario`, como o token ([Elevacao]).
            modifier = if (sombra > 0.dp) {
                Modifier.shadow(sombra, forma, clip = false, ambientColor = tinta, spotColor = tinta)
            } else {
                Modifier
            },
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

/** A faixa de tinta que corta a Pokébola ao meio (docs/02 §4.3). */
internal val AlturaDaFaixa = 4.dp

/** O botão central da Pokébola, sobre a faixa. */
private val DiametroDoBotao = 28.dp
private val DiametroDoMiolo = 12.dp

/**
 * Ficha de espécie: **a Pokébola aberta** (docs/02 §4.3, `CardDeEspecie.md`). É o objeto
 * colecionável do app e o único card com sombra.
 *
 * ```
 * ╭────────────────────────╮
 * │   tipo-*-suave + arte  │  metade de cima
 * ├──────────( ● )─────────┤  faixa de 4dp em texto-primario, botão central
 * │  corpo em superficie   │  número, nome, tipos, progresso
 * ╰────────────────────────╯
 * ```
 *
 * - **Metade de cima** em `tipo-<id>-suave` do tipo primário. Sem tipo conhecido, em
 *   `superficie-afundada`: é o tratamento da espécie oculta, e é o que a Home desenha
 *   enquanto os tipos não chegam da PokéAPI.
 * - **Botão central** em `FormaCirculo`: miolo preenchido quando a espécie foi capturada,
 *   vazio quando não foi.
 * - `raio-especie` (24dp) e `elevacao-especie`.
 *
 * **Sem superfície própria.** A ficha usa `superficie`, `texto-primario` e
 * `tipo-*-suave`, que já têm pares medidos (§2.2 e §2.5).
 *
 * Consumidores: o card da espécie atual na Home e, quando existir, o herói do detalhe da
 * espécie. **A grade da Pokédex não usa esta ficha:** cada célula é o item da Pokédex
 * (docs/02 §4.4).
 *
 * @param arte a arte oficial, centrada na metade de cima.
 * @param conteudo o corpo, abaixo da faixa. O chamador põe o recheio.
 */
@Composable
fun FichaDeEspecime(
    arte: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    tipo: TipoDePokemon? = null,
    capturada: Boolean = false,
    aoTocar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val metadeDeCima = tipo?.let { LocalCoresDeTipo.current[it].suave }
        ?: LocalCoresPokerun.current.superficieAfundada

    CompositionLocalProvider(LocalContentColor provides esquema.onSurface) {
        FichaBase(
            superficie = esquema.surface,
            tinta = esquema.onSurface,
            modifier = modifier,
            aoTocar = aoTocar,
            selecionada = false,
            habilitada = true,
            fonteDeInteracao = remember { MutableInteractionSource() },
            forma = FormaEspecie,
            sombra = Elevacao.especie,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(metadeDeCima)
                    .padding(Espacos.margem),
                contentAlignment = Alignment.Center,
                content = arte,
            )
            FaixaDaPokebola(capturada)
            conteudo()
        }
    }
}

/**
 * A faixa de 4dp com o botão central. O botão fica metade sobre a metade de cima e
 * metade sobre o corpo, como na Pokébola: a faixa é desenhada no meio da caixa dele.
 */
@Composable
private fun FaixaDaPokebola(capturada: Boolean) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val superficie = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DiametroDoBotao)
            .drawBehind {
                val altura = AlturaDaFaixa.toPx()
                drawRect(
                    color = tinta,
                    topLeft = Offset(0f, (size.height - altura) / 2),
                    size = Size(size.width, altura),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DiametroDoBotao)
                .background(superficie, FormaCirculo)
                .border(AlturaDaFaixa, tinta, FormaCirculo),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(DiametroDoMiolo)
                    .then(
                        if (capturada) {
                            Modifier.background(tinta, FormaCirculo)
                        } else {
                            Modifier.border(LarguraDaBorda * 2, tinta, FormaCirculo)
                        },
                    ),
            )
        }
    }
}
