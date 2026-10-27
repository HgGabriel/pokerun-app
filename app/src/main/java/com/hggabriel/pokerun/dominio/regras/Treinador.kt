package com.hggabriel.pokerun.dominio.regras

import java.text.Normalizer
import java.util.Locale

/** Um grupo curado do seletor de treinador, com o nome de arquivo de cada um. */
data class GrupoDeTreinadores(val nome: String, val treinadores: List<String>)

/**
 * O treinador: a pessoa, escolhida entre os sprites de treinador do Pokémon Showdown
 * (estudo de Progresso e Perfil, Revisão 3, decisão 22). Treinador é a pessoa e parceiro é o
 * Pokémon: os dois são escolhas independentes.
 *
 * O app embute só a lista de nomes (`assets/treinadores.txt`), e nenhum sprite: a imagem vem
 * do servidor do Showdown, e o cache de disco do Coil guarda as já vistas.
 */
object Treinador {

    const val ENDERECO = "https://play.pokemonshowdown.com/sprites/trainers/"

    /** O sprite de corpo inteiro, 80 por 80 px. Sempre sem suavização: é pixel art. */
    fun url(nome: String): String = "$ENDERECO$nome.png"

    /** Regra 1: os 1.502 estão livres desde o primeiro dia, e só um nome da lista vale. */
    fun aceita(nome: String, lista: Collection<String>): Boolean = nome in lista

    /**
     * Regra 3: sem escolha não há sprite, e quem desenha mostra a silhueta de treinador em
     * `superficie-afundada`, como a espécie não alcançada.
     */
    fun urlOuSilhueta(nome: String?): String? = nome?.let(::url)

    /** Os três grupos curados do topo do seletor (regra 2). */
    val grupos: List<GrupoDeTreinadores> = listOf(
        GrupoDeTreinadores(
            "Corredores",
            listOf(
                "jogger", "cyclist", "cyclistf", "swimmer", "swimmerf", "battlegirl",
                "blackbelt", "hiker", "acetrainer", "acetrainerf", "youngster", "lass",
            ),
        ),
        GrupoDeTreinadores("Kanto", listOf("red", "leaf-masters", "green", "brock", "misty", "erika")),
        GrupoDeTreinadores(
            "Protagonistas",
            listOf(
                "ethan", "lyra", "brendan", "may", "lucas", "dawn", "hilbert", "hilda",
                "nate", "rosa", "calem", "serena", "elio", "selene", "victor", "gloria",
            ),
        ),
    )

    private val rotulos = mapOf(
        "jogger" to "Corredor", "cyclist" to "Ciclista", "cyclistf" to "Ciclista",
        "swimmer" to "Nadador", "swimmerf" to "Nadadora", "battlegirl" to "Lutadora",
        "blackbelt" to "Faixa-preta", "hiker" to "Montanhista", "acetrainer" to "Ás",
        "acetrainerf" to "Ás", "youngster" to "Jovem", "lass" to "Moça", "leaf-masters" to "Leaf",
    )

    /**
     * O nome que a tela mostra. Os curados têm rótulo em pt-BR; os outros saem do nome do
     * arquivo, com a primeira letra de cada parte em maiúscula (`acerola-masters2` vira
     * `Acerola Masters2`).
     */
    fun rotulo(nome: String): String = rotulos[nome]
        ?: nome.split('-', '_').filter { it.isNotEmpty() }
            .joinToString(" ") { parte -> parte.replaceFirstChar { it.titlecase(Locale.ROOT) } }

    /**
     * A busca do seletor, sobre a lista embutida. Casa o nome do arquivo e o rótulo, sem
     * acento nem caixa.
     */
    fun buscar(termo: String, lista: List<String>): List<String> {
        val alvo = normalizar(termo)
        if (alvo.isEmpty()) return emptyList()
        val achados = lista.filter { normalizar(it).contains(alvo) || normalizar(rotulo(it)).contains(alvo) }
        // O nome exato vem primeiro, depois o que começa com o termo, depois o que o contém;
        // dentro de cada faixa, o nome mais curto (o `red` antes do `red-masters4`).
        return achados.sortedWith(
            compareBy<String>(
                { if (normalizar(it) == alvo || normalizar(rotulo(it)) == alvo) 0 else 1 },
                { if (normalizar(it).startsWith(alvo) || normalizar(rotulo(it)).startsWith(alvo)) 0 else 1 },
                { it.length },
            ),
        )
    }

    /** A lista do arquivo: um nome por linha, sem linha vazia nem repetida. */
    fun lerLista(texto: String): List<String> =
        texto.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.distinct().toList()

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
