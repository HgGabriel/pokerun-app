package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import java.util.Locale

/** O filete que fecha o cabeçalho. 1dp em `borda`, idêntico em todas as telas. */
private val AlturaDoFilete = 1.dp

/** A goteira do cabeçalho. Ele é bloco de largura cheia: quem o usa não acrescenta padding. */
private val Goteira = 16.dp

/**
 * A goteira do lado da ação. Menor que a do texto porque o alvo da ação já reserva
 * 12dp entre a borda da sua área de toque de 48dp e o glifo — com 16dp aqui o ícone
 * ficaria 28dp para dentro e não alinharia com o texto do outro lado.
 */
private val GoteiraDaAcao = 4.dp

/**
 * A linha da sobrancelha tem 48dp **sempre**, com ou sem ação.
 *
 * Duas coisas de uma vez: é o piso de alvo de toque da engrenagem (docs/02 §8, item
 * 2) e é o que mantém o título na mesma altura nas 20 telas. Se a altura viesse do
 * conteúdo, o título de uma raiz de aba (com engrenagem) e o de um detalhe (sem)
 * cairiam em alturas diferentes — e a âncora de docs/02 §10 é exatamente "no mesmo
 * lugar, da mesma forma".
 *
 * **A sobrancelha e a ação centram nesta linha, as duas.** A primeira versão alinhava
 * o texto embaixo, para colá-lo ao título, e no emulador a engrenagem apareceu
 * flutuando 12dp acima do rótulo — o glifo centra na sua área de toque, e não havia
 * como as duas coisas se encontrarem. O desenho de docs/02 §10.1 põe sobrancelha e
 * ação na mesma linha, e é a linha que manda.
 */
private val AlturaDaSobrancelha = 48.dp

private val EspacoAntesDoTitulo = 4.dp
private val EspacoAntesDoFilete = 12.dp

/** O ponto médio que separa os níveis. Nunca travessão (docs/02 §9.1). */
private const val SEPARADOR = " · "

/**
 * O mesmo caminho para o TalkBack. O ponto médio vira vírgula porque o leitor de tela
 * pronuncia o glifo, e "Progresso ponto médio Histórico" não é o que a tela diz.
 */
private const val SEPARADOR_FALADO = ", "

/**
 * O número da sobrancelha, e a única forma dele (docs/02 §10.3).
 *
 * **Existe para que `n de N` seja a única maneira de escrever um número ali.** A regra
 * proíbe numeração ornamental — `01 / 02 / 03` decorando seções —, e regra que depende
 * de o autor da tela lembrar dela é regra que cai na décima tela. Com o índice sendo um
 * tipo de dois campos obrigatórios, `N` não tem como faltar: não existe construtor que
 * aceite só o `n`.
 *
 * @param n a posição, de 1 a [total].
 * @param total o denominador que o usuário reconhece: 21 semanas, 8 tiers, 151 entradas.
 */
data class Indice(val n: Int, val total: Int)

