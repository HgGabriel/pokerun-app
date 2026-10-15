package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * A edição de corrida da `RunEditScreen` (`F2-T10`, docs/03 §3.14), escrita antes da
 * implementação. RN-24, RN-31 a RN-35, e a reivindicação de docs/04 §3.2.
 *
 * A semana padrão prevê 3 sessões, 20 km e longão de 10 km: curtas de 5 km.
 */
class EdicaoDeCorridaTest {

    // -----------------------------------------------------------------------
    // RN-24 — corrigir grava um registro novo
    // -----------------------------------------------------------------------

    @Test
    fun `corrigir a distancia grava registro novo apontando para o antigo, e marca o antigo`() {
        val original = corrida("o", km = 8.4, duracaoSeg = 2900)

        val correcao = EdicaoDeCorrida.corrigir(original, valores(original, km = 5.2))

        assertEquals("o", correcao.nova.substituiRunId)
        assertEquals(5.2, correcao.nova.km, 1e-9)
        assertFalse(correcao.nova.substituida)
        assertFalse(correcao.nova.descartada)
        assertEquals(original.planoId, correcao.nova.planoId)
        assertEquals(original.temporadaId, correcao.nova.temporadaId)
        assertTrue(correcao.antiga.substituida)
        assertEquals(8.4, correcao.antiga.km, 1e-9)
    }

    @Test
    fun `o registro novo nao herda o XP do antigo`() {
        // XP-10: quem escreve XP é o replay. O registro novo nasce zerado.
        val original = corrida("o", km = 8.4, duracaoSeg = 2900).copy(xpCreditado = 1680)

        assertEquals(0L, EdicaoDeCorrida.corrigir(original, valores(original, km = 5.2)).nova.xpCreditado)
    }

    // -----------------------------------------------------------------------
    // RN-31 — descartar
    // -----------------------------------------------------------------------

    @Test
    fun `descartar tira a corrida de todos os calculos e a mantem na base`() {
        val corrida = corrida("d", km = 5.0, duracaoSeg = 1800)
        val outra = corrida("v", km = 6.0, duracaoSeg = 2100, dia = 1)

        val descartada = EdicaoDeCorrida.descartar(corrida)
        val base = listOf(descartada, outra)

        assertTrue(descartada.descartada)
        assertEquals(2, base.size)
        assertFalse("d" in Replay.recalcular(base, mapOf("p1" to listOf(semana())), emptyList(), Instant.EPOCH).porCorrida)
        assertEquals(1, CalculoDeAderencia.sessoesFeitas(semana(), base))
    }

    // -----------------------------------------------------------------------
    // RN-32 — correção livre a qualquer momento
    // -----------------------------------------------------------------------

    @Test
    fun `corrigir corrida de semana congelada ou de plano encerrado e permitido`() {
        val corrida = corrida("c", km = 5.0, duracaoSeg = 1800)
        val muitoDepois = Instant.parse("2027-03-01T12:00:00Z")

        assertTrue(EdicaoDeCorrida.edicaoPermitida(corrida, plano, muitoDepois))
        assertTrue(EdicaoDeCorrida.edicaoPermitida(corrida, plano.copy(encerrado = true), muitoDepois))
    }

    // -----------------------------------------------------------------------
    // RN-34 e docs/04 §3.2 — a escolha do usuário
    // -----------------------------------------------------------------------

    @Test
    fun `escolher o longao para uma corrida menor faz a escolha ficar e a maior ir para uma curta`() {
        val maior = corrida("maior", km = 9.0, duracaoSeg = 3240)
        val menor = EdicaoDeCorrida.reivindicar(corrida("menor", km = 5.0, duracaoSeg = 1800, dia = 2), SessaoReivindicada.Longao)
        val corridas = listOf(maior, menor)

        val segmentos = segmentosDaSemana(semana(), corridas)
        assertEquals("menor", segmentos.single { it.longao }.corridaId)
        assertEquals("maior", segmentos.first { !it.longao }.corridaId)

        // A maior cumpre a curta de 5 km: 2,0×, sem bônus de longão.
        val xpDaMaior = MotorDeXp.xpDaCorrida(maior, contexto(corridas))
        assertEquals(2.0, xpDaMaior.multiplicador, 1e-9)
        assertEquals(0.0, xpDaMaior.longao, 1e-9)
    }

