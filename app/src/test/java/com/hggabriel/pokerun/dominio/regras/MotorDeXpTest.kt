package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * A fórmula de XP de docs/04 §3 (`F2-T06`), escrita **antes** da implementação
 * (`EXECUCAO.md §3.2`). Os casos vêm de docs/06 §3 e da ficha da tarefa.
 *
 * ```
 * XP = (km × 100 + bônus_longão + bônus_pace) × multiplicador + bônus_recorde
 * ```
 *
 * A semana padrão prevê 3 sessões, 20 km e um longão de 10 km: as duas curtas têm
 * alvo de 5 km cada, metade do longão, como o gerador as monta.
 */
class MotorDeXpTest {

    private val delta = 1e-9

    // -----------------------------------------------------------------------
    // docs/04 §3.1 — sem termo de tempo, a lentidão não rende
    // -----------------------------------------------------------------------

    @Test
    fun `dez km a 6 por km rendem mais ou igual que a 8 por km, com qualquer media`() {
        listOf(null, 300.0, 420.0, 600.0).forEach { media ->
            val rapida = corrida(km = 10.0, duracaoSeg = 3600)
            val lenta = corrida(km = 10.0, duracaoSeg = 4800)

            val xpRapida = xp(rapida, semana(), listOf(rapida), media = media).total
            val xpLenta = xp(lenta, semana(), listOf(lenta), media = media).total

            assertTrue("média $media: $xpRapida contra $xpLenta", xpRapida >= xpLenta)
        }
    }

    // -----------------------------------------------------------------------
    // bônus_longão e multiplicador — o longão de 16 km contra quatro de 4 km
    // -----------------------------------------------------------------------

    @Test
    fun `o longao de 16 km rende mais que quatro corridas de 4 km`() {
        val semana = semana(sessoes = 4, kmAlvo = 40.0, longao = 16.0)

        val longao = corrida(km = 16.0, duracaoSeg = 5760)
        val xpDoLongao = xp(longao, semana, listOf(longao)).total

        val quatro = (0 until 4).map { corrida(km = 4.0, duracaoSeg = 1440, em = dia(it)) }
        val xpDasQuatro = quatro.sumOf { xp(it, semana, quatro).total }

        assertTrue("$xpDoLongao contra $xpDasQuatro", xpDoLongao > xpDasQuatro)
    }

    @Test
    fun `o bonus de longao paga 50 por km acima de 60 por cento do alvo`() {
        // Alvo de 10 km: 60% são 6 km. A corrida de 9 km tem 3 km acima.
        val detalhe = xp(corrida(km = 9.0, duracaoSeg = 3240), semana(), null)

        assertEquals(150.0, detalhe.longao, delta)
    }

    @Test
    fun `abaixo de 60 por cento do alvo o bonus de longao e zero, nunca negativo`() {
        val detalhe = xp(corrida(km = 5.0, duracaoSeg = 1800), semana(), null)

        assertEquals(0.0, detalhe.longao, delta)
    }

    @Test
    fun `semana sem longao previsto nao paga bonus de longao`() {
        val taper = semana(kmAlvo = 15.0, longao = null, tipo = TipoDeSemana.TAPER)

        val detalhe = xp(corrida(km = 12.0, duracaoSeg = 4320), taper, null)

        assertEquals(0.0, detalhe.longao, delta)
    }

    // -----------------------------------------------------------------------
    // XP-02 — bônus de recorde
    // -----------------------------------------------------------------------

    @Test
    fun `as tres primeiras corridas nao geram bonus de recorde, mesmo batendo os cinco`() {
        (1..3).forEach { ordem ->
            val corrida = corrida(km = 5.0, duracaoSeg = 1500, tempo1k = 280, tempo5k = 1500)

            val detalhe = xp(corrida, semana(), null, ordem = ordem, recordes = Recordes())

            assertEquals("corrida $ordem", 5, detalhe.recordesBatidos.size)
            assertEquals("corrida $ordem", 0, detalhe.recorde)
        }
    }

    @Test
    fun `a quarta corrida batendo tres recordes soma 1500 depois do multiplicador`() {
        val anteriores = Recordes(
            melhorPaceSegKm = 360.0,
            melhor1kSeg = 300,
            melhor5kSeg = 1700,
            maiorDistanciaKm = 10.0,
            maiorDuracaoSeg = 3600,
        )
        // 6 km em 35 min: pace de 350 s/km, 1 km em 290 s, 5 km em 1690 s.
        val corrida = corrida(km = 6.0, duracaoSeg = 2100, tempo1k = 290, tempo5k = 1690)

        val detalhe = xp(corrida, semana(), null, ordem = 4, recordes = anteriores)

        assertEquals(
            setOf(TipoDeRecorde.MELHOR_PACE, TipoDeRecorde.MELHOR_1K, TipoDeRecorde.MELHOR_5K),
            detalhe.recordesBatidos,
        )
        assertEquals(1500, detalhe.recorde)
        // Sozinha na semana, a corrida ocupa o longão: 6 km de 10 ficam abaixo de 90%,
        // então 1,5×. (600 + 0 + 0) × 1,5 + 1.500. Se o recorde entrasse antes do
        // multiplicador, daria 3.150.
        assertEquals(1.5, detalhe.multiplicador, delta)
        assertEquals(2400L, detalhe.total)
    }

