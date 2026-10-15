package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * A ordem da abertura (`F4-T09`, docs/01 §4.6, docs/03 §3.20 e §3.20.1): só se apresenta
 * semana fechada que ainda não foi apresentada, e a estreia parte do `#001`.
 */
class FechamentoDaSemanaTest {

    @Test
    fun `sem semana fechada nao ha o que apresentar`() {
        assertNull(semanaAApresentar(emptyList(), semanaApresentada = null))
    }

    @Test
    fun `a ultima semana ja apresentada nao reabre a tela`() {
        val historico = listOf(MarcoDaSemana("p_1", 3), MarcoDaSemana("p_2", 8))

        assertNull(semanaAApresentar(historico, semanaApresentada = "p_2"))
    }

    @Test
    fun `a semana nova parte da posicao da ultima apresentada`() {
        val historico = listOf(MarcoDaSemana("p_1", 3), MarcoDaSemana("p_2", 8), MarcoDaSemana("p_3", 11))

        val semana = semanaAApresentar(historico, semanaApresentada = "p_2")!!

        assertEquals("p_3", semana.chave)
        assertEquals("p", semana.planoId)
        assertEquals(3, semana.numero)
        assertEquals(8, semana.posicaoAntes)
        assertEquals(11, semana.posicaoDepois)
        assertFalse(semana.estreia)
    }

    @Test
    fun `a estreia percorre do 001 ate a posicao acumulada`() {
        val historico = listOf(MarcoDaSemana("p_1", 3), MarcoDaSemana("p_2", 8), MarcoDaSemana("p_3", 11))

        val semana = semanaAApresentar(historico, semanaApresentada = null)!!

        assertTrue(semana.estreia)
        assertEquals(1, semana.posicaoAntes)
        assertEquals(11, semana.posicaoDepois)
        assertEquals(3, semana.numero)
    }

    @Test
    fun `varias semanas sem abrir o app viram um fechamento com o acumulado`() {
        val historico = (1..5).map { MarcoDaSemana("p_$it", it * 4) }

        val semana = semanaAApresentar(historico, semanaApresentada = "p_2")!!

        assertEquals(5, semana.numero)
        assertEquals(8, semana.posicaoAntes)
        assertEquals(20, semana.posicaoDepois)
    }

    @Test
    fun `semana apresentada que saiu do historico conta como estreia sem a marca`() {
        val historico = listOf(MarcoDaSemana("p_4", 9))

        val semana = semanaAApresentar(historico, semanaApresentada = "outro_2")!!

        assertEquals(1, semana.posicaoAntes)
        assertFalse(semana.estreia)
    }

    @Test
    fun `a posicao pode ter recuado e o fechamento a mostra`() {
        val historico = listOf(MarcoDaSemana("p_1", 120), MarcoDaSemana("p_2", 116))

        val semana = semanaAApresentar(historico, semanaApresentada = "p_1")!!

        assertTrue(semana.posicaoDepois < semana.posicaoAntes)
    }

    @Test
    fun `a quebra do XP aplica o multiplicador a volume, longao e pace e soma o recorde depois`() {
        val corridas = listOf(
            corrida("a", ParcelasDeXp(base = 500.0, longao = 100.0, pace = 200, multiplicador = 2.0, recorde = 500)),
            corrida("b", ParcelasDeXp(base = 300.0, longao = 0.0, pace = 0, multiplicador = 1.5, recorde = 0)),
            corrida("c", null),
            corrida("d", ParcelasDeXp(1000.0, 0.0, 0, 2.0, 0)).copy(descartada = true),
        )

        val quebra = quebraDoXp(corridas)

        // a: 500 × 2,0 = 1.000; b: 300 × 1,5 = 450.
        assertEquals(1450L, quebra.volume)
        assertEquals(200L, quebra.longao)
        assertEquals(400L, quebra.pace)
        assertEquals(500L, quebra.recordes)
        assertEquals(2550L, quebra.total)
    }

    private fun corrida(id: String, parcelas: ParcelasDeXp?) = Corrida(
        id = id,
        dataHoraInicio = Instant.parse("2026-09-01T10:00:00Z"),
        km = 5.0,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = 1,
        temporadaId = "t",
        xpDetalhe = parcelas,
    )
}
