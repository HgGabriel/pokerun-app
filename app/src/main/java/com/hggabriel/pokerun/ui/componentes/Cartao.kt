package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.hggabriel.pokerun.ui.theme.Espacos
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
