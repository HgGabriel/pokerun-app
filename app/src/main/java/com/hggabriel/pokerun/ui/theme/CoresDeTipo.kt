package com.hggabriel.pokerun.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon

/**
 * Os três tokens de um tipo (docs/02 §2.5).
 *
 * - [base]: chip e ícone de tipo. ≥ 3:1 sobre fundo, superfície e [suave].
 * - [sobre]: texto e ícone sobre a [base]. ≥ 4,5:1.
 * - [suave]: fundo da área do sprite no card de espécie, no item da Pokédex e no
 *   avatar. `texto-primario` sobre ele passa de 14,4:1.
 *
 * A cor de tipo só aparece onde o tipo é informação, e sempre com o nome ou o ícone
 * do tipo. Nunca vira fundo de tela.
 */
@Immutable
data class CorDeTipo(
    val base: Color,
    val sobre: Color,
    val suave: Color,
)

/**
 * O portador dos 18 tipos, exposto pelo tema em [LocalCoresDeTipo]. A chave é o
 * enum do domínio, [TipoDePokemon].
 *
 * Exige os 18 na construção: um tipo esquecido falha ao abrir o app, não numa tela
 * que por acaso mostre aquele tipo.
 */
@Immutable
class CoresDeTipo(porTipo: Map<TipoDePokemon, CorDeTipo>) {

    private val porTipo: Map<TipoDePokemon, CorDeTipo> = porTipo.toMap()

    init {
        val faltando = TipoDePokemon.entries - porTipo.keys
        require(faltando.isEmpty()) { "Tipos sem cor: $faltando" }
    }

    operator fun get(tipo: TipoDePokemon): CorDeTipo = porTipo.getValue(tipo)

    override fun equals(other: Any?): Boolean = other is CoresDeTipo && other.porTipo == porTipo

    override fun hashCode(): Int = porTipo.hashCode()
}

/** A única instância que existe hoje. O app entrega um tema claro (D-13). */
val LocalCoresDeTipo = staticCompositionLocalOf { CoresDeTipoClaro }
