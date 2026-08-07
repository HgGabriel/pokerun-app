package com.hggabriel.pokerun.ui.telas

/*
 * Uma pasta por tela, e dentro dela os três arquivos que a compõem: o `Screen`
 * (Compose, sem lógica), o `ViewModel` e o `UiState`. A arquitetura é ViewModel +
 * StateFlow + fluxo unidirecional (docs/README, docs/03 §1).
 *
 * Estas telas não são livres para inventar aparência: superfície é `Ficha`, nunca
 * `Card` (docs/02 §4.3), cor é `MaterialTheme.colorScheme.*` ou
 * `LocalCoresPokerun.current.*`, nunca um `val` importado de `ui.theme` — e há
 * teste que falha nisso (`TemaTest`, `F0-T13`).
 *
 * A primeira tela é `F1-T06`. O grafo de navegação que as liga é `F1-T07`, e ele
 * mora fora daqui: uma tela não conhece as rotas das outras.
 *
 * Este arquivo não declara nada. Ele existe porque um pacote vazio não sobrevive a
 * um clone — o git não versiona diretório — e porque a especificação não está no
 * repositório (`EXECUCAO.md §5`), então o mapa da arquitetura só existe aqui.
 */
