package de.parip69.homesprachsteuerung

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Application-owned: a finishing capture service must not destroy an initializing TTS engine. */
class FeedbackSpeaker(context: Context) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0L
    private var timeout: Runnable? = null

    fun stop() {
        generation++
        timeout?.let(handler::removeCallbacks)
        timeout = null
        val old = engine
        engine = null
        runCatching { old?.stop() }
        runCatching { old?.shutdown() }
    }

    fun speak(text: String, unavailable: () -> Unit) {
        stop()
        val ticket = generation
        fun fail() {
            if (ticket != generation) return
            stop()
            unavailable()
        }
        try {
            engine = TextToSpeech(context) { status ->
                // Some implementations may invoke initialization before the constructor returns.
                handler.post {
                    if (ticket != generation) return@post
                    val tts = engine ?: return@post
                    if (status != TextToSpeech.SUCCESS) { fail(); return@post }
                    try {
                        if (tts.setLanguage(Locale.GERMAN) < TextToSpeech.LANG_AVAILABLE) {
                            fail(); return@post
                        }
                        tts.setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) {
                                handler.post { if (ticket == generation) stop() }
                            }
                            @Deprecated("Required callback for older engines")
                            override fun onError(utteranceId: String?) { handler.post { fail() } }
                            override fun onError(utteranceId: String?, errorCode: Int) { handler.post { fail() } }
                        })
                        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-result-$ticket") == TextToSpeech.ERROR) fail()
                    } catch (e: Exception) { fail() }
                }
            }
            // Allow the echoed command to finish, while still bounding a stuck engine.
            val deadlineMs = (text.length * 100L + 10_000L).coerceIn(15_000L, 120_000L)
            timeout = Runnable { fail() }.also { handler.postDelayed(it, deadlineMs) }
        } catch (e: Exception) { fail() }
    }
}
