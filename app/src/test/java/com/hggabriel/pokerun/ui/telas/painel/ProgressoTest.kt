package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.Ritmo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * As contas do Progresso redesenhado (estudo de Progresso e Perfil, F3 e Revisão 3): a
 * estrada da temporada, a pílula da prova, o cartão do plano, a sequência, o ritmo e o Você
 * sabia. O dado de base é o de 27/10/2026: 10 km em 11/10 e 3 km em 15/10, a São Silvestre
 * de 15 km em 31/12.
 */
class ProgressoTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val fusos = mapOf("p" to fuso)
    private val hoje = LocalDate.of(2026, 10, 27)

    /** Escada de teste: 30 espécies de 100 XP, números 101 a 130. */
    private val kanto = Temporada(
        "kanto-2026", "Kanto 2026", "kanto",
        Instant.parse("2026-01-01T03:00:00Z"), Instant.parse("2027-01-01T03:00:00Z"),
        fuso, true, 30, listOf(Tier(1, "T1", "", (101..130).toList(), 100)),
    )

    private val reais = listOf(
        corrida("a", "2026-10-11T09:00", 10.0, 6600, xp = 2400),
        corrida("b", "2026-10-15T19:30", 3.0, 1440, xp = 300),
    )

    // ---------------------------------------------------------------------
    // EstradaDaTemporada
    // ---------------------------------------------------------------------

    @Test
    fun `a estrada vai da semana da primeira corrida ate a semana da prova`() {
        val estrada = estradaDaTemporada(reais, kanto, plano(), hoje, fuso)

        assertEquals(LocalDate.of(2026, 10, 5), estrada.semanas.first().inicio)
        assertEquals(LocalDate.of(2026, 12, 28), estrada.semanas.last().inicio)
        assertEquals(LocalDate.of(2026, 10, 11), estrada.inicio)
        assertEquals(LocalDate.of(2026, 12, 31), estrada.fim)
        assertTrue(estrada.comProva)
        assertEquals(13.0, estrada.kmNaTemporada, 0.001)
    }

    @Test
    fun `o km de cada semana e o acumulado ate ela e hoje e a semana de 26 de outubro`() {
        val estrada = estradaDaTemporada(reais, kanto, plano(), hoje, fuso)

        assertEquals(listOf(10.0, 13.0, 13.0, 13.0), estrada.semanas.take(4).map { it.kmAcumulado })
        assertEquals(LocalDate.of(2026, 10, 26), estrada.semanas[estrada.indiceDeHoje].inicio)
    }

    @Test
    fun `os rivais sao as especies ultrapassadas na semana em que foram passadas`() {
        // 2.400 XP na semana 0: posição 24 (a de `posicaoNaEscada`), passou as posições 1 a
        // 23 (números 101 a 123). +300 XP na semana 1: posição 27, passou 124 a 126.
        val estrada = estradaDaTemporada(reais, kanto, plano(), hoje, fuso)

        assertEquals(listOf(0, 1), estrada.rivais.map { it.indice })
        assertEquals((101..123).toList(), estrada.rivais[0].especies)
        assertEquals(listOf(124, 125, 126), estrada.rivais[1].especies)
        assertEquals(27, estrada.semanas.last().posicao)
    }

    @Test
    fun `sem plano a estrada termina no fim da temporada sem bandeira`() {
        val estrada = estradaDaTemporada(reais, kanto, planoAtivo = null, hoje = hoje, fuso = fuso)

        assertEquals(LocalDate.of(2026, 12, 31), estrada.fim)
        assertFalse(estrada.comProva)
    }

    @Test
    fun `sem corrida o parceiro fica parado na largada de hoje`() {
        val estrada = estradaDaTemporada(emptyList(), kanto, plano(), hoje, fuso)

        assertEquals(0, estrada.indiceDeHoje)
        assertEquals(0.0, estrada.kmNaTemporada, 0.0)
        assertTrue(estrada.rivais.isEmpty())
        assertEquals(LocalDate.of(2026, 10, 26), estrada.semanas.first().inicio)
    }

    @Test
    fun `corrida de outra temporada e corrida descartada nao andam na estrada`() {
        val corridas = reais + corrida("c", "2025-12-30T09:00", 20.0, 7200, temporada = "kanto-2025") +
            corrida("d", "2026-10-20T09:00", 50.0, 18000).copy(descartada = true)

        assertEquals(13.0, estradaDaTemporada(corridas, kanto, plano(), hoje, fuso).kmNaTemporada, 0.001)
    }

    // ---------------------------------------------------------------------
    // A pílula (decisão 17)
    // ---------------------------------------------------------------------

    @Test
    fun `abaixo da prova a pilula diz a parte dela`() {
        assertEquals(PilulaDaEstrada.PartesDaProva(87, 15.0), pilulaDaEstrada(13.0, 15.0))
    }

    @Test
    fun `a partir da prova a pilula diz quantas provas com uma casa`() {
        assertEquals(PilulaDaEstrada.ProvasInteiras(2.3, 15.0), pilulaDaEstrada(35.0, 15.0))
        assertEquals(PilulaDaEstrada.ProvasInteiras(1.0, 15.0), pilulaDaEstrada(15.0, 15.0))
    }

    @Test
    fun `sem plano a pilula compara com a distancia classica logo acima`() {
        assertEquals(PilulaDaEstrada.PartesDaClassica(60, DistanciaClassica.CINCO), pilulaDaEstrada(3.0, null))
        assertEquals(PilulaDaEstrada.PartesDaClassica(62, DistanciaClassica.MEIA), pilulaDaEstrada(13.0, null))
        assertEquals(PilulaDaEstrada.PartesDaClassica(0, DistanciaClassica.CINCO), pilulaDaEstrada(0.0, null))
        assertEquals(PilulaDaEstrada.Maratonas(1.4), pilulaDaEstrada(60.0, null))
    }

    // ---------------------------------------------------------------------
    // O cartão do plano
    // ---------------------------------------------------------------------

    @Test
    fun `o cartao da prova usa o plano ativo e a semana corrente`() {
        val grade = listOf(
            semana(1, "2026-10-19", 10.0, 4, TipoDeSemana.BUILD),
            semana(2, "2026-10-26", 11.3, 4, TipoDeSemana.BUILD),
            semana(3, "2026-12-28", null, 1, TipoDeSemana.PROVA),
        )
        val semanais = mapOf(2 to AgregadoDaSemana("p", 0, 1, 5.0, 5.0, 25.0, false))

        val prova = provaDoPlano(plano(), grade, semanais, Recordes(maiorDistanciaKm = 10.0), reais, hoje)

        assertEquals("São Silvestre", prova.nome)
        assertEquals(65L, prova.dias)
        assertEquals(10.0, prova.maiorCorridaKm, 0.0)
        assertEquals(1, prova.sessoesFeitas)
        assertEquals(4, prova.sessoesAlvo)
        assertEquals(listOf(false, true, false), prova.colunas.map { it.corrente })
        assertEquals(15.0, prova.colunas.last().alvoKm!!, 0.0)
        assertTrue(prova.colunas.last().prova)
        assertEquals(5.0, prova.colunas[1].feitoKm, 0.0)
    }

    // ---------------------------------------------------------------------
    // SequenciaTest: semanas seguidas com corrida
    // ---------------------------------------------------------------------

    @Test
    fun `a sequencia real de 27 de outubro e zero agora e duas no melhor`() {
        assertEquals(Sequencia(atual = 0, melhor = 2), sequenciaDeSemanas(reais, fusos, hoje))
    }

    @Test
    fun `a semana corrente sem corrida ainda nao quebra a sequencia`() {
        val corridas = reais + corrida("c", "2026-10-21T09:00", 5.0, 1800)

        assertEquals(Sequencia(atual = 3, melhor = 3), sequenciaDeSemanas(corridas, fusos, hoje))
    }

    @Test
    fun `semana parada no meio parte a sequencia em duas`() {
        val corridas = listOf(
            corrida("a", "2026-09-01T09:00", 5.0, 1800),
            corrida("b", "2026-09-08T09:00", 5.0, 1800),
            corrida("c", "2026-09-15T09:00", 5.0, 1800),
            corrida("d", "2026-09-29T09:00", 5.0, 1800),
        )

        assertEquals(Sequencia(atual = 0, melhor = 3), sequenciaDeSemanas(corridas, fusos, hoje))
        assertEquals(Sequencia(0, 0), sequenciaDeSemanas(emptyList(), fusos, hoje))
    }

    // ---------------------------------------------------------------------
    // RitmoDaTendenciaTest
    // ---------------------------------------------------------------------

    @Test
    fun `sem media movel o ritmo e o da ultima corrida e cada corrida e uma marca`() {
        val ritmo = ritmoDaTendencia(reais, PaceMovel.Faltam(8))

        assertEquals(listOf(660L, 480L), ritmo.marcas)
        assertEquals(480L, ritmo.atualSeg)
        assertEquals(Ritmo.PSYDUCK, ritmo.ritmo)
        assertFalse(ritmo.daMedia)
    }

    @Test
    fun `com media movel o numero passa a ser a media`() {
        val pace = PaceMovel.Serie(listOf(PontoDePace(LocalDate.of(2026, 10, 26), 432.0)))

        val ritmo = ritmoDaTendencia(reais, pace)

        assertEquals(432L, ritmo.atualSeg)
        assertEquals(Ritmo.PIKACHU, ritmo.ritmo)
        assertTrue(ritmo.daMedia)
    }

    @Test
    fun `sem corrida nao ha ritmo`() {
        val ritmo = ritmoDaTendencia(emptyList(), PaceMovel.Faltam(8))

        assertNull(ritmo.atualSeg)
        assertNull(ritmo.ritmo)
    }

    // ---------------------------------------------------------------------
    // Abaixo do mínimo, as pílulas que já existem (Revisão 3)
    // ---------------------------------------------------------------------

    @Test
    fun `abaixo do minimo os tres graficos levam as pilulas que ja existem`() {
        val volume = volumeAcumulado(reais, listOf(plano()), hoje) as VolumeAcumulado.Faltam
        val pace = paceEmMediaMovel(reais, fusos, hoje) as PaceMovel.Faltam
        val comFc = reais.map { it.copy(fcMedia = 150) }
        val eficiencia = eficienciaCardiaca(comFc) as EficienciaCardiaca.Faltam

        assertEquals(1, volume.semanas)
        assertEquals(listOf(10.0, 13.0, 13.0, 13.0), volume.parciais.map { it.kmAcumulado })
        assertEquals(listOf(660.0, 480.0), pace.parciais.map { it.paceSegKm })
        assertEquals(6, eficiencia.corridas)
        assertEquals(2, eficiencia.parciais.size)
    }

    // ---------------------------------------------------------------------
    // Os blocos da raiz
    // ---------------------------------------------------------------------

    @Test
    fun `o progresso real junta estrada, pilula, portas e fatos`() {
        val conteudo = montarProgresso(
            EntradaDoProgresso(
                corridas = reais,
                planos = listOf(plano()),
                recordes = Recordes(melhorPaceSegKm = 480.0, maiorDistanciaKm = 10.0, maiorDuracaoSeg = 6600),
                temporada = kanto,
                catalogo = listOf(kanto),
                planoAtivo = plano(),
                grade = emptyList(),
                semanais = emptyMap(),
                parceiro = 29,
                tipoDoParceiro = null,
                hoje = hoje,
            ),
        )

        assertEquals(PilulaDaEstrada.PartesDaProva(87, 15.0), conteudo.pilula)
        assertEquals(2, conteudo.corridas)
        assertEquals(13.0, conteudo.kmNaVida, 0.001)
        assertEquals(3, conteudo.fitas.count { !it.vaga })
        assertNull(conteudo.prova)
        assertEquals(29, conteudo.parceiro)
        assertTrue(conteudo.curiosidades.isNotEmpty())
    }

    // ---------------------------------------------------------------------

    private fun corrida(
        id: String,
        quando: String,
        km: Double,
        duracao: Long,
        xp: Long = 0,
        temporada: String = "kanto-2026",
    ) = Corrida(
        id = id,
        dataHoraInicio = ZonedDateTime.of(java.time.LocalDateTime.parse(quando), fuso).toInstant(),
        km = km,
        duracaoSeg = duracao,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = null,
        temporadaId = temporada,
        xpCreditado = xp,
        xpDetalhe = ParcelasDeXp(km * 100, 0.0, 0, 1.0, 0),
    )

    private fun plano(): Plano {
        val prova = ZonedDateTime.of(2026, 12, 31, 0, 0, 0, 0, fuso).toInstant()
        return Plano("p", "São Silvestre", 15.0, prova, fuso, "u", "X", false, ParametrosDeGeracao(prova, 15.0, 10.0, 4, 3.0))
    }

    private fun semana(n: Int, inicio: String, longao: Double?, sessoes: Int, tipo: TipoDeSemana): Semana {
        val i = LocalDate.parse(inicio).atStartOfDay(fuso).toInstant()
        return Semana(
            numero = n,
            dataInicio = i,
            dataFim = LocalDate.parse(inicio).plusDays(7).atStartOfDay(fuso).toInstant(),
            sessoesAlvo = sessoes,
            kmAlvo = 20.0,
            longaoKm = longao,
            tipo = tipo,
            parcial = false,
        )
    }

    @Test
    fun `o odometro tem 4 casas inteiras e 1 decimal, com zero a esquerda`() {
        assertEquals(listOf(0, 1, 2, 0, 4), digitosDoOdometro(120.4))
        assertEquals(listOf(0, 0, 0, 0, 0), digitosDoOdometro(0.0))
        // Arredonda a decimal: 8,96 km mostra 9,0.
        assertEquals(listOf(0, 0, 0, 9, 0), digitosDoOdometro(8.96))
        // Acima de 9.999,9 o odômetro para no máximo.
        assertEquals(listOf(9, 9, 9, 9, 9), digitosDoOdometro(12_345.0))
    }

    @Suppress("unused")
    private val domingo = DayOfWeek.SUNDAY
}
