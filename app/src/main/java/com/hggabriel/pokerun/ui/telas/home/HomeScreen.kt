package com.hggabriel.pokerun.ui.telas.home

import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import com.hggabriel.pokerun.ui.componentes.esqueleto
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.abertura.AvisarProntidao
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.ui.componentes.AvatarEmPilha
import com.hggabriel.pokerun.ui.componentes.BannerEscuro
import com.hggabriel.pokerun.ui.componentes.PilhaDeAvatares
import com.hggabriel.pokerun.ui.componentes.PilulaDoBanner
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.TituloDoBanner
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.componentes.CarimbosDaSemana
import com.hggabriel.pokerun.ui.componentes.DiaDoTreino
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.FracaoDeSessoes
import com.hggabriel.pokerun.ui.componentes.HospedeiroDeSnackbar
import com.hggabriel.pokerun.ui.componentes.AcaoDeIcone
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.SegmentoDaSemana
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.formatarPace
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.componentes.periodoDaSemana
import com.hggabriel.pokerun.ui.componentes.rotuloCurtoDoDia
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Raios
import java.time.LocalDate
import androidx.compose.ui.draw.clip
import com.hggabriel.pokerun.dominio.regras.ritmoDe
import com.hggabriel.pokerun.ui.componentes.naFigura
import com.hggabriel.pokerun.ui.telas.detalhecorrida.SeloDoRitmo
import com.hggabriel.pokerun.ui.telas.detalhecorrida.paceDe
import com.hggabriel.pokerun.ui.telas.detalhecorrida.partesDaDistancia
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = 16.dp

private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp

/** Do plano à ficha da espécie, que é o jogo (`espaco-secao`). */
private val EspacoAntesDoJogo = Espacos.secao
private val RecheioDaFicha = 16.dp

/**
 * A folga no fim da lista, para a Pokébola da barra não cobrir o último bloco: ela sobe
 * 26dp sobre o conteúdo, e o resto é respiro.
 */
private val FolgaDoRodape = 48.dp

/** A margem do snackbar até as bordas da tela. */
private val MargemDoSnackbar = 16.dp

/** O raio da folha branca que sobe sobre a camisa (`raio-especie`). */
private val RaioDaFolha = Raios.especie

/** O filete do cabeçalho sobre o vermelho: branco translúcido, e não o cinza de `borda`. */
private const val OpacidadeDoFileteNaCamisa = 0.3f

private val AlturaDoEsqueletoDoCard = 160.dp

private val AlvoDeToque = Espacos.alvoToque

/** O banner do pulso: o sprite de 132dp cabe com folga. */
private val AlturaDoPulso = 140.dp

/**
 * O painel do plano ativo (`F1-T09`, docs/03 §3.3), recomposto em 18/10/2026 em volta
 * da placa de peito (docs/02 §4.5).
 *
 * **Uma `LazyColumn` com o cabeçalho de ficha dentro dela**, e não uma barra presa ao
 * `Scaffold`: o cabeçalho é conteúdo e rola junto (docs/02 §10).
 *
 * A ordem: a camisa vermelha com o cabeçalho e a placa de peito; depois, na folha branca,
 * o card da semana, a última corrida, o pulso do grupo e a ficha da espécie. O grupo vem
 * a 24dp; a espécie, que é o jogo, a 32dp.
 *
 * **O FAB é o registro manual, a única entrada de corrida do app** (D-20). Ele aparece
 * só no estado [HomeUiState.Ativo]: sem plano não há o que reivindicar, num plano que não
 * começou a corrida cairia fora do intervalo (RN-03), e plano encerrado é somente-leitura
 * (RN-07).
 */
