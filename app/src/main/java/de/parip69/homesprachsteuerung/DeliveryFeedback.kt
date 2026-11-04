package de.parip69.homesprachsteuerung

import java.net.UnknownHostException

enum class DeliveryKind { SENT, LOCAL_ONLY, FAILED, UNCERTAIN, CANCELLED }

/** Feedback follows the transport outcome, never a guessed substring in a status message. */
data class DeliveryFeedback(val kind: DeliveryKind, val detail: String, val command: String? = null) {
    val title: String get() = when (kind) {
        DeliveryKind.SENT -> "Befehl gesendet"
        DeliveryKind.LOCAL_ONLY -> "Nur lokal gespeichert"
        DeliveryKind.FAILED -> "Befehl nicht gesendet"
        DeliveryKind.UNCERTAIN -> "Sendestatus unklar"
        DeliveryKind.CANCELLED -> "Aufnahme abgebrochen"
    }
    private val hasCommand: Boolean get() = !command.isNullOrBlank()
    private val resultText: String get() = when (kind) {
        DeliveryKind.SENT -> "Erfolgreich gesendet"
        DeliveryKind.LOCAL_ONLY -> "Nur lokal gespeichert · Testmodus"
        DeliveryKind.FAILED -> "Nicht gesendet"
        DeliveryKind.UNCERTAIN -> "Sendestatus unklar"
        DeliveryKind.CANCELLED -> "Aufnahme abgebrochen"
    }
    val popup: String get() = if (hasCommand) "$command\n$resultText" else "$title\n$detail"
    val overview: String get() = if (hasCommand) "$command\n$resultText\n$detail" else "$title\n$detail"
    val spoken: String get() {
        if (hasCommand) {
            val raw = command!!.trim()
            val separator = if (raw.last() in ".!?") " " else ". "
            val outcome = when (kind) {
                DeliveryKind.SENT -> "Erfolgreich gesendet."
                DeliveryKind.LOCAL_ONLY -> "Nur lokal gespeichert. Testmodus."
                DeliveryKind.FAILED -> "Nicht gesendet."
                DeliveryKind.UNCERTAIN -> "Sendestatus unklar. Bitte Verlauf prüfen."
                DeliveryKind.CANCELLED -> "Aufnahme abgebrochen."
            }
            return raw + separator + outcome
        }
        return when (kind) {
            DeliveryKind.SENT -> "Befehl gesendet."
            DeliveryKind.LOCAL_ONLY -> "Nur lokal gespeichert. Testmodus."
            DeliveryKind.FAILED -> "Befehl konnte nicht gesendet werden."
            DeliveryKind.UNCERTAIN -> "Sendestatus unklar. Bitte Verlauf prüfen."
            DeliveryKind.CANCELLED -> "Aufnahme abgebrochen."
        }
    }
    /** Short speech leaves the command in the overview/history, without reading it aloud. */
    fun speech(includeCommand: Boolean): String = if (includeCommand) spoken else when (kind) {
        DeliveryKind.SENT -> "Erfolgreich gesendet."
        DeliveryKind.LOCAL_ONLY -> "Nur lokal gespeichert. Testmodus."
        DeliveryKind.FAILED -> detail.ifBlank { "Befehl konnte nicht gesendet werden." }
        DeliveryKind.UNCERTAIN -> detail.ifBlank { "Sendestatus unklar. Bitte Verlauf prüfen." }
        DeliveryKind.CANCELLED -> "Aufnahme abgebrochen."
    }
    companion object {
        fun failureKind(error: Exception): DeliveryKind = when (error) {
            is HttpStatusException, is UnknownHostException, is IllegalArgumentException -> DeliveryKind.FAILED
            else -> DeliveryKind.UNCERTAIN
        }
    }
}
