package com.hggabriel.pokerun.dados.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.hggabriel.pokerun.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

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
     *
     * **Não há prazo** (`F1-T25`, decisão nº 84). `F1-T22` pôs 15 s e eles cortaram a
     * pessoa escolhendo a conta; `F1-T24` subiu para 120 s e então o Google que nunca
     * responde custava dois minutos. O SDK não diz quando a folha apareceu, e sem esse
     * sinal nenhum número separa as duas coisas. A saída do caso travado passou a ser
     * da pessoa: o `LoginViewModel` devolve o botão aos 15 s e, no toque, cancela esta
     * chamada.
     *
     * @param aoEscolherConta chamado quando o Google devolve o token, **antes** da troca
     *   com o Firebase. É o que tira o `Tentar de novo` da tela nesse trecho: repetir
     *   ali cancelaria uma entrada que já está dando certo.
     */
    suspend fun entrarComGoogle(
        contexto: Context,
        aoEscolherConta: () -> Unit = {},
    ): ResultadoDeEntrada = try {
        val token = tokenDoGoogle(contexto)
        aoEscolherConta()
        val credencial = GoogleAuthProvider.getCredential(token, null)
        val uid = auth.signInWithCredential(credencial).await().user?.uid

        if (uid == null) {
            ResultadoDeEntrada.Falhou(IllegalStateException("sessão criada sem uid"))
        } else {
            ResultadoDeEntrada.Autenticado(uid)
        }
    } catch (cancelamento: CancellationException) {
        // **O cancelamento da corrotina não é falha de entrada, e tem de subir.** É
        // assim que o `Tentar de novo` abandona o pedido pendurado (`F1-T25`). Engolido
        // pelo `catch` genérico, ele viraria `Falhou`, e a tentativa abandonada
        // escreveria "não deu para entrar" por cima da tentativa nova. Vem antes do
        // genérico porque `CancellationException` é uma `Exception`.
        throw cancelamento
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
     * **`GetSignInWithGoogleOption`, e não `GetGoogleIdOption`** (`F1-T24`). As duas
     * devolvem o mesmo [GoogleIdTokenCredential], e a diferença está em como a folha
     * de contas é aberta:
     *
     * - `GetGoogleIdOption` monta **uma entrada por conta do aparelho** e manda todas
     *   no `Intent` que abre a folha. Em 20/09, num Xiaomi com **treze contas Google**,
     *   esse pacote deu **562 KB** e estourou o limite de transação do Binder
     *   (`TransactionTooLargeException`): o processo que desenha a folha morria antes
     *   de existir, e a entrada era **impossível** — não lenta, impossível. O mesmo APK
     *   entrava no emulador, que tem cinco contas e cabe no limite.
     * - `GetSignInWithGoogleOption` é a opção para **botão de login**, que é o que esta
     *   tela tem. Ela delega a escolha ao fluxo do próprio Google, sem carregar as
     *   contas no `Intent`, e o tamanho para de depender de quantas contas existem.
     *
     * **O filtro por contas autorizadas some junto, e isso é ganho:** ele existia para
     * não barrar a primeira entrada de quem nunca autorizou o app, e esta opção nunca
     * filtra.
     */
    private suspend fun tokenDoGoogle(contexto: Context): String {
        val opcao = GetSignInWithGoogleOption
            .Builder(contexto.getString(R.string.default_web_client_id))
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

    /** Rede, configuração ou qualquer outra coisa. A tela oferece repetir (docs/02 §8, item 7). */
    data class Falhou(val erro: Throwable) : ResultadoDeEntrada
}
