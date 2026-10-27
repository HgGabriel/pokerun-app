package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import com.hggabriel.pokerun.dominio.regras.MotorDeXp
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.escada
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.ritmoDe
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/*
 * As contas do Progresso redesenhado (estudo de Progresso e Perfil, Revisão 3): a estrada, a
 * pílula da prova, o cartão do plano, a sequência, o ritmo e o Você sabia. Tudo é cálculo
 * sobre o que o painel já observa; nenhuma conta aqui escreve no Firestore.
 */

/** Corrida que conta: nem descartada (RN-31) nem substituída (RN-24). */
internal fun Corrida.valida(): Boolean = !descartada && !substituida

// ---------------------------------------------------------------------------
// A estrada da temporada (Direção A)
// ---------------------------------------------------------------------------

/** Uma semana do chão da estrada: o km da temporada somado até o fim dela e a posição na escada. */
data class SemanaDaEstrada(val inicio: LocalDate, val kmAcumulado: Double, val posicao: Int)

/** As espécies ultrapassadas numa semana, que ficam à beira da estrada atrás do parceiro. */
data class RivaisDaSemana(val indice: Int, val especies: List<Int>)

/**
 * A estrada: do início da semana da primeira corrida da temporada até a semana da prova do
 * plano ativo, ou até o fim da temporada sem plano (estudo, Direção A e decisão 17).
 *
 * @property indiceDeHoje a semana em que o parceiro está parado.
 * @property comProva há bandeira de prova no fim; sem plano ativo, a estrada só acaba.
 */
data class EstradaDaTemporada(
    val semanas: List<SemanaDaEstrada>,
    val indiceDeHoje: Int,
    val kmNaTemporada: Double,
    val rivais: List<RivaisDaSemana>,
    val inicio: LocalDate,
    val fim: LocalDate,
    val comProva: Boolean,
)

/**
 * A posição de cada semana vem do XP **creditado** das corridas da temporada, somado em
 * ordem: é a mesma escada que o replay grava, sem a regressão. A estrada mostra quem a
 * pessoa já ultrapassou, e um rival ultrapassado não volta à frente dela por inatividade.
 */
fun estradaDaTemporada(
    corridas: List<Corrida>,
    temporada: Temporada,
    planoAtivo: Plano?,
    hoje: LocalDate,
    fuso: ZoneId,
): EstradaDaTemporada {
    val daTemporada = corridas.filter { it.valida() && it.temporadaId == temporada.id }.sortedBy { it.dataHoraInicio }
    val dia: (Corrida) -> LocalDate = { it.dataHoraInicio.atZone(fuso).toLocalDate() }
    val primeira = daTemporada.firstOrNull()?.let(dia) ?: hoje
    val fimDaTemporada = temporada.fim.atZone(temporada.fuso).toLocalDate().minusDays(1)
    val prova = planoAtivo?.let { it.dataProva.atZone(it.fuso).toLocalDate() }
    val fim = maxOf(prova ?: fimDaTemporada, hoje)

    val semanas = semanasEntre(inicioDaSemanaDe(minOf(primeira, hoje)), inicioDaSemanaDe(fim))
    var km = 0.0
    var xp = 0L
    var restantes = daTemporada
    var posicaoAnterior = 1
    val rivais = mutableListOf<RivaisDaSemana>()
    val degraus = escada(temporada).map { it.first }
    val pontos = semanas.mapIndexed { indice, inicio ->
        val fimDaSemana = inicio.plusDays(7)
        val nestaSemana = restantes.takeWhile { dia(it).isBefore(fimDaSemana) }
        restantes = restantes.drop(nestaSemana.size)
        km += nestaSemana.sumOf { it.km }
        xp += nestaSemana.sumOf { it.xpCreditado }
        val posicao = maxOf(posicaoAnterior, posicaoNaEscada(xp, temporada))
        if (posicao > posicaoAnterior) {
            rivais += RivaisDaSemana(indice, degraus.subList(posicaoAnterior - 1, posicao - 1))
        }
        posicaoAnterior = posicao
        SemanaDaEstrada(inicio, km, posicao)
    }
    return EstradaDaTemporada(
        semanas = pontos,
        indiceDeHoje = semanas.indexOf(inicioDaSemanaDe(hoje)).coerceAtLeast(0),
        kmNaTemporada = daTemporada.sumOf { it.km },
        rivais = rivais,
        inicio = primeira,
        fim = fim,
        comProva = prova != null,
    )
}

