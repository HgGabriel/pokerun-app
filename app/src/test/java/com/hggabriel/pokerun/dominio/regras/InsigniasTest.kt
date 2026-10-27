package com.hggabriel.pokerun.dominio.regras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** As insígnias oficiais de Kanto (decisão 19): a tabela do nome para o arquivo. */
class InsigniasTest {

    @Test
    fun `as oito de Kanto sao os arquivos 1 a 8 na ordem dos ginasios`() {
        assertEquals((1..8).toList(), DesafioColetivo.INSIGNIAS.map { Insignias.numero(it) })
        assertEquals(1, Insignias.numero("rocha"))
        assertEquals(8, Insignias.numero("terra"))
    }

    @Test
    fun `a url e a do repositorio de sprites da PokeAPI`() {
        assertEquals(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/badges/3.png",
            Insignias.url("trovao"),
        )
        assertNull(Insignias.url("johto"))
    }

    @Test
    fun `as insignias da vida juntam os planos sem repetir e na ordem dos ginasios`() {
        val planoA = listOf(desafio("cascata"), desafio(null))
        val planoB = listOf(desafio("rocha"), desafio("cascata"))

        assertEquals(listOf("rocha", "cascata"), Insignias.daVida(listOf(planoA, planoB)))
        assertEquals(emptyList<String>(), Insignias.daVida(emptyList()))
    }

    private fun desafio(insignia: String?) = Desafio("km_grupo", 40.0, 41.0, insignia != null, insignia)
}
