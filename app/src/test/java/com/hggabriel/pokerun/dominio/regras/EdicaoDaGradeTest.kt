package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A grade **depois** de gerada (DA-10, docs/01 §3.3), escrita antes da implementação.
 *
 * A regra cabe numa linha: **cada semana de build vale o que foi pedido — a rampa ou a
 * edição do dono —, limitada a 110% do maior longão das 4 semanas anteriores.** A mesma
 * função gera, edita e migra; estes testes cobrem os dois últimos usos.
 *
 * O exemplo é o de docs/01 §3.3: a São Silvestre do casal, com as semanas 1 e 2 já
 * corridas, e o dono baixando a semana 3 para 8 km. Os números vêm de
 * `execucao/simular-gerador.py`, seção 4.
 */
class EdicaoDaGradeTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    private val parametros = ParametrosDeGeracao(
        dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = 15.0,
        baselineKm = 10.0,
        sessoesPorSemana = 3,
        curtaKm = 3.0,
    )

    private val grade: List<Semana> = GeradorDePlano.gerar(
        parametros,
        LocalDate.of(2026, 10, 21).atStartOfDay(fuso).toInstant(),
        fuso,
    )

    private val congeladas = setOf(1, 2)

    private fun aplicada(resultado: ResultadoDaEdicao): ResultadoDaEdicao.Aplicada {
        assertTrue("esperava edição aplicada, veio $resultado", resultado is ResultadoDaEdicao.Aplicada)
        return resultado as ResultadoDaEdicao.Aplicada
    }

    private fun List<Semana>.longoes(de: Int, ate: Int) = filter { it.numero in de..ate }.map { it.longaoKm!! }

    // -----------------------------------------------------------------------
    // O teto
    // -----------------------------------------------------------------------

    @Test
    fun `o teto da semana 3 e 110 por cento do maior longao das 4 semanas anteriores`() {
        // Semanas 0 (a baseline, 10), 1 (10,74) e 2 (11,54): o maior é o da 2.
        assertEquals(1.1 * grade[1].longaoKm!!, tetoDoLongao(parametros, grade, 3), TOLERANCIA)
    }

    @Test
    fun `o teto da semana 1 olha a baseline como semana 0`() {
        assertEquals(11.0, tetoDoLongao(parametros, grade, 1), TOLERANCIA)
    }

    @Test
    fun `o teto nunca passa de 32 km`() {
        val maratona = parametros.copy(distanciaAlvoKm = 42.2, baselineKm = 31.0)
        val gradeLonga = GeradorDePlano.gerar(
            maratona.copy(dataProva = LocalDate.of(2027, 3, 28).atStartOfDay(fuso).toInstant()),
            LocalDate.of(2026, 10, 26).atStartOfDay(fuso).toInstant(),
            fuso,
        )

        assertEquals(32.0, tetoDoLongao(maratona, gradeLonga, 5), TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // Baixar uma semana: aceita sempre, e segura as seguintes
    // -----------------------------------------------------------------------

    @Test
    fun `baixar a semana 3 para 8 km segura as semanas 4 a 6 e volta a rampa na 7`() {
        val r = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas))

        val esperado = listOf(8.0, 1.1 * grade[1].longaoKm!!, 1.1 * 1.1 * grade[1].longaoKm!!, 1.1 * 1.1 * 1.1 * grade[1].longaoKm!!, 16.5)
        r.grade.longoes(3, 7).zip(esperado).forEach { (atual, esperado) -> assertEquals(esperado, atual, 0.001) }
        assertEquals(listOf(12.69, 13.96, 15.36, 16.5), r.grade.longoes(4, 7).map { Math.round(it * 100) / 100.0 })
    }

    @Test
    fun `a edicao lista as outras semanas que mudaram de forma visivel`() {
        val r = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas))

        // A semana 6 também desce, de 15,361 para 15,358 km: 3 metros, que a tela mostra
        // como "15,4 → 15,4". Só entra na lista a mudança de 50 m ou mais.
        assertEquals(listOf(4, 5), r.tambemMudam.map { it.numero })
        assertEquals(grade[3].longaoKm!!, r.tambemMudam.first().deKm, TOLERANCIA)
        assertEquals(r.grade[3].longaoKm!!, r.tambemMudam.first().paraKm, TOLERANCIA)
    }

    @Test
    fun `a semana editada guarda o pedido e o volume sai dele com as curtas`() {
        val r = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas))
        val semana = r.grade[2]

        assertEquals(8.0, semana.longaoPedidoKm!!, TOLERANCIA)
        assertEquals(8.0 + 2 * 3.0, semana.kmAlvo, TOLERANCIA)
        assertTrue(r.grade.filter { it.numero != 3 }.all { it.longaoPedidoKm == null })
    }

    @Test
    fun `o pico e o taper nao mudam quando a grade volta a rampa`() {
        val r = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas))

        assertEquals(grade.drop(6), r.grade.drop(6))
    }

    @Test
    fun `as semanas congeladas nao mudam`() {
        val r = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas))

        assertEquals(grade.take(2), r.grade.take(2))
    }

    // -----------------------------------------------------------------------
    // Subir uma semana: até o teto, e acima dele recusa
    // -----------------------------------------------------------------------

    @Test
    fun `subir acima do teto e recusado e diz o teto`() {
        val r = editarLongao(parametros, grade, 3, 13.0, congeladas)

        assertTrue(r is ResultadoDaEdicao.AcimaDoTeto)
        assertEquals(1.1 * grade[1].longaoKm!!, (r as ResultadoDaEdicao.AcimaDoTeto).tetoKm, TOLERANCIA)
    }

    @Test
    fun `subir ate o teto e aceito e nenhuma outra semana muda`() {
        val teto = tetoDoLongao(parametros, grade, 3)
        val r = aplicada(editarLongao(parametros, grade, 3, teto, congeladas))

        assertEquals(teto, r.grade[2].longaoKm!!, TOLERANCIA)
        assertTrue(r.tambemMudam.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Desfazer e pedidos em cadeia
    // -----------------------------------------------------------------------

    @Test
    fun `desfazer devolve a grade a rampa`() {
        val editada = aplicada(editarLongao(parametros, grade, 3, 8.0, congeladas)).grade
        val desfeita = aplicada(editarLongao(parametros, editada, 3, null, congeladas)).grade

        assertEquals(grade, desfeita)
    }

    @Test
    fun `o pedido de uma semana seguinte sobrevive a edicao de uma anterior`() {
        val comSemana5 = aplicada(editarLongao(parametros, grade, 5, 13.0, congeladas)).grade
        val comAs2 = aplicada(editarLongao(parametros, comSemana5, 3, 8.0, congeladas)).grade

        // A semana 5 continua pedindo 13, e o teto novo dela (13,96) deixa.
        assertEquals(13.0, comAs2[4].longaoKm!!, TOLERANCIA)
        assertEquals(13.0, comAs2[4].longaoPedidoKm!!, TOLERANCIA)

        val desfeita = aplicada(editarLongao(parametros, comAs2, 3, null, congeladas)).grade
        assertEquals(comSemana5, desfeita)
    }

    @Test
    fun `um pedido acima do teto novo fica limitado e guardado`() {
        // Sem semanas congeladas. A semana 4 pede o próprio teto (13,63). Depois a
        // semana 2 cai para 5 km: a 3 fica em 11,82, e o teto da 4 vira 13,0. O pedido
        // de 13,63 continua gravado, e o longão fica no teto novo.
        val teto4 = tetoDoLongao(parametros, grade, 4)
        val comSemana4 = aplicada(editarLongao(parametros, grade, 4, teto4, emptySet())).grade
        val comSemana2 = aplicada(editarLongao(parametros, comSemana4, 2, 5.0, emptySet())).grade

        val semana4 = comSemana2[3]
        assertEquals(teto4, semana4.longaoPedidoKm!!, TOLERANCIA)
        assertTrue(semana4.longaoKm!! < teto4)
        assertEquals(tetoDoLongao(parametros, comSemana2, 4), semana4.longaoKm!!, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // O que não se edita
    // -----------------------------------------------------------------------

    @Test
    fun `semana congelada nao se edita`() {
        assertSame(ResultadoDaEdicao.NaoEditavel, editarLongao(parametros, grade, 2, 9.0, congeladas))
    }

    @Test
    fun `taper e prova nao se editam`() {
        assertSame(ResultadoDaEdicao.NaoEditavel, editarLongao(parametros, grade, 8, 9.0, congeladas))
        assertSame(ResultadoDaEdicao.NaoEditavel, editarLongao(parametros, grade, 10, 9.0, congeladas))
    }

    @Test
    fun `longao zero ou negativo nao se aplica`() {
        assertSame(ResultadoDaEdicao.NaoEditavel, editarLongao(parametros, grade, 3, 0.0, congeladas))
    }

    // -----------------------------------------------------------------------
    // A curta do plano
    // -----------------------------------------------------------------------

    @Test
    fun `trocar a curta refaz o volume das semanas nao fixas e nao mexe no longao`() {
        val nova = trocarCurta(parametros, grade, 4.0, fixas = setOf(1, 2, 3))

        assertEquals(grade.take(3), nova.take(3))
        nova.drop(3).dropLast(1).forEach { semana ->
            assertEquals(grade[semana.numero - 1].longaoKm!!, semana.longaoKm!!, TOLERANCIA)
            assertEquals(semana.longaoKm!! + 2 * 4.0, semana.kmAlvo, TOLERANCIA)
        }
        assertEquals(grade.last(), nova.last())
    }

    // -----------------------------------------------------------------------
    // A migração: a mesma função, sobre uma grade da regra anterior
    // -----------------------------------------------------------------------

    @Test
    fun `migrar o plano real reescreve so as semanas futuras`() {
        // O Plano Teste do export de 15/10/2026: 10 km em 30/11, baseline 6, 3 sessões,
        // a partir de 17/08. As semanas 1 a 10 ficam (congeladas e a corrente em 21/10).
        val real = ParametrosDeGeracao(
            dataProva = LocalDate.of(2026, 11, 30).atStartOfDay(fuso).toInstant(),
            distanciaAlvoKm = 10.0,
            baselineKm = 6.0,
            sessoesPorSemana = 3,
            curtaKm = 3.0,
        )
        val antiga = gradeDaRegraAnterior()

        val migrada = recalcularGrade(real, antiga, fixas = (1..10).toSet())

        assertEquals(antiga.take(10), migrada.take(10))
        assertEquals(listOf(10.02, 10.50, 11.0, 6.6, 4.4), migrada.slice(10..14).map { Math.round(it.longaoKm!! * 100) / 100.0 })
        migrada.slice(10..14).forEach { assertEquals(it.longaoKm!! + 6.0, it.kmAlvo, TOLERANCIA) }
        assertEquals(antiga.last(), migrada.last())
        assertTrue(migrada.slice(10..14).all { it.tipo == antiga[it.numero - 1].tipo })
        assertNull(migrada.last().longaoKm)
    }

    /** As 16 semanas do Plano Teste como estão no export de 15/10/2026. */
    private fun gradeDaRegraAnterior(): List<Semana> {
        val segunda = LocalDate.of(2026, 8, 17)
        fun semana(n: Int, km: Double, longao: Double?, tipo: TipoDeSemana, sessoes: Int = 3) = Semana(
            numero = n,
            dataInicio = segunda.plusWeeks((n - 1).toLong()).atStartOfDay(fuso).toInstant(),
            dataFim = if (n == 16) {
                LocalDate.of(2026, 12, 1).atStartOfDay(fuso).toInstant()
            } else {
                segunda.plusWeeks(n.toLong()).atStartOfDay(fuso).toInstant()
            },
            sessoesAlvo = sessoes,
            kmAlvo = km,
            longaoKm = longao,
            tipo = tipo,
            parcial = n == 16,
        )
        val build = (1..13).map { n ->
            val l = 6.0 + (11.0 - 6.0) * (n - 1) / 12
            semana(n, l * 2, l, TipoDeSemana.BUILD)
        }
        return build + listOf(
            semana(14, 13.2, 6.6, TipoDeSemana.TAPER),
            semana(15, 8.8, null, TipoDeSemana.TAPER),
            semana(16, 10.0, null, TipoDeSemana.PROVA, sessoes = 1),
        )
    }

    private companion object {
        const val TOLERANCIA = 1e-9
    }
}
