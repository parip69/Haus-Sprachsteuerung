package de.parip69.homesprachsteuerung

import java.io.EOFException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/** Never expose exception messages, credentials or response bodies in the diagnostic output. */
internal object ConnectionDiagnostics {
    fun describe(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.take(8).toList()
        val codes = listOf("ECONNREFUSED", "ENETUNREACH", "EHOSTUNREACH", "EACCES", "EPERM", "ECONNRESET")
            .filter { code -> causes.any { it.message?.contains(code) == true } }
        val reason = when {
            causes.any { it.message?.contains("CLEARTEXT", ignoreCase = true) == true } ->
                "HTTP wurde durch die Android-Netzwerkrichtlinie blockiert."
            causes.any { it is UnknownHostException } -> "DNS-Fehler: Der Servername konnte nicht aufgelöst werden."
            causes.any { it is SocketTimeoutException } -> "Zeitüberschreitung: Der Verbindungsaufbau oder die Antwort dauerte zu lange."
            causes.any { it is SSLHandshakeException } -> "TLS-Verbindungsaufbau fehlgeschlagen. HTTPS, Zertifikat und Zielport prüfen."
            causes.any { it is SSLException } -> "TLS-Fehler: HTTPS und Zielport passen möglicherweise nicht zusammen."
            causes.any { it is NoRouteToHostException } -> "Keine Netzwerkroute zum Zielserver."
            causes.any { it is SecurityException } || codes.any { it == "EACCES" || it == "EPERM" } ->
                "Netzwerkzugriff wurde auf dem Gerät verweigert. Berechtigungen, VPN oder Filter prüfen."
            causes.any { it is ConnectException } -> "TCP-Verbindung fehlgeschlagen. Erreichbarkeit und Portweiterleitung prüfen."
            causes.any { it is EOFException } -> "Der Zielserver hat die Verbindung vor einer vollständigen Antwort geschlossen."
            else -> "Netzwerkprüfung fehlgeschlagen; siehe Fehlertyp."
        }
        val types = causes.map { it.javaClass.simpleName }.distinct().joinToString(" → ")
        return reason + "\nFehlertyp: $types" + if (codes.isEmpty()) "" else "\nNetzwerkcode: ${codes.joinToString()}"
    }
}
