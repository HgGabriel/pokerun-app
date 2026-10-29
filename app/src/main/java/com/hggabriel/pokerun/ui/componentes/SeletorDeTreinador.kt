package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.IndiceDeTreinadores
import com.hggabriel.pokerun.dominio.regras.Treinador
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.FormaCirculo
import com.hggabriel.pokerun.ui.theme.FormaControle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Quanto a busca espera a pessoa parar de digitar. Apagar tudo volta aos grupos sem esperar. */
const val ESPERA_DA_BUSCA_MS = 250L

/** Quatro células por linha, como no mock e na grade da Pokédex. */
const val COLUNAS_DE_TREINADOR = 4

private val MarcaDaEscolha = 14.dp

/**
 * O que a grade mostra. [achados] nulo é "sem busca": a grade mostra os três grupos curados.
 * [total] é zero enquanto a lista não chegou.
 */
data class ResultadoDaBusca(val total: Int = 0, val achados: List<String>? = null)

/**
 * A busca entre os 1.500 treinadores, a mesma no Perfil e no onboarding (29/10/2026).
 *
 * O [termo] muda na hora, e é dele que o campo lê: a tecla aparece sem esperar a busca. O
 * [resultado] espera [espera] ms sem tecla nova e roda em [calculo], fora da main, sobre um
 * [IndiceDeTreinadores] montado uma vez.
 */
class BuscaDeTreinadores(
    escopo: CoroutineScope,
    carregar: suspend () -> List<String>,
    espera: Long = ESPERA_DA_BUSCA_MS,
    calculo: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _termo = MutableStateFlow("")
    val termo: StateFlow<String> = _termo.asStateFlow()

    private val indice: StateFlow<IndiceDeTreinadores?> = flow { emit(IndiceDeTreinadores(carregar())) }
        .flowOn(calculo)
        .catch { emit(IndiceDeTreinadores(emptyList())) }
        .stateIn(escopo, SharingStarted.Eagerly, null)

    @OptIn(FlowPreview::class)
    val resultado: StateFlow<ResultadoDaBusca> = combine(
        _termo.debounce { if (it.isBlank()) 0L else espera }.map { it.trim() }.distinctUntilChanged(),
        indice.filterNotNull(),
    ) { termo, indice ->
        ResultadoDaBusca(total = indice.tamanho, achados = if (termo.isEmpty()) null else indice.buscar(termo))
    }
        .flowOn(calculo)
        .stateIn(escopo, SharingStarted.Eagerly, ResultadoDaBusca())

    fun mudou(texto: String) {
        _termo.value = texto
    }

    /** Regra 1: só um nome da lista vale. Falso enquanto a lista não chegou. */
    fun aceita(nome: String): Boolean = indice.value?.contem(nome) == true
}

/**
 * Os treinadores dentro de um `LazyVerticalGrid` de [COLUNAS_DE_TREINADOR] colunas: sem busca,
 * os três grupos curados, cada um com o rótulo em largura cheia; com busca, os achados. A chave
 * de cada célula é o nome, e só as células à vista são compostas.
 */
fun LazyGridScope.gradeDeTreinadores(
    achados: List<String>?,
    escolhido: String?,
    aoEscolher: (String) -> Unit,
) {
    when {
        achados == null -> Treinador.grupos.forEach { grupo ->
            item(key = ":grupo:${grupo.nome}", span = { GridItemSpan(maxLineSpan) }, contentType = "rotulo") {
                RotuloDoCartao(grupo.nome, Modifier.padding(top = Espacos.interno, bottom = Espacos.minimo))
            }
            items(grupo.treinadores, key = { it }, contentType = { "celula" }) { nome ->
                CelulaDeTreinador(nome, nome == escolhido, aoEscolher)
            }
        }
        achados.isEmpty() -> item(key = ":nenhum", span = { GridItemSpan(maxLineSpan) }, contentType = "nenhum") {
            Text(
                stringResource(R.string.treinador_nenhum),
                style = Estilos.corpo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Espacos.interno),
            )
        }
        else -> {
            item(key = ":resultados", span = { GridItemSpan(maxLineSpan) }, contentType = "rotulo") {
                RotuloDoCartao(stringResource(R.string.treinador_resultados), Modifier.padding(top = Espacos.interno, bottom = Espacos.minimo))
            }
            items(achados, key = { it }, contentType = { "celula" }) { nome ->
                CelulaDeTreinador(nome, nome == escolhido, aoEscolher)
            }
        }
    }
}

/** A busca entre os [total] treinadores. O texto vem de [BuscaDeTreinadores.termo]. */
@Composable
fun CampoDeBuscaDeTreinador(
    termo: String,
    total: Int,
    aoBuscar: (String) -> Unit,
    modifier: Modifier = Modifier,
    aoMudarFoco: (Boolean) -> Unit = {},
) {
    val foco = LocalFocusManager.current
    val dica = stringResource(R.string.treinador_busca, String.format(LocaleDoApp, "%,d", total))
    Row(
        modifier = modifier
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
            value = termo,
            onValueChange = aoBuscar,
            singleLine = true,
            textStyle = Estilos.corpoForte.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { aoMudarFoco(it.isFocused) }
                .semantics { contentDescription = dica },
            decorationBox = { campo ->
                Box {
                    if (termo.isEmpty()) Text(dica, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    campo()
                }
            },
        )
    }
}

/** A célula do treinador: o corpo inteiro sobre o branco; escolhido, em tinta. */
@Composable
private fun CelulaDeTreinador(nome: String, escolhido: Boolean, aoEscolher: (String) -> Unit) {
    val rotulo = Treinador.rotulo(nome)
    CelulaDeEscolha(
        escolhida = escolhido,
        fundo = if (escolhido) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        rotulo = rotulo,
        corDoRotulo = if (escolhido) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        falado = rotulo,
        aoTocar = { aoEscolher(nome) },
    ) {
        SpriteDoTreinador(nome, Modifier.offset(y = 6.dp))
    }
}

/**
 * Uma célula de escolha: bloco cheio com a figura e o rótulo embaixo; escolhida, vira tinta
 * com o ponto em `destaque` no canto (a "tinta como segunda voz" da segunda linguagem).
 */
@Composable
internal fun CelulaDeEscolha(
    escolhida: Boolean,
    fundo: Color,
    rotulo: String,
    corDoRotulo: Color,
    falado: String,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
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

/**
 * Estica o item [lado] para cada lado, por cima do recheio horizontal de uma grade: o item de
 * largura cheia que precisa da borda da tela (a barra de voltar, o cabeçalho) passa por cima
 * da margem que as células usam.
 */
fun Modifier.sangria(lado: Dp): Modifier = layout { medivel, restricoes ->
    val extra = lado.roundToPx()
    val largura = restricoes.maxWidth + 2 * extra
    val colocavel = medivel.measure(restricoes.copy(minWidth = largura, maxWidth = largura))
    layout(restricoes.maxWidth, colocavel.height) { colocavel.place(-extra, 0) }
}
