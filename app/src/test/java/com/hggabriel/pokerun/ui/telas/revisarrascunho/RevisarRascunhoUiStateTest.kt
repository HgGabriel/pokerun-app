package com.hggabriel.pokerun.ui.telas.revisarrascunho

import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.Viabilidade
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * O que libera e o que segura o botão de criar (`F1-T11`, docs/03 §3.6).
 *
 * Vira teste porque o botão consulta uma propriedade do estado, e não uma condição
 * escrita dentro do `@Composable`. **O pico reduzido não entra na conta** (DA-10): o
 * plano cabe, e a tela só diz quanto falta para o pico desejado.
 */
class RevisarRascunhoUiStateTest {

    private val grade = listOf(
        Semana(
            numero = 1,
            dataInicio = Instant.EPOCH,
            dataFim = Instant.EPOCH.plusSeconds(7 * 24 * 3600),
            sessoesAlvo = 3,
            kmAlvo = 10.0,
            longaoKm = 5.0,
            tipo = TipoDeSemana.BUILD,
            parcial = false,
        ),
    )

    private val revisando = RevisarRascunhoUiState(nomeDoPlano = "São Silvestre", grade = grade)

    @Test
    fun `o pico reduzido nao bloqueia a criacao`() {
        val reduzido = revisando.copy(
            picoReduzido = Viabilidade.PicoReduzido(semanas = 12, picoKm = 8.25, picoDesejadoKm = 11.0),
        )

        assertTrue(reduzido.podeCriar)
    }

    @Test
    fun `sem rede nao cria`() {
        // A reserva do código de convite é transacional (RN-29) e não resolve no cache:
        // offline a escrita fica pendurada em vez de falhar (docs/05 §2.6).
        assertFalse(revisando.copy(online = false).podeCriar)
    }

    @Test
    fun `enquanto grava, o botao nao aceita um segundo toque`() {
        assertFalse(revisando.copy(salvando = true).podeCriar)
    }

    @Test
    fun `grade vazia nao cria`() {
        // Só acontece se a geração falhar. Um plano sem semanas é um plano quebrado, e
        // `PlanoRepositorio.criar` são duas idas: a primeira gravaria o documento.
        assertFalse(revisando.copy(grade = emptyList()).podeCriar)
    }
}
