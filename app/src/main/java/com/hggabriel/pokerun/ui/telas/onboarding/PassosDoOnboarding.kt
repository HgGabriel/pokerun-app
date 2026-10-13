package com.hggabriel.pokerun.ui.telas.onboarding

/*
 * A leitura dos campos do perfil (`F1-T08`, docs/03 §3.2).
 *
 * Está fora do `ViewModel` porque é a parte da tela que se prova sem aparelho e sem
 * Firestore — e é justamente a que a revisão de olho deixa passar.
 * `PassosDoOnboardingTest` roda o nome e a distância.
 */

/** O nome do perfil, sem espaço sobrando. Nulo é "o usuário não respondeu". */
internal fun nomeDoPerfil(texto: String): String? = texto.trim().ifBlank { null }