@Composable
fun HomeScreen(
    aoAbrirPerfil: () -> Unit,
    aoAbrirPlanos: () -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    aoRetomarCadastro: () -> Unit,
    aoAbrirSemana: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    aoAbrirCorrida: ((String) -> Unit)? = null,
    aoAbrirPokedex: (() -> Unit)? = null,
    aoAbrirPlano: ((String) -> Unit)? = null,
    aoAbrirGrupo: (() -> Unit)? = null,
    vm: HomeViewModel = homeViewModel(),
    especieVm: EspecieAtualViewModel = especieAtualViewModel(),
    pulsoVm: PulsoDoGrupoViewModel = pulsoDoGrupoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val especie by especieVm.card.collectAsStateWithLifecycle()
    val pulso by pulsoVm.pulso.collectAsStateWithLifecycle()
    val lendoPulso by pulsoVm.lendo.collectAsStateWithLifecycle()
    val avisos = LocalAppContainer.current.avisoDeCorrida
    val aviso by avisos.atual.collectAsStateWithLifecycle()

    // A Home é a primeira tela de quem tem sessão: a abertura anima quando ela sai do esqueleto.
    AvisarProntidao(estado !is HomeUiState.Carregando)

    // O pulso lê uma vez por plano (docs/03 §3.3). A chave é o plano: trocar o ativo
    // (RN-13) lê de novo, e voltar de outra tela não.
    val planoAtivo = (estado as? HomeUiState.Ativo)?.planoId
    LaunchedEffect(planoAtivo) {
        planoAtivo?.let(pulsoVm::carregar)
    }

    HomeScreen(
        estado = estado,
        aoAbrirPerfil = aoAbrirPerfil,
        aoAbrirPlanos = aoAbrirPlanos,
        aoCriarPlano = aoCriarPlano,
        aoEntrarComCodigo = aoEntrarComCodigo,
        aoRetomarCadastro = aoRetomarCadastro,
        aoAbrirSemana = aoAbrirSemana,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
        aoAbrirCorrida = aoAbrirCorrida,
        especie = especie,
        aoAbrirPokedex = aoAbrirPokedex,
        aviso = aviso,
        aoConsumirAviso = avisos::consumir,
        pulso = pulso,
        lendoPulso = lendoPulso,
        aoAbrirPlano = aoAbrirPlano,
        aoAbrirGrupo = aoAbrirGrupo,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun HomeScreen(
    estado: HomeUiState,
    aoAbrirPerfil: () -> Unit,
    aoAbrirPlanos: () -> Unit,
    aoCriarPlano: () -> Unit,
    aoEntrarComCodigo: () -> Unit,
    aoRetomarCadastro: () -> Unit,
    aoAbrirSemana: (String, Int) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    aoAbrirCorrida: ((String) -> Unit)? = null,
    especie: CardDaEspecie? = null,
    aoAbrirPokedex: (() -> Unit)? = null,
    aviso: CorridaRegistrada? = null,
    aoConsumirAviso: (String) -> Unit = {},
    pulso: PulsoDoGrupo? = null,
    lendoPulso: Boolean = false,
    aoAbrirPlano: ((String) -> Unit)? = null,
    aoAbrirGrupo: (() -> Unit)? = null,
) {
    val resposta = respostaAoRegistro(aviso, aoConsumirAviso, aoAbrirCorrida)

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
                contentPadding = PaddingValues(bottom = FolgaDoRodape),
            ) {
                item {
                    CamisaDaHome(
                        cabecalho = {
                            CabecalhoDeAba(
                                aba = stringResource(R.string.destino_hoje),
                                titulo = tituloDaHome(estado),
                                aoAbrirPerfil = aoAbrirPerfil,
                                // docs/03 §3.3: o ícone de lista, ao lado da engrenagem, abre a
                                // lista de planos. A spec põe a troca no toque do nome do plano,
                                // mas título tocável não se lê como botão no aparelho. É a única
                                // porta para a `PlansListScreen` que a especificação desenha, e
                                // ela vale mesmo sem plano ativo — quem tem plano dormente
                                // (RN-15) precisa chegar lá para tornar um deles ativo.
                                acaoExtra = AcaoDeIcone(
                                    icone = Icons.AutoMirrored.Filled.List,
                                    descricao = stringResource(R.string.home_trocar_de_plano),
                                    aoTocar = aoAbrirPlanos,
                                ).takeIf { estado is HomeUiState.Ativo || estado is HomeUiState.NaoIniciado || estado is HomeUiState.Encerrado || estado is HomeUiState.SemPlano },
                            )
                        },
                    ) {
                        PlacaDaHome(estado, aoAbrirPlano)
                    }
                }

                when (estado) {
                    HomeUiState.Carregando -> esqueleto()
                    HomeUiState.SemPlano -> semPlano(aoCriarPlano, aoEntrarComCodigo)
                    is HomeUiState.NaoIniciado -> naoIniciado(estado)
                    is HomeUiState.Ativo -> ativo(
                        estado = estado,
                        aoAbrirSemana = aoAbrirSemana,
                        aoAbrirCorrida = aoAbrirCorrida,
                        especie = especie,
                        aoAbrirPokedex = aoAbrirPokedex,
                        corridaRecente = resposta.corridaRecente,
                        pulso = pulso,
                        lendoPulso = lendoPulso,
                        aoAbrirGrupo = aoAbrirGrupo,
                    )
                    is HomeUiState.Encerrado -> encerrado(aoCriarPlano, aoEntrarComCodigo)
                    HomeUiState.Falhou -> falhou(aoTentarDeNovo)
                    // A saída já foi lançada acima: o esqueleto é o que fica no quadro
                    // que sobra, e não um vazio piscando.
                    HomeUiState.SemPerfil -> esqueleto()
                }
            }

            // Registrar corrida (D-20) é a Pokébola no centro da barra, desde 27/10/2026: ela
            // abre o registro de qualquer aba, e o FAB da Home saiu.

            // Acima da Pokébola, e não sobre ela: a folga é a mesma que a lista reserva no fim.
            HospedeiroDeSnackbar(
                estado = resposta.snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = MargemDoSnackbar, end = MargemDoSnackbar, bottom = FolgaDoRodape),
            )
        }
    }
}

