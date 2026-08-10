package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Os casos que a ficha de `F1-T04` e docs/06 §3 exigem, escritos **antes** da
 * implementação (`EXECUCAO.md §3.2`).
 *
 * A aderência conta **sessões**, nunca quilômetros (D-06). A escolha é uma defesa
 * contra erro de medição: o Galaxy Fit 3 não tem GPS próprio, e uma aderência
 * baseada em distância transformaria um relógio ruim em falta de disciplina.
 *
 * Os dois erros plausíveis que estes testes existem para pegar são 133% numa semana
 * de 3 previstas com 4 corridas, e o longão entrando na conta.
 */
class CalculoDeAderenciaTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    // -----------------------------------------------------------------------
    // RN-08 — sessões feitas ÷ previstas, com teto
    // -----------------------------------------------------------------------

    @Test
    fun `quatro corridas numa semana de tres previstas dao 100 por cento e nao 133`() {
        val semana = grade().semana(1)
        val corridas = List(4) { corrida(semanaRef = 1, km = 5.0) }

        assertEquals(1.0, CalculoDeAderencia.daSemana(semana, corridas), TOLERANCIA)
    }

    @Test
    fun `duas corridas numa semana de tres previstas dao dois tercos`() {
        val semana = grade().semana(1)
        val corridas = List(2) { corrida(semanaRef = 1, km = 5.0) }

        assertEquals(2.0 / 3, CalculoDeAderencia.daSemana(semana, corridas), TOLERANCIA)
    }

    @Test
    fun `a quarta corrida da semana nao vira sessao feita`() {
        val semana = grade().semana(1)

        assertEquals(3, CalculoDeAderencia.sessoesFeitas(semana, List(4) { corrida(semanaRef = 1) }))
    }

    @Test
    fun `so contam as corridas da propria semana`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1),
            corrida(semanaRef = 2),
            corrida(semanaRef = null),
        )

        assertEquals(1, CalculoDeAderencia.sessoesFeitas(semana, corridas))
    }

    // -----------------------------------------------------------------------
    // RN-11 — semana perdida não penaliza
    // -----------------------------------------------------------------------

    @Test
    fun `semana sem corrida da zero e nao penaliza a semana seguinte`() {
        val grade = grade()
        val corridas = List(3) { corrida(semanaRef = 2) }

        assertEquals(0.0, CalculoDeAderencia.daSemana(grade.semana(1), corridas), TOLERANCIA)
        assertEquals(1.0, CalculoDeAderencia.daSemana(grade.semana(2), corridas), TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // D-06 — o longão não entra na aderência
    // -----------------------------------------------------------------------

    @Test
    fun `longao nao cumprido com as tres sessoes feitas ainda da 100 por cento`() {
        val semana = grade().semana(1)
        // O longão previsto é 5,0 km; a maior corrida da semana tem 1,2 km.
        val corridas = List(3) { corrida(semanaRef = 1, km = 1.2) }

        assertEquals(1.0, CalculoDeAderencia.daSemana(semana, corridas), TOLERANCIA)
        assertFalse(CalculoDeAderencia.longaoCumprido(semana, corridas)!!)
    }

    // -----------------------------------------------------------------------
    // RN-10 — o longão é indicador de UI, com piso de 90%
    // -----------------------------------------------------------------------

    @Test
    fun `longao com 90 por cento do alvo conta como cumprido`() {
        val semana = grade().semana(1) // longão previsto: 5,0 km
        val corridas = listOf(corrida(semanaRef = 1, km = 4.5))

        assertTrue(CalculoDeAderencia.longaoCumprido(semana, corridas)!!)
    }

    @Test
    fun `longao com 89 por cento do alvo nao conta como cumprido`() {
        val semana = grade().semana(1)
        val corridas = listOf(corrida(semanaRef = 1, km = 4.45))

        assertFalse(CalculoDeAderencia.longaoCumprido(semana, corridas)!!)
    }

    @Test
    fun `o longao da semana e a maior corrida, nao a primeira`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 2.0),
            corrida(semanaRef = 1, km = 4.8),
            corrida(semanaRef = 1, km = 3.0),
        )

        assertTrue(CalculoDeAderencia.longaoCumprido(semana, corridas)!!)
    }

    @Test
    fun `semana sem longao previsto nao tem longao a cumprir`() {
        val grade = grade()
        val corridas = List(3) { corrida(semanaRef = 20, km = 9.0) }

        assertNull(
            "a 2ª semana de taper não planeja longão: o indicador não se aplica",
            CalculoDeAderencia.longaoCumprido(grade.semana(20), corridas),
        )
    }

    @Test
    fun `semana sem corrida nenhuma nao tem longao cumprido`() {
        val semana = grade().semana(1)

        assertFalse(CalculoDeAderencia.longaoCumprido(semana, emptyList())!!)
    }

    // -----------------------------------------------------------------------
    // RN-26 — a semana da prova
    // -----------------------------------------------------------------------

    @Test
    fun `a semana da prova tem denominador 1`() {
        val prova = grade().semana(21)

        assertEquals(0.0, CalculoDeAderencia.daSemana(prova, emptyList()), TOLERANCIA)
        assertEquals(
            1.0,
            CalculoDeAderencia.daSemana(prova, listOf(corrida(semanaRef = 21, km = 15.0))),
            TOLERANCIA,
        )
    }

    // -----------------------------------------------------------------------
    // RN-24 e RN-31 — o que não conta como sessão
    // -----------------------------------------------------------------------

    @Test
    fun `corrida descartada nao conta como sessao`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1),
            corrida(semanaRef = 1, descartada = true),
        )

        assertEquals(1, CalculoDeAderencia.sessoesFeitas(semana, corridas))
    }

    @Test
    fun `corrida substituida nao conta duas vezes`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, substituida = true),
            corrida(semanaRef = 1, substituiRunId = "run-antiga"),
        )

        assertEquals(1, CalculoDeAderencia.sessoesFeitas(semana, corridas))
    }

    @Test
    fun `corrida descartada nao vira longao`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 2.0),
            corrida(semanaRef = 1, km = 9.0, descartada = true),
        )

        assertFalse(
            "a corrida descartada tinha 9 km e não pode cumprir o longão",
            CalculoDeAderencia.longaoCumprido(semana, corridas)!!,
        )
    }

    // -----------------------------------------------------------------------
    // RN-19 — quem entra depois
    // -----------------------------------------------------------------------

    @Test
    fun `membro que entra na semana 8 tem denominador a partir da semana 8`() {
        val grade = grade()
        // Fez tudo da semana 8 à 10, e nada antes — porque não estava no plano.
        val corridas = (8..10).flatMap { n -> List(3) { corrida(semanaRef = n) } }

        assertEquals(
            1.0,
            CalculoDeAderencia.doPlano(grade, corridas, membro(entrouNaSemana = 8), ateSemana = 10),
            TOLERANCIA,
        )
    }

    @Test
    fun `o mesmo historico para quem estava desde a semana 1 nao da 100 por cento`() {
        val grade = grade()
        val corridas = (8..10).flatMap { n -> List(3) { corrida(semanaRef = n) } }

        // 9 sessões feitas em 30 previstas.
        assertEquals(
            9.0 / 30,
            CalculoDeAderencia.doPlano(grade, corridas, membro(entrouNaSemana = 1), ateSemana = 10),
            TOLERANCIA,
        )
    }

    @Test
    fun `a aderencia do plano ignora as semanas que ainda nao chegaram`() {
        val grade = grade()
        val corridas = List(3) { corrida(semanaRef = 1) }

        assertEquals(
            1.0,
            CalculoDeAderencia.doPlano(grade, corridas, membro(entrouNaSemana = 1), ateSemana = 1),
            TOLERANCIA,
        )
    }

    @Test
    fun `uma semana com corrida sobrando nao compensa outra semana vazia`() {
        val grade = grade()
        // 6 corridas na semana 1 (3 previstas) e nada na 2. O teto é por semana:
        // as 3 que sobram não podem cobrir a semana vazia.
        val corridas = List(6) { corrida(semanaRef = 1) }

        assertEquals(
            3.0 / 6,
            CalculoDeAderencia.doPlano(grade, corridas, membro(entrouNaSemana = 1), ateSemana = 2),
            TOLERANCIA,
        )
    }

    @Test
    fun `a aderencia do plano nunca passa de 100 por cento`() {
        val grade = grade()
        val corridas = (1..3).flatMap { n -> List(9) { corrida(semanaRef = n) } }

        assertEquals(
            1.0,
            CalculoDeAderencia.doPlano(grade, corridas, membro(entrouNaSemana = 1), ateSemana = 3),
            TOLERANCIA,
        )
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private var proximoId = 0

    private fun corrida(
        semanaRef: Int?,
        km: Double = 5.0,
        descartada: Boolean = false,
        substituida: Boolean = false,
        substituiRunId: String? = null,
    ) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = Instant.parse("2026-08-12T09:00:00Z"),
        km = km,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-teste",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
        descartada = descartada,
        substituida = substituida,
        substituiRunId = substituiRunId,
    )

    private fun membro(entrouNaSemana: Int) = Membro(
        uid = "uid-membro",
        nome = "Corredor",
        entrouEm = Instant.parse("2026-08-10T12:00:00Z"),
        entrouNaSemana = entrouNaSemana,
    )

    private fun grade(): List<Semana> = GeradorDePlano.gerar(
        ParametrosDeGeracao(
            dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(fuso).toInstant(),
            distanciaAlvoKm = 15.0,
            baselineKm = 5.0,
            sessoesPorSemana = 3,
        ),
        LocalDate.of(2026, 8, 10).atStartOfDay(fuso).toInstant(),
        fuso,
    )

    private fun List<Semana>.semana(n: Int) = single { it.numero == n }

    private companion object {
        const val TOLERANCIA = 1e-9
    }
}
