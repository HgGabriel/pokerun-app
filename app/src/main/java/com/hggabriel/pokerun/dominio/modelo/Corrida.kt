package com.hggabriel.pokerun.dominio.modelo

import java.time.Instant

/**
 * Uma corrida registrada (`users/{uid}/runs/{runId}`, docs/05 §1).
 *
 * **Corridas são append-only (RN-24).** Corrigir uma não é sobrescrevê-la: grava-se
 * um registro novo com [substituiRunId] apontando para o antigo, e o antigo recebe
 * `substituida = true`. [substituida] é **o único campo que uma escrita pode
 * alterar** num documento já gravado.
 *
 * Isso não vira `var` aqui. O modelo é imutável do começo ao fim porque a
 * imutabilidade que importa é a do documento, não a do objeto em memória, e um
 * único `var` no meio de uma `data class` daria a impressão errada de que mutar o
 * objeto muda alguma coisa. Quem garante a regra é `firestore.rules` e o
 * repositório de `F1-T05`; quem corrige em memória usa `copy`.
 *
 * **[planoId], [semanaRef] e [temporadaId] são snapshots, não referências vivas**
 * (RN-14, RN-28, RN-40). Foram calculados no momento da gravação e não seguem o
 * plano ativo do usuário depois. [semanaRef] em particular saiu do fuso do plano, e
 * recalculá-lo no fuso do aparelho move a corrida de semana.
 *
 * **[semanaRef] é nulo quando a corrida cai fora do intervalo do plano** (RN-03):
 * ela entra no histórico vitalício e não conta para a aderência.
 *
 * **[xpCreditado] é resultado, não fonte.** Ele guarda o que o último replay
 * cronológico atribuiu, para auditoria. Recalcular XP a partir dele localmente
 * produz total errado, porque `bonus_pace` depende da média móvel do momento e
 * `bonus_recorde` de todas as corridas anteriores (XP-10).
 */
data class Corrida(
    val id: String,
    val dataHoraInicio: Instant,
    val km: Double,
    val duracaoSeg: Long,
    val tipoExercicio: String,
    val origem: OrigemDaCorrida,
    val planoId: String,
    val semanaRef: Int?,
    val temporadaId: String,
    val fcMedia: Int? = null,
    val fcMax: Int? = null,
    val fcMin: Int? = null,
    val caloriasAtivas: Int? = null,
    val passos: Int? = null,
    val cadenciaMedia: Int? = null,
    /**
     * Segundos de cada km completo. **Vazio é o caso de toda corrida hoje:** o registro
     * manual não tem splits, e não há outra entrada (D-20, docs/05 §4.1).
     */
    val splitsKm: List<Long> = emptyList(),
    /**
     * O tempo do melhor 1 km e do melhor 5 km **dentro desta corrida**, digitados à mão
     * (decisão nº 107). Sem splits, é a única fonte desses dois recordes (docs/04 §3.3).
     * Nulo quando o corredor não informou, e sempre nulo em corrida mais curta que a marca.
     */
    val tempo1kSeg: Long? = null,
    val tempo5kSeg: Long? = null,
    /** 1 a 10, opcional no registro manual (docs/03 §3.10). */
    val esforcoPercebido: Int? = null,
    /** Qual sessão da semana esta corrida reivindicou (RN-34). */
    val sessaoReivindicada: SessaoReivindicada? = null,
    val xpCreditado: Long = 0L,
    /** As parcelas do último replay (`xp_detalhe`). Nulo antes do primeiro replay. */
    val xpDetalhe: ParcelasDeXp? = null,
    val substituiRunId: String? = null,
    val substituida: Boolean = false,
    /** Duplicata, tipo errado ou corrida fantasma (RN-31). Descarte, não edição. */
    val descartada: Boolean = false,
)

/**
 * A conta aberta do XP de uma corrida (`xp_detalhe`, docs/05 §1), na ordem da fórmula
 * de docs/04 §3: `(base + longao + pace) × multiplicador + recorde`.
 */
data class ParcelasDeXp(
    val base: Double,
    val longao: Double,
    val pace: Int,
    val multiplicador: Double,
    val recorde: Int,
)

/** De onde a corrida veio. O registro manual é a única entrada (D-20, docs/05 §4.4). */
enum class OrigemDaCorrida {
    MANUAL,
}

/**
 * A sessão da semana que a corrida reivindicou (RN-34, XP-03).
 *
 * Cada sessão prevista aceita **no máximo uma** corrida. A atribuição é automática e
 * o usuário sobrescreve na `RunEditScreen` (`F2-T10`).
 *
 * É tipo fechado, e não `String`, porque o vocabulário está fixado no schema e o
 * multiplicador de XP sai exatamente daqui: `"curta_0"` ou `"Longao"` digitados
 * errado não viram erro de compilação, viram multiplicador silenciosamente ausente.
 * O [token] é a forma canônica do documento, que docs/05 §1 fixa; converter de e
 * para ele é `F1-T05`.
 */
sealed interface SessaoReivindicada {
    val token: String

    /** O longão da semana. Ausente em `N−1` e na prova, junto com `Semana.longaoKm`. */
    data object Longao : SessaoReivindicada {
        override val token: String = "longao"
    }

    /**
     * A escolha explícita de não cumprir sessão nenhuma (`F2-T10`, docs/03 §3.14). Fica
     * em `1,0×` e a atribuição automática não a puxa para um slot (docs/04 §3.2).
     */
    data object Avulsa : SessaoReivindicada {
        override val token: String = "avulsa"
    }

    /** Uma das `sessoes_alvo − 1` sessões curtas. [indice] começa em 1. */
    data class Curta(val indice: Int) : SessaoReivindicada {
        override val token: String = "curta_$indice"
    }
}
