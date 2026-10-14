package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * O desafio coletivo da semana e as Insígnias de Ginásio (`F2-T13`, docs/03 §3.16,
 * decisões nº 92, 109 e 129), escritos antes da implementação. O desafio não dá XP:
 * só corrida gera XP.
 */
class DesafioColetivoTest {

    @Test
    fun `a meta e o km planejado da semana vezes os membros ativos`() {
        val desafio = DesafioColetivo.atualizar(null, semana(kmAlvo = 20.0), membrosAtivos = 3, semanais = emptyList(), concluidosAntes = 0)

        assertEquals("km_grupo", desafio.tipo)
        assertEquals(60.0, desafio.meta, 1e-9)
    }

    @Test
    fun `a meta arredonda para cima ao km inteiro`() {
        val desafio = DesafioColetivo.atualizar(null, semana(kmAlvo = 13.3), membrosAtivos = 2, semanais = emptyList(), concluidosAntes = 0)

        assertEquals(27.0, desafio.meta, 1e-9)
    }

    @Test
    fun `o acumulado soma o km de todos, e quem nao correu soma zero`() {
        val desafio = DesafioColetivo.atualizar(
            null, semana(), membrosAtivos = 3, semanais = listOf(semanal(12.5), null, semanal(7.0)), concluidosAntes = 0,
        )

        assertEquals(19.5, desafio.acumulado, 1e-9)
        assertFalse(desafio.concluido)
        assertNull(desafio.insigniaId)
    }

    @Test
    fun `bater a meta conclui e libera a proxima insignia de Kanto`() {
        val desafio = DesafioColetivo.atualizar(
            null, semana(kmAlvo = 20.0), membrosAtivos = 2, semanais = listOf(semanal(22.0), semanal(18.0)), concluidosAntes = 2,
        )

        assertTrue(desafio.concluido)
        assertEquals(DesafioColetivo.INSIGNIAS[2], desafio.insigniaId)
    }

    @Test
    fun `a meta fica a da criacao, mesmo que entre gente no meio da semana`() {
        val existente = Desafio("km_grupo", 40.0, 10.0, false, null)

        val desafio = DesafioColetivo.atualizar(existente, semana(kmAlvo = 20.0), membrosAtivos = 3, semanais = listOf(semanal(41.0)), concluidosAntes = 0)

        assertEquals(40.0, desafio.meta, 1e-9)
        assertTrue(desafio.concluido)
    }

    @Test
    fun `insignia conquistada nao volta atras quando uma correcao baixa o acumulado`() {
        val existente = Desafio("km_grupo", 40.0, 42.0, true, DesafioColetivo.INSIGNIAS[0])

        val desafio = DesafioColetivo.atualizar(existente, semana(), membrosAtivos = 2, semanais = listOf(semanal(30.0)), concluidosAntes = 0)

        assertEquals(30.0, desafio.acumulado, 1e-9)
        assertTrue(desafio.concluido)
        assertEquals(DesafioColetivo.INSIGNIAS[0], desafio.insigniaId)
    }

    @Test
    fun `depois da oitava insignia o desafio conclui sem insignia nova`() {
        val desafio = DesafioColetivo.atualizar(
            null, semana(kmAlvo = 10.0), membrosAtivos = 1, semanais = listOf(semanal(11.0)), concluidosAntes = DesafioColetivo.INSIGNIAS.size,
        )

        assertTrue(desafio.concluido)
        assertNull(desafio.insigniaId)
    }

    @Test
    fun `as insignias conquistadas saem na ordem dos ginasios`() {
        val i = DesafioColetivo.INSIGNIAS
        val desafios = listOf(
            Desafio("km_grupo", 1.0, 1.0, true, i[1]),
            Desafio("km_grupo", 1.0, 0.0, false, null),
            Desafio("km_grupo", 1.0, 1.0, true, i[0]),
        )

        assertEquals(listOf(i[0], i[1]), DesafioColetivo.conquistadas(desafios))
        assertEquals(8, i.size)
    }

    private fun semana(kmAlvo: Double = 20.0) = Semana(
        numero = 1,
        dataInicio = Instant.parse("2026-08-10T03:00:00Z"),
        dataFim = Instant.parse("2026-08-17T03:00:00Z"),
        sessoesAlvo = 3,
        kmAlvo = kmAlvo,
        longaoKm = 10.0,
        tipo = TipoDeSemana.BUILD,
        parcial = false,
    )

    private fun semanal(km: Double) = AgregadoDaSemana("p1", 0L, 1, km, km, 0.0, false)
}
