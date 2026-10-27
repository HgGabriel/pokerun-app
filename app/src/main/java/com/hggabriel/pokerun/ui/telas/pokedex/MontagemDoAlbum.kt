package com.hggabriel.pokerun.ui.telas.pokedex

import androidx.compose.runtime.Immutable
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Temporada
import com.hggabriel.pokerun.dominio.regras.EstadoDaCelula
import com.hggabriel.pokerun.dominio.regras.MarcoDaSemana
import com.hggabriel.pokerun.dominio.regras.albumDaPokedex
import com.hggabriel.pokerun.dominio.regras.coladasNaSemana
import com.hggabriel.pokerun.dominio.regras.gradeDaPokedex
import com.hggabriel.pokerun.dominio.regras.kmAteOProximo
import com.hggabriel.pokerun.dominio.regras.painelDaPokedex
import com.hggabriel.pokerun.dominio.regras.posicaoNaEscada
import com.hggabriel.pokerun.dominio.regras.tierDaPosicao
import java.time.Instant

/** Quantas coladas a fita mostra antes da posição, e quantas vagas depois dela. */
private const val COLADAS_NA_FITA = 6
private const val VAGAS_NA_FITA = 5

/**
 * O álbum montado para a tela.
 *
 * @property coladaEm a corrida que colou cada figurinha, para o verso.
 * @property pacote as posições que a coleção ganhou e o pacote ainda não colou.
 * @property kmParaAlcancar o `~790 km` de cada posição fora da coleção, no ritmo de agora.
 * @property fracaoParaAlcancar quanto do XP de cada posição fora da coleção já foi feito.
 */
@Immutable
internal data class AlbumMontado(
    val capa: CapaDoAlbum,
    val paginas: List<PaginaDoAlbum>,
    val tierAtual: Int,
    val pacote: IntRange,
    val coladaEm: Map<Int, Corrida>,
    val kmParaAlcancar: Map<Int, Double> = emptyMap(),
    val fracaoParaAlcancar: Map<Int, Float> = emptyMap(),
)

/**
 * Junta a coleção, as corridas e a meta da semana no álbum que a Pokédex desenha (docs/03
 * §3.18). Só leitura: tudo sai das regras de domínio.
 *
 * @param xpDaMeta o XP que a semana ainda rende se o plano for cumprido, ou nulo.
 * @param pacote as posições que o pacote ainda não colou: até ele abrir, elas são vagas.
 */
internal fun montarAlbum(
    temporada: Temporada,
    xp: Long,
    colecaoMax: Int,
    historico: List<MarcoDaSemana>,
    corridas: List<Corrida>,
    xpDaMeta: Long?,
    agora: Instant,
    pacote: IntRange = IntRange.EMPTY,
): AlbumMontado {
    val posicao = posicaoNaEscada(xp, temporada)
    val colecao = maxOf(colecaoMax, posicao)
    val celulas = gradeDaPokedex(temporada, posicao, colecao, xp)
    val album = albumDaPokedex(temporada, corridas, colecao)
    val painel = painelDaPokedex(temporada, xp, colecao, historico, xpDaMeta)
    val novas = coladasNaSemana(colecao, historico)
    val alcanceDaMeta = xpDaMeta?.let { posicaoNaEscada(xp + it, temporada) } ?: posicao
    val ultimoTier = temporada.tiers.maxOfOrNull { it.ordem }
    val proximaPosicao = (posicao + 1).takeIf { it <= celulas.size }

    val figurinhas = celulas.map { c ->
        FigurinhaDoAlbum(
            numero = c.numero,
            posicao = c.posicao,
            colada = c.estado != EstadoDaCelula.NAO_ALCANCADA && c.posicao !in pacote,
            atual = c.estado == EstadoDaCelula.ATUAL,
            proximo = c.posicao == proximaPosicao,
            nova = c.posicao in novas && c.posicao !in pacote,
            naMeta = c.posicao in (posicao + 1)..alcanceDaMeta,
        )
    }
    val porTier = celulas.zip(figurinhas).groupBy({ it.first.tier }, { it.second })
    val paginas = temporada.tiers.sortedBy { it.ordem }.map { tier ->
        PaginaDoAlbum(
            ordem = tier.ordem,
            nome = tier.nome,
            figurinhas = porTier[tier.ordem].orEmpty(),
            km = album.kmPorTier[tier.ordem],
            lacrada = tier.ordem == ultimoTier && temporada.tiers.size > 1,
        )
    }
    // A fita não entra no envelope lacrado: ele não promete vaga (XP-11).
    val lacradas = paginas.filter { it.lacrada }.flatMap { p -> p.figurinhas.map { it.posicao } }.toSet()
    val fita = figurinhas
        .filter { it.posicao in (posicao - COLADAS_NA_FITA)..(posicao + VAGAS_NA_FITA) && it.posicao !in lacradas }

    val capa = CapaDoAlbum(
        proximo = painel.proximoAlvo.takeIf { proximaPosicao != null && proximaPosicao !in lacradas },
        kmAteOProximo = kmAteOProximo(painel.faltamXp, corridas, temporada, agora),
        recuo = if (painel.recuou) Recuo(colecao = painel.colecaoMax, formaAtual = posicao) else null,
        fita = fita,
        indice = paginas.map { p -> p.ordem to if (p.total == 0) 0f else p.coladas.toFloat() / p.total },
        tierAtual = tierDaPosicao(temporada, posicao),
        colecao = colecao,
        total = temporada.totalEspecies,
    )
    return AlbumMontado(
        capa = capa,
        paginas = paginas,
        tierAtual = capa.tierAtual,
        pacote = pacote,
        coladaEm = album.coladaEm,
        kmParaAlcancar = celulas.filter { it.estado == EstadoDaCelula.NAO_ALCANCADA }
            .mapNotNull { c -> kmAteOProximo(c.faltamXp, corridas, temporada, agora)?.let { c.posicao to it } }
            .toMap(),
        fracaoParaAlcancar = celulas.filter { it.estado == EstadoDaCelula.NAO_ALCANCADA && it.xpParaAlcancar > 0 }
            .associate { it.posicao to (xp.toFloat() / it.xpParaAlcancar).coerceIn(0f, 1f) },
    )
}
