package com.hggabriel.pokerun.ui.telas.editarcorrida

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.regras.OpcaoDeSessao
import com.hggabriel.pokerun.dominio.regras.ResumoDaCorrecao
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import com.hggabriel.pokerun.ui.componentes.FormaDoCartao
import com.hggabriel.pokerun.ui.componentes.IconesClarosNaBarraDeStatus
import com.hggabriel.pokerun.ui.componentes.LocalVoltarDoCabecalho
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.SpriteAnimado
import com.hggabriel.pokerun.ui.componentes.esqueleto
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.telas.corridamanual.ArenaDaCorrida
import com.hggabriel.pokerun.ui.telas.corridamanual.CalendarioDoTreino
import com.hggabriel.pokerun.ui.telas.corridamanual.CartaoComoFoi
import com.hggabriel.pokerun.ui.telas.corridamanual.CartaoQuando
import com.hggabriel.pokerun.ui.telas.corridamanual.CartaoRecordes
import com.hggabriel.pokerun.ui.telas.corridamanual.CorridaManualUiState
import com.hggabriel.pokerun.ui.telas.corridamanual.RelogioDoTreino
import com.hggabriel.pokerun.ui.telas.corridamanual.especieDaArenaViewModel
import com.hggabriel.pokerun.ui.telas.corridamanual.pokebola
import com.hggabriel.pokerun.ui.telas.home.CardDaEspecie
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.LocalTime

private val EntreCartoes = 12.dp
private val FormaDaArenaEmEspera = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
private val AlturaDaArenaEmEspera = 460.dp
private val AlturaDaOpcao = 60.dp
private val LadoDoSelo = 28.dp
private val SpriteDeAntes = 72.dp
private val SpriteDeAgora = 112.dp
private val LadoDaPokebola = 72.dp
private const val OPACIDADE_DESABILITADA = 0.38f

/**
 * A `RunEditScreen` (`F2-T10`, docs/03 §3.14), o centro de agência do usuário: corrigir,
 * descartar e escolher a sessão que a corrida cumpre. Sem janela de tempo (RN-32).
 *
 * ### Na linguagem do registro (29/10/2026)
 *
 * Corrigir é registrar de novo, então a tela é a do registro: a arena em tinta com o placar,
 * já preenchido, e os cartões brancos na página tingida. O parceiro acorda com a distância
 * da corrida e o pace sai ao vivo a cada correção. Os cartões são os mesmos do registro, mais
 * o da sessão (RN-34) no topo. Descartar (RN-31) é a lixeira no canto da arena; a ação da tela
 * é uma só, salvar, no pé. O resumo do replay põe a escada numa peça em tinta: a espécie de
 * antes, apagada, e a de agora, com o XP novo e a diferença.
 *
 * Fica na pilha modal (docs/03 §1), fora da `CascaDeNavegacao`: a tela liga os ícones claros
 * da barra de status e dá à seta o voltar da pilha.
 */
