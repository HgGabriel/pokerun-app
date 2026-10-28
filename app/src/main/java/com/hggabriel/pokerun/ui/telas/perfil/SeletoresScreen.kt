package com.hggabriel.pokerun.ui.telas.perfil

import com.hggabriel.pokerun.ui.componentes.CartoesEmEspera
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.LocalAppContainer
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.dominio.modelo.TipoDePokemon
import com.hggabriel.pokerun.dominio.regras.Treinador
import com.hggabriel.pokerun.ui.componentes.BannerEscuro
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.ChipDeTipo
import com.hggabriel.pokerun.ui.componentes.EstadoVazio
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.SobrancelhaDoBanner
import com.hggabriel.pokerun.ui.componentes.SpriteDoTreinador
import com.hggabriel.pokerun.ui.componentes.TituloDoBanner
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import com.hggabriel.pokerun.ui.theme.LocalCoresDeTipo
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/** Quatro células por linha, como no mock e na grade da Pokédex. */
private const val COLUNAS = 4

private val SpriteDaCelula = 64.dp
private val MarcaDaEscolha = 14.dp
private val AlturaDoBanner = 156.dp

// ---------------------------------------------------------------------------
// O seletor de parceiro
// ---------------------------------------------------------------------------

/**
 * O seletor de parceiro (estudo de Progresso e Perfil, d.2 regra 7, mock `perfil-parceiro`).
 *
 * Uma grade por tier, com o nome e a `descricao` do tier. Liberado: sprite de jogo sobre o
 * `tipo-suave`; escolhido: em tinta, com o ponto em `destaque`. O que não foi liberado não
 * aparece: a grade não é a Pokédex. O toque escolhe e salva (decisão 11).
 */
@Composable
fun EscolherParceiroScreen(aoVoltar: () -> Unit, modifier: Modifier = Modifier, vm: EscolherParceiroViewModel = escolherParceiroViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    EscolherParceiroScreen(estado, vm::escolher, modifier, aoVoltar)
}

@Composable
fun EscolherParceiroScreen(estado: SeletorDeParceiro, aoEscolher: (Int) -> Unit, modifier: Modifier = Modifier, aoVoltar: () -> Unit = {}) {
    val cores = LocalCoresDeTipo.current
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            item(key = "voltar") { BarraDeVoltar(aoVoltar) }
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.perfil_sobrancelha)),
                    titulo = stringResource(R.string.parceiro_titulo),
                )
            }
            item(key = "banner") {
                val escolhido = estado.escolhido
                val nome = estado.nomeDoEscolhido ?: escolhido?.let { stringResource(R.string.parceiro_numero, it) }.orEmpty()
                val liberados = pluralStringResource(R.plurals.parceiro_liberados, estado.liberados, estado.liberados)
                BannerEscuro(
                    descricao = "$liberados. $nome",
                    aoTocar = null,
                    brilho = estado.tipoDoEscolhido?.let { cores[it].base } ?: MaterialTheme.colorScheme.primary,
                    sprite = escolhido?.let(::renderDoHome),
                    filtroDoSprite = FilterQuality.Medium,
                    alturaMinima = AlturaDoBanner,
                    modifier = Modifier.padding(horizontal = Espacos.margem),
                ) {
                    SobrancelhaDoBanner(liberados)
                    Spacer(Modifier.height(Espacos.minimo))
                    TituloDoBanner(nome)
                    Spacer(Modifier.height(Espacos.relacionado))
                    if (estado.tipoDoEscolhido != null) ChipDeTipo(estado.tipoDoEscolhido)
                }
            }
            if (estado.carregando) {
                item(key = "esqueleto") {
                    CartoesEmEspera(listOf(280.dp, 280.dp), Modifier.padding(horizontal = Espacos.margem))
                }
            }
            if (!estado.carregando && estado.grade.isEmpty()) {
                item(key = "vazio") {
                    EstadoVazio(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                        Text(stringResource(R.string.parceiro_vazio_titulo), style = Estilos.tituloSecao)
                        Text(stringResource(R.string.parceiro_vazio_texto), style = Estilos.corpo, textAlign = TextAlign.Center)
                    }
                }
            }
            items(estado.grade, key = { "${it.temporadaId}-${it.ordem}" }) { tier ->
                Cartao(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                    RotuloDoCartao(stringResource(R.string.parceiro_tier, tier.ordem, tier.nome))
                    Spacer(Modifier.height(Espacos.minimo))
                    Text(tier.descricao, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Espacos.interno))
                    GradeDeCelulas(tier.especies) { numero, celula ->
                        CelulaDeParceiro(numero, numero == estado.escolhido, estado.tipos[numero], { aoEscolher(numero) }, celula)
                    }
                }
            }
        }
    }
}

