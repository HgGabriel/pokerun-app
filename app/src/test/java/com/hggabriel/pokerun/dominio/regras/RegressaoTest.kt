package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A regressão por inatividade (`F4-T10`, docs/04 §5.2, RN-36 a RN-38, XP-07 a XP-09),
 * escrita antes da implementação.
 *
 * Duas escalas de temporada. A **pequena** (8 espécies: 4 de 100 XP e 4 de 250 XP) deixa
 * os números à mão. A **de Kanto** repete a tabela de docs/04 §4 e prova os números do
 * documento: *−1.516 XP* por semana no fim do Tier 5 e *~34 semanas* até o piso.
 */
class RegressaoTest {

    // -----------------------------------------------------------------------
    // Quando a queda começa (XP-08)
    // -----------------------------------------------------------------------

    @Test
    fun `20 dias parado nao tira XP, 21 dias tira a primeira queda de 2 por cento`() {
        val ultima = Instant.parse("2026-09-01T10:00:00Z")

        assertEquals(0, Regressao.semanasDeQueda(ultima, ultima.plus(Duration.ofDays(20))))
        assertEquals(1, Regressao.semanasDeQueda(ultima, ultima.plus(Duration.ofDays(21))))
        assertEquals(1, Regressao.semanasDeQueda(ultima, ultima.plus(Duration.ofDays(27))))
        assertEquals(2, Regressao.semanasDeQueda(ultima, ultima.plus(Duration.ofDays(28))))

        assertEquals(1200L, Regressao.aplicar(1200, semanas = 0, temporada = pequena))
        assertEquals(1176L, Regressao.aplicar(1200, semanas = 1, temporada = pequena))
    }

    // -----------------------------------------------------------------------
    // O piso de tier
    // -----------------------------------------------------------------------

    @Test
    fun `o XP trava no piso do tier atual`() {
        // 1.200 XP passam 7 espécies: a 7ª é do tier 2, cujo piso é o fim do tier 1 (400).
        assertEquals(400L, Regressao.pisoDoTier(1200, pequena))
        assertEquals(400L, Regressao.aplicar(1200, semanas = 200, temporada = pequena))
    }

    @Test
    fun `quem esta no primeiro tier cai ate perto de zero`() {
        assertEquals(0L, Regressao.pisoDoTier(300, pequena))
        assertTrue(Regressao.aplicar(300, semanas = 500, temporada = pequena) < 5)
    }

    @Test
    fun `no fim do tier 5 a queda e de 1516 XP e o piso chega em cerca de 34 semanas`() {
        assertEquals(38_100L, Regressao.pisoDoTier(75_800, kanto))
        assertEquals(75_800L - 1_516L, Regressao.aplicar(75_800, semanas = 1, temporada = kanto))
        // 75.800 × 0,98^34 ≈ 38.150: ainda acima do piso. Na 35ª ele trava.
        assertTrue(Regressao.aplicar(75_800, semanas = 34, temporada = kanto) >= 38_100L)
        assertEquals(38_100L, Regressao.aplicar(75_800, semanas = 35, temporada = kanto))
        assertEquals(38_100L, Regressao.aplicar(75_800, semanas = 100, temporada = kanto))
    }

    @Test
    fun `quem acabou de passar para o tier seguinte ja tem o fim do anterior como piso`() {
        // 8.600 XP é o fim do tier 2; o XP seguinte passa a primeira espécie do tier 3.
        assertEquals(8_600L, Regressao.pisoDoTier(9_100, kanto))
    }

    // -----------------------------------------------------------------------
    // O replay com a regressão
    // -----------------------------------------------------------------------

    @Test
    fun `o replay aplica a queda ao fim e a posicao recua`() {
        val corridas = quatroCorridas()

        val sem = Replay.recalcular(corridas, emptyMap(), listOf(pequena), agora = dia(3)).porTemporada.getValue("t")
        val com = Replay.recalcular(corridas, emptyMap(), listOf(pequena), agora = dia(3 + 21)).porTemporada.getValue("t")

        assertEquals(1200L, sem.xpTotal)
        assertEquals(1176L, com.xpTotal)
        assertEquals(dia(3), com.ultimaCorridaEm)
    }

    @Test
    fun `a colecao nao recua durante a regressao, so a posicao`() {
        val longa = Replay.recalcular(quatroCorridas(), emptyMap(), listOf(pequena), agora = dia(3 + 700))
            .porTemporada.getValue("t")

        assertEquals(400L, longa.xpTotal)
        assertEquals(posicaoNaEscada(400, pequena), longa.especieAtual)
        assertEquals(7, longa.colecaoMax)
        assertTrue(longa.especieAtual < longa.colecaoMax)
    }

    @Test
    fun `uma corrida depois da regressao zera a contagem e a queda para`() {
        val corridas = quatroCorridas() + corrida("e", em = dia(3 + 22))

        val progresso = Replay.recalcular(corridas, emptyMap(), listOf(pequena), agora = dia(3 + 23))
            .porTemporada.getValue("t")

        // 1.200, menos 2% na lacuna de 22 dias, mais 300 da corrida nova. A lacuna de
        // 1 dia até hoje não tira nada.
        assertEquals(1176L + 300L, progresso.xpTotal)
        assertEquals(dia(3 + 22), progresso.ultimaCorridaEm)
    }

