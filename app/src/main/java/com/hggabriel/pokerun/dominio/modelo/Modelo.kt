package com.hggabriel.pokerun.dominio.modelo

/*
 * Os modelos de domínio: `Plano`, `Semana`, `Corrida`, `Usuario`, `Membro`,
 * `Temporada` e `Tier` (`F1-T01`, docs/05 §1 — o esquema é a §1; a §3 são as
 * Security Rules, e a citação errada veio de `F0-T04`).
 *
 * Eles não sabem que existe Firestore. O mapeamento de e para documento é
 * `dados/firestore`, e é de propósito que a fronteira fique lá: um modelo com
 * `@DocumentId` dentro obriga toda regra pura a carregar o SDK junto.
 *
 * Duas armadilhas que a especificação já nomeia e que aparecem aqui primeiro:
 * a contagem de espécies vem de `temporada.total_especies` e nunca é literal
 * (RN-43), e corrida é append-only — o único campo mutável é `substituida`
 * (RN-24).
 *
 * **O que ainda não mora aqui, e de quem é:** o progresso sazonal de
 * `users/{uid}/temporadas/{id}` e os agregados `weekly` e `records` são `F2-T08`,
 * o documento de `invites/` é `F1-T14` e o desafio coletivo é `F2-T13`. Os
 * repositórios de `F1-T05` cobrem planos, semanas, membros, usuário e corridas, que
 * são exatamente os cinco primeiros daqui.
 *
 * Este arquivo não declara nada: ele é o mapa do pacote, e os modelos ficam um por
 * agregado nos arquivos ao lado.
 */
