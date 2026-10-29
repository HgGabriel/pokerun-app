package com.hggabriel.pokerun.ui.telas.criarplano

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Viabilidade
import com.hggabriel.pokerun.ui.componentes.IconesClarosNaBarraDeStatus
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.EscolhaCheia
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.navegacao.RevisarRascunho
import com.hggabriel.pokerun.ui.telas.home.CardDaEspecie
import com.hggabriel.pokerun.ui.telas.home.EspecieAtualViewModel
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val EspacoEntreCartoes = 12.dp

/**
 * O formulário de criação de plano (`F1-T10`, docs/03 §3.5, DA-10), redesenhado em
 * 27/10/2026 na segunda linguagem (docs/02 §1).
 *
 * **Cinco parâmetros, e nenhum a mais** (D-02, D-14): nome e data da prova, distância da
 * prova, distância confortável, sessões por semana e a distância das corridas curtas. Cada
 * um é algo que o corredor sabe; o app calcula de onde o longão começa e até onde vai.
 * **Não existe campo de semana leve nem de volume**: o volume sai do longão e da curta
 * (docs/01 §3.2).
 *
 * ### A montanha e os cartões
 *
 * Em cima, a [MontanhaDoPlano] em tinta: o nome se digita como o título, e a curva do longão
 * sobe enquanto a data e as distâncias chegam, com o Pokémon da temporada no pico. Embaixo,
 * na página tingida, dois cartões: **a prova** (data, distância e os atalhos de 5K a 42K) e
 * **você hoje** (a confortável, as sessões e a curta). O botão fica fixo no pé.
 *
 * **Plano que não cabe não sai daqui.** `Gerar plano` confere se o longão chega a 75% da
 * prova sem subir mais de 10% por semana; se não chega, a tela diz o motivo e oferece as
 * duas saídas, cada uma com um toque. A montanha mostra o mesmo antes do toque: a linha do
 * mínimo acima do pico.
 *
 * **A distância confortável chega pré-preenchida** com o `baseline_km` do cadastro. A
 * pergunta já foi feita uma vez, e repeti-la em branco é o jeito mais rápido de alguém
 * responder diferente das duas vezes.
 *
 * **A tela não grava nada.** `Gerar plano` valida e sai para a revisão do rascunho
 * (`F1-T11`), que é quem escreve depois de o usuário conferir a grade.
 *
 * **Sem rede, o botão fica indisponível e a tela diz por quê.** A reserva do código de
 * convite é transacional (RN-29) e não resolve no cache.
 *
 * Erro de campo e falha de tela saem por [BannerDeAlerta] (docs/02 §2.4); a caixa com erro
 * ganha o contorno em `destaque`, e nenhuma cor de alerta é pintada aqui.
 */