@Composable
fun EditarCorridaScreen(
    corridaId: String,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
    aoAbrirMedalhas: (Ritmo) -> Unit = {},
    vm: EditarCorridaViewModel = editarCorridaViewModel(corridaId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val especie by especieDaArenaViewModel().card.collectAsStateWithLifecycle()

    EditarCorridaScreen(
        estado = estado,
        especie = especie,
        aoMudarFormulario = vm::mudarFormulario,
        aoEscolherSessao = vm::escolherSessao,
        aoSalvar = vm::salvar,
        aoPedirDescarte = vm::pedirDescarte,
        aoConfirmarDescarte = vm::confirmarDescarte,
        aoCancelarDescarte = vm::cancelarDescarte,
        aoTentarDeNovo = vm::tentarDeNovo,
        aoSair = aoSair,
        aoAbrirMedalhas = aoAbrirMedalhas,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun EditarCorridaScreen(
    estado: EditarCorridaUiState,
    aoMudarFormulario: ((CorridaManualUiState) -> CorridaManualUiState) -> Unit,
    aoEscolherSessao: (SessaoReivindicada) -> Unit,
    aoSalvar: () -> Unit,
    aoPedirDescarte: () -> Unit,
    aoConfirmarDescarte: () -> Unit,
    aoCancelarDescarte: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier,
    especie: CardDaEspecie? = null,
    aoAbrirMedalhas: (Ritmo) -> Unit = {},
) {
    // A arena, e a espera dela, põem tinta atrás da barra de status.
    IconesClarosNaBarraDeStatus(ativo = estado is EditarCorridaUiState.Editando || estado == EditarCorridaUiState.Carregando)

    CompositionLocalProvider(LocalVoltarDoCabecalho provides aoSair) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(LocalCoresPokerun.current.superficieAfundada),
        ) {
            when (estado) {
                EditarCorridaUiState.Carregando -> EmEspera()
                EditarCorridaUiState.Recalculando -> Recalculando()
                EditarCorridaUiState.Falhou -> Falhou(aoTentarDeNovo)
                is EditarCorridaUiState.Concluido -> Concluido(estado, aoSair)
                is EditarCorridaUiState.Editando -> Editando(
                    estado = estado,
                    especie = especie,
                    aoMudarFormulario = aoMudarFormulario,
                    aoEscolherSessao = aoEscolherSessao,
                    aoSalvar = aoSalvar,
                    aoPedirDescarte = aoPedirDescarte,
                    aoAbrirMedalhas = aoAbrirMedalhas,
                )
            }
            // A faixa em tinta fica presa atrás da barra de status, como a da casca no registro:
            // os cartões claros rolam por baixo dela, e os ícones claros continuam legíveis.
            if (estado is EditarCorridaUiState.Editando) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }

    val editando = estado as? EditarCorridaUiState.Editando ?: return
    val f = editando.formulario
    if (f.escolhendoData) {
        CalendarioDoTreino(
            escolhida = f.data,
            aoFechar = { aoMudarFormulario { it.copy(escolhendoData = false) } },
            aoEscolher = { data: LocalDate ->
                aoMudarFormulario { it.copy(data = data, escolhendoData = false, erros = it.erros.copy(dataHora = null)) }
            },
        )
    }
    if (f.escolhendoHora) {
        RelogioDoTreino(
            escolhida = f.hora,
            aoFechar = { aoMudarFormulario { it.copy(escolhendoHora = false) } },
            aoEscolher = { hora: LocalTime ->
                aoMudarFormulario { it.copy(hora = hora, escolhendoHora = false, erros = it.erros.copy(dataHora = null)) }
            },
        )
    }
    if (editando.confirmandoDescarte) {
        AlertDialog(
            onDismissRequest = aoCancelarDescarte,
            title = { Text(stringResource(R.string.editar_descartar_titulo)) },
            text = { Text(stringResource(R.string.editar_descartar_texto)) },
            confirmButton = { BotaoDeTexto(aoTocar = aoConfirmarDescarte) { Text(stringResource(R.string.editar_descartar_confirmar)) } },
            dismissButton = { BotaoDeTexto(aoTocar = aoCancelarDescarte) { Text(stringResource(R.string.editar_descartar_voltar)) } },
        )
    }
}

// ---------------------------------------------------------------------------
// Editando
// ---------------------------------------------------------------------------

@Composable
private fun Editando(
    estado: EditarCorridaUiState.Editando,
    especie: CardDaEspecie?,
    aoMudarFormulario: ((CorridaManualUiState) -> CorridaManualUiState) -> Unit,
    aoEscolherSessao: (SessaoReivindicada) -> Unit,
    aoSalvar: () -> Unit,
    aoPedirDescarte: () -> Unit,
    aoAbrirMedalhas: (Ritmo) -> Unit,
) {
    val f = estado.formulario

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ArenaDaCorrida(
                estado = f,
                especie = especie,
                aoMudarKm = { t -> aoMudarFormulario { it.copy(km = t, erros = it.erros.copy(km = null)) } },
                aoMudarHoras = { t -> aoMudarFormulario { it.copy(horas = t, erros = it.erros.copy(duracao = null)) } },
                aoMudarMinutos = { t -> aoMudarFormulario { it.copy(minutos = t, erros = it.erros.copy(duracao = null)) } },
                aoMudarSegundos = { t -> aoMudarFormulario { it.copy(segundos = t, erros = it.erros.copy(duracao = null)) } },
                sobrancelha = listOfNotNull(
                    stringResource(R.string.editar_sobrancelha),
                    estado.numeroDaSemana?.let { stringResource(R.string.manual_arena_semana, it) },
                ).joinToString(" · "),
                titulo = textoDoDia(estado.dia),
                acao = { BotaoDescartar(aoPedirDescarte) },
                atrasDaBarraDeStatus = true,
                aoAbrirMedalhas = aoAbrirMedalhas,
            )

            Column(
                modifier = Modifier.padding(horizontal = Espacos.margem, vertical = EntreCartoes),
                verticalArrangement = Arrangement.spacedBy(EntreCartoes),
            ) {
                if (estado.erroDeTela != null) {
                    BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erroDeTela))
                }
                // Os erros do placar saem aqui, no primeiro lugar claro abaixo da arena.
                listOfNotNull(f.erros.km, f.erros.duracao).forEach { erro ->
                    BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(erro))
                }

                if (estado.opcoes.isNotEmpty()) CartaoDaSessao(estado, aoEscolherSessao)
                CartaoQuando(
                    estado = f,
                    aoEscolherData = { data -> aoMudarFormulario { it.copy(data = data, erros = it.erros.copy(dataHora = null)) } },
                    aoAbrirCalendario = { aoMudarFormulario { it.copy(escolhendoData = true) } },
                    aoAbrirRelogio = { aoMudarFormulario { it.copy(escolhendoHora = true) } },
                )
                CartaoComoFoi(
                    estado = f,
                    aoMudarEsforco = { t -> aoMudarFormulario { it.copy(esforco = t, erros = it.erros.copy(esforco = null)) } },
                    aoMudarFrequencia = { t -> aoMudarFormulario { it.copy(fcMedia = t, erros = it.erros.copy(fcMedia = null)) } },
                )
                CartaoRecordes(
                    estado = f,
                    aoMudarTempo1kMinutos = { t -> aoMudarFormulario { it.copy(tempo1kMinutos = t, erros = it.erros.copy(tempo1k = null)) } },
                    aoMudarTempo1kSegundos = { t -> aoMudarFormulario { it.copy(tempo1kSegundos = t, erros = it.erros.copy(tempo1k = null)) } },
                    aoMudarTempo5kMinutos = { t -> aoMudarFormulario { it.copy(tempo5kMinutos = t, erros = it.erros.copy(tempo5k = null)) } },
                    aoMudarTempo5kSegundos = { t -> aoMudarFormulario { it.copy(tempo5kSegundos = t, erros = it.erros.copy(tempo5k = null)) } },
                )
                // A regra da correção (RN-24), dita uma vez, junto de quem salva.
                Text(
                    text = stringResource(R.string.editar_aviso),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacos.margem),
                )
            }
        }

        BotaoPrimario(
            aoTocar = aoSalvar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
        ) {
            Text(stringResource(R.string.editar_salvar))
        }
    }
}

