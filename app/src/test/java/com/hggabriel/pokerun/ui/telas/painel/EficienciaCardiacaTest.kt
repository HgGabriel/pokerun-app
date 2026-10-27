package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * A eficiência cardíaca (`F3-T06`, docs/03 §3.17, decisão nº 108): FC média ÷ velocidade
 * média, por corrida, ao longo do tempo. O mínimo é 8 corridas com FC; abaixo dele, o
 * bloco diz quantas faltam.
 */
class EficienciaCardiacaTest {

    @Test
    fun `com menos de 8 corridas com FC o bloco diz quantas faltam`() {
        val corridas = (1..3).map { corrida(it, fc = 150) } + corrida(9, fc = null)

        assertEquals(5, (eficienciaCardiaca(corridas) as EficienciaCardiaca.Faltam).corridas)
    }

    @Test
    fun `o valor e a FC media dividida pela velocidade media`() {
        // 10 km em 1 h: 10 km/h. FC média 150: 15 bpm por km/h.
        val corridas = (1..8).map { corrida(it, fc = 150, km = 10.0, duracaoSeg = 3600) }

        val serie = eficienciaCardiaca(corridas) as EficienciaCardiaca.Serie

        assertEquals(8, serie.pontos.size)
        assertEquals(15.0, serie.pontos.first().valor, 1e-9)
    }

    @Test
    fun `a serie segue a ordem do tempo e ignora descartada, substituida e corrida sem FC`() {
        val corridas = (1..8).map { corrida(it, fc = 140 + it) }.reversed() +
            corrida(20, fc = 150).copy(descartada = true) +
            corrida(21, fc = 150).copy(substituida = true) +
            corrida(22, fc = null)

        val serie = eficienciaCardiaca(corridas) as EficienciaCardiaca.Serie

        assertEquals((1..8).map { "c$it" }, serie.pontos.map { it.id })
        assertTrue(serie.pontos.zipWithNext().all { (a, b) -> a.instante < b.instante })
    }

    private fun corrida(dia: Int, fc: Int?, km: Double = 8.0, duracaoSeg: Long = 2880) = Corrida(
        id = "c$dia",
        dataHoraInicio = Instant.parse("2026-08-01T10:00:00Z").plusSeconds(dia * 86_400L),
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = 1,
        temporadaId = "kanto-2026",
        fcMedia = fc,
    )
}
