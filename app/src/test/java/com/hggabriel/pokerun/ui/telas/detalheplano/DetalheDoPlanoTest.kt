package com.hggabriel.pokerun.ui.telas.detalheplano

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * O detalhe do plano (`F1-T13`, docs/03 §3.7, RN-05, RN-06 e RN-27).
 *
 * **A tela é sobre permissão**, e é isso que se prova sem aparelho. Três regras decidem
 * o que cada pessoa pode fazer com um plano, e as três se sobrepõem na mesma linha da
 * grade:
 *
 * - **RN-06** — só o dono edita a estrutura.
 * - **RN-05** — nem o dono edita semana que já acabou.
 * - **RN-27** — em plano encerrado ninguém edita nada, e o encerramento tem **dois
 *   caminhos**: o botão do dono e o fim da semana da prova.
 *
 * O segundo caminho de RN-27 é o que escapa de toda revisão de olho: um plano que
 * ninguém fechou continua com o botão de editar aceso para sempre, e o dono é
 * justamente quem some depois da prova. Aqui ele é um teste.
 */
class DetalheDoPlanoTest {

    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val dono = "uid-dono"
    private val outro = "uid-outro"

    /** 10 semanas a partir da segunda-feira de 10/08/2026, prova em 17/10. */
    private val inicio: Instant = LocalDate.of(2026, 8, 13).atStartOfDay(fuso).toInstant()

    private val parametros = ParametrosDeGeracao(
        dataProva = LocalDate.of(2026, 10, 17).atStartOfDay(fuso).toInstant(),
        distanciaAlvoKm = 10.0,
        baselineKm = 5.0,
        sessoesPorSemana = 3,
    )

    private val grade: List<Semana> = GeradorDePlano.gerar(parametros, inicio, fuso)

    private fun plano(encerrado: Boolean = false, ownerUid: String = dono) = Plano(
        id = "plano-1",
        nome = "Sao Silvestre 2026",
        distanciaAlvoKm = parametros.distanciaAlvoKm,
        dataProva = parametros.dataProva,
        fuso = fuso,
        ownerUid = ownerUid,
        codigoConvite = "FYQJE6",
        encerrado = encerrado,
        parametros = parametros,
    )

    private fun membro(uid: String, nome: String, entrouNaSemana: Int = 1) = Membro(
        uid = uid,
        nome = nome,
        entrouEm = inicio.plusSeconds(entrouNaSemana.toLong()),
        entrouNaSemana = entrouNaSemana,
        ativo = true,
    )

    private var proximoId = 1

