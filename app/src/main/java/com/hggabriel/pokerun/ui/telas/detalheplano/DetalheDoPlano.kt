package com.hggabriel.pokerun.ui.telas.detalheplano

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.alertaDeVolume
import java.time.Instant

/*
 * O que a `PlanDetailScreen` mostra, e o que cada pessoa pode fazer nela (`F1-T13`,
 * docs/03 §3.7).
 *
 * Está fora do `ViewModel` pelo mesmo motivo de `PainelDeHoje.kt`: é a parte da tela que
 * se prova sem aparelho e sem Firestore — o relógio chega por parâmetro —, e é
 * justamente a que a revisão de olho deixa passar. Um plano que já passou da prova e
 * continua com o botão de editar aceso compila igual.
 */

/**
 * // RN-27
 *
 * O estado da tela, a partir do plano, da grade, dos membros e do que o usuário correu.
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
 * @param uid quem está olhando. Decide [DetalhePlanoUiState.Conteudo.ehDono] (RN-06) e
 *   de quem é a aderência do cabeçalho (RN-19).
 * @param planoAtivoId o `plano_ativo_id` de `users/{uid}`, que é o que separa ativo de
 *   dormente (RN-12, RN-15).
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

    val eu = membros.firstOrNull { it.uid == uid }
    val ate = ateSemana(plano, grade, agora)

    return DetalhePlanoUiState.Conteudo(
        nome = plano.nome,
        // RN-28: a prova é uma data no calendário do plano, e sai no fuso dele.
        dataDaProva = plano.dataProva.atZone(plano.fuso).toLocalDate(),
        situacao = situacao,
        sessoesFeitas = eu?.let { sessoesFeitas(grade, corridas, it, ate) } ?: 0,
        sessoesPrevistas = eu?.let { sessoesPrevistas(grade, it, ate) } ?: 0,
        semanas = grade,
        congeladas = grade.filter { CalendarioDoPlano.congelada(it, agora) }.map { it.numero }.toSet(),
        // O dono primeiro, e o resto na ordem em que entrou. A especificação não pede
        // ordenação; sem nenhuma, a lista muda de ordem a cada emissão do listener,
        // porque uma consulta de subcoleção não promete ordem estável.
        membros = membros.sortedWith(compareBy({ it.uid != plano.ownerUid }, { it.entrouEm })),
        codigoConvite = plano.codigoConvite,
        ehDono = ehDono,
        // RN-30 acompanha a grade corrente. Aqui ele existe para o dono ver o efeito da
        // edição dele; para quem não edita, seria um aviso sobre decisão alheia.
        alerta = if (ehDono && !encerrado) alertaDeVolume(grade) else null,
    )
}

/**
 * // RN-08
 *
 * O numerador da aderência acumulada: soma de slots cumpridos, com o **teto por semana**.
 *
 * A soma passa por [CalculoDeAderencia.sessoesFeitas] semana a semana em vez de contar
 * corridas: a sessão é um slot (RN-34), então a 4ª corrida de uma semana de 3 não
 * reivindica nada — e é isso que impede uma semana cheia de cobrir uma semana vazia.
 */
private fun sessoesFeitas(
    grade: List<Semana>,
    corridas: List<Corrida>,
    membro: Membro,
    ateSemana: Int,
): Int = janela(grade, membro, ateSemana).sumOf { CalculoDeAderencia.sessoesFeitas(it, corridas) }

/**
 * // RN-19
 *
 * O denominador: as sessões previstas da semana em que o membro entrou até a corrente.
 *
 * Quem entra na semana 8 não carrega as sete em que não estava, e o futuro também não
 * entra — contar o plano inteiro mostraria `2 de 28` a quem está em dia na semana 1.
 */
private fun sessoesPrevistas(grade: List<Semana>, membro: Membro, ateSemana: Int): Int =
    janela(grade, membro, ateSemana).sumOf { it.sessoesAlvo }

private fun janela(grade: List<Semana>, membro: Membro, ateSemana: Int): List<Semana> =
    grade.filter { it.numero in membro.entrouNaSemana..ateSemana }

/**
 * Até que semana a aderência acumulada conta.
 *
 * A semana corrente enquanto o plano corre, e **a última quando ele acabou** — o resumo
 * de um plano encerrado (D-05) é sobre o plano inteiro, não sobre a semana em que alguém
 * abriu a tela meses depois.
 *
 * `semanaRef` devolve nulo fora do intervalo (RN-03), e os dois lados de fora têm
 * respostas diferentes: depois do fim é o plano todo; antes do começo é a primeira
 * semana, que é o estado defensivo — a grade sempre arranca na segunda-feira da semana
 * corrente (RN-01), então plano criado pelo app não cai aí.
 */
private fun ateSemana(plano: Plano, grade: List<Semana>, agora: Instant): Int =
    CalendarioDoPlano.semanaRef(agora, plano, grade)
        ?: if (agora >= grade.last().dataFim) grade.last().numero else grade.first().numero
