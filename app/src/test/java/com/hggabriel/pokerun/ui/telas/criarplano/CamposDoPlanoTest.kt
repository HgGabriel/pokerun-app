package com.hggabriel.pokerun.ui.telas.criarplano

import com.hggabriel.pokerun.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * As três validações da `CreatePlanScreen` (`F1-T10`, docs/03 §3.5), escritas **antes**
 * da implementação (`EXECUCAO.md §3.2`).
 *
 * Data no futuro, mínimo de 8 semanas e alvo maior que a distância confortável. As três
 * existem para a mesma coisa: **impedir que o `GeradorDePlano` receba entrada que ele
 * recusa**. Ele falha alto com `require`, e uma tela que deixasse passar 7 semanas
 * derrubaria o app em vez de mostrar uma mensagem.
 *
 * A que mais escapa é a contagem: `N` conta da **segunda-feira da semana corrente**
 * (RN-01), e não do dia em que a pessoa está preenchendo. Num domingo a diferença é de
 * seis dias, que é quase uma semana inteira do denominador.
 */
class CamposDoPlanoTest {

    /** Uma quinta-feira. A segunda-feira desta semana é 10/08. */
    private val hoje: LocalDate = LocalDate.of(2026, 8, 13)

    // -----------------------------------------------------------------------
    // O caminho feliz
    // -----------------------------------------------------------------------

    @Test
    fun `formulario coerente e valido e devolve o total de semanas`() {
        val resultado = validar(dataProva = LocalDate.of(2026, 12, 31))

        assertTrue(resultado is ValidacaoDoPlano.Ok)
        assertEquals(21, (resultado as ValidacaoDoPlano.Ok).semanas)
    }

