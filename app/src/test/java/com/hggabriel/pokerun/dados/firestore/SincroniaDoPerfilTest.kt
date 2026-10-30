package com.hggabriel.pokerun.dados.firestore

import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * O lote do Perfil e o documento de membro (estudo de Progresso e Perfil, §b e F0).
 *
 * O defeito que estes testes guardam: o nome novo ficava só em `users/{uid}`, e toda tela de
 * grupo lê a cópia de `members/{uid}`. O avatar nascia ausente e o grupo mostrava o Caterpie.
 */
class SincroniaDoPerfilTest {

    private val uid = "KFocQH"

    @Test
    fun `o lote leva users e um members por plano`() {
        val lote = loteDoPerfil(uid, listOf("oMlOX", "SnBEZ"), MudancaDoPerfil(nome = "Hiago", baselineKm = 10.0))

        assertEquals(
            listOf(
                listOf("users", uid),
                listOf("plans", "oMlOX", "members", uid),
                listOf("plans", "SnBEZ", "members", uid),
            ),
            lote.map { it.caminho },
        )
        assertEquals(mapOf("nome" to "Hiago", "baseline_km" to 10.0), lote[0].campos)
        assertEquals(mapOf("nome" to "Hiago"), lote[1].campos)
        assertEquals(mapOf("nome" to "Hiago"), lote[2].campos)
    }

    @Test
    fun `a baseline sozinha nao toca membro nenhum`() {
        val lote = loteDoPerfil(uid, listOf("oMlOX"), MudancaDoPerfil(baselineKm = 12.0))

        assertEquals(listOf(listOf("users", uid)), lote.map { it.caminho })
    }

    @Test
    fun `parceiro e treinador vao para os membros junto do nome`() {
        val lote = loteDoPerfil(uid, listOf("oMlOX"), MudancaDoPerfil(parceiro = 29, treinador = "red"))

        assertEquals(mapOf("parceiro" to 29, "treinador" to "red"), lote[0].campos)
        assertEquals(mapOf("parceiro" to 29, "treinador" to "red"), lote[1].campos)
    }

    @Test
    fun `voltar ao parceiro automatico apaga o campo no usuario e nos membros`() {
        val lote = loteDoPerfil(uid, listOf("oMlOX"), MudancaDoPerfil(parceiroAutomatico = true))

        assertEquals(mapOf("parceiro" to ApagarCampo), lote[0].campos)
        assertEquals(mapOf("parceiro" to ApagarCampo), lote[1].campos)
    }

    @Test
    fun `plano repetido em users planos grava o membro uma vez`() {
        val lote = loteDoPerfil(uid, listOf("oMlOX", "oMlOX"), MudancaDoPerfil(nome = "Hiago"))

        assertEquals(2, lote.size)
    }

    @Test
    fun `sem plano o lote e so o usuario`() {
        val lote = loteDoPerfil(uid, emptyList(), MudancaDoPerfil(nome = "Hiago"))

        assertEquals(listOf(listOf("users", uid)), lote.map { it.caminho })
    }

    @Test
    fun `mudanca vazia nao grava nada`() {
        assertTrue(loteDoPerfil(uid, listOf("oMlOX"), MudancaDoPerfil()).isEmpty())
    }

    @Test
    fun `o documento de membro nasce com o avatar`() {
        val documento = Membro(uid, "Hiago", Instant.EPOCH, 1, avatarEspecie = 25).paraDocumento()

        assertEquals(25, documento["avatar_especie"])
        assertEquals("Hiago", documento["nome"])
    }

    @Test
    fun `avatar desconhecido fica fora do documento para o merge nao apagar o do replay`() {
        val documento = Membro(uid, "Hiago", Instant.EPOCH, 1).paraDocumento()

        assertFalse(documento.containsKey("avatar_especie"))
    }

    @Test
    fun `a foto enviada ao grupo leva nome, parceiro, treinador e o avatar da colecao`() {
        val usuario = Usuario(uid, "Hiago", baselineKm = 10.0, parceiro = 25, treinador = "red")

        assertEquals(
            mapOf("nome" to "Hiago", "parceiro" to 25, "treinador" to "red", "avatar_especie" to 31),
            fotoDoMembro(usuario, avatar = 31),
        )
    }

    @Test
    fun `parceiro automatico apaga a escolha no membro, e o grupo cai no avatar`() {
        val foto = fotoDoMembro(Usuario(uid, "Hiago", baselineKm = 10.0), avatar = 31)

        assertTrue(foto.getValue("parceiro") === ApagarCampo)
        assertEquals(31, foto["avatar_especie"])
        assertFalse(foto.containsKey("treinador"))
    }

    @Test
    fun `sem avatar lido, a foto nao apaga o que o replay gravou`() {
        assertFalse(fotoDoMembro(Usuario(uid, "Hiago", baselineKm = 10.0), avatar = null).containsKey("avatar_especie"))
    }
}