@Composable
fun CriarPlanoScreen(
    aoGerar: (RevisarRascunho) -> Unit,
    modifier: Modifier = Modifier,
    vm: CriarPlanoViewModel = criarPlanoViewModel(),
    especieVm: EspecieAtualViewModel = especieDaMontanhaViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val especie by especieVm.card.collectAsStateWithLifecycle()

    LaunchedEffect(estado.rascunho) {
        val rascunho = estado.rascunho ?: return@LaunchedEffect
        vm.rascunhoConsumido()
        aoGerar(rascunho)
    }

    CriarPlanoScreen(
        estado = estado,
        especie = especie,
        aoMudarNome = vm::nomeMudou,
        aoMudarAlvo = vm::alvoMudou,
        aoMudarBaseline = vm::baselineMudou,
        aoMudarSessoes = vm::sessoesMudaram,
        aoMudarCurta = vm::curtaMudou,
        aoUsarPrimeiraData = vm::usarPrimeiraDataQueCabe,
        aoUsarMaiorAlvo = vm::usarMaiorAlvoQueCabe,
        aoAbrirCalendario = vm::abrirCalendario,
        aoFecharCalendario = vm::fecharCalendario,
        aoEscolherData = vm::dataEscolhida,
        aoGerarPlano = vm::gerarPlano,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun CriarPlanoScreen(
    estado: CriarPlanoUiState,
    aoMudarNome: (String) -> Unit,
    aoMudarAlvo: (String) -> Unit,
    aoMudarBaseline: (String) -> Unit,
    aoMudarSessoes: (Int) -> Unit,
    aoMudarCurta: (String) -> Unit,
    aoUsarPrimeiraData: () -> Unit,
    aoUsarMaiorAlvo: () -> Unit,
    aoAbrirCalendario: () -> Unit,
    aoFecharCalendario: () -> Unit,
    aoEscolherData: (LocalDate) -> Unit,
    aoGerarPlano: () -> Unit,
    modifier: Modifier = Modifier,
    especie: CardDaEspecie? = null,
) {
    val pagina = LocalCoresPokerun.current.superficieAfundada
    val ancora = remember { FocusRequester() }
    val alvo = remember { FocusRequester() }
    val baseline = remember { FocusRequester() }
    val curta = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    val primeiro = primeiroErro(estado)

    IconesClarosNaBarraDeStatus()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pagina),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                MontanhaDoPlano(
                    estado = estado,
                    especie = especie,
                    aoMudarNome = aoMudarNome,
                    ancora = ancora,
                    proximo = alvo,
                )

                Column(
                    modifier = Modifier.padding(horizontal = Espacos.margem, vertical = EspacoEntreCartoes),
                    verticalArrangement = Arrangement.spacedBy(EspacoEntreCartoes),
                ) {
                    // O erro do nome e o plano que não cabe saem no primeiro lugar claro.
                    estado.erros.nome?.let { erro ->
                        Visivel(primeiro == Campo.NOME, estado.tentativa) { modificador ->
                            BannerDeAlerta(
                                rotulo = stringResource(R.string.alerta_corrija),
                                texto = stringResource(erro),
                                modifier = modificador,
                            )
                        }
                    }
                    estado.naoCabe?.let { naoCabe ->
                        Visivel(primeiro == Campo.NAO_CABE, estado.tentativa) { modificador ->
                            PlanoNaoCabe(
                                naoCabe = naoCabe,
                                alvo = estado.alvo,
                                aoUsarPrimeiraData = aoUsarPrimeiraData,
                                aoUsarMaiorAlvo = aoUsarMaiorAlvo,
                                modifier = modificador,
                            )
                        }
                    }

                    CartaoDaProva(
                        estado = estado,
                        primeiro = primeiro,
                        aoAbrirCalendario = aoAbrirCalendario,
                        aoMudarAlvo = aoMudarAlvo,
                        alvo = alvo,
                        aoProximo = { baseline.requestFocus() },
                    )
                    CartaoVoceHoje(
                        estado = estado,
                        primeiro = primeiro,
                        aoMudarBaseline = aoMudarBaseline,
                        aoMudarSessoes = aoMudarSessoes,
                        aoMudarCurta = aoMudarCurta,
                        baseline = baseline,
                        curta = curta,
                        aoConcluir = {
                            teclado?.hide()
                            ancora.requestFocus()
                        },
                    )

                    if (!estado.online) {
                        Text(
                            text = stringResource(R.string.criar_offline),
                            style = Estilos.apoio,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Espacos.margem),
                        )
                    }
                }
            }

            BotaoPrimario(
                aoTocar = aoGerarPlano,
                habilitado = estado.online,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espacos.margem, vertical = Espacos.relacionado),
            ) {
                Text(stringResource(R.string.criar_gerar))
            }
        }
    }

    if (estado.escolhendoData) {
        CalendarioDaProva(
            escolhida = estado.dataProva,
            aoFechar = aoFecharCalendario,
            aoEscolher = aoEscolherData,
        )
    }
}

// ---------------------------------------------------------------------------
// Os cartões
// ---------------------------------------------------------------------------

/** A prova: a data (a linha abre o calendário), a distância e os atalhos das provas de rua. */
@Composable
private fun CartaoDaProva(
    estado: CriarPlanoUiState,
    primeiro: Campo?,
    aoAbrirCalendario: () -> Unit,
    aoMudarAlvo: (String) -> Unit,
    alvo: FocusRequester,
    aoProximo: () -> Unit,
) {
    val alvoKm = distanciaEmKm(estado.alvo)
    Cartao {
        RotuloDoCartao(stringResource(R.string.criar_a_prova))
        Spacer(Modifier.height(Espacos.interno))
        LinhaDaData(estado.dataProva, aoAbrirCalendario)
        estado.erros.data?.let { erro ->
            Spacer(Modifier.height(Espacos.relacionado))
            Visivel(primeiro == Campo.DATA, estado.tentativa) { modificador ->
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_corrija),
                    texto = stringResource(erro),
                    modifier = modificador,
                )
            }
        }

        Spacer(Modifier.height(Espacos.margem))
        LinhaDeKm(
            rotulo = stringResource(R.string.criar_distancia),
            apoio = null,
            valor = estado.alvo,
            descricao = stringResource(R.string.criar_campo_alvo),
            erro = estado.erros.alvo,
            trazer = primeiro == Campo.ALVO,
            tentativa = estado.tentativa,
            aoMudar = aoMudarAlvo,
            foco = alvo,
            acao = ImeAction.Next,
            aoAcao = aoProximo,
        )
        Spacer(Modifier.height(Espacos.relacionado))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
            ATALHOS_DA_PROVA.forEach { (rotulo, km) ->
                EscolhaCheia(
                    texto = rotulo,
                    selecionada = alvoKm != null && alvoKm == distanciaEmKm(km),
                    aoTocar = { aoMudarAlvo(km) },
                )
            }
        }
    }
}

