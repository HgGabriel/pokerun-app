package com.hggabriel.pokerun.ui.telas.detalhesemana

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.ui.componentes.BarraDeSessoes
import com.hggabriel.pokerun.ui.componentes.Cadeado
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.DiaDoTreino
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.FracaoDeSessoes
import com.hggabriel.pokerun.ui.componentes.GradeDeDias
import com.hggabriel.pokerun.ui.componentes.Indice
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import com.hggabriel.pokerun.ui.componentes.TagDoTipo
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.formatarPace
import com.hggabriel.pokerun.ui.componentes.nomeDoDia
import com.hggabriel.pokerun.ui.componentes.periodoDaSemana
import com.hggabriel.pokerun.ui.componentes.rotuloCurtoDoDia
import com.hggabriel.pokerun.ui.componentes.rotuloDoTipo
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val RecheioDaFicha = 16.dp
private val RecheioDaLinha = 12.dp

/** A folga no fim da lista, para o último bloco não colar na barra inferior. */
private val FolgaDoFim = 24.dp

private val AlturaDoEsqueleto = 96.dp

/** A largura da coluna do dia na lista de corridas: cabe `SÁB 15` sem quebrar. */
private val ColunaDoDia = 72.dp

/**
 * O detalhe de uma semana (`F1-T15`, docs/03 §3.9).
 *
 * **É a mesma semana do card da Home, aberta.** As três peças do card sem anel são as de
 * `ui/componentes` — fração, barra de sessões e grade de dias —, e a tela acrescenta o que
 * não cabe num card: o tipo da semana, o alvo, o cadeado de RN-05 e a lista de corridas.
 *
 * **Somente leitura, e por regra e não por escopo.** A edição do longão é do dono e mora
 * na `PlanDetailScreen` (RN-06); registrar corrida é a `ManualRunScreen`. Uma segunda
 * porta para a mesma escrita seria um segundo caminho para o mesmo dado.
 *
 * **Vive dentro da casca, sob a aba `Hoje`** (docs/03 §1), então não leva
 * `safeDrawingPadding`: o `Scaffold` da casca já entrega o espaçamento. Foi o defeito que
 * a `PlansListScreen` pegou no emulador por ser modal, e o inverso aqui empurraria o
 * cabeçalho para baixo do nada.
 */
@Composable
fun DetalheSemanaScreen(
    planoId: String,
    numero: Int,
    modifier: Modifier = Modifier,
    vm: DetalheSemanaViewModel = detalheSemanaViewModel(planoId, numero),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    DetalheSemanaScreen(
        estado = estado,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun DetalheSemanaScreen(
    estado: DetalheSemanaUiState,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conteudo = estado as? DetalheSemanaUiState.Conteudo

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.semana_detalhe_sobrancelha)),
                    // `n de N` é a única forma de número na sobrancelha (docs/02 §10.3), e
                    // o `Indice` é o que impede o `N` de faltar.
                    indice = conteudo?.let { Indice(it.numero, it.totalDeSemanas) },
                    titulo = conteudo?.let { periodoDaSemana(it.primeiroDia, it.ultimoDia) }
                        .orEmpty(),
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                DetalheSemanaUiState.Carregando -> esqueleto()
                DetalheSemanaUiState.Falhou -> falhou(aoTentarDeNovo)
                is DetalheSemanaUiState.Conteudo -> conteudo(estado)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// O conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.conteudo(estado: DetalheSemanaUiState.Conteudo) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Cabecalho(estado)

            Spacer(Modifier.height(EspacoEntreBlocos))

            FracaoDeSessoes(feitas = estado.feitas, previstas = estado.previstas)

            Spacer(Modifier.height(EspacoEntreBlocos))

            BarraDeSessoes(
                segmentos = estado.segmentos,
                longaoKm = estado.longaoKm,
                longaoCumprido = estado.longaoCumprido,
                // A `RunDetailScreen` é `F2-T09` e não tem rota em `Rotas.kt`: o segmento
                // fica não tocável em vez de virar um alvo que não faz nada.
                aoAbrirCorrida = null,
            )

            Spacer(Modifier.height(EspacoEntreBlocos))

            GradeDeDias(dias = estado.dias)

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))

            RotuloDeBloco(stringResource(R.string.semana_detalhe_corridas_rotulo))
            Spacer(Modifier.height(EspacoEntreLinhas))

            if (estado.vazia) Vazio(estado) else Corridas(estado.corridas)
        }
    }
}

/**
 * Tipo da semana, cadeado e o que ela previa.
 *
 * **O cadeado é desenho, e o TalkBack precisa da palavra** (docs/02 §8): o bloco inteiro é
 * um nó de semântica única, e a descrição dele diz "semana encerrada" por extenso — o
 * ícone entra sem rótulo próprio, como na grade de semanas.
 */
