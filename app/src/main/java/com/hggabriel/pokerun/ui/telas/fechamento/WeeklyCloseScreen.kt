package com.hggabriel.pokerun.ui.telas.fechamento

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.dominio.regras.QuebraDoXp
import com.hggabriel.pokerun.dominio.regras.SemanaAApresentar
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.BarraDeSessoes
import com.hggabriel.pokerun.ui.componentes.BlocoEmEspera
import com.hggabriel.pokerun.ui.componentes.BotaoDeTexto
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import com.hggabriel.pokerun.ui.componentes.IconesClarosNaBarraDeStatus
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.telas.pokedex.FigurinhaColada
import com.hggabriel.pokerun.ui.telas.pokedex.FigurinhaVaga
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import com.hggabriel.pokerun.ui.theme.Raios
import kotlinx.coroutines.delay

/** As etapas da sequência de docs/03 §3.20, e a final, que é o estado que o toque salta. */
private const val ETAPA_SESSOES = 1
private const val ETAPA_XP = 2
private const val ETAPA_ESCADA = 3
private const val ETAPA_SILHUETAS = 4
private const val ETAPA_ALVO = 5
private const val ETAPA_FINAL = ETAPA_ALVO

/** O intervalo entre uma figurinha e a seguinte (docs/02 §6): cada uma tem um nome. */
private const val INTERVALO_DAS_SILHUETAS_MS = Movimento.Tempo.BATIDA.toLong()

/** O respiro entre as etapas: 5 etapas e as figurinhas cabem nos 2 a 3 segundos da spec. */
private const val RESPIRO_ENTRE_ETAPAS_MS = Movimento.Tempo.PADRAO.toLong()

private const val COLUNAS = 3

private const val OPACIDADE_DO_ROTULO = 0.72f
private const val OPACIDADE_DO_BRILHO = 0.6f
private const val OPACIDADE_DA_POKEBOLA = 0.08f

/** O Pokémon da posição no topo: o render do HOME, como no banner do parceiro. */
private val RenderDoTopo = 132.dp

private val FormaDoTopo = RoundedCornerShape(bottomStart = Raios.especie, bottomEnd = Raios.especie)

/**
 * A `WeeklyCloseScreen` (`F4-T09`, docs/03 §3.20), na segunda linguagem desde 29/10/2026:
 * página tingida, a semana no topo em tinta com o Pokémon onde o corredor está, e um cartão
 * branco por bloco de sentido. **Não é notificação**: o roteador da abertura a abre uma vez
 * por semana, sem barra inferior.
 *
 * - **A sequência** (docs/03 §3.20): sessões, XP por origem em pílulas, o tier da posição,
 *   as figurinhas ultrapassadas colando de 80ms em 80ms, o próximo alvo e a semana seguinte.
 *   2 a 3 segundos, uma vez, **interrompível**: um toque salta para o final.
 * - **Dispensa obrigatória:** o `X` no canto, além do voltar do sistema, que durante a
 *   sequência salta para o final, e um segundo toque fecha.
 * - **O pacote:** se a semana deixou figurinhas no pacote do álbum, a ação é abri-lo, e as
 *   figurinhas colam na Pokédex. A estreia da Fase 4 saiu: a Pokédex está na barra desde o
 *   cadastro, e a primeira semana fecha como as outras.
 * - **Movimento reduzido:** o estado final de imediato (docs/02 §6).
 */
