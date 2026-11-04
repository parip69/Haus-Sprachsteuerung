package de.parip69.homesprachsteuerung

import com.android.apksig.ApkVerifier
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class UpdatePolicyTest {
    private val hash = "a".repeat(64)
    private fun metadata(code: Long = 26) = UpdateMetadata(UPDATE_PACKAGE, code, "1.1.15", "Alfonso-1.1.15-26.apk", hash)
    private fun asset(name: String, size: Long = 10) = JSONObject().put("name", name)
        .put("size", size).put("browser_download_url", "${RELEASE_ROOT}v1.1.15/$name")
    private fun release(draft: Boolean = false, pre: Boolean = false) = JSONObject()
        .put("draft", draft).put("prerelease", pre).put("published_at", "2026-11-04T00:00:00Z")
        .put("body", "Updatefunktion").put("assets", org.json.JSONArray()
            .put(asset("alfonso-update.json", 400)).put(asset(metadata().apkFileName))).toString()
    private fun rejects(block: () -> Unit) { assertTrue(runCatching(block).isFailure) }
    private fun identity(meta: UpdateMetadata = metadata(), installed: Long = 25,
        pkg: String = UPDATE_PACKAGE, code: Long = meta.versionCode, version: String = meta.versionName,
        signers: Set<String> = setOf(UPDATE_CERT)) = UpdatePolicy.verifyIdentity(meta, installed, pkg, code, version, signers)

    @Test fun numericVersionCodeWinsRegardlessOfDisplayVersion() {
        identity(metadata(100).copy(versionName = "0.1"), 99, version = "0.1")
        rejects { identity(installed = 26) }; rejects { identity(installed = 27) }
        identity(metadata(Int.MAX_VALUE.toLong() + 1), Int.MAX_VALUE.toLong())
    }
    @Test fun exactPackageVersionAndSingleOriginalSignerRequired() {
        identity()
        rejects { identity(pkg = "another.app") }; rejects { identity(code = 27) }
        rejects { identity(version = "unexpected") }; rejects { identity(signers = emptySet()) }
        rejects { identity(signers = setOf(hash)) }; rejects { identity(signers = setOf(hash, UPDATE_CERT)) }
    }
    @Test fun stablePublishedReleaseOnly() {
        assertNotNull(UpdatePolicy.release(release()))
        assertNull(UpdatePolicy.release(release(draft = true)))
        assertNull(UpdatePolicy.release(release(pre = true)))
        rejects { UpdatePolicy.release(JSONObject(release()).put("published_at", JSONObject.NULL).toString()) }
    }
    @Test fun actualUniqueAssetUrlRequired() {
        val r = UpdatePolicy.release(release())!!
        assertEquals("${RELEASE_ROOT}v1.1.15/${metadata().apkFileName}", UpdatePolicy.asset(r, metadata().apkFileName).url)
        rejects { UpdatePolicy.asset(r, "guessed.apk") }
        rejects { UpdatePolicy.asset(r.copy(assets = r.assets + r.assets.last()), metadata().apkFileName) }
        val good = UpdateAsset("a.apk", "${RELEASE_ROOT}tag/a.apk", 20)
        for (bad in listOf(good.copy(url = "http://github.com/a.apk"), good.copy(url = "https://evil.example/a.apk"),
            good.copy(url = "https://user@github.com/parip69/Haus-Sprachsteuerung/releases/download/tag/a.apk"),
            good.copy(size = 0), good.copy(size = MAX_APK_BYTES + 1))) {
            rejects { UpdatePolicy.asset(UpdateRelease("", listOf(bad)), "a.apk") }
        }
    }
    @Test fun metadataRoundTripsAndRejectsInvalidFields() {
        assertEquals(metadata(), UpdateMetadata.parse(metadata().json()))
        for (text in listOf("{}", metadata().copy(applicationId = "wrong").json(),
            metadata().copy(apkFileName = "../a.apk").json(), metadata().copy(sha256 = "bad").json(),
            metadata(0).json(), JSONObject(metadata().json()).put("versionCode", 26.5).toString())) {
            rejects { UpdateMetadata.parse(text) }
        }
    }
    @Test fun fullFileLengthAndSha256Required() {
        val f = File.createTempFile("alfonso", ".apk")
        try {
            f.writeText("original")
            val sha = UpdatePolicy.hex(MessageDigest.getInstance("SHA-256").digest(f.readBytes()))
            UpdatePolicy.verifyFile(f, f.length(), sha)
            rejects { UpdatePolicy.verifyFile(f, f.length() + 1, sha) }
            f.writeText("modified"); rejects { UpdatePolicy.verifyFile(f, f.length(), sha) }
            f.writeBytes(byteArrayOf()); rejects { UpdatePolicy.verifyFile(f, 0, sha) }
        } finally { f.delete() }
    }
    private fun manifest() = JSONObject(metadata().json()).put("channel", "stable").put("size", 123)
        .put("downloadUrl", REPOSITORY_DOWNLOADS + metadata().apkFileName).put("notes", "Updatefunktion")
    private fun source(status: Int = 200, offline: Boolean = false, body: String = manifest().toString()) = UpdateSource(OkHttpClient.Builder()
        .addInterceptor { chain ->
            if (offline) throw IOException("offline")
            val req = chain.request()
            assertEquals("raw.githubusercontent.com", req.url.host)
            assertEquals("/parip69/Haus-Sprachsteuerung/main/downloads/alfonso-update.json", req.url.encodedPath)
            assertNotNull(req.url.queryParameter("check"))
            assertEquals("no-cache, no-store", req.header("Cache-Control"))
            Response.Builder().request(req).protocol(okhttp3.Protocol.HTTP_1_1).code(status).message("test")
                .body(body.toResponseBody()).build()
        }.build())
    @Test fun fetchesManifestAndActualApkAsset() {
        val result = source().latest(25)!!
        assertEquals(metadata(), result.first); assertEquals(metadata().apkFileName, result.second.name)
    }
    @Test fun noReleaseRateLimitsServerErrorAndOfflineHandled() {
        assertNull(source(404).latest(25))
        for (status in listOf(403, 429, 500)) rejects { source(status).latest(25) }
        rejects { source(offline = true).latest(25) }
    }
    @Test fun repositoryManifestRequiresStableChannelAndValidDownload() {
        assertNull(source(body = manifest().put("channel", "preview").toString()).latest(25))
        assertNull(source(body = manifest().put("channel", "draft").toString()).latest(25))
        for (url in listOf("http://evil.example/app.apk", "https://raw.githubusercontent.com/other/repo/main/a.apk",
            REPOSITORY_DOWNLOADS + "wrong.apk", REPOSITORY_DOWNLOADS + metadata().apkFileName + "?x=1")) {
            rejects { source(body = manifest().put("downloadUrl", url).toString()).latest(25) }
        }
        for (size in listOf(0L, -1L, MAX_APK_BYTES + 1)) {
            rejects { source(body = manifest().put("size", size).toString()).latest(25) }
        }
        rejects { source(body = "not json").latest(25) }
        rejects { source(body = "x".repeat(65537)).latest(25) }
    }
    @Test fun realSignedApkPassesAndDamagedApksFail() {
        val apk = File(requireNotNull(System.getProperty("alfonso.testApk")))
        assertTrue("assembleRelease fixture missing", apk.isFile)
        val result = ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(26).build().verify()
        assertTrue(result.errors.toString(), result.isVerified)
        assertEquals(setOf(UPDATE_CERT), result.signerCertificates.map {
            UpdatePolicy.hex(MessageDigest.getInstance("SHA-256").digest(it.encoded))
        }.toSet())
        val bad = File.createTempFile("alfonso-broken", ".apk")
        try {
            bad.writeBytes(apk.readBytes().copyOf(100))
            assertFalse(runCatching { ApkVerifier.Builder(bad).build().verify().isVerified }.getOrDefault(false))
            val bytes = apk.readBytes(); bytes[1000] = (bytes[1000].toInt() xor 1).toByte(); bad.writeBytes(bytes)
            assertFalse(runCatching { ApkVerifier.Builder(bad).build().verify().isVerified }.getOrDefault(false))
        } finally { bad.delete() }
    }
}
