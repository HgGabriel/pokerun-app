package com.hggabriel.pokerun.ui.telas.home

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Os cinco estados da `HomeScreen` e a contagem regressiva (`F1-T09`, docs/03 §3.3),
 * escritos **antes** da implementação.
 *
 * Duas regras carregam a suíte:
 *
 * - **RN-27:** o plano encerra sozinho ao fim da semana da prova. Um estado que só
 *   olhasse `plano.encerrado` deixaria a Home mostrando contagem regressiva negativa
 *   para todo plano que ninguém encerrou à mão — e o dono é justamente quem some
 *   depois da prova.
 * - **RN-28:** a contagem sai no fuso do plano. Em 30/12 às 23h em São Paulo faltam
 *   dois dias para a virada, e um dia para a prova; a mesma conta em UTC já é 31/12 e
 *   responde zero.
 *
 * A grade fixa é a São Silvestre: 21 semanas de 10/08 a 31/12, com a última parcial
 * (RN-26).
 */
class PainelDeHojeTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    // -----------------------------------------------------------------------
    // Os cinco estados (docs/03 §3.3)
    // -----------------------------------------------------------------------

    @Test
    fun `sem plano ativo, a Home cai no estado sem plano`() {
        assertEquals(HomeUiState.SemPlano, painelDeHoje(null, emptyList(), emptyList(), agora("2026-08-25T12:00:00Z")))
    }

    @Test
    fun `antes da primeira segunda, o plano ainda nao comecou`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-09T12:00:00Z"))

        assertTrue(estado is HomeUiState.NaoIniciado)
        assertEquals(LocalDate.of(2026, 8, 10), (estado as HomeUiState.NaoIniciado).comecaEm)
    }

    @Test
    fun `o estado nao iniciado traz o resumo da primeira semana`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-09T12:00:00Z"))
            as HomeUiState.NaoIniciado

        assertEquals(1, estado.primeiraSemana.numero)
        assertEquals(3, estado.primeiraSemana.sessoes)
        assertEquals(grade().first().longaoKm, estado.primeiraSemana.longaoKm)
    }

    @Test
    fun `dentro do intervalo, a semana corrente e a que contem agora`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z"))
            as HomeUiState.Ativo

        // 24 a 30 de agosto é a terceira semana da grade que começa em 10/08.
        assertEquals(3, estado.semana.numero)
        assertEquals(21, estado.semana.totalDeSemanas)
        assertEquals(LocalDate.of(2026, 8, 24), estado.semana.primeiroDia)
        assertEquals(LocalDate.of(2026, 8, 30), estado.semana.ultimoDia)
    }

    @Test
    fun `plano encerrado pelo dono cai no estado encerrado`() {
        // RN-27: encerrar é do dono e o plano não reabre.
        val estado = painelDeHoje(
            plano().copy(encerrado = true),
            grade(),
            emptyList(),
            agora("2026-08-25T12:00:00Z"),
        )

        assertEquals("São Silvestre", (estado as HomeUiState.Encerrado).nomeDoPlano)
    }

    @Test
    fun `o plano encerra sozinho ao fim da semana da prova`() {
        // RN-27. `data_fim` da 21ª é a meia-noite de 01/01 no fuso do plano, que é
        // 03:00Z. Um estado preso a `plano.encerrado` mostraria a contagem regressiva
        // para sempre, porque o dono some depois da prova.
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2027-01-01T03:00:00Z"))

        assertEquals("São Silvestre", (estado as HomeUiState.Encerrado).nomeDoPlano)
    }

    @Test
    fun `um minuto antes da virada o plano ainda esta ativo`() {
        // O par do teste acima: sem ele, `>` e `>=` passam os dois.
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2027-01-01T02:59:00Z"))

        assertTrue(estado is HomeUiState.Ativo)
    }

    @Test
    fun `grade vazia nao vira sem plano, e sim carregando`() {
        // `PlanoRepositorio.criar` são duas idas ao servidor, e o listener de `weeks`
        // emite do cache antes de o servidor responder. Dizer "você não está em um
        // plano" a quem acabou de criar um seria mentira; a espera se resolve sozinha.
        assertEquals(
            HomeUiState.Carregando,
            painelDeHoje(plano(), emptyList(), emptyList(), agora("2026-08-25T12:00:00Z")),
        )
    }

    // -----------------------------------------------------------------------
    // RN-28 — a contagem regressiva, no fuso do plano
    // -----------------------------------------------------------------------

    @Test
    fun `a contagem regressiva conta dias de calendario no fuso do plano`() {
        // 2026-12-31T02:00Z é 30/12 às 23h em São Paulo: falta um dia. Em UTC o mesmo
        // instante já é 31/12, e a resposta seria zero.
        assertEquals(1, diasAteAProva(agora("2026-12-31T02:00:00Z"), plano(), grade()))
    }

    @Test
    fun `no dia da prova a contagem chega a zero`() {
        assertEquals(0, diasAteAProva(agora("2026-12-31T12:00:00Z"), plano(), grade()))
    }

    @Test
    fun `a contagem nao fica negativa depois da prova`() {
        assertEquals(0, diasAteAProva(agora("2027-01-05T12:00:00Z"), plano(), grade()))
    }

    @Test
    fun `a contagem do comeco do plano cobre as 21 semanas menos um dia`() {
        // De 10/08 a 31/12 são 143 dias de calendário.
        assertEquals(143, diasAteAProva(agora("2026-08-10T12:00:00Z"), plano(), grade()))
    }

    @Test
    fun `o estado ativo carrega a contagem regressiva`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z"))
            as HomeUiState.Ativo

        assertEquals(diasAteAProva(agora("2026-08-25T12:00:00Z"), plano(), grade()), estado.diasAteAProva)
    }

    // -----------------------------------------------------------------------
    // O card da semana (docs/03 §3.3.1)
    // -----------------------------------------------------------------------

    @Test
    fun `o card traz a fracao de sessoes feitas, e nunca um percentual`() {
        val estado = painelDeHoje(
            plano(),
            grade(),
            listOf(corrida(semanaRef = 3), corrida(semanaRef = 3)),
            agora("2026-08-25T12:00:00Z"),
        ) as HomeUiState.Ativo

        assertEquals(2, estado.semana.feitas)
        assertEquals(3, estado.semana.previstas)
    }

    @Test
    fun `a quarta corrida da semana nao passa a fracao de tres`() {
        val estado = painelDeHoje(
            plano(),
            grade(),
            List(4) { corrida(semanaRef = 3) },
            agora("2026-08-25T12:00:00Z"),
        ) as HomeUiState.Ativo

        assertEquals(3, estado.semana.feitas)
    }

    @Test
    fun `o longao cumprido do card sai de RN-10`() {
        val alvo = grade().single { it.numero == 3 }.longaoKm!!
        val estado = painelDeHoje(
            plano(),
            grade(),
            listOf(corrida(semanaRef = 3, km = 0.9 * alvo)),
            agora("2026-08-25T12:00:00Z"),
        ) as HomeUiState.Ativo

        assertEquals(true, estado.semana.longaoCumprido)
    }

    @Test
    fun `a semana sem longao previsto devolve longao cumprido nulo`() {
        // Plano de antes da DA-10: a 2ª de taper não planejava longão, e um X ali puniria
        // quem seguiu o plano.
        val antiga = grade().map { if (it.numero == 20) it.copy(longaoKm = null, kmAlvo = 13.2) else it }
        val estado = painelDeHoje(plano(), antiga, emptyList(), agora("2026-12-22T12:00:00Z"))
            as HomeUiState.Ativo

        assertEquals(20, estado.semana.numero)
        assertEquals(null, estado.semana.longaoCumprido)
    }

    @Test
    fun `a ultima semana de taper tem longao a cumprir`() {
        // DA-10: toda semana de treino tem longão, inclusive a N-1.
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-12-22T12:00:00Z"))
            as HomeUiState.Ativo

        assertEquals(20, estado.semana.numero)
        assertEquals(false, estado.semana.longaoCumprido)
    }

    @Test
    fun `o card da semana desenha um segmento por sessao prevista`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z"))
            as HomeUiState.Ativo

        assertEquals(3, estado.semana.segmentos.size)
        assertEquals(7, estado.semana.dias.size)
    }

    // -----------------------------------------------------------------------
    // A placa de peito (docs/02 §4.5) e a última corrida (docs/03 §3.3)
    // -----------------------------------------------------------------------

    @Test
    fun `a placa tem uma barra por semana e marca a corrente`() {
        val placa = (painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z")) as HomeUiState.Ativo).placa

        assertEquals(21, placa.arco.size)
        assertEquals(3, placa.semanaAtual)
        assertEquals(listOf(SituacaoDaBarra.PASSADA, SituacaoDaBarra.PASSADA), placa.arco.take(2).map { it.situacao })
        assertTrue(placa.arco.drop(3).all { it.situacao == SituacaoDaBarra.FUTURA })
    }

    @Test
    fun `a placa marca o taper e as duas semanas de taper tem longao`() {
        val arco = (painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z")) as HomeUiState.Ativo).placa.arco

        // N-2 e N-1 são taper, e desde a DA-10 as duas têm longão: 60% e 40% do pico.
        assertEquals(listOf(19, 20), arco.filter { it.taper }.map { it.numero })
        assertEquals(0.4 * 16.5, arco.first { it.numero == 20 }.longaoKm!!, 1e-9)
    }

    @Test
    fun `a barra da semana da prova e a distancia da prova`() {
        val arco = (painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z")) as HomeUiState.Ativo).placa.arco

        assertEquals(15.0, arco.last().longaoKm)
    }

    @Test
    fun `a data da prova da placa sai da grade, no fuso do plano`() {
        val placa = (painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z")) as HomeUiState.Ativo).placa

        assertEquals(LocalDate.of(2026, 12, 31), placa.dataDaProva)
        assertEquals("plano-teste", placa.planoId)
    }

    @Test
    fun `antes do inicio a placa nao tem semana corrente`() {
        val placa = (painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-09T12:00:00Z")) as HomeUiState.NaoIniciado).placa

        assertEquals(null, placa.semanaAtual)
        assertTrue(placa.arco.all { it.situacao == SituacaoDaBarra.FUTURA })
        assertTrue(placa.diasAteAProva > 0)
    }

    @Test
    fun `plano encerrado tem o arco todo passado`() {
        val estado = painelDeHoje(plano().copy(encerrado = true), grade(), emptyList(), agora("2026-08-25T12:00:00Z"))
            as HomeUiState.Encerrado

        assertTrue(estado.placa.arco.all { it.situacao == SituacaoDaBarra.PASSADA })
    }

    @Test
    fun `sem corrida no plano nao ha ultima corrida`() {
        val estado = painelDeHoje(plano(), grade(), emptyList(), agora("2026-08-25T12:00:00Z")) as HomeUiState.Ativo

        assertEquals(null, estado.ultimaCorrida)
    }

    @Test
    fun `a ultima corrida e a mais recente, avulsa incluida`() {
        val antiga = corrida(semanaRef = 3).copy(dataHoraInicio = Instant.parse("2026-08-24T09:00:00Z"))
        val avulsa = corrida(semanaRef = null, km = 4.2).copy(dataHoraInicio = Instant.parse("2026-08-26T09:00:00Z"))

        val ultima = ultimaCorrida(listOf(antiga, avulsa))

        assertEquals(UltimaCorrida(avulsa.id, 4.2, 1800), ultima)
    }

    @Test
    fun `corrida substituida ou descartada nao e a ultima`() {
        val valida = corrida(semanaRef = 3).copy(dataHoraInicio = Instant.parse("2026-08-24T09:00:00Z"))
        val substituida = corrida(semanaRef = 3).copy(dataHoraInicio = Instant.parse("2026-08-26T09:00:00Z"), substituida = true)
        val descartada = corrida(semanaRef = 3).copy(dataHoraInicio = Instant.parse("2026-08-27T09:00:00Z"), descartada = true)

        assertEquals(valida.id, ultimaCorrida(listOf(valida, substituida, descartada))?.id)
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private var proximoId = 0

    private fun agora(instante: String): Instant = Instant.parse(instante)

    private fun corrida(semanaRef: Int?, km: Double = 5.0) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = Instant.parse("2026-08-25T09:00:00Z"),
        km = km,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-teste",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
    )

    private fun parametros() = ParametrosDeGeracao(
        dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = 15.0,
        baselineKm = 5.0,
        sessoesPorSemana = 3,
        curtaKm = 2.5,
    )

    private fun plano() = Plano(
        id = "plano-teste",
        nome = "São Silvestre",
        distanciaAlvoKm = 15.0,
        dataProva = parametros().dataProva,
        fuso = fuso,
        ownerUid = "uid-dono",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = parametros(),
    )

    private fun grade(): List<Semana> = GeradorDePlano.gerar(
        parametros(),
        LocalDate.of(2026, 8, 10).atStartOfDay(fuso).toInstant(),
        fuso,
    )
}
