package com.hggabriel.pokerun.ui.telas.detalhecorrida

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.ParcelasDeXp
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.nomeDoMes
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.LocalDate
import java.util.Locale

private val Goteira = 16.dp
private val AlturaDoBotao = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val FolgaDoFim = 24.dp
private val AlturaDoEsqueleto = 96.dp

/** Colunas fixas do bloco de ficha, em caracteres: a fonte de dado é monoespaçada. */
private const val COLUNA_DA_FRACAO = 3

/** Acima desta escala o número grande cai de `displayLarge` para `displayMedium` (docs/02 §8). */
private const val ESCALA_QUE_REBAIXA = 1.5f
private const val COLUNA_DA_UNIDADE = 6

/**
 * A `RunDetailScreen` (`F2-T09`, docs/03 §3.13): a corrida inteira, num bloco de ficha.
 *
 * Mora dentro da aba que a abriu: `Hoje`, pelo segmento da barra de sessões, e
 * `Progresso`, pelo histórico de `F2-T11` (docs/03 §1). `Corrigir` leva à
 * `RunEditScreen`, na pilha modal.
 */
@Composable
fun DetalheCorridaScreen(
    corridaId: String,
    aoCorrigir: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: DetalheCorridaViewModel = detalheCorridaViewModel(corridaId),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    DetalheCorridaScreen(
        estado = estado,
        aoCorrigir = aoCorrigir,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun DetalheCorridaScreen(
    estado: DetalheCorridaUiState,
    aoCorrigir: (String) -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conteudo = estado as? DetalheCorridaUiState.Conteudo

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FolgaDoFim),
        ) {
            item {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.corrida_detalhe_sobrancelha)),
                    titulo = conteudo?.let { textoDoDia(it.dia) }.orEmpty(),
                    acao = conteudo?.let { { MenuDaCorrida(aoCorrigir = { aoCorrigir(it.corridaId) }) } },
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                DetalheCorridaUiState.Carregando -> esqueleto()
                DetalheCorridaUiState.Falhou -> falhou(aoTentarDeNovo)
                is DetalheCorridaUiState.Conteudo -> conteudo(estado)
            }
        }
    }
}

private fun LazyListScope.conteudo(estado: DetalheCorridaUiState.Conteudo) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            if (estado.foraDosCalculos) {
                Text(
                    text = stringResource(R.string.corrida_detalhe_fora),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(EspacoEntreBlocos))
            }

            // docs/03 §3.13.1: um número em destaque, acima do bloco.
            NumeroGrande(estado.distancia)
            Spacer(Modifier.height(EspacoEntreBlocos))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            estado.linhas.forEach { LinhaDeFicha(it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(EspacoEntreBlocos))
            Text(
                text = textoDoPlano(estado),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            ContaDoXp(estado)
        }
    }
}

@Composable
private fun NumeroGrande(distancia: String) {
    val unidade = stringResource(R.string.corrida_detalhe_unidade_km)
    Text(
        text = buildAnnotatedString {
            append(distancia)
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(" ")
                append(unidade)
            }
        },
        // docs/02 §8, item 9: o número grande tem uma linha só, e cai de degrau acima de
        // [ESCALA_QUE_REBAIXA], como a contagem da Home.
        style = if (LocalDensity.current.fontScale > ESCALA_QUE_REBAIXA) {
            MaterialTheme.typography.displayMedium
        } else {
            MaterialTheme.typography.displayLarge
        },
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$distancia $unidade" },
    )
}

/**
 * Uma linha do bloco: rótulo em mono caixa alta à esquerda; valor alinhado **pelo
 * separador decimal** à direita; unidade em `tinta-fraca` (docs/03 §3.13.1).
 *
 * O alinhamento sai de colunas de largura fixa, medidas no dígito da fonte de dado: a
 * parte depois da vírgula ocupa sempre [COLUNA_DA_FRACAO] dígitos, e a unidade
 * [COLUNA_DA_UNIDADE]. Preencher com espaço não serve: o Android não conta espaço no fim
 * da linha, e o emulador mostrou a distância três colunas fora do lugar. A FC média é série de
 * dado secundário, e por isso sai em `sinal` (docs/02 §2.1).
 */
