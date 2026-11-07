package com.scripturedaily.features.story

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Reads story scenes aloud with the device's text-to-speech engine, one scene at a time. */
class StoryNarrator(
    context: Context,
    private val languageCode: String,
    private val onSceneStart: (Int) -> Unit,
    private val onFinished: () -> Unit,
    private val onProblem: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var rate = 0.9f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (status != TextToSpeech.SUCCESS) { onProblem("Read-aloud isn't available on this device."); return@post }
                val engine = tts ?: return@post
                val result = engine.setLanguage(localeFor(languageCode))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onProblem("The ${displayName(languageCode)} voice isn't installed on this device. Install it in Settings → Text-to-speech.")
                    return@post
                }
                engine.setSpeechRate(rate)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { sceneOf(utteranceId)?.let { i -> main.post { onSceneStart(i) } } }
                    override fun onDone(utteranceId: String?) { if (utteranceId?.startsWith(LAST_PREFIX) == true) main.post { onFinished() } }
                    @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { main.post { onProblem("Couldn't read this story aloud.") } }
                })
                ready = true
                pending?.invoke(); pending = null
            }
        }
    }

    private fun sceneOf(id: String?): Int? = id?.substringAfterLast('_')?.toIntOrNull()

    fun setRate(r: Float) { rate = r; tts?.setSpeechRate(r) }

    /** Speaks [texts] starting at [from]; calls [onSceneStart] as each scene begins. */
    fun play(texts: List<String>, from: Int) {
        val start = {
            val engine = tts
            if (engine != null) {
                engine.stop()
                for (i in from until texts.size) {
                    val id = (if (i == texts.lastIndex) LAST_PREFIX else "scene_") + i
                    engine.speak(texts[i], TextToSpeech.QUEUE_ADD, null, id)
                    engine.playSilentUtterance(450, TextToSpeech.QUEUE_ADD, "gap_$i")
                }
            }
        }
        if (ready) start() else pending = start
    }

    fun stop() { pending = null; tts?.stop() }

    fun shutdown() { pending = null; tts?.stop(); tts?.shutdown(); tts = null }

    companion object {
        private const val LAST_PREFIX = "last_"
        fun localeFor(code: String): Locale = when (code) {
            "hi" -> Locale("hi", "IN"); "bn" -> Locale("bn", "IN"); "es" -> Locale("es", "ES"); "ar" -> Locale("ar")
            "ur" -> Locale("ur", "PK"); "th" -> Locale("th", "TH"); else -> Locale.ENGLISH
        }
        fun displayName(code: String) = localeFor(code).getDisplayLanguage(Locale.ENGLISH).ifBlank { code }
    }
}
