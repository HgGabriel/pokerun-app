package com.hggabriel.pokerun.dados.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import com.hggabriel.pokerun.dominio.regras.temporadaDe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Os casos que a ficha de `F2-T02` exige, escritos **antes** da implementação
 * (`EXECUCAO.md §3.2`), mais as bordas que as mesmas regras pedem.
 *
 * A leitura do Health Connect em si só se prova em aparelho. O que se prova aqui é o
 * que acontece com cada sessão depois de lida: se entra (só corrida, só da fonte
 * canônica — RN-22 e decisão nº 93), de onde a leitura começa (o cursor), e com que
 * snapshots ela vira `Corrida` — a semana do plano (RN-03, RN-14) e a temporada em
 * que aconteceu, no fuso da temporada (RN-40).
 */
class LeituraDeSessoesTest {

    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")

    private val samsungHealth = "com.sec.android.app.shealth"
    private val strava = "com.strava"

    // -----------------------------------------------------------------------
    // RN-22 e decisão nº 93 — o que entra
    // -----------------------------------------------------------------------

    @Test
    fun `caminhada da fonte canonica fica fora`() {
        val caminhada = sessao(tipo = ExerciseSessionRecord.EXERCISE_TYPE_WALKING)

        val triagem = triar(listOf(caminhada), samsungHealth)

        assertTrue(triagem.daFonte.isEmpty())
        assertEquals("caminhada não é corrida de outra origem", 0, triagem.deOutraOrigem)
    }

    @Test
    fun `esteira da fonte canonica entra`() {
        val esteira = sessao(tipo = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL)

        assertEquals(listOf(esteira), triar(listOf(esteira), samsungHealth).daFonte)
    }

    @Test
    fun `corrida da fonte canonica entra`() {
        val corrida = sessao()

        assertEquals(listOf(corrida), triar(listOf(corrida), samsungHealth).daFonte)
    }

    @Test
    fun `corrida do Strava com a fonte no Samsung Health fica fora e e contada para o aviso`() {
        val doStrava = sessao(origem = strava)
        val daFonte = sessao(id = "hc-2", origem = samsungHealth)

        val triagem = triar(listOf(doStrava, daFonte), samsungHealth)

        assertEquals(listOf(daFonte), triagem.daFonte)
        assertEquals(1, triagem.deOutraOrigem)
    }

    @Test
    fun `caminhada de outra origem nao entra no aviso`() {
        // O aviso da ImportReviewScreen fala de corrida que não veio. Uma caminhada do
        // Strava não viria nem da fonte canônica.
        val caminhada = sessao(origem = strava, tipo = ExerciseSessionRecord.EXERCISE_TYPE_WALKING)

        assertEquals(0, triar(listOf(caminhada), samsungHealth).deOutraOrigem)
    }

    // -----------------------------------------------------------------------
    // Cursor
    // -----------------------------------------------------------------------

    @Test
    fun `cursor nulo le desde o inicio do plano ativo`() {
        val grade = grade()

        assertEquals(grade.first().dataInicio, inicioDaLeitura(cursor = null, grade = grade))
    }

    @Test
    fun `cursor gravado le desde o cursor`() {
        val cursor = emSaoPaulo(2026, 9, 20, 7, 0)

        assertEquals(cursor, inicioDaLeitura(cursor = cursor, grade = grade()))
    }

    @Test
    fun `sem plano ativo e sem cursor nao ha de onde ler`() {
        assertNull(inicioDaLeitura(cursor = null, grade = emptyList()))
    }

    // -----------------------------------------------------------------------
    // RN-40 — a temporada é a da data, no fuso da temporada
    // -----------------------------------------------------------------------

