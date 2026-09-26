package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.modelo.Tier
import kotlinx.coroutines.tasks.await

private const val TEMPORADAS = "temporadas"

private const val NOME = "nome"
private const val REGIAO = "regiao"
private const val INICIO = "inicio"
private const val FIM = "fim"
private const val FUSO = "fuso"
private const val ATIVA = "ativa"
private const val TOTAL_ESPECIES = "total_especies"
private const val TIERS = "tiers"

private const val ORDEM = "ordem"
private const val DESCRICAO = "descricao"
private const val ESPECIES = "especies"
private const val XP_POR_ESPECIE = "xp_por_especie"

/**
 * O catálogo de temporadas: `temporadas/{temporadaId}` (`F2-T02`, docs/05 §1, docs/08).
 *
 * **Só lê.** A rule é `write: if false`: quem escreve é o console (`F2-T00`), e é isso
 * que faz da definição dado e não código (RN-43) — virar o ano não pede release.
 *
 * **[todas] e não "a ativa"**, porque quem grava corrida precisa da temporada **da
 * data** (RN-40), e na virada a corrida de 31/12 importada em janeiro é de uma temporada
 * que já não é a ativa. O catálogo cresce uma temporada por ano; ler inteiro é barato.
 */
class TemporadaRepositorio(private val firestore: FirebaseFirestore) {

    /**
     * `get()` sem fonte explícita: com rede, pergunta ao servidor; sem rede, cai no
     * cache local (docs/05 §2.6). O registro manual offline funciona assim para quem já
     * abriu o app com rede alguma vez.
     */
    suspend fun todas(): List<Temporada> =
        firestore.collection(TEMPORADAS).get().await().documents
            .map { it.paraTemporada() }
            .sortedBy { it.inicio }
}

internal fun DocumentSnapshot.paraTemporada(): Temporada = Temporada(
    id = id,
    nome = exigirTexto(NOME),
    regiao = exigirTexto(REGIAO),
    inicio = exigirInstante(INICIO),
    fim = exigirInstante(FIM),
    fuso = fusoDoDocumento(exigirTexto(FUSO)),
    ativa = exigirBooleano(ATIVA),
    totalEspecies = exigirInteiro(TOTAL_ESPECIES),
    tiers = tiers(),
)

/**
 * `tiers` é uma lista de mapas, e o SDK devolve os dois níveis sem tipo. Mesmo critério
 * de `parametrosDeGeracao`: campo ausente falha alto, porque a curva de custo que sairia
 * daqui é a que transforma XP em espécie.
 */
private fun DocumentSnapshot.tiers(): List<Tier> {
    val bruto = get(TIERS) as? List<*>
        ?: error("campo '$TIERS' ausente ou fora de forma em ${reference.path} (docs/08)")

    return bruto.mapIndexed { posicao, item ->
        val tier = item as? Map<*, *>
            ?: error("'$TIERS[$posicao]' fora de forma em ${reference.path} (docs/08)")

        fun valor(campo: String): Any = tier[campo]
            ?: error("campo '$TIERS[$posicao].$campo' ausente em ${reference.path} (docs/08)")

        Tier(
            ordem = (valor(ORDEM) as Number).toInt(),
            nome = valor(NOME) as String,
            descricao = valor(DESCRICAO) as String,
            especies = (valor(ESPECIES) as List<*>).map { (it as Number).toInt() },
            xpPorEspecie = (valor(XP_POR_ESPECIE) as Number).toInt(),
        )
    }
}
