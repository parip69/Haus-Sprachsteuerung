package de.parip69.homesprachsteuerung

import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class TransportTest {
    @Test fun fibaroSendsExactRawTextAsEscapedJsonAndBasicAuth() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val c = AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = server.url("/").toString(),
                variable = "Sprache / Küche", fibaroUser = "gerhard", fibaroPassword = "a:b")
            val raw = "  Licht \"Küche\" an!\nÄÖÜ ß  "
            val result = NetworkSender().send(c, raw)
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("PUT", request.method)
            assertEquals("/api/globalVariables/Sprache%20%2F%20K%C3%BCche", request.path)
            assertEquals(Credentials.basic("gerhard", "a:b", Charsets.UTF_8), request.getHeader("Authorization"))
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
            val json = JSONObject(request.body.readUtf8())
            assertEquals(1, json.length())
            assertEquals(raw, json.getString("value"))
            assertTrue(result.contains("204"))
        }
    }

    @Test fun rejectsUnauthorizedWithoutReportingSuccess() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            val error = runCatching {
                NetworkSender().send(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = server.url("/").toString(), fibaroUser = "u"), "Licht an")
            }.exceptionOrNull()
            assertTrue(error is HttpStatusException)
            assertEquals(401, (error as HttpStatusException).status)
        }
    }

    @Test fun redirectsAreNotFollowed() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/elsewhere"))
            val e = runCatching {
                NetworkSender().send(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = server.url("/").toString(), fibaroUser = "u"), "Licht an")
            }.exceptionOrNull()
            assertEquals(302, (e as HttpStatusException).status)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun validatesMqttPortsTopicsAndIdentity() {
        val c = AppConfig(protocol = Protocol.MQTT, brokerUri = "tcp://192.168.1.20:1883", clientId = "HV123")
        assertNull(c.validate())
        assertEquals("tcp://192.168.1.20:1883", c.mqttEndpoint())
        assertNotNull(c.copy(port = 8883).validate())
        assertNotNull(c.copy(topic = "home/+").validate())
        assertNotNull(c.copy(topic = "home/#").validate())
        assertNotNull(c.copy(clientId = "").validate())
        assertNotNull(c.copy(brokerUri = "tcp://user:pass@broker").validate())
        assertNotNull(c.copy(brokerUri = "https://broker").validate())
        assertNotNull(c.copy(port = 65536).validate())
        assertNotNull(c.copy(mqttPassword = "secret").validate())
        assertEquals("ssl://broker.example:8883",
            c.copy(brokerUri = "ssl://broker.example", port = 8883).mqttEndpoint())
    }

    @Test fun rejectsFibaroInvalidUrlsAndEmptyVariable() {
        assertNotNull(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = "not a url", fibaroUser = "u").validate())
        assertNotNull(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = "http://host/api", fibaroUser = "u").validate())
        assertNotNull(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = "http://u:p@host", fibaroUser = "u").validate())
        assertNotNull(AppConfig(protocol = Protocol.FIBARO, invokeScenes = false, fibaroUrl = "http://host", fibaroUser = "u", variable = "").validate())
    }
    @Test fun testModeWorksWithoutAnyNetworkSettings() {
        val c = AppConfig()
        assertEquals(Protocol.TEST, c.protocol)
        assertNull(c.validate())
        assertTrue(NetworkSender().send(c, "Küche an").contains("kein Versand"))
    }

    @Test fun invokeScenesIsAnOptionalJsonBoolean() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200))
            NetworkSender().send(AppConfig(protocol = Protocol.FIBARO,
                fibaroUrl = server.url("/").toString(), fibaroUser = "u", invokeScenes = true), "Licht an")
            val body = JSONObject(server.takeRequest(2, TimeUnit.SECONDS)!!.body.readUtf8())
            assertEquals("Licht an", body.getString("value"))
            assertEquals(true, body.get("invokeScenes"))
        }
    }

}
