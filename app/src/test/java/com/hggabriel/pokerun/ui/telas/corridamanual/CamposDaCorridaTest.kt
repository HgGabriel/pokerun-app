package com.hggabriel.pokerun.ui.telas.corridamanual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * O formulário de registro manual (`F1-T16`, docs/03 §3.10, RN-04 e RN-28).
 *
 * Escrito **antes** da implementação (`EXECUCAO.md §3.2`), porque a tarefa cita regra.
 *
 * **RN-04 é a única regra de negócio da tela**, e é a que some numa revisão de olho:
 * *"registro retroativo é livre até 7 dias; após esse período, exige confirmação
 * explícita"*. Três defeitos cabem nessa frase sem aparecer em diff nenhum:
 *
 * - **Contar em horas em vez de dias de calendário.** `7 × 24h` e "sete dias" divergem
 *   sempre que a corrida foi de manhã e o app é aberto de noite: a corrida das 5h de
 *   sete dias atrás está a 185 horas de distância, e o diálogo de confirmação aparece
 *   na frente de quem está dentro da regra. É o mesmo erro que `CalendarioDoPlano`
 *   existe para impedir em `semana_ref`, um campo adiante.
 * - **Contar no fuso do aparelho.** RN-28 manda tudo que é fronteira de dia sair do
 *   fuso **do plano**. Uma corrida de domingo 22h em São Paulo é segunda 10h em Tóquio,
 *   e o dia de referência muda com ela.
 * - **Errar a beirada.** *"Livre até 7 dias"* inclui o sétimo. Um `>=` no lugar do `>`
 *   põe um diálogo de confirmação na frente de quem está dentro da regra.
 *
 * O resto são as validações de forma do formulário, que existem pela mesma razão das de
 * `F1-T10`: elas impedem que o repositório receba entrada que a spec não descreve — e
 * `Corrida` é **append-only** (RN-24), então um valor errado gravado aqui não é
 * corrigido, é substituído por outro documento para sempre.
 */
class CamposDaCorridaTest {

    private val fuso: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val toquio: ZoneId = ZoneId.of("Asia/Tokyo")

    /** Quinta-feira, 20/08/2026, 10h em São Paulo. O "agora" de quase todo caso. */
    private val agora: Instant =
        LocalDate.of(2026, 8, 20).atTime(10, 0).atZone(fuso).toInstant()

    private fun validar(
        data: LocalDate? = LocalDate.of(2026, 8, 20),
        hora: LocalTime? = LocalTime.of(6, 30),
        km: String = "8",
        horas: String = "",
        minutos: String = "45",
        segundos: String = "",
        fcMedia: String = "",
        esforco: String = "",
        fuso: ZoneId = this.fuso,
        agora: Instant = this.agora,
    ) = validarCorrida(
        data = data,
        hora = hora,
        km = km,
        horas = horas,
        minutos = minutos,
        segundos = segundos,
        fcMedia = fcMedia,
        esforco = esforco,
        fuso = fuso,
        agora = agora,
    )

    private fun ok(
        data: LocalDate? = LocalDate.of(2026, 8, 20),
        hora: LocalTime? = LocalTime.of(6, 30),
        km: String = "8",
        horas: String = "",
        minutos: String = "45",
        segundos: String = "",
        fcMedia: String = "",
        esforco: String = "",
        fuso: ZoneId = this.fuso,
        agora: Instant = this.agora,
    ) = validar(data, hora, km, horas, minutos, segundos, fcMedia, esforco, fuso, agora)
        as ValidacaoDaCorrida.Ok

    private fun erros(
        data: LocalDate? = LocalDate.of(2026, 8, 20),
        hora: LocalTime? = LocalTime.of(6, 30),
        km: String = "8",
        horas: String = "",
        minutos: String = "45",
        segundos: String = "",
        fcMedia: String = "",
        esforco: String = "",
        fuso: ZoneId = this.fuso,
        agora: Instant = this.agora,
    ) = validar(data, hora, km, horas, minutos, segundos, fcMedia, esforco, fuso, agora)
        as ValidacaoDaCorrida.Falhou

    // -----------------------------------------------------------------------
    // RN-04 — a confirmação do registro retroativo
    // -----------------------------------------------------------------------

    @Test
    fun `corrida de hoje nao pede confirmacao`() {
        assertFalse(ok().exigeConfirmacao)
    }

    @Test
    fun `corrida de sete dias atras ainda e livre`() {
        // RN-04 diz "livre **até** 7 dias". O sétimo está dentro, e um `>=` no corte
        // põe um diálogo na frente de quem seguiu a regra.
        val sete = ok(data = LocalDate.of(2026, 8, 13))

        assertFalse(sete.exigeConfirmacao)
    }

    @Test
    fun `corrida de oito dias atras pede confirmacao`() {
        assertTrue(ok(data = LocalDate.of(2026, 8, 12)).exigeConfirmacao)
    }