/** Descartar (RN-31) como lixeira no canto da arena. A confirmação explica o que acontece. */
@Composable
private fun BotaoDescartar(aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.editar_descartar),
            tint = MaterialTheme.colorScheme.surface,
        )
    }
}

/**
 * O seletor de RN-34 (docs/03 §3.14) em escolhas cheias: cada sessão com o alvo e o
 * multiplicador; a marcada vira tinta. O slot escolhido pelo usuário para outra corrida
 * aparece apagado e não aceita toque.
 */
@Composable
private fun CartaoDaSessao(estado: EditarCorridaUiState.Editando, aoEscolher: (SessaoReivindicada) -> Unit) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.editar_sessao_rotulo))
        Spacer(Modifier.height(Espacos.interno))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            estado.opcoes.forEach { opcao ->
                OpcaoDaSessao(
                    opcao = opcao,
                    escolhida = opcao.sessao == estado.sessaoEscolhida,
                    aoEscolher = { aoEscolher(opcao.sessao) },
                )
            }
        }
    }
}

@Composable
private fun OpcaoDaSessao(opcao: OpcaoDeSessao, escolhida: Boolean, aoEscolher: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val fundo by animateColorAsState(
        if (escolhida) tinta else LocalCoresPokerun.current.superficieAfundada,
        animationSpec = Movimento.padrao(LocalMovimentoReduzido.current),
        label = "fundo da sessão",
    )
    val texto = if (escolhida) claro else tinta
    val apoio = if (escolhida) claro.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val alvo = opcao.alvoKm?.let { stringResource(R.string.editar_sessao_alvo, formatarKm(it, LocaleDoApp)) }
        ?: stringResource(R.string.editar_sessao_sem_alvo)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDaOpcao)
            .clip(FormaControle)
            .background(fundo)
            .selectable(selected = escolhida, enabled = opcao.habilitada, role = Role.RadioButton, onClick = aoEscolher)
            .alpha(if (opcao.habilitada) 1f else OPACIDADE_DESABILITADA)
            .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDaOpcao(conta = opcao.sessao != SessaoReivindicada.Avulsa, escolhida = escolhida)
        Spacer(Modifier.width(Espacos.interno))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nomeDaSessao(opcao.sessao), style = Estilos.corpoForte, color = texto)
            Text(text = alvo, style = Estilos.apoio, color = apoio)
        }
        Spacer(Modifier.width(Espacos.relacionado))
        Text(
            text = stringResource(R.string.editar_sessao_multiplicador, String.format(LocaleDoApp, "%.1f", opcao.multiplicador)),
            style = Estilos.numeroM,
            color = texto,
        )
    }
}

