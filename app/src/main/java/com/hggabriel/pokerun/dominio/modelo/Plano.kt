package com.hggabriel.pokerun.dominio.modelo

import java.time.Instant
import java.time.ZoneId

/**
 * Um plano de treino (`plans/{planId}`, docs/05 §1).
 *
 * O plano é o objeto compartilhado do grupo: quem cria é dono, quem entra pelo
 * código vira `Membro`. As semanas ficam na subcoleção e são geradas por
 * `GeradorDePlano` (`F1-T02`) a partir de [parametros].
 *
 * **[fuso] é `ZoneId`, e não `String`, de propósito.** RN-28 manda calcular
 * `semana_ref` no fuso do plano, nunca no do aparelho, e o erro é permanente
 * porque o valor é snapshot: uma corrida de domingo 22h em `America/Sao_Paulo`
 * vira segunda em UTC e cai na semana errada para sempre. Guardar o fuso como
 * texto solto convida `ZoneId.systemDefault()` a aparecer no meio do cálculo; um
 * `ZoneId` no modelo obriga quem calcula a receber este aqui.
 */
data class Plano(
    val id: String,
    val nome: String,
    val distanciaAlvoKm: Double,
    val dataProva: Instant,
    val fuso: ZoneId,
    val ownerUid: String,
    /** Seis caracteres num alfabeto de 32 (RN-29). O documento em `invites/` é `F1-T14`. */
    val codigoConvite: String,
    /** Encerramento é do dono e não apaga nada (RN-27). */
    val encerrado: Boolean,
    val parametros: ParametrosDeGeracao,
)

/**
 * O que gerou a grade (`params_geracao`, D-02).
 *
 * D-02 diz *"gerado a partir de 4 parâmetros, depois editável"*, e é o "depois
 * editável" que obriga este bloco a existir separado: assim que o usuário puxa um
 * longão na `PlanDraftReviewScreen`, a grade deixa de ser dedutível dos campos do
 * plano. **Isto é o snapshot da entrada**, não uma cópia decorativa dos campos de
 * cima.
 *
 * O nome do plano faz parte do primeiro campo do formulário (docs/01 §3.1) e **não
 * está aqui**: ele não entra em conta nenhuma da geração. Os quatro campos do
 * formulário viram cinco valores porque o primeiro deles pede duas coisas.
 */
data class ParametrosDeGeracao(
    val dataProva: Instant,
    val distanciaAlvoKm: Double,
    /** Maior distância confortável atual, respondível por iniciante (docs/01 §3.1). */
    val baselineKm: Double,
    /** 2, 3 ou 4. O plano não atribui dias, só quantidade (XP-03). */
    val sessoesPorSemana: Int,
)

/**
 * Uma semana da grade (`plans/{planId}/weeks/{n}`, docs/05 §1).
 *
 * **[longaoKm] é nulo em duas situações previstas**, e nenhuma delas é erro: na
 * segunda semana de taper (`N−1`), onde o volume cai a 40% do pico e se divide
 * igualmente entre as sessões, e na semana da prova. Toda tela que mostra longão
 * tolera a ausência.
 *
 * **[parcial] existe por causa da 21ª semana** (RN-26): de 28 a 31/12 são quatro
 * dias e uma sessão prevista. Todo cálculo que assume semana de sete dias quebra
 * ali, e é a armadilha nº 1 do projeto.
 *
 * A semana **não tem dias atribuídos**: [sessoesAlvo] é *"3 sessões"*, nunca
 * *"terça, quinta, sábado"*. O multiplicador de XP vem de reivindicar um slot, e
 * não do dia (XP-03).
 *
 * **Não existe campo `congelada`, e a ausência é a decisão** (`F1-T05c`): semana
 * passada não se edita (RN-05), mas isso é `agora >= dataFim` e não um booleano que
 * alguém precisa lembrar de gravar. Use `CalendarioDoPlano.congelada`.
 */
data class Semana(
    /** O `n` do caminho, de 1 a N. */
    val numero: Int,
    val dataInicio: Instant,
    /**
     * **Exclusivo:** a meia-noite do dia seguinte ao último dia da semana. É a única
     * representação em que *"a semana contém t"* é `dataInicio <= t < dataFim` — sem
     * vão e sem sobreposição, inclusive na virada do horário de verão. RN-05 depende
     * dela, e `GeradorDePlano` a documenta por inteiro.
     */
    val dataFim: Instant,
    val sessoesAlvo: Int,
    val kmAlvo: Double,
    val longaoKm: Double?,
    val tipo: TipoDeSemana,
    val parcial: Boolean,
)

/**
 * Os três tipos de semana. **Não existe `DELOAD`** (D-14).
 *
 * A rampa do longão é linear e suave o bastante; o único alívio programado é o
 * taper. D-14 tirou um parâmetro do formulário e um tipo daqui, e acrescentar um
 * quarto valor reabre uma decisão fechada.
 */
enum class TipoDeSemana {
    BUILD,
    TAPER,
    PROVA,
}
