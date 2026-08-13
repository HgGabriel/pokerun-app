package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import kotlin.math.ceil

/*
 * A grade **depois** de gerada (`F1-T11`, docs/01 §3.3, RN-30).
 *
 * `GeradorDePlano` responde "que grade sai dos quatro parâmetros". Este arquivo responde
 * as duas perguntas seguintes, que são as da revisão do rascunho e as da edição do dono
 * (`F1-T13`): **o que muda quando o usuário puxa um longão**, e **quando isso vira
 * aviso**.
 *
 * As duas são funções puras sobre `List<Semana>` — não conhecem Compose, Firestore nem
 * relógio. É o que permite `F1-T13` reusá-las inteiras sem arrastar tela junto.
 */

/** Acima disto o salto de volume entre semanas de `build` vira aviso (RN-30). */
private const val SALTO_MAXIMO = 0.15

/**
 * Um salto de volume que passou de 15% entre duas semanas de `build` consecutivas.
 *
 * [de] e [para] são **números de semana**, não índices: é assim que a frase de
 * docs/03 §3.6 fala — *"entre as semanas 6 e 7"* —, e é o que o usuário vê na lista.
 *
 * [percentual] é inteiro e **arredondado para cima**. Ver [alertaDeVolume].
 */
data class SaltoDeVolume(
    val de: Int,
    val para: Int,
    val percentual: Int,
)

/**
 * // RN-30
 *
 * O maior salto de volume entre semanas de `build` **consecutivas**, ou nulo quando
 * nenhum passa de 15%.
 *
 * A regra roda sobre a **grade corrente** — gerada ou editada — e **nunca bloqueia a
 * gravação**. Ela não é decorativa em nenhum dos dois casos (docs/01 §3.3): na grade
 * recém-gerada dispara quando a `baseline_km` é baixa em relação ao alvo, que é
 * exatamente a pessoa que mais precisa do aviso; na editada, quando o usuário puxa um
 * longão para cima.
 *
 * ### Só `build`, e o filtro não é detalhe
 *
 * O taper corta o volume de propósito, e da 2ª semana de taper para a semana da prova
 * ele **sobe** de novo: com 2 sessões, de 9,9 km para 15 km, um salto de 51%. Sem o
 * filtro, o app acusaria risco de lesão na semana em que o plano manda descansar — e o
 * aviso apareceria em todo plano de 2 sessões já ao ser gerado, o que o transformaria em
 * ruído que ninguém mais lê. Duas semanas de `build` separadas por um taper também não
 * formam par: elas não são vizinhas no calendário.
 *
 * ### Um salto, e é o maior
 *
 * docs/03 §3.6 escreve uma frase com um par de semanas, não uma lista. Numa grade
 * recém-gerada com baseline baixa, os saltos caem em progressão — o primeiro é sempre o
 * maior — e listar seis linhas de aviso diria a mesma coisa seis vezes. Numa grade
 * editada, o maior é o que o usuário acabou de criar. Empate fica com o par mais cedo,
 * que é o que dá mais tempo de corrigir.
 *
 * ### Por que o percentual arredonda para cima
 *
 * Um salto de 15,4% arredondado para o mais próximo imprimiria *"Salto de 15%. Aumentos
 * acima de 15% elevam risco de lesão"* — uma contradição visível na tela, no único
 * elemento do app cuja função é ser levado a sério. O limiar é comparado sobre a fração
 * real; só o número exibido sobe.
 */
fun alertaDeVolume(grade: List<Semana>): SaltoDeVolume? =
    grade.zipWithNext()
        .filter { (anterior, seguinte) ->
            anterior.tipo == TipoDeSemana.BUILD &&
                seguinte.tipo == TipoDeSemana.BUILD &&
                seguinte.numero == anterior.numero + 1 &&
                anterior.kmAlvo > 0.0
        }
        .map { (anterior, seguinte) ->
            anterior to (seguinte.kmAlvo - anterior.kmAlvo) / anterior.kmAlvo
        }
        .filter { (_, salto) -> salto > SALTO_MAXIMO }
        .maxByOrNull { (_, salto) -> salto }
        ?.let { (anterior, salto) ->
            SaltoDeVolume(
                de = anterior.numero,
                para = anterior.numero + 1,
                percentual = ceil(salto * 100).toInt(),
            )
        }

/**
 * A grade com o longão da semana [numero] trocado por [longaoKm], e o volume **derivado**
 * (docs/01 §3.3).
 *
 * **O usuário não edita o volume**, e é a única forma de os dois não divergirem: um campo
 * manda, o outro sai dele pela mesma fórmula da geração (docs/01 §3.2). Um segundo campo
 * editável deixaria uma grade em que ninguém sabe qual dos dois números é o verdadeiro.
 *
 * **As sessões saem da própria semana**, e não de um parâmetro: a única semana cujo
 * `sessoes_alvo` difere do plano é a da prova (RN-26), e ela não planeja longão. Passar o
 * número por fora abriria a porta para a tela mandar um valor e o documento ter outro.
 *
 * **Nada mais muda.** As fronteiras de data ficam onde estavam — é delas que RN-05 deriva
 * o congelamento —, e o tipo também: uma edição não promove nem rebaixa semana. Semana
 * sem longão (a 2ª de taper e a da prova) não é editável, e uma chamada para ela é
 * ignorada em vez de inventar um longão que a grade não previu.
 */
fun editarLongao(grade: List<Semana>, numero: Int, longaoKm: Double): List<Semana> =
    grade.map { semana ->
        if (semana.numero != numero || semana.longaoKm == null) {
            semana
        } else {
            semana.copy(
                longaoKm = longaoKm,
                kmAlvo = GeradorDePlano.volume(longaoKm, semana.sessoesAlvo),
            )
        }
    }