/**
 * O selo dos carimbos da semana, em miniatura: a sessão prevista é o selo cheio em `destaque`
 * com o anel por dentro; a avulsa não ocupa sessão e fica como vaga.
 */
@Composable
private fun SeloDaOpcao(conta: Boolean, escolhida: Boolean) {
    val claro = MaterialTheme.colorScheme.surface
    val vermelho = MaterialTheme.colorScheme.primary
    val vaga = if (escolhida) claro.copy(alpha = 0.16f) else claro
    Canvas(modifier = Modifier.size(LadoDoSelo)) {
        if (conta) {
            drawCircle(vermelho)
            drawCircle(claro.copy(alpha = 0.45f), radius = size.minDimension / 2 - 3.dp.toPx(), style = Stroke(1.dp.toPx()))
        } else {
            drawCircle(vaga)
        }
    }
}

@Composable
private fun nomeDaSessao(sessao: SessaoReivindicada): String = when (sessao) {
    SessaoReivindicada.Longao -> stringResource(R.string.corrida_detalhe_sessao_longao)
    SessaoReivindicada.Avulsa -> stringResource(R.string.corrida_detalhe_sessao_avulsa)
    is SessaoReivindicada.Curta -> stringResource(R.string.corrida_detalhe_sessao_curta, sessao.indice)
}

// ---------------------------------------------------------------------------
// Espera, recálculo e falha
// ---------------------------------------------------------------------------