    @Test
    fun `estabelecer o primeiro valor de um recorde conta como bater`() {
        // docs/04 §3.3: na primeira corrida os cinco recordes "cairiam de uma vez".
        val anteriores = veterano.copy(melhor5kSeg = null)
        val corrida = corrida(km = 5.0, duracaoSeg = 2000, tempo5k = 2000)

        val detalhe = xp(corrida, null, null, ordem = 4, recordes = anteriores)

        assertEquals(setOf(TipoDeRecorde.MELHOR_5K), detalhe.recordesBatidos)
        assertEquals(500, detalhe.recorde)
    }

    @Test
    fun `corrida de menos de 1 km nao bate o recorde de pace`() {
        // docs/04 §3.3: senão um tiro de 300 m vira o recorde.
        val corrida = corrida(km = 0.5, duracaoSeg = 100)

        val detalhe = xp(corrida, null, null, ordem = 5, recordes = veterano)

        assertFalse(TipoDeRecorde.MELHOR_PACE in detalhe.recordesBatidos)
    }

    // -----------------------------------------------------------------------
    // XP-05 — corrida avulsa
    // -----------------------------------------------------------------------

    @Test
    fun `corrida avulsa nao recebe bonus de longao e fica em 1x`() {
        val avulsa = corrida(km = 16.0, duracaoSeg = 5760, semanaRef = null)

        val detalhe = xp(avulsa, null, null)

        assertEquals(0.0, detalhe.longao, delta)
        assertEquals(1.0, detalhe.multiplicador, delta)
        assertEquals(1600L, detalhe.total)
    }

    // -----------------------------------------------------------------------
    // XP-03 e docs/04 §3.2 — o multiplicador vem da sessão reivindicada
    // -----------------------------------------------------------------------

    @Test
    fun `reivindicar uma sessao e cumprir a distancia vale 2x`() {
        val corrida = corrida(km = 5.0, duracaoSeg = 1800, sessao = SessaoReivindicada.Curta(1))

        assertEquals(2.0, xp(corrida, semana(), null).multiplicador, delta)
    }

    @Test
    fun `a 92 por cento do alvo ainda vale 2x`() {
        val corrida = corrida(km = 4.6, duracaoSeg = 1656, sessao = SessaoReivindicada.Curta(1))

        assertEquals(2.0, xp(corrida, semana(), null).multiplicador, delta)
    }

    @Test
    fun `abaixo de 90 por cento do alvo vale 1,5x`() {
        val corrida = corrida(km = 4.4, duracaoSeg = 1584, sessao = SessaoReivindicada.Curta(1))

        assertEquals(1.5, xp(corrida, semana(), null).multiplicador, delta)
    }

    @Test
    fun `a quarta corrida da semana de tres previstas, a menor delas, fica em 1x`() {
        val corridas = listOf(
            corrida(km = 6.0, duracaoSeg = 2160, em = dia(0)),
            corrida(km = 5.0, duracaoSeg = 1800, em = dia(1)),
            corrida(km = 4.5, duracaoSeg = 1620, em = dia(2)),
            corrida(km = 3.0, duracaoSeg = 1080, em = dia(3)),
        )

        assertEquals(1.0, xp(corridas[3], semana(), corridas).multiplicador, delta)
    }

    @Test
    fun `a quarta corrida, a maior delas, toma o longao e a mais recente das outras sobra em 1x`() {
        val corridas = listOf(
            corrida(km = 5.0, duracaoSeg = 1800, em = dia(0)),
            corrida(km = 4.0, duracaoSeg = 1440, em = dia(1)),
            corrida(km = 4.5, duracaoSeg = 1620, em = dia(2)),
            corrida(km = 9.0, duracaoSeg = 3240, em = dia(3)),
        )

        val maior = xp(corridas[3], semana(), corridas)
        assertEquals(2.0, maior.multiplicador, delta)
        assertEquals(150.0, maior.longao, delta)

        assertEquals(1.0, xp(corridas[2], semana(), corridas).multiplicador, delta)
        assertEquals(2.0, xp(corridas[0], semana(), corridas).multiplicador, delta)
        assertEquals(1.5, xp(corridas[1], semana(), corridas).multiplicador, delta)
    }

