package com.hggabriel.pokerun.ui.telas.detalhecorrida

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.regras.ProgressoNaTemporada
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A `RunDetailScreen` (`F2-T09`, docs/03 §3.13) na segunda linguagem (29/10/2026): o placar
 * com a distância em km e metros e o pace derivado, a espécie e as figurinhas que a corrida
 * deixou no álbum, os recordes que ela bateu e a medalha do ritmo.
 */
class DetalheDaCorridaTest {

    // -----------------------------------------------------------------------
    // O placar
    // -----------------------------------------------------------------------

    @Test
    fun `o pace e derivado da distancia e da duracao`() {
        // 12,40 km em 1:04:22 é 5:11 por km, o exemplo de docs/03 §3.13.1: ritmo de Ponyta.
        val estado = conteudo(corrida())

        assertEquals(311L, estado.paceSeg)
        assertEquals(Ritmo.PONYTA, estado.ritmo)
    }

    @Test
    fun `sem distancia nao ha pace nem medalha`() {
        val estado = conteudo(corrida(km = 0.0))

        assertNull(estado.paceSeg)
        assertNull(estado.ritmo)
    }

    @Test
    fun `a distancia sai em km e metros, sem virgula`() {
        assertEquals(12 to 400, partesDaDistancia(12.4))
        assertEquals(5 to 0, partesDaDistancia(5.0))
        assertEquals(0 to 800, partesDaDistancia(0.8))
        // Arredonda ao metro uma vez: nada de 12 km 1000 m.
        assertEquals(13 to 0, partesDaDistancia(12.9996))
    }

    @Test
    fun `o dia e o horario sao os do fuso do plano`() {
        // 01h em UTC do dia 15 ainda é 22h do dia 14 em São Paulo.
        val estado = conteudo(corrida(em = Instant.parse("2026-09-15T01:00:00Z")))

        assertEquals(LocalDate.of(2026, 9, 14), estado.dia)
        assertEquals(LocalTime.of(22, 0), estado.inicio)
        assertEquals(LocalTime.of(23, 4, 22), estado.fim)
    }

    @Test
    fun `o ano so aparece quando nao e o de hoje`() {
        assertTrue(!conteudo(corrida(), hoje = LocalDate.of(2026, 10, 29)).mostraAno)
        assertTrue(conteudo(corrida(), hoje = LocalDate.of(2027, 1, 2)).mostraAno)
    }

    @Test
    fun `corrida descartada continua visivel e marcada fora dos calculos`() {
        val estado = conteudo(corrida(descartada = true), historico = emptyList())

        assertTrue(estado.foraDosCalculos)
        assertTrue(estado.figurinhas.isEmpty())
    }

    @Test
    fun `corrida ou plano ausentes falham`() {
        assertEquals(DetalheCorridaUiState.Falhou, detalheDaCorrida(null, plano))
        assertEquals(DetalheCorridaUiState.Falhou, detalheDaCorrida(corrida(), null))
    }

    // -----------------------------------------------------------------------
    // O álbum: a espécie e as figurinhas da corrida
    // -----------------------------------------------------------------------

    @Test
    fun `a corrida mostra as figurinhas que colou e a ultima e a especie dela`() {
        // 1.000 XP por espécie: 2.000 leva à posição 2, 3.500 à 3.
        val a = corrida(id = "a", xp = 2_000, em = Instant.parse("2026-09-01T10:00:00Z"))
        val b = corrida(id = "b", xp = 1_500, em = Instant.parse("2026-09-03T10:00:00Z"))
        val estado = conteudo(b, historico = listOf(a, b), progresso = progresso(3_500))

        assertEquals(listOf(12), estado.figurinhas)
        assertEquals(12, estado.especie)
    }

    @Test
    fun `corrida que nao colou figurinha mostra a especie em que o corredor estava`() {
        val a = corrida(id = "a", xp = 2_000, em = Instant.parse("2026-09-01T10:00:00Z"))
        val c = corrida(id = "c", xp = 200, em = Instant.parse("2026-09-05T10:00:00Z"))
        val estado = conteudo(c, historico = listOf(a, c), progresso = progresso(2_200))

        assertTrue(estado.figurinhas.isEmpty())
        assertEquals(11, estado.especie)
    }

