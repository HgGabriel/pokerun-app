package com.hggabriel.pokerun.ui.telas.perfil

import com.hggabriel.pokerun.dados.firestore.MudancaDoPerfil
import com.hggabriel.pokerun.dominio.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O rascunho do Perfil: nada vai ao Firestore antes do "Salvar alterações" (27/10/2026). */
class RascunhoDoPerfilTest {

    private val gravado = Usuario(uid = "u", nome = "Phantom", planos = listOf("p"), baselineKm = 5.0, parceiro = 4, treinador = "hilbert")
    private val fonte = MutableStateFlow<Usuario?>(gravado)
    private val lotes = mutableListOf<MudancaDoPerfil>()
    private val rascunho = RascunhoDoPerfil { _, mudanca -> lotes += mudanca }

    private fun visto(): Usuario = runBlocking { rascunho.sobre(fonte).first()!! }

    @Test
    fun `escolher nos seletores muda o que a tela mostra sem gravar`() {
        visto()
        rascunho.definirParceiro(7)
        rascunho.definirTreinador("cynthia")

        assertEquals(7, visto().parceiro)
        assertEquals("cynthia", visto().treinador)
        assertTrue(rascunho.pendente)
        assertTrue(lotes.isEmpty())
    }

    @Test
    fun `salvar grava um lote so com o que mudou e esvazia o rascunho`() {
        visto()
        rascunho.definirNome("Phantom Silva ")
        rascunho.definirParceiro(7)

        assertTrue(rascunho.salvar())

        assertEquals(listOf(MudancaDoPerfil(nome = "Phantom Silva", parceiro = 7)), lotes)
        assertFalse(rascunho.pendente)
    }

    @Test
    fun `voltar ao valor gravado nao deixa alteracao pendente`() {
        visto()
        rascunho.definirParceiro(7)
        rascunho.definirParceiro(4)

        assertFalse(rascunho.pendente)
    }

    @Test
    fun `voltar ao automatico mostra o usuario sem parceiro e grava o apagar`() {
        visto()
        rascunho.voltarAoParceiroAutomatico()

        assertNull(visto().parceiro)
        assertTrue(rascunho.pendente)
        assertTrue(rascunho.salvar())
        assertEquals(listOf(MudancaDoPerfil(parceiroAutomatico = true)), lotes)
    }

    @Test
    fun `escolher depois do automatico desfaz o automatico`() {
        visto()
        rascunho.voltarAoParceiroAutomatico()
        rascunho.definirParceiro(7)

        assertEquals(MudancaDoPerfil(parceiro = 7), rascunho.mudanca.value)
    }

    @Test
    fun `automatico sem parceiro gravado nao e mudanca`() {
        fonte.value = gravado.copy(parceiro = null)
        visto()
        rascunho.voltarAoParceiroAutomatico()

        assertFalse(rascunho.pendente)
    }

    @Test
    fun `o espaco digitado no fim do nome fica no campo`() {
        // Aparado na digitação, o espaço antes do sobrenome sumiria a cada letra.
        visto()
        rascunho.definirNome("Phantom ")

        assertEquals("Phantom ", rascunho.mudanca.value.nome)
        assertFalse("só espaço nas pontas não é mudança", rascunho.pendente)
    }

    @Test
    fun `nome em branco nao e gravado`() {
        visto()
        rascunho.definirNome("   ")
        rascunho.definirTreinador("cynthia")

        rascunho.salvar()

        assertNull(lotes.single().nome)
        assertEquals("cynthia", lotes.single().treinador)
    }

    @Test
    fun `descartar volta ao gravado sem gravar`() {
        visto()
        rascunho.definirTreinador("cynthia")

        rascunho.descartar()

        assertEquals("hilbert", visto().treinador)
        assertFalse(rascunho.salvar())
        assertTrue(lotes.isEmpty())
    }

    @Test
    fun `sem nome no fluxo, o Perfil le o nome gravado e o rascunho guarda o digitado`() {
        visto()
        rascunho.definirNome("Julia")

        assertEquals("Phantom", runBlocking { rascunho.sobre(fonte, comNome = false).first()!!.nome })
        assertEquals("Julia", rascunho.mudanca.value.nome)
    }
}
