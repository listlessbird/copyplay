package com.copyplay.domain.server

import java.net.InetAddress

sealed interface TailscaleStatus {
    data object NotInstalled : TailscaleStatus
    data object Disconnected : TailscaleStatus
    data object Connected : TailscaleStatus
}

interface TailscaleDetector {
    suspend fun detect(): TailscaleStatus
}

object TailscalePolicy {
    const val PACKAGE_NAME = "com.tailscale.ipn"

    fun resolve(isInstalled: Boolean, hasTailnetAddress: Boolean): TailscaleStatus =
        when {
            !isInstalled -> TailscaleStatus.NotInstalled
            hasTailnetAddress -> TailscaleStatus.Connected
            else -> TailscaleStatus.Disconnected
        }

    fun isTailnetAddress(address: InetAddress): Boolean {
        val octets = address.address
        if (octets.size != 4) return false
        return octets[0].toInt() and 0xFF == 100 &&
            octets[1].toInt() and 0xFF in 64..127
    }
}
