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

/**
 * A distância confortável do passo 2, em quilômetros (docs/01 §3.1).
 *
 * **Vírgula e ponto valem o mesmo**: o teclado decimal de um aparelho em pt-BR entrega
 * vírgula, e recusá-la é recusar o que o aparelho digita.
 *
 * A forma é conferida por [FORMA] antes de qualquer conversão, e não por
 * `toDoubleOrNull` sozinho, que aceita `1e3`, `Infinity` e `NaN` sem reclamar. O
 * teclado decimal não digita nenhum dos três, mas colar passa por cima do teclado — e
 * uma `baseline_km` de 1.000 gera as 21 semanas inteiras erradas, em silêncio.
 *
 * Zero também não passa: o gerador interpola **de** `baseline_km` até o alvo
 * (docs/01 §3.2), e partir de zero desfigura a grade toda.
 */
internal fun distanciaEmKm(texto: String): Double? {
    val limpo = texto.trim()
    if (!FORMA.matches(limpo)) return null
    val km = limpo.replace(',', '.').toDoubleOrNull() ?: return null
    return km.takeIf { it > 0.0 }
}

/**
 * Até três dígitos e até duas casas decimais.
 *
 * O teto de três dígitos não é validação de negócio inventada: é o que separa distância
 * de dedo escorregado. Nenhum ser humano responde 1.000 km à pergunta "qual a maior
 * distância que você corre hoje", e a São Silvestre tem 15.
 */
private val FORMA = Regex("""\d{1,3}([.,]\d{1,2})?""")
