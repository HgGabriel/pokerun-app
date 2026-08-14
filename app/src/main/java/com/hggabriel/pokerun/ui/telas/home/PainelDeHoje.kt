package com.hggabriel.pokerun.ui.telas.home

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.ui.componentes.diasDaSemana
import com.hggabriel.pokerun.ui.componentes.segmentosDaSemana
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Que estado a Home mostra, e quantos dias faltam para a prova (`F1-T09`,
 * docs/03 §3.3).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `PassosDoOnboarding.kt`: são as duas
 * partes da tela que se provam sem aparelho, sem Firestore e sem relógio — o instante
 * chega por parâmetro —, e são justamente as que a revisão de olho deixa passar. Um
 * plano que já passou da prova e continua desenhando contagem regressiva compila igual.
 */

/**
 * // RN-27
 *
 * O estado da Home, a partir do plano ativo e do que o usuário correu.
 *
 * ### O encerramento tem dois caminhos, e o segundo é o que escapa
 *
 * O dono encerra à mão pelo `PlanDetailScreen` (`plano.encerrado`), **e o plano encerra
 * sozinho ao fim da semana da prova**. Olhar só o booleano deixaria todo plano que
 * ninguém fechou mostrando a contagem regressiva para sempre — e o dono é exatamente
 * quem some depois da prova. A fronteira é [Semana.dataFim] da última semana, que é
 * exclusivo, então a comparação é `>=` e não sobra buraco.
 *
 * ### Grade vazia é espera, e não ausência de plano
 *
 * `PlanoRepositorio.criar` são duas idas ao servidor — a rule de `weeks` exige um
 * `get()` no plano, que não enxerga escrita da mesma remessa — e o listener da
 * subcoleção emite do cache antes de o servidor responder. Dizer *"você ainda não está
 * em um plano"* a quem acabou de criar um seria mentira que se desfaz sozinha em um
 * segundo; [HomeUiState.Carregando] espera. Grade que nunca chega é defeito de
 * criação, e o dono dele é `F1-T10`.
 *
 * @param agora o relógio, por parâmetro. É o que torna esta função testável e o que
 *   impede um `Instant.now()` escondido no meio da decisão.
 */
internal fun painelDeHoje(
    plano: Plano?,
    grade: List<Semana>,
    corridas: List<Corrida>,
    agora: Instant,
): HomeUiState = when {
    plano == null -> HomeUiState.SemPlano
    grade.isEmpty() -> HomeUiState.Carregando
    plano.encerrado || agora >= grade.last().dataFim -> HomeUiState.Encerrado(plano.nome)
    agora < grade.first().dataInicio -> naoIniciado(plano, grade)
    else -> ativo(plano, grade, corridas, agora)
}

/**
 * // RN-28
 *
 * Quantos dias de calendário faltam para a prova, **contados no fuso do plano**.
 *
 * O erro que esta função existe para impedir é o mesmo de `CalendarioDoPlano`, uma
 * casa adiante: às 23h de 30/12 em São Paulo faltam dois dias para a virada do ano e um
 * para a prova, mas o instante já é 31/12 em UTC — e a Home diria zero na véspera.
 *
 * Conta em **dias de calendário**, nunca em horas divididas por 24: a semana que contém
 * a virada do horário de verão tem 167 ou 169 horas, e sete dias.
 *
 * Nunca devolve negativo. O plano já encerrou nesse ponto (RN-27), e uma contagem
 * negativa só chegaria à tela por um estado calculado errado.
 */
internal fun diasAteAProva(agora: Instant, plano: Plano, grade: List<Semana>): Int {
    if (grade.isEmpty()) return 0
    val hoje = agora.atZone(plano.fuso).toLocalDate()
    return ChronoUnit.DAYS.between(hoje, diaDaProva(plano, grade)).coerceAtLeast(0).toInt()
}

private fun naoIniciado(plano: Plano, grade: List<Semana>): HomeUiState {
    val primeira = grade.first()
    return HomeUiState.NaoIniciado(
        nomeDoPlano = plano.nome,
        comecaEm = primeira.dataInicio.atZone(plano.fuso).toLocalDate(),
        primeiraSemana = ResumoDaSemana(
            numero = primeira.numero,
            sessoes = primeira.sessoesAlvo,
            kmAlvo = primeira.kmAlvo,
            longaoKm = primeira.longaoKm,
        ),
    )
}

private fun ativo(
    plano: Plano,
    grade: List<Semana>,
    corridas: List<Corrida>,
    agora: Instant,
): HomeUiState {
    // Dentro do intervalo do plano a semana existe sempre — as duas pernas de fora já
    // foram tratadas acima. O nulo aqui seria grade fora de ordem ou com buraco, que é
    // dado quebrado e não estado de tela.
    val semana = CalendarioDoPlano.semanaDe(agora, plano, grade) ?: return HomeUiState.Falhou

    return HomeUiState.Ativo(
        planoId = plano.id,
        nomeDoPlano = plano.nome,
        diasAteAProva = diasAteAProva(agora, plano, grade),
        semana = CardDaSemana(
            numero = semana.numero,
            totalDeSemanas = grade.size,
            primeiroDia = semana.dataInicio.atZone(plano.fuso).toLocalDate(),
            // `data_fim` é exclusivo: o último dia da semana é a véspera dele.
            ultimoDia = semana.dataFim.atZone(plano.fuso).toLocalDate().minusDays(1),
            feitas = CalculoDeAderencia.sessoesFeitas(semana, corridas),
            previstas = semana.sessoesAlvo,
            longaoKm = semana.longaoKm,
            longaoCumprido = CalculoDeAderencia.longaoCumprido(semana, corridas),
            segmentos = segmentosDaSemana(semana, corridas),
            dias = diasDaSemana(semana, corridas, plano.fuso, agora),
        ),
    )
}

/**
 * O dia da prova, tirado da grade e não de `plano.dataProva`.
 *
 * É a mesma escolha de `CalendarioDoPlano`, e pelo mesmo motivo: [Semana.dataFim] é
 * exclusivo, então subtrair um dia dele devolve o último dia incluído. Com as duas
 * funções amarradas à mesma fronteira, não existe o dia em que a contagem regressiva
 * diz `1` e o estado já diz encerrado.
 */
private fun diaDaProva(plano: Plano, grade: List<Semana>): LocalDate =
    grade.last().dataFim.atZone(plano.fuso).toLocalDate().minusDays(1)
