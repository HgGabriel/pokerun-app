package com.hggabriel.pokerun.ui.telas.login

import androidx.annotation.StringRes

/**
 * O estado da `LoginScreen` (`F1-T06`, docs/03 §3.1).
 *
 * A ficha nomeia três estados — ocioso, carregando e erro — e aqui há um quarto,
 * [Autenticado], que é o **terminal**: a tela não muda de aparência nele, ela sai.
 * Ele existe porque o roteamento é decidido aqui e executado lá fora — a tela não
 * conhece as rotas das outras (`ui/telas/Telas.kt`), e `F1-T07` é quem liga o
 * destino ao grafo.
 *
 * [Erro.mensagem] é um id de recurso, e não texto. Microcopy do app mora em
 * `strings.xml`, que é onde a varredura de travessão e emoji de `F1-T20` olha — uma
 * frase montada dentro do `ViewModel` escaparia dela em silêncio.
 */
sealed interface LoginUiState {

    /** Parado, esperando o toque. É também para onde volta quem fecha a folha de contas. */
    data object Ocioso : LoginUiState

    /** A folha do Google está aberta, ou a troca de token está em curso. */
    data object Entrando : LoginUiState

    data class Erro(@param:StringRes val mensagem: Int) : LoginUiState

    /**
     * Autenticado, e já se sabe para onde ir.
     *
     * [temPerfil] é a existência de `users/{uid}`, não um palpite: quem tem
     * documento vai para a `HomeScreen`, quem não tem vai para a
     * `OnboardingScreen` (docs/03 §3.1).
     */
    data class Autenticado(val uid: String, val temPerfil: Boolean) : LoginUiState
}
