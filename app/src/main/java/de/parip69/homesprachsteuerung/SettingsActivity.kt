package de.parip69.homesprachsteuerung

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.parip69.homesprachsteuerung.databinding.SettingsActivityBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var ui: SettingsActivityBinding
    private lateinit var store: SettingsStore
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ui = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(ui.root)
        ui.root.applySystemInsets()
        store = SettingsStore(this)
        ui.toolbar.setNavigationOnClickListener { finish() }
        ui.modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.test_mode), getString(R.string.fibaro), getString(R.string.mqtt)))
        ui.connectionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Automatisch", "Lokal", "Öffentlich"))
        ui.modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateSections(position)
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        if (savedInstanceState == null) populate(store.load())
        ui.saveButton.setOnClickListener { save() }
        ui.testLocalConnection.setOnClickListener { testConnection(FibaroConnection.LOCAL) }
        ui.testConnection.setOnClickListener { testConnection(FibaroConnection.PUBLIC) }
    }

    private fun updateSections(position: Int) {
        ui.fibaroFields.visibility = if (position == Protocol.FIBARO.ordinal) View.VISIBLE else View.GONE
        ui.mqttFields.visibility = if (position == Protocol.MQTT.ordinal) View.VISIBLE else View.GONE
    }

    private fun populate(c: AppConfig) {
        ui.modeSpinner.setSelection(c.protocol.ordinal)
        updateSections(c.protocol.ordinal)
        ui.connectionSpinner.setSelection(c.fibaroConnection.ordinal)
        ui.invokeScenes.isChecked = c.invokeScenes
        ui.voiceEntry.isChecked = c.voiceEntryEnabled
        ui.voiceFeedback.isChecked = c.voiceFeedback
        ui.speechFeedback.isChecked = c.speechFeedback
        ui.speakCommand.isChecked = c.speakCommand
        ui.initialWait.setText(c.initialWaitSeconds.toString())
        ui.speechPause.setText(c.speechPauseSeconds.toString())
        ui.maximumSpeech.setText(c.maximumSpeechSeconds.toString())
        ui.fibaroUrl.setText(c.fibaroUrl)
        ui.variable.setText(c.variable)
        ui.fibaroUser.setText(c.fibaroUser)
        ui.fibaroPassword.setText(c.fibaroPassword)
        ui.publicFibaroUrl.setText(c.publicFibaroUrl)
        ui.publicFibaroUser.setText(c.publicFibaroUser)
        ui.publicFibaroPassword.setText(c.publicFibaroPassword)
        ui.brokerUri.setText(c.brokerUri)
        ui.port.setText(c.port.toString())
        ui.clientId.setText(c.clientId)
        ui.topic.setText(c.topic)
        ui.mqttUser.setText(c.mqttUser)
        ui.mqttPassword.setText(c.mqttPassword)
    }

    private fun importPublicLink(): Boolean {
        if (busy) return false
        val imported = try {
            FibaroLink.fromAddress(ui.publicFibaroUrl.text.toString(), ui.publicFibaroUser.text.toString().trim(),
                ui.publicFibaroPassword.text.toString(), ui.variable.text.toString().trim()) ?: return true
        } catch (e: IllegalArgumentException) {
            ui.connectionResult.text = e.message ?: "Fibaro-Link konnte nicht übernommen werden."
            return false
        }
        ui.publicFibaroUrl.setText(imported.baseUrl)
        ui.publicFibaroUser.setText(imported.username)
        ui.publicFibaroPassword.setText(imported.password)
        ui.variable.setText(imported.variable)
        if (imported.invokeScenes) ui.invokeScenes.isChecked = true
        ui.connectionResult.text = "Link übernommen. Zugangsdaten sind in die einzelnen Felder eingetragen. Jetzt prüfen und anschließend speichern."
        return true
    }

    private fun readConfig(): AppConfig? {
        if (!importPublicLink()) return null
        val protocol = Protocol.entries[ui.modeSpinner.selectedItemPosition]
        val port = ui.port.text.toString().toIntOrNull()
        if (protocol == Protocol.MQTT && port == null) {
            showError("MQTT-Port muss eine ganze Zahl zwischen 1 und 65535 sein.")
            return null
        }
        val initialWait = ui.initialWait.text.toString().toIntOrNull()
        val speechPause = ui.speechPause.text.toString().toIntOrNull()
        val maximumSpeech = ui.maximumSpeech.text.toString().toIntOrNull()
        if (initialWait == null || speechPause == null || maximumSpeech == null) {
            showError("Bitte die drei Sprechzeiten als ganze Sekunden eintragen.")
            return null
        }
        return AppConfig(
            protocol = protocol,
            invokeScenes = ui.invokeScenes.isChecked,
            voiceEntryEnabled = ui.voiceEntry.isChecked,
            voiceFeedback = ui.voiceFeedback.isChecked,
            speechFeedback = ui.speechFeedback.isChecked,
            speakCommand = ui.speakCommand.isChecked,
            initialWaitSeconds = initialWait,
            speechPauseSeconds = speechPause,
            maximumSpeechSeconds = maximumSpeech,
            fibaroConnection = FibaroConnection.entries[ui.connectionSpinner.selectedItemPosition],
            fibaroUrl = ui.fibaroUrl.text.toString().trim(),
            variable = ui.variable.text.toString().trim(),
            fibaroUser = ui.fibaroUser.text.toString().trim(),
            fibaroPassword = ui.fibaroPassword.text.toString(),
            publicFibaroUrl = ui.publicFibaroUrl.text.toString().trim(),
            publicFibaroUser = ui.publicFibaroUser.text.toString().trim(),
            publicFibaroPassword = ui.publicFibaroPassword.text.toString(),
            brokerUri = ui.brokerUri.text.toString().trim(),
            port = port ?: store.load().port,
            clientId = ui.clientId.text.toString().trim(),
            topic = ui.topic.text.toString(),
            mqttUser = ui.mqttUser.text.toString(),
            mqttPassword = ui.mqttPassword.text.toString()
        )
    }

    private fun setBusy(value: Boolean) {
        busy = value
        ui.saveButton.isEnabled = !value
        ui.testConnection.isEnabled = !value
        ui.testLocalConnection.isEnabled = !value
    }

    private fun save() {
        if (busy) return
        val config = readConfig() ?: return
        config.validate()?.let { showError(it); return }
        setBusy(true)
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { store.save(config) }
            setBusy(false)
            if (success) {
                Toast.makeText(this@SettingsActivity, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
                finish()
            } else showError("Einstellungen konnten nicht dauerhaft gespeichert werden.")
        }
    }

    private fun testConnection(connection: FibaroConnection) {
        if (busy) return
        val config = readConfig()?.copy(protocol = Protocol.FIBARO, fibaroConnection = connection) ?: return
        val resultView = if (connection == FibaroConnection.LOCAL) ui.localConnectionResult else ui.connectionResult
        config.validate()?.let { resultView.text = it; return }
        val profile = if (connection == FibaroConnection.LOCAL)
            FibaroProfile(config.fibaroUrl, config.fibaroUser, config.fibaroPassword)
        else FibaroProfile(config.publicFibaroUrl, config.publicFibaroUser, config.publicFibaroPassword)
        val target = "GET ${profile.endpoint(config.variable)}"
        val startedAt = SystemClock.elapsedRealtime()
        ui.errorText.visibility = View.GONE
        resultView.text = "${connection.label} wird geprüft …\n$target\nNur Lesen, kein Sprachbefehl."
        setBusy(true)
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { NetworkSender().testFibaro(config) }
                resultView.text = "$result\n$target\nDauer: ${SystemClock.elapsedRealtime() - startedAt} ms"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val trace = (e as? FibaroProbeException)?.connectionTrace.orEmpty()
                resultView.text = "${connection.label}: ${ConnectionDiagnostics.describe(e)}\n$trace\n$target\nDauer: ${SystemClock.elapsedRealtime() - startedAt} ms\nEs wurde kein Befehl gesendet."
            } finally { setBusy(false) }
        }
    }

    private fun showError(message: String) {
        ui.errorText.text = message
        ui.errorText.visibility = View.VISIBLE
    }
}
