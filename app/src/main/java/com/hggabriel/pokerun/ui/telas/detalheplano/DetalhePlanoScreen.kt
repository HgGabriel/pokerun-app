package com.hggabriel.pokerun.ui.telas.detalheplano

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.SituacaoDoPlano
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.ui.componentes.BannerDeAlerta
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.CampoComErro
import com.hggabriel.pokerun.ui.componentes.CarimbosDaSemana
import com.hggabriel.pokerun.ui.componentes.ColunaDoTrilho
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.FaseDoPlano
import com.hggabriel.pokerun.ui.componentes.Ficha
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.MarcaDeSituacao
import com.hggabriel.pokerun.ui.componentes.TempoDaSemana
import com.hggabriel.pokerun.ui.componentes.TrilhoDoLongao
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.geometriaDoTrilho
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.telas.revisarrascunho.textoDasMudancas
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.FormaDado
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.Raios
import com.hggabriel.pokerun.ui.theme.Tamanhos

/** A goteira do corpo, a mesma do cabeçalho de ficha. */
private val Goteira = Espacos.margem

/** Do filete do cabeçalho ao número principal, como no sketch r2. */
private val EspacoAntesDoNumero = 20.dp

/** O sprite de jogo transborda o avatar de 48dp: 96px de origem, a espécie lê no miolo. */
private val SpriteDoAvatar = 64.dp

private val AlturaDaCelula = 24.dp
private val TracoDaCelula = 1.5.dp
private val MolduraDaSelecao = 2.dp

/** Acima disto o número de `numero-l` não cabe em 320dp: cai um degrau (docs/02 §8, item 9). */
private const val ESCALA_QUE_CAI_O_NUMERO = 1.5f

/** A folga no fim da lista, para o último bloco não colar na borda inferior. */
private val FolgaDoFim = Espacos.bloco

/**
 * O que a tela pode pedir ao `ViewModel`. Um portador só, com tudo vazio por padrão,
 * para os previews não repetirem quinze lambdas.
 */
class AcoesDoDetalhe(
    val aoEditarSemana: (Semana) -> Unit = {},
    val aoMudarLongao: (String) -> Unit = {},
    val aoCancelarEdicao: () -> Unit = {},
    val aoConfirmarEdicao: () -> Unit = {},
    val aoDesfazerEdicao: () -> Unit = {},
    val aoAbrirCurta: () -> Unit = {},
    val aoMudarCurta: (String) -> Unit = {},
    val aoCancelarCurta: () -> Unit = {},
    val aoConfirmarCurta: () -> Unit = {},
    val aoPedirEncerrar: () -> Unit = {},
    val aoCancelarEncerrar: () -> Unit = {},
    val aoConfirmarEncerrar: () -> Unit = {},
    val aoTentarDeNovo: () -> Unit = {},
    val aoSelecionarSemana: (Int) -> Unit = {},
    val aoSemanaAnterior: () -> Unit = {},
    val aoProximaSemana: () -> Unit = {},
    val aoAbrirCorrida: (String) -> Unit = {},
)

/**
 * O detalhe do plano (`F1-T13`, r2 de 26/10/2026, docs/03 §3.7).
 *
 * **O plano inteiro lê numa figura, e a edição chega à semana pela régua** (D6). De cima
 * para baixo: o número principal, que muda com a situação (D2); a curta, com `Mudar` só para
 * o dono (D4); o gráfico do longão com a régua de semanas; o card da semana selecionada,
 * com os seus carimbos e o lápis no longão editável (D7); o grupo, com a fração e as
 * células de cada membro; o convite (D3) e o encerrar.
 *
 * **A tela é a mesma para todo mundo; o que muda é o que se pode fazer nela.** As
 * permissões vêm do estado e têm teste. Nenhuma delas é uma condição escrita aqui dentro.
 *
 * **O vermelho é sempre de quem olha.** Na visão do membro, a barra vermelha e as células
 * vermelhas são dele, e o dono vira o traço em tinta.
 */
