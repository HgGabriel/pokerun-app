package com.hggabriel.pokerun.ui.telas.ajustes

import androidx.annotation.StringRes

/**
 * O estado da `SettingsScreen` (`F1-T17`, docs/03 §3.11).
 *
 * **É um estado só, e não uma máquina**, pelo mesmo motivo de `CorridaManualUiState`: a
 * tela é uma lista de opções independentes, e nenhuma depende da ordem em que a pessoa
 * mexeu nelas.
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
    // Sair
    // ------------------------------------------------------------------

    /** O diálogo está aberto. Fica no estado para sobreviver à rotação. */
    val confirmandoSaida: Boolean = false,
    /** Terminal: a sessão foi encerrada e a tela sai. */
    val saiuDaConta: Boolean = false,

    /** Falha de gravação ou de leitura do perfil. Sai pelo `BannerDeAlerta`. */
    @param:StringRes val erroDeTela: Int? = null,
)