    @Test
    fun `com o longao escolhido, o indicador de RN-10 le a corrida do longao e nao a maior`() {
        // A resolução da herança de `F1-T09b`: a barra e a linha de RN-10 não divergem.
        val maior = corrida("maior", km = 9.5, duracaoSeg = 3420)
        val menor = EdicaoDeCorrida.reivindicar(corrida("menor", km = 5.0, duracaoSeg = 1800, dia = 2), SessaoReivindicada.Longao)

        assertEquals(false, CalculoDeAderencia.longaoCumprido(semana(), listOf(maior, menor)))
    }

    @Test
    fun `escolher avulsa deixa a corrida em 1x e fora de todo slot`() {
        val outra = corrida("outra", km = 4.0, duracaoSeg = 1440)
        val avulsa = EdicaoDeCorrida.reivindicar(corrida("a", km = 9.5, duracaoSeg = 3420, dia = 1), SessaoReivindicada.Avulsa)
        val corridas = listOf(outra, avulsa)

        assertEquals(SessaoReivindicada.Avulsa, avulsa.sessaoReivindicada)
        assertTrue(segmentosDaSemana(semana(), corridas).none { it.corridaId == "a" })
        // A maior da semana é a avulsa, mas quem toma o longão é a outra.
        assertEquals("outra", segmentosDaSemana(semana(), corridas).single { it.longao }.corridaId)
        assertEquals(1.0, MotorDeXp.xpDaCorrida(avulsa, contexto(corridas)).multiplicador, 1e-9)
        assertEquals(1, CalculoDeAderencia.sessoesFeitas(semana(), corridas))
    }

    @Test
    fun `voltar para a atribuicao automatica apaga a escolha`() {
        val escolhida = corrida("e", km = 5.0, duracaoSeg = 1800).copy(sessaoReivindicada = SessaoReivindicada.Longao)

        assertNull(EdicaoDeCorrida.reivindicar(escolhida, null).sessaoReivindicada)
    }

    @Test
    fun `as opcoes mostram o alvo e o multiplicador, e desabilitam o slot escolhido por outra corrida`() {
        val dona = corrida("dona", km = 5.0, duracaoSeg = 1800).copy(sessaoReivindicada = SessaoReivindicada.Curta(1))
        val editada = corrida("ed", km = 5.0, duracaoSeg = 1800, dia = 1)

        val opcoes = EdicaoDeCorrida.opcoesDeSessao(editada, semana(), listOf(dona, editada))
            .associateBy { it.sessao }

        assertEquals(
            listOf(SessaoReivindicada.Avulsa, SessaoReivindicada.Longao, SessaoReivindicada.Curta(1), SessaoReivindicada.Curta(2)),
            EdicaoDeCorrida.opcoesDeSessao(editada, semana(), listOf(dona, editada)).map { it.sessao },
        )
        assertEquals(1.0, opcoes.getValue(SessaoReivindicada.Avulsa).multiplicador, 1e-9)
        assertNull(opcoes.getValue(SessaoReivindicada.Avulsa).alvoKm)
        // 5 km no longão de 10: abaixo de 90%, 1,5×. Na curta de 5: 2,0×.
        assertEquals(10.0, opcoes.getValue(SessaoReivindicada.Longao).alvoKm!!, 1e-9)
        assertEquals(1.5, opcoes.getValue(SessaoReivindicada.Longao).multiplicador, 1e-9)
        assertEquals(2.0, opcoes.getValue(SessaoReivindicada.Curta(2)).multiplicador, 1e-9)
        // A curta 1 foi escolhida pelo usuário para outra corrida.
        assertFalse(opcoes.getValue(SessaoReivindicada.Curta(1)).habilitada)
        assertTrue(opcoes.getValue(SessaoReivindicada.Curta(2)).habilitada)
        // Sem escolha gravada, a marcada é a da atribuição automática: o longão livre.
        assertTrue(opcoes.getValue(SessaoReivindicada.Longao).escolhida)
        assertEquals(1, opcoes.values.count { it.escolhida })
    }

