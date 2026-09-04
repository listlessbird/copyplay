package com.copyplay.data.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscalePolicy
import com.copyplay.domain.server.TailscaleStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class AndroidTailscaleDetector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TailscaleDetector {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override fun observe(): Flow<TailscaleStatus> = callbackFlow {
        fun refresh() {
            trySend(detectUncached())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = refresh()
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = refresh()
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        refresh()
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().flowOn(ioDispatcher)

    private fun detectUncached(): TailscaleStatus {
        val isInstalled = runCatching {
            context.packageManager.getPackageInfo(TailscalePolicy.PACKAGE_NAME, 0)
        }.isSuccess

        val activeNetwork = connectivityManager.activeNetwork
        val isActiveVpn = activeNetwork != null &&
            connectivityManager.getNetworkCapabilities(activeNetwork)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val tailnetLinkProperties = activeNetwork
            ?.takeIf { isActiveVpn }
            ?.let(connectivityManager::getLinkProperties)
            ?.takeIf { linkProperties ->
                linkProperties.linkAddresses.any { linkAddress ->
                    TailscalePolicy.isTailnetAddress(linkAddress.address)
                }
            }

        val dnsSearchDomains = tailnetLinkProperties
            ?.domains
            .orEmpty()
            .split(Regex("\\s+"))
            .map { it.trim().trimEnd('.').lowercase() }
            .filter { it.endsWith(".ts.net") }
            .distinct()

        return TailscalePolicy.resolve(
            isInstalled = isInstalled,
            hasTailnetAddress = tailnetLinkProperties != null,
            dnsSearchDomains = dnsSearchDomains,
        )
    }
}
