package com.hggabriel.pokerun.dominio.regras

/**
 * A arte oficial das Insígnias de Ginásio (decisão 19): `sprites/badges/1..8.png` do mesmo
 * repositório dos sprites, na ordem dos ginásios de Kanto. O dado já está em
 * `desafios.insignia_id`; aqui mora só a tabela do nome para o número do arquivo.
 */
object Insignias {

    private const val ENDERECO = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/badges/"

    /** O número do arquivo de cada insígnia: Pedra 1, Cascata 2, ... Terra 8. */
    fun numero(id: String): Int? = DesafioColetivo.INSIGNIAS.indexOf(id).takeIf { it >= 0 }?.plus(1)

    fun url(id: String): String? = numero(id)?.let { "$ENDERECO$it.png" }

    /**
     * As insígnias da vida: as conquistadas em qualquer plano da pessoa, sem repetir, na ordem
     * dos ginásios. São os broches presos no cordão da credencial do Perfil.
     */
    fun daVida(desafiosPorPlano: Collection<Collection<Desafio>>): List<String> =
        DesafioColetivo.conquistadas(desafiosPorPlano.flatten())
}
