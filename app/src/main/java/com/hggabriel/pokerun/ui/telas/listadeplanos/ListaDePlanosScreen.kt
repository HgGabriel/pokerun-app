package com.hggabriel.pokerun.ui.telas.listadeplanos

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.MarcaDeSituacao
import com.hggabriel.pokerun.ui.componentes.rotuloDaSituacao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreItens = 8.dp
private val RecheioDaFicha = 16.dp
private val EspacoAntesDoGrupo = 16.dp
private val EspacoAntesDoBotaoDaLinha = 12.dp

/** A folga no fim da lista, para o último bloco não colar na borda inferior. */
private val FolgaDoFim = 24.dp

/** Alturas dos blocos do esqueleto: três linhas na forma da linha do plano. */
private val AlturaDoEsqueleto = 88.dp

/**
 * A lista de planos (`F1-T12`, docs/03 §3.4).
 *
 * **Três grupos:** o plano ativo em `leitura`, os dormentes em `tinta-fraca` e os
 * encerrados agrupados ao fim (D-05). Quem decide qual plano é qual é [itensDePlano],
 * que é função pura e tem teste; aqui mora só o desenho.
 *
 * **A troca do plano ativo nunca é silenciosa** (RN-13). `[Tornar ativo]` abre um
 * diálogo que nomeia os dois planos e diz o que muda: as corridas novas passam a contar
 * para o plano escolhido, e as já registradas continuam onde estão (RN-14). O botão só
 * existe em plano dormente — no encerrado ele levaria a Home a um beco sem saída (RN-07).
 *
 * **Chega pelo toque no nome do plano no cabeçalho da Home** (docs/03 §1), que é a única
 * porta que a especificação desenha para ela. Por isso o cabeçalho daqui **não** tem
 * engrenagem: ela é da raiz de aba, e esta tela é modal.
 */
@Composable
fun ListaDePlanosScreen(
    aoAbrirPlano: (String) -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ListaDePlanosViewModel = listaDePlanosViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    ListaDePlanosScreen(
        estado = estado,
        aoAbrirPlano = aoAbrirPlano,
        aoCriarPlano = aoCriarPlano,
        aoEntrarComCodigo = aoEntrarComCodigo,
        aoPedirTroca = vm::pedirConfirmacao,
        aoCancelarTroca = vm::cancelarTroca,
        aoConfirmarTroca = vm::confirmarTroca,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun ListaDePlanosScreen(
    estado: ListaDePlanosUiState,
    aoAbrirPlano: (String) -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    aoPedirTroca: (ItemDePlano) -> Unit,
    aoCancelarTroca: () -> Unit,
    aoConfirmarTroca: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            // `safeDrawingPadding` porque esta tela é modal: ela vive no `NavHost` de
            // fora, sem o `Scaffold` da casca para descontar a barra de status. Sem ele
            // a sobrancelha sai debaixo do relógio, e foi o que o emulador mostrou.
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.planos_sobrancelha)),
                    titulo = stringResource(R.string.planos_titulo),
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                ListaDePlanosUiState.Carregando -> esqueleto()
                ListaDePlanosUiState.Vazio -> vazio(aoCriarPlano, aoEntrarComCodigo)
                is ListaDePlanosUiState.Lista -> lista(
                    estado = estado,
                    aoAbrirPlano = aoAbrirPlano,
                    aoPedirTroca = aoPedirTroca,
                    aoCriarPlano = aoCriarPlano,
                    aoEntrarComCodigo = aoEntrarComCodigo,
                )
                ListaDePlanosUiState.Falhou -> falhou(aoTentarDeNovo)
            }
        }
    }

    val lista = estado as? ListaDePlanosUiState.Lista
    val alvo = lista?.confirmando
    if (lista != null && alvo != null) {
        DialogoDaTroca(
            alvo = alvo,
            atual = lista.ativo,
            aoCancelar = aoCancelarTroca,
            aoConfirmar = aoConfirmarTroca,
        )
    }
}

// ---------------------------------------------------------------------------
// A lista
// ---------------------------------------------------------------------------

