package de.parip69.homesprachsteuerung

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** One explicit app launch = one bounded speech turn and at most one network send. */
class CaptureService : Service(), RecognitionListener {
    private val repository get() = (application as BridgeApplication).repository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var working = false
    private var backgroundRequest = false
    private var feedbackEnabled = true
    private var speechFeedbackEnabled = false
    private var speakCommandEnabled = false
    private var completionFeedback: DeliveryFeedback? = null
    private var recognizedCommand: String? = null
    private var completionMessage: String? = null
    private lateinit var config: AppConfig
    private lateinit var speechTurn: SpeechTurn
    private var segmentGeneration = 0L
    private var readyAnnounced = false
    private var finishRequested = false
    private var lastClockText = ""
    private val restart = Runnable { if (listening && !finishRequested) startSegment() }
    private val finalResultTimeout = Runnable {
        if (listening) finish("Abschluss der Spracherkennung hat zu lange gedauert · nicht gesendet.")
    }
    private val sessionTick = object : Runnable {
        override fun run() {
            handler.removeCallbacks(this)
            if (!listening || finishRequested) return
            val now = SystemClock.elapsedRealtime()
            when (speechTurn.decision(now)) {
                SpeechTurn.Decision.SEND -> { sendCaptured(); return }
                SpeechTurn.Decision.NO_SPEECH -> { finish("Keine Sprache innerhalb der eingestellten Wartezeit erkannt."); return }
                SpeechTurn.Decision.LIMIT -> { finish("Maximale Aufnahmezeit erreicht · unvollständigen Befehl nicht gesendet."); return }
                SpeechTurn.Decision.WAIT -> Unit
            }
            val pause = speechTurn.remainingPause(now)
            val initial = speechTurn.remainingInitial(now)
            val text = when {
                pause != null -> "${speechTurn.text}\nNoch ${(pause + 999) / 1000} s für eine Fortsetzung · sonst wird gesendet"
                initial != null -> "Bitte jetzt sprechen · noch ${(initial + 999) / 1000} s zum Beginnen"
                else -> null
            }
            if (text != null && text != lastClockText) {
                lastClockText = text
                repository.update(Phase.LISTENING, text)
                getSystemService(NotificationManager::class.java).notify(1, notification(text, true))
            }
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Sprachaufnahme", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == CANCEL) {
            if (listening) finish("Aufnahme abgebrochen.", DeliveryKind.CANCELLED)
            else if (!working) stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == SEND_NOW) {
            if (listening) requestSendNow() else if (!working) stopSelf()
            return START_NOT_STICKY
        }
        if (working) return START_NOT_STICKY
        working = true
        backgroundRequest = intent?.getBooleanExtra(BACKGROUND_REQUEST, false) == true
        val feedbackSettings = SettingsStore(this).load()
        feedbackEnabled = feedbackSettings.voiceFeedback
        speechFeedbackEnabled = feedbackSettings.speechFeedback
        speakCommandEnabled = feedbackSettings.speakCommand
        (application as BridgeApplication).feedbackSpeaker.stop()
        try {
            ServiceCompat.startForeground(this, 1, notification("Spracherkennung startet …", true),
                if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
            config = SettingsStore(this).load()
            config.validate()?.let { finish("Einstellungen prüfen: $it"); return START_NOT_STICKY }
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                finish("Kein Spracherkennungsdienst installiert oder aktiviert.")
                return START_NOT_STICKY
            }
            speechTurn = SpeechTurn(SystemClock.elapsedRealtime(), config.initialWaitSeconds * 1000L,
                config.speechPauseSeconds * 1000L, config.maximumSpeechSeconds * 1000L)
            listening = true
            repository.update(Phase.STARTING, "Spracherkennung startet … bitte kurz warten")
            startSegment()
            handler.post(sessionTick)
        } catch (e: Exception) { finish("Spracherkennung konnte nicht gestartet werden. Berechtigungen prüfen.") }
        return START_NOT_STICKY
    }