    // -----------------------------------------------------------------------
    // docs/01 §5.3 — o resumo depois do replay
    // -----------------------------------------------------------------------

    @Test
    fun `o resumo abre a conta de antes e depois e diz qual recorde mudou`() {
        val antes = ProgressoNaTemporada(74_200, 118, Instant.parse("2026-09-12T10:00:00Z"))
        val depois = ProgressoNaTemporada(71_100, 116, Instant.parse("2026-09-12T10:00:00Z"))

        val resumo = EdicaoDeCorrida.resumo(antes, depois, Recordes(melhor5kSeg = 1634), Recordes(melhor5kSeg = 1700))

        assertEquals(74_200L, resumo.xpAntes)
        assertEquals(71_100L, resumo.xpDepois)
        assertEquals(118, resumo.posicaoAntes)
        assertEquals(116, resumo.posicaoDepois)
        assertEquals(setOf(TipoDeRecorde.MELHOR_5K), resumo.recordesQueMudaram)
    }

    @Test
    fun `sem progresso antes o resumo parte do 001 com zero XP`() {
        val depois = ProgressoNaTemporada(500, 5, Instant.parse("2026-09-12T10:00:00Z"))

        val resumo = EdicaoDeCorrida.resumo(null, depois, Recordes(), Recordes())

        assertEquals(0L, resumo.xpAntes)
        assertEquals(1, resumo.posicaoAntes)
        assertTrue(resumo.recordesQueMudaram.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Montagem
    // -----------------------------------------------------------------------

    private val inicio = Instant.parse("2026-08-10T03:00:00Z")

    private fun semana() = Semana(
        numero = 1,
        dataInicio = inicio,
        dataFim = inicio.plusSeconds(7 * 86_400L),
        sessoesAlvo = 3,
        kmAlvo = 20.0,
        longaoKm = 10.0,
        tipo = TipoDeSemana.BUILD,
        parcial = false,
    )

    private val plano = Plano(
        id = "p1",
        nome = "Plano",
        distanciaAlvoKm = 15.0,
        dataProva = Instant.parse("2026-12-31T11:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ownerUid = "u1",
        codigoConvite = "ABC234",
        encerrado = false,
        parametros = ParametrosDeGeracao(Instant.parse("2026-12-31T11:00:00Z"), 15.0, 5.0, 3),
    )

    private fun corrida(id: String, km: Double, duracaoSeg: Long, dia: Int = 0) = Corrida(
        id = id,
        dataHoraInicio = inicio.plusSeconds(dia * 86_400L + 7 * 3_600L),
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = 1,
        temporadaId = "kanto-2026",
    )

    private fun valores(original: Corrida, km: Double) = ValoresCorrigidos(
        dataHoraInicio = original.dataHoraInicio,
        km = km,
        duracaoSeg = original.duracaoSeg,
        fcMedia = original.fcMedia,
        esforcoPercebido = original.esforcoPercebido,
        tempo1kSeg = original.tempo1kSeg,
        tempo5kSeg = original.tempo5kSeg,
        sessao = original.sessaoReivindicada,
    )

    private fun contexto(corridas: List<Corrida>) = ContextoDeXp(
        ordemNoHistorico = 10,
        recordesAnteriores = Recordes(),
        mediaMovelPaceSegKm = null,
        semana = semana(),
        corridasDaSemana = corridas,
    )
}
