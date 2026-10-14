package com.hggabriel.pokerun.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Os raios do design system, em dp, para quem desenha em `Canvas` e precisa do
 * número em vez da forma (docs/02 §4).
 *
 * Os raios variam por papel, sem pílula. `raio-circulo` não tem número: é
 * [FormaCirculo].
 */
object Raios {
    /** Segmentos da barra de sessões, marcas da escada, células do heatmap. */
    val dado = 2.dp
    /** Chip de tipo e etiquetas. */
    val chip = 6.dp
    /** Botões, campos, item da Pokédex, avatar, indicador da barra inferior. */
    val controle = 12.dp
    /** Cards de conteúdo e FAB. */
    val card = 16.dp
    /** Card de espécie e herói do detalhe. */
    val especie = 24.dp
}

/** Barras de dado, segmentos da barra de sessões, marcas da escada. */
val FormaDado = RoundedCornerShape(Raios.dado)

/** Chip de tipo e etiquetas. Não é pílula. */
val FormaChip = RoundedCornerShape(Raios.chip)

/** Botões, campos, item da Pokédex, avatar e indicador da barra inferior. */
val FormaControle = RoundedCornerShape(Raios.controle)

/** Cards de conteúdo (semana, corrida, desafio) e FAB. */
val FormaCard = RoundedCornerShape(Raios.card)

/** O card de espécie, a ficha em forma de Pokébola, e o herói do detalhe. */
val FormaEspecie = RoundedCornerShape(Raios.especie)

/** Só para Pokébola: marcador de captura, botão central da ficha, loader. */
val FormaCirculo = CircleShape

/**
 * Os cinco slots do M3, cada um escolhido pelos componentes que o consomem, e não
 * pela ordem de tamanho: por isso `extraSmall` é maior que `small`.
 *
 * Botão, segmented button e indicador da `NavigationBar` não leem slot nenhum: o M3
 * os desenha em pílula fixa, e o raio deles é passado no componente.
 */
val PokerunShapes = Shapes(
    // `OutlinedTextField`, `Snackbar`, `DropdownMenu`: controles.
    extraSmall = FormaControle,
    // Chips.
    small = FormaChip,
    // `Card`.
    medium = FormaCard,
    // `FloatingActionButton` e `ExtendedFloatingActionButton`: o FAB usa `raio-card`.
    large = FormaCard,
    // `AlertDialog`, `ModalBottomSheet`, `DatePickerDialog`.
    extraLarge = FormaCard,
)
