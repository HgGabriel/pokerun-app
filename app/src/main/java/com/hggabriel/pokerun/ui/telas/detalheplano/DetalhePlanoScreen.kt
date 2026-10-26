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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
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
import com.hggabriel.pokerun.ui.telas.revisarrascunho.textoDasMudancas
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlvoDeToque = 48.dp
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
 * nomeado. O diálogo do longão usa a mesma regra da revisão do rascunho — o teto da
 * semana, as seguintes que descem, o "voltar à rampa" (docs/01 §3.3) —, e aqui cada
 * confirmação é uma escrita em `weeks/{n}`. A curta é do plano e se troca no cabeçalho.
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
        aoDesfazerEdicao = vm::desfazerEdicao,
        aoAbrirCurta = vm::abrirCurta,
        aoMudarCurta = vm::curtaMudou,
        aoCancelarCurta = vm::cancelarCurta,
        aoConfirmarCurta = vm::confirmarCurta,
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
    aoDesfazerEdicao: () -> Unit,
    aoAbrirCurta: () -> Unit,
    aoMudarCurta: (String) -> Unit,
    aoCancelarCurta: () -> Unit,
    aoConfirmarCurta: () -> Unit,
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
                is DetalhePlanoUiState.Conteudo -> conteudo(estado, aoEditarSemana, aoAbrirCurta, aoPedirEncerrar)
            }
        }
    }

    val conteudo = estado as? DetalhePlanoUiState.Conteudo
    val edicao = conteudo?.editando

    if (edicao != null) {
        DialogoDoLongao(edicao, aoMudarLongao, aoCancelarEdicao, aoConfirmarEdicao, aoDesfazerEdicao)
    }

    conteudo?.editandoCurta?.let { curta ->
        DialogoDaCurta(curta, aoMudarCurta, aoCancelarCurta, aoConfirmarCurta)
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
    aoAbrirCurta: () -> Unit,
    aoPedirEncerrar: () -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Cabecalho(estado, aoAbrirCurta)

            estado.erro?.let { erro ->
                Spacer(Modifier.height(EspacoEntreBlocos))
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_falha_ao_salvar),
                    texto = stringResource(erro),
                )
            }

            if (estado.tambemMudaram.isNotEmpty()) {
                Spacer(Modifier.height(EspacoEntreBlocos))
                Text(
                    text = textoDasMudancas(estado.tambemMudaram),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                BotaoSecundario(
                    aoTocar = aoPedirEncerrar,
                    modifier = Modifier
                        .fillMaxWidth(),
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
 * **A aderência é uma razão, e não um percentual solto.** docs/02 §9.1.1 põe `87%`
 * grande e isolado entre os vícios, e pede a razão explícita, com numerador e denominador,
 * então o acumulado usa a mesma fração do card da semana.
 */
@Composable
private fun Cabecalho(estado: DetalhePlanoUiState.Conteudo, aoAbrirCurta: () -> Unit) {
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
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(EspacoEntreBlocos))

    RotuloDeBloco(stringResource(R.string.detalhe_aderencia_rotulo))
    Spacer(Modifier.height(EspacoEntreLinhas))
    FracaoDeSessoes(feitas = estado.sessoesFeitas, previstas = estado.sessoesPrevistas)

    Spacer(Modifier.height(EspacoEntreBlocos))
    CurtaDoPlano(estado, aoAbrirCurta)

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

/**
 * A curta do plano (docs/01 §3.3): o número para todo mundo, e o `Mudar` só para o dono
 * enquanto o plano corre. A troca vale a partir da semana seguinte.
 */
@Composable
private fun CurtaDoPlano(estado: DetalhePlanoUiState.Conteudo, aoAbrirCurta: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.detalhe_curta, formatarKm(estado.parametros.curtaKm)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (estado.podeMudarCurta) {
            BotaoDeTexto(aoTocar = aoAbrirCurta) { Text(stringResource(R.string.detalhe_curta_mudar)) }
        }
    }
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
                    .heightIn(min = AlvoDeToque)
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
                    style = MaterialTheme.typography.bodyMedium,
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
    BotaoSecundario(
        aoTocar = {
            val envio = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, convite)
            }
            contexto.startActivity(Intent.createChooser(envio, null))
        },
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

/** O mesmo campo único da revisão do rascunho, com o teto da semana e o "voltar à rampa". */
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
        title = { Text(stringResource(R.string.detalhe_editar_titulo, edicao.numero)) },
        text = {
            CampoComErro(
                valor = edicao.texto,
                aoMudar = aoMudar,
                rotulo = R.string.detalhe_editar_campo,
                erro = edicao.erro,
                apoio = R.string.detalhe_editar_apoio,
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
                Text(stringResource(R.string.detalhe_editar_confirmar))
            }
        },
        dismissButton = {
            Row {
                if (edicao.editada) {
                    BotaoDeTexto(aoTocar = aoDesfazer) { Text(stringResource(R.string.detalhe_editar_desfazer)) }
                }
                BotaoDeTexto(aoTocar = aoCancelar) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}

/** A curta do plano: um campo, e o apoio diz que a troca vale da semana seguinte. */
@Composable
private fun DialogoDaCurta(
    edicao: EdicaoDaCurta,
    aoMudar: (String) -> Unit,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.detalhe_curta_titulo)) },
        text = {
            CampoComErro(
                valor = edicao.texto,
                aoMudar = aoMudar,
                rotulo = R.string.detalhe_curta_campo,
                erro = edicao.erro,
                apoio = R.string.detalhe_curta_apoio,
                sufixo = { Text(stringResource(R.string.criar_km)) },
                opcoesDeTeclado = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            BotaoDeTexto(aoTocar = aoConfirmar) { Text(stringResource(R.string.detalhe_editar_confirmar)) }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoCancelar) { Text(stringResource(android.R.string.cancel)) }
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
            BotaoDeTexto(aoTocar = aoConfirmar) { Text(stringResource(R.string.detalhe_encerrar)) }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoCancelar) { Text(stringResource(android.R.string.cancel)) }
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
            BotaoPrimario(
                aoTocar = aoTentarDeNovo,
                modifier = Modifier
                    .fillMaxWidth(),
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
            aoDesfazerEdicao = {},
            aoAbrirCurta = {},
            aoMudarCurta = {},
            aoCancelarCurta = {},
            aoConfirmarCurta = {},
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
    numeroPrincipal = NumeroPrincipal.Dias(66),
    semanaCorrente = 3,
    ateSemana = 3,
    linhaDoTempo = emptyList(),
    progresso = emptyList(),
    semanaSelecionada = 3,
    semanas = listOf(
        semanaDeExemplo(1, 16.7, 10.7, TipoDeSemana.BUILD),
        semanaDeExemplo(2, 17.5, 11.5, TipoDeSemana.BUILD),
        semanaDeExemplo(3, 18.4, 12.4, TipoDeSemana.BUILD),
        semanaDeExemplo(9, 12.6, 6.6, TipoDeSemana.TAPER),
        semanaDeExemplo(10, 15.0, null, TipoDeSemana.PROVA, sessoes = 1),
    ),
    congeladas = setOf(1, 2),
    comecadas = setOf(1, 2, 3),
    membros = MEMBROS_DE_EXEMPLO,
    codigoConvite = "FYQJE6",
    ehDono = true,
    parametros = ParametrosDeGeracao(
        dataProva = Instant.EPOCH,
        distanciaAlvoKm = 15.0,
        baselineKm = 10.0,
        sessoesPorSemana = 3,
        curtaKm = 3.0,
    ),
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
    CONTEUDO_DE_EXEMPLO.copy(situacao = SituacaoDoPlano.ENCERRADO, congeladas = setOf(1, 2, 3, 9, 10)),
)

@Preview(name = "Dormente", showBackground = true)
@Composable
private fun DormentePreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(situacao = SituacaoDoPlano.DORMENTE),
)

@Preview(name = "Depois de uma edição", showBackground = true)
@Composable
private fun DepoisDaEdicaoPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(tambemMudaram = listOf(MudancaDeLongao(4, 13.3, 12.7), MudancaDeLongao(5, 14.3, 14.0))),
)

@Preview(name = "Editando o longão", showBackground = true)
@Composable
private fun EditandoPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(editando = EdicaoDoLongao(numero = 3, texto = "8", tetoKm = 12.7, editada = true)),
)

@Preview(name = "Mudando a curta", showBackground = true)
@Composable
private fun MudandoCurtaPreview() = DetalhePreview(
    CONTEUDO_DE_EXEMPLO.copy(editandoCurta = EdicaoDaCurta(texto = "4")),
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
