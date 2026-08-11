package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Os casos que a ficha de `F1-T02` e docs/06 §3 exigem, escritos **antes** da
 * implementação (`EXECUCAO.md §3.2`).
 *
 * O gerador é a primeira função pura do projeto e o risco declarado como mais
 * provável é *"código plausível e errado"*. Uma grade errada é plausível por
 * construção: os números saem bonitos, sobem semana a semana e ninguém percebe até
 * a pessoa lesionar em outubro.
 *
 * Os números esperados não vêm da implementação. Vêm da tabela de exemplo de
 * docs/01 §3.2 — baseline 5 km, alvo 15 km, 3 sessões, N = 21 — que a
 * especificação publica linha a linha.
 */
class GeradorDePlanoTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** A segunda-feira de início da São Silvestre de 2026. */
    private fun inicio(dia: LocalDate = LocalDate.of(2026, 8, 10)) =
        dia.atStartOfDay(fuso).toInstant()

    private fun parametros(
        alvo: Double = 15.0,
        baseline: Double = 5.0,
        sessoes: Int = 3,
        prova: LocalDate = LocalDate.of(2026, 12, 31),
    ) = ParametrosDeGeracao(
        dataProva = prova.atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = alvo,
        baselineKm = baseline,
        sessoesPorSemana = sessoes,
    )

    private fun dataDe(instante: java.time.Instant): LocalDate =
        instante.atZone(fuso).toLocalDate()

    // -----------------------------------------------------------------------
    // O caso real: a prova de 31/12/2026 (RN-26)
    // -----------------------------------------------------------------------

    @Test
    fun `a prova de 31 de dezembro com inicio em 10 de agosto rende 21 semanas`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals(21, grade.size)
        assertEquals(listOf(1, 21), listOf(grade.first().numero, grade.last().numero))
        assertEquals(LocalDate.of(2026, 8, 10), dataDe(grade.first().dataInicio))
    }

    @Test
    fun `a semana da prova vai de 28 a 31 de dezembro e e parcial`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)
        val prova = grade.last()

        assertEquals(TipoDeSemana.PROVA, prova.tipo)
        assertEquals(LocalDate.of(2026, 12, 28), dataDe(prova.dataInicio))
        // `dataFim` é exclusivo: a semana termina quando 01/01 começa. Ver o KDoc
        // de `GeradorDePlano`.
        assertEquals(LocalDate.of(2027, 1, 1), dataDe(prova.dataFim))
        assertTrue("a 21ª semana tem quatro dias, não sete (RN-26)", prova.parcial)
    }

    @Test
    fun `a semana da prova tem uma sessao so e o volume e a distancia da prova`() {
        val prova = GeradorDePlano.gerar(parametros(), inicio(), fuso).last()

        assertEquals(1, prova.sessoesAlvo)
        assertEquals(15.0, prova.kmAlvo, TOLERANCIA)
        assertNull("não se planeja longão na semana da prova", prova.longaoKm)
    }

    @Test
    fun `so a semana da prova e parcial`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals(listOf(21), grade.filter { it.parcial }.map { it.numero })
    }

    // -----------------------------------------------------------------------
    // Os números publicados em docs/01 §3.2
    // -----------------------------------------------------------------------

    @Test
    fun `baseline 5 e alvo 15 com 3 sessoes dao 10 km na semana 1 e 33 km na semana 18`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals(10.0, grade.semana(1).kmAlvo, TOLERANCIA)
        assertEquals(33.0, grade.semana(18).kmAlvo, TOLERANCIA)
    }

    @Test
    fun `o longao comeca na baseline e chega ao pico de 1,1 vezes o alvo na semana N menos 3`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals(5.0, grade.semana(1).longaoKm!!, TOLERANCIA)
        assertEquals(16.5, grade.semana(18).longaoKm!!, TOLERANCIA)
    }

    @Test
    fun `a primeira semana de taper tem 60 por cento do longao pico`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)
        val taper = grade.semana(19)

        assertEquals(TipoDeSemana.TAPER, taper.tipo)
        assertEquals(9.9, taper.longaoKm!!, TOLERANCIA)
        assertEquals(19.8, taper.kmAlvo, TOLERANCIA)
    }

    @Test
    fun `a segunda semana de taper nao tem longao e vale 40 por cento do volume pico`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)
        val taper = grade.semana(20)

        assertEquals(TipoDeSemana.TAPER, taper.tipo)
        assertNull("a 2ª semana de taper não tem longão", taper.longaoKm)
        assertEquals(13.2, taper.kmAlvo, TOLERANCIA)
        // 40% de 33,0 dividido igualmente entre as 3 sessões previstas.
        assertEquals(4.4, taper.kmAlvo / taper.sessoesAlvo, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // A interpolação
    // -----------------------------------------------------------------------

    @Test
    fun `a interpolacao do longao e linear e nunca composta`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)
        val build = grade.filter { it.tipo == TipoDeSemana.BUILD }

        // Linear de 5,0 a 16,5 em 17 passos: o degrau é sempre 11,5/17.
        val degraus = build.zipWithNext { a, b -> b.longaoKm!! - a.longaoKm!! }
        degraus.forEach { assertEquals(11.5 / 17, it, TOLERANCIA) }

        // A composta que 18 semanas a juros produziria passaria por ~5,36 aqui, e
        // multiplicaria o volume por 5,5× no fim (docs/01 §3.2).
        assertEquals(5.0 + 11.5 / 17, grade.semana(2).longaoKm!!, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // A matriz de sessões — o bug da fórmula antiga
    // -----------------------------------------------------------------------

    @Test
    fun `nenhuma sessao curta e maior que o longao da mesma semana`() {
        for (sessoes in 2..4) {
            val grade = GeradorDePlano.gerar(parametros(sessoes = sessoes), inicio(), fuso)
            grade.filter { it.longaoKm != null && it.sessoesAlvo > 1 }.forEach { semana ->
                val curta = (semana.kmAlvo - semana.longaoKm!!) / (semana.sessoesAlvo - 1)
                assertTrue(
                    "semana ${semana.numero} com $sessoes sessões: curta $curta > longão ${semana.longaoKm}",
                    curta <= semana.longaoKm!! + TOLERANCIA,
                )
            }
        }
    }

    @Test
    fun `a curta e sempre metade do longao nas tres configuracoes`() {
        for (sessoes in 2..4) {
            val grade = GeradorDePlano.gerar(parametros(sessoes = sessoes), inicio(), fuso)
            grade.filter { it.longaoKm != null && it.sessoesAlvo > 1 }.forEach { semana ->
                val curta = (semana.kmAlvo - semana.longaoKm!!) / (semana.sessoesAlvo - 1)
                assertEquals(semana.longaoKm!! / 2, curta, TOLERANCIA)
            }
        }
    }

    @Test
    fun `com 4 sessoes o volume e o longao dividido por 0,4`() {
        val grade = GeradorDePlano.gerar(parametros(sessoes = 4), inicio(), fuso)

        grade.filter { it.longaoKm != null }.forEach { semana ->
            assertEquals(semana.longaoKm!! / 0.4, semana.kmAlvo, TOLERANCIA)
        }
    }

    // -----------------------------------------------------------------------
    // N mínimo
    // -----------------------------------------------------------------------

    @Test
    fun `o N minimo de 8 semanas rende 5 de build 2 de taper e 1 de prova`() {
        // 8 semanas a partir de 10/08: a última começa em 28/09 e a prova cai nela.
        val grade = GeradorDePlano.gerar(
            parametros(prova = LocalDate.of(2026, 10, 4)),
            inicio(),
            fuso,
        )

        assertEquals(8, grade.size)
        assertEquals(5, grade.count { it.tipo == TipoDeSemana.BUILD })
        assertEquals(2, grade.count { it.tipo == TipoDeSemana.TAPER })
        assertEquals(1, grade.count { it.tipo == TipoDeSemana.PROVA })
        // O pico continua em N−3, que aqui é a semana 5.
        assertEquals(16.5, grade.semana(5).longaoKm!!, TOLERANCIA)
    }

    @Test
    fun `prova no domingo fecha sete dias e a semana da prova nao e parcial`() {
        // RN-26 diz que a semana da prova **pode** ser parcial, e o critério é ter
        // menos de sete dias. 04/10/2026 é domingo, então ali ela é inteira.
        val grade = GeradorDePlano.gerar(
            parametros(prova = LocalDate.of(2026, 10, 4)),
            inicio(),
            fuso,
        )
        val prova = grade.last()

        assertEquals(LocalDate.of(2026, 9, 28), dataDe(prova.dataInicio))
        assertEquals(LocalDate.of(2026, 10, 5), dataDe(prova.dataFim))
        assertFalse("prova no domingo: a semana tem sete dias (RN-26)", prova.parcial)
        assertEquals(1, prova.sessoesAlvo)
    }

    // -----------------------------------------------------------------------
    // A semana de início é a segunda-feira, não o dia do cadastro
    // -----------------------------------------------------------------------

    @Test
    fun `plano criado no meio da semana comeca na segunda anterior`() {
        // Quarta-feira, 12/08/2026.
        val grade = GeradorDePlano.gerar(
            parametros(),
            inicio(LocalDate.of(2026, 8, 12)),
            fuso,
        )

        assertEquals(LocalDate.of(2026, 8, 10), dataDe(grade.first().dataInicio))
        assertEquals(21, grade.size)
    }

    @Test
    fun `toda semana que nao e a da prova vai de segunda a segunda`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        grade.dropLast(1).forEach { semana ->
            assertEquals(
                "semana ${semana.numero} não começa numa segunda",
                java.time.DayOfWeek.MONDAY,
                dataDe(semana.dataInicio).dayOfWeek,
            )
            assertEquals(
                "semana ${semana.numero} não fecha sete dias depois",
                dataDe(semana.dataInicio).plusDays(7),
                dataDe(semana.dataFim),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Invariantes da grade
    // -----------------------------------------------------------------------

    @Test
    fun `as semanas saem numeradas de 1 a N e nenhuma nasce congelada`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals((1..21).toList(), grade.map { it.numero })
        // Não há campo `congelada` desde `F1-T05c`: a pergunta é uma data. No
        // instante em que a grade nasce, nenhuma semana dela já acabou.
        assertTrue(
            "grade recém-gerada não tem semana congelada",
            grade.none { CalendarioDoPlano.congelada(it, inicio()) },
        )
    }

    @Test
    fun `toda semana de build tem longao`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        grade.filter { it.tipo == TipoDeSemana.BUILD }.forEach {
            assertNotNull("semana ${it.numero} é build e não tem longão", it.longaoKm)
        }
    }

    @Test
    fun `as semanas de build sao as N menos 3 primeiras`() {
        val grade = GeradorDePlano.gerar(parametros(), inicio(), fuso)

        assertEquals((1..18).toList(), grade.filter { it.tipo == TipoDeSemana.BUILD }.map { it.numero })
        assertEquals(listOf(19, 20), grade.filter { it.tipo == TipoDeSemana.TAPER }.map { it.numero })
    }

    private fun List<com.hggabriel.pokerun.dominio.modelo.Semana>.semana(n: Int) =
        single { it.numero == n }

    private companion object {
        /** Os números da spec têm uma casa decimal; a conta roda em precisão cheia. */
        const val TOLERANCIA = 1e-9
    }
}
