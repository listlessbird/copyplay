package com.copyplay.domain.server

import java.net.InetAddress
import kotlinx.coroutines.flow.Flow

sealed interface TailscaleStatus {
    data object NotInstalled : TailscaleStatus
    data object Disconnected : TailscaleStatus
    data class Connected(
        val dnsSearchDomains: List<String>,
    ) : TailscaleStatus
}

interface TailscaleDetector {
    fun observe(): Flow<TailscaleStatus>
}

object TailscalePolicy {
    const val PACKAGE_NAME = "com.tailscale.ipn"

    fun resolve(
        isInstalled: Boolean,
        hasTailnetAddress: Boolean,
        dnsSearchDomains: List<String> = emptyList(),
    ): TailscaleStatus =
        when {
            !isInstalled -> TailscaleStatus.NotInstalled
            hasTailnetAddress -> TailscaleStatus.Connected(dnsSearchDomains)
            else -> TailscaleStatus.Disconnected
        }

    fun isTailnetAddress(address: InetAddress): Boolean {
        val octets = address.address
        return when (octets.size) {
            4 -> octets[0].toInt() and 0xFF == 100 &&
                octets[1].toInt() and 0xFF in 64..127

            16 -> octets[0].toInt() and 0xFF == 0xFD &&
                octets[1].toInt() and 0xFF == 0x7A &&
                octets[2].toInt() and 0xFF == 0x11 &&
                octets[3].toInt() and 0xFF == 0x5C &&
                octets[4].toInt() and 0xFF == 0xA1 &&
                octets[5].toInt() and 0xFF == 0xE0

            else -> false
        }
    }
}
