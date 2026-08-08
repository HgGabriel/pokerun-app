package com.hggabriel.pokerun.ui.telas.dump

/**
 * O estado da tela de dump (`F0-T09`). **Descartável**: some junto com a pasta
 * quando `F0-T10` estiver preenchida.
 *
 * A tela é um instrumento de medição, então o estado é achatado de propósito: uma
 * `sealed interface` de fases esconderia justamente o que interessa olhar ao mesmo
 * tempo, que é o status do SDK ao lado do que já deu para ler.
 */
data class DumpUiState(
    val status: StatusDoSdk = StatusDoSdk.NaoConsultado,
    val permissoesFaltando: Set<String> = emptySet(),
    val lendo: Boolean = false,
    val relatorio: String = "",
    val falha: String? = null,
) {
    val temPermissao: Boolean get() = permissoesFaltando.isEmpty()
}

/**
 * O `getSdkStatus()` de `docs/05 §4.4`, nomeado.
 *
 * **`Indisponivel` não é erro** e é por isso que ele é um valor e não uma exceção:
 * é o modo manual do app, um caminho previsto. Aqui ele só impede o dump, que é a
 * única tela do projeto que não tem o que fazer sem o Health Connect.
 */
enum class StatusDoSdk {
    NaoConsultado,
    Indisponivel,
    PrecisaAtualizar,
    Disponivel,
}