@Composable
fun WeeklyCloseScreen(
    aoFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val vm: FechamentoViewModel = viewModel {
        FechamentoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.planoRepositorio,
            container.corridaRepositorio,
            container.especiesRepositorio,
            container.roteadorDaAbertura,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val noPacote by remember(container) { container.pacoteDaPokedex.quantas() }.collectAsStateWithLifecycle(initialValue = 0)

    // Nada a apresentar (o estado mudou entre o roteador e aqui): a tela sai sozinha.
    LaunchedEffect(estado) { if (estado is FechamentoUiState.Nada) aoFechar() }

    WeeklyCloseScreen(
        estado = estado,
        noPacote = noPacote,
        aoFechar = aoFechar,
        aoAbrirPacote = {
            vm.abrirOPacote()
            aoFechar()
        },
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
internal fun WeeklyCloseScreen(
    estado: FechamentoUiState,
    noPacote: Int,
    aoFechar: () -> Unit,
    aoAbrirPacote: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Com conteúdo, quem decide os ícones é a rolagem (a `Sequencia`): o topo em tinta sai.
    if (estado == FechamentoUiState.Carregando) IconesClarosNaBarraDeStatus()
    Box(modifier = modifier.fillMaxSize().background(LocalCoresPokerun.current.superficieAfundada)) {
        when (estado) {
            FechamentoUiState.Carregando -> EsqueletoDoFechamento()
            FechamentoUiState.Nada -> Unit
            FechamentoUiState.Falhou -> Column(
                modifier = Modifier.statusBarsPadding().padding(Espacos.margem),
                verticalArrangement = Arrangement.spacedBy(Espacos.bloco),
            ) {
                Text(
                    text = stringResource(R.string.fechamento_erro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_erro_repetir))
                }
                BotaoPrimario(aoTocar = aoFechar, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.fechamento_fechar))
                }
            }
            is FechamentoUiState.Conteudo -> Sequencia(estado.fechamento, noPacote, aoFechar, aoAbrirPacote)
        }
    }
}

/** O fechamento em espera: o topo em tinta e os cartões, pulsando. */
@Composable
private fun EsqueletoDoFechamento() {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.interno)) {
        Topo(aoFechar = null) {
            BlocoEmEspera(14.dp, Modifier.width(140.dp), cor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
            Spacer(Modifier.height(Espacos.relacionado))
            BlocoEmEspera(48.dp, Modifier.width(200.dp), cor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
        }
        CartoesEmEspera(listOf(96.dp, 160.dp), Modifier.padding(horizontal = Espacos.margem))
    }
}

@Composable
private fun Sequencia(
    fechamento: FechamentoDaTela,
    noPacote: Int,
    aoFechar: () -> Unit,
    aoAbrirPacote: () -> Unit,
) {
    val reduzido = LocalMovimentoReduzido.current
    val total = fechamento.ultrapassadas.size
    var etapa by remember { mutableIntStateOf(if (reduzido) ETAPA_FINAL else 0) }
    var reveladas by remember { mutableIntStateOf(if (reduzido) total else 0) }

    LaunchedEffect(Unit) {
        if (reduzido) return@LaunchedEffect
        for (proxima in 1..ETAPA_FINAL) {
            delay(RESPIRO_ENTRE_ETAPAS_MS)
            etapa = proxima
            if (proxima == ETAPA_SILHUETAS) {
                repeat(total) {
                    delay(INTERVALO_DAS_SILHUETAS_MS)
                    reveladas += 1
                }
            }
        }
    }

    fun saltarParaOFinal() {
        etapa = ETAPA_FINAL
        reveladas = total
    }

    // O voltar do sistema salta para o final; um segundo toque fecha (docs/03 §1).
    BackHandler(enabled = etapa < ETAPA_FINAL) { saltarParaOFinal() }

    val posicao by animateIntAsState(
        targetValue = if (etapa >= ETAPA_ESCADA) fechamento.semana.posicaoDepois else fechamento.semana.posicaoAntes,
        animationSpec = Movimento.lento(reduzido),
        label = "posicao-no-tier",
    )
    val concluida = etapa >= ETAPA_FINAL

    // Enquanto o topo em tinta está sob a barra de status, os ícones são claros; quando ele sai,
    // a barra ganha o chão da página e os ícones voltam a escuros.
    val rolagem = rememberScrollState()
    val limite = with(LocalDensity.current) { RenderDoTopo.roundToPx() }
    val sobreOTopo by remember { derivedStateOf { rolagem.value < limite } }
    IconesClarosNaBarraDeStatus(ativo = sobreOTopo)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (!concluida) Modifier.clickable { saltarParaOFinal() } else Modifier)
            .verticalScroll(rolagem)
            .animateContentSize(Movimento.padrao(reduzido)),
        verticalArrangement = Arrangement.spacedBy(Espacos.interno),
    ) {
        TopoDaSemana(fechamento, mostrarXp = etapa >= ETAPA_XP, aoFechar = aoFechar)

        Column(
            modifier = Modifier.padding(horizontal = Espacos.margem),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            // Semana sem treino não mostra "0 de 3 sessões" nem "0 XP": o vazio é não punitivo
            // (docs/03 §3.20), e com regressão quem fala é a frase de recuo.
            if (fechamento.semTreino) {
                if (!fechamento.recuou) CartaoDoVazio(fechamento)
            } else {
                if (etapa >= ETAPA_SESSOES) CartaoDasSessoes(fechamento)
                if (etapa >= ETAPA_XP) CartaoDoXp(fechamento.quebra)
            }
            if (fechamento.recuou && etapa >= ETAPA_ESCADA) CartaoDaRegressao(fechamento)
            if (etapa >= ETAPA_ESCADA) CartaoDaPokedex(fechamento, posicao, reveladas, mostrarFigurinhas = etapa >= ETAPA_SILHUETAS, mostrarProximo = etapa >= ETAPA_ALVO)
            if (etapa >= ETAPA_ALVO) fechamento.proximaSemana?.let { CartaoDaSemanaSeguinte(it) }

            if (concluida) {
                Spacer(Modifier.height(Espacos.relacionado))
                val proxima = fechamento.proximaSemana?.numero ?: (fechamento.semana.numero + 1)
                if (noPacote > 0) {
                    BotaoPrimario(aoTocar = aoAbrirPacote, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.manual_abrir_pacote))
                    }
                    BotaoDeTexto(aoTocar = aoFechar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.fechamento_ver_semana, proxima))
                    }
                } else {
                    BotaoPrimario(aoTocar = aoFechar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.fechamento_ver_semana, proxima))
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(Espacos.bloco))
        }
    }
    if (!sobreOTopo) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(LocalCoresPokerun.current.superficieAfundada),
        )
    }
    }
}

