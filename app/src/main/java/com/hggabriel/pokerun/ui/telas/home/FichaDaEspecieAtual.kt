package com.hggabriel.pokerun.ui.telas.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.Escada
import com.hggabriel.pokerun.ui.componentes.FichaDeEspecime
import com.hggabriel.pokerun.ui.componentes.VarianteDaEscada
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.nomeDoTipo
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Tamanhos

private val EspacoEntreLinhas = Espacos.relacionado

/**
 * De quem é a espécie ao progresso dela. A escada compacta já tem 18dp de ar acima da
 * trilha, dentro do controle de 48dp, e somado a isto o grupo do progresso se separa do
 * nome sem precisar de filete.
 */
private val EspacoAntesDoProgresso = Espacos.interno

/**
 * O card da espécie atual (docs/01 §2.1, docs/03 §3.3.2), na ficha de espécie em forma
 * de Pokébola aberta (docs/02 §4.3):
 *
 * ```
 *    [arte oficial, 152dp]           metade de cima em tipo-*-suave do primário
 *  ──────────( ● )──────────
 *     #014 · Kakuna
 *    [Inseto] [Venenoso]           chips de tipo
 *
 *  ▐██▌▐█▌▐  ▌▐  ▌ …              escada compacta (Fase 4)
 *     Tier 2 · Passo Firme
 *  faltam 60 XP para Paras         fecha o grupo do progresso
 * ```
 *
 * Sem navegação para detalhe. Sem rede e sem cache, a arte não aparece: a metade de cima
 * guarda o espaço dela e o número fica no corpo, nunca um placeholder de outro serviço
 * (ficha de `F2-T14`). A silhueta de RN-36 é preenchimento chapado, não opacidade
 * (docs/03 §3.18).
 *
 * **Os tipos vêm da PokéAPI**, como o nome. Enquanto não chegam (sem rede e sem cópia
 * guardada), a metade de cima fica em `superficie-afundada` e não há chip. A cor de tipo
 * nunca aparece sem o nome: o chip o escreve, e a descrição o diz ao TalkBack (docs/02
 * §2.5). O botão central vem preenchido enquanto a espécie está alcançada, e vazio quando
 * RN-36 a esvaneceu.
 */
@Composable
fun FichaDaEspecieAtual(card: CardDaEspecie, modifier: Modifier = Modifier, aoTocar: (() -> Unit)? = null) {
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
    val nomesDosTipos = card.tipos.map { stringResource(nomeDoTipo(it)) }
    val quem = if (nomesDosTipos.isEmpty()) {
        nome ?: numero
    } else {
        stringResource(
            R.string.especie_descricao_tipos,
            nome ?: numero,
            nomesDosTipos.reduce { a, b -> stringResource(R.string.especie_tipos_e, a, b) },
        )
    }
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
        tipo = card.tipos.firstOrNull(),
        capturada = !card.silhueta,
        aoTocar = aoTocar,
        modifier = modifier.clearAndSetSemantics { contentDescription = descricao },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espacos.margem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = identidade, style = Estilos.nomeEspecie, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            if (card.tipos.isNotEmpty()) {
                Spacer(Modifier.height(EspacoEntreLinhas))
                Row(horizontalArrangement = Arrangement.spacedBy(EspacoEntreLinhas)) {
                    card.tipos.forEach { ChipDeTipo(it) }
                }
            }
            // Dois grupos, na ordem em que se lê a ficha (layout de 17/10/2026): **quem**
            // (nome e tipos) e **o progresso** (a escada e o que falta). Antes o `faltam`
            // vinha colado aos chips, 4dp abaixo deles, e a escada 8dp abaixo do `faltam`: a
            // frase do próximo alvo se lia como legenda dos tipos. Agora ela fecha a escada,
            // que é de onde o próximo alvo vem.
            Spacer(Modifier.height(EspacoAntesDoProgresso))
            // A escada compacta: só o nível 1 (docs/02 §5). Fala a frase de docs/02 §5, e a
            // ficha inteira é a porta para a aba Pokédex.
            if (card.escada != null) {
                Escada(
                    escada = card.escada,
                    variante = VarianteDaEscada.COMPACTA,
                    nomeDaEspecie = { numero -> if (numero == card.proximoNumero) card.proximoNome else null },
                )
            }
            if (alvo != null) {
                if (card.escada != null) Spacer(Modifier.height(EspacoEntreLinhas / 2))
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
