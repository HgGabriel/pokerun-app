package com.hggabriel.pokerun.ui.telas.ajustes

import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.healthconnect.StatusDoHealthConnect
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.telas.onboarding.passoDepoisDoPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os campos do perfil em Ajustes (`F1-T17`, docs/03 §3.11).
 *
 * Escrito **antes** da implementação (`EXECUCAO.md §3.2`). A ficha não cita regra de
 * negócio, e mesmo assim há o que provar sem aparelho: esta tela **reabre dois campos
 * que o onboarding só deixava escrever uma vez**, e o segundo deles semeia a geração de
 * plano (`baseline_km`, docs/01 §3.1 e §4). Errar aqui não quebra a tela — muda o
 * número que a próxima grade vai interpolar.
 *
 * Três coisas que a revisão de olho não pega:
 *
 * - **O ida e volta do campo de baseline.** O valor gravado é `Double` e o campo é
 *   texto. Pré-preencher com um formato que a própria validação recusa deixaria o botão
 *   morto num formulário que ninguém tocou; pré-preencher com uma casa a menos faz
 *   *abrir e salvar* mexer no número de quem não pediu.
 * - **Gravar o que não mudou.** `7,50` e `7,5` são textos diferentes e a mesma
 *   distância. Comparar texto manda uma escrita ao Firestore para não mudar nada.
 * - **O bloco de origem sem Health Connect.** Indisponível é o modo manual e não uma
 *   falha (docs/05 §4.4) — a mesma leitura que `passoDepoisDoPerfil` já faz no
 *   cadastro.
 */
class CamposDoPerfilTest {

    private fun erros(v: ValidacaoDoPerfil): ErrosDoPerfil =
        (v as ValidacaoDoPerfil.Invalido).erros

    private fun valido(v: ValidacaoDoPerfil): ValidacaoDoPerfil.Valido =
        v as ValidacaoDoPerfil.Valido

    // ---------------------------------------------------------------------
    // validarPerfil — os dois campos que a tela reabre
    // ---------------------------------------------------------------------

    @Test
    fun `nome e baseline preenchidos passam`() {
        val v = valido(validarPerfil(nome = "Hiago", baseline = "8"))

        assertEquals("Hiago", v.nome)
        assertEquals(8.0, v.baselineKm, 0.0)
    }

    @Test
    fun `o nome perde o espaco das pontas`() {
        // O mesmo `trim` do cadastro (`nomeDoPerfil`). Sem ele, `" Hiago "` e `"Hiago"`
        // viram dois nomes diferentes no leaderboard, que é denormalizado (docs/05 §1).
        assertEquals("Hiago", valido(validarPerfil(nome = "  Hiago  ", baseline = "8")).nome)
    }

    @Test
    fun `nome em branco falha`() {
        assertEquals(
            R.string.ajustes_erro_nome,
            erros(validarPerfil(nome = "   ", baseline = "8")).nome,
        )
    }

    @Test
    fun `baseline fora da forma falha`() {
        // Mesma forma dos outros campos de km do app (`distanciaEmKm`): até três dígitos
        // e até duas casas. `1e3` é o caso que o `toDoubleOrNull` cru engoliria.
        assertEquals(
            R.string.ajustes_erro_baseline,
            erros(validarPerfil(nome = "Hiago", baseline = "1e3")).baseline,
        )
    }

    @Test
    fun `baseline zero falha`() {
        assertNotNull(erros(validarPerfil(nome = "Hiago", baseline = "0")).baseline)
    }

    @Test
    fun `a virgula do teclado pt-BR e aceita`() {
        assertEquals(7.5, valido(validarPerfil(nome = "Hiago", baseline = "7,5")).baselineKm, 0.0)
    }

    @Test
    fun `os dois erros saem juntos`() {
        // Acusar em série faria a pessoa descobrir o segundo erro no segundo toque. É a
        // mesma escolha de `ErrosDaCorrida` e `ErrosDoPlano`.
        val e = erros(validarPerfil(nome = "", baseline = ""))

        assertNotNull("o nome tinha que ser acusado", e.nome)
        assertNotNull("a baseline tinha que ser acusada", e.baseline)
        assertTrue(e.algum)
    }

    @Test
    fun `sem erro nenhum o algum e falso`() {
        assertFalse(ErrosDoPerfil().algum)
    }

    // ---------------------------------------------------------------------
    // textoDaBaseline — o ida e volta entre o Double gravado e o campo
    // ---------------------------------------------------------------------

    @Test
    fun `baseline inteira nao ganha casa decimal`() {
        // `8` e não `8,0`, pela mesma razão de `formatarKm`: casa decimal que não carrega
        // informação só ocupa a largura que a `fontScale` 2,0 não tem.
        assertEquals("8", textoDaBaseline(8.0))
    }

    @Test
    fun `baseline com uma casa mantem a virgula`() {
        assertEquals("7,5", textoDaBaseline(7.5))
    }

    @Test
    fun `baseline com duas casas nao perde a segunda`() {
        // **O caso que `formatarKm` não serve.** Ele arredonda para uma casa, e o
        // onboarding aceita duas: pré-preencher `7,25` como `7,3` faria abrir os Ajustes
        // e salvar mexer na baseline de quem não pediu.
        assertEquals("7,25", textoDaBaseline(7.25))
    }

    @Test
    fun `o texto pre-preenchido sempre passa pela propria validacao`() {
        // O invariante que fecha o ida e volta. Um formato que a validação recusa
        // deixaria o botão morto num formulário que ninguém tocou — e o campo acusado
        // por um valor que o próprio app escreveu.
        listOf(3.0, 5.5, 7.25, 10.0, 21.1, 999.99).forEach { km ->
            val texto = textoDaBaseline(km)
            assertEquals("ida e volta de $km (texto: $texto)", km, distanciaEmKm(texto))
        }
    }

