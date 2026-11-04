package de.parip69.homesprachsteuerung

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

enum class Protocol { TEST, FIBARO, MQTT }
enum class FibaroConnection { AUTO, LOCAL, PUBLIC }

data class AppConfig(
    val protocol: Protocol = Protocol.TEST,
    val invokeScenes: Boolean = true,
    val voiceEntryEnabled: Boolean = true,
    val voiceFeedback: Boolean = true,
    val speechFeedback: Boolean = false,
    val speakCommand: Boolean = false,
    val initialWaitSeconds: Int = 1,
    val speechPauseSeconds: Int = 1,
    val maximumSpeechSeconds: Int = 10,
    val fibaroConnection: FibaroConnection = FibaroConnection.LOCAL,
    val publicFibaroUrl: String = "",
    val publicFibaroUser: String = "",
    val publicFibaroPassword: String = "",
    val fibaroUrl: String = "",
    val variable: String = "GesprochenerText",
    val fibaroUser: String = "",
    val fibaroPassword: String = "",
    val brokerUri: String = "",
    val port: Int = 1883,
    val clientId: String = "",
    val topic: String = "smarthome/sprache",
    val mqttUser: String = "",
    val mqttPassword: String = ""
) {
    fun validate(): String? = runCatching {
        require(initialWaitSeconds in 0..60) { "Wartezeit zum Beginnen: 0–60 Sekunden." }
        require(speechPauseSeconds in 0..15) { "Zusätzliche Sprechpause: 0–15 Sekunden." }
        require(maximumSpeechSeconds in 2..180 && maximumSpeechSeconds >= initialWaitSeconds) {
            "Maximale Aufnahmezeit: 2–180 Sekunden und mindestens so lang wie die Wartezeit."
        }
        when (protocol) {
            Protocol.TEST -> Unit
            Protocol.FIBARO -> {
                val localError = localFibaro().validationError(variable)
                val publicError = publicFibaro().validationError(variable)
                when (fibaroConnection) {
                    FibaroConnection.LOCAL -> require(localError == null) { "Lokal: $localError" }
                    FibaroConnection.PUBLIC -> require(publicError == null) { "Öffentlich: $publicError" }
                    FibaroConnection.AUTO -> require(localError == null || publicError == null) {
                        "Mindestens ein Fibaro-Profil vollständig einrichten. Lokal: $localError Öffentlich: $publicError"
                    }
                }
            }
            Protocol.MQTT -> {
                mqttEndpoint()
                // Conservative MQTT 3.1.1 client-ID for compatibility with older brokers.
                require(clientId.matches(Regex("[A-Za-z0-9]{1,23}"))) {
                    "Client-ID: 1–23 Buchstaben oder Ziffern, pro Gerät eindeutig."
                }
                require(topic.isNotBlank() && '+' !in topic && '#' !in topic && '\u0000' !in topic &&
                    topic.toByteArray(Charsets.UTF_8).size <= 65535) {
                    "MQTT-Topic fehlt oder enthält ungültige Zeichen (+, #, NUL)."
                }
                require(mqttPassword.isEmpty() || mqttUser.isNotBlank()) {
                    "Zum MQTT-Passwort bitte auch einen Benutzernamen eintragen."
                }
            }
        }
    }.exceptionOrNull()?.message

    fun localFibaro() = FibaroProfile(fibaroUrl, fibaroUser, fibaroPassword)
    fun publicFibaro() = FibaroProfile(publicFibaroUrl, publicFibaroUser, publicFibaroPassword)
    fun fibaroEndpoint(): HttpUrl = localFibaro().endpoint(variable)

    fun mqttEndpoint(): String {
        require(port in 1..65535) { "MQTT-Port muss zwischen 1 und 65535 liegen." }
        val uri = runCatching { URI(brokerUri) }.getOrNull()
        require(uri != null && uri.scheme in setOf("tcp", "ssl") && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawPath.orEmpty() in setOf("", "/")) {
            "MQTT: tcp://Host oder ssl://Host eingeben, ohne Pfad oder Zugangsdaten."
        }
        require(uri.port == -1 || uri.port == port) {
            "Port in Broker-URI und Port-Feld stimmen nicht überein."
        }
        return URI(uri.scheme, null, uri.host, port, null, null, null).toASCIIString()
    }
}
