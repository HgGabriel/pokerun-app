package com.hggabriel.pokerun.ui.telas.detalhesemana

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.ui.componentes.diasDaSemana
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
import java.time.Instant

/*
 * O que a `WeekDetailScreen` mostra (`F1-T15`, docs/03 §3.9).
 *
 * Está fora do `ViewModel` pelo mesmo motivo de `PainelDeHoje.kt` e `DetalheDoPlano.kt`:
 * é a parte da tela que se prova sem aparelho e sem Firestore — o relógio chega por
 * parâmetro —, e é justamente a que a revisão de olho deixa passar. Uma semana que já
 * acabou e continua sem cadeado compila igual, e o dia errado de uma corrida por causa
 * de fuso não aparece em tela nenhuma dizendo que o problema foi fuso.
 */

/**
 * // RN-05
 *
 * A semana [numero] do plano, com o card sem anel, a grade de dias e as corridas dela.
 *
 * ### O cadeado é derivado, e a fronteira é exclusiva
 *
 * `congelada` não é campo de documento (docs/05 §2.8): enquanto o dono não abrisse o app,
 * uma semana passada continuaria destravada no banco e o cadeado mentiria. Quem responde
 * é [CalendarioDoPlano.congelada], que compara com [Semana.dataFim] — **exclusivo**, então
 * a semana congela no instante dele e não um dia depois. A rule de `weeks/{n}` faz a mesma
 * conta com `request.time`.
 *
 * ### Grade vazia é espera, e não semana quebrada
 *
 * `PlanoRepositorio.criar` são duas idas ao servidor e o listener de `weeks` emite do
 * cache antes de a segunda voltar. Quem abre esta tela logo depois de criar veria uma
 * semana sem fronteiras; [DetalheSemanaUiState.Carregando] espera.
 *
 * ### Semana fora da grade é erro, e não conteúdo com zeros
 *
 * O número vem da rota e sobrevive a morte de processo. Um plano trocado embaixo dela, ou
 * uma grade encurtada, deixa a tela apontando para uma semana que não existe — e um
 * `Conteudo` com zeros diria que a pessoa não correu, em vez de dizer que a tela não achou
 * a semana.
 *
 * @param agora o relógio, por parâmetro. É o que decide o cadeado (RN-05) e qual quadrado
 *   da grade é o de hoje, e o que impede um `Instant.now()` escondido no meio da decisão.
 */
internal fun detalheDaSemana(
    plano: Plano?,
    grade: List<Semana>,
    corridas: List<Corrida>,
    numero: Int,
    agora: Instant,
): DetalheSemanaUiState {
    if (plano == null) return DetalheSemanaUiState.Falhou
    if (grade.isEmpty()) return DetalheSemanaUiState.Carregando

    val semana = grade.firstOrNull { it.numero == numero } ?: return DetalheSemanaUiState.Falhou

    return DetalheSemanaUiState.Conteudo(
        nomeDoPlano = plano.nome,
        numero = semana.numero,
        totalDeSemanas = grade.size,
        // RN-28: as duas fronteiras saem no fuso do plano, e `data_fim` é exclusivo —
        // o último dia da semana é a véspera dele.
        primeiroDia = semana.dataInicio.atZone(plano.fuso).toLocalDate(),
        ultimoDia = semana.dataFim.atZone(plano.fuso).toLocalDate().minusDays(1),
        tipo = semana.tipo,
        kmAlvo = semana.kmAlvo,
        longaoKm = semana.longaoKm,
        longaoCumprido = CalculoDeAderencia.longaoCumprido(semana, corridas),
        feitas = CalculoDeAderencia.sessoesFeitas(semana, corridas),
        previstas = semana.sessoesAlvo,
        segmentos = segmentosDaSemana(semana, corridas),
        dias = diasDaSemana(semana, corridas, plano.fuso, agora),
        corridas = corridasDaSemana(semana, corridas, plano),
        congelada = CalendarioDoPlano.congelada(semana, agora),
    )
}

/**
 * // RN-24, RN-31
 *
 * As corridas que a lista mostra, em ordem cronológica.
 *
 * O filtro é [CalculoDeAderencia.validasDa], e não uma cópia: descartada (RN-31) e
 * substituída (RN-24) ficam de fora aqui pela mesma linha que as tira da fração e da
 * barra. Uma segunda leitura do que conta faria a lista mostrar uma corrida que o `2 de 3`
 * logo acima não conta — duas respostas para a mesma pergunta, na mesma tela.
 *
 * **A ordem é a do relógio, e não a da consulta.** `observarDoPlano` filtra por `plano_id`
 * sem `orderBy` para não exigir índice composto (`F1-T05`), então a ordem que chega é a
 * que o Firestore quiser. Uma semana cujas corridas trocam de lugar a cada emissão do
 * listener é o tipo de defeito que só aparece com o app aberto.
 */
private fun corridasDaSemana(
    semana: Semana,
    corridas: List<Corrida>,
    plano: Plano,
): List<CorridaDaSemana> =
    CalculoDeAderencia.validasDa(semana, corridas)
        .sortedBy { it.dataHoraInicio }
        .map { corrida ->
            CorridaDaSemana(
                id = corrida.id,
                // RN-28: o dia é o dia no fuso do plano. Domingo 22h em UTC−3 é domingo.
                dia = corrida.dataHoraInicio.atZone(plano.fuso).toLocalDate(),
                km = corrida.km,
                duracaoSeg = corrida.duracaoSeg,
            )
        }
