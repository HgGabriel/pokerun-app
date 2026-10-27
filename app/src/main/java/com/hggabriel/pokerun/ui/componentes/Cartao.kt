package com.hggabriel.pokerun.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Raios

/** A forma do cartão e do banner das páginas tingidas. */
val FormaDoCartao = RoundedCornerShape(Raios.especie)

/**
 * O cartão das páginas tingidas (o detalhe do plano, o grupo do plano e a aba Grupo): branco,
 * `raio-especie`, sem borda. Sobre `superficie-afundada` ele se delimita sozinho.
 */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    recheio: PaddingValues = PaddingValues(Espacos.margem),
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(MaterialTheme.colorScheme.surface)
            .padding(recheio),
        content = conteudo,
    )
}

/** O rótulo do cartão: `rotulo` em caixa alta e `texto-secundario`, no canto de cima (docs/02 §1). */
@Composable
fun RotuloDoCartao(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = Estilos.rotulo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private val AlturaDaEscolha = 48.dp

/**
 * A escolha cheia da segunda linguagem (docs/02 §1): bloco de `superficie-afundada` com
 * `raio-controle`; marcada, vira tinta com o texto em `superficie`. Sem check e sem contorno,
 * para poucas opções lado a lado. Nasceu no registro de corrida (`Hoje`, `Ontem`, `Outro dia`).
 */
@Composable
fun RowScope.EscolhaCheia(
    texto: String,
    selecionada: Boolean,
    aoTocar: () -> Unit,
    icone: ImageVector? = null,
) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val fundo by animateColorAsState(
        if (selecionada) tinta else LocalCoresPokerun.current.superficieAfundada,
        label = "fundo da escolha",
    )
    val cor = if (selecionada) claro else tinta
    Row(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = AlturaDaEscolha)
            .clip(FormaControle)
            .background(fundo)
            .clickable(role = Role.RadioButton, onClick = aoTocar)
            .semantics { this.selected = selecionada }
            .padding(horizontal = Espacos.relacionado),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = texto, style = MaterialTheme.typography.labelLarge, color = cor, maxLines = 1)
    }
}
