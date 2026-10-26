package com.hggabriel.pokerun.ui.telas.detalheplano

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hggabriel.pokerun.dominio.modelo.Corrida
import com.hggabriel.pokerun.dominio.modelo.Membro
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.ParametrosDeGeracao
import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.modelo.Semana
import com.hggabriel.pokerun.dominio.regras.CalculoDeAderencia
import com.hggabriel.pokerun.dominio.regras.GeradorDePlano
import com.hggabriel.pokerun.dominio.regras.MudancaDeLongao
import com.hggabriel.pokerun.ui.theme.PokerunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Os previews da `PlanDetailScreen` r2, uma situação por sketch (`detalhe-r2-*.html`).
 *
 * O estado sai da própria função pura, [detalheDoPlano], sobre uma grade gerada pelo
 * `GeradorDePlano` e corridas sintéticas no formato do sketch: o preview mostra o que o
 * aparelho mostraria com os mesmos dados, e não um `Conteudo` montado à mão que pode
 * divergir dele.
 */

private val FUSO: ZoneId = ZoneId.of("America/Sao_Paulo")
private const val HIAGO = "uid-hiago"
private const val JHU = "uid-jhusilva"

private val PARAMETROS = ParametrosDeGeracao(
    dataProva = LocalDate.of(2026, 12, 31).atStartOfDay(FUSO).toInstant(),
    distanciaAlvoKm = 15.0,
    baselineKm = 6.0,
    sessoesPorSemana = 3,
    curtaKm = 5.0,
)

private val GRADE: List<Semana> =
    GeradorDePlano.gerar(PARAMETROS, LocalDate.of(2026, 9, 14).atStartOfDay(FUSO).toInstant(), FUSO)

private fun plano(encerrado: Boolean = false) = Plano(
    id = "plano-preview",
    nome = "São Silvestre 2026",
    distanciaAlvoKm = PARAMETROS.distanciaAlvoKm,
    dataProva = PARAMETROS.dataProva,
    fuso = FUSO,
    ownerUid = HIAGO,
    codigoConvite = "FYQJE6",
    encerrado = encerrado,
    parametros = PARAMETROS,
)

private val MEMBROS = listOf(
    Membro(uid = HIAGO, nome = "Hiago", entrouEm = Instant.EPOCH, entrouNaSemana = 1),
    Membro(uid = JHU, nome = "jhusilva", entrouEm = Instant.EPOCH.plusSeconds(60), entrouNaSemana = 3),
)

/** Quarta da semana 7, ao meio-dia: o "agora" do sketch. */
private val NA_SEMANA_7: Instant = GRADE[6].dataInicio.plusSeconds(2 * 86_400L + 12 * 3600L)

private val DEPOIS_DA_PROVA: Instant = GRADE.last().dataFim.plusSeconds(86_400L)

/**
 * As corridas de alguém: em cada semana, [sessoes] corridas, a última delas o longão de
 * [longao] km. As curtas têm 5 km.
 */
private fun corridas(dono: String, sessoes: List<Int?>, longao: List<Double?>): List<Corrida> =
    sessoes.flatMapIndexed { i, n ->
        val semana = GRADE.getOrNull(i) ?: return@flatMapIndexed emptyList()
        (0 until (n ?: 0)).map { j ->
            val ultima = j == (n ?: 0) - 1
            Corrida(
                id = "$dono-${i + 1}-$j",
                dataHoraInicio = semana.dataInicio.plusSeconds((j * 2L) * 86_400L + 7 * 3600L),
                km = if (ultima) longao.getOrNull(i) ?: 5.2 else 5.2,
                duracaoSeg = if (semana == GRADE.last()) 5554 else 1900,
                tipoExercicio = "RUNNING",
                origem = OrigemDaCorrida.MANUAL,
                planoId = "plano-preview",
                semanaRef = i + 1,
                temporadaId = "kanto-2026",
            )
        }
    }

/** O `weekly` que o replay de alguém gravaria para as corridas dele. */
private fun feitos(corridas: List<Corrida>): Map<Int, FeitoNaSemana> =
    GRADE.associate { semana ->
        val validas = CalculoDeAderencia.validasDa(semana, corridas)
        semana.numero to FeitoNaSemana(CalculoDeAderencia.sessoesFeitas(semana, corridas), validas.maxOfOrNull { it.km } ?: 0.0)
    }

private val H_LONGAO = listOf(6.2, 6.8, 5.1, 8.0, 8.1, 7.0, null, 10.0, 11.2, 11.0, 12.4, 12.8, 13.1, 10.2, 7.1, 15.3)
private val J_LONGAO = listOf(null, null, 7.0, 7.5, 8.0, 8.6, null, 9.8, 10.6, 11.4, 12.0, 12.5, 12.8, 9.9, 7.0, 15.3)