@Composable
fun DetalhePlanoScreen(
    planoId: String,
    aoAbrirCorrida: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: DetalhePlanoViewModel = detalhePlanoViewModel(planoId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val acoes = remember(vm, aoAbrirCorrida) {
        AcoesDoDetalhe(
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
            aoSelecionarSemana = vm::selecionarSemana,
            aoSemanaAnterior = vm::semanaAnterior,
            aoProximaSemana = vm::proximaSemana,
            aoAbrirCorrida = aoAbrirCorrida,
        )
    }

    DetalhePlanoScreen(estado = estado, acoes = acoes, modifier = modifier)
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun DetalhePlanoScreen(
    estado: DetalhePlanoUiState,
    acoes: AcoesDoDetalhe,
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
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.detalhe_sobrancelha)),
                    titulo = (estado as? DetalhePlanoUiState.Conteudo)?.nome.orEmpty(),
                )
            }

            when (estado) {
                DetalhePlanoUiState.Carregando -> esqueleto()
                DetalhePlanoUiState.Falhou -> falhou(acoes.aoTentarDeNovo)
                is DetalhePlanoUiState.Conteudo -> conteudo(estado, acoes)
            }
        }
    }

    val conteudo = estado as? DetalhePlanoUiState.Conteudo

    conteudo?.editando?.let { edicao ->
        DialogoDoLongao(edicao, acoes.aoMudarLongao, acoes.aoCancelarEdicao, acoes.aoConfirmarEdicao, acoes.aoDesfazerEdicao)
    }

    conteudo?.editandoCurta?.let { curta ->
        DialogoDaCurta(curta, acoes.aoMudarCurta, acoes.aoCancelarCurta, acoes.aoConfirmarCurta)
    }

    if (conteudo?.confirmandoEncerrar == true) {
        DialogoDeEncerrar(acoes.aoCancelarEncerrar, acoes.aoConfirmarEncerrar)
    }
}

// ---------------------------------------------------------------------------
// O conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.conteudo(estado: DetalhePlanoUiState.Conteudo, acoes: AcoesDoDetalhe) {
    item(key = "topo") { Topo(estado, acoes.aoAbrirCurta) }
    item(key = "trilho") { BlocoDoTrilho(estado, acoes.aoSelecionarSemana) }
    item(key = "semana") { BlocoDaSemana(estado, acoes) }
    item(key = "grupo") { Grupo(estado) }

    if (estado.mostraConvite) {
        item(key = "convite") { CodigoDeConvite(estado.codigoConvite) }
    }

    if (estado.podeEncerrar) {
        item(key = "encerrar") {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(Espacos.secao))
                BotaoSecundario(aoTocar = acoes.aoPedirEncerrar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detalhe_encerrar))
                }
            }
        }
    }
}

/**
 * O número principal, a etiqueta da situação, a frase de regra e a curta (D2, D4, D5).
 *
 * A frase só aparece no guardado e no encerrado, e só com a regra: a etiqueta já diz
 * `GUARDADO` ou `ENCERRADO`. A troca de plano ativo continua só na lista de planos (D5).
 */
@Composable
private fun Topo(estado: DetalhePlanoUiState.Conteudo, aoAbrirCurta: () -> Unit) {
    val escala = LocalDensity.current.fontScale
    val empilha = escala > ESCALA_QUE_EMPILHA

    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Spacer(Modifier.height(EspacoAntesDoNumero))

        if (empilha) {
            // Acima de 1,3 a etiqueta sobe para cima do número, e o número fica com a linha.
            MarcaDeSituacao(estado.situacao)
            Spacer(Modifier.height(Espacos.relacionado))
            NumeroPrincipal(estado.numeroPrincipal, escala)
        } else {
            Row(verticalAlignment = Alignment.Top) {
                NumeroPrincipal(estado.numeroPrincipal, escala, Modifier.weight(1f))
                Spacer(Modifier.width(Espacos.interno))
                MarcaDeSituacao(estado.situacao, Modifier.padding(top = 6.dp))
            }
        }

        val regra = when (estado.situacao) {
            SituacaoDoPlano.ENCERRADO -> R.string.detalhe_encerrado_aviso
            SituacaoDoPlano.DORMENTE -> R.string.detalhe_dormente_aviso
            SituacaoDoPlano.ATIVO -> null
        }
        regra?.let {
            Spacer(Modifier.height(Espacos.interno))
            Text(
                text = stringResource(it),
                style = Estilos.apoio,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!estado.encerrado) {
            Spacer(Modifier.height(Espacos.relacionado))
            LinhaDaCurta(estado, aoAbrirCurta)
        }
    }
}

