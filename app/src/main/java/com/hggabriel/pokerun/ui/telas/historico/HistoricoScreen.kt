package com.hggabriel.pokerun.ui.telas.historico

import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.ChipDeFiltro
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.formatarPace
import com.hggabriel.pokerun.ui.componentes.nomeDoDia
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.componentes.rotuloCurtoDoDia
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.YearMonth

private val Goteira = 16.dp
private val AlvoDeToque = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val RecheioDaLinha = 12.dp
private val FolgaDoFim = 24.dp
private val ColunaDoDia = 72.dp

/**
 * A `RunHistoryScreen` (`F2-T11`, docs/03 §3.15): o histórico vitalício, agrupado por
 * mês, com os chips Todas e Plano Atual.
 *
 * **Na Fase 2 era a raiz de Progresso** (decisão nº 97). Desde `F3-T09` é a aba interna
 * `Histórico` da `StatsDashboardScreen`, que lhe entrega o cabeçalho com o seletor de aba;
 * sem ele, leva o cabeçalho de aba com a engrenagem (docs/02 §10.1). Cada linha abre a
 * `RunDetailScreen`.
 */
@Composable
fun HistoricoScreen(
    aoAbrirCorrida: (String) -> Unit,
    aoRegistrarCorrida: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HistoricoViewModel = historicoViewModel(),
    cabecalho: (@Composable () -> Unit)? = null,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()

    HistoricoScreen(
        estado = estado,
        filtro = filtro,
        aoEscolherFiltro = vm::escolherFiltro,
        aoAbrirCorrida = aoAbrirCorrida,
        aoRegistrarCorrida = aoRegistrarCorrida,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
        cabecalho = cabecalho,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun HistoricoScreen(
    estado: HistoricoUiState,
    filtro: FiltroDoHistorico,
    aoEscolherFiltro: (FiltroDoHistorico) -> Unit,
    aoAbrirCorrida: (String) -> Unit,
    aoRegistrarCorrida: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    cabecalho: (@Composable () -> Unit)? = null,
) {
    // A posição sobrevive à volta da `RunDetailScreen` (docs/03 §1): o estado da lista é
    // salvável, e a entrada da pilha guarda o dele.
    val rolagem = rememberLazyListState()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            state = rolagem,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                // `F3-T09`: dentro do hospedeiro de abas de Progresso o cabeçalho é o dele,
                // com o seletor de aba. Sozinha, a tela leva o seu (docs/02 §10.1).
                if (cabecalho != null) {
                    cabecalho()
                } else {
                    CabecalhoDeAba(
                        aba = stringResource(R.string.destino_progresso),
                        titulo = stringResource(R.string.historico_titulo),
                    )
                }
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
                Chips(filtro, aoEscolherFiltro)
                Spacer(Modifier.height(EspacoEntreBlocos))
            }

            when (estado) {
                // Cinco linhas de corrida em espera, na altura da ficha.
                HistoricoUiState.Carregando -> item(key = "esqueleto") {
                    CartoesEmEspera(
                        alturas = List(5) { AlvoDeToque + 8.dp },
                        modifier = Modifier.padding(horizontal = Goteira),
                        cor = MaterialTheme.colorScheme.outlineVariant,
                        espaco = EspacoEntreLinhas,
                    )
                }
                HistoricoUiState.Falhou -> falhou(aoTentarDeNovo)
                HistoricoUiState.Vazio -> vazio(aoRegistrarCorrida)
                is HistoricoUiState.Lista -> lista(estado.meses, aoAbrirCorrida)
            }
        }
    }
}

@Composable
private fun Chips(filtro: FiltroDoHistorico, aoEscolher: (FiltroDoHistorico) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = Goteira),
        horizontalArrangement = Arrangement.spacedBy(EspacoEntreLinhas),
    ) {
        FiltroDoHistorico.entries.forEach { opcao ->
            ChipDeFiltro(
                selecionado = opcao == filtro,
                aoTocar = { aoEscolher(opcao) },
                rotulo = stringResource(
                    if (opcao == FiltroDoHistorico.TODAS) R.string.historico_filtro_todas else R.string.historico_filtro_plano,
                ),
            )
        }
    }
}