    private fun startSegment() {
        if (!listening || finishRequested) return
        releaseRecognizer()
        val generation = segmentGeneration
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(guardedListener(generation))
                it.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                })
            }
        } catch (e: Exception) { finish("Spracherkennung konnte nicht fortgesetzt werden · nicht gesendet.") }
    }

    private fun guardedListener(generation: Long): RecognitionListener {
        fun deliver(action: () -> Unit) { if (listening && generation == segmentGeneration) action() }
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = deliver { this@CaptureService.onReadyForSpeech(params) }
            override fun onBeginningOfSpeech() = deliver { this@CaptureService.onBeginningOfSpeech() }
            override fun onEndOfSpeech() = deliver { this@CaptureService.onEndOfSpeech() }
            override fun onResults(results: Bundle?) = deliver { this@CaptureService.onResults(results) }
            override fun onError(error: Int) = deliver { this@CaptureService.onError(error) }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    override fun onResults(results: Bundle?) {
        if (!listening) return
        releaseRecognizer()
        handler.removeCallbacks(finalResultTimeout)
        val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (raw.isNullOrBlank()) {
            if (speechTurn.speechInProgress) finish("Fortsetzung nicht verstanden · nicht gesendet.")
            else retryAfterSilence()
            return
        }
        speechTurn.result(raw, SystemClock.elapsedRealtime())
        recognizedCommand = speechTurn.text
        if (finishRequested) { sendCaptured(); return }
        // Native end-pointing may end an utterance early. Keep one bounded command open.
        handler.removeCallbacks(restart)
        handler.postDelayed(restart, 350)
        sessionTick.run()
    }

    private fun retryAfterSilence() {
        if (!listening) return
        if (finishRequested) {
            if (speechTurn.hasText && !speechTurn.speechInProgress) sendCaptured()
            else finish("Kein vollständiger Sprachtext erkannt · nicht gesendet.")
            return
        }
        when (speechTurn.decision(SystemClock.elapsedRealtime())) {
            SpeechTurn.Decision.SEND -> sendCaptured()
            SpeechTurn.Decision.NO_SPEECH -> finish("Keine Sprache innerhalb der eingestellten Wartezeit erkannt.")
            SpeechTurn.Decision.LIMIT -> finish("Maximale Aufnahmezeit erreicht · nicht gesendet.")
            SpeechTurn.Decision.WAIT -> {
                handler.removeCallbacks(restart)
                handler.postDelayed(restart, 750)
            }
        }
    }

    private fun requestSendNow() {
        if (!listening || finishRequested) return
        if (speechTurn.hasText && !speechTurn.speechInProgress) { sendCaptured(); return }
        finishRequested = true
        handler.removeCallbacks(restart)
        handler.removeCallbacks(sessionTick)
        repository.update(Phase.LISTENING, "Spracherkennung wird abgeschlossen …")
        try {
            recognizer?.stopListening() ?: run { finish("Kein Sprachtext zum Senden vorhanden."); return }
            handler.postDelayed(finalResultTimeout, 8_000)
        } catch (e: Exception) { finish("Spracherkennung konnte nicht abgeschlossen werden · nicht gesendet.") }
    }

    private fun sendCaptured() {
        if (!listening) return
        val raw = speechTurn.takeSubmission() ?: run { finish("Kein Sprachtext zum Senden vorhanden."); return }
        listening = false
        handler.removeCallbacksAndMessages(null)
        releaseRecognizer()
        recognizedCommand = raw
        repository.update(Phase.SENDING, if (config.protocol == Protocol.TEST) "Wird im Verlauf gespeichert …" else "Wird gespeichert und gesendet …")
        getSystemService(NotificationManager::class.java).notify(1, notification("Sprachtext wird verarbeitet …", false))
        scope.launch {
            val id = try { repository.add(raw, config.protocol.name) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                finish("Speicherfehler · Text wurde nicht gesendet.")
                return@launch
            }
            var outcome = DeliveryKind.FAILED
            val result = try {
                withContext(Dispatchers.IO) { NetworkSender().send(config, raw) }.also {
                    outcome = if (config.protocol == Protocol.TEST) DeliveryKind.LOCAL_ONLY else DeliveryKind.SENT
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                outcome = DeliveryFeedback.failureKind(e)
                when (e) {
                    is HttpStatusException -> "Fehler · Fibaro ${e.connection.orEmpty()} HTTP ${e.status}"
                    is UnexpectedFibaroResponseException -> e.message.orEmpty()
                    is MqttException -> "Fehler · MQTT-Code ${e.reasonCode}; Zustellung ggf. unklar"
                    is SocketTimeoutException -> "Zeitüberschreitung · Zustellung unklar, Ziel prüfen"
                    is UnknownHostException -> "Fehler · Servername nicht gefunden"
                    is SSLException -> "Fehler · TLS-Zertifikat oder TLS-Verbindung"
                    else -> "Verbindungsfehler · Zustellung unklar, Ziel prüfen"
                }
            }
            try { repository.finish(id, result) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                finish("$result · Verlaufsstatus konnte nicht gespeichert werden.", outcome)
                return@launch
            }
            finish(result, outcome)
        }
    }

    override fun onError(error: Int) {
        if (!listening) return
        releaseRecognizer()
        if (error in setOf(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH) &&
            !speechTurn.speechInProgress) {
            retryAfterSilence()
            return
        }
        val reason = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Mikrofonfehler"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon-Berechtigung fehlt"
            SpeechRecognizer.ERROR_NETWORK -> "Netzwerkfehler beim Sprachdienst"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sprachdienst antwortet nicht"
            SpeechRecognizer.ERROR_NO_MATCH -> "Sprache nicht verstanden"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Sprachdienst ist beschäftigt"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Keine Sprache erkannt"
            SpeechRecognizer.ERROR_CLIENT -> "Spracherkennung abgebrochen"
            SpeechRecognizer.ERROR_SERVER -> "Fehler beim Sprachdienst"
            else -> "Sprachdienst-Fehler"
        }
        finish("$reason (Code $error)")
    }

    private fun notification(text: String, canCancel: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_HISTORY)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_mic).setContentTitle(getString(R.string.app_name))
            .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (canCancel) builder.addAction(0, "Jetzt senden", PendingIntent.getService(this, 2,
            Intent(this, CaptureService::class.java).setAction(SEND_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        if (canCancel) builder.addAction(0, "Aufnahme abbrechen", PendingIntent.getService(this, 1,
            Intent(this, CaptureService::class.java).setAction(CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        return builder.build()
    }

    private fun releaseRecognizer() {
        segmentGeneration++ // Late results/errors from a destroyed native session are ignored.
        val old = recognizer
        recognizer = null
        runCatching { old?.destroy() }
    }

    private fun finish(message: String, kind: DeliveryKind = DeliveryKind.FAILED) {
        val unsent = if (listening && ::speechTurn.isInitialized && speechTurn.hasText) speechTurn.text else null
        listening = false
        handler.removeCallbacksAndMessages(null)
        releaseRecognizer()
        if (unsent != null) {
            recognizedCommand = unsent
            repository.update(Phase.SENDING, "Erkannten Text ungesendet im Verlauf speichern …")
            scope.launch {
                var finalMessage = message
                try {
                    val id = repository.add(unsent, config.protocol.name)
                    repository.finish(id, message)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    finalMessage += " · Verlauf konnte nicht gespeichert werden."
                }
                stopCapture(finalMessage, kind)
            }
        } else stopCapture(message, kind)
    }

    private fun stopCapture(message: String, kind: DeliveryKind) {
        completionMessage = message
        completionFeedback = DeliveryFeedback(kind, message, recognizedCommand)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        listening = false
        handler.removeCallbacksAndMessages(null)
        releaseRecognizer()
        scope.cancel()
        val completed = completionMessage
        val feedback = if (completed != null) completionFeedback else
            if (repository.state.value.phase != Phase.IDLE)
                DeliveryFeedback(DeliveryKind.UNCERTAIN, "Vorgang unterbrochen · Verlauf und Ziel prüfen.", recognizedCommand) else null
        if (feedback != null) {
            // Service owns the completion feedback for both background and manual capture.
            // Do not queue another old toast for the next overview launch.
            repository.completeCapture(feedback.overview, toast = false)
            if (feedbackEnabled) Toast.makeText(this, feedback.popup, Toast.LENGTH_LONG).show()
            if (speechFeedbackEnabled) {
                val appContext = applicationContext
                val spokenText = feedback.speech(speakCommandEnabled)
                (application as BridgeApplication).feedbackSpeaker.speak(spokenText) {
                    Toast.makeText(appContext,
                        "Sprachausgabe nicht verfügbar. ${spokenText}", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onDestroy()
    }

    override fun onEndOfSpeech() {
        if (listening) {
            repository.update(Phase.LISTENING, "Sprache wird erkannt …")
            getSystemService(NotificationManager::class.java).notify(1, notification("Sprache wird erkannt …", true))
        }
    }
    override fun onReadyForSpeech(params: Bundle?) {
        if (listening) {
            speechTurn.ready(SystemClock.elapsedRealtime())
            if (!readyAnnounced) {
                readyAnnounced = true
                repository.update(Phase.LISTENING, "Bitte jetzt sprechen …")
                if (backgroundRequest && feedbackEnabled)
                    Toast.makeText(this, "Bitte jetzt sprechen", Toast.LENGTH_SHORT).show()
            }
            sessionTick.run()
        }
    }
    override fun onBeginningOfSpeech() {
        if (listening) {
            speechTurn.began()
            lastClockText = ""
            repository.update(Phase.LISTENING, "Sprache wird aufgenommen …")
            getSystemService(NotificationManager::class.java).notify(1, notification("Sprache wird aufgenommen …", true))
        }
    }
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "speech_capture"
        const val BACKGROUND_REQUEST = "backgroundRequest"
        const val SEND_NOW = "de.parip69.homesprachsteuerung.SEND_NOW"
        const val CANCEL = "de.parip69.homesprachsteuerung.CANCEL"
    }
}