    @Test
    fun `o corte conta dias de calendario e nao multiplos de 24 horas`() {
        // O defeito que este teste existe para pegar: contar `Duration` entre os dois
        // instantes. A corrida foi às 5h de 13/08 e o app está aberto às 22h de 20/08 —
        // **sete** dias de calendário, e 185 horas de relógio. Contando em horas, o
        // diálogo de RN-04 aparece na frente de quem está dentro da regra, e o erro só
        // se manifesta em quem correu de manhã e registrou de noite.
        //
        // A divergência só existe neste sentido: oito dias de calendário são sempre mais
        // de 7×24h, mas sete dias de calendário chegam a 191 horas.
        val noite = LocalDate.of(2026, 8, 20).atTime(22, 0).atZone(fuso).toInstant()

        val cedoNoDia = ok(
            data = LocalDate.of(2026, 8, 13),
            hora = LocalTime.of(5, 0),
            agora = noite,
        )

        assertFalse(cedoNoDia.exigeConfirmacao)
    }

    @Test
    fun `o corte sai do fuso do plano e nao do aparelho`() {
        // RN-28. A data digitada é a data **no fuso do plano**, então o dia da corrida é
        // o mesmo dos dois lados; o que muda de fuso para fuso é que dia é hoje. Este
        // instante — 17/08 às 20h em São Paulo — já é 18/08 às 8h em Tóquio, e são as
        // doze horas de diferença que jogam a mesma corrida de 10/08 de sete para oito
        // dias de distância.
        val dia = LocalDate.of(2026, 8, 10)
        val instanteDeAgora =
            LocalDate.of(2026, 8, 17).atTime(20, 0).atZone(fuso).toInstant()

        val emSaoPaulo = ok(
            data = dia,
            hora = LocalTime.of(6, 0),
            agora = instanteDeAgora,
        )
        assertFalse(emSaoPaulo.exigeConfirmacao)

        val emToquio = ok(
            data = dia,
            hora = LocalTime.of(6, 0),
            fuso = toquio,
            agora = instanteDeAgora,
        )
        assertTrue(emToquio.exigeConfirmacao)
    }

    // -----------------------------------------------------------------------
    // O instante gravado
    // -----------------------------------------------------------------------

    @Test
    fun `o instante sai da data e da hora no fuso do plano`() {
        // É este instante que vira `data_hora_inicio`, e é dele que `semana_ref` é
        // derivado uma linha adiante (RN-02, RN-28). Montá-lo no fuso do aparelho é a
        // corrida de domingo à noite que aparece na semana seguinte.
        //
        // **Os dois fusos são obrigatórios, e é a lição de um defeito plantado.** A
        // primeira versão deste teste conferia só o plano de São Paulo, e trocar
        // `atZone(fuso)` por `atZone(systemDefault())` na implementação passava verde:
        // a máquina que roda a suíte **é** São Paulo. Com Tóquio no par, qualquer
        // máquina do mundo derruba pelo menos uma das duas asserções.
        val quando = LocalDate.of(2026, 8, 16).atTime(22, 0)

        assertEquals(
            quando.atZone(fuso).toInstant(),
            ok(data = quando.toLocalDate(), hora = quando.toLocalTime()).dataHoraInicio,
        )

        assertEquals(
            quando.atZone(toquio).toInstant(),
            ok(
                data = quando.toLocalDate(),
                hora = quando.toLocalTime(),
                fuso = toquio,
                // Em Tóquio a mesma data e hora é um instante 12h mais cedo, e o
                // "agora" precisa continuar depois dele para o caso não virar futuro.
                agora = LocalDate.of(2026, 8, 17).atTime(10, 0).atZone(fuso).toInstant(),
            ).dataHoraInicio,
        )
    }

    // -----------------------------------------------------------------------
    // Data e hora — ausência e futuro
    // -----------------------------------------------------------------------

    @Test
    fun `data ausente acusa`() {
        assertNotNull(erros(data = null).erros.dataHora)
    }

    @Test
    fun `hora ausente acusa`() {
        // Nulo é *"ainda não respondeu"*, como em `F1-T10`: tratar a hora ausente como
        // meia-noite inventaria uma resposta que o usuário não deu, e meia-noite é a
        // fronteira que decide a semana da corrida (RN-01).
        assertNotNull(erros(hora = null).erros.dataHora)
    }

    @Test
    fun `corrida no futuro acusa`() {
        val depois = erros(
            data = LocalDate.of(2026, 8, 21),
            hora = LocalTime.of(6, 0),
        )

        assertNotNull(depois.erros.dataHora)
    }

