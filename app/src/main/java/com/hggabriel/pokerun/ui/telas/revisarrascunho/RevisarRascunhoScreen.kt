package com.hggabriel.pokerun.ui.telas.revisarrascunho

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.dominio.regras.Viabilidade
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.GradeDeSemanas
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp

/**
 * A revisão do rascunho (`F1-T11`, docs/03 §3.6).
 *
 * **O usuário só edita o longão das semanas de build.** O volume sai dele e da curta
 * (docs/01 §3.2), e o taper sai do maior longão do build. O diálogo diz o teto da semana
 * antes de o dono digitar, e recusa acima dele (RN-30): a grade nunca sai do limite, e
 * por isso não há alerta.
 *
 * **Pico reduzido é aviso, e não erro.** O plano cabe, e a tela diz que o longão não
 * chega ao pico desejado e quanto a prova fica acima dele.
 *
 * **É esta tela que grava.** `Criar plano` escreve `plans/{id}`, as semanas, o documento
 * do dono e o vínculo em `users/{uid}`, e só então sai. Sem rede o botão fica indisponível
 * pelo mesmo motivo da tela anterior: a reserva do código de convite é transacional
 * (RN-29) e offline a escrita fica pendurada em vez de falhar.
 */
@Composable
fun RevisarRascunhoScreen(
    rascunho: RevisarRascunho,
    aoCriar: () -> Unit,
    modifier: Modifier = Modifier,
    vm: RevisarRascunhoViewModel = revisarRascunhoViewModel(rascunho),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.criado) {
        if (estado.criado) aoCriar()
    }

    RevisarRascunhoScreen(
        estado = estado,
        aoEditarSemana = vm::abrirEdicao,
        aoMudarLongao = vm::longaoMudou,
        aoCancelarEdicao = vm::cancelarEdicao,
        aoConfirmarEdicao = vm::confirmarEdicao,
        aoDesfazerEdicao = vm::desfazerEdicao,
        aoCriarPlano = vm::criarPlano,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun RevisarRascunhoScreen(
    estado: RevisarRascunhoUiState,
    aoEditarSemana: (Semana) -> Unit,
    aoMudarLongao: (String) -> Unit,
    aoCancelarEdicao: () -> Unit,
    aoConfirmarEdicao: () -> Unit,
    aoDesfazerEdicao: () -> Unit,
    aoCriarPlano: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CabecalhoDeFicha(
                sobrancelha = listOf(stringResource(R.string.revisar_sobrancelha)),
                titulo = estado.nomeDoPlano,
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                if (estado.grade.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.revisar_resumo,
                            estado.grade.size,
                            estado.grade.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(EspacoEntreBlocos))
                }

                estado.picoReduzido?.let { reduzido ->
                    Aviso(textoDoPicoReduzido(reduzido, estado.grade))
                    Spacer(Modifier.height(EspacoEntreBlocos))
                }

                if (estado.tambemMudaram.isNotEmpty()) {
                    Aviso(textoDasMudancas(estado.tambemMudaram))
                    Spacer(Modifier.height(EspacoEntreBlocos))
                }

                GradeDeSemanas(semanas = estado.grade, aoEditar = aoEditarSemana)

                // RN-13: o plano novo nasce dormente para quem já tem um ativo, e a tela
                // diz isso antes de gravar. Trocar o ativo é da PlansListScreen.
                if (estado.jaTemPlanoAtivo) {
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    Aviso(stringResource(R.string.revisar_dormente))
                }

                if (!estado.online) {
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    Aviso(stringResource(R.string.revisar_offline))
                }

                estado.erro?.let { erro ->
                    Spacer(Modifier.height(EspacoEntreBlocos))
                    BannerDeAlerta(
                        rotulo = stringResource(R.string.alerta_falha_ao_criar),
                        texto = stringResource(erro),
                    )
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))

                BotaoPrimario(
                    aoTocar = aoCriarPlano,
                    habilitado = estado.podeCriar,
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (estado.salvando) R.string.revisar_criando else R.string.revisar_criar,
                        ),
                    )
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }
        }
    }

    estado.editando?.let { edicao ->
        DialogoDoLongao(
            edicao = edicao,
            aoMudar = aoMudarLongao,
            aoCancelar = aoCancelarEdicao,
            aoConfirmar = aoConfirmarEdicao,
            aoDesfazer = aoDesfazerEdicao,
        )
    }
}

/** Quanto falta para o pico, e quanto a prova fica acima do maior longão. */
@Composable
private fun textoDoPicoReduzido(reduzido: Viabilidade.PicoReduzido, grade: List<Semana>): String {
    val maior = grade.mapNotNull { it.longaoKm }.maxOrNull() ?: reduzido.picoKm
    val prova = grade.lastOrNull()?.kmAlvo ?: return ""
    return stringResource(
        R.string.revisar_pico_reduzido,
        formatarKm(maior),
        formatarKm(reduzido.picoDesejadoKm),
        Math.round((prova / maior - 1) * 100).toInt(),
    )
}

/** *"Também mudaram: semana 4, 13,3 → 12,7 km; semana 5, …"* (docs/01 §3.3). */
@Composable
internal fun textoDasMudancas(mudancas: List<MudancaDeLongao>): String {
    val itens = mudancas.map {
        stringResource(R.string.edicao_mudanca, it.numero, formatarKm(it.deKm), formatarKm(it.paraKm))
    }
    return stringResource(R.string.edicao_tambem_mudaram, itens.joinToString("; "))
}