@Composable
private fun LinhaDeFicha(linha: LinhaDaFicha) {
    val rotulo = stringResource(rotuloDa(linha.tipo))
    val unidade = unidadeDa(linha.tipo)?.let { stringResource(it) }.orEmpty()
    val inteiro = linha.valor.substringBefore(',')
    val fracao = if (',' in linha.valor) "," + linha.valor.substringAfter(',') else ""
    // A largura de um dígito da fonte de dado, que é monoespaçada. Medida, e não fixa em
    // dp, para acompanhar a escala de fonte do sistema.
    val medidor = rememberTextMeasurer()
    val digito = with(LocalDensity.current) { medidor.measure("0", EstiloDado).size.width.toDp() }
    val corDoValor = if (linha.tipo == TipoDeLinha.FC_MEDIA) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val descricao = "$rotulo ${linha.valor} $unidade".trim()
    val textoDoRotulo: @Composable (Modifier) -> Unit = { modificador ->
        Text(
            text = rotulo.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modificador,
        )
    }
    val valor: @Composable () -> Unit = {
        Text(text = inteiro, style = EstiloDado, color = corDoValor)
        Text(text = fracao, style = EstiloDado, color = corDoValor, modifier = Modifier.width(digito * COLUNA_DA_FRACAO))
        Spacer(Modifier.width(digito * 2))
        Text(
            text = unidade,
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(digito * COLUNA_DA_UNIDADE),
        )
    }

    // Acima de [ESCALA_QUE_EMPILHA] o rótulo sobe e o valor desce, como nas outras telas:
    // a 2,0 em 320dp o rótulo ao lado das três colunas quebrava `DURAÇÃO` no meio.
    if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlturaDoBotao)
                .padding(vertical = EspacoEntreLinhas / 2)
                .clearAndSetSemantics { contentDescription = descricao },
        ) {
            textoDoRotulo(Modifier)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { valor() }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDoBotao)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        textoDoRotulo(Modifier.weight(1f))
        valor()
    }
}

/**
 * O XP creditado com a conta aberta (docs/03 §3.13): as parcelas de `xp_detalhe`, na
 * ordem da fórmula de docs/04 §3. Bônus zerado não ganha linha, como campo ausente no
 * bloco de cima. Sem `xp_detalhe`, só o total, e uma frase dizendo por quê.
 */
@Composable
private fun ContaDoXp(estado: DetalheCorridaUiState.Conteudo) {
    val locale = LocaleDoApp

    Text(
        text = stringResource(R.string.corrida_detalhe_xp_rotulo).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(EspacoEntreLinhas))

    val conta = estado.conta
    if (conta != null) {
        LinhaDaConta(R.string.corrida_detalhe_xp_base, inteiro(conta.base, locale))
        if (conta.longao > 0) LinhaDaConta(R.string.corrida_detalhe_xp_longao, "+" + inteiro(conta.longao, locale))
        if (conta.pace > 0) LinhaDaConta(R.string.corrida_detalhe_xp_pace, "+" + inteiro(conta.pace.toDouble(), locale))
        LinhaDaConta(R.string.corrida_detalhe_xp_multiplicador, "×" + String.format(locale, "%.1f", conta.multiplicador))
        if (conta.recorde > 0) LinhaDaConta(R.string.corrida_detalhe_xp_recorde, "+" + inteiro(conta.recorde.toDouble(), locale))
    }
    LinhaDaConta(
        R.string.corrida_detalhe_xp_total,
        stringResource(R.string.corrida_detalhe_xp_total_valor, String.format(locale, "%,d", estado.xpCreditado)),
    )
    if (conta == null) {
        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = stringResource(R.string.corrida_detalhe_xp_pendente),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LinhaDaConta(rotulo: Int, valor: String) {
    val texto = stringResource(rotulo)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EspacoEntreLinhas / 2)
            .clearAndSetSemantics { contentDescription = "$texto $valor" },
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = valor, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * `Corrigir` no menu (docs/03 §3.13.1). O alvo é um `Box` clicável de 48dp com
 * `Role.Button`, como a engrenagem do cabeçalho: o `IconButton` ignora o
 * `LocalIndication` do tema (`F1-T07b`).
 */
@Composable
private fun MenuDaCorrida(aoCorrigir: () -> Unit) {
    var aberto by rememberSaveable { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(AlturaDoBotao)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = { aberto = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.corrida_detalhe_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.corrida_detalhe_corrigir)) },
                onClick = {
                    aberto = false
                    aoCorrigir()
                },
            )
        }
    }
}

