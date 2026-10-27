package com.hggabriel.pokerun.ui.telas.fechamento

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import com.hggabriel.pokerun.dominio.regras.CelulaDaPokedex
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.dominio.regras.QuebraDoXp
import com.hggabriel.pokerun.dominio.regras.SemanaAApresentar
import com.hggabriel.pokerun.ui.componentes.BarraDeSessoes
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.Escada
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.VarianteDaEscada
import com.hggabriel.pokerun.ui.componentes.escadaDe
import com.hggabriel.pokerun.ui.telas.pokedex.FigurinhaColada
import com.hggabriel.pokerun.ui.telas.pokedex.FigurinhaVaga
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalMovimentoReduzido
import com.hggabriel.pokerun.ui.theme.Movimento
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import kotlinx.coroutines.delay

/** As etapas da sequência de docs/03 §3.20, e a final, que é o estado que o toque salta. */
private const val ETAPA_SESSOES = 1
private const val ETAPA_XP = 2
private const val ETAPA_ESCADA = 3
private const val ETAPA_SILHUETAS = 4
private const val ETAPA_ALVO = 5
private const val ETAPA_FINAL = ETAPA_ALVO

/** O intervalo entre uma silhueta e a seguinte (docs/02 §6): cada uma tem um nome. */
private const val INTERVALO_DAS_SILHUETAS_MS = 80L

/** O respiro entre as etapas: 5 etapas e as silhuetas cabem nos 2 a 3 segundos da spec. */
private const val RESPIRO_ENTRE_ETAPAS_MS = 320L

private const val COLUNAS = 3