// ---------------------------------------------------------------------------
// O topo em tinta
// ---------------------------------------------------------------------------

/**
 * A peça em tinta: sangra sob a barra de status, com o brilho em `destaque` e a Pokébola em
 * marca d'água do `BannerEscuro`. O `X` mora no canto dela.
 */
@Composable
private fun Topo(aoFechar: (() -> Unit)?, direita: @Composable () -> Unit = {}, conteudo: @Composable () -> Unit) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val claro = MaterialTheme.colorScheme.surface
    val brilho = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaDoTopo)
            .background(tinta)
            .drawBehind {
                drawRect(Brush.horizontalGradient(0.35f to Color.Transparent, 1f to brilho.copy(alpha = OPACIDADE_DO_BRILHO)))
                val raio = size.height * 0.55f
                val centro = Offset(size.width - raio * 0.45f, size.height * 0.62f)
                val traco = raio * 0.14f
                val marca = claro.copy(alpha = OPACIDADE_DA_POKEBOLA)
                drawCircle(marca, raio, centro, style = Stroke(traco))
                drawLine(marca, Offset(centro.x - raio, centro.y), Offset(centro.x + raio, centro.y), traco)
                drawCircle(marca, raio * 0.3f, centro, style = Stroke(traco))
            }
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Espacos.margem, top = Espacos.alvoToque, bottom = Espacos.margem),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) { conteudo() }
            direita()
        }
        if (aoFechar != null) {
            val fechar = stringResource(R.string.fechamento_fechar)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(Espacos.alvoToque)
                    .clickable(onClick = aoFechar)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = fechar
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = claro)
            }
        }
    }
}