private fun LazyListScope.lista(
    estado: ListaDePlanosUiState.Lista,
    aoAbrirPlano: (String) -> Unit,
    aoPedirTroca: (ItemDePlano) -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
) {
    itemsIndexed(estado.itens, key = { _, item -> item.id }) { posicao, item ->
        // D-05: os encerrados são um grupo, e o grupo precisa de um rótulo. Ele nasce na
        // primeira linha encerrada, e não num `item` separado, porque a lista já está
        // ordenada — perguntar pela linha anterior é mais barato que particioná-la de novo.
        val abreOGrupo = item.situacao == SituacaoDoPlano.ENCERRADO &&
            estado.itens.getOrNull(posicao - 1)?.situacao != SituacaoDoPlano.ENCERRADO

        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            if (abreOGrupo) {
                Spacer(Modifier.height(EspacoAntesDoGrupo))
                TituloDoGrupo(stringResource(R.string.planos_grupo_encerrados))
                Spacer(Modifier.height(EspacoEntreItens))
            }

            LinhaDoPlano(
                item = item,
                aoAbrir = { aoAbrirPlano(item.id) },
                aoTornarAtivo = { aoPedirTroca(item) },
            )

            Spacer(Modifier.height(EspacoEntreItens))
        }
    }

    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            estado.erro?.let { erro ->
                Spacer(Modifier.height(EspacoEntreBlocos))
                Text(
                    text = stringResource(erro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            AcoesDePlano(aoCriarPlano, aoEntrarComCodigo)
        }
    }
}

/**
 * Uma linha da lista.
 *
 * **O ativo usa o estado selecionado da [Ficha]** — fundo em `leitura-toque` e borda em
 * `borda-forte` —, mais a marca em `leitura` ao lado do nome. São os canais que
 * docs/02 §4.2 exige: nunca só a cor.
 *
 * A linha inteira abre a `PlanDetailScreen` (docs/03 §3.4), e `[Tornar ativo]` fica fora
 * da descrição acessível do bloco para o TalkBack anunciá-lo como o botão que ele é.
 */
@Composable
private fun LinhaDoPlano(item: ItemDePlano, aoAbrir: () -> Unit, aoTornarAtivo: () -> Unit) {
    val locale = LocaleDoApp
    val ativo = item.situacao == SituacaoDoPlano.ATIVO

    val dados = stringResource(
        R.string.planos_dados,
        item.dataDaProva.dayOfMonth,
        nomeDoMes(item.dataDaProva, locale),
        item.dataDaProva.year,
        formatarKm(item.distanciaAlvoKm, locale),
    )
    val marca = stringResource(rotuloDaSituacao(item.situacao))
    val descricao = stringResource(R.string.planos_descricao, item.nome, marca, dados)

    Ficha(aoTocar = aoAbrir, selecionada = ativo) {
        Column(modifier = Modifier.padding(RecheioDaFicha)) {
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = descricao }) {
                // Acima de 1,3 a marca desce para a linha de baixo: com o nome de um
                // plano e `ENCERRADO` lado a lado em 320dp, um dos dois trunca.
                if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
                    NomeDoPlano(item.nome, ativo)
                    if (item.situacao != SituacaoDoPlano.DORMENTE) {
                        Spacer(Modifier.height(EspacoEntreItens))
                        MarcaDeSituacao(item.situacao)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NomeDoPlano(item.nome, ativo, modifier = Modifier.weight(1f, fill = false))
                        // O dormente não ganha marca: ele é o estado comum da lista, e um
                        // rótulo em toda linha faria a do ativo deixar de saltar.
                        if (item.situacao != SituacaoDoPlano.DORMENTE) {
                            Spacer(Modifier.width(EspacoEntreItens))
                            MarcaDeSituacao(item.situacao)
                        }
                    }
                }

                Spacer(Modifier.height(EspacoEntreItens))

                Text(
                    text = dados,
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // RN-13: só o dormente pode virar ativo, e o toque aqui abre a confirmação —
            // nunca a escrita.
            if (item.podeTornarAtivo) {
                Spacer(Modifier.height(EspacoAntesDoBotaoDaLinha))
                OutlinedButton(
                    onClick = aoTornarAtivo,
                    modifier = Modifier.heightIn(min = AlturaDoBotao),
                ) {
                    Text(stringResource(R.string.planos_tornar_ativo))
                }
            }
        }
    }
}

@Composable
private fun NomeDoPlano(nome: String, ativo: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = nome,
        style = MaterialTheme.typography.titleSmall,
        // docs/03 §3.4: o ativo em `leitura`, o resto em `tinta-fraca`.
        color = if (ativo) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    )
}

@Composable
private fun TituloDoGrupo(texto: String) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// A confirmação de RN-13
// ---------------------------------------------------------------------------

/**
 * // RN-13
 *
 * **A troca é uma decisão explícita**, e o diálogo é o lugar dela. Ele nomeia os dois
 * planos porque a pergunta que o usuário faz não é *"quero este?"*, e sim *"o que
 * acontece com o outro?"* — e a resposta é RN-14 mais RN-15: o histórico fica onde está,
 * e o plano de antes continua visível sem receber corridas.
 *
 * [atual] é nulo para quem ainda não tem plano ativo. Nesse caso não há plano que saia,
 * e a segunda frase não é escrita: inventá-la com um nome vazio seria pior que a
 * ausência dela.
 */