/** Você hoje: a maior distância confortável, as sessões por semana e a curta. */
@Composable
private fun CartaoVoceHoje(
    estado: CriarPlanoUiState,
    primeiro: Campo?,
    aoMudarBaseline: (String) -> Unit,
    aoMudarSessoes: (Int) -> Unit,
    aoMudarCurta: (String) -> Unit,
    baseline: FocusRequester,
    curta: FocusRequester,
    aoConcluir: () -> Unit,
) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.criar_voce_hoje))
        Spacer(Modifier.height(Espacos.interno))
        LinhaDeKm(
            rotulo = stringResource(R.string.criar_campo_baseline),
            apoio = stringResource(R.string.criar_campo_baseline_apoio),
            valor = estado.baseline,
            descricao = stringResource(R.string.criar_campo_baseline),
            erro = estado.erros.baseline,
            trazer = primeiro == Campo.BASELINE,
            tentativa = estado.tentativa,
            aoMudar = aoMudarBaseline,
            foco = baseline,
            acao = ImeAction.Next,
            aoAcao = { curta.requestFocus() },
        )

        // 2, 3 ou 4 (docs/01 §3.1): é seletor e não campo livre, porque o gerador recusa
        // qualquer outro número.
        Spacer(Modifier.height(Espacos.bloco))
        Text(
            text = stringResource(R.string.criar_campo_sessoes),
            style = Estilos.corpoForte,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Espacos.relacionado))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
            CriarPlanoUiState.SESSOES.forEach { sessoes ->
                EscolhaCheia(
                    texto = sessoes.toString(),
                    selecionada = estado.sessoesPorSemana == sessoes,
                    aoTocar = { aoMudarSessoes(sessoes) },
                )
            }
        }

        Spacer(Modifier.height(Espacos.bloco))
        LinhaDeKm(
            rotulo = stringResource(R.string.criar_curtas),
            apoio = stringResource(R.string.criar_campo_curta_apoio),
            valor = estado.curta,
            descricao = stringResource(R.string.criar_campo_curta),
            erro = estado.erros.curta,
            trazer = primeiro == Campo.CURTA,
            tentativa = estado.tentativa,
            aoMudar = aoMudarCurta,
            foco = curta,
            acao = ImeAction.Done,
            aoAcao = aoConcluir,
        )
    }
}

/** O rótulo à esquerda, com o apoio embaixo, e a distância em número grande à direita. */
@Composable
private fun LinhaDeKm(
    rotulo: String,
    apoio: String?,
    valor: String,
    descricao: String,
    erro: Int?,
    trazer: Boolean,
    tentativa: Int,
    aoMudar: (String) -> Unit,
    foco: FocusRequester,
    acao: ImeAction,
    aoAcao: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rotulo, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface)
                if (apoio != null) {
                    Text(text = apoio, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(Espacos.interno))
            CampoDeKm(
                valor = valor,
                aoMudar = aoMudar,
                descricao = descricao,
                erro = erro != null,
                foco = foco,
                acao = acao,
                aoAcao = aoAcao,
            )
        }
        if (erro != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            Visivel(trazer, tentativa) { modificador ->
                BannerDeAlerta(
                    rotulo = stringResource(R.string.alerta_corrija),
                    texto = stringResource(erro),
                    modifier = modificador,
                )
            }
        }
    }
}

/** Os lugares de erro da tela, na ordem em que aparecem de cima para baixo. */
private enum class Campo { NOME, NAO_CABE, DATA, ALVO, BASELINE, CURTA }

