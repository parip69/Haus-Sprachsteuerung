package de.parip69.homesprachsteuerung

import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.io.IOException
import java.util.concurrent.TimeUnit

class Ipv4FirstDnsTest {
    private fun stubDns(block: () -> List<InetAddress>) = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = block()
    }
    private val v4 = InetAddress.getByName("127.0.0.1")
    private val v6 = InetAddress.getByName("::1")
    @Test fun ipv4IsPreferredWithoutDroppingOrReorderingSameFamilyAddresses() {
        val other = InetAddress.getByName("127.0.0.2")
        val dns = Ipv4FirstDns(stubDns { listOf(v6, v4, other) })
        assertEquals(listOf(v4, other, v6), dns.lookup("example.invalid"))
    }
    @Test fun ipv6OnlyRemainsUsableAndDnsErrorsPropagate() {
        assertEquals(listOf(v6), Ipv4FirstDns(stubDns { listOf(v6) }).lookup("example.invalid"))
        assertTrue(runCatching { Ipv4FirstDns(stubDns { throw java.net.UnknownHostException() }).lookup("invalid") }.exceptionOrNull() is java.net.UnknownHostException)
    }
    private fun client() = NetworkSender.defaultHttpClient().newBuilder()
        .dns(Ipv4FirstDns(stubDns { listOf(v6, v4) })).connectTimeout(1, TimeUnit.SECONDS).build()
    private fun config(port: Int) = AppConfig(protocol = Protocol.FIBARO, fibaroConnection = FibaroConnection.PUBLIC,
        publicFibaroUrl = "http://example.invalid:$port", publicFibaroUser = "u", publicFibaroPassword = "p", variable = "Example")
    @Test fun dualStackReadUsesWorkingIpv4AndReportsActualTarget() {
        MockWebServer().use { server ->
            server.start(v4, 0); server.enqueue(MockResponse().setBody("{\"name\":\"Example\",\"value\":\"\"}"))
            val result = NetworkSender(client()).testFibaro(config(server.port))
            assertTrue(result.contains("API erreichbar")); assertTrue(result.contains("IPv4 127.0.0.1"))
            assertEquals("GET", server.takeRequest(2, TimeUnit.SECONDS)!!.method); assertEquals(1, server.requestCount)
        }
    }
    @Test fun possiblyDeliveredPutIsNeverRepeatedAfterConnectionDrops() {
        MockWebServer().use { server ->
            server.start(v4, 0); server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            assertTrue(runCatching { NetworkSender(client()).send(config(server.port), "Licht an") }.exceptionOrNull() is IOException)
            assertEquals("PUT", server.takeRequest(2, TimeUnit.SECONDS)!!.method)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun failedReadKeepsConnectionTargetInDiagnosticException() {
        MockWebServer().use { server ->
            server.start(v4, 0); val port = server.port; server.shutdown()
            val error = runCatching { NetworkSender(client()).testFibaro(config(port)) }.exceptionOrNull()
            assertTrue(error is FibaroProbeException)
            assertTrue((error as FibaroProbeException).connectionTrace.contains("IPv4 127.0.0.1"))
            assertTrue(ConnectionDiagnostics.describe(error).contains("TCP-Verbindung"))
        }
    }
}
