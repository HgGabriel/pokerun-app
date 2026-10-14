package com.hggabriel.pokerun.ui.telas.corridamanual

import com.hggabriel.pokerun.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Os tempos de 1 km e de 5 km digitados no registro manual (`F2-T08`, decisão nº 110),
 * escritos antes da validação deles.
 *
 * Sem splits, são a única fonte dos recordes de melhor 1 km e melhor 5 km (docs/04
 * §3.3). Os dois são opcionais, só valem em corrida que alcançou a marca, e um trecho
 * não dura mais que o treino inteiro.
 */
class TempoDaMarcaTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val agora: Instant = LocalDate.of(2026, 8, 20).atTime(10, 0).atZone(fuso).toInstant()

    @Test
    fun `sem os tempos a corrida grava como antes`() {
        val ok = validar() as ValidacaoDaCorrida.Ok

        assertNull(ok.tempo1kSeg)
        assertNull(ok.tempo5kSeg)
    }

    @Test
    fun `o tempo de 1 km vira segundos`() {
        val ok = validar(m1 = "4", s1 = "30") as ValidacaoDaCorrida.Ok

        assertEquals(270L, ok.tempo1kSeg)
    }

    @Test
    fun `o tempo de 5 km aceita so os minutos`() {
        val ok = validar(m5 = "25") as ValidacaoDaCorrida.Ok

        assertEquals(1500L, ok.tempo5kSeg)
    }

    @Test
    fun `corrida de menos de 1 km nao tem melhor 1 km`() {
        val erros = (validar(km = "0,8", minutos = "5", m1 = "4") as ValidacaoDaCorrida.Falhou).erros

        assertEquals(R.string.manual_erro_marca_1k_curta, erros.tempo1k)
    }

    @Test
    fun `corrida de menos de 5 km nao tem melhor 5 km`() {
        val erros = (validar(km = "4,5", minutos = "25", m5 = "22") as ValidacaoDaCorrida.Falhou).erros

        assertEquals(R.string.manual_erro_marca_5k_curta, erros.tempo5k)
    }

    @Test
    fun `o trecho nao dura mais que o treino`() {
        // 8 km em 45 min, e um "melhor 5 km" de 50 min.
        val erros = (validar(m5 = "50") as ValidacaoDaCorrida.Falhou).erros

        assertEquals(R.string.manual_erro_marca_maior, erros.tempo5k)
    }

    @Test
    fun `segundos acima de 59 e letras sao forma errada`() {
        val segundos = (validar(m1 = "4", s1 = "75") as ValidacaoDaCorrida.Falhou).erros
        val letras = (validar(m5 = "vinte") as ValidacaoDaCorrida.Falhou).erros

        assertEquals(R.string.manual_erro_marca_forma, segundos.tempo1k)
        assertEquals(R.string.manual_erro_marca_forma, letras.tempo5k)
    }

    private fun validar(
        km: String = "8",
        minutos: String = "45",
        m1: String = "",
        s1: String = "",
        m5: String = "",
        s5: String = "",
    ) = validarCorrida(
        data = LocalDate.of(2026, 8, 20),
        hora = LocalTime.of(6, 30),
        km = km,
        horas = "",
        minutos = minutos,
        segundos = "",
        fcMedia = "",
        esforco = "",
        tempo1kMinutos = m1,
        tempo1kSegundos = s1,
        tempo5kMinutos = m5,
        tempo5kSegundos = s5,
        fuso = fuso,
        agora = agora,
    )
}
