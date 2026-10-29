package com.hggabriel.pokerun.ui.telas.medalhas

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.IconesClarosNaBarraDeStatus
import com.hggabriel.pokerun.ui.componentes.LocalVoltarDoCabecalho
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.telas.corridamanual.nome
import com.hggabriel.pokerun.ui.telas.corridamanual.paceEmTexto
import com.hggabriel.pokerun.ui.telas.detalhecorrida.EstadoDaMedalha
import com.hggabriel.pokerun.ui.telas.detalhecorrida.MedalhaDoRitmo
import com.hggabriel.pokerun.ui.telas.detalhecorrida.SeloDoRitmo
import com.hggabriel.pokerun.ui.telas.detalhecorrida.coreografiaDaMedalha
import com.hggabriel.pokerun.ui.telas.detalhecorrida.tipo
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme

private val FormaDaVitrine = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
private val AlturaDoPalco = 176.dp
private val AlturaDaLinha = 64.dp
private const val OPACIDADE_A_CONQUISTAR = 0.35f

/**
 * As medalhas do ritmo (29/10/2026): as sete, do Jolteon ao Slowpoke, com a faixa de pace de
 * cada uma e quantas corridas já caíram nela. Abre pelo toque na medalha da arena (registro e
 * edição) e do detalhe da corrida, já com aquele ritmo na vitrine.
 *
 * Em cima, a vitrine em tinta: a medalha escolhida toca a entrada dela, e o toque nela repete.
 * Embaixo, os sete ritmos em escolhas cheias; o que ainda não tem corrida fica apagado, e tocar
 * num ritmo leva a medalha dele para a vitrine. Fica na pilha modal, fora da casca.
 */
@Composable
fun MedalhasDoRitmoScreen(
    ritmoInicial: Ritmo?,
    modifier: Modifier = Modifier,
    vm: MedalhasDoRitmoViewModel = medalhasDoRitmoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    MedalhasDoRitmoScreen(estado = estado, ritmoInicial = ritmoInicial, modifier = modifier)
}

/** A tela sem `ViewModel`, que é o que os previews usam. */
@Composable
fun MedalhasDoRitmoScreen(
    estado: MedalhasDoRitmoUiState,
    ritmoInicial: Ritmo?,
    modifier: Modifier = Modifier,
) {
    var escolhido by rememberSaveable { mutableStateOf(ritmoInicial ?: Ritmo.SLOWPOKE) }
    // Cada toque na mesma medalha soma uma vez: o estado novo toca a entrada de novo.
    var vez by remember { mutableIntStateOf(0) }

    IconesClarosNaBarraDeStatus()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalCoresPokerun.current.superficieAfundada),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            // A vitrine fica parada no topo: a lista rola embaixo dela, e a medalha nunca sai da tela
            // no meio da entrada.
            Vitrine(
                ritmo = escolhido,
                vez = vez,
                corridas = estado.corridasPorRitmo?.let { it[escolhido] ?: 0 },
                aoRepetir = { vez++ },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacos.margem, vertical = Espacos.interno),
            ) {
                CartaoDosRitmos(
                    escolhido = escolhido,
                    corridasPorRitmo = estado.corridasPorRitmo,
                    aoEscolher = { ritmo ->
                        if (ritmo == escolhido) vez++ else escolhido = ritmo
                    },
                )
            }
        }
    }
}

/**
 * A vitrine: a medalha de [ritmo] sobre a luz do tipo dela, a faixa de pace em número e quantas
 * corridas já caíram nela. Sem histórico ainda ([corridas] nulo), a linha da conquista espera.
 */
@Composable
private fun Vitrine(ritmo: Ritmo, vez: Int, corridas: Int?, aoRepetir: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val luz = LocalCoresDeTipo.current[ritmo.tipo].base
    val reduzido = LocalMovimentoReduzido.current
    val medalha = remember(ritmo, vez, reduzido) { EstadoDaMedalha(reduzido) }
    LaunchedEffect(medalha) { medalha.tocar(coreografiaDaMedalha(ritmo)) }
    val aoVoltar = LocalVoltarDoCabecalho.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaDaVitrine)
            .background(tinta)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = Espacos.bloco),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Espacos.margem, end = Espacos.margem, top = Espacos.minimo)
                .heightIn(min = Espacos.alvoToque)
                // O glifo alinha com a margem: o alvo de 48dp tem 12dp de folga até ele.
                .offset(x = if (aoVoltar != null) -Espacos.interno else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.editar_voltar), tint = claro)
                }
            }
            SobrancelhaDoBanner(stringResource(R.string.medalhas_titulo))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlturaDoPalco)
                // O tranco dos ritmos mais rápidos sacode a vitrine.
                .graphicsLayer { translationX = medalha.tremor.value.dp.toPx() }
                .drawBehind {
                    val raio = size.height * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(luz.copy(alpha = 0.5f), Color.Transparent), center, raio),
                        radius = raio,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            MedalhaDoRitmo(
                ritmo = ritmo,
                estado = medalha,
                aoTocar = aoRepetir,
                descricao = stringResource(R.string.medalhas_repetir),
                // A medalha pende pelo topo: o centro do palco fica na roseta, e não no meio das fitas.
                modifier = Modifier.offset(y = 6.dp),
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = textoDaFaixa(faixaDo(ritmo)).replaceFirstChar { it.titlecase(LocaleDoApp) },
                style = Estilos.numeroL,
                color = claro,
            )
            Spacer(Modifier.width(Espacos.minimo))
            Text(
                text = stringResource(R.string.medalhas_por_km).uppercase(LocaleDoApp),
                style = Estilos.rotulo,
                color = claro.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(Espacos.relacionado))
        if (corridas != null) PilulaDaConquista(corridas)
    }
}

