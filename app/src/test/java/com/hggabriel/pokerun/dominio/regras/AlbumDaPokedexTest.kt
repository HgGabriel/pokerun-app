package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * O álbum da Pokédex (docs/03 §3.18): a corrida que colou cada figurinha, o km de cada
 * página, o km até o próximo e as figurinhas novas. A temporada de teste tem 4 espécies de
 * 100 XP e 4 de 250 XP: a posição 2 custa 200, a 4 custa 400, a 5 (1ª do tier 2) custa 650.
 */
class AlbumDaPokedexTest {

    @Test
    fun `cada posicao e colada pela corrida que a alcancou e a primeira e de graca`() {
        val album = albumDaPokedex(
            temporada,
            listOf(corrida("a", dia = 0, km = 3.0, xp = 250), corrida("b", dia = 2, km = 5.0, xp = 450)),
            colecaoMax = 5,
        )

        assertNull(album.coladaEm[1])
        assertEquals("a", album.coladaEm[2]?.id)
        assertEquals("b", album.coladaEm[3]?.id)
        assertEquals("b", album.coladaEm[5]?.id)
        assertNull(album.coladaEm[6])
    }

    @Test
    fun `a ordem e a da data e nao a da lista, e corrida substituida ou descartada nao cola`() {
        val album = albumDaPokedex(
            temporada,
            listOf(
                corrida("tarde", dia = 3, km = 2.0, xp = 200),
                corrida("velha", dia = 1, km = 2.0, xp = 200, substituida = true),
                corrida("ruim", dia = 1, km = 2.0, xp = 200, descartada = true),
                corrida("cedo", dia = 2, km = 2.0, xp = 200),
                corrida("outra", dia = 0, km = 2.0, xp = 900, temporadaId = "x"),
            ),
            colecaoMax = 4,
        )

        assertEquals("cedo", album.coladaEm[2]?.id)
        assertEquals("tarde", album.coladaEm[4]?.id)
    }

    @Test
    fun `nada cola acima da colecao`() {
        val album = albumDaPokedex(temporada, listOf(corrida("a", dia = 0, km = 10.0, xp = 2_000)), colecaoMax = 3)

        assertEquals(setOf(2, 3), album.coladaEm.keys)
    }

    @Test
    fun `o km da corrida se divide entre as paginas na proporcao do XP que ela cobriu`() {
        // 0 → 400 XP no tier 1 (fim em 650): 4 km. Depois 400 → 900: 250 XP no tier 1 e
        // 250 no tier 2, 10 km meio a meio.
        val album = albumDaPokedex(
            temporada,
            listOf(corrida("a", dia = 0, km = 4.0, xp = 400), corrida("b", dia = 1, km = 10.0, xp = 500)),
            colecaoMax = 6,
        )

        assertEquals(9.0, album.kmPorTier.getValue(1), 1e-9)
        assertEquals(5.0, album.kmPorTier.getValue(2), 1e-9)
    }

    @Test
    fun `corrida sem XP conta o km na pagina em que o corredor esta`() {
        val album = albumDaPokedex(
            temporada,
            listOf(corrida("a", dia = 0, km = 4.0, xp = 650), corrida("b", dia = 1, km = 3.0, xp = 0)),
            colecaoMax = 5,
        )

        assertEquals(3.0, album.kmPorTier.getValue(2), 1e-9)
    }

    @Test
    fun `o km ate o proximo usa o XP por km das ultimas 4 semanas e arredonda para cima em meio km`() {
        // Na janela: 10 km, 1.000 XP, 100 XP por km. Fora dela, uma corrida lenta que não conta.
        val corridas = listOf(
            corrida("antiga", dia = 0, km = 10.0, xp = 100),
            corrida("a", dia = 40, km = 4.0, xp = 400),
            corrida("b", dia = 45, km = 6.0, xp = 600),
        )

        assertEquals(2.5, kmAteOProximo(faltamXp = 230, corridas, temporada, agora = inicio + dias(50))!!, 1e-9)
        assertEquals(2.0, kmAteOProximo(faltamXp = 200, corridas, temporada, agora = inicio + dias(50))!!, 1e-9)
    }

