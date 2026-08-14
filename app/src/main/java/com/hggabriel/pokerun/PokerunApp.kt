package com.hggabriel.pokerun

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.hggabriel.pokerun.dados.auth.AutenticacaoRepositorio
import com.hggabriel.pokerun.dados.firestore.ConviteRepositorio
import com.hggabriel.pokerun.dados.firestore.CorridaRepositorio
import com.hggabriel.pokerun.dados.firestore.PlanoRepositorio
import com.hggabriel.pokerun.dados.firestore.UsuarioRepositorio
import com.hggabriel.pokerun.dados.healthconnect.SaudeRepositorio
import com.hggabriel.pokerun.dados.rede.ConectividadeRepositorio

/**
 * A `Application` do PokéRun. Existe para hospedar o [AppContainer] — o grafo de
 * dependências vive enquanto o processo viver, e não pode nascer de novo a cada
 * rotação da [MainActivity].
 *
 * `by lazy` e não inicialização direta de campo: uma propriedade de `Application`
 * é construída **antes** de `attachBaseContext`, então qualquer dependência que
 * precise de `Context` — o cliente do Health Connect, em `F2-T01` — quebraria ali,
 * com uma pilha que não aponta para a causa.
 */
class PokerunApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * O grafo de dependências, montado à mão.
 *
 * **Sem Hilt, e isso é decisão de projeto** (ficha de `F0-T03`): KSP cobra tempo de
 * build em toda alteração, e um app de 20 telas com uma instância de cada coisa não
 * tem grafo que pague o custo. O que a DI manual pede em troca é disciplina — nada
 * de `Firebase.firestore` no meio de uma tela.
 *
 * **Os repositórios de `F1-T05` entraram**, construídos sobre [firestore]. Uma tela
 * usa [planoRepositorio], [usuarioRepositorio], [corridaRepositorio] ou
 * [conviteRepositorio]; [firestore] continua exposto porque `F2-T08` precisa dos
 * agregados e eles ainda não têm repositório. Este é o único lugar do app onde o SDK
 * do Firebase é instanciado.
 *
 * **`by lazy` em tudo:** o Firestore levanta a engine de persistência local na
 * primeira chamada. Numa abertura que termina na `LoginScreen` (`F1-T06`), isso é
 * trabalho de cold start gasto para nada — e um repositório construído junto
 * arrastaria a engine para dentro dessa abertura.
 *
 * **[contexto] entrou com `F1-T08`**, e é o da aplicação: [saudeRepositorio] consulta
 * o `PackageManager` e abre o cliente do Health Connect, as duas coisas que precisam
 * de `Context`. É o caso que o `by lazy` da [PokerunApp] já antecipava — uma
 * propriedade de `Application` nasce antes de `attachBaseContext`, e construir isto
 * ali quebraria com uma pilha que não aponta para a causa.
 */
class AppContainer(private val contexto: Context) {

    val auth: FirebaseAuth by lazy { Firebase.auth }

    val firestore: FirebaseFirestore by lazy { Firebase.firestore }

    val autenticacaoRepositorio: AutenticacaoRepositorio by lazy { AutenticacaoRepositorio(auth) }

    val planoRepositorio: PlanoRepositorio by lazy { PlanoRepositorio(firestore) }

    val usuarioRepositorio: UsuarioRepositorio by lazy { UsuarioRepositorio(firestore) }

    val corridaRepositorio: CorridaRepositorio by lazy { CorridaRepositorio(firestore) }

    /**
     * Entrou com `F1-T14`. A coleção `invites` é raiz e não subcoleção do plano, então
     * ela tem repositório próprio: quem resolve um código ainda **não** é membro de
     * plano nenhum, e as duas rules não se parecem.
     */
    val conviteRepositorio: ConviteRepositorio by lazy { ConviteRepositorio(firestore) }

    val saudeRepositorio: SaudeRepositorio by lazy { SaudeRepositorio(contexto) }

    /**
     * Entrou com `F1-T10`. Só a `CreatePlanScreen` pergunta: a criação de plano é a
     * única escrita do app que **não** resolve offline, porque a reserva do código de
     * convite é transacional (RN-29).
     */
    val conectividadeRepositorio: ConectividadeRepositorio by lazy {
        ConectividadeRepositorio(contexto)
    }
}

/**
 * O caminho de acesso ao [AppContainer] dentro da composição, provido uma vez pela
 * [MainActivity].
 *
 * **Por que um `CompositionLocal` e não um parâmetro:** o container atravessaria a
 * casca de navegação inteira e cada nível intermediário só para chegar às telas que
 * o usam, e a barra inferior não tem o que fazer com ele. O app já resolve isso
 * assim uma vez, em `LocalCoresPokerun` (`docs/02 §2.6`) — o padrão não é novo aqui.
 *
 * **Como as telas vão usá-lo em `F1`:** o `ViewModel` recebe os repositórios pelo
 * construtor, e é a fábrica no ponto de chamada que lê daqui. O `ViewModel` nunca
 * enxerga o `CompositionLocal`, porque ele sobrevive à composição que o proveu:
 *
 * ```
 * val container = LocalAppContainer.current
 * val vm: HomeViewModel = viewModel { HomeViewModel(container.planoRepositorio) }
 * ```
 *
 * `staticCompositionLocalOf` porque o valor não muda depois de provido: trocá-lo
 * recomporia o app inteiro, e é justamente por não haver troca que a leitura sai de
 * graça.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error(
        "Nenhum AppContainer provido. Ele vem da PokerunApp e é provido na MainActivity — " +
            "um @Preview que chegue aqui precisa prover o seu.",
    )
}
