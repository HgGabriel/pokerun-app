package com.hggabriel.pokerun.ui.telas.revisarrascunho

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hggabriel.pokerun.dominio.regras.SaltoDeVolume
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.GradeDeSemanas
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp

/**
 * A revisão do rascunho (`F1-T11`, docs/03 §3.6).
 *
 * **O usuário só edita o longão.** O volume deriva dele pela fórmula de docs/01 §3.2, e
 * não existe campo de volume: um campo manda, o outro sai dele. Com dois campos, os dois
 * divergem e ninguém sabe qual é o verdadeiro.
 *
 * **O alerta de 15% não bloqueia** (RN-30). Ele aparece na grade recém-gerada quando a
 * distância confortável é baixa em relação à prova — a pessoa que mais precisa do aviso —
 * e reaparece sempre que uma edição cria um salto. O botão de criar nunca olha para ele.
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
                        style = EstiloDado,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(EspacoEntreBlocos))
                }

                estado.alerta?.let { salto ->
                    AlertaDeLesao(salto)
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
                    Text(
                        text = stringResource(erro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(EspacoDepoisDoCabecalho))

                Button(
                    onClick = aoCriarPlano,
                    enabled = estado.podeCriar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDoBotao),
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
        )
    }
}

/** // RN-30 */
@Composable
private fun AlertaDeLesao(salto: SaltoDeVolume) {
    BannerDeAlerta(
        rotulo = stringResource(R.string.revisar_alerta_rotulo),
        texto = stringResource(
            R.string.revisar_alerta_texto,
            salto.percentual,
            salto.de,
            salto.para,
        ),
    )
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
 * recalcula sozinho ao confirmar — mostrá-lo aqui como segundo número convidaria a
 * pergunta "posso mudar este também?", que docs/01 §3.3 responde com não.
 */
@Composable
private fun DialogoDoLongao(
    edicao: EdicaoDoLongao,
    aoMudar: (String) -> Unit,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.revisar_editar_titulo, edicao.numero)) },
        text = {
            OutlinedTextField(
                value = edicao.texto,
                onValueChange = aoMudar,
                label = { Text(stringResource(R.string.revisar_editar_campo)) },
                suffix = { Text(stringResource(R.string.criar_km)) },
                singleLine = true,
                isError = edicao.erro != null,
                supportingText = {
                    Text(stringResource(edicao.erro ?: R.string.revisar_editar_apoio))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(R.string.revisar_editar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(android.R.string.cancel))
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
        semanaDeExemplo(1, 10.0, 5.0, TipoDeSemana.BUILD),
        semanaDeExemplo(2, 11.3, 5.7, TipoDeSemana.BUILD),
        semanaDeExemplo(3, 12.6, 6.3, TipoDeSemana.BUILD),
        semanaDeExemplo(19, 19.8, 9.9, TipoDeSemana.TAPER),
        semanaDeExemplo(20, 13.2, null, TipoDeSemana.TAPER),
        semanaDeExemplo(21, 15.0, null, TipoDeSemana.PROVA, sessoes = 1),
    ),
)

@Preview(name = "A grade sem alerta", showBackground = true)
@Composable
private fun SemAlertaPreview() = RevisarPreview(RASCUNHO_DE_EXEMPLO)

@Preview(name = "Com o alerta de 15%", showBackground = true)
@Composable
private fun ComAlertaPreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(alerta = SaltoDeVolume(de = 6, para = 7, percentual = 22)),
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
    RASCUNHO_DE_EXEMPLO.copy(editando = EdicaoDoLongao(numero = 3, texto = "6,3")),
)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = RevisarPreview(
    RASCUNHO_DE_EXEMPLO.copy(alerta = SaltoDeVolume(de = 6, para = 7, percentual = 22)),
)