@Composable
private fun textoDoDia(dia: LocalDate): String =
    stringResource(R.string.corrida_detalhe_dia, dia.dayOfMonth, nomeDoMes(dia, LocaleDoApp), dia.year)

@Composable
private fun textoDoPlano(estado: DetalheCorridaUiState.Conteudo): String =
    estado.semanaRef?.let { stringResource(R.string.corrida_detalhe_plano, estado.nomeDoPlano, it) }
        ?: stringResource(R.string.corrida_detalhe_plano_fora, estado.nomeDoPlano)

private fun inteiro(valor: Double, locale: Locale): String = String.format(locale, "%,d", Math.round(valor))

private fun rotuloDa(tipo: TipoDeLinha): Int = when (tipo) {
    TipoDeLinha.DISTANCIA -> R.string.corrida_detalhe_distancia
    TipoDeLinha.DURACAO -> R.string.corrida_detalhe_duracao
    TipoDeLinha.PACE -> R.string.corrida_detalhe_pace
    TipoDeLinha.FC_MEDIA -> R.string.corrida_detalhe_fc
    TipoDeLinha.ESFORCO -> R.string.corrida_detalhe_esforco
}

private fun unidadeDa(tipo: TipoDeLinha): Int? = when (tipo) {
    TipoDeLinha.DISTANCIA -> R.string.corrida_detalhe_unidade_km
    TipoDeLinha.DURACAO -> null
    TipoDeLinha.PACE -> R.string.corrida_detalhe_unidade_pace
    TipoDeLinha.FC_MEDIA -> R.string.corrida_detalhe_unidade_bpm
    TipoDeLinha.ESFORCO -> R.string.corrida_detalhe_unidade_esforco
}

// ---------------------------------------------------------------------------
// Os estados sem conteúdo
// ---------------------------------------------------------------------------

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.corrida_detalhe_erro_corpo),
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

/** O esqueleto na forma do conteúdo, estático (docs/02 §8, item 6). */
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

/** A chave carrega o ID: sem ela, abrir a segunda corrida devolveria o `ViewModel` da primeira. */
@Composable
private fun detalheCorridaViewModel(corridaId: String): DetalheCorridaViewModel {
    val container = LocalAppContainer.current
    return viewModel(key = "corrida-$corridaId") {
        DetalheCorridaViewModel(
            corridaId,
            container.autenticacaoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val CORRIDA_DE_EXEMPLO = DetalheCorridaUiState.Conteudo(
    corridaId = "run-1",
    dia = LocalDate.of(2026, 9, 14),
    distancia = "12,40",
    linhas = listOf(
        LinhaDaFicha(TipoDeLinha.DISTANCIA, "12,40"),
        LinhaDaFicha(TipoDeLinha.DURACAO, "1:04:22"),
        LinhaDaFicha(TipoDeLinha.PACE, "5:11"),
        LinhaDaFicha(TipoDeLinha.FC_MEDIA, "148"),
        LinhaDaFicha(TipoDeLinha.ESFORCO, "7"),
    ),
    nomeDoPlano = "São Silvestre",
    semanaRef = 6,
    xpCreditado = 3_200,
    conta = ParcelasDeXp(base = 1240.0, longao = 260.0, pace = 200, multiplicador = 2.0, recorde = 0),
    foraDosCalculos = false,
)

@Composable
private fun CorridaPreview(estado: DetalheCorridaUiState) {
    PokerunTheme {
        DetalheCorridaScreen(estado = estado, aoCorrigir = {}, aoTentarDeNovo = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CompletaPreview() = CorridaPreview(CORRIDA_DE_EXEMPLO)

@Preview(showBackground = true)
@Composable
private fun SemContaPreview() = CorridaPreview(
    CORRIDA_DE_EXEMPLO.copy(
        linhas = CORRIDA_DE_EXEMPLO.linhas.filter { it.tipo != TipoDeLinha.FC_MEDIA && it.tipo != TipoDeLinha.ESFORCO },
        conta = null,
        xpCreditado = 0,
    ),
)

@Preview(showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun FonteGrandePreview() = CorridaPreview(CORRIDA_DE_EXEMPLO)

@Preview(showBackground = true)
@Composable
private fun FalhouPreview() = CorridaPreview(DetalheCorridaUiState.Falhou)
