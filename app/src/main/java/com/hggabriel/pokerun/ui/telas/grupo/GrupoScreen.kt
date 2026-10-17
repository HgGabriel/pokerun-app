package com.hggabriel.pokerun.ui.telas.grupo

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.componentes.BotaoPrimario
import com.hggabriel.pokerun.ui.componentes.BotaoSecundario
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeAba
import com.hggabriel.pokerun.ui.componentes.ESCALA_QUE_EMPILHA
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.theme.EstiloDado
import java.util.Locale

private val Goteira = 16.dp
private val AlvoDeToque = 48.dp
private val EspacoDepoisDoCabecalho = 24.dp
private val EspacoEntreBlocos = 16.dp
private val EspacoEntreLinhas = 8.dp
private val FolgaDoFim = 24.dp
private val TamanhoDoAvatar = 48.dp
private val ColunaDaPosicao = 28.dp

/**
 * A `SocialLeaderboardScreen` (`F2-T12`, docs/03 §3.16), raiz da aba Grupo: troca o
 * `EmConstrucao` dela e mantém a engrenagem (docs/02 §10.1).
 *
 * O ranking ordena pelo XP da semana (RN-18), com a aderência ao lado; o volume em km é
 * um bloco à parte, que não ordena (RN-09). Entre os dois, o desafio coletivo da semana
 * e as Insígnias de Ginásio (`F2-T13`).
 */
@Composable
fun GrupoScreen(
    aoAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
    vm: GrupoViewModel = grupoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    GrupoScreen(
        estado = estado,
        aoAbrirAjustes = aoAbrirAjustes,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun GrupoScreen(
    estado: GrupoUiState,
    aoAbrirAjustes: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conteudo = estado as? GrupoUiState.Conteudo

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FolgaDoFim)) {
            item {
                CabecalhoDeAba(
                    aba = stringResource(R.string.destino_grupo),
                    titulo = conteudo?.nomeDoPlano ?: stringResource(R.string.grupo_titulo),
                    aoAbrirAjustes = aoAbrirAjustes,
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                GrupoUiState.Carregando -> item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = Goteira))
                }
                GrupoUiState.SemPlano -> mensagem(R.string.grupo_sem_plano)
                GrupoUiState.Falhou -> falhou(aoTentarDeNovo)
                is GrupoUiState.Conteudo -> conteudo(estado)
            }
        }
    }
}