@Composable
private fun DialogoDaTroca(
    alvo: ItemDePlano,
    atual: ItemDePlano?,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.planos_confirmar_titulo)) },
        text = {
            Text(
                text = if (atual != null) {
                    stringResource(R.string.planos_confirmar_texto, alvo.nome, atual.nome)
                } else {
                    stringResource(R.string.planos_confirmar_texto_sem_ativo, alvo.nome)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(R.string.planos_tornar_ativo))
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Os estados sem lista
// ---------------------------------------------------------------------------

/**
 * O estado vazio, com **a mesma copy da `HomeScreen · SemPlano`** (docs/03 §3.4).
 *
 * As três strings são as mesmas do recurso, e não cópias: a especificação manda a mesma
 * copy nos dois lugares, e duas strings iguais divergem na primeira revisão que mexer
 * numa delas.
 */
private fun LazyListScope.vazio(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.home_sem_plano_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            AcoesDePlano(aoCriarPlano, aoEntrarComCodigo)
        }
    }
}

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.planos_erro_corpo),
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

/** O esqueleto na forma do conteúdo (docs/02 §8, item 6): três linhas de plano. */
private fun LazyListScope.esqueleto() {
    item {
        Column(
            modifier = Modifier.padding(horizontal = Goteira),
            verticalArrangement = Arrangement.spacedBy(EspacoEntreItens),
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

@Composable
private fun AcoesDePlano(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    Button(
        onClick = aoCriarPlano,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.home_criar_plano))
    }

    Spacer(Modifier.height(EspacoEntreBlocos))

    OutlinedButton(
        onClick = aoEntrarComCodigo,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao),
    ) {
        Text(stringResource(R.string.home_entrar_com_codigo))
    }
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun listaDePlanosViewModel(): ListaDePlanosViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        ListaDePlanosViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun ListaPreview(estado: ListaDePlanosUiState) {
    PokerunTheme {
        ListaDePlanosScreen(
            estado = estado,
            aoAbrirPlano = {},
            aoCriarPlano = {},
            aoEntrarComCodigo = {},
            aoPedirTroca = {},
            aoCancelarTroca = {},
            aoConfirmarTroca = {},
            aoTentarDeNovo = {},
        )
    }
}

private val ATIVO_DE_EXEMPLO = ItemDePlano(
    id = "a",
    nome = "São Silvestre 2026",
    dataDaProva = LocalDate.of(2026, 12, 31),
    distanciaAlvoKm = 15.0,
    situacao = SituacaoDoPlano.ATIVO,
)

private val DORMENTE_DE_EXEMPLO = ItemDePlano(
    id = "b",
    nome = "Meia de Interlagos",
    dataDaProva = LocalDate.of(2027, 3, 14),
    distanciaAlvoKm = 21.1,
    situacao = SituacaoDoPlano.DORMENTE,
)

private val ENCERRADO_DE_EXEMPLO = ItemDePlano(
    id = "c",
    nome = "Corrida do Trabalhador",
    dataDaProva = LocalDate.of(2026, 5, 1),
    distanciaAlvoKm = 10.0,
    situacao = SituacaoDoPlano.ENCERRADO,
)

private val LISTA_DE_EXEMPLO = ListaDePlanosUiState.Lista(
    listOf(ATIVO_DE_EXEMPLO, DORMENTE_DE_EXEMPLO, ENCERRADO_DE_EXEMPLO),
)

@Preview(name = "Os três grupos", showBackground = true)
@Composable
private fun ListaComTresGruposPreview() = ListaPreview(LISTA_DE_EXEMPLO)

@Preview(name = "Sem plano ativo", showBackground = true)
@Composable
private fun SemAtivoPreview() = ListaPreview(
    ListaDePlanosUiState.Lista(
        listOf(
            ATIVO_DE_EXEMPLO.copy(situacao = SituacaoDoPlano.DORMENTE),
            DORMENTE_DE_EXEMPLO,
        ),
    ),
)

@Preview(name = "Confirmando a troca", showBackground = true)
@Composable
private fun ConfirmandoPreview() = ListaPreview(
    LISTA_DE_EXEMPLO.copy(confirmando = DORMENTE_DE_EXEMPLO),
)

@Preview(name = "Erro ao trocar", showBackground = true)
@Composable
private fun ErroAoTrocarPreview() = ListaPreview(
    LISTA_DE_EXEMPLO.copy(erro = R.string.planos_erro_trocar),
)

@Preview(name = "Vazio", showBackground = true)
@Composable
private fun VazioPreview() = ListaPreview(ListaDePlanosUiState.Vazio)

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun CarregandoPreview() = ListaPreview(ListaDePlanosUiState.Carregando)

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = ListaPreview(ListaDePlanosUiState.Falhou)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = ListaPreview(LISTA_DE_EXEMPLO)

@Preview(name = "320dp", showBackground = true, widthDp = 320)
@Composable
private fun EstreitoPreview() = ListaPreview(LISTA_DE_EXEMPLO)