private fun primeiroErro(estado: CriarPlanoUiState): Campo? = when {
    estado.erros.nome != null -> Campo.NOME
    estado.naoCabe != null -> Campo.NAO_CABE
    estado.erros.data != null -> Campo.DATA
    estado.erros.alvo != null -> Campo.ALVO
    estado.erros.baseline != null -> Campo.BASELINE
    estado.erros.curta != null -> Campo.CURTA
    else -> null
}

/**
 * Leva o alerta até a vista quando ele é o primeiro erro da tela. O botão fica fixo no pé, e
 * o erro da curta nasce abaixo da dobra: sem isto, o toque em `Gerar plano` não mostrava nada.
 * Roda a cada toque ([tentativa]), e não só quando o erro muda.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Visivel(trazer: Boolean, tentativa: Int, conteudo: @Composable (Modifier) -> Unit) {
    val requisitor = remember { BringIntoViewRequester() }
    LaunchedEffect(trazer, tentativa) { if (trazer) requisitor.bringIntoView() }
    conteudo(Modifier.bringIntoViewRequester(requisitor))
}

/**
 * A data da prova: a linha inteira abre o calendário, e a data sai em número. **Digitar data
 * à mão continua fora**: `03/04` é março ou abril conforme quem digita.
 */
@Composable
private fun LinhaDaData(data: LocalDate?, aoAbrir: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val rotulo = stringResource(R.string.criar_campo_data)
    val texto = data?.let { stringResource(R.string.criar_data_curta, it.dayOfMonth, nomeCurtoDoMes(it), it.year) }
    val falado = data?.let { stringResource(R.string.criar_data_escolhida, it.dayOfMonth, nomeDoMes(it), it.year) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FormaControle)
            .background(LocalCoresPokerun.current.superficieAfundada)
            .clickable(role = Role.Button, onClick = aoAbrir)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(rotulo, falado).joinToString(", ")
                role = Role.Button
            }
            .padding(horizontal = Espacos.margem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Calendario(tinta)
        Spacer(Modifier.width(Espacos.interno))
        Text(
            text = stringResource(R.string.criar_data),
            style = Estilos.corpoForte,
            color = tinta,
            modifier = Modifier.weight(1f),
        )
        if (texto != null) {
            Text(text = texto, style = Estilos.numeroM, color = tinta)
        } else {
            Text(
                text = stringResource(R.string.criar_escolher),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** O calendário desenhado: a folha, as duas argolas e a linha do cabeçalho. */
@Composable
private fun Calendario(cor: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val traco = 2.dp.toPx()
        val topo = 3.dp.toPx()
        drawRoundRect(
            color = cor,
            topLeft = Offset(traco / 2, topo),
            size = Size(size.width - traco, size.height - topo - traco / 2),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(traco),
        )
        drawLine(cor, Offset(traco, topo + 5.dp.toPx()), Offset(size.width - traco, topo + 5.dp.toPx()), traco)
        listOf(0.32f, 0.68f).forEach { f ->
            drawLine(cor, Offset(size.width * f, 0f), Offset(size.width * f, topo + 2.dp.toPx()), traco)
        }
    }
}

/**
 * O plano não cabe até a prova (docs/01 §3.2): o motivo, em números, e as duas saídas.
 *
 * Vai pelo canal de alerta porque bloqueia `Gerar plano` até o usuário mexer em algo. As
 * saídas são botões e não sugestão em texto: trocar a data ou a distância à mão pediria
 * que a pessoa fizesse a conta que o app já fez.
 */
@Composable
private fun PlanoNaoCabe(
    naoCabe: Viabilidade.NaoCabe,
    alvo: String,
    aoUsarPrimeiraData: () -> Unit,
    aoUsarMaiorAlvo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = naoCabe.primeiraProvaQueCabe
    Cartao(modifier = modifier) {
        BannerDeAlerta(
            rotulo = stringResource(R.string.criar_nao_cabe_rotulo),
            texto = stringResource(
                R.string.criar_nao_cabe_texto,
                formatarKm(naoCabe.picoKm),
                alvo,
                formatarKm(naoCabe.picoMinimoKm),
            ),
        )
        Spacer(Modifier.height(Espacos.interno))
        BotaoSecundario(aoTocar = aoUsarPrimeiraData, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.criar_nao_cabe_data, data.dayOfMonth, nomeDoMes(data), data.year))
        }
        Spacer(Modifier.height(Espacos.relacionado))
        BotaoSecundario(aoTocar = aoUsarMaiorAlvo, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.criar_nao_cabe_alvo, formatarKm(naoCabe.maiorAlvoQueCabeKm)))
        }
    }
}

