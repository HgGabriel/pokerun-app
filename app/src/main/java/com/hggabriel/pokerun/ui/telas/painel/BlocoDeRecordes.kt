package com.hggabriel.pokerun.ui.telas.painel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.regras.Recordes
import com.hggabriel.pokerun.dominio.regras.TipoDeRecorde
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.formatarDuracao
import com.hggabriel.pokerun.ui.componentes.formatarKm
import com.hggabriel.pokerun.ui.theme.EstiloDado
import java.util.Locale
import kotlin.math.roundToLong

private val AlturaDaLinha = 48.dp
private val EspacoEntreLinhas = 8.dp

/** Um slot do bloco: o recorde e o valor escrito, ou nulo se ainda não existe. */
data class SlotDeRecorde(val tipo: TipoDeRecorde, val valor: String?)

/**
 * Os cinco slots de docs/04 §3.3, sempre na mesma ordem: melhor pace, melhor 1 km,
 * melhor 5 km, maior distância e maior duração. O recorde é vitalício (RN-39): atravessa
 * planos e temporadas. Pace e tempos saem em `m:ss`, a duração em `h:mm:ss`.
 */
fun slotsDeRecorde(recordes: Recordes, locale: Locale = LocaleDoApp): List<SlotDeRecorde> = listOf(
    SlotDeRecorde(TipoDeRecorde.MELHOR_PACE, recordes.melhorPaceSegKm?.let { formatarDuracao(it.roundToLong(), locale) }),
    SlotDeRecorde(TipoDeRecorde.MELHOR_1K, recordes.melhor1kSeg?.let { formatarDuracao(it, locale) }),
    SlotDeRecorde(TipoDeRecorde.MELHOR_5K, recordes.melhor5kSeg?.let { formatarDuracao(it, locale) }),
    SlotDeRecorde(TipoDeRecorde.MAIOR_DISTANCIA, recordes.maiorDistanciaKm?.let { formatarKm(it, locale) }),
    SlotDeRecorde(TipoDeRecorde.MAIOR_DURACAO, recordes.maiorDuracaoSeg?.let { formatarDuracao(it, locale) }),
)

/**
 * O bloco de recordes pessoais da `StatsDashboardScreen` (`F3-T03`, docs/03 §3.17), lido
 * de `users/{uid}/agregados/records`. O mínimo é uma corrida; abaixo dele, e em todo slot
 * ainda sem recorde, **o rótulo fica em `texto-secundario` e o valor não aparece**. Nunca um
 * traço no lugar do número.
 */
@Composable
fun BlocoDeRecordes(recordes: Recordes?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.painel_recordes_titulo).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(EspacoEntreLinhas))
        slotsDeRecorde(recordes ?: Recordes()).forEach { slot -> LinhaDoRecorde(slot) }
    }
}

@Composable
private fun LinhaDoRecorde(slot: SlotDeRecorde) {
    val rotulo = stringResource(rotuloDo(slot.tipo))
    val unidade = unidadeDo(slot.tipo)?.let { stringResource(it) }
    val escrito = slot.valor?.let { valor -> if (unidade != null) "$valor $unidade" else valor }
    val descricao = escrito?.let { "$rotulo, $it" } ?: stringResource(R.string.painel_recorde_vazio, rotulo)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AlturaDaLinha)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = if (escrito != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (escrito != null) {
            Text(text = escrito, style = EstiloDado, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun rotuloDo(tipo: TipoDeRecorde): Int = when (tipo) {
    TipoDeRecorde.MELHOR_PACE -> R.string.painel_recorde_pace
    TipoDeRecorde.MELHOR_1K -> R.string.painel_recorde_1k
    TipoDeRecorde.MELHOR_5K -> R.string.painel_recorde_5k
    TipoDeRecorde.MAIOR_DISTANCIA -> R.string.painel_recorde_distancia
    TipoDeRecorde.MAIOR_DURACAO -> R.string.painel_recorde_duracao
}

private fun unidadeDo(tipo: TipoDeRecorde): Int? = when (tipo) {
    TipoDeRecorde.MELHOR_PACE -> R.string.painel_unidade_pace
    TipoDeRecorde.MAIOR_DISTANCIA -> R.string.painel_unidade_km
    else -> null
}