@Composable
private fun Aviso(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * O diálogo do longão.
 *
 * **Só um campo, e é o longão.** O volume da semana aparece na linha da grade e se
 * recalcula sozinho ao confirmar. O apoio diz o teto antes de o dono digitar; acima dele
 * o mesmo número volta como erro. "Voltar à rampa" só aparece em semana editada.
 */
@Composable
private fun DialogoDoLongao(
    edicao: EdicaoDoLongao,
    aoMudar: (String) -> Unit,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
    aoDesfazer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.revisar_editar_titulo, edicao.numero)) },
        text = {
            CampoComErro(
                valor = edicao.texto,
                aoMudar = aoMudar,
                rotulo = R.string.revisar_editar_campo,
                erro = edicao.erro,
                apoio = R.string.revisar_editar_apoio,
                argumentos = listOf(formatarKm(edicao.tetoKm)),
                sufixo = { Text(stringResource(R.string.criar_km)) },
                opcoesDeTeclado = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            BotaoDeTexto(aoTocar = aoConfirmar) {
                Text(stringResource(R.string.revisar_editar_confirmar))
            }
        },
        dismissButton = {
            Row {
                if (edicao.editada) {
                    BotaoDeTexto(aoTocar = aoDesfazer) {
                        Text(stringResource(R.string.revisar_editar_desfazer))
                    }
                }
                BotaoDeTexto(aoTocar = aoCancelar) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O [rascunho] entra pelo construtor: ele é argumento de rota, sobrevive a morte de
 * processo, e é a entrada inteira da geração.
 */
@Composable
private fun revisarRascunhoViewModel(rascunho: RevisarRascunho): RevisarRascunhoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        RevisarRascunhoViewModel(
            rascunho,
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.conviteRepositorio,
            container.conectividadeRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun RevisarPreview(estado: RevisarRascunhoUiState) {
    PokerunTheme {
        RevisarRascunhoScreen(
            estado = estado,
            aoEditarSemana = {},
            aoMudarLongao = {},
            aoCancelarEdicao = {},
            aoConfirmarEdicao = {},
            aoDesfazerEdicao = {},
            aoCriarPlano = {},
        )
    }
}

private fun semanaDeExemplo(
    numero: Int,
    kmAlvo: Double,
    longaoKm: Double?,
    tipo: TipoDeSemana,
    sessoes: Int = 3,
) = Semana(
    numero = numero,
    dataInicio = Instant.EPOCH,
    dataFim = Instant.EPOCH,
    sessoesAlvo = sessoes,
    kmAlvo = kmAlvo,
    longaoKm = longaoKm,
    tipo = tipo,
    parcial = false,
)

private val RASCUNHO_DE_EXEMPLO = RevisarRascunhoUiState(
    nomeDoPlano = "São Silvestre",
    grade = listOf(
        semanaDeExemplo(1, 16.7, 10.7, TipoDeSemana.BUILD),
        semanaDeExemplo(2, 17.5, 11.5, TipoDeSemana.BUILD),
        semanaDeExemplo(3, 18.4, 12.4, TipoDeSemana.BUILD),
        semanaDeExemplo(8, 15.9, 9.9, TipoDeSemana.TAPER),
        semanaDeExemplo(9, 12.6, 6.6, TipoDeSemana.TAPER),
        semanaDeExemplo(10, 15.0, null, TipoDeSemana.PROVA, sessoes = 1),
    ),
)

@Preview(name = "A grade", showBackground = true)
@Composable
private fun GradePreview() = RevisarPreview(RASCUNHO_DE_EXEMPLO)

@Preview(name = "Pico reduzido e semanas que mudaram", showBackground = true)
@Composable
private fun PicoReduzidoPreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(
        picoReduzido = Viabilidade.PicoReduzido(semanas = 10, picoKm = 12.4, picoDesejadoKm = 16.5),
        tambemMudaram = listOf(MudancaDeLongao(4, 13.3, 12.7), MudancaDeLongao(5, 14.3, 14.0)),
    ),
)

@Preview(name = "Já tem plano ativo", showBackground = true)
@Composable
private fun DormentePreview() = RevisarPreview(RASCUNHO_DE_EXEMPLO.copy(jaTemPlanoAtivo = true))

@Preview(name = "Offline", showBackground = true)
@Composable
private fun OfflinePreview() = RevisarPreview(RASCUNHO_DE_EXEMPLO.copy(online = false))

@Preview(name = "Editando o longão", showBackground = true)
@Composable
private fun EditandoPreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(editando = EdicaoDoLongao(numero = 3, texto = "8", tetoKm = 12.7, editada = true)),
)

@Preview(name = "Editando acima do teto", showBackground = true)
@Composable
private fun AcimaDoTetoPreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(
        editando = EdicaoDoLongao(numero = 3, texto = "14", tetoKm = 12.7, erro = R.string.revisar_editar_erro_teto),
    ),
)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(
        picoReduzido = Viabilidade.PicoReduzido(semanas = 10, picoKm = 12.4, picoDesejadoKm = 16.5),
    ),
)
