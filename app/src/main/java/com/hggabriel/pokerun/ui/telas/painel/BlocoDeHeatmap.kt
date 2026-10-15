package com.hggabriel.pokerun.ui.telas.painel

import com.hggabriel.pokerun.dominio.modelo.Corrida
import java.time.LocalDate
import java.time.ZoneId

/** O quanto o dia correu: quilômetros somados e quantas corridas. */
data class DiaDoHeatmap(val data: LocalDate, val km: Double, val corridas: Int) {
    val degrau: Int get() = degrauDoDia(km, corridas)
}

/** A grade anual: uma coluna por semana (segunda a domingo), com nulo fora do ano. */
data class GradeDoHeatmap(val ano: Int, val colunas: List<List<DiaDoHeatmap?>>) {
    val dias: List<DiaDoHeatmap> get() = colunas.flatten().filterNotNull()
    val diasComCorrida: Int get() = dias.count { it.corridas > 0 }
    val kmNoAno: Double get() = dias.sumOf { it.km }
}

/** Quilômetros e dias de um mês, para a leitura falada. */
data class ResumoDoMes(val mes: Int, val diasComCorrida: Int, val km: Double)

val LIMITES_DO_HEATMAP_KM: List<Int> = listOf(5, 10, 15)

fun degrauDoDia(km: Double, corridas: Int): Int = 0

fun gradeDoHeatmap(corridas: List<Corrida>, fusos: Map<String, ZoneId>, ano: Int): GradeDoHeatmap =
    GradeDoHeatmap(ano, emptyList())

fun diaDaColuna(grade: GradeDoHeatmap, coluna: Int, linha: Int): DiaDoHeatmap? = null

fun resumoDosMeses(grade: GradeDoHeatmap): List<ResumoDoMes> = emptyList()
