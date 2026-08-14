package com.hggabriel.pokerun.ui.telas.revisarrascunho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ConviteRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.rede.ConectividadeRepositorio
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import com.hggabriel.pokerun.dominio.regras.alertaDeVolume
import com.hggabriel.pokerun.dominio.regras.editarLongao
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * O motor da revisão do rascunho (`F1-T11`, docs/03 §3.6).
 *
 * ### É aqui que o primeiro plano do app nasce
 *
 * `F1-T10` valida quatro campos e navega; quem chama `PlanoRepositorio.criar` é este
 * `ViewModel`. Até ele existir, **nenhum plano tinha sido gravado** — e é disso que
 * dependiam as duas `🔄` de `F1-T05` (o `criar` de duas idas nunca exercitado contra a
 * rule) e de `F1-T09` (os estados `Ativo`, `NaoIniciado` e `Encerrado` da Home, vistos só
 * em andaime).
 *
 * ### A grade é regerada, não transportada
 *
 * A rota carrega os **parâmetros de entrada** (`Rotas.kt`), e a grade sai de
 * [GeradorDePlano] aqui dentro. Ele é função pura: os mesmos cinco valores mais o fuso
 * devolvem as mesmas 21 semanas, então serializar a lista inteira num argumento de rota
 * seria carregar o resultado quando a entrada cabe em cinco campos — e a tela renasce
 * idêntica depois de morte de processo de graça.
 *
 * ### Quatro escritas, e a ordem importa
 *
 * `reservarCodigo` (o convite), `criar` (o plano, a grade e o dono), `acrescentarPlano`
 * (o vínculo em `users/{uid}`) e, **só quando não há plano ativo**, `definirPlanoAtivo`.
 *
 * **A reserva vem primeiro, e é `F1-T14` que a trouxe** (RN-29). O ID do plano sai de
 * `novoId` sem gravar nada, então dá para reservar `invites/{codigo}` antes de o plano
 * existir — e o `codigo_convite` que vai para `plans/{id}` é sempre um código único. A
 * ordem inversa deixaria uma janela em que dois planos carregam o mesmo código.
 *
 * A última é condicional por RN-13: trocar o plano ativo é decisão explícita com
 * confirmação, e nunca efeito colateral de criar outro plano. Quem já tem um ativo cria o
 * novo como dormente, e a troca é da `PlansListScreen` (`F1-T12`).
 */
