package com.hggabriel.pokerun.ui.telas.detalheplano

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.SaltoDeVolume
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.FracaoDeSessoes
import com.hggabriel.pokerun.ui.componentes.GradeDeSemanas
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.MarcaDeSituacao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val RecheioDaFicha = 16.dp
private val RecheioDaLinha = 12.dp

/** A folga no fim da lista, para o último bloco não colar na borda inferior. */
private val FolgaDoFim = 24.dp

private val AlturaDoEsqueleto = 96.dp

/**
 * O detalhe do plano (`F1-T13`, docs/03 §3.7).
 *
 * **A tela é a mesma para todo mundo; o que muda é o que se pode fazer nela.** As três
 * permissões vêm do estado e têm teste: só o dono edita (RN-06), semana que já acabou não
 * se edita nem para ele (RN-05), e plano encerrado é somente leitura (RN-07, RN-27).
 * Nenhuma delas é uma condição escrita aqui dentro.
 *
 * **A grade é o componente de `F1-T11`**, agora com o cadeado que aquela tarefa deixou
 * nomeado. O diálogo do longão usa a mesma derivação e o mesmo alerta de 15% da revisão do
 * rascunho — a diferença é que aqui cada confirmação é uma escrita em `weeks/{n}`.
 */
@Composable
fun DetalhePlanoScreen(
    planoId: String,
    modifier: Modifier = Modifier,
    vm: DetalhePlanoViewModel = detalhePlanoViewModel(planoId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    DetalhePlanoScreen(
        estado = estado,
        aoEditarSemana = vm::abrirEdicao,
        aoMudarLongao = vm::longaoMudou,
        aoCancelarEdicao = vm::cancelarEdicao,
        aoConfirmarEdicao = vm::confirmarEdicao,
        aoPedirEncerrar = vm::pedirEncerrar,
        aoCancelarEncerrar = vm::cancelarEncerrar,
        aoConfirmarEncerrar = vm::confirmarEncerrar,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun DetalhePlanoScreen(
    estado: DetalhePlanoUiState,
    aoEditarSemana: (Semana) -> Unit,
    aoMudarLongao: (String) -> Unit,
    aoCancelarEdicao: () -> Unit,
    aoConfirmarEdicao: () -> Unit,
    aoPedirEncerrar: () -> Unit,
    aoCancelarEncerrar: () -> Unit,
    aoConfirmarEncerrar: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            // Modal: sem o `Scaffold` da casca, a sobrancelha sairia debaixo do relógio.
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.detalhe_sobrancelha)),
                    titulo = (estado as? DetalhePlanoUiState.Conteudo)?.nome.orEmpty(),
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                DetalhePlanoUiState.Carregando -> esqueleto()
                DetalhePlanoUiState.Falhou -> falhou(aoTentarDeNovo)
                is DetalhePlanoUiState.Conteudo -> conteudo(estado, aoEditarSemana, aoPedirEncerrar)
            }
        }
    }

    val conteudo = estado as? DetalhePlanoUiState.Conteudo
    val edicao = conteudo?.editando

    if (edicao != null) {
        DialogoDoLongao(edicao, aoMudarLongao, aoCancelarEdicao, aoConfirmarEdicao)
    }

    if (conteudo?.confirmandoEncerrar == true) {
        DialogoDeEncerrar(aoCancelarEncerrar, aoConfirmarEncerrar)
    }
}

// ---------------------------------------------------------------------------
// O conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.conteudo(
    estado: DetalhePlanoUiState.Conteudo,
    aoEditarSemana: (Semana) -> Unit,
    aoPedirEncerrar: () -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Cabecalho(estado)

            estado.erro?.let { erro ->
                Spacer(Modifier.height(EspacoEntreBlocos))
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_falha_ao_salvar),
                    texto = stringResource(erro),
                )
            }

            estado.alerta?.let { salto ->
                Spacer(Modifier.height(EspacoEntreBlocos))
                AlertaDeLesao(salto)
            }

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            RotuloDeBloco(stringResource(R.string.detalhe_semanas_rotulo))
            Spacer(Modifier.height(EspacoEntreLinhas))
            GradeDeSemanas(
                semanas = estado.semanas,
                // RN-06 e RN-05: o membro recebe a lista em modo leitura, e o dono não
                // recebe toque nas semanas que já acabaram.
                aoEditar = aoEditarSemana.takeIf { estado.ehDono && !estado.encerrado },
                congeladas = estado.congeladas,
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            RotuloDeBloco(stringResource(R.string.detalhe_membros_rotulo))
            Spacer(Modifier.height(EspacoEntreLinhas))
            Membros(estado.membros, estado.semanas.firstOrNull()?.numero ?: 1)

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            CodigoDeConvite(estado.codigoConvite)

            if (estado.podeEncerrar) {
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
                OutlinedButton(
                    onClick = aoPedirEncerrar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AlturaDoBotao),
                ) {
                    Text(stringResource(R.string.detalhe_encerrar))
                }
            }
        }
    }
}

