package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Semana
import kotlin.math.ceil

/** O documento `plans/{planId}/desafios/{semanaRef}` (docs/05 §1). */
data class Desafio(
    val tipo: String,
    val meta: Double,
    val acumulado: Double,
    val concluido: Boolean,
    val insigniaId: String?,
)

/**
 * O desafio coletivo da semana e as Insígnias de Ginásio (`F2-T13`, docs/03 §3.16).
 *
 * **Quem cria e define a meta é o app** (decisão nº 109), pela regra da nº 129: o km
 * que o plano prevê para a semana, vezes os membros ativos. O grupo bate a meta quando
 * corre, junto, o volume que o plano pediu a cada um. **Sem XP**: concluir só libera a
 * insígnia; só corrida gera XP (decidido pelo humano em 25/09).
 */
object DesafioColetivo {

    private const val TIPO_KM_DO_GRUPO = "km_grupo"

    /** Os oito ginásios de Kanto, na ordem do jogo. */
    val INSIGNIAS: List<String> = listOf("rocha", "cascata", "trovao", "arco_iris", "alma", "pantano", "vulcao", "terra")

    /**
     * O desafio da semana depois de somar os `weekly` do grupo.
     *
     * A meta é fixada na criação e não muda depois, mesmo que alguém entre no meio da
     * semana. O acumulado se refaz a cada leitura: uma correção que baixa o km baixa o
     * acumulado. **O concluído e a insígnia não voltam atrás**, como a coleção (RN-38): o
     * grupo não perde uma insígnia por correção de dado.
     *
     * @param concluidosAntes quantos desafios do plano já tinham sido concluídos: a
     * insígnia nova é a próxima da lista. Depois da oitava, o desafio conclui sem insígnia.
     */
    fun atualizar(
        existente: Desafio?,
        semana: Semana,
        membrosAtivos: Int,
        semanais: Collection<AgregadoDaSemana?>,
        concluidosAntes: Int,
    ): Desafio {
        val meta = existente?.meta ?: ceil(semana.kmAlvo * membrosAtivos)
        val acumulado = semanais.sumOf { it?.kmFeito ?: 0.0 }
        val jaConcluido = existente?.concluido == true
        val concluido = jaConcluido || acumulado >= meta
        val insignia = when {
            jaConcluido -> existente?.insigniaId
            concluido -> INSIGNIAS.getOrNull(concluidosAntes)
            else -> null
        }
        return Desafio(TIPO_KM_DO_GRUPO, meta, acumulado, concluido, insignia)
    }

    /** As insígnias do grupo, na ordem dos ginásios. */
    fun conquistadas(desafios: Collection<Desafio>): List<String> {
        val ganhas = desafios.mapNotNull { it.insigniaId }.toSet()
        return INSIGNIAS.filter { it in ganhas }
    }
}
