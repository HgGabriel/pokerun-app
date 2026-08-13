package com.hggabriel.pokerun.ui.telas.onboarding

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dados.healthconnect.OrigemDeTreino

/**
 * O estado da `OnboardingScreen` (`F1-T08`, docs/03 §3.2).
 *
 * Os cinco da ficha estão aqui com os nomes dela: [Perfil], [SolicitandoPermissao],
 * [LendoOrigens], [EscolhendoFonte] e [Salvando]. O sexto, [Concluido], é o terminal —
 * a tela não muda de aparência nele, ela sai —, e existe pelo mesmo motivo do
 * `LoginUiState.Autenticado`: o roteamento é decidido aqui e executado lá fora, porque
 * uma tela não conhece as rotas das outras.
 *
 * **A ordem entre eles é rígida** (docs/01, `EXECUCAO.md §8` item 9), e quem a executa
 * é `PassosDoOnboarding.kt` — funções puras, com teste.
 *
 * Toda mensagem é id de recurso, nunca texto: microcopy do app mora em `strings.xml`,
 * que é onde a varredura de travessão e emoji de `F1-T20` olha. Frase montada dentro do
 * `ViewModel` escapa dela em silêncio.
 */
sealed interface OnboardingUiState {

    /**
     * Os dois passos que terminam numa escrita no Firestore.
     *
     * Existe para que [Salvando] possa guardar **qual** passo está esperando o
     * servidor, sem admitir `Salvando(Salvando(...))`. O indicador de progresso vive
     * dentro do botão que o usuário tocou, e não numa tela própria (docs/02 §8, item 6,
     * e o mesmo raciocínio da `LoginScreen`): para desenhar o botão, a tela precisa do
     * corpo que está por baixo dele.
     */
    sealed interface PassoQueGrava : OnboardingUiState

    /**
     * Passos 1 e 2: nome e maior distância confortável (docs/01 §3.1).
     *
     * **Um passo, dois campos**, e não duas telas. A ficha lista cinco estados para
     * cinco passos e dá um único [Perfil] aos dois primeiros; a ordem que importa é a
     * do Health Connect, porque nenhum dos dois campos depende do outro.
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
    ) : PassoQueGrava

    /**
     * Passo 3: a permissão do Health Connect.
     *
     * [negada] não é erro: é a resposta do usuário, e o app segue inteiro sem ela, em
     * modo manual. Ela existe porque o Android bloqueia permissão de saúde depois de
     * duas negativas e a folha para de abrir — sem um caminho de saída visível, o botão
     * desta tela deixaria de fazer qualquer coisa e o cadastro travaria aqui.
     */
    data class SolicitandoPermissao(val negada: Boolean = false) : OnboardingUiState

    /** Passo 4: lendo os últimos 30 dias para descobrir quem grava treino. */
    data object LendoOrigens : OnboardingUiState

    /**
     * Passo 5: escolher a fonte canônica, com a contagem de cada origem (RN-22).
     *
     * **Lista vazia não é falha** (docs/03 §3.2): é o aparelho que não tem treino
     * gravado, e a saída é `Continuar sem sincronização`. [falhouALeitura] separa esse
     * vazio legítimo do vazio que veio de exceção, que é o único dos dois que merece
     * "tentar de novo" (docs/02 §8, item 7).
     */
    data class EscolhendoFonte(
        val origens: List<OrigemDeTreino> = emptyList(),
        val escolhida: String? = null,
        val falhouALeitura: Boolean = false,
        @param:StringRes val erroAoGravar: Int? = null,
    ) : PassoQueGrava

    /** Gravando no Firestore. O [passo] é o corpo que continua desenhado por baixo. */
    data class Salvando(val passo: PassoQueGrava) : OnboardingUiState

    /** Terminal: o perfil existe e a tela sai. Ver [OnboardingUiState]. */
    data object Concluido : OnboardingUiState
}
