package com.hggabriel.pokerun.ui.telas.detalhesemana

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * O detalhe de uma semana (`F1-T15`, docs/03 §3.9, RN-05, RN-10, RN-26 e RN-28).
 *
 * **A tela é sobre uma semana só, e é aí que três erros cabem sem aparecer:**
 *
 * - **RN-05** — o cadeado. `data_fim` é exclusivo, então a semana congela no instante
 *   dele e não um dia depois. Um `>` no lugar de `>=` deixa a semana editável por 24h
 *   fantasma, e ninguém olha a tela naquele dia para descobrir.
 * - **RN-10** — o longão cumprido a 90% do alvo, e **nulo** onde a semana não planeja
 *   longão. Devolver `false` na 2ª de taper puniria quem seguiu o plano à risca.
 * - **RN-28** — o dia de uma corrida é o dia **no fuso do plano**. Este é o teste que a
 *   máquina de desenvolvimento não pega sozinha: ela roda em São Paulo, então todo caso
 *   cujo plano seja de São Paulo passa com `ZoneId.systemDefault()`. O plano de Tóquio
 *   existe por isso.
 *
 * Somam-se RN-26 (a semana da prova tem menos de sete dias, e a grade de dias sai das
 * fronteiras dela e não de um `7` escrito no código) e RN-08 com RN-34 (a fração tem
 * teto por semana, e a barra tem um segmento por sessão prevista).
 */
class DetalheDaSemanaTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val toquio = ZoneId.of("Asia/Tokyo")

    /** 10 semanas a partir da segunda-feira de 10/08/2026, prova no sábado 17/10. */
    private val inicio: Instant = LocalDate.of(2026, 8, 13).atStartOfDay(fuso).toInstant()

    private val parametros = ParametrosDeGeracao(
        dataProva = LocalDate.of(2026, 10, 17).atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = 10.0,
        baselineKm = 5.0,
        sessoesPorSemana = 3,
    )

    private val grade: List<Semana> = GeradorDePlano.gerar(parametros, inicio, fuso)

    private fun plano(fuso: ZoneId = this.fuso) = Plano(
        id = "plano-1",
        nome = "Sao Silvestre 2026",
        distanciaAlvoKm = parametros.distanciaAlvoKm,
        dataProva = parametros.dataProva,
        fuso = fuso,
        ownerUid = "uid-dono",
        codigoConvite = "FYQJE6",
        encerrado = false,
        parametros = parametros,
    )

    private var proximoId = 1

    private fun corrida(
        semanaRef: Int?,
        km: Double = 5.0,
        quando: Instant = inicio,
        duracaoSeg: Long = 1800,
        descartada: Boolean = false,
        substituida: Boolean = false,
        sessao: SessaoReivindicada? = null,
    ) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = quando,
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-1",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
        sessaoReivindicada = sessao,
        descartada = descartada,
        substituida = substituida,
    )

    private fun estado(
        plano: Plano? = plano(),
        grade: List<Semana> = this.grade,
        corridas: List<Corrida> = emptyList(),
        numero: Int = 1,
        agora: Instant = inicio,
    ) = detalheDaSemana(plano, grade, corridas, numero, agora)

    private fun conteudo(
        plano: Plano? = plano(),
        grade: List<Semana> = this.grade,
        corridas: List<Corrida> = emptyList(),
        numero: Int = 1,
        agora: Instant = inicio,
    ) = estado(plano, grade, corridas, numero, agora) as DetalheSemanaUiState.Conteudo

    // -----------------------------------------------------------------------
    // RN-05 — o cadeado, e a fronteira exclusiva de `data_fim`
    // -----------------------------------------------------------------------

    @Test
    fun `a semana que ja acabou vem congelada`() {
        val primeira = grade.first()
        val depois = primeira.dataFim.plusSeconds(1)

        assertTrue(conteudo(numero = 1, agora = depois).congelada)
    }

    @Test
    fun `a semana corrente nao vem congelada`() {
        val primeira = grade.first()
        val antes = primeira.dataFim.minusSeconds(1)

        assertFalse(conteudo(numero = 1, agora = antes).congelada)
    }

    @Test
    fun `o instante de data_fim ja congela, porque ele e exclusivo`() {
        val primeira = grade.first()

        // `data_fim` é a meia-noite que já pertence à semana seguinte: a comparação é
        // `>=`. Com `>` a semana ficaria editável por um dia que não é dela, e a rule de
        // `weeks/{n}` negaria a escrita que a tela ofereceu.
        assertTrue(conteudo(numero = 1, agora = primeira.dataFim).congelada)
    }

    @Test
    fun `a semana futura nunca esta congelada`() {
        assertFalse(conteudo(numero = 5, agora = inicio).congelada)
    }

    // -----------------------------------------------------------------------
    // RN-10 — o longão cumprido é indicador de tela, com piso de 90%
    // -----------------------------------------------------------------------

    @Test
    fun `o longao cumprido sai de noventa por cento do alvo`() {
        val semana = grade.first()
        val alvo = semana.longaoKm!!

        val quase = conteudo(corridas = listOf(corrida(1, km = 0.9 * alvo)))
        assertTrue(quase.longaoCumprido == true)
    }

    @Test
    fun `abaixo de noventa por cento o longao nao esta cumprido`() {
        val semana = grade.first()
        val alvo = semana.longaoKm!!

        val curta = conteudo(corridas = listOf(corrida(1, km = 0.89 * alvo)))
        assertFalse(curta.longaoCumprido == true)
    }

    @Test
    fun `a semana da prova nao tem longao a cumprir`() {
        val prova = grade.last()
        assertNull(prova.longaoKm)

        val estado = conteudo(
            numero = prova.numero,
            corridas = listOf(corrida(prova.numero, km = 10.0)),
            agora = prova.dataInicio,
        )

        // Nulo não é falso: um X vermelho aqui puniria quem seguiu o plano à risca.
        assertNull(estado.longaoCumprido)
        assertNull(estado.longaoKm)
    }

    // -----------------------------------------------------------------------
    // RN-08 e RN-34 — a fração tem teto, e a barra tem um segmento por sessão
    // -----------------------------------------------------------------------

    @Test
    fun `a fracao tem teto na semana`() {
        val quatro = List(4) { corrida(1, km = 5.0) }
        val estado = conteudo(corridas = quatro)

        assertEquals(3, estado.previstas)
        assertEquals(3, estado.feitas)
    }

    @Test
    fun `a barra tem um segmento por sessao prevista`() {
        val estado = conteudo(corridas = listOf(corrida(1), corrida(1)))

        assertEquals(3, estado.segmentos.size)
        assertEquals(1, estado.segmentos.count { it.longao })
        assertEquals(2, estado.segmentos.count { it.km != null })
    }

    @Test
    fun `a semana da prova tem uma sessao prevista e nenhum segmento de longao`() {
        val prova = grade.last()
        val estado = conteudo(numero = prova.numero, agora = prova.dataInicio)

        assertEquals(1, estado.previstas)
        assertEquals(1, estado.segmentos.size)
        assertTrue(estado.segmentos.none { it.longao })
    }

    // -----------------------------------------------------------------------
    // RN-26 e RN-01 — a grade de dias sai das fronteiras da semana
    // -----------------------------------------------------------------------

    @Test
    fun `a semana comum tem sete dias`() {
        val estado = conteudo(numero = 2, agora = grade[1].dataInicio)

        assertEquals(7, estado.dias.size)
        assertEquals(LocalDate.of(2026, 8, 17), estado.primeiroDia)
        assertEquals(LocalDate.of(2026, 8, 23), estado.ultimoDia)
    }

    @Test
    fun `a semana da prova tem menos de sete dias`() {
        val prova = grade.last()
        val estado = conteudo(numero = prova.numero, agora = prova.dataInicio)

        // 12 a 17/10: a prova é no sábado, e 18/10 já está fora do plano.
        assertEquals(6, estado.dias.size)
        assertEquals(LocalDate.of(2026, 10, 12), estado.primeiroDia)
        assertEquals(LocalDate.of(2026, 10, 17), estado.ultimoDia)
        assertEquals(TipoDeSemana.PROVA, estado.tipo)
    }

    // -----------------------------------------------------------------------
    // RN-28 — o dia é o dia no fuso do plano
    // -----------------------------------------------------------------------

    @Test
    fun `o dia da corrida sai no fuso do plano, e nao no do aparelho`() {
        val emToquio = GeradorDePlano.gerar(parametros, inicio, toquio)
        val segunda = LocalDate.of(2026, 8, 10)

        // Segunda-feira 05h em Tóquio é **domingo 17h em São Paulo**: o fuso do aparelho
        // jogaria esta corrida para o dia anterior, que está fora da semana inteira.
        val cedo = segunda.atStartOfDay(toquio).plusHours(5).toInstant()

        val estado = conteudo(
            plano = plano(fuso = toquio),
            grade = emToquio,
            corridas = listOf(corrida(1, quando = cedo)),
            agora = cedo,
        )

        assertEquals(segunda, estado.corridas.single().dia)
        assertEquals(1, estado.dias.first().corridas)
        assertTrue(estado.dias.first().hoje)
    }

    @Test
    fun `o intervalo de datas sai no fuso do plano`() {
        val emToquio = GeradorDePlano.gerar(parametros, inicio, toquio)
        val estado = conteudo(plano = plano(fuso = toquio), grade = emToquio)

        // A meia-noite de 10/08 em Tóquio é 09/08 em São Paulo. O cabeçalho da tela sai
        // deste par, e um dia a menos aqui é uma semana inteira desalinhada.
        assertEquals(LocalDate.of(2026, 8, 10), estado.primeiroDia)
        assertEquals(LocalDate.of(2026, 8, 16), estado.ultimoDia)
    }

    // -----------------------------------------------------------------------
    // RN-24 e RN-31 — a lista mostra o que a fração conta
    // -----------------------------------------------------------------------

    @Test
    fun `corrida descartada fica fora da lista`() {
        val estado = conteudo(
            corridas = listOf(corrida(1), corrida(1, descartada = true)),
        )

        assertEquals(1, estado.corridas.size)
        assertEquals(1, estado.feitas)
    }

    @Test
    fun `corrida substituida fica fora da lista`() {
        val estado = conteudo(
            corridas = listOf(corrida(1), corrida(1, substituida = true)),
        )

        assertEquals(1, estado.corridas.size)
        assertEquals(1, estado.feitas)
    }

    @Test
    fun `corrida de outra semana fica fora`() {
        val estado = conteudo(corridas = listOf(corrida(2), corrida(null)))

        assertTrue(estado.corridas.isEmpty())
        assertEquals(0, estado.feitas)
    }

    @Test
    fun `as corridas saem em ordem cronologica`() {
        val terca = LocalDate.of(2026, 8, 11).atStartOfDay(fuso).plusHours(7).toInstant()
        val sabado = LocalDate.of(2026, 8, 15).atStartOfDay(fuso).plusHours(7).toInstant()

        val estado = conteudo(
            corridas = listOf(
                corrida(1, km = 12.0, quando = sabado),
                corrida(1, km = 6.0, quando = terca),
            ),
        )

        assertEquals(listOf(6.0, 12.0), estado.corridas.map { it.km })
    }

    // -----------------------------------------------------------------------
    // O estado vazio de docs/03 §3.9
    // -----------------------------------------------------------------------

    @Test
    fun `semana sem corrida vem vazia, com o que a semana previa`() {
        val estado = conteudo()
        val primeira = grade.first()

        assertTrue(estado.vazia)
        assertEquals(primeira.sessoesAlvo, estado.previstas)
        assertEquals(primeira.kmAlvo, estado.kmAlvo, 0.001)
        assertEquals(primeira.longaoKm, estado.longaoKm)
    }

    @Test
    fun `semana com corrida nao vem vazia`() {
        assertFalse(conteudo(corridas = listOf(corrida(1))).vazia)
    }

    // -----------------------------------------------------------------------
    // O roteamento: o que é espera, o que é erro
    // -----------------------------------------------------------------------

    @Test
    fun `grade vazia e espera, e nao semana quebrada`() {
        assertEquals(DetalheSemanaUiState.Carregando, estado(grade = emptyList()))
    }

    @Test
    fun `plano ausente e erro`() {
        assertEquals(DetalheSemanaUiState.Falhou, estado(plano = null))
    }

    @Test
    fun `semana fora da grade e erro, e nao tela vazia`() {
        // A rota carrega o número, e um plano trocado embaixo dela deixa a tela apontando
        // para uma semana que não existe. Um conteúdo com zeros seria pior que a mensagem.
        assertEquals(DetalheSemanaUiState.Falhou, estado(numero = 99))
    }

    @Test
    fun `o cabecalho conta a semana dentro do plano`() {
        val estado = conteudo(numero = 4, agora = grade[3].dataInicio)

        assertEquals(4, estado.numero)
        assertEquals(grade.size, estado.totalDeSemanas)
        assertEquals("Sao Silvestre 2026", estado.nomeDoPlano)
    }
}
