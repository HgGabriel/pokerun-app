package com.hggabriel.pokerun.ui.componentes

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A barra de sessões e a grade de dias do card da semana (`F1-T09`, docs/03 §3.3.1),
 * escritas **antes** da implementação (`EXECUCAO.md §3.2`).
 *
 * O card não tem anel (docs/02 §9.1.1), e o que entra no lugar dele carrega duas
 * regras que nenhuma revisão de olho verifica:
 *
 * - **RN-34:** cada sessão prevista aceita no máximo uma corrida. É o que impede a
 *   semana de 3 previstas com 5 corridas de desenhar 5 segmentos.
 * - **RN-01 e RN-28:** a grade de dias é segunda a domingo **no fuso do plano**. Uma
 *   corrida de domingo às 22h em UTC−3 é segunda em UTC, e o quadrado acenderia no dia
 *   errado sem nada na tela dizendo que o problema foi fuso.
 *
 * A semana da prova aparece nos dois blocos porque ela é a armadilha nº 1 do projeto:
 * uma sessão prevista (RN-26) e quatro dias de calendário, não sete.
 */
class SessoesDaSemanaTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    // -----------------------------------------------------------------------
    // RN-34 — um segmento por sessão prevista, e no máximo uma corrida em cada
    // -----------------------------------------------------------------------

    @Test
    fun `a barra tem exatamente uma sessao prevista por segmento`() {
        val segmentos = segmentosDaSemana(grade().semana(1), emptyList())

        assertEquals(3, segmentos.size)
    }

    @Test
    fun `o ultimo segmento e o longao, e e o unico`() {
        val segmentos = segmentosDaSemana(grade().semana(1), emptyList())

        assertTrue(segmentos.last().longao)
        assertEquals(1, segmentos.count { it.longao })
    }

    @Test
    fun `a semana sem longao previsto nao desenha segmento de longao`() {
        // A 2ª de taper (N−1) corta o volume a 40% e divide igual entre as sessões:
        // não há longão para cumprir, e um segmento de altura dupla ali seria um alvo
        // que a grade nunca previu.
        val taper = grade().semana(20)

        assertNull("a 2ª de taper não planeja longão", taper.longaoKm)
        assertTrue(segmentosDaSemana(taper, emptyList()).none { it.longao })
    }

    @Test
    fun `a semana da prova tem um segmento so`() {
        // RN-26: a prova é a única sessão prevista da última semana.
        assertEquals(1, segmentosDaSemana(grade().semana(21), emptyList()).size)
    }

    @Test
    fun `a corrida que reivindicou o longao cai no segmento do longao`() {
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 9.0, sessao = SessaoReivindicada.Longao),
            corrida(semanaRef = 1, km = 4.0),
        )

        val segmentos = segmentosDaSemana(semana, corridas)

        assertEquals(9.0, segmentos.single { it.longao }.km!!, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // docs/04 §3.2 e RN-10 — sem reivindicação, a maior corrida ocupa o longão
    // (`F1-T09b`). Na Fase 1 toda corrida é manual e `sessao_reivindicada` nasce
    // nula, então é esta passada que decide o que a barra mostra.
    // -----------------------------------------------------------------------

    @Test
    fun `uma corrida so, sem reivindicacao, ocupa o longao`() {
        // O caso que o aparelho mostrou em 21/09: 8,4 km sozinhos na semana 1 do
        // `Plano Teste`, a linha dizendo "Longão de 6 km cumprido" e o segmento alto
        // vazio, com a corrida num segmento curto.
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(corrida(semanaRef = 1, km = 8.4)),
        )

        assertEquals(8.4, segmentos.single { it.longao }.km!!, TOLERANCIA)
        assertTrue("as curtas continuam pendentes", segmentos.filterNot { it.longao }.all { it.km == null })
    }

    @Test
    fun `sem reivindicacao, a maior vai para o longao e as demais para as curtas em ordem cronologica`() {
        // docs/04 §3.2, com as duas metades: "a maior corrida da semana reivindica o
        // longão, as demais tomam os slots curtos em ordem cronológica até N". A maior
        // está no meio do tempo de propósito, para a ordem das curtas não sair dela.
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 5.0, em = "2026-08-13T09:00:00Z"),
            corrida(semanaRef = 1, km = 9.0, em = "2026-08-12T09:00:00Z"),
            corrida(semanaRef = 1, km = 4.0, em = "2026-08-11T09:00:00Z"),
        )

        val segmentos = segmentosDaSemana(semana, corridas)

        assertEquals(4.0, segmentos[0].km!!, TOLERANCIA)
        assertEquals(5.0, segmentos[1].km!!, TOLERANCIA)
        assertEquals(9.0, segmentos[2].km!!, TOLERANCIA)
        assertTrue(segmentos[2].longao)
    }

    @Test
    fun `sem longao previsto, as corridas seguem so a ordem cronologica`() {
        // Na 2ª de taper não há slot de longão, e nenhuma corrida pode ser puxada pelo
        // tamanho para um lugar que a grade não previu.
        val taper = grade().semana(20)
        val corridas = listOf(
            corrida(semanaRef = 20, km = 9.0, em = "2026-12-23T09:00:00Z"),
            corrida(semanaRef = 20, km = 4.0, em = "2026-12-21T09:00:00Z"),
        )

        val segmentos = segmentosDaSemana(taper, corridas)

        assertEquals(4.0, segmentos[0].km!!, TOLERANCIA)
        assertEquals(9.0, segmentos[1].km!!, TOLERANCIA)
    }

    @Test
    fun `com o longao ja reivindicado, a maior sem reivindicacao vai para uma curta`() {
        // RN-34: quem reivindicou não é desalojado, nem pela corrida maior.
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(
                corrida(semanaRef = 1, km = 6.0, sessao = SessaoReivindicada.Longao),
                corrida(semanaRef = 1, km = 9.0),
            ),
        )

        assertEquals(6.0, segmentos.single { it.longao }.km!!, TOLERANCIA)
        assertEquals(9.0, segmentos[0].km!!, TOLERANCIA)
    }

    @Test
    fun `a maior corrida que reivindicou uma curta fica na curta`() {
        // RN-34: a reivindicação é a palavra do usuário e passa na frente da atribuição
        // automática. Onde a linha de RN-10 fica nesse caso é pergunta de `F2-T10`, que
        // é quem grava a reivindicação.
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(
                corrida(semanaRef = 1, km = 9.0, sessao = SessaoReivindicada.Curta(1)),
                corrida(semanaRef = 1, km = 4.0),
            ),
        )

        assertEquals(9.0, segmentos[0].km!!, TOLERANCIA)
    }

    @Test
    fun `no empate de distancia, a primeira no tempo fica com o longao`() {
        // A ordem em que o Firestore devolve as corridas não é garantida, e o segmento
        // não pode trocar de dono entre duas aberturas da tela.
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(
                corrida(id = "segunda", semanaRef = 1, km = 7.0, em = "2026-08-13T09:00:00Z"),
                corrida(id = "primeira", semanaRef = 1, km = 7.0, em = "2026-08-11T09:00:00Z"),
            ),
        )

        assertEquals("primeira", segmentos.single { it.longao }.corridaId)
        assertEquals("segunda", segmentos[0].corridaId)
    }

    @Test
    fun `a linha de RN-10 e o segmento alto nao se contradizem`() {
        // O defeito visto no aparelho foi exatamente esta contradição: a linha dizendo
        // cumprido, o segmento alto dizendo prevista. Sem reivindicação, as duas leem a
        // mesma corrida — a maior da semana.
        val semana = grade().semana(1)
        val alvo = semana.longaoKm!!
        val cenarios = listOf(
            emptyList(),
            listOf(corrida(semanaRef = 1, km = alvo * 1.4)),
            listOf(corrida(semanaRef = 1, km = alvo * 0.5)),
            listOf(
                corrida(semanaRef = 1, km = alvo * 0.4, em = "2026-08-11T09:00:00Z"),
                corrida(semanaRef = 1, km = alvo * 0.95, em = "2026-08-12T09:00:00Z"),
                corrida(semanaRef = 1, km = alvo * 0.3, em = "2026-08-13T09:00:00Z"),
            ),
        )

        cenarios.forEach { corridas ->
            val noSegmento = segmentosDaSemana(semana, corridas).single { it.longao }.km
            val alto = noSegmento != null && noSegmento >= 0.9 * alvo

            assertEquals(
                "corridas: ${corridas.map { it.km }}",
                CalculoDeAderencia.longaoCumprido(semana, corridas),
                alto,
            )
        }
    }

    @Test
    fun `duas corridas reivindicando a mesma sessao ocupam um segmento so`() {
        // RN-34: cada sessão prevista aceita no máximo uma corrida. A segunda vai para
        // um slot livre em vez de sobrescrever a primeira ou sumir.
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 9.0, sessao = SessaoReivindicada.Longao),
            corrida(semanaRef = 1, km = 8.0, sessao = SessaoReivindicada.Longao),
        )

        val segmentos = segmentosDaSemana(semana, corridas)

        assertEquals(9.0, segmentos.single { it.longao }.km!!, TOLERANCIA)
        assertEquals(2, segmentos.count { it.corridaId != null })
    }

    @Test
    fun `a quarta corrida de uma semana de tres previstas nao vira segmento`() {
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            List(4) { corrida(semanaRef = 1) },
        )

        assertEquals(3, segmentos.size)
        assertTrue(segmentos.all { it.corridaId != null })
    }

    @Test
    fun `corrida descartada e corrida substituida nao preenchem segmento`() {
        // RN-31 e RN-24. A substituída já foi contada pela correção que a substituiu.
        val semana = grade().semana(1)
        val corridas = listOf(
            corrida(semanaRef = 1, km = 5.0, descartada = true),
            corrida(semanaRef = 1, km = 5.0, substituida = true),
        )

        assertTrue(segmentosDaSemana(semana, corridas).all { it.corridaId == null })
    }

    @Test
    fun `corrida de outra semana nao preenche segmento`() {
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(corrida(semanaRef = 2), corrida(semanaRef = null)),
        )

        assertTrue(segmentos.all { it.corridaId == null })
    }

    @Test
    fun `o segmento cumprido carrega o id da corrida, porque ele e enderecavel`() {
        // docs/03 §3.3.1: tocar no segmento abre a corrida que o cumpriu. Um anel não
        // faz isso, e um segmento sem o id também não.
        val segmentos = segmentosDaSemana(
            grade().semana(1),
            listOf(corrida(id = "run-do-sabado", semanaRef = 1)),
        )

        assertEquals("run-do-sabado", segmentos.first { it.corridaId != null }.corridaId)
    }

    // -----------------------------------------------------------------------
    // RN-01, RN-26 e RN-28 — a grade de dias
    // -----------------------------------------------------------------------

    @Test
    fun `a semana cheia tem sete dias, de segunda a domingo`() {
        val dias = diasDaSemana(grade().semana(1), emptyList(), fuso, Instant.parse("2026-08-12T12:00:00Z"))

        assertEquals(7, dias.size)
        assertEquals(DayOfWeek.MONDAY, dias.first().dia.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dias.last().dia.dayOfWeek)
    }

    @Test
    fun `a semana da prova tem quatro dias, e nao sete`() {
        // RN-26: de 28 a 31/12. Todo cálculo que assume sete dias quebra aqui, e é a
        // armadilha nº 1 do projeto.
        val dias = diasDaSemana(grade().semana(21), emptyList(), fuso, Instant.parse("2026-12-29T12:00:00Z"))

        assertEquals(4, dias.size)
        assertEquals(LocalDate.of(2026, 12, 28), dias.first().dia)
        assertEquals(LocalDate.of(2026, 12, 31), dias.last().dia)
    }

    @Test
    fun `a corrida de domingo as 22h em Sao Paulo acende o domingo, nao a segunda`() {
        // RN-28. O instante é 2026-08-17T01:00Z, que em UTC já é segunda-feira. No fuso
        // do plano continua sendo domingo 16, e é o fuso do plano que manda.
        val dias = diasDaSemana(
            grade().semana(1),
            listOf(corrida(semanaRef = 1, em = "2026-08-17T01:00:00Z")),
            fuso,
            Instant.parse("2026-08-16T12:00:00Z"),
        )

        assertEquals(1, dias.single { it.dia == LocalDate.of(2026, 8, 16) }.corridas)
        assertEquals(6, dias.count { it.corridas == 0 })
    }

    @Test
    fun `hoje e marcado no dia do fuso do plano`() {
        val dias = diasDaSemana(grade().semana(1), emptyList(), fuso, Instant.parse("2026-08-13T12:00:00Z"))

        assertEquals(LocalDate.of(2026, 8, 13), dias.single { it.hoje }.dia)
    }

    @Test
    fun `numa semana passada nenhum dia e hoje`() {
        val dias = diasDaSemana(grade().semana(1), emptyList(), fuso, Instant.parse("2026-09-20T12:00:00Z"))

        assertTrue(dias.none { it.hoje })
    }

    @Test
    fun `corrida descartada nao acende quadrado`() {
        val dias = diasDaSemana(
            grade().semana(1),
            listOf(corrida(semanaRef = 1, em = "2026-08-12T12:00:00Z", descartada = true)),
            fuso,
            Instant.parse("2026-08-13T12:00:00Z"),
        )

        assertTrue(dias.all { it.corridas == 0 })
    }

    @Test
    fun `duas corridas no mesmo dia contam as duas na grade`() {
        // A barra diz *quantas* sessões, com teto; a grade diz *quando*, sem teto. São
        // perguntas diferentes, e é por isso que as duas convivem no card.
        val dias = diasDaSemana(
            grade().semana(1),
            listOf(
                corrida(semanaRef = 1, em = "2026-08-12T09:00:00Z"),
                corrida(semanaRef = 1, em = "2026-08-12T21:00:00Z"),
            ),
            fuso,
            Instant.parse("2026-08-13T12:00:00Z"),
        )

        assertEquals(2, dias.single { it.dia == LocalDate.of(2026, 8, 12) }.corridas)
    }

    @Test
    fun `a grade ignora corrida de outra semana`() {
        val dias = diasDaSemana(
            grade().semana(1),
            listOf(corrida(semanaRef = 2, em = "2026-08-12T12:00:00Z")),
            fuso,
            Instant.parse("2026-08-13T12:00:00Z"),
        )

        assertFalse(dias.any { it.corridas > 0 })
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private var proximoId = 0

    private fun corrida(
        semanaRef: Int?,
        id: String = "run-${proximoId++}",
        km: Double = 5.0,
        em: String = "2026-08-12T09:00:00Z",
        sessao: SessaoReivindicada? = null,
        descartada: Boolean = false,
        substituida: Boolean = false,
    ) = Corrida(
        id = id,
        dataHoraInicio = Instant.parse(em),
        km = km,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-teste",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
        sessaoReivindicada = sessao,
        descartada = descartada,
        substituida = substituida,
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