// ---------------------------------------------------------------------------
// A pílula da estrada (decisão 17)
// ---------------------------------------------------------------------------

/** As quatro distâncias clássicas da comparação sem plano, cada uma com o seu artigo. */
enum class DistanciaClassica(val km: Double) { CINCO(5.0), DEZ(10.0), MEIA(21.1), MARATONA(42.2) }

/** O que a pílula diz. A tela escolhe o texto pela forma. */
sealed interface PilulaDaEstrada {
    /** `87% da prova de 15 km`. */
    data class PartesDaProva(val porcento: Int, val provaKm: Double) : PilulaDaEstrada

    /** `2,3 provas de 15 km`. */
    data class ProvasInteiras(val vezes: Double, val provaKm: Double) : PilulaDaEstrada

    /** `62% de uma meia maratona`. */
    data class PartesDaClassica(val porcento: Int, val distancia: DistanciaClassica) : PilulaDaEstrada

    /** `1,4 maratona`. */
    data class Maratonas(val vezes: Double) : PilulaDaEstrada
}

/**
 * A conta é km da temporada ÷ distância da prova do plano ativo. O nome do plano não entra:
 * é texto livre e quebraria o artigo (`de uma Treino de verão`). Sem plano, a distância
 * clássica imediatamente acima do km acumulado; acima da maratona, quantas maratonas.
 */
fun pilulaDaEstrada(km: Double, provaKm: Double?): PilulaDaEstrada {
    if (provaKm != null && provaKm > 0) {
        val razao = km / provaKm
        return if (razao < 1.0) {
            PilulaDaEstrada.PartesDaProva((razao * 100).roundToInt().coerceAtMost(99), provaKm)
        } else {
            PilulaDaEstrada.ProvasInteiras(umaCasa(razao), provaKm)
        }
    }
    val classica = DistanciaClassica.entries.firstOrNull { km < it.km }
        ?: return PilulaDaEstrada.Maratonas(umaCasa(km / DistanciaClassica.MARATONA.km))
    return PilulaDaEstrada.PartesDaClassica((km / classica.km * 100).roundToInt().coerceAtMost(99), classica)
}

private fun umaCasa(valor: Double): Double = Math.floor(valor * 10) / 10

// ---------------------------------------------------------------------------
// O cartão do plano: a prova vem sempre do plano ativo (decisão 17)
// ---------------------------------------------------------------------------

/** Uma coluna do mini trilho: o alvo do longão e o que foi feito. */
data class ColunaDaProva(
    val numero: Int,
    val alvoKm: Double?,
    val feitoKm: Double,
    val corrente: Boolean,
    val prova: Boolean,
    val polimento: Boolean,
    val passada: Boolean,
)

data class ProvaDoPlano(
    val nome: String,
    val data: LocalDate,
    val dias: Long,
    val colunas: List<ColunaDaProva>,
    val maiorCorridaKm: Double,
    val distanciaKm: Double,
    val sessoesFeitas: Int,
    val sessoesAlvo: Int,
)

/**
 * O cartão do plano ativo: o mini trilho do longão (alvo de `weeks.longao_km`, feito de
 * `weekly.longao_feito`), a maior corrida da vida contra a distância da prova, as sessões da
 * semana corrente e os dias até a largada. Sem plano ativo, o cartão não existe.
 */