/**
 * A `WeeklyCloseScreen` (`F4-T09`, docs/03 §3.20): tela cheia que consolida a semana, sem
 * barra inferior. **Não é notificação**: o roteador da abertura a abre uma vez por semana.
 *
 * - **A sequência** (docs/03 §3.20): sessões, XP com a quebra por origem, a escada, as
 *   silhuetas ultrapassadas ganhando cor de 80ms em 80ms, o próximo alvo e a semana
 *   seguinte. 2 a 3 segundos, uma vez, **interrompível**: um toque salta para o final.
 * - **Dispensa obrigatória:** o `X` no canto, além do voltar do sistema, que durante a
 *   sequência salta para o final, e um segundo toque fecha.
 * - **Movimento reduzido:** o estado final de imediato (docs/02 §6).
 * - **A estreia** (docs/03 §3.20.1) é a mesma sequência com a escada partindo do `#001`, e
 *   termina em `Abrir a Pokédex`.
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
    val especies = container.especiesRepositorio

    // Nada a apresentar (o estado mudou entre o roteador e aqui): a tela sai sozinha.
    LaunchedEffect(estado) { if (estado is FechamentoUiState.Nada) aoFechar() }

    WeeklyCloseScreen(
        estado = estado,
        sprite = especies::spriteDeJogo,
        aoFechar = aoFechar,
        aoAbrirAPokedex = {
            vm.abrirAPokedex()
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
    sprite: (Int) -> String,
    aoFechar: () -> Unit,
    aoAbrirAPokedex: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (estado) {
            FechamentoUiState.Carregando, FechamentoUiState.Nada -> Unit
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
            is FechamentoUiState.Conteudo -> Sequencia(estado.fechamento, sprite, aoFechar, aoAbrirAPokedex)
        }
    }
}

@Composable
private fun Sequencia(
    fechamento: FechamentoDaTela,
    sprite: (Int) -> String,
    aoFechar: () -> Unit,
    aoAbrirAPokedex: () -> Unit,
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

    val posicaoNaEscada by animateIntAsState(
        targetValue = if (etapa >= ETAPA_ESCADA) fechamento.semana.posicaoDepois else fechamento.semana.posicaoAntes,
        animationSpec = Movimento.especificacao(Movimento.Duracao.PREENCHIMENTO, Movimento.Curva.padrao, reduzido),
        label = "posicao-na-escada",
    )
    val concluida = etapa >= ETAPA_FINAL

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (!concluida) Modifier.clickable { saltarParaOFinal() } else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacos.margem),
            verticalArrangement = Arrangement.spacedBy(Espacos.bloco),
        ) {
            Spacer(Modifier.height(Espacos.alvoToque))
            Cabecalho(fechamento)

            // Semana sem treino não mostra "0 de 3 sessões" nem "0 XP": o vazio é não punitivo
            // (docs/03 §3.20), e com regressão quem fala é a frase de recuo.
            if (fechamento.semTreino) {
                if (!fechamento.recuou) Vazio(fechamento)
            } else {
                if (etapa >= ETAPA_SESSOES) Sessoes(fechamento)
                if (etapa >= ETAPA_XP) Xp(fechamento.xpDaSemana, fechamento.quebra)
            }

            if (etapa >= ETAPA_ESCADA) {
                Escada(
                    escada = escadaDe(fechamento.temporada, posicaoNaEscada, fechamento.colecaoMax),
                    variante = VarianteDaEscada.MEDIA,
                    nomeDaEspecie = { fechamento.nomes[it] },
                )
            }
            if (etapa >= ETAPA_SILHUETAS && fechamento.ultrapassadas.isNotEmpty()) {
                Silhuetas(fechamento, reveladas, sprite)
            }
            if (fechamento.recuou && etapa >= ETAPA_ESCADA) Regressao(fechamento)
            if (etapa >= ETAPA_ALVO) Proximo(fechamento)

            if (concluida) {
                Spacer(Modifier.height(Espacos.relacionado))
                if (fechamento.semana.estreia) {
                    BotaoPrimario(aoTocar = aoAbrirAPokedex, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.fechamento_abrir_pokedex))
                    }
                } else {
                    BotaoPrimario(aoTocar = aoFechar, modifier = Modifier.fillMaxWidth()) {
                        val proxima = fechamento.proximaSemana?.numero ?: (fechamento.semana.numero + 1)
                        Text(stringResource(R.string.fechamento_ver_semana, proxima))
                    }
                }
                Spacer(Modifier.height(Espacos.bloco))
            }
        }

        // O X no canto: dispensa explícita, 48dp (docs/02 §8).
        val fechar = stringResource(R.string.fechamento_fechar)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopEnd)
                .size(Espacos.alvoToque)
                .clickable(onClick = aoFechar)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = fechar
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.fechamento_x),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Cabecalho(fechamento: FechamentoDaTela) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.minimo)) {
        Text(
            text = stringResource(
                if (fechamento.semana.estreia) R.string.fechamento_sobrancelha_estreia else R.string.fechamento_sobrancelha,
            ).uppercase(LocaleDoApp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.fechamento_semana, fechamento.semana.numero),
            style = Estilos.numeroXl,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Vazio(fechamento: FechamentoDaTela) {
    val proxima = fechamento.proximaSemana
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        Text(
            text = stringResource(R.string.fechamento_vazio, fechamento.semana.numero, fechamento.posicao),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (proxima != null) Text(textoDaProxima(proxima), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Sessoes(fechamento: FechamentoDaTela) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        Text(
            text = stringResource(R.string.fechamento_sessoes, fechamento.sessoesFeitas, fechamento.sessoesPrevistas),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (fechamento.segmentos.isNotEmpty()) {
            BarraDeSessoes(
                segmentos = fechamento.segmentos,
                longaoKm = fechamento.longaoKm,
                longaoCumprido = null,
            )
        }
    }
}

@Composable
private fun Xp(total: Long, quebra: QuebraDoXp) {
    val formatar = { valor: Long -> String.format(LocaleDoApp, "%,d", valor) }
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.minimo)) {
        Text(
            text = stringResource(R.string.fechamento_xp, formatar(total)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        listOf(
            R.string.fechamento_quebra_volume to quebra.volume,
            R.string.fechamento_quebra_longao to quebra.longao,
            R.string.fechamento_quebra_pace to quebra.pace,
            R.string.fechamento_quebra_recordes to quebra.recordes,
        ).filter { it.second > 0 }.forEach { (rotulo, valor) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(rotulo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.pokedex_xp, formatar(valor)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** As silhuetas ultrapassadas, que ganham cor em sequência: itens da Pokédex (docs/02 §4.4), e não a ficha. */
