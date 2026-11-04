package de.parip69.homesprachsteuerung

import android.content.Context
import java.util.UUID

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("home_voice", Context.MODE_PRIVATE)

    init {
        // The requested result feedback is enabled once for this upgrade.
        if (!prefs.getBoolean("completionFeedbackV108", false)) {
            prefs.edit().putBoolean("voiceFeedback", true).putBoolean("completionFeedbackV108", true).apply()
        }
        if (!prefs.contains("clientId")) {
            prefs.edit().putString("clientId", "HV" + UUID.randomUUID().toString().replace("-", "").take(20)).apply()
        }
    }

    fun load() = AppConfig(
        protocol = runCatching { Protocol.valueOf(prefs.getString("protocol", "TEST")!!) }
            .getOrDefault(Protocol.TEST),
        invokeScenes = prefs.getBoolean("invokeScenes", true),
        voiceEntryEnabled = prefs.getBoolean("voiceEntryEnabled", true),
        voiceFeedback = prefs.getBoolean("voiceFeedback", true),
        speechFeedback = prefs.getBoolean("speechFeedback", false),
        speakCommand = prefs.getBoolean("speakCommand", false),
        initialWaitSeconds = prefs.getInt("initialWaitSeconds", 1),
        speechPauseSeconds = prefs.getInt("speechPauseSeconds", 1),
        maximumSpeechSeconds = prefs.getInt("maximumSpeechSeconds", 10),
        fibaroConnection = runCatching { FibaroConnection.valueOf(prefs.getString("fibaroConnection", "AUTO")!!) }
            .getOrDefault(FibaroConnection.AUTO),
        publicFibaroUrl = prefs.getString("publicFibaroUrl", FibaroDefaults.PUBLIC_URL).orEmpty(),
        publicFibaroUser = prefs.getString("publicFibaroUser", "").orEmpty(),
        publicFibaroPassword = prefs.getString("publicFibaroPassword", "").orEmpty(),
        fibaroUrl = prefs.getString("fibaroUrl", FibaroDefaults.LOCAL_URL).orEmpty(),
        variable = prefs.getString("variable", "GesprochenerText").orEmpty(),
        fibaroUser = prefs.getString("fibaroUser", "").orEmpty(),
        fibaroPassword = prefs.getString("fibaroPassword", "").orEmpty(),
        brokerUri = prefs.getString("brokerUri", "").orEmpty(),
        port = prefs.getInt("port", 1883),
        clientId = prefs.getString("clientId", "").orEmpty(),
        topic = prefs.getString("topic", "smarthome/sprache").orEmpty(),
        mqttUser = prefs.getString("mqttUser", "").orEmpty(),
        mqttPassword = prefs.getString("mqttPassword", "").orEmpty()
    )

    // Called on Dispatchers.IO; commit reports a failed write instead of pretending success.
    fun save(c: AppConfig): Boolean = prefs.edit()
        .putString("protocol", c.protocol.name).putBoolean("invokeScenes", c.invokeScenes)
        .putBoolean("voiceEntryEnabled", c.voiceEntryEnabled).putBoolean("voiceFeedback", c.voiceFeedback)
        .putBoolean("speechFeedback", c.speechFeedback)
        .putBoolean("speakCommand", c.speakCommand)
        .putInt("initialWaitSeconds", c.initialWaitSeconds).putInt("speechPauseSeconds", c.speechPauseSeconds)
        .putInt("maximumSpeechSeconds", c.maximumSpeechSeconds)
        .putString("fibaroConnection", c.fibaroConnection.name)
        .putString("publicFibaroUrl", c.publicFibaroUrl).putString("publicFibaroUser", c.publicFibaroUser)
        .putString("publicFibaroPassword", c.publicFibaroPassword)
        .putString("fibaroUrl", c.fibaroUrl).putString("variable", c.variable)
        .putString("fibaroUser", c.fibaroUser).putString("fibaroPassword", c.fibaroPassword)
        .putString("brokerUri", c.brokerUri).putInt("port", c.port)
        .putString("clientId", c.clientId).putString("topic", c.topic)
        .putString("mqttUser", c.mqttUser).putString("mqttPassword", c.mqttPassword)
        .commit()
}
