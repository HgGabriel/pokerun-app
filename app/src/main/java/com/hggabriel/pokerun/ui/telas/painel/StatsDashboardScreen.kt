package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.fonteAmpliada
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.naFigura
import androidx.compose.ui.text.style.TextAlign
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import java.time.DayOfWeek
import java.time.LocalDate

private val AlturaDoTrilho = 92.dp
private val LadoDaPorta = 68.dp

/** As quatro portas da estante (decisão 16). Cada uma abre uma tela da aba Progresso. */
enum class PortaDoProgresso { RITMO, FITAS, SELOS, CORRIDAS }

/**
 * A raiz de `Progresso` (estudo de Progresso e Perfil, Revisão 3): a estrada em tinta, o
 * cartão da prova do plano ativo, a estante com quatro portas e o Você sabia.
 *
 * O seletor `Painel | Histórico` saiu do topo (decisão 1): o histórico é a porta Corridas. A
 * página é tingida, com um cartão por bloco, e a peça em tinta é a estrada.
 */
@Composable
fun StatsDashboardScreen(
    aoAbrirPerfil: () -> Unit,
    aoAbrirPorta: (PortaDoProgresso) -> Unit,
    modifier: Modifier = Modifier,
    vm: PainelViewModel = progressoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    StatsDashboardScreen(estado, aoAbrirPerfil, aoAbrirPorta, vm::tentarDeNovo, modifier)
}

@Composable
fun StatsDashboardScreen(
    estado: PainelUiState,
    aoAbrirPerfil: () -> Unit,
    aoAbrirPorta: (PortaDoProgresso) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            when (estado) {
                PainelUiState.Carregando -> item(key = "estrada") {
                    EstradaEmTinta(montarPainel(emptyList(), emptyList(), null, LocalDate.now()), aoAbrirPerfil)
                }
                PainelUiState.Falhou -> falhou(aoTentarDeNovo, aoAbrirPerfil)
                is PainelUiState.Conteudo -> raiz(estado, aoAbrirPerfil, aoAbrirPorta)
            }
        }
    }
}

private fun LazyListScope.raiz(
    estado: PainelUiState.Conteudo,
    aoAbrirPerfil: () -> Unit,
    aoAbrirPorta: (PortaDoProgresso) -> Unit,
) {
    item(key = "estrada") { EstradaEmTinta(estado, aoAbrirPerfil) }
    estado.prova?.let { prova -> item(key = "prova") { CartaoDaProva(prova, Modifier.padding(horizontal = Espacos.margem)) } }
    item(key = "estante") { Estante(estado, aoAbrirPorta, Modifier.padding(horizontal = Espacos.margem)) }
    if (estado.curiosidades.isNotEmpty()) {
        item(key = "sabia") { VoceSabia(estado.curiosidades, Modifier.padding(horizontal = Espacos.margem)) }
    }
}

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit, aoAbrirPerfil: () -> Unit) {
    item(key = "estrada") { EstradaEmTinta(montarPainel(emptyList(), emptyList(), null, LocalDate.now()), aoAbrirPerfil) }
    item(key = "falhou") {
        Cartao(modifier = Modifier.padding(horizontal = Espacos.margem)) {
            Text(stringResource(R.string.painel_erro_corpo), style = Estilos.corpo, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Espacos.margem))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// O cartão da prova (decisão 17): o título é `nome · data` do plano ativo
// ---------------------------------------------------------------------------

@Composable
private fun CartaoDaProva(prova: ProvaDoPlano, modifier: Modifier) {
    val data = "${prova.data.dayOfMonth} ${nomeCurtoDoMes(prova.data)}"
    val maior = formatarKm(prova.maiorCorridaKm)
    val distancia = formatarKm(prova.distanciaKm)
    val descricao = stringResource(
        R.string.progresso_prova_descricao, prova.nome, data, prova.dias.toInt(), maior, distancia, prova.sessoesFeitas, prova.sessoesAlvo,
    )
    Cartao(modifier = modifier.clearAndSetSemantics { contentDescription = descricao }) {
        Row(verticalAlignment = Alignment.Bottom) {
            RotuloDoCartao(stringResource(R.string.progresso_prova_titulo, prova.nome, data), Modifier.weight(1f))
            Text(prova.dias.toString(), style = Estilos.tituloSecao.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(Espacos.minimo))
            Text(pluralStringResource(R.plurals.progresso_dias, quantidadeDoPlural(prova.dias.toInt())), style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Espacos.interno))
        TrilhoCompacto(prova)
        Spacer(Modifier.height(Espacos.margem))
        val metades: @Composable (@Composable (Modifier) -> Unit, @Composable (Modifier) -> Unit) -> Unit = { a, b ->
            // Ampliada, uma metade sobre a outra: lado a lado, MAIOR CORRIDA partia no meio.
            if (fonteAmpliada()) {
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.margem)) {
                    a(Modifier.fillMaxWidth())
                    b(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.margem)) {
                    a(Modifier.weight(1f))
                    b(Modifier.weight(1f))
                }
            }
        }
        metades({ m ->
            Column(modifier = m) {
                RotuloDoCartao(stringResource(R.string.progresso_maior_corrida))
                NumeroComApoio(maior, stringResource(R.string.progresso_de_km, distancia))
                Spacer(Modifier.height(Espacos.relacionado))
                BarraDePilula(
                    fracao = (prova.maiorCorridaKm / prova.distanciaKm).toFloat(),
                    cor = MaterialTheme.colorScheme.primary,
                )
            }
        }, { m ->
            Column(modifier = m) {
                RotuloDoCartao(stringResource(R.string.progresso_esta_semana))
                NumeroComApoio(prova.sessoesFeitas.toString(), stringResource(R.string.progresso_de, prova.sessoesAlvo))
                Spacer(Modifier.height(Espacos.relacionado))
                SessoesEmPilula(prova.sessoesFeitas, prova.sessoesAlvo)
            }
        })
    }
}

