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
// Dentro das abas
// ---------------------------------------------------------------------------

@Serializable
data class DetalheDoPlano(val planoId: String)

@Serializable
data class DetalheDaSemana(val planoId: String, val numero: Int)

@Serializable
data object CorridaManual

/**
 * Ajustes (`F1-T17`), **declarado uma vez e usado em cada grafo de aba** (`F1-T07c`).
 *
 * docs/03 §1 o desenha sob `Hoje` e a mesma seção manda a engrenagem aparecer na
 * **raiz de todas as abas**; docs/02 §10.1 repete. As duas juntas não cabem num
 * destino só: um destino que pertence ao grafo de `Hoje` acende `Hoje` na barra ao ser
 * aberto de `Grupo` — a aba muda embaixo do usuário — e o voltar devolve à pilha
 * errada.
 *
 * **`F1-T07` resolveu isso pondo `Ajustes` na pilha modal, contra o desenho**, e a
 * revisão de 17/08 mandou seguir o desenho. A saída que atende as duas regras é esta:
 * a rota é uma só, e a [CascaDeNavegacao] a registra dentro de **cada** aba. A barra
 * deriva a aba acesa da hierarquia do grafo, então em `AbaGrupo/Ajustes` quem acende é
 * `Grupo`, por construção — sem a casca guardar de onde o usuário veio, que seria
 * estado a sobreviver à morte de processo.
 *
 * **Não é mais destino da pilha modal**, e `AjustesPorAbaTest` falha se voltar a ser.
 */
@Serializable
data object Ajustes

// ---------------------------------------------------------------------------
// Pilha modal, fora da barra (docs/03 §1)
// ---------------------------------------------------------------------------

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
