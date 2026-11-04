package de.parip69.homesprachsteuerung.car

import android.content.Context
import de.parip69.homesprachsteuerung.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarCommandState(val busy: Boolean = false, val message: String = "Bereit")

/** Application lifetime: leaving the car screen cannot trigger a resend or lose history finalization. */
class CarCommandDispatcher(context: Context, private val repository: BridgeRepository) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gate = CarSubmissionGate()
    private val mutableState = MutableStateFlow(CarCommandState())
    val state = mutableState.asStateFlow()

    // Called only by an explicit host submit/row click, never by partial text updates or startup.
    fun submit(id: String, text: String): Boolean {
        if (mutableState.value.busy) return false
        if (repository.state.value.phase != Phase.IDLE) {
            mutableState.value = CarCommandState(false, "Am Handy läuft bereits eine Aufnahme oder Sendung.")
            return false
        }
        if (!gate.begin(id, text)) return false
        mutableState.value = CarCommandState(true, "Befehl wird verarbeitet …")
        scope.launch {
            var historyId: Long? = null
            var message = "Nicht gesendet. Bitte erneut versuchen."
            try {
                val config = SettingsStore(context).load() // Read existing profiles; never save settings here.
                if (config.validate() != null) {
                    message = "Verbindungseinstellungen am Handy prüfen. Nicht gesendet."
                    return@launch
                }
                historyId = repository.add(text, config.protocol.name)
                message = try {
                    withContext(Dispatchers.IO) { NetworkSender().send(config, text) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Never expose exception messages containing URLs, credentials or response bodies.
                    when (e) {
                        is HttpStatusException -> "Fibaro HTTP ${e.status}. Nicht erneut gesendet."
                        else -> "Verbindung fehlgeschlagen. Zustellung unklar; Ziel prüfen."
                    }
                }
                repository.finish(historyId, message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = if (historyId == null) "Verlauf nicht verfügbar. Nicht gesendet."
                    else "Verlaufsstatus unklar. Ziel prüfen; nicht erneut gesendet."
            } finally {
                gate.finish(id)
                mutableState.value = CarCommandState(false, message)
            }
        }
        return true
    }
}