/** O render do HOME para o banner, a mesma altura da credencial (decisão 13). */
private fun renderDoHome(numero: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$numero.png"

@Composable
private fun CelulaDeParceiro(numero: Int, escolhido: Boolean, tipo: TipoDePokemon?, aoTocar: () -> Unit, modifier: Modifier) {
    val tinta = MaterialTheme.colorScheme.onSurface
    val fundo = when {
        escolhido -> tinta
        tipo != null -> LocalCoresDeTipo.current[tipo].suave
        else -> LocalCoresPokerun.current.superficieAfundada
    }
    val rotulo = stringResource(R.string.parceiro_numero, numero)
    val falado = stringResource(R.string.parceiro_celula, numero)
    Celula(
        escolhida = escolhido,
        fundo = fundo,
        rotulo = rotulo,
        corDoRotulo = if (escolhido) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        falado = falado,
        aoTocar = aoTocar,
        modifier = modifier,
    ) {
        ImagemDaRede(
            model = spriteDeJogoDe(numero),
            filterQuality = FilterQuality.None,
            modifier = Modifier.size(SpriteDaCelula),
        )
    }
}

// ---------------------------------------------------------------------------
// O seletor de treinador
// ---------------------------------------------------------------------------

/**
 * O seletor de treinador (estudo de Progresso e Perfil, Revisão 3, mock `perfil-treinador`).
 *
 * Banner com o escolhido de corpo inteiro e o parceiro ao lado; a busca entre os 1.500; e
 * os três grupos curados. Com busca, a grade dos achados ocupa o lugar dos grupos.
 */
@Composable
fun EscolherTreinadorScreen(aoVoltar: () -> Unit, modifier: Modifier = Modifier, vm: EscolherTreinadorViewModel = escolherTreinadorViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    EscolherTreinadorScreen(estado, vm::buscaMudou, vm::escolher, modifier, aoVoltar)
}

@Composable
fun EscolherTreinadorScreen(
    estado: SeletorDeTreinador,
    aoBuscar: (String) -> Unit,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier,
    aoVoltar: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            item(key = "voltar") { BarraDeVoltar(aoVoltar) }
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.perfil_sobrancelha)),
                    titulo = stringResource(R.string.treinador_titulo),
                )
            }
            item(key = "banner") { BannerDoTreinador(estado) }
            item(key = "busca") { CampoDeBusca(estado, aoBuscar) }

            val achados = estado.achados
            if (achados != null) {
                item(key = "achados") {
                    Cartao(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                        RotuloDoCartao(stringResource(R.string.treinador_resultados))
                        Spacer(Modifier.height(Espacos.interno))
                        if (achados.isEmpty()) {
                            Text(
                                stringResource(R.string.treinador_nenhum),
                                style = Estilos.corpo,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            GradeDeCelulas(achados) { nome, celula ->
                                CelulaDeTreinador(nome, nome == estado.escolhido, { aoEscolher(nome) }, celula)
                            }
                        }
                    }
                }
            } else {
                items(Treinador.grupos, key = { it.nome }) { grupo ->
                    Cartao(modifier = Modifier.padding(horizontal = Espacos.margem)) {
                        RotuloDoCartao(grupo.nome)
                        Spacer(Modifier.height(Espacos.interno))
                        GradeDeCelulas(grupo.treinadores) { nome, celula ->
                            CelulaDeTreinador(nome, nome == estado.escolhido, { aoEscolher(nome) }, celula)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerDoTreinador(estado: SeletorDeTreinador) {
    val nome = estado.escolhido?.let(Treinador::rotulo) ?: stringResource(R.string.perfil_sem_escolha)
    val claro = MaterialTheme.colorScheme.surface
    BannerEscuro(
        descricao = "${stringResource(R.string.treinador_titulo)} $nome",
        aoTocar = null,
        alturaMinima = AlturaDoBanner,
        modifier = Modifier.padding(horizontal = Espacos.margem),
    ) {
        SobrancelhaDoBanner(stringResource(R.string.treinador_titulo))
        Spacer(Modifier.height(Espacos.minimo))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                TituloDoBanner(nome)
                if (estado.parceiro != null) {
                    Spacer(Modifier.height(Espacos.relacionado))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.treinador_com), style = Estilos.apoio, color = claro.copy(alpha = 0.72f))
                        ImagemDaRede(
                            model = spriteDeJogoDe(estado.parceiro),
                            filterQuality = FilterQuality.None,
                            corDaEspera = claro.copy(alpha = 0.18f),
                            modifier = Modifier.size(36.dp),
                        )
                        Text(estado.nomeDoParceiro.orEmpty(), style = Estilos.apoio, color = claro.copy(alpha = 0.72f))
                    }
                }
            }
            SpriteDoTreinador(estado.escolhido, Modifier.offset(y = (-8).dp))
        }
    }
}

