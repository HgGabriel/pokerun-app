package com.hggabriel.pokerun.ui.telas.entrarcomcodigo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ConviteRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import com.hggabriel.pokerun.dominio.regras.normalizarCodigo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/** A semana de entrada de quem começa junto com o plano, e o piso de RN-19. */
private const val PRIMEIRA_SEMANA = 1

/**
 * O motor da entrada por convite (`F1-T14`, docs/03 §3.8).
 *
 * ### O código resolve por leitura direta, e nunca por consulta
 *
 * RN-29: `invites/{codigo}` tem o código como **ID do documento**. Uma *query* por
 * `codigo_convite` em `plans` transformaria seis caracteres num alvo de força bruta, e a
 * rule fecha essa porta de qualquer jeito (`allow list: if false`).
 *
 * ### A troca do plano ativo é decisão explícita
 *
 * RN-13. Quem já tem plano ativo passa obrigatoriamente pelo diálogo de [entrar], que
 * nomeia o plano de antes e oferece as duas saídas: tornar o novo ativo, ou entrar
 * guardado. Quem **não** tem plano ativo não vê diálogo nenhum, porque não há troca — há
 * campo vazio a preencher, que é a mesma leitura que `F1-T11` faz ao criar um plano.
 *
 * ### `entrou_na_semana` precisa de duas escritas, e a ordem é imposta pela rule
 *
 * RN-19 manda contar a aderência a partir da semana em que a pessoa entrou, e esse número
 * sai da grade — que só um **membro** consegue ler (`allow read: if souMembro(planId)`).
 * Ninguém sabe em que semana está entrando antes de já ter entrado.
 *
 * Então [confirmarEntrada] grava a membresia com o piso [PRIMEIRA_SEMANA], lê a grade e
 * regrava o documento com a semana certa. A janela entre as duas é de milissegundos e a
 * segunda escrita é `update: if eu(uid)`, que não tem como ser negada. **O piso é o valor
 * conservador de propósito:** um `entrou_na_semana` baixo demais conta como falta as
 * semanas anteriores à entrada e aparece como uma aderência ruim na tela de quem entrou —
 * visível, e reclamável. Um valor alto demais some no silêncio de um denominador generoso.
 */
class EntrarComCodigoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val convites: ConviteRepositorio,
    private val relogio: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _estado = MutableStateFlow(EntrarComCodigoUiState())
    val estado: StateFlow<EntrarComCodigoUiState> = _estado.asStateFlow()

    /** O plano encontrado, guardado inteiro: a prévia é só o que a tela desenha. */
    private var encontrado: Plano? = null

    // -----------------------------------------------------------------------
    // O campo
    // -----------------------------------------------------------------------

    /**
     * // RN-29
     *
     * A normalização acontece **no estado, e não na tela**: o campo mostra o que ficou
     * gravado aqui, então digitar `l` num teclado desatento não deixa rastro nenhum.
     *
     * Mexer no código desfaz o resultado anterior. Sem isso, a prévia de um plano
     * continuaria na tela enquanto a pessoa digita o código de outro, e o botão de
     * entrar apontaria para o plano errado.
     */
    fun codigoMudou(texto: String) {
        val codigo = normalizarCodigo(texto)
        if (codigo == _estado.value.codigo) return

        encontrado = null
        _estado.update {
            it.copy(codigo = codigo, resultado = Resultado.Idle, erro = null)
        }
    }

    // -----------------------------------------------------------------------
    // A busca (docs/03 §3.8)
    // -----------------------------------------------------------------------

    fun buscar() {
        if (!_estado.value.podeBuscar) return

        val codigo = _estado.value.codigo
        val uid = autenticacao.uidAtual
        if (uid == null) {
            _estado.update { it.copy(erro = R.string.entrar_erro_buscar) }
            return
        }

        encontrado = null
        _estado.update { it.copy(resultado = Resultado.Buscando, erro = null) }

        viewModelScope.launch {
            try {
                resolver(codigo, uid)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // Volta ao `Idle` e não a `NaoEncontrado`: o código pode estar certo, e
                // acusá-lo faria a pessoa conferir letra por letra um erro de rede.
                _estado.update {
                    it.copy(resultado = Resultado.Idle, erro = R.string.entrar_erro_buscar)
                }
            }
        }
    }

    private suspend fun resolver(codigo: String, uid: String) {
        // RN-29: leitura direta pelo ID do documento. Convite órfão — que aponta para um
        // plano que não existe — cai no mesmo estado, e é o resíduo previsto de uma
        // criação de plano que falhou depois de reservar o código.
        val planoId = convites.planoDoCodigo(codigo)
        val plano = planoId?.let { planos.buscar(it) }

        if (plano == null) {
            _estado.update { it.copy(resultado = Resultado.NaoEncontrado) }
            return
        }

        if (planos.souMembro(plano.id, uid)) {
            _estado.update { it.copy(resultado = Resultado.JaMembro(plano.nome)) }
            return
        }

        val perfil = usuarios.buscar(uid)
        // O nome do plano ativo é só a frase do diálogo, e a leitura pode falhar sem
        // custo: `previaDaEntrada` exige a escolha pelo ID, e não pelo nome (RN-13).
        val nomeDoAtivo = perfil?.planoAtivoId?.let {
            runCatching { planos.buscar(it)?.nome }.getOrNull()
        }

        encontrado = plano
        _estado.update {
            it.copy(
                resultado = Resultado.Encontrado(
                    previaDaEntrada(
                        plano = plano,
                        planoAtivoId = perfil?.planoAtivoId,
                        nomeDoPlanoAtivo = nomeDoAtivo,
                        agora = Instant.now(relogio),
                    ),
                ),
            )
        }
    }

    // -----------------------------------------------------------------------
    // RN-13 — a entrada, e a escolha do plano ativo
    // -----------------------------------------------------------------------

    /**
     * // RN-13
     *
     * O toque em `Entrar`. **Não grava nada quando há plano ativo**: abre a escolha, que
     * é o único caminho até a troca do primário.
     */
    fun entrar() {
        val previa = _estado.value.previa ?: return
        if (!previa.podeEntrar) return

        if (previa.exigeEscolha) {
            _estado.update { it.copy(escolhendoAtivo = previa, erro = null) }
        } else {
            // Campo vazio a preencher, e não troca: o primeiro plano de alguém vira o
            // ativo, como na criação (`F1-T11`).
            confirmarEntrada(tornarAtivo = true)
        }
    }

    fun cancelarEscolha() = _estado.update { it.copy(escolhendoAtivo = null) }

    /**
     * // RN-13
     *
     * A única escrita da tela. [tornarAtivo] vem do botão que a pessoa tocou no diálogo,
     * ou de não haver plano ativo nenhum — nunca de uma decisão daqui.
     */
    fun confirmarEntrada(tornarAtivo: Boolean) {
        val plano = encontrado ?: return
        val uid = autenticacao.uidAtual
        if (uid == null) {
            _estado.update { it.copy(escolhendoAtivo = null, erro = R.string.entrar_erro_entrar) }
            return
        }

        _estado.update { it.copy(escolhendoAtivo = null, entrando = true, erro = null) }

        viewModelScope.launch {
            try {
                val perfil = usuarios.buscar(uid) ?: error("users/$uid não existe")
                val agora = Instant.now(relogio)

                val membro = Membro(
                    uid = uid,
                    // Denormalizado em `members/{uid}` para o leaderboard não precisar de
                    // uma leitura em `users/` por pessoa (docs/05 §1).
                    nome = perfil.nome,
                    entrouEm = agora,
                    entrouNaSemana = PRIMEIRA_SEMANA,
                )

                planos.entrar(plano.id, membro)
                resolverSemanaDeEntrada(plano, membro, agora)

                // Sem isto a membresia existe e a lista de planos não a encontra: `plans`
                // não aceita consulta (RN-17), e o vínculo em `users/{uid}.planos` é o
                // único caminho até ela (D-04).
                usuarios.acrescentarPlano(uid, plano.id)

                if (tornarAtivo) usuarios.definirPlanoAtivo(uid, plano.id)

                _estado.update { it.copy(entrando = false, entrou = true) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.update {
                    it.copy(entrando = false, erro = R.string.entrar_erro_entrar)
                }
            }
        }
    }

    /**
     * // RN-19
     *
     * A segunda escrita do documento de membro, e a razão dela está no KDoc da classe: a
     * grade só é legível depois de a membresia existir.
     *
     * **Falhar aqui não desfaz a entrada.** A pessoa já é membro do plano, e derrubar a
     * tela agora a mandaria tentar de novo um convite em que ela já entrou. O que sobra é
     * um `entrou_na_semana` no piso, corrigível por qualquer escrita posterior no mesmo
     * documento.
     */
    private suspend fun resolverSemanaDeEntrada(plano: Plano, membro: Membro, agora: Instant) {
        try {
            val grade = planos.buscarSemanas(plano.id)
            val semana = CalendarioDoPlano.semanaRef(agora, plano, grade) ?: return
            if (semana == membro.entrouNaSemana) return

            planos.entrar(plano.id, membro.copy(entrouNaSemana = semana))
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (erro: Exception) {
            // Ver o KDoc: a entrada já aconteceu, e ela vale mais que o ajuste.
        }
    }
}
