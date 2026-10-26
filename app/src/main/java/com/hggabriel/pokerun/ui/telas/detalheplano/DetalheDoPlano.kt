package com.hggabriel.pokerun.ui.telas.detalheplano

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.ui.componentes.comDias
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
import com.hggabriel.pokerun.ui.componentes.TempoDaSemana
import com.hggabriel.pokerun.ui.telas.home.diasAteAProva
import java.time.Instant

/*
 * O que a `PlanDetailScreen` mostra, e o que cada pessoa pode fazer nela (`F1-T13`,
 * r2 de 26/10/2026, docs/03 §3.7).
 *
 * Está fora do `ViewModel` pelo mesmo motivo de `PainelDeHoje.kt`: é a parte da tela que
 * se prova sem aparelho e sem Firestore — o relógio chega por parâmetro —, e é
 * justamente a que a revisão de olho deixa passar. Um plano que já passou da prova e
 * continua com o botão de editar aceso compila igual.
 */

/**
 * O que a outra pessoa fez numa semana, lido do `weekly` dela (`users/{uid}/weekly/
 * {planId}_{n}`): `sessoes_feitas` e `longao_feito`. Documento ausente é zero.
 */
data class FeitoNaSemana(val sessoesFeitas: Int, val longaoFeitoKm: Double)

/**
 * // RN-27
 *
 * O estado da tela, a partir do plano, da grade, dos membros, do que o usuário correu e
 * do que o grupo correu.
 *
 * ### O encerramento tem dois caminhos, e o segundo é o que escapa
 *
 * O dono encerra à mão (`plano.encerrado`), **e o plano encerra sozinho ao fim da semana
 * da prova**. A fronteira é o [Semana.dataFim] da última semana, que é exclusivo — a
 * mesma conta de `painelDeHoje`, sobre a mesma grade, para as duas telas nunca
 * discordarem sobre um plano estar vivo.
 *
 * ### Grade vazia é espera, e não plano quebrado
 *
 * `PlanoRepositorio.criar` são duas idas ao servidor e o listener de `weeks` emite do
 * cache antes de a segunda voltar. Quem chega aqui logo depois de criar veria uma tela
 * sem grade; [DetalhePlanoUiState.Carregando] espera.
 *
 * ### Plano ausente é erro, e não tela vazia
 *
 * O documento pode ter sido apagado pela console com a tela aberta — aconteceu nesta
 * fase. Um `Conteudo` com nome vazio e grade vazia seria pior que a mensagem com o botão
 * de repetir.
 *
 * @param uid quem está olhando. Decide [DetalhePlanoUiState.Conteudo.ehDono] (RN-06), de
 *   quem são os carimbos do card e de quem é a barra vermelha do gráfico.
 * @param planoAtivoId o `plano_ativo_id` de `users/{uid}`, que é o que separa ativo de
 *   dormente (RN-12, RN-15).
 * @param feitosDoGrupo o `weekly` de cada **outro** membro, pelo uid e pelo número da
 *   semana. Uid ausente é leitura que não voltou (S3): a linha dele fica sem fração e sem
 *   células. Semana ausente num mapa presente é semana sem corrida, e conta como zero.
 * @param agora o relógio, por parâmetro. É o que torna esta função testável e o que
 *   impede um `Instant.now()` escondido no meio da decisão.
 */