@Composable
private fun CampoDeBusca(estado: SeletorDeTreinador, aoBuscar: (String) -> Unit) {
    val foco = LocalFocusManager.current
    val total = String.format(LocaleDoApp, "%,d", estado.total)
    val dica = stringResource(R.string.treinador_busca, total)
    Row(
        modifier = Modifier
            .padding(horizontal = Espacos.margem)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(FormaControle)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = estado.busca,
            onValueChange = aoBuscar,
            singleLine = true,
            textStyle = Estilos.corpoForte.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = dica },
            decorationBox = { campo ->
                Box {
                    if (estado.busca.isEmpty()) Text(dica, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    campo()
                }
            },
        )
    }
}

@Composable
private fun CelulaDeTreinador(nome: String, escolhido: Boolean, aoTocar: () -> Unit, modifier: Modifier) {
    val rotulo = Treinador.rotulo(nome)
    Celula(
        escolhida = escolhido,
        fundo = if (escolhido) MaterialTheme.colorScheme.onSurface else LocalCoresPokerun.current.superficieAfundada,
        rotulo = rotulo,
        corDoRotulo = if (escolhido) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        falado = rotulo,
        aoTocar = aoTocar,
        modifier = modifier,
    ) {
        SpriteDoTreinador(nome, Modifier.offset(y = 6.dp))
    }
}

// ---------------------------------------------------------------------------
// Peças comuns
// ---------------------------------------------------------------------------

/** A grade de quatro colunas, em linhas: a lista já é preguiçosa por cartão. */
@Composable
private fun <T> GradeDeCelulas(itens: List<T>, celula: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
        itens.chunked(COLUNAS).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacos.relacionado)) {
                linha.forEach { item -> celula(item, Modifier.weight(1f)) }
                repeat(COLUNAS - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Uma célula de escolha: bloco cheio com a figura e o rótulo embaixo; escolhida, vira tinta
 * com o ponto em `destaque` no canto (a "tinta como segunda voz" da segunda linguagem).
 */
@Composable
private fun Celula(
    escolhida: Boolean,
    fundo: androidx.compose.ui.graphics.Color,
    rotulo: String,
    corDoRotulo: androidx.compose.ui.graphics.Color,
    falado: String,
    aoTocar: () -> Unit,
    modifier: Modifier,
    figura: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f / 1.15f)
            .clip(FormaControle)
            .background(fundo)
            .clickable(role = Role.RadioButton, onClick = aoTocar)
            .semantics {
                contentDescription = falado
                selected = escolhida
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 18.dp), contentAlignment = Alignment.Center) { figura() }
        Text(
            text = rotulo,
            style = Estilos.legenda,
            color = corDoRotulo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // O rótulo leva o fundo da célula, como no mock: o pé do sprite passa por trás dele.
            modifier = Modifier
                .padding(bottom = Espacos.minimo)
                .background(fundo, FormaControle)
                .padding(horizontal = Espacos.minimo),
        )
        if (escolhida) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(MarcaDaEscolha)
                    .background(MaterialTheme.colorScheme.primary, FormaCirculo),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Fábricas
// ---------------------------------------------------------------------------

@Composable
private fun escolherParceiroViewModel(): EscolherParceiroViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        EscolherParceiroViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.especiesRepositorio,
            container.rascunhoDoPerfil,
        )
    }
}

@Composable
private fun escolherTreinadorViewModel(): EscolherTreinadorViewModel {
    val container = LocalAppContainer.current
    return viewModel {
        EscolherTreinadorViewModel(
            container.autenticacaoRepositorio,
            container.usuarioRepositorio,
            container.temporadaRepositorio,
            container.progressoRepositorio,
            container.especiesRepositorio,
            container.treinadoresRepositorio,
            container.rascunhoDoPerfil,
        )
    }
}
