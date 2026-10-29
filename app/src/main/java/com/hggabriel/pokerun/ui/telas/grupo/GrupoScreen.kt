package com.hggabriel.pokerun.ui.telas.grupo

import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.dominio.regras.Insignias
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.DesafioColetivo
import com.hggabriel.pokerun.ui.componentes.BannerEscuro
import com.hggabriel.pokerun.ui.componentes.BarraDePilula
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.TituloDoBanner
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun
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
private val FolgaDoFim = 24.dp
private val TamanhoDoAvatar = 56.dp
/**
 * A arte oficial ocupa o quadro de ponta a ponta: maior que a vaga, ela saía cortada na borda
 * da imagem (29/10/2026). Dentro da vaga, com um respiro de 3dp de cada lado.
 */
private val SpriteDoAvatar = 50.dp
private val TamanhoDaPosicao = 28.dp
private val EspacoEntreCartoes = 12.dp
private val AlturaDaBarra = 8.dp
private val AlturaDaBarraDoDesafio = 12.dp
private val TamanhoDaInsignia = 28.dp

/** A trilha e a vaga sobre o banner em tinta: `superficie` a 14%. */
private const val OPACIDADE_DA_TRILHA_ESCURA = 0.14f

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
    aoAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    vm: GrupoViewModel = grupoViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    GrupoScreen(
        estado = estado,
        aoAbrirPerfil = aoAbrirPerfil,
        aoTentarDeNovo = vm::tentarDeNovo,
        modifier = modifier,
    )
}

/** A tela sem `ViewModel`, que é o que os previews e a revisão de estado usam. */
@Composable
fun GrupoScreen(
    estado: GrupoUiState,
    aoAbrirPerfil: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conteudo = estado as? GrupoUiState.Conteudo

    // A página tingida, como o detalhe do plano: os blocos são cartões e banner (26/10/2026).
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = FolgaDoFim)) {
            item {
                CabecalhoDeAba(
                    aba = stringResource(R.string.destino_grupo),
                    titulo = conteudo?.nomeDoPlano ?: stringResource(R.string.grupo_titulo),
                    aoAbrirPerfil = aoAbrirPerfil,
                )
                Spacer(Modifier.height(EspacoDepoisDoCabecalho))
            }

            when (estado) {
                // O ranking e as insígnias em espera, na forma dos cartões.
                GrupoUiState.Carregando -> item(key = "esqueleto") {
                    CartoesEmEspera(listOf(220.dp, 120.dp), Modifier.padding(horizontal = Goteira))
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
        item(key = "ranking") {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Ranking(estado.linhas)
            }
        }
    }

    estado.desafio?.let { desafio ->
        item(key = "desafio") {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(EspacoEntreCartoes))
                Desafio(desafio)
            }
        }
    }

    if (!estado.sozinho) {
        // RN-09: o volume é informativo e não ordena. A ordem é a do ranking.
        item(key = "volume") {
            Column(modifier = Modifier.padding(horizontal = Goteira)) {
                Spacer(Modifier.height(EspacoEntreCartoes))
                Volume(estado.linhas)
            }
        }
    }
}

/**
 * O ranking num cartão (26/10/2026): uma linha por membro e, embaixo dela, a pílula do XP
 * em relação ao maior XP da semana. O primeiro leva a posição num círculo cheio em tinta.
 */
@Composable
private fun Ranking(linhas: List<LinhaDoGrupo>) {
    val maior = linhas.maxOfOrNull { it.ranking.xpSemana }?.takeIf { it > 0 } ?: 1L
    Cartao {
        Rotulo(R.string.grupo_ranking_rotulo)
        linhas.forEachIndexed { i, linha ->
            if (i > 0) Spacer(Modifier.height(Espacos.interno))
            LinhaDoRanking(linha, fracaoDoMaior = linha.ranking.xpSemana.toFloat() / maior)
        }
    }
}

/**
 * O desafio coletivo de docs/03 §3.16, no [BannerEscuro]: a meta, o acumulado em número, a
 * barra grossa em pílula e as oito Insígnias de Ginásio desenhadas. A ganha acende na cor
 * do tipo do líder daquele ginásio; a que falta é só a vaga. A barra não anima (docs/02 §6).
 */
