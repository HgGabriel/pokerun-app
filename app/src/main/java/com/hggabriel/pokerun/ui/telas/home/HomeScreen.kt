package com.hggabriel.pokerun.ui.telas.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BarraDeSessoes
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.componentes.DiaDoTreino
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.FracaoDeSessoes
import com.hggabriel.pokerun.ui.componentes.GradeDeDias
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.nomeDoDia
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import java.time.LocalDate
import java.util.Locale

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val RecheioDaFicha = 16.dp

/** A folga no fim da lista, para o FAB não cobrir o último bloco. */
private val FolgaDoFab = 88.dp

private val EspacoDoFab = 16.dp

/** Alturas dos blocos do esqueleto: contagem, card da semana. */
private val AlturaDoEsqueletoAlto = 96.dp
private val AlturaDoEsqueletoBaixo = 160.dp

/** O ponto médio que separa dois dados na mesma linha. Nunca travessão (docs/02 §9.1). */
private const val SEPARADOR = " · "

/**
 * Acima disto, o número grande cai um degrau tipográfico (docs/02 §8, item 9).
 */
private const val ESCALA_QUE_REBAIXA = 1.5f

/**
 * O painel do plano ativo (`F1-T09`, docs/03 §3.3).
 *
 * **Uma `LazyColumn` com o cabeçalho de ficha dentro dela**, e não uma barra presa ao
 * `Scaffold`: o cabeçalho é conteúdo e rola junto (docs/02 §10). É a decisão que
 * `F1-T07b` tomou por esta tela, e é aqui que ela se paga.
 *
 * Quatro blocos, na ordem: cabeçalho, contagem regressiva, card da semana e — nas fases
 * seguintes — o card da espécie atual (`F2-T14`) e a escada compacta (Fase 4).
 *
 * **O FAB é registro manual e mais nada.** A importação do Health Connect acontece na
 * abertura do app (RN-25), nunca atrás de um botão, e um segundo caminho para o mesmo
 * dado é o jeito mais rápido de gravar corrida duas vezes. Ele aparece só no estado
 * [HomeUiState.Ativo]: sem plano não há o que reivindicar, num plano que não começou a
 * corrida cairia fora do intervalo (RN-03), e plano encerrado é somente-leitura (RN-07).
 *
 * **A contagem regressiva é o único uso de Archivo Expanded em 48sp do app**
 * (docs/02 §3.2), e por isso é o bloco de maior risco em `fontScale` 2,0: o número cai
 * para `displayMedium` acima de 1,5, e a linha com ponto médio empilha acima de 1,3.
 */