    private fun corrida(semanaRef: Int?, km: Double = 5.0) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = inicio,
        km = km,
        duracaoSeg = 1800,
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "plano-1",
        semanaRef = semanaRef,
        temporadaId = "kanto-2026",
    )

    private fun detalhe(
        plano: Plano? = plano(),
        grade: List<Semana> = this.grade,
        membros: List<Membro> = listOf(membro(dono, "Hiago")),
        corridas: List<Corrida> = emptyList(),
        uid: String = dono,
        planoAtivoId: String? = "plano-1",
        agora: Instant = inicio,
    ) = detalheDoPlano(plano, grade, membros, corridas, uid, planoAtivoId, agora)

    private fun conteudo(
        plano: Plano? = plano(),
        grade: List<Semana> = this.grade,
        membros: List<Membro> = listOf(membro(dono, "Hiago")),
        corridas: List<Corrida> = emptyList(),
        uid: String = dono,
        planoAtivoId: String? = "plano-1",
        agora: Instant = inicio,
    ) = detalhe(plano, grade, membros, corridas, uid, planoAtivoId, agora)
        as DetalhePlanoUiState.Conteudo

    // -----------------------------------------------------------------------
    // RN-06 — a edição é do dono
    // -----------------------------------------------------------------------

    @Test
    fun `o dono edita semana futura`() {
        val estado = conteudo()
        val futura = grade.first { it.numero == 5 }

        assertTrue(estado.ehDono)
        assertTrue(estado.podeEditar(futura))
    }

    @Test
    fun `o membro nao edita nada`() {
        // RN-06 é sobre a estrutura do plano: o membro vê a grade inteira e não muda
        // nenhuma linha dela. Modo somente leitura de docs/03 §3.7.
        val estado = conteudo(
            membros = listOf(membro(dono, "Hiago"), membro(outro, "Alguém")),
            uid = outro,
        )

        assertFalse(estado.ehDono)
        assertTrue(grade.none { estado.podeEditar(it) })
    }

    @Test
    fun `semana sem longao nao e editavel nem para o dono`() {
        // A 2ª de taper e a da prova não planejam longão, então não há o que editar:
        // o volume delas não deriva de um.
        val estado = conteudo()
        val semLongao = grade.filter { it.longaoKm == null }

        assertTrue(semLongao.isNotEmpty())
        assertTrue(semLongao.none { estado.podeEditar(it) })
    }

    // -----------------------------------------------------------------------
    // RN-05 — semana que acabou é congelada
    // -----------------------------------------------------------------------

    @Test
    fun `semana que ja acabou aparece congelada e sai da edicao`() {
        // Duas semanas depois do início, as semanas 1 e 2 já fecharam.
        val agora = grade[2].dataInicio.plusSeconds(3600)

        val estado = conteudo(agora = agora)

        assertEquals(setOf(1, 2), estado.congeladas)
        assertFalse(estado.podeEditar(grade[0]))
        assertFalse(estado.podeEditar(grade[1]))
        assertTrue(estado.podeEditar(grade[2]))
    }

    @Test
    fun `a fronteira do congelamento e o data_fim exclusivo`() {
        // `data_fim` é a meia-noite que já pertence à semana seguinte, então no
        // instante exato dela a semana está congelada. Trocar `>=` por `>` deixaria a
        // semana editável por um instante — e a rule, que faz a mesma conta, negaria a
        // escrita que a tela acabou de oferecer.
        val primeira = grade.first()

        val umInstanteAntes = conteudo(agora = primeira.dataFim.minusMillis(1))
        val emCima = conteudo(agora = primeira.dataFim)

        assertFalse(1 in umInstanteAntes.congeladas)
        assertTrue(1 in emCima.congeladas)
    }

    // -----------------------------------------------------------------------
    // RN-27 — os dois caminhos do encerramento
    // -----------------------------------------------------------------------

    @Test
    fun `plano com encerrado true fica somente leitura`() {
        val estado = conteudo(plano = plano(encerrado = true))

        assertEquals(SituacaoDoPlano.ENCERRADO, estado.situacao)
        assertTrue(grade.none { estado.podeEditar(it) })
        assertFalse(estado.podeEncerrar)
    }

    @Test
    fun `o plano encerra sozinho ao fim da semana da prova`() {
        // O segundo caminho de RN-27, e o que escapa: ninguém tocou no documento, mas a
        // última semana acabou. Olhar só o booleano deixaria o botão de editar aceso
        // para sempre num plano que já terminou.
        val depoisDaProva = grade.last().dataFim

        val estado = conteudo(plano = plano(encerrado = false), agora = depoisDaProva)

        assertEquals(SituacaoDoPlano.ENCERRADO, estado.situacao)
        assertTrue(grade.none { estado.podeEditar(it) })
    }

    @Test
    fun `encerrar e do dono, e some depois de encerrado`() {
        assertTrue(conteudo().podeEncerrar)
        assertFalse(conteudo(uid = outro).podeEncerrar)
        assertFalse(conteudo(plano = plano(encerrado = true)).podeEncerrar)
    }

    // -----------------------------------------------------------------------
    // A situação do plano (RN-12, RN-15, D-05)
    // -----------------------------------------------------------------------

    @Test
    fun `o plano aberto e ativo quando e o plano ativo do usuario, e dormente quando nao e`() {
        assertEquals(SituacaoDoPlano.ATIVO, conteudo(planoAtivoId = "plano-1").situacao)
        assertEquals(SituacaoDoPlano.DORMENTE, conteudo(planoAtivoId = "outro-plano").situacao)
        assertEquals(SituacaoDoPlano.DORMENTE, conteudo(planoAtivoId = null).situacao)
    }

    // -----------------------------------------------------------------------
    // A aderência acumulada do cabeçalho (RN-08, RN-19)
    // -----------------------------------------------------------------------

    @Test
    fun `a aderencia acumulada conta ate a semana corrente, e nao ate o fim do plano`() {
        // Na semana 2 de um plano de 3 sessões, o denominador é 6 e não as 28 do plano
        // inteiro: contar o futuro como falta mostraria 2 de 28 a quem está em dia.
        val agora = grade[1].dataInicio.plusSeconds(3600)

        val estado = conteudo(
            corridas = listOf(corrida(semanaRef = 1), corrida(semanaRef = 1)),
            agora = agora,
        )

        assertEquals(2, estado.sessoesFeitas)
        assertEquals(grade[0].sessoesAlvo + grade[1].sessoesAlvo, estado.sessoesPrevistas)
    }

    @Test
    fun `quem entrou depois nao carrega as semanas em que nao estava`() {
        // RN-19: o denominador começa na semana em que a pessoa entrou.
        val agora = grade[2].dataInicio.plusSeconds(3600)

        val estado = conteudo(
            membros = listOf(membro(dono, "Hiago"), membro(outro, "Alguém", entrouNaSemana = 3)),
            uid = outro,
            agora = agora,
        )

        assertEquals(grade[2].sessoesAlvo, estado.sessoesPrevistas)
    }

    @Test
    fun `em plano encerrado a aderencia conta o plano inteiro`() {
        // O resumo congelado de D-05 é sobre o plano todo, e não sobre a semana em que
        // alguém abriu a tela depois da prova.
        val estado = conteudo(
            plano = plano(encerrado = true),
            agora = grade.last().dataFim.plusSeconds(3600),
        )

        assertEquals(grade.sumOf { it.sessoesAlvo }, estado.sessoesPrevistas)
    }

    @Test
    fun `o teto de uma semana nao compensa outra vazia`() {
        // RN-08 tem teto por semana porque a sessão é slot (RN-34): cinco corridas na
        // semana 1 valem 3, e a semana 2 continua zerada.
        val agora = grade[1].dataInicio.plusSeconds(3600)

        val estado = conteudo(
            corridas = List(5) { corrida(semanaRef = 1) },
            agora = agora,
        )

        assertEquals(grade[0].sessoesAlvo, estado.sessoesFeitas)
    }

    // -----------------------------------------------------------------------
    // O resto do cabeçalho e da lista
    // -----------------------------------------------------------------------

    @Test
    fun `a data da prova sai no fuso do plano`() {
        // RN-28, o mesmo cuidado da lista de planos: no fuso do aparelho, a prova de
        // 17/10 à meia-noite em São Paulo viraria 16/10 para quem abrir viajando.
        assertEquals(LocalDate.of(2026, 10, 17), conteudo().dataDaProva)
    }

    @Test
    fun `o dono aparece primeiro na lista de membros`() {
        // A lista não tem ordenação na especificação, e a de chegada põe o dono em
        // primeiro por construção — menos quando alguém entra e o dono é lido depois.
        val estado = conteudo(
            membros = listOf(membro(outro, "Alguém", entrouNaSemana = 3), membro(dono, "Hiago")),
        )

        assertEquals(listOf(dono, outro), estado.membros.map { it.uid })
    }

    @Test
    fun `o alerta de quinze por cento e para quem edita`() {
        // RN-30 acompanha a grade corrente, e no detalhe ele existe para o dono ver o
        // efeito da edição dele. Para o membro, que não muda nada, seria um aviso sobre
        // uma decisão que não é dele.
        val agressiva = ParametrosDeGeracao(
            dataProva = parametros.dataProva,
            distanciaAlvoKm = 21.0,
            baselineKm = 3.0,
            sessoesPorSemana = 3,
        )
        val gradeAgressiva = GeradorDePlano.gerar(agressiva, inicio, fuso)

        val doDono = conteudo(grade = gradeAgressiva)
        val doMembro = conteudo(
            grade = gradeAgressiva,
            membros = listOf(membro(dono, "Hiago"), membro(outro, "Alguém")),
            uid = outro,
        )

        assertTrue(doDono.alerta != null)
        assertNull(doMembro.alerta)
    }

    // -----------------------------------------------------------------------
    // Os estados sem conteúdo
    // -----------------------------------------------------------------------

    @Test
    fun `grade vazia e espera, e nao plano quebrado`() {
        // Mesmo motivo da Home: `criar` são duas idas ao servidor e o listener de
        // `weeks` emite do cache antes de a segunda voltar.
        assertEquals(DetalhePlanoUiState.Carregando, detalhe(grade = emptyList()))
    }

    @Test
    fun `plano que nao existe vira erro, e nao tela vazia`() {
        // Acontece de verdade: o plano pode ter sido apagado pela console enquanto a
        // tela estava aberta. Um `Conteudo` com nome vazio seria pior.
        assertEquals(DetalhePlanoUiState.Falhou, detalhe(plano = null))
    }

    @Test
    fun `a grade chega inteira e em ordem`() {
        val estado = conteudo()

        assertEquals(grade.map { it.numero }, estado.semanas.map { it.numero })
        assertEquals("FYQJE6", estado.codigoConvite)
        assertEquals(TipoDeSemana.PROVA, estado.semanas.last().tipo)
    }
}
