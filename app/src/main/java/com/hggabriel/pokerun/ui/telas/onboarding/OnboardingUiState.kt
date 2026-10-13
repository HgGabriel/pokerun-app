package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.annotation.StringRes

/**
 * O estado da `OnboardingScreen` (`F1-T08`, docs/03 §3.2).
 *
 * São três: [Perfil], [Salvando] e [Concluido]. O último é o terminal — a tela não muda
 * de aparência nele, ela sai —, e existe pelo mesmo motivo do
 * `LoginUiState.Autenticado`: o roteamento é decidido aqui e executado lá fora, porque
 * uma tela não conhece as rotas das outras.
 *
 * Toda mensagem é id de recurso, nunca texto: microcopy do app mora em `strings.xml`,
 * que é onde a varredura de travessão e emoji de `F1-T20` olha. Frase montada dentro do
 * `ViewModel` escapa dela em silêncio.
 */
sealed interface OnboardingUiState {

    /**
     * Nome e maior distância confortável (docs/01 §3.1).
     *
     * **Um passo, dois campos**, e não duas telas (decisão nº 9): nenhum dos dois campos
     * depende do outro.
     *
     * [distancia] é o texto cru do campo, não um número: `7,` é um estado legítimo de
     * digitação, e converter a cada tecla apagaria a vírgula debaixo do dedo. A leitura
     * é `distanciaEmKm`, no toque do botão.
     */
    data class Perfil(
        val nome: String = "",
        val distancia: String = "",
        @param:StringRes val erroNoNome: Int? = null,
        @param:StringRes val erroNaDistancia: Int? = null,
        @param:StringRes val erroAoGravar: Int? = null,
    ) : OnboardingUiState

    /**
     * Gravando no Firestore. O [passo] é o corpo que continua desenhado por baixo: o
     * indicador de progresso vive dentro do botão que o usuário tocou, e não numa tela
     * própria (docs/02 §8, item 6, e o mesmo raciocínio da `LoginScreen`).
     */
    data class Salvando(val passo: Perfil) : OnboardingUiState

    /** Terminal: o perfil existe e a tela sai. Ver [OnboardingUiState]. */
    data object Concluido : OnboardingUiState
}