/** O que a Home guarda da resposta ao registro, enquanto ela está na tela. */
private class RespostaAoRegistro(val snackbar: SnackbarHostState, val corridaRecente: String?)

/**
 * A resposta ao registro manual (docs/03 §3.3): o carimbo da corrida nova bate na vaga, e
 * um snackbar diz quanto ela rendeu.
 *
 * **O aviso é consumido assim que o ganho chega, e a Home guarda o resto.** Consumir
 * depois do snackbar faria o mesmo aviso voltar a cada vez que a Home recompõe do zero,
 * como ao trocar de aba no meio dele. A corrida recente fica lembrada enquanto a Home está
 * na tela, porque a corrida pode chegar do cache depois do ganho, e o carimbo só bate
 * quando ela chega.
 *
 * Sem o número (a simulação falhou ou passou do teto), a frase sai sem o XP: a corrida
 * foi registrada, e é isso que o snackbar confirma.
 */
@Composable
private fun respostaAoRegistro(
    aviso: CorridaRegistrada?,
    aoConsumirAviso: (String) -> Unit,
    aoAbrirCorrida: ((String) -> Unit)?,
): RespostaAoRegistro {
    val snackbar = remember { SnackbarHostState() }
    var corridaRecente by remember { mutableStateOf<String?>(null) }
    var pendente by remember { mutableStateOf<CorridaRegistrada?>(null) }

    LaunchedEffect(aviso) {
        val chegou = aviso ?: return@LaunchedEffect
        corridaRecente = chegou.corridaId
        if (chegou.calculado) {
            pendente = chegou
            aoConsumirAviso(chegou.corridaId)
        }
    }

    val mensagem = pendente?.let { registrada ->
        registrada.xp?.let { xp ->
            stringResource(R.string.home_corrida_registrada_xp, String.format(LocaleDoApp, "%,d", xp))
        } ?: stringResource(R.string.home_corrida_registrada)
    }
    val ver = stringResource(R.string.home_corrida_ver)

    LaunchedEffect(pendente) {
        val registrada = pendente ?: return@LaunchedEffect
        val texto = mensagem ?: return@LaunchedEffect
        val resultado = snackbar.showSnackbar(
            message = texto,
            actionLabel = ver.takeIf { aoAbrirCorrida != null },
            duration = SnackbarDuration.Long,
        )
        pendente = null
        if (resultado == SnackbarResult.ActionPerformed) aoAbrirCorrida?.invoke(registrada.corridaId)
    }

    return RespostaAoRegistro(snackbar, corridaRecente)
}

// ---------------------------------------------------------------------------
// A camisa: o vermelho do topo, com o cabeçalho e a placa presa nele
// ---------------------------------------------------------------------------

/**
 * O topo da Home em `destaque`, de borda a borda, com a placa de peito presa como numa
 * camisa de corrida. Termina na borda arredondada da folha branca, onde o resto da tela
 * começa.
 *
 * O cabeçalho é o mesmo das outras abas, só com a tinta trocada para ler sobre o vermelho.
 */
@Composable
private fun CamisaDaHome(cabecalho: @Composable () -> Unit, placa: @Composable () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    val indicacao = LocalIndication.current
    Column(modifier = Modifier.fillMaxWidth().background(esquema.primary)) {
        MaterialTheme(
            colorScheme = esquema.copy(
                onBackground = esquema.onPrimary,
                onSurfaceVariant = esquema.onPrimary,
                outlineVariant = esquema.onPrimary.copy(alpha = OpacidadeDoFileteNaCamisa),
            ),
        ) {
            // O `MaterialTheme` aninhado devolveria o ripple que o tema do app tira.
            CompositionLocalProvider(LocalIndication provides indicacao, content = cabecalho)
        }
        Spacer(Modifier.height(EspacoDepoisDoCabecalho))
        Box(modifier = Modifier.padding(horizontal = Goteira)) { placa() }
        Spacer(Modifier.height(Espacos.bloco))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(RaioDaFolha)
                .background(esquema.background, RoundedCornerShape(topStart = RaioDaFolha, topEnd = RaioDaFolha)),
        )
    }
}

