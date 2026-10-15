package com.hggabriel.pokerun.ui.componentes

import androidx.compose.ui.unit.dp

/**
 * As sombras do design system, por componente (docs/02 §4). O tema não tem sombra:
 * `surfaceTint` é transparente e cada componente que flutua passa a sua.
 *
 * **A aproximação.** Cada token do `tokens.json` tem duas camadas de `box-shadow`: uma
 * curta e nítida, colada ao objeto, e uma longa e difusa, deslocada para baixo. O
 * Compose desenha uma sombra só, a de elevação do Android, mas essa sombra já tem duas
 * fontes: a luz ambiente, que escurece em volta por igual, e a luz pontual, que joga
 * a sombra para baixo. A camada curta vira a ambiente e a longa vira a pontual. O que
 * se escolhe é a elevação em dp, e a cor vai em `texto-primario`, a mesma base
 * `rgba(20, 22, 31)` do token, no lugar do preto puro.
 *
 * O alfa não se controla por componente: o Android aplica o do tema da janela (cerca
 * de 4% na ambiente e 19% na pontual), que fica perto dos 12% e 22% do token.
 */
internal object Elevacao {
    /**
     * `elevacao-especie`: só o card de espécie, o objeto colecionável que sai da página.
     * Token: `0 1px 0` a 6% e `0 4px 12px -6px` a 16%. A camada longa tem espalhamento
     * negativo de 6px, então quase não passa da borda: 2dp dá uma sombra curta, colada
     * embaixo, sem halo nos lados.
     */
    val especie = 2.dp

    /**
     * `elevacao-flutuante`: FAB e bottom sheet. Token: `0 2px 4px` a 12% e
     * `0 8px 20px -4px` a 22%. 6dp é o deslocamento e o desfoque mais próximos da
     * camada longa, e é o mesmo valor que o FAB do M3 já usava.
     *
     * O app ainda não tem bottom sheet. O primeiro `ModalBottomSheet` passa
     * `shadowElevation = Elevacao.flutuante` e `tonalElevation = 0.dp`.
     */
    val flutuante = 6.dp

    /** `elevacao-plana`: card de dado e linha de lista. Sem sombra, delimitados pela `borda`. */
    val plana = 0.dp
}