@Composable
fun HomeScreen(
    aoAbrirAjustes: () -> Unit,
    aoAbrirPlanos: () -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    aoRegistrarCorrida: () -> Unit,
    aoRetomarCadastro: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = homeViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    HomeScreen(
        estado = estado,
        aoAbrirAjustes = aoAbrirAjustes,
        aoAbrirPlanos = aoAbrirPlanos,
        aoCriarPlano = aoCriarPlano,
        aoEntrarComCodigo = aoEntrarComCodigo,
        aoRegistrarCorrida = aoRegistrarCorrida,
        aoRetomarCadastro = aoRetomarCadastro,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun HomeScreen(
    estado: HomeUiState,
    aoAbrirAjustes: () -> Unit,
    aoAbrirPlanos: () -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    aoRegistrarCorrida: () -> Unit,
    aoRetomarCadastro: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Terminal, como o `Concluido` do onboarding: a tela não muda de aparência, ela sai.
    // Quem foi morto entre autenticar e o passo 2 do cadastro volta para o cadastro. O
    // efeito é lançado, e não chamado na composição: navegar durante o desenho de um
    // quadro é o caminho mais curto para uma pilha em estado inválido.
    LaunchedEffect(estado) {
        if (estado is HomeUiState.SemPerfil) aoRetomarCadastro()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FolgaDoFab),
            ) {
                item {
                    CabecalhoDeAba(
                        aba = stringResource(R.string.destino_hoje),
                        titulo = tituloDaHome(estado),
                        aoAbrirAjustes = aoAbrirAjustes,
                        // docs/03 §3.3: o toque no nome do plano abre a lista. É a única
                        // porta para a `PlansListScreen` que a especificação desenha, e
                        // ela vale mesmo sem plano ativo — quem tem plano dormente
                        // (RN-15) precisa chegar lá para tornar um deles ativo.
                        aoTocarNoTitulo = aoAbrirPlanos.takeIf { estado is HomeUiState.Ativo || estado is HomeUiState.NaoIniciado || estado is HomeUiState.Encerrado || estado is HomeUiState.SemPlano },
                    )
                    Spacer(Modifier.height(EspacoDepoisDoCabecalho))
                }

                when (estado) {
                    HomeUiState.Carregando -> esqueleto()
                    HomeUiState.SemPlano -> semPlano(aoCriarPlano, aoEntrarComCodigo)
                    is HomeUiState.NaoIniciado -> naoIniciado(estado)
                    is HomeUiState.Ativo -> ativo(estado)
                    is HomeUiState.Encerrado -> encerrado(aoCriarPlano, aoEntrarComCodigo)
                    HomeUiState.Falhou -> falhou(aoTentarDeNovo)
                    // A saída já foi lançada acima: o esqueleto é o que fica no quadro
                    // que sobra, e não um vazio piscando.
                    HomeUiState.SemPerfil -> esqueleto()
                }
            }

            if (estado is HomeUiState.Ativo) {
                FloatingActionButton(
                    onClick = aoRegistrarCorrida,
                    // Duas correções que o emulador pediu e que o build não pega.
                    // (1) A cor: o FAB do Material nasce em `primaryContainer`, que
                    // neste tema é `leitura-fraca` — e `leitura-fraca` sobre `papel` não
                    // separa o controle do fundo, que é o que WCAG 1.4.11 cobra em 3:1.
                    // Registrar corrida é a ação principal da tela, e a cor da ação
                    // principal é `leitura`.
                    // (2) A forma: o FAB nasce com canto de 16dp, que não vem do
                    // `Shapes` do tema. 4dp é o raio de painel de docs/02 §4, e é o que
                    // tira o "visual bolha" que a seção inteira existe para evitar.
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(EspacoDoFab),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_registrar_corrida),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ativo — a contagem regressiva e o card da semana
// ---------------------------------------------------------------------------

private fun LazyListScope.ativo(estado: HomeUiState.Ativo) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            ContagemRegressiva(estado)
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            CardDaSemana(estado.semana)
        }
    }
}

/**
 * `129 / DIAS PARA SÃO SILVESTRE / Semana 3 de 21 · 3 sessões previstas`.
 *
 * **O rótulo não carrega artigo.** docs/03 §3.3 escreve `DIAS PARA A SÃO SILVESTRE`
 * porque aquele plano se chama assim; o nome é digitado pelo usuário, e nenhuma regra
 * deriva o gênero de `Meu plano` ou de `Maratona de SP`. Sem artigo a frase está certa
 * para todo nome.
 *
 * Os dois ajustes de escala de docs/02 §8, item 9, estão aqui, e é o único lugar do app
 * que precisa dos dois: o número cai de degrau acima de 1,5, e a linha com ponto médio
 * empilha acima de 1,3.
 */