private fun LazyListScope.conteudo(estado: GrupoUiState.Conteudo) {
    if (estado.sozinho) {
        item { Sozinho(estado.codigoConvite) }
    } else {
        item { Rotulo(R.string.grupo_ranking_rotulo) }
        items(estado.linhas, key = { it.ranking.uid }) { linha ->
            LinhaDoRanking(linha)
            HorizontalDivider(modifier = Modifier.padding(horizontal = Goteira), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    estado.desafio?.let { desafio ->
        item {
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            Desafio(desafio)
        }
    }

    if (!estado.sozinho) {
        // RN-09: o volume é informativo e não ordena. A ordem é a do ranking.
        item {
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            Rotulo(R.string.grupo_volume_rotulo)
        }
        items(estado.linhas, key = { "km-${it.ranking.uid}" }) { linha ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Goteira, vertical = EspacoEntreLinhas / 2)) {
                Text(
                    text = linha.ranking.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.grupo_km, formatarKm(linha.ranking.kmFeito, LocaleDoApp)),
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * O desafio coletivo de docs/03 §3.16: a meta do grupo, a barra somada e as insígnias.
 * A barra não anima (docs/02 §6), e o progresso também sai em número.
 */
@Composable
private fun Desafio(desafio: BlocoDoDesafio) {
    val locale = LocaleDoApp
    val meta = formatarKm(desafio.meta, locale)
    val acumulado = formatarKm(desafio.acumulado, locale)
    Column(modifier = Modifier.padding(horizontal = Goteira)) {
        Text(
            text = stringResource(R.string.grupo_desafio_rotulo).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = stringResource(R.string.grupo_desafio_meta, meta),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        LinearProgressIndicator(
            progress = { if (desafio.meta > 0) (desafio.acumulado / desafio.meta).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = stringResource(R.string.grupo_desafio_progresso, acumulado, meta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (desafio.concluido) {
            Spacer(Modifier.height(EspacoEntreLinhas))
            Text(
                text = desafio.insigniaDaSemana?.let { stringResource(R.string.grupo_desafio_cumprido_insignia, stringResource(nomeDaInsignia(it))) }
                    ?: stringResource(R.string.grupo_desafio_cumprido),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(EspacoEntreLinhas))
        Text(
            text = if (desafio.insignias.isEmpty()) {
                stringResource(R.string.grupo_insignias_nenhuma)
            } else {
                stringResource(R.string.grupo_insignias, desafio.insignias.map { stringResource(nomeDaInsignia(it)) }.joinToString(", "))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun nomeDaInsignia(id: String): Int = when (id) {
    "rocha" -> R.string.insignia_rocha
    "cascata" -> R.string.insignia_cascata
    "trovao" -> R.string.insignia_trovao
    "arco_iris" -> R.string.insignia_arco_iris
    "alma" -> R.string.insignia_alma
    "pantano" -> R.string.insignia_pantano
    "vulcao" -> R.string.insignia_vulcao
    else -> R.string.insignia_terra
}

@Composable
private fun Rotulo(texto: Int) {
    Text(
        text = stringResource(texto).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Goteira, end = Goteira, bottom = EspacoEntreLinhas),
    )
}

/**
 * Uma linha de docs/03 §3.16.1: posição, avatar, nome com a espécie embaixo, XP da semana
 * e a fração de sessões. A partir de [ESCALA_QUE_EMPILHA], XP e fração descem para baixo
 * do nome.
 */
@Composable
private fun LinhaDoRanking(linha: LinhaDoGrupo) {
    val r = linha.ranking
    val xp = stringResource(R.string.grupo_xp, String.format(LocaleDoApp, "%,d", r.xpSemana))
    val fracao = stringResource(R.string.grupo_fracao, r.sessoesFeitas, r.sessoesPrevistas)
    val especie = linha.numeroDaEspecie?.let { numero ->
        linha.nomeDaEspecie?.let { stringResource(R.string.especie_numero_nome, numero, it) }
            ?: stringResource(R.string.especie_numero, numero)
    }
    val posicaoNaPokedex = r.posicaoPokedex?.let { stringResource(R.string.grupo_posicao_pokedex, it) }
    val descricao = stringResource(R.string.grupo_linha_descricao, r.posicao, r.nome, xp, fracao) +
        (especie?.let { ". $it" } ?: "") + (posicaoNaPokedex?.let { ". $it" } ?: "")
    val empilhado = LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlvoDeToque)
            .padding(horizontal = Goteira, vertical = EspacoEntreLinhas)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = r.posicao.toString(),
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(ColunaDaPosicao),
        )
        Box(modifier = Modifier.size(TamanhoDoAvatar), contentAlignment = Alignment.Center) {
            if (linha.sprite != null) {
                AsyncImage(model = linha.sprite, contentDescription = null, modifier = Modifier.size(TamanhoDoAvatar))
            }
        }
        Spacer(Modifier.width(EspacoEntreLinhas))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = r.nome, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (especie != null) {
                Text(text = especie, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (posicaoNaPokedex != null) {
                Text(text = posicaoNaPokedex, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (empilhado) {
                Text(text = "$xp · $fracao", style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (!empilhado) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = xp, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
                Text(text = fracao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** O vazio de membro único (docs/03 §3.16), com o código e o compartilhar. */
@Composable
private fun Sozinho(codigo: String) {
    val contexto = LocalContext.current
    val convite = stringResource(R.string.detalhe_compartilhar_texto, codigo)
    EstadoVazio(modifier = Modifier.padding(horizontal = Goteira)) {
        Text(
            text = stringResource(R.string.grupo_sozinho, codigo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(EspacoEntreBlocos))
        BotaoSecundario(
            aoTocar = {
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, convite)
                }
                contexto.startActivity(Intent.createChooser(envio, null))
            },
        ) {
            Text(stringResource(R.string.grupo_compartilhar))
        }
    }
}

/** O vazio sem plano ativo, sobre o padrão de Pokébolas (docs/02 §4.2). */
private fun LazyListScope.mensagem(texto: Int) {
    item {
        EstadoVazio(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(texto),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun LazyListScope.falhou(aoTentarDeNovo: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = Goteira)) {
            Text(
                text = stringResource(R.string.grupo_erro_corpo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            BotaoPrimario(aoTocar = aoTentarDeNovo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_erro_repetir))
            }
        }
    }
}

@Composable
private fun grupoViewModel(): GrupoViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        GrupoViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.planoRepositorio,
            container.progressoRepositorio,
            container.temporadaRepositorio,
            container.especiesRepositorio,
            container.desafioRepositorio,
        )
    }
}
