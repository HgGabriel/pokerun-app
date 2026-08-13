package com.hggabriel.pokerun.ui.componentes

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

/*
 * O que o card da semana desenha no lugar do anel (`F1-T09`, docs/03 §3.3.1).
 *
 * O anel de aderência saiu do sistema em 06/08 e está proibido em docs/02 §9.1.1: é a
 * assinatura de outro produto, é um display retroiluminado dentro de um app cuja tese é
 * ficha impressa, e com denominador 2, 3 ou 4 ele lê pior que a fração. No lugar entram
 * três coisas, e as duas derivações delas moram aqui:
 *
 * - a **barra de sessões**, com exatamente um segmento por sessão prevista;
 * - a **grade de dias**, que diz *quando*, enquanto a barra diz *quantas*.
 *
 * **É Kotlin puro, sem Compose**, porque é aqui que RN-34, RN-01 e RN-28 viram código —
 * e regra desenhada dentro de um `@Composable` só se verifica de olho. Quem desenha é
 * `BarraDeSessoes.kt`, e `F1-T13` e `F1-T15` consomem os dois.
 */

/**
 * Uma sessão prevista da semana, e a corrida que a cumpriu (RN-34).
 *
 * [corridaId] nulo é o segmento **pendente**: contorno em `borda-forte`, sem
 * distância. Ele não é falta — a semana ainda está correndo.
 *
 * O id vem junto porque **cada segmento é endereçável**: o toque abre a corrida que o
 * cumpriu (docs/03 §3.3.1), e é isso que um anel não faz.
 */
data class SegmentoDaSemana(
    val sessao: SessaoReivindicada,
    val corridaId: String? = null,
    val km: Double? = null,
) {
    /** O segmento de altura dupla. É onde RN-10 ganha forma (docs/03 §3.3.1). */
    val longao: Boolean get() = sessao is SessaoReivindicada.Longao
}

/**
 * Um dia de calendário da semana, com quantas corridas caíram nele.
 *
 * **[corridas] não tem teto, e a diferença para a barra é proposital:** a barra
 * responde "quantas das previstas", com o teto de RN-34; a grade responde "em que
 * dias", e duas corridas no sábado são dois treinos no sábado.
 */
data class DiaDoTreino(
    val dia: LocalDate,
    val corridas: Int,
    val hoje: Boolean,
)

/**
 * // RN-34
 *
 * Um segmento por sessão prevista, na ordem em que a barra os desenha.
 *
 * **A quantidade sai de [Semana.sessoesAlvo] e de mais nada.** Uma semana de 3 previstas
 * com 5 corridas registradas continua com 3 segmentos: cada sessão aceita no máximo uma
 * corrida, e as que sobram não reivindicam slot nenhum — é a mesma trava que impede a
 * aderência de dar 133% (RN-08).
 *
 * **O longão é o último segmento**, e só existe quando a semana planeja um
 * ([Semana.longaoKm] não nulo). Na 2ª de taper e na semana da prova não há longão a
 * cumprir, e um segmento de altura dupla ali seria um alvo que a grade nunca previu.
 *
 * ### Como as corridas caem nos segmentos
 *
 * Em duas passadas, e a segunda é o que faz a Fase 1 funcionar:
 *
 * 1. **Quem reivindicou, ocupa o que reivindicou.** `sessao_reivindicada` é o vínculo
 *    de RN-34, e desprezá-lo poria a corrida do longão num segmento curto assim que
 *    `F2-T10` começar a gravar o campo.
 * 2. **Quem não reivindicou preenche os slots livres em ordem cronológica.** Toda
 *    corrida manual da Fase 1 nasce com o campo nulo — a atribuição automática é da
 *    Fase 2 —, e sem esta passada o card ficaria vazio para quem registrou tudo à mão.
 *
 * O filtro do que conta é [CalculoDeAderencia.validasDa], e não uma cópia: descartada
 * (RN-31) e substituída (RN-24) ficam de fora nos dois lugares pela mesma linha, senão
 * a barra e a fração divergiriam sem ninguém perceber.
 */
fun segmentosDaSemana(semana: Semana, corridas: List<Corrida>): List<SegmentoDaSemana> {
    val sessoes = sessoesPrevistas(semana)
    val validas = CalculoDeAderencia.validasDa(semana, corridas).sortedBy { it.dataHoraInicio }

    val ocupadas = mutableMapOf<String, Corrida>()
    val semDono = mutableListOf<Corrida>()

    validas.forEach { corrida ->
        val token = corrida.sessaoReivindicada?.token
        // Duas corridas reivindicando o mesmo slot é estado possível — o usuário
        // sobrescreve a atribuição na `RunEditScreen`. A segunda não some nem
        // sobrescreve: ela cai na fila dos slots livres.
        if (token != null && token !in ocupadas) ocupadas[token] = corrida else semDono += corrida
    }

    val fila = semDono.iterator()
    return sessoes.map { sessao ->
        val corrida = ocupadas[sessao.token] ?: fila.nextOrNull()
        SegmentoDaSemana(sessao = sessao, corridaId = corrida?.id, km = corrida?.km)
    }
}

/**
 * // RN-01
 *
 * Os dias de calendário da semana, de segunda a domingo **no fuso do plano**.
 *
 * **A quantidade vem das fronteiras da semana, e não de um `7` escrito no código**
 * (RN-26): a semana da prova vai de 28 a 31/12 e tem quatro dias. [Semana.dataFim] é
 * exclusivo, então o último dia é a véspera dele.
 *
 * **Nada aqui usa `ZoneId.systemDefault()`** (RN-28). O dia de uma corrida é o dia no
 * fuso do plano: um treino de domingo às 22h em UTC−3 é segunda-feira em UTC, e o
 * quadrado acenderia no dia errado sem nada na tela dizendo que o problema foi fuso.
 *
 * @param agora o instante que decide qual quadrado é o de hoje. Fora da semana, nenhum.
 */
fun diasDaSemana(
    semana: Semana,
    corridas: List<Corrida>,
    fuso: ZoneId,
    agora: Instant,
): List<DiaDoTreino> {
    val primeiro = semana.dataInicio.atZone(fuso).toLocalDate()
    val ultimo = semana.dataFim.atZone(fuso).toLocalDate().minusDays(1)
    val hoje = agora.atZone(fuso).toLocalDate()

    val porDia = CalculoDeAderencia.validasDa(semana, corridas)
        .groupingBy { it.dataHoraInicio.atZone(fuso).toLocalDate() }
        .eachCount()

    return generateSequence(primeiro) { it.plusDays(1) }
        .takeWhile { it <= ultimo }
        .map { dia -> DiaDoTreino(dia = dia, corridas = porDia[dia] ?: 0, hoje = dia == hoje) }
        .toList()
}

/**
 * As sessões que a semana prevê, na ordem da barra.
 *
 * O longão fecha a fila porque é o segmento de altura dupla, e um degrau no meio da
 * barra quebraria a leitura de "quantas faltam" que a linha inteira existe para dar.
 * O vocabulário é o de [SessaoReivindicada], que é o mesmo do documento (docs/05 §1):
 * `curta_1`, `curta_2`, `longao`.
 */
private fun sessoesPrevistas(semana: Semana): List<SessaoReivindicada> {
    val temLongao = semana.longaoKm != null
    val curtas = if (temLongao) semana.sessoesAlvo - 1 else semana.sessoesAlvo
    return buildList {
        (1..curtas).forEach { add(SessaoReivindicada.Curta(it)) }
        if (temLongao) add(SessaoReivindicada.Longao)
    }
}

private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
