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
     * acento nem caixa. Quem busca a cada tecla guarda um [IndiceDeTreinadores] e chama o dele:
     * este monta o índice de novo a cada chamada.
     */
    fun buscar(termo: String, lista: List<String>): List<String> = IndiceDeTreinadores(lista).buscar(termo)

    /** A lista do arquivo: um nome por linha, sem linha vazia nem repetida. */
    fun lerLista(texto: String): List<String> =
        texto.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.distinct().toList()

    private val marcas = Regex("\\p{M}+")

    internal fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marcas, "")
}

/**
 * A lista de treinadores pronta para a busca: o nome e o rótulo de cada um normalizados uma vez
 * só, na construção (29/10/2026). A busca antiga normalizava os dois de novo em cada comparação
 * da ordenação: com "a" eram ~978 achados e dezenas de milhares de normalizações por tecla.
 */
class IndiceDeTreinadores(val lista: List<String>) {

    private val nomes: Array<String> = Array(lista.size) { Treinador.normalizar(lista[it]) }
    private val rotulos: Array<String> = Array(lista.size) { Treinador.normalizar(Treinador.rotulo(lista[it])) }
    private val conjunto: Set<String> = lista.toHashSet()

    val tamanho: Int get() = lista.size

    /** A regra 1 ([Treinador.aceita]) sem varrer a lista. */
    fun contem(nome: String): Boolean = nome in conjunto

    /**
     * O nome exato vem primeiro, depois o que começa com o termo, depois o que o contém; dentro
     * de cada faixa, o nome mais curto (o `red` antes do `red-masters4`), e no empate a ordem da
     * lista. Nome e rótulo casam do mesmo jeito.
     */
    fun buscar(termo: String): List<String> {
        val alvo = Treinador.normalizar(termo)
        if (alvo.isEmpty()) return emptyList()
        val achados = ArrayList<Long>()
        for (i in nomes.indices) {
            val nome = nomes[i]
            val rotulo = rotulos[i]
            val faixa = when {
                nome == alvo || rotulo == alvo -> 0L
                nome.startsWith(alvo) || rotulo.startsWith(alvo) -> 1L
                nome.contains(alvo) || rotulo.contains(alvo) -> 2L
                else -> continue
            }
            // Uma chave só, que ordena por faixa, comprimento e posição: 2 bits, 20 e 32.
            achados += (faixa shl 52) or (lista[i].length.toLong() shl 32) or i.toLong()
        }
        achados.sort()
        return achados.map { lista[(it and 0xFFFFFFFFL).toInt()] }
    }
}