    @Test
    fun `sem corrida nas ultimas 4 semanas o km ate o proximo usa a temporada toda`() {
        val corridas = listOf(corrida("a", dia = 0, km = 5.0, xp = 1_000))

        assertEquals(1.0, kmAteOProximo(faltamXp = 200, corridas, temporada, agora = inicio + dias(60))!!, 1e-9)
    }

    @Test
    fun `sem corrida, sem XP ou sem proximo nao ha km ate o proximo`() {
        val agora = inicio + dias(5)

        assertNull(kmAteOProximo(faltamXp = 200, emptyList(), temporada, agora))
        assertNull(kmAteOProximo(faltamXp = 200, listOf(corrida("a", dia = 0, km = 5.0, xp = 0)), temporada, agora))
        assertNull(kmAteOProximo(faltamXp = null, listOf(corrida("a", dia = 0, km = 5.0, xp = 100)), temporada, agora))
    }

    @Test
    fun `o km ate o proximo nunca e menos que meio km`() {
        val corridas = listOf(corrida("a", dia = 0, km = 1.0, xp = 1_000))

        assertEquals(0.5, kmAteOProximo(faltamXp = 10, corridas, temporada, agora = inicio + dias(1))!!, 1e-9)
    }

    @Test
    fun `as figurinhas novas sao as que a colecao ganhou depois da ultima semana fechada`() {
        val historico = listOf(MarcoDaSemana("p_1", 3), MarcoDaSemana("p_2", 2))

        assertEquals(4..6, coladasNaSemana(colecaoMax = 6, historico))
        assertTrue(coladasNaSemana(colecaoMax = 3, historico).isEmpty())
        assertEquals(2..4, coladasNaSemana(colecaoMax = 4, emptyList()))
    }

    @Test
    fun `o pacote leva o que a colecao ganhou desde a ultima visita`() {
        assertEquals(5..7, figurinhasDoPacote(colecaoMax = 7, ultimaVista = 4))
        assertTrue(figurinhasDoPacote(colecaoMax = 4, ultimaVista = 4).isEmpty())
        assertTrue(figurinhasDoPacote(colecaoMax = 3, ultimaVista = 4).isEmpty())
        // Na primeira visita não há pacote: quem mostra a coleção é a cascata de estreia.
        assertTrue(figurinhasDoPacote(colecaoMax = 7, ultimaVista = null).isEmpty())
    }

    private val inicio = Instant.parse("2026-01-01T03:00:00Z")

    private fun dias(n: Int): Duration = Duration.ofDays(n.toLong())

    private fun corrida(
        id: String,
        dia: Int,
        km: Double,
        xp: Long,
        temporadaId: String = "t",
        substituida: Boolean = false,
        descartada: Boolean = false,
    ) = Corrida(
        id = id,
        dataHoraInicio = inicio + dias(dia),
        km = km,
        duracaoSeg = (km * 360).toLong(),
        tipoExercicio = "RUNNING",
        origem = OrigemDaCorrida.MANUAL,
        planoId = "p",
        semanaRef = null,
        temporadaId = temporadaId,
        xpCreditado = xp,
        substituida = substituida,
        descartada = descartada,
    )

    private val temporada = Temporada(
        id = "t",
        nome = "Teste",
        regiao = "Kanto",
        inicio = inicio,
        fim = Instant.parse("2027-01-01T03:00:00Z"),
        fuso = ZoneId.of("America/Sao_Paulo"),
        ativa = true,
        totalEspecies = 8,
        tiers = listOf(
            Tier(1, "Passo", "", listOf(10, 11, 13, 14), 100),
            Tier(2, "Trote", "", listOf(1, 4, 7, 9), 250),
        ),
    )
}
