package com.hggabriel.pokerun.dominio.modelo

import java.time.Instant

/**
 * O perfil do corredor (`users/{uid}`, docs/05 §1).
 *
 * **[fonteCanonica] é nula até o passo 5 do onboarding**, e continua nula para quem
 * não tem Health Connect. A ordem do onboarding é rígida (docs/01): nome, baseline,
 * permissão, ler as origens, escolher a fonte — não dá para listar quem grava no
 * Health Connect antes de pedir permissão e ler dele. E onde `getSdkStatus()`
 * responde indisponível, o app opera só em modo manual, sem tela de erro
 * (docs/05 §4.4): ali não existe fonte canônica para escolher.
 *
 * É ela que RN-22 usa para descartar treino vindo de outra origem. Nula significa
 * "nada a filtrar porque nada é importado", nunca "aceite tudo".
 */
data class Usuario(
    val uid: String,
    val nome: String,
    /** O plano que a `HomeScreen` mostra. Trocar de plano ativo é RN-13. */
    val planoAtivoId: String? = null,
    /**
     * Todo plano de que o usuário participa, ativo ou não (D-04, docs/05 §2.7).
     *
     * **Existe porque não há consulta que responda "de quais planos eu
     * participo".** `plans` não aceita `list` — não há listagem pública (RN-17) —,
     * então o vínculo é gravado do lado que já é privado e já é lido. A
     * `PlansListScreen` percorre estes IDs e lê cada plano **por ID**, leitura
     * direta e nunca consulta, do mesmo jeito que o convite (RN-29).
     *
     * Nada aqui é denormalizado: nome, data da prova e `encerrado` continuam vindo
     * de `plans/{id}`. O array também não encolhe — plano encerrado continua na
     * lista, agrupado ao final com o resumo congelado (D-05).
     */
    val planos: List<String> = emptyList(),
    /** Package name do app de origem, ex.: `com.sec.android.app.shealth` (RN-22). */
    val fonteCanonica: String? = null,
    /** Maior distância confortável declarada no onboarding. Semeia `ParametrosDeGeracao`. */
    val baselineKm: Double,
)

/**
 * A participação de alguém num plano (`plans/{planId}/members/{uid}`, docs/05 §1).
 *
 * **[nome] é denormalizado de propósito.** Sem ele, montar o leaderboard de oito
 * pessoas exigiria oito leituras em `users/`, e `exists()`/`get()` nas Security
 * Rules já custam leitura faturada — um quadro de 8 membros fica em ~24. Está
 * folgado no Spark, e é folga que não se multiplica sem pensar.
 *
 * **[entrouNaSemana] existe porque quem entra depois não pode ser punido.** É por
 * isso também que o leaderboard ordena por XP **da semana**, e não pelo acumulado
 * (RN-18): no acumulado, quem chegou na semana 8 nunca alcança ninguém.
 */
data class Membro(
    val uid: String,
    val nome: String,
    val entrouEm: Instant,
    val entrouNaSemana: Int,
    /** Denormalizado para o leaderboard da Fase 4 (`F4-T12`). Nulo antes dela. */
    val posicaoPokedex: Int? = null,
    val ativo: Boolean = true,
)
