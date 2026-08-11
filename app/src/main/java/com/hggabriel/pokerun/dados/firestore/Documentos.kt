package com.hggabriel.pokerun.dados.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.hggabriel.pokerun.dominio.modelo.OrigemDaCorrida
import com.hggabriel.pokerun.dominio.modelo.SessaoReivindicada
import com.hggabriel.pokerun.dominio.modelo.TipoDeSemana
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import java.time.ZoneId

/*
 * As primitivas de conversão documento ↔ modelo e a ponte entre o listener do
 * Firestore e `Flow` (`F1-T05`, docs/05 §1).
 *
 * Nada aqui conhece um agregado específico: os mapeamentos de `Plano`, `Usuario` e
 * `Corrida` ficam nos arquivos dos respectivos repositórios, um por agregado, como
 * em `dominio/modelo`.
 */

// ---------------------------------------------------------------------------
// Instante, fuso e vocabulário fechado
// ---------------------------------------------------------------------------

/**
 * O `Timestamp` do Firestore guarda **microssegundos**, e um `Instant` com precisão
 * de nanossegundo perde os três últimos dígitos na ida e volta. Nenhum campo do
 * schema depende disso: corrida vem do Health Connect em milissegundos e as
 * fronteiras de semana são meia-noite.
 */
internal fun Instant.paraTimestamp(): Timestamp = Timestamp(epochSecond, nano)

internal fun Timestamp.paraInstant(): Instant =
    Instant.ofEpochSecond(seconds, nanoseconds.toLong())

/**
 * // RN-28
 *
 * O fuso viaja como texto (`"America/Sao_Paulo"`) e volta como [ZoneId] tipado. A
 * conversão falha alto num identificador que a JVM não conhece, e é de propósito:
 * cair para `systemDefault()` aqui moveria a fronteira das semanas para quem
 * viajasse, e `semana_ref` é snapshot — o erro seria permanente.
 */
internal fun ZoneId.paraDocumento(): String = id

internal fun fusoDoDocumento(texto: String): ZoneId = ZoneId.of(texto)

/** `"build" | "taper" | "prova"`, exatamente como docs/05 §1 fixa. */
internal fun TipoDeSemana.paraDocumento(): String = name.lowercase()

internal fun tipoDeSemanaDoDocumento(texto: String): TipoDeSemana =
    TipoDeSemana.entries.firstOrNull { it.paraDocumento() == texto }
        ?: error("tipo de semana desconhecido: '$texto' (docs/05 §1 fixa build, taper e prova)")

/** `"hc" | "manual"` (docs/05 §1). Modo manual é caminho previsto, não falha. */
internal fun OrigemDaCorrida.paraDocumento(): String = name.lowercase()

internal fun origemDoDocumento(texto: String): OrigemDaCorrida =
    OrigemDaCorrida.entries.firstOrNull { it.paraDocumento() == texto }
        ?: error("origem de corrida desconhecida: '$texto' (docs/05 §1 fixa hc e manual)")

/** O prefixo das sessões curtas. O índice começa em 1 (`SessaoReivindicada.Curta`). */
private const val PREFIXO_DA_CURTA = "curta_"

/**
 * // RN-34
 *
 * Devolve o tipo fechado a partir do token do documento. **Token desconhecido é
 * erro, nunca `null`:** o multiplicador de XP sai exatamente daqui (XP-03), e um
 * `"curta_0"` virando ausência silenciosa tira o multiplicador da corrida sem que
 * nada apareça em tela nem em log.
 */
internal fun sessaoDoDocumento(token: String): SessaoReivindicada {
    if (token == SessaoReivindicada.Longao.token) return SessaoReivindicada.Longao

    val indice = token.removePrefix(PREFIXO_DA_CURTA).toIntOrNull()
    require(token.startsWith(PREFIXO_DA_CURTA) && indice != null && indice >= 1) {
        "sessão reivindicada fora do vocabulário de docs/05 §1: '$token'"
    }
    return SessaoReivindicada.Curta(indice)
}

