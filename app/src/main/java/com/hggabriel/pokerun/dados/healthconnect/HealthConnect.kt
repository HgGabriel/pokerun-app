package com.hggabriel.pokerun.dados.healthconnect

/*
 * O cliente do Health Connect, o filtro de fonte canônica, a derivação de splits e
 * a idempotência da ingestão (`F2-T01` a `F2-T04`, docs/05 §4).
 *
 * A leitura acontece **só na abertura do app** (RN-25): sem serviço, sem
 * WorkManager, sem notificação agendada. E `metadata.id` não é chave estável — ele
 * muda se o app de origem apagar e reinserir —, por isso a idempotência tem três
 * chaves (docs/05 §4.3).
 *
 * `getSdkStatus()` indisponível não é erro: é o modo manual, e o app continua
 * inteiro sem isto aqui.
 *
 * **`F1-T08` chegou antes da Fase 2** e pôs aqui o `SaudeRepositorio`: o mínimo que o
 * onboarding precisa para pedir permissão, descobrir se o aparelho tem Health Connect e
 * listar quem gravou treino nos últimos 30 dias. **`F2-T01` construiu o contrato de
 * leitura da ingestão em cima dele, não ao lado** (`ResumoDaSessao.kt` tem os tipos); o
 * filtro, os splits e a idempotência são `F2-T02` a `F2-T04`.
 *
 * Este arquivo não declara nada; ver `ui/telas/Telas.kt` para o porquê.
 */
