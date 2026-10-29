package com.hggabriel.pokerun.ui.telas.conta

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.ZoneId

private val AlturaDaEtapa = 6.dp

/**
 * Excluir conta (29/10/2026), a última linha do Perfil. Três etapas, para ninguém excluir
 * sem querer:
 * 1. **O que sai:** as corridas e os km da pessoa, o resto em uma linha, e os 30 dias em que
 *    a conta fica guardada, com a data;
 * 2. **Confirme:** digitar `EXCLUIR` libera o botão;
 * 3. **O diálogo final**, com a data de novo.
 *
 * Confirmada, a conta é marcada para o TTL do Firestore e a sessão sai. Fica na pilha modal.
 */
@Composable
fun ExcluirContaScreen(aoSair: () -> Unit, modifier: Modifier = Modifier, vm: ExcluirContaViewModel = excluirContaViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.saiu) { if (estado.saiu) aoSair() }
    BackHandler(enabled = estado.etapa == EtapaDaExclusao.CONFIRMACAO) { vm.voltarAoQueSai() }

    ExcluirContaScreen(
        estado = estado,
        aoContinuar = vm::continuar,
        aoDigitar = vm::digitar,
        aoPedirConfirmacaoFinal = vm::pedirConfirmacaoFinal,
        aoCancelarConfirmacaoFinal = vm::cancelarConfirmacaoFinal,
        aoExcluir = vm::excluir,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, para os previews. */
@Composable
fun ExcluirContaScreen(
    estado: ExcluirContaUiState,
    aoContinuar: () -> Unit,
    aoDigitar: (String) -> Unit,
    aoPedirConfirmacaoFinal: () -> Unit,
    aoCancelarConfirmacaoFinal: () -> Unit,
    aoExcluir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dataFinal = dataPorExtenso(estado.prazo)

    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.perfil_sobrancelha)),
                    titulo = stringResource(R.string.conta_excluir),
                )
                Column(
                    modifier = Modifier.padding(horizontal = Espacos.margem, vertical = Espacos.interno),
                    verticalArrangement = Arrangement.spacedBy(Espacos.interno),
                ) {
                    Etapas(estado.etapa)
                    when (estado.etapa) {
                        EtapaDaExclusao.O_QUE_SAI -> {
                            CartaoDoQueSai(estado.corridas, estado.km)
                            CartaoDosTrintaDias(dataFinal)
                        }
                        EtapaDaExclusao.CONFIRMACAO -> {
                            if (estado.erro != null) {
                                BannerDeAlerta(rotulo = stringResource(R.string.alerta_corrija), texto = stringResource(estado.erro))
                            }
                            CartaoDaConfirmacao(estado.digitado, aoDigitar)
                        }
                    }
                }
            }

            BotaoPrimario(
                aoTocar = if (estado.etapa == EtapaDaExclusao.O_QUE_SAI) aoContinuar else aoPedirConfirmacaoFinal,
                habilitado = estado.etapa == EtapaDaExclusao.O_QUE_SAI || estado.podeExcluir,
                ocupado = estado.excluindo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
            ) {
                Text(
                    stringResource(if (estado.etapa == EtapaDaExclusao.O_QUE_SAI) R.string.conta_continuar else R.string.conta_excluir),
                )
            }
        }
    }

    if (estado.confirmandoFinal) {
        AlertDialog(
            onDismissRequest = aoCancelarConfirmacaoFinal,
            title = { Text(stringResource(R.string.conta_final_titulo)) },
            text = { Text(stringResource(R.string.conta_final_texto, dataFinal)) },
            confirmButton = { BotaoDeTexto(aoTocar = aoExcluir) { Text(stringResource(R.string.conta_final_confirmar)) } },
            dismissButton = { BotaoDeTexto(aoTocar = aoCancelarConfirmacaoFinal) { Text(stringResource(R.string.conta_final_voltar)) } },
        )
    }
}

/** As duas etapas em pílulas: a feita e a atual cheias de tinta. */
@Composable
private fun Etapas(etapa: EtapaDaExclusao) {
    val numero = etapa.ordinal + 1
    val falado = stringResource(R.string.conta_excluir_etapa, numero)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = falado },
        horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado),
    ) {
        EtapaDaExclusao.entries.forEach { cada ->
            val cor by animateColorAsState(
                if (cada.ordinal <= etapa.ordinal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = Movimento.padrao(LocalMovimentoReduzido.current),
                label = "etapa",
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(AlturaDaEtapa)
                    .background(cor, FormaCirculo),
            )
        }
    }
}

/** As corridas e os km, em número; o resto sai numa linha. */
@Composable
private fun CartaoDoQueSai(corridas: Int?, km: Double?) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.conta_o_que_sai))
        if (corridas != null && km != null) {
            Spacer(Modifier.height(Espacos.interno))
            Row {
                Numero(corridas.toString(), pluralStringResource(R.plurals.conta_corridas, corridas), Modifier.weight(1f))
                Numero(formatarKm(km, LocaleDoApp), stringResource(R.string.conta_km), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Espacos.interno))
        Text(
            text = stringResource(R.string.conta_o_que_sai_resto),
            style = Estilos.apoio,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Numero(valor: String, rotulo: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = valor, style = Estilos.numeroL, color = MaterialTheme.colorScheme.onSurface)
        Text(text = rotulo, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Os 30 dias em número grande, e a data em que tudo sai de vez. */
@Composable
private fun CartaoDosTrintaDias(dataFinal: String) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.conta_guardada))
        Spacer(Modifier.height(Espacos.relacionado))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "30", style = Estilos.numeroXl, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(Espacos.relacionado))
            Text(
                text = stringResource(R.string.conta_dias).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Spacer(Modifier.height(Espacos.relacionado))
        Text(
            text = stringResource(R.string.conta_guardada_regra, dataFinal),
            style = Estilos.apoio,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A segunda etapa: digitar `EXCLUIR` libera o botão. */
@Composable
private fun CartaoDaConfirmacao(digitado: String, aoDigitar: (String) -> Unit) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.conta_confirme))
        Spacer(Modifier.height(Espacos.relacionado))
        Text(text = stringResource(R.string.conta_digite), style = Estilos.corpo, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Espacos.interno))
        CampoComErro(
            valor = digitado,
            aoMudar = aoDigitar,
            rotulo = R.string.conta_campo,
            opcoesDeTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
        )
    }
}

/** `28 de novembro de 2026`, no fuso do aparelho. */
@Composable
internal fun dataPorExtenso(instante: Instant): String {
    val dia = instante.atZone(ZoneId.systemDefault()).toLocalDate()
    return stringResource(R.string.corrida_detalhe_dia, dia.dayOfMonth, nomeDoMes(dia, LocaleDoApp), dia.year)
}

@Composable
private fun excluirContaViewModel(): ExcluirContaViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        ExcluirContaViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.corridaRepositorio,
            container.exclusaoDaContaRepositorio,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OQueSaiPreview() {
    PokerunTheme {
        ExcluirContaScreen(
            estado = ExcluirContaUiState(corridas = 3, km = 16.0, prazo = Instant.parse("2026-11-28T12:00:00Z")),
            aoContinuar = {}, aoDigitar = {}, aoPedirConfirmacaoFinal = {}, aoCancelarConfirmacaoFinal = {}, aoExcluir = {},
        )
    }
}