/**
 * O cabeçalho de ficha (`F1-T07b`, docs/02 §10).
 *
 * Três partes, sempre nesta ordem: **sobrancelha** em mono caixa alta, **título** com o
 * conteúdo daquela instância e **filete** de 1dp. Mais um slot de ação à direita.
 *
 * ```
 * PROGRESSO · HISTÓRICO            [engrenagem]
 * Setembro de 2026
 * ─────────────────────────────────────────────
 * ```
 *
 * **Não é uma `TopAppBar`, e a diferença não é de nome.** O cabeçalho é conteúdo: ele é
 * o primeiro item da `LazyColumn` da tela e rola junto com ela, que é onde a ficha de
 * `F1-T09` o coloca. Uma `TopAppBar` fica presa ao `Scaffold` e ganha elevação e sombra
 * ao rolar — as duas coisas que docs/02 §4 tira do sistema —, além de trazer o
 * vocabulário do Material para o topo de um app que é ficha impressa. **Nenhuma tela
 * desenha a sua**, e há teste que falha nisso.
 *
 * **A sobrancelha é o caminho, não o título.** Ela chega em caixa mista e sai em caixa
 * alta: quem chama escreve `listOf("Progresso", "Histórico")`, e a caixa é do
 * componente. Máximo de dois níveis, porque é o que cabe numa linha.
 *
 * **O título é o conteúdo daquela instância**, não o nome da tela: a data da corrida, o
 * nome do plano, `#059 Arcanine`. Ele quebra em quantas linhas precisar — nome longo de
 * plano é dado do usuário, e truncar dado é pior que ocupar duas linhas.
 *
 * @param sobrancelha os níveis do caminho, de um a dois, em caixa mista e **sem
 *   número** — número é [indice].
 * @param indice o `n de N` que se cola ao último nível: `PLANO · SEMANA 3 DE 21`.
 * @param acao o slot da direita. Na raiz de uma aba é a engrenagem, e para isso existe
 *   [CabecalhoDeAba], que não deixa esquecer.
 * @param aoTocarNoTitulo torna o título um alvo. Existe por docs/03 §3.3, que põe a
 *   troca de plano no toque do nome do plano na Home — e é a **única** porta para a
 *   `PlansListScreen` que a especificação desenha. Nulo, que é o caso das outras 19
 *   telas, deixa o título como texto e a geometria como estava.
 */
@Composable
fun CabecalhoDeFicha(
    sobrancelha: List<String>,
    titulo: String,
    modifier: Modifier = Modifier,
    indice: Indice? = null,
    acao: @Composable (() -> Unit)? = null,
    aoTocarNoTitulo: (() -> Unit)? = null,
) {
    val falada = sobrancelhaFalada(sobrancelha, indice)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Goteira,
                    end = if (acao != null) GoteiraDaAcao else Goteira,
                )
                .heightIn(min = AlturaDaSobrancelha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sobrancelhaVisivel(sobrancelha, indice),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Teto de duas linhas, que é o que docs/02 §8, item 9 dá a rótulo em
                // caixa alta. Passar disso em `fontScale` 2,0 é sinal de sobrancelha
                // comprida demais, e o conserto é encurtar a copy, não subir o teto.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    // A caixa alta é desenho, não pronúncia: parte dos leitores de
                    // tela soletra palavra inteira em maiúscula, tomando-a por sigla.
                    // O TalkBack ouve o caminho em caixa mista, com vírgula no lugar
                    // do ponto médio.
                    .clearAndSetSemantics { contentDescription = falada },
            )

            acao?.invoke()
        }

        Spacer(Modifier.height(EspacoAntesDoTitulo))

        // O título tocável cresce até 48dp e centra o texto nessa faixa (docs/02 §8,
        // item 2). O topo dele não se mexe: a linha da sobrancelha continua fixa acima,
        // e é ela que mantém o título na mesma altura nas 20 telas. O que cresce é a
        // folga até o filete, e só na tela que tem a ação.
        val alvoDoTitulo = if (aoTocarNoTitulo != null) {
            Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDaSobrancelha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = aoTocarNoTitulo,
                )
        } else {
            Modifier
        }

        Box(modifier = alvoDoTitulo, contentAlignment = Alignment.CenterStart) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = Goteira)
                    .semantics { heading() },
            )
        }

        Spacer(Modifier.height(EspacoAntesDoFilete))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Goteira),
            thickness = AlturaDoFilete,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * O cabeçalho da **raiz de uma aba**, com a engrenagem de Ajustes já no slot de ação
 * (docs/02 §10.1).
 *
 * Existe para que "a engrenagem fica na raiz de toda aba" seja verdade por construção e
 * não por disciplina. A alternativa — cada raiz de aba passar o próprio `acao` — dá o
 * mesmo pixel hoje e, na Fase 4, uma Pokédex sem engrenagem: quem escrever a quarta aba
 * em outubro não vai reler docs/02 §10 para descobrir que ela existe.
 *
 * @param aba o rótulo da aba, em caixa mista. É o único nível da sobrancelha.
 */