    @Test
    fun `corrida daqui a instantes acusa, e a de instantes atras passa`() {
        // A beirada do futuro é o próprio "agora", e ela é do lado de dentro: quem
        // termina de correr e registra na hora digita a hora que acabou de passar.
        val logoAntes = ok(
            data = LocalDate.of(2026, 8, 20),
            hora = LocalTime.of(9, 59),
        )
        assertFalse(logoAntes.exigeConfirmacao)

        assertNotNull(
            erros(data = LocalDate.of(2026, 8, 20), hora = LocalTime.of(10, 1)).erros.dataHora,
        )
    }

    // -----------------------------------------------------------------------
    // Distância — a mesma forma dos outros dois campos de km do app
    // -----------------------------------------------------------------------

    @Test
    fun `a distancia aceita virgula`() {
        assertEquals(8.4, ok(km = "8,4").km, 0.0001)
    }

    @Test
    fun `distancia vazia acusa`() {
        assertNotNull(erros(km = "").erros.km)
    }

    @Test
    fun `distancia zero acusa`() {
        // Sem distância não há corrida: `formatarPace` devolve nulo, a aderência conta
        // uma sessão que não aconteceu e o longão de RN-10 compara contra zero.
        assertNotNull(erros(km = "0").erros.km)
    }

    // -----------------------------------------------------------------------
    // Duração — três campos, um número
    // -----------------------------------------------------------------------

    @Test
    fun `a duracao soma os tres campos`() {
        assertEquals(
            3600L + 45 * 60 + 30,
            ok(horas = "1", minutos = "45", segundos = "30").duracaoSeg,
        )
    }

    @Test
    fun `campo vazio da duracao vale zero`() {
        // 45 minutos é `45` no campo do meio e nada nos outros dois. Exigir `0` nos
        // vazios seria pedir três respostas para um dado só.
        assertEquals(45L * 60, ok(horas = "", minutos = "45", segundos = "").duracaoSeg)
    }

    @Test
    fun `duracao zerada acusa`() {
        assertNotNull(erros(horas = "", minutos = "", segundos = "").erros.duracao)
    }

    @Test
    fun `minuto e segundo acima de 59 acusam`() {
        // O campo diz "min" e "s". Aceitar 90 minutos e somar em silêncio grava 1h30
        // sob um rótulo que diz outra coisa, e a corrida é append-only (RN-24).
        assertNotNull(erros(minutos = "60").erros.duracao)
        assertNotNull(erros(minutos = "45", segundos = "60").erros.duracao)
    }

    @Test
    fun `duracao com letra acusa`() {
        assertNotNull(erros(minutos = "45m").erros.duracao)
    }

    // -----------------------------------------------------------------------
    // Os dois campos opcionais
    // -----------------------------------------------------------------------

    @Test
    fun `frequencia e esforco vazios sao nulos e nao acusam`() {
        val vazios = ok(fcMedia = "", esforco = "")

        assertNull(vazios.fcMedia)
        assertNull(vazios.esforcoPercebido)
    }

    @Test
    fun `frequencia preenchida entra como numero`() {
        assertEquals(152, ok(fcMedia = "152").fcMedia)
    }

    @Test
    fun `frequencia fora de forma acusa`() {
        assertNotNull(erros(fcMedia = "0").erros.fcMedia)
        assertNotNull(erros(fcMedia = "1520").erros.fcMedia)
        assertNotNull(erros(fcMedia = "abc").erros.fcMedia)
    }

    @Test
    fun `o esforco vai de 1 a 10`() {
        // docs/03 §3.10 e docs/05 §4.1 fixam a escala. Zero e 11 não são esforço
        // percebido; são dedo escorregado num campo que ninguém é obrigado a preencher.
        assertEquals(1, ok(esforco = "1").esforcoPercebido)
        assertEquals(10, ok(esforco = "10").esforcoPercebido)
        assertNotNull(erros(esforco = "0").erros.esforco)
        assertNotNull(erros(esforco = "11").erros.esforco)
    }

    // -----------------------------------------------------------------------
    // Os erros saem juntos
    // -----------------------------------------------------------------------

    @Test
    fun `os campos errados acusam todos na mesma passada`() {
        // Mesmo motivo de `ErrosDoPlano` em `F1-T10`: acusar em série faria o usuário
        // descobrir o terceiro erro no terceiro toque no botão.
        val falha = erros(
            data = null,
            km = "",
            horas = "",
            minutos = "",
            segundos = "",
            fcMedia = "abc",
            esforco = "99",
        ).erros

        assertNotNull(falha.dataHora)
        assertNotNull(falha.km)
        assertNotNull(falha.duracao)
        assertNotNull(falha.fcMedia)
        assertNotNull(falha.esforco)
        assertTrue(falha.algum)
    }

    @Test
    fun `formulario inteiro certo nao tem erro nenhum`() {
        val bom = validar(km = "10", horas = "1", minutos = "2", segundos = "3", fcMedia = "150", esforco = "7")

        assertTrue(bom is ValidacaoDaCorrida.Ok)
        assertFalse(ErrosDaCorrida().algum)
    }
}
