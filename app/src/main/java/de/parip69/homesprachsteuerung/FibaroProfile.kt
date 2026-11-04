package de.parip69.homesprachsteuerung

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object FibaroDefaults {
    const val LOCAL_URL = "http://192.168.111.111:80"
    const val PUBLIC_URL = "http://n514fueyqz08svn0.myfritz.net:48884"
}

data class FibaroProfile(val url: String, val user: String, val password: String) {
    fun validationError(variable: String): String? = runCatching {
        endpoint(variable)
        require(user.isNotBlank()) { "Fibaro-Benutzername fehlt." }
        require(':' !in user && !user.any { it.isISOControl() }) {
            "Fibaro-Benutzername enthält unzulässige Zeichen."
        }
    }.exceptionOrNull()?.message

    fun endpoint(variable: String): HttpUrl {
        val base = url.toHttpUrlOrNull()
        require(base != null && base.username.isEmpty() && base.password.isEmpty() &&
            base.query == null && base.fragment == null) {
            "Gültige http(s)-Basis-URL ohne Zugangsdaten, Query oder Fragment eingeben."
        }
        require(variable.isNotBlank() && variable != "." && variable != ".." &&
            !variable.any { it.isISOControl() }) { "Gültigen Variablennamen eingeben." }
        val path = base.encodedPath.trimEnd('/')
        require(!path.endsWith("/api") && !path.contains("/api/globalVariables")) {
            "Nur die Basis-URL ohne /api/globalVariables und ohne /invoke eintragen."
        }
        return base.newBuilder().encodedPath(path + "/")
            .addPathSegments("api/globalVariables").addPathSegment(variable).build()
    }
}
