package com.hggabriel.pokerun.dominio.modelo

import java.time.Instant

/**
 * O perfil do corredor (`users/{uid}`, docs/05 §1).
 *
 * O documento nasce no cadastro, com [nome] e [baselineKm], e a existência dele é o
 * marcador de cadastro concluído (docs/03 §3.2).
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
    /**
     * A posição na escada que o avatar do ranking mostra (docs/03 §3.16.1): a espécie
     * atual na Fase 2. Escrita só pelo replay do dono do uid (`F2-T07`). Nula antes dele.
     */
    val avatarEspecie: Int? = null,
)
