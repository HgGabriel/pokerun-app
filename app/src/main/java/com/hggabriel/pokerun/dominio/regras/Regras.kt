package com.hggabriel.pokerun.dominio.regras

/*
 * As funções puras que implementam as regras: `GeradorDePlano`,
 * `CalendarioDoPlano`, `CalculoDeAderencia` (`F1-T02` a `F1-T04`), `MotorDeXp` e
 * o replay (`F2-T06`, `F2-T07`).
 *
 * **Este pacote é a mitigação do risco de maior probabilidade do projeto:** código
 * plausível e errado. Nada aqui conhece Android nem Firestore, então tudo aqui roda
 * em JVM pura, e toda função destas nasce de um teste escrito antes dela
 * (`EXECUCAO.md §3.2`) com os casos de docs/06 §3.
 *
 * A âncora é obrigatória: a primeira linha da função carrega o ID da regra que ela
 * implementa — `// RN-08`, `// XP-06`. É o que dá nome ao teste e o que torna a
 * revisão verificável.
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