@Composable
private fun Silhuetas(fechamento: FechamentoDaTela, reveladas: Int, sprite: (Int) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        Text(
            text = pluralStringResource(
                R.plurals.fechamento_ultrapassou,
                fechamento.totalUltrapassadas,
                fechamento.totalUltrapassadas,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        fechamento.ultrapassadas.chunked(COLUNAS).forEachIndexed { linha, grupo ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
                grupo.forEachIndexed { coluna, celula ->
                    val indice = linha * COLUNAS + coluna
                    val colorida = indice < reveladas
                    Box(modifier = Modifier.weight(1f)) {
                        // As mesmas figurinhas do álbum: a vaga vira colada em sequência.
                        if (colorida) {
                            FigurinhaColada(numero = celula.numero, nome = fechamento.nomes[celula.numero], tipos = emptyList(), aoTocar = {})
                        } else {
                            FigurinhaVaga(numero = celula.numero, nome = fechamento.nomes[celula.numero], aoTocar = {})
                        }
                    }
                }
                repeat(COLUNAS - grupo.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val restantes = fechamento.totalUltrapassadas - fechamento.ultrapassadas.size
        if (restantes > 0) {
            Text(
                text = pluralStringResource(R.plurals.fechamento_e_mais, restantes, restantes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A linguagem de recuo de docs/03 §3.20 (RN-37): a coleção está inteira, e o alvo se retoma. */
@Composable
private fun Regressao(fechamento: FechamentoDaTela) {
    val alvo = fechamento.proximoAlvo?.let { numero ->
        val nome = fechamento.nomes[numero] ?: stringResource(R.string.especie_numero, numero)
        fechamento.faltamXp?.let { stringResource(R.string.fechamento_regressao_alvo, nome, String.format(LocaleDoApp, "%,d", it)) }
    }.orEmpty()
    Text(
        text = stringResource(
            R.string.fechamento_regressao,
            fechamento.semanasSemCorrer ?: 0,
            fechamento.quedaEmPorcento ?: 0,
            fechamento.posicao,
            alvo,
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Proximo(fechamento: FechamentoDaTela) {
    // Com recuo, o alvo já está na frase da regressão: dizê-lo de novo repetiria a conta.
    val alvo = fechamento.proximoAlvo?.takeIf { !fechamento.recuou }?.let { numero ->
        val nome = fechamento.nomes[numero] ?: stringResource(R.string.especie_numero, numero)
        fechamento.faltamXp?.let { stringResource(R.string.fechamento_alvo, nome, String.format(LocaleDoApp, "%,d", it)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        if (alvo != null) Text(alvo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        val proxima = fechamento.proximaSemana
        if (proxima != null) {
            Text(textoDaProxima(proxima), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun textoDaProxima(proxima: ResumoDaProximaSemana): String =
    if (proxima.longaoKm != null) {
        stringResource(
            R.string.fechamento_proxima_com_longao,
            proxima.numero,
            proxima.sessoes,
            com.hggabriel.pokerun.ui.componentes.formatarKm(proxima.longaoKm),
        )
    } else {
        stringResource(R.string.fechamento_proxima, proxima.numero, proxima.sessoes)
    }

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

private fun fechamentoDeExemplo(antes: Int, depois: Int, estreia: Boolean = false, feitas: Int = 3) = FechamentoDaTela(
    temporada = TemporadaDoPreview,
    segmentos = emptyList(),
    longaoKm = 11.0,
    semana = SemanaAApresentar("p_7", "p", 7, antes, depois, estreia),
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
private fun FechamentoPreview(fechamento: FechamentoDaTela) {
    PokerunTheme {
        WeeklyCloseScreen(
            estado = FechamentoUiState.Conteudo(fechamento),
            sprite = { "" },
            aoFechar = {},
            aoAbrirAPokedex = {},
            aoTentarDeNovo = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun FechamentoComProgressoPreview() = FechamentoPreview(fechamentoDeExemplo(antes = 3, depois = 7))

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun FechamentoSemTreinoPreview() = FechamentoPreview(fechamentoDeExemplo(antes = 7, depois = 7, feitas = 0))

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun FechamentoEstreiaPreview() = FechamentoPreview(fechamentoDeExemplo(antes = 1, depois = 9, estreia = true))

@Preview(showBackground = true, widthDp = 320, heightDp = 900, fontScale = 2.0f)
@Composable
private fun FechamentoFonteGrandePreview() = FechamentoPreview(fechamentoDeExemplo(antes = 3, depois = 7))