class RevisarRascunhoViewModel(
    private val rascunho: RevisarRascunho,
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val convites: ConviteRepositorio,
    private val conectividade: ConectividadeRepositorio,
    private val relogio: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    /**
     * O fuso **do plano**, escolhido em `F1-T10` e transportado pela rota (RN-28). Nunca
     * `systemDefault()` daqui para a frente: é este valor que mantém a corrida de domingo
     * às 22h em domingo depois de o corredor viajar.
     */
    private val fuso: ZoneId = ZoneId.of(rascunho.fuso)

    private val parametros = ParametrosDeGeracao(
        // A prova é uma data no calendário do plano, e vira instante no fuso dele.
        dataProva = LocalDate.ofEpochDay(rascunho.dataProvaEpochDia)
            .atStartOfDay(fuso)
            .toInstant(),
        distanciaAlvoKm = rascunho.distanciaAlvoKm,
        baselineKm = rascunho.baselineKm,
        sessoesPorSemana = rascunho.sessoesPorSemana,
    )

    private val _estado = MutableStateFlow(gerarGrade())
    val estado: StateFlow<RevisarRascunhoUiState> = _estado.asStateFlow()

    /** Lido uma vez para o nome do membro e para saber se já há plano ativo (RN-13). */
    private var perfil: Usuario? = null

    init {
        lerPerfil()
        acompanharConexao()
    }

    // -----------------------------------------------------------------------
    // A grade
    // -----------------------------------------------------------------------

    /**
     * **`require` do gerador virando estado, e não pilha.** A tela só é alcançável pelo
     * formulário validado, então a geração não deveria falhar — mas o mínimo de 8 semanas
     * é contado da segunda-feira da semana **corrente**, e uma tela deixada aberta
     * atravessando a virada de semana passa a contar de uma segunda-feira depois. O
     * usuário veria o app fechar; aqui ele vê uma frase e volta.
     */
    private fun gerarGrade(): RevisarRascunhoUiState {
        val grade = runCatching {
            GeradorDePlano.gerar(parametros, inicio = Instant.now(relogio), fuso = fuso)
        }

        return RevisarRascunhoUiState(
            nomeDoPlano = rascunho.nome,
            grade = grade.getOrDefault(emptyList()),
            alerta = grade.getOrNull()?.let(::alertaDeVolume),
            erro = R.string.revisar_erro_gerar.takeIf { grade.isFailure },
        )
    }

    // -----------------------------------------------------------------------
    // A edição do longão (docs/01 §3.3)
    // -----------------------------------------------------------------------

    fun abrirEdicao(semana: Semana) {
        val longao = semana.longaoKm ?: return
        _estado.update {
            it.copy(editando = EdicaoDoLongao(numero = semana.numero, texto = formatarKm(longao)))
        }
    }

    fun longaoMudou(texto: String) = _estado.update { estado ->
        // Digitar limpa o erro: o usuário está consertando.
        estado.copy(editando = estado.editando?.copy(texto = texto, erro = null))
    }

    fun cancelarEdicao() = _estado.update { it.copy(editando = null) }

    /**
     * Aplica o longão digitado e **recalcula o alerta sobre a grade nova** (RN-30).
     *
     * A validação é a mesma `distanciaEmKm` das outras três distâncias do app, e por
     * isso mesmo: uma segunda leitura do que é um número aqui deixaria esta tela aceitar
     * `1e3` num plano em que o formulário recusa.
     */
    fun confirmarEdicao() {
        val edicao = _estado.value.editando ?: return
        val km = distanciaEmKm(edicao.texto)

        if (km == null) {
            _estado.update {
                it.copy(editando = edicao.copy(erro = R.string.criar_erro_distancia))
            }
            return
        }

        val grade = editarLongao(_estado.value.grade, edicao.numero, km)
        _estado.update { it.copy(grade = grade, alerta = alertaDeVolume(grade), editando = null) }
    }

    // -----------------------------------------------------------------------
    // A criação
    // -----------------------------------------------------------------------

    fun criarPlano() {
        if (!_estado.value.podeCriar) return

        val uid = autenticacao.uidAtual
        if (uid == null) {
            _estado.update { it.copy(erro = R.string.revisar_erro_criar) }
            return
        }

        _estado.update { it.copy(salvando = true, erro = null) }

        viewModelScope.launch {
            try {
                // O nome do membro é denormalizado em `members/{uid}` (docs/05 §1), então
                // o perfil é obrigatório aqui. Se a leitura do `init` falhou, tenta de
                // novo em vez de gravar um plano com o dono sem nome.
                val dono = perfil ?: usuarios.buscar(uid) ?: error("users/$uid não existe")
                perfil = dono

                val planoId = planos.novoId()
                val agora = Instant.now(relogio)

                // RN-29: sortear não é reservar. O `create` transacional é o que torna o
                // código único, e é por isso que esta tela bloqueia sem rede — a
                // transação não resolve no cache (docs/05 §2.6).
                val codigo = convites.reservarCodigo(planoId)

                planos.criar(
                    plano = Plano(
                        id = planoId,
                        nome = rascunho.nome,
                        distanciaAlvoKm = rascunho.distanciaAlvoKm,
                        dataProva = parametros.dataProva,
                        fuso = fuso,
                        ownerUid = uid,
                        codigoConvite = codigo,
                        encerrado = false,
                        parametros = parametros,
                    ),
                    grade = _estado.value.grade,
                    dono = Membro(
                        uid = uid,
                        nome = dono.nome,
                        entrouEm = agora,
                        // O criador está no plano desde a primeira semana, e é daqui que
                        // sai o denominador da aderência dele (RN-19).
                        entrouNaSemana = 1,
                        ativo = true,
                    ),
                )

                // Sem isto o plano existe e ninguém o encontra: `plans` não aceita
                // consulta (RN-17), então o vínculo em `users/{uid}.planos` é o único
                // caminho da `PlansListScreen` até ele (D-04).
                usuarios.acrescentarPlano(uid, planoId)

                // RN-13: só preenche o campo vazio. Nunca troca um ativo por outro.
                if (dono.planoAtivoId == null) {
                    usuarios.definirPlanoAtivo(uid, planoId)
                }

                _estado.update { it.copy(salvando = false, criado = true) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // A janela entre as duas idas de `criar` é real: uma queda de rede no
                // meio deixa um plano sem semanas. Tentar de novo cria outro plano, e
                // limpar o primeiro exigiria `delete` — que a rule de `plans` proíbe.
                // Fica para o humano pela console; o caso é raro e o app bloqueia offline.
                // O convite já reservado vira órfão pelo mesmo motivo, e a
                // `JoinPlanScreen` o trata como código não encontrado.
                _estado.update { it.copy(salvando = false, erro = R.string.revisar_erro_criar) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Costura
    // -----------------------------------------------------------------------

    private fun lerPerfil() {
        val uid = autenticacao.uidAtual ?: return
        viewModelScope.launch {
            val lido = try {
                usuarios.buscar(uid)
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                // Falhar aqui não custa a criação: ela lê de novo, e é lá que a falta de
                // perfil vira mensagem.
                null
            } ?: return@launch

            perfil = lido
            _estado.update { it.copy(jaTemPlanoAtivo = lido.planoAtivoId != null) }
        }
    }

    /** Igual à da `CreatePlanScreen`: a tela destrava sozinha quando a rede volta. */
    private fun acompanharConexao() {
        viewModelScope.launch {
            try {
                conectividade.online().collect { online ->
                    _estado.update { it.copy(online = online) }
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _estado.update { it.copy(online = true) }
            }
        }
    }
}