@Composable
private fun NumeroComApoio(numero: String, apoio: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(numero, style = Estilos.numeroM, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(2.dp))
        Text(apoio, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
    }
}

/** As sessões da semana em pílulas: a última, mais longa, é o longão. */
@Composable
private fun SessoesEmPilula(feitas: Int, alvo: Int) {
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val destaque = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(Espacos.minimo), modifier = Modifier.fillMaxWidth()) {
        repeat(alvo.coerceAtLeast(1)) { i ->
            Box(
                modifier = Modifier
                    .weight(if (i == alvo - 1) 1.4f else 1f)
                    .height(8.dp)
                    .background(if (i < feitas) destaque else afundada, RoundedCornerShape(50)),
            )
        }
    }
}

/**
 * O mini trilho do longão: uma pílula por semana, com o alvo de `weeks.longao_km` como a
 * pílula clara na cor da fase e o feito de `weekly.longao_feito` enchendo por cima em
 * `destaque`. A semana da prova é a coluna em tinta, e a corrente leva o alvo num balão.
 */
@Composable
private fun TrilhoCompacto(prova: ProvaDoPlano) {
    val cores = LocalCoresPokerun.current
    val tinta = MaterialTheme.colorScheme.onSurface
    val destaque = MaterialTheme.colorScheme.primary
    val maximo = prova.colunas.maxOfOrNull { maxOf(it.alvoKm ?: 0.0, it.feitoKm) }?.takeIf { it > 0 } ?: 1.0
    val ampliado = fonteAmpliada()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        prova.colunas.forEach { coluna ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
                    if (coluna.corrente && coluna.alvoKm != null) {
                        Text(
                            "${formatarKm(coluna.alvoKm)} km",
                            style = naFigura(Estilos.legenda),
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            softWrap = false,
                            // O balão passa da largura da coluna: o texto não pode cortar.
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .background(tinta, RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                val fase = when {
                    coluna.prova -> tinta
                    coluna.polimento -> cores.fasePolimento
                    else -> cores.faseBase
                }
                Canvas(modifier = Modifier.width(16.dp).height(AlturaDoTrilho)) {
                    val raio = CornerRadius(size.width / 2)
                    drawRoundRect(cores.superficieAfundada, cornerRadius = raio)
                    fun pilula(km: Double, cor: androidx.compose.ui.graphics.Color) {
                        val h = (size.height * (km / maximo).toFloat()).coerceIn(size.width, size.height)
                        drawRoundRect(cor, Offset(0f, size.height - h), Size(size.width, h), raio)
                    }
                    coluna.alvoKm?.let { pilula(it, if (coluna.prova) fase else fase.copy(alpha = if (coluna.corrente) 0.85f else 0.35f)) }
                    if (coluna.feitoKm > 0) pilula(coluna.feitoKm, destaque)
                }
                Spacer(Modifier.height(4.dp))
                // Ampliada, só a semana corrente e a da prova levam número: onze rótulos não
                // cabem lado a lado a 320dp, e o resto está na descrição falada.
                val rotulado = !ampliado || coluna.corrente || coluna.prova
                Text(
                    when {
                        !rotulado -> ""
                        coluna.prova -> dataNumerica(prova.data)
                        else -> coluna.numero.toString()
                    },
                    style = naFigura(Estilos.legenda.copy(fontSize = 11.sp, fontWeight = if (coluna.corrente) FontWeight.Bold else null)),
                    // A data da prova é mais larga que a coluna: passa dela em vez de cortar.
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    color = if (coluna.corrente) tinta else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun dataNumerica(data: LocalDate): String = "${data.dayOfMonth}/${data.monthValue}"

// ---------------------------------------------------------------------------
// A estante: quatro portas, cada uma com a sua figura (decisão 16)
// ---------------------------------------------------------------------------

@Composable
private fun Estante(estado: PainelUiState.Conteudo, aoAbrir: (PortaDoProgresso) -> Unit, modifier: Modifier) {
    Cartao(modifier = modifier, recheio = PaddingValues(start = Espacos.relacionado, end = Espacos.relacionado, top = Espacos.margem, bottom = Espacos.interno)) {
        // Ampliada, duas portas por linha: quatro lado a lado partiam os nomes (`Ritm`).
        val porLinha = if (fonteAmpliada()) 2 else PortaDoProgresso.entries.size
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.interno)) {
            PortaDoProgresso.entries.chunked(porLinha).forEach { linha ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    linha.forEach { porta -> Porta(porta, estado, { aoAbrir(porta) }, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Porta(porta: PortaDoProgresso, estado: PainelUiState.Conteudo, aoTocar: () -> Unit, modifier: Modifier) {
    val cores = LocalCoresPokerun.current
    val tipos = LocalCoresDeTipo.current
    val nome = stringResource(
        when (porta) {
            PortaDoProgresso.RITMO -> R.string.progresso_porta_ritmo
            PortaDoProgresso.FITAS -> R.string.progresso_porta_fitas
            PortaDoProgresso.SELOS -> R.string.progresso_porta_selos
            PortaDoProgresso.CORRIDAS -> R.string.progresso_porta_corridas
        },
    )
    val numero = when (porta) {
        PortaDoProgresso.RITMO -> estado.ritmo.atualSeg?.let { stringResource(R.string.progresso_por_km, paceCurto(it.toDouble())) } ?: "0:00"
        PortaDoProgresso.FITAS -> stringResource(R.string.progresso_de_cinco, estado.fitas.count { !it.vaga })
        PortaDoProgresso.SELOS -> if (estado.sequencia.atual > 0) {
            pluralStringResource(R.plurals.progresso_semanas, estado.sequencia.atual, estado.sequencia.atual)
        } else {
            stringResource(R.string.progresso_melhor_sequencia, estado.sequencia.melhor)
        }
        PortaDoProgresso.CORRIDAS -> stringResource(R.string.progresso_km, formatarKm(estado.kmNaVida))
    }
    val ritmo = estado.ritmo.ritmo ?: Ritmo.SLOWPOKE
    val fundo = when (porta) {
        PortaDoProgresso.RITMO -> tipos[tipoDoRitmo(ritmo)].suave
        PortaDoProgresso.FITAS -> MaterialTheme.colorScheme.onSurface
        PortaDoProgresso.SELOS -> cores.superficieAfundada
        PortaDoProgresso.CORRIDAS -> cores.destaqueSuave
    }
    Column(
        modifier = modifier
            .heightIn(min = Espacos.alvoToque)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = aoTocar)
            .clearAndSetSemantics {
                contentDescription = "$nome, $numero"
                role = Role.Button
            }
            .padding(vertical = Espacos.minimo),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(LadoDaPorta).clip(RoundedCornerShape(20.dp)).background(fundo),
            contentAlignment = Alignment.Center,
        ) {
            when (porta) {
                PortaDoProgresso.RITMO -> AsyncImage(
                    model = spriteDeJogoDe(ritmo.numero),
                    contentDescription = null,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(64.dp),
                )
                PortaDoProgresso.FITAS -> {
                    val fita = estado.fitas.lastOrNull { !it.vaga } ?: estado.fitas.firstOrNull()
                    if (fita != null) {
                        AsyncImage(
                            model = fita.fita.url,
                            contentDescription = null,
                            alpha = if (fita.vaga) 0.14f else 1f,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
                PortaDoProgresso.SELOS -> SelosEmMiniatura(estado)
                PortaDoProgresso.CORRIDAS -> Text(
                    estado.corridas.toString(),
                    style = Estilos.numeroL,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(Espacos.relacionado))
        Text(nome, style = Estilos.corpoForte.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(numero, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** O tipo da espécie de cada ritmo: o fundo `tipo-suave` da porta. */
internal fun tipoDoRitmo(ritmo: Ritmo): TipoDePokemon = when (ritmo) {
    Ritmo.JOLTEON, Ritmo.PIKACHU -> TipoDePokemon.ELETRICO
    Ritmo.RAPIDASH, Ritmo.PONYTA, Ritmo.GROWLITHE -> TipoDePokemon.FOGO
    Ritmo.PSYDUCK, Ritmo.SLOWPOKE -> TipoDePokemon.AGUA
}

/** As quatro últimas semanas do ano em selos, em miniatura: a figura da porta Selos. */
@Composable
private fun SelosEmMiniatura(estado: PainelUiState.Conteudo) {
    val destaque = MaterialTheme.colorScheme.primary
    val vazio = MaterialTheme.colorScheme.outlineVariant
    val semanaDeHoje = estado.heatmap.colunas.indexOfFirst { coluna -> coluna.any { it?.data == estado.hoje } }
    val colunas = if (semanaDeHoje >= 0) {
        estado.heatmap.colunas.subList((semanaDeHoje - 3).coerceAtLeast(0), semanaDeHoje + 1)
    } else {
        estado.heatmap.colunas.takeLast(4)
    }
    Canvas(modifier = Modifier.size(52.dp)) {
        val passoX = size.width / 4
        val passoY = size.height / 7
        colunas.forEachIndexed { s, dias ->
            dias.forEachIndexed { d, dia ->
                val cor = if (dia != null && dia.corridas > 0) destaque else vazio
                drawRoundRect(
                    cor,
                    Offset(s * passoX + passoX * 0.12f, d * passoY + passoY * 0.15f),
                    Size(passoX * 0.76f, passoY * 0.7f),
                    CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Você sabia: um fato por vez, e tocar troca (a troca é a recompensa)
// ---------------------------------------------------------------------------

@Composable
private fun VoceSabia(fatos: List<Curiosidade>, modifier: Modifier) {
    var indice by rememberSaveable(fatos.size) { mutableIntStateOf(0) }
    val fato = fatos[indice % fatos.size]
    val (grande, frase) = textoDoFato(fato)
    val proximo = { indice = (indice + 1) % fatos.size }
    val outro = stringResource(R.string.progresso_outro_fato)
    Cartao(modifier = modifier.clickable(onClick = proximo)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotuloDoCartao(stringResource(R.string.progresso_voce_sabia), Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fatos.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 6.dp)
                            .background(
                                if (i == indice % fatos.size) MaterialTheme.colorScheme.onSurface else LocalCoresPokerun.current.superficieAfundada,
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(Espacos.relacionado))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(grande, style = Estilos.numeroXl, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Espacos.minimo))
                Text(frase, style = Estilos.corpo, color = MaterialTheme.colorScheme.onSurface)
            }
            if (fatos.size > 1) {
                Box(
                    modifier = Modifier
                        .size(Espacos.alvoToque)
                        .clip(FormaCirculo)
                        .clickable(role = Role.Button, onClick = proximo)
                        .semantics { contentDescription = outro },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, FormaCirculo), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

/** O número grande e a frase de cada fato. */
@Composable
internal fun textoDoFato(fato: Curiosidade): Pair<String, String> = when (fato) {
    is Curiosidade.ParteDaMaior -> stringResource(R.string.progresso_porcento, fato.porcento) to
        stringResource(R.string.progresso_fato_maior, formatarKm(fato.km), dataPorExtenso(fato.dia))
    is Curiosidade.DiaPreferido -> stringResource(R.string.progresso_n_de_total, fato.corridas, fato.total) to
        stringResource(R.string.progresso_fato_dia, stringResource(nomeDoDiaPreferido(fato.dia)))
    is Curiosidade.HorarioPreferido -> stringResource(R.string.progresso_n_de_total, fato.corridas, fato.total) to
        stringResource(R.string.progresso_fato_horario, stringResource(nomeDaFaixa(fato.faixa)))
    is Curiosidade.MaiorXp -> stringResource(R.string.progresso_xp, String.format(LocaleDoApp, "%,d", fato.xp)) to
        stringResource(R.string.progresso_fato_xp, formatarKm(fato.km), dataPorExtenso(fato.dia))
    is Curiosidade.ParteDoMultiplicador -> stringResource(R.string.progresso_porcento, fato.porcento) to
        stringResource(R.string.progresso_fato_multiplicador)
}

@Composable
private fun dataPorExtenso(data: LocalDate): String = stringResource(R.string.progresso_data_extenso, data.dayOfMonth, nomeDoMes(data))

private fun nomeDoDiaPreferido(dia: DayOfWeek): Int = when (dia) {
    DayOfWeek.MONDAY -> R.string.progresso_dia_segunda
    DayOfWeek.TUESDAY -> R.string.progresso_dia_terca
    DayOfWeek.WEDNESDAY -> R.string.progresso_dia_quarta
    DayOfWeek.THURSDAY -> R.string.progresso_dia_quinta
    DayOfWeek.FRIDAY -> R.string.progresso_dia_sexta
    DayOfWeek.SATURDAY -> R.string.progresso_dia_sabado
    DayOfWeek.SUNDAY -> R.string.progresso_dia_domingo
}

private fun nomeDaFaixa(faixa: FaixaDoDia): Int = when (faixa) {
    FaixaDoDia.CEDO -> R.string.progresso_faixa_cedo
    FaixaDoDia.MANHA -> R.string.progresso_faixa_manha
    FaixaDoDia.ALMOCO -> R.string.progresso_faixa_almoco
    FaixaDoDia.TARDE -> R.string.progresso_faixa_tarde
    FaixaDoDia.ENTARDECER -> R.string.progresso_faixa_entardecer
    FaixaDoDia.NOITE -> R.string.progresso_faixa_noite
    FaixaDoDia.TARDE_DA_NOITE -> R.string.progresso_faixa_tarde_da_noite
}

// ---------------------------------------------------------------------------
// Fábrica: a raiz e as quatro portas usam o mesmo `ViewModel`
// ---------------------------------------------------------------------------

@Composable
internal fun progressoViewModel(): PainelViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        PainelViewModel(
            container.autenticacaoRepositorio,
            container.corridaRepositorio,
            container.planoRepositorio,
            container.progressoRepositorio,
            container.usuarioRepositorio,
            container.temporadaRepositorio,
            container.especiesRepositorio,
        )
    }
}

/**
 * O pt-BR do Android põe o zero no `one` ("0 semana"). O texto do app diz "0 semanas": no
 * zero, a quantidade pedida ao recurso é a do plural.
 */
internal fun quantidadeDoPlural(n: Int): Int = if (n == 0) 2 else n
