package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * O replay cronológico de docs/05 §2.5 (`F2-T07`), escrito **antes** da implementação
 * (`EXECUCAO.md §3.2`). Casos de docs/06 §3 e da ficha: RN-33, XP-01, XP-10, RN-39.
 *
 * O XP é dependente de caminho (docs/04 §6): a média móvel, os recordes e a atribuição
 * da semana mudam o XP de corridas que ninguém editou. Cada teste edita uma corrida e
 * olha **outra**.
 *
 * A grade de teste tem três semanas de 3 sessões, 20 km e longão de 10 km (curtas de
 * 5 km). A temporada tem 20 espécies: 10 a 100 XP e 10 a 250 XP.
 */
class ReplayTest {

    // -----------------------------------------------------------------------
    // XP-10 — idempotência
    // -----------------------------------------------------------------------

    @Test
    fun `o replay rodado duas vezes da o mesmo resultado`() {
        val corridas = historico()

        val primeiro = recalcular(corridas)
        // A segunda volta lê as corridas com o `xp_creditado` que a primeira gravou.
        val gravadas = corridas.map { it.copy(xpCreditado = primeiro.porCorrida[it.id]?.total ?: 0L) }
        val segundo = recalcular(gravadas)

        assertTrue(primeiro.porCorrida.isNotEmpty())
        assertEquals(primeiro, segundo)
    }

    @Test
    fun `a ordem em que as corridas chegam nao muda o resultado`() {
        val corridas = historico()

        assertEquals(recalcular(corridas), recalcular(corridas.reversed()))
    }

    // -----------------------------------------------------------------------
    // RN-33 — editar a corrida N recalcula as seguintes
    // -----------------------------------------------------------------------

    @Test
    fun `editar a corrida N recalcula o bonus de pace de N+1`() {
        val a = corrida("a", km = 5.0, duracaoSeg = 1500, em = dia(0))
        val b = corrida("b", km = 5.0, duracaoSeg = 1600, em = dia(1))
        val c = corrida("c", km = 5.0, duracaoSeg = 1550, em = dia(8))

        // Média das anteriores a `c`: 3.100 s em 10 km, 310 s/km. `c` empata e não ganha.
        assertEquals(0, recalcular(listOf(a, b, c)).porCorrida.getValue("c").pace)

        // `b` corrigida para 1.700 s: a média vira 320 s/km e `c` passa a batê-la.
        val editadas = listOf(a, b.copy(substituida = true), corrigida(b, "b2", duracaoSeg = 1700), c)
        assertEquals(200, recalcular(editadas).porCorrida.getValue("c").pace)
    }

    @Test
    fun `editar a corrida que detinha um recorde migra os 500 para quem passa a dete-lo`() {
        val calibracao = (0 until 3).map { corrida("r$it", km = 5.0, duracaoSeg = 1800, em = dia(it)) }
        val r4 = corrida("r4", km = 8.0, duracaoSeg = 2880, em = dia(7))
        val r5 = corrida("r5", km = 7.0, duracaoSeg = 2520, em = dia(9))

        val antes = recalcular(calibracao + r4 + r5).porCorrida
        assertEquals(1000, antes.getValue("r4").recorde)
        assertEquals(0, antes.getValue("r5").recorde)

        // `r4` tinha 4 km, não 8. Maior distância e maior duração passam para `r5`.
        val depois = recalcular(
            calibracao + r4.copy(substituida = true) + corrigida(r4, "r4b", km = 4.0, duracaoSeg = 1440) + r5,
        ).porCorrida
        assertEquals(0, depois.getValue("r4b").recorde)
        assertEquals(1000, depois.getValue("r5").recorde)
    }

    @Test
    fun `corrida gravada no passado recalcula os bonus das seguintes`() {
        val calibracao = (0 until 3).map { corrida("r$it", km = 5.0, duracaoSeg = 1800, em = dia(1 + it)) }
        val r4 = corrida("r4", km = 8.0, duracaoSeg = 2880, em = dia(8))
        assertEquals(1000, recalcular(calibracao + r4).porCorrida.getValue("r4").recorde)

        // Um registro retroativo de 10 km na segunda da semana 1 vira o dono dos recordes.
        val antiga = corrida("antiga", km = 10.0, duracaoSeg = 3600, em = dia(0))
        assertEquals(0, recalcular(calibracao + r4 + antiga).porCorrida.getValue("r4").recorde)
    }