@Composable
private fun TopoDaSemana(fechamento: FechamentoDaTela, mostrarXp: Boolean, aoFechar: () -> Unit) {
    val claro = MaterialTheme.colorScheme.surface
    val especie = numeroNaPosicao(fechamento.temporada, fechamento.posicao)
    Topo(
        aoFechar = aoFechar,
        direita = {
            if (especie != null) {
                ImagemDaRede(
                    model = renderDoHome(especie),
                    filterQuality = FilterQuality.Medium,
                    corDaEspera = claro.copy(alpha = 0.18f),
                    modifier = Modifier.padding(end = Espacos.relacionado).requiredSize(RenderDoTopo),
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.fechamento_sobrancelha).uppercase(LocaleDoApp),
            style = Estilos.rotulo,
            color = claro.copy(alpha = OPACIDADE_DO_ROTULO),
        )
        Text(
            text = stringResource(R.string.fechamento_semana, fechamento.semana.numero),
            style = Estilos.numeroXl,
            color = claro,
        )
        if (mostrarXp && !fechamento.semTreino) {
            Text(
                text = stringResource(R.string.manual_resultado_xp, String.format(LocaleDoApp, "%,d", fechamento.xpDaSemana)),
                style = Estilos.numeroM,
                color = claro,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Os cartões
// ---------------------------------------------------------------------------

@Composable
private fun CartaoDoVazio(fechamento: FechamentoDaTela) {
    Cartao {
        Text(
            text = stringResource(R.string.fechamento_vazio, fechamento.semana.numero, fechamento.posicao),
            style = Estilos.corpo,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CartaoDasSessoes(fechamento: FechamentoDaTela) {
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RotuloDoCartao(stringResource(R.string.fechamento_rotulo_sessoes), Modifier.weight(1f))
            Text(
                text = stringResource(R.string.detalhe_fracao, fechamento.sessoesFeitas, fechamento.sessoesPrevistas),
                style = Estilos.numeroS,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (fechamento.segmentos.isNotEmpty()) {
            Spacer(Modifier.height(Espacos.interno))
            BarraDeSessoes(segmentos = fechamento.segmentos, longaoKm = fechamento.longaoKm, longaoCumprido = null)
        }
    }
}

/** O XP da semana por origem: cada origem enche a pílula na proporção do que rendeu. */
@Composable
private fun CartaoDoXp(quebra: QuebraDoXp) {
    val origens = listOf(
        R.string.fechamento_quebra_volume to quebra.volume,
        R.string.fechamento_quebra_longao to quebra.longao,
        R.string.fechamento_quebra_pace to quebra.pace,
        R.string.fechamento_quebra_recordes to quebra.recordes,
    ).filter { it.second > 0 }
    if (origens.isEmpty()) return
    val maior = origens.maxOf { it.second }.toFloat()
    Cartao {
        RotuloDoCartao(stringResource(R.string.fechamento_rotulo_xp))
        Spacer(Modifier.height(Espacos.interno))
        Column(verticalArrangement = Arrangement.spacedBy(Espacos.interno)) {
            origens.forEach { (rotulo, valor) ->
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.minimo)) {
                    Row {
                        Text(stringResource(rotulo), style = Estilos.corpo, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.pokedex_xp, String.format(LocaleDoApp, "%,d", valor)),
                            style = Estilos.corpoForte,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    BarraDePilula(fracao = valor / maior, cor = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * O lugar na Pokédex: o tier da posição com a fração em pílula, as figurinhas ultrapassadas
 * colando em sequência e a vaga do próximo com o XP que falta. As mesmas figurinhas do álbum.
 */
@Composable
private fun CartaoDaPokedex(
    fechamento: FechamentoDaTela,
    posicao: Int,
    reveladas: Int,
    mostrarFigurinhas: Boolean,
    mostrarProximo: Boolean,
) {
    val tier = tierDaPosicao(fechamento.temporada, posicao)
    Cartao {
        if (tier != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotuloDoCartao(stringResource(R.string.parceiro_tier, tier.ordem, tier.nome), Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.detalhe_fracao, tier.dentro, tier.total),
                    style = Estilos.numeroS,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Espacos.relacionado))
            BarraDePilula(fracao = tier.dentro.toFloat() / tier.total, cor = MaterialTheme.colorScheme.primary)
        }

        if (mostrarFigurinhas && fechamento.ultrapassadas.isNotEmpty()) {
            Spacer(Modifier.height(Espacos.margem))
            Text(
                text = pluralStringResource(R.plurals.fechamento_ultrapassou, fechamento.totalUltrapassadas, fechamento.totalUltrapassadas),
                style = Estilos.corpoForte,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Espacos.relacionado))
            GradeDeFigurinhas(fechamento.ultrapassadas) { indice, celula, modificador ->
                if (indice < reveladas) {
                    FigurinhaColada(numero = celula.numero, nome = fechamento.nomes[celula.numero], tipos = emptyList(), aoTocar = {}, modifier = modificador)
                } else {
                    FigurinhaVaga(numero = celula.numero, nome = fechamento.nomes[celula.numero], aoTocar = {}, modifier = modificador)
                }
            }
            val restantes = fechamento.totalUltrapassadas - fechamento.ultrapassadas.size
            if (restantes > 0) {
                Spacer(Modifier.height(Espacos.relacionado))
                Text(
                    text = pluralStringResource(R.plurals.fechamento_e_mais, restantes, restantes),
                    style = Estilos.apoio,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // O próximo alvo é a vaga com o XP que falta: com recuo, ele já está na frase da regressão.
        val proximo = fechamento.proximoAlvo
        val falta = fechamento.faltamXp
        if (mostrarProximo && proximo != null && falta != null && !fechamento.recuou) {
            Spacer(Modifier.height(Espacos.margem))
            RotuloDoCartao(stringResource(R.string.fechamento_rotulo_proximo))
            Spacer(Modifier.height(Espacos.relacionado))
            GradeDeFigurinhas(listOf(proximo)) { _, numero, modificador ->
                FigurinhaVaga(
                    numero = numero,
                    nome = fechamento.nomes[numero],
                    aoTocar = {},
                    proximo = true,
                    falta = stringResource(R.string.pokedex_xp, String.format(LocaleDoApp, "%,d", falta)),
                    modifier = modificador,
                )
            }
        }
    }
}

/** A linguagem de recuo de docs/03 §3.20 (RN-37): a coleção está inteira, e o alvo se retoma. */
@Composable
private fun CartaoDaRegressao(fechamento: FechamentoDaTela) {
    val alvo = fechamento.proximoAlvo?.let { numero ->
        val nome = fechamento.nomes[numero] ?: stringResource(R.string.especie_numero, numero)
        fechamento.faltamXp?.let { stringResource(R.string.fechamento_regressao_alvo, nome, String.format(LocaleDoApp, "%,d", it)) }
    }.orEmpty()
    Cartao {
        Text(
            text = stringResource(
                R.string.fechamento_regressao,
                fechamento.semanasSemCorrer ?: 0,
                fechamento.quedaEmPorcento ?: 0,
                fechamento.posicao,
                alvo,
            ),
            style = Estilos.corpo,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A semana seguinte em números: as sessões e o longão, sem frase. */
@Composable
private fun CartaoDaSemanaSeguinte(proxima: ResumoDaProximaSemana) {
    Cartao {
        RotuloDoCartao(stringResource(R.string.fechamento_semana, proxima.numero))
        Spacer(Modifier.height(Espacos.relacionado))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacos.bloco)) {
            NumeroComLegenda(proxima.sessoes.toString(), pluralStringResource(R.plurals.fechamento_sessoes_seguinte, proxima.sessoes))
            proxima.longaoKm?.let { NumeroComLegenda(formatarKm(it), stringResource(R.string.fechamento_longao_km)) }
        }
    }
}

@Composable
private fun NumeroComLegenda(numero: String, legenda: String) {
    Column {
        Text(numero, style = Estilos.numeroM, color = MaterialTheme.colorScheme.onSurface)
        Text(legenda, style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A grade de três colunas das figurinhas, em linhas: são no máximo nove. */
@Composable
private fun <T> GradeDeFigurinhas(itens: List<T>, figurinha: @Composable (Int, T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        itens.chunked(COLUNAS).forEachIndexed { linha, grupo ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado), modifier = Modifier.heightIn(min = 0.dp)) {
                grupo.forEachIndexed { coluna, item -> figurinha(linha * COLUNAS + coluna, item, Modifier.weight(1f)) }
                repeat(COLUNAS - grupo.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// A escada da temporada
// ---------------------------------------------------------------------------

private class TierDaPosicao(val ordem: Int, val nome: String, val dentro: Int, val total: Int)

private fun tierDaPosicao(temporada: Temporada, posicao: Int): TierDaPosicao? {
    var antes = 0
    temporada.tiers.sortedBy { it.ordem }.forEach { tier ->
        if (posicao <= antes + tier.especies.size) return TierDaPosicao(tier.ordem, tier.nome, posicao - antes, tier.especies.size)
        antes += tier.especies.size
    }
    return null
}

private fun numeroNaPosicao(temporada: Temporada, posicao: Int): Int? =
    temporada.tiers.sortedBy { it.ordem }.flatMap { it.especies }.getOrNull(posicao - 1)

private fun renderDoHome(numero: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$numero.png"

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private val TemporadaDoPreview = Temporada(
    id = "kanto-2026", nome = "Kanto 2026", regiao = "Kanto",
    inicio = java.time.Instant.parse("2026-01-01T03:00:00Z"),
    fim = java.time.Instant.parse("2027-01-01T03:00:00Z"),
    fuso = java.time.ZoneId.of("America/Sao_Paulo"),
    ativa = true, totalEspecies = 12,
    tiers = listOf(
        Tier(1, "Passo", "", listOf(10, 11, 13, 14, 16, 17), 100),
        Tier(2, "Trote", "", listOf(19, 20, 21, 22, 23, 24), 250),
    ),
)

private fun fechamentoDeExemplo(antes: Int, depois: Int, feitas: Int = 3) = FechamentoDaTela(
    temporada = TemporadaDoPreview,
    segmentos = emptyList(),
    longaoKm = 11.0,
    semana = SemanaAApresentar("p_7", "p", 7, antes, depois, estreia = false),
    xpDaSemana = if (feitas == 0) 0 else 2_450,
    quebra = if (feitas == 0) QuebraDoXp(0, 0, 0, 0) else QuebraDoXp(1_450, 400, 400, 500),
    sessoesFeitas = feitas,
    sessoesPrevistas = 3,
    kmFeito = 24.0,
    posicao = depois,
    colecaoMax = maxOf(antes, depois),
    xpTotal = 1_800,
    ultrapassadas = (antes + 1..depois).mapNotNull { posicao ->
        CelulaDaPokedex(posicao, posicao + 9, 1, "Passo", EstadoDaCelula.ALCANCADA, 0, 0)
    },
    totalUltrapassadas = (depois - antes).coerceAtLeast(0),
    nomes = (1..30).associateWith { "Especie $it" },
    proximoAlvo = depois + 10,
    faltamXp = 400,
    semanasSemCorrer = null,
    quedaEmPorcento = null,
    proximaSemana = ResumoDaProximaSemana(8, 3, 11.0),
)

@Composable
private fun FechamentoPreview(fechamento: FechamentoDaTela, noPacote: Int = 0) {
    PokerunTheme {
        WeeklyCloseScreen(
            estado = FechamentoUiState.Conteudo(fechamento),
            noPacote = noPacote,
            aoFechar = {},
            aoAbrirPacote = {},
            aoTentarDeNovo = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun FechamentoComProgressoPreview() = FechamentoPreview(fechamentoDeExemplo(antes = 3, depois = 7), noPacote = 4)

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun FechamentoSemTreinoPreview() = FechamentoPreview(fechamentoDeExemplo(antes = 7, depois = 7, feitas = 0))

@Preview(showBackground = true, widthDp = 320, heightDp = 1400, fontScale = 2.0f)
@Composable
private fun FechamentoFonteGrandePreview() = FechamentoPreview(fechamentoDeExemplo(antes = 3, depois = 7))