    @Test
    fun `a reivindicacao gravada pelo usuario nunca e deslocada pela atribuicao automatica`() {
        // A de 10 km foi marcada pelo usuário como curta: fica na curta, sem bônus de
        // longão. A de 9,5 km, sem marca, é a maior livre e toma o longão.
        val corridas = listOf(
            corrida(km = 10.0, duracaoSeg = 3600, em = dia(0), sessao = SessaoReivindicada.Curta(1)),
            corrida(km = 9.5, duracaoSeg = 3420, em = dia(1)),
        )

        val marcada = xp(corridas[0], semana(), corridas)
        assertEquals(0.0, marcada.longao, delta)
        assertEquals(2.0, marcada.multiplicador, delta)

        val livre = xp(corridas[1], semana(), corridas)
        assertEquals(175.0, livre.longao, delta)
        assertEquals(2.0, livre.multiplicador, delta)
    }

    // -----------------------------------------------------------------------
    // bônus_pace
    // -----------------------------------------------------------------------

    @Test
    fun `bater a media movel de pace vale 200`() {
        val corrida = corrida(km = 5.0, duracaoSeg = 1950, semanaRef = null)

        assertEquals(200, xp(corrida, null, null, media = 400.0).pace)
    }

    @Test
    fun `empatar com a media movel nao vale bonus de pace`() {
        val corrida = corrida(km = 5.0, duracaoSeg = 2000, semanaRef = null)

        assertEquals(0, xp(corrida, null, null, media = 400.0).pace)
    }

    @Test
    fun `sem media movel nao ha bonus de pace`() {
        val corrida = corrida(km = 5.0, duracaoSeg = 1500, semanaRef = null)

        assertEquals(0, xp(corrida, null, null, media = null).pace)
    }

    // -----------------------------------------------------------------------
    // docs/04 §4 — a primeira corrida
    // -----------------------------------------------------------------------

    @Test
    fun `a primeira corrida de 5 km cumprindo uma sessao vale 1000 XP`() {
        // 5 × 100 × 2,0. Numa semana sem longão, as três curtas têm alvo de 5 km.
        val semana = semana(kmAlvo = 15.0, longao = null, tipo = TipoDeSemana.TAPER)
        val corrida = corrida(km = 5.0, duracaoSeg = 1800)

        val detalhe = xp(corrida, semana, null, ordem = 1, recordes = Recordes())

        assertEquals(1000L, detalhe.total)
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    /** Recordes fortes o bastante para nenhuma corrida de teste bater. */
    private val veterano = Recordes(
        melhorPaceSegKm = 200.0,
        melhor1kSeg = 150,
        melhor5kSeg = 900,
        maiorDistanciaKm = 50.0,
        maiorDuracaoSeg = 20_000,
    )

    private val inicio = Instant.parse("2026-08-10T03:00:00Z")

    private fun dia(n: Int): Instant = inicio.plusSeconds(n * 86_400L + 7 * 3_600L)

    private fun semana(
        sessoes: Int = 3,
        kmAlvo: Double = 20.0,
        longao: Double? = 10.0,
        tipo: TipoDeSemana = TipoDeSemana.BUILD,
    ) = Semana(
        numero = 1,
        dataInicio = inicio,
        dataFim = inicio.plusSeconds(7 * 86_400L),
        sessoesAlvo = sessoes,
        kmAlvo = kmAlvo,
        longaoKm = longao,
        tipo = tipo,
        parcial = false,
    )

    private var proximoId = 1

    private fun corrida(
        km: Double,
        duracaoSeg: Long,
        em: Instant = dia(0),
        semanaRef: Int? = 1,
        sessao: SessaoReivindicada? = null,
        tempo1k: Long? = null,
        tempo5k: Long? = null,
    ) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = em,
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-teste",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
        sessaoReivindicada = sessao,
        tempo1kSeg = tempo1k,
        tempo5kSeg = tempo5k,
    )

    /** [daSemana] nulo é a corrida sozinha na semana. */
    private fun xp(
        corrida: Corrida,
        semana: Semana?,
        daSemana: List<Corrida>?,
        ordem: Int = 10,
        recordes: Recordes = veterano,
        media: Double? = null,
    ): DetalheDeXp = MotorDeXp.xpDaCorrida(
        corrida,
        ContextoDeXp(
            ordemNoHistorico = ordem,
            recordesAnteriores = recordes,
            mediaMovelPaceSegKm = media,
            semana = semana,
            corridasDaSemana = daSemana ?: listOf(corrida),
        ),
    )
}
