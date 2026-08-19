package com.hggabriel.pokerun.ui.telas.corridamanual

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.LocalTime

/**
 * O estado da `ManualRunScreen` (`F1-T16`, docs/03 §3.10).
 *
 * **É um estado só, e não uma máquina**, como em `CriarPlanoUiState`: a tela é um
 * formulário. O que ela precisa saber de fora é uma coisa só — se existe plano ativo
 * —, porque `semana_ref` sai da grade dele (RN-02) e sem plano não há semana para a
 * corrida contar.
 *
 * ### Não há `Salvando`, e isso é a decisão nº 4 aplicada
 *
 * A escrita do Firestore **resolve na confirmação do servidor**. Uma tela que
 * esperasse o retorno de `registrar` para fechar ficaria pendurada offline, e o
 * documento estaria no cache local desde o primeiro instante — a pessoa veria um
 * botão travado sobre uma corrida que já está gravada. A tela fecha no toque e a
 * gravação segue no escopo do `ViewModel`.
 *
 * [confirmando] é RN-04: a data válida de mais de sete dias atrás não é erro de campo,
 * é uma pergunta a mais. Ela guarda o resultado já validado para o diálogo não precisar
 * revalidar nada — e, sobretudo, para o "sim" não recalcular o formulário e chegar a
 * outra resposta.
 */
data class CorridaManualUiState(
    val data: LocalDate? = null,
    val hora: LocalTime? = null,
    val km: String = "",
    val horas: String = "",
    val minutos: String = "",
    val segundos: String = "",
    val fcMedia: String = "",
    val esforco: String = "",
    val erros: ErrosDaCorrida = ErrosDaCorrida(),
    /** Falha da gravação, ou a ausência de plano ativo. Sai pelo `BannerDeAlerta`. */
    @param:StringRes val erroDeTela: Int? = null,
    /** O calendário está aberto. Fica no estado para sobreviver à rotação. */
    val escolhendoData: Boolean = false,
    /** O relógio está aberto. Mesmo motivo. */
    val escolhendoHora: Boolean = false,
    /** // RN-04 — a corrida validada à espera do "sim". Nulo é o caso comum. */
    val confirmando: ValidacaoDaCorrida.Ok? = null,
    /** Terminal: a corrida foi disparada e a tela sai. Ver o KDoc da classe. */
    val registrada: Boolean = false,
)
