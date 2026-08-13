package com.hggabriel.pokerun.dominio.regras

import kotlin.random.Random

/**
 * O alfabeto do código de convite (RN-29): 31 caracteres, todos em caixa alta.
 *
 * ### Por que 31, e não 32
 *
 * A primeira versão de RN-29 se contradizia numa frase só: listava a string
 * `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`, afirmava que ela tinha **32 caracteres** e dizia
 * excluir `0`, `O`, `1`, `I` e `L` — só que o `L` estava dentro da string, e é ele que
 * fazia a contagem chegar a 32.
 *
 * **O humano decidiu em 13/08 que o `L` sai** (decisão nº 29 do `STATUS.md`), e RN-29 foi
 * corrigida: a string perdeu o `L` e a contagem virou 31. A lista de exclusão era a parte
 * certa da regra; a string e o número eram os errados.
 *
 * O que a restrição protege: o código é **lido em voz alta** e digitado por outra pessoa,
 * então nada que se confunda entra — nem no papel (`1`/`I`/`L`, `0`/`O`) nem na fala.
 */
const val ALFABETO_DO_CONVITE = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

/** Seis caracteres é o comprimento de RN-29. 31⁶ dá 888 milhões de códigos. */
private const val TAMANHO_DO_CODIGO = 6

/**
 * // RN-29
 *
 * Sorteia um código de convite.
 *
 * **Sortear não é reservar.** A unicidade de RN-29 é estrutural e mora no servidor: o
 * código é o **ID do documento** em `invites/{codigo}`, então um `create` sobre um ID que
 * já existe falha sozinho, e a colisão dispara um novo sorteio. Essa reserva transacional
 * é `F1-T14`, dona da coleção — aqui só nasce o valor que `F1-T11` grava em
 * `plans/{id}.codigo_convite`.
 *
 * Consequência que vale saber enquanto `F1-T14` não chega: os planos criados agora têm
 * código, mas **não têm documento em `invites/`**, então ninguém entra neles por código
 * ainda. Está registrado no `STATUS.md`.
 *
 * @param aleatorio a fonte de aleatoriedade, por parâmetro para o teste fixá-la — e para
 *   `F1-T14` repetir o sorteio depois de uma colisão sem construir outra fonte.
 */
fun sortearCodigoDeConvite(aleatorio: Random = Random.Default): String =
    buildString(TAMANHO_DO_CODIGO) {
        repeat(TAMANHO_DO_CODIGO) {
            append(ALFABETO_DO_CONVITE[aleatorio.nextInt(ALFABETO_DO_CONVITE.length)])
        }
    }
