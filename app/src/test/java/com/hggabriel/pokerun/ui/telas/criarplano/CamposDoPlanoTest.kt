package com.hggabriel.pokerun.ui.telas.criarplano

import com.hggabriel.pokerun.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * As validações da `CreatePlanScreen` (`F1-T10`, docs/03 §3.5, DA-10).
 *
 * Data no futuro, pelo menos 4 semanas, as três distâncias em forma e **o plano caber**.
 * Elas existem para a mesma coisa: impedir que o `GeradorDePlano` receba entrada que ele
 * recusa. Ele falha alto com `require`, e uma tela que deixasse passar um plano que não
 * cabe derrubaria o app em vez de mostrar as saídas.
 *
 * A que mais escapa é a contagem: `N` conta da **próxima segunda-feira** (DA-10, nº 7), e
 * não do dia em que a pessoa está preenchendo.
 */
class CamposDoPlanoTest {

    /** Uma quinta-feira. O plano criado hoje começa na segunda 17/08. */
    private val hoje: LocalDate = LocalDate.of(2026, 8, 13)

    // -----------------------------------------------------------------------
    // O caminho feliz
    // -----------------------------------------------------------------------

    @Test
    fun `formulario coerente e valido e devolve o total de semanas`() {
        val resultado = validar()

        assertTrue(resultado is ValidacaoDoPlano.Ok)
        // De 17/08 a 31/12: 20 semanas, a última de 28 a 31/12.
        assertEquals(20, (resultado as ValidacaoDoPlano.Ok).semanas)
    }

    @Test
    fun `o formulario valido devolve os campos ja convertidos`() {
        val ok = validar(nome = "  São Silvestre  ", alvo = "15", baseline = "7,5", curta = "3") as ValidacaoDoPlano.Ok

        assertEquals("São Silvestre", ok.nome)
        assertEquals(15.0, ok.alvoKm, TOLERANCIA)
        assertEquals(7.5, ok.baselineKm, TOLERANCIA)
        assertEquals(3.0, ok.curtaKm, TOLERANCIA)
    }

    @Test
    fun `distancia da prova menor que a confortavel passa`() {
        // DA-10, nº 6: quem já corre a distância quer constância até a prova.
        assertTrue(validar(alvo = "10", baseline = "12") is ValidacaoDoPlano.Ok)
    }

    // -----------------------------------------------------------------------
    // Nome
    // -----------------------------------------------------------------------

    @Test
    fun `nome em branco recusa`() {
        assertEquals(R.string.criar_erro_nome, erros(validar(nome = "   ")).nome)
    }

    // -----------------------------------------------------------------------
    // Data
    // -----------------------------------------------------------------------

    @Test
    fun `data nao escolhida recusa`() {
        // O `DatePicker` abre vazio: nulo é "ainda não respondeu", não "hoje".
        assertEquals(R.string.criar_erro_data_ausente, erros(validar(dataProva = null)).data)
    }

    @Test
    fun `prova hoje recusa por ser data passada`() {
        assertEquals(R.string.criar_erro_data_passada, erros(validar(dataProva = hoje)).data)
    }

    @Test
    fun `prova ontem recusa por ser data passada`() {
        assertEquals(R.string.criar_erro_data_passada, erros(validar(dataProva = hoje.minusDays(1))).data)
    }

    @Test
    fun `exatamente quatro semanas passa`() {
        // De 17/08 a 07/09: a prova cai na 4ª semana. 10 km partindo de 10 cabe em 1 de build.
        assertTrue(validar(alvo = "10", dataProva = LocalDate.of(2026, 9, 7)) is ValidacaoDoPlano.Ok)
    }

    @Test
    fun `tres semanas recusa`() {
        assertEquals(
            R.string.criar_erro_data_curta,
            erros(validar(alvo = "10", dataProva = LocalDate.of(2026, 9, 6))).data,
        )
    }

    @Test
    fun `prova antes da proxima segunda recusa pelo minimo de semanas`() {
        // Amanhã é sexta: a prova cai antes de o plano começar.
        assertEquals(R.string.criar_erro_data_curta, erros(validar(dataProva = hoje.plusDays(1))).data)
    }

    @Test
    fun `a contagem parte da proxima segunda-feira, e nao de hoje`() {
        val domingo = LocalDate.of(2026, 8, 16)
        val segunda = LocalDate.of(2026, 8, 17)

        assertEquals(4, semanasAte(domingo, LocalDate.of(2026, 9, 7)))
        assertEquals(4, semanasAte(segunda, LocalDate.of(2026, 9, 7)))
        assertEquals(4, semanasAte(hoje, LocalDate.of(2026, 9, 7)))
    }

