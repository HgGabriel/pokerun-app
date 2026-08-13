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
 * RN-29 escreve o alfabeto literal `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`, diz que ele tem
 * **32 caracteres**, e na mesma frase afirma que exclui `0`, `O`, `1`, `I` e `L`. Os
 * três não fecham: a string tem 32 caracteres **com** o `L`, e sem ele teria 31. Dois
 * sinais contra um — a string e a contagem —, então o `L` fica. Está registrado na
 * tabela de decisões do `STATUS.md`.
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
    fun `o alfabeto tem 32 caracteres e nenhum repetido`() {
        assertEquals(32, ALFABETO_DO_CONVITE.length)
        assertEquals(32, ALFABETO_DO_CONVITE.toSet().size)
    }

    @Test
    fun `o alfabeto nao tem os caracteres que se confundem lidos em voz alta`() {
        // RN-29: sem `0`, `O`, `1` e `I`. O `L` fica — ver o KDoc da classe.
        listOf('0', 'O', '1', 'I').forEach { proibido ->
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
