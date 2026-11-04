package de.parip69.homesprachsteuerung

import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.CopyOnWriteArrayList

internal class FibaroConnectionTrace : EventListener() {
    private val targets = CopyOnWriteArrayList<String>()
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        val ip = inetSocketAddress.address
        val family = if (ip is Inet4Address) "IPv4" else "IPv6"
        val address = ip?.hostAddress ?: "unaufgelöst"
        targets += "$family $address, Port ${inetSocketAddress.port}"
    }
    val description: String get() = if (targets.isEmpty()) "Kein TCP-Verbindungsaufbau protokolliert."
        else "Verbindungsziel: ${targets.distinct().joinToString("; ")}"
}

internal class FibaroProbeException(val connectionTrace: String, cause: IOException) : IOException(cause)