/** A arena e os cartões em espera, na forma de cada um. */
@Composable
private fun EmEspera() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .esqueleto(FormaDaArenaEmEspera, MaterialTheme.colorScheme.onSurface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(AlturaDaArenaEmEspera),
        )
        CartoesEmEspera(
            alturas = listOf(220.dp, 150.dp, 200.dp),
            espaco = EntreCartoes,
            modifier = Modifier.padding(horizontal = Espacos.margem, vertical = EntreCartoes),
        )
    }
}

/**
 * O replay correndo: a Pokébola balança, como na captura, até a conta voltar. Com movimento
 * reduzido, ela fica parada.
 */
@Composable
private fun Recalculando() {
    val vermelho = MaterialTheme.colorScheme.primary
    val claro = MaterialTheme.colorScheme.surface
    val tinta = MaterialTheme.colorScheme.onSurface
    val reduzido = LocalMovimentoReduzido.current
    val giro = if (reduzido) {
        0f
    } else {
        val balanco by rememberInfiniteTransition(label = "pokébola").animateFloat(
            initialValue = -16f,
            targetValue = 16f,
            animationSpec = infiniteRepeatable(tween(Movimento.Tempo.LENTO, easing = LinearEasing), RepeatMode.Reverse),
            label = "balanço",
        )
        balanco
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Espacos.margem),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(LadoDaPokebola)) {
            val raio = size.minDimension / 2
            rotate(giro, pivot = Offset(center.x, size.height)) {
                pokebola(center, raio, vermelho, claro, tinta)
            }
        }
        Spacer(Modifier.height(Espacos.bloco))
        Text(
            text = stringResource(R.string.editar_recalculando),
            style = Estilos.apoio,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Falhou(aoTentarDeNovo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        CabecalhoDeFicha(sobrancelha = emptyList(), titulo = stringResource(R.string.editar_sobrancelha))
        Column(modifier = Modifier.padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.bloco)) {
            Text(
                text = stringResource(R.string.editar_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Espacos.bloco))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Concluído
// ---------------------------------------------------------------------------

/**
 * O resumo de docs/01 §5.3: a escada e o XP recalculados numa peça em tinta, o que mudou
 * num cartão e os recordes que mudaram em pílulas. Na Fase 2 não há a linha `Coleção` (D-19).
 */
@Composable
private fun Concluido(estado: EditarCorridaUiState.Concluido, aoSair: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // A saída é o botão do pé; a seta repetiria o mesmo gesto.
            CabecalhoDeFicha(
                sobrancelha = listOf(textoDoDia(estado.dia)),
                titulo = stringResource(if (estado.descartada) R.string.editar_titulo_descartada else R.string.editar_titulo_corrigida),
                aoVoltar = null,
            )
            Column(
                modifier = Modifier.padding(horizontal = Espacos.margem, vertical = EntreCartoes),
                verticalArrangement = Arrangement.spacedBy(EntreCartoes),
            ) {
                PecaDaEscada(estado)
                if (estado.mudancas.isNotEmpty()) CartaoDasMudancas(estado.mudancas)
                if (estado.resumo.recordesQueMudaram.isNotEmpty()) CartaoDosRecordes(estado.resumo.recordesQueMudaram)
            }
        }

        BotaoPrimario(
            aoTocar = aoSair,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
        ) {
            Text(stringResource(R.string.editar_voltar))
        }
    }
}

/**
 * A única tinta do resumo: a espécie de antes, apagada, e a de agora, de pé na luz; embaixo,
 * o XP novo e a diferença. Sem a temporada, a escada fica no número.
 */