@Composable
private fun ContagemRegressiva(estado: HomeUiState.Ativo) {
    val locale = LocaleDoApp
    val escala = LocalDensity.current.fontScale
    val dias = estado.diasAteAProva

    val estiloDoNumero: TextStyle = if (escala > ESCALA_QUE_REBAIXA) {
        MaterialTheme.typography.displayMedium
    } else {
        MaterialTheme.typography.displayLarge
    }

    val descricao = pluralStringResource(
        R.plurals.home_contagem_descricao,
        dias,
        dias,
        estado.nomeDoPlano,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = descricao },
    ) {
        Text(
            text = dias.toString(),
            style = estiloDoNumero,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = pluralStringResource(R.plurals.home_contagem_rotulo, dias, estado.nomeDoPlano)
                .uppercase(locale),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }

    Spacer(Modifier.height(EspacoEntreBlocos))

    val semana = stringResource(
        R.string.home_contagem_semana,
        estado.semana.numero,
        estado.semana.totalDeSemanas,
    )
    val sessoes = pluralStringResource(
        R.plurals.home_contagem_sessoes,
        estado.semana.previstas,
        estado.semana.previstas,
    )

    if (escala > ESCALA_QUE_EMPILHA) {
        Column {
            LinhaDeDado(semana)
            LinhaDeDado(sessoes)
        }
    } else {
        LinhaDeDado(semana + SEPARADOR + sessoes)
    }
}

/**
 * O card da semana, **sem anel** (docs/03 §3.3.1).
 *
 * A sobrancelha do card é a semana e o período; depois vêm a fração, a barra de sessões
 * e a grade de sete dias. Os três blocos são de `ui/componentes`, porque `F1-T13` e
 * `F1-T15` montam os mesmos.
 */