/** A placa de cada estado. Sem plano ela fica sem número; com o plano encerrado, carimbada. */
@Composable
private fun PlacaDaHome(estado: HomeUiState, aoAbrirPlano: ((String) -> Unit)?) {
    val locale = LocaleDoApp
    when (estado) {
        HomeUiState.Carregando, HomeUiState.SemPerfil -> EsqueletoDaPlaca()
        is HomeUiState.Ativo -> PlacaDoPlano(
            dados = estado.placa,
            tarja = stringResource(R.string.home_card_semana, estado.semana.numero, estado.semana.totalDeSemanas),
            aoTocar = aoAbrirPlano?.let { abrir -> { abrir(estado.planoId) } },
        )
        is HomeUiState.NaoIniciado -> PlacaDoPlano(
            dados = estado.placa,
            // A data de início mora na tarja; o corpo fica só com o resumo da semana 1.
            tarja = stringResource(
                R.string.home_placa_comeca,
                rotuloCurtoDoDia(estado.comecaEm.dayOfWeek, locale).lowercase(locale),
                estado.comecaEm.dayOfMonth,
                nomeDoMes(estado.comecaEm, locale).take(3),
            ),
            aoTocar = aoAbrirPlano?.let { abrir -> { abrir(estado.placa.planoId) } },
        )
        // A placa ainda sem número: a vaga tracejada dos carimbos, à espera de uma prova.
        HomeUiState.SemPlano -> PlacaDePeito(
            faixa = stringResource(R.string.home_placa_sem_prova).uppercase(locale),
            viva = false,
            descricao = stringResource(R.string.home_placa_nenhuma_prova),
            sobreODestaque = true,
        ) {
            VagaDoNumero(stringResource(R.string.home_placa_nenhuma_prova).uppercase(locale))
        }
        // RN-07: plano encerrado é somente-leitura e não reabre (RN-27). A placa fica,
        // carimbada, e é a porta do resumo congelado do `PlanDetailScreen` (D-05).
        is HomeUiState.Encerrado -> PlacaDePeito(
            faixa = faixaDaPlaca(estado.placa),
            viva = false,
            descricao = stringResource(R.string.home_placa_descricao_encerrado, estado.nomeDoPlano),
            aoTocar = aoAbrirPlano?.let { abrir -> { abrir(estado.placa.planoId) } },
            sobreODestaque = true,
            tarja = { TarjaDaPlaca(stringResource(R.string.home_placa_encerrado).uppercase(locale), estado.placa.arco) },
        ) {
            CarimboConcluido(stringResource(R.string.home_placa_concluido).uppercase(locale))
        }
        HomeUiState.Falhou -> Unit
    }
}

// ---------------------------------------------------------------------------
// Ativo: a semana, a última corrida, o grupo e a espécie
// ---------------------------------------------------------------------------

private fun LazyListScope.ativo(
    estado: HomeUiState.Ativo,
    aoAbrirSemana: (String, Int) -> Unit,
    aoAbrirCorrida: ((String) -> Unit)?,
    especie: CardDaEspecie?,
    aoAbrirPokedex: (() -> Unit)?,
    corridaRecente: String?,
    pulso: PulsoDoGrupo?,
    lendoPulso: Boolean,
    aoAbrirGrupo: (() -> Unit)?,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            CardDaSemana(
                semana = estado.semana,
                aoAbrir = { aoAbrirSemana(estado.planoId, estado.semana.numero) },
                aoAbrirCorrida = aoAbrirCorrida,
                corridaRecente = corridaRecente,
            )
        }
    }

    estado.ultimaCorrida?.let { corrida ->
        item {
            Spacer(Modifier.height(Espacos.interno))
            LinhaDaUltimaCorrida(corrida, aoAbrirCorrida)
        }
    }

    // A sua situação vem da semana, que escuta as suas corridas ao vivo; a dos outros, da
    // leitura única do pulso.
    if (pulso != null) {
        item {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(Espacos.interno))
                CartaoDoPulso(pulso.comVoce(correu = estado.semana.feitas > 0), aoAbrirGrupo)
            }
        }
    } else if (lendoPulso) {
        // O banner do pulso em espera, do tamanho dele: o card não pula para dentro da tela.
        item {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(Espacos.interno))
                CartoesEmEspera(listOf(AlturaDoPulso), cor = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    // `F2-T14`, D-19: a espécie atual, abaixo do plano (docs/03 §3.3).
    if (especie != null) {
        item {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(EspacoAntesDoJogo))
                // Na Fase 4 o card é a porta da aba Pokédex, quando ela já existe (docs/03 §3.3.2).
                FichaDaEspecieAtual(especie, aoTocar = aoAbrirPokedex)
            }
        }
    }
}

