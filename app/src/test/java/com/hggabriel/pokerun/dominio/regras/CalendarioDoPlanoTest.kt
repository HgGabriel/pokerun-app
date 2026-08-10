package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Os casos que a ficha de `F1-T03` e docs/06 §3 exigem, escritos **antes** da
 * implementação (`EXECUCAO.md §3.2`).
 *
 * RN-28 é o guard rail cujo erro é **permanente**: `semana_ref` é gravado como
 * snapshot (RN-02), então uma corrida que cai na semana errada fica na semana
 * errada para sempre, e nenhuma tela mostra que foi um problema de fuso. Calcular
 * em UTC produz exatamente isso e passa despercebido, porque só erra as corridas de
 * domingo à noite.
 *
 * **São Paulo não tem horário de verão desde 2019**, então o caso da fronteira de
 * DST roda num plano de Nova York. Não é hipótese: é a única forma de exercitar a
 * transição, e o fuso do plano é dado do plano, não constante do app.
 */
class CalendarioDoPlanoTest {

    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val novaYork: ZoneId = ZoneId.of("America/New_York")
    private val toquio: ZoneId = ZoneId.of("Asia/Tokyo")

    // -----------------------------------------------------------------------
    // RN-28 — a corrida de domingo à noite
    // -----------------------------------------------------------------------

    @Test
    fun `domingo as 22h em Sao Paulo fica na semana que termina e nao na seguinte`() {
        // 16/08/2026 é domingo. Às 22h em UTC−3 já é segunda 01h em UTC, e é aí que
        // o cálculo ingênuo joga a corrida para a semana 2.
        val corrida = emInstante(saoPaulo, 2026, 8, 16, 22, 0)

        assertEquals(1, CalendarioDoPlano.semanaRef(corrida, plano(), grade()))
    }

    @Test
    fun `segunda as 00h05 abre a semana seguinte`() {
        val corrida = emInstante(saoPaulo, 2026, 8, 17, 0, 5)

        assertEquals(2, CalendarioDoPlano.semanaRef(corrida, plano(), grade()))
    }

    @Test
    fun `a virada de semana acontece na meia-noite de segunda, no fuso do plano`() {
        val fimDoDomingo = emInstante(saoPaulo, 2026, 8, 16, 23, 59)
        val segundaEmPonto = emInstante(saoPaulo, 2026, 8, 17, 0, 0)

        assertEquals(1, CalendarioDoPlano.semanaRef(fimDoDomingo, plano(), grade()))
        assertEquals(2, CalendarioDoPlano.semanaRef(segundaEmPonto, plano(), grade()))
    }

    // -----------------------------------------------------------------------
    // RN-28 — o corredor que viaja
    // -----------------------------------------------------------------------

    @Test
    fun `o corredor que viaja continua na semana do plano`() {
        // O mesmo instante do primeiro teste. Em Tóquio já é segunda 10h da manhã,
        // e um cálculo no fuso do aparelho devolveria 2.
        val corrida = emInstante(saoPaulo, 2026, 8, 16, 22, 0)

        assertEquals(
            "o fuso é do plano, não de onde o corredor está",
            1,
            CalendarioDoPlano.semanaRef(corrida, plano(), grade()),
        )
        // A prova de que o caso não é trivial: no fuso do aparelho, seria a semana 2.
        assertEquals(
            LocalDate.of(2026, 8, 17),
            corrida.atZone(toquio).toLocalDate(),
        )
    }

    // -----------------------------------------------------------------------
    // A fronteira do horário de verão
    // -----------------------------------------------------------------------

    @Test
    fun `a fronteira do horario de verao nao duplica nem pula semana`() {
        // Plano de Nova York: 05/10/2026 a 29/11/2026, N = 8. O horário de verão
        // termina no domingo 01/11, que cai na semana 4 — a hora 01h acontece duas
        // vezes naquele dia.
        val planoNy = plano(fuso = novaYork, prova = LocalDate.of(2026, 11, 29))
        val gradeNy = grade(fuso = novaYork, prova = LocalDate.of(2026, 11, 29))

        val comeco = emInstante(novaYork, 2026, 10, 31, 0, 0)
        val fim = emInstante(novaYork, 2026, 11, 3, 0, 0)

        val visitadas = mutableListOf<Int>()
        var t = comeco
        while (t < fim) {
            val ref = CalendarioDoPlano.semanaRef(t, planoNy, gradeNy)
            assertNotNull("instante $t ficou sem semana na fronteira de DST", ref)
            if (visitadas.lastOrNull() != ref) visitadas += ref!!
            t = t.plus(Duration.ofMinutes(30))
        }

        // Passou por 4 e por 5, nesta ordem, e por nenhuma outra: nada duplicado,
        // nada pulado, nada revisitado.
        assertEquals(listOf(4, 5), visitadas)
    }

