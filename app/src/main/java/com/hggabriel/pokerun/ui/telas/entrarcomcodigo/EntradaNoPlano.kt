package com.hggabriel.pokerun.ui.telas.entrarcomcodigo

import com.hggabriel.pokerun.dominio.modelo.Plano
import com.hggabriel.pokerun.dominio.regras.CalendarioDoPlano
import java.time.Instant
import java.time.LocalDate

/**
 * O plano que um código de convite resolveu, pronto para a tela decidir (docs/03 §3.8).
 *
 * **Nenhuma data aqui é `Instant`.** A data da prova já saiu do fuso do plano (RN-28)
 * dentro de [previaDaEntrada]; um `Instant` aqui convidaria o `@Composable` a formatá-lo
 * com o fuso do aparelho, que é o bug que RN-28 existe para impedir. É a mesma escolha
 * de `ItemDePlano`, na lista.
 */
data class PreviaDaEntrada(
    val planoId: String,
    val nome: String,
    val dataDaProva: LocalDate,
    val distanciaAlvoKm: Double,
    /** RN-27, pelos dois caminhos: o botão do dono e o fim da semana da prova. */
    val encerrado: Boolean,
    /** RN-13: já existe plano ativo, então a troca precisa de decisão explícita. */
    val exigeEscolha: Boolean,
    /**
     * O nome do plano ativo, para o diálogo nomeá-lo. **Nulo é nome desconhecido, nunca
     * "não há plano ativo"** — quem responde isso é [exigeEscolha].
     */
    val nomeDoPlanoAtivo: String?,
) {
    /**
     * // RN-27
     *
     * Entrar num plano encerrado seria um beco sem saída oferecido por um botão: ele é
     * somente leitura (RN-07) e não volta a receber corridas. O código continua
     * resolvendo — a prévia aparece e diz por que a porta está fechada, que é mais útil
     * que um "código não encontrado" mentiroso.
     */
    val podeEntrar: Boolean get() = !encerrado
}

/**
 * // RN-13
 *
 * Monta a prévia do plano encontrado e responde as duas perguntas que a tela precisa
 * fazer antes de gravar qualquer coisa: **dá para entrar** (RN-27) e **o que acontece
 * com o plano em que a pessoa já está** (RN-13).
 *
 * **A escolha sai do ID, e não do plano lido.** [nomeDoPlanoAtivo] é enfeite da frase do
 * diálogo e pode faltar — a leitura do outro plano é uma ida a mais ao servidor e falha
 * como qualquer outra. Deixar de exigir a escolha porque o nome não veio trocaria o plano
 * primário em silêncio, que é a única coisa que RN-13 proíbe.
 *
 * **O plano ativo encerrado continua exigindo a escolha.** RN-27 encerra o plano ao fim
 * da semana da prova sem tocar em `users/{uid}.plano_ativo_id`, então o campo segue
 * apontando para ele (decisão nº 34 do `STATUS.md`). Apontá-lo para o plano novo por
 * conta própria seria decidir pelo usuário justamente no caso em que ele tem mais motivo
 * para querer decidir.
 *
 * @param planoAtivoId o `plano_ativo_id` de `users/{uid}`, nulo para quem ainda não tem
 *   nenhum. É o único campo do perfil que muda a resposta.
 * @param agora o relógio, por parâmetro. RN-27 encerra por data, e um `Instant.now()`
 *   escondido aqui tiraria a fronteira do teste — a mesma escolha de `itensDePlano`.
 */
fun previaDaEntrada(
    plano: Plano,
    planoAtivoId: String?,
    nomeDoPlanoAtivo: String?,
    agora: Instant,
): PreviaDaEntrada = PreviaDaEntrada(
    planoId = plano.id,
    nome = plano.nome,
    // RN-28: o fuso é o do plano. Quem recebe o convite de outro país precisa ver a data
    // que o dono marcou, e não a que o próprio fuso desloca em um dia.
    dataDaProva = plano.dataProva.atZone(plano.fuso).toLocalDate(),
    distanciaAlvoKm = plano.distanciaAlvoKm,
    encerrado = CalendarioDoPlano.planoEncerrado(plano, agora),
    exigeEscolha = planoAtivoId != null,
    nomeDoPlanoAtivo = nomeDoPlanoAtivo,
)
