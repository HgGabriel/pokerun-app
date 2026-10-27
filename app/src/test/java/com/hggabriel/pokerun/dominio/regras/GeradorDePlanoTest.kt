package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.pow

/**
 * A regra v2 do gerador (DA-10, docs/01 §3.2), escrita **antes** da implementação.
 *
 * Os números esperados não vêm do código. Vêm de `execucao/simular-gerador.py`, que gerou
 * a matriz da Fase 1 da DA-10 (`execucao/simulacao-gerador.md`) e o exemplo de §3.2: a São
 * Silvestre do casal, criada em 21/10/2026, longão de 10 km, curtas de 3 km, 3 corridas.
 *
 * A matriz inteira roda aqui contra um **verificador que não conhece a regra**: ele só
 * mede a grade contra os limites (passo, teto, piso da prova, curta). É o que pega uma
 * grade plausível e errada — a mais difícil de ver a olho.
 */
class GeradorDePlanoTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    private fun instante(dia: LocalDate): Instant = dia.atStartOfDay(fuso).toInstant()

    private fun dataDe(instante: Instant): LocalDate = instante.atZone(fuso).toLocalDate()

    private fun parametros(
        alvo: Double = 15.0,
        baseline: Double = 10.0,
        sessoes: Int = 3,
        curta: Double = 3.0,
        prova: LocalDate = LocalDate.of(2026, 12, 31),
    ) = ParametrosDeGeracao(
        dataProva = instante(prova),
        distanciaAlvoKm = alvo,
        baselineKm = baseline,
        sessoesPorSemana = sessoes,
        curtaKm = curta,
    )

    /** 21/10/2026, quarta-feira: o dia em que o casal criaria o plano. */
    private val criadoEm = instante(LocalDate.of(2026, 10, 21))

    private fun saoSilvestre() = GeradorDePlano.gerar(parametros(), criadoEm, fuso)

    // -----------------------------------------------------------------------
    // O início: a primeira segunda-feira que começa hoje ou depois (DA-10, nº 7)
    // -----------------------------------------------------------------------

    @Test
    fun `plano criado na quarta comeca na segunda seguinte`() {
        val grade = saoSilvestre()

        assertEquals(LocalDate.of(2026, 10, 26), dataDe(grade.first().dataInicio))
        assertEquals(10, grade.size)
    }

    @Test
    fun `plano criado numa segunda comeca no mesmo dia`() {
        val grade = GeradorDePlano.gerar(parametros(), instante(LocalDate.of(2026, 10, 26)), fuso)

        assertEquals(LocalDate.of(2026, 10, 26), dataDe(grade.first().dataInicio))
    }

    @Test
    fun `plano criado no domingo comeca no dia seguinte`() {
        val grade = GeradorDePlano.gerar(parametros(), instante(LocalDate.of(2026, 10, 25)), fuso)

        assertEquals(LocalDate.of(2026, 10, 26), dataDe(grade.first().dataInicio))
    }

    @Test
    fun `a semana 1 nunca e parcial e so a da prova pode ser`() {
        val grade = saoSilvestre()

        assertFalse(grade.first().parcial)
        assertEquals(listOf(10), grade.filter { it.parcial }.map { it.numero })
    }

    @Test
    fun `toda semana que nao e a da prova vai de segunda a segunda`() {
        saoSilvestre().dropLast(1).forEach { semana ->
            assertEquals(DayOfWeek.MONDAY, dataDe(semana.dataInicio).dayOfWeek)
            assertEquals(dataDe(semana.dataInicio).plusDays(7), dataDe(semana.dataFim))
        }
    }

    @Test
    fun `a semana da prova vai de 28 a 31 de dezembro com uma sessao so`() {
        val prova = saoSilvestre().last()

        assertEquals(TipoDeSemana.PROVA, prova.tipo)
        assertEquals(LocalDate.of(2026, 12, 28), dataDe(prova.dataInicio))
        // `dataFim` é exclusivo: a semana termina quando 01/01 começa.
        assertEquals(LocalDate.of(2027, 1, 1), dataDe(prova.dataFim))
        assertTrue(prova.parcial)
        assertEquals(1, prova.sessoesAlvo)
        assertEquals(15.0, prova.kmAlvo, TOLERANCIA)
        assertNull(prova.longaoKm)
    }

    @Test
    fun `primeiraSegundaDe e contarSemanas contam a partir da proxima segunda`() {
        val segunda = GeradorDePlano.primeiraSegundaDe(LocalDate.of(2026, 10, 21))

        assertEquals(LocalDate.of(2026, 10, 26), segunda)
        assertEquals(10, GeradorDePlano.contarSemanas(segunda, LocalDate.of(2026, 12, 31)))
    }

    // -----------------------------------------------------------------------
    // O exemplo de docs/01 §3.2: a São Silvestre do casal
    // -----------------------------------------------------------------------

    @Test
    fun `o longao comeca um passo acima da baseline e chega ao pico na semana N menos 3`() {
        val grade = saoSilvestre()
        val ritmo = (16.5 / 10.0).pow(1.0 / 7)

        assertEquals(10.0 * ritmo, grade.semana(1).longaoKm!!, TOLERANCIA)
        assertEquals(16.5, grade.semana(7).longaoKm!!, TOLERANCIA)
        assertEquals(10.742, grade.semana(1).longaoKm!!, 0.001)
    }

    @Test
    fun `a rampa e geometrica com o mesmo salto em toda semana de build`() {
        val build = saoSilvestre().filter { it.tipo == TipoDeSemana.BUILD }
        val ritmo = (16.5 / 10.0).pow(1.0 / 7)

        assertEquals((1..7).toList(), build.map { it.numero })
        build.zipWithNext { a, b -> b.longaoKm!! / a.longaoKm!! }
            .forEach { assertEquals(ritmo, it, TOLERANCIA) }
    }

    @Test
    fun `as curtas ficam na distancia escolhida e o volume e longao mais curtas`() {
        saoSilvestre().filter { it.tipo != TipoDeSemana.PROVA }.forEach { semana ->
            val curta = (semana.kmAlvo - semana.longaoKm!!) / (semana.sessoesAlvo - 1)
            assertEquals("semana ${semana.numero}", 3.0, curta, TOLERANCIA)
        }
        assertEquals(10.742 + 6.0, saoSilvestre().semana(1).kmAlvo, 0.001)
        assertEquals(22.5, saoSilvestre().semana(7).kmAlvo, TOLERANCIA)
    }

    @Test
    fun `o taper corta o longao a 60 e 40 por cento e mantem as curtas`() {
        val grade = saoSilvestre()

        assertEquals(TipoDeSemana.TAPER, grade.semana(8).tipo)
        assertEquals(9.9, grade.semana(8).longaoKm!!, TOLERANCIA)
        assertEquals(15.9, grade.semana(8).kmAlvo, TOLERANCIA)
        assertEquals(TipoDeSemana.TAPER, grade.semana(9).tipo)
        assertEquals(6.6, grade.semana(9).longaoKm!!, TOLERANCIA)
        assertEquals(12.6, grade.semana(9).kmAlvo, TOLERANCIA)
    }

    @Test
    fun `a curta nunca passa do longao da semana`() {
        // Curta de 5 km num plano curto: o 2º taper tem longão de 4,4 km.
        val grade = GeradorDePlano.gerar(
            parametros(alvo = 10.0, baseline = 8.0, curta = 5.0, prova = LocalDate.of(2026, 11, 22)),
            criadoEm,
            fuso,
        )
        grade.filter { it.longaoKm != null }.forEach { semana ->
            val curta = (semana.kmAlvo - semana.longaoKm!!) / (semana.sessoesAlvo - 1)
            assertTrue("semana ${semana.numero}", curta <= semana.longaoKm!! + TOLERANCIA)
        }
        val ultimaDeTaper = grade[grade.size - 2]
        assertEquals(ultimaDeTaper.longaoKm!! * 3, ultimaDeTaper.kmAlvo, TOLERANCIA)
    }

    @Test
    fun `nenhuma semana nasce com longao pedido`() {
        assertTrue(saoSilvestre().all { it.longaoPedidoKm == null })
    }

    // -----------------------------------------------------------------------
    // O pico: teto de 32 km e partida no menor entre baseline e pico
    // -----------------------------------------------------------------------

    @Test
    fun `a maratona para no teto de 32 km`() {
        val grade = GeradorDePlano.gerar(
            parametros(alvo = 42.2, baseline = 28.0, curta = 10.0, prova = segundaDaSemana(21)),
            instante(SEGUNDA),
            fuso,
        )

        assertEquals(32.0, grade.maxOf { it.longaoKm ?: 0.0 }, TOLERANCIA)
    }

    @Test
    fun `quem ja corre mais que o pico mantem o longao no pico`() {
        // DA-10, nº 6: baseline 12 para uma prova de 10 km.
        val grade = GeradorDePlano.gerar(
            parametros(alvo = 10.0, baseline = 12.0, prova = segundaDaSemana(12)),
            instante(SEGUNDA),
            fuso,
        )

        grade.filter { it.tipo == TipoDeSemana.BUILD }
            .forEach { assertEquals(11.0, it.longaoKm!!, TOLERANCIA) }
    }

    // -----------------------------------------------------------------------
    // Quando o plano cabe, cabe com pico reduzido, ou não cabe
    // -----------------------------------------------------------------------

    @Test
    fun `a sao silvestre do casal cabe`() {
        val v = GeradorDePlano.avaliar(parametros(), criadoEm, fuso)

        assertTrue(v is Viabilidade.Cabe)
        assertEquals(10, v.semanas)
    }

    @Test
    fun `10 km em 12 semanas partindo de 3,5 km cabe com pico reduzido de 8,25`() {
        val v = GeradorDePlano.avaliar(
            parametros(alvo = 10.0, baseline = 3.5, prova = segundaDaSemana(12)),
            instante(SEGUNDA),
            fuso,
        )

        assertTrue(v is Viabilidade.PicoReduzido)
        v as Viabilidade.PicoReduzido
        assertEquals(3.5 * 1.1.pow(9), v.picoKm, TOLERANCIA)
        assertEquals(11.0, v.picoDesejadoKm, TOLERANCIA)
    }

    @Test
    fun `maratona em 8 semanas partindo de 14 km nao cabe e oferece as duas saidas`() {
        val v = GeradorDePlano.avaliar(
            parametros(alvo = 42.2, baseline = 14.0, prova = segundaDaSemana(8)),
            instante(SEGUNDA),
            fuso,
        )

        assertTrue(v is Viabilidade.NaoCabe)
        v as Viabilidade.NaoCabe
        // N >= 12: a prova cabe a partir da segunda-feira da 12ª semana.
        assertEquals(segundaDaSemana(12), v.primeiraProvaQueCabe)
        assertEquals(30.0, v.maiorAlvoQueCabeKm, TOLERANCIA)
        assertEquals(31.65, v.picoMinimoKm, TOLERANCIA)
    }

    @Test
    fun `sao silvestre partindo de 5 km nao cabe em 10 semanas`() {
        val v = GeradorDePlano.avaliar(parametros(baseline = 5.0), criadoEm, fuso)

        assertTrue(v is Viabilidade.NaoCabe)
        v as Viabilidade.NaoCabe
        assertEquals(LocalDate.of(2027, 1, 11), v.primeiraProvaQueCabe)
        assertEquals(12.5, v.maiorAlvoQueCabeKm, TOLERANCIA)
    }

    @Test
    fun `o menor plano tem 4 semanas e 1 de build`() {
        val v = GeradorDePlano.avaliar(
            parametros(alvo = 10.0, baseline = 8.0, prova = segundaDaSemana(4)),
            instante(SEGUNDA),
            fuso,
        )
        val grade = GeradorDePlano.gerar(
            parametros(alvo = 10.0, baseline = 8.0, prova = segundaDaSemana(4)),
            instante(SEGUNDA),
            fuso,
        )

        assertTrue(v is Viabilidade.PicoReduzido)
        assertEquals(listOf(TipoDeSemana.BUILD, TipoDeSemana.TAPER, TipoDeSemana.TAPER, TipoDeSemana.PROVA), grade.map { it.tipo })
        assertEquals(8.8, grade.first().longaoKm!!, TOLERANCIA)
    }

    @Test
    fun `menos de 4 semanas e curto demais`() {
        val v = GeradorDePlano.avaliar(
            parametros(alvo = 10.0, baseline = 8.0, prova = segundaDaSemana(3)),
            instante(SEGUNDA),
            fuso,
        )

        assertTrue(v is Viabilidade.CurtoDemais)
        assertEquals(3, v.semanas)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gerar recusa plano que nao cabe`() {
        GeradorDePlano.gerar(parametros(baseline = 5.0), criadoEm, fuso)
    }

    // -----------------------------------------------------------------------
    // A matriz da Fase 1 contra o verificador
    // -----------------------------------------------------------------------

    @Test
    fun `a matriz da DA-10 da 54 que cabem 18 de pico reduzido e 18 que nao cabem`() {
        var cabe = 0
        var reduzido = 0
        var naoCabe = 0
        matriz { p ->
            when (GeradorDePlano.avaliar(p, instante(SEGUNDA), fuso)) {
                is Viabilidade.Cabe -> cabe++
                is Viabilidade.PicoReduzido -> reduzido++
                is Viabilidade.NaoCabe -> naoCabe++
                is Viabilidade.CurtoDemais -> error("a matriz não tem plano curto demais")
            }
        }

        assertEquals(listOf(54, 18, 18), listOf(cabe, reduzido, naoCabe))
    }

    @Test
    fun `nenhuma grade da matriz que cabe quebra um limite`() {
        var medidas = 0
        matriz { p ->
            if (GeradorDePlano.avaliar(p, instante(SEGUNDA), fuso) is Viabilidade.NaoCabe) return@matriz
            val grade = GeradorDePlano.gerar(p, instante(SEGUNDA), fuso)
            val violados = verificar(grade, p)
            assertTrue("alvo ${p.distanciaAlvoKm}, baseline ${p.baselineKm}, N ${grade.size}: $violados", violados.isEmpty())
            medidas++
        }
        // Trava de contagem (ARMADILHAS §8.2, item 37): um laço vazio passaria verde.
        assertEquals(72, medidas)
    }

    /**
     * Mede a grade contra os limites de docs/01 §3.2 sem saber de que regra ela saiu. É a
     * função `medir` do simulador, em Kotlin.
     */
    private fun verificar(grade: List<Semana>, p: ParametrosDeGeracao): List<String> {
        val treino = grade.filter { it.tipo != TipoDeSemana.PROVA }
        val maior = mutableMapOf(0 to p.baselineKm)
        treino.forEach { maior[it.numero] = it.longaoKm ?: (it.kmAlvo / it.sessoesAlvo) }
        val violados = mutableListOf<String>()
        treino.forEach { s ->
            val janela = (maxOf(0, s.numero - 4) until s.numero).maxOf { maior.getValue(it) }
            if (maior.getValue(s.numero) > 1.1 * janela + TOLERANCIA) violados += "passo na semana ${s.numero}"
            val curta = (s.kmAlvo - s.longaoKm!!) / (s.sessoesAlvo - 1)
            if (curta > s.longaoKm + TOLERANCIA) violados += "curta maior que o longão na semana ${s.numero}"
        }
        val pico = treino.maxOf { maior.getValue(it.numero) }
        if (pico > 32.0 + TOLERANCIA) violados += "pico acima de 32 km"
        if (p.distanciaAlvoKm > pico / 0.75 + TOLERANCIA) violados += "pico abaixo de 75% da prova"
        return violados
    }

    /** A matriz de `simular-gerador.py`: 5 alvos × 3 sessões × 3 N × 2 baselines. */
    private fun matriz(bloco: (ParametrosDeGeracao) -> Unit) {
        for (alvo in listOf(5.0, 10.0, 15.0, 21.1, 42.2)) {
            for (sessoes in 2..4) {
                for (n in listOf(8, 12, 21)) {
                    val baixo = Math.round(alvo / 3 * 2) / 2.0
                    val alto = Math.round(alvo * 2 / 3 * 2) / 2.0
                    for (baseline in listOf(baixo, alto)) {
                        val curta = maxOf(1.0, Math.round(baseline / 2 * 2) / 2.0)
                        bloco(parametros(alvo, baseline, sessoes, curta, segundaDaSemana(n)))
                    }
                }
            }
        }
    }

    private fun segundaDaSemana(n: Int): LocalDate = SEGUNDA.plusWeeks((n - 1).toLong())

    // -----------------------------------------------------------------------
    // A curva da tela de criação: a mesma conta da grade
    // -----------------------------------------------------------------------

    @Test
    fun `a curva do longao e o longao da grade semana a semana e termina na prova`() {
        val grade = saoSilvestre()
        val curva = GeradorDePlano.curvaDoLongao(15.0, 10.0, grade.size)

        assertEquals(grade.size, curva.size)
        grade.dropLast(1).forEachIndexed { i, semana ->
            assertEquals("semana ${semana.numero}", semana.longaoKm!!, curva[i], TOLERANCIA)
        }
        assertEquals(15.0, curva.last(), TOLERANCIA)
    }

    @Test
    fun `a curva do plano que nao cabe sobe 10 por cento por semana`() {
        // 5 km de base para uma meia em 10 semanas: o pico fica abaixo de 75% da prova.
        val curva = GeradorDePlano.curvaDoLongao(21.1, 5.0, 10)

        assertEquals(5.0 * 1.1, curva[0], TOLERANCIA)
        assertEquals(5.0 * 1.1.pow(7), curva[6], TOLERANCIA)
        assertTrue(curva[6] < GeradorDePlano.PISO_DA_PROVA * 21.1)
    }

    private fun List<Semana>.semana(n: Int) = single { it.numero == n }

    private companion object {
        /** Uma segunda-feira qualquer: o plano criado nela começa nela. */
        val SEGUNDA: LocalDate = LocalDate.of(2026, 8, 10)

        const val TOLERANCIA = 1e-9
    }
}
