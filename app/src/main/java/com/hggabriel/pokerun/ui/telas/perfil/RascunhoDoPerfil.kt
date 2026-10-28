package com.hggabriel.pokerun.ui.telas.perfil

import com.hggabriel.pokerun.dados.firestore.MudancaDoPerfil
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dominio.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/**
 * O que o Perfil mudou e ainda não gravou: o nome, o parceiro e o treinador (27/10/2026).
 *
 * Até aqui cada toque gravava sozinho (decisão 7), e quem editava não tinha onde confirmar
 * nem como desistir. Agora o campo do nome e os dois seletores escrevem **aqui**, e só o
 * botão "Salvar alterações" leva o lote ao Firestore. Sair com mudança pendente pergunta
 * antes (o diálogo de [DialogoDeAlteracoesPendentes]).
 *
 * **Vive com o app, e não com uma tela**: o Perfil e os seletores são três destinos com três
 * `ViewModel`, e a casca precisa saber se há o que perder antes de trocar de aba.
 *
 * A baseline fica de fora: a folha dela já tem o próprio "confirmar".
 *
 * @param gravar a escrita do lote. No app, `UsuarioRepositorio.atualizarPerfil`.
 */
class RascunhoDoPerfil(private val gravar: (Usuario, MudancaDoPerfil) -> Unit) {

    constructor(usuarios: UsuarioRepositorio) : this({ usuario, mudanca ->
        usuarios.atualizarPerfil(usuario.uid, usuario.planos, mudanca)
    })

    private val _mudanca = MutableStateFlow(MudancaDoPerfil())

    /** As mudanças pendentes. Campo nulo é campo igual ao gravado. */
    val mudanca: StateFlow<MudancaDoPerfil> = _mudanca.asStateFlow()

    /** O usuário gravado da última leitura: é contra ele que "pendente" se mede. */
    private var gravado: Usuario? = null

    /** Nome que só difere por espaço nas pontas não conta: o salvar o apara. */
    val pendente: Boolean
        get() = _mudanca.value.let { it.parceiro != null || it.treinador != null || (it.nome != null && it.nome.trim() != gravado?.nome) }

    /**
     * O usuário como as telas devem mostrá-lo: o gravado com as mudanças pendentes por cima.
     * Mudança que o servidor já alcançou (depois de salvar, ou trocada noutro aparelho) sai do
     * rascunho aqui.
     *
     * @param comNome falso deixa o nome gravado no usuário. O Perfil lê o nome do rascunho na
     * montagem do estado, para cada letra digitada não refazer as leituras que dependem do
     * usuário (as insígnias, o parceiro).
     */
    fun sobre(fonte: Flow<Usuario?>, comNome: Boolean = true): Flow<Usuario?> = combine(fonte, _mudanca) { usuario, mudanca ->
        gravado = usuario
        if (usuario == null) return@combine null
        val limpa = mudanca.semOQueJaEsta(usuario)
        if (limpa != mudanca) _mudanca.compareAndSet(mudanca, limpa)
        usuario.copy(
            nome = limpa.nome?.takeIf { comNome } ?: usuario.nome,
            parceiro = limpa.parceiro ?: usuario.parceiro,
            treinador = limpa.treinador ?: usuario.treinador,
        )
    }.distinctUntilChanged()

    // O texto vai cru, com o espaço do fim: aparado aqui, o campo engoliria o espaço antes do sobrenome.
    fun definirNome(nome: String) = _mudanca.update { it.copy(nome = nome.takeIf { n -> n != gravado?.nome }) }

    fun definirParceiro(numero: Int) = _mudanca.update { it.copy(parceiro = numero.takeIf { n -> n != gravado?.parceiro }) }

    fun definirTreinador(nome: String) = _mudanca.update { it.copy(treinador = nome.takeIf { n -> n != gravado?.treinador }) }

    /**
     * Grava as mudanças num lote só (`UsuarioRepositorio.atualizarPerfil`, que copia nome,
     * parceiro e treinador em `members/` de todo plano) e esvazia o rascunho. Nome em branco
     * não é gravado: o campo volta ao nome que já estava.
     *
     * @return falso quando não havia o que gravar ou o usuário ainda não foi lido.
     */
    fun salvar(): Boolean {
        val usuario = gravado ?: return false
        val mudanca = _mudanca.value
        val nome = mudanca.nome?.trim()?.takeIf { it.isNotEmpty() && it != usuario.nome }
        val lote = mudanca.copy(nome = nome)
        _mudanca.value = MudancaDoPerfil()
        if (!lote.temAlteracao()) return false
        gravar(usuario, lote)
        return true
    }

    fun descartar() {
        _mudanca.value = MudancaDoPerfil()
    }
}

private fun MudancaDoPerfil.temAlteracao(): Boolean = nome != null || parceiro != null || treinador != null

private fun MudancaDoPerfil.semOQueJaEsta(usuario: Usuario) = copy(
    nome = nome?.takeIf { it != usuario.nome },
    parceiro = parceiro?.takeIf { it != usuario.parceiro },
    treinador = treinador?.takeIf { it != usuario.treinador },
)
