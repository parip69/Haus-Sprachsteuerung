package de.parip69.homesprachsteuerung

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal const val UPDATE_PACKAGE = "de.parip69.homesprachsteuerung"
internal const val UPDATE_CERT = "c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564"
internal const val RELEASE_ROOT = "https://github.com/parip69/Haus-Sprachsteuerung/releases/download/"
internal const val MAX_APK_BYTES = 200L * 1024 * 1024

internal data class UpdateAsset(val name: String, val url: String, val size: Long)
internal data class UpdateRelease(val notes: String, val assets: List<UpdateAsset>)
internal data class UpdateMetadata(val applicationId: String, val versionCode: Long,
    val versionName: String, val apkFileName: String, val sha256: String) {
    fun json() = JSONObject().put("applicationId", applicationId).put("versionCode", versionCode)
        .put("versionName", versionName).put("apkFileName", apkFileName).put("sha256", sha256).toString()
    companion object {
        fun parse(text: String): UpdateMetadata {
            val j = JSONObject(text)
            val raw = j.get("versionCode").toString()
            require(raw.matches(Regex("[1-9][0-9]*"))) { "Ungültiger versionCode im Release." }
            return UpdateMetadata(j.getString("applicationId"), raw.toLong(), j.getString("versionName"),
                j.getString("apkFileName"), j.getString("sha256").lowercase()).also {
                require(it.applicationId == UPDATE_PACKAGE && it.versionName.isNotBlank() &&
                    it.apkFileName.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*\\.apk")) &&
                    it.sha256.matches(Regex("[a-f0-9]{64}"))) { "Unpassende oder unvollständige Update-Metadaten." }
            }
        }
    }
}

internal object UpdatePolicy {
    fun release(text: String): UpdateRelease? {
        val j = JSONObject(text)
        if (j.optBoolean("draft", true) || j.optBoolean("prerelease", true)) return null
        require(!j.isNull("published_at") && j.getString("published_at").isNotBlank()) { "Release ist nicht veröffentlicht." }
        val assets = j.getJSONArray("assets")
        return UpdateRelease(j.optString("body", "").take(20000), (0 until assets.length()).map { i ->
            val a = assets.getJSONObject(i)
            UpdateAsset(a.getString("name"), a.getString("browser_download_url"), a.getLong("size"))
        })
    }
    fun asset(release: UpdateRelease, name: String): UpdateAsset {
        val asset = release.assets.singleOrNull { it.name == name }
            ?: throw IOException("Im Release fehlt ein eindeutiges Asset: $name")
        val url = asset.url.toHttpUrl()
        require(asset.url.startsWith(RELEASE_ROOT) && url.isHttps && url.username.isEmpty() &&
            url.password.isEmpty() && url.port == 443 && url.query == null && url.fragment == null &&
            asset.size in 1..MAX_APK_BYTES) { "Ungültige Release-Datei oder Downloadadresse." }
        return asset
    }
    fun verifyIdentity(meta: UpdateMetadata, installed: Long, packageName: String, code: Long,
        versionName: String?, signers: Set<String>) {
        require(packageName == UPDATE_PACKAGE && meta.applicationId == packageName) { "Die APK gehört zu einer anderen App." }
        require(code == meta.versionCode && code > installed && versionName == meta.versionName) { "Die APK ist nicht die erwartete neuere Version." }
        require(signers == setOf(UPDATE_CERT)) { "Die APK hat eine abweichende Signatur." }
    }
    fun verifyFile(file: File, size: Long, sha256: String) {
        require(file.isFile && file.length() == size && size in 1..MAX_APK_BYTES) { "Der APK-Download ist unvollständig." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        require(hex(digest.digest()) == sha256) { "Die APK ist beschädigt (SHA-256 stimmt nicht)." }
    }
    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}

internal class UpdateSource(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
    .callTimeout(60, TimeUnit.SECONDS).followSslRedirects(false).build()) {
    fun latest(installed: Long): Triple<UpdateMetadata, UpdateAsset, String>? {
        val text = get("https://api.github.com/repos/parip69/Haus-Sprachsteuerung/releases/latest", true) ?: return null
        val release = UpdatePolicy.release(text) ?: return null
        val manifest = UpdatePolicy.asset(release, "alfonso-update.json")
        require(manifest.size <= 65536) { "Die Update-Metadaten sind zu groß." }
        val metadata = UpdateMetadata.parse(get(manifest.url, false)!!)
        val apk = UpdatePolicy.asset(release, metadata.apkFileName)
        // Numeric comparison only; tags and display names are never interpreted as versions.
        require(metadata.versionCode > 0 && installed >= 0)
        return Triple(metadata, apk, release.notes)
    }
    private fun get(url: String, releaseRequest: Boolean): String? {
        val request = Request.Builder().url(url).header("Accept", if (releaseRequest) "application/vnd.github+json" else "application/octet-stream")
            .header("User-Agent", "Alfonso-Android-Updater").build()
        client.newCall(request).execute().use { response ->
            if (releaseRequest && response.code == 404) return null
            if (response.code == 403 || response.code == 429) throw IOException("GitHub-Zugriff begrenzt. Bitte später erneut versuchen. Bei privaten Repositories sind öffentliche Release-Dateien erforderlich.")
            if (!response.isSuccessful) throw IOException("GitHub antwortet mit HTTP ${response.code}. Bitte später erneut versuchen.")
            val body = response.body ?: throw IOException("GitHub liefert keine Daten.")
            val limit = if (releaseRequest) 1024 * 1024 else 65536
            val bytes = body.byteStream().use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= limit) {
                    val n = input.read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
                    if (n < 0) break
                    output.write(buffer, 0, n)
                }
                output.toByteArray()
            }
            require(bytes.size <= limit) { "GitHub-Antwort ist zu groß." }
            return bytes.toString(Charsets.UTF_8)
        }
    }
}
