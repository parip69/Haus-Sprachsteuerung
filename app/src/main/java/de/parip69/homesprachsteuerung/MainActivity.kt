package de.parip69.homesprachsteuerung

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.parip69.homesprachsteuerung.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var ui: ActivityMainBinding
    private val repository get() = (application as BridgeApplication).repository
    private lateinit var settingsItem: MenuItem
    private lateinit var clearItem: MenuItem
    private var permissionPending = false
    private var autoListenPending = false
    private var settingsPending = false

    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionPending = false
        startCapture() // A denied notification permission does not prohibit a foreground service.
    }
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionPending = false
        if (granted) requestNotificationsAndStart()
        else {
            repository.update(Phase.IDLE, "Mikrofon-Berechtigung fehlt.", true)
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                MaterialAlertDialogBuilder(this).setTitle("Mikrofon freigeben")
                    .setMessage("Bitte unter App-Einstellungen → Berechtigungen das Mikrofon erlauben.")
                    .setPositiveButton("App-Einstellungen") { _, _ ->
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:$packageName")))
                    }.setNegativeButton("Abbrechen", null).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        ui.root.applySystemInsets()
        settingsItem = ui.toolbar.menu.add(getString(R.string.settings)).apply {
            setIcon(R.drawable.ic_settings); setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener { openSettings(); true }
        }
        ui.toolbar.menu.add("Widget hinzufügen").setOnMenuItemClickListener {
            pinSpeechWidget()
            true
        }
        clearItem = ui.toolbar.menu.add("Verlauf löschen").apply {
            setOnMenuItemClickListener {
                MaterialAlertDialogBuilder(this@MainActivity).setTitle("Verlauf löschen?")
                    .setMessage("Alle lokal gespeicherten Sprachtexte werden gelöscht.")
                    .setNegativeButton("Abbrechen", null).setPositiveButton("Löschen") { _, _ ->
                        lifecycleScope.launch {
                            try { repository.clear() }
                            catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                repository.update(Phase.IDLE, "Verlauf konnte nicht gelöscht werden.", true)
                            }
                        }
                    }.show()
                true
            }
        }
        permissionPending = savedInstanceState?.getBoolean("permissionPending") ?: false
        autoListenPending = shouldListenOnOpen(intent, savedInstanceState != null)
        settingsPending = savedInstanceState == null && intent?.action == ACTION_SETTINGS
        ui.micButton.setOnClickListener { requestCapture() }
        ui.overviewButton.setOnClickListener { showOverview() }
        ui.sendNowButton.setOnClickListener {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.SEND_NOW))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { repository.state.collect { state ->
                    val busy = state.phase != Phase.IDLE
                    ui.micButton.isEnabled = !busy
                    settingsItem.isEnabled = true
                    clearItem.isEnabled = !busy
                    ui.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
                    ui.statusText.text = state.status
                    ui.overviewButton.visibility = if (busy) View.VISIBLE else View.GONE
                    ui.sendNowButton.visibility = if (state.phase == Phase.LISTENING) View.VISIBLE else View.GONE
                    if (state.event > repository.consumedEvent) {
                        repository.consumedEvent = state.event
                        Toast.makeText(this@MainActivity, state.status, Toast.LENGTH_SHORT).show()
                    }
                } }
                launch { repository.history.collect { entries -> renderHistory(entries) } }
            }
        }
    }

    private fun shouldListenOnOpen(incoming: Intent?, restoring: Boolean = false): Boolean =
        VoiceEntryPolicy.shouldListen(incoming?.action,
            ((incoming?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0,
            SettingsStore(this).load().voiceEntryEnabled, restoring)

    override fun onPostResume() {
        super.onPostResume()
        if (settingsPending) {
            settingsPending = false
            openSettings()
            return
        }
        if (autoListenPending) {
            autoListenPending = false
            requestCapture()
        }
    }

    // Normal and legacy launches open the full screen. Never hide it after capture.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        autoListenPending = shouldListenOnOpen(intent) && !permissionPending && repository.state.value.phase == Phase.IDLE
        settingsPending = intent.action == ACTION_SETTINGS
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("permissionPending", permissionPending)
        super.onSaveInstanceState(outState)
    }

    private fun openSettings() {
        autoListenPending = false
        if (repository.state.value.phase in setOf(Phase.STARTING, Phase.LISTENING)) {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.CANCEL))
        }
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun pinSpeechWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, "Startbildschirm lange drücken → Widgets → Alfonso auswählen.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (!manager.requestPinAppWidget(ComponentName(this, SpeechWidgetProvider::class.java), null, null))
                Toast.makeText(this, "Bitte das Widget über Startbildschirm → Widgets → Alfonso hinzufügen.", Toast.LENGTH_LONG).show()
        } catch (e: RuntimeException) {
            Toast.makeText(this, "Bitte das Widget über Startbildschirm → Widgets → Alfonso hinzufügen.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOverview() {
        if (repository.state.value.phase in setOf(Phase.STARTING, Phase.LISTENING)) {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.CANCEL))
        }
    }

    private fun requestCapture() {
        if (permissionPending || repository.state.value.phase != Phase.IDLE) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            requestNotificationsAndStart()
        else {
            permissionPending = true
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionPending = true
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startCapture()
    }

    private fun startCapture() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || repository.state.value.phase != Phase.IDLE) return
        SettingsStore(this).load().validate()?.let {
            repository.update(Phase.IDLE, "Einstellungen prüfen: $it", true)
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        repository.update(Phase.STARTING, "Spracherkennung startet … bitte kurz warten")
        try { ContextCompat.startForegroundService(this, Intent(this, CaptureService::class.java)) }
        catch (e: Exception) {
            repository.update(Phase.IDLE, "Hintergrundaufnahme konnte nicht gestartet werden.", true)
        }
    }

    companion object {
        const val ACTION_HISTORY = "de.parip69.homesprachsteuerung.VIEW_HISTORY"
        const val ACTION_CAPTURE = "de.parip69.homesprachsteuerung.START_CAPTURE"
        const val ACTION_SETTINGS = "de.parip69.homesprachsteuerung.OPEN_SETTINGS"
    }

    private fun renderHistory(entries: List<HistoryEntry>) {
        ui.recognizedText.text = entries.firstOrNull()?.text ?: getString(R.string.no_text)
        ui.historyContainer.removeAllViews()
        if (entries.isEmpty()) {
            ui.historyContainer.addView(TextView(this).apply { text = "Noch keine Einträge. Testmodus funktioniert ohne Server." })
        }
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        for (entry in entries) {
            ui.historyContainer.addView(TextView(this).apply {
                text = "${formatter.format(Date(entry.time))} · ${entry.target}\n${entry.text}\n${entry.status}"
                // Keep the actual recognized text untouched; only presentation adds separators.
                textSize = 15f
                setTextIsSelectable(true)
                val pad = (16 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            })
        }
    }
}