    @Test
    fun `uma corrida maior na quinta refaz o multiplicador das anteriores da semana`() {
        val segunda = corrida("seg", km = 4.6, duracaoSeg = 1656, em = dia(0))
        // Sozinha, a de 4,6 km ocupa o longão de 10 km e fica abaixo de 90%: 1,5×.
        assertEquals(1.5, recalcular(listOf(segunda)).porCorrida.getValue("seg").multiplicador, 1e-9)

        // A de 9,5 km na quinta toma o longão. A de segunda cai na curta de 5 km e a cumpre.
        val quinta = corrida("qui", km = 9.5, duracaoSeg = 3420, em = dia(3))
        assertEquals(2.0, recalcular(listOf(segunda, quinta)).porCorrida.getValue("seg").multiplicador, 1e-9)
    }

    // -----------------------------------------------------------------------
    // RN-31 e RN-24 — descartada e substituída saem dos cálculos
    // -----------------------------------------------------------------------

    @Test
    fun `corrida descartada sai de todos os calculos`() {
        val valida = corrida("v", km = 5.0, duracaoSeg = 1800, em = dia(0))
        val fantasma = corrida("f", km = 30.0, duracaoSeg = 9000, em = dia(1), descartada = true)

        val comFantasma = recalcular(listOf(valida, fantasma))
        val sem = recalcular(listOf(valida))

        assertFalse("f" in comFantasma.porCorrida)
        assertEquals(sem.xpPorSemana, comFantasma.xpPorSemana)
        assertEquals(sem.porTemporada, comFantasma.porTemporada)
        assertEquals(sem.recordes, comFantasma.recordes)
    }

    @Test
    fun `corrida substituida sai e so o registro novo entra`() {
        val original = corrida("o", km = 8.0, duracaoSeg = 2880, em = dia(0))
        val resultado = recalcular(
            listOf(original.copy(substituida = true), corrigida(original, "novo", km = 6.0, duracaoSeg = 2160)),
        )

        assertEquals(setOf("novo"), resultado.porCorrida.keys)
        assertEquals(6.0, resultado.recordes.maiorDistanciaKm!!, 1e-9)
    }

    // -----------------------------------------------------------------------
    // Agregados: xp_semana e a temporada
    // -----------------------------------------------------------------------

    @Test
    fun `xp_semana soma o XP das corridas validas da semana`() {
        val corridas = historico()
        val resultado = recalcular(corridas)

        val daSemana1 = corridas.filter { it.semanaRef == 1 }.sumOf { resultado.porCorrida.getValue(it.id).total }
        assertEquals(daSemana1, resultado.xpPorSemana.getValue("p1_1"))
    }

    @Test
    fun `xp_total da temporada soma o XP das corridas dela, e a ultima corrida e a mais recente`() {
        val corridas = historico()
        val resultado = recalcular(corridas)
        val progresso = resultado.porTemporada.getValue("kanto-2026")

        assertEquals(resultado.porCorrida.values.sumOf { it.total }, progresso.xpTotal)
        assertEquals(corridas.maxOf { it.dataHoraInicio }, progresso.ultimaCorridaEm)
    }

    @Test
    fun `corrida de outra temporada credita la e nao aqui`() {
        // RN-40: o `temporada_id` gravado decide, e cada temporada soma só as suas.
        val desta = corrida("d", km = 5.0, duracaoSeg = 1800, em = dia(0))
        val daProxima = corrida("p", km = 5.0, duracaoSeg = 1800, em = dia(1), temporada = "kanto-2027")

        val resultado = recalcular(listOf(desta, daProxima), listOf(temporada, temporada.copy(id = "kanto-2027")))

        assertEquals(resultado.porCorrida.getValue("d").total, resultado.porTemporada.getValue("kanto-2026").xpTotal)
        assertEquals(resultado.porCorrida.getValue("p").total, resultado.porTemporada.getValue("kanto-2027").xpTotal)
    }

    @Test
    fun `correcao que reduz o XP faz a especie atual recuar`() {
        val longa = corrida("l", km = 9.0, duracaoSeg = 3240, em = dia(0))
        val antes = recalcular(listOf(longa)).porTemporada.getValue("kanto-2026").especieAtual

        val depois = recalcular(listOf(longa.copy(substituida = true), corrigida(longa, "l2", km = 3.0, duracaoSeg = 1080)))
            .porTemporada.getValue("kanto-2026").especieAtual

        assertTrue("$antes → $depois", depois < antes)
    }

    // -----------------------------------------------------------------------
    // A posição na escada (D-15, docs/04 §4 e §5.3)
    // -----------------------------------------------------------------------

    @Test
    fun `todo corredor comeca no 001`() {
        assertEquals(1, posicaoNaEscada(0, temporada))
        assertEquals(1, posicaoNaEscada(99, temporada))
    }

