package com.hggabriel.pokerun.ui.navegacao

import kotlinx.serialization.Serializable

/*
 * As rotas do grafo (`F1-T07`, docs/03 §1).
 *
 * **Type-safe:** cada destino é um tipo, e o argumento é campo de `data class` em vez
 * de string montada à mão. Um destino que ganha argumento novo passa a não compilar
 * em quem navega para ele, que é o oposto do que acontece com rota em texto.
 *
 * **Sem deep link**, e é recusa consciente de docs/03 §1: não há notificação, não há
 * web, e a distribuição é App Distribution para oito pessoas. O único candidato seria
 * o código de convite, e App Links verificados com domínio e chave são
 * desproporcionais para seis caracteres digitados uma vez.
 *
 * **Só existem aqui as rotas que a Fase 1 usa**, mais `EditarCorrida`, que a ficha de
 * `F1-T07` nomeia na pilha modal. As telas de Fase 2 a 4 — importação, fechamento
 * semanal, detalhe de corrida, Pokédex — entram com as tarefas donas. Declarar rota
 * para tela que ninguém escreveu só produz grafo com buraco.
 */

// ---------------------------------------------------------------------------
// Fora da barra: a porta de entrada
// ---------------------------------------------------------------------------

@Serializable
data object Login

@Serializable
data object Onboarding

/** A casca com a barra inferior. Tudo que tem aba mora dentro dela. */
@Serializable
data object Casca

// ---------------------------------------------------------------------------
// Os três destinos de topo (docs/03 §2)
// ---------------------------------------------------------------------------

/*
 * Cada aba é um **grafo aninhado**, e não um destino solto, porque docs/03 §1 exige
 * uma pilha por destino de topo. O grafo é o que dá identidade à pilha: com destinos
 * soltos, descer um nível dentro de `Progresso` deixaria a barra sem saber qual aba
 * acender, e a marca cairia em `Hoje`.
 *
 * `Aba*` é o grafo; o objeto sem prefixo é a raiz dele, que é a tela que abre.
 */

@Serializable
data object AbaHoje

@Serializable
data object Hoje

@Serializable
data object AbaProgresso

@Serializable
data object Progresso

@Serializable
data object AbaGrupo

@Serializable
data object Grupo

// ---------------------------------------------------------------------------
// Dentro da aba Hoje
// ---------------------------------------------------------------------------

@Serializable
data class DetalheDoPlano(val planoId: String)

@Serializable
data class DetalheDaSemana(val planoId: String, val numero: Int)

@Serializable
data object CorridaManual

// ---------------------------------------------------------------------------
// Pilha modal, fora da barra (docs/03 §1)
// ---------------------------------------------------------------------------

/**
 * Ajustes (`F1-T17`).
 *
 * **Fica na pilha modal, e não dentro de uma aba**, embora docs/03 §1 a desenhe sob
 * `Hoje`. A mesma seção manda a engrenagem aparecer na **raiz de todas as abas**, e um
 * destino que pertence ao grafo de `Hoje` acenderia `Hoje` na barra ao ser aberto de
 * `Grupo` — o usuário toca na engrenagem e a aba muda embaixo dele. Modal não tem aba,
 * então não há o que acender.
 */
@Serializable
data object Ajustes

@Serializable
data object ListaDePlanos

@Serializable
data object CriarPlano

/**
 * A revisão do rascunho (`F1-T11`).
 *
 * **A rota carrega os parâmetros de entrada, não a grade gerada.** O gerador é função
 * pura (`F1-T02`): dados os mesmos quatro parâmetros mais o fuso, ele devolve a mesma
 * grade de 21 semanas. Serializar a grade inteira num argumento de rota seria carregar
 * o resultado quando a entrada cabe em cinco campos — e um argumento de rota sobrevive
 * a morte de processo, o que faz a tela de revisão renascer idêntica de graça.
 *
 * [dataProvaEpochDia] é dia epoch e não instante porque a prova é uma data no
 * calendário do plano, não um momento.
 */
@Serializable
data class RevisarRascunho(
    val nome: String,
    val fuso: String,
    val dataProvaEpochDia: Long,
    val distanciaAlvoKm: Double,
    val baselineKm: Double,
    val sessoesPorSemana: Int,
)

@Serializable
data object EntrarComCodigo

/** `F2-T10`. A rota existe porque a ficha de `F1-T07` a nomeia na pilha modal. */
@Serializable
data class EditarCorrida(val corridaId: String)
