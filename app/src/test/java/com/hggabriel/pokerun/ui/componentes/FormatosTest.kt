package com.hggabriel.pokerun.ui.componentes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Duração e pace em texto (`F1-T15`).
 *
 * Existem testes porque são os dois números da tela que **ninguém confere de olho**: um
 * pace plausível e errado — truncado em vez de arredondado, ou com o segundo sem o zero à
 * esquerda — passa por revisão sem ninguém reparar, e depois vira comparação entre oito
 * pessoas.
 */
class FormatosTest {

    @Test
    fun `abaixo de uma hora a duracao nao mostra a hora`() {
        assertEquals("48:30", formatarDuracao(48 * 60 + 30L))
    }

    @Test
    fun `a partir de uma hora a duracao mostra as tres casas`() {
        assertEquals("1:04:22", formatarDuracao(3600 + 4 * 60 + 22L))
    }

    @Test
    fun `os campos internos tem sempre dois digitos`() {
        // `1:4:2` não é tempo, são três números.
        assertEquals("1:04:02", formatarDuracao(3600 + 4 * 60 + 2L))
        assertEquals("0:05", formatarDuracao(5L))
    }

    @Test
    fun `o pace arredonda para o segundo, e nao trunca`() {
        // 10 km em 51:46 dá 310,6 s/km: `5:11`, e não `5:10`.
        assertEquals("5:11", formatarPace(10.0, 51 * 60 + 46L))
    }

    @Test
    fun `o pace passa de uma hora por quilometro sem quebrar`() {
        assertEquals("61:40", formatarPace(1.0, 3700L))
    }

    @Test
    fun `sem distancia ou sem tempo nao existe pace`() {
        assertNull(formatarPace(0.0, 1800L))
        assertNull(formatarPace(5.0, 0L))
    }
}