@Composable
private fun PecaDaEscada(estado: EditarCorridaUiState.Concluido) {
    val resumo = estado.resumo
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val luz = MaterialTheme.colorScheme.primary
    val antes = posicaoEmTexto(resumo.posicaoAntes)
    val agora = posicaoEmTexto(resumo.posicaoDepois)
    val subiuOuDesceu = resumo.posicaoAntes != resumo.posicaoDepois
    val faladaDaEscada = stringResource(R.string.editar_resumo_especie, antes, agora)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaDoCartao)
            .background(tinta)
            .padding(Espacos.margem),
    ) {
        SobrancelhaDoBanner(stringResource(R.string.editar_resumo_recalculado))
        Spacer(Modifier.height(Espacos.interno))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = faladaDaEscada },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (subiuOuDesceu) {
                DegrauDaEscada(estado.especieAntes, antes, SpriteDeAntes, apagado = true)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = claro.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = Espacos.interno, end = Espacos.interno, bottom = 40.dp),
                )
            }
            Box(
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(luz.copy(alpha = 0.45f), Color.Transparent), center, size.maxDimension * 0.6f),
                        radius = size.maxDimension * 0.6f,
                    )
                },
            ) {
                DegrauDaEscada(estado.especieDepois, agora, SpriteDeAgora, apagado = false)
            }
        }

        Spacer(Modifier.height(Espacos.bloco))
        LinhaDoXp(resumo, claro)
    }
}

/** Uma espécie da escada com a posição embaixo. Sem o número da Pokédex, só a posição. */
@Composable
private fun DegrauDaEscada(numero: Int?, posicao: String, lado: Dp, apagado: Boolean) {
    val claro = MaterialTheme.colorScheme.surface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (numero != null) {
            SpriteAnimado(numero = numero, apagado = apagado, paraNoReduzido = true, modifier = Modifier.size(lado))
        }
        Text(
            text = posicao,
            style = if (numero != null) Estilos.numeroS else Estilos.numeroL,
            color = if (apagado) claro.copy(alpha = 0.5f) else claro,
        )
    }
}

/** O XP novo em número grande, a diferença numa pílula e o de antes embaixo. */
@Composable
private fun LinhaDoXp(resumo: ResumoDaCorrecao, claro: Color) {
    val locale = LocaleDoApp
    val diferenca = resumo.xpDepois - resumo.xpAntes
    val sinal = if (diferenca >= 0) "+" else "−"
    val textoDaDiferenca = sinal + String.format(locale, "%,d", kotlin.math.abs(diferenca))
    val falado = stringResource(R.string.editar_resumo_xp) + " " + stringResource(
        R.string.editar_resumo_xp_valor,
        String.format(locale, "%,d", resumo.xpAntes),
        String.format(locale, "%,d", resumo.xpDepois),
        textoDaDiferenca,
    )

    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = falado }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(text = String.format(locale, "%,d", resumo.xpDepois), style = Estilos.numeroL, color = claro, maxLines = 1)
                Spacer(Modifier.width(Espacos.minimo))
                Text(
                    text = stringResource(R.string.corrida_detalhe_xp_unidade),
                    style = Estilos.rotulo,
                    color = claro.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (diferenca != 0L) {
                Text(
                    text = textoDaDiferenca,
                    style = Estilos.numeroS,
                    color = claro,
                    maxLines = 1,
                    modifier = Modifier
                        .background(claro.copy(alpha = 0.14f), FormaCirculo)
                        .padding(horizontal = Espacos.interno, vertical = Espacos.minimo),
                )
            }
        }
        if (diferenca != 0L) {
            Text(
                text = stringResource(R.string.editar_resumo_antes, String.format(locale, "%,d", resumo.xpAntes)),
                style = Estilos.legenda,
                color = claro.copy(alpha = 0.6f),
            )
        }
    }
}

