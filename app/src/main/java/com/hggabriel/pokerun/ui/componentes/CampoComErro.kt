package com.hggabriel.pokerun.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    habilitado: Boolean = true,
    umaLinha: Boolean = true,
    opcoesDeTeclado: KeyboardOptions = KeyboardOptions.Default,
    sufixo: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = aoMudar,
            enabled = habilitado,
            label = { Text(stringResource(rotulo)) },
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
