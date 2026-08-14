package com.hggabriel.pokerun.ui.componentes

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import com.hggabriel.pokerun.ui.theme.EstiloDado
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant

/** O alvo de toque mínimo de docs/02 §8, item 2. A linha tem duas, então sobra. */
private val AlturaMinimaDaLinha = 48.dp

private val RecheioDaLinha = 12.dp
private val EspacoEntreLinhas = 2.dp
private val RecheioDaTag = 6.dp
private val RecheioVerticalDaTag = 2.dp
private val LarguraDaBordaDaTag = 1.dp
private val TamanhoDoCadeado = 16.dp
private val EspacoAntesDaTag = 6.dp

/** O ponto médio que separa os três dados da linha, o mesmo da `HomeScreen`. */
private const val SEPARADOR = " · "

/**
 * A lista de semanas de um plano (docs/03 §3.6 e §3.7).
 *
 * **Um componente, dois consumidores.** A `PlanDraftReviewScreen` (`F1-T11`) o usa com
 * [aoEditar] preenchido, para revisar a grade antes de ela existir; a `PlanDetailScreen`
 * (`F1-T13`) o usa em modo leitura para membro, e com edição para o dono. A ficha de
 * `F1-T11` manda escrever uma vez, e é por isso que ele mora em `ui/componentes` em vez
 * de dentro da tela.
 *
 * **A linha mostra os cinco dados de §3.6 e nada mais:** número, tipo como tag, longão,
 * volume e sessões. Datas ficam de fora de propósito — a grade é sobre estrutura, e o
 * calendário de uma semana é a `WeekDetailScreen`.
 *
 * **Só semana com longão é tocável.** A 2ª de taper e a da prova não planejam longão
 * (o volume delas não deriva de um), então não há o que editar: elas ficam na lista, sem
 * indicação de toque e sem alvo. Uma linha que responde ao toque e não abre nada é pior
 * que uma linha inerte.
 *
 * **O cadeado chegou com `F1-T13`.** RN-05 congela semana que já acabou, e semana
 * congelada não recebe toque nem para o dono — a rule de `weeks/{n}` faz a mesma conta com
 * `request.time`, então oferecer a edição seria oferecer uma escrita que o servidor nega.
 * Num rascunho o conjunto vem vazio, porque nenhuma semana começou.
 *
 * @param aoEditar nulo põe a lista em modo leitura. Não recebe toque, não indica toque.
 * @param congeladas os números das semanas que já acabaram (RN-05). Elas ganham cadeado e
 *   saem da edição, mesmo com [aoEditar] preenchido.
 */
