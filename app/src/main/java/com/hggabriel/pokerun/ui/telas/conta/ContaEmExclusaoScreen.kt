package com.hggabriel.pokerun.ui.telas.conta

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ExclusaoDaContaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.firestore.diasAteExcluir
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

data class ContaEmExclusaoUiState(
    val nome: String? = null,
    /** Nulo enquanto o usuário não foi lido. */
    val excluirEm: Instant? = null,
    val agora: Instant,
    val reativando: Boolean = false,
    /** O diálogo de "Excluir para sempre?", antes de apagar. */
    val confirmandoExclusao: Boolean = false,
    val excluindo: Boolean = false,
    @param:StringRes val erro: Int? = null,
    /** Terminais: a conta voltou (abre a casca), saiu de vez (mostra e volta ao login) ou a sessão saiu. */
    val reativada: Boolean = false,
    val excluida: Boolean = false,
    val saiu: Boolean = false,
) {
    val ocupada: Boolean get() = reativando || excluindo
}

/**
 * A conta com `excluir_em` no prazo, e as duas saídas: recuperar (tira a marca de tudo e encerra a
 * contagem) ou excluir permanentemente (apaga tudo agora e o login do Firebase). O voltar do
 * sistema só encerra a sessão: a conta continua em exclusão.
 */
class ContaEmExclusaoViewModel(
    private val autenticacao: AutenticacaoRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val exclusao: ExclusaoDaContaRepositorio,
    private val relogio: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _estado = MutableStateFlow(ContaEmExclusaoUiState(agora = relogio.instant()))
    val estado: StateFlow<ContaEmExclusaoUiState> = _estado.asStateFlow()

    private var usuario: Usuario? = null

    init {
        viewModelScope.launch {
            val uid = autenticacao.uidAtual ?: return@launch
            try {
                usuario = usuarios.buscar(uid)
                _estado.update { it.copy(nome = usuario?.nome, excluirEm = usuario?.excluirEm) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.update { it.copy(erro = R.string.conta_reativar_erro) }
            }
        }
    }

    fun reativar() {
        val uid = autenticacao.uidAtual ?: return
        if (_estado.value.ocupada) return
        _estado.update { it.copy(reativando = true, erro = null) }
        viewModelScope.launch {
            try {
                exclusao.reativar(uid, (usuario ?: usuarios.buscar(uid))?.planos.orEmpty())
                _estado.update { it.copy(reativando = false, reativada = true) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.update { it.copy(reativando = false, erro = R.string.conta_reativar_erro) }
            }
        }
    }

    fun pedirExclusao() = _estado.update { if (it.ocupada) it else it.copy(confirmandoExclusao = true) }

    fun cancelarExclusao() = _estado.update { it.copy(confirmandoExclusao = false) }

    fun excluirDeVez() {
        val uid = autenticacao.uidAtual ?: return
        if (_estado.value.ocupada) return
        _estado.update { it.copy(confirmandoExclusao = false, excluindo = true, erro = null) }
        viewModelScope.launch {
            try {
                exclusao.excluirDeVez(uid, (usuario ?: usuarios.buscar(uid))?.planos.orEmpty())
                autenticacao.excluirLogin()
                _estado.update { it.copy(excluindo = false, excluida = true) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (_: Exception) {
                _estado.update { it.copy(excluindo = false, erro = R.string.conta_excluir_de_vez_erro) }
            }
        }
    }

    fun sair() {
        if (_estado.value.excluida) {
            _estado.update { it.copy(saiu = true) }
            return
        }
        autenticacao.sair()
        _estado.update { it.copy(saiu = true) }
    }
}

/**
 * A conta em exclusão (29/10/2026): quem pediu a exclusão e entrou de novo dentro dos 30 dias
 * cai aqui, e não na Home. Os dias que faltam em número grande, a data, e as duas saídas:
 * recuperar a conta (a ação da tela) e excluir permanentemente, com um diálogo antes. É porta,
 * como o login: nada fica embaixo dela, e o voltar do sistema encerra a sessão.
 */
@Composable
fun ContaEmExclusaoScreen(
    aoReativar: () -> Unit,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ContaEmExclusaoViewModel = contaEmExclusaoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.reativada, estado.saiu) {
        if (estado.reativada) aoReativar()
        if (estado.saiu) aoSair()
    }
    BackHandler(enabled = !estado.ocupada) { vm.sair() }

    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.conta_em_exclusao_sobrancelha)),
                    titulo = if (estado.excluida) stringResource(R.string.conta_excluida_titulo) else estado.nome.orEmpty(),
                    aoVoltar = null,
                )
                Column(
                    modifier = Modifier.padding(horizontal = Espacos.margem, vertical = Espacos.interno),
                    verticalArrangement = Arrangement.spacedBy(Espacos.interno),
                ) {
                    val erro = estado.erro
                    if (erro != null) {
                        BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(erro))
                    }
                    val excluirEm = estado.excluirEm
                    when {
                        estado.excluida -> Cartao {
                            Text(
                                text = stringResource(R.string.conta_excluida),
                                style = Estilos.corpo,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        excluirEm == null -> CartoesEmEspera(listOf(200.dp))
                        else -> CartaoDosDias(excluirEm, estado.agora)
                    }
                }
            }

            if (estado.excluida) {
                BotaoPrimario(
                    aoTocar = vm::sair,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
                ) {
                    Text(stringResource(R.string.conta_voltar_ao_inicio))
                }
            } else {
                BotaoPrimario(
                    aoTocar = vm::reativar,
                    habilitado = estado.excluirEm != null && !estado.excluindo,
                    ocupado = estado.reativando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.relacionado),
                ) {
                    Text(stringResource(R.string.conta_reativar))
                }
                BotaoDeTexto(
                    aoTocar = vm::pedirExclusao,
                    habilitado = estado.excluirEm != null && !estado.ocupada,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacos.margem, vertical = Espacos.minimo),
                ) {
                    Text(stringResource(R.string.conta_excluir_de_vez))
                }
            }
        }
    }

    if (estado.confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = vm::cancelarExclusao,
            title = { Text(stringResource(R.string.conta_excluir_de_vez_titulo)) },
            text = { Text(stringResource(R.string.conta_excluir_de_vez_texto)) },
            confirmButton = { BotaoDeTexto(aoTocar = vm::excluirDeVez) { Text(stringResource(R.string.conta_final_confirmar)) } },
            dismissButton = { BotaoDeTexto(aoTocar = vm::cancelarExclusao) { Text(stringResource(R.string.conta_final_voltar)) } },
        )
    }
}

/** Os dias que faltam em `numero-xl` e a data em que a conta sai de vez. */
@Composable
private fun CartaoDosDias(excluirEm: Instant, agora: Instant) {
    Cartao {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = diasAteExcluir(excluirEm, agora).toString(),
                style = Estilos.numeroXl,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(Espacos.relacionado))
            Text(
                text = stringResource(R.string.conta_dias).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        RotuloDoCartao(stringResource(R.string.conta_em_exclusao_faltam))
        Spacer(Modifier.height(Espacos.interno))
        Text(
            text = stringResource(R.string.conta_em_exclusao_regra, dataPorExtenso(excluirEm)),
            style = Estilos.apoio,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun contaEmExclusaoViewModel(): ContaEmExclusaoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        ContaEmExclusaoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.exclusaoDaContaRepositorio,
        )
    }
}
