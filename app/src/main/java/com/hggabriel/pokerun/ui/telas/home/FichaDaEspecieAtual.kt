package com.hggabriel.pokerun.ui.telas.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.FichaDeEspecime
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Tamanhos

private val EspacoEntreLinhas = Espacos.relacionado

/**
 * O card da espécie atual (docs/01 §2.1, docs/03 §3.3.2), na ficha de espécie em forma
 * de Pokébola aberta (docs/02 §4.3):
 *
 * ```
 *    [arte oficial, 152dp]
 *  ──────────( ● )──────────
 *     #014 · Kakuna
 *  faltam 60 XP para Paras
 * ```
 *
 * Sem navegação para detalhe. Sem rede e sem cache, a arte não aparece: a metade de cima
 * guarda o espaço dela e o número fica no corpo, nunca um placeholder de outro serviço
 * (ficha de `F2-T14`). A silhueta de RN-36 é preenchimento chapado, não opacidade
 * (docs/03 §3.18).
 *
 * **O tipo ainda não chega aqui.** Da PokéAPI saem só nome e sprite (decisão nº 102), e
 * sem tipo a metade de cima fica em `superficie-afundada`. O botão central vem
 * preenchido enquanto a espécie está alcançada, e vazio quando RN-36 a esvaneceu.
 */
@Composable
fun FichaDaEspecieAtual(card: CardDaEspecie, modifier: Modifier = Modifier) {
    val cores = LocalCoresPokerun.current
    val nome = card.nome
    val numero = stringResource(R.string.especie_numero, card.numero)
    val identidade = if (nome != null) {
        stringResource(R.string.especie_numero_nome, card.numero, nome)
    } else {
        numero
    }
    val alvo = card.proximoNumero?.let { proximo ->
        card.faltamXp?.let { faltam ->
            stringResource(
                R.string.especie_faltam,
                String.format(LocaleDoApp, "%,d", faltam),
                card.proximoNome ?: stringResource(R.string.especie_numero, proximo),
            )
        }
    }
    val quem = nome ?: numero
    val descricao = buildString {
        append(stringResource(if (card.silhueta) R.string.especie_descricao_silhueta else R.string.especie_descricao, quem))
        if (alvo != null) append(". ").append(alvo)
    }

    var semSprite by remember(card.sprite) { mutableStateOf(false) }

    FichaDeEspecime(
        arte = {
            if (semSprite) {
                Spacer(Modifier.size(Tamanhos.arteCard))
            } else {
                AsyncImage(
                    model = card.sprite,
                    contentDescription = null,
                    modifier = Modifier.size(Tamanhos.arteCard),
                    colorFilter = if (card.silhueta) ColorFilter.tint(cores.silhueta, BlendMode.SrcIn) else null,
                    onError = { semSprite = true },
                )
            }
        },
        capturada = !card.silhueta,
        modifier = modifier.clearAndSetSemantics { contentDescription = descricao },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espacos.margem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = identidade, style = Estilos.nomeEspecie, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            if (alvo != null) {
                Spacer(Modifier.height(EspacoEntreLinhas / 2))
                Text(
                    text = alvo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
