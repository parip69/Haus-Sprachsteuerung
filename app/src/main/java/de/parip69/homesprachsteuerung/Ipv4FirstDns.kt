package de.parip69.homesprachsteuerung

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/** FRITZ IPv4 port mappings may not exist on the same hostname's IPv6 endpoint. */
internal class Ipv4FirstDns(private val delegate: Dns = Dns.SYSTEM) : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        delegate.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
}
