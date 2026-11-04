package de.parip69.homesprachsteuerung

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Blocking transport: invoke only from Dispatchers.IO, never from the UI thread. */
class NetworkSender(private val http: OkHttpClient = defaultHttpClient()) {
    fun send(config: AppConfig, rawText: String): String {
        config.validate()?.let { throw IllegalArgumentException(it) }
        require(rawText.isNotBlank()) { "Kein Text zum Senden vorhanden." }
        return when (config.protocol) {
            Protocol.TEST -> "Nur lokal gespeichert · Testmodus (kein Versand)"
            Protocol.FIBARO -> sendFibaro(config, rawText)
            Protocol.MQTT -> sendMqtt(config, rawText)
        }
    }

    private fun sendFibaro(c: AppConfig, rawText: String): String {
        val destination = selectFibaro(c)
        val profile = destination.second
        val label = destination.first.label
        // JSONObject escapes quotes, backslashes, line breaks and German characters correctly.
        val body = JSONObject().put("value", rawText)
            .apply { if (c.invokeScenes) put("invokeScenes", true) }.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(profile.endpoint(c.variable))
            .header("Authorization", Credentials.basic(profile.user, profile.password, Charsets.UTF_8))
            .header("Accept", "application/json")
            .put(body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpStatusException(response.code, label)
            val prefix = response.peekBody(4096).string().trimStart()
            if (response.header("Content-Type").orEmpty().contains("text/html", ignoreCase = true) ||
                prefix.startsWith("<html", true) || prefix.startsWith("<!doctype html", true))
                throw UnexpectedFibaroResponseException("$label: Web-Anmeldeseite statt Fibaro-API. URL/Port prüfen.")
            return "Erfolgreich · Fibaro $label HTTP ${response.code}"
        }
    }

    /** Only GET probes occur before selection. Never retry a PUT on another profile. */
    private fun selectFibaro(c: AppConfig): Pair<FibaroConnection, FibaroProfile> {
        val selected = when (c.fibaroConnection) {
            FibaroConnection.LOCAL -> FibaroConnection.LOCAL
            FibaroConnection.PUBLIC -> FibaroConnection.PUBLIC
            FibaroConnection.AUTO -> {
                val local = c.localFibaro()
                if (local.validationError(c.variable) == null && probeLocal(local, c.variable))
                    FibaroConnection.LOCAL else FibaroConnection.PUBLIC
            }
        }
        val profile = if (selected == FibaroConnection.LOCAL) c.localFibaro() else c.publicFibaro()
        profile.validationError(c.variable)?.let { throw IllegalArgumentException("${selected.label}: $it") }
        return selected to profile
    }

    private fun probeLocal(profile: FibaroProfile, variable: String): Boolean = try {
        val probe = http.newBuilder().connectTimeout(1_200, TimeUnit.MILLISECONDS)
            .readTimeout(1_200, TimeUnit.MILLISECONDS).callTimeout(1_500, TimeUnit.MILLISECONDS).build()
        probe.newCall(readRequest(profile, variable)).execute().use { response ->
            // A responding authentication challenge stays local: surface the credentials error.
            response.code in setOf(401, 403) || (response.isSuccessful && isVariable(response.peekBody(65536).string(), variable))
        }
    } catch (e: IOException) { false }

    private fun readRequest(profile: FibaroProfile, variable: String) = Request.Builder()
        .url(profile.endpoint(variable)).get().header("Accept", "application/json")
        .header("Authorization", Credentials.basic(profile.user, profile.password, Charsets.UTF_8)).build()

    private fun isVariable(body: String, variable: String): Boolean = runCatching {
        val json = JSONObject(body)
        json.optString("name") == variable && json.has("value")
    }.getOrDefault(false)

    /** Read-only, including when the UI has unsaved profile edits. */
    fun testFibaro(c: AppConfig): String {
        val error = c.copy(protocol = Protocol.FIBARO).validate()
        require(error == null) { error.orEmpty() }
        val (selected, profile) = selectFibaro(c)
        val trace = FibaroConnectionTrace()
        return try { http.newBuilder().eventListener(trace).build()
            .newCall(readRequest(profile, c.variable)).execute().use { response ->
            val prefix = if (c.fibaroConnection == FibaroConnection.AUTO) "Automatisch → ${selected.label}" else selected.label
            val body = response.peekBody(65536).string()
            val result = when {
                response.code == 401 && response.header("WWW-Authenticate").orEmpty().contains("fritz", true) -> "$prefix: FRITZ!Box-Anmeldung statt Fibaro-API. Portfreigabe/Zieladresse prüfen."
                response.code == 401 -> "$prefix: Anmeldung abgelehnt (HTTP 401). Zugangsdaten des Zielservers prüfen."
                response.code == 403 -> "$prefix: Zugriff verweigert (HTTP 403). Benutzerrechte prüfen."
                response.isSuccessful && isVariable(body, c.variable) -> "$prefix: Fibaro-API erreichbar. Variable ${c.variable} vorhanden. Nur gelesen, nichts gesendet."
                response.isSuccessful && (body.contains("FRITZ!", true) || body.contains("fritz.box", true)) -> "$prefix: FRITZ!Box-Webseite statt Fibaro-API. Portfreigabe/Zieladresse prüfen."
                response.isSuccessful -> "$prefix: Antwort ist keine passende Fibaro-Variable. Möglicherweise eine Web-Anmeldeseite oder falsche Zieladresse."
                response.isRedirect -> "$prefix: Weiterleitung (HTTP ${response.code}). Keine automatische Web-Anmeldung; API-Adresse prüfen."
                else -> "$prefix: HTTP ${response.code}. API-Adresse, Variable und Berechtigungen prüfen."
            }
            "$result\n${trace.description}"
        } } catch (e: IOException) { throw FibaroProbeException(trace.description, e) }
    }

    private fun sendMqtt(c: AppConfig, rawText: String): String {
        val client = MqttClient(c.mqttEndpoint(), c.clientId, MemoryPersistence())
        client.setTimeToWait(12_000)
        try {
            val options = MqttConnectOptions().apply {
                mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                isCleanSession = true
                isAutomaticReconnect = false
                connectionTimeout = 8
                keepAliveInterval = 20
                if (c.mqttUser.isNotEmpty()) userName = c.mqttUser
                if (c.mqttPassword.isNotEmpty()) password = c.mqttPassword.toCharArray()
                // Retain default trust store + verify hostname for ssl://.
                setHttpsHostnameVerificationEnabled(true)
            }
            client.connect(options)
            client.publish(c.topic, MqttMessage(rawText.toByteArray(Charsets.UTF_8)).apply {
                qos = 1
                isRetained = false
            })
            // Synchronous Paho publish waits for PUBACK at QoS 1.
            return "Erfolgreich · MQTT vom Broker bestätigt (QoS 1)"
        } finally {
            // Cleanup errors must not turn an acknowledged publish into a false failure.
            runCatching { if (client.isConnected) client.disconnectForcibly(0L, 1_000L) }
            runCatching { client.close(true) }
        }
    }

    companion object {
        fun defaultHttpClient() = OkHttpClient.Builder()
            .dns(Ipv4FirstDns())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}

val FibaroConnection.label: String get() = when (this) {
    FibaroConnection.AUTO -> "Automatisch"
    FibaroConnection.LOCAL -> "Lokal"
    FibaroConnection.PUBLIC -> "Öffentlich"
}
class HttpStatusException(val status: Int, val connection: String? = null) : IOException("HTTP $status")
class UnexpectedFibaroResponseException(message: String) : IOException(message)
