package com.hggabriel.pokerun.ui.telas.onboarding

import com.hggabriel.pokerun.ui.componentes.distanciaEmKm

/*
 * A leitura dos campos do perfil (`F1-T08`, docs/03 §3.2).
 *
 * Está fora do `ViewModel` porque é a parte da tela que se prova sem aparelho e sem
 * Firestore — e é justamente a que a revisão de olho deixa passar.
 * `PassosDoOnboardingTest` roda o nome e a distância.
 */

/** O nome do perfil, sem espaço sobrando. Nulo é "o usuário não respondeu". */
internal fun nomeDoPerfil(texto: String): String? = texto.trim().ifBlank { null }

/**
 * A licença só sai completa (29/10/2026): nome, distância e um treinador da lista. É o que
 * libera o botão de emitir, e o `ViewModel` confere de novo antes de gravar.
 */
internal fun fichaCompleta(nome: String, distancia: String, treinador: String?, aceita: (String) -> Boolean): Boolean =
    nomeDoPerfil(nome) != null && distanciaEmKm(distancia) != null && treinador != null && aceita(treinador)
