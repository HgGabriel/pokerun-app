package com.hggabriel.pokerun.ui.telas.entrarcomcodigo

import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A prévia do plano encontrado pelo código (`F1-T14`, docs/03 §3.8, RN-13 e RN-27).
 *
 * **A tela é uma decisão, não um formulário.** O usuário digita seis caracteres e o app
 * responde três coisas: qual plano é esse, dá para entrar nele, e o que acontece com o
 * plano em que ele já está. As três saem desta função pura, e é por isso que ela tem
 * teste enquanto a tela não tem.
 *
 * **RN-13 é a que não pode escorregar.** Entrar num plano com outro já ativo **não pode**
 * trocar o primário em silêncio: o diálogo é obrigatório e nomeia os dois planos. Quem
 * ainda não tem plano ativo não vê diálogo nenhum, porque não há troca — há preenchimento
 * de campo vazio, que é a mesma leitura que `F1-T11` faz ao criar.
 *
 * **RN-27 tem dois caminhos aqui também.** Um plano encerrado não recebe corridas (RN-07),
 * então entrar nele seria um beco sem saída oferecido por um botão — e "encerrado" não é
 * só o booleano do dono: o plano acaba sozinho ao fim da semana da prova, sem ninguém
 * tocar no documento.
 */
class EntradaNoPlanoTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val toquio = ZoneId.of("Asia/Tokyo")

    /** O relógio é parâmetro: RN-27 encerra por data, e `now()` mudaria a resposta em 2027. */
    private val antesDaProva = LocalDate.of(2026, 8, 14).atStartOfDay(saoPaulo).toInstant()
    private val depoisDaProva = LocalDate.of(2027, 1, 2).atStartOfDay(saoPaulo).toInstant()

    private fun plano(
        id: String = "plano-alvo",
        nome: String = "São Silvestre 2026",
        encerrado: Boolean = false,
        fuso: ZoneId = saoPaulo,
        dataProva: Instant = LocalDate.of(2026, 12, 31).atStartOfDay(saoPaulo).toInstant(),
        distanciaAlvoKm: Double = 15.0,
    ) = Plano(
        id = id,
        nome = nome,
        distanciaAlvoKm = distanciaAlvoKm,
        dataProva = dataProva,
        fuso = fuso,
        ownerUid = "uid-dono",
        codigoConvite = "FYQJE6",
        encerrado = encerrado,
        parametros = ParametrosDeGeracao(
            dataProva = dataProva,
            distanciaAlvoKm = distanciaAlvoKm,
            baselineKm = 7.5,
            sessoesPorSemana = 3,
        ),
    )

    // -----------------------------------------------------------------------
    // A prévia (docs/03 §3.8)
    // -----------------------------------------------------------------------

    @Test
    fun `a previa traz o nome, a data da prova e a distancia do plano`() {
        val previa = previaDaEntrada(
            plano = plano(),
            planoAtivoId = null,
            nomeDoPlanoAtivo = null,
            agora = antesDaProva,
        )

        assertEquals("plano-alvo", previa.planoId)
        assertEquals("São Silvestre 2026", previa.nome)
        assertEquals(LocalDate.of(2026, 12, 31), previa.dataDaProva)
        assertEquals(15.0, previa.distanciaAlvoKm, 0.001)
    }

    @Test
    fun `a data da prova sai no fuso do plano, e nao no de quem le`() {
        // RN-28. A prova de 31/12 à meia-noite em Tóquio é 30/12 em São Paulo: quem
        // recebe o convite de outro fuso precisa ver a data que o dono marcou.
        val previa = previaDaEntrada(
            plano = plano(
                fuso = toquio,
                dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(toquio).toInstant(),
            ),
            planoAtivoId = null,
            nomeDoPlanoAtivo = null,
            agora = antesDaProva,
        )

        assertEquals(LocalDate.of(2026, 12, 31), previa.dataDaProva)
    }

    // -----------------------------------------------------------------------
    // RN-27 — não se entra em plano encerrado
    // -----------------------------------------------------------------------

    @Test
    fun `plano aberto aceita entrada`() {
        val previa = previaDaEntrada(plano(), null, null, antesDaProva)

        assertFalse(previa.encerrado)
        assertTrue(previa.podeEntrar)
    }

    @Test
    fun `plano encerrado pelo dono nao aceita entrada`() {
        // RN-07: ele é somente leitura, e as corridas de quem entrasse não contariam
        // para nada. O código continua resolvendo, e a tela diz por que não dá.
        val previa = previaDaEntrada(plano(encerrado = true), null, null, antesDaProva)

        assertTrue(previa.encerrado)
        assertFalse(previa.podeEntrar)
    }

    @Test
    fun `plano que passou da prova nao aceita entrada, mesmo com o documento intacto`() {
        // O segundo caminho de RN-27, o que não passa por ninguém. Sem ele, o código de
        // um plano de janeiro continuaria abrindo a porta em dezembro.
        val previa = previaDaEntrada(plano(), null, null, depoisDaProva)

        assertTrue(previa.encerrado)
        assertFalse(previa.podeEntrar)
    }

    @Test
    fun `o dia da prova inteiro ainda aceita entrada`() {
        // A fronteira é a meia-noite do dia seguinte, no fuso do plano, como em
        // `CalendarioDoPlano`. Quem corre de manhã ainda tem o dia inteiro.
        val naProva = LocalDate.of(2026, 12, 31).atTime(23, 59).atZone(saoPaulo).toInstant()

        assertTrue(previaDaEntrada(plano(), null, null, naProva).podeEntrar)
    }

    // -----------------------------------------------------------------------
    // RN-13 — a troca do plano ativo nunca é silenciosa
    // -----------------------------------------------------------------------

    @Test
    fun `sem plano ativo nao ha escolha a fazer`() {
        // Não há troca: há campo vazio a preencher, que é a mesma leitura de `F1-T11`.
        val previa = previaDaEntrada(plano(), planoAtivoId = null, nomeDoPlanoAtivo = null, agora = antesDaProva)

        assertFalse(previa.exigeEscolha)
        assertNull(previa.nomeDoPlanoAtivo)
    }

    @Test
    fun `com plano ativo a escolha e obrigatoria e o dialogo nomeia o outro plano`() {
        val previa = previaDaEntrada(
            plano = plano(),
            planoAtivoId = "outro-plano",
            nomeDoPlanoAtivo = "Meia de Interlagos",
            agora = antesDaProva,
        )

        assertTrue(previa.exigeEscolha)
        assertEquals("Meia de Interlagos", previa.nomeDoPlanoAtivo)
    }

    @Test
    fun `a escolha continua obrigatoria quando o nome do plano ativo nao foi lido`() {
        // A leitura do plano ativo é só para a frase do diálogo, e pode falhar. Deixar
        // de exigir a escolha por causa disso trocaria o plano primário em silêncio,
        // que é exatamente o que RN-13 proíbe.
        val previa = previaDaEntrada(plano(), "outro-plano", null, antesDaProva)

        assertTrue(previa.exigeEscolha)
        assertNull(previa.nomeDoPlanoAtivo)
    }

    @Test
    fun `plano ativo encerrado ainda exige a escolha`() {
        // Caso real: RN-27 encerra o plano ao fim da semana da prova sem tocar em
        // `users/{uid}.plano_ativo_id`, que continua apontando para ele (decisão nº 34).
        // Apontar o campo para o plano novo por conta própria seria a troca silenciosa.
        val previa = previaDaEntrada(plano(), "plano-encerrado", "Corrida do Trabalhador", antesDaProva)

        assertTrue(previa.exigeEscolha)
    }
}
