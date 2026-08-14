package com.hggabriel.pokerun.ui.telas.listadeplanos

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
 * A lista de planos e a troca do ativo (`F1-T12`, docs/03 §3.4, RN-12 e RN-13).
 *
 * **Três grupos, e a ordem entre eles é a regra.** RN-12 dá **um** plano ativo primário
 * por vez; RN-15 mantém os dormentes visíveis para consulta; D-05 manda os encerrados
 * para o fim. A tela desenha as três coisas, mas quem decide qual plano é qual é a
 * função pura daqui — e é ela que tem teste, porque "qual dos meus três planos recebe as
 * minhas corridas" é a pergunta que a tela existe para responder.
 *
 * **RN-28 aparece num lugar que surpreende:** a data da prova. Ela é `Instant` no
 * documento e vira `LocalDate` para a tela, e a conversão tem de usar o fuso **do
 * plano**. No fuso do aparelho, a prova de 31/12 às 00h em São Paulo vira 30/12 para
 * quem abrir o app em Los Angeles — e o número da contagem regressiva da Home passaria a
 * discordar da data escrita na lista.
 */
class ItensDePlanoTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    private fun plano(
        id: String,
        nome: String = id,
        encerrado: Boolean = false,
        dataProva: Instant = LocalDate.of(2026, 12, 31).atStartOfDay(saoPaulo).toInstant(),
        fuso: ZoneId = saoPaulo,
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

    private fun situacoes(itens: List<ItemDePlano>) = itens.map { it.id to it.situacao }

    // -----------------------------------------------------------------------
    // RN-12 — um plano ativo primário por vez
    // -----------------------------------------------------------------------

    @Test
    fun `o plano apontado por plano ativo id e o unico ativo`() {
        // RN-12: as corridas contam apenas para ele. Dois marcados em `leitura` na mesma
        // tela seria a pergunta "para qual dos dois?" sem resposta.
        val itens = itensDePlano(
            planos = listOf(plano("a"), plano("b"), plano("c")),
            planoAtivoId = "b",
        )

        assertEquals(
            listOf(
                "b" to SituacaoDoPlano.ATIVO,
                "a" to SituacaoDoPlano.DORMENTE,
                "c" to SituacaoDoPlano.DORMENTE,
            ),
            situacoes(itens),
        )
    }

    @Test
    fun `sem plano ativo, todo plano aberto e dormente`() {
        // Acontece de verdade: quem entra por convite em plano de outra pessoa sem ter
        // criado o seu tem `plano_ativo_id` nulo até tornar um deles ativo.
        val itens = itensDePlano(listOf(plano("a"), plano("b")), planoAtivoId = null)

        assertTrue(itens.all { it.situacao == SituacaoDoPlano.DORMENTE })
    }

    @Test
    fun `plano ativo que ja foi encerrado conta como encerrado`() {
        // RN-07: plano encerrado é somente-leitura, e RN-27 diz que ele não reabre. Ele
        // continua sendo o `plano_ativo_id` do documento — a Home tem estado próprio para
        // isso —, mas marcá-lo em `leitura` no meio da lista diria que ele ainda recebe
        // corridas. Vai para o grupo do fim, com os outros encerrados.
        val itens = itensDePlano(
            planos = listOf(plano("velho", encerrado = true), plano("novo")),
            planoAtivoId = "velho",
        )

        assertEquals(
            listOf("novo" to SituacaoDoPlano.DORMENTE, "velho" to SituacaoDoPlano.ENCERRADO),
            situacoes(itens),
        )
    }

    // -----------------------------------------------------------------------
    // A ordem dos três grupos (docs/03 §3.4, D-05)
    // -----------------------------------------------------------------------

    @Test
    fun `o ativo vem antes dos dormentes`() {
        val itens = itensDePlano(listOf(plano("a"), plano("b")), planoAtivoId = "b")

        assertEquals("b", itens.first().id)
    }

    @Test
    fun `os encerrados vao para o fim, mesmo chegando primeiro`() {
        // D-05: encerrados agrupados ao final. A ordem de chegada é a de
        // `users/{uid}.planos`, que cresce por `arrayUnion` — o primeiro plano da conta é
        // o primeiro do array, e é justamente ele que encerra antes.
        val itens = itensDePlano(
            planos = listOf(
                plano("encerrado-1", encerrado = true),
                plano("dormente"),
                plano("encerrado-2", encerrado = true),
                plano("ativo"),
            ),
            planoAtivoId = "ativo",
        )

        assertEquals(listOf("ativo", "dormente", "encerrado-1", "encerrado-2"), itens.map { it.id })
    }

    @Test
    fun `dentro do grupo, a ordem de chegada e preservada`() {
        // A ordem do array é a ordem em que a pessoa entrou nos planos. Reordenar por
        // nome ou por data da prova embaralharia a lista a cada plano encerrado, e a
        // especificação não pede ordenação nenhuma dentro do grupo.
        val itens = itensDePlano(
            planos = listOf(plano("terceiro"), plano("primeiro"), plano("segundo")),
            planoAtivoId = null,
        )

        assertEquals(listOf("terceiro", "primeiro", "segundo"), itens.map { it.id })
    }

    @Test
    fun `sem planos, a lista sai vazia`() {
        assertTrue(itensDePlano(emptyList(), planoAtivoId = "a").isEmpty())
    }

    // -----------------------------------------------------------------------
    // RN-13 — a troca é explícita, e não é oferecida onde não cabe
    // -----------------------------------------------------------------------

    @Test
    fun `tornar ativo so aparece em plano dormente`() {
        // RN-13 é sobre o ativo mudar por decisão explícita; RN-07 tira o encerrado da
        // conversa. Sobra o dormente, que é o único plano que a troca alcança.
        val itens = itensDePlano(
            planos = listOf(plano("ativo"), plano("dormente"), plano("fim", encerrado = true)),
            planoAtivoId = "ativo",
        )

        val porId = itens.associateBy { it.id }
        assertFalse(porId.getValue("ativo").podeTornarAtivo)
        assertTrue(porId.getValue("dormente").podeTornarAtivo)
        assertFalse(porId.getValue("fim").podeTornarAtivo)
    }

    @Test
    fun `a lista sabe qual e o plano ativo, e devolve nulo quando nao ha`() {
        // O diálogo de confirmação nomeia o plano que sai (RN-14: as corridas já
        // registradas continuam nele), e é daqui que o nome vem.
        val comAtivo = ListaDePlanosUiState.Lista(
            itensDePlano(listOf(plano("a", nome = "São Silvestre"), plano("b")), planoAtivoId = "a"),
        )
        val semAtivo = ListaDePlanosUiState.Lista(
            itensDePlano(listOf(plano("a"), plano("b")), planoAtivoId = null),
        )

        assertEquals("São Silvestre", comAtivo.ativo?.nome)
        assertNull(semAtivo.ativo)
    }

    // -----------------------------------------------------------------------
    // RN-28 — a data da prova sai no fuso do plano
    // -----------------------------------------------------------------------

    @Test
    fun `a data da prova e lida no fuso do plano, nunca no do aparelho`() {
        // A prova é 31/12 à meia-noite em São Paulo, que é 31/12 às 03h em UTC e ainda
        // 30/12 às 19h em Los Angeles. Quem converter no fuso do aparelho escreve
        // `30 de dezembro` para quem abrir o app viajando, e a data passa a discordar da
        // contagem regressiva da Home, que usa o fuso do plano.
        val prova = LocalDate.of(2026, 12, 31).atStartOfDay(saoPaulo).toInstant()

        val item = itensDePlano(
            planos = listOf(plano("a", dataProva = prova, fuso = saoPaulo)),
            planoAtivoId = "a",
        ).single()

        assertEquals(LocalDate.of(2026, 12, 31), item.dataDaProva)
    }

    @Test
    fun `o fuso que manda e o do plano, e nao o do primeiro da lista`() {
        // Dois planos criados em fusos diferentes convivem na mesma conta: quem viaja
        // cria o segundo plano em outro lugar. Cada linha lê o seu.
        val instante = LocalDate.of(2027, 1, 1).atStartOfDay(ZoneId.of("UTC")).toInstant()

        val itens = itensDePlano(
            planos = listOf(
                plano("sp", dataProva = instante, fuso = saoPaulo),
                plano("utc", dataProva = instante, fuso = ZoneId.of("UTC")),
            ),
            planoAtivoId = null,
        )

        val porId = itens.associateBy { it.id }
        // 01/01/2027 00h UTC é 31/12/2026 21h em São Paulo.
        assertEquals(LocalDate.of(2026, 12, 31), porId.getValue("sp").dataDaProva)
        assertEquals(LocalDate.of(2027, 1, 1), porId.getValue("utc").dataDaProva)
    }

    // -----------------------------------------------------------------------
    // O que a linha carrega
    // -----------------------------------------------------------------------

    @Test
    fun `o item carrega nome e distancia da prova, que sao o que a linha mostra`() {
        val item = itensDePlano(
            planos = listOf(plano("a", nome = "São Silvestre 2026", distanciaAlvoKm = 15.0)),
            planoAtivoId = "a",
        ).single()

        assertEquals("a", item.id)
        assertEquals("São Silvestre 2026", item.nome)
        assertEquals(15.0, item.distanciaAlvoKm, 0.001)
    }
}
