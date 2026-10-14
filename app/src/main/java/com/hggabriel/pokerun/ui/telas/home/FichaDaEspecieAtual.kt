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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.FichaDeEspecime
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import java.util.Locale

private val TamanhoDoSprite = 120.dp
private val Recheio = 16.dp
private val EspacoEntreLinhas = 8.dp

/**
 * O card da espécie atual (docs/01 §2.1, docs/03 §3.3.2), numa ficha de espécime:
 *
 * ```
 *         [sprite]
 *  #014 · KAKUNA
 *  faltam 60 XP para Paras
 * ```
 *
 * Sem navegação para detalhe. Sem rede e sem cache, o sprite não aparece e o número fica
 * sozinho sobre o papel, nunca um placeholder de outro serviço (ficha de `F2-T14`). A
 * silhueta de RN-36 é preenchimento chapado, não opacidade (docs/03 §3.18).
 */
@Composable
fun FichaDaEspecieAtual(card: CardDaEspecie, modifier: Modifier = Modifier) {
    val cores = LocalCoresPokerun.current
    val nome = card.nome
    val numero = stringResource(R.string.especie_numero, card.numero)
    val identidade = if (nome != null) {
        stringResource(R.string.especie_numero_nome, card.numero, nome.uppercase(Locale.ROOT))
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

    FichaDeEspecime(modifier = modifier.clearAndSetSemantics { contentDescription = descricao }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Recheio),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!semSprite) {
                AsyncImage(
                    model = card.sprite,
                    contentDescription = null,
                    modifier = Modifier.size(TamanhoDoSprite),
                    colorFilter = if (card.silhueta) ColorFilter.tint(cores.especimeTintaFraca, BlendMode.SrcIn) else null,
                    onError = { semSprite = true },
                )
                Spacer(Modifier.height(EspacoEntreLinhas))
            }
            Text(text = identidade, style = EstiloDado, color = cores.especimeTinta, textAlign = TextAlign.Center)
            if (alvo != null) {
                Spacer(Modifier.height(EspacoEntreLinhas / 2))
                Text(
                    text = alvo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.especimeTintaFraca,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