    @Test
    fun `a semana que contem a virada do horario de verao tem sete dias`() {
        val planoNy = plano(fuso = novaYork, prova = LocalDate.of(2026, 11, 29))
        val gradeNy = grade(fuso = novaYork, prova = LocalDate.of(2026, 11, 29))
        val semana = gradeNy.single { it.numero == 4 }

        // 169 horas de relógio, sete dias de calendário. Contar semana em horas é
        // o que quebra aqui.
        assertEquals(
            Duration.ofHours(169),
            Duration.between(semana.dataInicio, semana.dataFim),
        )
        assertEquals(
            LocalDate.of(2026, 10, 26),
            semana.dataInicio.atZone(novaYork).toLocalDate(),
        )
    }

    // -----------------------------------------------------------------------
    // RN-03 — fora do intervalo do plano
    // -----------------------------------------------------------------------

    @Test
    fun `corrida antes do inicio do plano nao tem semana`() {
        val vespera = emInstante(saoPaulo, 2026, 8, 9, 23, 30)

        assertNull(CalendarioDoPlano.semanaRef(vespera, plano(), grade()))
    }

    @Test
    fun `corrida depois da prova nao tem semana, mesmo antes do domingo`() {
        // A prova é quinta 31/12. O domingo daquela semana seria 03/01, mas o plano
        // acaba na prova: 01/01 já está fora.
        val anoNovo = emInstante(saoPaulo, 2027, 1, 1, 8, 0)

        assertNull(CalendarioDoPlano.semanaRef(anoNovo, plano(), grade()))
    }

    // -----------------------------------------------------------------------
    // RN-26 — o dia da prova
    // -----------------------------------------------------------------------

    @Test
    fun `o dia da prova cai na ultima semana, que e a parcial`() {
        val largada = emInstante(saoPaulo, 2026, 12, 31, 9, 0)
        val ref = CalendarioDoPlano.semanaRef(largada, plano(), grade())

        assertEquals(21, ref)
        assertTrue(grade().single { it.numero == ref }.parcial)
    }

    @Test
    fun `a corrida das 23h do dia da prova ainda credita a semana 21`() {
        // O mesmo instante que RN-40 usa para decidir a temporada de 2026.
        val virada = emInstante(saoPaulo, 2026, 12, 31, 23, 0)

        assertEquals(21, CalendarioDoPlano.semanaRef(virada, plano(), grade()))
    }

    // -----------------------------------------------------------------------
    // O calendário e o gerador não podem divergir
    // -----------------------------------------------------------------------

    @Test
    fun `o semana_ref concorda com o intervalo de toda semana da grade`() {
        val grade = grade()

        grade.forEach { semana ->
            assertEquals(
                "o começo da semana ${semana.numero} não devolve ela mesma",
                semana.numero,
                CalendarioDoPlano.semanaRef(semana.dataInicio, plano(), grade),
            )
            assertEquals(
                "o último instante da semana ${semana.numero} não devolve ela mesma",
                semana.numero,
                CalendarioDoPlano.semanaRef(semana.dataFim.minusMillis(1), plano(), grade),
            )
        }
    }

    @Test
    fun `o fim de uma semana e o comeco da proxima, sem vao`() {
        val grade = grade()

        grade.dropLast(1).forEach { semana ->
            assertEquals(
                "a semana ${semana.numero} não encosta na seguinte",
                semana.numero + 1,
                CalendarioDoPlano.semanaRef(semana.dataFim, plano(), grade),
            )
        }
    }

    @Test
    fun `semanaDe devolve a propria semana e nao so o numero`() {
        val corrida = emInstante(saoPaulo, 2026, 8, 16, 22, 0)

        assertEquals(1, CalendarioDoPlano.semanaDe(corrida, plano(), grade())?.numero)
        assertNull(
            CalendarioDoPlano.semanaDe(
                emInstante(saoPaulo, 2027, 1, 1, 8, 0), plano(), grade(),
            ),
        )
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private fun emInstante(
        fuso: ZoneId,
        ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int,
    ): Instant = LocalDateTime.of(ano, mes, dia, hora, minuto).atZone(fuso).toInstant()

    private fun parametros(fuso: ZoneId, prova: LocalDate) = ParametrosDeGeracao(
        dataProva = prova.atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = 15.0,
        baselineKm = 5.0,
        sessoesPorSemana = 3,
    )

    private fun plano(
        fuso: ZoneId = saoPaulo,
        prova: LocalDate = LocalDate.of(2026, 12, 31),
    ) = Plano(
        id = "plano-teste",
        nome = "São Silvestre 2026",
        distanciaAlvoKm = 15.0,
        dataProva = prova.atStartOfDay(fuso).toInstant(),
        fuso = fuso,
        ownerUid = "uid-dono",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = parametros(fuso, prova),
    )

    private fun grade(
        fuso: ZoneId = saoPaulo,
        prova: LocalDate = LocalDate.of(2026, 12, 31),
        inicio: LocalDate = if (fuso == saoPaulo) LocalDate.of(2026, 8, 10) else LocalDate.of(2026, 10, 5),
    ): List<Semana> = GeradorDePlano.gerar(
        parametros(fuso, prova),
        inicio.atStartOfDay(fuso).toInstant(),
        fuso,
    )
}
