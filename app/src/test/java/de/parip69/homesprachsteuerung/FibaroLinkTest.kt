package de.parip69.homesprachsteuerung

import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class FibaroLinkTest {
    @Test fun browserLinkSplitsCredentialsPortAndVariable() {
        val r = FibaroLink.parse(" http://name%40example.org:p%40ss%3Aword@host.example:48884/api/globalVariables/MyVariable\u00a0", "old")
        assertEquals("http://host.example:48884", r.baseUrl)
        assertEquals("name@example.org", r.username); assertEquals("p@ss:word", r.password)
        assertEquals("MyVariable", r.variable)
    }
    @Test fun decodingOccursOnceAndPlusIsNotChangedToSpace() {
        val r = FibaroLink.parse("http://a%2540b:p+%2525@host.example/api/globalVariables/My%20Variable", "old")
        assertEquals("a%40b", r.username); assertEquals("p+%25", r.password); assertEquals("My Variable", r.variable)
    }
    @Test fun baseLinkKeepsExistingVariableAndIpv6() {
        val r = FibaroLink.parse("http://user:pw@[2001:db8::1]:48884/", "Existing")
        assertEquals("http://[2001:db8::1]:48884", r.baseUrl); assertEquals("Existing", r.variable)
    }
    @Test fun unsupportedLinksFailWithoutEchoingCredentials() {
        for (url in listOf("http://user:secret@host.example/login", "http://user:secret@host.example/?token=secret", "http://host.example/", "http://user:secret@host.example/api/globalVariables/x/invoke/extra")) {
            val e = runCatching { FibaroLink.parse(url, "x") }.exceptionOrNull()
            assertTrue(e is IllegalArgumentException); assertFalse(e!!.message.orEmpty().contains("secret"))
        }
    }
    @Test fun importedLinkUsesSameCredentialsForReadOnlyApiRequest() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"name\":\"Example\",\"value\":\"\"}"))
            val url = server.url("/api/globalVariables/Example").newBuilder().username("user@example.org").password("p@ss:word").build()
            val r = FibaroLink.parse(url.toString(), "old")
            val c = AppConfig(protocol = Protocol.FIBARO, fibaroConnection = FibaroConnection.PUBLIC,
                publicFibaroUrl = r.baseUrl, publicFibaroUser = r.username, publicFibaroPassword = r.password, variable = r.variable)
            assertTrue(NetworkSender().testFibaro(c).contains("API erreichbar"))
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("GET", request.method); assertEquals("/api/globalVariables/Example", request.path)
            assertEquals(Credentials.basic("user@example.org", "p@ss:word", Charsets.UTF_8), request.getHeader("Authorization"))
            assertEquals(0L, request.bodySize); assertFalse(c.publicFibaroUrl.contains("user"))
        }
    }
    @Test fun invokeSuffixIsImportedAsJsonOptionInsteadOfEndpointSuffix() {
        val r = FibaroLink.parse("http://user:pw@host.example:48884/api/globalVariables/Example/invoke", "old")
        assertTrue(r.invokeScenes)
        assertEquals("Example", r.variable)
        assertEquals("/api/globalVariables/Example", FibaroProfile(r.baseUrl, r.username, r.password).endpoint(r.variable).encodedPath)
    }
    @Test fun importedCredentialsAlsoWorkForOnePutWithoutUserInfoInTarget() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val url = server.url("/api/globalVariables/Example/invoke").newBuilder().username("user@example.org").password("pw").build()
            val r = FibaroLink.parse(url.toString(), "old")
            val c = AppConfig(protocol = Protocol.FIBARO, fibaroConnection = FibaroConnection.PUBLIC,
                publicFibaroUrl = r.baseUrl, publicFibaroUser = r.username, publicFibaroPassword = r.password, variable = r.variable, invokeScenes = r.invokeScenes)
            NetworkSender().send(c, "Licht an")
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("PUT", request.method); assertEquals(Credentials.basic("user@example.org", "pw", Charsets.UTF_8), request.getHeader("Authorization"))
            assertEquals("/api/globalVariables/Example", request.path); assertEquals(1, server.requestCount)
            val body = org.json.JSONObject(request.body.readUtf8())
            assertTrue(body.getBoolean("invokeScenes")); assertEquals("Licht an", body.getString("value"))
        }
    }
    @Test fun plainBaseAddressDoesNotOverwriteExistingCredentials() {
        assertNull(FibaroLink.fromAddress("http://host.example:48884", "user", "pw", "Example"))
    }
    @Test fun oneAddressFieldCanImportApiPathUsingExistingCredentials() {
        val r = FibaroLink.fromAddress("http://host.example:48884/api/globalVariables/NewVariable", "user@example.org", "pw", "old")!!
        assertEquals("http://host.example:48884", r.baseUrl)
        assertEquals("user@example.org", r.username); assertEquals("pw", r.password); assertEquals("NewVariable", r.variable)
    }
    @Test fun oneAddressFieldAcceptsBrowserLinkAndRemovesUserInfoFromResult() {
        val r = FibaroLink.fromAddress("http://user@example.org:pw@host.example:48884/api/globalVariables/Example/invoke", "old", "old", "old")!!
        assertEquals("user@example.org", r.username); assertEquals("pw", r.password)
        assertFalse(r.baseUrl.contains("@")); assertTrue(r.invokeScenes)
    }
}
