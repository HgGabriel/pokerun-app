package com.hggabriel.pokerun.dados.local

/*
 * O dado que viaja dentro do APK: o dataset de espécies da temporada (`F2-T14`, `F4-T02`,
 * docs/07).
 *
 * **Nome e sprite vêm da PokéAPI em runtime, com cache em disco** (D-10, revista pela
 * decisão nº 102). Tier e curva de custo são dado local e curado (docs/07): o `speed` da
 * API contradiz a curadoria.
 *
 * Nenhuma contagem de espécies é escrita aqui: ela vem de `temporada.total_especies`
 * (RN-43), porque a segunda temporada tem outro número.
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