@Composable
private fun NumeroPrincipal(numero: NumeroPrincipal, escala: Float, modifier: Modifier = Modifier) {
    val locale = LocaleDoApp
    val (valor, rotulo) = when (numero) {
        is NumeroPrincipal.Dias ->
            numero.dias.toString() to pluralStringResource(R.plurals.home_placa_rotulo, numero.dias)
        NumeroPrincipal.DiaDaProva ->
            stringResource(R.string.home_placa_hoje).uppercase(locale) to stringResource(R.string.home_placa_hoje_rotulo)
        is NumeroPrincipal.TempoNaProva ->
            formatarDuracao(numero.duracaoSeg) to stringResource(R.string.detalhe_tempo_na_prova)
        is NumeroPrincipal.Sessoes ->
            stringResource(R.string.detalhe_fracao, numero.feitas, numero.previstas) to
                stringResource(R.string.detalhe_suas_sessoes)
    }

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = valor,
            style = if (escala > ESCALA_QUE_CAI_O_NUMERO) Estilos.numeroM else Estilos.numeroL,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(Espacos.minimo))
        Text(
            text = rotulo.uppercase(locale),
            style = Estilos.rotulo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * // D4
 *
 * A curta do plano numa linha entre filetes: o número para todo mundo, e o `Mudar` só para
 * o dono enquanto o plano corre. A troca vale a partir da semana seguinte. Nunca há lápis
 * na vaga da curta: a porta é esta.
 */
@Composable
private fun LinhaDaCurta(estado: DetalhePlanoUiState.Conteudo, aoAbrirCurta: () -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Espacos.alvoToque),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detalhe_curta, formatarKm(estado.parametros.curtaKm)),
                style = Estilos.corpo,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (estado.podeMudarCurta) {
                // O recheio do botão de texto sai para a goteira: a palavra alinha à margem.
                BotaoDeTexto(aoTocar = aoAbrirCurta, modifier = Modifier.offset(x = Espacos.interno)) {
                    Text(
                        text = stringResource(R.string.detalhe_curta_mudar),
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** O título, o trilho do longão e a régua (D6, D8). */
@Composable
private fun BlocoDoTrilho(estado: DetalhePlanoUiState.Conteudo, aoSelecionar: (Int) -> Unit) {
    val voce = estado.voce
    // A outra pessoa: a primeira que não é você. O público é de duas pessoas (20/10/2026).
    // Leitura que falhou tira o traço e o nome da legenda (S3).
    val outro = estado.progresso.firstOrNull { !it.voce }?.takeIf { it.semanas != null }
    val colunas = estado.linhaDoTempo.map { s ->
        ColunaDoTrilho(
            numero = s.numero,
            fase = faseDa(s.semana.tipo),
            tempo = s.tempo,
            planejadoKm = s.planejadoKm,
            seuKm = voce?.na(s.numero)?.longaoFeitoKm,
            outroKm = outro?.na(s.numero)?.longaoFeitoKm,
        )
    }
    val prova = estado.dataDaProva
    val rotuloDaProva = "${prova.dayOfMonth} ${nomeCurtoDoMes(prova)}".uppercase(LocaleDoApp)

    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Spacer(Modifier.height(Espacos.bloco))
        RotuloDeBloco(stringResource(R.string.detalhe_longao_rotulo))
        Spacer(Modifier.height(Espacos.relacionado))
        TrilhoDoLongao(
            colunas = colunas,
            selecionada = estado.semanaSelecionada,
            nomeDoOutro = outro?.nome,
            rotuloDaProva = rotuloDaProva,
            aoSelecionar = aoSelecionar,
        )
    }
}

private fun faseDa(tipo: TipoDeSemana): FaseDoPlano = when (tipo) {
    TipoDeSemana.BUILD -> FaseDoPlano.BASE
    TipoDeSemana.TAPER -> FaseDoPlano.POLIMENTO
    TipoDeSemana.PROVA -> FaseDoPlano.PROVA
}

/**
 * O card da semana selecionada: as setas, o período e **os seus** carimbos (de quem olha).
 * O lápis fica só na vaga do longão, e só quando [DetalhePlanoUiState.Conteudo.podeEditar]
 * deixa (D7). O banner de erro e o *"Também mudaram"* ficam logo abaixo, onde a edição
 * acontece.
 */
@Composable
private fun BlocoDaSemana(estado: DetalhePlanoUiState.Conteudo, acoes: AcoesDoDetalhe) {
    val semana = estado.linhaDoTempo.firstOrNull { it.numero == estado.semanaSelecionada } ?: return
    val primeira = estado.linhaDoTempo.first().numero
    val ultima = estado.linhaDoTempo.last().numero
    val prova = semana.semana.tipo == TipoDeSemana.PROVA
    val editavel = estado.podeEditar(semana.semana)

    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Spacer(Modifier.height(Espacos.margem))
        Ficha(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = Espacos.minimo, bottom = Espacos.margem)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacos.minimo),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Seta(
                        esquerda = true,
                        habilitada = semana.numero > primeira,
                        descricao = stringResource(R.string.detalhe_semana_anterior),
                        aoTocar = acoes.aoSemanaAnterior,
                    )
                    Text(
                        text = stringResource(R.string.detalhe_semana, semana.numero).uppercase(LocaleDoApp),
                        style = Estilos.rotulo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Seta(
                        esquerda = false,
                        habilitada = semana.numero < ultima,
                        descricao = stringResource(R.string.detalhe_semana_seguinte),
                        aoTocar = acoes.aoProximaSemana,
                    )
                }
                Text(
                    text = periodoCurto(semana),
                    style = Estilos.numeroS,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espacos.margem),
                )
                Spacer(Modifier.height(Espacos.interno))
                CarimbosDaSemana(
                    segmentos = semana.segmentos,
                    longaoKm = semana.semana.longaoKm,
                    longaoCumprido = null,
                    modifier = Modifier.padding(horizontal = Espacos.margem),
                    aoAbrirCorrida = acoes.aoAbrirCorrida,
                    curtaKm = semana.curtaKm,
                    provaKm = semana.planejadoKm.takeIf { prova },
                    aoEditarLongao = { acoes.aoEditarSemana(semana.semana) }.takeIf { editavel },
                    descricaoDaEdicao = stringResource(R.string.detalhe_editar_longao, semana.numero),
                    mostrarLongaoCumprido = false,
                )
            }
        }

        estado.erro?.let { erro ->
            Spacer(Modifier.height(Espacos.margem))
            BannerDeAlerta(
                rotulo = stringResource(R.string.alerta_falha_ao_salvar),
                texto = stringResource(erro),
            )
        }

        if (estado.tambemMudaram.isNotEmpty()) {
            Spacer(Modifier.height(Espacos.margem))
            Text(
                text = textoDasMudancas(estado.tambemMudaram),
                style = Estilos.apoio,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** `26 out a 1 nov`, ou `9 a 15 nov` dentro do mês. O espaço inseparável prende o dia ao mês. */
@Composable
private fun periodoCurto(semana: SemanaDoPlano): String {
    val a = semana.primeiroDia
    val b = semana.ultimoDia
    return if (a.month == b.month) {
        stringResource(R.string.detalhe_periodo, a.dayOfMonth, b.dayOfMonth, nomeCurtoDoMes(b))
    } else {
        stringResource(R.string.detalhe_periodo_meses, a.dayOfMonth, nomeCurtoDoMes(a), b.dayOfMonth, nomeCurtoDoMes(b))
    }
}

/** O chevron do card: ícone de traço 1,75 num alvo de 48dp. Na ponta da grade, apagado. */
@Composable
private fun Seta(esquerda: Boolean, habilitada: Boolean, descricao: String, aoTocar: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(Espacos.alvoToque)
            .clickable(
                enabled = habilitada,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = aoTocar,
            )
            .semantics { contentDescription = descricao }
            .alpha(if (habilitada) 1f else APAGADO),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(Tamanhos.icone)) {
            val e = size.width / 24f
            val caminho = Path().apply {
                if (esquerda) {
                    moveTo(15 * e, 6 * e); lineTo(9 * e, 12 * e); lineTo(15 * e, 18 * e)
                } else {
                    moveTo(9 * e, 6 * e); lineTo(15 * e, 12 * e); lineTo(9 * e, 18 * e)
                }
            }
            drawPath(caminho, tinta, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Desabilitado é o controle inteiro a 38% (docs/02 §4.2). */
private const val APAGADO = 0.38f

/**
 * O grupo: dono primeiro, depois por `entrou_em`. Cada membro é uma linha (avatar, nome,
 * subtítulo e fração) e uma fileira de células, uma por semana, nas colunas do gráfico.
 */
@Composable
private fun Grupo(estado: DetalhePlanoUiState.Conteudo) {
    Column {
        Spacer(Modifier.height(Espacos.secao))
        RotuloDeBloco(stringResource(R.string.detalhe_grupo_rotulo), Modifier.padding(horizontal = Goteira))
        Spacer(Modifier.height(Espacos.interno))
        estado.progresso.forEach { membro ->
            LinhaDoMembro(membro, estado)
            if (membro.semanas != null) {
                Spacer(Modifier.height(Espacos.relacionado))
                CelulasDoMembro(membro, estado)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LinhaDoMembro(membro: ProgressoDoMembro, estado: DetalhePlanoUiState.Conteudo) {
    val empilha = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA
    val primeira = estado.linhaDoTempo.first().numero
    val partes = listOfNotNull(
        when {
            membro.dono -> stringResource(R.string.detalhe_membro_dono)
            // `desde a semana N` é texto de motivo: explica o denominador (RN-19).
            membro.entrouNaSemana > primeira -> stringResource(R.string.detalhe_membro_desde, membro.entrouNaSemana)
            else -> null
        },
        stringResource(R.string.detalhe_membro_voce).takeIf { membro.voce },
    )
    val subtitulo = partes.joinToString(" · ")
    val fracao = membro.semanas?.let { stringResource(R.string.detalhe_fracao, membro.feitas, membro.previstas) }
    val descricao = descricaoDoMembro(membro, partes, estado)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Goteira)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(membro, estado.avatares[membro.uid])
        Spacer(Modifier.width(Espacos.interno))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = membro.nome, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface)
            if (subtitulo.isNotEmpty()) {
                Text(text = subtitulo, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (empilha && fracao != null) {
                Spacer(Modifier.height(Espacos.minimo))
                Text(text = fracao, style = Estilos.numeroS, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (!empilha && fracao != null) {
            Spacer(Modifier.width(Espacos.interno))
            Text(text = fracao, style = Estilos.numeroS, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** *"Hiago, dono. 17 de 21 sessões no plano. Na semana 7, 1 de 3."* */
@Composable
private fun descricaoDoMembro(
    membro: ProgressoDoMembro,
    partes: List<String>,
    estado: DetalhePlanoUiState.Conteudo,
): String {
    val quem = (listOf(membro.nome) + partes).joinToString(", ") + "."
    val plano = membro.semanas?.let {
        pluralStringResource(R.plurals.detalhe_membro_descricao_plano, membro.previstas, membro.feitas, membro.previstas)
    }
    val sel = estado.semanaSelecionada
    val tempo = estado.linhaDoTempo.firstOrNull { it.numero == sel }?.tempo
    val semana = membro.na(sel)?.takeIf { tempo != TempoDaSemana.FUTURA }?.let { s ->
        s.sessoesFeitas?.let { stringResource(R.string.detalhe_membro_descricao_semana, sel, it.coerceAtMost(s.sessoesAlvo), s.sessoesAlvo) }
    }
    return listOfNotNull(quem, plano, semana).joinToString(" ")
}

/**
 * O avatar: o sprite de jogo da espécie do membro, de 64dp, transbordando o fundo de 48dp
 * em `tipo-*-suave` (docs/03 §3.16.1). Sem tipo, `superficie-afundada`; sem sprite, a
 * inicial em `texto-secundario`.
 */
@Composable
private fun Avatar(membro: ProgressoDoMembro, avatar: AvatarDoMembro?) {
    val fundo = avatar?.tipo?.let { LocalCoresDeTipo.current[it].suave } ?: LocalCoresPokerun.current.superficieAfundada
    Box(
        modifier = Modifier
            .size(Tamanhos.avatar)
            .background(fundo, FormaControle),
        contentAlignment = Alignment.Center,
    ) {
        if (avatar?.sprite != null) {
            AsyncImage(
                model = avatar.sprite,
                contentDescription = null,
                // Pixel art: sem filtro de suavização (docs/02 §4.4).
                filterQuality = FilterQuality.None,
                modifier = Modifier.requiredSize(SpriteDoAvatar),
            )
        } else {
            Text(
                text = membro.nome.take(1).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * As células do membro: uma por semana, nas colunas do gráfico.
 *
 * - Antes de ele entrar: vazia.
 * - Passada ou corrente: um segmento por sessão prevista, os cumpridos cheios de baixo para
 *   cima; os outros em `superficie-afundada`. A semana guardada aparece assim, vazia, e
 *   conta (D1).
 * - Futura: contorno tracejado em `borda-controle`.
 * - Selecionada: moldura de 2dp em tinta, afastada 2dp.
 */
@Composable
private fun CelulasDoMembro(membro: ProgressoDoMembro, estado: DetalhePlanoUiState.Conteudo) {
    val cumprido = if (membro.voce) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val vazio = LocalCoresPokerun.current.superficieAfundada
    val contorno = MaterialTheme.colorScheme.outline
    val tinta = MaterialTheme.colorScheme.onSurface
    val semanas = estado.linhaDoTempo

    Canvas(
        modifier = Modifier
            .padding(horizontal = Goteira)
            .fillMaxWidth()
            .height(AlturaDaCelula),
    ) {
        val dp = density
        val g = geometriaDoTrilho(size.width / dp, semanas.size)
        val largura = g.larguraDaColuna * dp
        val raio = CornerRadius(Raios.dado.toPx())

        semanas.forEachIndexed { i, s ->
            val x = g.x(i) * dp
            val feito = membro.na(s.numero)
            val feitas = feito?.sessoesFeitas
            when {
                s.numero < membro.entrouNaSemana -> Unit
                feitas != null && s.tempo != TempoDaSemana.FUTURA -> {
                    val n = s.semana.sessoesAlvo.coerceAtLeast(1)
                    val vao = 1.dp.toPx()
                    val altura = (size.height - vao * (n - 1)) / n
                    repeat(n) { j ->
                        val y = size.height - (j + 1) * altura - j * vao
                        drawRoundRect(
                            color = if (j < feitas.coerceAtMost(n)) cumprido else vazio,
                            topLeft = Offset(x, y),
                            size = Size(largura, altura),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                }
                else -> {
                    val traco = TracoDaCelula.toPx()
                    drawRoundRect(
                        color = contorno,
                        topLeft = Offset(x + traco / 2, traco / 2),
                        size = Size(largura - traco, size.height - traco),
                        cornerRadius = raio,
                        style = Stroke(traco, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
                    )
                }
            }

            if (s.numero == estado.semanaSelecionada) {
                val moldura = MolduraDaSelecao.toPx()
                val afastamento = 2.dp.toPx() + moldura / 2
                drawRoundRect(
                    color = tinta,
                    topLeft = Offset(x - afastamento, -afastamento),
                    size = Size(largura + 2 * afastamento, size.height + 2 * afastamento),
                    cornerRadius = CornerRadius(Raios.dado.toPx() + afastamento),
                    style = Stroke(moldura),
                )
            }
        }
    }
}

/**
 * O código de convite com `[Compartilhar]` (D3): para dono e membro, ativo e guardado.
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

    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Spacer(Modifier.height(Espacos.relacionado))
        RotuloDeBloco(stringResource(R.string.detalhe_codigo_rotulo))
        Spacer(Modifier.height(Espacos.relacionado))

        Ficha(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Espacos.margem)) {
                if (empilha) {
                    // Acima de 1,3 o código fica acima, e o botão ocupa a largura toda.
                    Codigo(codigo, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Espacos.relacionado))
                    BotaoCompartilhar(contexto, convite, Modifier.fillMaxWidth())
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Codigo(codigo)
                        Spacer(Modifier.width(Espacos.interno))
                        BotaoCompartilhar(contexto, convite)
                    }
                }
            }
        }
    }
}

@Composable
private fun Codigo(codigo: String, modifier: Modifier = Modifier) {
    Text(
        text = codigo,
        style = Estilos.numeroM.copy(letterSpacing = 0.12.em),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun BotaoCompartilhar(contexto: android.content.Context, convite: String, modifier: Modifier = Modifier) {
    BotaoSecundario(
        aoTocar = {
            val envio = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, convite)
            }
            contexto.startActivity(Intent.createChooser(envio, null))
        },
        modifier = modifier,
    ) {
        Text(stringResource(R.string.detalhe_compartilhar))
    }
}

@Composable
private fun RotuloDeBloco(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(LocaleDoApp),
        style = Estilos.rotulo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
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

/**
 * O erro de leitura no canal de alerta (docs/02 §2.4): triângulo, rótulo e corpo, com o
 * `Tentar de novo` logo abaixo. O cabeçalho continua visível acima.
 */
private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item(key = "falhou") {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Spacer(Modifier.height(Espacos.bloco))
            BannerDeAlerta(
                rotulo = stringResource(R.string.detalhe_erro_rotulo),
                texto = stringResource(R.string.detalhe_erro_corpo),
            )
            Spacer(Modifier.height(Espacos.interno))
            BotaoSecundario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

/**
 * O esqueleto na forma da tela (docs/02 §8, item 6): número e rótulo, linha da curta,
 * título, gráfico com a régua, card da semana e duas linhas de membro com as células.
 * Estático, sem shimmer e sem spinner.
 */
private fun LazyListScope.esqueleto() {
    item(key = "esqueleto") {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Spacer(Modifier.height(EspacoAntesDoNumero))
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Bloco(40, Modifier.width(64.dp))
                    Spacer(Modifier.height(6.dp))
                    Bloco(16, Modifier.width(150.dp))
                }
                Bloco(20, Modifier.padding(top = 6.dp).width(64.dp))
            }
            Spacer(Modifier.height(Espacos.relacionado))
            Bloco(48, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Espacos.bloco))
            Bloco(16, Modifier.width(140.dp))
            Spacer(Modifier.height(Espacos.interno))
            Bloco(244, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Espacos.margem))
            Bloco(168, Modifier.fillMaxWidth())
            repeat(2) {
                Spacer(Modifier.height(Espacos.secao))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Bloco(48, Modifier.width(Tamanhos.avatar))
                    Spacer(Modifier.width(Espacos.interno))
                    Column(modifier = Modifier.weight(1f)) {
                        Bloco(16, Modifier.fillMaxWidth(0.4f))
                        Spacer(Modifier.height(6.dp))
                        Bloco(14, Modifier.fillMaxWidth(0.3f))
                    }
                    Bloco(20, Modifier.width(64.dp))
                }
                Spacer(Modifier.height(Espacos.relacionado))
                Bloco(24, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Um bloco do esqueleto: `superficie-afundada` com `raio-dado`. */
@Composable
private fun Bloco(altura: Int, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .height(altura.dp)
            .background(LocalCoresPokerun.current.superficieAfundada, FormaDado),
    )
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
            container.progressoRepositorio,
            container.temporadaRepositorio,
            container.especiesRepositorio,
        )
    }
}
