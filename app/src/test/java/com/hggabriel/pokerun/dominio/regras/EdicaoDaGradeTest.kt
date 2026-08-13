package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A grade **depois** de gerada (`F1-T11`, docs/01 §3.3, RN-30), escrita antes da
 * implementação (`EXECUCAO.md §3.2`).
 *
 * Duas regras moram aqui e as duas são funções puras:
 *
 * - **A derivação da edição:** mexer no longão recalcula o volume pela fórmula de
 *   docs/01 §3.2. O usuário nunca edita o volume — um campo manda, o outro sai dele.
 * - **O alerta de 15% (RN-30):** o salto de volume entre semanas de `build`
 *   **consecutivas**. Ele roda sobre a grade corrente, gerada ou editada, e **nunca
 *   bloqueia** a gravação.
 *
 * O caso que mais escapa é o filtro de `build`. Do taper para a prova o volume **sobe**
 * — com 2 sessões, de 9,9 km para 15 km, um salto de 51% —, e quem esquecer o filtro
 * acusa risco de lesão exatamente na semana em que o plano manda descansar.
 */
class EdicaoDaGradeTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** Uma segunda-feira. A prova de 31/12/2026 dá as 21 semanas de docs/01 §3.2. */
    private val inicio: Instant = LocalDate.of(2026, 8, 10).atStartOfDay(fuso).toInstant()

    // -----------------------------------------------------------------------
    // RN-30 — na grade recém-gerada
    // -----------------------------------------------------------------------

    @Test
    fun `baseline baixa dispara o alerta na grade recem-gerada`() {
        // docs/01 §3.3: baseline de 3 km com alvo de 15 km dá ~26% no primeiro salto —
        // "exatamente a pessoa que mais precisa do aviso".
        val salto = alertaDeVolume(gerar(baselineKm = 3.0))

        assertNotNull(salto)
        assertEquals(1, salto!!.de)
        assertEquals(2, salto.para)
    }

    @Test
    fun `baseline confortavel nao dispara nada`() {
        // De 10 km para um pico de 16,5 km em 17 passos, cada salto fica em ~3,8%.
        assertNull(alertaDeVolume(gerar(baselineKm = 10.0)))
    }

    @Test
    fun `a subida do taper para a prova nao dispara, porque nao e build`() {
        // Com 2 sessões o volume do 2º taper é 9,9 km e o da prova é 15 km: +51%. Sem o
        // filtro de `build`, o alerta acusaria risco de lesão na semana do descanso.
        val grade = gerar(baselineKm = 10.0, sessoes = 2)

        assertNull(alertaDeVolume(grade))
    }

    // -----------------------------------------------------------------------
    // RN-30 — o limiar
    // -----------------------------------------------------------------------

    @Test
    fun `salto de exatamente 15 por cento nao dispara`() {
        val grade = listOf(
            build(numero = 1, km = 10.0),
            build(numero = 2, km = 11.5),
        )

        assertNull(alertaDeVolume(grade))
    }

    @Test
    fun `salto logo acima de 15 por cento dispara`() {
        val grade = listOf(
            build(numero = 1, km = 10.0),
            build(numero = 2, km = 11.51),
        )

        assertNotNull(alertaDeVolume(grade))
    }

    @Test
    fun `o percentual arredonda para cima, para o banner nunca imprimir o proprio limiar`() {
        // 15,4% arredondado para o mais próximo daria 15, e o banner diria "salto de 15%.
        // Aumentos acima de 15% elevam risco" — contradição visível na tela.
        val grade = listOf(
            build(numero = 1, km = 10.0),
            build(numero = 2, km = 11.54),
        )

        assertEquals(16, alertaDeVolume(grade)!!.percentual)
    }

    @Test
    fun `o alerta nomeia o par com o maior salto, e nao o primeiro que encontra`() {
        val grade = listOf(
            build(numero = 1, km = 10.0),
            build(numero = 2, km = 12.0),   // +20%
            build(numero = 3, km = 18.0),   // +50%
            build(numero = 4, km = 22.0),   // +22%
        )

        val salto = alertaDeVolume(grade)!!

        assertEquals(2, salto.de)
        assertEquals(3, salto.para)
        assertEquals(50, salto.percentual)
    }

    @Test
    fun `semanas de build nao consecutivas nao formam par`() {
        // Um taper no meio parte a sequência: 10 km e 30 km não são semanas vizinhas.
        val grade = listOf(
            build(numero = 1, km = 10.0),
            Semana(
                numero = 2,
                dataInicio = segunda(2),
                dataFim = segunda(3),
                sessoesAlvo = 3,
                kmAlvo = 8.0,
                longaoKm = 4.0,
                tipo = TipoDeSemana.TAPER,
                parcial = false,
            ),
            build(numero = 3, km = 30.0),
        )

        assertNull(alertaDeVolume(grade))
    }

    // -----------------------------------------------------------------------
    // docs/01 §3.3 — a edição deriva o volume
    // -----------------------------------------------------------------------

    @Test
    fun `editar o longao recalcula o volume pela formula de tres sessoes`() {
        val grade = gerar(baselineKm = 5.0)

        val editada = editarLongao(grade, numero = 4, longaoKm = 9.0).first { it.numero == 4 }

        assertEquals(9.0, editada.longaoKm!!, TOLERANCIA)
        // 3 sessões: volume = 2,0 × longão (docs/01 §3.2).
        assertEquals(18.0, editada.kmAlvo, TOLERANCIA)
    }

    @Test
    fun `a derivacao usa as sessoes da propria semana`() {
        val comDuas = editarLongao(gerar(baselineKm = 5.0, sessoes = 2), 4, 9.0)
        val comQuatro = editarLongao(gerar(baselineKm = 5.0, sessoes = 4), 4, 9.0)

        assertEquals(13.5, comDuas.first { it.numero == 4 }.kmAlvo, TOLERANCIA)
        assertEquals(22.5, comQuatro.first { it.numero == 4 }.kmAlvo, TOLERANCIA)
    }

    @Test
    fun `a edicao nao mexe nas fronteiras de data, no tipo nem nas sessoes`() {
        val grade = gerar(baselineKm = 5.0)
        val antes = grade.first { it.numero == 4 }

        val depois = editarLongao(grade, numero = 4, longaoKm = 9.0).first { it.numero == 4 }

        assertEquals(antes.dataInicio, depois.dataInicio)
        // RN-05 deriva o congelamento de `data_fim`. Uma edição que arrastasse a data
        // destravaria uma semana já fechada.
        assertEquals(antes.dataFim, depois.dataFim)
        assertEquals(antes.tipo, depois.tipo)
        assertEquals(antes.sessoesAlvo, depois.sessoesAlvo)
        assertEquals(antes.parcial, depois.parcial)
    }

    @Test
    fun `a edicao nao toca nas outras semanas`() {
        val grade = gerar(baselineKm = 5.0)

        val editada = editarLongao(grade, numero = 4, longaoKm = 9.0)

        assertEquals(grade.size, editada.size)
        assertEquals(grade.filter { it.numero != 4 }, editada.filter { it.numero != 4 })
    }

    // -----------------------------------------------------------------------
    // RN-30 — na grade editada
    // -----------------------------------------------------------------------

    @Test
    fun `puxar o longao de uma semana dispara o alerta numa grade que estava limpa`() {
        val grade = gerar(baselineKm = 10.0)
        assertNull(alertaDeVolume(grade))

        // A semana 5 sai de ~11,5 km de longão para 16 km.
        val salto = alertaDeVolume(editarLongao(grade, numero = 5, longaoKm = 16.0))!!

        assertEquals(4, salto.de)
        assertEquals(5, salto.para)
    }

    @Test
    fun `baixar o longao de uma semana apaga o alerta da grade`() {
        // Baseline 4,5 é a fronteira: só o primeiro salto passa de 15% (15,7%), e o
        // segundo já fica em 13,6%. Subir a semana 1 para 4,7 km fecha a única brecha.
        val grade = gerar(baselineKm = 4.5)
        assertNotNull(alertaDeVolume(grade))

        val corrigida = editarLongao(grade, numero = 1, longaoKm = 4.7)

        assertNull(alertaDeVolume(corrigida))
    }

    @Test
    fun `o alerta nao remove semana nenhuma da grade`() {
        // RN-30 nunca bloqueia: a grade alertada é a mesma grade, inteira, e é ela que
        // vai para o Firestore.
        val grade = gerar(baselineKm = 3.0)

        assertNotNull(alertaDeVolume(grade))
        assertEquals(21, grade.size)
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private fun gerar(baselineKm: Double, sessoes: Int = 3): List<Semana> =
        GeradorDePlano.gerar(
            parametros = ParametrosDeGeracao(
                dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(fuso).toInstant(),
                distanciaAlvoKm = 15.0,
                baselineKm = baselineKm,
                sessoesPorSemana = sessoes,
            ),
            inicio = inicio,
            fuso = fuso,
        )

    private fun segunda(numero: Int): Instant =
        LocalDate.of(2026, 8, 10).plusWeeks((numero - 1).toLong()).atStartOfDay(fuso).toInstant()

    /** Uma semana de `build` com o volume posto à mão, para medir o limiar de RN-30. */
    private fun build(numero: Int, km: Double): Semana = Semana(
        numero = numero,
        dataInicio = segunda(numero),
        dataFim = segunda(numero + 1),
        sessoesAlvo = 3,
        kmAlvo = km,
        longaoKm = km / 2.0,
        tipo = TipoDeSemana.BUILD,
        parcial = false,
    )

    private companion object {
        const val TOLERANCIA = 0.0001
    }
}
