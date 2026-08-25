package com.hggabriel.pokerun.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A distância entre o campo e o bloco de erro. Menor que a que separa dois campos. */
private val EspacoAntesDoErro = 4.dp

/** A distância entre o rótulo solto e o campo, quando ele sai de dentro dele. */
private val EspacoAntesDoCampo = 4.dp

/**
 * Se o rótulo **sai de dentro do campo** e vira uma linha própria acima dele (`F1-T20`,
 * docs/02 §8 item 9).
 *
 * O rótulo flutuante do `OutlinedTextField` mora num entalhe aberto no contorno, e o
 * Material corta esse entalhe para **uma** linha. A `fontScale` 2,0 em 320dp,
 * `Maior distância confortável hoje` flutua em duas: a primeira sobra acima do campo e a
 * segunda atravessa a borda de cima. O que se vê é um rótulo cortado ao meio pelo
 * contorno, encostado no campo de cima — e em `AjustesScreen`, onde o campo de cima é
 * `Nome`, ele parece pertencer àquele.
 *
 * Isto foi visto em 13/08 na varredura de `F1-T08` e anotado como *"quebra em 2 linhas
 * dentro dele e nada trunca"*. **O `dentro dele` é que não se confirmava**, e só ficou
 * visível em 25/08 com o campo preenchido: com o campo vazio o rótulo faz as vezes de
 * placeholder e desenha inteiro dentro da caixa, sem entalhe nenhum.
 *
 * Fora do campo o rótulo tem a largura toda e quebra em quantas linhas precisar, que é o
 * layout cedendo em vez de travar a escala.
 */
internal fun rotuloSaiDoCampo(escala: Float): Boolean = escala > ESCALA_QUE_EMPILHA

/**
 * Se o campo fica mesmo em uma linha só (`F1-T20`, docs/02 §8 item 9).
 *
 * **Campo somente-leitura nunca fica.** Uma linha só num campo que se digita é
 * comportamento correto: o texto rola dentro dele e quem digita alcança o que saiu da
 * vista. Num campo somente-leitura, que abre um seletor no toque, **não há rolagem** — o
 * que passou da borda ficou inalcançável. Em 320dp a `fontScale` 2,0 isso saiu como
 * `18 de agosto de 202` em `ManualRunScreen` e `31 de dezembro d` em `CriarPlanoScreen`:
 * o ano, que é o campo que a pessoa foi conferir, some.
 *
 * A regra é do componente e não das duas telas de propósito: todo campo que abre seletor
 * chega aqui, inclusive os que ainda vão ser escritos.
 */
internal fun campoEmUmaLinha(umaLinha: Boolean, somenteLeitura: Boolean): Boolean =
    umaLinha && !somenteLeitura

/**
 * Um campo de formulário cujo erro sai pelo canal de alerta (`F1-T06b`, docs/02 §2.4).
 *
 * **Nasce da decisão nº 12, revisada pelo humano em 17/08: aplicar o canal, sem
 * exceção escrita.** O `isError` do Material pinta contorno e rótulo do campo em
 * `alerta` e para por aí — é cor sozinha, e §2.4 exige que todo uso de `alerta`
 * carregue as três peças juntas. O contorno vermelho continua (ele é o que liga o
 * aviso ao campo certo), e embaixo dele entra o bloco que traz o triângulo, o filete
 * de 3dp e o rótulo em mono caixa alta.
 *
 * **É componente, e não um conserto em duas telas.** As telas restantes da Fase 1 são
 * formulários — `ManualRunScreen` em `F1-T16` é o próximo —, e uma regra que depende
 * de o autor da tela lembrar dela é regra que cai na décima tela. Aqui ela não tem
 * como faltar: quem usa `CampoComErro` ganha o canal sem saber que ele existe, e
 * quem não usa cai na varredura de `CanalDeAlertaTest`.
 *
 * **O rótulo do alerta é fixo, e curto de propósito.** `CORRIJA` e não o nome do
 * campo: `MAIOR DISTÂNCIA CONFORTÁVEL HOJE` em mono a `fontScale` 2,0 passa de duas
 * linhas em 320dp, que é o teto de docs/02 §8, item 9 — e repetiria, oito dp abaixo,
 * o rótulo que o próprio campo já mostra. Quem precisa de outro nome passa [rotuloDoErro];
 * é o que uma falha de gravação faria, mas essa não é erro de campo e usa
 * [BannerDeAlerta] direto.
 *
 * @param erro o id da mensagem, ou nulo quando o campo está bom. É ele que liga o canal.
 * @param apoio o texto de ajuda permanente, mostrado enquanto não há erro. O erro **não**
 *   vai para o `supportingText`: ali ele seria a quarta forma de dizer a mesma coisa e
 *   ficaria em `alerta` sem nenhuma das três peças.
 * @param aoTocar transforma o campo em alvo, para o campo que abre um seletor em vez de
 *   aceitar digitação — a data da prova de `F1-T10` é o caso. Vem com [somenteLeitura],
 *   e a camada de toque cobre **só o campo**: o bloco de erro continua sem toque.
 * @param explicaOErro desenha o bloco embaixo deste campo. Passar `false` **acende o
 *   campo sem explicá-lo**, e existe para o erro que fala de dois campos vizinhos: a
 *   data e a hora de `F1-T16` respondem uma pergunta só, e a mensagem *"escolha a data e
 *   a hora"* precisa dos **dois** contornos acesos — é o contorno que liga o aviso ao
 *   campo certo. Com o bloco em cada um, a mesma frase apareceria duas vezes; com ele só
 *   no segundo, o primeiro ficaria cinza e a pessoa que esqueceu a data veria a hora
 *   acusada. O bloco sai uma vez, embaixo do último da dupla.
 * @param vazio o `placeholder`, mostrado enquanto o campo não tem valor.
 */
