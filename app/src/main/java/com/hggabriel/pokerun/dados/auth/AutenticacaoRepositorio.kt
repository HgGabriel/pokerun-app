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
        val token = tokenDoGoogle(contexto)
        val credencial = GoogleAuthProvider.getCredential(token, null)
        val uid = auth.signInWithCredential(credencial).await().user?.uid

        if (uid == null) {
            ResultadoDeEntrada.Falhou(IllegalStateException("sessão criada sem uid"))
        } else {
            ResultadoDeEntrada.Autenticado(uid)
        }
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
        val credencial = CredentialManager.create(contexto)
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
