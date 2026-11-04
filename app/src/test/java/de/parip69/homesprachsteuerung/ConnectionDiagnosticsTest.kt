package de.parip69.homesprachsteuerung

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test

class ConnectionDiagnosticsTest {
    @Test fun nestedDnsFailureIsIdentifiedWithoutExposingMessages() {
        val text = ConnectionDiagnostics.describe(IOException("private-password", UnknownHostException("secret-host")))
        assertTrue(text.contains("DNS-Fehler")); assertTrue(text.contains("UnknownHostException"))
        assertFalse(text.contains("private-password")); assertFalse(text.contains("secret-host"))
    }
    @Test fun timeoutAndTlsAreNotMisreportedAsBadCredentials() {
        assertTrue(ConnectionDiagnostics.describe(SocketTimeoutException()).contains("Zeitüberschreitung"))
        assertTrue(ConnectionDiagnostics.describe(SSLHandshakeException("secret")).contains("TLS-Verbindungsaufbau"))
    }
    @Test fun connectionFailureKeepsOnlyWhitelistedNetworkCode() {
        val t = ConnectionDiagnostics.describe(ConnectException("ECONNREFUSED user:secret"))
        assertTrue(t.contains("TCP-Verbindung")); assertTrue(t.contains("ECONNREFUSED")); assertFalse(t.contains("user:secret"))
    }
    @Test fun androidNetworkPolicyAndPermissionDenialAreDistinct() {
        assertTrue(ConnectionDiagnostics.describe(IOException("CLEARTEXT secret not permitted")).contains("Android-Netzwerkrichtlinie"))
        assertTrue(ConnectionDiagnostics.describe(IOException("EACCES secret")).contains("verweigert"))
        assertTrue(ConnectionDiagnostics.describe(SecurityException("secret")).contains("verweigert"))
    }
    @Test fun unknownFailureStillShowsTypeWithoutLeakingMessage() {
        val t = ConnectionDiagnostics.describe(IOException("Authorization: Basic secret"))
        assertTrue(t.contains("IOException")); assertFalse(t.contains("Authorization")); assertFalse(t.contains("secret"))
    }
}