/** Semana 7 em curso: o Hiago correu uma curta na segunda, a jhusilva ainda nada. */
private val H_ATIVO = corridas(HIAGO, listOf(3, 3, 2, 3, 3, 2, 1), H_LONGAO)
private val J_ATIVO = corridas(JHU, listOf(null, null, 3, 2, 3, 3, 0), J_LONGAO)

private val H_ENCERRADO = corridas(HIAGO, List(GRADE.size) { if (it == GRADE.size - 1) 1 else 3 }, H_LONGAO)
private val J_ENCERRADO = corridas(JHU, List(GRADE.size) { if (it < 2) null else if (it == GRADE.size - 1) 1 else 3 }, J_LONGAO)

private fun conteudo(
    uid: String = HIAGO,
    planoAtivoId: String? = "plano-preview",
    agora: Instant = NA_SEMANA_7,
    encerrado: Boolean = false,
    minhas: List<Corrida> = if (uid == HIAGO) H_ATIVO else J_ATIVO,
    dela: List<Corrida> = if (uid == HIAGO) J_ATIVO else H_ATIVO,
) = detalheDoPlano(
    plano = plano(encerrado),
    grade = GRADE,
    membros = MEMBROS,
    corridas = minhas,
    uid = uid,
    planoAtivoId = planoAtivoId,
    agora = agora,
    feitosDoGrupo = mapOf((if (uid == HIAGO) JHU else HIAGO) to feitos(dela)),
) as DetalhePlanoUiState.Conteudo

@Composable
private fun DetalhePreview(estado: DetalhePlanoUiState) {
    PokerunTheme { DetalhePlanoScreen(estado = estado, acoes = AcoesDoDetalhe()) }
}

@Preview(name = "Dono, semana corrente", showBackground = true, heightDp = 1400)
@Composable
private fun DonoPreview() = DetalhePreview(conteudo())

@Preview(name = "Dono, semana futura com lápis", showBackground = true, heightDp = 1400)
@Composable
private fun DonoSemanaFuturaPreview() = DetalhePreview(conteudo().copy(semanaSelecionada = 9))

@Preview(name = "Membro", showBackground = true, heightDp = 1400)
@Composable
private fun MembroPreview() = DetalhePreview(conteudo(uid = JHU))

@Preview(name = "Guardado", showBackground = true, heightDp = 1400)
@Composable
private fun GuardadoPreview() = DetalhePreview(conteudo(planoAtivoId = "outro-plano"))

@Preview(name = "Encerrado com a prova", showBackground = true, heightDp = 1200)
@Composable
private fun EncerradoPreview() = DetalhePreview(
    conteudo(agora = DEPOIS_DA_PROVA, minhas = H_ENCERRADO, dela = J_ENCERRADO),
)

@Preview(name = "Encerrado sem a prova", showBackground = true, heightDp = 1200)
@Composable
private fun EncerradoSemProvaPreview() = DetalhePreview(
    conteudo(
        agora = DEPOIS_DA_PROVA,
        minhas = H_ENCERRADO.filter { it.semanaRef != GRADE.size },
        dela = J_ENCERRADO.filter { it.semanaRef != GRADE.size },
    ),
)

@Preview(name = "Depois de uma edição", showBackground = true, heightDp = 1400)
@Composable
private fun DepoisDaEdicaoPreview() = DetalhePreview(
    conteudo().copy(semanaSelecionada = 9, tambemMudaram = listOf(MudancaDeLongao(10, 11.6, 11.0))),
)

@Preview(name = "Editando o longão", showBackground = true)
@Composable
private fun EditandoPreview() = DetalhePreview(
    conteudo().copy(editando = EdicaoDoLongao(numero = 9, texto = "10,9", tetoKm = 11.2)),
)

@Preview(name = "Mudando a curta", showBackground = true)
@Composable
private fun MudandoCurtaPreview() = DetalhePreview(conteudo().copy(editandoCurta = EdicaoDaCurta(texto = "5")))

@Preview(name = "Confirmando encerrar", showBackground = true)
@Composable
private fun EncerrandoPreview() = DetalhePreview(conteudo().copy(confirmandoEncerrar = true))

@Preview(name = "Carregando", showBackground = true, heightDp = 1000)
@Composable
private fun CarregandoPreview() = DetalhePreview(DetalhePlanoUiState.Carregando)

@Preview(name = "Falhou", showBackground = true)
@Composable
private fun FalhouPreview() = DetalhePreview(DetalhePlanoUiState.Falhou)

@Preview(name = "fontScale 2,0 em 320dp", showBackground = true, fontScale = 2.0f, widthDp = 320, heightDp = 2400)
@Composable
private fun FonteGrandePreview() = DetalhePreview(conteudo())