/**
 * A placa do plano aberto, ativo ou ainda por começar (docs/02 §4.5). [tarja] é a semana
 * corrente, ou o dia em que o plano começa.
 *
 * No dia da prova o número vira `HOJE`: o zero de uma contagem regressiva leria como
 * prazo vencido, e é o dia que o plano inteiro preparou.
 */
@Composable
private fun PlacaDoPlano(dados: DadosDaPlaca, tarja: String, aoTocar: (() -> Unit)?) {
    val locale = LocaleDoApp
    val dias = dados.diasAteAProva
    val hoje = dias == 0 && dados.semanaAtual != null
    val contagem = pluralStringResource(R.plurals.home_contagem_descricao, dias, dias, dados.nomeDoPlano)
    val descricao = if (hoje) {
        stringResource(R.string.home_placa_descricao_hoje, dados.nomeDoPlano, tarja)
    } else {
        stringResource(
            R.string.home_placa_descricao,
            contagem,
            dados.dataDaProva.dayOfMonth,
            nomeDoMes(dados.dataDaProva, locale),
            tarja,
        )
    }

    PlacaDePeito(
        faixa = faixaDaPlaca(dados),
        viva = true,
        descricao = descricao,
        aoTocar = aoTocar,
        sobreODestaque = true,
        tarja = { TarjaDaPlaca(tarja.uppercase(locale), dados.arco) },
    ) {
        if (hoje) {
            NumeroDaPlaca(
                numero = stringResource(R.string.home_placa_hoje).uppercase(locale),
                rotulo = stringResource(R.string.home_placa_hoje_rotulo).uppercase(locale),
            )
        } else {
            NumeroDaPlaca(
                numero = dias.toString(),
                rotulo = pluralStringResource(R.plurals.home_placa_rotulo, dias).uppercase(locale),
            )
        }
    }
}

/** `SÃO SILVESTRE 2026 · 31 DEZ`: o nome do plano e o dia da prova, como num número de peito. */
@Composable
private fun faixaDaPlaca(dados: DadosDaPlaca): String {
    val locale = LocaleDoApp
    val data = dados.dataDaProva
    val dia = "${data.dayOfMonth} ${nomeDoMes(data, locale).take(3)}"
    return stringResource(R.string.home_placa_faixa, dados.nomeDoPlano, dia).uppercase(locale)
}

/**
 * O card da semana, **sem anel** (docs/03 §3.3.1).
 *
 * A sobrancelha é só o período, `24 A 30 DE AGOSTO`: a semana e o total estão na tarja da
 * placa logo acima (recomposição de 18/10/2026). Depois vêm a fração e os carimbos.
 *
 * **O card inteiro é a porta da `WeekDetailScreen`** (`F1-T15`), que é a aresta
 * `HomeScreen → WeekDetailScreen` de docs/03 §1. O alvo é o card e não um botão: a tela
 * aberta é o mesmo conteúdo com a lista de corridas embaixo.
 */
