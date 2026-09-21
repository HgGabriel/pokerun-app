package com.hggabriel.pokerun.dados.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.hggabriel.pokerun.R
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Entrar e sair (`F1-T06`, docs/03 §3.1).
 *
 * São **dois SDKs numa operação só**, e a ordem importa: o Credential Manager abre
 * a folha de contas do Google e devolve um `idToken`; o Firebase Auth troca esse
 * token por uma sessão. Nenhum dos dois sozinho autentica.
 *
 * **Nada de exceção do Credential Manager sai daqui.** Cancelar a folha de contas é
 * o gesto mais comum da tela e não é falha — vira [ResultadoDeEntrada.Cancelada], e
 * a tela volta ao estado ocioso sem mensagem de erro. Deixar a exceção subir faria
 * cada `ViewModel` importar os tipos do SDK só para saber que o usuário mudou de
 * ideia.
 *
 * **O `Context` entra por parâmetro e não é guardado.** O Credential Manager precisa
 * do contexto da Activity para desenhar a folha, e um repositório que segurasse essa
 * referência vazaria a Activity a cada rotação.
 */
class AutenticacaoRepositorio(private val auth: FirebaseAuth) {

    /**
     * O `CredentialManager`, criado **uma vez por `Context`** e reusado (`F1-T22`).
     *
     * Antes de 20/09 ele era construído a cada toque no botão, dentro da coroutine. O
     * gerenciador carrega estado de sessão: instância nova a cada tentativa é o que
     * faz o pedido de desenhar a folha chegar para uma sessão que o app já descartou
     * — o log do aparelho mostrou `ui invocation is needed` sem folha nenhuma na tela.
     *
     * **A chave é o `Context` e não um campo simples** porque a Activity morre e
     * renasce na rotação; guardar a primeira vazaria a Activity antiga, que é
     * exatamente o que o KDoc da classe diz para não fazer.
     */
    private var ultimoContexto: Context? = null
    private var ultimoGerenciador: CredentialManager? = null

    private fun gerenciador(contexto: Context): CredentialManager {
        val vigente = ultimoGerenciador
        if (vigente != null && ultimoContexto === contexto) return vigente

        return CredentialManager.create(contexto).also {
            ultimoContexto = contexto
            ultimoGerenciador = it
        }
    }

    /**
     * O `uid` da sessão vigente, ou nulo se não há ninguém autenticado.
     *
     * Síncrono e sem rede: a sessão do Firebase é persistida no aparelho e
     * sobrevive a reinício. É por aqui que `F1-T07` decide se o app abre na
     * `LoginScreen` ou já na casca de navegação, e é por isso que quem volta ao app
     * não vê a tela de entrada de novo.
     */
    val uidAtual: String? get() = auth.currentUser?.uid

    /**
     * Abre a folha de contas do Google e autentica no Firebase.
     *
     * Precisa de rede — é uma troca de token com o servidor do Firebase — e precisa
     * do SHA-1 do keystore em uso cadastrado no projeto (`F0-T05b`). Sem o SHA-1, o
     * Credential Manager falha antes mesmo de desenhar a folha, e a mensagem que
     * volta não diz isso.
     */
    suspend fun entrarComGoogle(contexto: Context): ResultadoDeEntrada = try {
        // **O prazo cobre a folha de contas, e só ela** (`F1-T22`). Em 20/09, num
        // aparelho real, o sistema pediu para desenhar a folha, ela nunca apareceu, e
        // a espera ficou pendurada para sempre — sem prazo o único fim possível era a
        // pessoa matar o app. A troca de token com o Firebase fica **fora**: ela tem
        // prazo próprio e já existe sessão em jogo quando ela roda.
        val token = withTimeout(PRAZO_DA_ENTRADA_MS) { tokenDoGoogle(contexto) }
        val credencial = GoogleAuthProvider.getCredential(token, null)
        val uid = auth.signInWithCredential(credencial).await().user?.uid

        if (uid == null) {
            ResultadoDeEntrada.Falhou(IllegalStateException("sessão criada sem uid"))
        } else {
            ResultadoDeEntrada.Autenticado(uid)
        }
    } catch (demorou: TimeoutCancellationException) {
        // **Prazo estourado não é ausência de conta**, e confundir os dois foi o
        // defeito de 20/09: a tela dizia "não há conta Google neste aparelho" a um
        // aparelho com nove. `TimeoutCancellationException` desce de
        // `CancellationException`, então este `catch` vem **antes** do genérico —
        // invertida a ordem, a demora viraria `Falhou` e a mensagem perderia a saída.
        ResultadoDeEntrada.Demorou
    } catch (cancelou: GetCredentialCancellationException) {
        // O usuário fechou a folha. Não é erro, e a tela não mostra mensagem.
        ResultadoDeEntrada.Cancelada
    } catch (semConta: NoCredentialException) {
        ResultadoDeEntrada.SemContaNoAparelho
    } catch (erro: Exception) {
        ResultadoDeEntrada.Falhou(erro)
    }