    // -----------------------------------------------------------------------
    // O plano cabe?
    // -----------------------------------------------------------------------

    @Test
    fun `a sao silvestre partindo de 5 km nao cabe e oferece as saidas`() {
        val resultado = validar(baseline = "5", hoje = LocalDate.of(2026, 10, 21))

        assertTrue(resultado is ValidacaoDoPlano.NaoCabe)
        val motivo = (resultado as ValidacaoDoPlano.NaoCabe).motivo
        assertEquals(LocalDate.of(2027, 1, 11), motivo.primeiraProvaQueCabe)
        assertEquals(12.5, motivo.maiorAlvoQueCabeKm, TOLERANCIA)
    }

    @Test
    fun `a sao silvestre partindo de 10 km cabe`() {
        assertTrue(validar(baseline = "10", hoje = LocalDate.of(2026, 10, 21)) is ValidacaoDoPlano.Ok)
    }

    @Test
    fun `campo errado vem antes de nao caber`() {
        // "Não cabe" fala de três campos ao mesmo tempo, e só faz sentido com os cinco certos.
        val resultado = validar(nome = "", baseline = "5", hoje = LocalDate.of(2026, 10, 21))

        assertEquals(R.string.criar_erro_nome, erros(resultado).nome)
    }

    // -----------------------------------------------------------------------
    // As distâncias
    // -----------------------------------------------------------------------

    @Test
    fun `notacao cientifica colada nao vira distancia`() {
        assertNotNull(erros(validar(alvo = "1e3")).alvo)
    }

    @Test
    fun `distancia zero recusa nos tres campos`() {
        assertNotNull(erros(validar(alvo = "0")).alvo)
        assertNotNull(erros(validar(baseline = "0")).baseline)
        assertNotNull(erros(validar(curta = "0")).curta)
    }

    @Test
    fun `virgula e ponto valem o mesmo`() {
        val comVirgula = validar(alvo = "15,5", baseline = "7,5", curta = "2,5") as ValidacaoDoPlano.Ok
        val comPonto = validar(alvo = "15.5", baseline = "7.5", curta = "2.5") as ValidacaoDoPlano.Ok

        assertEquals(comVirgula.alvoKm, comPonto.alvoKm, TOLERANCIA)
        assertEquals(comVirgula.baselineKm, comPonto.baselineKm, TOLERANCIA)
        assertEquals(comVirgula.curtaKm, comPonto.curtaKm, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // Todos de uma vez
    // -----------------------------------------------------------------------

    @Test
    fun `um formulario vazio acusa todos os campos de uma vez`() {
        val erros = erros(validar(nome = "", alvo = "", baseline = "", curta = "", dataProva = null))

        assertNotNull(erros.nome)
        assertNotNull(erros.data)
        assertNotNull(erros.alvo)
        assertNotNull(erros.baseline)
        assertNotNull(erros.curta)
    }

    // -----------------------------------------------------------------------
    // O total que a tela mostra antes de gerar
    // -----------------------------------------------------------------------

    @Test
    fun `o total de semanas aparece assim que a data serve`() {
        assertEquals(20, semanasAte(hoje, LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `sem data, com data passada ou curta demais nao ha total`() {
        assertNull(semanasAte(hoje, null))
        assertNull(semanasAte(hoje, hoje))
        assertNull(semanasAte(hoje, LocalDate.of(2026, 9, 6)))
    }

    private fun validar(
        nome: String = "São Silvestre",
        dataProva: LocalDate? = LocalDate.of(2026, 12, 31),
        alvo: String = "15",
        baseline: String = "10",
        curta: String = "3",
        hoje: LocalDate = this.hoje,
    ) = validarRascunho(
        nome = nome,
        dataProva = dataProva,
        alvo = alvo,
        baseline = baseline,
        curta = curta,
        hoje = hoje,
    )

    private fun erros(resultado: ValidacaoDoPlano): ErrosDoPlano {
        assertTrue("esperava falha, veio $resultado", resultado is ValidacaoDoPlano.Falhou)
        return (resultado as ValidacaoDoPlano.Falhou).erros
    }

    private companion object {
        const val TOLERANCIA = 1e-9
    }
}
