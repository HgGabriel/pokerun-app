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
import com.hggabriel.pokerun.ui.componentes.TempoDaSemana
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
        curtaKm = 2.5,
    )

    /**
     * A grade é gerada na segunda-feira, que é onde o plano começa (DA-10, nº 7). [inicio]
     * continua sendo a quinta da semana 1: é o "agora" padrão destes testes.
     */
    private val grade: List<Semana> = GeradorDePlano.gerar(
        parametros,
        LocalDate.of(2026, 8, 10).atStartOfDay(fuso).toInstant(),
        fuso,
    )

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

    private fun corrida(semanaRef: Int?, km: Double = 5.0, duracaoSeg: Long = 1800) = Corrida(
        id = "run-${proximoId++}",
        dataHoraInicio = inicio,
        km = km,
        duracaoSeg = duracaoSeg,
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
        feitosDoGrupo: Map<String, Map<Int, FeitoNaSemana>> = emptyMap(),
    ) = detalheDoPlano(plano, grade, membros, corridas, uid, planoAtivoId, agora, feitosDoGrupo)

    private fun conteudo(
        plano: Plano? = plano(),
        grade: List<Semana> = this.grade,
        membros: List<Membro> = listOf(membro(dono, "Hiago")),
        corridas: List<Corrida> = emptyList(),
        uid: String = dono,
        planoAtivoId: String? = "plano-1",
        agora: Instant = inicio,
        feitosDoGrupo: Map<String, Map<Int, FeitoNaSemana>> = emptyMap(),
    ) = detalhe(plano, grade, membros, corridas, uid, planoAtivoId, agora, feitosDoGrupo)
        as DetalhePlanoUiState.Conteudo

    /** Uma hora depois do começo da semana [numero]: o "agora" dentro dela. */
    private fun naSemana(numero: Int): Instant = grade[numero - 1].dataInicio.plusSeconds(3600)

    private val dupla = listOf(membro(dono, "Hiago"), membro(outro, "jhusilva", entrouNaSemana = 3))

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
    fun `so semana de build e editavel, nem o taper nem a prova`() {
        // O taper sai do maior longão do build e a prova não planeja longão (docs/01
        // §3.3): não há o que editar neles, mesmo com longão no taper.
        val estado = conteudo()
        val foraDoBuild = grade.filter { it.tipo != TipoDeSemana.BUILD }

        assertEquals(3, foraDoBuild.size)
        assertTrue(foraDoBuild.none { estado.podeEditar(it) })
        assertTrue(foraDoBuild.first().longaoKm != null)
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
    fun `so o dono muda a curta, e so enquanto o plano corre`() {
        val doMembro = conteudo(
            membros = listOf(membro(dono, "Hiago"), membro(outro, "Alguém")),
            uid = outro,
        )
        val encerrado = conteudo(plano = plano(encerrado = true))

        assertTrue(conteudo().podeMudarCurta)
        assertFalse(doMembro.podeMudarCurta)
        assertFalse(encerrado.podeMudarCurta)
    }

    @Test
    fun `as semanas comecadas sao as congeladas e a corrente`() {
        // A troca da curta vale a partir da semana seguinte (docs/01 §3.3).
        val agora = grade[2].dataInicio.plusSeconds(3600)

        val estado = conteudo(agora = agora)

        assertEquals(setOf(1, 2), estado.congeladas)
        assertEquals(setOf(1, 2, 3), estado.comecadas)
    }

    @Test
    fun `antes de o plano comecar a aderencia e zero de zero`() {
        // DA-10: o plano começa na segunda seguinte à criação, e os dias entre os dois
        // não têm sessão prevista.
        val antes = grade.first().dataInicio.minusSeconds(3600)

        val estado = conteudo(agora = antes)

        assertEquals(0, estado.sessoesPrevistas)
        assertEquals(0, estado.sessoesFeitas)
        assertTrue(estado.comecadas.isEmpty())
    }

    @Test
    fun `os parametros do plano chegam ao estado para a edicao`() {
        assertEquals(parametros, conteudo().parametros)
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

    // -----------------------------------------------------------------------
    // R1 — a trava do longão (D7, S1)
    // -----------------------------------------------------------------------

    @Test
    fun `semana de build futura sem corrida e editavel`() {
        val estado = conteudo(corridas = listOf(corrida(semanaRef = 1)))

        assertTrue(estado.podeEditar(grade[4]))
    }

    @Test
    fun `semana corrente com corrida sua nao e editavel`() {
        // D7: o alvo não muda depois de alguém correr para ele.
        val estado = conteudo(corridas = listOf(corrida(semanaRef = 1)))

        assertFalse(estado.podeEditar(grade[0]))
    }

    @Test
    fun `semana corrente com corrida so da outra pessoa nao e editavel`() {
        // S1: o longão é o mesmo para os dois, então a corrida de qualquer membro trava.
        val estado = conteudo(
            membros = listOf(membro(dono, "Hiago"), membro(outro, "jhusilva")),
            feitosDoGrupo = mapOf(outro to mapOf(1 to FeitoNaSemana(sessoesFeitas = 1, longaoFeitoKm = 4.0))),
        )

        assertEquals(setOf(1), estado.semanasCorridas)
        assertFalse(estado.podeEditar(grade[0]))
    }

    @Test
    fun `semana corrente sem corrida de ninguem e editavel`() {
        val estado = conteudo(
            membros = listOf(membro(dono, "Hiago"), membro(outro, "jhusilva")),
            feitosDoGrupo = mapOf(outro to emptyMap()),
        )

        assertTrue(estado.semanasCorridas.isEmpty())
        assertTrue(estado.podeEditar(grade[0]))
    }

    @Test
    fun `corrida avulsa nao trava a semana`() {
        // A trava usa a regra dos carimbos: avulsa não cumpre sessão (F2-T10).
        val avulsa = corrida(semanaRef = 1).copy(
            sessaoReivindicada = com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada.Avulsa,
        )

        assertTrue(conteudo(corridas = listOf(avulsa)).podeEditar(grade[0]))
    }

    // -----------------------------------------------------------------------
    // R2 — o número principal (D2, S2)
    // -----------------------------------------------------------------------

    private val diaDaProva: LocalDate = grade.last().dataFim.atZone(fuso).toLocalDate().minusDays(1)

    @Test
    fun `plano correndo mostra os dias ate a prova, na conta da placa`() {
        val esperado = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2026, 8, 13), diaDaProva).toInt()

        assertEquals(NumeroPrincipal.Dias(esperado), conteudo().numeroPrincipal)
        // Guardado também conta os dias: a prova é a mesma.
        assertEquals(NumeroPrincipal.Dias(esperado), conteudo(planoAtivoId = "outro").numeroPrincipal)
    }

    @Test
    fun `no dia da prova o numero e hoje`() {
        val meioDia = diaDaProva.atTime(12, 0).atZone(fuso).toInstant()

        assertEquals(NumeroPrincipal.DiaDaProva, conteudo(agora = meioDia).numeroPrincipal)
    }

    @Test
    fun `encerrado com a corrida da prova mostra o tempo dela`() {
        val prova = corrida(semanaRef = grade.last().numero, km = 10.1, duracaoSeg = 5554)

        val estado = conteudo(plano = plano(encerrado = true), corridas = listOf(corrida(semanaRef = 1), prova))

        assertEquals(NumeroPrincipal.TempoNaProva(5554), estado.numeroPrincipal)
    }

    @Test
    fun `encerrado sem a corrida da prova mostra a sua fracao`() {
        val estado = conteudo(
            plano = plano(encerrado = true),
            corridas = listOf(corrida(semanaRef = 1), corrida(semanaRef = 2)),
            agora = grade.last().dataFim.plusSeconds(60),
        )

        assertEquals(NumeroPrincipal.Sessoes(2, grade.sumOf { it.sessoesAlvo }), estado.numeroPrincipal)
    }

    // -----------------------------------------------------------------------
    // R3 — o progresso por membro
    // -----------------------------------------------------------------------

    @Test
    fun `a janela de quem entrou na semana 3 comeca na semana 3`() {
        val estado = conteudo(
            membros = dupla,
            agora = naSemana(4),
            feitosDoGrupo = mapOf(outro to mapOf(3 to FeitoNaSemana(2, 6.0), 4 to FeitoNaSemana(1, 0.0))),
        )
        val dela = estado.progresso.first { it.uid == outro }

        assertNull(dela.na(1)?.sessoesFeitas)
        assertNull(dela.na(2)?.sessoesFeitas)
        assertEquals(2, dela.na(3)?.sessoesFeitas)
        assertEquals(3, dela.feitas)
        assertEquals(grade[2].sessoesAlvo + grade[3].sessoesAlvo, dela.previstas)
        // Futuro fora da janela: nem zero, nem número.
        assertNull(dela.na(5)?.sessoesFeitas)
    }

    @Test
    fun `semana guardada conta como zero`() {
        // D1: a janela vai de entrou_na_semana até a corrente, e a semana em que o plano
        // ficou guardado está nela. É vazia e conta.
        val estado = conteudo(
            planoAtivoId = "outro-plano",
            corridas = listOf(corrida(semanaRef = 1), corrida(semanaRef = 3)),
            agora = naSemana(3),
        )
        val sua = estado.voce!!

        assertEquals(0, sua.na(2)?.sessoesFeitas)
        assertEquals(2, sua.feitas)
        assertEquals(grade.take(3).sumOf { it.sessoesAlvo }, sua.previstas)
    }

    @Test
    fun `documento ausente e zero, e leitura que falhou some`() {
        val lido = conteudo(membros = dupla, agora = naSemana(3), feitosDoGrupo = mapOf(outro to emptyMap()))
        val falhou = conteudo(membros = dupla, agora = naSemana(3))

        assertEquals(0, lido.progresso.first { it.uid == outro }.na(3)?.sessoesFeitas)
        assertEquals(grade[2].sessoesAlvo, lido.progresso.first { it.uid == outro }.previstas)
        // S3: sem a leitura, a linha dela fica sem fração e sem células, e a tela segue.
        val dela = falhou.progresso.first { it.uid == outro }
        assertNull(dela.semanas)
        assertEquals(0, dela.previstas)
        assertTrue(falhou.podeEditar(grade[4]))
    }

    @Test
    fun `o teto de sessoes vale por semana para os dois`() {
        val estado = conteudo(
            membros = dupla,
            corridas = List(5) { corrida(semanaRef = 3) },
            agora = naSemana(3),
            feitosDoGrupo = mapOf(outro to mapOf(3 to FeitoNaSemana(7, 8.0))),
        )

        assertEquals(grade[2].sessoesAlvo, estado.voce!!.na(3)?.sessoesFeitas)
        assertEquals(grade[2].sessoesAlvo, estado.progresso.first { it.uid == outro }.feitas)
    }

    @Test
    fun `o seu longao feito e a corrida da vaga do longao`() {
        // A maior corrida sem reivindicação vai para o longão (docs/04 §3.2).
        val estado = conteudo(corridas = listOf(corrida(semanaRef = 1, km = 3.0), corrida(semanaRef = 1, km = 6.4)))

        assertEquals(6.4, estado.voce!!.na(1)?.longaoFeitoKm!!, 0.0)
        assertNull(estado.voce!!.na(2)?.longaoFeitoKm)
    }

    @Test
    fun `o longao zero da outra pessoa nao vira traco`() {
        val estado = conteudo(
            membros = dupla,
            agora = naSemana(4),
            feitosDoGrupo = mapOf(outro to mapOf(3 to FeitoNaSemana(1, 7.5), 4 to FeitoNaSemana(0, 0.0))),
        )
        val dela = estado.progresso.first { it.uid == outro }

        assertEquals(7.5, dela.na(3)?.longaoFeitoKm!!, 0.0)
        assertNull(dela.na(4)?.longaoFeitoKm)
    }

    @Test
    fun `cada membro sabe se e o dono e se e voce`() {
        val estado = conteudo(membros = dupla.reversed(), uid = outro, feitosDoGrupo = mapOf(dono to emptyMap()))

        assertEquals(listOf(dono, outro), estado.progresso.map { it.uid })
        assertTrue(estado.progresso[0].dono)
        assertFalse(estado.progresso[0].voce)
        assertTrue(estado.progresso[1].voce)
    }

    // -----------------------------------------------------------------------
    // R4 — a semana selecionada
    // -----------------------------------------------------------------------

    @Test
    fun `a semana selecionada abre na corrente`() {
        val estado = conteudo(agora = naSemana(3))

        assertEquals(3, estado.semanaCorrente)
        assertEquals(3, estado.semanaSelecionada)
    }

    @Test
    fun `no encerrado a semana selecionada abre na ultima`() {
        val estado = conteudo(plano = plano(encerrado = true), agora = naSemana(3))

        assertNull(estado.semanaCorrente)
        assertEquals(grade.last().numero, estado.semanaSelecionada)
    }

    @Test
    fun `antes de o plano comecar a semana selecionada e a primeira`() {
        val estado = conteudo(agora = grade.first().dataInicio.minusSeconds(3600))

        assertEquals(1, estado.semanaSelecionada)
        assertTrue(estado.linhaDoTempo.all { it.tempo == TempoDaSemana.FUTURA })
    }

    @Test
    fun `a semana pedida fica sempre dentro da grade`() {
        assertEquals(1, semanaNaGrade(0, grade))
        assertEquals(grade.size, semanaNaGrade(grade.size + 1, grade))
        assertEquals(4, semanaNaGrade(4, grade))
    }

    @Test
    fun `o tempo de cada semana segue a corrente, e no encerrado todas passaram`() {
        val correndo = conteudo(agora = naSemana(3)).linhaDoTempo.map { it.tempo }
        val encerrado = conteudo(plano = plano(encerrado = true)).linhaDoTempo.map { it.tempo }

        assertEquals(listOf(TempoDaSemana.PASSADA, TempoDaSemana.PASSADA, TempoDaSemana.CORRENTE), correndo.take(3))
        assertTrue(correndo.drop(3).all { it == TempoDaSemana.FUTURA })
        assertTrue(encerrado.all { it == TempoDaSemana.PASSADA })
    }

    @Test
    fun `a coluna da prova planeja a distancia da prova`() {
        assertEquals(parametros.distanciaAlvoKm, conteudo().linhaDoTempo.last().planejadoKm!!, 0.0)
    }
}
