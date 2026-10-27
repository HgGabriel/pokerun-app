package com.hggabriel.pokerun.ui.telas.pokedex

import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.regras.MarcoDaSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * O álbum montado para a Pokédex (docs/03 §3.18). Três tiers de 4 espécies a 100, 250 e
 * 1.000 XP: a posição 4 custa 400, a 5 custa 650, e o tier 3 é o lacrado.
 */
class MontagemDoAlbumTest {

    private fun montar(xp: Long, colecao: Int, meta: Long? = null, pacote: IntRange = IntRange.EMPTY, historico: List<MarcoDaSemana> = emptyList()) =
        montarAlbum(temporada, xp, colecao, historico, emptyList(), meta, Instant.parse("2026-02-01T12:00:00Z"), pacote)

    @Test
    fun `o tier mais alto e o envelope lacrado e fica fora da fita`() {
        val album = montar(xp = 2_000, colecao = 8)

        assertEquals(listOf(false, false, true), album.paginas.map { it.lacrada })
        assertTrue(album.capa.fita.none { it.numero in listOf(144, 145, 146, 150) })
        // Na última posição antes do lacrado não há próximo para prometer.
        assertNull(album.capa.proximo)
    }

    @Test
    fun `a posicao e o dobro e o proximo e a vaga marcada`() {
        val album = montar(xp = 400, colecao = 4)
        val tier1 = album.paginas[0].figurinhas

        assertTrue(tier1.single { it.posicao == 4 }.atual)
        val proximo = album.paginas[1].figurinhas.first()
        assertTrue(proximo.proximo)
        assertFalse(proximo.colada)
        assertEquals(1, album.capa.proximo)
    }

    @Test
    fun `a meta marca as vagas que ela alcanca`() {
        // 400 + 300 = 700: alcança a posição 5 (650), não a 6 (900).
        val album = montar(xp = 400, colecao = 4, meta = 300)

        assertEquals(listOf(5), album.capa.fita.filter { it.naMeta }.map { it.posicao })
    }

    @Test
    fun `o pacote deixa vaga o que ainda nao colou, inclusive a posicao`() {
        val album = montar(xp = 650, colecao = 5, pacote = 4..5)
        val todas = album.paginas.flatMap { it.figurinhas }

        assertFalse(todas.single { it.posicao == 4 }.colada)
        assertFalse(todas.single { it.posicao == 5 }.colada)
        assertTrue(todas.single { it.posicao == 5 }.atual)
        assertTrue(todas.single { it.posicao == 3 }.colada)
    }

    @Test
    fun `nova e o que a colecao ganhou depois da ultima semana fechada`() {
        val album = montar(xp = 650, colecao = 5, historico = listOf(MarcoDaSemana("p_1", 3)))

        assertEquals(listOf(4, 5), album.paginas.flatMap { it.figurinhas }.filter { it.nova }.map { it.posicao })
    }

    @Test
    fun `no recuo a capa leva a colecao e a forma de hoje`() {
        val album = montar(xp = 400, colecao = 6)

        assertNotNull(album.capa.recuo)
        assertEquals(6, album.capa.recuo!!.colecao)
        assertEquals(4, album.capa.recuo!!.formaAtual)
        // A coleção atrás da posição fica colada (RN-38).
        assertTrue(album.paginas[1].figurinhas.take(2).all { it.colada })
    }

    @Test
    fun `o indice da a fracao colada de cada tier`() {
        val album = montar(xp = 650, colecao = 5)

        assertEquals(listOf(1 to 1f, 2 to 0.25f, 3 to 0f), album.capa.indice)
        assertEquals(2, album.tierAtual)
    }

    @Test
    fun `a temporada encerrada fica congelada, sem proximo, km nem meta`() {
        val album = montarAlbum(
            temporada, 400, 4, emptyList(), emptyList(), xpDaMeta = 300,
            agora = Instant.parse("2027-02-01T12:00:00Z"), encerrada = "Kanto 2026",
        )

        assertEquals("Kanto 2026", album.capa.encerrada)
        assertNull(album.capa.proximo)
        assertNull(album.capa.kmAteOProximo)
        assertTrue(album.capa.fita.none { it.proximo || it.naMeta })
        assertTrue(album.kmParaAlcancar.isEmpty())
    }

    private val temporada = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = Instant.parse("2026-01-01T03:00:00Z"),
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 12,
        tiers = listOf(
            Tier(1, "Passo", "", listOf(10, 11, 13, 14), 100),
            Tier(2, "Trote", "", listOf(1, 4, 7, 9), 250),
            Tier(3, "Lenda", "", listOf(144, 145, 146, 150), 1_000),
        ),
    )
}