/** O parceiro (estudo de Progresso e Perfil, d.2), para o pico da montanha: o `ViewModel` do card da Home no modo parceiro. */
@Composable
private fun especieDaMontanhaViewModel(): EspecieAtualViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        EspecieAtualViewModel(
            container.autenticacaoRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.especiesRepositorio,
            usuariosDoParceiro = container.usuarioRepositorio,
        )
    }
}

/**
 * O calendário da prova.
 *
 * **O `DatePicker` fala em milissegundos UTC**, e converter isso com o fuso do aparelho
 * move a data em um dia para quem está a oeste de Greenwich: a meia-noite UTC do dia 31
 * é o dia 30 às 21h em São Paulo. A conversão é em `ZoneOffset.UTC` nos dois sentidos,
 * porque o que o componente devolve é uma **data de calendário** disfarçada de instante,
 * e não um momento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioDaProva(
    escolhida: LocalDate?,
    aoFechar: () -> Unit,
    aoEscolher: (LocalDate) -> Unit,
) {
    val estadoDoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = escolhida
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = aoFechar,
        confirmButton = {
            BotaoDeTexto(
                aoTocar = {
                    estadoDoCalendario.selectedDateMillis?.let { millis ->
                        aoEscolher(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
                habilitado = estadoDoCalendario.selectedDateMillis != null,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            BotaoDeTexto(aoTocar = aoFechar) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DatePicker(state = estadoDoCalendario)
    }
}

/**
 * A fábrica do `ViewModel`, lida do [LocalAppContainer] no ponto de chamada.
 *
 * O `ViewModel` recebe os repositórios pelo construtor e nunca enxerga o
 * `CompositionLocal`: ele sobrevive à composição que o proveu.
 */
@Composable
private fun criarPlanoViewModel(): CriarPlanoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        CriarPlanoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.conectividadeRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun CriarPlanoPreview(estado: CriarPlanoUiState) {
    PokerunTheme {
        CriarPlanoScreen(
            estado = estado,
            aoMudarNome = {},
            aoMudarAlvo = {},
            aoMudarBaseline = {},
            aoMudarSessoes = {},
            aoMudarCurta = {},
            aoUsarPrimeiraData = {},
            aoUsarMaiorAlvo = {},
            aoAbrirCalendario = {},
            aoFecharCalendario = {},
            aoEscolherData = {},
            aoGerarPlano = {},
        )
    }
}

private val PREENCHIDO = CriarPlanoUiState(
    nome = "São Silvestre",
    dataProva = LocalDate.of(2026, 12, 31),
    alvo = "15",
    baseline = "10",
    baselinePreenchida = true,
    curta = "3",
    semanas = 10,
)

@Preview(name = "Vazio, com a baseline do cadastro", showBackground = true)
@Composable
private fun VazioPreview() =
    CriarPlanoPreview(CriarPlanoUiState(baseline = "5", baselinePreenchida = true))

@Preview(name = "Preenchido", showBackground = true)
@Composable
private fun PreenchidoPreview() = CriarPlanoPreview(PREENCHIDO)

@Preview(name = "Com os erros", showBackground = true)
@Composable
private fun ComErrosPreview() = CriarPlanoPreview(
    CriarPlanoUiState(
        alvo = "",
        baseline = "5",
        erros = ErrosDoPlano(
            nome = R.string.criar_erro_nome,
            data = R.string.criar_erro_data_curta,
            alvo = R.string.criar_erro_distancia,
            curta = R.string.criar_erro_distancia,
        ),
    ),
)

@Preview(name = "Não cabe", showBackground = true)
@Composable
private fun NaoCabePreview() = CriarPlanoPreview(
    PREENCHIDO.copy(
        baseline = "5",
        naoCabe = Viabilidade.NaoCabe(
            semanas = 10,
            picoKm = 9.74,
            picoMinimoKm = 11.25,
            primeiraProvaQueCabe = LocalDate.of(2027, 1, 11),
            maiorAlvoQueCabeKm = 12.5,
        ),
    ),
)

@Preview(name = "Offline", showBackground = true)
@Composable
private fun OfflinePreview() = CriarPlanoPreview(PREENCHIDO.copy(online = false))

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = CriarPlanoPreview(PREENCHIDO)
