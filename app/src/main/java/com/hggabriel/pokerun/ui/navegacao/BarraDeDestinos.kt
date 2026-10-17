package com.hggabriel.pokerun.ui.navegacao

import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Raios
import com.hggabriel.pokerun.ui.theme.Tamanhos

/** O filete superior da barra, em `borda` (`elevacao-barra`, docs/02 §4). */
private val AlturaDoFilete = 1.dp

/** O anel de foco de teclado e D-pad (docs/02 §4.2): 2dp, afastado 2dp. */
private val LarguraDoFoco = 2.dp
private val FolgaDoFoco = 2.dp

/** O indicador do destino ativo: 60 × 32dp em `destaque-suave`, com `raio-controle`. */
private val LarguraDoIndicador = 60.dp
private val AlturaDoIndicador = 32.dp

/**
 * Os três destinos de topo das Fases 1 a 3 (docs/03 §2).
 *
 * **A quarta aba, `Pokédex`, só entra na Fase 4** (`F4-T11`). Acrescentar um destino
 * em outubro é aceitável; mostrar uma aba vazia e bloqueada durante dois meses não é.
 *
 * **Os glifos continuam provisórios, e a razão não mudou desde `F0-T17`:**
 * `material-icons-core` não tem gráfico nem grupo, e `material-icons-extended` ficou
 * de fora quando dependência nova pedia ok. `Home`, `DateRange` e `Person` são o que
 * existe, e a troca é barata: dois campos deste enum.
 */
enum class DestinoDeTopo(
    @param:StringRes val rotulo: Int,
    /** O **grafo** da aba, não a tela raiz: é ele que carrega a pilha. */
    val grafo: Any,
    /**
     * A forma curta, usada **só** acima de [ESCALA_QUE_EMPILHA] (`F1-T20`). Nula quando
     * o nome inteiro cabe — ver [rotuloDe].
     */
    @param:StringRes val rotuloCurto: Int? = null,
) {
    HOJE(R.string.destino_hoje, AbaHoje),
    PROGRESSO(R.string.destino_progresso, AbaProgresso, R.string.destino_progresso_curto),
    GRUPO(R.string.destino_grupo, AbaGrupo),

    /**
     * A quarta aba, da Fase 4 (docs/03 §2). **Não aparece sozinha**: a porta é o primeiro
     * fechamento semanal (`F4-T09`), e até lá a barra tem três destinos.
     */
    POKEDEX(R.string.destino_pokedex, AbaPokedex, R.string.destino_pokedex_curto);

    val iconeDeContorno
        @Composable get() = when (this) {
            HOJE -> Icons.Outlined.Home
            PROGRESSO -> Icons.Outlined.DateRange
            GRUPO -> Icons.Outlined.Person
            POKEDEX -> IconesDaPokedex.Contorno
        }

    val iconePreenchido
        @Composable get() = when (this) {
            HOJE -> Icons.Filled.Home
            PROGRESSO -> Icons.Filled.DateRange
            GRUPO -> Icons.Filled.Person
            POKEDEX -> IconesDaPokedex.Preenchido
        }
}

/** O anel de foco, afastado [FolgaDoFoco] da borda do item, com `raio-controle`. */
private fun Modifier.anelDeFoco(cor: Color): Modifier = drawWithContent {
    drawContent()
    val traco = LarguraDoFoco.toPx()
    val recuo = FolgaDoFoco.toPx() + traco / 2
    drawRoundRect(
        color = cor,
        topLeft = Offset(recuo, recuo),
        size = Size(size.width - 2 * recuo, size.height - 2 * recuo),
        cornerRadius = CornerRadius(Raios.controle.toPx()),
        style = Stroke(width = traco),
    )
}

/**
 * O rótulo desenhado na barra, que **encurta acima de [ESCALA_QUE_EMPILHA]** em vez de
 * partir no meio da palavra (`F1-T20`, docs/02 §8 item 9).
 *
 * Em 320dp cada aba fica com uns 106dp, e `Progresso` em `fontScale` 2,0 pede uns 117dp.
 * O emulador mostrou `Progr`/`esso` — e a segunda linha, além de partir a palavra sem
 * hífen, empurrava o item para fora do alinhamento vertical de `Hoje` e `Grupo`, que
 * continuavam de uma linha só. Uma barra de três abas com uma delas fora de esquadro lê
 * como defeito antes de alguém sequer notar a palavra partida.
 *
 * **Só encurta quem não cabe.** `Hoje` e `Grupo` foram vistos inteiros a 2,0 no
 * emulador; abreviar os três por simetria trocaria um defeito real por duas perdas de
 * legibilidade sem causa.
 *
 * **O nome da aba não muda** — `docs/03 §2` fixa `Hoje · Progresso · Grupo`, e é o
 * [DestinoDeTopo.rotulo] inteiro que continua no `contentDescription` do item, para o
 * TalkBack. O que encurta é o glifo.
 */
internal fun rotuloDe(destino: DestinoDeTopo, escala: Float, quatroAbas: Boolean = false): Int {
    // Com quatro abas cada uma tem 80dp a 320dp, e `Progresso` já parte em `fontScale` 1,3
    // (visto em 15/10): o limiar desce até incluir o 1,3, que é um dos passos do sistema.
    val limiar = if (quatroAbas) ESCALA_QUE_EMPILHA - 0.01f else ESCALA_QUE_EMPILHA
    return destino.rotuloCurto?.takeIf { escala > limiar } ?: destino.rotulo
}

/**
 * Com quatro abas o rótulo da barra para de crescer em 1,5: `Grupo` partia em `Grup`/`o` a
 * `fontScale` 2,0 em 80dp, sem forma curta que o salve sem renomear a aba. O rótulo segue
 * inteiro e legível; o que o teto tira é o crescimento além de 150%, num texto que o ícone
 * e o indicador acompanham (decisão nº 140).
 */
