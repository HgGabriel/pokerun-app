package com.hggabriel.pokerun.dados.healthconnect

import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.TemporadaRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dominio.modelo.Corrida
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * O que uma sincronização achou, pronto para a `ImportReviewScreen` (`F2-T05`).
 *
 * **Nada aqui foi gravado** (RN-23): [corridas] são as candidatas, com os snapshots já
 * tirados, e só `Importar selecionadas` as grava. [ate] é para onde o cursor vai ao fim
 * da revisão, importando ou dispensando.
 *
 * [naoConvertidas] são corridas da fonte canônica que não viraram `Corrida`: sem distância,
 * ou sem temporada publicada que cubra a data (decisão nº 103). **O cursor não pode passar
 * da primeira sem temporada**, ou ela some quando o console publicar a temporada que falta
 * — é o cuidado que `F2-T05` herda.
 */
data class ResultadoDaLeitura(
    val desde: Instant,
    val ate: Instant,
    val corridas: List<Corrida>,
    val deOutraOrigem: Int,
    val naoConvertidas: List<SessaoDoHealthConnect>,
)

/**
 * A sincronização com o Health Connect, do cursor até agora (`F2-T02`, docs/05 §4.3).
 *
 * **Só na abertura do app** (RN-25): sem serviço, sem `WorkManager`. Quem chama é o
 * roteador da abertura de `F2-T05`, antes do fechamento semanal (RN-44).
 *
 * Costura, e não regra: lê o perfil, o plano ativo e o catálogo de temporadas, pede as
 * sessões a [SaudeRepositorio] e passa tudo pelas funções puras de `LeituraDeSessoes`,
 * que são as que têm teste. A idempotência contra o que já está gravado é `F2-T04`.
 */
class LeituraDoHealthConnect(
    private val saude: SaudeRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val planos: PlanoRepositorio,
    private val temporadas: TemporadaRepositorio,
) {

    /**
     * **Nulo quando não há o que ler**, e nenhum dos casos é erro: Health Connect
     * indisponível ou sem fonte canônica (o modo manual, docs/05 §4.4), ou sem plano
     * ativo — `plano_id` é snapshot obrigatório (RN-14), e o cursor intocado faz a
     * primeira leitura depois de criar o plano começar no início dele.
     *
     * Falha de leitura propaga, como em [SaudeRepositorio.sessoesEntre]: "não deu para
     * ler" e "não há corrida nova" não podem virar a mesma resposta.
     */
    suspend fun ler(uid: String, agora: Instant = Instant.now()): ResultadoDaLeitura? {
        if (saude.status() != StatusDoHealthConnect.Disponivel) return null

        // O listener entrega o cache primeiro: a leitura funciona offline para quem já
        // abriu o app com rede, e o Health Connect é local.
        val usuario = usuarios.observar(uid).first() ?: return null
        val fonte = usuario.fonteCanonica ?: return null
        val planoId = usuario.planoAtivoId ?: return null
        val plano = planos.observar(planoId).first() ?: return null
        val grade = planos.observarSemanas(planoId).first()

        val desde = inicioDaLeitura(usuario.hcSincronizadoAte, grade) ?: return null
        val triagem = triar(saude.sessoesEntre(desde, agora), fonte)
        val catalogo = temporadas.todas()

        val corridas = mutableListOf<Corrida>()
        val naoConvertidas = mutableListOf<SessaoDoHealthConnect>()
        for (sessao in triagem.daFonte) {
            val corrida = corridaImportada(sessao, saude.medidasDa(sessao), plano, grade, catalogo)
            if (corrida != null) corridas += corrida else naoConvertidas += sessao
        }

        return ResultadoDaLeitura(
            desde = desde,
            ate = agora,
            corridas = corridas,
            deOutraOrigem = triagem.deOutraOrigem,
            naoConvertidas = naoConvertidas,
        )
    }
}
