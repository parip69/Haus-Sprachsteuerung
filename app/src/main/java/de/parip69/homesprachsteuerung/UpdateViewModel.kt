package de.parip69.homesprachsteuerung

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.apksig.ApkVerifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest

internal data class UpdateUi(val message: String = "Suche manuell nach einem veröffentlichten Alfonso-Update.",
    val available: String = "–", val notes: String = "", val busy: Boolean = false,
    val downloading: Boolean = false, val progress: Int = 0, val canDownload: Boolean = false,
    val canInstall: Boolean = false, val launchInstaller: Boolean = false)

internal class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val context = getApplication<Application>()
    private val prefs = context.getSharedPreferences("githubUpdater", Context.MODE_PRIVATE)
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val mutable = MutableStateFlow(UpdateUi())
    val state = mutable.asStateFlow()
    private var selection: Triple<UpdateMetadata, UpdateAsset, String>? = null
    private var operation: Job? = null
    private var pollJob: Job? = null
    val installedInfo: PackageInfo get() = context.packageManager.getPackageInfo(context.packageName, 0)
    @Suppress("DEPRECATION")
    private fun code(info: PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    val installedLabel get() = installedInfo.versionName.orEmpty()
    private val file: File get() = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: error("Update-Speicher ist nicht verfügbar."), "updates/Alfonso-update.apk")
    private val id get() = prefs.getLong("downloadId", -1)
    private val verifiedFile get() = File(context.filesDir, "updates/Alfonso.apk")

    init {
        if (id != -1L) mutable.value = UpdateUi(message = "Vorhandener Update-Download wird geprüft.")
        if (prefs.getBoolean("installerOpen", false)) {
            prefs.edit().putBoolean("installerOpen", false).apply()
            mutable.value = UpdateUi(message = "Installation nicht bestätigt. Du kannst die APK erneut prüfen und installieren.", canInstall = id != -1L)
        }
        if (id != -1L) runCatching {
            val meta = UpdateMetadata.parse(prefs.getString("metadata", "")!!)
            mutable.value = mutable.value.copy(available = meta.versionName)
        }
    }

    fun check() {
        if (state.value.busy || state.value.downloading || operation?.isActive == true) return
        operation = viewModelScope.launch {
            mutable.value = UpdateUi(message = "GitHub wird geprüft …", busy = true)
            try {
                selection = withContext(Dispatchers.IO) { UpdateSource().latest(code(installedInfo)) }
                val found = selection
                mutable.value = if (found == null) UpdateUi(message = "Noch kein Update veröffentlicht") else {
                    val newer = found.first.versionCode > code(installedInfo)
                    UpdateUi(message = if (newer) "Ein Update ist verfügbar." else "Alfonso ist aktuell. Keine neuere Version verfügbar.",
                        available = found.first.versionName, notes = found.third, canDownload = newer)
                }
            } catch (e: Exception) { fail(e, "Updates konnten nicht geprüft werden. Bitte Internetverbindung prüfen.") }
        }
    }

    fun download() {
        if (!state.value.canDownload || state.value.busy || state.value.downloading) return
        val found = selection ?: return
        mutable.value = state.value.copy(busy = true, canDownload = false)
        try {
            if (id != -1L) downloads.remove(id)
            file.parentFile!!.mkdirs()
            require(!file.exists() || file.delete()) { "Alte Update-Datei kann nicht entfernt werden." }
            // Use the explicit download URL from the published repository manifest.
            val request = DownloadManager.Request(Uri.parse(found.second.url))
                .setTitle("Alfonso ${found.first.versionName}")
                .setDescription("App-Update herunterladen")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
            // Persist verification requirements before enqueue; no settings/history are touched.
            require(prefs.edit().putString("metadata", found.first.json()).putLong("size", found.second.size).remove("offered").commit()) { "Update-Metadaten konnten nicht gespeichert werden." }
            val newId = downloads.enqueue(request)
            if (!prefs.edit().putLong("downloadId", newId).commit()) {
                downloads.remove(newId); error("Downloadstatus konnte nicht gespeichert werden.")
            }
            mutable.value = state.value.copy(message = "Download läuft …", busy = false, downloading = true)
            observeDownload()
        } catch (e: Exception) { fail(e, "Download konnte nicht gestartet werden. Bitte Speicher und Internet prüfen.") }
    }

    fun observeDownload() {
        if (pollJob?.isActive == true || id == -1L) return
        pollJob = viewModelScope.launch {
            while (id != -1L) {
                try {
                    var complete = false
                    withContext(Dispatchers.IO) {
                        downloads.query(DownloadManager.Query().setFilterById(id)).use { c ->
                            require(c != null && c.moveToFirst()) { "Download nicht mehr vorhanden. Bitte erneut suchen und herunterladen." }
                            fun int(name: String) = c.getInt(c.getColumnIndexOrThrow(name))
                            fun long(name: String) = c.getLong(c.getColumnIndexOrThrow(name))
                            when (int(DownloadManager.COLUMN_STATUS)) {
                                DownloadManager.STATUS_SUCCESSFUL -> complete = true
                                DownloadManager.STATUS_FAILED -> error("Download fehlgeschlagen (Android-Code ${int(DownloadManager.COLUMN_REASON)}). Bitte Verbindung und Speicher prüfen und erneut herunterladen.")
                                else -> {
                                    val size = prefs.getLong("size", 0)
                                    val bytes = long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                    require(bytes <= size && size in 1..MAX_APK_BYTES) { "Unerwartete Downloadgröße." }
                                    mutable.value = mutable.value.copy(message = if (int(DownloadManager.COLUMN_STATUS) == DownloadManager.STATUS_PAUSED)
                                        "Download pausiert. Android wartet auf eine Verbindung; Abbrechen ist möglich." else "Download läuft …",
                                        downloading = true, progress = (bytes * 100 / size).toInt().coerceIn(0, 100), canDownload = false)
                                }
                            }
                        }
                    }
                    if (complete) {
                        if (!state.value.busy && !state.value.launchInstaller) {
                            mutable.value = state.value.copy(message = "Download vollständig. APK wird vor der Installation geprüft.", downloading = false, canInstall = true, progress = 100)
                            if (!prefs.getBoolean("offered", false)) {
                                prefs.edit().putBoolean("offered", true).apply()
                                install()
                            }
                        }
                        break
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    cancelDownload()
                    fail(e, "Downloadstatus konnte nicht gelesen werden.")
                    break
                }
                delay(700)
            }
        }
    }
    fun stopObserving() { pollJob?.cancel(); pollJob = null }
    fun cancelDownload() {
        stopObserving()
        if (id != -1L) downloads.remove(id)
        prefs.edit().remove("downloadId").remove("offered").apply()
        mutable.value = UpdateUi(message = "Download abgebrochen. Du kannst erneut nach Updates suchen.")
    }
    fun install() {
        if (state.value.busy || state.value.downloading || state.value.launchInstaller || operation?.isActive == true) return
        if (id == -1L) return
        operation = viewModelScope.launch {
            mutable.value = state.value.copy(busy = true, canInstall = false, message = "APK-Prüfung läuft …")
            try {
                withContext(Dispatchers.IO) { verify() }
                mutable.value = state.value.copy(busy = false, launchInstaller = true, message = "APK geprüft. Android-Installation wird vorbereitet.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (id != -1L) downloads.remove(id)
                prefs.edit().remove("downloadId").remove("offered").apply()
                fail(e, "APK-Prüfung fehlgeschlagen. Bitte erneut herunterladen.")
            }
        }
    }
    private fun verify() {
        val meta = UpdateMetadata.parse(prefs.getString("metadata", null) ?: error("Update-Metadaten fehlen."))
        // Verify and share only a private copy: external DownloadManager storage is not trusted.
        require(file.length() == prefs.getLong("size", 0) && file.length() in 1..MAX_APK_BYTES) { "Der APK-Download ist unvollständig." }
        verifiedFile.parentFile!!.mkdirs()
        file.copyTo(verifiedFile, overwrite = true)
        UpdatePolicy.verifyFile(verifiedFile, prefs.getLong("size", 0), meta.sha256)
        val verification = ApkVerifier.Builder(verifiedFile).setMinCheckedPlatformVersion(26).build().verify()
        require(verification.isVerified) { "Die APK ist beschädigt oder ihre kryptografische Signatur ist ungültig." }
        val info = context.packageManager.getPackageArchiveInfo(verifiedFile.absolutePath, 0) ?: error("Ungültige APK.")
        val signers = verification.signerCertificates.map {
            UpdatePolicy.hex(MessageDigest.getInstance("SHA-256").digest(it.encoded))
        }.toSet()
        UpdatePolicy.verifyIdentity(meta, code(installedInfo), info.packageName, code(info), info.versionName, signers)
    }
    fun consumeInstaller(): File? {
        if (!state.value.launchInstaller) return null
        mutable.value = state.value.copy(launchInstaller = false)
        return verifiedFile
    }
    fun externalOpened() {
        prefs.edit().putBoolean("installerOpen", true).apply()
        mutable.value = state.value.copy(busy = true, canInstall = false, message = "Bitte Installation im Android-Fenster bestätigen.")
    }
    fun returned(message: String) {
        prefs.edit().putBoolean("installerOpen", false).apply()
        mutable.value = state.value.copy(busy = false, canInstall = id != -1L, message = message)
    }
    private fun fail(e: Exception, fallback: String) {
        if (e is CancellationException) throw e
        val text = if (e is IllegalArgumentException || e is IllegalStateException || e is java.io.IOException && e.message?.startsWith("GitHub") == true)
            e.message ?: fallback else fallback
        mutable.value = state.value.copy(busy = false, downloading = false, canDownload = false, canInstall = false, launchInstaller = false, message = text)
    }
}