/** Conquistada é sua, em `destaque`; a conquistar fica clara e apagada sobre a tinta. */
@Composable
private fun PilulaDaConquista(corridas: Int) {
    val claro = MaterialTheme.colorScheme.surface
    val conquistada = corridas > 0
    Text(
        text = if (conquistada) {
            pluralStringResource(R.plurals.medalhas_corridas, corridas, corridas)
        } else {
            stringResource(R.string.medalhas_a_conquistar)
        }.uppercase(LocaleDoApp),
        style = Estilos.rotulo,
        color = if (conquistada) MaterialTheme.colorScheme.onPrimary else claro.copy(alpha = 0.72f),
        modifier = Modifier
            .background(if (conquistada) MaterialTheme.colorScheme.primary else claro.copy(alpha = 0.12f), FormaCirculo)
            .padding(horizontal = Espacos.interno, vertical = Espacos.minimo + 2.dp),
    )
}

/** Os sete ritmos, do mais rápido ao mais lento, em escolhas cheias; a escolhida em tinta. */
@Composable
private fun CartaoDosRitmos(escolhido: Ritmo, corridasPorRitmo: Map<Ritmo, Int>?, aoEscolher: (Ritmo) -> Unit) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.medalhas_os_ritmos))
        Spacer(Modifier.height(Espacos.interno))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Espacos.relacionado),
        ) {
            Ritmo.entries.forEach { ritmo ->
                LinhaDoRitmo(
                    ritmo = ritmo,
                    // Sem histórico ainda, nenhuma linha fica apagada antes da hora.
                    corridas = corridasPorRitmo?.let { it[ritmo] ?: 0 },
                    escolhido = ritmo == escolhido,
                    aoEscolher = { aoEscolher(ritmo) },
                )
            }
        }
    }
}

@Composable
private fun LinhaDoRitmo(ritmo: Ritmo, corridas: Int?, escolhido: Boolean, aoEscolher: () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val fundo by animateColorAsState(
        if (escolhido) tinta else LocalCoresPokerun.current.superficieAfundada,
        animationSpec = Movimento.padrao(LocalMovimentoReduzido.current),
        label = "fundo do ritmo",
    )
    val texto = if (escolhido) claro else tinta
    val apoio = if (escolhido) claro.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val aConquistar = corridas == 0
    val nome = stringResource(ritmo.nome)
    val faixa = textoDaFaixa(faixaDo(ritmo))
    val conquista = when {
        corridas == null -> ""
        corridas > 0 -> pluralStringResource(R.plurals.medalhas_corridas, corridas, corridas)
        else -> stringResource(R.string.medalhas_a_conquistar)
    }
    val falada = stringResource(R.string.medalhas_linha_falada, nome, faixa, conquista).trimEnd(',', ' ')

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDaLinha)
            .clip(FormaControle)
            .background(fundo)
            .selectable(selected = escolhido, role = Role.RadioButton, onClick = aoEscolher)
            .clearAndSetSemantics {
                contentDescription = falada
                selected = escolhido
                role = Role.RadioButton
            }
            .padding(horizontal = Espacos.interno, vertical = Espacos.relacionado),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeloDoRitmo(ritmo = ritmo, modifier = Modifier.alpha(if (aConquistar && !escolhido) OPACIDADE_A_CONQUISTAR else 1f))
        Spacer(Modifier.width(Espacos.interno))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nome, style = Estilos.corpoForte, color = texto)
            Text(text = faixa + " " + stringResource(R.string.medalhas_por_km), style = Estilos.apoio, color = apoio)
        }
        if (corridas != null && corridas > 0) {
            Spacer(Modifier.width(Espacos.relacionado))
            Text(text = stringResource(R.string.medalhas_vezes, corridas), style = Estilos.numeroM, color = texto)
        }
    }
}

/** `abaixo de 4:15`, `4:15 a 5:00` ou `9:00 ou mais`. */
@Composable
private fun textoDaFaixa(faixa: FaixaDoRitmo): String = when {
    faixa.de == null && faixa.ate != null -> stringResource(R.string.medalhas_abaixo_de, paceEmTexto(faixa.ate))
    faixa.de != null && faixa.ate == null -> stringResource(R.string.medalhas_acima_de, paceEmTexto(faixa.de))
    else -> stringResource(R.string.medalhas_entre, paceEmTexto(faixa.de ?: 0), paceEmTexto(faixa.ate ?: 0))
}

@Composable
private fun medalhasDoRitmoViewModel(): MedalhasDoRitmoViewModel {
    val container = LocalAppContainer.current
    return viewModel { MedalhasDoRitmoViewModel(container.autenticacaoRepositorio, container.corridaRepositorio) }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun MedalhasPreview() {
    PokerunTheme {
        MedalhasDoRitmoScreen(
            estado = MedalhasDoRitmoUiState(mapOf(Ritmo.SLOWPOKE to 4, Ritmo.PSYDUCK to 2, Ritmo.PIKACHU to 1)),
            ritmoInicial = Ritmo.PIKACHU,
        )
    }
}