internal fun detalheDoPlano(
    plano: Plano?,
    grade: List<Semana>,
    membros: List<Membro>,
    corridas: List<Corrida>,
    uid: String,
    planoAtivoId: String?,
    agora: Instant,
    feitosDoGrupo: Map<String, Map<Int, FeitoNaSemana>> = emptyMap(),
): DetalhePlanoUiState {
    if (plano == null) return DetalhePlanoUiState.Falhou
    if (grade.isEmpty()) return DetalhePlanoUiState.Carregando

    val encerrado = plano.encerrado || agora >= grade.last().dataFim
    val ehDono = plano.ownerUid == uid

    val situacao = when {
        encerrado -> SituacaoDoPlano.ENCERRADO
        plano.id == planoAtivoId -> SituacaoDoPlano.ATIVO
        else -> SituacaoDoPlano.DORMENTE
    }

    val ate = ateSemana(plano, grade, agora)
    val corrente = if (encerrado) null else CalendarioDoPlano.semanaDe(agora, plano, grade)?.numero
    val linhaDoTempo = grade.map { semanaDoPlano(it, plano, corridas, corrente, encerrado) }

    // O dono primeiro, e o resto na ordem em que entrou. Sem ordenação, a lista muda de
    // ordem a cada emissão do listener: consulta de subcoleção não promete ordem estável.
    val ordenados = membros.sortedWith(compareBy({ it.uid != plano.ownerUid }, { it.entrouEm }))
    val progresso = ordenados.map { membro ->
        val semanas = if (membro.uid == uid) {
            seuProgresso(linhaDoTempo, corridas, membro, ate)
        } else {
            feitosDoGrupo[membro.uid]?.let { progressoDoOutro(grade, it, membro, ate) }
        }
        ProgressoDoMembro(
            uid = membro.uid,
            nome = membro.nome,
            dono = membro.uid == plano.ownerUid,
            voce = membro.uid == uid,
            entrouNaSemana = membro.entrouNaSemana,
            semanas = semanas,
        )
    }
    val voce = progresso.firstOrNull { it.voce }

    return DetalhePlanoUiState.Conteudo(
        nome = plano.nome,
        // RN-28: a prova é uma data no calendário do plano, e sai no fuso dele.
        dataDaProva = plano.dataProva.atZone(plano.fuso).toLocalDate(),
        situacao = situacao,
        semanas = grade,
        congeladas = grade.filter { CalendarioDoPlano.congelada(it, agora) }.map { it.numero }.toSet(),
        comecadas = grade.filter { agora >= it.dataInicio }.map { it.numero }.toSet(),
        membros = ordenados,
        codigoConvite = plano.codigoConvite,
        ehDono = ehDono,
        parametros = plano.parametros,
        numeroPrincipal = numeroPrincipal(
            encerrado = encerrado,
            diasAteAProva = diasAteAProva(agora, plano, grade),
            semanaCorrente = corrente,
            corridaDaProva = corridaDaProva(grade, corridas),
            feitas = voce?.feitas ?: 0,
            previstas = voce?.previstas ?: 0,
        ),
        semanaCorrente = corrente,
        ateSemana = ate,
        linhaDoTempo = linhaDoTempo,
        progresso = progresso,
        semanaSelecionada = semanaPadrao(grade, corrente, encerrado),
    )
}

/**
 * // D2, S2
 *
 * O número grande do topo e o que ele diz.
 *
 * - **Plano correndo**, antes do dia da prova: os dias até ela, na conta de
 *   [diasAteAProva], a mesma da placa de peito, para as duas telas nunca discordarem.
 * - **No dia da prova:** `HOJE`, como a placa. A placa decide igual: zero dias com uma
 *   semana em curso.
 * - **Encerrado, com a corrida da prova:** o tempo dela.
 * - **Encerrado sem ela:** a sua fração de sessões. Ela se repete na sua linha do grupo,
 *   e a repetição foi aceita no sketch.
 */
internal fun numeroPrincipal(
    encerrado: Boolean,
    diasAteAProva: Int,
    semanaCorrente: Int?,
    corridaDaProva: Corrida?,
    feitas: Int,
    previstas: Int,
): NumeroPrincipal = when {
    encerrado && corridaDaProva != null -> NumeroPrincipal.TempoNaProva(corridaDaProva.duracaoSeg)
    encerrado -> NumeroPrincipal.Sessoes(feitas, previstas)
    diasAteAProva == 0 && semanaCorrente != null -> NumeroPrincipal.DiaDaProva
    else -> NumeroPrincipal.Dias(diasAteAProva)
}

/**
 * A corrida da prova: a que [segmentosDaSemana] põe na vaga da última semana, ou nula.
 * É a mesma atribuição que o carimbo da vaga usa, então o tempo do topo e o carimbo do card
 * falam da mesma corrida.
 */
internal fun corridaDaProva(grade: List<Semana>, corridas: List<Corrida>): Corrida? {
    val prova = grade.lastOrNull() ?: return null
    val id = segmentosDaSemana(prova, corridas).firstNotNullOfOrNull { it.corridaId } ?: return null
    return corridas.firstOrNull { it.id == id }
}

/**
 * // R4
 *
 * A semana que o card abre: a corrente, e a última no plano encerrado. Antes de o plano
 * começar, a primeira.
 */
internal fun semanaPadrao(grade: List<Semana>, semanaCorrente: Int?, encerrado: Boolean): Int = when {
    semanaCorrente != null -> semanaCorrente
    encerrado -> grade.last().numero
    else -> grade.first().numero
}

/** // R4 — a semana pedida pelas setas ou pela régua, sempre dentro da grade. */
internal fun semanaNaGrade(pedida: Int, grade: List<Semana>): Int =
    pedida.coerceIn(grade.first().numero, grade.last().numero)

