package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hggabriel.pokerun.ui.componentes.ImagemDaRede
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.rede.spriteDeJogoDe
import com.hggabriel.pokerun.dominio.regras.Ritmo
import com.hggabriel.pokerun.ui.componentes.BustoDeEspecie
import com.hggabriel.pokerun.ui.componentes.CabecalhoDeFicha
import com.hggabriel.pokerun.ui.componentes.Cartao
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.RotuloDoCartao
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.componentes.nomeCurtoDoMes
import com.hggabriel.pokerun.ui.componentes.rotuloCurtoDoDia
import com.hggabriel.pokerun.ui.telas.corridamanual.nome
import com.hggabriel.pokerun.ui.theme.Espacos
import com.hggabriel.pokerun.ui.theme.Estilos
import com.hggabriel.pokerun.ui.theme.LocalCoresPokerun

/*
 * As quatro telas atrás das portas do Progresso (decisão 16). Rotas internas da aba, com o
 * voltar devolvendo à raiz. Cada uma lê o mesmo estado da raiz.
 */

@Composable
private fun PaginaDaPorta(
    titulo: String,
    estado: PainelUiState,
    acao: (@Composable () -> Unit)? = null,
    conteudo: LazyListScope.(PainelUiState.Conteudo) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = LocalCoresPokerun.current.superficieAfundada) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espacos.bloco),
            verticalArrangement = Arrangement.spacedBy(Espacos.interno),
        ) {
            item(key = "cabecalho") {
                CabecalhoDeFicha(
                    sobrancelha = listOf(stringResource(R.string.destino_progresso)),
                    titulo = titulo,
                    acao = acao,
                )
            }
            if (estado is PainelUiState.Conteudo) conteudo(estado)
        }
    }
}

private val Margem = Modifier.padding(horizontal = Espacos.margem)

// ---------------------------------------------------------------------------
// Ritmo: a escada, o pace em média móvel e a eficiência cardíaca
// ---------------------------------------------------------------------------

@Composable
fun RitmoScreen(vm: PainelViewModel = progressoViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    PaginaDaPorta(stringResource(R.string.ritmo_titulo), estado) { conteudo ->
        item(key = "escada") { CartaoDaEscada(conteudo.ritmo, Margem) }
        item(key = "pace") { CartaoDePace(conteudo.pace, Margem) }
        item(key = "eficiencia") { CartaoDeEficiencia(conteudo.eficiencia, Margem) }
    }
}

/**
 * A escada do ritmo da arena, de Slowpoke a Jolteon, com uma marca por corrida no trilho
 * e o número da última (ou da média de 4 semanas, quando houver).
 */
