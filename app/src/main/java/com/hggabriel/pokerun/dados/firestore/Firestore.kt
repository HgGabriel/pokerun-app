package com.hggabriel.pokerun.dados.firestore

/*
 * Os repositórios e o mapeamento documento ↔ modelo (`F1-T05`, docs/05).
 *
 * É o único pacote que conhece o SDK do Firestore, e ele recebe a instância do
 * `AppContainer` — nada aqui chama `Firebase.firestore` por conta própria.
 *
 * Duas coisas que a especificação decide e que o código daqui não pode reabrir:
 * caminho de documento tem número par de segmentos, então os agregados vivem em
 * `users/{uid}/agregados/{doc}` e `users/{uid}/temporadas/{id}` (docs/05 §3); e
 * corrida não se sobrescreve — correção grava registro novo com `substitui_run_id`
 * (RN-24).
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
