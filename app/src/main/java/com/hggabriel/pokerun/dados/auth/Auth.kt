package com.hggabriel.pokerun.dados.auth

/*
 * A autenticação: Firebase Auth mais o Credential Manager, que é quem mostra a
 * folha de contas do Google (`F1-T06`, docs/03 §3.1).
 *
 * **É um pacote irmão de `dados/firestore`, e não uma parte dele.** Os dois falam
 * com o Firebase, mas com SDKs diferentes e sobre coisas diferentes: aqui vive
 * *quem é o usuário*, lá vive *o que ele tem*. Misturá-los faria o repositório de
 * planos carregar o Credential Manager junto.
 *
 * **O único lugar do app que sabe o que é um `idToken`.** A tela pede "entrar", o
 * `ViewModel` orquestra estado, e o vaivém de credencial não sai daqui — nem os
 * tipos de exceção do Credential Manager, que viram valores de `ResultadoDeEntrada`
 * antes de subir.
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
