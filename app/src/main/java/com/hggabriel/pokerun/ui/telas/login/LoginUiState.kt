package com.hggabriel.pokerun.ui.telas.login

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R

/**
 * O estado da `LoginScreen` (`F1-T06`, docs/03 §3.1).
 *
 * A ficha nomeia três estados — ocioso, carregando e erro —, `F1-T25` acrescentou
 * [Demorando], e aqui há mais um, [Autenticado], que é o **terminal**: a tela não muda
 * de aparência nele, ela sai.
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

    /**
     * A espera passou de [ESPERA_ATE_O_REPETIR_MS] sem resposta (`F1-T25`).
     *
     * **Não é erro, e o pedido continua vivo.** Se o Google responder agora, a entrada
     * segue normalmente; o que muda é que o botão volta a aceitar toque, como
     * `Tentar de novo`, para o caso em que ele não vai responder nunca.
     */
    data object Demorando : LoginUiState

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

/**
 * Quanto tempo de espera até o botão voltar como `Tentar de novo`, em milissegundos
 * (`F1-T25`, decisão nº 84).
 *
 * **A entrada não tem prazo**, e este número não é um. Nada é cancelado aos 15 s: o
 * pedido segue vivo e a resposta que chegar depois entra. O que muda é que a pessoa
 * ganha a saída, e é por isso que o número pode ser curto sem cortar ninguém — quem
 * ainda está escolhendo a conta tem a folha na frente e nem vê o botão.
 *
 * Os 15 s são do humano, e ficam acima dos **10 s** em que o próprio sistema foi visto
 * cancelando em 20/09: antes disso o botão disputaria com a resposta dele.
 */
const val ESPERA_ATE_O_REPETIR_MS = 15_000L

/** O que um toque no botão da `LoginScreen` faz (`F1-T25`). */
enum class ToqueNoBotao {
    /** Abre a folha de contas. */
    Abre,

    /** Nada: já há uma entrada em curso, ou a tela está saindo. */
    Ignora,

    /** Abandona o pedido pendurado e abre outra folha. */
    Reabre,
}

/**
 * O que o toque faz em cada estado. É também o que decide se o botão aceita toque.
 *
 * **Durante [LoginUiState.Entrando] o toque é ignorado** para o duplo toque não abrir
 * duas folhas. **Em [LoginUiState.Demorando] ele reabre**, e reabrir sem abandonar o
 * pedido anterior deixaria dois pedidos disputando a mesma folha — é o `ViewModel` quem
 * cancela, e é esta função que diz quando.
 */
fun toqueNoBotao(estado: LoginUiState): ToqueNoBotao = when (estado) {
    LoginUiState.Entrando, is LoginUiState.Autenticado -> ToqueNoBotao.Ignora
    LoginUiState.Demorando -> ToqueNoBotao.Reabre
    LoginUiState.Ocioso, is LoginUiState.Erro -> ToqueNoBotao.Abre
}

/**
 * O rótulo do botão, ou nulo quando ele mostra o indicador de progresso.
 *
 * **Em [LoginUiState.Erro] o rótulo não muda:** o mesmo botão é o repetir (docs/02 §8,
 * item 7), e o bloco de alerta embaixo já diz o que aconteceu. Em
 * [LoginUiState.Demorando] ele muda porque nada aconteceu ainda — o rótulo é a única
 * coisa que diz que tocar de novo faz outra coisa.
 */
@StringRes
fun rotuloDoBotao(estado: LoginUiState): Int? = when (estado) {
    LoginUiState.Entrando -> null
    LoginUiState.Demorando -> R.string.login_tentar_de_novo
    else -> R.string.login_entrar
}
