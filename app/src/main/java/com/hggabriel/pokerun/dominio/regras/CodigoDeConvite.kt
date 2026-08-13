package com.hggabriel.pokerun.dominio.regras

import kotlin.random.Random

/**
 * O alfabeto do código de convite (RN-29): 32 caracteres, todos em caixa alta.
 *
 * ### O `L` fica, e é decisão registrada
 *
 * RN-29 diz três coisas na mesma frase e as três não fecham: que o alfabeto é
 * `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`, que ele tem **32 caracteres**, e que exclui `0`,
 * `O`, `1`, `I` e `L`. A string literal tem 32 caracteres **com** o `L`; tirando-o,
 * sobram 31 e a contagem deixa de bater. Dois sinais contra um, então vale a string.
 *
 * O motivo declarado da restrição também sobrevive: o código é **lido em voz alta**, e
 * ali o que se confunde é `0`/`O` e `1`/`I` — o `L` falado não colide com nada. A
 * decisão está na tabela do `STATUS.md`, à espera de revisão.
 */
const val ALFABETO_DO_CONVITE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

/** Seis caracteres é o comprimento de RN-29. 32⁶ dá 1,07 bilhão de códigos. */
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