@Composable
fun CampoComErro(
    valor: String,
    aoMudar: (String) -> Unit,
    @StringRes rotulo: Int,
    modifier: Modifier = Modifier,
    @StringRes erro: Int? = null,
    @StringRes apoio: Int? = null,
    @StringRes rotuloDoErro: Int = R.string.alerta_corrija,
    @StringRes vazio: Int? = null,
    habilitado: Boolean = true,
    umaLinha: Boolean = true,
    somenteLeitura: Boolean = false,
    explicaOErro: Boolean = true,
    opcoesDeTeclado: KeyboardOptions = KeyboardOptions.Default,
    sufixo: (@Composable () -> Unit)? = null,
    aoTocar: (() -> Unit)? = null,
) {
    val rotuloFora = rotuloSaiDoCampo(LocalDensity.current.fontScale)
    val nomeDoCampo = stringResource(rotulo)

    Column(modifier = modifier.fillMaxWidth()) {
        if (rotuloFora) {
            Text(
                text = nomeDoCampo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(EspacoAntesDoCampo))
        }

        Box {
            OutlinedTextField(
                value = valor,
                onValueChange = aoMudar,
                enabled = habilitado,
                readOnly = somenteLeitura,
                // Com o rótulo fora, o campo não leva `label`: dois seriam o mesmo nome
                // dito duas vezes, e o de dentro voltaria a cruzar o contorno. O nome
                // segue chegando ao TalkBack pela semântica logo abaixo.
                label = if (rotuloFora) null else ({ Text(nomeDoCampo) }),
                placeholder = vazio?.let { { Text(stringResource(it)) } },
                suffix = sufixo,
                singleLine = campoEmUmaLinha(umaLinha, somenteLeitura),
                isError = erro != null,
                // O apoio some enquanto o erro está na tela: as duas linhas embaixo do
                // campo diriam coisas diferentes sobre o mesmo dado, e a que manda é a do
                // erro. Ele volta assim que o campo fica bom.
                supportingText = apoio?.takeIf { erro == null }?.let { { Text(stringResource(it)) } },
                keyboardOptions = opcoesDeTeclado,
                modifier = Modifier
                    .fillMaxWidth()
                    // Sem `label`, o campo perderia o nome na árvore de acessibilidade e
                    // o TalkBack anunciaria só o valor. Aqui ele volta, e é o mesmo nome.
                    .then(
                        if (rotuloFora) {
                            Modifier.semantics { contentDescription = nomeDoCampo }
                        } else {
                            Modifier
                        },
                    ),
            )

            // A camada de toque cobre **o campo**, e não o bloco de erro. Ela mora
            // dentro desta `Box` justamente por isso: um `matchParentSize` na `Column`
            // de fora tornaria o aviso tocável, e o banner de §2.4 é bloco de leitura —
            // sem toque, sem botão, sem dispensar.
            if (aoTocar != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = habilitado,
                            role = Role.Button,
                            onClick = aoTocar,
                        ),
                )
            }
        }

        if (erro != null && explicaOErro) {
            Spacer(Modifier.height(EspacoAntesDoErro))
            BannerDeAlerta(
                rotulo = stringResource(rotuloDoErro),
                texto = stringResource(erro),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CampoComErroPreview() {
    PokerunTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CampoComErro(
                valor = "",
                aoMudar = {},
                rotulo = R.string.onboarding_campo_nome,
                erro = R.string.onboarding_erro_nome,
            )
            Spacer(Modifier.height(16.dp))
            CampoComErro(
                valor = "7,5",
                aoMudar = {},
                rotulo = R.string.onboarding_campo_distancia,
                apoio = R.string.onboarding_campo_distancia_apoio,
                sufixo = { Text(stringResource(R.string.onboarding_campo_distancia_unidade)) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, fontScale = 2.0f, widthDp = 320)
@Composable
private fun CampoComErroFonteGrandePreview() {
    PokerunTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CampoComErro(
                valor = "",
                aoMudar = {},
                rotulo = R.string.onboarding_campo_distancia,
                erro = R.string.onboarding_erro_distancia,
            )
        }
    }
}
