package com.hggabriel.pokerun.ui.telas.ajustes

import androidx.annotation.StringRes
import com.hggabriel.pokerun.R
import com.hggabriel.pokerun.dados.healthconnect.StatusDoHealthConnect
import com.hggabriel.pokerun.dominio.modelo.Usuario
import com.hggabriel.pokerun.ui.componentes.LocaleDoApp
import com.hggabriel.pokerun.ui.componentes.distanciaEmKm
import java.util.Locale

/*
 * Os campos do perfil em Ajustes (`F1-T17`, docs/03 §3.11).
 *
 * Estão fora do `ViewModel` pelo mesmo motivo de `CamposDaCorrida.kt` e
 * `CamposDoPlano.kt`: são a parte da tela que se prova sem aparelho, sem Firestore e
 * sem Health Connect. `CamposDoPerfilTest` roda os quatro.
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

/**
 * O que a seção de fonte canônica consegue oferecer neste aparelho.
 *
 * Três estados e não quatro, e o teste prova que os três são alcançados.
 */
enum class BlocoDeOrigem {

    /**
     * Sem Health Connect utilizável. **A seção continua na tela e diz isso** — sumir
     * faria a tela mentir por omissão sobre por que a opção da ficha não está lá.
     */
    Indisponivel,

    /** Há Health Connect, falta `READ_EXERCISE`. A folha de permissão é o próximo passo. */
    SemPermissao,

    /** Dá para listar as origens dos últimos 30 dias e trocar a fonte (RN-22). */
    PodeEscolher,
}

/**
 * // RN-22
 *
 * **`PrecisaAtualizar` cai no mesmo bloco que `Indisponivel`**, e é a leitura que
 * `passoDepoisDoPerfil` já faz no cadastro: o cliente não conecta nos dois casos, e
 * docs/05 §4.4 trata os dois como o modo manual — caminho previsto, não falha. Um
 * terceiro estado aqui seria um segundo idioma para a mesma impossibilidade.
 *
 * Sem permissão a lista sai vazia, e essa vazia é **indistinguível** do aparelho que não
 * tem treino gravado. É o mesmo motivo pelo qual a ordem do onboarding é rígida: pedir
 * antes de listar é o que separa "ninguém gravou" de "ninguém deixou olhar".
 */
internal fun blocoDeOrigem(
    status: StatusDoHealthConnect,
    permissaoConcedida: Boolean,
): BlocoDeOrigem = when {
    status != StatusDoHealthConnect.Disponivel -> BlocoDeOrigem.Indisponivel
    permissaoConcedida -> BlocoDeOrigem.PodeEscolher
    else -> BlocoDeOrigem.SemPermissao
}