@Composable
private fun Cabecalho(estado: DetalheSemanaUiState.Conteudo) {
    val locale = LocaleDoApp
    val tipo = stringResource(rotuloDoTipo(estado.tipo))
    val sessoes = pluralStringResource(
        R.plurals.semana_sessoes,
        estado.previstas,
        estado.previstas,
    )
    val volume = formatarKm(estado.kmAlvo, locale)

    val previsto = estado.longaoKm?.let { longao ->
        stringResource(R.string.semana_dados, formatarKm(longao, locale), volume, sessoes)
    } ?: stringResource(R.string.semana_dados_sem_longao, volume, sessoes)

    val congelada = stringResource(R.string.semana_detalhe_congelada)
    val descricao = if (estado.congelada) "$tipo. $previsto. $congelada" else "$tipo. $previsto"

    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = descricao }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (estado.congelada) Cadeado()
            TagDoTipo(tipo)
        }

        Spacer(Modifier.height(EspacoEntreLinhas))

        Text(
            text = previsto,
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (estado.congelada) {
            Spacer(Modifier.height(EspacoEntreLinhas))
            Text(
                text = congelada,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * O estado vazio de docs/03 §3.9: a frase, e **o que a semana previa logo abaixo**.
 *
 * A segunda linha é a diferença entre um vazio que fecha a porta e um que diz o que ainda
 * dá para fazer. Ela repete a mesma frase do cabeçalho de propósito — quem rolou até aqui
 * não a tem mais na tela.
 */
@Composable
private fun Vazio(estado: DetalheSemanaUiState.Conteudo) {
    val locale = LocaleDoApp
    val sessoes = pluralStringResource(
        R.plurals.semana_sessoes,
        estado.previstas,
        estado.previstas,
    )
    val volume = formatarKm(estado.kmAlvo, locale)

    Column {
        Text(
            text = stringResource(R.string.semana_detalhe_vazia),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = estado.longaoKm?.let { longao ->
                stringResource(R.string.semana_dados, formatarKm(longao, locale), volume, sessoes)
            } ?: stringResource(R.string.semana_dados_sem_longao, volume, sessoes),
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A lista de corridas da semana (docs/03 §3.9).
 *
 * Quatro dados por linha: o dia, a distância, o tempo e o pace. O bloco de ficha completo
 * — FC, splits, esforço percebido, XP — é a `RunDetailScreen` (`F2-T09`), e **a linha não
 * é tocável enquanto ela não existir**, pelo mesmo motivo do segmento da barra.
 *
 * **Acima de `fontScale` 1,3 o dia sobe para a própria linha.** Com `SÁB 15` numa coluna
 * fixa e `12 km · 1:04:22 · 5:22 /km` na outra, em 320dp um dos dois trunca — é o mesmo
 * limiar e o mesmo defeito que o `SEG` da Home pegou no emulador.
 */
@Composable
private fun Corridas(corridas: List<CorridaDaSemana>) {
    Ficha {
        corridas.forEachIndexed { indice, corrida ->
            if (indice > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            LinhaDaCorrida(corrida)
        }
    }
}

@Composable
private fun LinhaDaCorrida(corrida: CorridaDaSemana) {
    val locale = LocaleDoApp
    val dia = "${rotuloCurtoDoDia(corrida.dia.dayOfWeek, locale)} ${corrida.dia.dayOfMonth}"
    val km = formatarKm(corrida.km, locale)
    val duracao = formatarDuracao(corrida.duracaoSeg, locale)
    val pace = formatarPace(corrida.km, corrida.duracaoSeg, locale)

    val dados = if (pace != null) {
        stringResource(R.string.semana_detalhe_corrida_pace, km, duracao, pace)
    } else {
        stringResource(R.string.semana_detalhe_corrida_dados, km, duracao)
    }

    val descricao = if (pace != null) {
        stringResource(
            R.string.semana_detalhe_corrida_descricao_pace,
            nomeDoDia(corrida.dia.dayOfWeek, locale),
            km,
            duracao,
            pace,
        )
    } else {
        stringResource(
            R.string.semana_detalhe_corrida_descricao,
            nomeDoDia(corrida.dia.dayOfWeek, locale),
            km,
            duracao,
        )
    }

    val bloco = Modifier
        .fillMaxWidth()
        .heightIn(min = AlturaDoBotao)
        .padding(RecheioDaLinha)
        .clearAndSetSemantics { contentDescription = descricao }

    if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
        Column(modifier = bloco, verticalArrangement = Arrangement.spacedBy(EspacoEntreLinhas)) {
            Text(
                text = dia,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun RotuloDeBloco(texto: String) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Os estados sem conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.semana_detalhe_erro_corpo),
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
                // O botão de repetir reusa a string da Home, e não uma cópia.
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

/**
 * O esqueleto na forma do conteúdo (docs/02 §8, item 6). Blocos estáticos em `borda`, sem
 * brilho varrendo — shimmer é decoração animada e contradiz "um momento animado por
 * semana" (docs/02 §9.1.1).
 */
private fun LazyListScope.esqueleto() {
    item {
        Column(
            modifier = Modifier.padding(horizontal = Goteira),
            verticalArrangement = Arrangement.spacedBy(EspacoEntreBlocos),
        ) {
            repeat(2) {
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
 * A chave carrega plano e número: sem ela, abrir a semana 4 depois da 3 na mesma pilha
 * devolveria o `ViewModel` da 3, porque a `key` padrão é o tipo.
 */
@Composable
private fun detalheSemanaViewModel(planoId: String, numero: Int): DetalheSemanaViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "semana-$planoId-$numero") {
        DetalheSemanaViewModel(
            planoId,
            numero,
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun SemanaPreview(estado: DetalheSemanaUiState) {
    PokerunTheme {
        DetalheSemanaScreen(estado = estado, aoTentarDeNovo = {})
    }
}

private val CORRIDAS_DE_EXEMPLO = listOf(
    CorridaDaSemana("run-1", LocalDate.of(2026, 8, 25), 6.2, 1_930),
    CorridaDaSemana("run-2", LocalDate.of(2026, 8, 27), 5.8, 1_805),
)

private val SEMANA_DE_EXEMPLO = DetalheSemanaUiState.Conteudo(
    nomeDoPlano = "São Silvestre",
    numero = 3,
    totalDeSemanas = 21,
    primeiroDia = LocalDate.of(2026, 8, 24),
    ultimoDia = LocalDate.of(2026, 8, 30),
    tipo = TipoDeSemana.BUILD,
    kmAlvo = 18.5,
    longaoKm = 6.5,
    longaoCumprido = false,
    feitas = 2,
    previstas = 3,
    segmentos = listOf(
        SegmentoDaSemana(SessaoReivindicada.Curta(1), "run-1", 6.2),
        SegmentoDaSemana(SessaoReivindicada.Curta(2), "run-2", 5.8),
        SegmentoDaSemana(SessaoReivindicada.Longao),
    ),
    dias = listOf(
        DiaDoTreino(LocalDate.of(2026, 8, 24), 0, false),
        DiaDoTreino(LocalDate.of(2026, 8, 25), 1, false),
        DiaDoTreino(LocalDate.of(2026, 8, 26), 0, false),
        DiaDoTreino(LocalDate.of(2026, 8, 27), 1, true),
        DiaDoTreino(LocalDate.of(2026, 8, 28), 0, false),
        DiaDoTreino(LocalDate.of(2026, 8, 29), 0, false),
        DiaDoTreino(LocalDate.of(2026, 8, 30), 0, false),
    ),
    corridas = CORRIDAS_DE_EXEMPLO,
    congelada = false,
)

@Preview(name = "Semana corrente", showBackground = true)
@Composable
private fun CorrentePreview() = SemanaPreview(SEMANA_DE_EXEMPLO)

@Preview(name = "Congelada, com o longão cumprido", showBackground = true)
@Composable
private fun CongeladaPreview() = SemanaPreview(
    SEMANA_DE_EXEMPLO.copy(
        congelada = true,
        feitas = 3,
        longaoCumprido = true,
        segmentos = SEMANA_DE_EXEMPLO.segmentos.dropLast(1) +
            SegmentoDaSemana(SessaoReivindicada.Longao, "run-3", 6.5),
        corridas = CORRIDAS_DE_EXEMPLO +
            CorridaDaSemana("run-3", LocalDate.of(2026, 8, 30), 6.5, 2_402),
        dias = SEMANA_DE_EXEMPLO.dias.map {
            if (it.dia.dayOfMonth == 30) it.copy(corridas = 1) else it
        },
    ),
)

@Preview(name = "Semana sem corrida", showBackground = true)
@Composable
private fun VaziaPreview() = SemanaPreview(
    SEMANA_DE_EXEMPLO.copy(
        feitas = 0,
        corridas = emptyList(),
        segmentos = SEMANA_DE_EXEMPLO.segmentos.map { it.copy(corridaId = null, km = null) },
        dias = SEMANA_DE_EXEMPLO.dias.map { it.copy(corridas = 0) },
    ),
)

@Preview(name = "Semana da prova", showBackground = true)
@Composable
private fun ProvaPreview() = SemanaPreview(
    DetalheSemanaUiState.Conteudo(
        nomeDoPlano = "São Silvestre",
        numero = 21,
        totalDeSemanas = 21,
        primeiroDia = LocalDate.of(2026, 12, 28),
        ultimoDia = LocalDate.of(2026, 12, 31),
        tipo = TipoDeSemana.PROVA,
        kmAlvo = 15.0,
        longaoKm = null,
        longaoCumprido = null,
        feitas = 0,
        previstas = 1,
        segmentos = listOf(SegmentoDaSemana(SessaoReivindicada.Curta(1))),
        dias = (28..31).map { DiaDoTreino(LocalDate.of(2026, 12, it), 0, it == 29) },
        corridas = emptyList(),
        congelada = false,
    ),
)

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun CarregandoPreview() = SemanaPreview(DetalheSemanaUiState.Carregando)

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = SemanaPreview(DetalheSemanaUiState.Falhou)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = SemanaPreview(SEMANA_DE_EXEMPLO)

@Preview(name = "320dp", showBackground = true, widthDp = 320)
@Composable
private fun EstreitaPreview() = SemanaPreview(SEMANA_DE_EXEMPLO)
