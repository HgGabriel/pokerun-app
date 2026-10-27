package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import com.hggabriel.pokerun.dominio.regras.Recordes
import java.time.LocalDate
import java.time.ZoneId

/**
 * O Progresso (`F3-T09`, redesenhado pelo estudo de Progresso e Perfil, Revisão 3): a raiz
 * com a estrada, a prova, a estante e o Você sabia, e as quatro telas atrás das portas.
 * Todas as telas leem o mesmo estado, e nenhuma escreve.
 */
sealed interface PainelUiState {

    data object Carregando : PainelUiState

    /**
     * Os campos de cima são os blocos de antes, que agora moram atrás das portas: o ano em
     * selos e o volume na tela Selos, o pace e a eficiência na tela Ritmo, os recordes nas
     * fitas. Os de baixo são as peças novas da raiz.
     */
    data class Conteudo(
        val hoje: LocalDate,
        val heatmap: GradeDoHeatmap,
        val recordes: Recordes?,
        val volume: VolumeAcumulado,
        val pace: PaceMovel,
        val eficiencia: EficienciaCardiaca,
        val fuso: ZoneId,
        val estrada: EstradaDaTemporada? = null,
        val pilula: PilulaDaEstrada = PilulaDaEstrada.PartesDaClassica(0, DistanciaClassica.CINCO),
        /** O número da Pokédex do parceiro, que anda pela estrada (decisão 13). */
        val parceiro: Int? = null,
        val tipoDoParceiro: TipoDePokemon? = null,
        val prova: ProvaDoPlano? = null,
        val ritmo: RitmoDaTendencia = RitmoDaTendencia(emptyList(), null, null, false),
        val sequencia: Sequencia = Sequencia(0, 0),
        val corridas: Int = 0,
        val kmNaVida: Double = 0.0,
        val curiosidades: List<Curiosidade> = emptyList(),
        /** As cinco fitas dos recordes, com o dia e a espécie da época (decisão 18). */
        val fitas: List<FitaDoRecorde> = emptyList(),
    ) : PainelUiState

    data object Falhou : PainelUiState
}

private val FUSO_DE_RESERVA: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * Monta os blocos de dado de [corridas] (docs/03 §3.17). O fuso de cada corrida é o do plano
 * dela, por RN-28; a sem plano conhecido cai no da temporada.
 */
fun montarPainel(
    corridas: List<Corrida>,
    planos: List<Plano>,
    recordes: Recordes?,
    hoje: LocalDate,
): PainelUiState.Conteudo {
    val fusos = planos.associate { it.id to it.fuso }
    val maisRecente = corridas.filter { !it.descartada && !it.substituida }.maxByOrNull { it.dataHoraInicio }

    return PainelUiState.Conteudo(
        hoje = hoje,
        heatmap = gradeDoHeatmap(corridas, fusos, hoje.year),
        recordes = recordes,
        volume = volumeAcumulado(corridas, planos, hoje),
        pace = paceEmMediaMovel(corridas, fusos, hoje),
        eficiencia = eficienciaCardiaca(corridas),
        fuso = maisRecente?.let { fusos[it.planoId] } ?: FUSO_DE_RESERVA,
    )
}

/** O que o `ViewModel` junta antes de montar o Progresso. */
data class EntradaDoProgresso(
    val corridas: List<Corrida>,
    val planos: List<Plano>,
    val recordes: Recordes?,
    val temporada: Temporada?,
    val catalogo: List<Temporada>,
    val planoAtivo: Plano?,
    val grade: List<Semana>,
    val semanais: Map<Int, AgregadoDaSemana>,
    val parceiro: Int?,
    val tipoDoParceiro: TipoDePokemon?,
    val hoje: LocalDate,
)

/**
 * O Progresso inteiro: os blocos de [montarPainel] e as peças da raiz. A prova vem sempre do
 * plano ativo (decisão 17); sem plano, o cartão some e a pílula compara com as distâncias
 * clássicas.
 */
fun montarProgresso(entrada: EntradaDoProgresso): PainelUiState.Conteudo {
    val painel = montarPainel(entrada.corridas, entrada.planos, entrada.recordes, entrada.hoje)
    val fusos = entrada.planos.associate { it.id to it.fuso }
    val validas = entrada.corridas.filter { it.valida() }
    val estrada = entrada.temporada?.let {
        estradaDaTemporada(entrada.corridas, it, entrada.planoAtivo, entrada.hoje, painel.fuso)
    }
    return painel.copy(
        estrada = estrada,
        pilula = pilulaDaEstrada(estrada?.kmNaTemporada ?: 0.0, entrada.planoAtivo?.distanciaAlvoKm),
        parceiro = entrada.parceiro,
        tipoDoParceiro = entrada.tipoDoParceiro,
        prova = entrada.planoAtivo?.takeIf { entrada.grade.isNotEmpty() }?.let {
            provaDoPlano(it, entrada.grade, entrada.semanais, entrada.recordes, entrada.corridas, entrada.hoje)
        },
        ritmo = ritmoDaTendencia(entrada.corridas, painel.pace),
        sequencia = sequenciaDeSemanas(entrada.corridas, fusos, entrada.hoje),
        corridas = validas.size,
        kmNaVida = validas.sumOf { it.km },
        curiosidades = curiosidades(entrada.corridas, fusos),
        fitas = fitasDosRecordes(entrada.corridas, entrada.recordes, entrada.catalogo, fusos),
    )
}