/**
 * Nome, data da prova e aderência acumulada (docs/03 §3.7).
 *
 * **A aderência é uma razão, e nunca um percentual solto.** docs/02 §9.1.1 proíbe `87%`
 * grande e isolado — *"sempre a razão explícita, com numerador e denominador visíveis"* —,
 * então o acumulado usa a mesma fração em mono do card da semana.
 */
@Composable
private fun Cabecalho(estado: DetalhePlanoUiState.Conteudo) {
    val locale = LocaleDoApp

    Row(verticalAlignment = Alignment.CenterVertically) {
        MarcaDeSituacao(estado.situacao)
    }

    Spacer(Modifier.height(EspacoEntreLinhas))

    Text(
        text = stringResource(
            R.string.detalhe_prova,
            estado.dataDaProva.dayOfMonth,
            nomeDoMes(estado.dataDaProva, locale),
            estado.dataDaProva.year,
            formatarKm(estado.semanas.last().kmAlvo, locale),
        ),
        style = EstiloDado,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(EspacoEntreBlocos))

    RotuloDeBloco(stringResource(R.string.detalhe_aderencia_rotulo))
    Spacer(Modifier.height(EspacoEntreLinhas))
    FracaoDeSessoes(feitas = estado.sessoesFeitas, previstas = estado.sessoesPrevistas)

    val aviso = when (estado.situacao) {
        SituacaoDoPlano.ENCERRADO -> R.string.detalhe_encerrado_aviso
        SituacaoDoPlano.DORMENTE -> R.string.detalhe_dormente_aviso
        SituacaoDoPlano.ATIVO -> null
    }

    aviso?.let {
        Spacer(Modifier.height(EspacoEntreBlocos))
        Text(
            text = stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** // RN-30 */
@Composable
private fun AlertaDeLesao(salto: SaltoDeVolume) {
    BannerDeAlerta(
        rotulo = stringResource(R.string.revisar_alerta_rotulo),
        texto = stringResource(R.string.revisar_alerta_texto, salto.percentual, salto.de, salto.para),
    )
}

/**
 * A lista de membros (docs/03 §3.7).
 *
 * O dono vem marcado, e quem entrou depois da primeira semana traz **em que semana
 * entrou** — é o denominador da aderência dele (RN-19), e sem isso a diferença entre dois
 * membros do mesmo plano fica sem explicação na tela.
 */
@Composable
private fun Membros(membros: List<Membro>, primeiraSemana: Int) {
    Ficha {
        membros.forEachIndexed { indice, membro ->
            if (indice > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AlturaDoBotao)
                    .padding(RecheioDaLinha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = membro.nome,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(EspacoEntreLinhas))
                Text(
                    text = if (indice == 0) {
                        stringResource(R.string.detalhe_membro_dono)
                    } else {
                        stringResource(R.string.detalhe_membro_entrou, membro.entrouNaSemana)
                    }.takeIf { indice == 0 || membro.entrouNaSemana > primeiraSemana }.orEmpty(),
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * O código de convite com `[Compartilhar]` (docs/03 §3.7).
 *
 * O compartilhamento é `ACTION_SEND` com texto, que é o que docs/03 §1 mantém no lugar dos
 * deep links: *"o `[Compartilhar]` continua enviando texto"*. O código vai dentro de uma
 * frase, porque um `FYQJE6` solto num grupo de mensagens não diz o que é.
 */
@Composable
private fun CodigoDeConvite(codigo: String) {
    val contexto = LocalContext.current
    val convite = stringResource(R.string.detalhe_compartilhar_texto, codigo)
    val empilha = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

    RotuloDeBloco(stringResource(R.string.detalhe_codigo_rotulo))
    Spacer(Modifier.height(EspacoEntreLinhas))

    Ficha {
        Column(modifier = Modifier.padding(RecheioDaFicha)) {
            if (empilha) {
                Codigo(codigo)
                Spacer(Modifier.height(EspacoEntreLinhas))
                BotaoCompartilhar(contexto = contexto, convite = convite)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Codigo(codigo)
                    BotaoCompartilhar(contexto = contexto, convite = convite)
                }
            }

            Spacer(Modifier.height(EspacoEntreLinhas))

            Text(
                text = stringResource(R.string.detalhe_codigo_apoio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Codigo(codigo: String) {
    Text(
        text = codigo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BotaoCompartilhar(contexto: android.content.Context, convite: String) {
    OutlinedButton(
        onClick = {
            val envio = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, convite)
            }
            contexto.startActivity(Intent.createChooser(envio, null))
        },
        modifier = Modifier.heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.detalhe_compartilhar))
    }
}

@Composable
private fun RotuloDeBloco(texto: String) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Os diálogos
// ---------------------------------------------------------------------------

/** O mesmo campo único da revisão do rascunho: um manda, o outro deriva. */
@Composable
private fun DialogoDoLongao(
    edicao: EdicaoDoLongao,
    aoMudar: (String) -> Unit,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.detalhe_editar_titulo, edicao.numero)) },
        text = {
            CampoComErro(
                valor = edicao.texto,
                aoMudar = aoMudar,
                rotulo = R.string.detalhe_editar_campo,
                erro = edicao.erro,
                apoio = R.string.detalhe_editar_apoio,
                sufixo = { Text(stringResource(R.string.criar_km)) },
                opcoesDeTeclado = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(R.string.detalhe_editar_confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/**
 * // RN-27
 *
 * A confirmação de encerrar, com a copy de docs/03 §3.7 ao pé da letra. Ela lista o que
 * congela e diz que não reabre, porque é a única ação da Fase 1 sem volta — nem a rule
 * aceita uma segunda escrita depois dela.
 */
@Composable
private fun DialogoDeEncerrar(aoCancelar: () -> Unit, aoConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.detalhe_encerrar_titulo)) },
        text = { Text(stringResource(R.string.detalhe_encerrar_texto)) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) { Text(stringResource(R.string.detalhe_encerrar)) }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

// ---------------------------------------------------------------------------
// Os estados sem conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.detalhe_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            Button(
                onClick = aoTentarDeNovo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AlturaDoBotao),
            ) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

/** O esqueleto na forma do conteúdo (docs/02 §8, item 6). */
private fun LazyListScope.esqueleto() {
    item {
        Column(
            modifier = Modifier.padding(horizontal = Goteira),
            verticalArrangement = Arrangement.spacedBy(EspacoEntreBlocos),
        ) {
            repeat(3) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AlturaDoEsqueleto)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium,
                        ),
                )
            }
        }
    }
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O [planoId] entra pelo construtor: ele é argumento de rota e sobrevive a morte de
 * processo, então o `ViewModel` renasce apontando para o mesmo plano.
 */
@Composable
private fun detalhePlanoViewModel(planoId: String): DetalhePlanoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        DetalhePlanoViewModel(
            planoId,
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun DetalhePreview(estado: DetalhePlanoUiState) {
    PokerunTheme {
        DetalhePlanoScreen(
            estado = estado,
            aoEditarSemana = {},
            aoMudarLongao = {},
            aoCancelarEdicao = {},
            aoConfirmarEdicao = {},
            aoPedirEncerrar = {},
            aoCancelarEncerrar = {},
            aoConfirmarEncerrar = {},
            aoTentarDeNovo = {},
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

private val MEMBROS_DE_EXEMPLO = listOf(
    Membro(uid = "a", nome = "Hiago", entrouEm = Instant.EPOCH, entrouNaSemana = 1),
    Membro(uid = "b", nome = "Alguém do grupo", entrouEm = Instant.EPOCH, entrouNaSemana = 3),
)

private val CONTEUDO_DE_EXEMPLO = DetalhePlanoUiState.Conteudo(
    nome = "São Silvestre 2026",
    dataDaProva = LocalDate.of(2026, 12, 31),
    situacao = SituacaoDoPlano.ATIVO,
    sessoesFeitas = 7,
    sessoesPrevistas = 9,
    semanas = listOf(
        semanaDeExemplo(1, 10.0, 5.0, TipoDeSemana.BUILD),
        semanaDeExemplo(2, 11.3, 5.7, TipoDeSemana.BUILD),
        semanaDeExemplo(3, 12.6, 6.3, TipoDeSemana.BUILD),
        semanaDeExemplo(20, 13.2, null, TipoDeSemana.TAPER),
        semanaDeExemplo(21, 15.0, null, TipoDeSemana.PROVA, sessoes = 1),
    ),
    congeladas = setOf(1, 2),
    membros = MEMBROS_DE_EXEMPLO,
    codigoConvite = "FYQJE6",
    ehDono = true,
)

@Preview(name = "Dono, plano ativo", showBackground = true)
@Composable
private fun DonoPreview() = DetalhePreview(CONTEUDO_DE_EXEMPLO)

@Preview(name = "Membro, somente leitura", showBackground = true)
@Composable
private fun MembroPreview() = DetalhePreview(CONTEUDO_DE_EXEMPLO.copy(ehDono = false))

@Preview(name = "Encerrado", showBackground = true)
@Composable
private fun EncerradoPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(situacao = SituacaoDoPlano.ENCERRADO, congeladas = setOf(1, 2, 3, 20, 21)),
)

@Preview(name = "Dormente", showBackground = true)
@Composable
private fun DormentePreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(situacao = SituacaoDoPlano.DORMENTE),
)

@Preview(name = "Com o alerta de 15%", showBackground = true)
@Composable
private fun ComAlertaPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(alerta = SaltoDeVolume(de = 6, para = 7, percentual = 22)),
)

@Preview(name = "Editando o longão", showBackground = true)
@Composable
private fun EditandoPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(editando = EdicaoDoLongao(numero = 3, texto = "6,3")),
)

@Preview(name = "Confirmando encerrar", showBackground = true)
@Composable
private fun EncerrandoPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(confirmandoEncerrar = true),
)

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun CarregandoPreview() = DetalhePreview(DetalhePlanoUiState.Carregando)

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = DetalhePreview(DetalhePlanoUiState.Falhou)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = DetalhePreview(CONTEUDO_DE_EXEMPLO)
