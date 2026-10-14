package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Membro
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * O ranking do grupo (`F2-T12`, docs/03 §3.16), escrito antes da implementação.
 * RN-18: ordena pelo XP da semana, nunca pelo acumulado. RN-09: km nunca ordena.
 */
class RankingTest {

    @Test
    fun `quem tem mais XP acumulado e menos na semana fica atras`() {
        // O avatar é a posição na escada, que só cresce com o acumulado: Ana está muito
        // à frente na temporada, e atrás nesta semana.
        val ranking = Ranking.ordenar(
            listOf(membro("ana", avatar = 120), membro("bia", avatar = 12)),
            mapOf("ana" to semanal(xp = 500), "bia" to semanal(xp = 800)),
            sessoesPrevistas = 3,
        )

        assertEquals(listOf("bia", "ana"), ranking.map { it.uid })
        assertEquals(listOf(1, 2), ranking.map { it.posicao })
    }

    @Test
    fun `membro sem weekly na semana aparece com zero XP, nao some`() {
        val ranking = Ranking.ordenar(
            listOf(membro("ana"), membro("bia")),
            mapOf("ana" to semanal(xp = 500), "bia" to null),
            sessoesPrevistas = 3,
        )

        val bia = ranking.single { it.uid == "bia" }
        assertEquals(0L, bia.xpSemana)
        assertEquals(0, bia.sessoesFeitas)
        assertEquals(3, bia.sessoesPrevistas)
        assertEquals(2, bia.posicao)
    }

    @Test
    fun `mais km e menos XP na semana fica atras`() {
        val ranking = Ranking.ordenar(
            listOf(membro("ana"), membro("bia")),
            mapOf("ana" to semanal(xp = 600, km = 30.0), "bia" to semanal(xp = 900, km = 12.0)),
            sessoesPrevistas = 3,
        )

        assertEquals(listOf("bia", "ana"), ranking.map { it.uid })
    }

    @Test
    fun `na semana 1 sem corrida todos aparecem com zero`() {
        // docs/03 §3.16: membros com 0%, e não lista vazia.
        val ranking = Ranking.ordenar(listOf(membro("ana"), membro("bia"), membro("caio")), emptyMap(), sessoesPrevistas = 3)

        assertEquals(3, ranking.size)
        assertEquals(setOf(0L), ranking.map { it.xpSemana }.toSet())
    }

    @Test
    fun `empate no XP divide a posicao e a ordem segue o nome`() {
        val ranking = Ranking.ordenar(
            listOf(membro("caio"), membro("ana"), membro("bia")),
            mapOf("caio" to semanal(xp = 700), "ana" to semanal(xp = 700), "bia" to semanal(xp = 300)),
            sessoesPrevistas = 3,
        )

        assertEquals(listOf("ana", "caio", "bia"), ranking.map { it.uid })
        assertEquals(listOf(1, 1, 3), ranking.map { it.posicao })
    }

    @Test
    fun `membro inativo nao entra`() {
        val ranking = Ranking.ordenar(
            listOf(membro("ana"), membro("bia").copy(ativo = false)),
            emptyMap(),
            sessoesPrevistas = 3,
        )

        assertEquals(listOf("ana"), ranking.map { it.uid })
    }

    private fun membro(uid: String, avatar: Int? = null) = Membro(
        uid = uid,
        nome = uid.replaceFirstChar { it.uppercase() },
        entrouEm = Instant.parse("2026-08-10T12:00:00Z"),
        entrouNaSemana = 1,
        avatarEspecie = avatar,
    )

    private fun semanal(xp: Long, km: Double = 10.0, feitas: Int = 2) = AgregadoDaSemana(
        planoId = "p1",
        xpSemana = xp,
        sessoesFeitas = feitas,
        kmFeito = km,
        longaoFeito = 0.0,
        aderenciaPct = 0.0,
        fechada = false,
    )
}