    // ---------------------------------------------------------------------
    // perfilMudou — não gravar o que não mudou
    // ---------------------------------------------------------------------

    private val gravado = Usuario(uid = "u1", nome = "Hiago", baselineKm = 7.5)

    @Test
    fun `nada mudou quando nome e baseline sao os mesmos`() {
        val novo = valido(validarPerfil(nome = "Hiago", baseline = "7,5"))

        assertFalse(perfilMudou(gravado, novo))
    }

    @Test
    fun `texto diferente com a mesma distancia nao e mudanca`() {
        // `7,50` e `7,5` são o mesmo número. Comparar o texto mandaria uma escrita ao
        // Firestore para gravar o valor que já está lá.
        val novo = valido(validarPerfil(nome = "Hiago", baseline = "7,50"))

        assertFalse(perfilMudou(gravado, novo))
    }

    @Test
    fun `espaco em volta do nome nao e mudanca`() {
        val novo = valido(validarPerfil(nome = " Hiago ", baseline = "7,5"))

        assertFalse(perfilMudou(gravado, novo))
    }

    @Test
    fun `trocar o nome e mudanca`() {
        assertTrue(perfilMudou(gravado, valido(validarPerfil("Gabriel", "7,5"))))
    }

    @Test
    fun `trocar a baseline e mudanca`() {
        assertTrue(perfilMudou(gravado, valido(validarPerfil("Hiago", "9"))))
    }

    // ---------------------------------------------------------------------
    // blocoDeOrigem — o que a seção de fonte canônica pode oferecer
    // ---------------------------------------------------------------------

    @Test
    fun `sem Health Connect no aparelho nao ha fonte a escolher`() {
        // docs/05 §4.4: indisponível é o modo manual, caminho previsto e não falha. A
        // seção existe, e diz isso — não some, ou a tela mentiria por omissão sobre por
        // que a opção da ficha não está lá.
        assertEquals(
            BlocoDeOrigem.Indisponivel,
            blocoDeOrigem(StatusDoHealthConnect.Indisponivel, permissaoConcedida = false),
        )
    }

    @Test
    fun `Health Connect desatualizado ganha bloco proprio`() {
        // `F1-T21`, decisão nº 71 revogada pelo humano em 20/09. Até então este caso caía
        // em `Indisponivel`, e a tela dizia a quem só precisava atualizar exatamente o
        // que diz a quem não tem Health Connect nenhum: nada a fazer. **Há conserto, e
        // ele é da pessoa** (docs/05 §4.4).
        assertEquals(
            BlocoDeOrigem.PrecisaAtualizar,
            blocoDeOrigem(StatusDoHealthConnect.PrecisaAtualizar, permissaoConcedida = true),
        )
    }

    @Test
    fun `desatualizado nao depende da permissao para ganhar o bloco`() {
        // A permissão não entra na conta: sem cliente que conecte, ter ou não
        // `READ_EXERCISE` dá no mesmo. O ramo é do status, e só dele.
        assertEquals(
            BlocoDeOrigem.PrecisaAtualizar,
            blocoDeOrigem(StatusDoHealthConnect.PrecisaAtualizar, permissaoConcedida = false),
        )
    }

    @Test
    fun `o cadastro continua colapsando os dois, e isso e deliberado`() {
        // **A metade que cabe errar, fixada em teste.** `passoDepoisDoPerfil` faz a
        // mesma leitura de três valores e **não muda**: lá existe fluxo a travar — o
        // passo 5 —, e mandar a pessoa à Play Store no meio do cadastro a tira dele sem
        // garantia de volta. A ressalva que o humano leu para revogar a nº 71 dizia
        // *"fora do cadastro"*, com todas as letras.
        //
        // Sem este teste, a próxima sessão que ler `blocoDeOrigem` alarga a mudança
        // por simetria e ninguém percebe.
        assertEquals(
            passoDepoisDoPerfil(StatusDoHealthConnect.Indisponivel, permissaoConcedida = false),
            passoDepoisDoPerfil(StatusDoHealthConnect.PrecisaAtualizar, permissaoConcedida = false),
        )
    }

    @Test
    fun `disponivel sem permissao pede a permissao antes da lista`() {
        // Listar origens sem `READ_EXERCISE` devolve lista vazia, e essa vazia é
        // indistinguível do aparelho que não tem treino gravado — o mesmo motivo pelo
        // qual a ordem do onboarding é rígida.
        assertEquals(
            BlocoDeOrigem.SemPermissao,
            blocoDeOrigem(StatusDoHealthConnect.Disponivel, permissaoConcedida = false),
        )
    }

    @Test
    fun `disponivel e concedida deixa escolher`() {
        assertEquals(
            BlocoDeOrigem.PodeEscolher,
            blocoDeOrigem(StatusDoHealthConnect.Disponivel, permissaoConcedida = true),
        )
    }

    @Test
    fun `os quatro blocos sao alcancaveis e nao ha um quinto`() {
        // Toda combinação de status e permissão cai num dos quatro, e os quatro são
        // atingidos por alguma delas. Um estado que ninguém alcança é tela morta; um
        // ramo novo sem alguém decidir o que desenhar nele quebra aqui.
        //
        // **Eram três até 20/09** (`F1-T21`). Esta trava é a que obrigou a decisão a ser
        // tomada de novo em vez de o ramo novo entrar calado.
        val vistos = StatusDoHealthConnect.entries.flatMap { status ->
            listOf(true, false).map { blocoDeOrigem(status, it) }
        }

        assertEquals(BlocoDeOrigem.entries.toSet(), vistos.toSet())
    }
}