private fun LazyListScope.lista(meses: List<MesDoHistorico>, aoAbrirCorrida: (String) -> Unit) {
    meses.forEach { mes ->
        item(key = "mes-${mes.mes}") {
            Text(
                text = textoDoMes(mes.mes).uppercase(LocaleDoApp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Goteira, end = Goteira, top = EspacoEntreBlocos, bottom = EspacoEntreLinhas),
            )
        }
        items(mes.corridas, key = { it.id }) { linha ->
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Ficha(aoTocar = { aoAbrirCorrida(linha.id) }) { LinhaDaCorrida(linha) }
                Spacer(Modifier.height(EspacoEntreLinhas))
            }
        }
    }
}

/** A mesma linha do detalhe da semana (decisão nº 51): dia, distância, tempo e pace. */
@Composable
private fun LinhaDaCorrida(linha: LinhaDoHistorico) {
    val locale = LocaleDoApp
    val dia = "${rotuloCurtoDoDia(linha.dia.dayOfWeek, locale)} ${linha.dia.dayOfMonth}"
    val km = formatarKm(linha.km, locale)
    val duracao = formatarDuracao(linha.duracaoSeg, locale)
    val pace = formatarPace(linha.km, linha.duracaoSeg, locale)
    val dados = if (pace != null) {
        stringResource(R.string.semana_detalhe_corrida_pace, km, duracao, pace)
    } else {
        stringResource(R.string.semana_detalhe_corrida_dados, km, duracao)
    }
    val descricao = if (pace != null) {
        stringResource(R.string.semana_detalhe_corrida_descricao_pace, nomeDoDia(linha.dia.dayOfWeek, locale), km, duracao, pace)
    } else {
        stringResource(R.string.semana_detalhe_corrida_descricao, nomeDoDia(linha.dia.dayOfWeek, locale), km, duracao)
    }

    val bloco = Modifier
        .fillMaxWidth()
        .heightIn(min = AlvoDeToque)
        .padding(RecheioDaLinha)
        .clearAndSetSemantics { contentDescription = descricao }

    if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
        Column(modifier = bloco, verticalArrangement = Arrangement.spacedBy(EspacoEntreLinhas)) {
            Text(text = dia, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = dados, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    Row(modifier = bloco, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dia,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(ColunaDoDia),
        )
        Spacer(Modifier.width(EspacoEntreLinhas))
        Text(text = dados, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * O estado vazio de docs/03 §3.15, com a ação de registrar, sobre o padrão de Pokébolas
 * (docs/02 §4.2). A borda da ficha substitui o divisor que abria o bloco.
 */
private fun LazyListScope.vazio(aoRegistrarCorrida: () -> Unit) {
    item {
        EstadoVazio(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.historico_vazio),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            BotaoPrimario(
                aoTocar = aoRegistrarCorrida,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(stringResource(R.string.manual_salvar))
            }
        }
    }
}

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.historico_erro_corpo),
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

@Composable
private fun textoDoMes(mes: YearMonth): String =
    stringResource(R.string.historico_mes, nomeDoMes(mes.atDay(1), LocaleDoApp), mes.year)

@Composable
private fun historicoViewModel(): HistoricoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        HistoricoViewModel(
            createSavedStateHandle(),
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
private fun HistoricoPreview(estado: HistoricoUiState) {
    PokerunTheme {
        HistoricoScreen(
            estado = estado,
            filtro = FiltroDoHistorico.TODAS,
            aoEscolherFiltro = {},
            aoAbrirCorrida = {},
            aoRegistrarCorrida = {},
            aoTentarDeNovo = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListaPreview() = HistoricoPreview(
    HistoricoUiState.Lista(
        listOf(
            MesDoHistorico(
                YearMonth.of(2026, 9),
                listOf(
                    LinhaDoHistorico("a", LocalDate.of(2026, 9, 14), 12.4, 3862),
                    LinhaDoHistorico("b", LocalDate.of(2026, 9, 10), 6.2, 1930),
                ),
            ),
            MesDoHistorico(YearMonth.of(2026, 8), listOf(LinhaDoHistorico("c", LocalDate.of(2026, 8, 30), 5.8, 1805))),
        ),
    ),
)

@Preview(showBackground = true)
@Composable
private fun VazioPreview() = HistoricoPreview(HistoricoUiState.Vazio)

@Preview(showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = ListaPreview()
