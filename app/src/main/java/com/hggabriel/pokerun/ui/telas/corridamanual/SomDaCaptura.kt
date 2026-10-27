package com.hggabriel.pokerun.ui.telas.corridamanual

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RawRes

/** O que o clique da Pokébola faz além da animação (estudo de Progresso e Perfil, Revisão 3). */
enum class EfeitoDaCaptura { VIBRAR, TOCAR_SOM }

/**
 * Ainda não há arquivo de som no APK: o humano não escolheu o clique (pergunta aberta da
 * Revisão 3). Por enquanto a chave liga só a vibração, e o ponto do `SoundPool` em
 * [SomDaCaptura] fica pronto atrás da mesma chave.
 */
internal const val SOM_DA_CAPTURA_DISPONIVEL = false

/**
 * A regra da chave "Som e vibração na captura": desligada, a captura não toca nem vibra.
 * Ligada, vibra, e toca o som quando houver um.
 */
fun efeitosDaCaptura(ligada: Boolean, somDisponivel: Boolean = SOM_DA_CAPTURA_DISPONIVEL): Set<EfeitoDaCaptura> =
    when {
        !ligada -> emptySet()
        somDisponivel -> setOf(EfeitoDaCaptura.VIBRAR, EfeitoDaCaptura.TOCAR_SOM)
        else -> setOf(EfeitoDaCaptura.VIBRAR)
    }

/**
 * Quem executa os efeitos. A vibração é o retorno háptico da própria `View`, que não pede
 * permissão nova. O som toca pelo `SoundPool`, e só com a campainha no modo normal: no
 * silencioso e no vibrar, ele não toca.
 *
 * @param recurso o clique da Pokébola em `res/raw`. Nulo enquanto o som não foi escolhido.
 */
class SomDaCaptura(private val contexto: Context, @param:RawRes private val recurso: Int? = null) {

    private var tocador: SoundPool? = null
    private var som: Int = 0

    fun executar(efeitos: Set<EfeitoDaCaptura>, vista: View) {
        if (EfeitoDaCaptura.VIBRAR in efeitos) {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            vista.performHapticFeedback(tipo)
        }
        if (EfeitoDaCaptura.TOCAR_SOM in efeitos) tocar()
    }

    private fun tocar() {
        val recurso = recurso ?: return
        val audio = contexto.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val pool = tocador ?: SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
            .also { novo ->
                tocador = novo
                som = novo.load(contexto, recurso, 1)
                novo.setOnLoadCompleteListener { p, id, status -> if (status == 0) p.play(id, 1f, 1f, 1, 0, 1f) }
                return
            }
        pool.play(som, 1f, 1f, 1, 0, 1f)
    }

    fun liberar() {
        tocador?.release()
        tocador = null
    }
}
