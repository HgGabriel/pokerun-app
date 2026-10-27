package com.hggabriel.pokerun.ui.telas.perfil

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import java.util.Locale

/*
 * Os campos do perfil em Ajustes (`F1-T17`, docs/03 §3.11).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `CamposDaCorrida.kt` e
 * `CamposDoPlano.kt`: são a parte da tela que se prova sem aparelho e sem Firestore.
 * `CamposDoPerfilTest` roda os três.
 *
 * **A diferença desta tela para o cadastro é que aqui os campos já vêm preenchidos.**
 * O onboarding parte do branco; os Ajustes partem do que está gravado, e é o caminho de
 * volta — `Double` do Firestore virando texto de campo — que traz os defeitos que não
 * aparecem em diff.
 */

/**
 * O que o formulário do perfil acusa, campo a campo.
 *
 * **Saem juntos**, como em [com.hggabriel.pokerun.ui.telas.corridamanual.ErrosDaCorrida]:
 * acusar em série faria a pessoa descobrir o segundo erro no segundo toque no botão.
 * Toda mensagem é id de recurso, nunca texto — microcopy mora em `strings.xml`, que é
 * onde a varredura de `F1-T20` olha.
 */
data class ErrosDoPerfil(
    @param:StringRes val nome: Int? = null,
    @param:StringRes val baseline: Int? = null,
) {
    val algum: Boolean get() = nome != null || baseline != null
}

/** O resultado de [validarPerfil]: ou os dois campos convertidos, ou o que falta neles. */
sealed interface ValidacaoDoPerfil {

    data class Valido(val nome: String, val baselineKm: Double) : ValidacaoDoPerfil

    data class Invalido(val erros: ErrosDoPerfil) : ValidacaoDoPerfil
}

/**
 * Lê os dois campos que docs/03 §3.11 deixa editar.
 *
 * A forma da distância é a de [distanciaEmKm], a mesma dos outros campos de km do app
 * (decisão nº 26): três telas pedem distância pelas mesmas regras, e a segunda cópia do
 * regex é a divergência silenciosa que a função existe para impedir.
 */
internal fun validarPerfil(nome: String, baseline: String): ValidacaoDoPerfil {
    val limpo = nome.trim().ifBlank { null }
    val km = distanciaEmKm(baseline)

    return if (limpo == null || km == null) {
        ValidacaoDoPerfil.Invalido(
            ErrosDoPerfil(
                nome = R.string.ajustes_erro_nome.takeIf { limpo == null },
                baseline = R.string.ajustes_erro_baseline.takeIf { km == null },
            ),
        )
    } else {
        ValidacaoDoPerfil.Valido(nome = limpo, baselineKm = km)
    }
}

/**
 * O texto que pré-preenche o campo de baseline, a partir do que está gravado.
 *
 * **Não é [com.hggabriel.pokerun.ui.componentes.formatarKm], e a diferença é a segunda
 * casa.** Aquele arredonda para uma casa porque é para *ler* distância de corrida, onde
 * a segunda casa é precisão que o GPS não tem. Este é para *editar*, e o campo aceita
 * duas (`distanciaEmKm`): pré-preencher `7,25` como `7,3` faria abrir os Ajustes e
 * salvar mexer na baseline de quem não pediu — e a baseline semeia a geração da próxima
 * grade (docs/01 §4).
 *
 * O invariante que fecha o ida e volta está no teste: todo texto que sai daqui passa
 * pela validação que o próprio campo aplica. Um formato recusado deixaria o botão morto
 * num formulário que ninguém tocou.
 */
internal fun textoDaBaseline(km: Double, locale: Locale = LocaleDoApp): String {
    val arredondado = Math.round(km * 100) / 100.0
    // `%.2f` sempre escreve o separador, então o `trimEnd` do separador nunca come
    // dígito: `10,00` perde os dois zeros e para na vírgula, e não vira `1`.
    return String.format(locale, "%.2f", arredondado).trimEnd('0').trimEnd(',', '.')
}

/**
 * Se há o que gravar.
 *
 * **Compara o que o campo mostra, e não o texto digitado nem o `Double` cru.** `7,50` e
 * `7,5` são textos diferentes e a mesma distância: comparar texto mandaria uma escrita
 * ao Firestore para gravar o valor que já está lá. Comparar `Double` com `!=` traria a
 * pergunta de quanta diferença conta, e a resposta certa é a precisão que o campo
 * consegue expressar — que é exatamente o que [textoDaBaseline] devolve.
 */
internal fun perfilMudou(gravado: Usuario, novo: ValidacaoDoPerfil.Valido): Boolean =
    gravado.nome != novo.nome ||
        textoDaBaseline(gravado.baselineKm) != textoDaBaseline(novo.baselineKm)
