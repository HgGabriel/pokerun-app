package com.hggabriel.pokerun.dominio.regras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * O sorteio do código de convite (`F1-T11`, RN-29), escrito antes da implementação.
 *
 * **A unicidade não é testada aqui, e é de propósito:** ela é estrutural e vive no
 * servidor — `invites/{codigo}` tem o código como ID do documento, e um `create` sobre
 * ID existente falha sozinho. A reserva transacional é `F1-T14`. O que se prova aqui é
 * a **forma**: seis caracteres do alfabeto de RN-29, sorteados de verdade.
 *
 * ### Sobre o `L`
 *
 * A primeira versão de RN-29 se contradizia: listava uma string de 32 caracteres que
 * **continha** o `L`, dizia ter 32 caracteres, e ao mesmo tempo afirmava excluir o `L`.
 * O humano decidiu em 13/08 que a lista de exclusão era a parte certa — **o `L` saiu**,
 * e a regra passou a dizer 31 (decisão nº 29 do `STATUS.md`).
 *
 * O teste abaixo é o que impede a volta: ele exige os cinco excluídos ausentes **e** a
 * contagem em 31, que é a conta que a versão contraditória não fechava.
 */
class CodigoDeConviteTest {

    @Test
    fun `o codigo tem seis caracteres`() {
        assertEquals(6, sortearCodigoDeConvite(Random(7)).length)
    }

    @Test
    fun `todo caractere do codigo vem do alfabeto`() {
        val aleatorio = Random(7)
        repeat(200) {
            sortearCodigoDeConvite(aleatorio).forEach { caractere ->
                assertTrue(
                    "'$caractere' não está no alfabeto de RN-29",
                    caractere in ALFABETO_DO_CONVITE,
                )
            }
        }
    }

    @Test
    fun `o alfabeto tem 31 caracteres e nenhum repetido`() {
        assertEquals(31, ALFABETO_DO_CONVITE.length)
        assertEquals(31, ALFABETO_DO_CONVITE.toSet().size)
    }

    @Test
    fun `o alfabeto nao tem os caracteres que se confundem lidos em voz alta`() {
        // Os cinco de RN-29, o `L` inclusive. Ver o KDoc da classe.
        listOf('0', 'O', '1', 'I', 'L').forEach { proibido ->
            assertFalse("'$proibido' não pode estar no alfabeto", proibido in ALFABETO_DO_CONVITE)
        }
    }

    @Test
    fun `o alfabeto e todo em caixa alta`() {
        // O código é digitado num campo e lido em voz alta: caixa mista pediria ao
        // usuário decidir uma coisa a mais.
        assertEquals(ALFABETO_DO_CONVITE.uppercase(), ALFABETO_DO_CONVITE)
    }

    @Test
    fun `a mesma semente devolve o mesmo codigo`() {
        // Prova que a fonte de aleatoriedade é a que entra por parâmetro. `F1-T14`
        // depende disso para repetir o sorteio depois de uma colisão.
        assertEquals(sortearCodigoDeConvite(Random(42)), sortearCodigoDeConvite(Random(42)))
    }

    @Test
    fun `cem sorteios seguidos nao se repetem`() {
        val aleatorio = Random(42)
        val codigos = List(100) { sortearCodigoDeConvite(aleatorio) }

        assertEquals(100, codigos.toSet().size)
    }
}