@Composable
fun CabecalhoDeAba(
    aba: String,
    titulo: String,
    aoAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
    indice: Indice? = null,
    aoTocarNoTitulo: (() -> Unit)? = null,
) {
    CabecalhoDeFicha(
        sobrancelha = listOf(aba),
        titulo = titulo,
        modifier = modifier,
        indice = indice,
        acao = { AcaoDeAjustes(aoAbrirAjustes) },
        aoTocarNoTitulo = aoTocarNoTitulo,
    )
}

/**
 * A engrenagem, e a razão de ela **não ser um `IconButton`**.
 *
 * O `IconButton` do Material 3 fixa `ripple()` no seu `clickable` e ignora o
 * `LocalIndication` do tema — no emulador a engrenagem apareceu com o halo circular
 * cinza que docs/02 §4.2 tira do sistema inteiro. É o mesmo motivo pelo qual a [Ficha]
 * não usa o overload clicável do `Card`, e não se enxerga em `compileDebugKotlin`: um
 * ripple errado compila igual.
 *
 * O que sobra do `IconButton` está aqui à mão: 48dp de alvo (docs/02 §8, item 2),
 * `Role.Button` para o TalkBack anunciar o que é, e o glifo centrado.
 *
 * A tinta é `tinta-fraca`, a mesma da sobrancelha ao lado: Ajustes é cromo de tela, e
 * `leitura` é a cor da ação principal — que Ajustes nunca é, em tela nenhuma.
 */
@Composable
private fun AcaoDeAjustes(aoAbrirAjustes: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AlturaDaSobrancelha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = aoAbrirAjustes,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.acao_ajustes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// A sobrancelha, em texto
// ---------------------------------------------------------------------------

/** O que a tela mostra: caixa alta, níveis separados por ponto médio. */
internal fun sobrancelhaVisivel(niveis: List<String>, indice: Indice?): String =
    // `Locale.ROOT`, e não o do aparelho: `uppercase()` sem argumento usa o locale
    // padrão, e num aparelho em turco o "i" de "Histórico" viraria "İ".
    montarSobrancelha(niveis, indice, SEPARADOR).uppercase(Locale.ROOT)

/** O que o TalkBack lê: caixa mista, níveis separados por vírgula. */
internal fun sobrancelhaFalada(niveis: List<String>, indice: Indice?): String =
    montarSobrancelha(niveis, indice, SEPARADOR_FALADO)

/**
 * A regra do índice (docs/02 §10.3), no único lugar onde ela é verificável.
 *
 * As exigências falham alto, e é de propósito: sobrancelha é copy de tela, fixa no
 * código, então o primeiro preview já derruba o erro. Devolver a string errada em
 * silêncio é o que produz `SEMANA 3` numa tela e `3 DE 21` na outra.
 */
private fun montarSobrancelha(
    niveis: List<String>,
    indice: Indice?,
    separador: String,
): String {
    require(niveis.isNotEmpty()) { "A sobrancelha precisa de pelo menos um nível (docs/02 §10.1)." }
    require(niveis.size <= 2) {
        "A sobrancelha nunca passa de dois níveis, porque cabe numa linha " +
            "(docs/02 §10.1). Veio: $niveis"
    }

    val comNumero = niveis.filter { nivel -> nivel.any(Char::isDigit) }
    require(comNumero.isEmpty()) {
        "Número na sobrancelha só na forma `n de N`, pelo Indice (docs/02 §10.3). " +
            "Sem N não há número. Veio: $comNumero"
    }

    if (indice != null) {
        require(indice.total >= 1) { "O total do índice é o que o usuário reconhece: $indice" }
        require(indice.n in 1..indice.total) { "A posição do índice está fora do total: $indice" }
    }

    return niveis
        .mapIndexed { posicao, nivel ->
            // O número cola no ÚLTIMO nível, que é o que ele conta: `Semana 3 de 21`,
            // nunca `Plano 3 de 21 · Semana`.
            if (indice != null && posicao == niveis.lastIndex) {
                "$nivel ${indice.n} de ${indice.total}"
            } else {
                nivel
            }
        }
        .joinToString(separador)
}
