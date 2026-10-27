package com.hggabriel.pokerun.dominio.regras

import com.hggabriel.pokerun.dominio.modelo.Temporada

/**
 * Um tier no seletor de parceiro: o nome, a `descricao` do catálogo e as espécies liberadas
 * dele, pelo número da Pokédex, na ordem da escada.
 */
data class TierDeParceiros(
    val temporadaId: String,
    val ordem: Int,
    val nome: String,
    val descricao: String,
    val especies: List<Int>,
)

/**
 * O parceiro: o Pokémon que a pessoa escolhe para representá-la (estudo de Progresso e
 * Perfil, d.2). Ocupa tudo que é "eu", menos o progresso: a arena, a montanha, a estrada, a
 * credencial e o avatar do grupo. O card da Home, a escada e a Pokédex continuam no XP.
 */
object Parceiro {

    /**
     * Regra 1: liberada é toda espécie com posição até a `colecao_max` de qualquer
     * temporada do usuário, traduzida pela escada **daquela** temporada.
     *
     * Vem da coleção, e não da espécie atual (decisão 8): a posição recua com a regressão
     * (RN-37), e um parceiro que some porque a pessoa parou de correr puniria a identidade.
     * A coleção só sobe (RN-38), então um liberado nunca deixa de ser.
     *
     * Temporada do mapa que não está no catálogo fica de fora: sem a escada, a posição não
     * diz espécie nenhuma.
     */
    fun liberados(colecoes: Map<String, Int>, catalogo: List<Temporada>): Set<Int> =
        grade(colecoes, catalogo).flatMap { it.especies }.toSet()

    /**
     * A grade do seletor: um bloco por tier com espécie liberada, do tier mais alto ao mais
     * baixo, e a temporada mais recente primeiro. Espécie liberada em duas temporadas
     * aparece uma vez, na primeira em que a grade a encontra.
     */
    fun grade(colecoes: Map<String, Int>, catalogo: List<Temporada>): List<TierDeParceiros> {
        val vistos = mutableSetOf<Int>()
        return catalogo.sortedByDescending { it.inicio }.flatMap { temporada ->
            val colecao = colecoes[temporada.id] ?: return@flatMap emptyList()
            val degraus = escada(temporada).map { it.first }.take(colecao.coerceAtLeast(0))
            val liberadas = degraus.toSet()
            temporada.tiers.sortedByDescending { it.ordem }.mapNotNull { tier ->
                val especies = tier.especies.filter { it in liberadas && vistos.add(it) }
                if (especies.isEmpty()) null
                else TierDeParceiros(temporada.id, tier.ordem, tier.nome, tier.descricao, especies)
            }
        }
    }

    /**
     * Regra 2: sem escolha, o parceiro é a espécie da `colecao_max` da temporada corrente, o
     * comportamento do avatar antes do parceiro existir. Quem nunca abre o Perfil não perde
     * nada, e o avatar continua subindo sozinho.
     */
    fun padrao(colecaoCorrente: Int?, temporadaCorrente: Temporada): Int =
        numeroNaEscada(colecaoCorrente ?: 1, temporadaCorrente)

    /** Regra 3: a escolha só vale entre os liberados. O seletor nem mostra os outros. */
    fun aceita(escolha: Int, liberados: Set<Int>): Boolean = escolha in liberados

    /**
     * Regra 6: o que os leitores de membro mostram. A escolha gravada em
     * `members/{uid}.parceiro`, e sem ela a espécie da escada no `avatar_especie`, como antes.
     */
    fun doMembro(parceiro: Int?, avatarEspecie: Int?, temporada: Temporada): Int =
        parceiro ?: numeroNaEscada(avatarEspecie ?: 1, temporada)

    /** O parceiro de quem está usando o app: a escolha, ou o [padrao]. */
    fun efetivo(escolhido: Int?, colecaoCorrente: Int?, temporadaCorrente: Temporada): Int =
        escolhido ?: padrao(colecaoCorrente, temporadaCorrente)
}