    @Test
    fun `corrida de 31-12 as 23h em Sao Paulo, que em UTC ja e 01-01, e kanto-2026`() {
        val instante = emSaoPaulo(2026, 12, 31, 23, 0)
        // A premissa do caso: em UTC a corrida já é de 2027.
        assertEquals(2027, instante.atZone(ZoneId.of("UTC")).year)

        assertEquals("kanto-2026", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `corrida de 01-01 as 00h30 em Sao Paulo e da temporada seguinte`() {
        val instante = emSaoPaulo(2027, 1, 1, 0, 30)

        assertEquals("johto-2027", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `a meia-noite da virada ja e da temporada seguinte`() {
        // Fim exclusivo, como o data_fim das semanas (decisão nº 100).
        val instante = emSaoPaulo(2027, 1, 1, 0, 0)

        assertEquals("johto-2027", temporadaDe(instante, temporadas())?.id)
    }

    @Test
    fun `corrida sem temporada publicada que a cubra fica sem temporada`() {
        val instante = emSaoPaulo(2027, 1, 1, 0, 30)

        assertNull(temporadaDe(instante, listOf(kanto())))
    }

    @Test
    fun `a importacao grava a temporada da data e nao a ativa`() {
        // Na virada, johto já é a ativa e a corrida de 31/12 ainda é de Kanto.
        val temporadas = listOf(kanto().copy(ativa = false), johto().copy(ativa = true))
        val sessao = sessao(inicio = emSaoPaulo(2026, 12, 31, 23, 0))

        val corrida = corridaImportada(sessao, medidas(), plano(), grade(), temporadas)

        assertEquals("kanto-2026", corrida?.temporadaId)
    }

    // -----------------------------------------------------------------------
    // RN-03 e RN-14 — o snapshot do plano
    // -----------------------------------------------------------------------

    @Test
    fun `corrida anterior ao inicio do plano entra com semana_ref nulo`() {
        val antes = sessao(inicio = emSaoPaulo(2026, 8, 5, 7, 0))

        val corrida = corridaImportada(antes, medidas(), plano(), grade(), temporadas())

        assertNotNull(corrida)
        assertNull(corrida!!.semanaRef)
        assertEquals("o plano é snapshot mesmo fora do intervalo", "plano-teste", corrida.planoId)
    }

    @Test
    fun `corrida dentro do plano leva a semana dele`() {
        // 16/08/2026 é o domingo da semana 1 de um plano que começa em 10/08.
        val dentro = sessao(inicio = emSaoPaulo(2026, 8, 16, 7, 0))

        val corrida = corridaImportada(dentro, medidas(), plano(), grade(), temporadas())

        assertEquals(1, corrida?.semanaRef)
    }

    // -----------------------------------------------------------------------
    // A corrida que sai da sessão
    // -----------------------------------------------------------------------

    @Test
    fun `a corrida importada leva as medidas, as chaves e a origem`() {
        val sessao = sessao(
            id = "hc-9",
            idDoCliente = "sh-9",
            tipo = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
            duracaoSeg = 1_950,
        )
        val medidas = MedidasDaSessao(
            metros = 5_230.0,
            fcMedia = 152,
            fcMax = 171,
            fcMin = 118,
            caloriasAtivas = 348.6,
            passos = 5_811,
            cadenciaMedia = 164.4,
        )

        val corrida = corridaImportada(sessao, medidas, plano(), grade(), temporadas())!!

        assertEquals(5.23, corrida.km, 1e-9)
        assertEquals(1_950L, corrida.duracaoSeg)
        assertEquals(sessao.inicio, corrida.dataHoraInicio)
        assertEquals("RUNNING_TREADMILL", corrida.tipoExercicio)
        assertEquals(OrigemDaCorrida.HC, corrida.origem)
        assertEquals("hc-9", corrida.hcRecordId)
        assertEquals("sh-9", corrida.hcClientRecordId)
        assertEquals(152, corrida.fcMedia)
        assertEquals(171, corrida.fcMax)
        assertEquals(118, corrida.fcMin)
        assertEquals(349, corrida.caloriasAtivas)
        assertEquals(5_811, corrida.passos)
        assertEquals(164, corrida.cadenciaMedia)
        assertEquals(0L, corrida.xpCreditado)
    }

    @Test
    fun `corrida de rua grava o mesmo token do registro manual`() {
        val corrida = corridaImportada(sessao(), medidas(), plano(), grade(), temporadas())

        assertEquals("RUNNING", corrida?.tipoExercicio)
    }

    @Test
    fun `medida ausente fica nula e nao zero`() {
        val soDistancia = MedidasDaSessao(
            metros = 5_000.0,
            fcMedia = null,
            fcMax = null,
            fcMin = null,
            caloriasAtivas = null,
            passos = null,
            cadenciaMedia = null,
        )

        val corrida = corridaImportada(sessao(), soDistancia, plano(), grade(), temporadas())!!

        assertNull(corrida.fcMedia)
        assertNull(corrida.caloriasAtivas)
        assertNull(corrida.passos)
        assertNull(corrida.cadenciaMedia)
    }

    @Test
    fun `sessao sem distancia nao vira corrida`() {
        val semDistancia = medidas().copy(metros = null)

        assertNull(corridaImportada(sessao(), semDistancia, plano(), grade(), temporadas()))
    }

    @Test
    fun `sessao sem temporada que a cubra nao vira corrida`() {
        val em2027 = sessao(inicio = emSaoPaulo(2027, 1, 1, 0, 30))

        assertNull(corridaImportada(em2027, medidas(), plano(), grade(), listOf(kanto())))
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private fun emSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Instant =
        LocalDateTime.of(ano, mes, dia, hora, minuto).atZone(saoPaulo).toInstant()

    private fun sessao(
        id: String = "hc-1",
        idDoCliente: String? = null,
        origem: String = samsungHealth,
        tipo: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        inicio: Instant = emSaoPaulo(2026, 9, 20, 7, 0),
        duracaoSeg: Long = 1_800,
    ) = SessaoDoHealthConnect(
        id = id,
        idDoCliente = idDoCliente,
        origem = origem,
        tipo = tipo,
        inicio = inicio,
        fim = inicio.plusSeconds(duracaoSeg),
        duracaoSeg = duracaoSeg,
    )

    private fun medidas() = MedidasDaSessao(
        metros = 5_000.0,
        fcMedia = 150,
        fcMax = 170,
        fcMin = 120,
        caloriasAtivas = 320.0,
        passos = 5_500,
        cadenciaMedia = 162.0,
    )

    private fun kanto() = Temporada(
        id = "kanto-2026",
        nome = "Kanto 2026",
        regiao = "Kanto",
        inicio = LocalDate.of(2026, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fim = LocalDate.of(2027, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ativa = true,
        totalEspecies = 150,
        tiers = emptyList(),
    )

    private fun johto() = Temporada(
        id = "johto-2027",
        nome = "Johto 2027",
        regiao = "Johto",
        inicio = LocalDate.of(2027, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fim = LocalDate.of(2028, 1, 1).atStartOfDay(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ativa = false,
        totalEspecies = 100,
        tiers = emptyList(),
    )

    private fun temporadas() = listOf(kanto(), johto())

    private val prova: LocalDate = LocalDate.of(2026, 12, 31)

    private fun parametros() = ParametrosDeGeracao(
        dataProva = prova.atStartOfDay(saoPaulo).toInstant(),
        distanciaAlvoKm = 15.0,
        baselineKm = 5.0,
        sessoesPorSemana = 3,
    )

    private fun plano() = Plano(
        id = "plano-teste",
        nome = "São Silvestre 2026",
        distanciaAlvoKm = 15.0,
        dataProva = prova.atStartOfDay(saoPaulo).toInstant(),
        fuso = saoPaulo,
        ownerUid = "uid-dono",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = parametros(),
    )

    /** Começa na segunda 10/08/2026, em São Paulo. */
    private fun grade(): List<Semana> = GeradorDePlano.gerar(
        parametros(),
        LocalDate.of(2026, 8, 10).atStartOfDay(saoPaulo).toInstant(),
        saoPaulo,
    )
}
