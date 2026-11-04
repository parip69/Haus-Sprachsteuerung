package de.parip69.homesprachsteuerung

import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class FibaroRoutingTest {
    private fun config(local: MockWebServer, public: MockWebServer, mode: FibaroConnection) = AppConfig(
        protocol = Protocol.FIBARO, fibaroConnection = mode, variable = "HomeAssistant69",
        fibaroUrl = local.url("/").toString(), fibaroUser = "local-user", fibaroPassword = "local-secret",
        publicFibaroUrl = public.url("/").toString(), publicFibaroUser = "public-user", publicFibaroPassword = "public-secret")
    private fun variable() = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"name":"HomeAssistant69","value":"old"}""")

    @Test fun explicitLocalDoesNotTouchPublicProfile() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(MockResponse().setResponseCode(204))
            val result = NetworkSender().send(config(local, public, FibaroConnection.LOCAL), "Licht an")
            val request = local.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("PUT", request.method)
            assertEquals(Credentials.basic("local-user", "local-secret"), request.getHeader("Authorization"))
            assertEquals(0, public.requestCount)
            assertTrue(result.contains("Lokal"))
        } }
    }
    @Test fun explicitPublicUsesItsOwnAddressAndCredentials() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            public.enqueue(MockResponse().setResponseCode(204))
            val c = config(local, public, FibaroConnection.PUBLIC).copy(fibaroUrl = "invalid", fibaroUser = "")
            val raw = "  Rollo \"Küche\" öffnen  "
            val result = NetworkSender().send(c, raw)
            val request = public.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("PUT", request.method)
            assertEquals("/api/globalVariables/HomeAssistant69", request.path)
            assertEquals(Credentials.basic("public-user", "public-secret"), request.getHeader("Authorization"))
            assertEquals(raw, JSONObject(request.body.readUtf8()).getString("value"))
            assertEquals(0, local.requestCount)
            assertTrue(result.contains("Öffentlich"))
        } }
    }
    @Test fun autoPrefersConfirmedLocalApiUsingReadOnlyProbeThenOnePut() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(variable()); local.enqueue(MockResponse().setResponseCode(204))
            NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch")
            assertEquals("GET", local.takeRequest(2, TimeUnit.SECONDS)!!.method)
            assertEquals("PUT", local.takeRequest(2, TimeUnit.SECONDS)!!.method)
            assertEquals(2, local.requestCount); assertEquals(0, public.requestCount)
        } }
    }
    @Test fun failedReadProbeSelectsPublicBeforeAnyPut() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(MockResponse().setResponseCode(503)); public.enqueue(MockResponse().setResponseCode(204))
            NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch")
            assertEquals("GET", local.takeRequest(2, TimeUnit.SECONDS)!!.method)
            val request = public.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("PUT", request.method)
            assertEquals(Credentials.basic("public-user", "public-secret"), request.getHeader("Authorization"))
            assertEquals(1, local.requestCount); assertEquals(1, public.requestCount)
        } }
    }
    @Test fun htmlOnLocalAddressIsNotMistakenForTheHomeApi() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(MockResponse().setBody("<html>Login</html>")); public.enqueue(MockResponse().setResponseCode(204))
            val result = NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch")
            assertTrue(result.contains("Öffentlich"))
            assertEquals(1, local.requestCount); assertEquals(1, public.requestCount)
        } }
    }
    @Test fun failedPutNeverTriggersReplayViaPublicProfile() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(variable()); local.enqueue(MockResponse().setResponseCode(500))
            val error = runCatching { NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch") }.exceptionOrNull()
            assertEquals(500, (error as HttpStatusException).status)
            assertEquals(2, local.requestCount); assertEquals(0, public.requestCount)
        } }
    }
    @Test fun authenticationFailureStaysOnSelectedLocalConnection() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(MockResponse().setResponseCode(401)); local.enqueue(MockResponse().setResponseCode(401))
            val error = runCatching { NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch") }.exceptionOrNull()
            assertEquals(401, (error as HttpStatusException).status)
            assertEquals(2, local.requestCount); assertEquals(0, public.requestCount)
        } }
    }
    @Test fun connectionTestReadsVariableWithoutChangingAnything() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            public.enqueue(variable())
            val result = NetworkSender().testFibaro(config(local, public, FibaroConnection.PUBLIC))
            val request = public.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("GET", request.method); assertEquals(0L, request.bodySize)
            assertTrue(result.contains("API erreichbar")); assertEquals(1, public.requestCount)
            assertEquals(0, local.requestCount)
        } }
    }
    @Test fun loginHtmlIsNeverReportedAsSuccessfulSend() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            public.enqueue(MockResponse().setBody("<!DOCTYPE html><html>FRITZ!Box Login</html>"))
            val error = runCatching { NetworkSender().send(config(local, public, FibaroConnection.PUBLIC), "Rollo hoch") }.exceptionOrNull()
            assertTrue(error is UnexpectedFibaroResponseException)
            assertEquals(DeliveryKind.UNCERTAIN, DeliveryFeedback.failureKind(error as Exception))
            public.enqueue(MockResponse().setBody("<html>FRITZ!Box Login</html>"))
            assertTrue(NetworkSender().testFibaro(config(local, public, FibaroConnection.PUBLIC)).contains("FRITZ!Box-Webseite"))
        } }
    }
    @Test fun unresponsiveLocalReadTimesOutBeforePublicSend() {
        MockWebServer().use { local -> MockWebServer().use { public ->
            local.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            public.enqueue(MockResponse().setResponseCode(204))
            val result = NetworkSender().send(config(local, public, FibaroConnection.AUTO), "Rollo hoch")
            assertTrue(result.contains("Öffentlich"))
            assertEquals(1, local.requestCount); assertEquals(1, public.requestCount)
        } }
    }
    @Test fun profilesValidateIndependentlyAndPublicPortIsCorrect() {
        assertTrue(FibaroDefaults.PUBLIC_URL.endsWith(":48884"))
        val c = AppConfig(protocol = Protocol.FIBARO, fibaroConnection = FibaroConnection.AUTO,
            publicFibaroUrl = "http://public.example:48884", publicFibaroUser = "public")
        assertNull(c.validate())
        assertNotNull(c.copy(fibaroConnection = FibaroConnection.LOCAL).validate())
        assertNotNull(c.copy(publicFibaroUser = "").validate())
        assertNotNull(c.copy(publicFibaroUrl = "http://u:p@public.example").validate())
        assertNotNull(c.copy(publicFibaroUrl = "http://public.example/api/globalVariables/HomeAssistant69/invoke").validate())
    }
}
