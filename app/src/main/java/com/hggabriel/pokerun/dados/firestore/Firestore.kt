package com.hggabriel.pokerun.dados.firestore

/*
 * Os repositórios e o mapeamento documento ↔ modelo (`F1-T05`, docs/05 §1).
 *
 * É o único pacote que conhece o SDK do Firestore, e ele recebe a instância do
 * `AppContainer` — nada aqui chama `Firebase.firestore` por conta própria.
 *
 * Duas coisas que a especificação decide e que o código daqui não pode reabrir:
 * caminho de documento tem número par de segmentos, então os agregados vivem em
 * `users/{uid}/agregados/{doc}` e `users/{uid}/temporadas/{id}` (docs/05 §1); e
 * corrida não se sobrescreve — correção grava registro novo com `substitui_run_id`
 * (RN-24).
 *
 * **Um arquivo por agregado**, como em `dominio/modelo`: `PlanoRepositorio` cobre
 * `plans` com as subcoleções `weeks` e `members`, `UsuarioRepositorio` cobre
 * `users/{uid}` e `CorridaRepositorio` cobre `users/{uid}/runs`. As conversões que
 * não pertencem a nenhum deles — instante, fuso, vocabulário fechado e a ponte de
 * listener para `Flow` — ficam em `Documentos.kt`.
 *
 * **O que ainda não tem repositório, e de quem é:** `weekly`, `agregados/records` e
 * o progresso sazonal de `users/{uid}/temporadas/{id}` são `F2-T08`, o documento de
 * `invites/` é `F1-T14` e o desafio coletivo é `F2-T13`.
 *
 * Este arquivo não declara nada: ele é o mapa do pacote.
 */