// ---------------------------------------------------------------------------
// Leitura de campo
// ---------------------------------------------------------------------------

/*
 * **Campo ausente é documento corrompido, e falha alto.** Todo documento é escrito
 * por este pacote com o schema inteiro, inclusive os booleanos — então a única
 * origem de uma ausência é edição manual no console ou um schema divergente, e as
 * duas produzem cálculo errado em silêncio se o código completar o buraco sozinho.
 * Uma semana sem `congelada` viraria semana editável (RN-05); um plano sem `fuso`
 * viraria fuso do aparelho (RN-28).
 *
 * A ausência do **documento inteiro** é outra coisa e é caso previsto: quem observa
 * recebe `null` (usuário antes do onboarding, plano que o dono nunca criou).
 */

private fun DocumentSnapshot.faltando(campo: String): Nothing =
    error("campo '$campo' ausente em ${reference.path} (docs/05 §1)")

internal fun DocumentSnapshot.exigirTexto(campo: String): String =
    getString(campo) ?: faltando(campo)

internal fun DocumentSnapshot.exigirDecimal(campo: String): Double =
    getDouble(campo) ?: faltando(campo)

internal fun DocumentSnapshot.exigirLongo(campo: String): Long =
    getLong(campo) ?: faltando(campo)

internal fun DocumentSnapshot.exigirInteiro(campo: String): Int =
    exigirLongo(campo).toInt()

internal fun DocumentSnapshot.exigirBooleano(campo: String): Boolean =
    getBoolean(campo) ?: faltando(campo)

internal fun DocumentSnapshot.exigirInstante(campo: String): Instant =
    getTimestamp(campo)?.paraInstant() ?: faltando(campo)

internal fun DocumentSnapshot.inteiroOuNulo(campo: String): Int? = getLong(campo)?.toInt()

internal fun DocumentSnapshot.decimalOuNulo(campo: String): Double? = getDouble(campo)

/**
 * `splits_km` é `List<Long>` no schema, mas o SDK devolve os números de uma lista
 * como `Number` — um valor que tenha ido ao banco como inteiro volta `Long`, e um
 * que tenha ido como decimal volta `Double`. Converter elemento a elemento é o que
 * impede um `ClassCastException` na primeira corrida com split derivado (`F2-T03`).
 *
 * **Lista vazia é o caso normal**, não a exceção: corrida manual nunca tem splits.
 */
internal fun DocumentSnapshot.listaDeSegundos(campo: String): List<Long> =
    (get(campo) as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toLong() }

// ---------------------------------------------------------------------------
// Listener → Flow
// ---------------------------------------------------------------------------

/*
 * A ficha de `F1-T05` pede os repositórios expostos como `Flow`, consumidos por
 * `StateFlow` nos ViewModels. `callbackFlow` é o que dá isso: o listener nasce na
 * primeira coleta, e `awaitClose` o remove quando o escopo do ViewModel morre —
 * listener vazado é leitura faturada rodando para uma tela que já saiu.
 *
 * **Erro fecha o `Flow` com a exceção**, em vez de virar emissão vazia. Uma lista
 * vazia é um estado legítimo de tela (semana 1 sem corrida) e não pode ser o
 * disfarce de um `PERMISSION_DENIED`.
 *
 * **A persistência offline fica ligada** (padrão do SDK, docs/05 §2.6): a primeira
 * emissão vem do cache, sem rede, e o servidor chega depois como nova emissão.
 */

internal fun DocumentReference.observarDocumento(): Flow<DocumentSnapshot?> = callbackFlow {
    val registro = addSnapshotListener { documento, erro ->
        when {
            erro != null -> close(erro)
            documento == null || !documento.exists() -> trySend(null)
            else -> trySend(documento)
        }
    }
    awaitClose { registro.remove() }
}

internal fun Query.observarColecao(): Flow<QuerySnapshot> = callbackFlow {
    val registro = addSnapshotListener { consulta, erro ->
        when {
            erro != null -> close(erro)
            consulta != null -> trySend(consulta)
        }
    }
    awaitClose { registro.remove() }
}
