package de.parip69.homesprachsteuerung

import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Real loopback socket: verifies MQTT wire payload, flags and PUBACK handling. */
class MqttContractTest {
    private data class Packet(val header: Int, val body: ByteArray)
    private fun readPacket(input: InputStream): Packet {
        val header = input.read()
        check(header >= 0) { "Unexpected EOF" }
        var length = 0
        var factor = 1
        var digit: Int
        do {
            digit = input.read()
            check(digit >= 0 && factor <= 128 * 128 * 128)
            length += (digit and 127) * factor
            factor *= 128
        } while (digit and 128 != 0)
        require(length <= 65536)
        val body = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val n = input.read(body, offset, length - offset)
            check(n > 0)
            offset += n
        }
        return Packet(header, body)
    }

    @Test fun publishesExactUtf8WithQosOneAndNoRetain() {
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<Pair<String, String>> {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val input = socket.getInputStream()
                        val output = socket.getOutputStream()
                        assertEquals(0x10, readPacket(input).header) // CONNECT
                        output.write(byteArrayOf(0x20, 0x02, 0x00, 0x00)); output.flush()
                        val p = readPacket(input)
                        assertEquals(0x32, p.header) // PUBLISH, DUP=0, QoS=1, RETAIN=0
                        val topicLength = ((p.body[0].toInt() and 255) shl 8) or (p.body[1].toInt() and 255)
                        val topic = String(p.body, 2, topicLength, Charsets.UTF_8)
                        val idOffset = 2 + topicLength
                        val payload = String(p.body, idOffset + 2, p.body.size - idOffset - 2, Charsets.UTF_8)
                        output.write(byteArrayOf(0x40, 0x02, p.body[idOffset], p.body[idOffset + 1]))
                        output.flush()
                        // Read graceful disconnect so the test broker doesn't cause a false connection loss.
                        assertEquals(0xE0, readPacket(input).header)
                        topic to payload
                    }
                }
                val raw = "  Küche \"hell\" – Grüße!\n  "
                val result = NetworkSender().send(AppConfig(protocol = Protocol.MQTT,
                    brokerUri = "tcp://127.0.0.1", port = server.localPort,
                    clientId = "HVtest123", topic = "haus/sprache"), raw)
                assertTrue(result.contains("bestätigt"))
                assertEquals("haus/sprache" to raw, received.get(8, TimeUnit.SECONDS))
            } finally { worker.shutdownNow() }
        }
    }

    @Test fun brokerAuthenticationRejectionIsAnError() {
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val worker = Executors.newSingleThreadExecutor()
            try {
                val broker = worker.submit {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        readPacket(socket.getInputStream())
                        socket.getOutputStream().write(byteArrayOf(0x20, 0x02, 0x00, 0x05))
                        socket.getOutputStream().flush()
                    }
                }
                val error = runCatching { NetworkSender().send(AppConfig(protocol = Protocol.MQTT,
                    brokerUri = "tcp://127.0.0.1", port = server.localPort,
                    clientId = "HVtest123"), "Licht an") }.exceptionOrNull()
                assertTrue(error is MqttException)
                assertEquals(5, (error as MqttException).reasonCode)
                broker.get(8, TimeUnit.SECONDS)
            } finally { worker.shutdownNow() }
        }
    }
}
