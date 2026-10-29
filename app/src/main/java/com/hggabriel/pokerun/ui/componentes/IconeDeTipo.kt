package com.hggabriel.pokerun.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.Tamanhos

/** A máscara do glifo do tipo, recortada do `symbol_icon` do Let's Go (60px). */
@DrawableRes
fun glifoDoTipo(tipo: TipoDePokemon): Int = when (tipo) {
    TipoDePokemon.NORMAL -> R.drawable.glifo_tipo_normal
    TipoDePokemon.FOGO -> R.drawable.glifo_tipo_fogo
    TipoDePokemon.AGUA -> R.drawable.glifo_tipo_agua
    TipoDePokemon.PLANTA -> R.drawable.glifo_tipo_planta
    TipoDePokemon.ELETRICO -> R.drawable.glifo_tipo_eletrico
    TipoDePokemon.GELO -> R.drawable.glifo_tipo_gelo
    TipoDePokemon.LUTADOR -> R.drawable.glifo_tipo_lutador
    TipoDePokemon.VENENOSO -> R.drawable.glifo_tipo_venenoso
    TipoDePokemon.TERRESTRE -> R.drawable.glifo_tipo_terrestre
    TipoDePokemon.VOADOR -> R.drawable.glifo_tipo_voador
    TipoDePokemon.PSIQUICO -> R.drawable.glifo_tipo_psiquico
    TipoDePokemon.INSETO -> R.drawable.glifo_tipo_inseto
    TipoDePokemon.PEDRA -> R.drawable.glifo_tipo_pedra
    TipoDePokemon.FANTASMA -> R.drawable.glifo_tipo_fantasma
    TipoDePokemon.DRAGAO -> R.drawable.glifo_tipo_dragao
    TipoDePokemon.SOMBRIO -> R.drawable.glifo_tipo_sombrio
    TipoDePokemon.ACO -> R.drawable.glifo_tipo_aco
    TipoDePokemon.FADA -> R.drawable.glifo_tipo_fada
}

/** O fundo de 512px do tipo (WebP, pokeos.com), esfumado, atrás da arte da espécie na ficha. */
@DrawableRes
fun fundoDoTipo(tipo: TipoDePokemon): Int = when (tipo) {
    TipoDePokemon.NORMAL -> R.drawable.fundo_tipo_normal
    TipoDePokemon.FOGO -> R.drawable.fundo_tipo_fogo
    TipoDePokemon.AGUA -> R.drawable.fundo_tipo_agua
    TipoDePokemon.PLANTA -> R.drawable.fundo_tipo_planta
    TipoDePokemon.ELETRICO -> R.drawable.fundo_tipo_eletrico
    TipoDePokemon.GELO -> R.drawable.fundo_tipo_gelo
    TipoDePokemon.LUTADOR -> R.drawable.fundo_tipo_lutador
    TipoDePokemon.VENENOSO -> R.drawable.fundo_tipo_venenoso
    TipoDePokemon.TERRESTRE -> R.drawable.fundo_tipo_terrestre
    TipoDePokemon.VOADOR -> R.drawable.fundo_tipo_voador
    TipoDePokemon.PSIQUICO -> R.drawable.fundo_tipo_psiquico
    TipoDePokemon.INSETO -> R.drawable.fundo_tipo_inseto
    TipoDePokemon.PEDRA -> R.drawable.fundo_tipo_pedra
    TipoDePokemon.FANTASMA -> R.drawable.fundo_tipo_fantasma
    TipoDePokemon.DRAGAO -> R.drawable.fundo_tipo_dragao
    TipoDePokemon.SOMBRIO -> R.drawable.fundo_tipo_sombrio
    TipoDePokemon.ACO -> R.drawable.fundo_tipo_aco
    TipoDePokemon.FADA -> R.drawable.fundo_tipo_fada
}

/**
 * O ícone de tipo (`ChipDeTipo.md`): o disco em `tipo-<id>` e o glifo do Let's Go em
 * `tipo-<id>-sobre`. O glifo vem como máscara, sem a cor do jogo: como vinha, 13 dos 18
 * tipos ficavam abaixo de 3:1 (o elétrico, 1,49:1). Com os tokens, o glifo passa 5:1 sobre o
 * disco, e o disco passa 3:1 sobre o branco e sobre o `tipo-suave`.
 *
 * Decorativo: o nome do tipo vai na descrição de quem o usa, a figurinha ou o chip.
 */
@Composable
fun IconeDeTipo(tipo: TipoDePokemon, modifier: Modifier = Modifier, lado: Dp = Tamanhos.iconeTipo) {
    val cor = LocalCoresDeTipo.current[tipo]
    Box(modifier = modifier.size(lado).background(cor.base, FormaCirculo)) {
        Icon(
            painter = painterResource(glifoDoTipo(tipo)),
            contentDescription = null,
            tint = cor.sobre,
            modifier = Modifier.size(lado),
        )
    }
}