private fun semanaDoPlano(
    semana: Semana,
    plano: Plano,
    corridas: List<Corrida>,
    corrente: Int?,
    encerrado: Boolean,
) = SemanaDoPlano(
    semana = semana,
    primeiroDia = semana.dataInicio.atZone(plano.fuso).toLocalDate(),
    // `data_fim` é exclusivo: o último dia da semana é a véspera dele.
    ultimoDia = semana.dataFim.atZone(plano.fuso).toLocalDate().minusDays(1),
    tempo = when {
        encerrado -> TempoDaSemana.PASSADA
        corrente == null -> TempoDaSemana.FUTURA
        semana.numero < corrente -> TempoDaSemana.PASSADA
        semana.numero == corrente -> TempoDaSemana.CORRENTE
        else -> TempoDaSemana.FUTURA
    },
    // A semana da prova não planeja longão: a coluna dela é a própria prova, como na placa.
    planejadoKm = semana.longaoKm ?: semana.kmAlvo.takeIf { semana.tipo == TipoDeSemana.PROVA },
    // O dia de cada corrida, no fuso do plano: é o que o carimbo escreve (RN-28).
    segmentos = comDias(segmentosDaSemana(semana, corridas), corridas, plano.fuso),
)

/**
 * // R3, RN-08, RN-10
 *
 * O seu progresso, das suas corridas: o gráfico muda na hora em que você registra, sem
 * esperar o replay. As sessões são a conta da aderência; o longão é o km da corrida que
 * [segmentosDaSemana] pôs na vaga do longão (na prova, na vaga da prova).
 */
private fun seuProgresso(
    linhaDoTempo: List<SemanaDoPlano>,
    corridas: List<Corrida>,
    membro: Membro,
    ate: Int,
): List<ProgressoNaSemana> = linhaDoTempo.map { s ->
    val naJanela = s.numero in membro.entrouNaSemana..ate
    val longao = if (s.semana.tipo == TipoDeSemana.PROVA) {
        s.segmentos.firstOrNull()?.km
    } else {
        s.segmentos.firstOrNull { it.longao }?.km
    }
    ProgressoNaSemana(
        numero = s.numero,
        sessoesAlvo = s.semana.sessoesAlvo,
        sessoesFeitas = if (naJanela) CalculoDeAderencia.sessoesFeitas(s.semana, corridas) else null,
        longaoFeitoKm = longao.takeIf { naJanela },
    )
}

/**
 * // R3, RN-08, D1
 *
 * O progresso da outra pessoa, do `weekly` dela. A semana sem documento é zero, e a
 * semana em que o plano ficou guardado para ela também: as duas estão na janela (D1).
 * O teto por semana de RN-08 vale de novo aqui, contra um `weekly` antigo sem ele.
 */
private fun progressoDoOutro(
    grade: List<Semana>,
    feitos: Map<Int, FeitoNaSemana>,
    membro: Membro,
    ate: Int,
): List<ProgressoNaSemana> = grade.map { semana ->
    val naJanela = semana.numero in membro.entrouNaSemana..ate
    val feito = feitos[semana.numero]
    ProgressoNaSemana(
        numero = semana.numero,
        sessoesAlvo = semana.sessoesAlvo,
        sessoesFeitas = if (naJanela) (feito?.sessoesFeitas ?: 0).coerceIn(0, semana.sessoesAlvo) else null,
        // Longão zero é semana sem corrida: um traço na linha de base não diria nada.
        longaoFeitoKm = feito?.longaoFeitoKm?.takeIf { naJanela && it > 0.0 },
    )
}

/**
 * Até que semana a fração de cada membro conta.
 *
 * A semana corrente enquanto o plano corre, e **a última quando ele acabou** — o resumo
 * de um plano encerrado (D-05) é sobre o plano inteiro, não sobre a semana em que alguém
 * abriu a tela meses depois.
 *
 * `semanaRef` devolve nulo fora do intervalo (RN-03), e os dois lados de fora têm
 * respostas diferentes: depois do fim é o plano todo; antes do começo é **nenhuma
 * semana**. Desde a DA-10 o plano começa na segunda seguinte à criação, então os dias
 * entre criar e começar são reais, e a fração deles é `0 de 0`, e não `0 de 3`.
 */
private fun ateSemana(plano: Plano, grade: List<Semana>, agora: Instant): Int =
    CalendarioDoPlano.semanaRef(agora, plano, grade)
        ?: if (agora >= grade.last().dataFim) grade.last().numero else grade.first().numero - 1
