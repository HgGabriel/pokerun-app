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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.ui.theme.PokerunTheme

/** A distância entre o campo e o bloco de erro. Menor que a que separa dois campos. */
private val EspacoAntesDoErro = 4.dp

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
    opcoesDeTeclado: KeyboardOptions = KeyboardOptions.Default,
    sufixo: (@Composable () -> Unit)? = null,
    aoTocar: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            OutlinedTextField(
                value = valor,
                onValueChange = aoMudar,
                enabled = habilitado,
                readOnly = somenteLeitura,
                label = { Text(stringResource(rotulo)) },
                placeholder = vazio?.let { { Text(stringResource(it)) } },
                suffix = sufixo,
                singleLine = umaLinha,
                isError = erro != null,
                // O apoio some enquanto o erro está na tela: as duas linhas embaixo do
                // campo diriam coisas diferentes sobre o mesmo dado, e a que manda é a do
                // erro. Ele volta assim que o campo fica bom.
                supportingText = apoio?.takeIf { erro == null }?.let { { Text(stringResource(it)) } },
                keyboardOptions = opcoesDeTeclado,
                modifier = Modifier.fillMaxWidth(),
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

        if (erro != null) {
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
