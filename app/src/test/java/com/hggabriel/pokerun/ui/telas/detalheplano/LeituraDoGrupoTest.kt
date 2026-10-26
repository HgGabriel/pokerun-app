package com.hggabriel.pokerun.ui.telas.detalheplano

import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.regras.AgregadoDaSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * A leitura do `weekly` do grupo na `PlanDetailScreen` (R3). O que se prova sem Firestore:
 * quem se lê, quais semanas, e que um documento ausente não vira dado.
 */
class LeituraDoGrupoTest {

    private fun membro(uid: String, entrouNaSemana: Int) =
        Membro(uid = uid, nome = uid, entrouEm = Instant.EPOCH, entrouNaSemana = entrouNaSemana)

    private fun agregado(sessoes: Int, longao: Double) = AgregadoDaSemana(
        planoId = "plano-1",
        xpSemana = 0L,
        sessoesFeitas = sessoes,
        kmFeito = longao,
        longaoFeito = longao,
        aderenciaPct = 0.0,
        fechada = false,
    )

    @Test
    fun `so a outra pessoa e lida, da semana em que entrou ate a corrente`() {
        val janelas = semanasALer(listOf(membro("eu", 1), membro("ela", 3)), uid = "eu", ateSemana = 7)

        assertEquals(mapOf("ela" to 3..7), janelas)
    }

    @Test
    fun `antes de o plano comecar a janela e vazia e nada se le`() {
        // `ateSemana` é 0 antes da primeira segunda-feira (DA-10).
        val janelas = semanasALer(listOf(membro("eu", 1), membro("ela", 1)), uid = "eu", ateSemana = 0)

        assertTrue(janelas.getValue("ela").isEmpty())
    }

    @Test
    fun `documento ausente sai do mapa`() {
        val feitos = feitosDosSemanais(mapOf(3 to agregado(2, 7.5), 4 to null))

        assertEquals(mapOf(3 to FeitoNaSemana(2, 7.5)), feitos)
    }

    @Test
    fun `a chave muda com a janela e com a tentativa, e nao com uma corrida sua`() {
        val janelas = mapOf("ela" to 3..7)

        assertEquals(ChaveDoGrupo(janelas, 0), ChaveDoGrupo(mapOf("ela" to 3..7), 0))
        assertNotEquals(ChaveDoGrupo(janelas, 0), ChaveDoGrupo(mapOf("ela" to 3..8), 0))
        assertNotEquals(ChaveDoGrupo(janelas, 0), ChaveDoGrupo(janelas, 1))
    }
}