internal const val ESCALA_MAXIMA_DO_ROTULO_COM_QUATRO_ABAS = 1.5f

/**
 * A barra inferior, com o estado selecionado em **três canais** (docs/02 §4.2 e o guia
 * `BarraInferior` do design system).
 *
 * | Canal | Não selecionado | Selecionado |
 * |---|---|---|
 * | Ícone | contorno, `texto-secundario` | preenchido, `destaque` |
 * | Rótulo | `legenda` em 600, `texto-secundario` | `legenda` em 700, `texto-primario` |
 * | Indicador | nenhum | 60 × 32dp em `destaque-suave`, `raio-controle` |
 *
 * Três canais e não só cor porque um deles sozinho falha para alguém: a cor some no
 * daltonismo e sob sol aberto, e o preenchimento do ícone é sutil em glifo pequeno.
 * `destaque` só aparece no destino ativo, e a barra nunca leva cor de tipo.
 *
 * **O indicador do Material está desligado e redesenhado no slot do ícone.** O
 * `NavigationBarItem` desenha o indicador sempre em pílula e não aceita outra forma, e
 * o design system pede `raio-controle`, sem pílula.
 *
 * **O toque também é do tema, e não do item.** O `NavigationBarItem` fixa um ripple em
 * pílula no lugar do indicador, para pressionado e foco. Ele sai por
 * `LocalRippleConfiguration provides null`, e o item inteiro recebe o `LocalIndication`
 * do tema: `estado-pressionado` no retângulo do alvo, da altura toda da barra (docs/02
 * §4.2). O foco vira o anel de 2dp em `foco`, afastado 2dp da borda do item **para
 * dentro**: a superfície da barra recorta o que passa das bordas dela, e um anel de fora
 * perderia o lado de cima e o de baixo. O item continua sendo o do M3, e com ele
 * a semântica de aba (`Role.Tab` e `selected`).
 *
 * A barra é `superficie` com o filete superior de 1dp em `borda` (`elevacao-barra`):
 * sem sombra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraDeDestinos(
    destinoAtual: DestinoDeTopo,
    aoEscolher: (DestinoDeTopo) -> Unit,
    modifier: Modifier = Modifier,
    destinos: List<DestinoDeTopo> = DestinoDeTopo.entries.filter { it != DestinoDeTopo.POKEDEX },
    comBadge: Set<DestinoDeTopo> = emptySet(),
) {
    val esquema = MaterialTheme.colorScheme
    val quatroAbas = destinos.size > 3
    val filete = esquema.outlineVariant
    val foco = LocalCoresPokerun.current.foco
    val indicacao = LocalIndication.current

    // `LocalRippleConfiguration` ainda é experimental no material3 1.3.
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        NavigationBar(
            modifier = modifier.drawBehind {
                drawRect(color = filete, size = Size(size.width, AlturaDoFilete.toPx()))
            },
            containerColor = esquema.surface,
            tonalElevation = 0.dp,
        ) {
            destinos.forEach { destino ->
                val selecionado = destino == destinoAtual
                val nomeInteiro = stringResource(destino.rotulo)
                val novidade = stringResource(R.string.destino_novo)
                val fonte = remember { MutableInteractionSource() }
                val focado by fonte.collectIsFocusedAsState()

                NavigationBarItem(
                    selected = selecionado,
                    onClick = { aoEscolher(destino) },
                    interactionSource = fonte,
                    modifier = Modifier
                        .then(if (focado) Modifier.anelDeFoco(foco) else Modifier)
                        .indication(fonte, indicacao),
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(LarguraDoIndicador, AlturaDoIndicador)
                                .background(
                                    color = if (selecionado) esquema.secondaryContainer else Color.Transparent,
                                    shape = FormaControle,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BadgedBox(
                                badge = {
                                    // O destino novo leva o ponto até o primeiro acesso (docs/03
                                    // §3.20.1). O ponto não tem texto: a novidade vai no rótulo
                                    // lido pelo TalkBack, abaixo.
                                    if (destino in comBadge) Badge(containerColor = esquema.primary)
                                },
                            ) {
                                Icon(
                                    imageVector = if (selecionado) {
                                        destino.iconePreenchido
                                    } else {
                                        destino.iconeDeContorno
                                    },
                                    // O rótulo já nomeia o item para o TalkBack. Repetir aqui
                                    // faria o leitor de tela dizer o nome duas vezes.
                                    contentDescription = null,
                                    modifier = Modifier.size(Tamanhos.icone),
                                )
                            }
                        }
                    },
                    label = {
                        val densidade = LocalDensity.current
                        val teto = if (quatroAbas) {
                            densidade.fontScale.coerceAtMost(ESCALA_MAXIMA_DO_ROTULO_COM_QUATRO_ABAS)
                        } else {
                            densidade.fontScale
                        }
                        CompositionLocalProvider(LocalDensity provides Density(densidade.density, teto)) {
                            Text(
                                text = stringResource(rotuloDe(destino, densidade.fontScale, quatroAbas)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selecionado) FontWeight.Bold else FontWeight.SemiBold,
                                ),
                                // O nome inteiro chega ao TalkBack mesmo quando o glifo encurta:
                                // `Prog.` é uma forma de desenhar `Progresso`, não outra aba.
                                modifier = Modifier.semantics {
                                    contentDescription = if (destino in comBadge) "$nomeInteiro, $novidade" else nomeInteiro
                                },
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = esquema.primary,
                        selectedTextColor = esquema.onSurface,
                        unselectedIconColor = esquema.onSurfaceVariant,
                        unselectedTextColor = esquema.onSurfaceVariant,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}
