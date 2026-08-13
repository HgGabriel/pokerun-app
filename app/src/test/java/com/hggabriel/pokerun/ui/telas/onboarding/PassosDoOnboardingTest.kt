package com.hggabriel.pokerun.ui.telas.onboarding

import com.hggabriel.pokerun.dados.healthconnect.StatusDoHealthConnect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A ordem do onboarding e a leitura dos dois campos do perfil (`F1-T08`,
 * docs/03 §3.2).
 *
 * **A ordem é rígida, e a razão é de dado, não de estética:** não dá para listar quem
 * grava no Health Connect antes de pedir permissão e ler dele. Uma tela que pule o
 * passo 3 chega ao passo 5 com lista vazia e conclui "este aparelho não tem origem
 * nenhuma", que é indistinguível do estado vazio legítimo. É o mesmo motivo pelo qual
 * `EXECUCAO.md §8` lista a ordem como armadilha conhecida do projeto.
 *
 * O que dá para verificar sem tela é exatamente o que a revisão de olho deixa passar:
 * a máquina de passos e o que o formulário aceita como distância.
 */
class PassosDoOnboardingTest {

    // ---------------------------------------------------------------------
    // A ordem rígida (docs/03 §3.2)
    // ---------------------------------------------------------------------

    @Test
    fun `com Health Connect e sem permissao, o passo depois do perfil e pedir permissao`() {
        val passo = passoDepoisDoPerfil(
            saude = StatusDoHealthConnect.Disponivel,
            permissaoConcedida = false,
        )

        assertEquals(OnboardingUiState.SolicitandoPermissao(), passo)
    }

    @Test
    fun `com a permissao ja concedida, o perfil cai direto na leitura das origens`() {
        // Reinstalação e segunda passada pelo onboarding: o Health Connect lembra da
        // concessão. Abrir a folha de permissão de novo não pede nada a ninguém.
        val passo = passoDepoisDoPerfil(
            saude = StatusDoHealthConnect.Disponivel,
            permissaoConcedida = true,
        )

        assertEquals(OnboardingUiState.LendoOrigens, passo)
    }

    @Test
    fun `sem Health Connect no aparelho, os passos 3 a 5 somem e o onboarding acaba`() {
        // docs/05 §4.4: indisponível não é erro, é o modo manual. Sem tela de erro.
        val passo = passoDepoisDoPerfil(
            saude = StatusDoHealthConnect.Indisponivel,
            permissaoConcedida = false,
        )

        assertEquals(OnboardingUiState.Concluido, passo)
    }

    @Test
    fun `Health Connect desatualizado tambem pula os passos 3 a 5`() {
        // `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` é indisponível para efeito de
        // leitura: o cliente não conecta. Mandar o usuário à Play Store no meio do
        // cadastro é travar o onboarding num caminho que docs/05 §4.4 já resolve.
        val passo = passoDepoisDoPerfil(
            saude = StatusDoHealthConnect.PrecisaAtualizar,
            permissaoConcedida = false,
        )

        assertEquals(OnboardingUiState.Concluido, passo)
    }

    @Test
    fun `permissao concedida leva a leitura das origens`() {
        assertEquals(OnboardingUiState.LendoOrigens, passoDepoisDaPermissao(concedida = true))
    }

    @Test
    fun `permissao negada volta ao passo 3, e nao segue para a escolha da fonte`() {
        // O caminho que precisa falhar: seguir para `EscolhendoFonte` com lista vazia
        // depois de uma negativa. A tela diria "nenhuma corrida encontrada", que é
        // mentira: ninguém procurou.
        val passo = passoDepoisDaPermissao(concedida = false)

        assertEquals(OnboardingUiState.SolicitandoPermissao(negada = true), passo)
    }

    // ---------------------------------------------------------------------
    // O nome (passo 1)
    // ---------------------------------------------------------------------

    @Test
    fun `o nome sai sem espaco sobrando`() {
        assertEquals("Hiago", nomeDoPerfil("  Hiago  "))
    }

    @Test
    fun `nome em branco nao e nome`() {
        assertNull(nomeDoPerfil(""))
        assertNull(nomeDoPerfil("   "))
        assertNull(nomeDoPerfil("\t\n"))
    }

    // ---------------------------------------------------------------------
    // A distância confortável (passo 2, docs/01 §3.1)
    // ---------------------------------------------------------------------

    @Test
    fun `a virgula e o ponto valem a mesma coisa`() {
        // O teclado decimal de um aparelho em pt-BR entrega vírgula. Recusá-la seria
        // recusar o que o aparelho digita.
        assertEquals(7.5, distanciaEmKm("7,5"))
        assertEquals(7.5, distanciaEmKm("7.5"))
    }

    @Test
    fun `inteiro e aceito, com ou sem espaco em volta`() {
        assertEquals(5.0, distanciaEmKm("5"))
        assertEquals(5.0, distanciaEmKm(" 5 "))
    }

    @Test
    fun `distancia menor que um quilometro e valida`() {
        // "Respondível por iniciantes" (docs/01 §3.1). Quem corre 800 m responde 0,8.
        assertEquals(0.8, distanciaEmKm("0,8"))
    }

    @Test
    fun `zero nao e uma distancia confortavel`() {
        // O gerador interpola de `baseline_km` até o alvo (docs/01 §3.2). Com zero, a
        // primeira semana nasce em zero e a grade inteira sai errada.
        assertNull(distanciaEmKm("0"))
        assertNull(distanciaEmKm("0,0"))
    }

    @Test
    fun `texto que nao e numero nao vira distancia`() {
        assertNull(distanciaEmKm(""))
        assertNull(distanciaEmKm("   "))
        assertNull(distanciaEmKm("cinco"))
        assertNull(distanciaEmKm("-3"))
        assertNull(distanciaEmKm("5,5,5"))
        assertNull(distanciaEmKm("5 km"))
    }

    @Test
    fun `notacao cientifica colada de fora nao passa`() {
        // `toDoubleOrNull("1e3")` devolve 1000.0 sem reclamar, e o teclado decimal não
        // digita `e` — mas colar do teclado passa por cima do teclado. Um plano com
        // baseline de 1.000 km é grade inteira absurda gerada em silêncio.
        assertNull(distanciaEmKm("1e3"))
        assertNull(distanciaEmKm("Infinity"))
        assertNull(distanciaEmKm("NaN"))
    }

    @Test
    fun `numero grande demais para uma perna humana nao passa`() {
        assertNull(distanciaEmKm("1000"))
        assertTrue((distanciaEmKm("999") ?: 0.0) > 0.0)
    }
}