@Composable
private fun CardDaSemana(
    semana: CardDaSemana,
    aoAbrir: () -> Unit,
    aoAbrirCorrida: ((String) -> Unit)? = null,
    corridaRecente: String? = null,
) {
    val locale = LocaleDoApp
    val periodo = periodoDaSemana(semana.primeiroDia, semana.ultimoDia).uppercase(locale)

    Ficha(aoTocar = aoAbrir) {
        Column(modifier = Modifier.padding(RecheioDaFicha)) {
            SobrancelhaDoCard(periodo)

            Spacer(Modifier.height(EspacoEntreBlocos))

            FracaoDeSessoes(feitas = semana.feitas, previstas = semana.previstas)

            Spacer(Modifier.height(EspacoEntreBlocos))

            // Os carimbos no lugar da barra e da grade de dias (delight de 17/10/2026): o
            // carimbo diz a sessão e o dia numa figura só.
            CarimbosDaSemana(
                segmentos = semana.segmentos,
                longaoKm = semana.longaoKm,
                longaoCumprido = semana.longaoCumprido,
                // docs/03 §3.3.1: o carimbo abre a corrida que o bateu (`F2-T09`). Nulo nos
                // previews, que não têm para onde ir.
                aoAbrirCorrida = aoAbrirCorrida,
                corridaRecente = corridaRecente,
            )

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

/**
 * A última corrida do plano (docs/03 §3.3), no vocabulário do detalhe da corrida
 * (29/10/2026): o selo do ritmo, o pace grande e, embaixo, o tempo e a distância. É um
 * bloco cheio em `superficie-afundada`, sem o dia, que o carimbo já diz. O bloco inteiro é
 * a porta da `RunDetailScreen`.
 */
@Composable
private fun LinhaDaUltimaCorrida(corrida: UltimaCorrida, aoAbrir: ((String) -> Unit)?) {
    val locale = LocaleDoApp
    val km = formatarKm(corrida.km, locale)
    val tempo = formatarDuracao(corrida.duracaoSeg, locale)
    val pace = formatarPace(corrida.km, corrida.duracaoSeg, locale)
    val ritmo = paceDe(corrida.km, corrida.duracaoSeg)?.let(::ritmoDe)
    val descricao = if (pace != null) {
        stringResource(R.string.home_ultima_corrida_descricao, km, tempo, pace)
    } else {
        stringResource(R.string.home_ultima_corrida_descricao_sem_pace, km, tempo)
    }
    val (inteiros, metros) = partesDaDistancia(corrida.km)
    val distancia = listOfNotNull(
        "$inteiros ${stringResource(R.string.corrida_detalhe_unidade_km)}".takeIf { inteiros > 0 || metros == 0 },
        "$metros ${stringResource(R.string.corrida_detalhe_unidade_metros)}".takeIf { metros > 0 },
    ).joinToString(" ")
    val toque = if (aoAbrir != null) Modifier.clickable { aoAbrir(corrida.id) } else Modifier

    Row(
        modifier = Modifier
            .padding(horizontal = Goteira)
            .fillMaxWidth()
            .clip(FormaDaUltimaCorrida)
            .background(LocalCoresPokerun.current.superficieAfundada)
            .then(toque)
            .heightIn(min = AlvoDeToque)
            .clearAndSetSemantics {
                contentDescription = descricao
                if (aoAbrir != null) role = Role.Button
            }
            .padding(start = Espacos.interno, end = Espacos.relacionado, top = Espacos.interno, bottom = Espacos.interno),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ritmo != null) {
            SeloDoRitmo(ritmo)
            Spacer(Modifier.width(Espacos.interno))
        }
        Column(modifier = Modifier.weight(1f)) {
            SobrancelhaDoCard(stringResource(R.string.home_ultima_corrida).uppercase(locale))
            Row {
                Text(
                    text = pace ?: tempo,
                    style = naFigura(Estilos.numeroL),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                if (pace != null) {
                    Spacer(Modifier.width(Espacos.minimo))
                    Text(
                        text = stringResource(R.string.corrida_detalhe_por_km).uppercase(locale),
                        style = naFigura(Estilos.rotulo),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Text(
                text = if (pace != null) "$tempo · $distancia" else distancia,
                style = naFigura(Estilos.apoio),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (aoAbrir != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val FormaDaUltimaCorrida = RoundedCornerShape(Raios.especie)

/**
 * O pulso do grupo (docs/03 §3.3): quem cumpriu ao menos uma sessão na semana, na ordem
 * de entrada no plano. Sem XP e sem posição, porque não é ranking.
 *
 * É o [BannerEscuro] (26/10/2026), irmão do banner do detalhe do plano: o seu Pokémon à
 * direita sobre o brilho em `destaque`, quantos correram, a pílula `Ver grupo` e os
 * avatares sobrepostos. Quem correu leva o anel em `destaque`; quem não correu, o anel na
 * cor do fundo.
 */
@Composable
private fun CartaoDoPulso(pulso: PulsoDoGrupo, aoAbrir: (() -> Unit)?) {
    val resumo = if (pulso.solo) {
        stringResource(R.string.home_grupo_solo)
    } else {
        pluralStringResource(R.plurals.home_grupo_contagem, pulso.correram, pulso.correram, pulso.membros.size)
    }
    // A pilha é de "quem correu": a pessoa, pelo busto do treinador (decisão 23).
    val avatares = pulso.membros.map { AvatarEmPilha(it.treinador, aceso = it.correu) }

    BannerEscuro(
        descricao = stringResource(R.string.home_grupo_descricao, resumo),
        aoTocar = aoAbrir,
        sprite = pulso.membros.firstOrNull { it.voce }?.sprite,
        alturaMinima = AlturaDoPulso,
    ) {
        SobrancelhaDoBanner(stringResource(R.string.home_grupo_rotulo))
        Spacer(Modifier.height(Espacos.minimo))
        TituloDoBanner(resumo)
        Spacer(Modifier.height(Espacos.interno))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (aoAbrir != null) {
                PilulaDoBanner(stringResource(R.string.detalhe_grupo_ver))
                Spacer(Modifier.width(Espacos.interno))
            }
            PilhaDeAvatares(avatares, fundo = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ---------------------------------------------------------------------------
// Os outros estados
// ---------------------------------------------------------------------------

private fun LazyListScope.naoIniciado(estado: HomeUiState.NaoIniciado) {
    item {
        val locale = LocaleDoApp
        val resumo = estado.primeiraSemana

        Column(modifier = Modifier.padding(horizontal = Goteira)) {
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(RecheioDaFicha),
                )
            }
        }
    }
}

private fun LazyListScope.semPlano(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    item {
        // A copy de docs/03 §3.3, ao pé da letra, sobre o padrão de Pokébolas (docs/02
        // §4.2), como todo estado vazio do app.
        EstadoVazio(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.home_sem_plano_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            AcoesDePlano(aoCriarPlano, aoEntrarComCodigo)
        }
    }
}

private fun LazyListScope.encerrado(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
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
            BotaoPrimario(
                aoTocar = aoTentarDeNovo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

/**
 * O esqueleto do card da semana (docs/02 §8, item 6); o da placa fica na camisa. O bloco
 * pulsa, para dizer que o app está lendo (27/10/2026).
 */
private fun LazyListScope.esqueleto() {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AlturaDoEsqueletoDoCard)
                    .esqueleto(MaterialTheme.shapes.medium),
            )
        }
    }
}

@Composable
private fun AcoesDePlano(aoCriarPlano: () -> Unit, aoEntrarComCodigo: () -> Unit) {
    BotaoPrimario(
        aoTocar = aoCriarPlano,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.home_criar_plano))
    }

    Spacer(Modifier.height(EspacoEntreBlocos))

    BotaoSecundario(
        aoTocar = aoEntrarComCodigo,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.home_entrar_com_codigo))
    }
}

@Composable
private fun SobrancelhaDoCard(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
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
 * As fábricas dos `ViewModel`s, lidas do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun especieAtualViewModel(): EspecieAtualViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        EspecieAtualViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.especiesRepositorio,
        )
    }
}

@Composable
private fun pulsoDoGrupoViewModel(): PulsoDoGrupoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        PulsoDoGrupoViewModel(
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.progressoRepositorio,
            container.temporadaRepositorio,
            container.especiesRepositorio,
        )
    }
}

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
private fun HomePreview(estado: HomeUiState, pulso: PulsoDoGrupo? = PULSO_DE_EXEMPLO, especie: CardDaEspecie? = null) {
    PokerunTheme {
        HomeScreen(
            estado = estado,
            aoAbrirPerfil = {},
            aoAbrirPlanos = {},
            aoCriarPlano = {},
            aoEntrarComCodigo = {},
            aoRetomarCadastro = {},
            aoAbrirSemana = { _, _ -> },
            aoTentarDeNovo = {},
            aoAbrirCorrida = {},
            pulso = pulso,
            especie = especie,
            aoAbrirPlano = {},
            aoAbrirGrupo = {},
        )
    }
}

/**
 * O arco da São Silvestre de exemplo: longão de 5 km a 16,5 km na semana 18, taper de
 * 9,9 km e sem longão, e a semana da prova. É a grade de docs/01 §3, sintética.
 */
private fun arcoDeExemplo(atual: Int?, encerrado: Boolean = false): List<BarraDoArco> = (1..21).map { n ->
    val longao = when {
        n <= 18 -> 5.0 + (16.5 - 5.0) * (n - 1) / 17
        n == 19 -> 9.9
        n == 21 -> 15.0
        else -> null
    }
    BarraDoArco(
        numero = n,
        longaoKm = longao,
        taper = n == 19 || n == 20,
        situacao = when {
            encerrado -> SituacaoDaBarra.PASSADA
            atual == null -> SituacaoDaBarra.FUTURA
            n < atual -> SituacaoDaBarra.PASSADA
            n == atual -> SituacaoDaBarra.ATUAL
            else -> SituacaoDaBarra.FUTURA
        },
    )
}

private fun placaDeExemplo(dias: Int, atual: Int?, encerrado: Boolean = false) = DadosDaPlaca(
    planoId = "plano-1",
    nomeDoPlano = "São Silvestre 2026",
    dataDaProva = LocalDate.of(2026, 12, 31),
    diasAteAProva = dias,
    arco = arcoDeExemplo(atual, encerrado),
)

private val SEMANA_DE_EXEMPLO = CardDaSemana(
    numero = 10,
    totalDeSemanas = 21,
    primeiroDia = LocalDate.of(2026, 10, 12),
    ultimoDia = LocalDate.of(2026, 10, 18),
    feitas = 2,
    previstas = 3,
    longaoKm = 11.0,
    longaoCumprido = false,
    segmentos = listOf(
        SegmentoDaSemana(SessaoReivindicada.Curta(1), "run-1", 6.2, LocalDate.of(2026, 10, 12)),
        SegmentoDaSemana(SessaoReivindicada.Curta(2), "run-2", 5.8, LocalDate.of(2026, 10, 14)),
        SegmentoDaSemana(SessaoReivindicada.Longao),
    ),
    dias = (12..18).map { DiaDoTreino(LocalDate.of(2026, 10, it), if (it == 12 || it == 14) 1 else 0, it == 14) },
)

private val ATIVO_DE_EXEMPLO = HomeUiState.Ativo(
    planoId = "plano-1",
    placa = placaDeExemplo(dias = 78, atual = 10),
    semana = SEMANA_DE_EXEMPLO,
    ultimaCorrida = UltimaCorrida(id = "run-2", km = 5.8, duracaoSeg = 2158),
)

/** Oito membros sintéticos, sem sprite (os previews não têm rede). */
private val PULSO_DE_EXEMPLO = PulsoDoGrupo(
    listOf("Você", "Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabi").mapIndexed { i, nome ->
        MembroDoPulso(uid = "uid-$i", nome = nome, voce = i == 0, correu = i in setOf(0, 1, 2, 3, 5), sprite = null)
    },
)

@Preview(name = "Ativo", showBackground = true, heightDp = 900)
@Composable
private fun AtivoPreview() = HomePreview(ATIVO_DE_EXEMPLO)

/** A espécie de exemplo, sem arte (os previews não têm rede). */
private val ESPECIE_DE_EXEMPLO = CardDaEspecie(
    numero = 26,
    nome = "Raichu",
    proximoNumero = 27,
    proximoNome = "Sandshrew",
    faltamXp = 900,
    silhueta = false,
    sprite = "",
)

@Preview(name = "Ativo · com a espécie", showBackground = true, heightDp = 1500)
@Composable
private fun ComEspeciePreview() = HomePreview(ATIVO_DE_EXEMPLO, especie = ESPECIE_DE_EXEMPLO)

@Preview(name = "Ativo · semana sem corrida", showBackground = true, heightDp = 900)
@Composable
private fun SemanaVaziaPreview() = HomePreview(
    ATIVO_DE_EXEMPLO.copy(
        semana = SEMANA_DE_EXEMPLO.copy(
            feitas = 0,
            segmentos = SEMANA_DE_EXEMPLO.segmentos.map { it.copy(corridaId = null, km = null, dia = null) },
            dias = SEMANA_DE_EXEMPLO.dias.map { it.copy(corridas = 0) },
        ),
        ultimaCorrida = null,
    ),
)

@Preview(name = "Ativo · plano solo", showBackground = true, heightDp = 900)
@Composable
private fun SoloPreview() = HomePreview(
    ATIVO_DE_EXEMPLO,
    pulso = PulsoDoGrupo(listOf(MembroDoPulso("uid-0", "Você", voce = true, correu = true, sprite = null))),
)

@Preview(name = "Ativo · dia da prova", showBackground = true, heightDp = 900)
@Composable
private fun DiaDaProvaPreview() = HomePreview(
    HomeUiState.Ativo(
        planoId = "plano-1",
        placa = placaDeExemplo(dias = 0, atual = 21),
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
            dias = (28..31).map { DiaDoTreino(LocalDate.of(2026, 12, it), 0, it == 31) },
        ),
    ),
)

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun CarregandoPreview() = HomePreview(HomeUiState.Carregando)

@Preview(name = "Sem plano", showBackground = true, heightDp = 900)
@Composable
private fun SemPlanoPreview() = HomePreview(HomeUiState.SemPlano)

@Preview(name = "Não iniciado", showBackground = true)
@Composable
private fun NaoIniciadoPreview() = HomePreview(
    HomeUiState.NaoIniciado(
        placa = placaDeExemplo(dias = 143, atual = null),
        comecaEm = LocalDate.of(2026, 8, 10),
        primeiraSemana = ResumoDaSemana(numero = 1, sessoes = 3, kmAlvo = 10.0, longaoKm = 5.0),
    ),
)

@Preview(name = "Encerrado", showBackground = true, heightDp = 900)
@Composable
private fun EncerradoPreview() = HomePreview(HomeUiState.Encerrado(placaDeExemplo(dias = 0, atual = null, encerrado = true)))

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = HomePreview(HomeUiState.Falhou)

@Preview(name = "Ativo em fontScale 2,0", showBackground = true, fontScale = 2.0f, widthDp = 320, heightDp = 1400)
@Composable
private fun AtivoFonteGrandePreview() = HomePreview(ATIVO_DE_EXEMPLO)

@Preview(name = "Ativo em 320dp", showBackground = true, widthDp = 320, heightDp = 900)
@Composable
private fun AtivoEstreitoPreview() = HomePreview(ATIVO_DE_EXEMPLO)
