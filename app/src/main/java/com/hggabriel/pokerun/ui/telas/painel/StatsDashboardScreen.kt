package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.ui.componentes.SegmentosDeEscolha
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.telas.historico.HistoricoScreen
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.time.ZoneId

private val Goteira = 16.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 24.dp
private val FolgaDoFim = 24.dp

/** As duas abas internas de `Progresso` (docs/03 §1, decisão nº 97). A troca é por toque. */
enum class AbaDeProgresso { PAINEL, HISTORICO }

/**
 * A raiz de `Progresso` (`F3-T09`, docs/03 §3.17 e §1): o painel e o histórico lado a lado,
 * numa troca **por toque, sem `HorizontalPager`**, que competiria com o arraste da escada
 * (docs/03 §1). Nasce no painel, onde o heatmap funciona desde o primeiro dia.
 *
 * A aba escolhida é salvável: trocar de aba da barra e voltar, ou a morte do processo,
 * devolve a aba onde estava (docs/03 §1). O cabeçalho, a engrenagem (docs/02 §10.1) e o
 * seletor de aba rolam junto com o conteúdo, e por isso são entregues a cada lista.
 */
@Composable
fun StatsDashboardScreen(
    aoAbrirAjustes: () -> Unit,
    aoAbrirCorrida: (String) -> Unit,
    aoRegistrarCorrida: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aba by rememberSaveable { mutableStateOf(AbaDeProgresso.PAINEL) }

    val cabecalho: @Composable () -> Unit = {
        CabecalhoDeProgresso(aba = aba, aoMudarDeAba = { aba = it }, aoAbrirAjustes = aoAbrirAjustes)
    }

    when (aba) {
        AbaDeProgresso.PAINEL -> PainelScreen(cabecalho = cabecalho, modifier = modifier)
        AbaDeProgresso.HISTORICO -> HistoricoScreen(
            aoAbrirAjustes = aoAbrirAjustes,
            aoAbrirCorrida = aoAbrirCorrida,
            aoRegistrarCorrida = aoRegistrarCorrida,
            modifier = modifier,
            cabecalho = cabecalho,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabecalhoDeProgresso(
    aba: AbaDeProgresso,
    aoMudarDeAba: (AbaDeProgresso) -> Unit,
    aoAbrirAjustes: () -> Unit,
) {
    Column {
        CabecalhoDeAba(
            aba = stringResource(R.string.destino_progresso),
            titulo = stringResource(if (aba == AbaDeProgresso.PAINEL) R.string.painel_titulo else R.string.historico_titulo),
            aoAbrirAjustes = aoAbrirAjustes,
        )
        Spacer(Modifier.height(EspacoDepoisDoCabecalho))
        SegmentosDeEscolha(
            opcoes = AbaDeProgresso.entries,
            escolhida = aba,
            aoEscolher = aoMudarDeAba,
            modifier = Modifier.padding(horizontal = Goteira),
        ) { opcao ->
            stringResource(if (opcao == AbaDeProgresso.PAINEL) R.string.painel_titulo else R.string.historico_titulo)
        }
    }
}

/** O painel com o `ViewModel`, que o app usa. */
@Composable
private fun PainelScreen(cabecalho: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: PainelViewModel = viewModel {
        PainelViewModel(
            container.autenticacaoRepositorio,
            container.corridaRepositorio,
            container.planoRepositorio,
            container.progressoRepositorio,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    PainelScreen(estado = estado, aoTentarDeNovo = vm::tentarDeNovo, cabecalho = cabecalho, modifier = modifier)
}

/** O painel sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
internal fun PainelScreen(
    estado: PainelUiState,
    aoTentarDeNovo: () -> Unit,
    cabecalho: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rolagem = rememberLazyListState()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            state = rolagem,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                cabecalho()
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }
            when (estado) {
                PainelUiState.Carregando -> Unit
                PainelUiState.Falhou -> falhou(aoTentarDeNovo)
                is PainelUiState.Conteudo -> blocos(estado)
            }
        }
    }
}

/** Os cinco blocos de docs/03 §3.17, na ordem da spec: o heatmap vai no topo. */
private fun LazyListScope.blocos(estado: PainelUiState.Conteudo) {
    bloco("heatmap", separar = false) { BlocoDeHeatmap(estado.heatmap, estado.hoje) }
    bloco("recordes") { BlocoDeRecordes(estado.recordes) }
    bloco("volume") { BlocoDeVolume(estado.volume) }
    bloco("pace") { BlocoDePace(estado.pace) }
    bloco("eficiencia") { BlocoDeEficiencia(estado.eficiencia, estado.fuso) }
}

private fun LazyListScope.bloco(chave: String, separar: Boolean = true, conteudo: @Composable () -> Unit) {
    item(key = chave) {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            if (separar) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(EspacoEntreBlocos))
            }
            conteudo()
            Spacer(Modifier.height(EspacoEntreBlocos))
        }
    }
}

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.painel_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val HojeDoPreview: LocalDate = LocalDate.of(2026, 10, 15)

@Composable
private fun PainelPreview(estado: PainelUiState) {
    PokerunTheme {
        PainelScreen(
            estado = estado,
            aoTentarDeNovo = {},
            cabecalho = { CabecalhoDeProgresso(AbaDeProgresso.PAINEL, aoMudarDeAba = {}, aoAbrirAjustes = {}) },
        )
    }
}

private fun painelVazio() = PainelUiState.Conteudo(
    hoje = HojeDoPreview,
    heatmap = gradeDoHeatmap(emptyList(), emptyMap(), 2026),
    recordes = null,
    volume = VolumeAcumulado.Faltam(3),
    pace = PaceMovel.Faltam(8),
    eficiencia = EficienciaCardiaca.Faltam(8),
    fuso = ZoneId.of("America/Sao_Paulo"),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PainelVazioPreview() = PainelPreview(painelVazio())

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun PainelComRecordesPreview() = PainelPreview(
    painelVazio().copy(recordes = Recordes(melhorPaceSegKm = 318.0, maiorDistanciaKm = 14.2, maiorDuracaoSeg = 4660)),
)

@Preview(showBackground = true, widthDp = 320, heightDp = 900, fontScale = 2.0f)
@Composable
private fun PainelFonteGrandePreview() = PainelPreview(painelVazio())

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PainelFalhouPreview() = PainelPreview(PainelUiState.Falhou)
