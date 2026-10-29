package com.hggabriel.pokerun.ui.telas.medalhas

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.regras.Ritmo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MedalhasDoRitmoTest {

    @Test
    fun `conta as corridas validas de cada ritmo pelo pace`() {
        val corridas = listOf(
            corrida(km = 5.0, duracaoSeg = 5 * 250L), // 4:10, Jolteon
            corrida(km = 10.0, duracaoSeg = 10 * 580L), // 9:40, Slowpoke
            corrida(km = 3.0, duracaoSeg = 3 * 580L), // 9:40, Slowpoke
        )

        assertEquals(mapOf(Ritmo.JOLTEON to 1, Ritmo.SLOWPOKE to 2), corridasPorRitmo(corridas))
    }

    @Test
    fun `descartada, substituida e corrida sem pace nao contam`() {
        val corridas = listOf(
            corrida(km = 5.0, duracaoSeg = 5 * 250L, descartada = true),
            corrida(km = 5.0, duracaoSeg = 5 * 250L, substituida = true),
            corrida(km = 0.0, duracaoSeg = 1_800L),
            corrida(km = 5.0, duracaoSeg = 0L),
        )

        assertEquals(emptyMap<Ritmo, Int>(), corridasPorRitmo(corridas))
    }

    @Test
    fun `o teto de cada ritmo e exclusivo`() {
        // 4:15 em ponto já não é Jolteon: o pace tem de ficar abaixo do teto.
        assertEquals(mapOf(Ritmo.RAPIDASH to 1), corridasPorRitmo(listOf(corrida(km = 1.0, duracaoSeg = 255L))))
    }

    @Test
    fun `a faixa vai do teto do ritmo mais rapido ao teto do proprio`() {
        assertEquals(FaixaDoRitmo(de = null, ate = 255), faixaDo(Ritmo.JOLTEON))
        assertEquals(FaixaDoRitmo(de = 255, ate = 300), faixaDo(Ritmo.RAPIDASH))
        assertEquals(FaixaDoRitmo(de = 540, ate = null), faixaDo(Ritmo.SLOWPOKE))
        assertNull(faixaDo(Ritmo.SLOWPOKE).ate)
    }

    private fun corrida(km: Double, duracaoSeg: Long, descartada: Boolean = false, substituida: Boolean = false) = Corrida(
        id = "c",
        dataHoraInicio = Instant.parse("2026-10-27T09:00:00Z"),
        km = km,
        duracaoSeg = duracaoSeg,
        tipoExercicio = "corrida",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = 1,
        temporadaId = "t",
        descartada = descartada,
        substituida = substituida,
    )
}