    @Test
    fun `sem temporada ou sem historico a tela mostra a corrida sem o album`() {
        val semTemporada = detalheDaCorrida(corrida(), plano, null, listOf(corrida())) as DetalheCorridaUiState.Conteudo
        val semHistorico = detalheDaCorrida(corrida(), plano, temporada, null) as DetalheCorridaUiState.Conteudo

        assertNull(semTemporada.especie)
        assertNull(semHistorico.especie)
        assertTrue(semHistorico.recordes.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Os recordes
    // -----------------------------------------------------------------------

    @Test
    fun `os recordes batidos aparecem quando a conta pagou por eles`() {
        val antiga = corrida(id = "a", km = 5.0, duracao = 1_800, em = Instant.parse("2026-09-01T10:00:00Z"))
        val nova = corrida(id = "b", conta = conta(recorde = 1_500))
        val estado = conteudo(nova, historico = listOf(antiga, nova))

        assertEquals(setOf(TipoDeRecorde.MELHOR_PACE, TipoDeRecorde.MAIOR_DISTANCIA, TipoDeRecorde.MAIOR_DURACAO), estado.recordes)
    }

    @Test
    fun `recorde sem bonus nao e marcado`() {
        // As três primeiras corridas estabelecem recorde sem pagar por ele (docs/04 §3.3).
        val nova = corrida(conta = conta(recorde = 0))

        assertTrue(conteudo(nova, historico = listOf(nova)).recordes.isEmpty())
    }

    // -----------------------------------------------------------------------
    // A medalha do ritmo
    // -----------------------------------------------------------------------

    @Test
    fun `a medalha fica mais expressiva quanto mais rapido o ritmo`() {
        // Ritmo.entries vai do mais rápido ao mais lento.
        val forcas = Ritmo.entries.reversed().map { forca(coreografiaDaMedalha(it)) }

        forcas.zipWithNext().forEach { (lenta, rapida) -> assertTrue("$forcas", rapida > lenta) }
    }

    @Test
    fun `o slowpoke pousa e o jolteon e batido como carimbo`() {
        assertEquals(EntradaDaMedalha.POUSO, coreografiaDaMedalha(Ritmo.SLOWPOKE).entrada)
        val jolteon = coreografiaDaMedalha(Ritmo.JOLTEON)
        assertEquals(EntradaDaMedalha.CARIMBO, jolteon.entrada)
        assertTrue(jolteon.impacto && jolteon.brilho && jolteon.faiscas > 0)
    }

    @Test
    fun `o sprite da medalha e o de Ruby e Sapphire, com Emerald de reserva`() {
        val fontes = spritesDoRitmo(77)

        assertTrue(fontes[0].endsWith("/generation-iii/ruby-sapphire/77.png"))
        assertTrue(fontes[1].endsWith("/generation-iii/emerald/77.png"))
    }

    private fun forca(c: CoreografiaDaMedalha): Int =
        c.entrada.ordinal + c.voltas + c.balancos + (if (c.brilho) 1 else 0) + (if (c.impacto) 1 else 0) + (if (c.faiscas > 0) 1 else 0)

    // -----------------------------------------------------------------------
    // A conta aberta soma na tela (docs/02 §9.2, item 5)
    // -----------------------------------------------------------------------

    private val ptBr = java.util.Locale.forLanguageTag("pt-BR")

    @Test
    fun `parcela inteira sai inteira`() {
        assertEquals("1.000", parcelaDaConta(1000.0, ptBr))
        // 4,6 km × 100 não é inteiro em ponto flutuante, e a tela não pode mostrar isso.
        assertEquals("460", parcelaDaConta(4.6 * 100, ptBr))
    }

    @Test
    fun `parcela fracionaria mostra a casa que tem, para a conta fechar`() {
        // A corrida de 10 km de 17/10 no aparelho: o bônus de longão era 232,5, a tela
        // mostrava +233, e 1.000 + 233 × 2,0 dava 2.466 contra o total de 2.465.
        assertEquals("232,5", parcelaDaConta(232.5, ptBr))
        val visivel = (1000.0 + "232,5".replace(",", ".").toDouble()) * 2.0
        assertEquals(2465L, Math.round(visivel))
    }

    // -----------------------------------------------------------------------

    private val fuso = ZoneId.of("America/Sao_Paulo")

    private val plano = Plano(
        id = "p1",
        nome = "São Silvestre",
        distanciaAlvoKm = 15.0,
        dataProva = Instant.parse("2026-12-31T11:00:00Z"),
        fuso = fuso,
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(
            dataProva = Instant.parse("2026-12-31T11:00:00Z"),
            distanciaAlvoKm = 15.0,
            baselineKm = 5.0,
            sessoesPorSemana = 3,
            curtaKm = 2.5,
        ),
    )

    private val temporada = Temporada(
        id = "kanto-2026",
        nome = "Kanto",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T00:00:00Z"),
        fim = Instant.parse("2026-12-31T23:59:59Z"),
        fuso = fuso,
        ativa = true,
        totalEspecies = 6,
        tiers = listOf(Tier(ordem = 1, nome = "Casulo", descricao = "", especies = listOf(10, 11, 12, 13, 14, 15), xpPorEspecie = 1_000)),
    )

    private fun progresso(xp: Long) = ProgressoNaTemporada(xp, 1, Instant.parse("2026-09-05T10:00:00Z"))

    private fun conta(recorde: Int) = ParcelasDeXp(base = 1240.0, longao = 0.0, pace = 0, multiplicador = 1.0, recorde = recorde)

    private fun corrida(
        id: String = "run-1",
        em: Instant = Instant.parse("2026-09-14T10:00:00Z"),
        km: Double = 12.4,
        duracao: Long = 3862,
        xp: Long = 0,
        conta: ParcelasDeXp? = null,
        descartada: Boolean = false,
    ) = Corrida(
        id = id,
        dataHoraInicio = em,
        km = km,
        duracaoSeg = duracao,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = 6,
        temporadaId = "kanto-2026",
        xpCreditado = xp,
        xpDetalhe = conta,
        descartada = descartada,
    )

    private fun conteudo(
        corrida: Corrida,
        historico: List<Corrida>? = null,
        progresso: ProgressoNaTemporada? = null,
        hoje: LocalDate = LocalDate.of(2026, 10, 29),
    ) = detalheDaCorrida(corrida, plano, temporada, historico, progresso, hoje) as DetalheCorridaUiState.Conteudo
}