    @Test
    fun `mil XP passam dez especies do tier 1`() {
        // docs/04 §4: 1.000 XP = 10 espécies do Tier 1 de uma vez.
        assertEquals(10, posicaoNaEscada(1000, temporada))
    }

    @Test
    fun `o custo da especie e o do tier dela`() {
        // Tier 1 inteiro custa 1.000; a 11ª espécie custa 250.
        assertEquals(10, posicaoNaEscada(1249, temporada))
        assertEquals(11, posicaoNaEscada(1250, temporada))
    }

    @Test
    fun `a posicao nunca passa do total de especies`() {
        assertEquals(temporada.totalEspecies, posicaoNaEscada(1_000_000, temporada))
    }

    // -----------------------------------------------------------------------
    // O intervalo do replay limita o que se grava
    // -----------------------------------------------------------------------

    @Test
    fun `as escritas cobrem so as corridas do intervalo e as semanas delas`() {
        val corridas = historico()
        val resultado = recalcular(corridas)
        val desde = semanaInicio(2)

        val escritas = Replay.escritas(corridas, resultado, mapOf("p1" to grade), desde, dia(30))

        val esperadas = corridas.filter { it.dataHoraInicio >= desde }.map { it.id }.toSet()
        assertEquals(esperadas, escritas.corridas.keys)
        assertEquals(setOf("p1_2", "p1_3"), escritas.semanas.keys)
    }

    @Test
    fun `a semana que perdeu sua unica corrida por descarte e regravada com zero`() {
        val unica = corrida("u", km = 5.0, duracaoSeg = 1800, em = dia(8), descartada = true)

        val escritas = Replay.escritas(listOf(unica), recalcular(listOf(unica)), mapOf("p1" to grade), semanaInicio(2), dia(30))

        assertEquals(setOf("p1_2"), escritas.semanas.keys)
        assertEquals(0L, escritas.semanas.getValue("p1_2").xpSemana)
        assertTrue(escritas.corridas.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private val inicio = Instant.parse("2026-08-10T03:00:00Z")

    private fun dia(n: Int): Instant = inicio.plusSeconds(n * 86_400L + 7 * 3_600L)

    private fun semanaInicio(numero: Int): Instant = inicio.plusSeconds((numero - 1) * 7 * 86_400L)

    private val grade: List<Semana> = (1..3).map { n ->
        Semana(
            numero = n,
            dataInicio = semanaInicio(n),
            dataFim = semanaInicio(n + 1),
            sessoesAlvo = 3,
            kmAlvo = 20.0,
            longaoKm = 10.0,
            tipo = TipoDeSemana.BUILD,
            parcial = false,
        )
    }

    private val temporada = Temporada(
        id = "kanto-2026",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 20,
        tiers = listOf(
            Tier(ordem = 2, nome = "Dois", descricao = "", especies = (11..20).toList(), xpPorEspecie = 250),
            Tier(ordem = 1, nome = "Um", descricao = "", especies = (1..10).toList(), xpPorEspecie = 100),
        ),
    )

    /** Seis corridas espalhadas pelas três semanas, com recordes e médias em jogo. */
    private fun historico(): List<Corrida> = listOf(
        corrida("h1", km = 5.0, duracaoSeg = 1800, em = dia(0)),
        corrida("h2", km = 9.0, duracaoSeg = 3150, em = dia(2)),
        corrida("h3", km = 4.0, duracaoSeg = 1400, em = dia(8)),
        corrida("h4", km = 6.0, duracaoSeg = 2040, em = dia(10)),
        corrida("h5", km = 10.5, duracaoSeg = 3600, em = dia(15)),
        corrida("h6", km = 5.0, duracaoSeg = 1650, em = dia(17)),
    )

    private fun corrida(
        id: String,
        km: Double,
        duracaoSeg: Long,
        em: Instant,
        descartada: Boolean = false,
        temporada: String = "kanto-2026",
    ) = Corrida(
        id = id,
        dataHoraInicio = em,
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = grade.firstOrNull { em >= it.dataInicio && em < it.dataFim }?.numero,
        temporadaId = temporada,
        descartada = descartada,
    )

    /** O registro novo de RN-24: mesma data, medição corrigida, apontando para o antigo. */
    private fun corrigida(original: Corrida, id: String, km: Double = original.km, duracaoSeg: Long) =
        original.copy(id = id, km = km, duracaoSeg = duracaoSeg, substituiRunId = original.id, substituida = false)

    private fun recalcular(
        corridas: List<Corrida>,
        temporadas: List<Temporada> = listOf(temporada),
    ): ResultadoDoReplay = Replay.recalcular(corridas, mapOf("p1" to grade), temporadas)
}