/** O que mudou na medição: o valor de antes riscado, o de agora em tinta. */
@Composable
private fun CartaoDasMudancas(mudancas: List<Mudanca>) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.editar_resumo_mudou))
        Spacer(Modifier.height(Espacos.interno))
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
            mudancas.forEach { mudanca ->
                val rotulo = stringResource(mudanca.rotulo)
                val falado = rotulo + ", " + stringResource(R.string.editar_resumo_seta, mudanca.antes, mudanca.depois)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = falado },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rotulo,
                        style = Estilos.apoio,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = mudanca.antes,
                        style = Estilos.numeroS.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = Espacos.relacionado)
                            .size(16.dp),
                    )
                    Text(text = mudanca.depois, style = Estilos.numeroS, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/** Os recordes que a correção mexeu, em pílulas cheias de tinta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDosRecordes(recordes: Set<TipoDeRecorde>) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.editar_resumo_recordes_rotulo))
        Spacer(Modifier.height(Espacos.interno))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            recordes.forEach { tipo ->
                Text(
                    text = stringResource(nomeDoRecorde(tipo)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSurface, FormaCirculo)
                        .padding(horizontal = Espacos.interno, vertical = Espacos.relacionado),
                )
            }
        }
    }
}

private fun posicaoEmTexto(posicao: Int): String = "#%03d".format(posicao)

private fun nomeDoRecorde(tipo: TipoDeRecorde): Int = when (tipo) {
    TipoDeRecorde.MELHOR_PACE -> R.string.recorde_melhor_pace
    TipoDeRecorde.MELHOR_1K -> R.string.recorde_melhor_1k
    TipoDeRecorde.MELHOR_5K -> R.string.recorde_melhor_5k
    TipoDeRecorde.MAIOR_DISTANCIA -> R.string.recorde_maior_distancia
    TipoDeRecorde.MAIOR_DURACAO -> R.string.recorde_maior_duracao
}

@Composable
private fun textoDoDia(dia: LocalDate): String =
    stringResource(R.string.corrida_detalhe_dia, dia.dayOfMonth, nomeDoMes(dia, LocaleDoApp), dia.year)

@Composable
private fun editarCorridaViewModel(corridaId: String): EditarCorridaViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "editar-$corridaId") {
        EditarCorridaViewModel(
            corridaId,
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.recalculoDeXp,
            temporadas = container.temporadaRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun EditarPreview(estado: EditarCorridaUiState) {
    PokerunTheme {
        EditarCorridaScreen(
            estado = estado,
            aoMudarFormulario = {},
            aoEscolherSessao = {},
            aoSalvar = {},
            aoPedirDescarte = {},
            aoConfirmarDescarte = {},
            aoCancelarDescarte = {},
            aoTentarDeNovo = {},
            aoSair = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun EditandoPreview() = EditarPreview(
    EditarCorridaUiState.Editando(
        formulario = CorridaManualUiState(
            data = LocalDate.of(2026, 9, 12),
            hora = LocalTime.of(6, 30),
            km = "8,4",
            minutos = "45",
            esforco = "6",
            fcMedia = "152",
        ),
        dia = LocalDate.of(2026, 9, 12),
        opcoes = listOf(
            OpcaoDeSessao(SessaoReivindicada.Avulsa, null, 1.0, habilitada = true, escolhida = false),
            OpcaoDeSessao(SessaoReivindicada.Longao, 12.4, 2.0, habilitada = true, escolhida = true),
            OpcaoDeSessao(SessaoReivindicada.Curta(1), 6.2, 2.0, habilitada = false, escolhida = false),
        ),
        numeroDaSemana = 12,
        sessaoEscolhida = SessaoReivindicada.Longao,
    ),
)

@Preview(showBackground = true)
@Composable
private fun ConcluidoPreview() = EditarPreview(
    EditarCorridaUiState.Concluido(
        descartada = false,
        dia = LocalDate.of(2026, 9, 12),
        mudancas = listOf(Mudanca(R.string.editar_resumo_distancia, "8,4", "5,2")),
        resumo = ResumoDaCorrecao(74_200, 71_100, 118, 116, setOf(TipoDeRecorde.MELHOR_5K)),
        especieAntes = 118,
        especieDepois = 116,
    ),
)

@Preview(showBackground = true)
@Composable
private fun RecalculandoPreview() = EditarPreview(EditarCorridaUiState.Recalculando)
