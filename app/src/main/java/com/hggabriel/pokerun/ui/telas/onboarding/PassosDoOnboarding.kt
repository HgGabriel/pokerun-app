package com.hggabriel.pokerun.ui.telas.onboarding

import com.hggabriel.pokerun.dados.healthconnect.StatusDoHealthConnect

/*
 * A ordem rígida do onboarding e a leitura dos dois campos do perfil (`F1-T08`,
 * docs/03 §3.2).
 *
 * Estão fora do `ViewModel` porque são as duas únicas partes da tela que se provam sem
 * aparelho, sem Firestore e sem Health Connect — e são justamente as que a revisão de
 * olho deixa passar. `PassosDoOnboardingTest` roda os dois.
 */

/**
 * O passo depois de gravar o perfil. **A ordem é rígida** (`EXECUCAO.md §8`, item 9).
 *
 * Não dá para listar quem grava no Health Connect antes de pedir permissão e ler dele:
 * pular o passo 3 leva ao passo 5 com lista vazia, e essa lista vazia é indistinguível
 * do aparelho que de fato não tem treino nenhum.
 *
 * **Sem Health Connect utilizável, os passos 3 a 5 somem e o cadastro acaba** — sem
 * tela de erro, porque indisponível é o modo manual e não uma falha (docs/05 §4.4).
 *
 * @param permissaoConcedida se `READ_EXERCISE` já está concedida. Segunda passada pelo
 *   onboarding depois de uma reinstalação cai aqui, e reabrir a folha de permissão não
 *   pediria nada a ninguém.
 */
internal fun passoDepoisDoPerfil(
    saude: StatusDoHealthConnect,
    permissaoConcedida: Boolean,
): OnboardingUiState = when {
    saude != StatusDoHealthConnect.Disponivel -> OnboardingUiState.Concluido
    permissaoConcedida -> OnboardingUiState.LendoOrigens
    else -> OnboardingUiState.SolicitandoPermissao()
}

/**
 * O passo depois da folha de permissão do Health Connect.
 *
 * Negada **volta ao passo 3**, com o caminho de saída aparecendo junto. O que não pode
 * acontecer é seguir para `EscolhendoFonte`: a tela diria "nenhuma corrida encontrada"
 * sem ninguém ter procurado.
 */
internal fun passoDepoisDaPermissao(concedida: Boolean): OnboardingUiState =
    if (concedida) {
        OnboardingUiState.LendoOrigens
    } else {
        OnboardingUiState.SolicitandoPermissao(negada = true)
    }

/** O nome do passo 1, sem espaço sobrando. Nulo é "o usuário não respondeu". */
internal fun nomeDoPerfil(texto: String): String? = texto.trim().ifBlank { null }