@Composable
private fun Desafio(desafio: BlocoDoDesafio) {
    val locale = LocaleDoApp
    val meta = formatarKm(desafio.meta, locale)
    val acumulado = formatarKm(desafio.acumulado, locale)
    val progresso = if (desafio.meta > 0) (desafio.acumulado / desafio.meta).toFloat().coerceIn(0f, 1f) else 0f
    val cumprido = if (desafio.concluido) {
        desafio.insigniaDaSemana?.let { stringResource(R.string.grupo_desafio_cumprido_insignia, stringResource(nomeDaInsignia(it))) }
            ?: stringResource(R.string.grupo_desafio_cumprido)
    } else {
        null
    }
    val insignias = if (desafio.insignias.isEmpty()) {
        stringResource(R.string.grupo_insignias_nenhuma)
    } else {
        stringResource(R.string.grupo_insignias, desafio.insignias.map { stringResource(nomeDaInsignia(it)) }.joinToString(", "))
    }
    val rotulo = stringResource(R.string.grupo_desafio_rotulo)
    val titulo = stringResource(R.string.grupo_desafio_meta, meta)
    val numero = stringResource(R.string.grupo_desafio_progresso, acumulado, meta)
    val descricao = listOfNotNull("$rotulo: $titulo.", "$numero.", cumprido, insignias).joinToString(" ")
    val claro = MaterialTheme.colorScheme.surface
    val destaque = MaterialTheme.colorScheme.primary

    BannerEscuro(descricao = descricao, aoTocar = null) {
        SobrancelhaDoBanner(rotulo)
        Spacer(Modifier.height(Espacos.minimo))
        TituloDoBanner(titulo)
        Spacer(Modifier.height(Espacos.interno))
        Text(text = numero, style = Estilos.numeroM, color = claro)
        Spacer(Modifier.height(Espacos.relacionado))
        Canvas(modifier = Modifier.fillMaxWidth().height(AlturaDaBarraDoDesafio)) {
            val raio = CornerRadius(size.height / 2)
            drawRoundRect(claro.copy(alpha = OPACIDADE_DA_TRILHA_ESCURA), cornerRadius = raio)
            if (progresso > 0f) {
                // Um pouco já é uma pílula inteira, nunca um traço achatado.
                val largura = (size.width * progresso).coerceAtLeast(size.height)
                drawRoundRect(destaque, size = Size(largura, size.height), cornerRadius = raio)
            }
        }
        if (cumprido != null) {
            Spacer(Modifier.height(Espacos.relacionado))
            Text(text = cumprido, style = MaterialTheme.typography.bodyMedium, color = claro)
        }
        Spacer(Modifier.height(Espacos.margem))
        Insignias(ganhas = desafio.insignias.toSet())
    }
}

/**
 * As oito insígnias em fila, na ordem dos ginásios, com a arte oficial (decisão 19). A
 * pendente é a silhueta da própria insígnia a 14%, e não um círculo.
 */
@Composable
private fun Insignias(ganhas: Set<String>) {
    val vaga = MaterialTheme.colorScheme.surface.copy(alpha = OPACIDADE_DA_TRILHA_ESCURA)
    Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        DesafioColetivo.INSIGNIAS.forEach { id ->
            ImagemDaRede(
                model = Insignias.url(id),
                filterQuality = FilterQuality.None,
                recuoDaEspera = 0.1f,
                corDaEspera = vaga,
                colorFilter = if (id in ganhas) null else ColorFilter.tint(vaga, BlendMode.SrcIn),
                modifier = Modifier.size(TamanhoDaInsignia),
            )
        }
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

/**
 * O volume num cartão: nome e km, e embaixo a pílula do km em relação ao maior volume da
 * semana. Não ordena (RN-09): a ordem é a do ranking.
 */
@Composable
private fun Volume(linhas: List<LinhaDoGrupo>) {
    val maior = linhas.maxOfOrNull { it.ranking.kmFeito }?.takeIf { it > 0.0 } ?: 1.0
    Cartao {
        Rotulo(R.string.grupo_volume_rotulo)
        linhas.forEachIndexed { i, linha ->
            if (i > 0) Spacer(Modifier.height(Espacos.interno))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = linha.ranking.nome,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.grupo_km, formatarKm(linha.ranking.kmFeito, LocaleDoApp)),
                    style = EstiloDado,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Espacos.minimo + 2.dp))
            BarraDePilula((linha.ranking.kmFeito / maior).toFloat(), altura = AlturaDaBarra)
        }
    }
}

@Composable
private fun Rotulo(texto: Int) {
    Text(
        text = stringResource(texto).uppercase(Locale.ROOT),
        style = Estilos.rotulo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Espacos.interno),
    )
}

/**
 * Uma linha de docs/03 §3.16.1: posição, avatar, nome com a espécie embaixo, XP da semana
 * e a fração de sessões; embaixo, a pílula do XP. A partir de [ESCALA_QUE_EMPILHA], XP e
 * fração descem para baixo do nome.
 */
@Composable
private fun LinhaDoRanking(linha: LinhaDoGrupo, fracaoDoMaior: Float) {
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
    val lider = r.posicao == 1

    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = descricao }) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = AlvoDeToque),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(TamanhoDaPosicao)
                    .background(
                        if (lider) MaterialTheme.colorScheme.onSurface else LocalCoresPokerun.current.superficieAfundada,
                        FormaCirculo,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = r.posicao.toString(),
                    style = Estilos.rotulo,
                    color = if (lider) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(Espacos.interno))
            Box(
                modifier = Modifier
                    .size(TamanhoDoAvatar)
                    .background(LocalCoresPokerun.current.superficieAfundada, FormaControle),
                contentAlignment = Alignment.Center,
            ) {
                if (linha.sprite != null) {
                    // Arte em alta resolução, e não pixel art: reduzida com suavização.
                    ImagemDaRede(
                        model = linha.sprite,
                        recuoDaEspera = 0.15f,
                        modifier = Modifier.size(SpriteDoAvatar),
                    )
                }
            }
            Spacer(Modifier.width(Espacos.interno))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = r.nome, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurface)
                if (especie != null) {
                    Text(text = especie, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (posicaoNaPokedex != null) {
                    Text(text = posicaoNaPokedex, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (empilhado) {
                    Text(text = "$xp · $fracao", style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (!empilhado) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = xp, style = Estilos.numeroM, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = fracao, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(Espacos.relacionado))
        BarraDePilula(fracaoDoMaior, altura = AlturaDaBarra)
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
