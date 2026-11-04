package de.parip69.homesprachsteuerung

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Browser-style URL input is decoded once, then stored as ordinary isolated profile fields. */
internal object FibaroLink {
    data class Imported(val baseUrl: String, val username: String, val password: String, val variable: String, val invokeScenes: Boolean)

    /** Plain base address leaves existing fields untouched; full links are imported in place. */
    fun fromAddress(input: String, username: String, password: String, variable: String): Imported? {
        val url = input.trim().toHttpUrlOrNull() ?: return null
        if (url.username.isEmpty() && url.password.isEmpty() && url.encodedPath.trimEnd('/').isEmpty()) return null
        val link = if (url.username.isNotEmpty()) url else url.newBuilder().username(username).password(password).build()
        return parse(link.toString(), variable)
    }

    fun parse(input: String, currentVariable: String): Imported {
        val url = input.trim().toHttpUrlOrNull()
        require(url != null) { "Vollständigen http(s)-Link eingeben." }
        require(url.username.isNotBlank()) { "Der Link muss den Fibaro-Benutzernamen vor dem @ enthalten." }
        require(url.query == null && url.fragment == null) { "Link ohne Query oder Fragment verwenden." }
        val originalPath = url.pathSegments.dropLastWhile { it.isEmpty() }
        val invokeScenes = originalPath.size == 4 && originalPath.last() == "invoke"
        val path = if (invokeScenes) originalPath.dropLast(1) else originalPath
        val variable = when {
            path.isEmpty() -> currentVariable
            path.size == 3 && path[0] == "api" && path[1] == "globalVariables" -> path[2]
            else -> throw IllegalArgumentException("Nur Basis-Link oder /api/globalVariables/Variablenname übernehmen; optional mit /invoke.")
        }
        val base = url.newBuilder().username("").password("").encodedPath("/").build().toString().removeSuffix("/")
        val result = Imported(base, url.username, url.password, variable, invokeScenes)
        val error = FibaroProfile(base, result.username, result.password).validationError(variable)
        require(error == null) { "Benutzername oder Variablenname im Link ist ungültig." }
        return result
    }
}