@Composable
fun GradeDeSemanas(
    semanas: List<Semana>,
    modifier: Modifier = Modifier,
    aoEditar: ((Semana) -> Unit)? = null,
    congeladas: Set<Int> = emptySet(),
) {
    Ficha(modifier = modifier) {
        semanas.forEachIndexed { indice, semana ->
            if (indice > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            val congelada = semana.numero in congeladas
            LinhaDaSemana(
                semana = semana,
                congelada = congelada,
                aoEditar = if (semana.longaoKm != null && !congelada) aoEditar else null,
            )
        }
    }
}

@Composable
private fun LinhaDaSemana(semana: Semana, congelada: Boolean, aoEditar: ((Semana) -> Unit)?) {
    val rotulo = stringResource(R.string.semana_rotulo, semana.numero)
    val tipo = stringResource(rotuloDoTipo(semana.tipo))
    val sessoes = pluralStringResource(R.plurals.semana_sessoes, semana.sessoesAlvo, semana.sessoesAlvo)
    val volume = formatarKm(semana.kmAlvo)

    val dados = semana.longaoKm?.let { longao ->
        stringResource(R.string.semana_dados, formatarKm(longao), volume, sessoes)
    } ?: stringResource(R.string.semana_dados_sem_longao, volume, sessoes)

    val descricao = when {
        aoEditar != null -> stringResource(R.string.semana_descricao_editavel, rotulo, tipo, dados)
        // O cadeado é desenho; para o TalkBack ele precisa ser palavra (docs/02 §8).
        congelada -> stringResource(R.string.semana_descricao_congelada, rotulo, tipo, dados)
        else -> stringResource(R.string.semana_descricao, rotulo, tipo, dados)
    }

    val toque = if (aoEditar != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = { aoEditar(semana) },
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(toque)
            .heightIn(min = AlturaMinimaDaLinha)
            .padding(RecheioDaLinha)
            .clearAndSetSemantics { contentDescription = descricao },
        verticalArrangement = Arrangement.spacedBy(EspacoEntreLinhas),
    ) {
        // Acima de 1,3 a tag desce para a linha de baixo: com `SEMANA 12` e `BUILD` lado
        // a lado em 320dp, um dos dois trunca — foi o defeito que o `SEG` da Home pegou.
        if (LocalDensity.current.fontScale > ESCALA_QUE_EMPILHA) {
            Text(text = rotulo, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (congelada) Cadeado()
                TagDoTipo(tipo)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = rotulo, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (congelada) Cadeado()
                    TagDoTipo(tipo)
                }
            }
        }

        Text(
            text = dados,
            style = EstiloDado,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * // RN-05
 *
 * O cadeado da semana congelada (docs/03 §3.7).
 *
 * **Sem `contentDescription` próprio**, e é de propósito: a linha inteira é um bloco de
 * semântica única, e a descrição dela já diz "semana encerrada" em palavra. Um rótulo
 * aqui faria o TalkBack anunciar o cadeado duas vezes. Quem o usa fora de uma linha —
 * a `WeekDetailScreen`, ao lado da tag do tipo — carrega a palavra no bloco que o contém,
 * pelo mesmo motivo.
 *
 * **Internal desde `F1-T15`**, junto de [TagDoTipo] e [rotuloDoTipo]: a tela da semana
 * desenha o mesmo par, e uma segunda cópia deles divergiria no tamanho e na cor sem nada
 * quebrar.
 */
@Composable
internal fun Cadeado() {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(end = EspacoAntesDaTag)
            .size(TamanhoDoCadeado),
    )
}

/**
 * A tag do tipo da semana.
 *
 * **É tag, não chip:** não seleciona, não filtra e não recebe toque, então a borda é a
 * decorativa de 1dp em `borda` (docs/02 §2.3). `borda-forte` ali mentiria — ela é o que
 * delimita **controle interativo**, e alguém tentaria tocar.
 */
@Composable
internal fun TagDoTipo(tipo: String) {
    Text(
        text = tipo.uppercase(LocaleDoApp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .border(
                width = LarguraDaBordaDaTag,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = RecheioDaTag, vertical = RecheioVerticalDaTag),
    )
}

internal fun rotuloDoTipo(tipo: TipoDeSemana): Int = when (tipo) {
    TipoDeSemana.BUILD -> R.string.semana_tipo_build
    TipoDeSemana.TAPER -> R.string.semana_tipo_taper
    TipoDeSemana.PROVA -> R.string.semana_tipo_prova
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private fun semanaDeExemplo(
    numero: Int,
    kmAlvo: Double,
    longaoKm: Double?,
    tipo: TipoDeSemana,
    sessoes: Int = 3,
) = Semana(
    numero = numero,
    dataInicio = Instant.EPOCH,
    dataFim = Instant.EPOCH,
    sessoesAlvo = sessoes,
    kmAlvo = kmAlvo,
    longaoKm = longaoKm,
    tipo = tipo,
    parcial = false,
)

private val GRADE_DE_EXEMPLO = listOf(
    semanaDeExemplo(1, 10.0, 5.0, TipoDeSemana.BUILD),
    semanaDeExemplo(2, 11.3, 5.7, TipoDeSemana.BUILD),
    semanaDeExemplo(19, 19.8, 9.9, TipoDeSemana.TAPER),
    semanaDeExemplo(20, 13.2, null, TipoDeSemana.TAPER),
    semanaDeExemplo(21, 15.0, null, TipoDeSemana.PROVA, sessoes = 1),
)

@Preview(name = "Editável", showBackground = true, backgroundColor = 0xFFFAF9F7)
@Composable
private fun GradeEditavelPreview() {
    PokerunTheme {
        GradeDeSemanas(
            semanas = GRADE_DE_EXEMPLO,
            modifier = Modifier.padding(16.dp),
            aoEditar = {},
        )
    }
}

@Preview(name = "Leitura", showBackground = true, backgroundColor = 0xFFFAF9F7)
@Composable
private fun GradeDeLeituraPreview() {
    PokerunTheme {
        GradeDeSemanas(semanas = GRADE_DE_EXEMPLO, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320)
@Composable
private fun GradeFonteGrandePreview() = GradeEditavelPreview()
