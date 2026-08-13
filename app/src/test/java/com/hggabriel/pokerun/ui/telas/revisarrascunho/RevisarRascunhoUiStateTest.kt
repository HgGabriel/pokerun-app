package com.hggabriel.pokerun.ui.telas.revisarrascunho

import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.SaltoDeVolume
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * A metade de RN-30 que a `EdicaoDaGradeTest` não alcança: **o alerta nunca bloqueia**
 * (`F1-T11`, docs/01 §3.3).
 *
 * A regra tem duas partes e as duas precisam de prova. A primeira — *quando* o salto de
 * 15% dispara — é função pura sobre a grade. A segunda é uma frase sobre a tela: *"nunca
 * bloqueia a gravação"*. Ela vira teste porque o botão de criar consulta uma propriedade
 * do estado, e não uma condição escrita dentro do `@Composable`.
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
    fun `o alerta de quinze por cento nao bloqueia a criacao`() {
        // RN-30: o aviso é discreto e não bloqueante. Quem quiser gravar uma grade
        // agressiva, grava — o app avisa, não impede.
        val comAlerta = revisando.copy(alerta = SaltoDeVolume(de = 6, para = 7, percentual = 22))

        assertTrue(comAlerta.podeCriar)
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
