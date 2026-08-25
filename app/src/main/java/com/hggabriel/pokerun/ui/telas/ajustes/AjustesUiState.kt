package com.hggabriel.pokerun.ui.telas.ajustes

import androidx.annotation.StringRes
import com.hggabriel.pokerun.dados.healthconnect.OrigemDeTreino

/**
 * O estado da `SettingsScreen` (`F1-T17`, docs/03 §3.11).
 *
 * **É um estado só, e não uma máquina**, pelo mesmo motivo de `CorridaManualUiState`: a
 * tela é uma lista de opções independentes. Trocar a fonte canônica não muda nada no
 * bloco do perfil, e nenhuma das duas depende da ordem em que a pessoa mexeu nelas.
 *
 * ### O botão de recalcular agregados não está aqui
 *
 * docs/03 §3.11 o lista, e a ficha de `F1-T17` diz que *"na Fase 1 ele pode não existir
 * ainda"*: a implementação é `F3-T08`, que expõe o motor de replay construído em
 * `F2-T07`. Um botão desabilitado sem nada que o explique seria pior que a ausência, e
 * o app não tem idioma para "existe mas ainda não" fora do andaime de tela inteira.
 *
 * ### [perfilSalvo] é confirmação, e não estado terminal
 *
 * Salvar o perfil **não** fecha a tela: a pessoa entrou nos Ajustes para mexer em mais
 * de uma coisa, e sair no primeiro toque a obrigaria a voltar. Quem fecha é [saiuDaConta],
 * e essa sim é terminal.
 */
data class AjustesUiState(
    val carregando: Boolean = true,

    // ------------------------------------------------------------------
    // O perfil
    // ------------------------------------------------------------------

    val nome: String = "",
    val baseline: String = "",
    val erros: ErrosDoPerfil = ErrosDoPerfil(),
    /**
     * Se há diferença entre o que está no campo e o que está gravado.
     *
     * Comparação de **texto**, e é de propósito: enquanto a pessoa digita `7,` o valor
     * não converte, e um botão que dependesse da conversão apagaria debaixo do dedo. A
     * pergunta "isso muda o número gravado" é de [perfilMudou], e ela é feita no toque.
     */
    val podeSalvar: Boolean = false,
    val salvandoPerfil: Boolean = false,
    /** A confirmação de que a escrita foi disparada. Some ao próximo toque num campo. */
    val perfilSalvo: Boolean = false,

    // ------------------------------------------------------------------
    // A origem dos treinos (RN-22)
    // ------------------------------------------------------------------

    val bloco: BlocoDeOrigem = BlocoDeOrigem.Indisponivel,
    val origens: List<OrigemDeTreino> = emptyList(),
    val lendoOrigens: Boolean = false,
    /**
     * Separa o vazio legítimo — ninguém gravou treino em 30 dias — do vazio que veio de
     * exceção. Só o segundo merece "tentar de novo" (docs/02 §8, item 7), e é a mesma
     * distinção de `OnboardingUiState.EscolhendoFonte`.
     */
    val falhouALeitura: Boolean = false,
    /** O que está gravado em `fonte_canonica`. Nulo é o modo manual (docs/05 §4.4). */
    val fonteAtual: String? = null,
    /** O que a pessoa tocou na lista, ainda não gravado. */
    val escolhida: String? = null,
    val salvandoFonte: Boolean = false,

    // ------------------------------------------------------------------
    // Sair
    // ------------------------------------------------------------------

    /** O diálogo está aberto. Fica no estado para sobreviver à rotação. */
    val confirmandoSaida: Boolean = false,
    /** Terminal: a sessão foi encerrada e a tela sai. */
    val saiuDaConta: Boolean = false,

    /** Falha de gravação ou de leitura do perfil. Sai pelo `BannerDeAlerta`. */
    @param:StringRes val erroDeTela: Int? = null,
) {
    /** O rótulo da origem gravada, quando ela está na lista lida agora. */
    val rotuloDaFonteAtual: String?
        get() = fonteAtual?.let { pacote ->
            origens.firstOrNull { it.pacote == pacote }?.rotulo ?: pacote
        }
}
