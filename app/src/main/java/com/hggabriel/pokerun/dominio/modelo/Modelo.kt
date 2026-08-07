package com.hggabriel.pokerun.dominio.modelo

/*
 * Os modelos de domínio: `Plano`, `Semana`, `Corrida`, `Usuario`, `Membro`,
 * `Temporada` e `Tier` (`F1-T01`, docs/05 §3).
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
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
