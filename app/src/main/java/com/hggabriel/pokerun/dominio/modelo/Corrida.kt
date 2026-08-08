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
     * Segundos de cada km completo. **Vazio é o caso normal**, não a exceção: não
     * existe `SplitRecord` no Health Connect, e os splits são derivados de amostras
     * de velocidade cuja granularidade depende do app que gravou (`F2-T03`).
     * Corrida manual nunca tem.
     */
    val splitsKm: List<Long> = emptyList(),
    /** 1 a 10, opcional na confirmação do import. O único dado que nenhuma automação captura. */
    val esforcoPercebido: Int? = null,
    /** `metadata.id` do Health Connect. **Não é chave estável** (docs/05 §4.3). */
    val hcRecordId: String? = null,
    /** `metadata.clientRecordId`. Tem precedência sobre [hcRecordId] quando existe. */
    val hcClientRecordId: String? = null,
    /** Qual sessão da semana esta corrida reivindicou (RN-34). */
    val sessaoReivindicada: SessaoReivindicada? = null,
    val xpCreditado: Long = 0L,
    val substituiRunId: String? = null,
    val substituida: Boolean = false,
    /** Duplicata, tipo errado ou corrida fantasma (RN-31). Descarte, não edição. */
    val descartada: Boolean = false,
)

/** De onde a corrida veio. Modo manual é caminho previsto, não falha (docs/05 §4.4). */
enum class OrigemDaCorrida {
    HC,
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

    /** Uma das `sessoes_alvo − 1` sessões curtas. [indice] começa em 1. */
    data class Curta(val indice: Int) : SessaoReivindicada {
        override val token: String = "curta_$indice"
    }
}
