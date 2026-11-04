package de.parip69.homesprachsteuerung

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class UpdateActivity : AppCompatActivity() {
    private lateinit var model: UpdateViewModel
    private lateinit var status: TextView
    private lateinit var versions: TextView
    private lateinit var notes: TextView
    private lateinit var progress: ProgressBar
    private lateinit var check: Button
    private lateinit var download: Button
    private lateinit var install: Button
    private lateinit var cancel: Button

    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.returned(if (packageManager.canRequestPackageInstalls()) "Installation erlaubt. APK wird erneut geprüft."
            else "Installation noch nicht erlaubt. Du kannst die Freigabe später erneut öffnen.")
        if (packageManager.canRequestPackageInstalls()) model.install()
    }
    private val installer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // An installer return is not evidence that a replacement actually succeeded.
        model.returned("Android-Installation beendet oder abgebrochen. Installierte Version oben prüfen; bei Bedarf erneut installieren.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[UpdateViewModel::class.java]
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(root)
        setContentView(scroll)
        scroll.applySystemInsets()
        val inset = (20 * resources.displayMetrics.density).toInt()
        root.setPadding(inset, inset, inset, inset)
        fun text(value: String) = TextView(this).also { it.text = value; it.setPadding(0, inset / 2, 0, inset / 2); root.addView(it) }
        fun button(value: String) = Button(this).also { it.text = value; root.addView(it) }
        text("Alfonso · App aktualisieren").textSize = 22f
        versions = text("")
        status = text("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).also { it.max = 100; root.addView(it) }
        check = button("Nach Updates suchen").apply { setOnClickListener { model.check() } }
        download = button("Herunterladen und installieren").apply { setOnClickListener { model.download() } }
        install = button("Installation fortsetzen").apply {
            setOnClickListener {
                if (!packageManager.canRequestPackageInstalls()) {
                    try {
                        model.externalOpened()
                        permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                    } catch (_: Exception) { model.returned("Android-Einstellung nicht verfügbar. Bitte unter Einstellungen → Unbekannte Apps installieren → Alfonso die Quelle freigeben.") }
                } else model.install()
            }
        }
        cancel = button("Download abbrechen").apply { setOnClickListener { model.cancelDownload() } }
        notes = text("")
        text("Updates kommen aus den öffentlichen GitHub-Releases dieses Projekts. Android fragt vor jeder Installation nach. Einstellungen und Verlauf bleiben beim Update erhalten. Keine Deinstallation nötig.")
        button("Zurück").setOnClickListener { finish() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.state.collect { state ->
                    versions.text = "Installiert: ${model.installedLabel}\nVerfügbar: ${state.available}"
                    status.text = if (state.downloading) "${state.message} ${state.progress} %" else state.message
                    notes.text = if (state.notes.isBlank()) "" else "Änderungen:\n${state.notes}"
                    check.isEnabled = !state.busy && !state.downloading
                    download.visibility = if (state.canDownload) View.VISIBLE else View.GONE
                    download.isEnabled = !state.busy && !state.downloading
                    install.visibility = if (state.canInstall) View.VISIBLE else View.GONE
                    install.isEnabled = !state.busy
                    install.text = if (packageManager.canRequestPackageInstalls()) "Installation fortsetzen" else "Installation durch Alfonso erlauben"
                    if (state.canInstall && !packageManager.canRequestPackageInstalls()) {
                        status.text = "${state.message}\nAndroid benötigt „Aus dieser Quelle zulassen“. Öffne die Freigabe unten. Nach deiner Rückkehr wird die APK erneut geprüft und die Installation fortgesetzt."
                    }
                    cancel.visibility = if (state.downloading) View.VISIBLE else View.GONE
                    progress.visibility = if (state.downloading || state.busy) View.VISIBLE else View.GONE
                    progress.isIndeterminate = state.busy
                    progress.progress = state.progress
                    if (state.launchInstaller) launchInstaller()
                }
            }
        }
    }
    override fun onStart() { super.onStart(); model.observeDownload() }
    override fun onStop() { model.stopObserving(); super.onStop() }
    @Suppress("DEPRECATION")
    private fun launchInstaller() {
        val file = model.consumeInstaller() ?: return
        if (!packageManager.canRequestPackageInstalls()) {
            model.returned("APK erfolgreich geprüft. Bitte die Installation durch Alfonso erlauben.")
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.updates", file)
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            intent.clipData = ClipData.newRawUri("Alfonso-Update", uri)
            model.externalOpened()
            installer.launch(intent)
        } catch (_: Exception) { model.returned("Android-Paketinstaller konnte nicht geöffnet werden. Bitte erneut versuchen.") }
    }
}