@Composable
private fun CartaoDaEscada(ritmo: RitmoDaTendencia, modifier: Modifier) {
    val destaque = MaterialTheme.colorScheme.primary
    val tinta = MaterialTheme.colorScheme.onSurface
    val afundada = LocalCoresPokerun.current.superficieAfundada
    val doLento = Ritmo.entries.reversed()
    fun posicao(paceSeg: Long): Float = (doLento.indexOf(ritmoDeSeg(paceSeg)) + 0.5f) / doLento.size
    val nome = ritmo.ritmo?.let { stringResource(it.nome) }
    val descricao = if (ritmo.atualSeg != null && nome != null) {
        stringResource(R.string.ritmo_escada_descricao, paceCurto(ritmo.atualSeg.toDouble()), nome, ritmo.marcas.size)
    } else {
        stringResource(R.string.ritmo_escada)
    }
    Cartao(modifier = modifier.clearAndSetSemantics { contentDescription = descricao }) {
        RotuloDoCartao(stringResource(R.string.ritmo_escada))
        Spacer(Modifier.height(Espacos.relacionado))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(ritmo.atualSeg?.let { paceCurto(it.toDouble()) } ?: "0:00", style = Estilos.numeroL, color = tinta)
            Spacer(Modifier.width(Espacos.minimo))
            Text(stringResource(R.string.painel_unidade_pace), style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (nome != null) Text(stringResource(R.string.ritmo_de, nome), style = Estilos.apoio, color = tinta)
        Spacer(Modifier.height(Espacos.interno))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            doLento.forEach { especie ->
                ImagemDaRede(
                    model = spriteDeJogoDe(especie.numero),
                    filterQuality = FilterQuality.None,
                    alpha = if (especie == ritmo.ritmo) 1f else 0.55f,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(Espacos.relacionado))
        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
            val raio = CornerRadius(size.height / 2)
            drawRoundRect(afundada, cornerRadius = raio)
            ritmo.atualSeg?.let { atual ->
                drawRoundRect(destaque, size = Size(size.width * posicao(atual), size.height), cornerRadius = raio)
            }
            ritmo.marcas.forEachIndexed { i, marca ->
                val x = size.width * posicao(marca)
                drawCircle(if (i == ritmo.marcas.lastIndex) tinta else tinta.copy(alpha = 0.35f), size.height * 0.3f, Offset(x, size.height / 2))
            }
        }
        if (ritmo.marcas.isNotEmpty()) {
            Spacer(Modifier.height(Espacos.minimo))
            Box(modifier = Modifier.fillMaxWidth()) {
                val primeira = ritmo.marcas.first()
                val ultima = ritmo.marcas.last()
                Text(paceCurto(primeira.toDouble()), style = Estilos.legenda, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ritmo.marcas.size > 1) {
                    Text(
                        paceCurto(ultima.toDouble()),
                        style = Estilos.legenda.copy(fontWeight = FontWeight.Bold),
                        color = tinta,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

private fun ritmoDeSeg(seg: Long): Ritmo = com.hggabriel.pokerun.dominio.regras.ritmoDe(seg)

// ---------------------------------------------------------------------------
// Selos: o ano em selos, a sequência e o volume
// ---------------------------------------------------------------------------

@Composable
fun SelosScreen(vm: PainelViewModel = progressoViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    PaginaDaPorta(stringResource(R.string.selos_titulo), estado) { conteudo ->
        item(key = "ano") {
            Cartao(modifier = Margem) {
                BlocoDeHeatmap(conteudo.heatmap, conteudo.hoje)
                Spacer(Modifier.height(Espacos.margem))
                Row(horizontalArrangement = Arrangement.spacedBy(Espacos.bloco)) {
                    Sequencia(stringResource(R.string.selos_sequencia_atual), conteudo.sequencia.atual)
                    Sequencia(stringResource(R.string.selos_sequencia_melhor), conteudo.sequencia.melhor)
                }
            }
        }
        item(key = "volume") { CartaoDeVolume(conteudo.volume, Margem) }
    }
}

@Composable
private fun Sequencia(rotulo: String, semanas: Int) {
    Column {
        RotuloDoCartao(rotulo)
        Text(
            pluralStringResource(R.plurals.progresso_semanas, quantidadeDoPlural(semanas), semanas),
            style = Estilos.numeroM,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Fitas: uma por recorde (decisão 18)
// ---------------------------------------------------------------------------

private val LadoDaFita = 84.dp
private val DiscoDaEspecie = 40.dp

@Composable
fun FitasScreen(vm: PainelViewModel = progressoViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val conquistadas = (estado as? PainelUiState.Conteudo)?.fitas?.count { !it.vaga } ?: 0
    PaginaDaPorta(
        titulo = stringResource(R.string.fitas_titulo),
        estado = estado,
        acao = {
            Text(
                stringResource(R.string.progresso_de_cinco, conquistadas),
                style = Estilos.numeroM,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = Espacos.interno),
            )
        },
    ) { conteudo ->
        items(conteudo.fitas, key = { it.fita.name }) { fita -> CartaoDaFita(fita, conteudo.prova, Margem) }
    }
}

@Composable
private fun CartaoDaFita(fita: FitaDoRecorde, prova: ProvaDoPlano?, modifier: Modifier) {
    val rotulo = stringResource(rotuloDaFita(fita.fita))
    val valor = fita.valor?.let { valorDaFita(fita.fita, it) }
    val unidade = unidadeDaFita(fita.fita)?.let { stringResource(it) }
    val apoio = linhaDaFita(fita)
    val descricao = stringResource(R.string.fitas_descricao, rotulo, listOfNotNull(valor?.let { "$it ${unidade.orEmpty()}".trim() } ?: stringResource(R.string.fitas_vaga), apoio).joinToString(". "))
    Cartao(modifier = modifier.clearAndSetSemantics { contentDescription = descricao }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(LadoDaFita)) {
                // A fita vaga é a silhueta da própria fita, preta a 14%, como a espécie não
                // alcançada da Pokédex. Ela não é tracejada e não é um bloco com texto.
                ImagemDaRede(
                    model = fita.fita.url,
                    recuoDaEspera = 0.1f,
                    colorFilter = if (fita.vaga) ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), BlendMode.SrcIn) else null,
                    modifier = Modifier.size(LadoDaFita),
                )
                fita.especie?.let { numero ->
                    BustoDeEspecie(
                        numero = numero,
                        tamanho = DiscoDaEspecie,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(Espacos.margem))
            Column(modifier = Modifier.weight(1f)) {
                RotuloDoCartao(rotulo)
                if (valor != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(valor, style = Estilos.numeroL, color = MaterialTheme.colorScheme.onSurface)
                        if (unidade != null) {
                            Spacer(Modifier.width(Espacos.minimo))
                            Text(unidade, style = Estilos.corpoForte, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                } else {
                    Text(stringResource(R.string.fitas_vaga), style = Estilos.numeroM, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (apoio != null) Text(apoio, style = Estilos.apoio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                proximaMarca(fita, prova)?.let { proxima ->
                    Spacer(Modifier.height(Espacos.minimo))
                    Text(
                        proxima,
                        style = Estilos.legenda,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(LocalCoresPokerun.current.destaqueSuave, androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .padding(horizontal = Espacos.relacionado, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun rotuloDaFita(fita: FitaDeRecorde): Int = when (fita) {
    FitaDeRecorde.MAIOR_DISTANCIA -> R.string.painel_recorde_distancia
    FitaDeRecorde.MAIOR_DURACAO -> R.string.painel_recorde_duracao
    FitaDeRecorde.MELHOR_PACE -> R.string.painel_recorde_pace
    FitaDeRecorde.MELHOR_1K -> R.string.painel_recorde_1k
    FitaDeRecorde.MELHOR_5K -> R.string.painel_recorde_5k
}

private fun unidadeDaFita(fita: FitaDeRecorde): Int? = when (fita) {
    FitaDeRecorde.MAIOR_DISTANCIA -> R.string.painel_unidade_km
    FitaDeRecorde.MELHOR_PACE -> R.string.painel_unidade_pace
    else -> null
}

private fun valorDaFita(fita: FitaDeRecorde, valor: Double): String = when (fita) {
    FitaDeRecorde.MAIOR_DISTANCIA -> formatarKm(valor)
    FitaDeRecorde.MELHOR_PACE -> paceCurto(valor)
    else -> formatarDuracao(valor.toLong())
}

/** A linha de baixo da fita: a regra da vaga, ou (F4) o dia e a espécie da época. */
@Composable
internal fun linhaDaFita(fita: FitaDoRecorde): String? = when {
    fita.vaga -> stringResource(
        when (fita.fita) {
            FitaDeRecorde.MELHOR_1K -> R.string.fitas_regra_1k
            FitaDeRecorde.MELHOR_5K -> R.string.fitas_regra_5k
            else -> R.string.fitas_regra_primeira
        },
    )
    else -> linhaDaEpoca(fita)
}

/** A próxima marca, quando o plano a define: só a distância tem uma, a prova. */
@Composable
private fun proximaMarca(fita: FitaDoRecorde, prova: ProvaDoPlano?): String? {
    if (fita.fita != FitaDeRecorde.MAIOR_DISTANCIA || prova == null) return null
    val atual = fita.valor ?: 0.0
    if (atual >= prova.distanciaKm) return null
    return proximaDaProva(prova)
}

/** `dom 11 out · como Snorlax`: o dia do recorde e o Pokémon da época (decisão 10). */
@Composable
private fun linhaDaEpoca(fita: FitaDoRecorde): String? {
    val dia = fita.dia ?: return null
    val data = "${rotuloCurtoDoDia(dia.dayOfWeek).lowercase(LocaleDoApp)} ${dia.dayOfMonth} ${nomeCurtoDoMes(dia)}"
    return fita.nomeDaEspecie?.let { stringResource(R.string.fitas_epoca, data, it) } ?: data
}

@Composable
private fun proximaDaProva(prova: ProvaDoPlano): String =
    stringResource(R.string.fitas_proxima, formatarKm(prova.distanciaKm), "${prova.data.dayOfMonth} ${nomeCurtoDoMes(prova.data)}")