    /** Encerra a sessão local. A `SettingsScreen` (`F1-T17`) é quem chama. */
    fun sair() {
        auth.signOut()
    }

    /**
     * O `idToken` do Google.
     *
     * **`serverClientId` é o cliente OAuth *web*, não o Android** — o de
     * `client_type: 3` do `google-services.json`, que o plugin do Google Services
     * publica como `default_web_client_id`. É o erro clássico deste fluxo: com o ID
     * do cliente Android, que é o que parece certo, a folha abre e a troca de token
     * falha depois, com uma mensagem que não aponta para a causa.
     *
     * `setFilterByAuthorizedAccounts(false)` porque ninguém do grupo autorizou o app
     * ainda: filtrando por contas já autorizadas, a primeira entrada de todo mundo
     * cairia em [NoCredentialException] num aparelho que tem conta Google.
     */
    private suspend fun tokenDoGoogle(contexto: Context): String {
        val opcao = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(contexto.getString(R.string.default_web_client_id))
            .build()

        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcao).build()
        val credencial = gerenciador(contexto)
            .getCredential(contexto, pedido)
            .credential

        val ehDoGoogle = credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        check(ehDoGoogle) { "credencial de tipo inesperado: ${credencial.type}" }

        return GoogleIdTokenCredential.createFrom(credencial.data).idToken
    }
}

/**
 * O que pode sair de uma tentativa de entrar.
 *
 * Tipo fechado em vez de `Result<String>` porque **cancelar não é falhar**, e um
 * `Result` obrigaria a tela a distinguir isso inspecionando o tipo da exceção —
 * exatamente o acoplamento ao SDK que este pacote existe para conter.
 */
sealed interface ResultadoDeEntrada {

    data class Autenticado(val uid: String) : ResultadoDeEntrada

    /** O usuário fechou a folha de contas. Estado ocioso, sem mensagem. */
    data object Cancelada : ResultadoDeEntrada

    /** Nenhuma conta Google no aparelho. A saída é adicionar uma nos Ajustes do sistema. */
    data object SemContaNoAparelho : ResultadoDeEntrada

    /**
     * A folha de contas não respondeu dentro de [PRAZO_DA_ENTRADA_MS] (`F1-T22`).
     *
     * **Separado de [SemContaNoAparelho] porque a saída é outra:** aqui a conta
     * existe e o caminho é tentar de novo, e foi confundir os dois que fez a tela
     * mandar a pessoa adicionar a décima conta num aparelho com nove.
     */
    data object Demorou : ResultadoDeEntrada

    /** Rede, configuração ou qualquer outra coisa. A tela oferece repetir (docs/02 §8, item 7). */
    data class Falhou(val erro: Throwable) : ResultadoDeEntrada
}

/**
 * O prazo da folha de contas, em milissegundos (`F1-T22`).
 *
 * **15 s é escolha medida, não redonda.** O aparelho de 20/09 mostrou o sistema
 * cancelando sozinho aos **10 s**: um prazo abaixo disso cortaria o Credential Manager
 * antes de ele ter chance de responder, e transformaria assinatura ruim em
 * impossibilidade. Acima de 30 s volta a ser a tela travada de que a pessoa reclamou.
 */
const val PRAZO_DA_ENTRADA_MS = 15_000L

/** O que uma espera pela folha de contas pode dar (`F1-T22`). */
enum class DesfechoDaEspera { Respondeu, Demorou, Falhou }

/**
 * A leitura do desfecho, isolada do SDK para caber em teste de unidade.
 *
 * **O prazo vence o token retardatário**, e essa ordem é o achado de 20/09: o log
 * mostrou o cancelamento aos 10 s e a credencial chegando 0,5 s **depois**. Quem chega
 * fora do prazo não entra — a tela já seguiu, e aceitar o retardatário autenticaria
 * alguém que já tinha desistido.
 */
fun desfechoDaEspera(token: String?, estourouOPrazo: Boolean): DesfechoDaEspera = when {
    estourouOPrazo -> DesfechoDaEspera.Demorou
    token != null -> DesfechoDaEspera.Respondeu
    else -> DesfechoDaEspera.Falhou
}