    @Test
    fun `a queda e dependente de caminho e cai entre as corridas, nao so no fim`() {
        val corridas = listOf(corrida("a", em = dia(0)), corrida("b", em = dia(60)))

        val progresso = Replay.recalcular(corridas, emptyMap(), listOf(pequena), agora = dia(60))
            .porTemporada.getValue("t")

        val queda = Regressao.aplicar(300, Regressao.semanasDeQueda(dia(0), dia(60)), pequena)
        assertEquals(queda + 300L, progresso.xpTotal)
        assertTrue(progresso.xpTotal < 600L)
    }

    @Test
    fun `correcao que cruza o piso atravessa, porque correcao e verdade`() {
        // Com as quatro corridas o corredor está no tier 2 (piso 400). Descartar três delas
        // deixa 300 XP, abaixo do piso: a correção não é a regressão e não é barrada (XP-09).
        val corridas = quatroCorridas().mapIndexed { i, c -> if (i > 0) c.copy(descartada = true) else c }

        val progresso = Replay.recalcular(
            corridas, emptyMap(), listOf(pequena), agora = dia(3), colecaoAnterior = mapOf("t" to 7),
        ).porTemporada.getValue("t")

        assertEquals(300L, progresso.xpTotal)
        assertEquals(3, progresso.especieAtual)
        // A coleção, essa, não recua nem na correção: ela é o máximo guardado (RN-38).
        assertEquals(7, progresso.colecaoMax)
    }

    @Test
    fun `o historico acompanha a queda semana a semana`() {
        val corridas = quatroCorridas()
        val resultado = Replay.recalcular(corridas, emptyMap(), listOf(pequena), agora = dia(3 + 50))

        fun semana(numero: Int, fim: Int) = Semana(
            numero = numero,
            dataInicio = dia(fim - 7),
            dataFim = dia(fim),
            sessoesAlvo = 3,
            kmAlvo = 10.0,
            longaoKm = 4.0,
            tipo = TipoDeSemana.entries.first(),
            parcial = false,
        )
        val grade = listOf(semana(1, 7), semana(2, 14), semana(3, 28), semana(4, 49))

        val historico = Replay.historico(pequena, corridas, resultado, mapOf("p" to grade), ate = dia(3 + 50))

        // Semanas 1 e 2 (7 e 14 dias): sem queda, posição 7. Semana 3 (28 dias, 25 desde
        // a última corrida): 1 queda. Semana 4 (49 dias, 46 desde a última): 4 quedas.
        val posicoes = historico.map { it.especie }
        assertEquals(posicaoNaEscada(1200, pequena), posicoes[0])
        assertEquals(posicaoNaEscada(1200, pequena), posicoes[1])
        assertEquals(
            posicaoNaEscada(Regressao.aplicar(1200, Regressao.semanasDeQueda(dia(3), dia(28)), pequena), pequena),
            posicoes[2],
        )
        assertEquals(
            posicaoNaEscada(Regressao.aplicar(1200, Regressao.semanasDeQueda(dia(3), dia(49)), pequena), pequena),
            posicoes[3],
        )
        assertTrue(posicoes[3] < posicoes[1])
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private val inicio = Instant.parse("2026-08-10T10:00:00Z")

    private fun dia(n: Int): Instant = inicio.plus(Duration.ofDays(n.toLong()))

    /** 3 km por corrida, a 6:00 por km, sem plano: 300 XP cada, sem bônus (pace e recorde empatam). */
    private fun corrida(id: String, em: Instant) = Corrida(
        id = id,
        dataHoraInicio = em,
        km = 3.0,
        duracaoSeg = 1080,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = null,
        temporadaId = "t",
    )

    private fun quatroCorridas() = (0..3).map { corrida("c$it", em = dia(it)) }

    private val pequena = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 8,
        tiers = listOf(
            Tier(1, "T1", "", listOf(1, 2, 3, 4), 100),
            Tier(2, "T2", "", listOf(5, 6, 7, 8), 250),
        ),
    )

    /** A tabela de docs/04 §4: 21, 26, 19, 25, 29, 15, 10 e 5 espécies. */
    private val kanto: Temporada = run {
        val custos = listOf(21 to 100, 26 to 250, 19 to 500, 25 to 800, 29 to 1300, 15 to 2200, 10 to 4000, 5 to 8000)
        var numero = 0
        Temporada(
            id = "kanto",
            nome = "Kanto",
            regiao = "Kanto",
            inicio = Instant.parse("2026-01-01T03:00:00Z"),
            fim = Instant.parse("2027-01-01T03:00:00Z"),
            fuso = ZoneId.of("America/Sao_Paulo"),
            ativa = true,
            totalEspecies = custos.sumOf { it.first },
            tiers = custos.mapIndexed { i, (quantas, xp) ->
                Tier(i + 1, "T${i + 1}", "", List(quantas) { ++numero }, xp)
            },
        )
    }
}
