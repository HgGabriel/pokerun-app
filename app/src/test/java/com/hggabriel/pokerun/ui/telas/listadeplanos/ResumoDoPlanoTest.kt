package com.hggabriel.pokerun.ui.telas.listadeplanos

import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * O resumo congelado do plano encerrado (D-05, docs/03 §3.4), que paga a dívida nº 37
 * com os agregados de `F2-T08`.
 */
class ResumoDoPlanoTest {

    @Test
    fun `soma o km e as sessoes do plano`() {
        val resumo = resumoDoPlano(mapOf(1 to semanal(km = 10.0, feitas = 3), 2 to semanal(km = 12.5, feitas = 2)), grade, 1)

        assertEquals(22.5, resumo.kmTotal, 1e-9)
        assertEquals(5, resumo.sessoesFeitas)
        assertEquals(9, resumo.sessoesPrevistas)
    }

    @Test
    fun `semana sem weekly entra nas previstas com zero feitas`() {
        val resumo = resumoDoPlano(mapOf(1 to semanal(km = 10.0, feitas = 3)), grade, 1)

        assertEquals(3, resumo.sessoesFeitas)
        assertEquals(9, resumo.sessoesPrevistas)
    }

    @Test
    fun `a aderencia comeca na semana em que o membro entrou`() {
        // RN-19: quem entrou na semana 2 não carrega a semana 1.
        val resumo = resumoDoPlano(mapOf(1 to semanal(km = 4.0, feitas = 1), 2 to semanal(km = 9.0, feitas = 3)), grade, 2)

        assertEquals(3, resumo.sessoesFeitas)
        assertEquals(6, resumo.sessoesPrevistas)
    }

    private val inicio = Instant.parse("2026-08-10T03:00:00Z")

    private val grade = (1..3).map { n ->
        Semana(
            numero = n,
            dataInicio = inicio.plusSeconds((n - 1) * 7 * 86_400L),
            dataFim = inicio.plusSeconds(n * 7 * 86_400L),
            sessoesAlvo = 3,
            kmAlvo = 20.0,
            longaoKm = 10.0,
            tipo = TipoDeSemana.BUILD,
            parcial = false,
        )
    }

    private fun semanal(km: Double, feitas: Int) = AgregadoDaSemana(
        planoId = "p1",
        xpSemana = 0L,
        sessoesFeitas = feitas,
        kmFeito = km,
        longaoFeito = 0.0,
        aderenciaPct = 0.0,
        fechada = true,
    )
}
