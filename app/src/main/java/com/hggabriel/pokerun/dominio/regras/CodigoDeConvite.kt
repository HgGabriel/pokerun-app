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
 * já existe falha sozinho, e a colisão dispara um novo sorteio. Quem faz essa reserva é
 * `ConviteRepositorio.reservarCodigo` (`F1-T14`), e é ela que chama esta função em laço
 * — aqui só nasce o valor.
 *
 * **Plano criado antes de `F1-T14` tem código sem documento em `invites/`**, e o código
 * dele não resolve. Nenhum backfill foi escrito: a conta tinha um plano de teste
 * encerrado quando a coleção entrou, e entrar em plano encerrado é justamente o que
 * RN-27 impede. Está registrado no `STATUS.md`.
 *
 * @param aleatorio a fonte de aleatoriedade, por parâmetro para o teste fixá-la — e para
 *   a reserva repetir o sorteio depois de uma colisão sem construir outra fonte.
 */
fun sortearCodigoDeConvite(aleatorio: Random = Random.Default): String =
    buildString(TAMANHO_DO_CODIGO) {
        repeat(TAMANHO_DO_CODIGO) {
            append(ALFABETO_DO_CONVITE[aleatorio.nextInt(ALFABETO_DO_CONVITE.length)])
        }
    }

/**
 * // RN-29
 *
 * A outra ponta da regra: o código **chega digitado**. Alguém lê `FYQJE6` em voz alta ou
 * cola de uma mensagem, e o outro lado teclea — com a caixa que o teclado deu, com o
 * espaço que veio junto e às vezes com o hífen que ninguém pediu.
 *
 * Três coisas acontecem aqui, e cada uma evita uma mensagem de erro que seria culpa do
 * app: a caixa sobe, o que não é do alfabeto some, e o resto é cortado em seis.
 *
 * **Os cinco caracteres ambíguos somem em vez de virar erro.** `0`, `O`, `1`, `I` e `L`
 * foram excluídos do alfabeto exatamente porque se confundem lidos em voz alta, então
 * **nenhum código válido os contém**: quem digitou `O` errou um `0` que também não
 * existe. Deixá-los no campo só adiaria a recusa para depois do sexto caractere.
 *
 * A alternativa seria traduzi-los para o par visual (`O` vira `0`), e ela não existe:
 * os pares também estão fora do alfabeto.
 */
fun normalizarCodigo(texto: String): String =
    texto.uppercase()
        .filter { it in ALFABETO_DO_CONVITE }
        .take(TAMANHO_DO_CODIGO)

/**
 * // RN-29
 *
 * Se o texto já tem os seis caracteres de um código. É o que habilita a busca: procurar
 * com quatro caracteres seria uma leitura faturada garantidamente vazia.
 *
 * Só o comprimento é conferido porque [normalizarCodigo] já garante o alfabeto — o campo
 * não tem como conter outra coisa.
 */
fun codigoCompleto(codigo: String): Boolean = codigo.length == TAMANHO_DO_CODIGO
