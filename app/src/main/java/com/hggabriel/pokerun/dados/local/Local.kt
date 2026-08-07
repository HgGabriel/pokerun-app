package com.hggabriel.pokerun.dados.local

/*
 * O dado que viaja dentro do APK: o dataset de espécies da temporada e o mapa para
 * os sprites em `assets` (`F2-T14`, `F4-T02`, docs/07).
 *
 * **A PokéAPI não é chamada em runtime** (D-10). As espécies são dado local e os
 * sprites são assets — um app de corrida não pede rede para desenhar a criatura que
 * o usuário já conquistou.
 *
 * Nenhuma contagem de espécies é escrita aqui: ela vem de `temporada.total_especies`
 * (RN-43), porque a segunda temporada tem outro número.
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