@Composable
private fun CardDaSemana(semana: CardDaSemana) {
    val locale = LocaleDoApp

    Ficha {
        Column(modifier = Modifier.padding(RecheioDaFicha)) {
            Text(
                text = (
                    stringResource(R.string.home_card_semana, semana.numero) +
                        SEPARADOR + periodo(semana.primeiroDia, semana.ultimoDia, locale)
                    ).uppercase(locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(EspacoEntreBlocos))

            FracaoDeSessoes(feitas = semana.feitas, previstas = semana.previstas)

            Spacer(Modifier.height(EspacoEntreBlocos))

            BarraDeSessoes(
                segmentos = semana.segmentos,
                longaoKm = semana.longaoKm,
                longaoCumprido = semana.longaoCumprido,
                // A `RunDetailScreen` é `F2-T09` e não tem rota em `Rotas.kt`: o
                // segmento fica não tocável em vez de virar um alvo que não faz nada.
                aoAbrirCorrida = null,
            )

            Spacer(Modifier.height(EspacoEntreBlocos))

            GradeDeDias(dias = semana.dias)

            if (semana.feitas == 0) {
                Spacer(Modifier.height(EspacoEntreBlocos))
                // A copy de docs/03 §3.3 sem a segunda frase: o plano não atribui dias
                // às sessões (XP-03), então "a primeira sessão prevista é hoje" seria
                // mentira em qualquer dia que não fosse o primeiro.
                Text(
                    text = stringResource(R.string.home_semana_sem_corrida),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** `24 a 30 de agosto`, ou `28 de julho a 3 de agosto` quando a semana vira o mês. */
@Composable
private fun periodo(primeiro: LocalDate, ultimo: LocalDate, locale: Locale): String =
    if (primeiro.month == ultimo.month) {
        stringResource(
            R.string.home_card_periodo,
            primeiro.dayOfMonth,
            ultimo.dayOfMonth,
            nomeDoMes(primeiro, locale),
        )
    } else {
        stringResource(
            R.string.home_card_periodo_meses,
            primeiro.dayOfMonth,
            nomeDoMes(primeiro, locale),
            ultimo.dayOfMonth,
            nomeDoMes(ultimo, locale),
        )
    }

// ---------------------------------------------------------------------------
// Os estados sem card
// ---------------------------------------------------------------------------

private fun LazyListScope.naoIniciado(estado: HomeUiState.NaoIniciado) {
    item {
        val locale = LocaleDoApp
        val resumo = estado.primeiraSemana

        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(
                    R.string.home_nao_iniciado_corpo,
                    nomeDoDia(estado.comecaEm.dayOfWeek, locale),
                    estado.comecaEm.dayOfMonth,
                    nomeDoMes(estado.comecaEm, locale),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(EspacoEntreBlocos))

            Ficha {
                val sessoes = pluralStringResource(
                    R.plurals.home_sessoes,
                    resumo.sessoes,
                    resumo.sessoes,
                )
                Text(
                    text = if (resumo.longaoKm != null) {
                        stringResource(
                            R.string.home_nao_iniciado_resumo,
                            resumo.numero,
                            sessoes,
                            formatarKm(resumo.kmAlvo, locale),
                            formatarKm(resumo.longaoKm, locale),
                        )
                    } else {
                        stringResource(
                            R.string.home_nao_iniciado_resumo_sem_longao,
                            resumo.numero,
                            sessoes,
                            formatarKm(resumo.kmAlvo, locale),
                        )
                    },
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(RecheioDaFicha),
                )
            }
        }
    }
}

private fun LazyListScope.semPlano(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            // A copy de docs/03 §3.3, ao pé da letra. Sem ilustração: o sistema não tem
            // ilustração em lugar nenhum, e uma só nos vazios cria um dialeto paralelo
            // (docs/02 §9.1.1).
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

private fun LazyListScope.encerrado(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            // RN-07: plano encerrado é somente-leitura e não reabre (RN-27). O resumo
            // congelado é do `PlanDetailScreen` (D-05); aqui sobram as duas saídas.
            Text(
                text = stringResource(R.string.home_encerrado_corpo),
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
                text = stringResource(R.string.home_erro_corpo),
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

/**
 * O esqueleto na forma do conteúdo (docs/02 §8, item 6): a contagem em cima e o card da
 * semana embaixo. Blocos estáticos em `borda`, sem brilho varrendo — shimmer é decoração
 * animada e contradiz "um momento animado por semana" (docs/02 §9.1.1).
 */
private fun LazyListScope.esqueleto() {
    item {
        Column(
            modifier = Modifier.padding(horizontal = Goteira),
            verticalArrangement = Arrangement.spacedBy(EspacoEntreBlocos),
        ) {
            listOf(AlturaDoEsqueletoAlto, AlturaDoEsqueletoBaixo).forEach { altura ->
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(altura)
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

@Composable
private fun LinhaDeDado(texto: String) {
    Text(
        text = texto,
        style = EstiloDado,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * O título do cabeçalho é o **nome do plano ativo** (docs/03 §3.3), e não o nome da tela.
 *
 * Sem plano e enquanto carrega ele não existe ainda, e a alternativa seria um título
 * vazio piscando: `Seu plano` e `Sem plano ativo` dizem a verdade dos dois estados sem
 * repetir a sobrancelha, que já diz `HOJE`.
 */
@Composable
private fun tituloDaHome(estado: HomeUiState): String = when (estado) {
    is HomeUiState.Ativo -> estado.nomeDoPlano
    is HomeUiState.NaoIniciado -> estado.nomeDoPlano
    is HomeUiState.Encerrado -> estado.nomeDoPlano
    HomeUiState.Carregando -> stringResource(R.string.home_titulo_carregando)
    else -> stringResource(R.string.home_titulo_sem_plano)
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun homeViewModel(): HomeViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        HomeViewModel(
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
private fun HomePreview(estado: HomeUiState) {
    PokerunTheme {
        HomeScreen(
            estado = estado,
            aoAbrirAjustes = {},
            aoAbrirPlanos = {},
            aoCriarPlano = {},
            aoEntrarComCodigo = {},
            aoRegistrarCorrida = {},
            aoRetomarCadastro = {},
            aoTentarDeNovo = {},
        )
    }
}

private val SEMANA_DE_EXEMPLO = CardDaSemana(
    numero = 3,
    totalDeSemanas = 21,
    primeiroDia = LocalDate.of(2026, 8, 24),
    ultimoDia = LocalDate.of(2026, 8, 30),
    feitas = 2,
    previstas = 3,
    longaoKm = 6.5,
    longaoCumprido = false,
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
)

private val ATIVO_DE_EXEMPLO = HomeUiState.Ativo(
    nomeDoPlano = "São Silvestre",
    diasAteAProva = 129,
    semana = SEMANA_DE_EXEMPLO,
)

@Preview(name = "Ativo", showBackground = true)
@Composable
private fun AtivoPreview() = HomePreview(ATIVO_DE_EXEMPLO)

@Preview(name = "Ativo · semana sem corrida", showBackground = true)
@Composable
private fun SemanaVaziaPreview() = HomePreview(
    ATIVO_DE_EXEMPLO.copy(
        semana = SEMANA_DE_EXEMPLO.copy(
            feitas = 0,
            segmentos = SEMANA_DE_EXEMPLO.segmentos.map { it.copy(corridaId = null, km = null) },
            dias = SEMANA_DE_EXEMPLO.dias.map { it.copy(corridas = 0) },
        ),
    ),
)

@Preview(name = "Ativo · longão cumprido", showBackground = true)
@Composable
private fun LongaoCumpridoPreview() = HomePreview(
    ATIVO_DE_EXEMPLO.copy(
        semana = SEMANA_DE_EXEMPLO.copy(
            feitas = 3,
            longaoCumprido = true,
            segmentos = listOf(
                SegmentoDaSemana(SessaoReivindicada.Curta(1), "run-1", 6.2),
                SegmentoDaSemana(SessaoReivindicada.Curta(2), "run-2", 5.8),
                SegmentoDaSemana(SessaoReivindicada.Longao, "run-3", 6.5),
            ),
        ),
    ),
)

@Preview(name = "Ativo · semana da prova", showBackground = true)
@Composable
private fun SemanaDaProvaPreview() = HomePreview(
    HomeUiState.Ativo(
        nomeDoPlano = "São Silvestre",
        diasAteAProva = 2,
        semana = CardDaSemana(
            numero = 21,
            totalDeSemanas = 21,
            primeiroDia = LocalDate.of(2026, 12, 28),
            ultimoDia = LocalDate.of(2026, 12, 31),
            feitas = 0,
            previstas = 1,
            longaoKm = null,
            longaoCumprido = null,
            segmentos = listOf(SegmentoDaSemana(SessaoReivindicada.Curta(1))),
            dias = (28..31).map { DiaDoTreino(LocalDate.of(2026, 12, it), 0, it == 29) },
        ),
    ),
)

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun CarregandoPreview() = HomePreview(HomeUiState.Carregando)

@Preview(name = "Sem plano", showBackground = true)
@Composable
private fun SemPlanoPreview() = HomePreview(HomeUiState.SemPlano)

@Preview(name = "Não iniciado", showBackground = true)
@Composable
private fun NaoIniciadoPreview() = HomePreview(
    HomeUiState.NaoIniciado(
        nomeDoPlano = "São Silvestre",
        comecaEm = LocalDate.of(2026, 8, 10),
        primeiraSemana = ResumoDaSemana(numero = 1, sessoes = 3, kmAlvo = 10.0, longaoKm = 5.0),
    ),
)

@Preview(name = "Encerrado", showBackground = true)
@Composable
private fun EncerradoPreview() = HomePreview(HomeUiState.Encerrado("São Silvestre"))

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = HomePreview(HomeUiState.Falhou)

@Preview(name = "Ativo em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun AtivoFonteGrandePreview() = HomePreview(ATIVO_DE_EXEMPLO)

@Preview(name = "Ativo em 320dp", showBackground = true, widthDp = 320)
@Composable
private fun AtivoEstreitoPreview() = HomePreview(ATIVO_DE_EXEMPLO)
