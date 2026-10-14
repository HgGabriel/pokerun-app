package com.hggabriel.pokerun.ui.telas.editarcorrida

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.regras.ValoresCorrigidos
import com.hggabriel.pokerun.ui.telas.corridamanual.ValidacaoDaCorrida
import com.hggabriel.pokerun.ui.telas.corridamanual.validarCorrida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * O formulário da `RunEditScreen` (`F2-T10`): preenchido com a corrida, ele volta pela
 * validação do registro manual com os mesmos valores, e só a medição mudando pede
 * registro novo (RN-24).
 */
class EdicaoDeCorridaFormularioTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `o formulario sai no fuso do plano e na forma que a validacao le`() {
        val f = formularioDe(corrida(), fuso)

        assertEquals(LocalDate.of(2026, 9, 12), f.data)
        assertEquals(LocalTime.of(6, 30), f.hora)
        assertEquals("8,4", f.km)
        assertEquals("", f.horas)
        assertEquals("48", f.minutos)
        assertEquals("20", f.segundos)
        assertEquals("152", f.fcMedia)
        assertEquals("4", f.tempo1kMinutos)
        assertEquals("51", f.tempo1kSegundos)
    }

    @Test
    fun `o formulario preenchido valida de volta nos mesmos valores`() {
        val f = formularioDe(corrida(), fuso)

        val ok = validarCorrida(
            f.data, f.hora, f.km, f.horas, f.minutos, f.segundos, f.fcMedia, f.esforco,
            f.tempo1kMinutos, f.tempo1kSegundos, f.tempo5kMinutos, f.tempo5kSegundos,
            fuso, Instant.parse("2026-10-14T12:00:00Z"),
        ) as ValidacaoDaCorrida.Ok

        assertEquals(8.4, ok.km, 1e-9)
        assertEquals(2900L, ok.duracaoSeg)
        assertEquals(291L, ok.tempo1kSeg)
    }

    @Test
    fun `so a medicao mudando pede registro novo`() {
        val c = corrida()
        val iguais = ValoresCorrigidos(c.dataHoraInicio, c.km, c.duracaoSeg, c.fcMedia, c.esforcoPercebido, c.tempo1kSeg, c.tempo5kSeg, null)

        assertFalse(medicaoMudou(c, iguais))
        assertTrue(medicaoMudou(c, iguais.copy(km = 5.2)))
        assertTrue(medicaoMudou(c, iguais.copy(duracaoSeg = 3000)))
    }

    private fun corrida() = Corrida(
        id = "c",
        dataHoraInicio = Instant.parse("2026-09-12T09:30:00Z"),
        km = 8.4,
        duracaoSeg = 2900,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p1",
        semanaRef = 5,
        temporadaId = "kanto-2026",
        fcMedia = 152,
        tempo1kSeg = 291,
    )
}