    @Test
    fun `o formulario valido devolve os campos ja convertidos`() {
        val ok = validar(
            nome = "  São Silvestre  ",
            alvo = "15",
            baseline = "7,5",
            dataProva = LocalDate.of(2026, 12, 31),
        ) as ValidacaoDoPlano.Ok

        assertEquals("São Silvestre", ok.nome)
        assertEquals(15.0, ok.alvoKm, TOLERANCIA)
        assertEquals(7.5, ok.baselineKm, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // Nome
    // -----------------------------------------------------------------------

    @Test
    fun `nome em branco recusa`() {
        val erros = erros(validar(nome = "   ", dataProva = LocalDate.of(2026, 12, 31)))

        assertNotNull(erros.nome)
    }

    // -----------------------------------------------------------------------
    // Data no futuro
    // -----------------------------------------------------------------------

    @Test
    fun `data nao escolhida recusa`() {
        // O `DatePicker` abre vazio: nulo é "ainda não respondeu", não "hoje".
        assertEquals(R.string.criar_erro_data_ausente, erros(validar(dataProva = null)).data)
    }

    @Test
    fun `prova hoje recusa por ser data passada, e nao pelo minimo de semanas`() {
        // **A asserção é a mensagem, e não "tem erro".** As duas validações caem no mesmo
        // campo, e o mínimo de 8 semanas recusa tudo que a de data futura recusaria — um
        // teste que só pedisse "não nulo" passaria com a validação de data apagada.
        // Descoberto plantando exatamente esse defeito.
        assertEquals(R.string.criar_erro_data_passada, erros(validar(dataProva = hoje)).data)
    }

    @Test
    fun `prova ontem recusa por ser data passada`() {
        assertEquals(
            R.string.criar_erro_data_passada,
            erros(validar(dataProva = hoje.minusDays(1))).data,
        )
    }

    // -----------------------------------------------------------------------
    // Mínimo de 8 semanas
    // -----------------------------------------------------------------------

    @Test
    fun `exatamente oito semanas passa`() {
        // 10/08 é a segunda desta semana; 28/09 fecha 49 dias, que dão 8 semanas.
        val resultado = validar(dataProva = LocalDate.of(2026, 9, 28))

        assertEquals(8, (resultado as ValidacaoDoPlano.Ok).semanas)
    }

    @Test
    fun `sete semanas recusa`() {
        // O par do teste acima: sem ele, `>` e `>=` passam os dois.
        assertEquals(
            R.string.criar_erro_data_curta,
            erros(validar(dataProva = LocalDate.of(2026, 9, 27))).data,
        )
    }

    @Test
    fun `a prova de amanha recusa pelo minimo de semanas, e nao pela data`() {
        // A data é futura e a mensagem tem de dizer o que está errado de verdade: mandar
        // "escolha uma data futura" para quem escolheu amanhã não ajuda ninguém.
        assertEquals(R.string.criar_erro_data_curta, erros(validar(dataProva = hoje.plusDays(1))).data)
    }

    @Test
    fun `a contagem parte da segunda-feira da semana corrente, e nao de hoje`() {
        // Num domingo a diferença é de seis dias. Contando de hoje, 28/09 daria 7
        // semanas e a tela recusaria um plano que o gerador aceita.
        val domingo = LocalDate.of(2026, 8, 16)
        val resultado = validar(dataProva = LocalDate.of(2026, 9, 28), hoje = domingo)

        assertEquals(8, (resultado as ValidacaoDoPlano.Ok).semanas)
    }

    // -----------------------------------------------------------------------
    // Alvo maior que a distância confortável
    // -----------------------------------------------------------------------

    @Test
    fun `alvo igual a distancia confortavel recusa`() {
        val erros = erros(
            validar(alvo = "10", baseline = "10", dataProva = LocalDate.of(2026, 12, 31)),
        )

        assertNotNull(erros.alvo)
    }

    @Test
    fun `alvo menor que a distancia confortavel recusa`() {
        val erros = erros(
            validar(alvo = "8", baseline = "10", dataProva = LocalDate.of(2026, 12, 31)),
        )

        assertNotNull(erros.alvo)
    }

    @Test
    fun `alvo um decimo maior ja passa`() {
        val resultado = validar(
            alvo = "10,1",
            baseline = "10",
            dataProva = LocalDate.of(2026, 12, 31),
        )

        assertTrue(resultado is ValidacaoDoPlano.Ok)
    }

    // -----------------------------------------------------------------------
    // A forma das duas distâncias, herdada de F1-T08
    // -----------------------------------------------------------------------

    @Test
    fun `notacao cientifica colada nao vira distancia`() {
        // `toDoubleOrNull` aceita `1e3` em silêncio, e 1.000 km gera as 21 semanas
        // inteiras erradas. É a mesma trava de `F1-T08`, e é por isso que a leitura
        // dos dois campos é a mesma função.
        assertNotNull(erros(validar(alvo = "1e3", dataProva = LocalDate.of(2026, 12, 31))).alvo)
    }

    @Test
    fun `distancia zero recusa nos dois campos`() {
        val alvoZero = erros(validar(alvo = "0", dataProva = LocalDate.of(2026, 12, 31)))
        val baseZero = erros(validar(baseline = "0", dataProva = LocalDate.of(2026, 12, 31)))

        assertNotNull(alvoZero.alvo)
        assertNotNull(baseZero.baseline)
    }

    @Test
    fun `virgula e ponto valem o mesmo nos dois campos`() {
        val comVirgula = validar(
            alvo = "15,5",
            baseline = "7,5",
            dataProva = LocalDate.of(2026, 12, 31),
        ) as ValidacaoDoPlano.Ok
        val comPonto = validar(
            alvo = "15.5",
            baseline = "7.5",
            dataProva = LocalDate.of(2026, 12, 31),
        ) as ValidacaoDoPlano.Ok

        assertEquals(comVirgula.alvoKm, comPonto.alvoKm, TOLERANCIA)
        assertEquals(comVirgula.baselineKm, comPonto.baselineKm, TOLERANCIA)
    }

    // -----------------------------------------------------------------------
    // Vários erros de uma vez
    // -----------------------------------------------------------------------

    @Test
    fun `um formulario vazio acusa todos os campos de uma vez`() {
        // Acusar um por vez faria o usuário descobrir o terceiro erro no terceiro
        // toque. Os quatro saem juntos.
        val erros = erros(validar(nome = "", alvo = "", baseline = "", dataProva = null))

        assertNotNull(erros.nome)
        assertNotNull(erros.data)
        assertNotNull(erros.alvo)
        assertNotNull(erros.baseline)
    }

    @Test
    fun `formulario valido nao acusa campo nenhum`() {
        val resultado = validar(dataProva = LocalDate.of(2026, 12, 31))

        assertNull((resultado as? ValidacaoDoPlano.Falhou)?.erros)
    }

    // -----------------------------------------------------------------------
    // O total de semanas que a tela mostra antes de gerar
    // -----------------------------------------------------------------------

    @Test
    fun `o total de semanas aparece assim que a data serve`() {
        // Acrescentado depois do verde: a primeira versão calculava o número reusando a
        // validação inteira com `alvo` e `baseline` de mentira, e aqueles dois valores
        // violavam a regra de alvo maior — o número era nulo sempre e a linha nunca
        // aparecia. Foi o emulador que pegou, porque nenhum teste pedia o número.
        assertEquals(21, semanasAte(hoje, LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `sem data escolhida nao ha total`() {
        assertNull(semanasAte(hoje, null))
    }

    @Test
    fun `data passada nao tem total`() {
        assertNull(semanasAte(hoje, hoje.minusDays(1)))
        assertNull(semanasAte(hoje, hoje))
    }

    @Test
    fun `plano curto demais nao tem total, porque quem fala e a mensagem de erro`() {
        assertNull(semanasAte(hoje, LocalDate.of(2026, 9, 27)))
        assertEquals(8, semanasAte(hoje, LocalDate.of(2026, 9, 28)))
    }

    // -----------------------------------------------------------------------
    // Apoio
    // -----------------------------------------------------------------------

    private fun validar(
        nome: String = "São Silvestre",
        dataProva: LocalDate?,
        alvo: String = "15",
        baseline: String = "5",
        hoje: LocalDate = this.hoje,
    ) = validarRascunho(
        nome = nome,
        dataProva = dataProva,
        alvo = alvo,
        baseline = baseline,
        hoje = hoje,
    )

    private fun erros(resultado: ValidacaoDoPlano): ErrosDoPlano =
        (resultado as ValidacaoDoPlano.Falhou).erros

    private companion object {
        const val TOLERANCIA = 1e-9
    }
}