fun provaDoPlano(
    plano: Plano,
    grade: List<Semana>,
    semanais: Map<Int, AgregadoDaSemana>,
    recordes: Recordes?,
    corridas: List<Corrida>,
    hoje: LocalDate,
): ProvaDoPlano {
    val dataDaProva = plano.dataProva.atZone(plano.fuso).toLocalDate()
    val corrente = grade.firstOrNull { semana ->
        val inicio = semana.dataInicio.atZone(plano.fuso).toLocalDate()
        val fim = semana.dataFim.atZone(plano.fuso).toLocalDate()
        !hoje.isBefore(inicio) && hoje.isBefore(fim)
    }
    val maior = recordes?.maiorDistanciaKm ?: corridas.filter { it.valida() }.maxOfOrNull { it.km } ?: 0.0
    return ProvaDoPlano(
        nome = plano.nome,
        data = dataDaProva,
        dias = ChronoUnit.DAYS.between(hoje, dataDaProva).coerceAtLeast(0),
        colunas = grade.map { semana ->
            ColunaDaProva(
                numero = semana.numero,
                alvoKm = semana.longaoKm ?: if (semana.tipo == TipoDeSemana.PROVA) plano.distanciaAlvoKm else null,
                feitoKm = semanais[semana.numero]?.longaoFeito ?: 0.0,
                corrente = semana.numero == corrente?.numero,
                prova = semana.tipo == TipoDeSemana.PROVA,
                polimento = semana.tipo == TipoDeSemana.TAPER,
                passada = !semana.dataFim.atZone(plano.fuso).toLocalDate().isAfter(hoje),
            )
        },
        maiorCorridaKm = maior,
        distanciaKm = plano.distanciaAlvoKm,
        sessoesFeitas = corrente?.let { semanais[it.numero]?.sessoesFeitas } ?: 0,
        sessoesAlvo = corrente?.sessoesAlvo ?: 0,
    )
}

// ---------------------------------------------------------------------------
// Ano em selos: a sequência de semanas com corrida
// ---------------------------------------------------------------------------

data class Sequencia(val atual: Int, val melhor: Int)

/**
 * Semanas de calendário seguidas com ao menos uma corrida válida, no fuso de cada plano.
 *
 * **A semana corrente ainda não acabou**: sem corrida nela, a sequência atual conta da
 * semana anterior, e não zera na segunda-feira.
 */
fun sequenciaDeSemanas(corridas: List<Corrida>, fusos: Map<String, ZoneId>, hoje: LocalDate): Sequencia {
    val comCorrida = corridasPorSemana(corridas, fusos).keys
    if (comCorrida.isEmpty()) return Sequencia(0, 0)

    var melhor = 0
    var corrida = 0
    var anterior: LocalDate? = null
    comCorrida.sorted().forEach { semana ->
        corrida = if (anterior != null && anterior!!.plusWeeks(1) == semana) corrida + 1 else 1
        melhor = maxOf(melhor, corrida)
        anterior = semana
    }

    val atual = inicioDaSemanaDe(hoje)
    var ponta = if (atual in comCorrida) atual else atual.minusWeeks(1)
    var seguidas = 0
    while (ponta in comCorrida) {
        seguidas++
        ponta = ponta.minusWeeks(1)
    }
    return Sequencia(seguidas, melhor)
}

// ---------------------------------------------------------------------------
// O ritmo
// ---------------------------------------------------------------------------

/**
 * A escada do ritmo: uma marca por corrida (o pace dela, em ordem de tempo) e o número da
 * tela, que é a última corrida até haver média móvel de 4 semanas; com ela, a média.
 */
data class RitmoDaTendencia(val marcas: List<Long>, val atualSeg: Long?, val ritmo: Ritmo?, val daMedia: Boolean)

fun ritmoDaTendencia(corridas: List<Corrida>, pace: PaceMovel): RitmoDaTendencia {
    val marcas = corridas
        .filter { it.valida() && it.km > 0 && it.duracaoSeg > 0 }
        .sortedBy { it.dataHoraInicio }
        .map { Math.round(MotorDeXp.paceDe(it)) }
    val media = (pace as? PaceMovel.Serie)?.pontos?.lastOrNull()?.paceSegKm?.let { Math.round(it) }
    val atual = media ?: marcas.lastOrNull()
    return RitmoDaTendencia(marcas, atual, atual?.let(::ritmoDe), daMedia = media != null)
}

// ---------------------------------------------------------------------------
// As fitas (decisão 18): quantas das cinco já existem
// ---------------------------------------------------------------------------

fun fitasConquistadas(recordes: Recordes?): Int = listOfNotNull(
    recordes?.maiorDistanciaKm,
    recordes?.maiorDuracaoSeg,
    recordes?.melhorPaceSegKm,
    recordes?.melhor1kSeg,
    recordes?.melhor5kSeg,
).size
